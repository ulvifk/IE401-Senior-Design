package data;

import java.util.ArrayList;

public class Job {
    private final int id;
    private final ArrayList<Task> tasks;
    private final int deadline;
    private final double priority;

    public Job(int id, int deadline, double priority) {
        this.id = id;
        this.tasks = new ArrayList<>();
        this.deadline = deadline;
        this.priority = priority;
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
}
