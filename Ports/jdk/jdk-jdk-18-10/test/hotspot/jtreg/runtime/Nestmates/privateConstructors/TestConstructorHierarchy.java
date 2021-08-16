/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8046171
 * @summary Test access to private constructors in the hierarchy that are
 * outside the nest
 * @compile TestConstructorHierarchy.java
 * @compile ExternalSuper.jcod
 *          ExternalSub.jcod
 * @run main TestConstructorHierarchy
 */

public class TestConstructorHierarchy {

    static class NestedA extends ExternalSuper {
        private NestedA() {}
        protected NestedA(int i) {} // for compile-time only
    }

    // Access to private members of classes outside the nest is
    // not permitted. These tests should throw IllegalAccessError
    // at runtime. To allow them to compile the classes below are
    // defined with public members. We then replace those class files
    // with jcod variants that make the member private again.

    public static void main(String[] args) throws Throwable {
        try {
            new ExternalSuper();
            throw new Error("Unexpected construction of ExternalSuper");
        }
        catch (IllegalAccessError iae) {
            if (iae.getMessage().contains("class TestConstructorHierarchy tried to access private method 'void ExternalSuper.<init>()'")) {
                System.out.println("Got expected exception constructing ExternalSuper: " + iae);
            }
            else throw new Error("Unexpected IllegalAccessError: " + iae);
        }
        try {
            new NestedA();
            throw new Error("Unexpected construction of NestedA and supers");
        }
        catch (IllegalAccessError iae) {
            if (iae.getMessage().contains("class TestConstructorHierarchy$NestedA tried to access private method 'void ExternalSuper.<init>()'")) {
                System.out.println("Got expected exception constructing NestedA: " + iae);
            }
            else throw new Error("Unexpected IllegalAccessError: " + iae);
        }
        try {
            new ExternalSub();
            throw new Error("Unexpected construction of ExternalSub");
        }
        catch (IllegalAccessError iae) {
            if (iae.getMessage().contains("class ExternalSub tried to access private method 'void TestConstructorHierarchy$NestedA.<init>()'")) {
                System.out.println("Got expected exception constructing ExternalSub: " + iae);
            }
            else throw new Error("Unexpected IllegalAccessError: " + iae);
        }
    }
}

// Classes that are not part of the nest.
// Being non-public allows us to declare them in this file.
// The constructor is public to allow this file to compile, but
// the jcod files change it back to private.

class ExternalSuper {
    public ExternalSuper() { }
}


class ExternalSub extends TestConstructorHierarchy.NestedA {
    public ExternalSub() {
        super(0); // this is changed to super() in jcod file
    }
}
