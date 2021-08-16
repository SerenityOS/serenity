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


import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import test.java.awt.regtesthelpers.Util;

/**
 * AWT/Swing overlapping test for {@link javax.swing.JScrollPane } component.
 * <p>See base class for test info.
 */
/*
 * @test
 * @key headful
 * @summary Overlapping test for javax.swing.JScrollPane
 * @author sergey.grinev@oracle.com: area=awt.mixing
 * @library /java/awt/patchlib  ../../regtesthelpers
 * @modules java.desktop/sun.awt
 *          java.desktop/java.awt.peer
 * @build java.desktop/java.awt.Helper
 * @build Util
 * @run main JScrollPaneOverlapping
 */
public class JScrollPaneOverlapping extends OverlappingTestBase {

//    {testEmbeddedFrame = true;}

    private boolean horizontalClicked = false;
    private boolean verticalClicked = false;
    private Point hLoc;
    private Point vLoc;

    private JFrame f;
    private JPanel p;
    private JScrollPane scrollPane;

    protected void prepareControls() {

        f = new JFrame("JScrollPane");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(120, 120);

        p = new JPanel(new GridLayout(0, 1));

        scrollPane = new JScrollPane(p);
        scrollPane.setPreferredSize(new Dimension(300,300));
        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        scrollPane.getHorizontalScrollBar().setValue(1);
        scrollPane.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {

            public void adjustmentValueChanged(AdjustmentEvent e) {
                horizontalClicked = true;
            }
        });

        scrollPane.getVerticalScrollBar().setValue(1);
        scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {

            public void adjustmentValueChanged(AdjustmentEvent e) {
                verticalClicked = true;
            }
        });

        f.getContentPane().add(scrollPane);
        f.setVisible(true);
        propagateAWTControls(p);
//        JButton b = new JButton("Space extender");
//        b.setPreferredSize(new Dimension(150,150));
//        p.add( b );

        //b.requestFocus(); // to change the look of AWT component, especially Choice
    }

    @Override
    protected boolean performTest() {
        // run robot
        Robot robot = Util.createRobot();
        robot.setAutoDelay(ROBOT_DELAY);

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    hLoc = scrollPane.getHorizontalScrollBar().getLocationOnScreen();
                    vLoc = scrollPane.getVerticalScrollBar().getLocationOnScreen();
                }
            });
        } catch (Exception e) {
        }
        hLoc.translate(2, 2);
        vLoc.translate(2, 2);

        clickAndBlink(robot, hLoc, false);
        clickAndBlink(robot, vLoc, false);

        return horizontalClicked && verticalClicked;
    }

    // this strange plumbing stuff is required due to "Standard Test Machinery" in base class
    public static void main(String args[]) throws InterruptedException {
        instance = new JScrollPaneOverlapping();
        OverlappingTestBase.doMain(args);
    }
}
