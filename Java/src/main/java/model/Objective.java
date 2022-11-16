package model;

import data.Job;
import data.Parameters;
import data.Task;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class Objective {

    public static void setObjective(GRBModel model, Parameters parameters, Variables variables) throws GRBException {
        GRBLinExpr obj = new GRBLinExpr();
        for(Job job : parameters.getSetOfJobs()){
            Task lastTask = job.getTasks().get(job.getTasks().size()-1);
            for(int t = 0; t<=parameters.getFinalTimePoint(); t++){
                int completionTime = t + lastTask.getDiscretizedProcessingTime();
                double priority = lastTask.getPriority();
                int oldScheduleTime = lastTask.getOldScheduleTime() != null ? lastTask.getOldScheduleTime() : t;
                double robustnessPenalty = Math.pow((oldScheduleTime - t), 2);

                double coefficient = priority * completionTime + robustnessPenalty;

                GRBVar var = variables.getZ().get(lastTask).get(t);
                obj.addTerm(coefficient, var);
            }
        }

        model.setObjective(obj, GRB.MINIMIZE);
    }
}
