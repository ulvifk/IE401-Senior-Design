import json

import numpy as np

from .solution import Solution
from .data import Parameters

def calculate_total_weighted_completion_time(solutions: list[Solution]) -> float:
    total = 0
    for solution in solutions:
        if solution.task.succeeding_task == None:
            total += solution.end_time * solution.task.priority

    return total

def calculate_deviation_from_earlier_plan(solutions: list[Solution]) -> float:
    total = 0
    for solution in solutions:
        deviation = solution.start_time - solution.task.old_scheduled_time
        total += np.power(deviation, 2)

    return total

def calculate_total_weighted_tardiness(solutions: list[Solution]) -> float:
    total = 0
    for solution in solutions:
        if solution.task.succeeding_task == None:
            tardiness = solution.end_time - solution.task.job.deadline
            tardiness = max(0, tardiness)
            total += np.power(tardiness, 2) * solution.task.priority

    return total

def write_stats(path: str, solutions: list[Solution], parameters: Parameters):
    stats = {}
    with open(path, "w") as file:
        stats["total_weighted_completion_time"] = calculate_total_weighted_completion_time(solutions)
        stats["deviation_from_earlier_plan"] = calculate_deviation_from_earlier_plan(solutions)
        stats["total_tardiness"] = calculate_total_weighted_tardiness(solutions)
        stats["n_jobs"] = len(parameters.set_of_jobs)
        stats["n_tasks"] = len(parameters.set_of_tasks)
        stats["n_machines"] = len(parameters.set_of_machines)

    with open(path, "w") as file:
        json.dump(stats, file)