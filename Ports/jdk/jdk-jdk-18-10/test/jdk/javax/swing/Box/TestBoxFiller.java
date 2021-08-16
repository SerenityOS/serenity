/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 8253016
   @key headful
   @summary Verifies Box.Filler components should be unfocusable by default
   @run main TestBoxFiller
 */
import java.awt.ContainerOrderFocusTraversalPolicy;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Robot;
import java.awt.event.KeyEvent;

public class TestBoxFiller
{
    private static JFrame frame;
    private static void showFocusOwner(PropertyChangeEvent e)
    {
        Object c = e.getNewValue();
        if (c instanceof Box.Filler) {
            throw new RuntimeException("Box.Filler having focus");
        }
    }

    public static void main(String[] args) throws Exception
    {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(100);
            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame();
                KeyboardFocusManager m = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                m.addPropertyChangeListener("focusOwner", TestBoxFiller::showFocusOwner);

                Box box = Box.createHorizontalBox();
                JTextField tf1 = new JTextField("Test");
                tf1.setColumns(40);
                JTextField tf2 = new JTextField("Test");
                tf2.setColumns(40);
                box.add(tf1);
                box.add(Box.createHorizontalStrut(20));
                box.add(tf2);
                frame.setContentPane(box);
                frame.setFocusTraversalPolicy(new ContainerOrderFocusTraversalPolicy());
                frame.pack();
                frame.setVisible(true);
                frame.setLocationRelativeTo(null);
                tf1.requestFocusInWindow();
            });
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_TAB);
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(frame::dispose);
            }
        }
    }
}
