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
 * @key headful
 * @bug 8173739
 * @summary  Verifies if JPopupMenu disappears on KeyEvent
 * @run main TestPopupMenu
 */
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyVetoException;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class TestPopupMenu {
    private JFrame frame;
    private JLabel label;
    private volatile Point p = null;
    private volatile Dimension d = null;

    public static void main(String[] args) throws Exception {
        new TestPopupMenu();
    }

    void blockTillDisplayed(JComponent comp) throws Exception {
        while (p == null) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    p = comp.getLocationOnScreen();
                    d = comp.getSize();
                });
            } catch (IllegalStateException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    public TestPopupMenu() throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(200);
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    createAndShowUI();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            blockTillDisplayed(label);
            robot.waitForIdle();
            robot.mouseMove(p.x + d.width/2, p.y + d.height/2);
            robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_U);
            robot.keyRelease(KeyEvent.VK_U);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            robot.waitForIdle();
            JPopupMenu popup = label.getComponentPopupMenu();
            if (popup != null && popup.isVisible()) {
                throw new RuntimeException("Popup is visible in wrong internal frame");
            }
        } finally {
            SwingUtilities.invokeAndWait(()->frame.dispose());
        }
    }

    private void createAndShowUI() throws Exception {
        frame = new JFrame();
        frame.setTitle("Test Frame");
        frame.setSize(800, 600);

        JDesktopPane pane = new JDesktopPane();
        TestInternalFrameWPopup testInternalFrame1 = new TestInternalFrameWPopup();
        pane.add(testInternalFrame1);

        testInternalFrame1.setVisible(true);
        JScrollPane scrollPane = new JScrollPane(pane);
        frame.getContentPane().add(scrollPane);
        testInternalFrame1.setMaximum(true);
        frame.getRootPane().registerKeyboardAction(e -> {
            TestInternalFrame testInternalFrame2 = new TestInternalFrame();
            pane.add(testInternalFrame2);
            try {
                testInternalFrame2.setMaximum(true);
            } catch (PropertyVetoException ex) {
                throw new RuntimeException(ex);
            }
            testInternalFrame2.setVisible(true);
        }, KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_MASK),
                                 JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        frame.setVisible(true);
    }

    /**
     * Background color Cyan
     */
    class TestInternalFrameWPopup extends JInternalFrame {

        TestInternalFrameWPopup() {
            jbInit();
        }

        private void jbInit() {
            setTitle("Test Internal Frame With Popup");
            setContentPane(getContainerPanel());
            setMaximizable(true);
            setClosable(true);
            setMinimumSize(new Dimension(500, 300));
            setSize(500, 300);
        }

        private JPanel getContainerPanel() {
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            label = new JLabel("Test Label");
            JPopupMenu popup = new JPopupMenu();
            JMenuItem menuItem1 = new JMenuItem("Item 1");
            JMenuItem menuItem2 = new JMenuItem("Item 2");
            JMenuItem menuItem3 = new JMenuItem("Item 3");
            JMenuItem menuItem4 = new JMenuItem("Item 4");
            JMenuItem menuItem5 = new JMenuItem("Item 5");
            menuItem1.setOpaque(false);
            menuItem2.setOpaque(false);
            menuItem3.setOpaque(false);
            menuItem4.setOpaque(false);
            menuItem5.setOpaque(false);
            popup.add(menuItem1);
            popup.add(menuItem2);
            popup.add(menuItem3);
            popup.add(menuItem4);
            popup.add(menuItem5);
            label.setComponentPopupMenu(popup);
            popup.setBackground(Color.CYAN);
            panel.add(label, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                    GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
            panel.setBackground(Color.CYAN);
            return panel;
        }
    }

    /**
     * Background color Gray
     *
     */
    class TestInternalFrame extends JInternalFrame {
        public TestInternalFrame() {
            jbInit();
        }

        private void jbInit() {
            setTitle("Test Internal Frame");
            getContentPane().setBackground(Color.GRAY);
            setMaximizable(true);
            setClosable(true);
            setMinimumSize(new Dimension(500, 300));
            setSize(500, 300);
        }
    }
}
