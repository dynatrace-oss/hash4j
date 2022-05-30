from os import listdir
from os.path import isfile, join
from pathlib import Path
from collections import namedtuple
from collections import defaultdict
import matplotlib
matplotlib.use("PDF")
import matplotlib.pyplot as plt
import git
import time

git_repo = git.Repo(".")
Record = namedtuple('Record', ['commit_date', 'exec_date', 'revision', 'algorithm', 'test', 'avg', 'std'])
DateCommit = namedtuple('DateCommit', ['commit_date', 'revision', 'exec_date'])

def parse_exec_date_from_benchmark_result_file(file_name):
    s = list(file_name.split()[0])
    s[-3] = ":"
    s[-6] = ":"
    s[-9] = " "
    return "".join(s)

def parse_revision_from_benchmark_result_file(file_name):
    return file_name.split(" ")[1].split(".")[0]

def get_commit_date(commit):
    return time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(git_repo.commit(commit).committed_date))

def read_data(bechmark_result_directory):

    bechmark_result_path = Path(bechmark_result_directory)
    benchmark_result_files = [f for f in listdir(bechmark_result_path) if isfile(join(bechmark_result_path, f)) and f.endswith(".txt")]

    data = []

    for benchmark_result_file in benchmark_result_files:
        exec_date = parse_exec_date_from_benchmark_result_file(benchmark_result_file)
        revision = parse_revision_from_benchmark_result_file(benchmark_result_file)
        commit_date = get_commit_date(revision)

        with open(bechmark_result_path / benchmark_result_file) as file:
            lines = file.readlines()
            for line in lines[1:]:
                split_line = line.split()
                algorithm = split_line[0].split(".")[0]
                test = split_line[0].split(".")[1]
                avg = float(split_line[3])
                std = float(split_line[5])
                data.append(Record(commit_date=commit_date, exec_date=exec_date, revision=revision, algorithm=algorithm, test=test, avg=avg, std=std))
    return data

def split_data_by_test(data):
    result = defaultdict(list)
    for d in data:
        result[d.test].append(d)
    return result

def split_data_by_algorithm(data):
    result = defaultdict(list)
    for d in data:
        result[d.algorithm].append(d)
    return result

def format_label(label_data):
    return "e:" +  label_data.exec_date + " c: " + label_data.commit_date + "\n" + label_data.revision

def plot_algorithm(ax, labels, algorithm, data):

    values = {DateCommit(commit_date = d.commit_date, revision= d.revision, exec_date = d.exec_date) : d for d in data}

    label_strings = []
    y_values = []

    for l in labels:
        if l in values:
            label_strings.append(format_label(l))
            y_values.append(values[l].avg)

    if algorithm.startswith('FarmHash'):
        color = "blue"
    elif algorithm.startswith('Komihash'):
        color = "green"
    elif algorithm.startswith('Murmur3_128'):
        color = "black"
    elif algorithm.startswith('Murmur3_32'):
        color = "red"
    elif algorithm.startswith('XXH3'):
        color = "brown"
    elif algorithm.startswith('Wyhash'):
        color = "magenta"
    elif algorithm.startswith('UnorderedHashTest'):
        color = "black"
    else:
        assert(False)

    linestyle = "solid"
    if 'ZeroAllocationHashing' in algorithm:
        linestyle = "dotted"
    elif 'Guava' in algorithm:
        linestyle = "dashed"
    elif 'GreenrobotEssentials' in algorithm:
        linestyle = (0, (3, 1, 1, 1, 1, 1))


    ax.plot(label_strings, y_values, label=algorithm, color=color, linestyle=linestyle, marker='o')

def make_chart(test, data, output_path):
    labels = sorted(set(DateCommit(commit_date = d.commit_date, revision= d.revision, exec_date = d.exec_date) for d in data))
    label_strings = [format_label(l) for l in labels]

    splitted_data_by_algorithm = split_data_by_algorithm(data)

    fig, ax = plt.subplots(1, 1)
    fig.set_size_inches(24,18)

    ax.plot(label_strings, [None for _ in label_strings], alpha=0.0)

    for algorithm in splitted_data_by_algorithm:
        plot_algorithm(ax, labels, algorithm, splitted_data_by_algorithm[algorithm])


    ax.legend(loc = "lower center", bbox_to_anchor=(0.5, -0.37), ncol=4, handlelength=8)

    fig.subplots_adjust(top=0.95, bottom=0.27, left=0.05, right=0.95)

    ax.set_title(test)
    ax.set_ylim([0, ax.get_ylim()[1]])
    ax.xaxis.set_tick_params(rotation=90)
    fig.savefig(output_path / (test + ".pdf"), format='pdf', dpi=1600, metadata={'creationDate': None} )
    plt.close(fig)


data = read_data("benchmark-results")

splitted_data_by_test = split_data_by_test(data)

for test in splitted_data_by_test:
    make_chart(test, splitted_data_by_test[test], Path("benchmark-results"))