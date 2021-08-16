/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8046685 8142948
 * @summary Uncompilable large expressions involving generics.
 * @compile T8046685.java
 */
class T8046685 {

    interface Predicate<T, U> {
        public boolean apply(T t, U u);
        public boolean equals(Object o);
    }

    static <X1, X2> Predicate<X1, X2> and(final Predicate<? super X1, ? super X2> first, final Predicate<? super X1, ? super X2> second) {
        return null;
    }

    public static void test(Predicate<Integer, Integer> even) {
        and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even,
                and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even,
                and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even,
                and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, and(even, even)
                ))))))))))))))))))))))))))))))))))))))))))))))));
    }
}
