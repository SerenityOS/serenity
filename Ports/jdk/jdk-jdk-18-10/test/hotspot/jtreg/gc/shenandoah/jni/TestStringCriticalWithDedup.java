/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
 *
 */

/* @test TestStringCriticalWithDedup
 * @summary Test string deduplication should not cause string critical to crash VM
 * @requires vm.gc.Shenandoah
 * @modules java.base/java.lang:open
 *
 * @run main/othervm/native -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xmx512m
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive -XX:+UseStringDeduplication -XX:-CompactStrings
 *      -XX:+ShenandoahVerify -XX:+ShenandoahDegeneratedGC -XX:ShenandoahTargetNumRegions=4096
 *      TestStringCriticalWithDedup
 *
 * @run main/othervm/native -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xmx512m
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive -XX:+UseStringDeduplication -XX:-CompactStrings
 *      -XX:+ShenandoahVerify -XX:-ShenandoahDegeneratedGC -XX:ShenandoahTargetNumRegions=4096
 *      TestStringCriticalWithDedup
 */

/* @test TestPinnedGarbage
 * @summary Test string deduplication should not cause string critical to crash VM
 * @requires vm.gc.Shenandoah
 * @modules java.base/java.lang:open
 *
 * @run main/othervm/native -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xmx512m
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive -XX:+UseStringDeduplication -XX:-CompactStrings
 *      -XX:ShenandoahTargetNumRegions=4096
 *      TestStringCriticalWithDedup
 *
 * @run main/othervm/native -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xmx256m
 *      -XX:+UseShenandoahGC -XX:+UseStringDeduplication -XX:-CompactStrings
 *      -XX:ShenandoahTargetNumRegions=4096 -XX:+ShenandoahVerify
 *      TestStringCriticalWithDedup
 */

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.lang.reflect.*;

public class TestStringCriticalWithDedup {
    private static Field valueField;

    static {
        System.loadLibrary("TestStringCriticalWithDedup");
        try {
            valueField = String.class.getDeclaredField("value");
            valueField.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final int NUM_RUNS      = 100;
    private static final int STRING_COUNT    = 1 << 16;
    private static final int LITTLE_GARBAGE_COUNT = 1 << 5;
    private static final int PINNED_STRING_COUNT = 1 << 4;

    private static native long pin(String s);
    private static native void unpin(String s, long p);


    private static volatile MyClass sink;
    public static void main(String[] args) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < NUM_RUNS; i++) {
            test(rng);
        }
    }

    private static Object getValue(String string) {
        try {
            return valueField.get(string);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void pissiblePinString(ThreadLocalRandom rng, List<Tuple> pinnedList, String s) {
        int oneInCounter = STRING_COUNT / PINNED_STRING_COUNT;
        if (rng.nextInt(oneInCounter) == 1) {
            long v = pin(s);
            Object value = getValue(s);
            pinnedList.add(new Tuple(s, value, v));
        }
    }

    private static void test(ThreadLocalRandom rng) {
        String[] strArray = new String[STRING_COUNT];
        List<Tuple> pinnedStrings = new ArrayList<>(PINNED_STRING_COUNT);
        for (int i = 0; i < STRING_COUNT; i++) {
            // Create some garbage inbetween, so strings can be scattered in
            // different regions
            createLittleGarbage(rng);

            strArray[i] = new String("Hello" + (i % 10));
            pissiblePinString(rng, pinnedStrings, strArray[i]);
        }

        // Let deduplication thread to run a bit
        try {
            Thread.sleep(10);
        } catch(Exception e) {
        }

        for (int i = 0; i < pinnedStrings.size(); i ++) {
            Tuple p = pinnedStrings.get(i);
            String s = p.getString();
            if (getValue(s) != p.getValue()) {
                System.out.println(getValue(s) + " != " + p.getValue());
                throw new RuntimeException("String value should be pinned");
            }
            unpin(p.getString(), p.getValuePointer());
        }
    }

    private static void createLittleGarbage(ThreadLocalRandom rng) {
        int count = rng.nextInt(LITTLE_GARBAGE_COUNT);
        for (int index = 0; index < count; index ++) {
            sink = new MyClass();
        }
    }

    private static class Tuple {
        String s;
        Object value;
        long   valuePointer;
        public Tuple(String s, Object value, long vp) {
            this.s = s;
            this.value = value;
            this.valuePointer = vp;
        }

        public String getString() {
            return s;
        }
        public Object getValue() { return value; }
        public long getValuePointer() {
            return valuePointer;
        }
    }

    private static class MyClass {
        public long[] payload = new long[10];
    }
}
