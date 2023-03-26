from __future__ import annotations
from .task import Task

class Job:
    id: int
    tasks: list[Task]
    deadline: int
    priority: float

    def __init__(self, id: int, deadline: int, priority: float):
        self.id = id
        self.tasks = []
        self.deadline = deadline
        self.priority = priority
