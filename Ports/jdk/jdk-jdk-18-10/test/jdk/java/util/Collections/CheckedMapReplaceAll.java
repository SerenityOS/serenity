/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8047795
 * @summary Ensure that replaceAll operator cannot add bad elements
 * @author  Mike Duigou
 */

import java.util.*;
import java.util.function.BiFunction;

public class CheckedMapReplaceAll {
    public static void main(String[] args) {
        Map<Integer,Double> unwrapped = new HashMap<>();
        unwrapped.put(1, 1.0);
        unwrapped.put(2, 2.0);
        unwrapped.put(3, 3.0);

        Map<Integer,Double> wrapped = Collections.checkedMap(unwrapped, Integer.class, Double.class);

        BiFunction evil = (k, v) -> (((int)k) % 2 != 0) ? v : "evil";

        try {
            wrapped.replaceAll(evil);
            System.out.printf("Bwahaha! I have defeated you! %s\n", wrapped);
            throw new RuntimeException("String added to checked Map<Integer,Double>");
        } catch (ClassCastException thwarted) {
            thwarted.printStackTrace(System.out);
            System.out.println("Curses! Foiled again!");
        }
    }
}
