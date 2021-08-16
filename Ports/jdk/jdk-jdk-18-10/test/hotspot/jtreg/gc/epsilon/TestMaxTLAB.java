/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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

package gc.epsilon;

/**
 * @test TestMaxTLAB
 * @requires vm.gc.Epsilon
 * @summary Check EpsilonMaxTLAB options
 * @bug 8212177
 *
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -Xmx128m -XX:+UseEpsilonGC -XX:EpsilonMaxTLABSize=1     gc.epsilon.TestMaxTLAB
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -Xmx128m -XX:+UseEpsilonGC -XX:EpsilonMaxTLABSize=1K    gc.epsilon.TestMaxTLAB
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -Xmx128m -XX:+UseEpsilonGC -XX:EpsilonMaxTLABSize=1M    gc.epsilon.TestMaxTLAB
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -Xmx128m -XX:+UseEpsilonGC -XX:EpsilonMaxTLABSize=12345 gc.epsilon.TestMaxTLAB
 *
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -Xmx128m -XX:+UseEpsilonGC -XX:EpsilonMaxTLABSize=1     -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=16 gc.epsilon.TestMaxTLAB
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -Xmx128m -XX:+UseEpsilonGC -XX:EpsilonMaxTLABSize=1K    -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=16 gc.epsilon.TestMaxTLAB
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -Xmx128m -XX:+UseEpsilonGC -XX:EpsilonMaxTLABSize=1M    -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=16 gc.epsilon.TestMaxTLAB
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -Xmx128m -XX:+UseEpsilonGC -XX:EpsilonMaxTLABSize=12345 -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=16 gc.epsilon.TestMaxTLAB
 */

public class TestMaxTLAB {
    static Object sink;

    public static void main(String[] args) throws Exception {
        for (int c = 0; c < 1000; c++) {
            sink = new byte[c];
        }
    }
}
