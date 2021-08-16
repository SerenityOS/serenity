/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8061636
 * @summary fix for 7079254 changes behavior of MouseListener, MouseMotionListener
 * @library ../../regtesthelpers
 * @build Util
 * @author Alexander Zvegintsev
 * @run main RemovedComponentMouseListener
 */

import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import test.java.awt.regtesthelpers.Util;

public class RemovedComponentMouseListener extends JFrame {

    static boolean mouseReleasedReceived;
    static JButton button;

    public RemovedComponentMouseListener() {
        JPanel panel = new JPanel();
        JPanel buttonPanel = new JPanel();
        button = new JButton("Button");

        setSize(300, 300);

        buttonPanel.add(button);
        panel.add(buttonPanel);
        setContentPane(panel);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                buttonPanel.remove(button);
                panel.add(button);
                button.revalidate();
                button.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mouseReleasedReceived = true;
            }
        });

        setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            new RemovedComponentMouseListener();
        });

        Robot r = Util.createRobot();
        r.setAutoDelay(100);
        r.waitForIdle();
        Util.pointOnComp(button, r);

        r.waitForIdle();
        r.mousePress(InputEvent.BUTTON1_MASK);
        r.waitForIdle();
        r.mouseRelease(InputEvent.BUTTON1_MASK);
        r.waitForIdle();
        if (!mouseReleasedReceived) {
            throw new RuntimeException("mouseReleased event was not received");
        }
    }
}
