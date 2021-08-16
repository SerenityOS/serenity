/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5014324
 * @summary generics + varargs API changes
 * @author gafter
 *
 * @compile  Varargs2.java
 * @run main Varargs2
 */

import java.util.*;

public enum Varargs2 {
    A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z;
    public static void main(String[] args) {
        System.out.println(EnumSet.of(B));
        System.out.println(EnumSet.of(B, C));
        System.out.println(EnumSet.of(B, C, D));
        System.out.println(EnumSet.of(B, C, D, F));
        System.out.println(EnumSet.of(B, C, D, F, G));
        System.out.println(EnumSet.of(B, C, D, F, G, H));
        System.out.println(EnumSet.of(B, C, D, F, G, H, J));
        System.out.println(EnumSet.of(B, C, D, F, G, H, J, K));
        System.out.println(EnumSet.of(B, C, D, F, G, H, J, K, L));
        System.out.println(EnumSet.of(B, C, D, F, G, H, J, K, L, M));
        System.out.println(EnumSet.of(B, C, D, F, G, H, J, K, L, M, N));
        System.out.println(EnumSet.of(B, C, D, F, G, H, J, K, L, M, N, P));
    }
}
