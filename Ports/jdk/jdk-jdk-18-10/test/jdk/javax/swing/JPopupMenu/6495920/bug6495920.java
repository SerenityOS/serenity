/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6495920
 * @summary Tests that if the JPopupMenu.setVisible method throws an exception,
            interaction with GNOME is not crippled
 * @author Sergey Malenkov
 * @library ../..
 * @modules java.desktop/sun.awt
 * @modules java.desktop/javax.swing.plaf.basic:open
 */

import sun.awt.AppContext;

import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.lang.reflect.Field;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicPopupMenuUI;

public class bug6495920 implements Thread.UncaughtExceptionHandler {

    public static void main(String[] args) throws Throwable {
        SwingTest.start(bug6495920.class);
    }

    private static Robot robot;
    private final JPanel panel;

    public bug6495920(JFrame frame) {
        JPopupMenu menu = new JPopupMenu() {
            public void setVisible(boolean visible) {
                super.setVisible(visible);
                throw new AssertionError(visible ? "show popup" : "hide popup");
            }
        };
        for (int i = 0; i < 10; i++) {
            menu.add(new JMenuItem(String.valueOf(i)));
        }
        this.panel = new JPanel();
        this.panel.setComponentPopupMenu(menu);
        frame.add(this.panel);
    }

    public void firstShowPopup() throws Exception {
        Point point = this.panel.getLocation();
        SwingUtilities.convertPointToScreen(point, this.panel);

        robot = new Robot(); // initialize shared static field first time
        robot.mouseMove(point.x + 1, point.y + 1);
        robot.mousePress(InputEvent.BUTTON3_MASK);
        Thread.currentThread().setUncaughtExceptionHandler(this);
        robot.mouseRelease(InputEvent.BUTTON3_MASK); // causes first AssertionError on EDT
    }

    public void secondHidePopup() {
        Point point = this.panel.getLocation();
        SwingUtilities.convertPointToScreen(point, this.panel);

        robot.mouseMove(point.x - 1, point.y - 1);
        Thread.currentThread().setUncaughtExceptionHandler(this);
        robot.mousePress(InputEvent.BUTTON1_MASK); // causes second AssertionError on EDT
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    public void thirdValidate() throws Exception {
        Field key = BasicPopupMenuUI.class.getDeclaredField("MOUSE_GRABBER_KEY");
        key.setAccessible(true);

        Object grabber = AppContext.getAppContext().get(key.get(null));
        if (grabber == null) {
            throw new Exception("cannot find a mouse grabber in app's context");
        }

        Field field = grabber.getClass().getDeclaredField("grabbedWindow");
        field.setAccessible(true);

        Object window = field.get(grabber);
        if (window != null) {
            throw new Exception("interaction with GNOME is crippled");
        }
    }

    public void uncaughtException(Thread thread, Throwable throwable) {
        System.out.println(throwable);
    }
}
