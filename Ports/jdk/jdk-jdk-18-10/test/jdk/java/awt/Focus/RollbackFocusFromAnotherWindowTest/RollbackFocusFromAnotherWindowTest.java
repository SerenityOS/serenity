/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug       8139218
  @summary   Dialog that opens and closes quickly changes focus in original
             focusowner
  @run       main RollbackFocusFromAnotherWindowTest
 */

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class RollbackFocusFromAnotherWindowTest extends JFrame
                                                          implements KeyListener
{
    private static RollbackFocusFromAnotherWindowTest tfs;
    private static Robot robot;

    public static void main(String[] args) throws Exception {
        robot = new Robot();

        SwingUtilities.invokeAndWait(() -> {
            tfs = new RollbackFocusFromAnotherWindowTest();
            tfs.setVisible(true);
        });

        robot.waitForIdle();
        robot.delay(200);

        try {
            for (int i = 0; i < 10; i++) {
                robot.keyPress(KeyEvent.VK_A);
                robot.delay(10);
                robot.keyRelease(KeyEvent.VK_A);
                robot.waitForIdle();
                robot.delay(200);
                SwingUtilities.invokeAndWait(() -> {
                    String name = tfs.getFocusOwner().getName();
                    System.out.println(name);
                    if (!"Comp0".equals(name)) {
                        throw new RuntimeException(
                                "Focus is not restored correctly");
                    }
                });
            }
            System.out.println("ok");
        } finally {
            SwingUtilities.invokeLater(() -> tfs.dispose());
        }
    }

    public RollbackFocusFromAnotherWindowTest()
    {
        setUndecorated(true);
        Container c = getContentPane();
        c.setLayout(new FlowLayout());
        for (int i = 0; i < 10; i++)
        {
            JTextField tf = new JTextField("" + i, 10);
            tf.setName("Comp" + i);
            c.add(tf);
            tf.addKeyListener(this);
        }
        pack();
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        Frame frame = new Frame();
        frame.setVisible(true);
        try {
            Thread.sleep(2);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        frame.dispose();
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
