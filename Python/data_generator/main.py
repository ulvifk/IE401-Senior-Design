import json
import random

import numpy as np
from data_generator import Task, Job, Machine
from task_type import TASK_TYPE


def generate_random_scenario(n_job, possible_task_numbers: list, processing_mean: float, processing_std: float,
                             deadline_factor, deadline_std_factor,
                             low_p, medium_p, high_p,
                             machine_seed, instance_seed, machine_count):
    random.seed(machine_seed)
    np.random.seed(machine_seed)

    machines: dict[TASK_TYPE, list[Machine]] = {}
    base_id = 1
    for task_type in TASK_TYPE:
        n_machine_for_task_type = machine_count
        machines[task_type] = [Machine(id=base_id + i, task_type_undertakes=task_type, machine_name="",
                                       processing_time_constant=np.random.uniform(0.8, 1.2))
                               for i in range(n_machine_for_task_type)]
        base_id += len(machines[task_type])

    assert low_p + medium_p + high_p == 1

    random.seed(instance_seed)
    np.random.seed(instance_seed)

    priority_list = []
    for i in range(n_job // 3):
        priority_list.append("HIGH")
    for i in range(n_job // 3):
        priority_list.append("MEDIUM")
    for i in range(n_job // 3):
        priority_list.append("LOW")
    while len(priority_list) < n_job:
        priority_list.append("LOW")

    list_of_jobs = [
        Job(priority=priority_list.pop(0), deadline=0, tasks=[],
            id=i) for i in range(1, n_job + 1)]

    unique_task_id = 1
    for job in list_of_jobs:
        list_of_tasks = []

        n_task = np.random.choice(possible_task_numbers)
        task_types = np.random.choice(list(TASK_TYPE), size=n_task, replace=False)
        task_types = sorted(task_types, key=lambda x: x.value)

        # First task
        processing_time = np.random.normal(processing_mean, processing_std)
        list_of_tasks.append(Task(id=unique_task_id,
                                  type=task_types[0],
                                  processing_time=processing_time,
                                  machines_can_undertake=[machine.id for machine in machines[task_types[0]]],
                                  preceding_task=-1,
                                  succeeding_task=unique_task_id + 1,
                                  scheduled_start_time=-1,
                                  scheduled_end_time=-1,
                                  scheduled_machine=-1))
        unique_task_id += 1

        for task_type in task_types[1:-1]:
            processing_time = int(np.random.normal(processing_mean, processing_std))
            list_of_tasks.append(Task(id=unique_task_id,
                                      type=task_type,
                                      processing_time=processing_time,
                                      machines_can_undertake=[machine.id for machine in machines[task_type]],
                                      preceding_task=unique_task_id - 1,
                                      succeeding_task=unique_task_id + 1,
                                      scheduled_start_time=-1,
                                      scheduled_end_time=-1,
                                      scheduled_machine=-1))

            unique_task_id += 1

        # Last task
        processing_time = int(np.random.normal(processing_mean, processing_std))
        list_of_tasks.append(Task(id=unique_task_id,
                                  type=task_types[-1],
                                  processing_time=processing_time,
                                  machines_can_undertake=[machine.id for machine in machines[task_types[-1]]],
                                  preceding_task=unique_task_id - 1,
                                  succeeding_task=-1,
                                  scheduled_start_time=-1,
                                  scheduled_end_time=-1,
                                  scheduled_machine=-1))
        unique_task_id += 1

        job.tasks = list_of_tasks

        job.deadline = 0

    #average_machine = np.mean([len(machines[task_type]) for task_type in TASK_TYPE])
    #n_task = sum([len(job.tasks) for job in list_of_jobs])
    #for job in list_of_jobs:
    #    deadline_factor = pow(n_task, 1 / 3) / (average_machine + 1) * 1.4
    #    deadline_mean = np.sum([task.processing_time for task in job.tasks]) * deadline_factor
    #    deadline_std = deadline_mean * deadline_std_factor
    #    deadline = np.round(np.random.normal(deadline_mean, deadline_std))
    #    job.deadline = deadline

    total_processing_time = 0
    for job in list_of_jobs:
        for task in job.tasks:
            avg_processing_time = 0
            for machine_id in task.machines_can_undertake:
                machine = [machine for machine in machines[task.type] if machine.id == machine_id][0]
                avg_processing_time += machine.processing_time_constant * task.processing_time
            avg_processing_time /= len(task.machines_can_undertake)
            total_processing_time += avg_processing_time

    total_processing_time = total_processing_time / len(machines)

    low_base_deadline = 1.3 * total_processing_time
    medium_base_deadline = 0.9 * total_processing_time
    high_base_deadline = 0.5 * total_processing_time
    for job in list_of_jobs:
        if job.priority == "LOW":
            job_base_deadline = low_base_deadline
        elif job.priority == "MEDIUM":
            job_base_deadline = medium_base_deadline
        else:
            job_base_deadline = high_base_deadline

        job_base_deadline += np.random.normal(0, job_base_deadline * 0.1)
        job.deadline = job_base_deadline

    list_of_jobs = [job.json_encoded() for job in list_of_jobs]
    list_of_machines = []
    for machine_list in machines.values():
        for machine in machine_list:
            list_of_machines.append(machine.__dict__())

    scenario = {}
    scenario["machines"] = list_of_machines
    scenario["jobs"] = list_of_jobs

    return scenario


if __name__ == "__main__":
    for seed in [0, 1, 2, 3, 4]:
        for n_job in [10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60]:
            for machine_count in [1]:
                scenario = generate_random_scenario(n_job=n_job, possible_task_numbers=[2, 3, 4],
                                                    processing_mean=10, processing_std=4,
                                                    deadline_factor=2.25, deadline_std_factor=0.1,
                                                    low_p=0.34, medium_p=0.33, high_p=0.33,
                                                    machine_seed=seed, instance_seed=seed, machine_count=machine_count)

                with open(f"../../Java/input/scenario_{seed}_{n_job}_{machine_count}.json", "w") as f:
                    json.dump(scenario, f)
