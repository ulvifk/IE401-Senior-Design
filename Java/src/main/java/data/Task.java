package data;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Task {
    private final int id;
    private final Map<Machine, Double> processingTimes;
    private final double processingTime;
    private final Map<Machine, Integer> discretizedProcessingTimes;
    private final List<Machine> machinesCanUndertake;
    private final Job jobWhichBelongs;
    private final double priority;
    private Task precedingTask;
    private Task succeedingTask;
    private final int precedingTaskId;
    private final int succeedingTaskId;
    private final int oldScheduleTime;
    private final Machine oldScheduleMachine;

    public Task(int id, double processingTime, int precedingTaskId,
                int succeedingTaskId, Job jobWhichBelongs, double priority, int oldScheduleTime, Machine oldScheduledMachine) {
        this.id = id;
        this.processingTime = processingTime;
        this.processingTimes = new HashMap<>();
        this.discretizedProcessingTimes = new HashMap<>();
        this.machinesCanUndertake = new LinkedList<>();
        this.jobWhichBelongs = jobWhichBelongs;
        this.precedingTaskId = precedingTaskId;
        this.succeedingTaskId = succeedingTaskId;
        this.priority = priority;
        this.oldScheduleTime = oldScheduleTime;
        this.oldScheduleMachine = oldScheduledMachine;
    }

    public double getProcessingTime(Machine k) {
        return processingTimes.get(k);
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

    public int getDiscretizedProcessingTime(Machine k) {
        return discretizedProcessingTimes.get(k);
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

    public void addProcessingTime(Machine machine) {
        this.processingTimes.put(machine, this.processingTime * machine.getProcessingTimeConstant());
    }

    public void addDiscretizedProcessingTime(Machine machine, int discretizedProcessingTime) {
        this.discretizedProcessingTimes.put(machine, discretizedProcessingTime);
    }
}
