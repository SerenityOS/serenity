/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4986231
 * @summary varargs versus anonymous constructors crashes javac
 * @author gafter
 *
 * @compile  Anon.java
 */

class Anon {
    Anon() {
        Fox fox1 = new Fox(1) {};
        Fox fox2 = new Fox(new String[] { "hello" }) {};
        Fox fox3 = new Fox(null) {};
        Fox fox4 = new Fox() {};
        Fox fox5 = new Fox("hello") {};
        Fox fox6 = new Fox("hello", "bye") {};
    }
}

class Fox {
    Fox(int a) {
        _keys = new String[0];
    }

    Fox(String... keys) {
        _keys = keys;
    }

    final String[] _keys;

}
