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
 * @bug 8078093 8077247
 * @summary Exponential performance regression Java 8 compiler compared to Java 7 compiler
 * @compile T8077247.java
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class T8077247 {
    public static void test() {
        int x = add(add(add(add(add(add(add(add(add(add(1, 2), 3), 4), 5), 6), 7), 8), 9), 10), 11);
    }

    public static int add(int x, int y) {
        long rslt = (long)x + (long)y;
        if (Integer.MIN_VALUE <= rslt && rslt <= Integer.MAX_VALUE) {
            return (int)rslt;
        }

        String msg = String.format("Integer overflow: %d + %d.", x, y);
        throw new IllegalArgumentException(msg);
    }

    public static double add(double x, double y) {
        double rslt = x + y;
        if (Double.isInfinite(rslt)) {
            String msg = String.format("Real overflow: %s + %s.", x, y);
            throw new IllegalArgumentException(msg);
        }
        return (rslt == -0.0) ? 0.0 : rslt;
    }

    public static <T> List<T> add(List<T> x, List<T> y) {
        List<T> rslt = new ArrayList<>(x.size() + y.size());
        rslt.addAll(x);
        rslt.addAll(y);
        return rslt;
    }

    public static String add(String x, String y) {
        return x + y;
    }

    public static <K, V> Map<K, V> add(Map<K, V> x, Map<K, V> y) {
        Map<K, V> rslt = new HashMap<>(x);
        rslt.putAll(y);
        return rslt;
    }
}
