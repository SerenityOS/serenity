/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import test.java.awt.regtesthelpers.Util;

/**
 * AWT/Swing overlapping test for {@link javax.swing.JSplitPane } component.
 * <p>This test puts heavyweight and lightweight components into different
 * panels and test if splitter image and components itself are drawn correctly.
 * <p>See base class for test info.
 */
/*
 * @test
 * @key headful
 * @bug 6986109
 * @summary Overlapping test for javax.swing.JSplitPane
 * @author sergey.grinev@oracle.com: area=awt.mixing
 * @library /java/awt/patchlib  ../../regtesthelpers
 * @modules java.desktop/sun.awt
 *          java.desktop/java.awt.peer
 * @build java.desktop/java.awt.Helper
 * @build Util
 * @run main JSplitPaneOverlapping
 */
public class JSplitPaneOverlapping extends OverlappingTestBase {

    private boolean clicked = false;
    private Point splitterLoc;
    private JScrollPane sp1;
    private JScrollPane sp2;

    protected void prepareControls() {
        JFrame frame = new JFrame("SplitPane Mixing");
        JPanel p = new JPanel(new GridLayout());
        p.setPreferredSize(new Dimension(500, 500));
        propagateAWTControls(p);
        sp1 = new JScrollPane(p);

        JButton button = new JButton("JButton");
        button.setBackground(Color.RED);
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                clicked = true;
            }
        });
        sp2 = new JScrollPane(button);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sp1, sp2);
        splitPane.setOneTouchExpandable(false);
        splitPane.setDividerLocation(150);

        splitPane.setPreferredSize(new Dimension(400, 200));

        frame.getContentPane().add(splitPane);
        frame.pack();
        frame.setVisible(true);
    }

    private static final boolean ignoreFail = false;

    @Override
    protected boolean performTest() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    splitterLoc = sp2.getLocationOnScreen();
                    Point leftLoc = sp1.getLocationOnScreen();
                    leftLoc.translate(sp1.getWidth(), 0);
                    splitterLoc.translate(-(splitterLoc.x - leftLoc.x) / 2, 30);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Where is splitter?");
        }
        // run robot
        Robot robot = Util.createRobot();
        robot.setAutoDelay(ROBOT_DELAY);

        robot.mouseMove(splitterLoc.x, splitterLoc.y);
        Util.waitForIdle(robot);

        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseMove(splitterLoc.x - 50, splitterLoc.y);
        Color c = robot.getPixelColor(splitterLoc.x - 50, splitterLoc.y);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        System.out.println("Actual: "+c+", (not) expected: "+AWT_VERIFY_COLOR+" at "+(splitterLoc.x - 50)+", "+ splitterLoc.y);
        if (!ignoreFail && c.equals(AWT_VERIFY_COLOR)) {
            fail("The JSplitPane drag-n-drop image did not pass pixel color check and is overlapped");
        }
        clickAndBlink(robot, splitterLoc);

        return clicked;
    }

    // this strange plumbing stuff is required due to "Standard Test Machinery" in base class
    public static void main(String args[]) throws InterruptedException {
        instance = new JSplitPaneOverlapping();
        OverlappingTestBase.doMain(args);
    }
}
