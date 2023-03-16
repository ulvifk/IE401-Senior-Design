package data;

import java.util.LinkedList;
import java.util.List;

public class Task {
    private final int id;
    private final double processingTime;
    private final int discretizedProcessingTime;
    private final List<Machine> machinesCanUndertake;
    private final Job jobWhichBelongs;
    private final double priority;
    private Task precedingTask;
    private Task succeedingTask;
    private final int precedingTaskId;
    private final int succeedingTaskId;
    private final int oldScheduleTime;
    private final Machine oldScheduleMachine;

    public Task(int id, double processingTime, int discretizedProcessingTime, int precedingTaskId,
                int succeedingTaskId, Job jobWhichBelongs, double priority, int oldScheduleTime, Machine oldScheduledMachine) {
        this.id = id;
        this.processingTime = processingTime;
        this.discretizedProcessingTime = discretizedProcessingTime;
        this.machinesCanUndertake = new LinkedList<>();
        this.jobWhichBelongs = jobWhichBelongs;
        this.precedingTaskId = precedingTaskId;
        this.succeedingTaskId = succeedingTaskId;
        this.priority = priority;
        this.oldScheduleTime = oldScheduleTime;
        this.oldScheduleMachine = oldScheduledMachine;
    }

    public double getProcessingTime() {
        return processingTime;
    }

    public List<Machine> getMachinesCanUndertake() {
        return machinesCanUndertake;
    }

    public void addMachineCanUnderTake(Machine machine) {
        this.machinesCanUndertake.add(machine);
    }

    public Job getJobWhichBelongs() {
        return jobWhichBelongs;
    }

    public double getPriority() {
        return priority;
    }

    public Task getPrecedingTask() {
        return precedingTask;
    }

    public Task getSucceedingTask() {
        return succeedingTask;
    }

    public int getId() {
        return id;
    }

    public int getDiscretizedProcessingTime() {
        return discretizedProcessingTime;
    }

    public int getPrecedingTaskId() {
        return precedingTaskId;
    }

    public int getSucceedingTaskId() {
        return succeedingTaskId;
    }

    public Integer getOldScheduleTime() {
        return oldScheduleTime;
    }

    public void setSucceedingTask(Task succeedingTask) {
        this.succeedingTask = succeedingTask;
    }

    public void setPrecedingTask(Task precedingTask) {
        this.precedingTask = precedingTask;
    }

    public Machine getOldScheduleMachine() {
        return oldScheduleMachine;
    }
}
