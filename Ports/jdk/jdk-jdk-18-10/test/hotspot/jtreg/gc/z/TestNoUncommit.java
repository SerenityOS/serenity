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
 * @test TestNoUncommit
 * @requires vm.gc.Z & !vm.graal.enabled
 * @summary Test ZGC uncommit unused memory disabled
 * @run main/othervm -XX:+UseZGC -Xlog:gc*,gc+heap=debug,gc+stats=off -Xms512M -Xmx512M -XX:ZUncommitDelay=1 gc.z.TestNoUncommit
 * @run main/othervm -XX:+UseZGC -Xlog:gc*,gc+heap=debug,gc+stats=off -Xms128M -Xmx512M -XX:ZUncommitDelay=1 -XX:-ZUncommit gc.z.TestNoUncommit
 */

public class TestNoUncommit {
    private static final int allocSize = 200 * 1024 * 1024; // 200M
    private static volatile Object keepAlive = null;

    private static long capacity() {
        return Runtime.getRuntime().totalMemory();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Allocating");
        keepAlive = new byte[allocSize];
        final var afterAlloc = capacity();

        System.out.println("Reclaiming");
        keepAlive = null;
        System.gc();

        // Wait longer than the uncommit delay (which is 1 second)
        Thread.sleep(5 * 1000);

        final var afterDelay = capacity();

        // Verify
        if (afterAlloc > afterDelay) {
            throw new Exception("Should not uncommit");
        }

        System.out.println("Success");
    }
}
