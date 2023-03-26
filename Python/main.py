from heuristic.data import Parameters
from heuristic import Heuristic

input_path = f"../Java/input/scenario_0_15.json"
output_path = "solution.json"

parameters = Parameters()
parameters.read_data(input_path)

heuristic = Heuristic(parameters)
heuristic.optimize()
heuristic.write_solution(input_path, output_path)
heuristic.write_stats("stats.json")