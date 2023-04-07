from task_type import TASK_TYPE
from machine import Machine

class Scenario:
    machine_map: dict[TASK_TYPE, list[Machine]] = {}