import json

import numpy as np
import pandas as pd
import plotly.express as px


def create_gantt_chart(title, scenario_path: str, stats_path: str, save_path=""):
    df = pd.DataFrame(columns=["job", "task", "type", "scheduled_machine", "processing_time", "scheduled_time",
                               "deadline", "priority", "end", "score"])

    with open(scenario_path, "r") as f:
        scenario = json.load(f)

    priorities = {}
    for job in scenario["jobs"]:
        priorities[int(job["id"])] = job["priority"]

        if len(job["tasks"]) == 0:
            row = [int(job["id"]), -1, -1,
                   -1, -1, int(job["deadline"]), job["priority"]]
            df.loc[len(df)] = row

        for task in job["tasks"]:
            row = [int(job["id"]), int(task["id"]), task["type"], int(task["scheduled_machine"]),
                   float(task["processing_time"]), float(task["scheduled_start_time"]), int(job["deadline"]),
                   job["priority"], float(task["scheduled_end_time"]), float(task["score"])]
            df.loc[len(df)] = row

    df["delta"] = df["end"] - df["scheduled_time"]

    df["type"] = df["type"].astype(str)
    df["scheduled_machine"] = df["scheduled_machine"].map(lambda x: "Machine " + str(x))
    df.sort_values(by=["job"], inplace=True)

    for index, row in df.iterrows():
        string_name = f"Job {row['job']} | {row['priority']}"
        df.at[index, "job"] = string_name

    gantt_fig = px.timeline(df, x_start="scheduled_time", x_end="end", y="job", color="scheduled_machine",
                            hover_data=["task", "type", "deadline", "priority", "score"], )
    gantt_fig.update_yaxes(categoryorder="array", categoryarray=df["job"].unique())
    gantt_fig.layout.xaxis.type = 'linear'
    for d in gantt_fig.data:
        filt = df["scheduled_machine"] == d.name
        d.x = df[filt]["delta"].tolist()
        d.width = 0.7

    df_deadlines = pd.DataFrame(columns=["job", "deadline", "end", "delta"])
    jobs = df["job"].unique()
    for job in jobs:
        deadline = df[df["job"] == job]["deadline"].iloc[0]
        df_deadlines.loc[len(df_deadlines)] = [job, deadline, deadline + 0.5, 0.5]

    deadline_fig = px.timeline(df_deadlines, x_start="deadline", x_end="end", y="job", hover_data=[])
    deadline_fig.update_yaxes(autorange="reversed")
    deadline_fig.layout.xaxis.type = 'linear'
    deadline_fig.data[0].x = df_deadlines["delta"].tolist()
    deadline_fig.data[0].width = 1

    gantt_fig.add_trace(deadline_fig.data[0])

    with open(stats_path, "r") as f:
        stats = json.load(f)

    total_objective = np.sum([float(stats["total_weighted_completion_time"]),
                              float(stats["total_tardiness"]),
                              float(stats["deviation_from_earlier_plan"])])

    weighted_objective = float(stats["total_weighted_completion_time"]) * 1 + float(
        stats["total_tardiness"]) * 10 + float(
        stats["deviation_from_earlier_plan"]) * 0.1

    gantt_fig.add_annotation(text=f"Weighted Completion Time: {stats['total_weighted_completion_time']} | "
                                  f"Total Deviation: {stats['deviation_from_earlier_plan']} | "
                                  f"Total Weighted Tardiness: {stats['total_tardiness']} | ",
                             align='left',
                             showarrow=False,
                             xref='paper',
                             yref='paper',
                             x=0,
                             y=1.09,
                             bordercolor='black',
                             borderwidth=1)

    gantt_fig.add_annotation(text=f"Total Objective: {total_objective} | "
                                  f"Weighted Objective: {weighted_objective}",
                             align='left',
                             showarrow=False,
                             xref='paper',
                             yref='paper',
                             x=0,
                             y=1.05,
                             bordercolor='black',
                             borderwidth=1)

    gantt_fig.show()


if __name__ == "__main__":
    for seed in [0]:
        for n in [10]:
            normal = f"solution_{seed}_{n}.json"
            stats = f"stats_{seed}_{n}.json"
            create_gantt_chart("Normal", normal, stats)

            normal = f"../Java/output/scenario_{seed}_{n}/model_solution.json"
            stats = f"../Java/output/scenario_{seed}_{n}/model_stats.json"
            create_gantt_chart("Normal", normal, stats)

