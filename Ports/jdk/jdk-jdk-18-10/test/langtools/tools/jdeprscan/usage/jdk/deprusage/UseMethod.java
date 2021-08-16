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

public class UseMethod {
    static class Direct {
        void m(ExampleClass ec) {
            ec.method1();
        }
    }

    static class Inherited {
        void m(ExampleSubclass esc) {
            esc.method2();
        }
    }

    static class InheritedDefault {
        void m(ExampleSubclass esc) {
            esc.defaultMethod();
        }
    }

    static class InterfaceDirect {
        void m(ExampleInterface ei) {
            ei.interfaceMethod1();
        }
    }

    static class InterfaceDefault {
        void m(ExampleInterface ei) {
            ei.defaultMethod();
        }
    }

    static class ClassStatic {
        void m() {
            ExampleClass.staticmethod1();
        }
    }

    static class InterfaceStatic {
        void m() {
            ExampleInterface.staticmethod2();
        }
    }

    static class SuperFromSubclass extends ExampleClass {
        void m() {
            super.method1();
        }
    }

    static class InheritedFromSubclass extends ExampleClass {
        void m() {
            method1();
        }
    }

    static class Constructor {
        Object m() {
            return new ExampleClass(true);
        }
    }

    static class ConstructorFromSubclass extends ExampleClass {
        public ConstructorFromSubclass() {
            super(true);
        }
    }

    abstract static class InheritedInterfaceDefault extends ExampleSubclass {
        void m() {
            defaultMethod();
        }
    }

    abstract static class InheritedInterface extends ExampleSubclass {
        void m() {
            interfaceMethod1();
        }
    }

    static class OverrideClassMethod extends ExampleClass {
        @Override
        public void method1() { }
    }

    abstract static class OverrideInterfaceMethod implements ExampleInterface {
        @Override
        public void interfaceMethod1() { }
    }

    abstract static class OverrideDefaultMethod implements ExampleInterface {
        @Override
        public void defaultMethod() { }
    }
}
