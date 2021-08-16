/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 * @bug 8022595
 * @summary JSR292: deadlock during class loading of MethodHandles, MethodHandleImpl & MethodHandleNatives
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @run main/othervm compiler.jsr292.ConcurrentClassLoadingTest
 */

package compiler.jsr292;

import jdk.test.lib.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ConcurrentClassLoadingTest {
    int numThreads = 0;
    CyclicBarrier l;
    private static final Random rand = Utils.getRandomInstance();

    public static void main(String[] args) throws Throwable {
        ConcurrentClassLoadingTest test = new ConcurrentClassLoadingTest();
        test.parseArgs(args);
        test.run();
    }

    void parseArgs(String[] args) {
        int i = 0;
        while (i < args.length) {
            String flag = args[i];
            switch(flag) {
                case "-numThreads":
                    numThreads = Integer.parseInt(args[++i]);
                    break;
                default:
                    throw new Error("Unknown flag: " + flag);
            }
            ++i;
        }
    }

    void init() {
        if (numThreads == 0) {
            numThreads = Runtime.getRuntime().availableProcessors();
        }

        l = new CyclicBarrier(numThreads + 1);

        System.out.printf("Threads: %d\n", numThreads);
    }

    final List<Loader> loaders = new ArrayList<>();

    void prepare() {
        List<String> c = new ArrayList<>(Arrays.asList(classNames));

        // Split classes between loading threads
        int count = (classNames.length / numThreads) + 1;
        for (int t = 0; t < numThreads; t++) {
            List<String> sel = new ArrayList<>();

            System.out.printf("Thread #%d:\n", t);
            for (int i = 0; i < count; i++) {
                if (c.isEmpty()) {
                    break;
                }

                int k = rand.nextInt(c.size());
                String elem = c.remove(k);
                sel.add(elem);
                System.out.printf("\t%s\n", elem);
            }
            loaders.add(new Loader(sel));
        }

        // Print diagnostic info when the test hangs
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                boolean alive = false;
                for (Loader l : loaders) {
                    if (!l.isAlive())  continue;

                    if (!alive) {
                        System.out.println("Some threads are still alive:");
                        alive = true;
                    }

                    System.out.println(l.getName());
                    for (StackTraceElement elem : l.getStackTrace()) {
                        System.out.println("\t"+elem.toString());
                    }
                }
            }
        });
    }

    public void run() throws Throwable {
        init();
        prepare();

        for (Loader loader : loaders) {
            loader.start();
        }

        l.await();

        for (Loader loader : loaders) {
            loader.join();
        }
    }

    class Loader extends Thread {
        List<String> classes;

        public Loader(List<String> classes) {
            this.classes = classes;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                l.await();

                for (String name : classes) {
                    Class.forName(name).getName();
                }
            } catch (ClassNotFoundException | BrokenBarrierException | InterruptedException e) {
                throw new Error(e);
            }
        }
    }

    final static String[] classNames = {
            "java.lang.invoke.CallSite",
            "java.lang.invoke.ConstantCallSite",
            "java.lang.invoke.LambdaConversionException",
            "java.lang.invoke.LambdaMetafactory",
            "java.lang.invoke.MethodHandle",
            "java.lang.invoke.MethodHandleInfo",
            "java.lang.invoke.MethodHandleProxies",
            "java.lang.invoke.MethodHandles",
            "java.lang.invoke.MethodType",
            "java.lang.invoke.MutableCallSite",
            "java.lang.invoke.SerializedLambda",
            "java.lang.invoke.SwitchPoint",
            "java.lang.invoke.VolatileCallSite",
            "java.lang.invoke.WrongMethodTypeException"
    };
}
