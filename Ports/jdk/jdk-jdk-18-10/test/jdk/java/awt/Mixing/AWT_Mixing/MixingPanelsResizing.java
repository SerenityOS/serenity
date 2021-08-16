/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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


import java.awt.*;
import java.awt.event.InputEvent;
import javax.swing.*;
import java.io.*;
import test.java.awt.regtesthelpers.Util;

/**
 * AWT/Swing overlapping test for Panel and JPanel behavior during resizing.
 * <p>See <a href="https://bugs.openjdk.java.net/browse/JDK-6786219">JDK-6786219</a> for details
 */
/*
 * @test
 * @key headful
 * @bug 6786219 8221823
 * @summary Issues when resizing the frame after mixing of heavy weight & light weight components
 * @author sergey.grinev@oracle.com: area=awt.mixing
 * @library ../../regtesthelpers
 * @build Util
 * @build FrameBorderCounter
 * @run main MixingPanelsResizing
 */
public class MixingPanelsResizing {

    static volatile boolean failed = false;

    private static JFrame frame;
    private static JButton jbutton;
    private static Button awtButton;
    private static JButton jbutton2;
    private static Button awtButton2;
    private static final Color jbColor = Color.RED;
    private static final Color awtColor = Color.ORANGE;
    private static final Color jb2Color = Color.BLUE;
    private static final Color awt2Color = Color.CYAN;
    private static final int ROBOT_DELAY = 500;

    private static Point lLoc;
    private static int borderShift;

    private static int frameBorderCounter() {
        String JAVA_HOME = System.getProperty("java.home");
        try {
            Process p = Runtime.getRuntime().exec(JAVA_HOME + "/bin/java FrameBorderCounter");
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            if (p.exitValue() != 0) {
                throw new RuntimeException("FrameBorderCounter exited with not null code!\n" + readInputStream(p.getErrorStream()));
            }
            return Integer.parseInt(readInputStream(p.getInputStream()).trim());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static String readInputStream(InputStream is) throws IOException {
        byte[] buffer = new byte[4096];
        int len = 0;
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader isr = new InputStreamReader(is)) {
            while ((len = is.read(buffer)) > 0) {
                sb.append(new String(buffer, 0, len));
            }
        }
        return sb.toString();
    }

    private static void init() throws Exception {
        //*** Create instructions for the user here ***

        borderShift = frameBorderCounter();
        borderShift = Math.abs(borderShift) == 1 ? borderShift : (borderShift / 2);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                // prepare controls

                frame = new JFrame();

                Panel awtPanel = new Panel();
                awtPanel.setBackground(Color.GREEN);
                awtButton = new Button("AWTButton");
                awtPanel.add(awtButton);
                awtButton.setForeground(awtColor);
                awtButton.setBackground(awtColor);
                jbutton = new JButton("SwingButton");
                awtPanel.add(jbutton);
                jbutton.setForeground(jbColor);
                jbutton.setBackground(jbColor);

                JPanel jPanel = new JPanel();
                jbutton2 = new JButton("SwingButton2");
                jPanel.add(jbutton2);
                jbutton2.setForeground(jb2Color);
                jbutton2.setBackground(jb2Color);
                awtButton2 = new Button("AWT Button2");
                jPanel.add(awtButton2);
                awtButton2.setForeground(awt2Color);
                awtButton2.setBackground(awt2Color);
                jPanel.setBackground(Color.YELLOW);

                frame.add(awtPanel, BorderLayout.SOUTH);
                frame.add(jPanel, BorderLayout.NORTH);

                frame.pack();
                frame.setVisible(true);
            }
        });

        /////////////////////////

        final Robot robot = Util.createRobot();
        robot.setAutoDelay(ROBOT_DELAY);

        Util.waitForIdle(robot);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                lLoc = frame.getLocationOnScreen();
                lLoc.translate(frame.getWidth() + borderShift, frame.getHeight() + borderShift);
            }
        });

        //grow
        robot.mouseMove(lLoc.x, lLoc.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);

        Runnable test = new Runnable() {

            public void run() {
                Point btnLoc = jbutton.getLocationOnScreen();
                Color c = robot.getPixelColor(btnLoc.x + 5, btnLoc.y + 5);
                if (!c.equals(jbColor)) {
                    fail("JButton was not redrawn properly on AWT Panel during move");
                }

                btnLoc = awtButton.getLocationOnScreen();
                c = robot.getPixelColor(btnLoc.x + 5, btnLoc.y + 5);
                if (!c.equals(awtColor)) {
                    fail("AWT Button was not redrawn properly on AWT Panel during move");
                }

                btnLoc = jbutton2.getLocationOnScreen();
                c = robot.getPixelColor(btnLoc.x + 5, btnLoc.y + 5);
                if (!c.equals(jb2Color)) {
                    fail("JButton was not redrawn properly on JPanel during move");
                }

                btnLoc = awtButton2.getLocationOnScreen();
                c = robot.getPixelColor(btnLoc.x + 5, btnLoc.y + 5);
                if (!c.equals(awt2Color)) {
                    fail("ATW Button was not redrawn properly on JPanel during move");
                }
            }
        };

        for (int i = 0; i < 30; i++) {
            test.run();
            robot.mouseMove(lLoc.x + 20 * i, lLoc.y + 10 * i);
        }
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        //back
        System.out.println("fast back");
        robot.mousePress(InputEvent.BUTTON1_MASK);
        for (int i = 5; i >= 0; i--) {
            test.run();
            robot.mouseMove(lLoc.x + 120 * i, lLoc.y + 60 * i);
        }
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        pass();
    }//End  init()
    /*****************************************************
     * Standard Test Machinery Section
     * DO NOT modify anything in this section -- it's a
     * standard chunk of code which has all of the
     * synchronisation necessary for the test harness.
     * By keeping it the same in all tests, it is easier
     * to read and understand someone else's test, as
     * well as insuring that all tests behave correctly
     * with the test harness.
     * There is a section following this for test-
     * classes
     ******************************************************/
    private static boolean theTestPassed = false;
    private static boolean testGeneratedInterrupt = false;
    private static String failureMessage = "";
    private static Thread mainThread = null;
    private static int sleepTime = 300000;

    // Not sure about what happens if multiple of this test are
    //  instantiated in the same VM.  Being static (and using
    //  static vars), it aint gonna work.  Not worrying about
    //  it for now.
    public static void main(String args[]) throws Exception {
        if (!Toolkit.getDefaultToolkit().isDynamicLayoutActive()) {
            System.out.println("Dynamic layout is not active. Test passes.");
            return;
        }
        mainThread = Thread.currentThread();
        try {
            init();
        } catch (TestPassedException e) {
            //The test passed, so just return from main and harness will
            // interepret this return as a pass
            return;
        }
        //At this point, neither test pass nor test fail has been
        // called -- either would have thrown an exception and ended the
        // test, so we know we have multiple threads.

        //Test involves other threads, so sleep and wait for them to
        // called pass() or fail()
        try {
            Thread.sleep(sleepTime);
            //Timed out, so fail the test
            throw new RuntimeException("Timed out after " + sleepTime / 1000 + " seconds");
        } catch (InterruptedException e) {
            //The test harness may have interrupted the test.  If so, rethrow the exception
            // so that the harness gets it and deals with it.
            if (!testGeneratedInterrupt) {
                throw e;
            }

            //reset flag in case hit this code more than once for some reason (just safety)
            testGeneratedInterrupt = false;

            if (theTestPassed == false) {
                throw new RuntimeException(failureMessage);
            }
        }

    }//main

    public static synchronized void setTimeoutTo(int seconds) {
        sleepTime = seconds * 1000;
    }

    public static synchronized void pass() {
        System.out.println("The test passed.");
        System.out.println("The test is over, hit  Ctl-C to stop Java VM");
        //first check if this is executing in main thread
        if (mainThread == Thread.currentThread()) {
            //Still in the main thread, so set the flag just for kicks,
            // and throw a test passed exception which will be caught
            // and end the test.
            theTestPassed = true;
            throw new TestPassedException();
        }
        theTestPassed = true;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }//pass()

    public static synchronized void fail() {
        //test writer didn't specify why test failed, so give generic
        fail("it just plain failed! :-)");
    }

    public static synchronized void fail(String whyFailed) {
        System.out.println("The test failed: " + whyFailed);
        System.out.println("The test is over, hit  Ctl-C to stop Java VM");
        //check if this called from main thread
        if (mainThread == Thread.currentThread()) {
            //If main thread, fail now 'cause not sleeping
            throw new RuntimeException(whyFailed);
        }
        theTestPassed = false;
        testGeneratedInterrupt = true;
        failureMessage = whyFailed;
        mainThread.interrupt();
    }//fail()
}// class JButtonInGlassPane
class TestPassedException extends RuntimeException {
}
