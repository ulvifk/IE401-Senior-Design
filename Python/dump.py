import matplotlib.pyplot as plt
import pandas as pd


def plot_iterative_objective_graph(df: pd.DataFrame):
    fig, ax = plt.subplots()
    fig: plt.Figure
    ax: plt.Axes

    for index, row in df.iterrows():
        row: pd.Series
        total_completion_times = [row["High Total Weighted Completion Time"],
                                  row["Medium Total Weighted Completion Time"],
                                  row["Low Total Weighted Completion Time"],
                                  row["After Tune Weighted Completion Time"]]
        total_tardiness = [row["High Total Weighted Tardiness"],
                           row["Medium Total Weighted Tardiness"],
                           row["Low Total Weighted Tardiness"],
                           row["After Tune Weighted Total Tardiness"]]

        cpu_times = [row["High Cpu Time"],
                     row["Medium Cpu Time"],
                     row["Low Cpu Time"],
                     row["Tune Cpu Time"]]

        # Calculate additive cpu times, total_completion_times and total_tardiness
        for i in range(1, 3):
            cpu_times[i] += cpu_times[i - 1]
            total_completion_times[i] += total_completion_times[i - 1]
            total_tardiness[i] += total_tardiness[i - 1]

        objectives = []
        for i in range(4):
            objectives.append(total_completion_times[i] + total_tardiness[i])

        axes = [1, 2, 3, 4]
        # ax.plot(axes, total_completion_times, label=f"Completion Times {index}")
        # ax.plot(axes, total_tardiness, label=f"Tardiness {index}")
        ax.plot(axes, objectives, label=f"Objective {index}")

        ax.set_xticks(axes)
        ax.legend()

        fig.show()


if __name__ == ("__main__"):
    file_name = "summary_full.csv"
    df = pd.read_csv(file_name)

    plot_iterative_objective_graph(df.loc[[37, 39]])
