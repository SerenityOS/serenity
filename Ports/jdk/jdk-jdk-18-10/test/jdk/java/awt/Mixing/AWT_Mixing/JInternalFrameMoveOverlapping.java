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

import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import test.java.awt.regtesthelpers.Util;

/**
 * AWT/Swing overlapping test for {@link javax.swing.JInternalFrame } component during move.
 * <p>See <a href="http://monaco.sfbay.sun.com/detail.jsf?cr=6985399">CR6768230</a> for details and base class for test info.
 */
/*
 * @test
 * @key headful
 * @bug 6985399
 * @summary Overlapping test for javax.swing.JScrollPane
 * @author sergey.grinev@oracle.com: area=awt.mixing
 * @library /java/awt/patchlib  ../../regtesthelpers
 * @modules java.desktop/sun.awt
 *          java.desktop/java.awt.peer
 * @build java.desktop/java.awt.Helper
 * @build Util
 * @run main JInternalFrameMoveOverlapping
 */
public class JInternalFrameMoveOverlapping extends OverlappingTestBase {

    private boolean lwClicked = true;
    private Point locTopFrame;
    private Point locTarget;

    protected boolean performTest() {
        // run robot
        Robot robot = Util.createRobot();
        robot.setAutoDelay(ROBOT_DELAY);

        robot.mouseMove(locTopFrame.x + 25, locTopFrame.y + 25);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
        }
        robot.mouseMove(locTopFrame.x + (locTarget.x - locTopFrame.x)/2, locTopFrame.y + (locTarget.y - locTopFrame.y)/2);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
        }
        robot.mouseMove(locTarget.x, locTarget.y);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
        }
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        clickAndBlink(robot, locTarget);

        return lwClicked;
    }

    //static {debugClassName = "Choice";}

    @Override
    protected void prepareControls() {


        JDesktopPane desktopPane = new JDesktopPane();

        JInternalFrame bottomFrame = new JInternalFrame("bottom frame", false, false, false, false);
        bottomFrame.setSize(220, 220);
        super.propagateAWTControls(bottomFrame);
        desktopPane.add(bottomFrame);
        bottomFrame.setVisible(true);

        JInternalFrame topFrame = new JInternalFrame("top frame", false, false, false, false);
        topFrame.setSize(200, 200);
        topFrame.add(new JButton("LW Button") {

            {
                addMouseListener(new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        lwClicked = true;
                    }
                });
            }
        });
        desktopPane.add(topFrame);
        topFrame.setVisible(true);

        JFrame frame = new JFrame("Test Window");
        frame.setSize(300, 300);
        frame.setContentPane(desktopPane);
        frame.setVisible(true);

        locTopFrame = topFrame.getLocationOnScreen();
        locTarget = new Point(locTopFrame.x + bottomFrame.getWidth() / 2, locTopFrame.y + bottomFrame.getHeight()/2);
    }

    // this strange plumbing stuff is required due to "Standard Test Machinery" in base class
    public static void main(String args[]) throws InterruptedException {
        instance = new JInternalFrameMoveOverlapping();
        OverlappingTestBase.doMain(args);
    }
}
