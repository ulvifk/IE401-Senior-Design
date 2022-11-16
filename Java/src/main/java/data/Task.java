package data;

import data.enums.Priority;

public class Task {
    private int id;
    private double processingTime;
    private int discretizedProcessingTime;
    private Machine assignedMachine;
    private Job jobWhichBelongs;
    private double priority;
    private Task precedingTask;
    private Task succeedingTask;

    private int precedingTaskId;
    private int succeedingTaskId;

    private Integer oldScheduleTime;

    public double getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(double processingTime) {
        this.processingTime = processingTime;
    }

    public Machine getAssignedMachine() {
        return assignedMachine;
    }

    public void setAssignedMachine(Machine assignedMachine) {
        this.assignedMachine = assignedMachine;
    }

    public Job getJobWhichBelongs() {
        return jobWhichBelongs;
    }

    public void setJobWhichBelongs(Job jobWhichBelongs) {
        this.jobWhichBelongs = jobWhichBelongs;
    }

    public double getPriority() {
        return priority;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }

    public Task getPrecedingTask() {
        return precedingTask;
    }

    public void setPrecedingTask(Task precedingTask) {
        this.precedingTask = precedingTask;
    }

    public Task getSucceedingTask() {
        return succeedingTask;
    }

    public void setSucceedingTask(Task succeedingTask) {
        this.succeedingTask = succeedingTask;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDiscretizedProcessingTime() {
        return discretizedProcessingTime;
    }

    public void setDiscretizedProcessingTime(int discretizedProcessingTime) {
        this.discretizedProcessingTime = discretizedProcessingTime;
    }

    public int getPrecedingTaskId() {
        return precedingTaskId;
    }

    public void setPrecedingTaskId(int precedingTaskId) {
        this.precedingTaskId = precedingTaskId;
    }

    public int getSucceedingTaskId() {
        return succeedingTaskId;
    }

    public void setSucceedingTaskId(int succeedingTaskId) {
        this.succeedingTaskId = succeedingTaskId;
    }

    public Integer getOldScheduleTime() {
        return oldScheduleTime;
    }

    public void setOldScheduleTime(Integer oldScheduleTime) {
        this.oldScheduleTime = oldScheduleTime;
    }
}
