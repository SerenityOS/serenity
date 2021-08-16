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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * @test
 * @bug 4245587 4474813 4425878 4767478 8015599
 * @key headful
 * @author Mark Davidson
 * @summary Tests the location of the heavy weight popup portion of JComboBox,
 * JMenu and JPopupMenu.
 * @library ../regtesthelpers
 * @build Util
 * @run main TaskbarPositionTest
 */
public class TaskbarPositionTest extends JFrame implements ActionListener {

    private boolean done;
    private Throwable error;
    private static TaskbarPositionTest test;
    private static JPopupMenu popupMenu;
    private static JPanel panel;
    private static JComboBox<String> combo1;
    private static JComboBox<String> combo2;
    private static JMenuBar menubar;
    private static JMenu menu1;
    private static JMenu menu2;
    private static Rectangle fullScreenBounds;
    // The usable desktop space: screen size - screen insets.
    private static Rectangle screenBounds;
    private static String[] numData = {
        "One", "Two", "Three", "Four", "Five", "Six", "Seven"
    };
    private static String[] dayData = {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };
    private static char[] mnDayData = {
        'M', 'T', 'W', 'R', 'F', 'S', 'U'
    };

    public TaskbarPositionTest() {
        super("Use CTRL-down to show a JPopupMenu");
        setContentPane(panel = createContentPane());
        setJMenuBar(createMenuBar("1 - First Menu", true));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // CTRL-down will show the popup.
        panel.getInputMap().put(KeyStroke.getKeyStroke(
                KeyEvent.VK_DOWN, InputEvent.CTRL_MASK), "OPEN_POPUP");
        panel.getActionMap().put("OPEN_POPUP", new PopupHandler());

        pack();

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        fullScreenBounds = new Rectangle(new Point(), toolkit.getScreenSize());
        screenBounds = new Rectangle(new Point(), toolkit.getScreenSize());

        // Place the frame near the bottom. This is a pretty wild guess.
        this.setLocation(0, (int) screenBounds.getHeight() - 2 * this.getHeight());

        // Reduce the screen bounds by the insets.
        GraphicsConfiguration gc = this.getGraphicsConfiguration();
        if (gc != null) {
            Insets screenInsets = toolkit.getScreenInsets(gc);
            screenBounds = gc.getBounds();
            screenBounds.width -= (screenInsets.left + screenInsets.right);
            screenBounds.height -= (screenInsets.top + screenInsets.bottom);
            screenBounds.x += screenInsets.left;
            screenBounds.y += screenInsets.top;
        }

        setVisible(true);
    }

    public static class ComboPopupCheckListener implements PopupMenuListener {

        public void popupMenuCanceled(PopupMenuEvent ev) {
        }

        public void popupMenuWillBecomeVisible(PopupMenuEvent ev) {
        }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent ev) {
            Point cpos = combo1.getLocation();
            SwingUtilities.convertPointToScreen(cpos, panel);

            JPopupMenu pm = (JPopupMenu) combo1.getUI().getAccessibleChild(combo1, 0);

            if (pm != null) {
                Point p = pm.getLocation();
                SwingUtilities.convertPointToScreen(p, pm);
                if (p.y < cpos.y) {
                    throw new RuntimeException("ComboBox popup is wrongly aligned");
                }  // check that popup was opened down
            }
        }
    }

    private class PopupHandler extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            if (!popupMenu.isVisible()) {
                popupMenu.show((Component) e.getSource(), 40, 40);
            }
            isPopupOnScreen(popupMenu, fullScreenBounds);
        }
    }

    class PopupListener extends MouseAdapter {

        private JPopupMenu popup;

        public PopupListener(JPopupMenu popup) {
            this.popup = popup;
        }

        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup.show(e.getComponent(), e.getX(), e.getY());
                isPopupOnScreen(popup, fullScreenBounds);
            }
        }
    }

    /**
     * Tests if the popup is on the screen.
     */
    public static void isPopupOnScreen(JPopupMenu popup, Rectangle checkBounds) {
        Dimension dim = popup.getSize();
        Point pt = new Point();
        SwingUtilities.convertPointToScreen(pt, popup);
        Rectangle bounds = new Rectangle(pt, dim);

        if (!SwingUtilities.isRectangleContainingRectangle(checkBounds, bounds)) {
            throw new RuntimeException("We do not match! " + checkBounds + " / " + bounds);
        }

    }

    private JPanel createContentPane() {
        JPanel panel = new JPanel();

        combo1 = new JComboBox<>(numData);
        panel.add(combo1);
        combo2 = new JComboBox<>(dayData);
        combo2.setEditable(true);
        panel.add(combo2);
        panel.setSize(300, 200);

        popupMenu = new JPopupMenu();
        JMenuItem item;
        for (int i = 0; i < dayData.length; i++) {
            item = popupMenu.add(new JMenuItem(dayData[i], mnDayData[i]));
            item.addActionListener(this);
        }
        panel.addMouseListener(new PopupListener(popupMenu));

        JTextField field = new JTextField("CTRL+down for Popup");
        // CTRL-down will show the popup.
        field.getInputMap().put(KeyStroke.getKeyStroke(
                KeyEvent.VK_DOWN, InputEvent.CTRL_MASK), "OPEN_POPUP");
        field.getActionMap().put("OPEN_POPUP", new PopupHandler());

        panel.add(field);

        return panel;
    }

    /**
     * @param str name of Menu
     * @param bFlag set mnemonics on menu items
     */
    private JMenuBar createMenuBar(String str, boolean bFlag) {
        menubar = new JMenuBar();

        menu1 = new JMenu(str);
        menu1.setMnemonic(str.charAt(0));
        menu1.addActionListener(this);

        menubar.add(menu1);
        for (int i = 0; i < 8; i++) {
            JMenuItem menuitem = new JMenuItem("1 JMenuItem" + i);
            menuitem.addActionListener(this);
            if (bFlag) {
                menuitem.setMnemonic('0' + i);
            }
            menu1.add(menuitem);
        }

        // second menu
        menu2 = new JMenu("2 - Second Menu");
        menu2.addActionListener(this);
        menu2.setMnemonic('2');

        menubar.add(menu2);
        for (int i = 0; i < 5; i++) {
            JMenuItem menuitem = new JMenuItem("2 JMenuItem" + i);
            menuitem.addActionListener(this);

            if (bFlag) {
                menuitem.setMnemonic('0' + i);
            }
            menu2.add(menuitem);
        }
        JMenu submenu = new JMenu("Sub Menu");
        submenu.setMnemonic('S');
        submenu.addActionListener(this);
        for (int i = 0; i < 5; i++) {
            JMenuItem menuitem = new JMenuItem("S JMenuItem" + i);
            menuitem.addActionListener(this);
            if (bFlag) {
                menuitem.setMnemonic('0' + i);
            }
            submenu.add(menuitem);
        }
        menu2.add(new JSeparator());
        menu2.add(submenu);

        return menubar;
    }

    public void actionPerformed(ActionEvent evt) {
        Object obj = evt.getSource();
        if (obj instanceof JMenuItem) {
            // put the focus on the noneditable combo.
            combo1.requestFocus();
        }
    }

    public static void main(String[] args) throws Throwable {


        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                test = new TaskbarPositionTest();
            }
        });

        // Use Robot to automate the test
        Robot robot;
        robot = new Robot();
        robot.setAutoDelay(125);

        // 1 - menu
        Util.hitMnemonics(robot, KeyEvent.VK_1);

        robot.waitForIdle();
        isPopupOnScreen(menu1.getPopupMenu(), screenBounds);

        // 2 menu with sub menu
        robot.keyPress(KeyEvent.VK_RIGHT);
        robot.keyRelease(KeyEvent.VK_RIGHT);
        Util.hitMnemonics(robot, KeyEvent.VK_S);

        robot.waitForIdle();
        isPopupOnScreen(menu2.getPopupMenu(), screenBounds);

        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);

        // Focus should go to non editable combo box
        robot.waitForIdle();
        Thread.sleep(500);

        robot.keyPress(KeyEvent.VK_DOWN);

        // How do we check combo boxes?

        // Editable combo box
        robot.keyPress(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_TAB);
        robot.keyPress(KeyEvent.VK_DOWN);
        robot.keyRelease(KeyEvent.VK_DOWN);

        // combo1.getUI();

        // Popup from Text field
        robot.keyPress(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_TAB);
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_DOWN);
        robot.keyRelease(KeyEvent.VK_DOWN);
        robot.keyRelease(KeyEvent.VK_CONTROL);

        // Popup from a mouse click.
        Point pt = new Point(2, 2);
        SwingUtilities.convertPointToScreen(pt, panel);
        robot.mouseMove((int) pt.getX(), (int) pt.getY());
        robot.mousePress(InputEvent.BUTTON3_MASK);
        robot.mouseRelease(InputEvent.BUTTON3_MASK);

        robot.waitForIdle();
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                test.setLocation(-30, 100);
                combo1.addPopupMenuListener(new ComboPopupCheckListener());
                combo1.requestFocus();
            }
        });

        robot.keyPress(KeyEvent.VK_DOWN);
        robot.keyRelease(KeyEvent.VK_DOWN);
        robot.keyPress(KeyEvent.VK_ESCAPE);
        robot.keyRelease(KeyEvent.VK_ESCAPE);

        robot.waitForIdle();
        Thread.sleep(500);
    }
}
