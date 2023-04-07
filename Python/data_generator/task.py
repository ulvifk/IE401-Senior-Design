from dataclasses import dataclass
from task_type import TASK_TYPE


@dataclass
class Task:
    id: int
    type: TASK_TYPE
    processing_time: float
    machines_can_undertake: list[int]
    preceding_task: int
    succeeding_task: int
    scheduled_start_time: int
    scheduled_end_time: int
    scheduled_machine: int

    def __dict__(self):
        return {
            "id": self.id,
            "type": self.type.name,
            "processing_time": self.processing_time,
            "machines_can_undertake": [machine_id for machine_id in self.machines_can_undertake],
            "preceding_task": self.preceding_task,
            "succeeding_task": self.succeeding_task,
            "scheduled_start_time": self.scheduled_start_time,
            "scheduled_end_time": self.scheduled_end_time,
            "scheduled_machine": self.scheduled_machine
        }

