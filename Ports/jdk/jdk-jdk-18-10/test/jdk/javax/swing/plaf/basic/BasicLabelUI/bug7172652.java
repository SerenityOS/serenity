/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 7172652
 * @summary With JDK 1.7 text field does not obtain focus when using mnemonic Alt/Key combin
 * @author Semyon Sadetsky
 * @requires (os.family == "windows")
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @run main bug7172652
 */

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyEvent;

import jdk.test.lib.Platform;

public class bug7172652  {

    private static JMenu menu;
    private static JFrame frame;
    private static Boolean selected;

    public static void main(String[] args) throws Exception {
        if (!Platform.isWindows()) {
            System.out.println("ok");
            return;
        }
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                setup();
            }
        });

        test();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.dispose();
            }
        });
    }

    private static void test() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                menu.getModel().addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        selected = menu.isSelected();
                    }
                });
            }
        });

        Robot robot = new Robot();
        robot.setAutoDelay(200);

        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_F);
        robot.keyRelease(KeyEvent.VK_F);
        robot.keyRelease(KeyEvent.VK_ALT);

        robot.waitForIdle();
        if( selected != null ) {
            throw new RuntimeException("Menu is notified selected= " + selected);
        }

        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_F);
        robot.keyRelease(KeyEvent.VK_F);
        robot.keyRelease(KeyEvent.VK_ALT);
        if( selected != null ) {
            throw new RuntimeException("Menu is notified selected= " + selected);
        }

        robot.waitForIdle();

        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_F);
        robot.keyRelease(KeyEvent.VK_F);
        robot.keyRelease(KeyEvent.VK_ALT);
        if( selected != null ) {
            throw new RuntimeException("Menu is notified selected= " + selected);
        }

        robot.waitForIdle();

        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_F);
        robot.keyRelease(KeyEvent.VK_F);
        robot.keyRelease(KeyEvent.VK_ALT);
        if( selected != null ) {
            throw new RuntimeException("Menu is notified selected= " + selected);
        }

        robot.waitForIdle();

        System.out.printf("ok");
    }

    private static void setup() {
        JLabel firstLbl = new JLabel("First name");
        JLabel lastLbl = new JLabel("Last name");
        JMenuBar menuBar = new JMenuBar();

        JTextField firstTxtFld = new JTextField(20);
        JTextField lastTxtFld = new JTextField(20);
        JDesktopPane desktopPane = new JDesktopPane();
        JInternalFrame iframe = new JInternalFrame("A frame", true, true, true, true);

        // Set an initial size
        iframe.setSize(200, 220);

        // By default, internal frames are not visible; make it visible
        iframe.setVisible(true);

        JPanel pane = new JPanel();
        pane.setLayout(new FlowLayout());

        pane.add(firstLbl);
        pane.add(firstTxtFld);
        pane.add(lastLbl);
        pane.add(lastTxtFld);

        firstLbl.setLabelFor(firstTxtFld);
        firstLbl.setDisplayedMnemonic('F');

        lastLbl.setLabelFor(lastTxtFld);
        lastLbl.setDisplayedMnemonic('L');

        iframe.getContentPane().add(pane);
        iframe.setJMenuBar(menuBar);
        menu = new JMenu("FirstMenu");
        //m.setMnemonic('i');
        menuBar.add(menu);
        desktopPane.add(iframe);

        frame = new JFrame();
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(desktopPane);
        frame.setSize(300, 300);
        frame.setVisible(true);
    }

}
