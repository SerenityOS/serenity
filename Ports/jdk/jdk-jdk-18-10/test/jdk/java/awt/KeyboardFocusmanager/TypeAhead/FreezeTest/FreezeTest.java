/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4799136
 * @summary Tests that type-ahead for dialog works and doesn't block program
 * @author Dmitry.Cherepanov@SUN.COM area=awt.focus
 * @run main FreezeTest
 */

/*
 * Tests that type-ahead doesn't block program.
 */

import java.awt.AWTEvent;
import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Robot;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FreezeTest
{
    static Frame f;
    static Button b;
    static Dialog d;
    static TextField tf;
    static CountDownLatch robotLatch = new CountDownLatch(1);
    static Robot robot;
    static int click_count = 100;
    static int deliver_count = 0;

    public static void main(String args[]) throws Exception {
        FreezeTest test = new FreezeTest();
        try {
            test.init();
            test.start();
        } finally {
            if (d != null) {
                d.dispose();
            }
            if (f != null) {
                f.dispose();
            }
        }
    }

    public void init()
    {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
                public void eventDispatched(AWTEvent e) {
                    if (e instanceof KeyEvent){
                        deliver_count++;
                        System.err.println("key_event# "+deliver_count);
                    }

                    if (e instanceof InputEvent){
                        System.err.println(e.toString()+","+((InputEvent)e).getWhen());
                    }else{
                        System.err.println(e.toString());
                    }
                 }
            }, AWTEvent.KEY_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK);


        f = new Frame("frame");
        b = new Button("press");
        d = new Dialog(f, "dialog", true);
        tf = new TextField("");
        d.add(tf);
        d.pack();

        f.add(b);
        f.pack();
        b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.err.println(e.toString()+","+e.getWhen());
                    System.err.println("B pressed");
                    robotLatch.countDown();

                    EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                waitTillShown(d);
                                FreezeTest.this.d.toFront();
                                FreezeTest.this.moveMouseOver(d);
                            }
                        });
                    d.setVisible(true);
                }
            });

    }//End  init()

    public void start () throws Exception
    {
        robot = new Robot();

        f.setVisible(true);
        waitTillShown(b);
        System.err.println("b is shown");
        f.toFront();
        moveMouseOver(f);
        robot.waitForIdle();
        makeFocused(b);
        robot.waitForIdle();
        System.err.println("b is focused");

        robot.keyPress(KeyEvent.VK_SPACE);
        robot.keyRelease(KeyEvent.VK_SPACE);
        boolean ok = robotLatch.await(1, TimeUnit.SECONDS);
        if(!ok) {
            throw new RuntimeException("Was B button pressed?");
        }

        for (int i = 0; i < click_count; i++){
            System.err.println("click# "+(i+1));
            robot.keyPress(KeyEvent.VK_SPACE);
            robot.delay(10);
            robot.keyRelease(KeyEvent.VK_SPACE);
            robot.delay(50);
        }

        robot.waitForIdle();

        int deliver_count = this.deliver_count;
        int expected_count = (click_count + 1) * 3;

        if (deliver_count != expected_count){
            System.err.println("deliver_count = "+deliver_count+" (!="+expected_count+")");
            throw new RuntimeException("incorrect behaviour");
        }
    }// start()

    private void moveMouseOver(Container c) {
        Point p = c.getLocationOnScreen();
        Dimension d = c.getSize();
        robot.mouseMove(p.x + (int)(d.getWidth()/2), p.y + (int)(d.getHeight()/2));
    }

    private void waitTillShown(Component c) {
        while (true) {
            try {
                Thread.sleep(100);
                c.getLocationOnScreen();
                break;
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                break;
            } catch (Exception e) {
            }
        }
    }
    private void makeFocused(Component comp) {
        if (comp.isFocusOwner()) {
            return;
        }
        final Semaphore sema = new Semaphore();
        final FocusAdapter fa = new FocusAdapter() {
                public void focusGained(FocusEvent fe) {
                    sema.raise();
                }
            };
        comp.addFocusListener(fa);
        comp.requestFocusInWindow();
        if (comp.isFocusOwner()) {
            return;
        }
        try {
            sema.doWait(3000);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        comp.removeFocusListener(fa);
        if (!comp.isFocusOwner()) {
            throw new RuntimeException("Can't make " + comp + " focused, current owner is " + KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
        }
    }

static class Semaphore {
    boolean state = false;
    int waiting = 0;
    public Semaphore() {
    }
    public synchronized void doWait() throws InterruptedException {
        if (state) {
            return;
        }
        waiting++;
        wait();
        waiting--;
    }
    public synchronized void doWait(int timeout) throws InterruptedException {
        if (state) {
            return;
        }
        waiting++;
        wait(timeout);
        waiting--;
    }
    public synchronized void raise() {
        state = true;
        if (waiting > 0) {
            notifyAll();
        }
    }
    public synchronized boolean getState() {
        return state;
    }
}
}
