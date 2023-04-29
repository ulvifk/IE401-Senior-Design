import pandas as pd


file_name = "mip_summary_percentage"
df = pd.read_csv(file_name + ".csv")

n_jobs = df["#jobs"].unique()
n_machines = df["#machines"].unique()
increments = df["increment"].unique()

df_means = pd.DataFrame(columns = df.columns)
df_std = pd.DataFrame(columns = df.columns)

for n_job in n_jobs:
    for n_machine in n_machines:
        for increment in increments:
            df_means.loc[len(df_means)] = df[(df["#jobs"] == n_job) & (df["#machines"] == n_machine) & (df["increment"] == increment) & (df["Objective Value"] != 0)].mean()
            df_std.loc[len(df_std)] = df[(df["#jobs"] == n_job) & (df["#machines"] == n_machine) & (df["increment"] == increment) & (df["Objective Value"] != 0)].std()
            df_std.loc[len(df_std) - 1]["#jobs"] = n_job
            df_std.loc[len(df_std) - 1]["#machines"] = n_machine
            df_std.loc[len(df_std) - 1]["increment"] = increment

df_means.to_csv(file_name + "_means_.csv", index=False)
df_std.to_csv(file_name + "_std_.csv", index=False)