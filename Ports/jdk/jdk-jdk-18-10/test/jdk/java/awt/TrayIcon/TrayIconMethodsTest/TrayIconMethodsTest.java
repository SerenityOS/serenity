/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.image.BufferedImage;

/*
 * @test
 * @key headful
 * @summary Check various methods of the TrayIcon - whether the methods
 *          return the proper values, throws the proper exceptions etc
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @run main TrayIconMethodsTest
 */

public class TrayIconMethodsTest {

    public static void main(String[] args) throws Exception {
        if (! SystemTray.isSupported()) {
            System.out.println("SystemTray not supported on the platform under test. " +
                    "Marking the test passed");
        } else {
            new TrayIconMethodsTest().doTest();
        }
    }

    void doTest() throws Exception {
        SystemTray tray = SystemTray.getSystemTray();

        String toolTip = "Sample Icon";
        PopupMenu pm = new PopupMenu();
        pm.add(new MenuItem("Sample"));

        Image image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        TrayIcon icon = new TrayIcon(image, toolTip, pm);

        ActionListener al1 = event -> {};
        ActionListener al2 = event -> {};
        MouseMotionListener mml1 = new MouseMotionAdapter() {};
        MouseMotionListener mml2 = new MouseMotionAdapter() {};
        MouseListener ml1 = new MouseAdapter() {};
        MouseListener ml2 = new MouseAdapter() {};

        icon.addActionListener(al1);
        icon.addActionListener(al2);
        icon.addMouseMotionListener(mml1);
        icon.addMouseMotionListener(mml2);
        icon.addMouseListener(ml1);
        icon.addMouseListener(ml2);
        tray.add(icon);

        ActionListener[] actionListeners = icon.getActionListeners();
        if (actionListeners == null || actionListeners.length != 2)
            throw new RuntimeException("FAIL: getActionListeners did not return the correct value " +
                    "when there were two listeners present " + actionListeners);

        if (! isPresent(actionListeners, al1) || ! isPresent(actionListeners, al2))
            throw new RuntimeException("FAIL: All the action listeners added are not returned " +
                    "by the method");

        MouseListener[] mouseListeners = icon.getMouseListeners();
        if (mouseListeners == null || mouseListeners.length != 2)
            throw new RuntimeException("FAIL: getMouseListeners did not return the correct value " +
                    "when there were two listeners present " + mouseListeners);

        if (! isPresent(mouseListeners, ml1) || ! isPresent(mouseListeners, ml2))
            throw new RuntimeException("FAIL: All the mouse listeners added are not returned " +
                    "by the method");

        MouseMotionListener[] mouseMotionListeners = icon.getMouseMotionListeners();
        if (mouseMotionListeners == null || mouseMotionListeners.length != 2)
            throw new RuntimeException("FAIL: getMouseMotionListeners did not return the correct value " +
                    "when there were two listeners present " + mouseMotionListeners);

        if (! isPresent(mouseMotionListeners, mml1) || ! isPresent(mouseMotionListeners, mml2))
            throw new RuntimeException("FAIL: All the mouse motion listeners added are not returned " +
                    "by the method");

        Image im = icon.getImage();
        if (! image.equals(im))
            throw new RuntimeException("FAIL: Images are not the same getImage()=" + im +
                    " Image=" + image);

        if (! pm.equals(icon.getPopupMenu()))
            throw new RuntimeException("FAIL: PopupMenus are not the same getPopupMenu()=" +
                    icon.getPopupMenu() + " PopupMenu=" + pm);

        if (! toolTip.equals(icon.getToolTip()))
            throw new RuntimeException("FAIL: ToolTips are not the same getToolTip()=" +
                               icon.getToolTip() + " ToolTip=" + toolTip);

        if (icon.isImageAutoSize())
            throw new RuntimeException("FAIL: Auto size property is true by default");

        icon.setImageAutoSize(true);
        if (! icon.isImageAutoSize())
            throw new RuntimeException("FAIL: Auto size property is not set to " +
                    "true by call to setImageAutoSize(true)");

        icon.removeActionListener(al1);
        icon.removeActionListener(al2);
        actionListeners = icon.getActionListeners();
        if (actionListeners == null || actionListeners.length != 0)
            throw new RuntimeException("FAIL: removeActionListener did not " +
                    "remove the ActionListeners added " + actionListeners);

        icon.removeMouseListener(ml1);
        icon.removeMouseListener(ml2);
        mouseListeners = icon.getMouseListeners();
        if (mouseListeners == null || mouseListeners.length != 0)
            throw new RuntimeException("FAIL: removeMouseListener did not " +
                    "remove the MouseListeners added " + mouseListeners);

        icon.removeMouseMotionListener(mml1);
        icon.removeMouseMotionListener(mml2);
        mouseMotionListeners = icon.getMouseMotionListeners();
        if (mouseMotionListeners == null || mouseMotionListeners.length != 0)
            throw new RuntimeException("FAIL: removeMouseMotionListener did not " +
                    "remove the MouseMotionListeners added " + mouseMotionListeners);

        try {
            icon.setImage(null);
            throw new RuntimeException("FAIL: setImage(null) did not throw NullPointerException");
        } catch (NullPointerException npe) {
        }
    }

    boolean isPresent(Object[] array, Object obj) {
        if (array == null || array.length == 0 || obj == null) {
            return false;
        }
        for (int i = 0; i < array.length; i++) {
            if (obj.equals(array[i])) {
                return true;
            }
        }
        return false;
    }
}
