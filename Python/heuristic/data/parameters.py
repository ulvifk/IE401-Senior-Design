from . import Task, Job, Machine
import json

class Parameters:
    set_of_jobs: list[Job]
    set_of_tasks: list[Task]
    set_of_machines: list[Machine]
    alpha_completion_time: float
    alpha_tardiness: float
    alpha_robust: float
    low_priority: float
    medium_priority: float
    high_priority: float
    scenario: dict

    def __init__(self):
        self.set_of_jobs = []
        self.set_of_tasks = []
        self.set_of_machines = []

        self.alpha_completion_time = 1
        self.alpha_tardiness = 10
        self.alpha_robust = 0.1

        self.low_priority = 1
        self.medium_priority = 4
        self.high_priority = 16

    def get_priority(self, priority: str):
        if priority == 'LOW':
            return self.low_priority
        elif priority == 'MEDIUM':
            return self.medium_priority
        elif priority == 'HIGH':
            return self.high_priority

    def read_data(self, path=None, json_file=None):
        if path is not None:
            with open(path, 'r') as f:
                scenario = json.load(f)

        if json_file is not None:
            scenario = json_file

        self.scenario = scenario

        for machine in scenario['machines']:
            id = int(machine['id'])
            processing_time_constant = float(machine['processing_time_constant'])
            type = machine['task_type_undertakes']
            mac = Machine(id, type, processing_time_constant)
            self.set_of_machines.append(mac)

        for job in scenario['jobs']:
            id = int(job['id'])
            deadline = int(job['deadline'])
            priority = job['priority']
            string_priority = priority
            priority = self.get_priority(string_priority)
            self.set_of_jobs.append(Job(id, deadline, priority, string_priority))

            for task in job['tasks']:
                id = int(task['id'])
                processing_time = float(task['processing_time'])
                machines_can_undertake_ids = [int(machine_id) for machine_id in task['machines_can_undertake']]
                machines_can_undertake = []
                for mac_id in machines_can_undertake_ids:
                    for mac in self.set_of_machines:
                        if mac.id == mac_id:
                            machines_can_undertake.append(mac)
                            break

                preceding_task_id = int(task['preceding_task'])
                succeeding_task_id = int(task['succeeding_task'])
                old_scheduled_time = float(task['scheduled_start_time'])
                t = Task(id, processing_time, preceding_task_id, succeeding_task_id, self.set_of_jobs[-1], priority,
                         old_scheduled_time)
                t.machines_can_undertake = machines_can_undertake
                for mac in machines_can_undertake:
                    mac.set_of_assigned_tasks.append(t)
                    t.processing_times[mac] = processing_time * mac.processing_time_constant

                self.set_of_tasks.append(t)
                self.set_of_jobs[-1].tasks.append(t)

        self._find_precedence_relations()

    def _find_precedence_relations(self):
        for task in self.set_of_tasks:
            if task.preceding_task_id != -1:
                for t in self.set_of_tasks:
                    if t.id == task.preceding_task_id:
                        task.preceding_task = t
                        break
            if task.succeeding_task_id != -1:
                for t in self.set_of_tasks:
                    if t.id == task.succeeding_task_id:
                        task.succeeding_task = t
                        break