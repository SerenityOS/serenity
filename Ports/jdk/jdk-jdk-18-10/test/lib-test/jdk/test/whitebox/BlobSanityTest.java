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
 * @test BlobSanityTest
 * @bug 8132980
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.management/sun.management
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI BlobSanityTest
 * @summary sanity testing of allocateCodeBlob, freeCodeBlob and getCodeBlob
 */


import jdk.test.whitebox.WhiteBox;

import java.util.function.Consumer;
import jdk.test.lib.Utils;

public class BlobSanityTest {

    private static void runTest(Consumer<Integer> consumer, int val, String testCaseName, Class<? extends Throwable>
            expectedException) {
            System.out.println("Calling " + testCaseName);
            Utils.runAndCheckException(() -> consumer.accept(val), expectedException);
            System.out.println("Looks ok");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Crash means that sanity check failed");

        WhiteBox wb = WhiteBox.getWhiteBox();

        runTest(wb::freeCodeBlob, 0, "wb::freeCodeBlob(0)", null);
        runTest(wb::getCodeBlob, 0, "wb::getCodeBlob(0)", NullPointerException.class);
        runTest(x -> wb.allocateCodeBlob(x, 0), -1, "wb::allocateCodeBlob(-1,0)", IllegalArgumentException.class);
    }
}
