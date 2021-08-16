/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.util.*;

// This is a test case executed by DumpClassList.java to load classes
// from various places to ensure that they are not written to the class list.
public class DumpClassListApp {
    public static void main(String args[]) throws Exception {
        // The following lambda usage should generate various classes like
        // java.lang.invoke.LambdaForm$MH/1146743572. All of them should be excluded from
        // the class list.
        List<String> a = new ArrayList<>();
        a.add("hello world.");
        a.forEach(str -> System.out.println(str));

        System.out.println(Class.forName("jdk.jfr.NewClass")); // should be excluded from the class list.
        System.out.println(Class.forName("boot.append.Foo"));    // should be excluded from the class list.
    }
}
