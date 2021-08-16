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
class MtdDefaultScope {
    MtdParameterized<String, String> unannotated() { return null; }
    MtdParameterized<@MRtnA String, String> firstTypeArg() { return null; }
    MtdParameterized<String, @MRtnA String> secondTypeArg() { return null; }
    MtdParameterized<@MRtnA String, @MRtnB String> bothTypeArgs() { return null; }

    MtdParameterized<@MRtnA MtdParameterized<@MRtnA String, @MRtnB String>, @MRtnB String>
    nestedMtdParameterized() { return null; }

    public <T> @MRtnA String method() { return null; }

    @MRtnA String [] array1() { return null; }
    @MRtnA String @MRtnB [] array1Deep() { return null; }
    @MRtnA String [] [] array2() { return null; }
    @MRtnA String @MRtnA [] @MRtnB [] array2Deep() { return null; }
    String @MRtnA [] [] array2First() { return null; }
    String [] @MRtnB [] array2Second() { return null; }

    // Old-style array syntax
    String array2FirstOld() @MRtnA [] { return null; }
    String array2SecondOld() [] @MRtnB [] { return null; }
}

class MtdModifiedScoped {
    public final MtdParameterized<String, String> unannotated() { return null; }
    public final MtdParameterized<@MRtnA String, String> firstTypeArg() { return null; }
    public final MtdParameterized<String, @MRtnA String> secondTypeArg() { return null; }
    public final MtdParameterized<@MRtnA String, @MRtnB String> bothTypeArgs() { return null; }

    public final MtdParameterized<@MRtnA MtdParameterized<@MRtnA String, @MRtnB String>, @MRtnB String>
    nestedMtdParameterized() { return null; }

    public final @MRtnA String [] array1() { return null; }
    public final @MRtnA String @MRtnB [] array1Deep() { return null; }
    public final @MRtnA String [] [] array2() { return null; }
    public final @MRtnA String @MRtnA [] @MRtnB [] array2Deep() { return null; }
    public final String @MRtnA [] [] array2First() { return null; }
    public final String [] @MRtnB [] array2Second() { return null; }
}

class MtdParameterized<K, V> { }

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Documented
@interface MRtnA { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Documented
@interface MRtnB { }
