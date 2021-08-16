/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress
 *
 * @summary converted from VM Testbase gc/gctests/LoadUnloadGC.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, monitoring]
 * VM Testbase readme:
 * In this test a 1000 classes are loaded and unloaded in a loop.
 * Class0 gets loaded which results in Class1 getting loaded and so on all
 * the way uptill class1000.  The classes should be unloaded whenever a
 * garbage collection takes place because their classloader is made unreachable
 * at the end of the each loop iteration. The loop is repeated 1000 times.
 *
 * @requires vm.opt.final.ClassUnloading
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.share.gc.ClassChain
 * @run main/othervm
 *      -XX:MaxMetaspaceSize=64M
 *      -XX:MetaspaceSize=32M
 *      -XX:CompressedClassSpaceSize=32M
 *      gc.gctests.LoadUnloadGC.LoadUnloadGC
 */

package gc.gctests.LoadUnloadGC;

import nsk.share.test.*;
import nsk.share.gc.*;
import nsk.share.classload.ClassPathNonDelegatingClassLoader;
import vm.share.monitoring.MemoryPoolFinder;

import java.io.*;
import java.util.*;
import java.lang.management.MemoryPoolMXBean;

/**
 * This test checks that classes are unloaded when loaded multiple times
 * with custom class loader.
 */
public class LoadUnloadGC extends ThreadedGCTest {
        private final String className = "nsk.share.gc.ClassChain";
        private int [] memory_reserve = new int[10000];

        private class Loader implements Runnable {
                private Class class_zero_class;
                private Object class_zero_object;

                public void run() {
                        try {
                                // load Class0 and instantiate it
                                // This will cause all thousand classes to get loaded
                                ClassPathNonDelegatingClassLoader loader = new ClassPathNonDelegatingClassLoader();
                                class_zero_class = loader.loadClass(className, false);
                                class_zero_object = class_zero_class.newInstance();
                                // Set all references to null . This should cause a GC
                                // which should forces an unloading of all these
                                // unreferenced classes.
                                class_zero_class = null;
                                class_zero_object = null;
                                loader = null;
                        } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                        } catch (InstantiationException e) {
                                throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                        }
                }
        }

        protected Runnable createRunnable(int i) {
                return new Loader();
        }

        protected static int getThreadCount() {
                MemoryPoolMXBean bean = MemoryPoolFinder.findPool(MemoryPoolFinder.METASPACE);
                ClassPathNonDelegatingClassLoader loader = new ClassPathNonDelegatingClassLoader();
                long used = bean.getUsage().getUsed();
                long free = 0;
                int classesCount = 1000;
                int classesToLoad = 10;
                if(bean.getUsage().getMax() == -1) {
                        throw new RuntimeException("Metaspace size should be limited for this test.");
                }
                try {
                        for(int i = 1; i <= classesToLoad; i++) {
                                loader.loadClass("nsk.share.gc.Class"+i);
                        }
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
                used = bean.getUsage().getUsed() - used;
                free = (bean.getUsage().getMax() - bean.getUsage().getUsed())/2;
                return Math.min((int)(0.95*free/(classesCount/classesToLoad*used)),
                                Runtime.getRuntime().availableProcessors());
        }

        public static void main(String args[]) {
                int threadCount = getThreadCount();
                if (Arrays.binarySearch(args,"-t") < 0) {
                       args = Arrays.copyOf(args,args.length+2);
                       args[args.length-2] = "-t";
                       args[args.length-1] = Integer.toString(threadCount);
                }
                GC.runTest(new LoadUnloadGC(), args);
        }
}
