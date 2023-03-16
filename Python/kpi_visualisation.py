import json

import matplotlib.figure
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

df = pd.DataFrame(columns=["window", "seed", "n_job", "cpu_time", "obj", "tardiness", "gap"])

for i in [1, 2, 5]:
    file_path = f"kpis/kpis{i}.json"

    window = i

    with open(file_path, "r") as f:
        kpi_json_1 = json.load(f)
    for kpi in kpi_json_1:
        n_job = 0
        try:
            n_job = int(kpi["scenarioPath"][36:38])
        except:
            n_job = 5
        df.loc[len(df)] = [window,
                           int(kpi["scenarioPath"][34]),
                           n_job,
                           float(kpi["cpuTime"]),
                           float(kpi["objective"]),
                           int(kpi["totalTardiness"]),
                           float(kpi["gap"])]

windows = df["window"].values
seeds = df["seed"].values
n_jobs = df["n_job"].values
tardinesses = df["tardiness"].values
cpu_times = df["cpu_time"].values

fig, ax = plt.subplots()
fig: matplotlib.figure.Figure
ax: matplotlib.pyplot.Axes

against_2_means = []

agains_2_s = []
agains_5_s = []

def plot_cpu_times():
    plt.style.use("seaborn")

    fig, ax = plt.subplots()
    fig: matplotlib.figure.Figure
    ax: matplotlib.pyplot.Axes

    #ax.set_yscale("log")
    x_ticks = [5, 10, 15, 20]
    ax.set_xticks(x_ticks)

    ax.set_xlabel("Number of jobs")
    ax.set_ylabel("CPU times (s)")

    window_scatters = []
    mean_cpu_plots = []
    for window_size in np.unique(windows):
        window_scatters.append(ax.scatter(n_jobs[windows == window_size],
                                          cpu_times[windows == window_size], label=f"Window size {int(window_size)}",
                                          s=30, edgecolor="black", linewidth=0.5))

        mean_cpu_times = [cpu_times[np.logical_and(windows == window_size, n_jobs == n_job)].mean()
                          for n_job in np.unique(n_jobs)]

        mean_cpu_plots.append(ax.plot(np.unique(n_jobs), mean_cpu_times,
                                      label=f"Mean CPU time for window size {int(window_size)}")[0])

    ax.legend(window_scatters, [scatter.get_label() for scatter in window_scatters], loc="upper left")

    fig.show()
    fig.savefig("cpu_times.png")

plot_cpu_times()
