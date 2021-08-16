/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestMemBarAcquire
 * @bug 8048879
 * @summary Tests optimization of MemBarAcquireNodes
 * @run main/othervm -XX:-TieredCompilation -XX:-BackgroundCompilation
 *                   compiler.membars.TestMemBarAcquire
 */

package compiler.membars;

public class TestMemBarAcquire {
    private volatile static Object defaultObj = new Object();
    private Object obj;

    public TestMemBarAcquire(Object param) {
        // Volatile load. MemBarAcquireNode is added after the
        // load to prevent following loads from floating up past.
        // StoreNode is added to store result of load in 'obj'.
        this.obj = defaultObj;
        // Overrides 'obj' and therefore makes previous StoreNode
        // and the corresponding LoadNode useless. However, the
        // LoadNode is still connected to the MemBarAcquireNode
        // that should now release the reference.
        this.obj = param;
    }

    public static void main(String[] args) throws Exception {
        // Make sure TestMemBarAcquire::<init> is compiled
        for (int i = 0; i < 100000; ++i) {
            TestMemBarAcquire p = new TestMemBarAcquire(new Object());
        }
    }
}

