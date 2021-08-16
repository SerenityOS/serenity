/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package nsk.monitoring.share;

import nsk.share.*;

/**
 * An abstract base class for operating VM state. The follow methods must
 * be implemented by its subclasses:
 * <ul>
 *      <li><code>run()</code> -- brings VM into defined state.
 *      <li><code>reset()</code> -- tries to reclaim VM into initial state.
 * </ul>
 */
public abstract class StateController implements Runnable {

    /**
     * Public default constructor.
     */
    public StateController() {
        super();
    }

    /**
     * A string that is printed before each string when {@link Log#complain
     * <code>Log.complain()</code>}, and {@link Log#display
     * <code>Log.display()</code>} are invoked.
     */
    protected String logPrefix;

    /**
     * A variable to save reference to {@link Log <code>Log.Logger</code>}
     * class.
     */
    protected Log.Logger logger;

    /**
     * Brings VM into defined state.
     */
    public abstract void run();

    /**
     * Tries to reclaim VM into initial state
     */
    public abstract void reset();

    /**
     * Outputs <code>message</code> using {@link Log <code>Log.Logger</code>}
     * object.
     */
    protected void display(String message) {
        if (logger != null)
            logger.display(message);
    }

    /**
     * Outputs <code>message</code> using {@link Log <code>Log.Logger</code>}
     * object.
     */
    protected void complain(String message) {
        if (logger != null)
            logger.complain(message);
    }

    /**
     * Defines {@link Log <code>Log.Logger</code>} object.
     */
    public void setLog(Log log) {
        logger = new Log.Logger(log, logPrefix);
    }

    /**
     * Converts an integer to string.
     *
     * @param i an integer to convert.
     * @return a string that represents the int value.
     */
    public String int2Str(int i) {
        String tmp = "";

        if (i < 10) {
            tmp = "00";
        } else if (i >= 10 && i < 100) {
            tmp = "0";
        }
        return tmp + String.valueOf(i);
    }
}
