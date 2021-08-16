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
 * @test GetCodeHeapEntriesTest
 * @bug 8059624
 * @summary testing of WB::getCodeHeapEntries()
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:-SegmentedCodeCache
 *                   compiler.whitebox.GetCodeHeapEntriesTest
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:+SegmentedCodeCache
 *                   compiler.whitebox.GetCodeHeapEntriesTest
 */

package compiler.whitebox;

import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;
import sun.hotspot.code.BlobType;
import sun.hotspot.code.CodeBlob;

import java.util.Arrays;
import java.util.EnumSet;

public class GetCodeHeapEntriesTest {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final int SIZE = 1024;
    private static final String DUMMY_NAME = "WB::DummyBlob";
    private static EnumSet<BlobType> SEGMENTED_TYPES
            = EnumSet.complementOf(EnumSet.of(BlobType.All));

    public static void main(String[] args) {
        EnumSet<BlobType> blobTypes = BlobType.getAvailable();
        for (BlobType type : blobTypes) {
            new GetCodeHeapEntriesTest(type).test();
        }
    }

    private final BlobType type;
    private GetCodeHeapEntriesTest(BlobType type) {
        this.type = type;
    }

    private void test() {
        System.out.printf("type %s%n", type);
        long addr = WHITE_BOX.allocateCodeBlob(SIZE, type.id);
        Asserts.assertNE(0, addr, "allocation failed");
        CodeBlob[] blobs = CodeBlob.getCodeBlobs(type);
        Asserts.assertNotNull(blobs);
        CodeBlob blob = Arrays.stream(blobs)
                              .filter(GetCodeHeapEntriesTest::filter)
                              .findAny()
                              .orElse(null);
        Asserts.assertNotNull(blob);
        Asserts.assertEQ(blob.code_blob_type, type);
        Asserts.assertGTE(blob.size, SIZE);

        WHITE_BOX.freeCodeBlob(addr);
        blobs = CodeBlob.getCodeBlobs(type);
        long count = Arrays.stream(blobs)
                           .filter(GetCodeHeapEntriesTest::filter)
                           .count();
        Asserts.assertEQ(0L, count);
    }

    private static boolean filter(CodeBlob blob) {
        if (blob == null) {
            return false;
        }
        return DUMMY_NAME.equals(blob.name);
    }
}
