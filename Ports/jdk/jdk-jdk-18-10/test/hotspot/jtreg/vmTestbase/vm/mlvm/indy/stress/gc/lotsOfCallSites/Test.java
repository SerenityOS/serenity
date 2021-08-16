/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase vm/mlvm/indy/stress/gc/lotsOfCallSites.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.indy.stress.gc.lotsOfCallSites.Test
 *        vm.mlvm.indy.stress.gc.lotsOfCallSites.INDIFY_Testee
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   vm.mlvm.indy.stress.gc.lotsOfCallSites.Test
 */

package vm.mlvm.indy.stress.gc.lotsOfCallSites;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.HashSet;

import sun.hotspot.WhiteBox;

import nsk.share.test.Stresser;
import vm.mlvm.share.CustomClassLoaders;
import vm.mlvm.share.Env;
import vm.mlvm.share.MlvmTest;
import vm.share.FileUtils;
import vm.share.options.Option;

/**
 * The test creates a lot of CallSites by loading a class with a bootstrap method and invokedynamic
 * via a custom classloader in a loop.
 *
 * The test verifies that all CallSites are "delivered to heaven" by creating a PhantomReference per
 *  a CallSite and checking the number of references put into a queue.
 *
 */
public class Test extends MlvmTest {

    // TODO (separate bug should be filed): move this option to MlvmTest level
    @Option(name = "heapdump", default_value = "false", description = "Dump heap after test has finished")
    private boolean heapDumpOpt = false;

    @Option(name = "iterations", default_value = "100000", description = "Iterations: each iteration loads one new class")
    private int iterations = 100_000;

    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final int GC_COUNT = 6;
    private static final boolean TERMINATE_ON_FULL_METASPACE = false;

    private static final ReferenceQueue<CallSite> objQueue = new ReferenceQueue<CallSite>();
    private static final HashSet<PhantomReference<CallSite>> references = new HashSet<PhantomReference<CallSite>>();
    private static long loadedClassCount = 0;

    // We avoid direct references to the testee class to avoid loading it by application class loader
    // Otherwise the testee class is loaded both by the custom and the application class loaders,
    // and when java.lang.invoke.MH.COMPILE_THRESHOLD={0,1} is defined, the test fails with
    // "java.lang.IncompatibleClassChangeError: disagree on InnerClasses attribute"
    private static final String TESTEE_CLASS_NAME = Test.class.getPackage().getName() + "." + "INDIFY_Testee";
    private static final String TESTEE_REFERENCES_FIELD = "references";
    private static final String TESTEE_OBJQUEUE_FIELD = "objQueue";
    private static final String TESTEE_BOOTSTRAP_CALLED_FIELD = "bootstrapCalled";
    private static final String TESTEE_TARGET_CALLED_FIELD = "targetCalled";
    private static final String TESTEE_INDY_METHOD = "indyWrapper";

    private static int removeQueuedReferences() {
        int count = 0;
        Reference<? extends CallSite> r;
        while ((r = objQueue.poll()) != null) {
            if (!references.remove(r)) {
                Env.traceNormal("Reference " + r + " was not registered!");
            }
            ++count;
        }
        if (count > 0) {
            Env.traceVerbose("Removed " + count + " phantom references");
        } else {
            Env.traceDebug("Removed " + count + " phantom references");
        }
        return count;
    }

    private MemoryPoolMXBean getClassMetadataMemoryPoolMXBean() {
    MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
    for (MemoryPoolMXBean memPool : ManagementFactory.getMemoryPoolMXBeans()) {
            String name = memPool.getName();
        if ((name.contains("Compressed class space") || name.contains("Metaspace")) && memPool.getUsage() != null) {
                return memPool;
            }
        }
        return null;
    }

    @Override
    public boolean run() throws Throwable {
        setHeapDumpAfter(heapDumpOpt);

        final byte[] classBytes = FileUtils.readClass(TESTEE_CLASS_NAME);
        final MemoryPoolMXBean classMetadataPoolMXB = getClassMetadataMemoryPoolMXBean();
        final String memPoolName = classMetadataPoolMXB == null ? "" : classMetadataPoolMXB.getName();

        int removedEntries = 0;

        Stresser stresser = createStresser();
        stresser.start(iterations);
        try {
            while (stresser.continueExecution()) {
                stresser.iteration();

                iteration(classBytes, TESTEE_CLASS_NAME);
                removedEntries += removeQueuedReferences();

                if (stresser.getIteration() % 1000 == 0) {
                    Env.traceNormal("Iterations: " + stresser.getIteration() + " removed entries: " + removedEntries);
                    if (TERMINATE_ON_FULL_METASPACE && classMetadataPoolMXB != null) {
                        MemoryUsage mu = classMetadataPoolMXB.getUsage();
                        Env.traceNormal(memPoolName + " usage: " + mu);
                        if  (mu.getUsed() >= mu.getMax() * 9 / 10) {
                            Env.traceNormal(memPoolName + " is nearly out of space: " + mu + ". Terminating.");
                            break;
                        }
                    }
                }
            }

        } catch (OutOfMemoryError e) {
            Env.traceNormal(e, "Out of memory. This is OK");
        } finally {
            stresser.finish();
        }

        for (int i = 0; i < GC_COUNT; ++i) {
            WHITE_BOX.fullGC();
            Thread.sleep(500);
            removedEntries += removeQueuedReferences();
        }

        removedEntries += removeQueuedReferences();

        Env.traceNormal("Loaded classes: " + loadedClassCount
                      + "; References left in set: " + references.size()
                      + "; References removed from queue: " + removedEntries);

        if (references.size() != 0 || removedEntries != loadedClassCount) {
            Env.complain("Not all of the created CallSites were GC-ed");
            return false;
        }

        return true;
    }

    private void iteration(byte[] classBytes, String indyClassName) throws Throwable {
        ClassLoader cl = CustomClassLoaders.makeClassBytesLoader(classBytes, indyClassName);
        Class<?> c = cl.loadClass(indyClassName);
        ++loadedClassCount;

        if (c.getClassLoader() != cl) {
            throw new RuntimeException("Invalid class loader: " + c.getClassLoader() + "; required=" + cl);
        }

        Field vr = c.getDeclaredField(TESTEE_REFERENCES_FIELD);
        vr.set(null, references);

        Field voq = c.getDeclaredField(TESTEE_OBJQUEUE_FIELD);
        voq.set(null, objQueue);

        Field vbc = c.getDeclaredField(TESTEE_BOOTSTRAP_CALLED_FIELD);
        if (vbc.getBoolean(null)) {
            throw new RuntimeException(TESTEE_BOOTSTRAP_CALLED_FIELD + " flag should not be set. Not a fresh copy of the testee class?");
        }

        Field vt = c.getDeclaredField(TESTEE_TARGET_CALLED_FIELD);
        if (vt.getBoolean(null)) {
            throw new RuntimeException(TESTEE_TARGET_CALLED_FIELD + " flag should not be set. Not a fresh copy of the testee class?");
        }

        Method m = c.getDeclaredMethod(TESTEE_INDY_METHOD);
        m.invoke(null);

        if (!vbc.getBoolean(null) ) {
            throw new RuntimeException("Bootstrap method of the testee class was not called");
        }

        if (!vt.getBoolean(null) ) {
            throw new RuntimeException("Target method of the testee class was not called");
        }
    }

    public static void main(String[] args) {
        MlvmTest.launch(args);
    }
}
