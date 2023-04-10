package model;

import data.Machine;
import data.Parameters;
import data.Task;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.ArrayList;

public class Constraints {

    public static void setConstraints(GRBModel model, Variables variables, Parameters parameters) throws GRBException {
        setCapacityConstraint(model, variables, parameters);
        setAssignOnceConstraint(model, variables, parameters);
        setPrecedenceConstraint(model, variables, parameters);
    }

    private static void setAssignOnceConstraint(GRBModel model, Variables variables, Parameters parameters) throws GRBException {
        for(Task i : parameters.getSetOfTasks()){
            GRBLinExpr lhs = new GRBLinExpr();
            for (Machine machine : i.getMachinesCanUndertake()) {
                for(int t : parameters.getSetOfTimePoints()){
                    GRBVar z = variables.getZ().get(i).get(machine).get(t);
                    lhs.addTerm(1, z);
                }
            }

            model.addConstr(lhs, GRB.EQUAL, 1, "Task_%d_only_once_assignment_cons");
        }
    }

    private static void setCapacityConstraint(GRBModel model, Variables variables, Parameters parameters) throws GRBException {
        for(Machine k : parameters.getSetOfMachines()){
            for(int t : parameters.getSetOfTimePoints()){
                GRBLinExpr lhs = new GRBLinExpr();
                for(Task i : k.getSetOfAssignedTasks()){
                    ArrayList<Integer> setOfTBar = getSetOfTBar(i, k, t, parameters);
                    for(Integer tBar : setOfTBar){
                        GRBVar var = variables.getZ().get(i).get(k).get(tBar);
                        lhs.addTerm(1, var);
                    }
                }

                model.addConstr(lhs, GRB.LESS_EQUAL, 1, String.format("Capacity_constraint_machine_%d", k.getId()));
            }
        }
    }

    private static void setPrecedenceConstraint(GRBModel model, Variables variables, Parameters parameters) throws GRBException {
        for(Task i : parameters.getSetOfTasks()){
            if(i.getSucceedingTask() == null) continue;
            Task l = i.getSucceedingTask();

            GRBLinExpr lhs = new GRBLinExpr();
            for (Machine k : i.getMachinesCanUndertake()) {
                for (int t : parameters.getSetOfTimePoints()) {
                    GRBVar var = variables.getZ().get(i).get(k).get(t);
                    lhs.addTerm(t + i.getDiscretizedProcessingTime(k), var);
                }
            }

            GRBLinExpr rhs = new GRBLinExpr();
            for (Machine k : l.getMachinesCanUndertake()) {
                for (int t : parameters.getSetOfTimePoints()) {
                    GRBVar var = variables.getZ().get(l).get(k).get(t);
                    rhs.addTerm(t, var);
                }
            }
            model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("Precedence_constraint_task[%d]_task[%d]", i.getId(), l.getId()));
        }
    }

    private static ArrayList<Integer> getSetOfTBar(Task i, Machine k, int t, Parameters parameters) {
        ArrayList<Integer> setOfTBar = new ArrayList<>();

        int lowerBound = (t - i.getDiscretizedProcessingTime(k)) >= 0 ? (t - i.getDiscretizedProcessingTime(k)) + 1 : 0;
        int upperBound = t;

        for (int tBar : parameters.getSetOfTimePoints()){
            if (tBar >= lowerBound && tBar <= upperBound) setOfTBar.add(tBar);
        }

        return setOfTBar;
    }
}
