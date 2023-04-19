import data.Parameters;
import gurobi.GRB;
import model.IterativeModel;
import model.Model;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class iterative_model {
    public static void main(String[] args) throws Exception {
        Integer[] n_jobs = {20, 30, 40, 50};
        Integer[] machineCounts = {1};
        Integer[] instances = {0, 1, 2};
        Integer[] increments = {1, 2};

        int highWeight = 1;
        int mediumWeight = 2;
        int lowWeight = 4;
        boolean doReduce = false;

        String keyWord = "iterative_model";

        String summaryDirectory = String.format("Java/output/%s", keyWord);
        File directoryFile = new File(summaryDirectory);
        if (!directoryFile.exists()) {
            directoryFile.mkdirs();
        }

        String summaryPath = String.format(summaryDirectory + "/summary_0_1_2.csv");

        PrintWriter out = new PrintWriter(summaryPath);
        out.println("instance,#jobs,#machines,increment,#Varibles,final time point," +
                "Before Tune Weighted Completion Time,Before Tune Total Deviation,Before Tune Weighted Total Tardiness," +
                "After Tune Weighted Completion Time,After Tune Total Deviation,After Tune Weighted Total Tardiness," +
                "Before Tune Objective Value,After Tune Objective,After Tune Gap," +
                "Low Cpu Time,Medium Cpu Time,High Cpu Time,Tune Cpu Time");

        for (int n_job : n_jobs) {
            for (int seed : instances){
                for (int increment : increments) {
                    for (int machineCount : machineCounts) {
                        String inputPath = String.format("Java/input/scenario_%d_%d_%d.json", seed, n_job, machineCount);
                        String outputDirectory = String.format("Java/output/%s/scenario_seed_%d_nJob_%d_machineCount_%d/mip_increment_%d", keyWord, seed, n_job, machineCount, increment);

                        directoryFile = new File(outputDirectory);
                        if (!directoryFile.exists()) {
                            directoryFile.mkdirs();
                        }

                        Parameters parameters = new Parameters(increment, doReduce);
                        parameters.highWeight = highWeight;
                        parameters.mediumWeight = mediumWeight;
                        parameters.lowWeight = lowWeight;
                        parameters.readData(inputPath);

                        IterativeModel model = new IterativeModel(inputPath, increment);
                        model.optimize(600, false, outputDirectory + "/log.txt");
                        model.writeSolutions(inputPath, outputDirectory + "/model_solution.json");
                        model.writeStats(outputDirectory + "/model_stats.json");

                        out.println(String.format("%d,%d,%d,%d,%d,%d," +
                                "%f,%f,%f," +
                                "%f,%f,%f," +
                                "%f,%f,%f," +
                                "%f,%f,%f,%f",
                                seed, n_job, parameters.getSetOfMachines().size(), increment, 0, parameters.getFinalTimePoint(),
                                model.beforeTuneTotalWeightedCompletionTime, model.beforeTuneTotalDeviation, model.beforeTuneTotalWeightedTardiness,
                                model.afterTuneTotalWeightedCompletionTime, model.afterTuneTotalDeviation, model.afterTuneTotalWeightedTardiness,
                                model.beforeTuneObjective, model.afterTuneObjective, model.afterTuneGap,
                                model.lowCpuTime, model.mediumCpuTime, model.highCpuTime, model.fineTuneCpuTime));
                    }
                }
            }
        }
        out.close();
    }
}


