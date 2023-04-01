from __future__ import annotations

class Machine:
    id: int
    type: str
    set_of_assigned_tasks: list[Task]
    processing_time_constant: float

    def __init__(self, id: int, type: str, processing_time_constant: float):
        self.id = id
        self.type = type
        self.set_of_assigned_tasks = []
        self.processing_time_constant = processing_time_constant

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
    heuristic_special_score: float
    average_processing_time: float
    required_time: float
    earliest_start_time: float | None
    processing_times_by_id: dict[int, float]

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
        self.heuristic_special_score = 0
        self.processing_times_by_id = {}
        self.earliest_start_time = 0 if self.preceding_task_id == -1 else None

class Job:
    id: int
    tasks: list[Task]
    deadline: int
    priority: float
    string_priority: str

    def __init__(self, id: int, deadline: int, priority: float, string_priority: str):
        self.id = id
        self.tasks = []
        self.deadline = deadline
        self.priority = priority
        self.string_priority = string_priority