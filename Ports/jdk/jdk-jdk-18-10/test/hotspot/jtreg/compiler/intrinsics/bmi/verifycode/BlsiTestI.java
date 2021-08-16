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
 * @bug 8031321
 * @requires vm.flavor == "server" & !vm.emulatedClient & !vm.graal.enabled
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -Xbatch -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:+IgnoreUnrecognizedVMOptions -XX:+UseBMI1Instructions
 *      compiler.intrinsics.bmi.verifycode.BlsiTestI
 */

package compiler.intrinsics.bmi.verifycode;

import compiler.intrinsics.bmi.TestBlsiI;

import java.lang.reflect.Method;

public class BlsiTestI extends BmiIntrinsicBase.BmiTestCase {

    protected BlsiTestI(Method method) {
        super(method);
        //from intel manual VEX.NDD.LZ.0F38.W0 F3 /3
        instrMask = new byte[]{
                (byte) 0xFF,
                (byte) 0x1F,
                (byte) 0x00,
                (byte) 0xFF,
                (byte) 0b0011_1000};
        instrPattern = new byte[]{
                (byte) 0xC4, // prefix for 3-byte VEX instruction
                (byte) 0x02, // 00010 implied 0F 38 leading opcode bytes
                (byte) 0x00,
                (byte) 0xF3,
                (byte) 0b0001_1000}; // bits 543 == 011 (3)
    }

    public static void main(String[] args) throws Exception {
        BmiIntrinsicBase.verifyTestCase(BlsiTestI::new, TestBlsiI.BlsiIExpr.class.getDeclaredMethods());
        BmiIntrinsicBase.verifyTestCase(BlsiTestI::new, TestBlsiI.BlsiICommutativeExpr.class.getDeclaredMethods());
    }
}
