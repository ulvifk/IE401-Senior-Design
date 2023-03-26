package output;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import data.Job;
import data.Machine;
import data.Parameters;
import data.Task;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBVar;
import heuristic.Solution;
import model.Variables;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class ScenarioUpdater {
    public static void updateScenario(String filePath, String outputPath,Parameters parameters, Variables variables) throws FileNotFoundException, GRBException {
        FileReader reader = new FileReader(filePath);
        JsonElement jsonElement = JsonParser.parseReader(reader);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        JsonArray jobsArray = jsonObject.getAsJsonArray("jobs");

        testPrint(parameters, variables);
        Map<Task, Double> startTimes = getStartTime(parameters, variables);
        Map<Task, Machine> machineMap = getMachineMap(parameters, variables);

        for(JsonElement jobElement : jobsArray){
            JsonObject jobObject = jobElement.getAsJsonObject();
            int jobId = jobObject.get("id").getAsInt();
            Job job = parameters.getSetOfJobs().stream().filter(job1 -> job1.getId() == jobId).findAny().orElse(null);

            JsonArray taskArray = jobObject.getAsJsonArray("tasks");
            for(JsonElement taskElement : taskArray){
                JsonObject taskObject = taskElement.getAsJsonObject();
                int taskId = taskObject.get("id").getAsInt();

                Task task = job.getTasks().stream().filter(task1 -> task1.getId() == taskId).findAny().orElse(null);

                taskObject.remove("scheduled_start_time");
                taskObject.addProperty("scheduled_start_time", startTimes.get(task));

                taskObject.remove("scheduled_end_time");
                taskObject.addProperty("scheduled_end_time", startTimes.get(task) + task.getProcessingTime(machineMap.get(task)));

                taskObject.remove("scheduled_machine");
                taskObject.addProperty("scheduled_machine", machineMap.get(task).getId());

                taskObject.addProperty("score", 0);
            }
        }

        String scenario = jsonObject.toString();
        PrintWriter out = new PrintWriter(outputPath);
        out.println(scenario);
        out.close();
    }

    private static void testPrint(Parameters parameters, Variables variables) throws GRBException {
        Machine machine = parameters.getSetOfMachines().get(0);
        for(int t = 0; t<=parameters.getFinalTimePoint(); t++){
            for(Task i : machine.getSetOfAssignedTasks()){
                for (Machine k : i.getMachinesCanUndertake()) {
                    if (variables.getZ().get(i).get(k).get(t).get(GRB.DoubleAttr.X) > 0.5) {
                        System.out.printf("Task_%d, Time_%d%n", i.getId(), t);
                    }
                }
            }
        }
    }

    private static Map<Task, Machine> getMachineMap(Parameters parameters, Variables variables) throws GRBException {
        Map<Task, Machine> machineMap = new HashMap<>();
        for (Task i : parameters.getSetOfTasks()) {
            for (Machine k : i.getMachinesCanUndertake()) {
                for (int t = 0; t <= parameters.getFinalTimePoint(); t++) {
                    if (variables.getZ().get(i).get(k).get(t).get(GRB.DoubleAttr.X) > 0.5) {
                        machineMap.put(i, k);
                    }
                }
            }
        }
        return machineMap;
    }

    private static Map<Task, Double> getStartTime(Parameters parameters, Variables variables) throws GRBException {
        Map<Task, Double> startTime = new HashMap<>();

        Map<Task, Double> determinedTasks = new HashMap<>();
        List<Task> undeterminedTasks = new LinkedList<>(parameters.getSetOfTasks());
        Map<Task, Machine> machineMap = getMachineMap(parameters, variables);

        while (undeterminedTasks.size() > 0){
            ListIterator<Task> undeterminedTaskIterator = undeterminedTasks.listIterator();
            while (undeterminedTaskIterator.hasNext()) {
                Task task = undeterminedTaskIterator.next();

                Task precedingTask = task.getPrecedingTask();
                if (precedingTask != null && !determinedTasks.containsKey(precedingTask)) continue;
                Task beforeTask = getBeforeTask(task, variables);
                if (beforeTask != null && !determinedTasks.containsKey(beforeTask)) continue;

                double precedingTaskEnd = precedingTask != null ? determinedTasks.get(precedingTask) + precedingTask.getProcessingTime(machineMap.get(precedingTask)) : 0;
                double beforeTaskEnd = beforeTask != null ? determinedTasks.get(beforeTask) + beforeTask.getProcessingTime(machineMap.get(beforeTask)) : 0;

                determinedTasks.put(task, Math.max(precedingTaskEnd, beforeTaskEnd));
                undeterminedTaskIterator.remove();
            }
        }

        return determinedTasks;
    }

    public static double calculateTotalWeightedCompletionTime(Map<Task, Solution> solutions) {
        double totalWeightedCompletionTime = 0;
        for (Solution solution : solutions.values()) {
            if (solution.getTask().getSucceedingTask() == null) {
                totalWeightedCompletionTime += solution.getTask().getPriority() * solution.getFinishTime();
            }
        }

        return totalWeightedCompletionTime;
    }

    public static double calculateDeviationFromEarlierPlan(Map<Task, Solution> solutions) {
        double totalDeviation = 0;
        for (Solution solution : solutions.values()) {
            double deviation = solution.getStartTime() - solution.getTask().getOldScheduleTime();
            totalDeviation += Math.pow(deviation, 2);
        }

        return totalDeviation;
    }

    public static double calculateTotalWeightedTardiness(Map<Task, Solution> solutions) {
        double totalTardiness = 0;
        for (Solution solution : solutions.values()) {
            if (solution.getTask().getSucceedingTask() == null) {
                double tardiness = solution.getStartTime() +
                        solution.getTask().getProcessingTime(solution.getMachine()) -
                        solution.getTask().getJobWhichBelongs().getDeadline();
                tardiness = Math.max(0, tardiness);
                totalTardiness += Math.pow(tardiness, 2) * solution.getTask().getPriority();
            }
        }

        return totalTardiness;
    }

    public static Map<Task, Solution> getSolutionMap(Parameters parameters, Variables variables) throws GRBException {
        Map<Task, Machine> machineMap = getMachineMap(parameters, variables);
        Map<Task, Double> startTimes = getStartTime(parameters, variables);
        Map<Task, Solution> solutions = new HashMap<>();
        for (Task task : parameters.getSetOfTasks()){
            solutions.put(task, new Solution(task, machineMap.get(task), startTimes.get(task),
                    startTimes.get(task) + task.getProcessingTime(machineMap.get(task))));
        }

        return solutions;
    }

    public static void writeStats(String path, Parameters parameters, Map<Task, Solution> solutions) throws FileNotFoundException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("total_weighted_completion_time", calculateTotalWeightedCompletionTime(solutions));
        jsonObject.addProperty("deviation_from_earlier_plan", calculateDeviationFromEarlierPlan(solutions));
        jsonObject.addProperty("total_tardiness", calculateTotalWeightedTardiness(solutions));
        jsonObject.addProperty("n_jobs", parameters.getSetOfJobs().size());
        jsonObject.addProperty("n_machines", parameters.getSetOfMachines().size());
        jsonObject.addProperty("n_tasks", parameters.getSetOfTasks().size());


        String stats = jsonObject.toString();
        PrintWriter out = new PrintWriter(path);
        out.println(stats);
        out.close();
    }

    private static Task getBeforeTask(Task task, Variables variables) throws GRBException {
        int assignedTime = 0;
        Machine assignedMachine = null;
        for (Machine k : task.getMachinesCanUndertake()) {
            for (int t : variables.getZ().get(task).get(k).keySet()) {
                GRBVar z = variables.getZ().get(task).get(k).get(t);
                if (z.get(GRB.DoubleAttr.X) > 0.5) {
                    assignedTime = t;
                    assignedMachine = k;
                }
            }
        }

        if(assignedTime == 0) return null;

        for(int t = assignedTime-1; t>=0; t--){
            for(Task i : assignedMachine.getSetOfAssignedTasks()){
                GRBVar z = variables.getZ().get(i).get(assignedMachine).get(t);
                if(z.get(GRB.DoubleAttr.X) > 0.5){
                    return i;
                }
            }
        }

        return null;
    }
}
