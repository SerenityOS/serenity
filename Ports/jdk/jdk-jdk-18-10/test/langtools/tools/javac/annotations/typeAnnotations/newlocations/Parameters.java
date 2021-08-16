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
 * @summary new type annotation location: parameter type array/generics
 * @author Mahmood Ali
 * @compile Parameters.java
 */

class Parameters {
  void unannotated(Parameterized<String, String> a) {}
  void firstTypeArg(Parameterized<@A String, String> a) {}
  void secondTypeArg(Parameterized<String, @A String> a) {}
  void bothTypeArgs(Parameterized<@A String, @B String> both) {}

  void nestedParameterized(Parameterized<@A Parameterized<@A String, @B String>, @B String> a) {}

  void array1(@A String [] a) {}
  void array1Deep(@A String @B [] a) {}
  void array2(@A String [] [] a) {}
  void array2Deep(@A String @A [] @B [] a) {}
  void array2First(String @A [] [] a) {}
  void array2Second(String [] @B [] a) {}
}

class Parameterized<K, V> { }

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface A { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface B { }
