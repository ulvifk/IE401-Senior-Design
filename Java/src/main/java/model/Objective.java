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
            Task lastTask = job.getTasks().get(job.getTasks().size()-1);
            for(int t = 0; t<=parameters.getFinalTimePoint(); t++){
                int completionTime = t + lastTask.getDiscretizedProcessingTime();
                double priority = lastTask.getPriority();

                GRBVar var = variables.getZ().get(lastTask).get(t);
                obj.addTerm(completionTime * priority * parameters.getAlphaCompletionTime(), var);
            }
        }

        for(Job job : parameters.getSetOfJobs()){
            Task lastTask = job.getTasks().get(job.getTasks().size()-1);
            for(int t : getSetOfTardyTimes(parameters, lastTask)){
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
        double robustnessPenalty = Math.pow(oldScheduleTime, 2) * Math.pow((oldScheduleTime - t), 2);

        return robustnessPenalty;
    }

    private static double getTardinessPenalty(Task lastTask, int t){
        double tardinessAmount = t + lastTask.getDiscretizedProcessingTime() - lastTask.getJobWhichBelongs().getDeadline();

        return Math.pow(tardinessAmount, 2);
    }
    private static List<Integer> getSetOfTardyTimes(Parameters parameters, Task i){
        List<Integer> setOfTardyPoints = new ArrayList<>();
        for(int t = (i.getJobWhichBelongs().getDeadline() - i.getDiscretizedProcessingTime()) > 0 ? (i.getJobWhichBelongs().getDeadline() - i.getDiscretizedProcessingTime() + 1) : 0;
        t <= parameters.getFinalTimePoint(); t++){
            setOfTardyPoints.add(t);
        }

        return setOfTardyPoints;
    }

}
