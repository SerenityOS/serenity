/*
 * Copyright (c) 2004, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @bug     4529206
 * @summary JToolBar - setFloating does not work correctly
 * @author  Konstantin Eremin
 * @run     main bug4529206
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class bug4529206 extends JFrame {
    static JFrame frame;
    static JToolBar jToolBar1;
    public bug4529206() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel jPanFrame = (JPanel) this.getContentPane();
        jPanFrame.setLayout(new BorderLayout());
        this.setSize(new Dimension(200, 100));
        this.setLocation(125, 75);
        this.setTitle("Test Floating Toolbar");
        jToolBar1 = new JToolBar();
        JButton jButton1 = new JButton("Float");
        jPanFrame.add(jToolBar1, BorderLayout.NORTH);
        JTextField tf = new JTextField("click here");
        jPanFrame.add(tf);
        jToolBar1.add(jButton1, null);
        jButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                buttonPressed(e);
            }
        });
        makeToolbarFloat();
        setVisible(true);
    }

    private void makeToolbarFloat() {
        javax.swing.plaf.basic.BasicToolBarUI ui = (javax.swing.plaf.basic.BasicToolBarUI) jToolBar1.getUI();
        if (!ui.isFloating()) {
            ui.setFloatingLocation(100, 100);
            ui.setFloating(true, jToolBar1.getLocation());
        }
    }

    private void buttonPressed(ActionEvent e) {
        makeToolbarFloat();
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new bug4529206();
            }
        });
        Robot robot = new Robot();
        robot.waitForIdle();
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                if (frame.isFocused()) {
                    throw (new RuntimeException("setFloating does not work correctly"));
                }
            }
        });
    }
}
