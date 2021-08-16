/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jdeprusage;

import jdk.deprcases.types.DeprecatedEnum;

public class UseEnum {
    static class ReturnValue {
        static DeprecatedEnum returnValue() { return null; }
    }

    static class MethodParameter {
        static void methodParameterType(DeprecatedEnum e) { }
    }

    static class FieldType {
        static DeprecatedEnum field;
    }

    static class EnumConstant {
        static Object field2 = DeprecatedEnum.FIRST;
    }

    static class ValuesMethod {
        static Object[] valuesMethod() {
            return DeprecatedEnum.values();
        }
    }

    static class ValueOfMethod {
        static Object valueOfMethod(String s) {
            return DeprecatedEnum.valueOf(s);
        }
    }

    static class ClassObject {
        static Object classObject() {
            return DeprecatedEnum.class;
        }
    }
}
