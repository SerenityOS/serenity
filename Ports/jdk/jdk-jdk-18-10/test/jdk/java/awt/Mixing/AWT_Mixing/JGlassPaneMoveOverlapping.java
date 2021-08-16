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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;
import test.java.awt.regtesthelpers.Util;

/**
 * AWT/Swing overlapping test with JInternalFrame being moved in GlassPane.
 * See <a href="https://bugs.openjdk.java.net/browse/JDK-6637655">JDK-6637655</a> and
 * <a href="https://bugs.openjdk.java.net/browse/JDK-6981919">JDK-6981919</a>.
 * <p>See base class for details.
 */
/*
 * @test
 * @key headful
 * @bug 6637655 6981919
 * @summary Overlapping test for javax.swing.JScrollPane
 * @author sergey.grinev@oracle.com: area=awt.mixing
 * @library /java/awt/patchlib  ../../regtesthelpers
 * @modules java.desktop/sun.awt
 *          java.desktop/java.awt.peer
 * @build java.desktop/java.awt.Helper
 * @build Util
 * @run main JGlassPaneMoveOverlapping
 */
public class JGlassPaneMoveOverlapping extends OverlappingTestBase {

    private boolean lwClicked = true;
    private volatile Point lLoc;
    private volatile Point lLoc2;

    private JInternalFrame internalFrame;
    private JFrame frame = null;
    private volatile Point frameLoc;

    protected boolean performTest() {

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    lLoc = internalFrame.getContentPane().getLocationOnScreen();
                    lLoc2 = lLoc.getLocation();
                    lLoc2.translate(0, internalFrame.getHeight());
                    frameLoc = frame.getLocationOnScreen();
                    frameLoc.translate(frame.getWidth()/2, 3);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Where is internal frame?");
        }

        // run robot
        Robot robot = Util.createRobot();
        robot.setAutoDelay(ROBOT_DELAY);

        // force focus on JFrame (jtreg workaround)
        robot.mouseMove(frameLoc.x, frameLoc.y);
        Util.waitForIdle(robot);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        Util.waitForIdle(robot);


        //slow move
        robot.mouseMove(lLoc.x +  25, lLoc.y - 5);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(100);
        robot.mouseMove(lLoc.x +  45, lLoc.y - 5);
        robot.delay(100);
        robot.mouseMove(lLoc.x +  internalWidth - 75, lLoc.y - 5);
        robot.delay(100);
        robot.mouseMove(lLoc.x +  internalWidth - 55, lLoc.y - 5);
        robot.delay(100);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        Color c2 = robot.getPixelColor(lLoc.x +  internalWidth + 15, lLoc.y - 5);
        if (c2.equals(AWT_BACKGROUND_COLOR)) {
            error("Foreground frame is not drawn properly");
        }
        Color c = robot.getPixelColor(lLoc.x +  internalWidth - 95, lLoc.y + 5);
        robot.mouseMove(lLoc.x +  internalWidth - 95, lLoc.y + 5);
        System.out.println("color: " + c + " " + AWT_BACKGROUND_COLOR);
        if (!c.equals(AWT_BACKGROUND_COLOR) && currentAwtControl.getClass() != java.awt.Scrollbar.class) {
            error("Background AWT component is not drawn properly");
        }

        return true;
    }

    // {debugClassName = "Choice";}

    private static void error(String st) {
        //System.out.println(st);
        fail(st);
    }

    private static final int internalWidth = 200;

    @Override
    protected void prepareControls() {
        if(frame != null) {
            frame.setVisible(false);
        }
        frame = new JFrame("Glass Pane children test");
        frame.setLayout(null);

        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        super.propagateAWTControls(contentPane);

        Container glassPane = (Container) frame.getRootPane().getGlassPane();
        glassPane.setVisible(true);
        glassPane.setLayout(null);

        internalFrame = new JInternalFrame("Internal Frame", true);
        internalFrame.setBounds(50, 0, internalWidth, 100);
        internalFrame.setVisible(true);
        glassPane.add(internalFrame);

        internalFrame.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                lwClicked = true;
            }
        });

        frame.setSize(400, 180);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // this strange plumbing stuff is required due to "Standard Test Machinery" in base class
    public static void main(String args[]) throws InterruptedException {
        if (System.getProperty("os.name").toLowerCase().contains("os x")) {
            System.out.println("Aqua L&F ignores setting color to component. Test passes on Mac OS X.");
            return;
        }
        instance = new JGlassPaneMoveOverlapping();
        OverlappingTestBase.doMain(args);
    }
}
