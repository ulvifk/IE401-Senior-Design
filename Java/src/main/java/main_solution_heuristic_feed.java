import data.Parameters;
import model.Model;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class main_solution_heuristic_feed{
    public static void main(String[] args) throws Exception {
        Integer[] n_jobs = new Integer[]{25, 30, 35, 40, 45};
        Integer[] instances = new Integer[]{0};
        Integer[] increments = {-1};
        Integer[] machineCounts = {1};

        String summaryPath = "Java/output/heuristic_feed_solution_summary.csv";
        PrintWriter out = new PrintWriter(summaryPath);
        out.println("instance,#jobs,#machines,increment,final time point,Weighted Completion Time,Total Deviation,Weighted Total Tardiness,Objective Value,Gap,Cpu Time");

        for (int n_job : n_jobs) {
            for (int seed : instances){
                for (int increment : increments) {
                    for (int machineCount : machineCounts) {
                        String inputPath = String.format("Java/input/scenario_%d_%d_%d.json", seed, n_job, machineCount);
                        String outputDirectory = String.format("Java/output/scenario_seed_%d_nJob_%d_machineCount_%d/solution_feed_%d", seed, n_job, machineCount, increment);
                        String solutionPath = String.format("Java/output/scenario_seed_%d_nJob_%d_machineCount_%d/heuristic/heuristic_solution.json", seed, n_job, machineCount, increment);

                        File directoryFile = new File(outputDirectory);
                        if (!directoryFile.exists()) {
                            directoryFile.mkdirs();
                        }

                        Parameters parameters = new Parameters(1, false);
                        parameters.readData(inputPath);

                        Model model = new Model(parameters);
                        model.create();
                        model.feedSolution(solutionPath);
                        model.optimize(600, false, outputDirectory + "/log.txt");
                        model.writeSolution(inputPath, outputDirectory + "/model_solution.json");
                        model.writeStats(outputDirectory + "/model_stats.json");

                        out.println(String.format("%d,%d,%d,%d,%d,%f,%f,%f,%f,%f,%f",
                                seed, n_job, parameters.getSetOfMachines().size(), increment, parameters.getFinalTimePoint(),
                                model.totalWeightedCompletionTime, model.totalDeviation, model.totalWeightedTardiness,
                                model.objective, model.gap, model.cpuTime));
                    }
                }
            }
        }
        out.close();
    }
}
