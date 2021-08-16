/*
 * Copyright (c) 2017, Red Hat, Inc. All rights reserved.
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
 * @bug 8174164
 * @summary SafePointNode::_replaced_nodes breaks with irreducible loops
 * @run main/othervm -XX:-BackgroundCompilation TestReplacedNodesOSR
 *
 */

public class TestReplacedNodesOSR {

    static Object dummy;

    static interface I {
    }

    static class A implements I {
    }

    static final class MyException extends Exception {
    }

    static final A obj = new A();
    static I static_field() { return obj; }

    // When OSR compiled, this method has an irreducible loop
    static void test(int v, MyException e) {
        int i = 0;
        for (;;) {
            if (i == 1000) {
                break;
            }
            try {
                if ((i%2) == 0) {
                    int j = 0;
                    for (;;) {
                        j++;
                        if (i+j != v) {
                            if (j == 1000) {
                                break;
                            }
                        } else {
                            A a = (A)static_field();
                            // replaced node recorded here
                            throw e;
                        }
                    }
                }
            } catch(MyException ex) {
            }
            i++;
            // replaced node applied on return of the method
            // replaced node used here
            dummy = static_field();
        }
    }


    static public void main(String[] args) {
        for (int i = 0; i < 1000; i++) {
            test(1100, new MyException());
        }
    }
}
