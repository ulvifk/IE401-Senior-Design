package data;

import data.enums.Priority;

import java.util.ArrayList;

public class Job {
    private int id;
    private ArrayList<Task> tasks;
    private int deadline;
    private double priority;

    public ArrayList<Task> getTasks() {
        return tasks;
    }

    public void setTasks(ArrayList<Task> tasks) {
        this.tasks = tasks;
    }

    public int getDeadline() {
        return deadline;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

    public double getPriority() {
        return priority;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }
}
