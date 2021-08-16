/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @test LFSingleThreadCachingTest
 * @bug 8046703
 * @key randomness
 * @summary Test verifies that lambda forms are cached when run with single thread
 * @author kshefov
 * @library /lib/testlibrary /java/lang/invoke/common /test/lib
 * @modules java.base/java.lang.ref:open
 *          java.base/java.lang.invoke:open
 *          java.management
 * @build jdk.test.lib.TimeLimitedRunner
 * @build TestMethods
 * @build LambdaFormTestCase
 * @build LFCachingTestCase
 * @build LFSingleThreadCachingTest
 * @run main/othervm -XX:ReservedCodeCacheSize=128m LFSingleThreadCachingTest
 */

import java.lang.invoke.MethodHandle;
import java.util.EnumSet;
import java.util.Map;

/**
 * Single threaded lambda forms caching test class.
 */
public final class LFSingleThreadCachingTest extends LFCachingTestCase {

    /**
     * Constructor for a single threaded lambda forms caching test case.
     *
     * @param testMethod A method from {@code j.l.i.MethodHandles} class that
     * returns a {@code j.l.i.MethodHandle} instance.
     */
    public LFSingleThreadCachingTest(TestMethods testMethod) {
        super(testMethod);
    }

    @Override
    public void doTest() {
        MethodHandle adapter1;
        MethodHandle adapter2;
        Map<String, Object> data = getTestMethod().getTestCaseData();
        try {
            adapter1 = getTestMethod().getTestCaseMH(data, TestMethods.Kind.ONE);
            adapter2 = getTestMethod().getTestCaseMH(data, TestMethods.Kind.TWO);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new Error("Unexpected exception", ex);
        }
        checkLFCaching(adapter1, adapter2);
    }

    /**
     * Main routine for single threaded lambda forms caching test.
     *
     * @param args Accepts no arguments.
     */
    public static void main(String[] args) {
        LambdaFormTestCase.runTests(LFSingleThreadCachingTest::new,
                                    EnumSet.allOf(TestMethods.class));
    }
}
