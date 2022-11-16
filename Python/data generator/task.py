from dataclasses import dataclass


@dataclass
class Task:
    id: int
    processing_time: float
    assigned_machine: int
    preceding_task: int
    succeeding_task: int
    schedule: int

