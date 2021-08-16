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
 *
 */

/*
 * @test
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC                         TestWrongArrayMember
 * @run main/othervm -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu TestWrongArrayMember
 */

public class TestWrongArrayMember {
    public static void main(String... args) throws Exception {
        Object[] src = new Object[3];
        src[0] = new Integer(0);
        src[1] = new Object();
        src[2] = new Object();
        Object[] dst = new Integer[3];
        dst[0] = new Integer(1);
        dst[1] = new Integer(2);
        dst[2] = new Integer(3);
        try {
            System.arraycopy(src, 0, dst, 0, 3);
            throw new RuntimeException("Expected ArrayStoreException");
        } catch (ArrayStoreException e) {
            if (src[0] != dst[0]) {
                throw new RuntimeException("First element not copied");
            } else if (src[1] == dst[1] || src[2] == dst[2]) {
                throw new RuntimeException("Second and third elements are affected");
            } else {
                return; // Passed!
            }
        }
    }
}

