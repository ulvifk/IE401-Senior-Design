package model;

import data.Job;
import data.Parameters;
import data.Task;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.ArrayList;
import java.util.List;

public class Objective {

    public static void setObjective(GRBModel model, Parameters parameters, Variables variables) throws GRBException {
        GRBLinExpr obj = new GRBLinExpr();
        for(Job job : parameters.getSetOfJobs()){
            if(job.getTasks().size() == 0) continue;
            Task lastTask = job.getTasks().get(job.getTasks().size()-1);
            for(int t = 0; t<=parameters.getFinalTimePoint(); t++){
                int completionTime = t + lastTask.getDiscretizedProcessingTime();
                double priority = lastTask.getPriority();

                GRBVar var = variables.getZ().get(lastTask).get(t);
                obj.addTerm(completionTime * priority * parameters.getAlphaCompletionTime(), var);
            }
        }

        for(Job job : parameters.getSetOfJobs()){
            if(job.getTasks().size() == 0) continue;
            Task lastTask = job.getTasks().get(job.getTasks().size()-1);
            for(int t : parameters.getSetOfTardyTimes(lastTask)){
                double tardinessPenalty = getTardinessPenalty(lastTask, t);

                GRBVar var = variables.getZ().get(lastTask).get(t);
                obj.addTerm(tardinessPenalty * parameters.getAlphaTardiness(), var);
            }
        }

        for(Task i : parameters.getSetOfTasks()){
            for(int t = 0; t<=parameters.getFinalTimePoint(); t++){
                double robustnessPenalty = getRobustnessPenalty(i, t);

                GRBVar var = variables.getZ().get(i).get(t);
                obj.addTerm(robustnessPenalty * parameters.getAlphaRobust(), var);
            }
        }

        model.setObjective(obj, GRB.MINIMIZE);
    }

    private static double getRobustnessPenalty(Task lastTask, int t){
        int oldScheduleTime = lastTask.getOldScheduleTime() != -1 ? lastTask.getOldScheduleTime() : t;
        double robustnessPenalty = Math.pow((oldScheduleTime - t), 2);

        return robustnessPenalty;
    }

    private static double getTardinessPenalty(Task lastTask, int t) {
        double tardinessAmount = t + lastTask.getDiscretizedProcessingTime() - lastTask.getJobWhichBelongs().getDeadline();

        return Math.pow(lastTask.getPriority() *tardinessAmount, 2);
    }

}
