package heuristic;

import data.Machine;
import data.Task;

public class Solution {
    final Task task;
    final Machine machine;
    final double startTime;
    final double finishTime;

    public Solution(Task task, Machine machine, double startTime, double finishTime) {
        this.task = task;
        this.machine = machine;
        this.startTime = startTime;
        this.finishTime = finishTime;
    }

    public Task getTask() {
        return task;
    }

    public Machine getMachine() {
        return machine;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getFinishTime() {
        return finishTime;
    }
}
