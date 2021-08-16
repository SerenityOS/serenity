/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6843077 8006775 8031744
 * @summary random tests for new locations
 * @author Matt Papi
 * @compile BasicTest.java
 */

import java.lang.annotation.*;
import java.util.*;
import java.io.*;

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface A {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface B {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface C {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface D {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface E {}
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface F {}

/**
 * Tests basic JSR 308 parser functionality. We don't really care about what
 * the parse tree looks like, just that these annotations can be parsed.
 */
class BasicTest<@D T extends @A Object> extends @B LinkedList<@E T> implements @C List<@F T> {

    void test() {

        // Handle annotated cast types
        Object o = (@A Object) "foo";

        // Handle annotated "new" expressions (except arrays; see ArrayTest)
        String s = new @A String("bar");

        boolean b = o instanceof @A Object;

        @A Map<@B List<@C String>, @D String> map =
            new @A HashMap<@B List<@C String>, @D String>();

        Class<? extends @A String> c2 = null;
    }

    // Handle receiver annotations
    // Handle annotations on a qualified identifier list
    void test2(@C @D BasicTest<T> this) throws @A IllegalArgumentException, @B IOException {

    }

    // Handle annotations on a varargs element type
    void test3(@B Object @A... objs) { }

    void test4(@B Class<@C ?> @A ... clz) { }


    // TODO: add more tests... nested classes, etc.
}
