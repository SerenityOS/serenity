/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package typeannos;

import java.lang.annotation.*;

/*
 * This class is replicated from test/tools/javac/annotations/typeAnnotations/newlocations.
 */
class DefaultScope {
    Parameterized<String, String> unannotated;
    Parameterized<@FldA String, String> firstTypeArg;
    Parameterized<String, @FldA String> secondTypeArg;
    Parameterized<@FldA String, @FldB String> bothTypeArgs;

    Parameterized<@FldA Parameterized<@FldA String, @FldB String>, @FldB String>
    nestedParameterized;

    @FldA String [] array1;
    @FldA String @FldB [] array1Deep;
    @FldA String [] [] array2;
    @FldD String @FldC @FldA [] @FldC @FldB [] array2Deep;
    String @FldA [] [] array2First;
    String [] @FldB [] array2Second;

    // Old-style array syntax
    String array2FirstOld @FldA [];
    String array2SecondOld [] @FldB [];
}

class ModifiedScoped {
    public final Parameterized<String, String> unannotated = null;
    public final Parameterized<@FldA String, String> firstTypeArg = null;
    public final Parameterized<String, @FldA String> secondTypeArg = null;
    public final Parameterized<@FldA String, @FldB String> bothTypeArgs = null;

    public final Parameterized<@FldA Parameterized<@FldA String, @FldB String>, @FldB String>
    nestedParameterized = null;

    public final @FldA String [] array1 = null;
    public final @FldA String @FldB [] array1Deep = null;
    public final @FldA String [] [] array2 = null;
    public final @FldA String @FldA [] @FldB [] array2Deep = null;
    public final String @FldA [] [] array2First = null;
    public final String [] @FldB [] array2Second = null;
}

class Parameterized<K, V> { }

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Documented
@interface FldA { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Documented
@interface FldB { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Documented
@interface FldC { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Documented
@interface FldD { }
