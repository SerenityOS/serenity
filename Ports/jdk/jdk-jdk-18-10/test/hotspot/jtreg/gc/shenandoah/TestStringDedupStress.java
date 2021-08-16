/*
 * Copyright (c) 2017, 2021, Red Hat, Inc. All rights reserved.
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

/*
 * @test id=passive
 * @summary Test Shenandoah string deduplication implementation
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @modules java.base/java.lang:open
 *          java.management
 *
 * @run main/othervm -Xmx1g -Xlog:gc+stats -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahDegeneratedGC
 *      TestStringDedupStress
 *
 * @run main/othervm -Xmx1g -Xlog:gc+stats -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:-ShenandoahDegeneratedGC
 *      TestStringDedupStress
 */

/*
 * @test id=default
 * @summary Test Shenandoah string deduplication implementation
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @modules java.base/java.lang:open
 *          java.management
 *
 * @run main/othervm -Xmx1g -Xlog:gc+stats -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication
 *      -XX:+UseShenandoahGC
 *      -DtargetStrings=3000000
 *      TestStringDedupStress
 *
 * @run main/othervm -Xmx1g -Xlog:gc+stats -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -DtargetStrings=2000000
 *      TestStringDedupStress
 *
 * @run main/othervm -Xmx1g -Xlog:gc+stats -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahOOMDuringEvacALot
 *      -DtargetStrings=2000000
 *      TestStringDedupStress
 *
 * @run main/othervm -Xmx1g -Xlog:gc+stats -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=compact
 *      TestStringDedupStress
 */

 /*
 * @test id=iu
 * @summary Test Shenandoah string deduplication implementation
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @modules java.base/java.lang:open
 *          java.management
 *
 * @run main/othervm -Xmx1g -Xlog:gc+stats -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      TestStringDedupStress
 *
 * @run main/othervm -Xmx1g -Xlog:gc+stats -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -DtargetStrings=2000000
 *      TestStringDedupStress
 *
 * @run main/othervm -Xmx1g -Xlog:gc+stats -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      -XX:+ShenandoahOOMDuringEvacALot
 *      -DtargetStrings=2000000
 *      TestStringDedupStress
 *
 * @run main/othervm -Xmx1g -Xlog:gc+stats -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahOOMDuringEvacALot
 *      -DtargetStrings=2000000
 *      TestStringDedupStress
 */

import java.lang.management.*;
import java.lang.reflect.*;
import java.util.*;
import jdk.test.lib.Utils;

public class TestStringDedupStress {
    private static Field valueField;

    private static final int TARGET_STRINGS = Integer.getInteger("targetStrings", 2_500_000);
    private static final long MAX_REWRITE_GC_CYCLES = 6;
    private static final long MAX_REWRITE_TIME = 30*1000; // ms

    private static final int UNIQUE_STRINGS = 20;

    static {
        try {
            valueField = String.class.getDeclaredField("value");
            valueField.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object getValue(String string) {
        try {
            return valueField.get(string);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class StringAndId {
        private String str;
        private int id;

        public StringAndId(String str, int id) {
            this.str = str;
            this.id = id;
        }

        public String str() {
            return str;
        }

        public int id() {
            return id;
        }
    }

    // Generate uniqueStrings number of strings
    private static void generateStrings(ArrayList<StringAndId> strs, int uniqueStrings) {
        Random rn = Utils.getRandomInstance();
        for (int u = 0; u < uniqueStrings; u++) {
            int n = rn.nextInt(uniqueStrings);
            strs.add(new StringAndId("Unique String " + n, n));
        }
    }

    private static int verifyDedupString(ArrayList<StringAndId> strs) {
        Map<Object, StringAndId> seen = new HashMap<>(TARGET_STRINGS*2);
        int total = 0;
        int dedup = 0;

        for (StringAndId item : strs) {
            total++;
            StringAndId existingItem = seen.get(getValue(item.str()));
            if (existingItem == null) {
                seen.put(getValue(item.str()), item);
            } else {
                if (item.id() != existingItem.id() ||
                        !item.str().equals(existingItem.str())) {
                    System.out.println("StringDedup error:");
                    System.out.println("id: " + item.id() + " != " + existingItem.id());
                    System.out.println("or String: " + item.str() + " != " + existingItem.str());
                    throw new RuntimeException("StringDedup Test failed");
                } else {
                    dedup++;
                }
            }
        }
        System.out.println("Dedup: " + dedup + "/" + total + " unique: " + (total - dedup));
        return (total - dedup);
    }

    static volatile ArrayList<StringAndId> astrs = new ArrayList<>();
    static GarbageCollectorMXBean gcCycleMBean;

    public static void main(String[] args) {
        Random rn = Utils.getRandomInstance();

        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if ("Shenandoah Cycles".equals(bean.getName())) {
                gcCycleMBean = bean;
                break;
            }
        }

        if (gcCycleMBean == null) {
            throw new RuntimeException("Can not find Shenandoah GC cycle mbean");
        }

        // Generate roughly TARGET_STRINGS strings, only UNIQUE_STRINGS are unique
        int genIters = TARGET_STRINGS / UNIQUE_STRINGS;
        for (int index = 0; index < genIters; index++) {
            generateStrings(astrs, UNIQUE_STRINGS);
        }

        long cycleBeforeRewrite = gcCycleMBean.getCollectionCount();
        long timeBeforeRewrite = System.currentTimeMillis();

        long loop = 1;
        while (true) {
            int arrSize = astrs.size();
            int index = rn.nextInt(arrSize);
            StringAndId item = astrs.get(index);
            int n = rn.nextInt(UNIQUE_STRINGS);
            item.str = "Unique String " + n;
            item.id = n;

            if (loop++ % 1000 == 0) {
                // enough GC cycles for rewritten strings to be deduplicated
                if (gcCycleMBean.getCollectionCount() - cycleBeforeRewrite >= MAX_REWRITE_GC_CYCLES) {
                    break;
                }

                // enough time is spent waiting for GC to happen
                if (System.currentTimeMillis() - timeBeforeRewrite >= MAX_REWRITE_TIME) {
                    break;
                }
            }
        }
        verifyDedupString(astrs);
    }
}
