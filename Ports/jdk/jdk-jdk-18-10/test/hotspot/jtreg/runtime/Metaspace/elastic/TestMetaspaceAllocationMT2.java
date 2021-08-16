/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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
 *
 */

/*
 * This is a stress test for allocating from a single MetaspaceArena from
 *  multiple threads, optionally with reserve limit (mimicking the non-expandable CompressedClassSpace)
 * or commit limit (mimimcking MaxMetaspaceSize).
 *
 * The test threads will start to allocate from the Arena, and occasionally deallocate.
 * The threads run with a safety allocation max; if reached (or, if the underlying arena
 * hits either commit or reserve limit, if given) they will switch to deallocation and then
 * kind of float at the allocation ceiling, alternating between allocation and deallocation.
 *
 * We test with various flags, to exercise all 3 reclaim policies (none, balanced (default)
 * and aggessive) as well as one run with allocation guards enabled.
 *
 * We also set MetaspaceVerifyInterval very low to trigger many verifications in debug vm.
 *
 */

/*
 * @test id=debug-default
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @key randomness
 * @requires (vm.debug == true)
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm/timeout=400
 *      -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:VerifyMetaspaceInterval=10
 *      TestMetaspaceAllocationMT2
 */

/*
 * @test id=debug-none
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @key randomness
 * @requires (vm.debug == true)
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm/timeout=400
 *      -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:VerifyMetaspaceInterval=10
 *      -XX:MetaspaceReclaimPolicy=none
 *      TestMetaspaceAllocationMT2
 */

/*
 * @test id=debug-aggressive
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @key randomness
 * @requires (vm.debug == true)
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm/timeout=400
 *      -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:VerifyMetaspaceInterval=10
 *      -XX:MetaspaceReclaimPolicy=aggressive
 *      TestMetaspaceAllocationMT2
 */

/*
 * @test id=debug-guard
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @key randomness
 * @requires (vm.debug == true)
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm/timeout=400
 *      -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:VerifyMetaspaceInterval=10
 *      -XX:+MetaspaceGuardAllocations
 *      TestMetaspaceAllocationMT2
 */

/*
 * @test id=ndebug-default
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @key randomness
 * @requires (vm.debug == false)
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm/timeout=400
 *      -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      TestMetaspaceAllocationMT2
 */

/*
 * @test id=ndebug-none
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @key randomness
 * @requires (vm.debug == false)
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm/timeout=400
 *      -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:MetaspaceReclaimPolicy=none
 *      TestMetaspaceAllocationMT2
 */

/*
 * @test id=ndebug-aggressive
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @key randomness
 * @requires (vm.debug == false)
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm/timeout=400
 *      -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:MetaspaceReclaimPolicy=aggressive
 *      TestMetaspaceAllocationMT2
 */

public class TestMetaspaceAllocationMT2 {

    public static void main(String[] args) throws Exception {

        final long testAllocationCeiling = 1024 * 1024 * 6; // 8m words = 64M on 64bit
        final int numThreads = 4;
        final int seconds = 10;

        for (int i = 0; i < 3; i ++) {

            long commitLimit = (i == 1) ? 1024 * 256 : 0;

            // Note: reserve limit must be a multiple of Metaspace::reserve_alignment_words()
            //  (512K on 64bit, 1M on 32bit)
            long reserveLimit = (i == 2) ? 1024 * 1024 : 0;

            System.out.println("#### Test: ");
            System.out.println("#### testAllocationCeiling: " + testAllocationCeiling);
            System.out.println("#### numThreads: " + numThreads);
            System.out.println("#### seconds: " + seconds);
            System.out.println("#### commitLimit: " + commitLimit);
            System.out.println("#### reserveLimit: " + reserveLimit);
            System.out.println("#### ReclaimPolicy: " + Settings.settings().reclaimPolicy);
            System.out.println("#### guards: " + Settings.settings().usesAllocationGuards);

            MetaspaceTestContext context = new MetaspaceTestContext(commitLimit, reserveLimit);
            MetaspaceTestManyArenasManyThreads test = new MetaspaceTestManyArenasManyThreads(context, testAllocationCeiling, numThreads, seconds);

            try {
                test.runTest();
            } catch (RuntimeException e) {
                System.out.println(e);
                context.printToTTY();
                throw e;
            }

            context.destroy();

            System.out.println("#### Done. ####");
            System.out.println("###############");

        }

    }

}

