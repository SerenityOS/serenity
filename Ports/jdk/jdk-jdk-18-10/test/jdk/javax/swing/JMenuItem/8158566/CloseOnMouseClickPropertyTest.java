/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * @test
 * @key headful
 * @bug 8158566 8160879 8160977 8158566
 * @summary Provide a Swing property which modifies MenuItemUI behaviour
 */

public class CloseOnMouseClickPropertyTest {

    private static final String CHECK_BOX_PROP = "CheckBoxMenuItem."
            + "doNotCloseOnMouseClick";
    private static final String RADIO_BUTTON_PROP = "RadioButtonMenuItem"
            + ".doNotCloseOnMouseClick";

    private static JFrame frame;
    private static JMenu menu;

    private static TestItem[] TEST_ITEMS = {
        new TestItem(TestType.CHECK_BOX_MENU_ITEM, true, true),
        new TestItem(TestType.CHECK_BOX_MENU_ITEM, true, false),
        new TestItem(TestType.CHECK_BOX_MENU_ITEM, false, true),
        new TestItem(TestType.CHECK_BOX_MENU_ITEM, false, false),

        new TestItem(TestType.CHECK_BOX_MENU_ITEM, null, true),
        new TestItem(TestType.CHECK_BOX_MENU_ITEM, null, false),
        new TestItem(TestType.CHECK_BOX_MENU_ITEM, true, null),
        new TestItem(TestType.CHECK_BOX_MENU_ITEM, false, null),


        new TestItem(TestType.RADIO_BUTTON_MENU_ITEM, true, true),
        new TestItem(TestType.RADIO_BUTTON_MENU_ITEM, true, false),
        new TestItem(TestType.RADIO_BUTTON_MENU_ITEM, false, true),
        new TestItem(TestType.RADIO_BUTTON_MENU_ITEM, false, false),

        new TestItem(TestType.RADIO_BUTTON_MENU_ITEM, true, null),
        new TestItem(TestType.RADIO_BUTTON_MENU_ITEM, false, null),
        new TestItem(TestType.RADIO_BUTTON_MENU_ITEM, null, true),
        new TestItem(TestType.RADIO_BUTTON_MENU_ITEM, null, false),

        new TestItem(TestType.MENU_ITEM, true, true),
        new TestItem(TestType.MENU_ITEM, true, false),
        new TestItem(TestType.MENU_ITEM, false, true),
        new TestItem(TestType.MENU_ITEM, false, false),

        new TestItem(TestType.MENU_ITEM, true, null),
        new TestItem(TestType.MENU_ITEM, false, null),
        new TestItem(TestType.MENU_ITEM, null, true),
        new TestItem(TestType.MENU_ITEM, null, false),
    };

    public static void main(String[] args) throws Exception {

        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            UIManager.setLookAndFeel(info.getClassName());
            for (TestItem testItem : TEST_ITEMS) {
                test(testItem);
            }
        }
    }

    private static void test(TestItem item) throws Exception {

        Robot robot = new Robot();
        robot.setAutoDelay(50);
        SwingUtilities.invokeAndWait(() -> createAndShowGUI(item));
        robot.waitForIdle();

        Point point = getClickPoint(true);
        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();

        point = getClickPoint(false);
        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(() -> {
            JMenuItem menuItem = menu.getItem(0);
            boolean isShowing = menuItem.isShowing();
            frame.dispose();
            if (isShowing ^ item.doNotCloseOnMouseClick()) {
                throw new RuntimeException("Property is not taken into account!");
            }
        });
    }

    private static void createAndShowGUI(TestItem testItem) {

        frame = new JFrame();
        frame.setSize(300, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();
        menu = new JMenu("Menu");
        JMenuItem menuItem = testItem.getMenuItem();
        testItem.setProperties(menuItem);
        menu.add(menuItem);
        menuBar.add(menu);

        frame.setJMenuBar(menuBar);
        frame.setVisible(true);
    }

    private static Point getClickPoint(boolean parent) throws Exception {
        Point points[] = new Point[1];

        SwingUtilities.invokeAndWait(() -> {

            JComponent comp = parent ? menu : menu.getItem(0);

            Point point = comp.getLocationOnScreen();
            Rectangle bounds = comp.getBounds();
            point.x += bounds.getWidth() / 2;
            point.y += bounds.getHeight() / 2;

            points[0] = point;
        });

        return points[0];
    }

    enum TestType {

        MENU_ITEM,
        CHECK_BOX_MENU_ITEM,
        RADIO_BUTTON_MENU_ITEM
    }

    static class TestItem {

        TestType type;
        Boolean compDoNotCloseOnMouseClick;
        Boolean lafDoNotCloseOnMouseClick;

        public TestItem(TestType type,
                        Boolean compDoNotCloseOnMouseClick,
                        Boolean lafDoNotCloseOnMouseClick)
        {
            this.type = type;
            this.compDoNotCloseOnMouseClick = compDoNotCloseOnMouseClick;
            this.lafDoNotCloseOnMouseClick = lafDoNotCloseOnMouseClick;
        }

        boolean doNotCloseOnMouseClick() {
            switch (type) {
                case MENU_ITEM:
                    return false;
                default:
                    return compDoNotCloseOnMouseClick != null
                            ? compDoNotCloseOnMouseClick
                            : lafDoNotCloseOnMouseClick;
            }
        }

        void setProperties(JMenuItem menuItem) {
            switch (type) {
                case CHECK_BOX_MENU_ITEM:
                    menuItem.putClientProperty(CHECK_BOX_PROP, compDoNotCloseOnMouseClick);
                    UIManager.put(CHECK_BOX_PROP, lafDoNotCloseOnMouseClick);
                    break;
                case RADIO_BUTTON_MENU_ITEM:
                    menuItem.putClientProperty(RADIO_BUTTON_PROP, compDoNotCloseOnMouseClick);
                    UIManager.put(RADIO_BUTTON_PROP, lafDoNotCloseOnMouseClick);
                    break;
                default:
                    menuItem.putClientProperty(CHECK_BOX_PROP, compDoNotCloseOnMouseClick);
                    menuItem.putClientProperty(RADIO_BUTTON_PROP, compDoNotCloseOnMouseClick);
                    UIManager.put(CHECK_BOX_PROP, lafDoNotCloseOnMouseClick);
                    UIManager.put(RADIO_BUTTON_PROP, lafDoNotCloseOnMouseClick);
            }
        }

        JMenuItem getMenuItem() {
            switch (type) {
                case CHECK_BOX_MENU_ITEM:
                    return new JCheckBoxMenuItem("Check Box");
                case RADIO_BUTTON_MENU_ITEM:
                    return new JRadioButtonMenuItem("Radio Button");
                default:
                    return new JMenuItem("Menu Item");
            }
        }
    }
}

