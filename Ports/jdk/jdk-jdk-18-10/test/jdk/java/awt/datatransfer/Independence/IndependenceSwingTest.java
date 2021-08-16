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

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Properties;

/*
 * @test
 * @key headful
 * @summary To make sure that System & Primary clipboards should behave independently
 * @author Jitender(jitender.singh@eng.sun.com) area=AWT
 * @author dmitriy.ermashov@oracle.com
 * @library /lib/client
 * @build ExtendedRobot
 * @run main IndependenceSwingTest
 */

public class IndependenceSwingTest {

    JFrame frame;
    JPanel panel;
    JTextField tf1, tf2, tf3;
    Clipboard sClip, pClip;

    public static void main (String[] args) throws Exception {
        new IndependenceSwingTest().doTest();
    }

    public IndependenceSwingTest() {

        frame = new JFrame();
        frame.setSize(200, 200);

        // This textfield will be used to update the contents of clipboards
        tf1 = new JTextField();
        tf1.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent fe) {
                tf1.setText("Clipboards_Independance_Testing");
            }
        });

        // TextFields to get the contents of clipboard
        tf2 = new JTextField();
        tf3 = new JTextField();

        panel = new JPanel();
        panel.setLayout(new BorderLayout());

        panel.add(tf2, BorderLayout.NORTH);
        panel.add(tf3, BorderLayout.SOUTH);

        frame.add(tf1, BorderLayout.NORTH);
        frame.add(panel, BorderLayout.CENTER);

        frame.setVisible(true);
        tf1.requestFocus();
    }

    public void checkSecurity() {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null)  {
            System.out.println("security manager is not there");
            getPrimaryClipboard();
        } else {
            sm.checkPermission(new AWTPermission("accessClipboard"));
            getPrimaryClipboard();
        }
    }

    // Get System Selection i.e. Primary Clipboard
    private void getPrimaryClipboard() {
        Properties ps = System.getProperties();
        String operSys = ps.getProperty("os.name");
        try {
            pClip = Toolkit.getDefaultToolkit().getSystemSelection();
            if (pClip == null)
                if ((operSys.substring(0,3)).equalsIgnoreCase("Win") || operSys.toLowerCase().contains("os x"))
                    System.out.println(operSys + "Operating system does not support system selection ");
                else
                    throw new RuntimeException("Method getSystemSelection() is returning null on X11 platform");
        } catch(HeadlessException e) {
            System.out.println("Headless exception thrown " + e);
        }
    }

    // Method to get the contents of both of the clipboards
    public void getClipboardsContent() throws Exception {
        sClip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable tp;
        Transferable ts;

        StringSelection content = new StringSelection(tf1.getText());
        sClip.setContents(content,content);

        tp = pClip.getContents(this);
        ts = sClip.getContents(this);

        // Paste the contents of System clipboard on textfield tf2 while the paste the contents of
        // of primary clipboard on textfiled tf3
        if ((ts != null) && (ts.isDataFlavorSupported(DataFlavor.stringFlavor))) {
            tf2.setBackground(Color.white);
            tf2.setForeground(Color.black);
            tf2.setText((String) ts.getTransferData(DataFlavor.stringFlavor));
        }

        if ((tp != null) && (tp.isDataFlavorSupported(DataFlavor.stringFlavor))) {
            tf3.setBackground(Color.white);
            tf3.setForeground(Color.black);
            tf3.setText((String) tp.getTransferData(DataFlavor.stringFlavor));
        }
    }

    // Method to compare the Contents return by system & primary clipboard
    public void compareText (boolean mustEqual) {
        if ((tf2.getText()).equals(tf3.getText())) {
            if (mustEqual)
                System.out.println("Selected text & clipboard contents are same\n");
            else
                throw new RuntimeException("Selected text & clipboard contents are same\n");
        } else {
            if (mustEqual)
                throw new RuntimeException("Selected text & clipboard contents differs\n");
            else
                System.out.println("Selected text & clipboard contents differs\n");
        }
    }

    public void doTest() throws Exception {
        checkSecurity();
        ExtendedRobot robot = new ExtendedRobot();
        robot.waitForIdle(1000);
        frame.setLocation(100, 100);
        robot.waitForIdle(1000);

        if (pClip != null) {
            Point ttf1Center = tf1.getLocationOnScreen();
            ttf1Center.translate(tf1.getWidth()/2, tf1.getHeight()/2);

            robot.glide(new Point(0, 0), ttf1Center);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle(20);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle(20);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle(20);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle(2000);

            getClipboardsContent();
            compareText(true);

            //Change the text selection to update the contents of primary clipboard
            robot.mouseMove(ttf1Center);
            robot.mousePress(MouseEvent.BUTTON1_MASK);
            robot.delay(200);
            robot.mouseMove(ttf1Center.x + 15, ttf1Center.y);
            robot.mouseRelease(MouseEvent.BUTTON1_MASK);
            robot.waitForIdle(2000);

            getClipboardsContent();
            compareText(false);
        }
    }
}

