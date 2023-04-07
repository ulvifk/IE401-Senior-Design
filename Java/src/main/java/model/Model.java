package model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import data.Machine;
import data.Parameters;
import data.Task;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import heuristic.Pair;
import heuristic.Solution;
import output.ScenarioUpdater;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class Model {

    private final GRBEnv env;
    private final GRBModel model;

    private final Parameters parameters;
    private Variables variables;
    public double totalWeightedCompletionTime;
    public double totalDeviation;
    public double totalWeightedTardiness;
    public double objective;
    public double gap;
    public double cpuTime;

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

        Map<Task, Solution> solutions = ScenarioUpdater.revokeDiscretiziation(parameters, variables);
        this.totalWeightedCompletionTime = ScenarioUpdater.calculateTotalWeightedCompletionTime(solutions);
        this.totalDeviation = ScenarioUpdater.calculateDeviationFromEarlierPlan(solutions);
        this.totalWeightedTardiness = ScenarioUpdater.calculateTotalWeightedTardiness(solutions);
        this.objective = this.parameters.getAlphaRobust() * this.totalDeviation +
                this.parameters.getAlphaTardiness() * this.totalWeightedTardiness +
                this.parameters.getAlphaCompletionTime() * this.totalWeightedCompletionTime;
        this.gap = model.get(GRB.DoubleAttr.MIPGap);
        this.cpuTime = model.get(GRB.DoubleAttr.Runtime);
    }

    public void writeSolution(String inputPath, String outputPath) throws FileNotFoundException, GRBException {
        ScenarioUpdater.updateScenario(inputPath, outputPath, this.parameters, this.variables);
    }

    public void dispose() throws GRBException {
        this.env.dispose();
        this.model.dispose();
    }

    public void writeStats(String path) throws GRBException, FileNotFoundException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("total_weighted_completion_time", this.totalWeightedCompletionTime);
        jsonObject.addProperty("deviation_from_earlier_plan", this.totalDeviation);
        jsonObject.addProperty("total_tardiness", this.totalWeightedTardiness);
        jsonObject.addProperty("objective", this.objective);
        jsonObject.addProperty("gap", this.gap);
        jsonObject.addProperty("cpu_time", this.cpuTime);
        jsonObject.addProperty("n_jobs", parameters.getSetOfJobs().size());
        jsonObject.addProperty("n_machines", parameters.getSetOfMachines().size());
        jsonObject.addProperty("n_tasks", parameters.getSetOfTasks().size());
        jsonObject.addProperty("increment", parameters.getTimeWindowLength());


        String stats = jsonObject.toString();
        PrintWriter out = new PrintWriter(path);
        out.println(stats);
        out.close();
    }

    public GRBModel getModel() {
        return model;
    }

    public Variables getVariables() {
        return variables;
    }

    public boolean isSolutionFound() throws GRBException {
        return this.model.get(GRB.IntAttr.SolCount) > 0;
    }

    public void feedSolution(String solutionPath) throws FileNotFoundException, GRBException {


        FileReader reader = new FileReader(solutionPath);

        JsonElement solutionElement = JsonParser.parseReader(reader);
        JsonObject solutionObject = solutionElement.getAsJsonObject();

        Map<Task, Pair<Machine, Double>> taskMapping = new HashMap<>();

        JsonArray jobsArray = solutionObject.get("jobs").getAsJsonArray();
        for (JsonElement jobElement : jobsArray){
            JsonObject jobObject = jobElement.getAsJsonObject();
            int jobId = jobObject.get("id").getAsInt();
            JsonArray tasksArray = jobObject.get("tasks").getAsJsonArray();
            for (JsonElement taskElement : tasksArray){
                JsonObject taskObject = taskElement.getAsJsonObject();
                int taskId = taskObject.get("id").getAsInt();
                int machineId = taskObject.get("scheduled_machine").getAsInt();
                double startTime = taskObject.get("scheduled_start_time").getAsDouble();
                double endTime = taskObject.get("scheduled_end_time").getAsDouble();

                Task task = parameters.getSetOfTasks().stream().filter(t -> t.getId() == taskId).findFirst().orElseThrow();
                Machine machine = parameters.getSetOfMachines().stream().filter(m -> m.getId() == machineId).findFirst().orElseThrow();

                taskMapping.put(task, new Pair<>(machine, startTime));
            }
        }

        Map<Machine, List<Task>> machineOrders = new HashMap<>();
        for (Task task : taskMapping.keySet()){
            Pair<Machine, Double> pair = taskMapping.get(task);
            Machine machine = pair.first;
            double startTime = pair.second;
            if (!machineOrders.containsKey(machine)){
                machineOrders.put(machine, new LinkedList<>());
            }

            machineOrders.get(machine).add(task);
        }

        for (List<Task> tasks : machineOrders.values()){
            tasks.sort(Comparator.comparing(task -> taskMapping.get(task).second));
        }

        Map<Task, Task> machinePredecessors = new HashMap<>();
        for (Machine machine : machineOrders.keySet()){
            List<Task> tasks = machineOrders.get(machine);
            machinePredecessors.put(tasks.get(0), null);
            for (int i = 0; i < tasks.size() - 1; i++){
                Task task = tasks.get(i);
                Task nextTask = tasks.get(i + 1);
                machinePredecessors.put(nextTask, task);
            }
        }

        List<Task> unassignedTasks = new LinkedList<>(parameters.getSetOfTasks());
        Map<Task, Integer> discreteStartTimes = new HashMap<>();
        Map<Task, Integer> discreteEndTimes = new HashMap<>();

        while (unassignedTasks.size() > 0){
            ListIterator<Task> iterator = unassignedTasks.listIterator();
            while (iterator.hasNext()){
                Task task = iterator.next();
                Task jobPredecessor = task.getPrecedingTask();
                Task machinePredecessor = machinePredecessors.get(task);
                Machine machine = taskMapping.get(task).first;

                if (jobPredecessor != null && !discreteEndTimes.containsKey(jobPredecessor)) continue;
                if (machinePredecessor != null && !discreteEndTimes.containsKey(machinePredecessor)) continue;

                int jobPredecessorTime = jobPredecessor != null ? discreteEndTimes.get(jobPredecessor) : 0;
                int machinePredecessorTime = machinePredecessor != null ? discreteEndTimes.get(machinePredecessor) : 0;

                int startTime = Math.max(jobPredecessorTime, machinePredecessorTime);
                int endTime = startTime + task.getDiscretizedProcessingTime(machine);

                discreteStartTimes.put(task, startTime);
                discreteEndTimes.put(task, endTime);

                iterator.remove();
            }
        }

        for (Task task : discreteStartTimes.keySet()){
            int startTime = discreteStartTimes.get(task);
            int endTime = discreteEndTimes.get(task);
            Machine machine = taskMapping.get(task).first;

            variables.getZ().get(task).get(machine).get(startTime).set(GRB.DoubleAttr.Start, 1.0);
        }
    }
}
