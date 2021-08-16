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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import test.java.awt.regtesthelpers.Util;


/**
 * AWT/Swing overlapping test for {@link javax.swing.JCombobox } component.
 * <p>This test creates combobox and test if heavyweight component is drawn correctly then dropdown is shown.
 * <p>See base class for details.
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
 * @run main JComboBoxOverlapping
 */
public class JComboBoxOverlapping extends OverlappingTestBase {

    private boolean lwClicked = false;
    private Point loc;
    private Point loc2;
    private JComboBox cb;
    private JFrame frame;

    {testEmbeddedFrame = true;}

    protected void prepareControls() {
        frame = new JFrame("Mixing : Dropdown Overlapping test");
        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        frame.setSize(200, 200);
        frame.setVisible(true);

        cb = new JComboBox(petStrings);
        cb.setPreferredSize(new Dimension(frame.getContentPane().getWidth(), 20));
        cb.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == cb) {
                    lwClicked = true;
                }
            }
        });

        frame.add(cb);
        propagateAWTControls(frame);
        frame.setVisible(true);
    }

    @Override
    protected boolean performTest() {
        // run robot
        Robot robot = Util.createRobot();
        robot.setAutoDelay(ROBOT_DELAY);
        robot.waitForIdle();
        robot.delay(200);
        try {
            SwingUtilities.invokeAndWait(() -> {
                loc = cb.getLocationOnScreen();
                loc2 = frame.getContentPane().getLocationOnScreen();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        loc2.translate(75, 75);
        pixelPreCheck(robot, loc2, currentAwtControl);

        loc.translate(3, 3);
        clickAndBlink(robot, loc, false);

        clickAndBlink(robot, loc2, false);

        return lwClicked;
    }

    // this strange plumbing stuff is required due to "Standard Test Machinery" in base class
    public static void main(String args[]) throws InterruptedException {
        instance = new JComboBoxOverlapping();
        OverlappingTestBase.doMain(args);
    }
}
