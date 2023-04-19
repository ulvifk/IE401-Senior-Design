package model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import data.Job;
import data.Machine;
import data.Parameters;
import data.Task;
import data.enums.Priority;
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

public class IterativeModel {
    private final Parameters parameters;
    public Variables variables;
    public double beforeTuneTotalWeightedCompletionTime;
    public double afterTuneTotalWeightedCompletionTime;
    public double beforeTuneTotalDeviation;
    public double afterTuneTotalDeviation;
    public double beforeTuneTotalWeightedTardiness;
    public double afterTuneTotalWeightedTardiness;
    public double afterTuneGap=0;
    public double cpuTime=0;
    private List<Solution> solutions;
    private String inputPath;

    public double lowCpuTime = 0;
    public double mediumCpuTime = 0;
    public double highCpuTime = 0;
    public double fineTuneCpuTime = 0;
    public double beforeTuneObjective = 0;
    public double afterTuneObjective = 0;


    public IterativeModel(String inputPath, int timeWindowLength) throws Exception {
        this.parameters = new Parameters(timeWindowLength, false);
        this.parameters.readData(inputPath);
        this.solutions = new LinkedList<>();
        this.inputPath = inputPath;
    }

    public void optimize(int timeLimit, boolean isWriteLp, String logPath) throws Exception {
        Priority[] priorities = {Priority.HIGH, Priority.MEDIUM, Priority.LOW};
        int timePerPriority = timeLimit / 3;

        int additionalTimeFromPrevious = 0;
        for (Priority priority : priorities){
            System.out.println(String.format("Solving for priority %s", priority));

            Parameters priorityParameters =getPriorityParameters(priority);
            Model model = new Model(priorityParameters);
            model.create();

            int timeLimit1 = timePerPriority + additionalTimeFromPrevious;
            model.optimize(timeLimit1, isWriteLp, logPath);
            List<Solution> newSolutions = getSolutions(model.variables);
            this.solutions.addAll(newSolutions);

            double cpuTime = model.cpuTime;
            additionalTimeFromPrevious = (int) Math.max(0, timeLimit1 - cpuTime);

            if (priority == Priority.HIGH){
                this.highCpuTime = cpuTime;
            } else if (priority == Priority.MEDIUM){
                this.mediumCpuTime = cpuTime;
            } else if (priority == Priority.LOW){
                this.lowCpuTime = cpuTime;
            }
            model.dispose();
        }

        revokeDiscretization();

        Map<Task, Solution> dummySolutions = new HashMap<>();
        for (Solution solution : this.solutions){
            dummySolutions.put(solution.getTask(), solution);
        }

        this.beforeTuneTotalWeightedCompletionTime = ScenarioUpdater.calculateTotalWeightedCompletionTime(dummySolutions);
        this.beforeTuneTotalDeviation = ScenarioUpdater.calculateDeviationFromEarlierPlan(dummySolutions);
        this.beforeTuneTotalWeightedTardiness = ScenarioUpdater.calculateTotalWeightedTardiness(dummySolutions);
        this.beforeTuneObjective = this.parameters.getAlphaRobust() * this.beforeTuneTotalDeviation +
                this.parameters.getAlphaTardiness() * this.beforeTuneTotalWeightedTardiness +
                this.parameters.getAlphaCompletionTime() * this.beforeTuneTotalWeightedCompletionTime;


        if (additionalTimeFromPrevious > 5){
            System.out.println("Further optimizing...");

            Parameters newParameters = new Parameters(1, false);
            newParameters.readData(inputPath);
            Model fullModel = new Model(newParameters);
            fullModel.create();
            feedToFullModel(fullModel, newParameters);
            fullModel.optimize(additionalTimeFromPrevious, isWriteLp, logPath);
            this.solutions = getSolutions(fullModel.variables);

            this.fineTuneCpuTime = fullModel.cpuTime;
            this.afterTuneGap = fullModel.gap;
        }

        dummySolutions = new HashMap<>();
        for (Solution solution : this.solutions){
            dummySolutions.put(solution.getTask(), solution);
        }

        this.afterTuneTotalWeightedCompletionTime = ScenarioUpdater.calculateTotalWeightedCompletionTime(dummySolutions);
        this.afterTuneTotalDeviation = ScenarioUpdater.calculateDeviationFromEarlierPlan(dummySolutions);
        this.afterTuneTotalWeightedTardiness = ScenarioUpdater.calculateTotalWeightedTardiness(dummySolutions);
        this.afterTuneObjective = this.parameters.getAlphaRobust() * this.afterTuneTotalDeviation +
                this.parameters.getAlphaTardiness() * this.afterTuneTotalWeightedTardiness +
                this.parameters.getAlphaCompletionTime() * this.afterTuneTotalWeightedCompletionTime;
    }

    private void feedToFullModel(Model model, Parameters parameters) throws GRBException {
        for (Solution solution : solutions){
            Task task = parameters.getSetOfTasks().stream().filter(t -> t.getId() == solution.getTask().getId()).findFirst().orElseThrow();
            Machine machine = parameters.getSetOfMachines().stream().filter(m -> m.getId() == solution.getMachine().getId()).findFirst().orElseThrow();
            int startTime = (int) solution.getStartTime();
            model.variables.getZ().get(task).get(machine).get(startTime).set(GRB.DoubleAttr.Start, 1);
        }
    }

    private List<Solution> getSolutions(Variables variables) throws GRBException {
        List<Solution> solutions = new LinkedList<>();
        for (Task task : variables.getZ().keySet()){
            for (Machine machine : variables.getZ().get(task).keySet()){
                for (int timePoint : variables.getZ().get(task).get(machine).keySet()){
                    if (variables.getZ().get(task).get(machine).get(timePoint).get(GRB.DoubleAttr.X) > 0.5){
                        double endTime = timePoint + task.getDiscretizedProcessingTime(machine);
                        solutions.add(new Solution(task, machine, timePoint, endTime));
                    }
                }
            }
        }

        return solutions;
    }

    private void revokeDiscretization() {
        Map<Task, Pair<Machine, Integer>> taskMapping = new HashMap<>();
        Map<Machine, List<Task>> machineOrderings = new HashMap<>();
        for (Machine machine : parameters.getSetOfMachines()){
            machineOrderings.put(machine, new LinkedList<>());
        }

        for (Solution solution : solutions){
            Task task = solution.getTask();
            Machine machine = solution.getMachine();
            int startTime = (int) solution.getStartTime();
            taskMapping.put(task, new Pair<>(machine, startTime));
            machineOrderings.get(machine).add(task);
        }

        for (Machine machine : machineOrderings.keySet()){
            List<Task> taskList = machineOrderings.get(machine);
            taskList.sort(Comparator.comparing(t -> taskMapping.get(t).second));
        }

        Map<Task, Task> machinePredecessors = new HashMap<>();
        for (Machine machine : machineOrderings.keySet()){
            List<Task> tasks = machineOrderings.get(machine);
            if (tasks.size() == 0) continue;
            machinePredecessors.put(tasks.get(0), null);
            for (int i = 0; i < tasks.size() - 1; i++){
                Task task = tasks.get(i);
                Task nextTask = tasks.get(i + 1);
                machinePredecessors.put(nextTask, task);
            }
        }

        List<Task> unassignedTasks = new LinkedList<>(parameters.getSetOfTasks());
        Map<Task, Double> startTimes = new HashMap<>();
        Map<Task, Double> endTimes = new HashMap<>();

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

                double startTime = Math.max(jobPredecessorTime, machinePredecessorTime);
                double endTime = startTime + task.getProcessingTime(machine);

                startTimes.put(task, startTime);
                endTimes.put(task, endTime);

                iterator.remove();
            }
        }

        for (Solution solution : this.solutions){
            solution.setStartTime(startTimes.get(solution.getTask()));
            solution.setFinishTime(endTimes.get(solution.getTask()));
        }
    }

    private Parameters getPriorityParameters(Priority priority){
        Parameters priorityParameters = new Parameters(this.parameters.getTimeWindowLength(), this.parameters.doSizeReduction);

        priorityParameters.lowWeight = this.parameters.lowWeight;
        priorityParameters.mediumWeight = this.parameters.mediumWeight;
        priorityParameters.highWeight = this.parameters.highWeight;
        priorityParameters.getSetOfJobs().addAll(this.parameters.getSetOfJobs().stream().filter(job -> job.getPriorityEnum() == priority).toList());
        priorityParameters.getSetOfTasks().addAll(this.parameters.getSetOfTasks().stream().filter(task -> task.getPriorityEnum() == priority).toList());

        for (Machine machine : this.parameters.getSetOfMachines()){
            Machine newMachine = new Machine(machine.getId(), machine.getProcessingTimeConstant(), machine.getType());
            newMachine.getSetOfAssignedTasks().addAll(machine.getSetOfAssignedTasks().stream().filter(task -> task.getPriorityEnum() == priority).toList());
            priorityParameters.getSetOfMachines().add(newMachine);
        }

        for (Task task : priorityParameters.getSetOfTasks()){
            priorityParameters.setOfTimePoints.put(task, new HashMap<>());
            for (Machine machine : task.getMachinesCanUndertake()){
                priorityParameters.setOfTimePoints.get(task).put(machine, new LinkedList<>(this.parameters.getSetOfTimePoints(task, machine)));
            }
        }


        priorityParameters.getAllTimePoints().addAll(this.parameters.getAllTimePoints());
        priorityParameters.finalTimePoint = (int) (priorityParameters.getSetOfJobs().stream().mapToDouble(job -> job.getDeadline()).max().orElse(0) * 1.1);

        for (Solution solution : this.solutions){
            int startTime = (int) solution.getStartTime();
            int endTime = (int) solution.getFinishTime();
            Machine machine = solution.getMachine();
            for (Task task : priorityParameters.getSetOfTasks()){
                if (task.getMachinesCanUndertake().contains(machine)){
                    ListIterator<Integer> iterator = priorityParameters.getSetOfTimePoints(task, machine).listIterator();
                    while (iterator.hasNext()){
                        int t = iterator.next();
                        int wouldEndAt = startTime + task.getDiscretizedProcessingTime(machine);
                        if (startTime <= wouldEndAt && t <= endTime){
                            iterator.remove();
                        }
                    }
                }
            }
        }

        return priorityParameters;
    }

    public void writeSolutions(String inputPath, String outputPath) throws FileNotFoundException {
        FileReader reader = new FileReader(inputPath);
        JsonElement jsonElement = JsonParser.parseReader(reader);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        JsonArray jobsArray = jsonObject.getAsJsonArray("jobs");

        for(JsonElement jobElement : jobsArray){
            JsonObject jobObject = jobElement.getAsJsonObject();
            int jobId = jobObject.get("id").getAsInt();
            Job job = parameters.getSetOfJobs().stream().filter(job1 -> job1.getId() == jobId).findAny().orElse(null);

            JsonArray taskArray = jobObject.getAsJsonArray("tasks");
            for(JsonElement taskElement : taskArray){
                JsonObject taskObject = taskElement.getAsJsonObject();
                int taskId = taskObject.get("id").getAsInt();

                Solution solution = solutions.stream().filter(solution1 -> solution1.getTask().getId() == taskId).findAny().orElse(null);

                taskObject.remove("scheduled_start_time");
                taskObject.addProperty("scheduled_start_time", solution.getStartTime());

                taskObject.remove("scheduled_end_time");
                taskObject.addProperty("scheduled_end_time", solution.getFinishTime());

                taskObject.remove("scheduled_machine");
                taskObject.addProperty("scheduled_machine", solution.getMachine().getId());

                taskObject.addProperty("score", 0);
            }
        }

        String scenario = jsonObject.toString();
        PrintWriter out = new PrintWriter(outputPath);
        out.println(scenario);
        out.close();
    }

    public void writeStats(String path) throws GRBException, FileNotFoundException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("total_weighted_completion_time", this.afterTuneTotalWeightedCompletionTime);
        jsonObject.addProperty("deviation_from_earlier_plan", this.afterTuneTotalDeviation);
        jsonObject.addProperty("total_tardiness", this.afterTuneTotalWeightedTardiness);
        jsonObject.addProperty("objective", this.afterTuneObjective);
        jsonObject.addProperty("gap", this.afterTuneGap);
        jsonObject.addProperty("cpu_time", this.lowCpuTime + this.mediumCpuTime + this.highCpuTime + this.fineTuneCpuTime);
        jsonObject.addProperty("n_jobs", parameters.getSetOfJobs().size());
        jsonObject.addProperty("n_machines", parameters.getSetOfMachines().size());
        jsonObject.addProperty("n_tasks", parameters.getSetOfTasks().size());
        jsonObject.addProperty("increment", parameters.getTimeWindowLength());


        String stats = jsonObject.toString();
        PrintWriter out = new PrintWriter(path);
        out.println(stats);
        out.close();
    }
}
