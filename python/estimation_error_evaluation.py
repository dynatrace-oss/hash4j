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
import csv
import matplotlib.pyplot as plt
import glob


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


def plot_charts(filename):
    d = read_data(filename)

    values = d[1]
    headers = d[0]

    fig, ax = plt.subplots(1, 1, sharey="row", sharex=True)
    fig.set_size_inches(6, 3)

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
    theory = to_percent(values["theoretical relative standard error"])[0]

    if headers["sketch_name"] == "ultraloglog":
        ax.set_ylim([-theory * 0.1, theory * 1.15])
    elif headers["sketch_name"] == "hyperloglog":
        ax.set_ylim([-theory * 0.25, theory * 1.55])
    else:
        assert False
    ax.set_xlim([1, values["distinct count"][-1]])
    ax.xaxis.grid(True)
    ax.set_xlabel("distinct count")
    ax.yaxis.grid(True)
    ax.set_ylabel("relative error (%)")
    ax.plot(values["distinct count"], to_percent(values["relative bias"]), label="bias")
    ax.plot(values["distinct count"], to_percent(values["relative rmse"]), label="rmse")
    ax.plot(
        values["distinct count"],
        to_percent(values["theoretical relative standard error"]),
        label="theory",
    )
    # fig.legend(loc="center right")
    ax.legend(loc="center right")
    fig.savefig(
        "test-results/"
        + headers["sketch_name"]
        + "-estimation-error-p"
        + headers["p"]
        + ".png",
        format="png",
        dpi=300,
        metadata={"creationDate": None},
        bbox_inches="tight",
    )
    plt.close(fig)


filenames = glob.glob("test-results/*loglog-estimation-error-p*.csv")

for filename in filenames:
    plot_charts(filename)
