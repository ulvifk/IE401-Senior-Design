package heuristic;

import data.Machine;
import data.Task;

public class Solution {
    final Task task;
    final Machine machine;
    final double startTime;

    public Solution(Task task, Machine machine, double startTime) {
        this.task = task;
        this.machine = machine;
        this.startTime = startTime;
    }
}
