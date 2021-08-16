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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import test.java.awt.regtesthelpers.Util;

/**
 * AWT/Swing overlapping test for viewport
 * <p>This test verify if AWT components are drawn correctly being partially shown through viewport
 * <p>See <a href="http://monaco.sfbay.sun.com/detail.jsf?cr=6778882">CR6778882</a> for details
 * <p>See base class for test info.
 */
/*
 * @test
 * @key headful
 * @bug 6778882
 * @summary Viewport overlapping test for each AWT component
 * @author sergey.grinev@oracle.com: area=awt.mixing
 * @library /java/awt/patchlib ../../regtesthelpers
 * @modules java.desktop/sun.awt
 *          java.desktop/java.awt.peer
 * @build java.desktop/java.awt.Helper
 * @build Util
 * @run main ViewportOverlapping
 */
public class ViewportOverlapping extends OverlappingTestBase {

    private volatile int frameClicked;
    private Point hLoc;
    private Point vLoc;
    private Point testLoc;
    private Point resizeLoc;

    private JFrame f;
    private JPanel p;
    private JButton b;
    private JScrollPane scrollPane;

    protected void prepareControls() {
        p = new JPanel(new GridLayout(0, 1));
        propagateAWTControls(p);
        b = new JButton("Space extender");
        p.add(b);
        p.setPreferredSize(new Dimension(500, 500));
        scrollPane = new JScrollPane(p);

        f = new JFrame();
        f.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                frameClicked++;
            }
        });
        f.getContentPane().add(scrollPane, BorderLayout.CENTER);
        ((JComponent) f.getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(50, 50, 50, 50));
        f.setSize(400, 400);
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }

    @Override
    protected boolean performTest() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    // prepare test data
                    frameClicked = 0;

                    b.requestFocus();

                    scrollPane.getHorizontalScrollBar().setUnitIncrement(40);
                    scrollPane.getVerticalScrollBar().setUnitIncrement(40);

                    hLoc = scrollPane.getHorizontalScrollBar().getLocationOnScreen();
                    hLoc.translate(scrollPane.getHorizontalScrollBar().getWidth() - 3, 3);
                    vLoc = scrollPane.getVerticalScrollBar().getLocationOnScreen();
                    vLoc.translate(3, scrollPane.getVerticalScrollBar().getHeight() - 3);

                    testLoc = p.getLocationOnScreen();
                    testLoc.translate(-3, -3);

                    resizeLoc = f.getLocationOnScreen();
                    resizeLoc.translate(f.getWidth() - 1, f.getHeight() - 1);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Problem preparing test GUI.");
        }
        // run robot
        Robot robot = Util.createRobot();
        robot.setAutoDelay(ROBOT_DELAY);

        robot.mouseMove(hLoc.x, hLoc.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        Util.waitForIdle(robot);

        robot.mouseMove(vLoc.x, vLoc.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        Util.waitForIdle(robot);

        clickAndBlink(robot, testLoc, false);
        robot.mouseMove(resizeLoc.x, resizeLoc.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseMove(resizeLoc.x + 5, resizeLoc.y + 5);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        Util.waitForIdle(robot);

        clickAndBlink(robot, testLoc, false);
        return frameClicked == 2;
    }

    // this strange plumbing stuff is required due to "Standard Test Machinery" in base class
    public static void main(String args[]) throws InterruptedException {
        instance = new ViewportOverlapping();
        OverlappingTestBase.doMain(args);
    }
}
