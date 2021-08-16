/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test LFGarbageCollectedTest
 * @bug 8046703
 * @key randomness
 * @library /lib/testlibrary /java/lang/invoke/common
 * @summary Test verifies that lambda forms are garbage collected
 * @author kshefov
 * @build jdk.test.lib.TimeLimitedRunner
 * @build TestMethods
 * @build LambdaFormTestCase
 * @build LFGarbageCollectedTest
 * @run main/othervm -Xmx64m
 *                   -XX:SoftRefLRUPolicyMSPerMB=0
 *                   -XX:+HeapDumpOnOutOfMemoryError
 *                   -DHEAP_DUMP=false
 *                   LFGarbageCollectedTest
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.Map;

/**
 * Lambda forms garbage collection test class.
 */
public final class LFGarbageCollectedTest extends LambdaFormTestCase {
    private static boolean HEAP_DUMP = Boolean.getBoolean("HEAP_DUMP");

    /**
     * Constructor for a lambda forms garbage collection test case.
     *
     * @param testMethod A method from {@code j.l.i.MethodHandles} class that
     * returns a {@code j.l.i.MethodHandle} instance.
     */
    public LFGarbageCollectedTest(TestMethods testMethod) {
        super(testMethod);
    }

    PhantomReference ph;
    ReferenceQueue rq = new ReferenceQueue();
    MethodType mtype;
    Map<String, Object> data;

    @Override
    public void doTest() {
        try {
            TestMethods testCase = getTestMethod();
            data = testCase.getTestCaseData();
            MethodHandle adapter;
            try {
                adapter = testCase.getTestCaseMH(data, TestMethods.Kind.ONE);
            } catch (NoSuchMethodException ex) {
                throw new Error("Unexpected exception", ex);
            }
            mtype = adapter.type();
            Object lambdaForm = INTERNAL_FORM.invoke(adapter);
            if (lambdaForm == null) {
                throw new Error("Unexpected error: Lambda form of the method handle is null");
            }

            String kind = KIND_FIELD.get(lambdaForm).toString();
            if (kind.equals("IDENTITY")) {
                // Ignore identity LambdaForms.
                return;
            }

            ph = new PhantomReference(lambdaForm, rq);
            lambdaForm = null;
            adapter = null;

            collectLambdaForm();
        } catch (IllegalAccessException | IllegalArgumentException |
                InvocationTargetException ex) {
            throw new Error("Unexpected exception", ex);
        }
    }


    private void collectLambdaForm() throws IllegalAccessException {
        // Usually, 2 System.GCs are necessary to enqueue a SoftReference.
        System.gc();
        System.gc();

        Reference ref = null;
        for (int i = 0; i < 10; i++) {
            try {
                ref = rq.remove(1000);
            } catch (InterruptedException e) {
                /* ignore */
            }
            if (ref != null) {
                break;
            }
            System.gc(); // If the reference hasn't been queued yet, trigger one more GC.
        }

        if (ref == null) {
            dumpTestData();
            System.err.println("Method type: " + mtype);
            System.err.println("LambdaForm:  " + REF_FIELD.get(ph));

            if (HEAP_DUMP) {
                // Trigger OOM to force heap dump for post-mortem analysis.
                val = new long[1_000_000_000];
            }
            throw new AssertionError("Error: LambdaForm is not garbage collected");
        };
    }

    private void dumpTestData() {
        System.err.println("Test case: " + getTestMethod());
        for (String s : data.keySet()) {
            System.err.printf("\t%20s => %s\n", s, data.get(s));
        }
    }

    private static long[] val;

    /**
     * Main routine for lambda forms garbage collection test.
     *
     * @param args Accepts no arguments.
     */
    public static void main(String[] args) {
        // The "identity", "constant", "arrayElementGetter" and "arrayElementSetter"
        // methods should be removed from this test,
        // because their lambda forms are stored in a static field and are not GC'ed.
        // There can be only a finite number of such LFs for each method,
        // so no memory leak happens.
        EnumSet<TestMethods> testMethods = EnumSet.complementOf(EnumSet.of(
                TestMethods.IDENTITY,
                TestMethods.CONSTANT,
                TestMethods.ARRAY_ELEMENT_GETTER,
                TestMethods.ARRAY_ELEMENT_SETTER,
                TestMethods.EXACT_INVOKER,
                TestMethods.INVOKER));
        LambdaFormTestCase.runTests(LFGarbageCollectedTest::new, testMethods);
    }
}
