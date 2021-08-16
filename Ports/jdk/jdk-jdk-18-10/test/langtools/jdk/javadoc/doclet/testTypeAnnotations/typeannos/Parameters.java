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
class Parameters {
    void unannotated(ParaParameterized<String, String> a) {}
    void firstTypeArg(ParaParameterized<@ParamA String, String> a) {}
    void secondTypeArg(ParaParameterized<String, @ParamA String> a) {}
    void bothTypeArgs(ParaParameterized<@ParamA String, @ParamB String> both) {}

    void nestedParaParameterized(ParaParameterized<@ParamA ParaParameterized<@ParamA String, @ParamB String>, @ParamB String> a) {}

    void array1(@ParamA String [] a) {}
    void array1Deep(@ParamA String @ParamB [] a) {}
    void array2(@ParamA String [] [] a) {}
    void array2Deep(@ParamA String @ParamA [] @ParamB [] a) {}
    void array2First(String @ParamA [] [] a) {}
    void array2Second(String [] @ParamB [] a) {}
}

class ParaParameterized<K, V> { }

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Documented
@interface ParamA { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Documented
@interface ParamB { }
