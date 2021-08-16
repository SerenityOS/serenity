/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7002666
 * @summary eclipse CDT projects crash with compressed oops
 *
 * @run main/othervm -Xbatch
 *      -XX:CompileCommand=compileonly,compiler.c2.Test7002666::test
 *      -XX:CompileCommand=compileonly,java.lang.reflect.Array::*
 *      compiler.c2.Test7002666
 */

package compiler.c2;
/*
 * This will only reliably fail with a fastdebug build since it relies
 * on seeing garbage in the heap to die.  It could be made more
 * reliable in product mode but that would require greatly increasing
 * the runtime.
 */

public class Test7002666 {
    public static void main(String[] args) {
        for (int i = 0; i < 25000; i++) {
            Object[] a = test(Test7002666.class, new Test7002666());
            if (a[0] != null) {
                // The element should be null but if it's not then
                // we've hit the bug.  This will most likely crash but
                // at least throw an exception.
                System.err.println(a[0]);
                throw new InternalError(a[0].toString());

            }
        }
    }
    public static Object[] test(Class c, Object o) {
        // allocate an array small enough to be trigger the bug
        Object[] a = (Object[])java.lang.reflect.Array.newInstance(c, 1);
        return a;
    }
}
