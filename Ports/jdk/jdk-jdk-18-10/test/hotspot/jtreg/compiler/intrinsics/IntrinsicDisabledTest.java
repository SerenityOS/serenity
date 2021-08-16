/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8138651
 *
 * @requires !vm.graal.enabled
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:DisableIntrinsic=_putCharVolatile,_putInt
 *                   -XX:DisableIntrinsic=_putIntVolatile
 *                   -XX:CompileCommand=option,jdk.internal.misc.Unsafe::putChar,ccstrlist,DisableIntrinsic,_getCharVolatile,_getInt
 *                   -XX:CompileCommand=option,jdk.internal.misc.Unsafe::putCharVolatile,ccstrlist,DisableIntrinsic,_getIntVolatile
 *                   compiler.intrinsics.IntrinsicDisabledTest
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:ControlIntrinsic=-_putCharVolatile,-_putInt
 *                   -XX:ControlIntrinsic=-_putIntVolatile
 *                   -XX:CompileCommand=ControlIntrinsic,jdk.internal.misc.Unsafe::putChar,-_getCharVolatile,-_getInt
 *                   -XX:CompileCommand=ControlIntrinsic,jdk.internal.misc.Unsafe::putCharVolatile,-_getIntVolatile
 *                   compiler.intrinsics.IntrinsicDisabledTest
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:ControlIntrinsic=+_putIntVolatile,+_putCharVolatile,+_putInt
 *                   -XX:DisableIntrinsic=_putCharVolatile,_putInt
 *                   -XX:DisableIntrinsic=_putIntVolatile
 *                   -XX:CompileCommand=ControlIntrinsic,jdk.internal.misc.Unsafe::putChar,+_getCharVolatile,+_getInt
 *                   -XX:CompileCommand=ControlIntrinsic,jdk.internal.misc.Unsafe::putCharVolatile,+_getIntVolatile
 *                   -XX:CompileCommand=DisableIntrinsic,jdk.internal.misc.Unsafe::putChar,_getCharVolatile,_getInt
 *                   -XX:CompileCommand=DisableIntrinsic,jdk.internal.misc.Unsafe::putCharVolatile,_getIntVolatile
 *                   compiler.intrinsics.IntrinsicDisabledTest
*/

package compiler.intrinsics;

import jdk.test.lib.Platform;
import sun.hotspot.WhiteBox;
import compiler.whitebox.CompilerWhiteBoxTest;

import java.lang.reflect.Executable;
import java.util.Objects;

public class IntrinsicDisabledTest {

    private static final WhiteBox wb = WhiteBox.getWhiteBox();

    /* Determine if tiered compilation is enabled. */
    private static final boolean TIERED_COMPILATION = wb.getBooleanVMFlag("TieredCompilation");

    private static final int TIERED_STOP_AT_LEVEL = wb.getIntxVMFlag("TieredStopAtLevel").intValue();

    /* This test uses several methods from jdk.internal.misc.Unsafe. The method
     * getMethod() returns a different Executable for each different
     * combination of its input parameters. There are eight possible
     * combinations, getMethod can return an Executable representing
     * the following methods: putChar, putCharVolatile, getChar,
     * getCharVolatile, putInt, putIntVolatile, getInt,
     * getIntVolatile. These methods were selected because they can
     * be intrinsified by both the C1 and the C2 compiler.
     */
    static Executable getMethod(boolean isChar, boolean isPut, boolean isVolatile) throws RuntimeException {
        Executable aMethod;
        String methodTypeName = isChar ? "Char" : "Int";

        try {
            Class aClass = Class.forName("jdk.internal.misc.Unsafe");
            if (isPut) {
                aMethod = aClass.getDeclaredMethod("put" + methodTypeName + (isVolatile ? "Volatile" : ""),
                                                   Object.class,
                                                   long.class,
                                                   isChar ? char.class : int.class);
            } else {
                aMethod = aClass.getDeclaredMethod("get" + methodTypeName + (isVolatile ? "Volatile" : ""),
                                                   Object.class,
                                                   long.class);
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Test bug, method is unavailable. " + e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Test bug, class is unavailable. " + e);
        }

        return aMethod;
    }

    public static void test(int compLevel) {

        Executable putChar = getMethod(true, /*isPut =*/ true, /*isVolatile = */ false);
        Executable getChar = getMethod(true, /*isPut =*/ false, /*isVolatile = */ false);
        Executable putCharVolatile = getMethod(true, /*isPut =*/ true, /*isVolatile = */ true);
        Executable getCharVolatile = getMethod(true, /*isPut =*/ false, /*isVolatile = */ true);
        Executable putInt = getMethod(false, /*isPut =*/ true, /*isVolatile = */ false);
        Executable getInt = getMethod(false, /*isPut =*/ false, /*isVolatile = */ false);
        Executable putIntVolatile = getMethod(false, /*isPut =*/ true, /*isVolatile = */ true);
        Executable getIntVolatile = getMethod(false, /*isPut =*/ false, /*isVolatile = */ true);

        /* Test if globally disabling intrinsics works. */
        if (!wb.isIntrinsicAvailable(putChar, compLevel)) {
            throw new RuntimeException("Intrinsic for [" + putChar.toGenericString() +
                                       "] is not available globally although it should be.");
        }

        if (wb.isIntrinsicAvailable(putCharVolatile, compLevel)) {
            throw new RuntimeException("Intrinsic for [" + putCharVolatile.toGenericString() +
                                       "] is available globally although it should not be.");
        }

        if (wb.isIntrinsicAvailable(putInt, compLevel)) {
            throw new RuntimeException("Intrinsic for [" + putInt.toGenericString() +
                                       "] is available globally although it should not be.");
        }

        if (wb.isIntrinsicAvailable(putIntVolatile, compLevel)) {
            throw new RuntimeException("Intrinsic for [" + putIntVolatile.toGenericString() +
                                       "] is available globally although it should not be.");
        }

        /* Test if disabling intrinsics on a per-method level
         * works. The method for which intrinsics are
         * disabled (the compilation context) is putChar. */
        if (!wb.isIntrinsicAvailable(getChar, putChar, compLevel)) {
            throw new RuntimeException("Intrinsic for [" + getChar.toGenericString() +
                                       "] is not available for intrinsification in [" +
                                       putChar.toGenericString() + "] although it should be.");
        }

        if (wb.isIntrinsicAvailable(getCharVolatile, putChar, compLevel)) {
            throw new RuntimeException("Intrinsic for [" + getCharVolatile.toGenericString() +
                                       "] is available for intrinsification in [" +
                                       putChar.toGenericString() + "] although it should not be.");
        }

        if (wb.isIntrinsicAvailable(getInt, putChar, compLevel)) {
            throw new RuntimeException("Intrinsic for [" + getInt.toGenericString() +
                                       "] is available for intrinsification in [" +
                                       putChar.toGenericString() + "] although it should not be.");
        }

        if (wb.isIntrinsicAvailable(getIntVolatile, putCharVolatile, compLevel)) {
            throw new RuntimeException("Intrinsic for [" + getIntVolatile.toGenericString() +
                                       "] is available for intrinsification in [" +
                                       putCharVolatile.toGenericString() + "] although it should not be.");
        }

        /* Test if disabling intrinsics on a per-method level
         * leaves those intrinsics enabled globally. */
        if (!wb.isIntrinsicAvailable(getCharVolatile, compLevel)) {
            throw new RuntimeException("Intrinsic for [" + getCharVolatile.toGenericString() +
                                       "] is not available globally although it should be.");
        }

        if (!wb.isIntrinsicAvailable(getInt, compLevel)) {
            throw new RuntimeException("Intrinsic for [" + getInt.toGenericString() +
                                       "] is not available globally although it should be.");
        }


        if (!wb.isIntrinsicAvailable(getIntVolatile, compLevel)) {
            throw new RuntimeException("Intrinsic for [" + getIntVolatile.toGenericString() +
                                       "] is not available globally although it should be.");
        }

        /* Test if disabling an intrinsic globally disables it on a
         * per-method level as well. */
        if (!wb.isIntrinsicAvailable(putChar, getChar, compLevel)) {
            throw new RuntimeException("Intrinsic for [" + putChar.toGenericString() +
                                       "] is not available for intrinsification in [" +
                                       getChar.toGenericString() + "] although it should be.");
        }

        if (wb.isIntrinsicAvailable(putCharVolatile, getChar, compLevel)) {
            throw new RuntimeException("Intrinsic for [" + putCharVolatile.toGenericString() +
                                       "] is available for intrinsification in [" +
                                       getChar.toGenericString() + "] although it should not be.");
        }

        if (wb.isIntrinsicAvailable(putInt, getChar, compLevel)) {
            throw new RuntimeException("Intrinsic for [" + putInt.toGenericString() +
                                       "] is available for intrinsification in [" +
                                       getChar.toGenericString() + "] although it should not be.");
        }

        if (wb.isIntrinsicAvailable(putIntVolatile, getChar, compLevel)) {
            throw new RuntimeException("Intrinsic for [" + putIntVolatile.toGenericString() +
                                       "] is available for intrinsification in [" +
                                       getChar.toGenericString() + "] although it should not be.");
        }
    }

    public static void main(String args[]) {
        if (Platform.isServer() && !Platform.isEmulatedClient() &&
                                   (TIERED_STOP_AT_LEVEL == CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION)) {
            if (TIERED_COMPILATION) {
                test(CompilerWhiteBoxTest.COMP_LEVEL_SIMPLE);
            }
            test(CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION);
        } else {
            test(CompilerWhiteBoxTest.COMP_LEVEL_SIMPLE);
        }
    }
}
