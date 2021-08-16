/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.event.AWTEventListener;
import java.util.EventListener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.event.EventListenerList;

/**
 * @test
 * @bug 5031664
 * @summary EventListenerList.getXXX should always return up to date data
 */
public final class GetUpToDateData {

    static final EventListenerList listeners = new EventListenerList();

    static final EventListener o1 = new EventListener() {
    };
    static final AWTEventListener o2 = event -> {
    };

    public static void main(final String[] args) throws Exception {
        CountDownLatch go = new CountDownLatch(3);

        Thread t1 = new Thread(() -> {
            try {
                // Time to warm-up t3, t4, t5
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            listeners.add(EventListener.class, o1);
        });
        Thread t2 = new Thread(() -> {
            try {
                // Time to warm-up t3, t4, t5
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            listeners.add(AWTEventListener.class, o2);
        });

        Thread t3 = new Thread(() -> {
            while (listeners.getListenerCount() != 2) {
            }
            go.countDown();
        });
        Thread t4 = new Thread(() -> {
            while (listeners.getListeners(EventListener.class).length != 1
                    || listeners.getListeners(EventListener.class)[0] != o1) {
            }
            go.countDown();
        });
        Thread t5 = new Thread(() -> {
            while (listeners.getListeners(AWTEventListener.class).length != 1
                    || listeners.getListeners(AWTEventListener.class)[0] != o2) {
            }
            go.countDown();
        });

        t1.setDaemon(true);
        t2.setDaemon(true);
        t3.setDaemon(true);
        t4.setDaemon(true);
        t5.setDaemon(true);

        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        if (!go.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("The test hangs");
        }
    }
}
