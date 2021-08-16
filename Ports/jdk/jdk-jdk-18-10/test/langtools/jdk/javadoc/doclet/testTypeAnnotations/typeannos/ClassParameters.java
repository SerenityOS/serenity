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
class Unannotated<K> { }

class ExtendsBound<K extends @ClassParamA String> { }
class ExtendsGeneric<K extends @ClassParamA Unannotated<@ClassParamB String>> { }
class TwoBounds<K extends @ClassParamA String, V extends @ClassParamB String> { }

class Complex1<K extends @ClassParamA String&Runnable> { }
class Complex2<K extends String & @ClassParamB Runnable> { }
class ComplexBoth<K extends @ClassParamA String & @ClassParamA Runnable> { }

class ClassParamOuter {
    void inner() {
        class LUnannotated<K> { }

        class LExtendsBound<K extends @ClassParamA String> { }
        class LExtendsGeneric<K extends @ClassParamA LUnannotated<@ClassParamB String>> { }
        class LTwoBounds<K extends @ClassParamA String, V extends @ClassParamB String> { }

        class LComplex1<K extends @ClassParamA String&Runnable> { }
        class LComplex2<K extends String & @ClassParamB Runnable> { }
        class LComplexBoth<K extends @ClassParamA String & @ClassParamA Runnable> { }
    }
}

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Documented
@interface ClassParamA { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Documented
@interface ClassParamB { }
