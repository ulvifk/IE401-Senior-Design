package data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Parameters {
    private final ArrayList<Job> setOfJobs;
    private final ArrayList<Task> setOfTasks;
    private final ArrayList<Machine> setOfMachines;
    public int finalTimePoint = 250;
    private final double alphaCompletionTime = 1;
    private final double alphaTardiness = 10;
    private final double alphaRobust = 0.1;

    private final int timeWindowLength = 1;

    public Parameters(){
        this.setOfJobs = new ArrayList<>();
        this.setOfTasks = new ArrayList<>();
        this.setOfMachines = new ArrayList<>();
        this.finalTimePoint = getRoundUpToClosestFactor(this.finalTimePoint);
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

            Job job = new Job(id, deadline, priority);
            ArrayList<Task> taskList = new ArrayList<>();

            JsonArray tasksArray = jobObject.getAsJsonArray("tasks");
            for(JsonElement taskElement : tasksArray){
                JsonObject taskObject = taskElement.getAsJsonObject();

                int taskId = taskObject.get("id").getAsInt();
                String type = taskObject.get("type").getAsString();
                double processingTime = taskObject.get("processing_time").getAsDouble();
                int discretizedProcessingTime = getRoundUpToClosestFactor(processingTime);
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

                Task task = new Task(taskId, processingTime, discretizedProcessingTime, predecessorId, successorId, job, priority, oldScheduledTime, oldScheduledMachine);

                List<Machine> machinesList = this.setOfMachines.stream().filter(m -> m.getType().equals(type)).toList();
                if(machinesCanUndertake.size() == 0){
                    throw new Exception("A task is not assigned to any machine");
                }

                machinesList.forEach(m -> {
                    m.addTask(task);
                    task.addMachineCanUnderTake(m);
                });

                taskList.add(task);
                this.setOfTasks.add(task);
                job.addTask(task);
            }
            this.setOfJobs.add(job);
        }

        findPrecedenceRelationTasks();
    }

    private void findPrecedenceRelationTasks(){
        for(Task i : this.setOfTasks){
            i.setPrecedingTask(this.setOfTasks.stream().filter(task -> task.getId() == i.getPrecedingTaskId()).findAny().orElse(null));
            i.setSucceedingTask(this.setOfTasks.stream().filter(task -> task.getId() == i.getSucceedingTaskId()).findAny().orElse(null));
        }
    }

    public List<Integer> getSetOfTardyTimes(Task i){
        List<Integer> setOfTardyPoints = new ArrayList<>();
        for(int t = (i.getJobWhichBelongs().getDeadline() - i.getDiscretizedProcessingTime()) > 0 ? (i.getJobWhichBelongs().getDeadline() - i.getDiscretizedProcessingTime() + 1) : 0;
            t <= this.finalTimePoint; t++){
            setOfTardyPoints.add(t);
        }
        return setOfTardyPoints;
    }

    public int getRoundUpToClosestFactor(double val){
        return (int) (Math.ceil(val / (double) this.timeWindowLength));
    }

    public int getRoundDownToClosestFactor(double val){
        return (int) (Math.floor(val / (double) this.timeWindowLength) );
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
}
