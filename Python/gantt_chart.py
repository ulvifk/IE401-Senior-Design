import sys
import warnings
warnings.filterwarnings("ignore")

import matplotlib.axes
from matplotlib.figure import Figure
import pandas as pd
import json
import matplotlib.pyplot as plt
import numpy as np

def create_gantt_chart(ax: matplotlib.axes.Axes, fig: matplotlib.figure.Figure, job_list, machine_list, is_save=False, save_path=""):
    df = pd.DataFrame(columns=["job", "task", "machine", "processing_time", "schedule", "deadline", "priority"])

    priorities = {}
    for job in job_list:
        priorities[int(job["id"])] = job["priority"]

        if len(job["tasks"]) == 0:
            row = [int(job["id"]), -1, -1,
                   -1, -1, int(job["deadline"]), job["priority"]]
            df.loc[len(df)] = row

        for task in job["tasks"]:
            row = [int(job["id"]), int(task["id"]), int(task["assigned_machine"]),
                   int(task["processing_time"]), int(task["schedule"]), int(job["deadline"]), job["priority"]]
            df.loc[len(df)] = row

    jobs = df["job"].values
    machines = df["machine"].values
    tasks = df["task"].values
    schedules = df["schedule"].values
    processing_times = df["processing_time"].values
    deadlines = df["deadline"].values


    colors = ["b", "g", "r", "c", "m", "y", "k"]
    color_map = [colors[i] for i in machines]
    x_ticks = [i for i in range(20, np.max(schedules + processing_times) + 30, 10)]
    y_ticks = [i for i in range(1, np.max(jobs) + 1)]

    machine_bars = []

    for machineObj in machine_list:
        machine = int(machineObj["id"])
        bar = ax.barh(y=jobs[machines == machine], width=processing_times[machines == machine],
                      left=schedules[machines == machine] + 20)
        machine_bars.append(bar)

    ax.set_xticks(x_ticks)
    ax.set_yticks(y_ticks, labels=[f"Job {job_id} ({priorities[job_id]})" for job_id in np.unique(jobs)])
    ax.set_ylabel("Jobs")

    ax.legend(machine_bars, [f"Machine {i}" for i in [int(machine["id"]) for machine in machine_list]])

    ax.barh(y=jobs, width=1, left=deadlines, height=1.2)

    fig.subplots_adjust(left=0.2)

    if(is_save):
        fig.savefig(save_path)

    plt.show(block=True)


if __name__ == "__main__":
    plt.style.use("seaborn-v0_8")
    plt.ion()

    file_path = "../Java/output/scenario.json"
    if len(sys.argv) > 0:
        file_path = sys.argv[0]

    with open("../Java/output/scenario.json", "r") as f:
        scenario = json.load(f)
    machine_list = scenario["machines"]
    job_list = scenario["jobs"]

    save_path = "../Java/output/robustness/scenario_gantt.png"

    fig, ax = plt.subplots()
    create_gantt_chart(ax, fig, job_list, machine_list, is_save=False, save_path=save_path)
