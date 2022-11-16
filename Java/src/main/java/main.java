import data.Parameters;
import model.Model;

public class main {
    public static void main(String[] args) throws Exception {
        Parameters parameters = new Parameters();
        parameters.readData("scenario.json");

        Model model = new Model(parameters);
    }
}
