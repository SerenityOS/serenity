/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7042497
 * @summary  Verifies if JOptionPane.showInternalConfirmDialog
             throws RuntimeException if parentComponent argument is null
 * @run main/manual JOptionPaneConfirmDlgTest
 */

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class JOptionPaneConfirmDlgTest {
    JInternalFrame  textFrame;
    JFrame f = null;

    public static void main(String[] args) throws Exception{
            new JOptionPaneConfirmDlgTest();
    }

    public JOptionPaneConfirmDlgTest() throws Exception {

        try {
            SwingUtilities.invokeAndWait(()->createGUI());
            Thread.sleep(10000);
        } finally {
            SwingUtilities.invokeAndWait(()->f.dispose());
        }

    }

    public void createGUI() {
        JOptionPane.showMessageDialog(
            (Component) null,
            "An internalFrame with 2 buttons will be displayed. \n" +
           " Press \"Hit me 1\" button. The bug causes a RuntimeException to be thrown here\n" +
           " But If a confirmation dialog comes, test has passed\n" +
           " Similarly, press \"Hit me 2\" button. The bug will cause a RuntimeException\n" +
           " to be thrown here but if a confirmation dialog comes, test has passed.\n" +
           " Close the dialog and frame.",
            "information", JOptionPane.INFORMATION_MESSAGE);
        f = new JFrame();

        textFrame = new JInternalFrame("Main-Frame", true);
        f.setContentPane(textFrame);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        f.setSize(dim.width/6, dim.height/5);
        textFrame.setBounds(10, 10, dim.width/8, dim.height/8);

        textFrame.setVisible(true);

        JButton b1 = new JButton("Hit me 1");
        b1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showInternalConfirmDialog(null, "Test?");
            }});

        JButton b2 = new JButton("Hit me 2");
        b2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showInternalConfirmDialog(new JInternalFrame(), "Test?");
            }});

        textFrame.setLayout(new FlowLayout());
        textFrame.add(b1);
        textFrame.add(b2);
        f.setVisible(true);
    }
}
