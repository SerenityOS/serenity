/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8015748
 * @summary verifies ProgressBar RightToLeft orientations for all Look and Feels
 * @library ../../regtesthelpers
 * @build Util
 * @run main JProgressBarOrientationRobotTest
 */
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Point;
import java.awt.Robot;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class JProgressBarOrientationRobotTest {

    private static JFrame frame;
    private static JProgressBar progressBar;
    private static Robot robot;
    private static Color colorCenter;
    private static Color colorLeft;
    private static Color colorRight;
    private static final int widthBuffer = 20;
    private static volatile String errorString = "";

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        robot.waitForIdle();
        UIManager.LookAndFeelInfo[] lookAndFeelArray
                = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo lookAndFeelItem : lookAndFeelArray) {
            executeCase(lookAndFeelItem.getClassName(),
                    lookAndFeelItem.getName());

        }
        if (!"".equals(errorString)) {
            System.err.println(errorString);
        }
    }

    private static void executeCase(String lookAndFeelString,
            String shortenedLandFeelString) throws Exception {
        if (tryLookAndFeel(lookAndFeelString)) {
            createUI(shortenedLandFeelString);
            robot.waitForIdle();

            createLTR();
            robot.delay(1000);
            runTestCase();
            robot.delay(1000);
            testCaseLTR(shortenedLandFeelString);
            robot.delay(1000);

            createRTL();
            robot.delay(1000);
            runTestCase();
            robot.delay(1000);
            testCaseRTL(shortenedLandFeelString);
            robot.delay(1000);

            cleanUp();
        }

    }

    private static void createUI(final String shortenedLookAndFeelString)
            throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                progressBar = new JProgressBar();
                progressBar.setValue(30);
                frame = new JFrame(shortenedLookAndFeelString);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().add(progressBar);
                frame.pack();
                frame.setSize(500, frame.getSize().height);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.toFront();
            }
        });
    }

    private static void createLTR()
            throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                progressBar.applyComponentOrientation(
                        ComponentOrientation.LEFT_TO_RIGHT);
                progressBar.repaint();
            }
        });
    }

    private static void createRTL()
            throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                progressBar.applyComponentOrientation(
                        ComponentOrientation.RIGHT_TO_LEFT);
                progressBar.repaint();
            }
        });
    }

    private static void runTestCase() throws Exception {
        Point centerPoint = Util.getCenterPoint(progressBar);
        colorCenter = robot.getPixelColor(centerPoint.x, centerPoint.y);
        colorRight = robot.getPixelColor(
                (centerPoint.x + progressBar.getWidth() / 2 - widthBuffer),
                centerPoint.y);
        colorLeft = robot.getPixelColor(
                (centerPoint.x - progressBar.getWidth() / 2 + widthBuffer),
                centerPoint.y);
        robot.waitForIdle();
    }

    private static void testCaseLTR(String shortenedLookAndFeelString)
            throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {

                if (colorCenter.equals(colorRight)) {
                    if (!colorCenter.equals(colorLeft)) {
                        System.out.println("[" + shortenedLookAndFeelString
                                + "]: LTR orientation test passed");
                    }
                } else {
                    frame.dispose();
                    String error = "[" + shortenedLookAndFeelString
                            + "]: [Error]: LTR orientation test failed";
                    errorString += error;
                    System.err.println(error);
                }
            }
        });

    }

    private static void testCaseRTL(String shortenedLookAndFeelString)
            throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                if (colorCenter.equals(colorLeft)) {
                    if (!colorCenter.equals(colorRight)) {
                        System.out.println("[" + shortenedLookAndFeelString
                                + "]: RTL orientation test passed");
                    }
                } else {
                    frame.dispose();
                    String error = "[" + shortenedLookAndFeelString
                            + "]: [Error]: LTR orientation test failed";
                    errorString += error;
                    System.err.println(error);
                }
            }
        });
    }

    private static void cleanUp() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.dispose();
            }
        });
    }

    private static boolean tryLookAndFeel(String lookAndFeelString)
            throws Exception {
        try {
            UIManager.setLookAndFeel(
                    lookAndFeelString);

        } catch (UnsupportedLookAndFeelException
                | ClassNotFoundException
                | InstantiationException
                | IllegalAccessException e) {
            errorString += e.getMessage() + "\n";
            System.err.println("[Exception]: " + e.getMessage());
            return false;
        }
        return true;
    }
}
