from __future__ import annotations
from . import Machine
from . import Job

class Task:
    id: int
    processing_time: float
    processing_times: dict[Machine, float]
    machines_can_undertake: list[Machine]
    job: Job
    priority: float
    preceding_task: Task
    succeeding_task: Task
    preceding_task_id: int
    succeeding_task_id: int
    old_scheduled_time: float

    def __init__(self, id: int, processing_time, preceding_task_id: int, succeeding_task_id: int, job: Job, priority,
                 old_scheduled_time):
        self.id = id
        self.processing_time = processing_time
        self.processing_times = {}
        self.machines_can_undertake = []
        self.job = job
        self.priority = priority
        self.preceding_task = None
        self.succeeding_task = None
        self.preceding_task_id = preceding_task_id
        self.succeeding_task_id = succeeding_task_id
        self.old_scheduled_time = old_scheduled_time


