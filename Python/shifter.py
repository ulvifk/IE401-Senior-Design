import json

with open("../Java/output/computational_runs_1/scenario_1_10_03_03_03.json", "r") as f:
    scenario = json.load(f)

shift_amount = 20
change_in_deadlines = {4: 30}

change_tp_high = [4]

for job in scenario["jobs"]:
    job_id = int(job["id"])
    deadline = int(job["deadline"])

    if job_id in change_tp_high:
        job["priority"] = "HIGH"

    job["deadline"] = deadline - shift_amount

    if job_id in change_in_deadlines:
        job["deadline"] = job["deadline"] - change_in_deadlines[job_id]

    if job["deadline"] < 0:
        job["deadline"] = 0

    tasks_to_be_removed = []
    for task in job["tasks"]:
        scheduled_time = int(task["schedule"])
        processing_time = int(task["processing_time"])
        end_time = scheduled_time + processing_time

        if shift_amount >= end_time:
            tasks_to_be_removed.append(task)

        elif shift_amount > scheduled_time:
            task["processing_time"] = end_time - shift_amount
            task["schedule"] = 0

        else:
            task["schedule"] = scheduled_time - shift_amount

    job["tasks"] = [task for task in job["tasks"] if task not in tasks_to_be_removed]
with open(f"../Java/input/robustness/shifted_scenario_{shift_amount}.json", "w") as f:
    json.dump(scenario, f)



