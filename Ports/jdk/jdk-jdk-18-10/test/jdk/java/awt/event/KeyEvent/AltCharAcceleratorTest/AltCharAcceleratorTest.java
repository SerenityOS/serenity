/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8068283
 * @summary Checks that <Alt>+Char accelerators work when pressed in a text component
 * @author Anton Nashatyrev
 * @modules java.desktop/sun.awt
 * @run main AltCharAcceleratorTest
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AltCharAcceleratorTest {

    boolean action1 = false;
    boolean action2 = false;

    CountDownLatch focusLatch = new CountDownLatch(1);
    CountDownLatch actionLatch = new CountDownLatch(2);

    public AltCharAcceleratorTest() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JFrame f = new JFrame("frame");
                final JTextField t = new JTextField();
                JMenuBar mb = new JMenuBar();
                JMenu m1 = new JMenu("File");
                JMenuItem i1 = new JMenuItem("Save");
                JMenuItem i2 = new JMenuItem("Load");

                i1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.ALT_MASK));
                i2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.ALT_MASK));

                i1.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        System.out.println("Action1!");
                        action1 = true;
                        actionLatch.countDown();
                    }
                });

                i2.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        System.out.println("Action2!");
                        action2 = true;
                        actionLatch.countDown();
                    }
                });

                t.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        System.out.println("Focused!");
                        focusLatch.countDown();
                    }
                });

                t.setColumns(10);
                t.requestFocusInWindow();

                f.setJMenuBar(mb);
                mb.add(m1);
                m1.add(i1);
                m1.add(i2);

                f.setLayout(new FlowLayout());
                f.add(t);
                f.setSize(200, 200);

                f.setVisible(true);
            }
        });
    }

    void test() throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(100);
        robot.waitForIdle();

        focusLatch.await(5, TimeUnit.SECONDS);

        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_T);
        robot.keyRelease(KeyEvent.VK_T);
        robot.keyRelease(KeyEvent.VK_ALT);

        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_F);
        robot.keyRelease(KeyEvent.VK_F);
        robot.keyRelease(KeyEvent.VK_ALT);

        actionLatch.await(5, TimeUnit.SECONDS);

        if (!action1 || !action2) {
            throw new RuntimeException("Actions not performed");
        }

        System.out.println("Passed.");
    }

    public static void main(String[] args) throws Exception {
        AltCharAcceleratorTest t = new AltCharAcceleratorTest();
        t.test();
    }
}
