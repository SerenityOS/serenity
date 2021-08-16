/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8076538
 * @summary Verify error at runtime due to incorrect classification of a lambda as being instance capturing
 * @run main NestedCapture04
 */
public class NestedCapture04 {

    public static interface Ftype {
        int get(int v);
    }

    public static class A {
        static int counter = 0;
    }
    public static Ftype x0;
    public static void main(String[] args) throws Throwable {
        doit();
    }
    public static Object doit() throws Throwable {
        Ftype x0_ =
        (int y0) -> {
    A.counter++;
    Ftype x1 = (int y1) -> {
        A.counter++;
        class Cltype2 {
            Cltype2 meth(Cltype2 w) {
                A.counter++;
                class Cltype3 {
                    class Inclass3 {
                        public int iv;
                        Inclass3() { iv = 0; }
                        Inclass3 clmeth(Inclass3 a) {
                            A.counter++;
                            class Cltype4 {
                                Cltype4 (Cltype4 z) {
                                    Ftype x5 = (int y5) -> {
                                        A.counter++;
                                        class Cltype6 {
                                            Cltype6 meth(Cltype6 w) {
                                                A.counter++;
                                                class Cltype7 {
                                                    class Inclass7 {
                                                        public int iv;
                                                        Inclass7() { iv = 0; }
                                                        Inclass7 clmeth(Inclass7 a) {
                                                            A.counter++;
                                                            class Cltype8 {
                                                                Cltype8 (Cltype8 z) {
                                                                    Ftype x9 = (int y9) -> {
                                                                        A.counter++;
                                                                        return y9;
                                                                    };
                                                                    x9.get(2);
                                                                    if ( z == null) {
                                                                        A.counter++;
                                                                        return;
                                                                    }
                                                                    A.counter+=100;
                                                                }
                                                            }
                                                            Cltype8 v = new Cltype8(null);
                                                            return a;
                                                        }
                                                    }
                                                }
                                                Cltype7.Inclass7 c = new Cltype7().new Inclass7();
                                                c.clmeth((Cltype7.Inclass7)null);
                                                return w;
                                            }
                                        }
                                        Cltype6 v = new Cltype6().meth(new Cltype6());
                                        return y5;
                                    };
                                    x5.get(2);
                                    if ( z == null) {
                                        A.counter++;
                                        return;
                                    }
                                    A.counter+=100;
                                }
                            }
                            Cltype4 v = new Cltype4(null);
                            return a;
                        }
                    }
                }
                Cltype3.Inclass3 c = new Cltype3().new Inclass3();
                c.clmeth((Cltype3.Inclass3)null);
                return w;
            }
        }
        Cltype2 v = new Cltype2().meth(new Cltype2());
        return y1;
    };
    x1.get(2);
    return y0;
};
        return x0 = x0_;
    }
}
