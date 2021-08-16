/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTEvent;
import java.awt.ActiveEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @bug 4137796
 * @summary Checks that the posting of events whose sources are not Components
 *          does not corrupt the EventQueue.
 */
public class NonComponentSourcePost {

    public static final int COUNT = 100;
    public static CountDownLatch go = new CountDownLatch(COUNT);

    public static void main(String[] args) throws Throwable {
        EventQueue q = new EventQueue();
        for (int i = 0; i < COUNT; i++) {
            q.postEvent(new NewActionEvent());
        }
        if (!go.await(30, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timeout");
        }
        AWTEvent event = q.peekEvent();
        if (event != null) {
            throw new Exception("Non null event: " + event);
        }

        if (NewActionEvent.result != NewActionEvent.sum) {
            throw new Exception("result: " + NewActionEvent.result +
                                "  sum: " + NewActionEvent.sum);
        }
        // Simple way to shutdown the AWT machinery
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(q);
    }

    static class NewActionEvent extends ActionEvent implements ActiveEvent {
        static int counter = 1;
        static int sum = 0;
        static int result = 0;

        int myval;

        public NewActionEvent() {
            super("", ACTION_PERFORMED, "" + counter);
            myval = counter++;
            sum += myval;
        }

        public synchronized void dispatch() {
            result += myval;
            try {
                wait(100);
            } catch (InterruptedException e) {
            }
            go.countDown();
        }
    }
}
