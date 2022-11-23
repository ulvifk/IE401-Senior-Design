import com.google.gson.Gson;
import data.Job;
import data.Parameters;
import data.Task;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBVar;
import kpi.Kpi;
import model.Model;
import output.ScenarioUpdater;

import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class main {
    public static void main(String[] args) throws Exception {
        String inputPath = "input/scenario.json";
        String outputPath = "output/scenario.json";

        Parameters parameters = new Parameters();
        parameters.readData(inputPath);

        Model model = new Model(parameters);
        model.create();
        model.optimize(600, false);

        if(model.isSolutionFound()){
            model.writeSolution(inputPath, outputPath);
        }
    }

    private static void runTests(List<String> inputPaths, List<String> outputPaths) throws Exception {
        List<Kpi> kpis = new ArrayList<>();
        for(int i = 0; i<inputPaths.size(); i++){
            Parameters parameters = new Parameters();
            parameters.readData(inputPaths.get(i));

            Model model = new Model(parameters);
            model.create();
            model.optimize(600, false);
            while(model.getModel().get(GRB.IntAttr.Status) == GRB.INFEASIBLE){
                parameters.finalTimePoint += 50;
                model.dispose();

                model = new Model(parameters);
                model.create();
                model.optimize(600, false);
            }
            if(model.isSolutionFound()){
                model.writeSolution(inputPaths.get(i), outputPaths.get(i));
            }

            Kpi kpi = generateKPI(inputPaths.get(i), outputPaths.get(i), model, parameters);
            kpis.add(kpi);
            model.dispose();
        }

        String json = new Gson().toJson(kpis);
        PrintWriter out = new PrintWriter("output/robustness/kpis_shifted_20.json");
        out.println(json);
        out.close();
    }

    private static Kpi generateKPI(String input, String output, Model model, Parameters parameters) throws GRBException {
        Kpi kpi = new Kpi();
        kpi.cpuTime = model.getModel().get(GRB.DoubleAttr.Runtime);
        kpi.objective = model.isSolutionFound() ? model.getModel().get(GRB.DoubleAttr.ObjVal): -1;
        kpi.gap = model.isSolutionFound() ? model.getModel().get(GRB.DoubleAttr.MIPGap): -1;
        kpi.scenarioPath = input;
        kpi.outputScenario = output;

        kpi.totalTardiness = 0;
        if(model.isSolutionFound()) {
            for (Job job : parameters.getSetOfJobs()) {
                if(job.getTasks().size() == 0) continue;
                Task lastTask = job.getTasks().get(job.getTasks().size() - 1);
                for (int t : parameters.getSetOfTardyTimes(lastTask)) {
                    GRBVar var = model.getVariables().getZ().get(lastTask).get(t);
                    if (var.get(GRB.DoubleAttr.X) > 0.5) {
                        kpi.totalTardiness += t - job.getDeadline();
                    }
                }
            }
        }

        return kpi;
    }
}
