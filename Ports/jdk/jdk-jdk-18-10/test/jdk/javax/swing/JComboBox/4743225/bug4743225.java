/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4743225
 * @summary Size of JComboBox list is wrong when list is populated via PopupMenuListener
 * @author Alexander Potochkin
 */

import javax.accessibility.AccessibleContext;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboPopup;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;

public class bug4743225 extends JFrame {

    private static JComboBox cb;
    private static volatile boolean flag;

    public bug4743225() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());
        cb = new JComboBox(new Object[] {"one", "two", "three"});
        cb.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                cb.addItem("Test");
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        add(cb);
        pack();
        setLocationRelativeTo(null);
    }

    public static BasicComboPopup getPopup() {
        AccessibleContext c = cb.getAccessibleContext();
        for(int i = 0; i < c.getAccessibleChildrenCount(); i ++) {
            if (c.getAccessibleChild(i) instanceof BasicComboPopup) {
                return (BasicComboPopup) c.getAccessibleChild(i);
            }
        }
        throw new AssertionError("No BasicComboPopup found");
    }

    public static void main(String... args) throws Exception {

        Robot robot = new Robot();
        robot.setAutoDelay(100);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                new bug4743225().setVisible(true);
            }
        });
        robot.waitForIdle();

        // calling this method from main thread is ok
        Point point = cb.getLocationOnScreen();
        robot.mouseMove(point.x + 10, point.y + 10);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                if(getPopup().getList().getLastVisibleIndex() == 3) {
                    flag = true;
                }
            }
        });

        if (!flag) {
            throw new RuntimeException("The ComboBox popup wasn't correctly updated");
        }
    }
}
