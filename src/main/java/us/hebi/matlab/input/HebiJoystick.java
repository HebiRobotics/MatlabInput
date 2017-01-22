package us.hebi.matlab.input;

import net.java.games.input.*;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Component.POV;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.*;

/**
 * Backing class for custom joystick implementation. See original
 * API: https://mathworks.com/help/sl3d/vrjoystick.html
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 20 Jan 2017
 */
public class HebiJoystick {

    public double[][][] read() {

        // Poll events since last poll
        if (!joystick.poll()) {
            throw new MatlabError("Joystick device error: Failed to read joystick status.");
        }

        // Work through events to build current state
        EventQueue queue = joystick.getEventQueue();
        Event event = new Event();
        while (queue.getNextEvent(event)) {

            Component component = event.getComponent();
            final double value = event.getValue();

            if (isPOV(component)) {

                final int i = povIndex.get(component);
                povs[i] = convertPovToDeg(value);

            } else if (isAxis(component)) {

                final int i = axisIndex.get(component);
                axes[i] = component.isRelative() ? axes[i] + value : value;

            } else if (isButton(component)) {

                final int i = buttonIndex.get(component);
                buttons[i] = component.isRelative() ? buttons[i] + value : value;
            }

        }

        // Return state as a cell array that can be directly assigned to varargout
        return matlabCellArray;

    }

    public void force(int[] index, float[] value) {
        if (rumblers.length == 0)
            throw new MatlabError("This device does not support force feedback.");
        if (index == null || value == null)
            throw new MatlabError("Indices and values can't be empty");
        if (value.length != 1 && value.length != index.length)
            throw new MatlabError("Values must be a scalar or a vector of the same length as indices");

        for (int i = 0; i < index.length; i++) {
            final int javaIndex = index[i] - 1;
            if (javaIndex >= rumblers.length)
                throw new MatlabError("Index out of range");
            rumblers[javaIndex].rumble(value.length == 1 ? value[0] : value[i]);
        }
    }

    public void close() {
        JInputUtils.closeNativeResource(joystick);
    }

    private static class CapabilityStruct {
        public int Axes;
        public int Buttons;
        public int POVs;
        public int Forces;
    }

    public CapabilityStruct caps() {
        // Return a class with public fields that can be
        // converted to a MATLAB struct via 'struct()'
        CapabilityStruct struct = new CapabilityStruct();
        struct.Axes = axes.length;
        struct.Buttons = buttons.length;
        struct.POVs = povs.length;
        struct.Forces = rumblers.length;
        return struct;
    }

    public Object getName() {
        // Returning String on 'Object' automatically converts to char array
        return joystick.getName();
    }

    public void setEventQueueSize(int value) {
        joystick.setEventQueueSize(value);
    }

    public HebiJoystick(int matlabId) {

        // Select joystick
        joystick = getJoystick(matlabId);

        // Create lookup tables
        for (Component component : joystick.getComponents()) {
            if (isPOV(component)) {
                povIndex.put(component, povIndex.size());
            } else if (isAxis(component)) {
                axisIndex.put(component, axisIndex.size());
            } else if (isButton(component)) {
                buttonIndex.put(component, buttonIndex.size());
            }
        }
        this.rumblers = joystick.getRumblers();

        // Create initial states zeroes for axes and -1 for povs
        axes = new double[axisIndex.size()];
        buttons = new double[buttonIndex.size()];
        povs = new double[povIndex.size()];
        Arrays.fill(povs, -1);

        // Create a MATLAB readable format. "double[3][1][N]" converts to a cell array of row vectors
        // assuming that the 3rd dimension is jagged, i.e., different amounts of buttons than povs.
        if (axes.length == buttons.length && buttons.length == povs.length) {
            throw new AssertionError("Assuming different numbers of buttons than povs and axes.");
        }
        matlabCellArray = new double[][][]{
                new double[][]{axes},
                new double[][]{buttons},
                new double[][]{povs}};

    }

    private static double convertPovToDeg(double pov) {
        if (pov == POV.OFF) return -1;
        if (pov == POV.UP) return 0;
        if (pov == POV.UP_RIGHT) return 45;
        if (pov == POV.RIGHT) return 90;
        if (pov == POV.DOWN_RIGHT) return 135;
        if (pov == POV.DOWN) return 180;
        if (pov == POV.DOWN_LEFT) return 225;
        if (pov == POV.LEFT) return 270;
        if (pov == POV.UP_LEFT) return 315;
        return -1;
    }

    private static boolean isJoystick(Controller.Type type) {
        return type == Controller.Type.GAMEPAD || type == Controller.Type.STICK;
    }

    private static boolean isPOV(Component component) {
        return Identifier.Axis.POV.equals(component.getIdentifier());
    }

    private static boolean isAxis(Component component) {
        return component.getIdentifier() instanceof Identifier.Axis;
    }

    private static boolean isButton(Component component) {
        return component.getIdentifier() instanceof Identifier.Button;
    }

    private static Controller getJoystick(final int matlabId) {

        // Find controllers asynchronously so that we can recover from getting stuck if
        // something goes wrong in native code.
        Future<Controller> getJoystickFuture = executor.submit(new Callable<Controller>() {
            int id = matlabId;

            @Override
            public Controller call() throws Exception {
                ControllerEnvironment environment = JInputUtils.createDefaultEnvironment();
                for (Controller controller : environment.getControllers()) {
                    if (isJoystick(controller.getType()) && --id == 0) {
                        return controller;
                    }
                }
                return null;
            }
        });

        try {

            Controller controller = getJoystickFuture.get(5, TimeUnit.SECONDS);
            if (controller == null)
                throw new MatlabError("Joystick is not connected.");
            return controller;

        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
        } catch (TimeoutException e) {
        } finally {
            getJoystickFuture.cancel(true);
        }
        throw new MatlabError("Controller search timed out. Limit may have been reached.");

    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("HebiJoystick Controller Lookup");
            return t;
        }
    });

    private final Controller joystick;

    private final HashMap<Component, Integer> buttonIndex = new HashMap<Component, Integer>();
    private final HashMap<Component, Integer> axisIndex = new HashMap<Component, Integer>();
    private final HashMap<Component, Integer> povIndex = new HashMap<Component, Integer>();

    private final double[] axes;
    private final double[] buttons;
    private final double[] povs;
    private final double[][][] matlabCellArray;

    private final Rumbler[] rumblers;

}
