/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test ReturnBlobToWrongHeapTest
 * @key stress
 * @summary Test if VM attempts to return code blobs to an incorrect code heap or to outside of the code cache.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @build sun.hotspot.WhiteBox compiler.codecache.stress.Helper compiler.codecache.stress.TestCaseImpl
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=dontinline,compiler.codecache.stress.Helper$TestCase::method
 *                   -XX:+SegmentedCodeCache
 *                   -XX:ReservedCodeCacheSize=16M
 *                   -XX:CodeCacheMinBlockLength=1
 *                   compiler.codecache.stress.ReturnBlobToWrongHeapTest
 */

package compiler.codecache.stress;

import sun.hotspot.code.BlobType;

import java.util.ArrayList;

public class ReturnBlobToWrongHeapTest {
    private static final long largeBlobSize = Helper.WHITE_BOX.getUintxVMFlag("ReservedCodeCacheSize") >> 6;
    private static final long codeCacheMinBlockLength = Helper.WHITE_BOX.getUintxVMFlag("CodeCacheMinBlockLength");
    private static final BlobType[] BLOB_TYPES = BlobType.getAvailable().toArray(new BlobType[0]);

    // Allocate blob in first code heap (the code heap with index 0).
    private static long allocate(int size) {
        return Helper.WHITE_BOX.allocateCodeBlob(size, BLOB_TYPES[0].id);
    }

    // Free blob.
    private static void free(long address) {
        Helper.WHITE_BOX.freeCodeBlob(address);
    }

    public static void main(String[] args) {
        if (codeCacheMinBlockLength == 1) {
            // start with allocating a small block
            long firstSegmentSizedAddress = 0;
            firstSegmentSizedAddress = allocate(0);
            if (firstSegmentSizedAddress == 0) {
                throw new RuntimeException("Test failed: Failed allocating first segment-sized blob");
            }

            // Fill first code heap with large blobs until allocation fails.
            long address;
            while ((address = allocate((int)largeBlobSize)) != 0) {
            }

            // Allocate segment-sized blocks in first code heap until it runs out
            // Remember the last one
            // Use the pre-allocated one as backup if the code cache is already completely full.
            long lastSegmentSizedAddress = firstSegmentSizedAddress;
            while ((address = allocate(0)) != 0) {
                lastSegmentSizedAddress = address;
            }

            if (lastSegmentSizedAddress == 0) {
                throw new RuntimeException("Test failed: Not possible to allocate segment-sized blob");
            }

            // Remove last segment-sized block from the first code heap.
            free(lastSegmentSizedAddress);
        } else {
            throw new RuntimeException("Test requires CodeCacheMinBlockLength==1; CodeCacheMinBlockLength is " +
                                       codeCacheMinBlockLength);
        }
    }
}
