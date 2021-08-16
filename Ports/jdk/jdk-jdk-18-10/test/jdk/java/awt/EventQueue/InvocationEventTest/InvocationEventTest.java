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


import java.awt.*;
import java.awt.event.*;

/*
 * @test
 * @summary  To Test the following assertions in InvovationEvent.
 * 1.InvocationEvent when dispatched, should invoke the
 *   run() method of the Runnable Interface.
 * 2.If catchExceptions is false, Exception should be
 *   propagated up to the EventDispatchThread's dispatch loop.
 * 3.If catchExceptions is true, InvocationEvent.getExceptions()
 *   should return the exception thrown inside thr run() method.
 * 4.When InvocationEvent object is posted on to the EventQueue,
 *   InvocationEvent.dispatch() method should be invoked by the
 *   EventQueue.
 * 5.If the notifier object is not null, notifyAll() of the
 *   notifier object should be invoked when the run() method returns.
 * 6.To test whether the threads are invoked in the right order
 *   When InvocationEvents are nested.
 * 7.The getWhen method should return timestamp which is less than
 *   current System time and greater than the time before it has
 *   actually happened
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @run main InvocationEventTest
 */

public class InvocationEventTest {
    EventQueue eventQ1 = new EventQueue();

    Object lock = new Object();

    static final int delay = 5000;

    public volatile boolean notifierStatus = false;
    public Object notifierLock = new Object();

    public volatile boolean threadStatus = false;
    public volatile boolean childInvoked = false;

    public synchronized void doTest() throws Exception {
        // Testing assertions 1, 2 and 7:
        // 1.InvocationEvent when dispatched, should invoke the
        //   run() method of the Runnable Interface.
        // 2.If catchExceptions is false, Exception should be
        //   propagated up to the EventDispatchThread's dispatch loop.
        // 7.The getWhen method should return timestamp which is less than
        //   current System time and greater than the time before it has
        //   actually happened

        long timeBeforeInvoking = System.currentTimeMillis();

        Thread.sleep(10);

        InvocationEvent invoc = new InvocationEvent(this, () -> { threadStatus = true; }, lock, false);
        invoc.dispatch();

        Thread.sleep(10);

        if (!threadStatus) {
            synchronized (lock) {
                lock.wait(delay);
            }
        }

        // testing getException() when no exception is thrown
        if (invoc.getWhen() <= timeBeforeInvoking ||
                invoc.getWhen() >= System.currentTimeMillis()) {
            throw new RuntimeException("getWhen method is not getting the time at which event occured");
        }

        if (invoc.getException() != null) {
            throw new RuntimeException("InvocationEvent.getException() does not return null " +
                    "when catchException is false");
        }

        // testing the normal behaviour of InvocationEvent
        if (!threadStatus) {
            throw new RuntimeException("InvocationEvent when dispatched, did not" +
                    " invoke the run() of the Runnable interface  ");
        }
        threadStatus = false;

        // Testing assertion 3:
        // 3.If catchExceptions is true, InvocationEvent.getExceptions()
        //   should return the exception thrown inside the run() method.
        RuntimeException sampleExn = new RuntimeException(" test exception");

        invoc = new InvocationEvent(this, () -> { threadStatus = true; throw sampleExn; }, lock, true);
        invoc.dispatch();
        if (!threadStatus) {
            synchronized (lock) {
                lock.wait(delay);
            }
        }
        // testing getException() when exception is thrown
        // Should return the same exception thrown inside the run() method
        if (!invoc.getException().equals(sampleExn)) {
            throw new RuntimeException("getException() does not return " +
                    "the same Exception thrown inside the run() method ");
        }
        threadStatus = false;

        // Testing assertions 4 and 5:
        // 4.When InvocationEvent object is posted on to the EventQueue,
        //   InvocationEvent.dispatch() method should be invoked by the
        //   EventQueue.
        // 5.If the notifier object is not null, notifyAll() of the
        //   notifier object should be invoked when the run() method returns.

        Thread notify = new Thread(){
            public void run() {
                synchronized (this) {
                    try { wait(); } catch (InterruptedException e) { throw new RuntimeException(e); }
                }
                notifierStatus = true;
                synchronized (notifierLock) {
                    notifierLock.notifyAll();
                }
            }
        };
        notify.start();

        while (notify.getState() != Thread.State.WAITING)
            Thread.sleep(delay/5);

        InvocationEvent invocation = new InvocationEvent(this, () -> { }, (Object) notify, false);
        eventQ1.postEvent(invocation);

        while(!invocation.isDispatched())
            synchronized (notifierLock) {
                notifierLock.wait(delay);
            }

        while (notify.getState() != Thread.State.TERMINATED)
            Thread.sleep(delay/5);

        if (!notifierStatus) {
            throw new RuntimeException("Notifier object did not get notified" +
                    " When the run method of the Runnable returns ");
        }

        // Testing assertion 6:
        // 6.To test whether the threads are invoked in the right order
        //   When InvocationEvents are nested.
        Thread thread = new Thread(){
            public void run() {
                InvocationEvent evt = new InvocationEvent(this, () -> { childInvoked = true; }, (Object) this, false);
                new EventQueue().postEvent(evt);
                synchronized (this) {
                    try {
                        wait(delay);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                threadStatus = true;
            }
        };

        invocation = new InvocationEvent(this, thread, lock, false);

        eventQ1.postEvent(invocation);

        while (!invocation.isDispatched())
            synchronized (lock) {
                lock.wait(delay);
            }

        if (!threadStatus || !childInvoked) {
            throw new RuntimeException("Nesting of InvocationEvents when dispatched," +
                    " did not invoke the run() of the Runnables properly ");
        }
    }

    public static void main(String[] args) throws Exception {
        new InvocationEventTest().doTest();
    }
}

