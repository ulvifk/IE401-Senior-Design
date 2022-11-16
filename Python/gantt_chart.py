from matplotlib.figure import Figure
from matplotlib.axes import Axes
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

plt.style.use("seaborn")

df = pd.read_csv("Solution.csv")

colors = ["b", "g", "r", "c", "m", "y", "k"]

jobs = df["Job"].values
machines = df["Machine"].values
tasks = df["Task"].values
time = df["Time"].values
processing_times = df["Processing_time"].values

machine_list = [i for i in range(1, np.max(machines) + 1)]

fig, ax = plt.subplots()
fig: Figure
ax: Axes

color_map = [colors[i] for i in machines]
x_ticks = [i for i in range(0, np.max(time + processing_times) + 10 , 10)]
y_ticks = [i for i in range(1, np.max(jobs)+1)]

machine_bars =[]

for machine in machine_list:
    bar = ax.barh(y=jobs[machines == machine], width=processing_times[machines == machine], left=time[machines == machine])
    machine_bars.append(bar)

ax.set_xticks(x_ticks)
ax.set_yticks(y_ticks)
ax.set_ylabel("Jobs")

ax.legend(machine_bars, [f"Machine {i}" for i in machine_list])

fig.show()
