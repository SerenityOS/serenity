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
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import test.java.awt.regtesthelpers.Util;

/**
 * AWT/Swing overlapping test for {@link javax.swing.JPopupMenu } component.
 * <p>This test creates menu and test if heavyweight component is drawn correctly then menu dropdown is shown.
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
 * @run main JPopupMenuOverlapping
 */
public class JPopupMenuOverlapping extends OverlappingTestBase {

    {testEmbeddedFrame = true;}

    private boolean lwClicked = false;
    private Point loc;
    private JPopupMenu popup;
    private JFrame frame=null;

    protected void prepareControls() {
        if(frame != null) {
            frame.setVisible(false);
        }
        frame = new JFrame("Mixing : Dropdown Overlapping test");
        frame.setLayout(new SpringLayout());
        frame.setSize(200, 200);

        popup = new JPopupMenu();
        ActionListener menuListener = new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                lwClicked = true;
            }
        };
        JMenuItem item;
        for (int i = 0; i < petStrings.length; i++) {
            popup.add(item = new JMenuItem(petStrings[i]));
            item.addActionListener(menuListener);
        }
        propagateAWTControls(frame);
        frame.setVisible(true);
        loc = frame.getContentPane().getLocationOnScreen();
    }

    @Override
    protected boolean performTest() {
        // run robot
        Robot robot = Util.createRobot();
        robot.setAutoDelay(ROBOT_DELAY);

        loc.translate(75, 75);

        pixelPreCheck(robot, loc, currentAwtControl);

        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    popup.show(frame.getContentPane(), 15, 15);
                }
            });

            robot.waitForIdle();

            clickAndBlink(robot, loc, false);

            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    popup.setVisible(false);
                }
            });
        } catch (InterruptedException ex) {
            fail(ex.getMessage());
        } catch (InvocationTargetException ex) {
            fail(ex.getMessage());
        }

        return lwClicked;
    }

    // this strange plumbing stuff is required due to "Standard Test Machinery" in base class
    public static void main(String args[]) throws InterruptedException {
        instance = new JPopupMenuOverlapping();
        OverlappingTestBase.doMain(args);
    }
}
