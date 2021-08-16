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

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @test
 * @bug 8263108
 * @summary Verify that concurrent classloading of java.lang.constant.DynamicConstantDesc and
 * java.lang.constant.ConstantDescs doesn't lead to a deadlock
 * @run main/othervm DynamicConstantDescTest
 * @run main/othervm DynamicConstantDescTest
 * @run main/othervm DynamicConstantDescTest
 * @run main/othervm DynamicConstantDescTest
 * @run main/othervm DynamicConstantDescTest
 */
// Implementation note: This test cannot use testng, since by the time this test gets a chance
// to trigger a concurrent classloading of the classes it's interested in, the testng infrastructure
// would already have loaded those classes in a single main thread.
public class DynamicConstantDescTest {

    /**
     * Loads {@code java.lang.constant.DynamicConstantDesc} and {@code java.lang.constant.ConstantDescs}
     * and invokes {@code java.lang.constant.DynamicConstantDesc#ofCanonical()} concurrently in a thread
     * of their own and expects the classloading of both those classes
     * to succeed.
     */
    public static void main(final String[] args) throws Exception {
        final CountDownLatch taskTriggerLatch = new CountDownLatch(4);
        final List<Callable<?>> tasks = new ArrayList<>();
        // a bunch of tasks - some doing just Class.forName and some
        // invoking DynamicConstantDesc.ofCanonical
        tasks.add(new Task("java.lang.constant.DynamicConstantDesc", taskTriggerLatch));
        tasks.add(new InvokeOfCanonical(taskTriggerLatch));
        tasks.add(new Task("java.lang.constant.ConstantDescs", taskTriggerLatch));
        tasks.add(new InvokeOfCanonical(taskTriggerLatch));
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

    enum MyEnum {A, B}

    private static class InvokeOfCanonical implements Callable<Object> {
        private final CountDownLatch latch;

        private InvokeOfCanonical(final CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public Object call() {
            System.out.println(Thread.currentThread().getName()
                    + " calling  DynamicConstantDesc.ofCanonical()");
            try {
                // let the other tasks know we are ready to trigger our work
                latch.countDown();
                // wait for the other task to let us know they are ready to trigger their work too
                latch.await();
                ConstantDesc desc = DynamicConstantDesc.ofCanonical(boostrapMethodForEnumConstant(),
                        "A", ClassDesc.of("DynamicConstantDescTest").nested("MyEnum"),
                        new ConstantDesc[0]);
                if (desc == null) {
                    throw new Exception("DynamicConstantDesc.ofCanonical unexpectedly returned null");
                }
                if (!MyEnum.A.equals(desc.resolveConstantDesc(MethodHandles.lookup()))) {
                    throw new Exception("DynamicConstantDesc.ofCanonical returned unexpected result " + desc);
                }
                return desc;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static DirectMethodHandleDesc boostrapMethodForEnumConstant() {
            ClassDesc[] args = {ClassDesc.of("java.lang.invoke.MethodHandles").nested("Lookup"),
                    ClassDesc.of("java.lang.String"),
                    ClassDesc.of("java.lang.Class")};
            return MethodHandleDesc.ofMethod(java.lang.constant.DirectMethodHandleDesc.Kind.STATIC,
                    ClassDesc.of("java.lang.invoke.ConstantBootstraps"),
                    "enumConstant", MethodTypeDesc.of(ClassDesc.of("java.lang.Enum"), new ClassDesc[0])
                            .insertParameterTypes(0, args));
        }

    }

}