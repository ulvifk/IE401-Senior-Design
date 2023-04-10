import data.Parameters;
import heuristic.Heuristic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class main_heuristic_run {
    public static void main(String[] args) throws Exception {

        Integer[] n_jobs = new Integer[]{20};
        Integer[] instances = new Integer[]{0};
        int[] machineCounts = {1};

        String summaryPath = "Java/output/heuristic_summary.csv";
        PrintWriter out = new PrintWriter(summaryPath);
        out.println("instance,#jobs,#machines,increment,Weighted Completion Time,Total Deviation," +
                "Weighted Total Tardiness,Objective Value");

        for (int n_job : n_jobs) {
            for (int seed : instances) {
                for (int machineCount : machineCounts) {
                    String inputPath = String.format("Java/input/scenario_%d_%d_%d.json", seed, n_job, machineCount);
                    String outputDirectory = String.format("Java/output/scenario_seed_%d_nJob_%d_machineCount_%d/heuristic", seed, n_job, machineCount);
                    File directoryFile = new File(outputDirectory);
                    if (!directoryFile.exists()) {
                        directoryFile.mkdirs();
                    }

                    Parameters parameters = new Parameters(1);
                    parameters.readData(inputPath);

                    Heuristic heuristic = new Heuristic(parameters);
                    heuristic.optimize();
                    heuristic.writeSolution(inputPath, outputDirectory + "/heuristic_solution.json");
                    heuristic.writeStats(outputDirectory + "/heuristic_stats.json");

                    out.println(String.format("%d,%d,%d,%f,%f,%f,%f",
                            seed, n_job, parameters.getSetOfMachines().size(),
                            heuristic.totalWeightedCompletionTime, heuristic.totalDeviation, heuristic.totalWeightedTardiness,
                            heuristic.objective));
                }
            }
        }

        out.close();
    }
}
