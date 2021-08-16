/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, Red Hat Inc. All rights reserved.
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

/**
 * @test
 * @bug 8026049 8151163
 * @summary Verify that byte buffers are correctly accessed.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 *
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:-UseUnalignedAccesses -Djdk.test.lib.random.seed=42
 *      HeapByteBufferTest
 * @run main/othervm -Djdk.test.lib.random.seed=42
 *      HeapByteBufferTest
 */

public class HeapByteBufferTest extends ByteBufferTest {

    public HeapByteBufferTest(long iterations, boolean direct) {
        super(iterations, direct);
    }

    public static void main(String[] args) {
        // The number of iterations is high to ensure that tiered
        // compilation kicks in all the way up to C2.
        long iterations = 100000;
        if (args.length > 0)
            iterations = Long.parseLong(args[0]);

        new HeapByteBufferTest(iterations, false).run();
    }
}
