from task import Task
from dataclasses import dataclass


@dataclass
class Job:
    id: int
    tasks: list[Task]
    deadline: int
    priority: str

    def json_encoded(self):
        json_string = {"id": self.id, "deadline": self.deadline, "priority": self.priority,
            "tasks": [task.__dict__() for task in self.tasks]}
        return json_string