package us.hebi.matlab.input;

import java.io.PrintWriter;

/**
 * Exception class that renders nicer in MATLAB.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 20 Jan 2017
 */
class MatlabError extends RuntimeException {


    MatlabError(String message) {
        super(message);
    }

    /**
     * Removes type information from error message
     */
    @Override
    public void printStackTrace(PrintWriter s) {
        synchronized (s) {
            s.println("\n" + getMessage());
        }
    }

    /**
     * Removes stack trace in >1.6 environments
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
