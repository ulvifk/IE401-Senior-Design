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

    // Hash by id
    @Override
    public int hashCode() {
        return id;
    }

    // Compare by id
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Machine other = (Machine) obj;
        return this.id == other.id;
    }
}
