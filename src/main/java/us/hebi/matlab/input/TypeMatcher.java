package us.hebi.matlab.input;

import net.java.games.input.Controller;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 25 Jan 2017
 */
interface TypeMatcher {

    public boolean matches(Controller.Type type);

}
