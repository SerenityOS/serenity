/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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


import java.awt.Frame;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import test.java.awt.regtesthelpers.Util;

/**
 * AWT/Swing overlapping test for opaque Swing components.
 * <p>This test verify if AWT components are drawn correctly under opaque components.
 * <p>See <a href="https://bugs.openjdk.java.net/browse/JDK-6776743">JDK-6776743</a> for details
 * <p>See base class for test info.
 */
/*
 * @test
 * @key headful
 * @bug 6776743 8173409
 * @summary Opaque overlapping test for each AWT component
 * @library /java/awt/patchlib  ../../regtesthelpers
 * @modules java.desktop/java.awt.peer
 *          java.desktop/sun.awt
 * @build java.desktop/java.awt.Helper
 * @build Util
 * @run main OpaqueOverlapping
 */
public class OpaqueOverlapping extends OverlappingTestBase {

    {
        useClickValidation = false;
        failMessage = "Opacity test mismatchs";

        // CR 6994264 (Choice autohides dropdown on Solaris 10)
        skipClassNames = new String[] { "Choice" };
    }
    private String testSeq;
    private final static String checkSeq = "010000101";
    private Point heavyLoc;
    private JButton light;
    private Frame frame = null;

    protected void prepareControls() {
        testSeq = "";
        // Create components
        if(frame != null) {
            frame.setVisible(false);
        }
        frame = new Frame("OpaqueOverlapping mixing test");
        final Panel panel = new Panel();
        panel.setLayout(null);

        propagateAWTControls(panel);

        // Overlap the buttons
        currentAwtControl.setBounds(30, 30, 200, 200);

        light = new JButton("  LW Button  ");
        light.setBounds(10, 10, 50, 50);

        // Put the components into the frame
        panel.add(light);
        frame.add(panel);
        frame.setBounds(50, 50, 400, 400);
        frame.setVisible(true);

        currentAwtControl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                panel.setComponentZOrder(light, 0);
                frame.validate();
                testSeq = testSeq + "0";
            }
        });
        light.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                panel.setComponentZOrder(currentAwtControl, 0);
                frame.validate();
                testSeq = testSeq + "1";
            }
        });
    }

    @Override
    protected boolean performTest() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    heavyLoc = currentAwtControl.getLocationOnScreen();
                }
            });
        } catch (Exception e) {
        }
        Robot robot = Util.createRobot();
        robot.setAutoDelay(ROBOT_DELAY);

        Util.waitForIdle(robot);

        // Move the mouse pointer to the position where both
        //    components overlap
        robot.mouseMove(heavyLoc.x + 5, heavyLoc.y + 5);

        // Now perform the click at this point for 9 times
        // In the middle of the process toggle the opaque
        // flag value.
        for (int i = 0; i < 9; ++i) {
            if (i == 3) {
                light.setMixingCutoutShape(new Rectangle());
            }
            if (i == 6) {
                light.setMixingCutoutShape(null);
            }

            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            Util.waitForIdle(robot);

            if (currentAwtControl.getClass() == java.awt.Choice.class && i != 1 && i != 6 && i != 8) {
                // due to the fact that Choice doesn't get mouseClicked event if its dropdown is shown
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                Util.waitForIdle(robot);
            }
        }

        Util.waitForIdle(robot);

        boolean result = testSeq.equals(checkSeq);
        if (!result) {
            System.err.println("Expected: " + checkSeq);
            System.err.println("Observed: " + testSeq);
        }
        return result;
    }

    // this strange plumbing stuff is required due to "Standard Test Machinery" in base class
    public static void main(String args[]) throws InterruptedException {
        instance = new OpaqueOverlapping();
        OverlappingTestBase.doMain(args);
    }
}
