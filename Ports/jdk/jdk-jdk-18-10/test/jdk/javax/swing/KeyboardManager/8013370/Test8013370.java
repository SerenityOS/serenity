/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeAndWait;

/*
 * @test
 * @key headful
 * @bug 8013370
 * @summary Ensure that key stroke is not null
 * @author Sergey Malenkov
 */

public class Test8013370 implements Runnable {
    public static void main(String[] args) throws Exception {
        Test8013370 task = new Test8013370();
        invokeAndWait(task);

        Robot robot = new Robot();
        robot.waitForIdle();
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.waitForIdle();

        invokeAndWait(task);
        task.validate();
    }

    private JFrame frame;
    private boolean error;

    @Override
    public void run() {
        if (this.frame == null) {
            JMenuBar menu = new JMenuBar() {
                @Override
                protected boolean processKeyBinding(KeyStroke stroke, KeyEvent event, int condition, boolean pressed) {
                    if (stroke == null) {
                        Test8013370.this.error = true;
                        return false;
                    }
                    return super.processKeyBinding(stroke, event, condition, pressed);
                }
            };
            menu.add(new JMenuItem("Menu"));

            InputMap map = menu.getInputMap(WHEN_IN_FOCUSED_WINDOW);
            // We add exactly 10 actions because the ArrayTable is converted
            // from a array to a hashtable when more than 8 values are added.
            for (int i = 0; i < 9; i++) {
                String name = " Action #" + i;
                map.put(KeyStroke.getKeyStroke(KeyEvent.VK_A + i, CTRL_DOWN_MASK), name);

                menu.getActionMap().put(name, new AbstractAction(name) {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        showMessageDialog(null, getValue(NAME));
                    }
                });
            }
            this.frame = new JFrame("8013370");
            this.frame.setJMenuBar(menu);
            this.frame.setVisible(true);
        }
        else {
            this.frame.dispose();
        }
    }

    private void validate() {
        if (this.error) {
            throw new Error("KeyStroke is null");
        }
    }
}
