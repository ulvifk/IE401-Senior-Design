import json

import matplotlib.figure
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

file_path = "../Java/output/computational_runs_3/kpis_03_03_03.json"

with open(file_path, "r") as f:
    kpi_json = json.load(f)

df = pd.DataFrame(columns=["seed", "n_job", "cpu_time", "obj", "tardiness", "gap"])
for kpi in kpi_json:
    n_job = 0
    try:
        n_job = int(kpi["scenarioPath"][36:38])
    except:
        n_job = 5
    df.loc[len(df)] = [int(kpi["scenarioPath"][36]),
         n_job,
         float(kpi["cpuTime"]),
         float(kpi["objective"]),
         int(kpi["totalTardiness"]),
         float(kpi["gap"])]

mean_cpu_times = df.groupby(["n_job"]).mean()["cpu_time"]
mean_gap = df.groupby(["n_job"]).mean()["gap"]
mean_tardiness = df.groupby(["n_job"]).mean()["tardiness"]

print(mean_cpu_times)
print(mean_gap)
print(mean_tardiness)
fig, ax = plt.subplots()
fig: matplotlib.figure.Figure
ax: matplotlib.pyplot.Axes

mean_line, = ax.plot(mean_cpu_times)
ax.scatter(df["n_job"], df["cpu_time"])


