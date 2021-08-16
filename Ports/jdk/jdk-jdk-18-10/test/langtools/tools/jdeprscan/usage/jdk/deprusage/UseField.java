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

import jdk.deprcases.members.ExampleClass;
import jdk.deprcases.members.ExampleInterface;
import jdk.deprcases.members.ExampleSubclass;

public class UseField {
    static class Direct {
        int f(ExampleClass ec) {
            return ec.field1;
        }
    }

    static class Inherited {
        int f(ExampleSubclass esc) {
            return esc.field2;
        }
    }

    static class InterfaceInherited {
        int f(ExampleSubclass esc) {
            return esc.DEP_FIELD2;
        }
    }

    static class InterfaceDirect {
        int f(ExampleInterface ei) {
            return ei.DEP_FIELD1;
        }
    }

    static class FromSubclass extends ExampleClass {
        int f() {
            return field1;
        }
    }

    static class SuperFromSubclass extends ExampleClass {
        int f() {
            return super.field1;
        }
    }

    static class StaticField {
        int f() {
            return ExampleClass.staticfield3;
        }
    }
}
