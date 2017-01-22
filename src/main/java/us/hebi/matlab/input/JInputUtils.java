package us.hebi.matlab.input;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Various utilities to work around JInput limitations. The MATLAB library is using JInput in a
 * very atypical way, i.e., joysticks can be disconnected at any time, users may instantiate the
 * same joystick multiple times, etc.
 * <p>
 * Doing this requires low-level access for cleaning up native resources that is not supported
 * by vanilla JInput. We can work around this by accessing private fields using reflection, but
 * this makes the code brittle and difficult to test.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 20 Jan 2017
 */
public class JInputUtils {

    static {
        // Fix Windows >7 warnings by defining a working plugin
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                String os = System.getProperty("os.name", "").trim();
                if (os.startsWith("Windows")) {  // 7, 8, 8.1, 10, etc.

                    // disable default plugin lookup
                    System.setProperty("jinput.useDefaultPlugin", "false");

                    // set to same as windows 7 (tested for windows 8, 8.1, and 10)
                    System.setProperty("net.java.games.input.plugins", "net.java.games.input.DirectAndRawInputEnvironmentPlugin");
                    // net.java.games.input.DirectInputEnvironmentPlugin => for keyboard/mouse events without selecting a window

                }
                return null;
            }
        });
    }

    static {
        // disable console logger to clean up error displays in MATLAB
        LogManager.getLogManager().reset(); // global reset (somehow disabling doesn't work without)
        disableLogger(ControllerEnvironment.class.getName());
        disableLogger("net.java.games.input.DefaultControllerEnvironment");
    }

    private static void disableLogger(String name) {
        Logger logger = Logger.getLogger(name);
        logger.setLevel(Level.OFF); // disable logging
        logger.setUseParentHandlers(false); // remove propagation to root handler
        Handler[] handlers = logger.getHandlers(); // Remove directly registered handlers
        for (Handler handler : handlers) {
            logger.removeHandler(handler);
        }
    }

    /**
     * Returns a new instance of the default environment for input controllers.
     * This usually corresponds to the environment for the local machine.
     * <p>
     * Controller lookup typically happens only once at the first creation, so
     * the process (e.g. MATLAB) would have to be restarted whenever a controller
     * gets connected.
     * <p>
     * By reflectively creating a new environment we can force a new lookup. Note
     * that this does not harm existing controllers that were created with a different
     * environment.
     *
     * @return default environment for input controllers
     */
    private static ControllerEnvironment createDefaultEnvironment() {

        try {
            // Find constructor (class is package private, so we can't access it directly)
            @SuppressWarnings("unchecked")
            Constructor<ControllerEnvironment> constructor = (Constructor<ControllerEnvironment>)
                    Class.forName("net.java.games.input.DefaultControllerEnvironment").getDeclaredConstructors()[0];

            // Constructor is package private, so we have to deactivate access control checks
            constructor.setAccessible(true);

            // Create object with default constructor
            return constructor.newInstance();

        } catch (Exception e) {
            throw new AssertionError("Could not create controller environment. Message: " + e.getMessage());
        }

    }

    /**
     * Unfortunately controllers don't have a public release method. To work around
     * this limitation we need to reflectively close them. This is brittle and hard
     * to test, but there doesn't seem to be another way. Also, some controllers
     * don't have a way to close native resources at all.
     */
    static void closeNativeDevice(Controller controller) {

        try {

            if (isAssignableFrom("DIAbstractController", controller)) {

                getDeviceAndRelease(controller, "device", "release"); // DirectInput

            } else if (isAssignableFrom("OSXAbstractController", controller)) {

                getDeviceAndRelease(controller, "queue", "release"); // OSX

            } else if (isAssignableFrom("LinuxAbstractController", controller)
                    || isAssignableFrom("LinuxJoystickAbstractController", controller)) {

                getDeviceAndRelease(controller, "device", "close"); // Linux

            } else if (isAssignableFrom("LinuxCombinedController", controller)) {

                closeNativeDevice((Controller) controller.getClass().getDeclaredField("eventController").get(controller));
                closeNativeDevice((Controller) controller.getClass().getDeclaredField("joystickController").get(controller));

            } else {
                System.err.println("Close not implemented for: " + controller.getClass().getSimpleName());
            }

        } catch (Exception e) {
            System.err.println("Failed to close device. Message: " + e.getMessage());
        }

    }

    private static boolean isAssignableFrom(String simpleClassName, Object object) throws ClassNotFoundException {
        return Class.forName("net.java.games.input." + simpleClassName).isAssignableFrom(object.getClass());
    }

    private static void getDeviceAndRelease(Controller controller, String deviceFieldName, String releaseMethodName) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Field deviceField = controller.getClass().getDeclaredField(deviceFieldName);
        deviceField.setAccessible(true);
        Object nativeDevice = deviceField.get(controller);
        Method releaseMethod = nativeDevice.getClass().getMethod(releaseMethodName);
        releaseMethod.setAccessible(true);
        releaseMethod.invoke(nativeDevice);
    }

    /**
     * Some environments add shutdown hooks that would cause a memory leak. We
     * also find controllers asynchronously so that we can recover from getting stuck
     * if something goes wrong in native code.
     */
    public static ControllerWithHooks getJoystickOrTimeout(final int matlabId, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

        Future<ControllerWithHooks> getJoystickFuture = lookupExecutor.submit(new Callable<ControllerWithHooks>() {
            int id = matlabId; // 1 indexed

            @Override
            public ControllerWithHooks call() throws Exception {

                Controller[] controllers;
                List<Thread> addedHooks;

                // Create environment and remove any shutdown hooks that were added
                Class clazz = Class.forName("java.lang.ApplicationShutdownHooks");
                Field hookField = clazz.getDeclaredField("hooks");
                hookField.setAccessible(true);
                @SuppressWarnings("unchecked")
                IdentityHashMap<Thread, Thread> hooks = (IdentityHashMap<Thread, Thread>) hookField.get(null);
                synchronized (clazz) { // static add/remove methods synchronize on class object

                    // Create new environment
                    List<Thread> previousHooks = new ArrayList<Thread>(hooks.keySet());
                    controllers = JInputUtils.createDefaultEnvironment().getControllers();

                    // Get all newly added hooks
                    addedHooks = new ArrayList<Thread>(hooks.keySet());
                    addedHooks.removeAll(previousHooks);

                    // Remove new hooks from app shutdown
                    for (Thread hook : addedHooks) {
                        hooks.remove(hook);
                        hook.setDaemon(true);
                    }

                }

                // Find controller
                for (Controller controller : controllers) {
                    if (isJoystick(controller.getType()) && --id == 0) {
                        return new ControllerWithHooks(controller, addedHooks);
                    }
                }
                return new ControllerWithHooks(null, addedHooks);

            }
        });

        try {
            return getJoystickFuture.get(timeout, unit);
        } finally {
            getJoystickFuture.cancel(true);
        }
    }

    private static boolean isJoystick(Controller.Type type) {
        return type == Controller.Type.GAMEPAD || type == Controller.Type.STICK;
    }

    private static final ExecutorService lookupExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("HebiJoystick Controller Lookup");
            return t;
        }
    });

    static class ControllerWithHooks implements Closeable {
        ControllerWithHooks(Controller controller, Collection<Thread> shutdownHooks) {
            this.controller = controller;
            this.shutdownHooks = shutdownHooks;
        }

        public Controller getController() {
            return controller;
        }

        private final Controller controller;
        private final Collection<Thread> shutdownHooks;
        private boolean isClosed = false;

        @Override
        public synchronized void close() {
            if (isClosed) return;
            isClosed = true;

            // Run environment shutdown hooks
            for (Thread hook : shutdownHooks) {
                hook.start();
            }
            for (Thread hook : shutdownHooks) {
                try {
                    hook.join(1000);
                } catch (InterruptedException e) {
                    System.err.println("Environment shutdown timed out");
                }
            }

            // Release native device
            if (controller != null) {
                closeNativeDevice(controller);
            }
        }
    }

}
