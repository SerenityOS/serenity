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
 * @summary new type annotation location: field type array/generics
 * @author Mahmood Ali
 * @compile Fields.java
 */

class DefaultScope {
  Parameterized<String, String> unannotated;
  Parameterized<@A String, String> firstTypeArg;
  Parameterized<String, @A String> secondTypeArg;
  Parameterized<@A String, @B String> bothTypeArgs;

  Parameterized<@A Parameterized<@A String, @B String>, @B String>
    nestedParameterized;

  @A String [] array1;
  @A String @B [] array1Deep;
  @A String [] [] array2;
  @A String @A [] @B [] array2Deep;
  String @A [] [] array2First;
  String [] @B [] array2Second;

  // Old-style array syntax
  String array2FirstOld @A [];
  String array2SecondOld [] @B [];
}

class ModifiedScoped {
  public final Parameterized<String, String> unannotated = null;
  public final Parameterized<@A String, String> firstTypeArg = null;
  public final Parameterized<String, @A String> secondTypeArg = null;
  public final Parameterized<@A String, @B String> bothTypeArgs = null;

  public final Parameterized<@A Parameterized<@A String, @B String>, @B String>
    nestedParameterized = null;

  public final @A String [] array1 = null;
  public final @A String @B [] array1Deep = null;
  public final @A String [] [] array2 = null;
  public final @A String @A [] @B [] array2Deep = null;
  public final String @A [] [] array2First = null;
  public final String [] @B [] array2Second = null;
}

class Parameterized<K, V> { }

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface A { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface B { }
