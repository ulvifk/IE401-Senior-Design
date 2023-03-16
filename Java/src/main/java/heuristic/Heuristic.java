package heuristic;

import data.Machine;
import data.Parameters;
import data.Task;

import java.util.*;

public class Heuristic {
    private final Parameters parameters;
    private final List<Task> unscheduledTasks;
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

        this.I = new HashMap<>();
        for (Machine machine : parameters.getSetOfMachines()) {
            I.put(machine, 0.0);
        }

        this.solutions = new HashMap<>();
    }

    private void updateD(){
        D.clear();
        for (Task task : unscheduledTasks) {
            if (this.H.contains(task.getPrecedingTask()) || task.getPrecedingTask() == null) {
                D.add(task);
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

    public void optimize(){
        double t = 0;

        while (this.unscheduledTasks.size() > 0){
            this.updateD();

            Machine machine;
            Task task;
            if (this.W.size() == 1){
                machine = this.W.get(0);
                task = this.D.stream().filter(
                        ts -> ts.getMachinesCanUndertake().contains(machine)).
                        min(Comparator.comparing(Task::getProcessingTime)).orElse(null);
            }
            else{
                Pair<Task, Machine> bestTaskAndMachine = this.findBestTaskAndMachine();
                task = bestTaskAndMachine.first;
                machine = bestTaskAndMachine.second;
            }
            t = this.I.get(machine);
            Solution solution = new Solution(task, machine, t);
            this.solutions.put(task, solution);
            this.H.add(task);
            t = this.findT(this.W);
            if (t == -1){
                throw new RuntimeException("t is -1");
            }

            this.W = this.updateW(t, this.parameters.getSetOfMachines());
            this.updateC(t);
            this.updateD();
            while (this.D.size() == 0){
                List<Machine> mPrime = this.parameters.getSetOfMachines().stream().filter(mac -> !this.W.contains(mac)).toList();
                t = this.findT(mPrime);
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
}
