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

/*
 * @test
 * @bug 7187618 7184799
 * @summary Tests just a benchmark of Introspector.getBeanInfo(Class) performance
 * @author Sergey Malenkov
 * @run main/manual Test7184799
 */

import java.beans.Introspector;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Test7184799 {
    private static final Class[] TYPES = {
            Class.class,
            String.class,
            Character.class,
            Boolean.class,
            Byte.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            Collection.class,
            Set.class,
            HashSet.class,
            TreeSet.class,
            LinkedHashSet.class,
            Map.class,
            HashMap.class,
            TreeMap.class,
            LinkedHashMap.class,
            WeakHashMap.class,
            ConcurrentHashMap.class,
            Dictionary.class,
            Exception.class,
    };

    public static void main(String[] args) throws Exception {
        long time = System.nanoTime();
        for (Class type : TYPES) {
            Introspector.getBeanInfo(type);
        }
        time -= System.nanoTime();
        System.out.println("Time (ms): " + (-time / 1000000));
    }
}
