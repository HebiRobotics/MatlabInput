package us.hebi.matlab.input;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.EventQueue;
import net.java.games.input.Rumbler;

import java.io.Closeable;
import java.util.Collection;

/**
 * Controller facade that adds the ability to close native resources.
 * Assumes that a new environment is created for each controller.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 22 Jan 2017
 */
public class CloseableController implements Controller, Closeable {

    CloseableController(Controller controller, Collection<Thread> shutdownHooks) {
        if (controller == null || shutdownHooks == null)
            throw new IllegalArgumentException("can't be null");
        this.controller = controller;
        this.shutdownHooks = shutdownHooks;
    }

    private final Controller controller;
    private final Collection<Thread> shutdownHooks;

    private boolean isClosed = false;

    @Override
    public synchronized void close() {
        if (isClosed)
            return;
        isClosed = true;
        JInputUtils.runShutdownHooks(shutdownHooks);
        JInputUtils.closeNativeDevice(controller);
    }

    @Override
    public synchronized boolean poll() {
        if (isClosed)
            throw new MatlabError("Joystick device error: Closed or invalid joystick device");
        return controller.poll();
    }

    @Override
    public Controller[] getControllers() {
        return controller.getControllers();
    }

    @Override
    public Type getType() {
        return controller.getType();
    }

    @Override
    public Component[] getComponents() {
        return controller.getComponents();
    }

    @Override
    public Component getComponent(Component.Identifier id) {
        return controller.getComponent(id);
    }

    @Override
    public Rumbler[] getRumblers() {
        return controller.getRumblers();
    }

    @Override
    public void setEventQueueSize(int size) {
        controller.setEventQueueSize(size);
    }

    @Override
    public EventQueue getEventQueue() {
        return controller.getEventQueue();
    }

    @Override
    public PortType getPortType() {
        return controller.getPortType();
    }

    @Override
    public int getPortNumber() {
        return controller.getPortNumber();
    }

    @Override
    public String getName() {
        return controller.getName();
    }

}
