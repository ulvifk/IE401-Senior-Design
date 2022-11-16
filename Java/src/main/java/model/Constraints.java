package model;

import data.Machine;
import data.Parameters;
import data.Task;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBLinExpr;
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
            for(int t = 0; t<=parameters.getFinalTimePoint(); t++){
                GRBVar z = variables.getZ().get(i).get(t);
                lhs.addTerm(1, z);
            }

            model.addConstr(lhs, GRB.EQUAL, 1, "Task_%d_only_once_assignment_cons");
        }
    }

    private static void setCapacityConstraint(GRBModel model, Variables variables, Parameters parameters) throws GRBException {
        for(Machine k : parameters.getSetOfMachines()){
            for(int t = 0; t<=parameters.getFinalTimePoint(); t++){
                GRBLinExpr lhs = new GRBLinExpr();
                for(Task i : k.getSetOfAssignedTasks()){
                    ArrayList<Integer> setOfTBar = getSetOfTBar(i, t);
                    for(Integer tBar : setOfTBar){
                        GRBVar var = variables.getZ().get(i).get(tBar);
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
            for(int t = 0; t<=parameters.getFinalTimePoint(); t++){
                GRBVar var = variables.getZ().get(i).get(t);
                lhs.addTerm(t+i.getDiscretizedProcessingTime(), var);
            }

            GRBLinExpr rhs = new GRBLinExpr();
            for(int t = 0; t<=parameters.getFinalTimePoint(); t++){
                GRBVar var = variables.getZ().get(l).get(t);
                rhs.addTerm(t, var);
            }

            model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("Precedence_constraint_task[%d]_task[%d]", i.getId(), l.getId()));
        }
    }

    private static ArrayList<Integer> getSetOfTBar(Task i, int t){
        ArrayList<Integer> setOfTBar = new ArrayList<>();
        for(int tBar = (t - i.getDiscretizedProcessingTime()) >= 0 ? (t - i.getDiscretizedProcessingTime()) + 1 : 0
            ; tBar<=t; tBar++){
            setOfTBar.add(tBar);
        }

        return setOfTBar;
    }
}
