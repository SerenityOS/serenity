/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @library ../../regtesthelpers
 * @build Util
 * @bug 8036819
 * @summary JAB: mnemonics not read for textboxes
 * @author Vivi An
 * @run main bug8036819
 */

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
import javax.accessibility.*;

public class bug8036819 {

    public static volatile Boolean passed = false;

    public static void main(String args[]) throws Throwable {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });


        Robot robo = new Robot();
        robo.setAutoDelay(300);
        robo.waitForIdle();

        // Using mnemonic key to focus on the textfield
        Util.hitMnemonics(robo, KeyEvent.VK_P);
        robo.waitForIdle();

        if (!passed){
            throw new RuntimeException("Test failed.");
        }
    }

    private static void createAndShowGUI() {
        JFrame mainFrame = new JFrame("bug 8036819");

        JLabel usernameLabel = new JLabel("Username: ");
        JTextField usernameField = new JTextField(20);
        usernameLabel.setDisplayedMnemonic(KeyEvent.VK_U);
        usernameLabel.setLabelFor(usernameField);

        JLabel pwdLabel = new JLabel("Password: ");
        JTextField pwdField = new JTextField(20);
        pwdLabel.setDisplayedMnemonic(KeyEvent.VK_P);
        pwdLabel.setLabelFor(pwdField);

        pwdField.addKeyListener(
            new KeyListener(){
                @Override
                public void keyPressed(KeyEvent keyEvent) {
                }

                @Override
                public void keyTyped(KeyEvent keyEvent) {
                }

                @Override
                public void keyReleased(KeyEvent keyEvent){
                    JComponent comp = (JComponent) pwdField;
                    AccessibleContext ac = comp.getAccessibleContext();
                    AccessibleExtendedComponent aec = (AccessibleExtendedComponent)ac.getAccessibleComponent();
                    AccessibleKeyBinding akb = aec.getAccessibleKeyBinding();
                    if (akb != null){
                         int count = akb.getAccessibleKeyBindingCount();
                        if (count != 1){
                            passed = false;
                            return;
                        }

                        // there is 1 accessible key for the text field
                        System.out.println("Retrieved AccessibleKeyBinding for textfield " + count);

                        // the key code is KeyEvent.VK_P
                        Object o = akb.getAccessibleKeyBinding(0);
                        if (o instanceof KeyStroke){
                            javax.swing.KeyStroke key = (javax.swing.KeyStroke)o;
                            System.out.println("keystroke is " + key.getKeyCode());
                            if (key.getKeyCode() == KeyEvent.VK_P)
                                passed = true;
                        }
                    }
                }
            }
        );

        mainFrame.getContentPane().add(usernameLabel);
        mainFrame.getContentPane().add(usernameField);
        mainFrame.getContentPane().add(pwdLabel);
        mainFrame.getContentPane().add(pwdField);

        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new FlowLayout(FlowLayout.LEFT));

        mainFrame.setSize(200, 200);
        mainFrame.setLocation(200, 200);
        mainFrame.setVisible(true);
        mainFrame.toFront();
    }
 }
