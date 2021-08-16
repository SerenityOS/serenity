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

/*
 * @test
 * @key randomness
 *
 * @summary converted from VM Testbase metaspace/stressDictionary.
 * VM Testbase keywords: [nonconcurrent, javac]
 *
 * @library /vmTestbase /test/lib
 * @run main/othervm/timeout=600 metaspace.stressDictionary.StressDictionary -stressTime 30
 */

package metaspace.stressDictionary;

import java.util.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import nsk.share.gc.GCTestBase;
import nsk.share.test.*;
import vm.share.InMemoryJavaCompiler;

/**
 * There is a data structure named "dictionary" in class BlockFreelist. It stores
 * information about free memory blocks for further reusing. Allocation of new block goes
 * from dictionary only if dictionary is fat enough. (At the moment of test creation this limit is 64K.)
 * So to stress dictionary we should fill it permanently. The easiest way to fill the dictionary
 * is to fail class loading. This failed action will return allocated blocks to dictionary.
 *
 * There are two type of threads in this test: threads, failing classloading and threads,
 * loading regular classes and checking they work properly.
 */
public class StressDictionary extends GCTestBase {

    private static byte[] bytecode;

    private class FillingDictionaryWorker implements Callable<Object> {
        private final Random random;
        public FillingDictionaryWorker(long seed) {
            this.random = new Random(seed);
        }
        @Override
        public Object call() throws Exception {
            while (stresser.continueExecution()) {
                try {
                    byte[] badBytecode = bytecode.clone();
                    badBytecode[random.nextInt(badBytecode.length)] = (byte) 42;
                    classloader.define(badBytecode);
                } catch (Throwable e) {
                    // We can get ClassFormatError, ClassNotFoundException or anything else here
                }
            }
            return null;
        }
    }

    private class RegularWorker implements Callable<Object> {
        @Override
        public Object call() throws Exception {
            while (stresser.continueExecution()) {
                Class<?> c = classloader.define(bytecode);
                testClass(c);
            }
            return null;
        }
    }

    private static String[] args;

    private static final String methodName = "myMethod";

    private static final int NUMBER_OF_CORRUPTING_THREADS = 10;

    private static final int NUMBER_OF_METHOD_CALLS = 50;

    private static final int NUMBER_OF_NOT_CORRUPTING_THREADS = 10;

    private AtomicLong classesCounter = new AtomicLong(0);

    private volatile ClassloaderUnderTest classloader = new ClassloaderUnderTest();

    private Random random;

    private ExecutionController stresser;

    public static void main(String[] args) {
        StressDictionary.args = args;
        Tests.runTest(new StressDictionary(), args);
    }

    public void run() {
        random = new Random(runParams.getSeed());
        stresser = new Stresser(args);
        stresser.start(1);
        // Generate some bytecodes.
        bytecode = generateAndCompile();
        List<Callable<Object>> tasks = new LinkedList<Callable<Object>>();
        for (int i = 0; i < NUMBER_OF_CORRUPTING_THREADS; i++) {
            tasks.add(this.new FillingDictionaryWorker(random.nextLong()));
        }
        for (int i = 0; i < NUMBER_OF_NOT_CORRUPTING_THREADS; i++) {
            tasks.add(this.new RegularWorker());
        }
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<Object>> results = null;
        try {
            results = executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int act_results = results.size();
        int exp_results = NUMBER_OF_CORRUPTING_THREADS +
                          NUMBER_OF_NOT_CORRUPTING_THREADS;
        if (act_results == exp_results) {
            System.err.println("INFO: There are " + act_results + " results.");
        } else {
            throw new RuntimeException("Wrong # of results from invokeAll(); "
                                       + "exp_results=" + exp_results + "; "
                                       + "act_results=" + act_results + ".");
        }

        int cancelled_cnt = 0;
        int not_done_cnt = 0;
        for (int i = 0; i < act_results; i++) {
            if (!results.get(i).isDone()) {
                not_done_cnt++;
                System.err.println("ERROR: task #" + i + " is not done.");
            }
            if (results.get(i).isCancelled()) {
                cancelled_cnt++;
                System.err.println("ERROR: task #" + i + " was canceled.");
            }
        }

        if (cancelled_cnt == 0) {
            System.err.println("INFO: no tasks were cancelled.");
        }
        if (not_done_cnt == 0) {
            System.err.println("INFO: all tasks are done.");
        }
        if (cancelled_cnt != 0 && not_done_cnt != 0) {
            throw new RuntimeException(cancelled_cnt
                                       + " tasks were cancelled and "
                                       + not_done_cnt
                                       + " tasks are not done.");
        } else if (cancelled_cnt != 0) {
            throw new RuntimeException(cancelled_cnt
                                       + " tasks were cancelled.");
        } else if (not_done_cnt != 0) {
            throw new RuntimeException(not_done_cnt + " tasks are not done.");
        }
    }

    private byte[] generateAndCompile() {
        Map<String, CharSequence> sources = new HashMap<String, CharSequence>();
        String className = "MyClass" + classesCounter.incrementAndGet();
        sources.put(className, generateSource(className));
        return InMemoryJavaCompiler.compile(sources).values().iterator().next();
    }

    private CharSequence generateSource(String className) {
        return "public class " + className + " { " +
                        "public static String s1 = \"s1" + random.nextInt() + "\"; " +
                                        "public String s2 = \"s2" + random.nextInt() + "\"; " +
                                        "public String " + methodName + "() {return s1 + s2; } " +
                        "}";
    }

    private void testClass(Class<?> clazz) {
        try {
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(methodName)) {
                    for (int j = 0; j < NUMBER_OF_METHOD_CALLS; j++) {
                        m.invoke(clazz.newInstance());
                    }
                }
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("Class check failed: " + e.getMessage());
            e.printStackTrace();
            setFailed(true);
        }

    }

}
