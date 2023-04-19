package model;

import data.Machine;
import data.Parameters;
import data.Task;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.HashMap;

public class Variables {
    private HashMap<Task, HashMap<Machine, HashMap<Integer, GRBVar>>> Z;
    public int variableCount = 0;

    public void createVariables(GRBModel model, Parameters parameters) throws GRBException {
        this.Z = new HashMap<>();
        for(Task i : parameters.getSetOfTasks()){
            this.Z.put(i, new HashMap<>());
            for (Machine machine  : i.getMachinesCanUndertake()) {
                this.Z.get(i).put(machine, new HashMap<>());
                for (int t : parameters.getSetOfTimePoints(i, machine)) {
                    GRBVar var = model.addVar(0, 1, 0, GRB.BINARY, String.format("z_[%d][%d][%d]", i.getId(), machine.getId(), t));
                    this.Z.get(i).get(machine).put(t, var);
                    this.variableCount ++;
                }
            }
        }
    }

    public HashMap<Task, HashMap<Machine, HashMap<Integer, GRBVar>>> getZ() {
        return Z;
    }
}
