/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. All rights reserved.
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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

/*
 * @test id=default
 * @requires vm.gc.Shenandoah & vm.gc == "null"
 * @run main/othervm -Dexpected=false -Xmx64m                                                       TestEnabled
 * @run main/othervm -Dexpected=true  -Xmx64m -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC TestEnabled
 */

/*
 * @test id=already
 * @requires vm.gc.Shenandoah & vm.gc == "Shenandoah"
 * @run main/othervm -Dexpected=true -Xmx64m                                                        TestEnabled
 */
public class TestEnabled {

    public static void main(String... args) {
        boolean expected = Boolean.getBoolean("expected");
        boolean actual = isEnabled();
        if (expected != actual) {
            throw new IllegalStateException("Error: expected = " + expected + ", actual = " + actual);
        }
    }

    public static boolean isEnabled() {
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean.getName().contains("Shenandoah")) {
                return true;
            }
        }
        return false;
    }

}
