import data.Parameters;
import heuristic.Heuristic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class main_genetic_run {
    public static void main(String[] args) throws Exception {

        Integer[] n_jobs = new Integer[]{10, 15 ,20, 25, 30};
        Integer[] instances = new Integer[]{0, 1, 2};

        String summaryPath = "Java/output/heuristic_summary.csv";
        PrintWriter out = new PrintWriter(summaryPath);
        out.println("instance,#jobs,#machines,increment,Weighted Completion Time,Total Deviation," +
                "Weighted Total Tardiness,Objective Value");

        for (int n_job : n_jobs) {
            for (int seed : instances){
                String inputPath = String.format("Java/input/scenario_%d_%d.json", seed, n_job);
                String outputDirectory = String.format("Java/output/scenario_seed_%d_nJob_%d/heuristic", seed, n_job);
                File directoryFile = new File(outputDirectory);
                if (!directoryFile.exists()) {
                    directoryFile.mkdirs();
                }

                Parameters parameters = new Parameters(1);
                parameters.readData(inputPath);

                Genet

                out.println(String.format("%d,%d,%d,%f,%f,%f,%f",
                        seed, n_job, parameters.getSetOfMachines().size(),
                        heuristic.totalWeightedCompletionTime, heuristic.totalDeviation, heuristic.totalWeightedTardiness,
                        heuristic.objective));
            }
        }

        out.close();
    }
}
