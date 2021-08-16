/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.util.Properties;

/*
 * @test
 * @key headful
 * @summary To check the functionality of newly added API getSystemSelection & make sure
 *          that it's mapped to primary clipboard
 * @author Jitender(jitender.singh@eng.sun.com) area=AWT
 * @library /lib/client
 * @build ExtendedRobot
 * @run main SystemSelectionAWTTest
 */

public class SystemSelectionAWTTest {

    Frame frame;
    TextField tf1, tf2;
    Clipboard clip;
    Transferable t;

    public static void main(String[] args) throws Exception {
        new SystemSelectionAWTTest().doTest();
    }

    SystemSelectionAWTTest() {
        frame = new Frame();
        frame.setSize(200, 200);

        tf1 = new TextField();
        tf1.addFocusListener( new FocusAdapter() {
            public void focusGained(FocusEvent fe) {
                fe.getSource();
            }
        });

        tf2 = new TextField();

        frame.add(tf2, BorderLayout.NORTH);
        frame.add(tf1, BorderLayout.CENTER);

        frame.setVisible(true);
        frame.toFront();
        tf1.requestFocus();
        tf1.setText("Selection Testing");
    }

    // Check whether Security manager is there
    public void checkSecurity() {
        SecurityManager sm = System.getSecurityManager();

        if (sm == null) {
            System.out.println("security manager is not there");
            getPrimaryClipboard();
        } else {
            try {
                sm.checkPermission(new AWTPermission("accessClipboard"));
                getPrimaryClipboard();
            } catch(SecurityException e) {
                clip = null;
                System.out.println("Access to System selection is not allowed");
            }
        }
    }

    // Get the contents from the clipboard
    void getClipboardContent() throws Exception {
        t = clip.getContents(this);
        if ( (t != null) && (t.isDataFlavorSupported(DataFlavor.stringFlavor) )) {
            tf2.setBackground(Color.red);
            tf2.setForeground(Color.black);
            tf2.setText((String) t.getTransferData(DataFlavor.stringFlavor));
        }
    }

    // Get System Selection i.e. Primary Clipboard
    private void getPrimaryClipboard() {
        Properties ps = System.getProperties();
        String operSys = ps.getProperty("os.name");
        clip = Toolkit.getDefaultToolkit().getSystemSelection();
        if (clip == null) {
            if ((operSys.substring(0,3)).equalsIgnoreCase("Win") ||
                    (operSys.substring(0,3)).equalsIgnoreCase("Mac"))
                System.out.println(operSys + " operating system does not support system selection ");
            else
                throw new RuntimeException("Method getSystemSelection() is returning null on X11 platform");
        }
    }

    // Compare the selected text with one pasted from the clipboard
    public void compareText() {
        if ((tf2.getText()).equals(tf1.getSelectedText()) &&
                System.getProperties().getProperty("os.name").substring(0,3) != "Win") {
            System.out.println("Selected text & clipboard contents are same\n");
        } else  {
            throw new RuntimeException("Selected text & clipboard contents differs\n");
        }
    }

    public void doTest() throws Exception {
        ExtendedRobot robot = new ExtendedRobot();

        frame.setLocation(100, 100);
        robot.waitForIdle(2000);

        Point tf1Location = tf1.getLocationOnScreen();
        Dimension tf1Size = tf1.getSize();
        checkSecurity();

        if (clip != null) {
            robot.mouseMove(tf1Location.x + 5, tf1Location.y + tf1Size.height / 2);
            robot.waitForIdle(2000);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle(20);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle(20);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle(20);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle(2000);

            getClipboardContent();
            compareText();

            robot.mouseMove(tf1Location.x + tf1Size.width / 2, tf1Location.y + tf1Size.height / 2);
            robot.waitForIdle(2000);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle(20);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle(20);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle(20);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle(2000);

            getClipboardContent();
            compareText();
        }
    }
}

