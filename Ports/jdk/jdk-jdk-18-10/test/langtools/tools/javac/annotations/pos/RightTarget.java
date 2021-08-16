/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4901271
 * @summary java.lang.annotation.Target
 * @author gafter
 *
 * @compile RightTarget.java
 */

import java.lang.annotation.ElementType;

@java.lang.annotation.Target({ElementType.TYPE})
@type
@interface type {
}


@java.lang.annotation.Target({ElementType.FIELD})
@interface field {
}
class field1 {
    @field int x;
}


@java.lang.annotation.Target({ElementType.METHOD})
@interface method {
}
class method1 {
    @method void m() {}
}


/* 4901285
@java.lang.annotation.Target({ElementType.PARAMETER})
@interface parameter {
}
class parameter1 {
    void m(@parameter int x) {}
}
*/


@java.lang.annotation.Target({ElementType.CONSTRUCTOR})
@interface constructor {
}
class constructor1 {
    @constructor constructor1() {}
}


/* 4936182
@java.lang.annotation.Target({ElementType.LOCAL_VARIABLE})
@interface local {
}
class local1 {
    void f() {
        @local int x;
    }
}
*/


@java.lang.annotation.Target({ElementType.ANNOTATION_TYPE})
@annotation
@interface annotation {
}
