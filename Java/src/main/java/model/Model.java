package model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import data.Machine;
import data.Parameters;
import data.Task;
import gurobi.GRB;
import gurobi.GRBConstr;
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
    public Variables variables;
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

    public void optimize(int timeLimit, boolean isWriteLp, String logPath) throws GRBException {
        model.set(GRB.DoubleParam.TimeLimit, timeLimit);

        if(isWriteLp)
            model.write("model.lp");

        model.set(GRB.StringParam.LogFile, logPath);

        model.optimize();

        if (model.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
            model.computeIIS();
            for (GRBConstr c : model.getConstrs()) {
                if (c.get(GRB.IntAttr.IISConstr) == 1) {
                    System.out.println(c.get(GRB.StringAttr.ConstrName));
                }
            }
        }

        if (model.get(GRB.IntAttr.SolCount) > 0) {
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
        else {
            this.totalWeightedCompletionTime = 0;
            this.totalDeviation = 0;
            this.totalWeightedTardiness = 0;
            this.objective = 0;
            this.gap = 0;
            this.cpuTime = 0;
        }
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

        Map<Machine, List<Pair<Task, Integer>>> machinePairMap = new HashMap<>();
        Map<Task, Pair<Machine, Integer>> taskMapping = new HashMap<>();

        JsonArray jobsArray = solutionObject.get("jobs").getAsJsonArray();
        for (JsonElement jobElement : jobsArray){
            JsonObject jobObject = jobElement.getAsJsonObject();
            int jobId = jobObject.get("id").getAsInt();
            JsonArray tasksArray = jobObject.get("tasks").getAsJsonArray();
            for (JsonElement taskElement : tasksArray){
                JsonObject taskObject = taskElement.getAsJsonObject();
                int taskId = taskObject.get("id").getAsInt();
                int machineId = taskObject.get("scheduled_machine").getAsInt();
                int startTime = taskObject.get("scheduled_start_time").getAsInt();
                int endTime = taskObject.get("scheduled_end_time").getAsInt();

                Task task = parameters.getSetOfTasks().stream().filter(t -> t.getId() == taskId).findFirst().orElseThrow();
                Machine machine = parameters.getSetOfMachines().stream().filter(m -> m.getId() == machineId).findFirst().orElseThrow();

                if (!machinePairMap.containsKey(machine)){
                    machinePairMap.put(machine, new LinkedList<>());
                }

                machinePairMap.get(machine).add(new Pair<>(task, startTime));
                taskMapping.put(task, new Pair<>(machine, startTime));
            }
        }

        for (Machine machine : machinePairMap.keySet()){
            List<Pair<Task, Integer>> tasks = machinePairMap.get(machine);
            tasks.sort(Comparator.comparing(p -> p.second));
        }

        Map<Task, Task> machinePredecessors = new HashMap<>();
        for (Machine machine : machinePairMap.keySet()){
            List<Pair<Task, Integer>> tasks = machinePairMap.get(machine);
            for (int i = 1; i<tasks.size(); i++){
                machinePredecessors.put(tasks.get(i).first, tasks.get(i-1).first);
            }
        }

        List<Task> unassignedTasks = new LinkedList<>(parameters.getSetOfTasks());
        Map<Task, Integer> startTimes = new HashMap<>();
        Map<Task, Integer> endTimes = new HashMap<>();

        while (unassignedTasks.size() > 0){
            ListIterator<Task> iterator = unassignedTasks.listIterator();
            while (iterator.hasNext()){
                Task task = iterator.next();
                Task jobPredecessor = task.getPrecedingTask();
                Task machinePredecessor = machinePredecessors.get(task);
                Machine machine = taskMapping.get(task).first;

                if (jobPredecessor != null && !endTimes.containsKey(jobPredecessor)) continue;
                if (machinePredecessor != null && !endTimes.containsKey(machinePredecessor)) continue;

                double jobPredecessorTime = jobPredecessor != null ? endTimes.get(jobPredecessor) : 0;
                double machinePredecessorTime = machinePredecessor != null ? endTimes.get(machinePredecessor) : 0;

                int startTime = (int) Math.max(jobPredecessorTime, machinePredecessorTime);

                for (int t : this.parameters.getSetOfTimePoints(task, machine)){
                    if (t > startTime){
                        startTime = t;
                        break;
                    }

                    if (t == this.parameters.getSetOfTimePoints(task, machine).get(this.parameters.getSetOfTimePoints(task, machine).size() - 1)){
                        throw new RuntimeException(String.format("Failed to find a time point for task %d", task.getId()));
                    }
                }

                int endTime = startTime + task.getDiscretizedProcessingTime(machine);

                startTimes.put(task, startTime);
                endTimes.put(task, endTime);

                iterator.remove();
            }
        }

        for (Task task : startTimes.keySet()){
            int startTime = startTimes.get(task);
            int endTime = endTimes.get(task);
            Machine machine = taskMapping.get(task).first;

            variables.getZ().get(task).get(machine).get(startTime).set(GRB.DoubleAttr.Start, 1.0);
        }
    }
}
