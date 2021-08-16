/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8050478
 * @summary Cursor not updating correctly after closing a modal dialog.
 *    The root cause of the issue was the lack of a mouse exit event
 *    during displaying of a modal dialog.
 * @author Dmitry Markov
 * @library ../../regtesthelpers
 * @build Util
 * @run main ModalDialogEnterExitEventsTest
 */

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;

import test.java.awt.regtesthelpers.Util;

public class ModalDialogEnterExitEventsTest {
    private static volatile AtomicInteger mouseEnterCount = new AtomicInteger();
    private static volatile AtomicInteger mouseExitCount = new AtomicInteger();

    private static JFrame frame;
    private static JDialog dialog;
    private static JButton openButton;
    private static JButton closeButton;

    public static void main(String[] args) throws Exception {
        try {
            Robot robot = Util.createRobot();

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    createAndShowGUI();
                }
            });
            Util.waitForIdle(robot);

            Util.clickOnComp(frame, robot, 500);
            Util.waitForIdle(robot);

            mouseEnterCount.set(0);
            mouseExitCount.set(0);

            Util.clickOnComp(openButton, robot, 500);
            Util.waitForIdle(robot);
            Util.waitTillShown(dialog);
            if (mouseExitCount.get() != 1) {
                throw new RuntimeException("Test FAILED. Wrong number of "
                    + "MouseExited events = " + mouseExitCount.get());
            }

            Util.clickOnComp(closeButton, robot, 500);
            Util.waitForIdle(robot);
            robot.delay(200);
            if (mouseEnterCount.get() != 1) {
                throw new RuntimeException("Test FAILED. Wrong number of "
                    + "MouseEntered events = "+ mouseEnterCount.get());
            }
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(() -> frame.dispose());
            }
        }
    }

    private static void createAndShowGUI() {
        frame = new JFrame("ModalDialogEnterExitEventsTest");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout());
        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                mouseExitCount.getAndIncrement();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                mouseEnterCount.getAndIncrement();
            }
        });
        openButton = new JButton("Open Dialog");
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog = new JDialog(frame, "Modal Dialog", true);
                dialog.setLayout(new FlowLayout());
                closeButton = new JButton("Close");
                closeButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        dialog.dispose();
                    }
                });
                dialog.add(closeButton);
                dialog.setSize(200, 200);
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }
        });
        frame.add(openButton);
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setVisible(true);
    }
}

