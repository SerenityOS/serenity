/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4023283
 * @key headful
 * @summary Checks that an Error which propogates up to the EventDispatch
 * loop does not crash AWT.
 * @author Andrei Dmitriev: area=awt.event
 * @library ../../regtesthelpers
 * @modules java.desktop/sun.awt
 * @build Util
 * @run main LoopRobustness
 */

import java.awt.*;
import java.awt.event.*;

import sun.awt.SunToolkit;

import test.java.awt.regtesthelpers.Util;

public class LoopRobustness {

    final static long TIMEOUT = 5000;
    final static Object LOCK = new Object();

    public static int clicks = 0;
    public static volatile boolean notifyOccured = false;
    public static volatile boolean otherExceptionsCaught = false;

    public static void main(String [] args) throws Exception {
        SunToolkit.createNewAppContext();

        ThreadGroup mainThreadGroup = Thread.currentThread().getThreadGroup();

        long at;
        //wait for a TIMEOUT giving a chance to a new Thread above to accomplish its stuff.
        synchronized (LoopRobustness.LOCK) {
            new Thread(new TestThreadGroup(mainThreadGroup, "TestGroup"), new Impl()).start();
            at = System.currentTimeMillis();
            try {
                while (!notifyOccured && (System.currentTimeMillis() - at < TIMEOUT)) {
                    LoopRobustness.LOCK.wait(1000);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Test interrupted.", e);
            }
        }

        if (!notifyOccured) {
            //notify doesn't occur after a reasonable time.
            throw new RuntimeException("Test FAILED: second thread hasn't notified MainThread");
        }

        //now wait for two clicks
        at = System.currentTimeMillis();
        while(System.currentTimeMillis() - at < TIMEOUT && clicks < 2) {
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
                throw new RuntimeException("Test interrupted.", e);
            }
        }
        if (clicks != 2) {
            throw new RuntimeException("Test FAILED: robot should press button twice");
        }
        if (otherExceptionsCaught) {
            throw new RuntimeException("Test FAILED: unexpected exceptions caught");
        }
    }
}

class Impl implements Runnable{
    static Robot robot;
    public void run() {
        SunToolkit.createNewAppContext();

        Button b = new Button("Press me to test the AWT-Event Queue thread");
        Frame lr = new Frame("ROBUST FRAME");
        lr.setBounds(100, 100, 300, 100);
        b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    LoopRobustness.clicks++;
                    //throwing an exception in Static Initializer
                    System.out.println(HostileCrasher.aStaticMethod());
                }
            });
        lr.add(b);
        lr.setVisible(true);

        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException("Test interrupted.", e);
        }
        Util.waitForIdle(robot);

        synchronized (LoopRobustness.LOCK){
            LoopRobustness.LOCK.notify();
            LoopRobustness.notifyOccured = true;
        }

        int i = 0;
        while (i < 2) {
            robot.mouseMove(b.getLocationOnScreen().x + b.getWidth()/2,
                            b.getLocationOnScreen().y + b.getHeight()/2);
            Util.waitForIdle(robot);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            Util.waitForIdle(robot);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            Util.waitForIdle(robot);
            i++;
        }
    }
}

class TestThreadGroup extends ThreadGroup {
    TestThreadGroup(ThreadGroup threadGroup, String name) {
        super(threadGroup, name);
    }

    public void uncaughtException(Thread thread, Throwable e) {
        System.out.println("Exception caught: " + e);
        e.printStackTrace(System.out);
        System.out.flush();
        if ((e instanceof ExceptionInInitializerError) ||
            (e instanceof NoClassDefFoundError))
        {
            // These two are expected
            return;
        }
        LoopRobustness.otherExceptionsCaught = true;
    }
}

class HostileCrasher {
    static {
        if (Math.random() >= 0.0) {
            throw new RuntimeException("Die, AWT-Event Queue thread!");
        }
    }
    public static String aStaticMethod() {
        return "hello, world";
    }
}
