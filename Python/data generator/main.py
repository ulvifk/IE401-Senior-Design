import json
from machine import Machine
from job import Job
from task import Task
import numpy as np


def generate_random_scenario(n_machine, n_job, possible_task_numbers: list, processing_mean, processing_std):
    list_of_machines = [Machine(id=machine_id, machine_name="").__dict__ for machine_id in range(1, n_machine + 1)]
    list_of_jobs = [Job(priority="LOW", deadline=0, tasks=[], id=i) for i in range(1, n_job + 1)]

    unique_task_id = 1
    for job in list_of_jobs:
        list_of_tasks = []
        processing_time = np.round(np.random.normal(processing_mean, processing_std))
        processing_time = processing_time if processing_time > 1 else 1
        list_of_tasks.append(Task(id=unique_task_id, processing_time=processing_time,
                                  assigned_machine=np.random.choice(list_of_machines)["id"],
                                  preceding_task=-1, succeeding_task=unique_task_id + 1, schedule=-1))
        unique_task_id += 1
        n_task = np.random.choice(possible_task_numbers)
        for i in range(1, n_task-1):
            processing_time = np.round(np.random.normal(processing_mean, processing_std))
            processing_time = processing_time if processing_time > 1 else 1
            list_of_tasks.append(Task(id=unique_task_id, processing_time=processing_time,
                                      assigned_machine=np.random.choice(list_of_machines)["id"],
                                      preceding_task=list_of_tasks[len(list_of_tasks)-1].id,
                                      succeeding_task=unique_task_id + 1, schedule=-1))
            unique_task_id += 1

        processing_time = np.round(np.random.normal(processing_mean, processing_std))
        processing_time = processing_time if processing_time > 1 else 1
        list_of_tasks.append(Task(id=unique_task_id, processing_time=processing_time,
                                  assigned_machine=np.random.choice(list_of_machines)["id"],
                                  preceding_task=list_of_tasks[len(list_of_tasks)-1].id,
                                  succeeding_task=-1, schedule=-1))
        unique_task_id += 1

        job.tasks = list_of_tasks

    list_of_jobs = [job.json_encoded() for job in list_of_jobs]

    scenario = {}
    scenario["machines"] = list_of_machines
    scenario["jobs"] = list_of_jobs

    return scenario


if __name__ == "__main__":
    scenario = generate_random_scenario(n_machine=4, n_job=10, possible_task_numbers=[3, 4, 5], processing_mean=10, processing_std=4)

    with open("scenario.json", "w") as f:
        json.dump(scenario, f)
