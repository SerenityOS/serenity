/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4116460
 * @summary Test access methods for private constructors.
 * @author William Maddox (maddox)
 *
 * @compile ConstructorAccess.java
 * @run main ConstructorAccess
 */

public class ConstructorAccess {
    int i = 0;
    char c = 'x';

    private ConstructorAccess() {
        this.i = 42;
    }

    private ConstructorAccess(int i, char c) {
        this.i = i;
        this.c = c;
    }

    class Inner {
        int j;
        char c;
        boolean b;

        private Inner() {
            this.j = 0;
            this.b = false;
            this.c = 'x';
        }
        private Inner(int i, char c) {
            this.j = i;
            this.b = true;
            this.c = c;
            ConstructorAccess.this.i = i;
        }
        private Inner(int i, boolean b) {
            this.j = i;
            this.b = b;
            this.c = 'x';
        }
        void foo() throws Exception {
            ConstructorAccess x = new ConstructorAccess();
            if (x.i != 42 || x.c != 'x') {
                throw new Exception("error 1");
            }
            ConstructorAccess y = new ConstructorAccess(555, 'y');
            if (y.i != 555 || y.c != 'y') {
                throw new Exception("error2");
            }
        }
        void check(int j, char c, boolean b) throws Exception {
            if (this.j != j || this.c != c || this.b != b) {
                throw new Exception("error3");
            }
        }
    }

    void bar() throws Exception {
        Inner x = new Inner();
        x.check(0, 'x', false);
        x.foo();
        Inner y = new Inner(747, 'z');
        y.check(747, 'z', true);
        if (this.i != 747) {
            throw new Exception("error 4");
        }
        Inner z = new Inner(777, true);
        z.check(777, 'x' , true);
    }

    class InnerSub extends Inner {
        private InnerSub() {
            super();
        }
        private InnerSub(int i) {
            super(i, 'w');
        }
        private InnerSub(int i, boolean b) {
            super(i, b);
        }
    }

    public static void main(String[] args) throws Exception {
        ConstructorAccess o = new ConstructorAccess();
        o.bar();
        InnerSub x = o.new InnerSub();
        x.check(0, 'x', false);
        x.foo();
        InnerSub y = o.new InnerSub(767);
        y.check(767, 'w', true);
        if (o.i != 767) {
            throw new Exception("error 5");
        }
        InnerSub z = o.new InnerSub(777, true);
        z.check(777, 'x' , true);
    }
}
