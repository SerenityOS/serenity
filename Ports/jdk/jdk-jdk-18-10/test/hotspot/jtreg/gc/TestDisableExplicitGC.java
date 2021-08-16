/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc;

/*
 * @test TestDisableExplicitGC
 * @requires vm.opt.DisableExplicitGC == null
 * @summary Verify GC behavior with DisableExplicitGC flag.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules jdk.management/com.sun.management.internal
 * @run main/othervm                             -Xlog:gc=debug gc.TestDisableExplicitGC
 * @run main/othervm/fail -XX:+DisableExplicitGC -Xlog:gc=debug gc.TestDisableExplicitGC
 * @run main/othervm      -XX:-DisableExplicitGC -Xlog:gc=debug gc.TestDisableExplicitGC
 */
import java.lang.management.GarbageCollectorMXBean;
import java.util.List;
import static jdk.test.lib.Asserts.*;

public class TestDisableExplicitGC {

    public static void main(String[] args) throws InterruptedException {
        List<GarbageCollectorMXBean> list = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
        long collectionCountBefore = getCollectionCount(list);
        System.gc();
        long collectionCountAfter = getCollectionCount(list);
        assertLT(collectionCountBefore, collectionCountAfter);
    }

    private static long getCollectionCount(List<GarbageCollectorMXBean> list) {
        int collectionCount = 0;
        for (GarbageCollectorMXBean gcMXBean : list) {
            collectionCount += gcMXBean.getCollectionCount();
        }
        return collectionCount;
    }
}
