/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8027751
 * @summary C1 crashes generating G1 post-barrier in Unsafe.getAndSetReference() intrinsic because of the new value spill
 * @requires vm.gc.G1
 *
 * @run main/othervm -XX:+UseG1GC compiler.regalloc.C1ObjectSpillInLogicOp
 */

package compiler.regalloc;

import java.util.concurrent.atomic.AtomicReferenceArray;

/*
 * G1 barriers use logical operators (xor) on T_OBJECT mixed with T_LONG or T_INT.
 * The current implementation of logical operations on x86 in C1 doesn't allow for long operands to be on stack.
 * There is a special code in the register allocator that forces long arguments in registers on x86. However T_OBJECT
 * can be spilled just fine, and in that case the xor emission will fail.
 */
public class C1ObjectSpillInLogicOp {
    public static void main(String[] args) {
        AtomicReferenceArray<Integer> x = new AtomicReferenceArray(128);
        Integer y = new Integer(0);
        for (int i = 0; i < 50000; i++) {
            x.getAndSet(i % x.length(), y);
        }
    }
}
