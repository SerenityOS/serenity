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

import java.util.function.BiConsumer;
import java.util.function.Function;
import jdk.test.whitebox.WhiteBox;
import sun.management.*;
import com.sun.management.*;
import jdk.test.lib.Asserts;
import java.lang.management.ManagementFactory;

public final class VmFlagTest<T> {
    public static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    private static final String NONEXISTENT_FLAG = "NonexistentFlag";
    private final String flagName;
    private final BiConsumer<T, T> test;
    private final BiConsumer<String, T> set;
    private final Function<String, T> get;
    private final boolean isPositive;

    protected VmFlagTest(String flagName, BiConsumer<String, T> set,
            Function<String, T> get, boolean isPositive) {
        this.flagName = flagName;
        this.set = set;
        this.get = get;
        this.isPositive = isPositive;
        if (isPositive) {
            test = this::testWritePositive;
        } else {
            test = this::testWriteNegative;
        }
    }

    private void setNewValue(T value) {
        set.accept(flagName, value);
    }

    private T getValue() {
        return get.apply(flagName);
    }

    protected static <T> void runTest(String existentFlag, T[] tests,
            BiConsumer<String, T> set, Function<String, T> get) {
        runTest(existentFlag, tests, tests, set, get);
    }

    protected static <T> void runTest(String existentFlag, Function<String, T> get) {
        runTest(existentFlag, null, null, null, get);
    }

    protected static <T> void runTest(String existentFlag, T[] tests,
            T[] results, BiConsumer<String, T> set, Function<String, T> get) {
        if (existentFlag != null) {
            new VmFlagTest(existentFlag, set, get, true).test(tests, results);
        }
        new VmFlagTest(NONEXISTENT_FLAG, set, get, false).test(tests, results);
    }

    public final void test(T[] tests, T[] results) {
        if (isPositive) {
            testRead();
        }
        if (tests != null) {
            Asserts.assertEQ(tests.length, results.length, "[TESTBUG] tests.length != results.length");
            for (int i = 0, n = tests.length ; i < n; ++i) {
                test.accept(tests[i], results[i]);
            }
        }
    }

    protected String getVMOptionAsString() {
        if (WHITE_BOX.isConstantVMFlag(flagName) || WHITE_BOX.isLockedVMFlag(flagName)) {
          // JMM cannot access debug flags in product builds or locked flags,
          // use whitebox methods to get such flags value.
          return asString(getValue());
        }
        HotSpotDiagnosticMXBean diagnostic
                = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        VMOption tmp;
        try {
            tmp = diagnostic.getVMOption(flagName);
        } catch (IllegalArgumentException e) {
            tmp = null;
        }
        return tmp == null ? null : tmp.getValue();
    }

    private String testRead() {
        String value = getVMOptionAsString();
        Asserts.assertNotNull(value);
        Asserts.assertEQ(value, asString(getValue()));
        Asserts.assertEQ(value, asString(WHITE_BOX.getVMFlag(flagName)));
        return value;
    }

    private void testWritePositive(T value, T expected) {
        setNewValue(value);
        String newValue = testRead();
        Asserts.assertEQ(newValue, asString(expected));
    }

    private void testWriteNegative(T value, T expected) {
        // Should always return false for non-existing flags
        Asserts.assertFalse(WHITE_BOX.isConstantVMFlag(flagName));
        Asserts.assertFalse(WHITE_BOX.isLockedVMFlag(flagName));
        String oldValue = getVMOptionAsString();
        Asserts.assertEQ(oldValue, asString(getValue()));
        Asserts.assertEQ(oldValue, asString(WHITE_BOX.getVMFlag(flagName)));
        setNewValue(value);
        String newValue = getVMOptionAsString();
        Asserts.assertEQ(oldValue, newValue);
    }

    private String asString(Object value) {
        return value == null ? null : "" + value;
    }
}
