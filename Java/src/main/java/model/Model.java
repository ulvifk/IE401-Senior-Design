package model;

import data.Parameters;
import data.Task;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Model {

    public Model(Parameters parameters) throws GRBException, FileNotFoundException {
        GRBEnv env = new GRBEnv();
        GRBModel model = new GRBModel(env);

        model.set(GRB.DoubleParam.TimeLimit, 300);

        Variables variables = new Variables();
        variables.createVariables(model, parameters);

        Constraints.setConstraints(model, variables, parameters);
        Objective.setObjective(model, parameters, variables);


        model.write("model.lp");

        model.optimize();

        writeSolutionToCsv("Solution.csv", variables);

        env.dispose();
        model.dispose();
    }

    private void printSolution(Variables variables) throws GRBException {
        for(Task i : variables.getZ().keySet()){
            for(int t : variables.getZ().get(i).keySet()){
                GRBVar var = variables.getZ().get(i).get(t);
                double val = var.get(GRB.DoubleAttr.X);

                if(val > 0.5){
                    System.out.println(var.get(GRB.StringAttr.VarName));
                }
            }
        }
    }

    private void writeSolutionToCsv(String filePath, Variables variables) throws FileNotFoundException, GRBException {
        PrintWriter out = new PrintWriter(filePath);
        out.println("Job,Task,Machine,Time,Processing_time");
        for(Task i : variables.getZ().keySet()){
            for(int t : variables.getZ().get(i).keySet()){
                GRBVar var = variables.getZ().get(i).get(t);
                double val = var.get(GRB.DoubleAttr.X);
                if(val > 0.5){
                    out.println(String.format("%d,%d,%d,%d,%d",i.getJobWhichBelongs().getId(),i.getId(), i.getAssignedMachine().getId(),t,i.getDiscretizedProcessingTime()));
                }
            }
        }
        out.close();
    }
}
