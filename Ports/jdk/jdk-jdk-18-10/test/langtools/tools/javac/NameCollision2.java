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
 * @bug 4615601
 * @summary False detection of duplicate local class declaration
 * @author gafter
 *
 * @compile NameCollision2.java
 * @run main NameCollision2
 */

public class NameCollision2 {
    boolean x1 = false;
    boolean x2 = false;
    void foo() {
        class Local {{ x1 = true; }}
        { new Local(); }
        new Object() {
            class Local {{ x2 = true; }}
            { new Local(); }
        };
    }
    void check() {
        foo();
        if (!x1) throw new Error("x1");
        if (!x2) throw new Error("x2");
    }
    public static void main(String[] args) {
        new NameCollision2().check();
    }
}
