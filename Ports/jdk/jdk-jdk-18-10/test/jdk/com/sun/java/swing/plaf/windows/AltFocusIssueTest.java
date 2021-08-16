/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8211987
 * @key headful
 * @requires (os.family == "windows")
 * @summary Verify if Menu bar gets input focus even if Alt-released event is consumed.
 * @modules java.desktop/com.sun.java.swing.plaf.windows
 * @run main AltFocusIssueTest
 */

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.awt.Robot;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Try to demonstrate the wrong behavior
 */
public class AltFocusIssueTest {

    /**
     * Menu inside menu bar of the frame
     */
    private static JMenu menu;

    /**
     * Text area to test on.
     */
    private static JTextArea ta;

    private static JFrame frame;

    /**
     * Test that the text area loses input focus although Alt-released event is consumed.
     *
     * @throws InterruptedException
     * @throws InvocationTargetException
     */
    public static void testAltEvents() throws Exception {
        Robot robot = new Robot();
        SwingUtilities.invokeAndWait(() -> {
            try {
                createUI();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        robot.waitForIdle();
        SwingUtilities.invokeAndWait(() -> ta.requestFocusInWindow());
        robot.waitForIdle();
        if (!ta.isFocusOwner()) {
            throw new RuntimeException("textarea should have input focus");
        }
        if (menu.isSelected()) {
            throw new RuntimeException("menu is selected...");
        }

        // Simulate an Alt-typed event
        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyRelease(KeyEvent.VK_ALT);
        robot.waitForIdle();

        // Since the event is consumed, I expect the input focus to be in the text area
        if (!ta.isFocusOwner()) {
            throw new RuntimeException("textarea should still have input focus");
        }
        // OR
        if (SwingUtilities.getRootPane(ta).isFocusOwner()) {
            throw new RuntimeException("Focus should not be changed from the text area");
        }
        // OR
        if (menu.isSelected()) {
            throw new RuntimeException("Menu must not be selected");
        }
    }

    /**
     * Builds UI to test.
     *
     */
    private static void createUI() throws Exception {
        // Install Windows L&F
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        menu = new JMenu("Menu");
        menu.add(new JMenuItem("Menu item"));
        menuBar.add(menu);

        ta = new JTextArea();
        frame.getContentPane().add(ta);

        ta.addKeyListener( new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ALT) {
                    /*
                     * This is where I need to do special handling of the Alt-released event.
                     * After, nobody else must react to this event, thus I consume it.
                     */
                    e.consume();
                }
            }
        });

        frame.setSize(400, 300);
        frame.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        try {
            testAltEvents();
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(() -> frame.dispose());
            }
        }
    }
}
