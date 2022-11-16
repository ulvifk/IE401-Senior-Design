package model;

import data.Parameters;
import data.Task;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.HashMap;

public class Variables {
    private HashMap<Task, HashMap<Integer, GRBVar>> Z;

    public void createVariables(GRBModel model, Parameters parameters) throws GRBException {
        this.Z = new HashMap<>();
        for(Task i : parameters.getSetOfTasks()){
            this.Z.put(i, new HashMap<>());
            for(int t = 0; t<=parameters.getFinalTimePoint(); t++){
                GRBVar var = model.addVar(0, 1, 0, GRB.BINARY, String.format("z_[%d][%d]", i.getId(), t));
                this.Z.get(i).put(t, var);
            }
        }
    }

    public HashMap<Task, HashMap<Integer, GRBVar>> getZ() {
        return Z;
    }
}
