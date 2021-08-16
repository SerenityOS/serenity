/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8254557
 * @summary Method Attr.preFlow shouldn't visit class definitions that have not yet been entered and attributed.
 * @compile T8254557.java
 */

import java.util.Iterator;
import java.util.function.Function;

public class T8254557 {
    // test anonymous class in if statement
    public <T> void testIf(boolean b) {
        test(rs -> {
            if (b) {
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public T next() {
                        return null;
                    }
                };
            } else {
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public T next() {
                        return null;
                    }
                };
            }
        });
    }

    // test anonymous class in while statement
    public <T> void testWhile(boolean b) {
        test(rs -> {
            while (b) {
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public T next() {
                        return null;
                    }
                };
            }
            return null;
        });
    }

    // test anonymous class in do while statement
    public <T> void testDoWhileLoop(boolean b) {
        test(rs -> {
            do {
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public T next() {
                        return null;
                    }
                };
            } while (b);
        });
    }

    // test anonymous class in for statement
    public <T> void testForLoop(boolean b) {
        test(rs -> {
            for ( ; ; ) {
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public T next() {
                        return null;
                    }
                };
            }
        });
    }

    private void test(Function function) { }
}