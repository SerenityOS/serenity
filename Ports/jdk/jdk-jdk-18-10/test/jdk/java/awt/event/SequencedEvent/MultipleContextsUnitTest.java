/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.InvocationEvent;
import java.lang.reflect.Constructor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import sun.awt.AppContext;
import sun.awt.SunToolkit;

/**
 * @test
 * @bug 8204142
 * @author Sergey Bylokhov
 * @key headful
 * @modules java.desktop/java.awt:open java.desktop/sun.awt
 * @run main/othervm/timeout=30 MultipleContextsUnitTest
 */
public final class MultipleContextsUnitTest {

    private static final int COUNT = 20;
    private static final AppContext[] apps = new AppContext[COUNT];
    private static final CountDownLatch go = new CountDownLatch(1);
    private static final CountDownLatch end = new CountDownLatch(COUNT);

    private static volatile int createSENumber = 0;
    private static volatile int dispatchSENumber = 0;

    public static void main(final String[] args) throws Exception {
        for (int i = 0; i < COUNT; i++) {
            Thread t = testThread(i);
            t.start();
            t.join();
        }

        for (AppContext app : apps) {
            SunToolkit.postEvent(app, new InvocationEvent(new Object(), () -> {
                try {
                    go.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        // eventOne - created first, but posted last
        AWTEvent eventOne = getSequencedEvent();
        {
            // eventTwo and eventThree - posted in the reverse order
            AppContext app = apps[1];
            AWTEvent eventTwo = getSequencedEvent();
            AWTEvent eventThree = getSequencedEvent();
            SunToolkit.postEvent(app, eventThree);
            SunToolkit.postEvent(app, eventTwo);
            SunToolkit.postEvent(app, new InvocationEvent(new Object(), () -> {
                System.err.println(AppContext.getAppContext());
                end.countDown();
            }));
        }

        for (int i = 2; i < apps.length; i++) {
            // eventTwo and eventThree - posted in the correct order
            AppContext app = apps[i];

            AWTEvent eventTwo = getSequencedEvent();
            SunToolkit.postEvent(app, eventTwo);

            AtomicReference<Boolean> called1 = new AtomicReference(false);
            AtomicReference<Boolean> called2 = new AtomicReference(false);
            int num1 = createSENumber;
            SunToolkit.postEvent(app, new InvocationEvent(new Object(), () -> {
                if (dispatchSENumber < num1) {
                    throw new RuntimeException("Dispatched too early");
                }
                called1.set(true);
                if (called2.get()) {
                    throw new RuntimeException("Second event is called before first");
                }
            }));
            AWTEvent eventThree = getSequencedEvent();
            SunToolkit.postEvent(app, eventThree);
            int num2 = createSENumber;
            SunToolkit.postEvent(app, new InvocationEvent(new Object(), () -> {
                if (dispatchSENumber < num2) {
                    throw new RuntimeException("Dispatched too early");
                }
                called2.set(true);
                if (!called1.get()) {
                    throw new RuntimeException("First event is not called before second");
                }
                System.err.println(AppContext.getAppContext());
                end.countDown();
            }));
        }



        // eventOne should flush all EDT
        SunToolkit.postEvent(apps[0], eventOne);
        SunToolkit.postEvent(apps[0], new InvocationEvent(new Object(), () -> {
            System.err.println(AppContext.getAppContext());
            end.countDown();
        }));

        go.countDown();

        System.err.println("Start to wait");
        end.await();
        System.err.println("End to wait");
    }

    private static Thread testThread(int index) {
        final ThreadGroup group = new ThreadGroup("TG " + index);
        return new Thread(group, () -> {
            apps[index] = SunToolkit.createNewAppContext();
        });
    }

    private static AWTEvent getSequencedEvent()
    {
        int num = createSENumber++;

        InvocationEvent wrapMe = new InvocationEvent(new Object(), () -> {
            if (num != dispatchSENumber++) {
                System.err.println("num: " + num);
                System.err.println("dispatchSENumber: " + dispatchSENumber);
                throw new RuntimeException("Wrong order");
            }
        });

        try {
            /*
             * SequencedEvent is a package private class, which cannot be instantiated
             * by importing. So use reflection to create an instance.
             */
            Class<? extends AWTEvent> seqClass = (Class<? extends AWTEvent>) Class.forName("java.awt.SequencedEvent");
            Constructor<? extends AWTEvent>
                    seqConst = seqClass.getConstructor(AWTEvent.class);
            seqConst.setAccessible(true);
            return seqConst.newInstance(wrapMe);
        } catch (Throwable err) {
            throw new RuntimeException("Unable to instantiate SequencedEvent",err);
        }
    }
}
