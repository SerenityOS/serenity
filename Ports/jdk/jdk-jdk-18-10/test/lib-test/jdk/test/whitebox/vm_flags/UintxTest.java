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
 * @test UintxTest
 * @bug 8028756
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management/sun.management
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm/timeout=600 -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI UintxTest
 * @summary testing of WB::set/getUintxVMFlag()
 * @author igor.ignatyev@oracle.com
 */
import jdk.test.lib.Platform;

public class UintxTest {
    private static final String FLAG_NAME = "VerifyGCStartAt";
    private static final String FLAG_DEBUG_NAME = "CodeCacheMinimumUseSpace";
    private static final Long[] TESTS = {0L, 100L, (long) Integer.MAX_VALUE,
        (1L << 32L) - 1L, 1L << 32L};
    private static final Long[] EXPECTED_64 = TESTS;
    private static final Long[] EXPECTED_32 = {0L, 100L,
        (long) Integer.MAX_VALUE, (1L << 32L) - 1L, 0L};

    public static void main(String[] args) throws Exception {
        VmFlagTest.runTest(FLAG_NAME, TESTS,
            Platform.is64bit() ? EXPECTED_64 : EXPECTED_32,
            VmFlagTest.WHITE_BOX::setUintxVMFlag,
            VmFlagTest.WHITE_BOX::getUintxVMFlag);
        VmFlagTest.runTest(FLAG_DEBUG_NAME, VmFlagTest.WHITE_BOX::getUintxVMFlag);
    }
}

