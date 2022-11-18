package model;

import data.Parameters;
import data.Task;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;
import output.ScenarioUpdater;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Model {

    private final GRBEnv env;
    private final GRBModel model;

    private final Parameters parameters;
    private Variables variables;

    public Model(Parameters parameters) throws GRBException{
        this.env = new GRBEnv();
        this.model = new GRBModel(env);
        this.parameters = parameters;

    }

    public void create() throws GRBException {
        this.variables = new Variables();
        variables.createVariables(model, parameters);

        Constraints.setConstraints(model, variables, parameters);
        Objective.setObjective(model, parameters, variables);
    }

    public void optimize(int timeLimit, boolean isWriteLp) throws GRBException {
        model.set(GRB.DoubleParam.TimeLimit, timeLimit);

        if(isWriteLp)
            model.write("model.lp");

        model.optimize();
    }

    public void writeSolution(String inputPath, String outputPath) throws FileNotFoundException, GRBException {
        writeSolutionToCsv("Solution.csv", this.variables);
        ScenarioUpdater.updateScenario(inputPath, outputPath, this.parameters, this.variables);
    }

    public void dispose() throws GRBException {
        this.env.dispose();
        this.model.dispose();
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

    public GRBModel getModel() {
        return model;
    }

    public Variables getVariables() {
        return variables;
    }
}
