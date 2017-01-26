package us.hebi.matlab.input;

import net.java.games.input.Component.Identifier;
import net.java.games.input.Controller;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Backing class for a keyboard interface for MATLAB
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 24 Jan 2017
 */
public class HebiKeyboard {

    public HebiKeyboard(String driver, int id) {
        if ("AWT".equalsIgnoreCase(driver)) {
            keyboard = JInputUtils.createAWTKeyboard();
        } else if ("native".equalsIgnoreCase(driver)) {
            keyboard = getNativeKeyboard(id);
        } else {
            throw new MatlabError("Unknown driver. Expected 'AWT' or 'native'");
        }
    }

    static class KeyboardState {
        // Letters and numbers
        public final double[][] keys = new double[1][LAST_KEY]; // row vector
        private static final char LAST_KEY = 'z';

        // Meta keys
        public double CTRL;
        public double ALT;
        public double SHIFT;
        public double CAPS_LOCK;
        public double TAB;
        public double SPACE;
        public double ESC;
        public double UP;
        public double LEFT;
        public double RIGHT;
        public double DOWN;

        // Not exposed temp state
        private double CTRL_LEFT;
        private double CTRL_RIGHT;
        private double ALT_LEFT;
        private double ALT_RIGHT;
        private double SHIFT_LEFT;
        private double SHIFT_RIGHT;
    }

    public KeyboardState read() {

        // Poll events since last poll
        if (!keyboard.poll()) {
            throw new MatlabError("Keyboard device error: Failed to read keyboard status.");
        }

        // Work through events to build current state
        EventQueue queue = keyboard.getEventQueue();
        Event event = new Event();
        while (queue.getNextEvent(event)) {

            final Identifier id = event.getComponent().getIdentifier();
            final double value = event.getValue();

            if (!(id instanceof Identifier.Key))
                continue;

            // Letters and numbers
            final char c = id.getName().charAt(0);
            if (id.getName().length() == 1 && c >= '0' && c <= KeyboardState.LAST_KEY) {
                int upper = (int) c - 1;
                state.keys[0][upper] = value;
                // allow for upper and lower case indexing
                if (c >= 'A' && c <= 'Z') {
                    int offset = 'a' - 'A';
                    state.keys[0][upper + offset] = value;
                }

            } else if (id == Identifier.Key.LSHIFT) {
                state.SHIFT_LEFT = value;

            } else if (id == Identifier.Key.RSHIFT) {
                state.SHIFT_RIGHT = value;

            } else if (id == Identifier.Key.LALT) {
                state.ALT_LEFT = value;

            } else if (id == Identifier.Key.RALT) {
                state.ALT_RIGHT = value;

            } else if (id == Identifier.Key.CAPITAL) {
                state.CAPS_LOCK = value;

            } else if (id == Identifier.Key.LCONTROL) {
                state.CTRL_LEFT = value;

            } else if (id == Identifier.Key.RCONTROL) {
                state.CTRL_RIGHT = value;

            } else if (id == Identifier.Key.TAB) {
                state.TAB = value;

            } else if (id == Identifier.Key.SPACE) {
                state.SPACE = value;

            } else if (id == Identifier.Key.ESCAPE) {
                state.ESC = value;

            } else if (id == Identifier.Key.UP) {
                state.UP = value;

            } else if (id == Identifier.Key.DOWN) {
                state.DOWN = value;

            } else if (id == Identifier.Key.LEFT) {
                state.LEFT = value;

            } else if (id == Identifier.Key.RIGHT) {
                state.RIGHT = value;
            }

        }

        // Build combined states
        state.CTRL = Math.max(state.CTRL_LEFT, state.CTRL_RIGHT);
        state.ALT = Math.max(state.ALT_LEFT, state.ALT_RIGHT);
        state.SHIFT = Math.max(state.SHIFT_LEFT, state.SHIFT_RIGHT);

        return state;
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

    private static CloseableController getNativeKeyboard(int matlabId) {
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

    private static final TypeMatcher isKeyboard = new TypeMatcher() {
        @Override
        public boolean matches(Controller.Type type) {
            return type == Controller.Type.KEYBOARD;
        }
    };

}
