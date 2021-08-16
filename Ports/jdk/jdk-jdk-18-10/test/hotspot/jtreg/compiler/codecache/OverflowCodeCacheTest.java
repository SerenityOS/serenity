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
 * @test OverflowCodeCacheTest
 * @bug 8059550
 * @summary testing of code cache segments overflow
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:CompileCommand=compileonly,null::*
 *                   -XX:-SegmentedCodeCache
 *                   compiler.codecache.OverflowCodeCacheTest
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:CompileCommand=compileonly,null::*
 *                   -XX:+SegmentedCodeCache
 *                   compiler.codecache.OverflowCodeCacheTest
 */

package compiler.codecache;

import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;
import sun.hotspot.code.BlobType;
import sun.hotspot.code.CodeBlob;

import java.lang.management.MemoryPoolMXBean;
import java.util.ArrayList;
import java.util.EnumSet;

public class OverflowCodeCacheTest {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    public static void main(String[] args) {
        EnumSet<BlobType> blobTypes = BlobType.getAvailable();
        for (BlobType type : blobTypes) {
            new OverflowCodeCacheTest(type).test();
        }
    }

    private final BlobType type;
    private final MemoryPoolMXBean bean;
    private OverflowCodeCacheTest(BlobType type) {
        this.type = type;
        this.bean = type.getMemoryPool();
    }

    private void test() {
        System.out.printf("type %s%n", type);
        System.out.println("allocating till possible...");
        ArrayList<Long> blobs = new ArrayList<>();
        int compilationActivityMode = -1;
        try {
            long addr;
            int size = (int) (getHeapSize() >> 7);
            while ((addr = WHITE_BOX.allocateCodeBlob(size, type.id)) != 0) {
                blobs.add(addr);

                BlobType actualType = CodeBlob.getCodeBlob(addr).code_blob_type;
                if (actualType != type) {
                    // check we got allowed overflow handling
                    Asserts.assertTrue(type.allowTypeWhenOverflow(actualType),
                            type + " doesn't allow using " + actualType + " when overflow");
                }
            }
            /* now, remember compilationActivityMode to check it later, after freeing, since we
               possibly have no free cache for futher work */
            compilationActivityMode = WHITE_BOX.getCompilationActivityMode();
        } finally {
            for (Long blob : blobs) {
                WHITE_BOX.freeCodeBlob(blob);
            }
        }
        Asserts.assertNotEquals(compilationActivityMode, 1 /* run_compilation*/,
                "Compilation must be disabled when CodeCache(CodeHeap) overflows");
    }

    private long getHeapSize() {
        return bean.getUsage().getMax();
    }

}
