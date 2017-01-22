package us.hebi.matlab.input;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Various utilities to work around JInput
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
    public static ControllerEnvironment createDefaultEnvironment() {

        try {
            // Find constructor (class is package private, so we can't access it directly)
            @SuppressWarnings("unchecked")
            Constructor<ControllerEnvironment> constructor = (Constructor<ControllerEnvironment>)
                    Class.forName("net.java.games.input.DefaultControllerEnvironment").getDeclaredConstructors()[0];

            // Constructor is package private, so we have to deactivate access control checks
            constructor.setAccessible(true);

            // Create object with default constructor
            return constructor.newInstance();

        } catch (ClassNotFoundException e) {
        } catch (IllegalAccessException e) {
        } catch (InstantiationException e) {
        } catch (InvocationTargetException e) {
        }
        throw new AssertionError("Could not create controller environment");

    }

    /**
     * Unfortunately controllers don't have a public release method. To work around
     * this limitation we need to reflectively close them. This is brittle and hard
     * to test, but there doesn't seem to be another way. Also, some controllers
     * don't have a way to close native resources at all.
     */
    static void closeNativeResource(Controller controller) {

        try {

            if (isAssignableFrom("DIAbstractController", controller)) {

                getDeviceAndRelease(controller, "device", "release"); // DirectInput

            } else if (isAssignableFrom("OSXAbstractController", controller)) {

                getDeviceAndRelease(controller, "queue", "release"); // OSX

            } else if (isAssignableFrom("LinuxAbstractController", controller)
                    || isAssignableFrom("LinuxJoystickAbstractController", controller)) {

                getDeviceAndRelease(controller, "device", "close"); // Linux

            } else if (isAssignableFrom("LinuxCombinedController", controller)) {

                closeNativeResource((Controller) controller.getClass().getDeclaredField("eventController").get(controller));
                closeNativeResource((Controller) controller.getClass().getDeclaredField("joystickController").get(controller));

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

}
