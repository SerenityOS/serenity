/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8220018
 * @summary Verify that try-catch-finally inside a switch expression works properly.
 * @compile TryCatchFinally.java
 * @run main TryCatchFinally
 */
public class TryCatchFinally {//TODO: yield <double>
    public static void main(String[] args) {
        for (int p1 = 0; p1 < 2; p1++) {
            for (int p2 = 0; p2 < 2; p2++) {
                for (int p3 = 0; p3 < 2; p3++) {
                    for (int p4 = 0; p4 < 2; p4++) {
                        for (int p5 = 0; p5 < 2; p5++) {
                            for (int p6 = 0; p6 < 3; p6++) {
                                int actual = runSwitchesOrdinary(p1, p2, p3, p4, p5, p6);
                                int expected = computeExpectedOrdinary(p1, p2, p3, p4, p5, p6);
                                if (actual != expected) {
                                    throw new IllegalStateException("actual=" + actual + "; " +
                                                                    "expected=" + expected + ", parameters: " + p1 + ", " + p2 + ", " + p3 + ", " + p4 + ", " + p5 + ", " + p6 + ", ");
                                }
                            }
                        }
                    }
                }
            }
        }
        {
            boolean correct = false;
            int v;
            if (switch (0) {
                case 0:
                    try {
                        if (true) {
                            throw new MarkerException();
                        }
                        yield false;
                    } catch (MarkerException ex) {
                        yield false;
                    } finally {
                        v = 0;
                        yield true;
                    }
                default: yield false;
            } && v == 0) {
                correct = true;
            }
            if (!correct) {
                throw new IllegalStateException();
            }
        }
        {
            boolean correct = false;
            if (switch (0) {
                case 0:
                    try {
                        if (true) {
                            throw new MarkerException();
                        }
                        yield new TryCatchFinally().fls();
                    } catch (MarkerException ex) {
                        yield new TryCatchFinally().fls();
                    } finally {
                        yield true;
                    }
                default: yield new TryCatchFinally().fls();
            }) {
                correct = true;
            }
            if (!correct) {
                throw new IllegalStateException();
            }
        }
        {
            E e = E.A;
            boolean correct = false;
            int v;
            if (switch (0) {
                case 0:
                    try {
                        if (true) {
                            throw new MarkerException();
                        }
                        yield false;
                    } catch (MarkerException ex) {
                        v = 0;
                        yield true;
                    } finally {
                        try {
                            if (true)
                                throw new MarkerException();
                        } catch (MarkerException ex) {
                            e = e.next();
                        }
                    }
                default: yield false;
            } && v == 0) {
                correct = true;
            }
            if (!correct) {
                throw new IllegalStateException();
            }
        }
        {
            E e = E.A;
            boolean correct = false;
            int v;
            if (switch (0) {
                case 0:
                    try {
                        if (true) {
                            throw new MarkerException();
                        }
                        yield false;
                    } catch (MarkerException ex) {
                        yield false;
                    } finally {
                        try {
                            if (true)
                                throw new MarkerException();
                        } catch (MarkerException ex) {
                            e = e.next();
                        } finally {
                            v = 0;
                            yield true;
                        }
                    }
                default: yield false;
            } && v == 0) {
                correct = true;
            }
            if (!correct) {
                throw new IllegalStateException();
            }
        }
        {
            boolean correct = false;
            if (!switch (0) {
                default -> {
                    try {
                        yield switch(0) { default -> true; };
                    }
                    finally {
                        yield false;
                    }
                }
            }) {
                correct = true;
            }
            if (!correct) {
                throw new IllegalStateException();
            }
        }
    }

    private static int runSwitchesOrdinary(int p1, int p2, int p3, int p4, int p5, int p6) {
        return 1 + switch (p1) {
            case 0:
                try {
                    if (p2 == 0) {
                        new TryCatchFinally().throwException();
                    }
                    try {
                        yield 10 + switch (p3) {
                            case 0 -> {
                                try {
                                    if (p4  == 0) {
                                        new TryCatchFinally().throwException();
                                    }
                                    yield 100;
                                } catch (Throwable ex) {
                                    yield 200;
                                } finally {
                                    if (p6 == 0) {
                                        yield 300;
                                    } else if (p6 == 1) {
                                        throw new MarkerException();
                                    }
                                }
                            }
                            default -> 400;
                        };
                    } catch (MarkerException me) {
                        yield 510;
                    }
                } catch(Throwable ex) {
                    try {
                        yield 20 + switch (p3) {
                            case 0 -> {
                                try {
                                    if (p4  == 0) {
                                        new TryCatchFinally().throwException();
                                    }
                                    yield 100;
                                } catch (Throwable ex2) {
                                    yield 200;
                                } finally {
                                    if (p6 == 0) {
                                        yield 300;
                                    } else if (p6 == 1) {
                                        throw new MarkerException();
                                    }
                                }
                            }
                            default -> 400;
                        };
                    } catch (MarkerException me) {
                        yield 520;
                    }
                } finally {
                    if (p5 == 0) {
                        try {
                            yield 30 + switch (p3) {
                                case 0 -> {
                                    try {
                                        if (p4  == 0) {
                                            new TryCatchFinally().throwException();
                                        }
                                        yield 100;
                                    } catch (Throwable ex) {
                                        yield 200;
                                    } finally {
                                        if (p6 == 0) {
                                            yield 300;
                                        } else if (p6 == 1) {
                                            throw new MarkerException();
                                        }
                                    }
                                }
                                default -> 400;
                            };
                        } catch (MarkerException me) {
                            yield 530;
                        }
                    }
                }
            default: yield 40;
        };
    }

    private static int computeExpectedOrdinary(int p1, int p2, int p3, int p4, int p5, int p6) {
        int expected = 0;

        if (p1 == 0) {
            if (p5 == 0) {
                expected = 30;
            } else if (p2 == 0) {
                expected = 20;
            } else {
                expected = 10;
            }
            if (p3 == 0) {
                if (p6 == 0) {
                    expected += 300;
                } else if (p6 == 1) {
                    expected += 500;
                } else if (p4 == 0) {
                    expected += 200;
                } else {
                    expected += 100;
                }
            } else {
                expected += 400;
            }
        } else {
            expected = 40;
        }

        expected += 1;

        return expected;
    }

    private boolean fls() {
        return false;
    }
    private void throwException() {
        throw new RuntimeException();
    }

    static class MarkerException extends Throwable {}

    enum E {
        A, B, C;
        public E next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }
}
