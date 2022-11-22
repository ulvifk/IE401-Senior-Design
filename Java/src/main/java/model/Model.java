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

    public GRBModel getModel() {
        return model;
    }

    public Variables getVariables() {
        return variables;
    }

    public boolean isSolutionFound() throws GRBException {
        if(this.model.get(GRB.IntAttr.SolCount) > 0)
            return true;
        else
            return false;
    }
}
