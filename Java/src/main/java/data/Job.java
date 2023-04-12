package data;

import data.enums.Priority;

import java.util.ArrayList;

public class Job {
    private final int id;
    private final ArrayList<Task> tasks;
    private final int deadline;
    private final double priority;
    private final Priority priorityEnum;

    public Job(int id, int deadline, double priority, Priority priorityEnum) {
        this.id = id;
        this.tasks = new ArrayList<>();
        this.deadline = deadline;
        this.priority = priority;
        this.priorityEnum = priorityEnum;
    }

    public ArrayList<Task> getTasks() {
        return tasks;
    }

    public int getDeadline() {
        return deadline;
    }

    public double getPriority() {
        return priority;
    }

    public void addTask(Task task){
        this.tasks.add(task);
    }

    public int getId() {
        return id;
    }

    public Priority getPriorityEnum() {
        return priorityEnum;
    }
}
