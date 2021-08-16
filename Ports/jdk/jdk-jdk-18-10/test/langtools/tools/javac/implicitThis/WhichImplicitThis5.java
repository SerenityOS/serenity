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

/**
 * @test
 * @bug 4717593
 * @summary Hiding of fields not fully supported
 *
 * @compile WhichImplicitThis5.java
 * @run main WhichImplicitThis5
 */

public class WhichImplicitThis5 {
    static int init;
    public int i = ++init;
    class One extends WhichImplicitThis5 {
        private Object i; // hide enclosing i
    }
    class Two extends One {
        // has no i member
        Two() {
            WhichImplicitThis5.this.super();
        }
        int j = i; // i from enclosing scope
        int k = ((WhichImplicitThis5) this).i; // get hidden i
        Object l = super.i;
    }
    public static void main(String[] args) {
        Two t = new WhichImplicitThis5().new Two();
        if (t.j != 1 || t.k != 2 || t.l != null)
            throw new Error();
    }
}
