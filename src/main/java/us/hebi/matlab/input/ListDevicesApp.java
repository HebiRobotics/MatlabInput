package us.hebi.matlab.input;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 26 Jan 2017
 */
public class ListDevicesApp {

    public static void main(String[] args) {

        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

        for (Controller controller : controllers) {
            System.out.println(controller);
            for (Component component : controller.getComponents()) {
                System.out.println("    " + component);
            }
        }

        System.out.println("-------- Controllers --------");
        for (Controller controller : controllers) {
            System.out.println(controller + " ==> " + controller.getType());
        }

    }

}
