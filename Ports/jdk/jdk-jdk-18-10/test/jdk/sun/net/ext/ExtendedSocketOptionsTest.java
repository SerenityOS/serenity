/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @test
 * @bug 8260366
 * @summary Verify that concurrent classloading of sun.net.ext.ExtendedSocketOptions and
 * jdk.net.ExtendedSocketOptions doesn't lead to a deadlock
 * @modules java.base/sun.net.ext:open
 *          jdk.net
 * @run testng/othervm ExtendedSocketOptionsTest
 * @run testng/othervm ExtendedSocketOptionsTest
 * @run testng/othervm ExtendedSocketOptionsTest
 * @run testng/othervm ExtendedSocketOptionsTest
 * @run testng/othervm ExtendedSocketOptionsTest
 */
public class ExtendedSocketOptionsTest {

    /**
     * Loads {@code jdk.net.ExtendedSocketOptions} and {@code sun.net.ext.ExtendedSocketOptions}
     * and invokes {@code sun.net.ext.ExtendedSocketOptions#getInstance()} concurrently in a thread
     * of their own and expects the classloading of both those classes
     * to succeed. Additionally, after these tasks are done, calls the
     * sun.net.ext.ExtendedSocketOptions#getInstance() and expects it to return a registered
     * ExtendedSocketOptions instance.
     */
    @Test
    public void testConcurrentClassLoad() throws Exception {
        final CountDownLatch taskTriggerLatch = new CountDownLatch(4);
        final List<Callable<?>> tasks = new ArrayList<>();
        tasks.add(new Task("jdk.net.ExtendedSocketOptions", taskTriggerLatch));
        tasks.add(new Task("sun.net.ext.ExtendedSocketOptions", taskTriggerLatch));
        // add a couple of tasks which call sun.net.ext.ExtendedSocketOptions#getInstance
        tasks.add(new GetInstanceTask(taskTriggerLatch));
        tasks.add(new GetInstanceTask(taskTriggerLatch));
        final ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        try {
            final Future<?>[] results = new Future[tasks.size()];
            // submit
            int i = 0;
            for (final Callable<?> task : tasks) {
                results[i++] = executor.submit(task);
            }
            // wait for completion
            for (i = 0; i < tasks.size(); i++) {
                results[i].get();
            }
        } finally {
            executor.shutdownNow();
        }
        // check that the sun.net.ext.ExtendedSocketOptions#getInstance() does indeed return
        // the registered instance
        final Object extSocketOptions = callSunNetExtSocketOptionsGetInstance();
        Assert.assertNotNull(extSocketOptions, "sun.net.ext.ExtendedSocketOptions#getInstance()" +
                " unexpectedly returned null");
        // now verify that each call to getInstance(), either in the tasks or here, returned the exact
        // same instance of ExtendedSocketOptions
        Assert.assertEquals(2, GetInstanceTask.extendedSocketOptionsInstances.size());
        for (final Object inst : GetInstanceTask.extendedSocketOptionsInstances) {
            Assert.assertSame(inst, extSocketOptions, "sun.net.ext.ExtendedSocketOptions#getInstance()" +
                    " returned different instances");
        }
    }

    /**
     * Reflectively calls sun.net.ext.ExtendedSocketOptions#getInstance() and returns
     * the result
     */
    private static Object callSunNetExtSocketOptionsGetInstance() throws Exception {
        final Class<?> k = Class.forName("sun.net.ext.ExtendedSocketOptions");
        return k.getDeclaredMethod("getInstance").invoke(null);
    }

    private static class Task implements Callable<Class<?>> {
        private final String className;
        private final CountDownLatch latch;

        private Task(final String className, final CountDownLatch latch) {
            this.className = className;
            this.latch = latch;
        }

        @Override
        public Class<?> call() {
            System.out.println(Thread.currentThread().getName() + " loading " + this.className);
            try {
                // let the other tasks know we are ready to trigger our work
                latch.countDown();
                // wait for the other task to let us know they are ready to trigger their work too
                latch.await();
                return Class.forName(this.className);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class GetInstanceTask implements Callable<Object> {
        // keeps track of the instances returned by calls to sun.nex.ext.ExtendedSocketOptions#getInstance()
        // by the GetInstanceTask(s)
        private static final List<Object> extendedSocketOptionsInstances = Collections.synchronizedList(new ArrayList<>());
        private final CountDownLatch latch;

        private GetInstanceTask(final CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public Object call() {
            System.out.println(Thread.currentThread().getName()
                    + " calling  sun.net.ext.ExtendedSocketOptions#getInstance()");
            try {
                // let the other tasks know we are ready to trigger our work
                latch.countDown();
                // wait for the other task to let us know they are ready to trigger their work too
                latch.await();
                // let's call getInstance on sun.net.ext.ExtendedSocketOptions
                final Object inst = callSunNetExtSocketOptionsGetInstance();
                extendedSocketOptionsInstances.add(inst);
                return inst;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}