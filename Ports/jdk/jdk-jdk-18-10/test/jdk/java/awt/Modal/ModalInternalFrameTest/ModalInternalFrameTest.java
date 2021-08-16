/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6518753
  @summary Tests the functionality of modal Swing internal frames
  @author artem.ananiev: area=awt.modal
  @run main/timeout=30 ModalInternalFrameTest
*/

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class ModalInternalFrameTest
{
    private boolean passed = true;
    private static Robot r;

    private JDesktopPane pane1;
    private JDesktopPane pane2;

    private JFrame frame1;
    private JFrame frame2;

    private JButton bn1;
    private JButton bs1;
    private JButton bn2;
    private JButton bs2;

    private Point bn1Loc;
    private Point bs1Loc;
    private Point bn2Loc;
    private Point bs2Loc;

    private volatile boolean unblocked1 = true;
    private volatile boolean unblocked2 = true;

    public ModalInternalFrameTest()
    {
    }

    public void init()
    {
        pane1 = new JDesktopPane();
        pane2 = new JDesktopPane();

        frame1 = new JFrame("F1");
        frame1.setBounds(100, 100, 320, 240);
        frame1.getContentPane().setLayout(new BorderLayout());
        frame1.getContentPane().add(pane1);
        bn1 = new JButton("Test");
        bn1.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                unblocked1 = true;
            }
        });
        frame1.getContentPane().add(bn1, BorderLayout.NORTH);
        bs1 = new JButton("Show dialog");
        bs1.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                JOptionPane.showInternalMessageDialog(pane1, "Dialog1");
            }
        });
        frame1.getContentPane().add(bs1, BorderLayout.SOUTH);

        frame2 = new JFrame("F2");
        frame2.setBounds(100, 400, 320, 240);
        frame2.getContentPane().setLayout(new BorderLayout());
        frame2.getContentPane().add(pane2);
        bn2 = new JButton("Test");
        bn2.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                unblocked2 = true;
            }
        });
        frame2.getContentPane().add(bn2, BorderLayout.NORTH);
        bs2 = new JButton("Show dialog");
        bs2.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                JOptionPane.showInternalMessageDialog(pane2, "Dialog2");
            }
        });
        frame2.getContentPane().add(bs2, BorderLayout.SOUTH);

        frame1.setVisible(true);
        frame2.setVisible(true);
    }

    private void getLocations()
    {
        bn1Loc = bn1.getLocationOnScreen();
        bn1Loc.translate(bn1.getWidth() / 2, bn1.getHeight() / 2);

        bn2Loc = bn2.getLocationOnScreen();
        bn2Loc.translate(bn2.getWidth() / 2, bn2.getHeight() / 2);

        bs1Loc = bs1.getLocationOnScreen();
        bs1Loc.translate(bs1.getWidth() / 2, bs1.getHeight() / 2);

        bs2Loc = bs2.getLocationOnScreen();
        bs2Loc.translate(bs2.getWidth() / 2, bs2.getHeight() / 2);
    }

    private void mouseClick(Robot r, Point p)
        throws Exception
    {
        r.mouseMove(p.x, p.y);
        r.mousePress(InputEvent.BUTTON1_MASK);
        r.mouseRelease(InputEvent.BUTTON1_MASK);
        r.waitForIdle();
    }

    private void start()
        throws Exception
    {
        r.setAutoDelay(200);

        unblocked1 = false;
        mouseClick(r, bn1Loc);
        if (!unblocked1)
        {
            throw new RuntimeException("Test FAILED: frame1 must be unblocked, if no modal internal frames are shown");
        }

        unblocked2 = false;
        mouseClick(r, bn2Loc);
        if (!unblocked2)
        {
            throw new RuntimeException("Test FAILED: frame2 must be unblocked, if no modal internal frame is shown in it");
        }

        mouseClick(r, bs1Loc);

        unblocked1 = false;
        mouseClick(r, bn1Loc);
        if (unblocked1)
        {
            throw new RuntimeException("Test FAILED: frame1 must be blocked, if a modal internal frame is shown in it");
        }

        unblocked2 = false;
        mouseClick(r, bn2Loc);
        if (!unblocked2)
        {
            throw new RuntimeException("Test FAILED: frame2 must be unblocked, if no modal internal frame is shown in it");
        }

        mouseClick(r, bs2Loc);

        unblocked2 = false;
        mouseClick(r, bn2Loc);
        if (unblocked2)
        {
            throw new RuntimeException("Test FAILED: frame2 must be blocked, if a modal internal frame is shown in it");
        }
    }

    private static ModalInternalFrameTest test;

    public static void main(String[] args)
        throws Exception
    {
        r = new Robot();
        test = new ModalInternalFrameTest();
        SwingUtilities.invokeAndWait(new Runnable()
        {
            public void run()
            {
                test.init();
            }
        });
        r.waitForIdle();
        SwingUtilities.invokeAndWait(new Runnable()
        {
            public void run()
            {
                test.getLocations();
            }
        });
        test.start();
    }
}
