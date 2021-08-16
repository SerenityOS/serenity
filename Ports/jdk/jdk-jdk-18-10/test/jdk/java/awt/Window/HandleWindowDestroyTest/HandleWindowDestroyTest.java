/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
  @test
  @key headful
  @bug 6260648
  @summary Tests that WINDOW_DESTROY event can be handled by overriding handleEvent(). Also,
tests that handleEvent() is not called by AWT if any listener is added to the component
(i. e. when post-1.1 events schema is used)
  @run main HandleWindowDestroyTest
*/

import java.awt.*;
import java.awt.event.*;

public class HandleWindowDestroyTest {

    private static volatile boolean handleEventCalled;

    public static void main(final String[] args) {
        Robot robot;
        try {
            robot = new Robot();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unexpected failure");
        }

        Frame f = new Frame("Frame")
        {
            public boolean handleEvent(Event e)
            {
                if (e.id == Event.WINDOW_DESTROY)
                {
                    handleEventCalled = true;
                }
                return super.handleEvent(e);
            }
        };
        f.setBounds(100, 100, 100, 100);
        f.setVisible(true);
        robot.waitForIdle();

        handleEventCalled = false;
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new WindowEvent(f, Event.WINDOW_DESTROY));
        robot.waitForIdle();

        if (!handleEventCalled)
        {
            throw new RuntimeException("Test FAILED: handleEvent() is not called");
        }

        f.addWindowListener(new WindowAdapter()
        {
        });

        handleEventCalled = false;
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new WindowEvent(f, Event.WINDOW_DESTROY));
        robot.waitForIdle();

        if (handleEventCalled)
        {
            throw new RuntimeException("Test FAILED: handleEvent() is called event with a listener added");
        }
    }
}
