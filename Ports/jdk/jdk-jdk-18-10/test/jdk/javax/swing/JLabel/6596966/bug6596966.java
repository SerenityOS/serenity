/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6596966
 * @summary Some JFileChooser mnemonics do not work with sticky keys
 * @library ../../regtesthelpers
 * @library /test/lib
 * @build Util jdk.test.lib.Platform
 * @run main bug6596966
 * @author Pavel Porvatov
 */

import java.awt.EventQueue;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import jdk.test.lib.Platform;

public class bug6596966 {
    private static JFrame frame;

    private static JLabel label;
    private static JButton button;
    private static JComboBox comboBox;

    public static void main(String[] args) throws Exception {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(100);

            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    button = new JButton("Button");
                    comboBox = new JComboBox();

                    label = new JLabel("Label");
                    label.setDisplayedMnemonic('L');
                    label.setLabelFor(comboBox);

                    JPanel pnContent = new JPanel();

                    pnContent.add(button);
                    pnContent.add(label);
                    pnContent.add(comboBox);

                    frame = new JFrame();

                    frame.add(pnContent);
                    frame.pack();
                    frame.setVisible(true);
                }
            });

            robot.waitForIdle();
            robot.delay(1000);

            int keyMask = InputEvent.ALT_MASK;
            if (Platform.isOSX()) {
                keyMask = InputEvent.CTRL_MASK | InputEvent.ALT_MASK;
            }
            ArrayList<Integer> keys = Util.getKeyCodesFromKeyMask(keyMask);
            for (int i = 0; i < keys.size(); ++i) {
                robot.keyPress(keys.get(i));
            }

            robot.keyPress(KeyEvent.VK_L);

            robot.waitForIdle();
            Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new KeyEvent(label, KeyEvent.KEY_RELEASED,
                    EventQueue.getMostRecentEventTime(), 0, KeyEvent.VK_L, 'L'));

            robot.waitForIdle();

            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        if (!comboBox.isFocusOwner()) {
                            throw new RuntimeException("comboBox isn't focus owner");
                        }
                    }
                });
            } finally {
                robot.keyRelease(KeyEvent.VK_L);
                for (int i = 0; i < keys.size(); ++i) {
                    robot.keyRelease(keys.get(i));
                }
                robot.waitForIdle();
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }
}
