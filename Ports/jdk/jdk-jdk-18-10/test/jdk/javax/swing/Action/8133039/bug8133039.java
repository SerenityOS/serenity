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

import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import sun.swing.UIAction;

/**
 * @test
 * @key headful
 * @bug 8133039
 * @summary Provide public API to sun.swing.UIAction#isEnabled(Object)
 * @modules java.desktop/sun.swing
 * @author Alexander Scherbatiy
 */
public class bug8133039 {

    private static volatile int ACTION_PERFORMED_CALLS = 0;
    private static volatile int ACTION_ACCEPTED_CALLS = 0;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            testActionNotification();
            testPopupAction();
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    private static void testActionNotification() {

        KeyEvent keyEvent = new KeyEvent(new JLabel("Test"), 0, 0, 0, 0, 'A');
        SenderObject rejectedSenderObject = new SenderObject();
        SwingUtilities.notifyAction(new TestAction(false), null, keyEvent,
                rejectedSenderObject, 0);

        if (rejectedSenderObject.accepted) {
            throw new RuntimeException("The sender is incorrectly accepted!");
        }

        if (rejectedSenderObject.performed) {
            throw new RuntimeException("The action is incorrectly performed!");
        }

        SenderObject acceptedSenderObject = new SenderObject();
        SwingUtilities.notifyAction(new TestAction(true), null, keyEvent,
                acceptedSenderObject, 0);

        if (!acceptedSenderObject.accepted) {
            throw new RuntimeException("The sender is not accepted!");
        }

        if (!acceptedSenderObject.performed) {
            throw new RuntimeException("The action is not performed!");
        }
    }

    private static void testPopupAction() throws Exception {

        SwingUtilities.invokeAndWait(bug8133039::createAndShowGUI);

        Robot robot = new Robot();
        robot.setAutoDelay(100);
        robot.waitForIdle();

        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        robot.waitForIdle();

        if (ACTION_ACCEPTED_CALLS != 1) {
            throw new RuntimeException("Method accept is not invoked!");
        }

        if (ACTION_PERFORMED_CALLS != 1) {
            throw new RuntimeException("Method actionPerformed is not invoked!");
        }

        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        robot.waitForIdle();

        if (ACTION_ACCEPTED_CALLS != 2) {
            throw new RuntimeException("Method accept is not invoked!");
        }

        if (ACTION_PERFORMED_CALLS != 1) {
            throw new RuntimeException("Method actionPerformed is invoked twice!");
        }
    }

    private static void createAndShowGUI() {

        frame = new JFrame();
        frame.setSize(300, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JComboBox<String> comboBox = new JComboBox<>(new String[]{"1", "2", "3"});

        Action showPopupAction = new ShowPopupAction();
        comboBox.getInputMap().put(KeyStroke.getKeyStroke("A"), "showPopup");
        comboBox.getActionMap().put("showPopup", showPopupAction);

        frame.getContentPane().add(comboBox);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static class ShowPopupAction extends UIAction {

        public ShowPopupAction() {
            super("showPopup");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ACTION_PERFORMED_CALLS++;
            Object src = e.getSource();
            if (src instanceof JComboBox) {
                ((JComboBox) src).showPopup();
            }
        }

        @Override
        public boolean accept(Object sender) {
            ACTION_ACCEPTED_CALLS++;
            if (sender instanceof JComboBox) {
                JComboBox c = (JComboBox) sender;
                return !c.isPopupVisible();
            }
            return false;
        }
    }

    private static class SenderObject {

        private boolean accepted;
        private boolean performed;
    }

    private static class TestAction extends AbstractAction {

        private final boolean acceptSender;

        public TestAction(boolean acceptSender) {
            this.acceptSender = acceptSender;
        }

        @Override
        public boolean accept(Object sender) {
            ((SenderObject) sender).accepted = acceptSender;
            return acceptSender;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ((SenderObject) e.getSource()).performed = true;
        }
    }
}
