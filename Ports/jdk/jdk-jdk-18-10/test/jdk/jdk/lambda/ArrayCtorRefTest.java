/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;

import java.util.function.IntFunction;
import java.util.function.Supplier;

import static org.testng.Assert.assertTrue;

/**
 * ArrayCtorRefTest
 *
 * @author Brian Goetz
 */
@Test
public class ArrayCtorRefTest {
    interface ArrayMaker<T> {
        public T[] make(int size);
    }

    private static<T> Supplier<T[]> emptyArrayFactory(ArrayMaker<T> maker) {
        return () -> maker.make(0);
    }

    public void testLambda() {
        ArrayMaker<String> am = i -> new String[i];
        String[] arr = am.make(3);
        arr[0] = "Foo";
        assertTrue(arr instanceof String[]);
        assertTrue(arr.length == 3);
    }

    public void testIntCtorRef() {
        IntFunction<int[]> factory = int[]::new;
        int[] arr = factory.apply(6);
        assertTrue(arr.length == 6);
    }

    public void testLambdaInference() {
        Supplier<Object[]> oF = emptyArrayFactory(i -> new Object[i]);
        Supplier<String[]> sF = emptyArrayFactory(i -> new String[i]);
        assertTrue(oF.get() instanceof Object[]);
        assertTrue(sF.get() instanceof String[]);
    }

    public void testCtorRef() {
        ArrayMaker<String> am = String[]::new;
        String[] arr = am.make(3);
        arr[0] = "Foo";
        assertTrue(arr instanceof String[]);
        assertTrue(arr.length == 3);
    }
}
