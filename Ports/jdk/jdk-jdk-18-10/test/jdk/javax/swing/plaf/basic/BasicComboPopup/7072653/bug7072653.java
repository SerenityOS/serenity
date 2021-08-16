/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7072653 8144161 8176448
 * @summary JComboBox popup mispositioned if its height exceeds the screen height
 * @run main bug7072653
 */

import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class bug7072653 {

    private static JComboBox combobox;
    private static JFrame frame;
    private static Robot robot;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        UIManager.LookAndFeelInfo[] lookAndFeelArray =
                UIManager.getInstalledLookAndFeels();
        for (GraphicsDevice sd : ge.getScreenDevices()) {
            for (UIManager.LookAndFeelInfo lookAndFeelItem : lookAndFeelArray) {
                executeCase(lookAndFeelItem.getClassName(), sd);
                robot.waitForIdle();
            }
        }
    }

    private static void executeCase(String lookAndFeelString, GraphicsDevice sd)
            throws Exception {
        if (tryLookAndFeel(lookAndFeelString)) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    setup(lookAndFeelString, sd);
                    test();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                } finally {
                    frame.dispose();
                }
            });
        }
    }

    private static void setup(String lookAndFeelString, GraphicsDevice sd)
            throws Exception {
        GraphicsConfiguration gc = sd.getDefaultConfiguration();
        Rectangle gcBounds = gc.getBounds();
        frame = new JFrame("JComboBox Test " + lookAndFeelString, gc);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);
        frame.getContentPane().setLayout(new FlowLayout());
        frame.setLocation(
                gcBounds.x + gcBounds.width / 2 - frame.getWidth() / 2,
                gcBounds.y + gcBounds.height / 2 - frame.getHeight() / 2);

        combobox = new JComboBox(new DefaultComboBoxModel() {
            @Override
            public Object getElementAt(int index) {
                return "Element " + index;
            }

            @Override
            public int getSize() {
                return 400;
            }
        });

        combobox.setMaximumRowCount(400);
        combobox.putClientProperty("JComboBox.isPopDown", true);
        frame.getContentPane().add(combobox);
        frame.setVisible(true);
        robot.delay(3000); // wait some time to stabilize the size of the
                           // screen insets after the window is shown
        combobox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                int height = 0;
                for (Window window : JFrame.getWindows()) {
                    if (Window.Type.POPUP == window.getType()) {
                        if (window.getOwner().isVisible()) {
                            height = window.getSize().height;
                            break;
                        }
                    }
                }
                GraphicsConfiguration gc = combobox.getGraphicsConfiguration();
                Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
                int gcHeight = gc.getBounds().height;
                gcHeight = gcHeight - insets.top - insets.bottom;
                if (height == gcHeight) {
                    return;
                }

                String exception = "Popup window height "
                        + "For LookAndFeel" + lookAndFeelString + " is wrong"
                        + "\nShould be " + gcHeight + ", Actually " + height;
                throw new RuntimeException(exception);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }

        });

    }

    private static void test() throws Exception {
        combobox.setPopupVisible(true);
        combobox.setPopupVisible(false);
    }

    private static boolean tryLookAndFeel(String lookAndFeelString)
            throws Exception {
        try {
            UIManager.setLookAndFeel(
                    lookAndFeelString);

        } catch (UnsupportedLookAndFeelException
                | ClassNotFoundException
                | InstantiationException
                | IllegalAccessException e) {
            return false;
        }
        return true;
    }
}
