/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.AWTException;
import java.awt.IllegalComponentStateException;
import java.awt.event.InputEvent;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.table.DefaultTableModel;

/**
 * @test
 * @key headful
 * @bug 6219960
 * @summary null reference in ToolTipManager
 * @run main bug6219960
 */
public class bug6219960 {

    private static final String QUESTION = "Question";

    static volatile JFrame frame;
    static JTable table;

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        SwingUtilities.invokeAndWait(bug6219960::createAndShowGUI);
        robot.waitForIdle();
        showModal("The tooltip should be showing. Press ok with mouse. And don't move it.");
        robot.waitForIdle();
        showModal("Now press ok and move the mouse inside the table (don't leave it).");
        robot.waitForIdle();
    }

    private static void createAndShowGUI() {
        ToolTipManager.sharedInstance().setDismissDelay(10 * 60 * 1000);
        frame = new JFrame();
        frame.setLocation(20, 20);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JDesktopPane desk = new JDesktopPane();
        JInternalFrame iframe = new JInternalFrame();
        iframe.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
        desk.add(iframe);
        JButton save = new JButton();
        save.setToolTipText("Wait for dialog to show.");
        save.setText("Wait for the tooltip to show.");
        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(save);
        table = createTable();
        panel.add(new JScrollPane(table));
        iframe.setContentPane(panel);
        frame.getContentPane().add(desk);
        frame.setSize(800, 600);
        iframe.setSize(640, 480);
        iframe.validate();
        iframe.setVisible(true);
        frame.validate();
        frame.setVisible(true);
        try {
            iframe.setSelected(true);
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        try {
            Robot robot = new Robot();
            Rectangle bounds = frame.getBounds();
            int centerX = (int) (bounds.getX() + bounds.getWidth() / 6);
            int centerY = (int) (bounds.getY() + bounds.getHeight() / 6);
            robot.mouseMove(centerX, centerY);
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    private static void showModal(final String msg) throws Exception {

        new Thread(() -> {

            int timeout = 3000;
            long endTime = System.currentTimeMillis() + timeout;

            while (System.currentTimeMillis() <= endTime) {
                if (pressOK(frame)) {
                    return;
                }
            }
            throw new RuntimeException("Internal frame has not been found!");
        }).start();

        Thread.sleep(900);

        SwingUtilities.invokeAndWait(() -> {
            JOptionPane.showInternalMessageDialog(table, msg,
                    QUESTION,
                    JOptionPane.PLAIN_MESSAGE);
        });
    }

    private static JTable createTable() {
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        return table;
    }

    private static boolean pressOK(Component comp) {

        JInternalFrame internalFrame
                = findModalInternalFrame(comp, QUESTION);

        if (internalFrame == null) {
            return false;
        }

        JButton button = (JButton) findButton(internalFrame);

        if (button == null) {
            return false;
        }

        try {
            Robot robot = new Robot();
            Point location = button.getLocationOnScreen();
            Rectangle bounds = button.getBounds();
            int centerX = (int) (location.getX() + bounds.getWidth() / 2);
            int centerY = (int) (location.getY() + bounds.getHeight() / 2);
            robot.mouseMove(centerX, centerY);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
        } catch (IllegalComponentStateException ignore) {
            return false;
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    private static JInternalFrame findModalInternalFrame(Component comp, String title) {

        if (comp instanceof JInternalFrame) {
            JInternalFrame internalFrame = (JInternalFrame) comp;
            if (internalFrame.getTitle().equals(title)) {
                return (JInternalFrame) comp;
            }
        }

        if (comp instanceof Container) {
            Container cont = (Container) comp;
            for (int i = 0; i < cont.getComponentCount(); i++) {
                JInternalFrame result = findModalInternalFrame(cont.getComponent(i), title);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static JButton findButton(Component comp) {

        if (comp instanceof JButton) {
            return (JButton) comp;
        }

        if (comp instanceof Container) {
            Container cont = (Container) comp;
            for (int i = 0; i < cont.getComponentCount(); i++) {
                JButton result = findButton(cont.getComponent(i));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
