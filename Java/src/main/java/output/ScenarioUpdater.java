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
import heuristic.Pair;
import heuristic.Solution;
import model.Variables;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Comparator;
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

        Map<Task, Solution> solutions = revokeDiscretiziation(parameters, variables);

        for(JsonElement jobElement : jobsArray){
            JsonObject jobObject = jobElement.getAsJsonObject();
            int jobId = jobObject.get("id").getAsInt();
            Job job = parameters.getSetOfJobs().stream().filter(job1 -> job1.getId() == jobId).findAny().orElse(null);
            if (job == null) continue;


            JsonArray taskArray = jobObject.getAsJsonArray("tasks");
            for(JsonElement taskElement : taskArray){
                JsonObject taskObject = taskElement.getAsJsonObject();
                int taskId = taskObject.get("id").getAsInt();

                Task task = job.getTasks().stream().filter(task1 -> task1.getId() == taskId).findAny().orElse(null);

                Solution solution = solutions.get(task);

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
            if (solution.getTask().getOldScheduleTime() < 0) continue;
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

    public static Map<Task, Solution> revokeDiscretiziation(Parameters parameters, Variables variables) throws GRBException {
        Map<Task, Pair<Machine, Integer>> taskMapping = new HashMap<>();
        Map<Machine, List<Task>> machineOrderings = new HashMap<>();
        for (Machine machine : parameters.getSetOfMachines()){
            machineOrderings.put(machine, new LinkedList<>());
        }

        for (Task task : variables.getZ().keySet()){
            for (Machine machine : variables.getZ().get(task).keySet()){
                for (int t : variables.getZ().get(task).get(machine).keySet()){
                    if (variables.getZ().get(task).get(machine).get(t).get(GRB.DoubleAttr.X) > 0.5){
                        taskMapping.put(task, new Pair<>(machine, t));
                    }
                }
            }
        }

        for (Task task : taskMapping.keySet()){
            Pair<Machine, Integer> pair = taskMapping.get(task);
            Machine machine = pair.first;

            if (!machineOrderings.containsKey(machine)){
                machineOrderings.put(machine, new LinkedList<>());
            }

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

        Map<Task, Solution> solutions = new HashMap<>();
        for (Task task : parameters.getSetOfTasks()){
            solutions.put(task, new Solution(task, taskMapping.get(task).first, startTimes.get(task), endTimes.get(task)));
        }

        return solutions;
    }

    public static void writeStats(String path, Parameters parameters, Map<Task, Solution> solutions, double obj) throws FileNotFoundException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("total_weighted_completion_time", calculateTotalWeightedCompletionTime(solutions));
        jsonObject.addProperty("deviation_from_earlier_plan", calculateDeviationFromEarlierPlan(solutions));
        jsonObject.addProperty("total_tardiness", calculateTotalWeightedTardiness(solutions));
        jsonObject.addProperty("objective", obj);
        jsonObject.addProperty("n_jobs", parameters.getSetOfJobs().size());
        jsonObject.addProperty("n_machines", parameters.getSetOfMachines().size());
        jsonObject.addProperty("n_tasks", parameters.getSetOfTasks().size());
        jsonObject.addProperty("increment", parameters.getTimeWindowLength());


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
