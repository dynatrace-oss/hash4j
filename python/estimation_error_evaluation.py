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


def plot_charts(filename):
    d = read_data(filename)

    values = d[1]
    headers = d[0]

    fig, ax = plt.subplots(1, 1, sharey="row", sharex=True)
    fig.set_size_inches(6, 3)

    ax.set_title("p = " + headers["p"] + ", sample size = " + headers["num_cycles"])
    ax.set_xscale("log", base=10)
    theory = values["theoretical relative standard error"][0]
    ax.set_ylim([-theory * 0.05, theory * 1.25])
    ax.set_xlim([1, values["distinct count"][-1]])
    ax.xaxis.grid(True)
    ax.set_xlabel("distinct count")
    ax.yaxis.grid(True)
    ax.set_ylabel("relative error")
    ax.plot(values["distinct count"], values["relative bias"], label="bias")
    ax.plot(values["distinct count"], values["relative rmse"], label="rmse")
    ax.plot(
        values["distinct count"],
        values["theoretical relative standard error"],
        label="theory",
    )
    fig.legend(loc="center right")
    fig.savefig(
        "test-results/estimation-error-p" + headers["p"] + ".png",
        format="png",
        dpi=300,
        metadata={"creationDate": None},
        bbox_inches="tight",
    )
    plt.close(fig)


filenames = glob.glob("test-results/estimation-error-p*.csv")

for filename in filenames:
    plot_charts(filename)
