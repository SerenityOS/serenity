/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Frame;
import javax.swing.JFrame;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
/*
 * @test
 * @key headful
 * @bug 8022302
 * @summary Set extendedState Frame.MAXIMIZED_BOTH for undecorated Frame and JFrame.
 *          Check if resulted size is equal to GraphicsEnvironment.getMaximumWindowBounds().
 *
 * @library /lib/client
 * @build ExtendedRobot
 * @run main MaximizedUndecorated
 */


public class MaximizedUndecorated {
    private Frame frame;
    private ExtendedRobot robot;
    public static void main(String args[]) {
        if (!Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.MAXIMIZED_BOTH)) {
            return;
        }
        MaximizedUndecorated test = new MaximizedUndecorated();
        boolean doPass = true;
        try{
            if( !test.doTest(true) ) {
                System.out.println("Actual bounds differ from Maximum Window Bounds for JFrame");
                doPass = false;
            }
            if( !test.doTest(false) ) {
                System.out.println("Actual bounds differ from Maximum Window Bounds for Frame");
                doPass = false;
            }
        }catch(Exception ie) {
            ie.printStackTrace();
            throw new RuntimeException("Interrupted or InvocationTargetException occured");
        }
        if(!doPass) {
            throw new RuntimeException("Actual bounds of undecorated frame differ from Maximum Windows Bounds for this platform");
        }
    }
    MaximizedUndecorated() {
        try {
            robot = new ExtendedRobot();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Cannot create robot");
        }
    }
    boolean doTest(boolean swingFrame) throws InterruptedException, InvocationTargetException {
        EventQueue.invokeAndWait( () -> {
            frame = swingFrame? new JFrame("Test Frame") : new Frame("Test Frame");
            frame.setLayout(new FlowLayout());
            frame.setBounds(50,50,300,300);
            frame.setUndecorated(true);
            frame.setVisible(true);
        });
        robot.waitForIdle(2000);
        EventQueue.invokeAndWait( () -> {
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        });
        robot.waitForIdle(2000);
        Rectangle actualBounds = frame.getBounds();
        Rectangle expectedBounds = GraphicsEnvironment.
               getLocalGraphicsEnvironment().getMaximumWindowBounds();
        EventQueue.invokeAndWait( () -> {
            frame.dispose();
        });

        return actualBounds.equals(expectedBounds);
    }
}
