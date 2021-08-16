/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @bug 8031694
  @summary [macosx] TwentyThousandTest test intermittently hangs
  @author Oleg Pekhovskiy
  @modules java.desktop/sun.awt
  @modules java.desktop/java.awt:open
  @run main EDTShutdownTest
 */

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import sun.awt.AWTAccessor;

public class EDTShutdownTest {

    private static boolean passed = false;

    public static void main(String[] args) {
        // Force EDT start with InvocationEvent
        EventQueue.invokeLater(() -> {
            // EventQueue is empty now
            EventQueue queue = Toolkit.getDefaultToolkit()
                               .getSystemEventQueue();
            Thread thread = AWTAccessor.getEventQueueAccessor()
                            .getDispatchThread(queue);
            try {
                /*
                 * Clear EventDispatchThread.doDispatch flag to break message
                 * loop in EventDispatchThread.pumpEventsForFilter()
                 */
                Method stopDispatching = thread.getClass()
                        .getDeclaredMethod("stopDispatching", null);
                stopDispatching.setAccessible(true);
                stopDispatching.invoke(thread, null);

                /*
                 * Post another InvocationEvent that must be handled by another
                 * instance of EDT
                 */
                EventQueue.invokeLater(() -> {
                    passed = true;
                });
            }
            catch (InvocationTargetException | NoSuchMethodException
                   | IllegalAccessException e) {
            }
        });

        // Wait for EDT shutdown
        EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        Thread thread = AWTAccessor.getEventQueueAccessor()
                        .getDispatchThread(queue);
        try {
            thread.join();

            /*
             * Wait for another EDT instance to handle the InvocationEvent
             * and shutdown
             */
            thread = AWTAccessor.getEventQueueAccessor()
                     .getDispatchThread(queue);
            if (thread != null) {
                thread.join();
            }
        }
        catch (InterruptedException e) {
        }

        if (passed) {
            System.out.println("Test PASSED!");
        }
        else {
            throw new RuntimeException("Test FAILED!");
        }
    }
}
