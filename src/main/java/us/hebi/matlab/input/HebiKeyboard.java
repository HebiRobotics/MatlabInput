package us.hebi.matlab.input;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 24 Jan 2017
 */
public class HebiKeyboard {

    public HebiKeyboard(int matlabId) {
        keyboard = getKeyboard(matlabId);
    }

    public double[][] read() {

        // Poll events since last poll
        if (!keyboard.poll()) {
            throw new MatlabError("Joystick device error: Failed to read joystick status.");
        }

        // Work through events to build current state
        EventQueue queue = keyboard.getEventQueue();
        Event event = new Event();
        while (queue.getNextEvent(event)) {

            Component component = event.getComponent();

            if (component.getIdentifier() instanceof Component.Identifier.Key && component.getName().length() == 1) {

                char c = component.getName().charAt(0);
                if (c >= '0' && c <= 'Z') {
                    state.keys[((int) c) - 1] = event.getValue();
                }

            }

        }

        return new double[][]{ // cell array
                state.keys,
                null};
    }

    public Object getName() {
        return keyboard.getName();
    }

    public void setEventQueueSize(int value) {
        keyboard.setEventQueueSize(value);
    }

    public void close() {
        keyboard.close();
    }


    private static CloseableController getKeyboard(int matlabId) {
        CloseableController joystick = null;
        try {
            joystick = JInputUtils.getControllerOrTimeout(matlabId, 5, TimeUnit.SECONDS, isKeyboard);
            if (joystick != null)
                return joystick;
        } catch (TimeoutException e) {
            throw new MatlabError("Controller search timed out.");
        } catch (Exception e) {
            throw new MatlabError("Could not get keyboard. Message: " + e.getMessage());
        }
        throw new MatlabError("Keyboard is not connected.");
    }

    private final CloseableController keyboard;
    private final KeyboardState state = new KeyboardState();

    static class KeyboardState {
        public final double[] keys = new double[LAST_SUPPORTED_KEY];
        private static final char LAST_SUPPORTED_KEY = 'Z';
    }

    private static final TypeMatcher isKeyboard = new TypeMatcher() {
        @Override
        public boolean matches(Controller.Type type) {
            return type == Controller.Type.KEYBOARD;
        }
    };

}
