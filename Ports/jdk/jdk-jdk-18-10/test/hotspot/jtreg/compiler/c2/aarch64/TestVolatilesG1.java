/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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
 * @test
 * @summary C2 should use ldar, stlr and ldaxr+stlxr insns for volatile operations
 * @library /test/lib /
 *
 * @modules java.base/jdk.internal.misc
 *
 * @requires vm.flagless
 * @requires os.arch=="aarch64" & vm.debug == true &
 *           vm.flavor == "server" & !vm.graal.enabled &
 *           vm.gc.G1
 *
 * @build compiler.c2.aarch64.TestVolatiles
 *        compiler.c2.aarch64.TestVolatileLoad
 *        compiler.c2.aarch64.TestUnsafeVolatileLoad
 *        compiler.c2.aarch64.TestVolatileStore
 *        compiler.c2.aarch64.TestUnsafeVolatileStore
 *        compiler.c2.aarch64.TestUnsafeVolatileCAS
 *        compiler.c2.aarch64.TestUnsafeVolatileWeakCAS
 *        compiler.c2.aarch64.TestUnsafeVolatileCAE
 *        compiler.c2.aarch64.TestUnsafeVolatileGAS
 *        compiler.c2.aarch64.TestUnsafeVolatileGAA
 *
 * @run driver compiler.c2.aarch64.TestVolatilesG1
 *      TestVolatileLoad G1
 *
 * @run driver compiler.c2.aarch64.TestVolatilesG1
 *      TestVolatileStore G1
 *
 * @run driver compiler.c2.aarch64.TestVolatilesG1
 *      TestUnsafeVolatileLoad G1
 *
 * @run driver compiler.c2.aarch64.TestVolatilesG1
 *      TestUnsafeVolatileStore G1
 *
 * @run driver compiler.c2.aarch64.TestVolatilesG1
 *      TestUnsafeVolatileCAS G1
 *
 * @run driver compiler.c2.aarch64.TestVolatilesG1
 *      TestUnsafeVolatileWeakCAS G1
 *
 * @run driver compiler.c2.aarch64.TestVolatilesG1
 *      TestUnsafeVolatileCAE G1
 *
 * @run driver compiler.c2.aarch64.TestVolatilesG1
 *      TestUnsafeVolatileGAS G1
 *
 * @run driver compiler.c2.aarch64.TestVolatilesG1
 *      TestUnsafeVolatileGAA G1
 */


package compiler.c2.aarch64;

public class TestVolatilesG1 {
    public static void main(String args[]) throws Throwable
    {
        // delegate work to shared code
        new TestVolatiles().runtest(args[0], args[1]);
    }
}
