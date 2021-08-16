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

/**
 * @test
 * @bug 8152832
 * @summary Type inference regression in javac
 * @compile T8152832.java
 */

import java.util.function.*;
import java.util.stream.*;
import java.util.*;

class T8152832 {
    interface MyStream<T> extends Stream<T> {
        public <U> List<U> toFlatList(Function<? super T, ? extends Collection<U>> mapper);
    }

    static class MyStreamSupplier<T> {
        public MyStream<T> get() {
            return null;
        }
    }

    public static <T> void myStream(Supplier<Stream<T>> base, Consumer<MyStreamSupplier<T>> consumer) {
    }

    public static void assertEquals(Object expected, Object actual) {
    }

    public void test() {
        List<List<String>> strings = Arrays.asList();
        List<String> expectedList = Arrays.asList();
        myStream(strings::stream, supplier -> {
            assertEquals(expectedList, supplier.get().toFlatList(Function.identity()));
        });
    }
}