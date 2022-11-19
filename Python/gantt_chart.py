import matplotlib.axes
from matplotlib.figure import Figure
from matplotlib.axes import Axes
import pandas as pd
import json
import matplotlib.pyplot as plt
import numpy as np

def create_gantt_chart(ax: matplotlib.axes.Axes, fig: matplotlib.figure.Figure, job_list, machine_list):
    df = pd.DataFrame(columns=["job", "task", "machine", "processing_time", "schedule", "deadline"])
    for job in job_list:
        for task in job["tasks"]:
            row = [int(job["id"]), int(task["id"]), int(task["assigned_machine"]),
                   int(task["processing_time"]), int(task["schedule"]), int(job["deadline"])]
            df.loc[len(df)] = row

    jobs = df["job"].values
    machines = df["machine"].values
    tasks = df["task"].values
    schedules = df["schedule"].values
    processing_times = df["processing_time"].values
    deadlines = df["deadline"].values

    colors = ["b", "g", "r", "c", "m", "y", "k"]
    color_map = [colors[i] for i in machines]
    x_ticks = [i for i in range(0, np.max(schedules + processing_times) + 10, 10)]
    y_ticks = [i for i in range(1, np.max(jobs) + 1)]

    machine_bars = []

    for machineObj in machine_list:
        machine = int(machineObj["id"])
        bar = ax.barh(y=jobs[machines == machine], width=processing_times[machines == machine],
                      left=schedules[machines == machine])
        machine_bars.append(bar)

    ax.set_xticks(x_ticks)
    ax.set_yticks(y_ticks)
    ax.set_ylabel("Jobs")

    ax.legend(machine_bars, [f"Machine {i}" for i in [int(machine["id"]) for machine in machine_list]])

    ax.barh(y=jobs, width=1, left=deadlines, height=1.2)

    fig.show()



if __name__ == "__main__":
    plt.style.use("seaborn")
    with open("../Java/output/new_scenario.json", "r") as f:
        scenario = json.load(f)

    machine_list = scenario["machines"]
    job_list = scenario["jobs"]

    fig, ax = plt.subplots()
    create_gantt_chart(ax, fig, job_list, machine_list)
