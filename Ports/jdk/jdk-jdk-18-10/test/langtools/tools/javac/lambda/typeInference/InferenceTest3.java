/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003280
 * @summary Add lambda tests
 *  Interface inheritance, sub-interface resolves the type of the super interface.
 * @compile InferenceTest3.java
 * @run main InferenceTest3
 */

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;

public class InferenceTest3 {

    private static void assertTrue(boolean cond) {
        if (!cond)
            throw new AssertionError();
    }

    public static void main(String[] args) {
        InferenceTest3 test = new InferenceTest3();
        test.m1(a -> a.getTime());
        test.m2(a -> a.toString());

        C<String, Integer> c = a -> a.length();
        assertTrue(c.m("lambda") == 6);

        E<Double, String> e = a -> Double.toHexString(a);
        assertTrue(e.m(Double.MAX_VALUE).equals("0x1.fffffffffffffp1023"));
        assertTrue(e.m(Double.MIN_VALUE).equals("0x0.0000000000001p-1022"));
        assertTrue(e.m(1.0).equals("0x1.0p0"));
    }

    private void m1(C<Date, Long> c) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(1970, 0, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        assertTrue(c.m(date) == 0L);
    }

    private void m2(E<Integer, String> e) {
        assertTrue(e.m(2).equals("2"));
    }

    interface A<T extends Serializable, U> {
        U m(T t);
    }

    interface C<X extends Serializable, Y extends Number> extends A<X,Y> {}

    interface E<X extends Serializable, Y extends String> extends A<X,Y> {}
}
