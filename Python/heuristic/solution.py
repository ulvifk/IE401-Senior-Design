from .data import Task, Machine

class Solution:
    task: Task
    machine: Machine
    start_time: float
    end_time: float
    score: float
    machine_predecessor: Task

    def __init__(self, task: Task, machine: Machine, start_time: float, end_time: float, score: float):
        self.task = task
        self.machine = machine
        self.start_time = start_time
        self.end_time = end_time
        self.score = score
