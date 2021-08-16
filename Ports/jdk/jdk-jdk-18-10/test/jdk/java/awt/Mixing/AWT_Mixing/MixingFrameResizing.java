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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import javax.swing.JFrame;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import test.java.awt.regtesthelpers.Util;

/**
 * AWT/Swing overlapping test.
 * <p>This test puts heavyweight component into JFrame and verifies that it's being drawn correctly after resizing the frame.
 * <p>See base class for test info.
 */
/*
 * @test
 * @key headful
 * @bug 6777370 8221823
 * @summary Issues when resizing the JFrame with HW components
 * @author sergey.grinev@oracle.com: area=awt.mixing
 * @library /java/awt/patchlib  ../../regtesthelpers
 * @modules java.desktop/sun.awt
 *          java.desktop/java.awt.peer
 * @build java.desktop/java.awt.Helper
 * @build Util
 * @run main MixingFrameResizing
 */
public class MixingFrameResizing extends OverlappingTestBase {

    {testEmbeddedFrame = true;}

    private JFrame frame = null;
    private Point lLoc;
    private Point lLoc2;
    private Dimension size;

    protected void prepareControls() {
        if(frame != null) {
            frame.setVisible(false);
        }
        frame = new JFrame("Mixing : Frame Resizing test");
        frame.setLayout(new SpringLayout());
        frame.setSize(50, 50);
        frame.setVisible(true);
        propagateAWTControls(frame);
        Util.waitTillShown(frame);
    }

    @Override
    protected boolean performTest() {
        int BORDER_SHIFT = frameBorderCounter();
        BORDER_SHIFT = Math.abs(BORDER_SHIFT) == 1 ? BORDER_SHIFT : (BORDER_SHIFT / 2);
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    lLoc = frame.getLocationOnScreen();
                    size = frame.getSize();
                    lLoc2 = frame.getContentPane().getLocationOnScreen();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Where is frame?");
        }
        Robot robot = Util.createRobot();
        robot.setAutoDelay(ROBOT_DELAY/2);

        // resize window
        robot.mouseMove(lLoc.x + size.width / 2 + BORDER_SHIFT, lLoc.y + size.height + BORDER_SHIFT);
        Util.waitForIdle(robot);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        for (int i = 0; i < 10; i++) {
            robot.mouseMove(lLoc.x + size.width / 2 + BORDER_SHIFT, lLoc.y + size.height + BORDER_SHIFT + 20 * i);
        }
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        robot.mouseMove(lLoc.x + size.width + BORDER_SHIFT, lLoc.y + size.height + BORDER_SHIFT);
        Util.waitForIdle(robot);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        for (int i = 0; i < 10; i++) {
            robot.mouseMove(lLoc.x + size.width + BORDER_SHIFT + 20 * i, lLoc.y + size.height + BORDER_SHIFT);
        }
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        Util.waitForIdle(robot);
        // check if component is visible on the opened space
        try {
            Thread.sleep(300); //some more wait for Solaris (for some reason)
        }catch(Exception ex) {}
        lLoc2.translate(75, 75);
        Color c = robot.getPixelColor(lLoc2.x, lLoc2.y);
        System.out.println("Actual: "+c+", expected: "+AWT_VERIFY_COLOR);

        if (!c.equals(AWT_VERIFY_COLOR)) {
            fail("HW component is not visible after resizing");
        }

        return true;
    }

    // this strange plumbing stuff is required due to "Standard Test Machinery" in base class
    public static void main(String args[]) throws InterruptedException {
        if (System.getProperty("os.name").toLowerCase().contains("os x")) {
            System.out.println("Aqua L&F ignores setting color to component. Test passes on Mac OS X.");
            return;
        }
        instance = new MixingFrameResizing();
        OverlappingTestBase.doMain(args);
    }
}
