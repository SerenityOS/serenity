/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 *  @test
 *  @bug 8048109
 *  @summary JToggleButton does not fire actionPerformed under certain
 *  conditions
 *  @key headful
 *  @run main SetInvokerJPopupMenuTest
 */

public class SetInvokerJPopupMenuTest {

    private static MyPopupMenu popup;
    private static MyButton jtb ;
    private static Robot robot;
    private static JFrame f;
    private static Point p;
    private static boolean isPopupVisible;

    public static void main(String[] args) throws Exception {
        UIManager.LookAndFeelInfo[] installedLookAndFeels;
        installedLookAndFeels = UIManager.getInstalledLookAndFeels();

        for(UIManager.LookAndFeelInfo LF : installedLookAndFeels) {
            try {
                robot = new Robot();
                UIManager.setLookAndFeel(LF.getClassName());
                SwingUtilities.invokeAndWait(() -> {
                    jtb = new MyButton("Press Me");
                    jtb.addActionListener(new ActionListener( ) {
                        @Override
                        public void actionPerformed(ActionEvent ev) {
                            if (!popup.isVisible()) {
                                postUp();
                            }
                            else {
                                postDown();
                            }
                        }
                    });

                    f = new JFrame( );
                    f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    f.setLocationRelativeTo(null);
                    f.setSize(300, 400);
                    Container c = f.getContentPane( );
                    c.setLayout(new FlowLayout( ));
                    c.add(jtb);
                    f.setVisible(true);
                    popup = new MyPopupMenu();
                    popup.add(new JMenuItem("A"));
                    popup.add(new JMenuItem("B"));
                    popup.add(new JMenuItem("C"));
                    popup.setVisible(false);
                    p = jtb.getLocationOnScreen();
                });

                robot.waitForIdle();
                robot.setAutoDelay(50);
                robot.mouseMove(p.x + 15, p.y + 15);
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

                robot.waitForIdle();
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

            } catch (UnsupportedLookAndFeelException e) {
                System.out.println("Note: LookAndFeel " + LF.getClassName()
                        + " is not supported on this configuration");
            } finally {
                if (f != null) {
                    SwingUtilities.invokeAndWait(() -> f.dispose());
                }
            }

            SwingUtilities.invokeAndWait(() -> {
                isPopupVisible = popup.isVisible();
            });

            if (isPopupVisible) {
                throw new RuntimeException("PopupMenu is not taken down after"+
                        " single button click");
            }
        }
    }

    public static void postUp() {
        popup.setInvoker(jtb);
        popup.setVisible(true);
    }

    public static void postDown() {
        popup.setVisible(false);
    }

    private static class MyButton extends JButton {
        public MyButton(String string) {
            super (string);
        }
        @Override
        protected void processMouseEvent(MouseEvent e) {
            super.processMouseEvent(e);
        }
    }

    private static class MyPopupMenu extends JPopupMenu {
        @Override
        public void setVisible( boolean state ) {
            if( !state ) {
                Exception ex = new Exception();
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter( stringWriter );
                ex.printStackTrace( printWriter );
                String traceString = stringWriter.getBuffer().toString();
                if( traceString.lastIndexOf( "windowDeactivated" ) > 0
                        || traceString.lastIndexOf( "menuSelectionChanged" )
                        > 0 ) {
                    return;
                }
            }
            super.setVisible(state);
        }
    }
}
