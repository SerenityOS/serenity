/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.*;

/*
 * @test
 * @bug 6843077 8006775
 * @summary new type annotation location: constructor type args
 * @author Mahmood Ali
 * @compile ConstructorTypeArgs.java
 */

class ConstructorTypeArgs {
  void oneArg() {
    new @A MyList<@A String>();
    new MyList<@A MyList<@B(0) String>>();
  }

  void twoArg() {
    new MyMap<String, String>();
    new MyMap<@A String, @B(0) MyList<@A String>>();
  }

  void withArraysIn() {
    new MyList<String[]>();
    new MyList<@A String @B(0) [] @A []>();

    new MyMap<@A String[], @B(0) MyList<@A String> @A []>();
  }
}

class MyList<E> { }
class MyMap<K, V> { }

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface A { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface B { int value(); }
