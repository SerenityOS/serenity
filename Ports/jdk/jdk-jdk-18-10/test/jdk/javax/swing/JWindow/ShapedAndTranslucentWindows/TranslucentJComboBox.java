/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8024627 8190347
 * @summary Check if a JComboBox present in a window set with opacity less than
 *          1.0 shows a translucent drop down
 * Test Description: Check if TRANSLUCENT translucency type is supported on the
 *      current platform. Proceed if supported. Show a window which contains an
 *      JComboBox and set with opacity less than 1.0. Another Window having a canvas
 *      component drawn with an image can be used as the background for the test
 *      window. Click on the ComboBox to show the drop down. Check if the drop down
 *      appears translucent. Repeat this for JWindow, JDialog and JFrame
 * Expected Result: If TRANSLUCENT Translucency type is supported, the drop down
 *      should appear translucent.
 * @author mrkam
 * @library /lib/client
 * @build Common ExtendedRobot
 * @run main TranslucentJComboBox
 */

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.GraphicsDevice;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Color;
import java.awt.Window;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TranslucentJComboBox extends Common {

    JComponent south;
    JComponent center;
    JPanel north;
    volatile boolean southClicked = false;

    JPopupMenu popup = null;

    public static void main(String[] args) throws Exception {
        if (checkTranslucencyMode(GraphicsDevice.WindowTranslucency.TRANSLUCENT))
            for (Class<Window> windowClass: WINDOWS_TO_TEST)
                new TranslucentJComboBox(windowClass).doTest();
    }

    public TranslucentJComboBox(Class windowClass) throws Exception {
        super(windowClass, 0.3f, 1.0f, false);
    }

    @Override
    public void initBackgroundFrame() {
        super.initBackgroundFrame();
    }

    @Override
    public void createSwingComponents() {
        Container contentPane = RootPaneContainer.class.cast(window).getContentPane();
        window.setLayout(new BorderLayout());

        north = new JPanel();
        contentPane.add(north, BorderLayout.NORTH);

        center = new JList(new String [] { "Center" });
        contentPane.add(center, BorderLayout.CENTER);

        JComboBox jComboBox = new JComboBox();
        for(int i = 0; i < 20; i++) {
            jComboBox.addItem("item " + i);
        }
        south = jComboBox;
        jComboBox.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                popup = (JPopupMenu) jComboBox.getAccessibleContext()
                        .getAccessibleChild(0);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });

        south.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                southClicked = true;
            }
        });
        contentPane.add(south, BorderLayout.SOUTH);
    }


    @Override
    public void doTest() throws Exception {
        robot.waitForIdle(delay);
        // Make window an active
        Point ls = north.getLocationOnScreen();
        robot.mouseMove(ls.x + north.getWidth()/2, ls.y + north.getHeight()/2);
        robot.click();

        // Invoke list
        ls = south.getLocationOnScreen();

        Point p1 = new Point(
                (int) (ls.x + south.getWidth() * 0.75),
                ls.y + south.getHeight() * 3);

        Point p2 = new Point(
                (int) (ls.x + south.getWidth() * 0.75),
                ls.y - south.getHeight() * 2);

        Color c1 = robot.getPixelColor(p1.x, p1.y);
        Color c2 = robot.getPixelColor(p2.x, p2.y);

        int x = ls.x + south.getWidth()/2;
        int y = ls.y + south.getHeight()/2;

        System.out.println("Trying to click point "+x+", "+y+
                ", looking for flag to trigger.");

        robot.mouseMove(x, y);
        robot.waitForIdle(delay);
        robot.click();
        robot.waitForIdle(delay);

        if (!southClicked)
            throw new RuntimeException("Flag is not triggered for point "+x+", "+y+"!");

        robot.waitForIdle();

        Color c1b = robot.getPixelColor(p1.x, p1.y);
        Color c2b = robot.getPixelColor(p2.x, p2.y);

        if (!c1.equals(c1b) && !south.getBackground().equals(c1b))
            throw new RuntimeException(
                    "Check for opaque drop down failed at point " + p1 +
                            ". Before click: " + c1 + ", after click: " + c1b +
                            ", expected is " + south.getBackground());

        //This following check is only valid if the popup was created below the
        // JComboBox and will be opaque or it is created above the JComboBox
        // and it can not fit inside the JWindow along with JComboBox and will
        // be opaque
        if ( !c2.equals(c2b) && !south.getBackground().equals(c2b) &&
                (popup.getLocationOnScreen().y > ls.y ||
                        window.getHeight() < popup.getHeight() + south.getHeight()))
            throw new RuntimeException(
                    "Check for opaque drop down failed at point " + p2 +
                            ". Before click: " + c2 + ", after click: " + c2b +
                            ", expected is " + south.getBackground());

        popup = null;
        EventQueue.invokeAndWait(this::dispose);
    }
}
