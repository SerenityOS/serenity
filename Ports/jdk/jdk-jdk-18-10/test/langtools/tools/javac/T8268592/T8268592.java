/*
 * Copyright (c) 2021, Alphabet LLC. All rights reserved.
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

/* @test
 * @bug 8268592
 * @summary JDK-8262891 causes an NPE in Lint.augment
 * @compile T8268592.java
 */

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

abstract class T {

    abstract <T> T r(Function<String, Supplier<T>> x);

    enum E {
        ONE
    }

    abstract <T> Supplier<T> f(Function<T, Supplier<T>> x);

    public void updateAcl(E e, Supplier<Void> v) {
        r(
                (String t) -> {
                    switch (e) {
                        case ONE:
                            return f(
                                    a -> {
                                        Collection<String> m = null;
                                        return v;
                                    });
                        default:
                            return v;
                    }
                });
    }
}
