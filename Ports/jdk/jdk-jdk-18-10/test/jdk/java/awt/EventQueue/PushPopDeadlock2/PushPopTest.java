/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4913324
  @author Oleg Sukhodolsky: area=eventqueue
  @modules java.desktop/sun.awt
  @run main/timeout=30 PushPopTest
*/

import java.awt.*;
import java.awt.event.*;
import java.util.EmptyStackException;
import sun.awt.SunToolkit;

public class PushPopTest {

    public static Frame frame;
    public static void main(String[] args) {
        frame = new Frame("");
        frame.pack();

        Runnable dummy = new Runnable() {
                public void run() {
                    System.err.println("Dummy is here.");
                    System.err.flush();
                }
            };
        EventQueue seq = Toolkit.getDefaultToolkit().getSystemEventQueue();
        MyEventQueue1 eq1 = new MyEventQueue1();
        MyEventQueue2 eq2 = new MyEventQueue2();
        EventQueue.invokeLater(dummy);

        seq.push(eq1);
        EventQueue.invokeLater(dummy);

        eq1.push(eq2);
        EventQueue.invokeLater(dummy);
        Runnable runnable = new Runnable() {
                public void run() {
                    System.err.println("Dummy from SunToolkit");
                    System.err.flush();
                }
            };
        InvocationEvent ie = new InvocationEvent(eq2, runnable, null, false);
//        System.err.println(ie);
        SunToolkit.postEvent(SunToolkit.targetToAppContext(frame), ie);
        eq1.pop();
        frame.dispose();
    }
}

class MyEventQueue1 extends EventQueue {

    public void pop() {
        super.pop();
    }
}

class MyEventQueue2 extends EventQueue {

    protected void pop() {
        System.err.println("pop2()");
        Thread.dumpStack();
        try {
            EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                        Runnable runnable = new Runnable() {
                                public void run() {
                                    System.err.println("Dummy from pop");
                                    System.err.flush();
                                }
                             };
                        InvocationEvent ie = new InvocationEvent(MyEventQueue2.this, runnable, null, false);
                        SunToolkit.postEvent(SunToolkit.targetToAppContext(PushPopTest.frame), ie);
                        postEvent(ie);
                    }
                });
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } catch (java.lang.reflect.InvocationTargetException ie) {
            ie.printStackTrace();
        }
        super.pop();
    }
}
