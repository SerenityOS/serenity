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
 *      -XX:+IgnoreUnrecognizedVMOptions -XX:+UseCountTrailingZerosInstruction
 *      compiler.intrinsics.bmi.verifycode.TZcntTestI
 */
package compiler.intrinsics.bmi.verifycode;

import compiler.intrinsics.bmi.TestTzcntI;

import java.lang.reflect.Method;

public class TZcntTestI extends BmiIntrinsicBase.BmiTestCase_x64 {

    protected TZcntTestI(Method method) {
        super(method);
        instrMask = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        instrPattern = new byte[]{(byte) 0xF3, (byte) 0x0F, (byte) 0xBC};

        instrMask_x64 = new byte[]{(byte) 0xFF, (byte) 0x00, (byte) 0xFF, (byte) 0xFF};
        instrPattern_x64 = new byte[]{(byte) 0xF3, (byte) 0x00, (byte) 0x0F, (byte) 0xBC};
    }

    public static void main(String[] args) throws Exception {
        // j.l.Integer and Long should be loaded to allow a compilation of the methods that use their methods
        System.out.println("class java.lang.Integer should be loaded. Proof: " + Integer.class);
        // Avoid uncommon traps.
        System.out.println("Num trailing zeroes: " + new TestTzcntI.TzcntIExpr().intExpr(12341341));
        BmiIntrinsicBase.verifyTestCase(TZcntTestI::new, TestTzcntI.TzcntIExpr.class.getDeclaredMethods());
    }

    @Override
    protected String getVMFlag() {
        return "UseCountTrailingZerosInstruction";
    }
}
