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
 * @test id=debug
 * @bug 8251158
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @requires (vm.debug == true)
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI  -XX:VerifyMetaspaceInterval=10                                        TestMetaspaceAllocation
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI  -XX:VerifyMetaspaceInterval=10  -XX:MetaspaceReclaimPolicy=none       TestMetaspaceAllocation
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI  -XX:VerifyMetaspaceInterval=10  -XX:MetaspaceReclaimPolicy=aggressive TestMetaspaceAllocation
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI  -XX:VerifyMetaspaceInterval=10  -XX:+MetaspaceGuardAllocations        TestMetaspaceAllocation
 *
 */

/*
 * @test id=ndebug
 * @bug 8251158
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @requires (vm.debug == false)
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI                                        TestMetaspaceAllocation
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI  -XX:MetaspaceReclaimPolicy=none       TestMetaspaceAllocation
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI  -XX:MetaspaceReclaimPolicy=aggressive TestMetaspaceAllocation
 */

public class TestMetaspaceAllocation {

    public static void main(String[] args) {

        MetaspaceTestContext context = new MetaspaceTestContext();
        MetaspaceTestArena arena1 = context.createArena(false, 1024 * 1024 * 4);
        MetaspaceTestArena arena2 = context.createArena(true,1024 * 1024 * 4);

        Allocation a1 = arena1.allocate(100);
        Allocation a2 = arena2.allocate(100);

        long used = context.usedWords();
        long committed = context.committedWords();

        System.out.println("used " + used + " committed " + committed);

        arena1.deallocate(a1);

        context.destroyArena(arena2);
        context.destroyArena(arena1);

        context.purge();

        context.destroy();

    }
}
