/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4336291
 * @summary wrong bytecode for qualified access of superclass field
 * @author gafter
 *
 * @compile SuperField.java
 * @run main SuperField
 */

public class SuperField extends B {
    class IC extends B {
        IC(String name) { super(name); }
        void doCheck() {
            if (SuperField.super.ref.name != "obj.super.ref")
                throw new Error();
        }
    }
    protected B ref = null;

    SuperField(String name) { super(name); }

    public static void main(String[] args) {
        SuperField obj = new SuperField("obj");
        obj.ref = new SuperField("obj.ref");
        obj.put(new SuperField("obj.super.ref"));
        IC ins = obj.new IC("ins");
        ins.doCheck();
    }

}

class B {
    String name;
    protected B ref;

    B(String name) { this.name = name; }
    void put(B b) { ref = b; }

    class S {
        String get() { return name; }
    }
}
