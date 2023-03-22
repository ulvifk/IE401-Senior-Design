package heuristic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import data.Job;
import data.Machine;
import data.Parameters;
import data.Task;
import gurobi.GRBException;
import output.ScenarioUpdater;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.*;

public class Heuristic {
    private final Parameters parameters;
    private final List<Task> unscheduledTasks;
    private final List<Machine> M;
    private final List<Task> H;
    private final List<Task> C;
    private final List<Task> D;
    private List<Machine> W;
    private final Map<Machine, Double> I;
    private final Map<Task, Solution> solutions;

    public Heuristic(Parameters parameters) {
        this.parameters = parameters;
        this.unscheduledTasks = new LinkedList<>(parameters.getSetOfTasks());
        this.H = new LinkedList<>();
        this.C = new LinkedList<>();
        this.D = new LinkedList<>();
        this.W = new LinkedList<>(parameters.getSetOfMachines());
        this.M = new LinkedList<>(parameters.getSetOfMachines());

        this.I = new HashMap<>();
        for (Machine machine : parameters.getSetOfMachines()) {
            I.put(machine, 0.0);
        }

        this.solutions = new HashMap<>();
    }

    private void updateD(){
        D.clear();
        for (Task task : unscheduledTasks) {
            if (task.getPrecedingTask() == null || this.C.contains(task.getPrecedingTask())) {
                if (task.getMachinesCanUndertake().stream().anyMatch(this.W::contains)) {
                    D.add(task);
                }
            }
        }
    }

    private List<Machine> updateW(double t, List<Machine> machineList){
        List<Machine> newMachineList = new LinkedList<>();
        for (Machine machine : machineList){
            if (I.get(machine) == t){
                newMachineList.add(machine);
            }
        }
        return newMachineList;
    }

    private double findT(List<Machine> machines){
        double t = Double.MAX_VALUE;
        for (Machine machine : machines) {
            double idleTime = this.I.get(machine);
            if (idleTime < t) {
                t = idleTime;
            }
        }

        return t;
    }

    private void updateC(double t){
        this.C.clear();
        for (Task task : this.H){
            double finishTime = this.solutions.get(task).startTime + task.getProcessingTime();
            if (finishTime <= t){
                this.C.add(task);
            }
        }
    }

    private void calculateScore(Task task, Machine machine, int time){

    }

    private Pair<Task, Machine> findBestTaskAndMachine(){
        double minProcessingTime = Double.MAX_VALUE;
        Task bestTask = null;
        Machine bestMachine = null;

        for (Task task : D) {
            for (Machine machine : task.getMachinesCanUndertake()){
                if (this.W.contains(machine)){
                    double processingTime = task.getProcessingTime() * machine.getProcessingTimeConstant();
                    if (task.getProcessingTime() * machine.getProcessingTimeConstant() < minProcessingTime){
                        minProcessingTime = processingTime;
                        bestTask = task;
                        bestMachine = machine;
                    }
                }
            }
        }

        return new Pair<>(bestTask, bestMachine);
    }

    private void removeRedundantMachines(){
        ListIterator<Machine> iterator = this.M.listIterator();
        whileLoop:
        while (iterator.hasNext()){
            Machine machine = iterator.next();
            for (Task task : machine.getSetOfAssignedTasks()){
                if (this.unscheduledTasks.contains(task)){
                    continue whileLoop;
                }
            }
            System.out.println("Removing machine: " + machine.getId());

            iterator.remove();
            this.I.remove(machine);
            this.W.remove(machine);
        }
    }

    public void optimize(){
        System.out.println("Heuristic started...");

        double t = 0;
        this.updateD();

        while (this.unscheduledTasks.size() > 0){

            Machine machine;
            Task task;
            if (this.W.size() == 1){
                machine = this.W.get(0);
                task = this.D.stream().filter(
                        ts -> ts.getMachinesCanUndertake().contains(machine)).
                        min(Comparator.comparing(Task::getProcessingTime)).orElse(null);

                if (task == null){
                    int x = 0;
                }
            }
            else{
                Pair<Task, Machine> bestTaskAndMachine = this.findBestTaskAndMachine();
                task = bestTaskAndMachine.first;
                machine = bestTaskAndMachine.second;
            }

            this.unscheduledTasks.remove(task);
            this.H.add(task);
            this.I.replace(machine, t + task.getProcessingTime() * machine.getProcessingTimeConstant());
            this.removeRedundantMachines();

            Solution solution = new Solution(task, machine, t);
            System.out.println(String.format("Task %d scheduled on machine %d to time %f",
                    task.getId(), machine.getId(), t));
            this.solutions.put(task, solution);
            if (this.unscheduledTasks.size() == 0){
                break;
            }

            t = this.findT(this.W);

            if (t == -1){
                throw new RuntimeException("t is -1");
            }

            this.W = this.updateW(t, this.M);
            this.updateC(t);
            this.updateD();
            while (this.D.size() == 0){
                List<Machine> mPrime = this.M.stream().filter(mac -> !this.W.contains(mac)).toList();

                if (mPrime.size() == 0){
                    t++;
                }
                else{
                    t = this.findT(mPrime);
                }

                List<Machine> wPrime = this.updateW(t, mPrime);
                for (Machine mac : wPrime){
                    if (!this.W.contains(mac)){
                        this.W.add(mac);
                    }
                }
                this.updateC(t);
                this.updateD();
            }
        }
    }

    public void writeSolution(String inputPath, String outputPath) throws FileNotFoundException, GRBException {
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

                Task task = job.getTasks().stream().filter(task1 -> task1.getId() == taskId).findAny().orElse(null);
                Solution solution = this.solutions.get(task);

                taskObject.remove("scheduled_start_time");
                taskObject.addProperty("scheduled_start_time", solution.startTime);

                taskObject.remove("scheduled_end_time");
                taskObject.addProperty("scheduled_end_time", solution.startTime + task.getProcessingTime() * solution.machine.getProcessingTimeConstant());

                taskObject.remove("scheduled_machine");
                taskObject.addProperty("scheduled_machine", solution.machine.getId());
            }
        }

        String scenario = jsonObject.toString();
        PrintWriter out = new PrintWriter(outputPath);
        out.println(scenario);
        out.close();
    }

    public void writeStats(String path) throws FileNotFoundException {
        ScenarioUpdater.writeStats(path, this.parameters, this.solutions);
    }
}
