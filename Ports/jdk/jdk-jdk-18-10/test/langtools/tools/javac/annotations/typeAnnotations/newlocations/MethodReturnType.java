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
 * @summary new type annotation location: method return type array/generics
 * @author Mahmood Ali
 * @compile MethodReturnType.java
 */

class DefaultScope {
  Parameterized<String, String> unannotated() { return null; }
  Parameterized<@A String, String> firstTypeArg() { return null; }
  Parameterized<String, @A String> secondTypeArg() { return null; }
  Parameterized<@A String, @B String> bothTypeArgs() { return null; }

  Parameterized<@A Parameterized<@A String, @B String>, @B String>
    nestedParameterized() { return null; }

  public <T> @A String method() { return null; }

  @A String [] array1() { return null; }
  @A String @B [] array1Deep() { return null; }
  @A String [] [] array2() { return null; }
  @A String @A [] @B [] array2Deep() { return null; }
  String @A [] [] array2First() { return null; }
  String [] @B [] array2Second() { return null; }

  // Old-style array syntax
  String array2FirstOld() @A [] { return null; }
  String array2SecondOld() [] @B [] { return null; }
}

class ModifiedScoped {
  public final Parameterized<String, String> unannotated() { return null; }
  public final Parameterized<@A String, String> firstTypeArg() { return null; }
  public final Parameterized<String, @A String> secondTypeArg() { return null; }
  public final Parameterized<@A String, @B String> bothTypeArgs() { return null; }

  public final Parameterized<@A Parameterized<@A String, @B String>, @B String>
    nestedParameterized() { return null; }

  public final @A String [] array1() { return null; }
  public final @A String @B [] array1Deep() { return null; }
  public final @A String [] [] array2() { return null; }
  public final @A String @A [] @B [] array2Deep() { return null; }
  public final String @A [] [] array2First() { return null; }
  public final String [] @B [] array2Second() { return null; }
}

class Parameterized<K, V> { }

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface A { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface B { }
