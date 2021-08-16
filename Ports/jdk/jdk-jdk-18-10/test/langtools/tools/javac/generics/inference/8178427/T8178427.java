/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8178427
 * @summary NPE in Infer$CheckUpperBounds
 * @compile T8178427.java
 */

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.*;
import java.util.stream.*;

abstract class X {
    public interface N<K, V> {
        Stream<V> getValues();
    }

    abstract <K, V> N<K, V> c();

    abstract <T, K, V, M extends N<K, V>> Collector<T, ?, M> f(
            Function<? super T, ? extends K> k,
            Function<? super T, ? extends Stream<? extends V>> v,
            Supplier<M> multimapSupplier);

    void m(Map<String, N<?, ?>> c, ExecutorService s) {
        s.submit(() -> {
            return c.entrySet().parallelStream()
                    .collect(f(Map.Entry::getKey, e -> e.getValue().getValues(), this::c));
        });
    }
}

