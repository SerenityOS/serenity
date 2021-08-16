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

/**
 * @test
 * @bug 8141508
 * @summary Test that Call site initialization exception is not thrown when the method
   reference receiver is of intersection type.
 * @run main IntersectionTypeReceiverTest
 */

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class IntersectionTypeReceiverTest {

    public static void main(String[] args) {
        String output = valueOfKey(Size.class, LocalDate.now()).toString();
        if (!"Optional[S]".equals(output))
            throw new AssertionError("Unexpected output: " + output);
    }

    enum Size implements Supplier<LocalDate> {
        S, M, L;

        @Override
        public LocalDate get() {
            return LocalDate.now();
        }

    }

    public static <K, T extends Enum<T> & Supplier<K>> Optional<T> valueOfKey(Class<T> enumType, K key) {
        return valueOf(enumType, key, T::get);
    }

    public static <K, T extends Enum<T>> Optional<T> valueOf(Class<T> enumType, K key, Function<T, K> keyExtractor) {
        return EnumSet.allOf(enumType).stream().filter(t -> Objects.equals(keyExtractor.apply(t), key)).findFirst();
    }
}
