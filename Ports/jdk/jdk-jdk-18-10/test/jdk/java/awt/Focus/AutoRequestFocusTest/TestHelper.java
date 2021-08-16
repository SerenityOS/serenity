/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Util class used for testing RFE 6187066.
 * @author anton.tarasov
 */

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.InvocationTargetException;

public class TestHelper {
    private static volatile boolean focusChanged;
    private static volatile boolean trackFocusChange;
    private static boolean focusChangeTrackerSet;

    /*
     * @param action the action to perform
     * @return if {@code action} caused focus change
     */
    public static boolean trackFocusChangeFor(Runnable action, Robot robot) {
        if (!focusChangeTrackerSet) {
            setFocusChangeTracker();
        }

        focusChanged = false;
        trackFocusChange = true;

        action.run();

        Util.waitForIdle(robot);

        trackFocusChange = false;

        return focusChanged;
    }

    public static void invokeLaterAndWait(Runnable action, Robot robot) {
        EventQueue.invokeLater(action);
        try {
            EventQueue.invokeAndWait(new Runnable() { // waiting for action
                    public void run() {}
                });
        } catch (InterruptedException ie) {
        } catch (InvocationTargetException ite) {}

        Util.waitForIdle(robot); // waiting for events
    }

    private static void setFocusChangeTracker() {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
                public void eventDispatched(AWTEvent e) {
                    int id = e.getID();
                    if (trackFocusChange &&
                        (id == FocusEvent.FOCUS_GAINED || id == FocusEvent.FOCUS_LOST ||
                         id == WindowEvent.WINDOW_GAINED_FOCUS || id == WindowEvent.WINDOW_LOST_FOCUS ||
                         id == WindowEvent.WINDOW_ACTIVATED || id == WindowEvent.WINDOW_DEACTIVATED))
                    {
                        System.out.println(e.toString());
                        focusChanged = true;
                    }
                }
            }, FocusEvent.FOCUS_EVENT_MASK | WindowEvent.WINDOW_FOCUS_EVENT_MASK | WindowEvent.WINDOW_EVENT_MASK);

        focusChangeTrackerSet = true;
    }
}
