/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8157149
 * @summary Inference: weird propagation of thrown inference variables
 *
 * @compile T8157149b.java
 */

class T8157149b {

    void test() {
        computeException1(this::computeThrowable);
        computeException2(this::computeThrowable);
        computeException1(() -> {
            Integer integer = computeThrowable();
            return integer;
        });
        computeException2(() -> {
            Integer integer = computeThrowable();
            return integer;
        });
    }

    <T, E extends Exception> void computeException1(ThrowableComputable1<T, E> c) throws E {}

    <T, E extends Exception> void computeException2(ThrowableComputable2<T, E> c) throws E {}

    <E1 extends Throwable> Integer computeThrowable() throws E1 {
        return 0;
    }

    interface ThrowableComputable1<T, E extends Throwable> {
        T compute() throws E;
    }

    interface ThrowableComputable2<T, E extends Throwable> {
        Integer compute() throws E;
    }
}
