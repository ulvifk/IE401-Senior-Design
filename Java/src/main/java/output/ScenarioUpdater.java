package output;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import data.Job;
import data.Parameters;
import data.Task;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBVar;
import model.Variables;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Map;

public class ScenarioUpdater {
    public static void updateScenario(String filePath, String outputPath,Parameters parameters, Variables variables) throws FileNotFoundException, GRBException {
        FileReader reader = new FileReader(filePath);
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
                int newSchedule = 0;
                for(Map.Entry<Integer, GRBVar> pair : variables.getZ().get(task).entrySet()){
                    int t = pair.getKey();
                    GRBVar z = pair.getValue();
                    if(z.get(GRB.DoubleAttr.X) > 0.5){
                        newSchedule = t;
                        break;
                    }
                }

                taskObject.remove("schedule");
                taskObject.addProperty("schedule", newSchedule);
            }
        }

        String scenario = jsonObject.toString();
        PrintWriter out = new PrintWriter(outputPath);
        out.println(scenario);
        out.close();
    }
}
