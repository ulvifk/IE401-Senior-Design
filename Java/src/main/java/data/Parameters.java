package data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import data.enums.Priority;
import heuristic.Pair;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parameters {
    private final ArrayList<Job> setOfJobs;
    private final ArrayList<Task> setOfTasks;
    private final ArrayList<Machine> setOfMachines;
    private final Map<Task, List<Integer>> setOfTimePoints;
    private final List<Integer> allTimePoints;
    public int finalTimePoint = 250;
    private final double alphaCompletionTime = 1;
    private final double alphaTardiness = 10;
    private final double alphaRobust = 0.1;

    private final int timeWindowLength;

    public Parameters(int timeWindowLength) {
        this.setOfJobs = new ArrayList<>();
        this.setOfTasks = new ArrayList<>();
        this.setOfMachines = new ArrayList<>();
        this.setOfTimePoints = new HashMap<>();
        this.allTimePoints = new ArrayList<>();
        this.timeWindowLength = timeWindowLength;
        this.finalTimePoint = this.finalTimePoint;
    }
    public void readData(String jsonPath) throws Exception {
        FileReader reader = new FileReader(jsonPath);
        JsonElement jsonElement = JsonParser.parseReader(reader);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        JsonArray machinesArray = jsonObject.getAsJsonArray("machines");
        JsonArray jobsArray = jsonObject.getAsJsonArray("jobs");

        for (JsonElement machineElement : machinesArray) {
            JsonObject machineObject = machineElement.getAsJsonObject();

            int id = machineObject.get("id").getAsInt();
            double processingTime = machineObject.get("processing_time_constant").getAsDouble();
            String type = machineObject.get("task_type_undertakes").getAsString();
            Machine machine = new Machine(id, processingTime, type);

            this.setOfMachines.add(machine);
        }

        for (JsonElement jobElement : jobsArray) {
            JsonObject jobObject = jobElement.getAsJsonObject();

            int id = jobObject.get("id").getAsInt();
            int deadline = jobObject.get("deadline").getAsInt();

            double priority = switch (jobObject.get("priority").getAsString()) {
                case "LOW" -> 1;
                case "MEDIUM" -> 4;
                case "HIGH" -> 16;
                default -> throw new Exception("Invalid priority definition");
            };

            Priority priorityEnum = switch (jobObject.get("priority").getAsString()) {
                case "LOW" -> Priority.LOW;
                case "MEDIUM" -> Priority.MEDIUM;
                case "HIGH" -> Priority.HIGH;
                default -> throw new Exception("Invalid priority definition");
            };

            Job job = new Job(id, deadline, priority, priorityEnum);
            ArrayList<Task> taskList = new ArrayList<>();

            JsonArray tasksArray = jobObject.getAsJsonArray("tasks");
            for(JsonElement taskElement : tasksArray){
                JsonObject taskObject = taskElement.getAsJsonObject();

                int taskId = taskObject.get("id").getAsInt();
                String type = taskObject.get("type").getAsString();
                double processingTime = taskObject.get("processing_time").getAsDouble();
                JsonArray machinesCanUnderTakeJsonArr = taskObject.get("machines_can_undertake").getAsJsonArray();
                ArrayList<Integer> machinesCanUndertake = new ArrayList<>();
                for(JsonElement machineId : machinesCanUnderTakeJsonArr){
                    machinesCanUndertake.add(machineId.getAsInt());
                }
                int predecessorId = taskObject.get("preceding_task").getAsInt();
                int successorId = taskObject.get("succeeding_task").getAsInt();
                int oldScheduledTime = taskObject.get("scheduled_start_time").getAsInt();
                int oldScheduledMachineId = taskObject.get("scheduled_machine").getAsInt();
                Machine oldScheduledMachine = this.setOfMachines.stream().filter(m -> m.getId() == oldScheduledMachineId).findFirst().orElse(null);
                if (oldScheduledMachine == null && oldScheduledMachineId != -1){
                    throw new Exception("Invalid old scheduled machine id");
                }

                Task task = new Task(taskId, processingTime, predecessorId, successorId, job, priority, oldScheduledTime, oldScheduledMachine, priorityEnum);

                List<Machine> machinesList = this.setOfMachines.stream().filter(m -> m.getType().equals(type)).toList();
                if (machinesCanUndertake.size() == 0) {
                    throw new Exception("A task is not assigned to any machine");
                }

                machinesList.forEach(m -> {
                    m.addTask(task);
                    task.addMachineCanUnderTake(m);
                });

                for (Machine machine : task.getMachinesCanUndertake()) {
                    task.addProcessingTime(machine);
                    task.addDiscretizedProcessingTime(machine, this.getRoundToClosestFactor(task.getProcessingTime(machine)));
                }

                taskList.add(task);
                this.setOfTasks.add(task);
                job.addTask(task);
            }
            this.setOfJobs.add(job);
        }

        findPrecedenceRelationTasks();

        for (Task task : this.setOfTasks){
            double avgProcessingTime = task.getProcessingTimes().values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double avgDiscretizedProcessingTime = task.getDiscretizedProcessingTimes().values().stream().mapToDouble(Integer::doubleValue).average().orElse(0);
            task.setAverageProcessingTime(avgProcessingTime);
            task.setAverageDiscreteProcessingTime(avgDiscretizedProcessingTime);
        }

        double totalProcessingTime = 0;
        for (Task task : this.setOfTasks){
            totalProcessingTime += this.getRoundUpToClosestFactor(task.getAverageProcessingTime());
        }
        this.finalTimePoint = (int) (totalProcessingTime / this.setOfMachines.size() * 2);

        for (Task task : this.setOfTasks){
            List<Integer> timePoints = new ArrayList<>();

            Pair<Integer, Integer> lowerAndUpperTimeLimits = getLowerAndUpperTimeLimits(task);
            int startPoint = lowerAndUpperTimeLimits.first;
            int endPoint = lowerAndUpperTimeLimits.second;
            int increment = this.timeWindowLength;

            if (task.getPriorityEnum() == Priority.MEDIUM){
                increment *= 2;
            }
            else if (task.getPriorityEnum() == Priority.LOW){
                increment *= 4;
            }

            for (int t = startPoint; t <= endPoint; t+=increment) {
                timePoints.add(t);
            }

            this.setOfTimePoints.put(task, timePoints);
        }

        for (int t = 0; t<= this.finalTimePoint; t+=this.timeWindowLength){
            this.allTimePoints.add(t);
        }
    }

    private int redZoneStartTime(Task task){
        double requiredTime = task.getAverageProcessingTime();
        Task currentTask = task;
        while (currentTask.getSucceedingTask() != null){
            currentTask = currentTask.getSucceedingTask();
            requiredTime += currentTask.getAverageProcessingTime();
        }

        double safetyCoefficient = 3;
        requiredTime *= safetyCoefficient;

        return (int) (task.getJobWhichBelongs().getDeadline() - requiredTime);
    }

    private int earliestPossibleStartTime(Task task){
        double earliestPossibleStartTime = 0;

        Task currentTask = task;
        if (currentTask.getPrecedingTask() != null){
            currentTask = currentTask.getPrecedingTask();
            earliestPossibleStartTime += currentTask.getAverageProcessingTime();
        }
        return (int) earliestPossibleStartTime;
    }

    private Pair<Integer, Integer> getLowerAndUpperTimeLimits(Task task){
        int lowerTimeLimit = earliestPossibleStartTime(task);
        lowerTimeLimit = Math.max(lowerTimeLimit, redZoneStartTime(task));

        int upperTimeLimit = this.finalTimePoint;

        return new Pair<>(lowerTimeLimit, upperTimeLimit);
    }

    private void findPrecedenceRelationTasks(){
        for(Task i : this.setOfTasks){
            i.setPrecedingTask(this.setOfTasks.stream().filter(task -> task.getId() == i.getPrecedingTaskId()).findAny().orElse(null));
            i.setSucceedingTask(this.setOfTasks.stream().filter(task -> task.getId() == i.getSucceedingTaskId()).findAny().orElse(null));
        }
    }

    public List<Integer> getSetOfTardyTimes(Task i, Machine k) {
        List<Integer> setOfTardyPoints = new ArrayList<>();

        int lowerBound = (i.getJobWhichBelongs().getDeadline() - i.getProcessingTime(k)) > 0 ? (i.getJobWhichBelongs().getDeadline() - i.getDiscretizedProcessingTime(k) + 1) : 0;

        for (int t : this.setOfTimePoints.get(i)){
            if (t >= lowerBound){
                setOfTardyPoints.add(t);
            }
        }
        return setOfTardyPoints;
    }

    public int getRoundUpToClosestFactor(double val){
        return (int) (Math.ceil(val / this.timeWindowLength) * this.timeWindowLength);
    }

    public int getRoundToClosestFactor(double val){
        return (int) (Math.round(val / this.timeWindowLength) * this.timeWindowLength);
    }

    public int getRoundDownToClosestFactor(double val){
        return (int) (Math.floor(val / timeWindowLength) * timeWindowLength);
    }

    public ArrayList<Job> getSetOfJobs() {
        return setOfJobs;
    }

    public ArrayList<Task> getSetOfTasks() {
        return setOfTasks;
    }

    public ArrayList<Machine> getSetOfMachines() {
        return setOfMachines;
    }

    public int getFinalTimePoint() {
        return finalTimePoint;
    }

    public double getAlphaCompletionTime() {
        return alphaCompletionTime;
    }

    public double getAlphaRobust() {
        return alphaRobust;
    }

    public double getAlphaTardiness() {
        return alphaTardiness;
    }

    public int getTimeWindowLength() {
        return timeWindowLength;
    }

    public List<Integer> getSetOfTimePoints(Task task) {
        return setOfTimePoints.get(task);
    }

    public List<Integer> getAllTimePoints() {
        return allTimePoints;
    }
}
