/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6359129
  @summary REGRESSION: Popup menus dont respond to selections when extend outside Applet
  @author oleg.sukhodolsky area=awt.grab
  @modules java.desktop/java.awt.peer
           java.desktop/sun.awt
  @library /java/awt/patchlib  ../../regtesthelpers
  @build java.desktop/java.awt.Helper
  @build Util UtilInternal
  @run main EmbeddedFrameTest1
*/

/**
 * EmbeddedFrameTest1.java
 *
 * summary: REGRESSION: Popup menus dont respond to selections when extend outside Applet
 */

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Robot;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.AWTException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPopupMenu;

import test.java.awt.regtesthelpers.Util;
import test.java.awt.regtesthelpers.UtilInternal;

public class EmbeddedFrameTest1
{
    public static void main( String args[] ) throws AWTException
    {
        try {
            final Frame frame = new Frame("AWT Frame");
            frame.pack();
            frame.setSize(200,200);

            final Frame embedded_frame = UtilInternal.createEmbeddedFrame(frame);
            embedded_frame.setSize(200, 200);
            System.out.println("embedded_frame = " + embedded_frame);

            final JPopupMenu menu = new JPopupMenu();
            JButton item = new JButton("A button in popup");
            item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        System.out.println("Button pressed");
                    }
                });

            menu.add(item);

            final JButton btn = new JButton("Press me to see popup");
            btn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        menu.show(btn, 0, btn.getHeight());
                    }
                });
            final Panel p = new Panel();
            p.setLayout(new BorderLayout());
            embedded_frame.add(p,BorderLayout.CENTER);
            embedded_frame.validate();
            p.add(btn);
            p.validate();
            frame.setVisible(true);
            Robot robot = new Robot();
            robot.waitForIdle();
            Util.clickOnComp(btn, robot);
            robot.waitForIdle();

            Util.clickOnComp(item, robot);
            robot.waitForIdle();
            if (item.getMousePosition() == null) {
                throw new RuntimeException("Popup was not closed (mouse above it)");
            }
            embedded_frame.remove(p);
            embedded_frame.dispose();
            frame.dispose();
        } catch (Throwable thr) {
            thr.printStackTrace();
            throw new RuntimeException("TEST FAILED: " + thr);
        }
    }
}
