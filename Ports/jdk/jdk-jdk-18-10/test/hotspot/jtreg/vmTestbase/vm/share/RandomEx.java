/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
package vm.share;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jdk.test.lib.Utils;

public class RandomEx extends Random {
    private final Map<Class<?>, Supplier<?>> map = new HashMap<>();

    {
        map.put(Boolean.class, this::nextBoolean);
        map.put(boolean.class, this::nextBoolean);
        map.put(Byte.class, this::nextByte);
        map.put(byte.class, this::nextByte);
        map.put(Short.class, this::nextShort);
        map.put(short.class, this::nextShort);
        map.put(Character.class, this::nextChar);
        map.put(char.class, this::nextChar);
        map.put(Integer.class, this::nextInt);
        map.put(int.class, this::nextInt);
        map.put(Long.class, this::nextLong);
        map.put(long.class, this::nextLong);
        map.put(Float.class, this::nextFloat);
        map.put(float.class, this::nextFloat);
        map.put(Double.class, this::nextDouble);
        map.put(double.class, this::nextDouble);
    }

    public RandomEx() {
        super(Utils.getRandomInstance().nextLong());
    }

    public RandomEx(long seed) {
        super(seed);
    }

    public byte nextByte() {
        return (byte) next(Byte.SIZE);
    }

    public short nextShort() {
        return (short) next(Short.SIZE);
    }

    public char nextChar() {
        return (char) next(Character.SIZE);
    }

    public <T> T next(Predicate<T> p, T dummy) {
        T result;
        do {
            result = next(dummy);
        } while (!p.test(result));
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> T next(T dummy) {
        Supplier<?> supplier = map.get(dummy.getClass());
        if (supplier == null) {
            throw new IllegalArgumentException("supplier for <" +
                    dummy.getClass() + ">is not found");
        }
        return (T) supplier.get();
    }
}
