/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+WhiteBoxAPI -XX:+LogCompilation
 *                   -XX:CompileCommand=compileonly,compiler.intrinsics.mathexact.sanity.MathIntrinsic*::execMathMethod
 *                   -XX:LogFile=hs_neg.log -XX:-UseMathExactIntrinsics
 *                   compiler.intrinsics.mathexact.sanity.DecrementExactIntTest
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+WhiteBoxAPI -XX:+LogCompilation
 *                   -XX:CompileCommand=compileonly,compiler.intrinsics.mathexact.sanity.MathIntrinsic*::execMathMethod
 *                   -XX:LogFile=hs.log -XX:+UseMathExactIntrinsics
 *                   compiler.intrinsics.mathexact.sanity.DecrementExactIntTest
 * @run driver compiler.testlibrary.intrinsics.Verifier hs_neg.log hs.log
 */

package compiler.intrinsics.mathexact.sanity;

public class DecrementExactIntTest {

    public static void main(String[] args) throws Exception {
        new IntrinsicBase.IntTest(MathIntrinsic.IntIntrinsic.Decrement).test();
    }
}
