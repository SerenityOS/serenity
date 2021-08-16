/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 6380743 8158380 8198624
  @summary Submenu should be shown by mnemonic key press.
  @author anton.tarasov@...: area=awt.focus
  @library ../../../regtesthelpers
  @library /test/lib
  @build Util
  @build jdk.test.lib.Platform
  @run main SubMenuShowTest
*/

import java.awt.Robot;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.util.concurrent.atomic.AtomicBoolean;
import jdk.test.lib.Platform;
import test.java.awt.regtesthelpers.Util;

public class SubMenuShowTest {
    private static Robot robot;
    private static JFrame frame;
    private static JMenuBar bar;
    private static JMenu menu;
    private static JMenu submenu;
    private static JMenuItem item;
    private static AtomicBoolean activated = new AtomicBoolean(false);

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(SubMenuShowTest::createAndShowGUI);

        try {
            robot = new Robot();
            robot.setAutoDelay(100);
            robot.setAutoWaitForIdle(true);

            doTest();
        } catch (Exception ex) {
            throw new RuntimeException("Test failed: Exception thrown:"+ex);
        } finally {
            dispose();
        }

        System.out.println("Test passed.");
    }

    public static void dispose() throws Exception {
        if(frame != null) {
            SwingUtilities.invokeAndWait(() -> {
                frame.dispose();
            });
        }
    }

    public static void createAndShowGUI() {
        // Create instructions for the user here, as well as set up
        // the environment -- set the layout manager, add buttons,
        // etc.
        frame = new JFrame("Test Frame");
        bar = new JMenuBar();
        menu = new JMenu("Menu");
        submenu = new JMenu("More");
        item = new JMenuItem("item");

        frame.setLayout (new BorderLayout ());
        menu.setMnemonic('f');
        submenu.setMnemonic('m');
        menu.add(submenu);
        submenu.add(item);
        bar.add(menu);
        frame.setJMenuBar(bar);
        frame.pack();

        item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.out.println(e.toString());
                    synchronized (activated) {
                        activated.set(true);
                        activated.notifyAll();
                    }
                }
            });

        frame.setVisible(true);
    }

    public static void doTest() {
        boolean isMacOSX = Platform.isOSX();
        if (isMacOSX) {
            robot.keyPress(KeyEvent.VK_CONTROL);
        }
        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_F);
        robot.keyRelease(KeyEvent.VK_F);
        robot.keyRelease(KeyEvent.VK_ALT);

        if (isMacOSX) {
            robot.keyRelease(KeyEvent.VK_CONTROL);
        }

        robot.keyPress(KeyEvent.VK_M);
        robot.keyRelease(KeyEvent.VK_M);
        robot.keyPress(KeyEvent.VK_SPACE);
        robot.keyRelease(KeyEvent.VK_SPACE);

        if (!Util.waitForCondition(activated, 1500)) {
            throw new TestFailedException("A submenu wasn't activated by mnemonic key press");
        }
    }
}

class TestFailedException extends RuntimeException {
    TestFailedException(String msg) {
        super("Test failed: " + msg);
    }
}
