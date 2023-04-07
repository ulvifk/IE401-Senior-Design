from dataclasses import dataclass
from task_type import TASK_TYPE


@dataclass
class Machine:
    id: int
    task_type_undertakes: TASK_TYPE
    machine_name: str
    processing_time_constant: float

    def __dict__(self):
        return {
            "id": self.id,
            "task_type_undertakes": self.task_type_undertakes.name,
            "machine_name": self.machine_name,
            "processing_time_constant": self.processing_time_constant
        }