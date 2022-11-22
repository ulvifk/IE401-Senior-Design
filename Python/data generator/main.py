import json
import random

from machine import Machine
from job import Job
from task import Task
import numpy as np


def generate_random_scenario(n_machine, n_job, possible_task_numbers: list, processing_mean, processing_std,
                             deadline_factor, deadline_std_factor,
                             low_p, medium_p, high_p,
                             seed):
    random.seed(seed)
    np.random.seed(seed)

    assert low_p + medium_p + high_p == 1

    list_of_machines = [Machine(id=machine_id, machine_name="").__dict__ for machine_id in range(1, n_machine + 1)]
    list_of_jobs = [
        Job(priority=np.random.choice(["LOW", "MEDIUM", "HIGH"], p=[low_p, medium_p, high_p]), deadline=0, tasks=[],
            id=i) for i in range(1, n_job + 1)]

    unique_task_id = 1
    for job in list_of_jobs:
        list_of_tasks = []

        processing_time = np.round(np.random.normal(processing_mean, processing_std))
        processing_time = processing_time if processing_time > 1 else 1

        list_of_tasks.append(Task(id=unique_task_id, processing_time=processing_time,
                                  assigned_machine=1,
                                  preceding_task=-1, succeeding_task=unique_task_id + 1, schedule=-1))
        unique_task_id += 1

        n_task = np.random.choice(possible_task_numbers)
        for i in range(1, n_task - 1):
            processing_time = np.round(np.random.normal(processing_mean, processing_std))
            processing_time = processing_time if processing_time > 1 else 1
            list_of_tasks.append(Task(id=unique_task_id, processing_time=processing_time,
                                      assigned_machine=i + 1,
                                      preceding_task=list_of_tasks[len(list_of_tasks) - 1].id,
                                      succeeding_task=unique_task_id + 1, schedule=-1))
            unique_task_id += 1

        processing_time = np.round(np.random.normal(processing_mean, processing_std))
        processing_time = processing_time if processing_time > 1 else 1
        list_of_tasks.append(Task(id=unique_task_id, processing_time=processing_time,
                                  assigned_machine=n_machine,
                                  preceding_task=list_of_tasks[len(list_of_tasks) - 1].id,
                                  succeeding_task=-1, schedule=-1))
        unique_task_id += 1

        job.tasks = list_of_tasks

        deadline_mean = np.sum([task.processing_time for task in job.tasks]) * deadline_factor
        deadline_std = deadline_mean * deadline_std_factor
        deadline = np.round(np.random.normal(deadline_mean, deadline_std))

        job.deadline = deadline

    counts = [2, 4, 4]
    for i in range(len(list_of_jobs)):
        if i < 2:
            list_of_jobs[i].priority = "HIGH"
        elif i < 6:
            list_of_jobs[i].priority = "MEDIUM"
        elif i < 10:
            list_of_jobs[i].priority = "LOW"

    list_of_jobs = [job.json_encoded() for job in list_of_jobs]

    scenario = {}
    scenario["machines"] = list_of_machines
    scenario["jobs"] = list_of_jobs

    return scenario


if __name__ == "__main__":

    n_machine = 5

    for seed in [0, 1, 2, 3, 4]:
        for n_job in [5, 10, 15, 20]:
            scenario = generate_random_scenario(n_machine=n_machine, n_job=n_job, possible_task_numbers=[n_machine],
                                                processing_mean=10, processing_std=4,
                                                deadline_factor=2.25, deadline_std_factor=0.1,
                                                low_p=0.34, medium_p=0.33, high_p=0.33,
                                                seed=seed)

            with open(f"../../Java/input/computational_runs/scenario_{seed}_{n_job}_03_03_03.json", "w") as f:
                json.dump(scenario, f)
