#
# Copyright 2022 Dynatrace LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


import time
from collections import defaultdict, namedtuple
from os import listdir
from os.path import basename, getsize, isfile, join
from pathlib import Path

import git
import pandas as pd

import matplotlib

matplotlib.use("PDF")
import matplotlib.pyplot as plt


Record = namedtuple(
    "Record",
    [
        "commit_date",
        "exec_date",
        "revision",
        "algorithm",
        "test",
        "avg",
        "std",
        "parameters",
    ],
)
DateCommit = namedtuple("DateCommit", ["commit_date", "revision", "exec_date"])


def parse_exec_date_from_benchmark_result_file(file_name):
    s = list(file_name.split()[0])
    s[-3] = ":"
    s[-6] = ":"
    s[-9] = " "
    return "".join(s)


def parse_revision_from_benchmark_result_file(file_name):
    return file_name.split(" ")[1].split(".")[0]


def get_commit_date(commit, git_repo):
    return time.strftime(
        "%Y-%m-%d %H:%M:%S", time.localtime(git_repo.commit(commit).committed_date)
    )


def read_data_file(benchmark_result_path, benchmark_result_file, git_repo):
    data = []
    exec_date = parse_exec_date_from_benchmark_result_file(benchmark_result_file)
    revision = parse_revision_from_benchmark_result_file(benchmark_result_file)
    commit_date = get_commit_date(revision, git_repo)

    # print(f"Reading file {benchmark_result_path / benchmark_result_file} as .txt")

    with open(benchmark_result_path / benchmark_result_file) as file:
        lines = file.readlines()
        split_header = lines[0].split()
        parameter_names = []
        for w in split_header:
            if w[0] == "(" and w[-1] == ")":
                parameter_names.append(w[1:-1])

        num_parameters = len(parameter_names)
        for line in lines[1:]:
            split_line = line.split()
            algorithm = split_line[0].split(".")[-2]
            test = split_line[0].split(".")[-1]
            avg = float(split_line[3 + num_parameters])
            std = float(split_line[5 + num_parameters])
            parameters = {}
            for h in range(0, num_parameters):
                p = split_line[1 + h]
                if p != "N/A":
                    parameters[parameter_names[h]] = split_line[1 + h]

            data.append(
                Record(
                    commit_date=commit_date,
                    exec_date=exec_date,
                    revision=revision,
                    algorithm=algorithm,
                    test=test,
                    avg=avg,
                    std=std,
                    parameters=parameters,
                )
            )
    return data


def read_data_files_json_df(benchmark_result_path, benchmark_result_files, git_repo):
    def read_json(f):
        # print(f"Reading file {f} as json")
        return pd.read_json(f, orient="record")

    # read all files
    files = [join(benchmark_result_path, f) for f in benchmark_result_files]
    df = pd.concat({f: read_json(f) for f in files})

    # add columns "filename" and "indexInFile"
    df.index.names = ["filename", "indexInFile"]
    df = df.reset_index()

    # add more columns
    df[["benchmarkClassPath", "algorithm", "test"]] = df["benchmark"].str.rsplit(
        ".", n=2, expand=True
    )
    df[["exec_date", "revision"]] = (
        df["filename"]
        .map(basename)
        .str.rsplit(".", n=1, expand=True)[0]
        .str.rsplit(" ", n=1, expand=True)
    )
    df["commit_date"] = df["revision"].map(lambda x: get_commit_date(x, git_repo))

    # add params column if missing
    if "params" not in df:
        df["params"] = None
    df["params"] = [{} if x is None else x for x in df["params"]]

    # add the avg and std columns, note that this only makes sense if score actually represents an average
    df["avg"] = df["primaryMetric"].apply(lambda pm: pm["score"])
    df["std"] = df["primaryMetric"].apply(lambda pm: pm["scoreError"])
    return df


def read_data_files_json(benchmark_result_path, benchmark_result_files, git_repo):
    df_json = read_data_files_json_df(
        benchmark_result_path, benchmark_result_files, git_repo
    )
    data_json = [
        Record(*row[1:])  # list comprehension, make new records...
        for row in df_json.rename(columns={"params": "parameters"})[
            [  # select the columns in the correct order for the "Record" NamedTuple
                "commit_date",
                "exec_date",
                "revision",
                "algorithm",
                "test",
                "avg",
                "std",
                "parameters",
            ]
        ].itertuples()
    ]
    return data_json


def read_data(bechmark_result_directory, git_repo_path="."):
    git_repo = git.Repo(git_repo_path)
    benchmark_result_path = Path(bechmark_result_directory)

    benchmark_result_files_json = [
        f
        for f in listdir(benchmark_result_path)
        if isfile(join(benchmark_result_path, f))
        and getsize(join(benchmark_result_path, f)) > 0
        and f.endswith(".json")
    ]

    # skip txt files if a json-file with the same base filename was alreasy read
    benchmark_result_files_txt_exclude = [
        f.rsplit(".", 1)[0] for f in benchmark_result_files_json
    ]
    benchmark_result_files_txt = [
        f
        for f in listdir(benchmark_result_path)
        if isfile(join(benchmark_result_path, f))
        and getsize(join(benchmark_result_path, f)) > 0
        and f.endswith(".txt")
        and f.rsplit(".", 1)[0] not in benchmark_result_files_txt_exclude
    ]

    data = []

    # add json data
    data = data + read_data_files_json(
        benchmark_result_path, benchmark_result_files_json, git_repo
    )

    # add txt data
    for benchmark_result_file in benchmark_result_files_txt:
        data = data + read_data_file(
            benchmark_result_path, benchmark_result_file, git_repo
        )

    return data


def splitted_data_by_test_and_parameters(data):
    result = defaultdict(list)
    for d in data:
        test_name = d.test
        if len(d.parameters) > 0:
            test_name += " ("
            for param in sorted(d.parameters):
                test_name += param + "=" + d.parameters[param] + ";"
            test_name = test_name[:-1]
            test_name += ")"
        result[test_name].append(d)
    return result


def split_data_by_algorithm(data):
    result = defaultdict(list)
    for d in data:
        result[d.algorithm].append(d)
    return result


def format_label(label_data):
    return (
        "e:"
        + label_data.exec_date
        + " c: "
        + label_data.commit_date
        + "\n"
        + label_data.revision
    )


def plot_algorithm(ax, labels, algorithm, data):
    values = {
        DateCommit(
            commit_date=d.commit_date, revision=d.revision, exec_date=d.exec_date
        ): d
        for d in data
    }

    label_strings = []
    y_values = []

    for l in labels:
        if l in values:
            label_strings.append(format_label(l))
            y_values.append(values[l].avg)

    if "FarmHash" in algorithm:
        color = "blue"
    elif "Komihash" in algorithm:
        color = "green"
    elif "Murmur3_128" in algorithm:
        color = "black"
    elif "Murmur3_32" in algorithm:
        color = "red"
    elif "XXH3" in algorithm:
        color = "brown"
    elif "Wyhash" in algorithm:
        color = "magenta"
    elif "UnorderedHashTest" in algorithm:
        color = "black"
    else:
        color = None

    linestyle = "solid"
    if "ZeroAllocationHashing" in algorithm:
        linestyle = "dotted"
    elif "Guava" in algorithm:
        linestyle = "dashed"
    elif "GreenrobotEssentials" in algorithm:
        linestyle = (0, (3, 1, 1, 1, 1, 1))

    ax.plot(
        label_strings,
        y_values,
        label=algorithm,
        color=color,
        linestyle=linestyle,
        marker="o",
    )


def make_chart(test, data, output_path):
    labels = sorted(
        set(
            DateCommit(
                commit_date=d.commit_date, revision=d.revision, exec_date=d.exec_date
            )
            for d in data
        )
    )
    label_strings = [format_label(l) for l in labels]

    splitted_data_by_algorithm = split_data_by_algorithm(data)

    fig, ax = plt.subplots(1, 1)
    fig.set_size_inches(24, 18)

    ax.plot(label_strings, [None for _ in label_strings], alpha=0.0)

    for algorithm in splitted_data_by_algorithm:
        plot_algorithm(ax, labels, algorithm, splitted_data_by_algorithm[algorithm])

    ax.legend(loc="lower center", bbox_to_anchor=(0.5, -0.37), ncol=4, handlelength=8)

    fig.subplots_adjust(top=0.95, bottom=0.27, left=0.05, right=0.95)

    ax.set_title(test)
    ax.set_ylim([0, ax.get_ylim()[1]])
    ax.xaxis.set_tick_params(rotation=90)
    fig.savefig(
        output_path / (test + ".pdf"),
        format="pdf",
        dpi=1600,
        metadata={"creationDate": None},
    )
    plt.close(fig)


def main():
    data = read_data("benchmark-results")
    splitted_data_by_test_and_parameters_ = splitted_data_by_test_and_parameters(data)

    for test in splitted_data_by_test_and_parameters_:
        make_chart(
            test, splitted_data_by_test_and_parameters_[test], Path("benchmark-results")
        )


if __name__ == "__main__":
    main()
