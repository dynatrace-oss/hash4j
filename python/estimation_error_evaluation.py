#
# Copyright 2022-2023 Dynatrace LLC
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
import csv
import matplotlib.pyplot as plt
import glob
from matplotlib.lines import Line2D


def read_data(data_file):
    info = {}

    with open(data_file, "r") as file:
        reader = csv.reader(file, skipinitialspace=True, delimiter=";")
        row_counter = 0
        headers = []
        values = []
        for r in reader:
            if row_counter == 0:
                for i in r:
                    if i != "":
                        g = i.split("=")
                        info[g[0]] = g[1]

            elif row_counter == 1:
                for i in r:
                    if i != "":
                        headers.append(i)
                        values.append([])
            elif row_counter >= 2:
                k = 0
                for i in r:
                    if i != "":
                        values[k].append(float(i))
                        k += 1
            row_counter += 1

    data = {h: v for h, v in zip(headers, values)}
    size = row_counter - 2
    return info, data, size


def to_percent(values):
    return [100.0 * v for v in values]


def plot_individual_chart(filename, d, estimation_algorithm):
    colors = ["C0", "C1", "C2"]

    values = d[1]
    headers = d[0]

    fig, ax = plt.subplots(1, 1, sharey="row", sharex=True)
    fig.set_size_inches(6, 4)

    p = int(headers["p"])

    state_size_unit = "B"
    if headers["sketch_name"] == "ultraloglog":
        state_size = 2**p
    elif headers["sketch_name"] == "hyperloglog":
        state_size = 2**p * 6 // 8
    else:
        assert False

    if state_size % 1024 == 0:
        state_size //= 1024
        state_size_unit = "kB"
    if state_size % 1024 == 0:
        state_size //= 1024
        state_size_unit = "MB"

    num_simulation_runs_unit = ""
    num_simulation_runs = int(headers["num_cycles"])
    if num_simulation_runs % 1000 == 0:
        num_simulation_runs //= 1000
        num_simulation_runs_unit = "k"
    if num_simulation_runs % 1000 == 0:
        num_simulation_runs //= 1000
        num_simulation_runs_unit = "M"

    ax.set_title(
        "p = "
        + str(p)
        + ", state size = "
        + str(state_size)
        + state_size_unit
        + ", #simulation runs = "
        + str(num_simulation_runs)
        + num_simulation_runs_unit
    )
    ax.set_xscale("log", base=10)
    theory = to_percent(
        values["theoretical relative standard error " + estimation_algorithm]
    )[0]

    if headers["sketch_name"] == "ultraloglog":
        ax.set_ylim([-theory * 0.1, theory * 1.15])
    elif headers["sketch_name"] == "hyperloglog":
        ax.set_ylim([-theory * 0.2, theory * 1.25])
    else:
        assert False
    ax.set_xlim([1, values["distinct count"][-1]])
    ax.xaxis.grid(True)
    ax.set_xlabel("distinct count")
    ax.yaxis.grid(True)
    ax.set_ylabel("relative error (%)")

    ax.plot(
        values["distinct count"],
        to_percent(
            values["theoretical relative standard error " + estimation_algorithm]
        ),
        label="theory (default)",
        color=colors[2],
    )

    ax.plot(
        values["distinct count"],
        to_percent(values["relative rmse " + estimation_algorithm]),
        label="rmse (default)",
        color=colors[1],
    )

    ax.plot(
        values["distinct count"],
        to_percent(values["relative bias " + estimation_algorithm]),
        label="bias (default)",
        color=colors[0],
    )

    legend_elements = [
        Line2D([0], [0], color=colors[0]),
        Line2D([0], [0], color=colors[1]),
        Line2D([0], [0], color=colors[2]),
    ]
    fig.legend(
        legend_elements,
        ["bias", "rmse", "theory"],
        loc="lower center",
        ncol=3,
    )
    fig.subplots_adjust(top=0.93, bottom=0.21, left=0.11, right=0.99)
    filename = filename[:-4] + "-" + estimation_algorithm.replace(" ", "-") + ".png"

    fig.savefig(
        filename,
        format="png",
        dpi=300,
        metadata={"creationDate": None},
    )
    plt.close(fig)


def plot_individual_charts(filename):
    d = read_data(filename)
    values = d[1]
    estimation_algorithms = set()
    for k in ["relative rmse", "relative bias", "theoretical relative standard error"]:
        for h in values:
            if h.startswith(k):
                estimation_algorithms.add(h[len(k) + 1 :])

    for estimation_algorithm in estimation_algorithms:
        plot_individual_chart(filename, d, estimation_algorithm)


def plot_charts(filename):
    d = read_data(filename)

    colors = ["C0", "C1", "C2"]

    values = d[1]
    headers = d[0]

    fig, ax = plt.subplots(1, 1, sharey="row", sharex=True)
    fig.set_size_inches(6, 4)

    p = int(headers["p"])

    state_size_unit = "B"
    if headers["sketch_name"] == "ultraloglog":
        state_size = 2**p
    elif headers["sketch_name"] == "hyperloglog":
        state_size = 2**p * 6 // 8
    else:
        assert False

    if state_size % 1024 == 0:
        state_size //= 1024
        state_size_unit = "kB"
    if state_size % 1024 == 0:
        state_size //= 1024
        state_size_unit = "MB"

    num_simulation_runs_unit = ""
    num_simulation_runs = int(headers["num_cycles"])
    if num_simulation_runs % 1000 == 0:
        num_simulation_runs //= 1000
        num_simulation_runs_unit = "k"
    if num_simulation_runs % 1000 == 0:
        num_simulation_runs //= 1000
        num_simulation_runs_unit = "M"

    large_scale_simulation_mode_distinct_count_limit = int(
        headers["large_scale_simulation_mode_distinct_count_limit"]
    )

    ax.set_title(
        "p = "
        + str(p)
        + ", state size = "
        + str(state_size)
        + state_size_unit
        + ", #simulation runs = "
        + str(num_simulation_runs)
        + num_simulation_runs_unit
    )
    ax.set_xscale("log", base=10)
    theory = to_percent(values["theoretical relative standard error default"])[0]

    if headers["sketch_name"] == "ultraloglog":
        ax.set_ylim([-theory * 0.1, theory * 1.15])
    elif headers["sketch_name"] == "hyperloglog":
        ax.set_ylim([-theory * 0.2, theory * 1.25])
    else:
        assert False
    ax.set_xlim([1, values["distinct count"][-1]])
    ax.xaxis.grid(True)
    ax.set_xlabel("distinct count")
    ax.yaxis.grid(True)
    ax.set_ylabel("relative error (%)")
    ax.plot(
        values["distinct count"],
        to_percent(values["theoretical relative standard error martingale"]),
        label="theory (martingale)",
        color=colors[2],
        linestyle="dotted",
    )
    ax.plot(
        values["distinct count"],
        to_percent(values["theoretical relative standard error maximum likelihood"]),
        label="theory (ML)",
        color=colors[2],
        linestyle="dashed",
    )
    ax.plot(
        values["distinct count"],
        to_percent(values["theoretical relative standard error default"]),
        label="theory (default)",
        color=colors[2],
    )

    ax.plot(
        values["distinct count"],
        to_percent(values["relative rmse martingale"]),
        label="rmse (martingale)",
        color=colors[1],
        linestyle="dotted",
    )
    ax.plot(
        values["distinct count"],
        to_percent(values["relative rmse maximum likelihood"]),
        label="rmse (ML)",
        color=colors[1],
        linestyle="dashed",
    )
    ax.plot(
        values["distinct count"],
        to_percent(values["relative rmse default"]),
        label="rmse (default)",
        color=colors[1],
    )

    ax.plot(
        values["distinct count"],
        to_percent(values["relative bias martingale"]),
        label="bias (martingale)",
        color=colors[0],
        linestyle="dotted",
    )
    ax.plot(
        values["distinct count"],
        to_percent(values["relative bias maximum likelihood"]),
        label="bias (ML)",
        color=colors[0],
        linestyle="dashed",
    )
    ax.plot(
        values["distinct count"],
        to_percent(values["relative bias default"]),
        label="bias (default)",
        color=colors[0],
    )

    # draw transition
    # ax.plot([large_scale_simulation_mode_distinct_count_limit, large_scale_simulation_mode_distinct_count_limit], [-theory * 2, theory * 2], color="red", linestyle="dotted")

    legend_elements = [
        Line2D([0], [0], color=colors[0]),
        Line2D([0], [0], color="gray"),
        Line2D([0], [0], color=colors[1]),
        Line2D([0], [0], color="gray", linestyle="dashed"),
        Line2D([0], [0], color=colors[2]),
        Line2D([0], [0], color="gray", linestyle="dotted"),
    ]
    fig.legend(
        legend_elements,
        ["bias", "default", "rmse", "ML", "theory", "martingale"],
        loc="lower center",
        ncol=3,
    )
    fig.subplots_adjust(top=0.93, bottom=0.25, left=0.11, right=0.97)

    fig.savefig(
        filename[:-3] + "png",
        format="png",
        dpi=300,
        metadata={"creationDate": None},
    )
    plt.close(fig)


filenames = glob.glob("test-results/*-estimation-error-p*.csv")

for filename in filenames:
    plot_charts(filename)
    plot_individual_charts(filename)
