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
 * @test RandomAllocationTest
 * @key stress randomness
 * @summary stressing code cache by allocating randomly sized "dummy" code blobs
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @build sun.hotspot.WhiteBox compiler.codecache.stress.Helper compiler.codecache.stress.TestCaseImpl
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=dontinline,compiler.codecache.stress.Helper$TestCase::method
 *                   -XX:-SegmentedCodeCache
 *                   compiler.codecache.stress.RandomAllocationTest
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=dontinline,compiler.codecache.stress.Helper$TestCase::method
 *                   -XX:+SegmentedCodeCache
 *                   compiler.codecache.stress.RandomAllocationTest
 */

package compiler.codecache.stress;

import sun.hotspot.code.BlobType;

import java.util.ArrayList;
import java.util.Random;
import jdk.test.lib.Utils;

public class RandomAllocationTest implements Runnable {
    private static final long CODE_CACHE_SIZE
            = Helper.WHITE_BOX.getUintxVMFlag("ReservedCodeCacheSize");
    private static final int MAX_BLOB_SIZE = (int) (CODE_CACHE_SIZE >> 7);
    private static final BlobType[] BLOB_TYPES
            = BlobType.getAvailable().toArray(new BlobType[0]);
    private final Random rng = Utils.getRandomInstance();

    public static void main(String[] args) {
        new CodeCacheStressRunner(new RandomAllocationTest()).runTest();
    }

    private final ArrayList<Long> blobs = new ArrayList<>();
    @Override
    public void run() {
        boolean allocate = blobs.isEmpty() || rng.nextBoolean();
        if (allocate) {
            int type = rng.nextInt(BLOB_TYPES.length);
            long addr = Helper.WHITE_BOX.allocateCodeBlob(
                    rng.nextInt(MAX_BLOB_SIZE), BLOB_TYPES[type].id);
            if (addr != 0) {
                blobs.add(addr);
            }
        } else {
            int index = rng.nextInt(blobs.size());
            Helper.WHITE_BOX.freeCodeBlob(blobs.remove(index));
        }
    }

}
