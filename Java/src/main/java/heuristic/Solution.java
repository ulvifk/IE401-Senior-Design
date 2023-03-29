package heuristic;

import data.Machine;
import data.Task;

public class Solution {
    final Task task;
    Machine machine;
    double startTime;
    double finishTime;
    double score;

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

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public void setFinishTime(double finishTime) {
        this.finishTime = finishTime;
    }
}
