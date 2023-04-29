import pandas as pd

file_name = "mip_summary"
df = pd.read_csv(file_name + ".csv")

colum_names = ["Weighted Completion Time", "Weighted Total Tardiness"]
coeffs = [1, 10]
objective_column_name = "Objective Value"

for i, colum_name in enumerate(colum_names):
    df[colum_name] = coeffs[i] * df[colum_name].values / df[objective_column_name].values


df.to_csv(file_name + "_percentage.csv", index=False)