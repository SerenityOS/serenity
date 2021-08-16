/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @key headful
 * @bug 8012004
 * @summary JINTERNALFRAME NOT BEING FINALIZED AFTER CLOSING
 * @author mcherkas
 * @run main InternalFrameIsNotCollectedTest
 */
import javax.swing.*;
import java.awt.*;
import java.beans.PropertyVetoException;
import java.util.Date;

public class InternalFrameIsNotCollectedTest {

    public static final int maxWaitTime = 100000;
    public static final int waitTime = 5000;
    private static Robot robot;
    private static CustomInternalFrame iFrame;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            initRobot();
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    initUI();
                    try {
                        closeInternalFrame();
                    } catch (PropertyVetoException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            robot.waitForIdle();
            invokeGC();
            System.runFinalization();
            Thread.sleep(1000); // it's better to wait 1 sec now then 10 sec later
            Date startWaiting = new Date();
            synchronized (CustomInternalFrame.waiter) {
                // Sync with finalization thread.
                Date now = new Date();
                while (now.getTime() - startWaiting.getTime() < maxWaitTime && !CustomInternalFrame.finalized) {
                    CustomInternalFrame.waiter.wait(waitTime);
                    now = new Date();
                }
            }
            if (!CustomInternalFrame.finalized) {
                throw new RuntimeException("Closed internal frame wasn't collected");
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    private static void initRobot() throws AWTException {
        robot = new Robot();
        robot.setAutoDelay(100);
    }

    private static void closeInternalFrame() throws PropertyVetoException {
        iFrame.setClosed(true);
        iFrame = null;
    }

    private static void initUI() {
        frame = new JFrame("Internal Frame Test");
        frame.getContentPane().setLayout(new BorderLayout());
        JDesktopPane desktopPane = new JDesktopPane();
        desktopPane.setDesktopManager(new DefaultDesktopManager());
        frame.getContentPane().add(desktopPane, BorderLayout.CENTER);

        iFrame = new CustomInternalFrame("Dummy Frame");

        iFrame.setSize(200, 200);
        iFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        desktopPane.add(iFrame);

        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
        iFrame.setVisible(true);
    }

    private static void invokeGC() {
        System.out.println("Firing garbage collection!");
        try {
            StringBuilder sb = new StringBuilder();
            while (true) {
                sb.append("any string. some test. a little bit more text." + sb.toString());
            }
        } catch (Throwable e) {
            // do nothing
        }
    }


    public static class CustomInternalFrame extends JInternalFrame {
        public static volatile boolean finalized = false;
        public static Object waiter = new Object();

        public CustomInternalFrame(String title) {
            super(title, true, true, true, true);
        }

        protected void finalize() {
            System.out.println("Finalized!");
            finalized = true;
            waiter.notifyAll();
        }
    }
}
