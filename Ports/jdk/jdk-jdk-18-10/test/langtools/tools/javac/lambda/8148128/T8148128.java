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

/*
 * @test
 * @bug 8148128
 * @summary Regression: array constructor references marked as inexact
 * @compile T8148128.java
 */
import java.util.*;
import java.util.stream.*;

class T8148128 {
    public static void doSomething (List<String>[] stuff) {
        System.out.println("List Stuff");
    }

    public static void doSomething (Set<String>[] stuff) {
        System.out.println("Set Stuff");
    }

    public static void main (String[] args) {
        doSomething(Stream.of("Foo", "Bar").map(Collections::singletonList).toArray(List[]::new));
    }
}
