import data.Job;
import data.Parameters;
import data.Task;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBVar;
import kpi.Kpi;
import model.Model;
import output.ScenarioUpdater;

import java.util.List;

public class main {
    public static void main(String[] args) throws Exception {
        Parameters parameters = new Parameters();
        parameters.readData("scenario.json");

        Model model = new Model(parameters);
        model.create();
        model.optimize(10, false);
        model.writeSolution("scenario.json", "new_scenario.json");
        model.dispose();
    }

    private static void runTests(List<String> filePaths) throws Exception {
        for(String filePath : filePaths){
            Parameters parameters = new Parameters();
            parameters.readData(filePath);

            Model model = new Model(parameters);
            model.create();
            model.optimize(10, false);
            model.writeSolution(filePath, filePath);

            Kpi kpi = generateKPI(filePath, filePath, model, parameters);
            model.dispose();
        }
    }

    private static Kpi generateKPI(String input, String output, Model model, Parameters parameters) throws GRBException {
        Kpi kpi = new Kpi();
        kpi.cpuTime = model.getModel().get(GRB.DoubleAttr.Runtime);
        kpi.objective = model.getModel().get(GRB.DoubleAttr.Obj);
        kpi.gap = model.getModel().get(GRB.DoubleAttr.MIPGap);
        kpi.scenarioPath = input;
        kpi.outputScenario = output;

        kpi.totalTardiness = 0;
        for(Job job : parameters.getSetOfJobs()){
            Task lastTask = job.getTasks().get(job.getTasks().size()-1);
            for(int t : parameters.getSetOfTardyTimes(lastTask)){
                GRBVar var = model.getVariables().getZ().get(lastTask).get(t);
                if(var.get(GRB.DoubleAttr.X) > 0.5){
                    kpi.totalTardiness += t - job.getDeadline();
                }
            }
        }

        return kpi;
    }
}
