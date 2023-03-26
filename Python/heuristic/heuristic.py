import json
import time

import numpy as np

from .data import Parameters, Task, Job, Machine
from .solution import Solution
from .stats import write_stats
import streamlit as st

class Heuristic:
    slack = 2
    parameters: Parameters
    unscheduled_tasks: list[Task]
    M: list[Machine]
    H: list[Task]
    C: list[Task]
    D: list[Task]
    W: list[Machine]
    I: dict[Machine, float]
    solutions: dict[Task, Solution]

    def __init__(self, parameters: Parameters):
        self.parameters = parameters
        self.unscheduled_tasks = parameters.set_of_tasks.copy()
        self.H = []
        self.C = []
        self.D = []
        self.W = parameters.set_of_machines.copy()
        self.M = parameters.set_of_machines.copy()
        self.I = {machine: 0 for machine in self.M}
        self.solutions = {}

    def reset(self):
        self.unscheduled_tasks = self.parameters.set_of_tasks.copy()
        self.H = []
        self.C = []
        self.D = []
        self.W = self.parameters.set_of_machines.copy()
        self.M = self.parameters.set_of_machines.copy()
        self.I = {machine: 0 for machine in self.M}
        self.solutions = {}

    def _tardiness_score(self, task: Task, machine: Machine, time: float) -> float:
        slack = task.job.deadline - (time + task.processing_times[machine])

        required_time = 0
        current_task = task
        while current_task.succeeding_task != None:
            current_task = current_task.succeeding_task

            processing_times = [current_task.processing_times[machine] for machine in current_task.machines_can_undertake]
            average_processing_time = sum(processing_times) / len(processing_times)

            required_time += average_processing_time

        slack = slack - required_time
        penalty = max(0, 30-slack)

        return np.power(penalty, 2)

    def _time_score(self, time):
        return -time

    def _deviation_score(self, task: Task, time: float) -> float:
        return np.power(task.old_scheduled_time - time, 2)

    def _calculate_score(self, task: Task, machine: Machine, time: float) -> float:
        tardiness_score = self.parameters.alpha_tardiness * self._tardiness_score(task, machine, time)
        time_score = self.parameters.alpha_completion_time * self._time_score(time)
        deviation_score = self.parameters.alpha_robust * self._deviation_score(task, time)

        return task.priority * (tardiness_score + time_score + deviation_score)

    def _remove_redundant_machines(self):
        machines_to_remove = []
        for machine in self.M:

            is_redundant = True
            for task in machine.set_of_assigned_tasks:
                if task in self.unscheduled_tasks:
                    is_redundant = False
                    break
            if not is_redundant:
                continue

            machines_to_remove.append(machine)

        for machine in machines_to_remove:
            self.M.remove(machine)
            if machine in self.W:
                self.W.remove(machine)
            self.I.pop(machine)

    def _get_next_task_and_machine(self) -> tuple[Task, Machine, float]:
        next_machine = None
        next_task = None
        time = 0
        best_Score = -float('inf')
        for machine in self.W:
            for task in machine.set_of_assigned_tasks:
                if task in self.D:
                    predecessor = task.preceding_task
                    predecessor_end_time = self.solutions[predecessor].end_time if predecessor is not None else 0
                    machine_idle_time = self.I[machine]
                    time_2 = max(predecessor_end_time, machine_idle_time)
                    score = self._calculate_score(task, machine, time_2)
                    if score > best_Score:
                        best_Score = score
                        next_machine = machine
                        next_task = task
                        time = time_2

        return next_task, next_machine, time

    def _update_W(self, t, machine_list: list[Machine]) -> list[Machine]:
        new_machine_list = []
        for machine in machine_list:
            if self.I[machine] <= t + self.slack and self.I[machine] >= t - self.slack:
                new_machine_list.append(machine)

        return new_machine_list

    def _update_C(self, t):
        self.C = []
        for task in self.H:
            end_time = self.solutions[task].end_time
            if end_time <= t + self.slack:
                self.C.append(task)

    def _update_D(self):
        self.D = []

        for task in self.unscheduled_tasks:
            is_found = False
            if task.preceding_task == None or task.preceding_task in self.C:
                for machine in task.machines_can_undertake:
                    if machine in self.W:
                        self.D.append(task)
                        break

    def _find_T(self, machines: list[Machine]) -> float:
        t = float('inf')
        for machine in machines:
            if self.I[machine] < t:
                t = self.I[machine]

        return t

    def optimize(self):
        st.write(f"Optimizing... | {time.time()}")

        t = 0
        self._update_D()

        while len(self.unscheduled_tasks) > 0:
            task, machine, start_time = self._get_next_task_and_machine()

            self.unscheduled_tasks.remove(task)
            self.H.append(task)
            self.I[machine] = start_time + task.processing_times[machine]
            self._remove_redundant_machines()

            solution = Solution(task, machine, start_time, start_time + task.processing_times[machine],
                                self._calculate_score(task, machine, start_time))
            self.solutions[task] = solution

            if len(self.unscheduled_tasks) == 0:
                break

            t = self._find_T(self.M)

            self.W = self._update_W(t, self.W)
            self._update_C(t)
            self._update_D()
            while len(self.D) == 0:
                m_prime = [mac for mac in self.M if mac not in self.W]

                if len(m_prime) == 0:
                    candidate_t_values = [s.end_time for s in self.solutions.values() if s.end_time > t and s.task not in self.C]
                    t = np.min(candidate_t_values)
                else:
                    t = self._find_T(m_prime)

                w_prime = self._update_W(t, m_prime)
                for mac in w_prime:
                    if mac not in self.W:
                        self.W.append(mac)

                self._update_C(t)
                self._update_D()

    def write_solution(self, scenario, output_path: str):
        for job in scenario['jobs']:
            for task_object in job['tasks']:
                task = None
                for tsk in self.parameters.set_of_tasks:
                    if tsk.id == int(task_object['id']):
                        task = tsk
                        break

                solution = self.solutions[task]
                task_object['scheduled_start_time'] = solution.start_time
                task_object['scheduled_end_time'] = solution.end_time
                task_object['scheduled_machine'] = solution.machine.id
                task_object['score'] = solution.score

        with open(output_path, 'w') as output_file:
            json.dump(scenario, output_file)

    def write_stats(self, path):
        write_stats(path, list(self.solutions.values()), self.parameters)


