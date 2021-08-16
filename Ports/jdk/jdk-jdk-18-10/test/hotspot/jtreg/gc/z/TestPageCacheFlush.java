/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.z;

/*
 * @test TestPageCacheFlush
 * @requires vm.gc.Z
 * @summary Test ZGC page cache flushing
 * @library /test/lib
 * @run driver gc.z.TestPageCacheFlush
 */

import java.util.LinkedList;
import jdk.test.lib.process.ProcessTools;

public class TestPageCacheFlush {
    static class Test {
        private static final int K = 1024;
        private static final int M = K * K;
        private static volatile LinkedList<byte[]> keepAlive;

        public static void fillPageCache(int size) {
            System.out.println("Begin allocate (" + size + ")");

            keepAlive = new LinkedList<>();

            try {
                for (;;) {
                    keepAlive.add(new byte[size]);
                }
            } catch (OutOfMemoryError e) {
                keepAlive = null;
                System.gc();
            }

            System.out.println("End allocate (" + size + ")");
        }

        public static void main(String[] args) throws Exception {
            // Allocate small objects to fill the page cache with small pages
            fillPageCache(10 * K);

            // Allocate large objects to provoke page cache flushing to rebuild
            // cached small pages into large pages
            fillPageCache(10 * M);
        }
    }

    public static void main(String[] args) throws Exception {
        ProcessTools.executeProcess(ProcessTools.createJavaProcessBuilder(
                                    "-XX:+UseZGC",
                                    "-Xms128M",
                                    "-Xmx128M",
                                    "-Xlog:gc,gc+init,gc+heap=debug",
                                    Test.class.getName()))
                    .outputTo(System.out)
                    .errorTo(System.out)
                    .shouldContain("Page Cache Flushed:")
                    .shouldHaveExitValue(0);
    }
}
