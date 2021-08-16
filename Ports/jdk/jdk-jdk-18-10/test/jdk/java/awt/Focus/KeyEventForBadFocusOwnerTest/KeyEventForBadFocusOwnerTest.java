/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4476629
  @library ../../../../javax/swing/regtesthelpers
  @build Util
  @summary KeyEvents dispatched to old focus owner that is no longer showing
  @author son@sparc.spb.su: area=awt.focus
  @run main KeyEventForBadFocusOwnerTest
*/

/**
 * KeyEventForBadFocusOwnerTest.java
 *
 * summary: KeyEvents dispatched to old focus owner that is no longer showing
 */


import java.awt.Robot;

import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

public class KeyEventForBadFocusOwnerTest {
    final static String ITEM_ONE_TEXT = "one";
    final static String ITEM_TWO_TEXT = "two";

    volatile static boolean itemOneSelected = false;
    volatile static boolean itemTwoSelected = false;
    volatile static boolean unexpectedItemSelected = false;

    static Robot robot;
    static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame = new JFrame("TEST");
                    JMenuBar mb = new JMenuBar();
                    JMenu one = new JMenu(ITEM_ONE_TEXT);
                    JMenu two = new JMenu(ITEM_TWO_TEXT);

                    mb.add(one);
                    mb.add(two);

                    ActionListener al = new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            String itemText = ((JMenuItem)ae.getSource()).getText();
                            System.out.println("--> " + itemText);
                            unexpectedItemSelected = true;
                        }
                    };
                    one.setMnemonic(KeyEvent.VK_O);
                    JMenuItem item = new JMenuItem("one 1");
                    item.setMnemonic(KeyEvent.VK_O);
                    item.addActionListener(al);
                    one.add(item);
                    one.add("two");
                    one.add("three");

                    two.setMnemonic(KeyEvent.VK_T);
                    item = new JMenuItem("two 2");
                    item.setMnemonic(KeyEvent.VK_T);
                    item.addActionListener(al);
                    two.add(item);
                    two.add("three");
                    two.add("four");

                    PopupMenuListener popupMenuListener = new PopupMenuListener() {
                        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                            System.out.print(e);
                            System.out.print(e.getSource());
                            String itemText = ((JPopupMenu)e.getSource()).getName();
                            System.out.println("Menu " + itemText + "is opened.");
                            switch(itemText) {
                                case ITEM_ONE_TEXT:
                                    itemOneSelected = true;
                                    break;
                                case ITEM_TWO_TEXT:
                                    itemTwoSelected = true;
                                    break;
                            }
                        }

                        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
                        public void popupMenuCanceled(PopupMenuEvent e) {}
                    };
                    one.getPopupMenu().setName(ITEM_ONE_TEXT);
                    two.getPopupMenu().setName(ITEM_TWO_TEXT);
                    one.getPopupMenu().addPopupMenuListener(popupMenuListener);
                    two.getPopupMenu().addPopupMenuListener(popupMenuListener);
                    frame.setJMenuBar(mb);
                    frame.setSize(100,100);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setLocationRelativeTo(null);
                    frame.pack();
                    frame.setVisible(true);
                }
            });


            robot = new Robot();
            robot.setAutoDelay(100);
            robot.waitForIdle();
            robot.delay(1000);

            Util.hitMnemonics(robot, KeyEvent.VK_O);
            Util.hitMnemonics(robot, KeyEvent.VK_T);

            robot.waitForIdle();
            Thread.sleep(1000); // workaround for MacOS

            if (unexpectedItemSelected) {
                throw new Exception("Test failed. KeyEvent dispatched to old focus owner. ");
            }
            if (!itemOneSelected || !itemTwoSelected) {
                throw new Exception("Not all expected events were received");
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame != null) {
                    frame.dispose();
                }
            });
        }
    }
}
