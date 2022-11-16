package data;

import javax.crypto.Mac;
import java.util.ArrayList;

public class Machine {
    private int id;
    private ArrayList<Task> setOfAssignedTasks;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Machine(){
        this.setOfAssignedTasks = new ArrayList<>();
    }
    public ArrayList<Task> getSetOfAssignedTasks() {
        return setOfAssignedTasks;
    }

    public void setSetOfAssignedTasks(ArrayList<Task> setOfAssignedTasks) {
        this.setOfAssignedTasks = setOfAssignedTasks;
    }
}
