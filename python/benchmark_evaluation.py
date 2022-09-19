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
import math
import time
from collections import defaultdict, namedtuple
from os import listdir
from os.path import basename, getsize, isfile, join
from pathlib import Path

import click
import git
import pandas as pd

import matplotlib

matplotlib.use("PDF")
import matplotlib.pyplot as plt


def get_commit_date(commit, git_repo):
    return (
        time.strftime(
            "%Y-%m-%d %H:%M:%S", time.localtime(git_repo.commit(commit).committed_date)
        )
        if git_repo
        else None
    )


def read_data_files_json_df(benchmark_result_path, benchmark_result_files, git_repo):
    # read all files
    files = [join(benchmark_result_path, f) for f in benchmark_result_files]
    df = pd.concat({f: pd.read_json(f, orient="record") for f in files})

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
    df["params"] = [
        {} if x is None or (isinstance(x, float) and math.isnan(x)) else x
        for x in df["params"]
    ]

    # make the params column hashable (necessary for groupby,...)
    df["params"] = df["params"].apply(lambda mydict: frozenset(mydict.items()))

    # make the values of primaryMetric easily accessible as individual columns
    df = pd.concat(
        [df, df["primaryMetric"].apply(pd.Series).add_prefix("primaryMetric.")], axis=1
    )
    df["primaryMetric.scoreConfidence.lower"] = df[
        "primaryMetric.scoreConfidence"
    ].apply(lambda x: x[0])
    df["primaryMetric.scoreConfidence.upper"] = df[
        "primaryMetric.scoreConfidence"
    ].apply(lambda x: x[1])
    return df


def read_data(benchmark_result_path, git_repo_path="."):
    git_repo = git.Repo(git_repo_path) if git_repo_path else None

    benchmark_result_files = [
        f
        for f in listdir(benchmark_result_path)
        if isfile(join(benchmark_result_path, f))
        and getsize(join(benchmark_result_path, f)) > 0
        and f.endswith(".json")
    ]

    return read_data_files_json_df(
        benchmark_result_path, benchmark_result_files, git_repo
    )


def get_plot_linestyles(algorithm):
    styles = {}
    algorithms_ordered = [
        "FarmHash",
        "Komihash",
        "Murmur3_128",
        "Murmur3_32",
        "XXH3",
        "Wyhash",
        "UnorderedHashTest",
    ]
    for i, alg in enumerate(algorithms_ordered):
        if alg in algorithm:
            styles["color"] = f"C{i+1}"

    linestyle_map = {
        "ZeroAllocationHashing": "dotted",
        "Guava": "dashed",
        "GreenrobotEssentials": (0, (3, 1, 1, 1, 1, 1)),
    }
    for alg, linestyle in linestyle_map.items():
        if alg in algorithm:
            styles["linestyle"] = linestyle
    return styles


def make_chart(df, output_path, name, scoreUnit, mode, show_confidence_interval):
    df["revision"] = "\n" + df["revision"]  # add a line break to the xlabels

    df_plot = df.pivot(
        columns=["algorithm"],
        index=["exec_date", "commit_date", "revision"],  # x axis for plotting
    )

    fig, ax = plt.subplots(1, 1)
    fig.set_size_inches(24, 18)

    for algorithm in df_plot["primaryMetric.score"].columns:
        df_plot["primaryMetric.score"][algorithm].plot(
            ax=ax,
            marker="o",
            ylabel=f"score ({mode}) in {scoreUnit}",
            **{
                "yerr": [
                    df_plot["primaryMetric.score"][algorithm]
                    - df_plot["primaryMetric.scoreConfidence.lower"][algorithm],
                    df_plot["primaryMetric.scoreConfidence.upper"][algorithm]
                    - df_plot["primaryMetric.score"][algorithm],
                ],
                "linewidth": 0.5,
                "capsize": 4,
            }
            if show_confidence_interval
            else {},
            **get_plot_linestyles(algorithm),
        )

    ax.legend(loc="lower center", bbox_to_anchor=(0.5, -0.37), ncol=4, handlelength=8)

    fig.subplots_adjust(top=0.95, bottom=0.27, left=0.05, right=0.95)

    ax.set_title(name)
    ax.set_ylim([0, ax.get_ylim()[1]])
    ax.xaxis.set_tick_params(rotation=90)
    fig.savefig(
        output_path / (name + ".pdf"),
        format="pdf",
        dpi=1600,
        metadata={"creationDate": None},
    )
    plt.close(fig)


@click.command()
@click.option(
    "--in-path",
    default="./benchmark-results/",
    help="Input folder, should contain the JSON files.",
    show_default=True,
    type=click.Path(exists=True, file_okay=False, path_type=Path),
)
@click.option(
    "--out-path",
    default="./benchmark-results/",
    help="Output folder, will contain the plots.",
    show_default=True,
    type=click.Path(exists=True, file_okay=False, path_type=Path),
)
@click.option(
    "--git-path",
    default=".",
    help="Set this to an empty string to disable fetching of commit date.",
    show_default=True,
    type=click.Path(exists=False, file_okay=False),
)
@click.option(
    "--show-confidence-interval",
    default=False,
    help="Add the confidence interval from the JMH results to the plots.",
    is_flag=True,
    show_default=True,
)
def main(in_path, out_path, git_path, show_confidence_interval):
    # load all .json benchmark result files to a single pandas.DataFrame
    df = read_data(in_path, git_path)

    # group by test and params (and more, but we expect this to be unique, otherwise the output file would be overwritten)
    # create and save plot for each group
    for (test, params, scoreUnit, mode), data in df.groupby(
        ["test", "params", "primaryMetric.scoreUnit", "mode"]
    ):
        name = test + (
            " (" + ";".join([f"{i[0]}={i[1]}" for i in params]) + ")"
            if len(params) > 0
            else ""
        )
        make_chart(data, out_path, name, scoreUnit, mode, show_confidence_interval)


if __name__ == "__main__":
    main()
