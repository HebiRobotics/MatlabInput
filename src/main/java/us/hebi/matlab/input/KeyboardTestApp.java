package us.hebi.matlab.input;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

import java.util.concurrent.TimeUnit;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 24 Jan 2017
 */
public class KeyboardTestApp {

    public static void main(String[] args) throws Exception {

        Controller keyboard = getKeyboard();
        for (Component component : keyboard.getComponents()) {
            System.out.println(component + " = " + component.getPollData());
        }

        while (true) {

            keyboard.poll();
            EventQueue queue = keyboard.getEventQueue();
            Event event = new Event();
            while (queue.getNextEvent(event)) {

                System.out.println(event + " : " + event.getValue());
                TimeUnit.MILLISECONDS.sleep(100);
            }

        }

    }

    static Controller getKeyboard() {
        for (Controller controller : JInputUtils.createDefaultEnvironment().getControllers()) {
            System.out.println(controller + " = " + controller.getType());
            if (controller.getType() == Controller.Type.KEYBOARD) {
                return controller;
            }
        }
        return null;
    }


}
