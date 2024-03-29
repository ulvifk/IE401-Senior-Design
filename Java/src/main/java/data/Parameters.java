package data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.crypto.Mac;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Parameters {
    private ArrayList<Job> setOfJobs;
    private ArrayList<Task> setOfTasks;
    private ArrayList<Machine> setOfMachines;
    public int finalTimePoint = 250;
    private double alphaCompletionTime = 1;
    private double alphaTardiness = 10;
    private double alphaRobust = 0.1;

    private int timeWindowLength = 1;

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
            Machine machine = new Machine();
            machine.setId(machineObject.get("id").getAsInt());

            this.setOfMachines.add(machine);
        }

        for (JsonElement jobElement : jobsArray) {
            JsonObject jobObject = jobElement.getAsJsonObject();
            Job job = new Job();

            job.setId(jobObject.get("id").getAsInt());
            job.setDeadline(getRoundDownToClosestFactor(jobObject.get("deadline").getAsInt()));
            double priority;
            switch (jobObject.get("priority").getAsString()){
                case "LOW":
                    priority = 1;
                    break;
                case "MEDIUM":
                    priority = 4;
                    break;
                case "HIGH":
                    priority = 16;
                    break;
                default:
                    throw new Exception("Invalid priority definition");
            }

            job.setPriority(priority);
            ArrayList<Task> taskList = new ArrayList<>();

            JsonArray tasksArray = jobObject.getAsJsonArray("tasks");
            for(JsonElement taskElement : tasksArray){
                JsonObject taskObject = taskElement.getAsJsonObject();

                Task task = new Task();
                task.setId(taskObject.get("id").getAsInt());
                task.setPriority(priority);
                task.setProcessingTime(taskObject.get("processing_time").getAsInt());
                task.setDiscretizedProcessingTime(getRoundUpToClosestFactor(task.getProcessingTime()));
                task.setJobWhichBelongs(job);
                task.setPrecedingTaskId(taskObject.get("preceding_task").getAsInt());
                task.setSucceedingTaskId(taskObject.get("succeeding_task").getAsInt());
                task.setOldScheduleTime(taskObject.get("schedule").getAsInt());

                int machineId = taskObject.get("assigned_machine").getAsInt();
                Machine machine = this.setOfMachines.stream().filter(m -> m.getId() == machineId).findAny().orElse(null);
                if(machine == null){
                    throw new Exception("A task is not assigned to any machine");
                }

                machine.getSetOfAssignedTasks().add(task);
                task.setAssignedMachine(machine);


                taskList.add(task);
                this.setOfTasks.add(task);
            }

            job.setTasks(taskList);
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
