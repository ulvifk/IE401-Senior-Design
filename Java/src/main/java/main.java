import data.Job;
import data.Machine;
import data.Parameters;
import data.Task;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBVar;
import heuristic.Heuristic;
import kpi.Kpi;
import model.Model;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class main {
    public static void main(String[] args) throws Exception {

        List<String> inputs = new ArrayList<>();
        List<String> outputs = new ArrayList<>();

        Integer[] n_jobs = new Integer[] {5, 10, 15};
        Integer[] seeds = new Integer[] {0};

        for(Integer seed : seeds) {
            for (Integer n_job : n_jobs) {
                inputs.add(String.format("input/scenario_%d_%d_dar.json", seed, n_job));
                outputs.add(String.format("output/scenario_%d_%d_dar", seed, n_job));
            }
        }

        for (String input : inputs){
            String outputDirectory = outputs.get(inputs.indexOf(input));
            File directoryFile = new File(outputDirectory);
            if (!directoryFile.exists()) {
                directoryFile.mkdirs();
            }

            Parameters parameters = new Parameters();
            parameters.readData(input);

            Heuristic heuristic = new Heuristic(parameters);
            heuristic.optimize();
            heuristic.writeSolution(input, outputDirectory + "/heuristic_solution.json");
            heuristic.writeStats(outputDirectory + "/heuristic_stats.json");
        }

        for (String input : inputs){
            String outputDirectory = outputs.get(inputs.indexOf(input));
            File directoryFile = new File(outputDirectory);
            if (!directoryFile.exists()) {
                directoryFile.mkdirs();
            }

            Parameters parameters = new Parameters();
            parameters.readData(input);

            Model model = new Model(parameters);
            model.create();
            model.optimize(600, false);
            model.writeSolution(input, outputDirectory + "/model_solution.json");
            model.writeStats(outputDirectory + "/model_stats.json");
        }

    }

}
