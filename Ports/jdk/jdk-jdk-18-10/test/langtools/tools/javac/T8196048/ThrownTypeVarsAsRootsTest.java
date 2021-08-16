/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test 8196048
 * @summary thrown type variables should be roots in the minimum inference graph
 * @compile ThrownTypeVarsAsRootsTest.java
 */

import java.io.UnsupportedEncodingException;
import java.net.BindException;
import java.util.function.Function;
import java.util.Optional;
import java.util.function.BiFunction;

class ThrownTypeVarsAsRootsTest {
    void foo() {
        try {
            BiFunction<String, String, Optional<String>> function = rethrowBiFunction((x, y) -> {
                    return Optional.of(x).map(rethrowFunction(z -> createZ(z)));
            });
        } catch (Exception ex) {}
    }

    public static String createZ(String x) throws UnsupportedEncodingException, BindException {
        return null;
    }

    public static <T, R, E extends Exception> Function<T, R> rethrowFunction(ThrowingFunction<T, R, E> function) throws E {
        return null;
    }

    public static <T, U, R, E extends Exception> BiFunction<T, U, R> rethrowBiFunction(
            ThrowingBiFunction<T, U, R, E> function) throws E {
        return null;
    }

    public interface ThrowingBiFunction<T, U, R, E extends Exception> {
        R apply(T t, U u) throws E;
    }

    public interface ThrowingFunction<T, R, E extends Exception> {
        R apply(T t) throws E;
    }
}
