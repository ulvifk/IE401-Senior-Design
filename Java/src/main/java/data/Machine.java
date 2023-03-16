package data;

import java.util.ArrayList;

public class Machine {
    private final int id;
    private final String type;
    private ArrayList<Task> setOfAssignedTasks;
    private final double processingTimeConstant;

    public Machine(int id, double processingTimeConstant, String type) {
        this.setOfAssignedTasks = new ArrayList<>();
        this.id = id;
        this.processingTimeConstant = processingTimeConstant;
        this.type = type;
    }

    public int getId() {
        return id;
    }
    public ArrayList<Task> getSetOfAssignedTasks() {
        return setOfAssignedTasks;
    }

    public void setSetOfAssignedTasks(ArrayList<Task> setOfAssignedTasks) {
        this.setOfAssignedTasks = setOfAssignedTasks;
    }

    public double getProcessingTimeConstant() {
        return processingTimeConstant;
    }

    public String getType() {
        return type;
    }

    public void addTask(Task task){
        this.setOfAssignedTasks.add(task);
    }
}
