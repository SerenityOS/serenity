/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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
 * @test id=default
 * @summary Test clone barriers work correctly
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:+UseShenandoahGC
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:+UseShenandoahGC
 *                   -Xint
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:+UseShenandoahGC
 *                   -XX:-TieredCompilation
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:+UseShenandoahGC
 *                   -XX:TieredStopAtLevel=1
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:+UseShenandoahGC
 *                   -XX:TieredStopAtLevel=4
 *                   TestClone
 */

/*
 * @test id=default-verify
 * @summary Test clone barriers work correctly
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:+UseShenandoahGC
 *                   -XX:+ShenandoahVerify
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:+UseShenandoahGC
 *                   -XX:+ShenandoahVerify
 *                   -Xint
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:+UseShenandoahGC
 *                   -XX:+ShenandoahVerify
 *                   -XX:-TieredCompilation
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:+UseShenandoahGC
 *                   -XX:+ShenandoahVerify
 *                   -XX:TieredStopAtLevel=1
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:+UseShenandoahGC
 *                   -XX:+ShenandoahVerify
 *                   -XX:TieredStopAtLevel=4
 *                   TestClone
 */

/*
 * @test id=aggressive
 * @summary Test clone barriers work correctly
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *                   -Xint
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *                   -XX:-TieredCompilation
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *                   -XX:TieredStopAtLevel=1
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *                   -XX:TieredStopAtLevel=4
 *                   TestClone
 */

/*
 * @test id=no-coops
 * @summary Test clone barriers work correctly
 * @requires vm.gc.Shenandoah
 * @requires vm.bits == "64"
 *
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:-UseCompressedOops
 *                   -XX:+UseShenandoahGC
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:-UseCompressedOops
 *                   -XX:+UseShenandoahGC
 *                   -Xint
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:-UseCompressedOops
 *                   -XX:+UseShenandoahGC
 *                   -XX:-TieredCompilation
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:-UseCompressedOops
 *                   -XX:+UseShenandoahGC
 *                   -XX:TieredStopAtLevel=1
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:-UseCompressedOops
 *                   -XX:+UseShenandoahGC
 *                   -XX:TieredStopAtLevel=4
 *                   TestClone
 */

/*
 * @test id=no-coops-verify
 * @summary Test clone barriers work correctly
 * @requires vm.gc.Shenandoah
 * @requires vm.bits == "64"
 *
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:-UseCompressedOops
 *                   -XX:+UseShenandoahGC
 *                   -XX:+ShenandoahVerify
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:-UseCompressedOops
 *                   -XX:+UseShenandoahGC
 *                   -XX:+ShenandoahVerify
 *                   -Xint
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:-UseCompressedOops
 *                   -XX:+UseShenandoahGC
 *                   -XX:+ShenandoahVerify
 *                   -XX:-TieredCompilation
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:-UseCompressedOops
 *                   -XX:+UseShenandoahGC
 *                   -XX:+ShenandoahVerify
 *                   -XX:TieredStopAtLevel=1
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:-UseCompressedOops
 *                   -XX:+UseShenandoahGC
 *                   -XX:+ShenandoahVerify
 *                   -XX:TieredStopAtLevel=4
 *                   TestClone
 */

/*
 * @test id=no-coops-aggressive
 * @summary Test clone barriers work correctly
 * @requires vm.gc.Shenandoah
 * @requires vm.bits == "64"
 *
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:-UseCompressedOops
 *                   -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:-UseCompressedOops
 *                   -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *                   -Xint
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:-UseCompressedOops
 *                   -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *                   -XX:-TieredCompilation
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:-UseCompressedOops
 *                   -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *                   -XX:TieredStopAtLevel=1
 *                   TestClone
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xms1g -Xmx1g
 *                   -XX:-UseCompressedOops
 *                   -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *                   -XX:TieredStopAtLevel=4
 *                   TestClone
 */


public class TestClone {

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 10000; i++) {
            Object[] src = new Object[i];
            for (int c = 0; c < src.length; c++) {
                src[c] = new Object();
            }
            testWith(src);
        }
    }

    static void testWith(Object[] src) {
        Object[] dst = src.clone();
        int srcLen = src.length;
        int dstLen = dst.length;
        if (srcLen != dstLen) {
            throw new IllegalStateException("Lengths do not match: " + srcLen + " vs " + dstLen);
        }
        for (int c = 0; c < src.length; c++) {
            Object s = src[c];
            Object d = dst[c];
            if (s != d) {
                throw new IllegalStateException("Elements do not match at " + c + ": " + s + " vs " + d);
            }
        }
    }
}
