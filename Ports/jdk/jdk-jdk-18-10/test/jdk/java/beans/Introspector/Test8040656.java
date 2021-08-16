/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.Introspector;
import java.beans.MethodDescriptor;

/*
 * @test
 * @bug 8040656
 * @summary Tests that a normal method is preferred to a synthetic one
 * @author Sergey Malenkov
 */

public class Test8040656 {
    public static void main(String[] args) throws Exception {
        test(String.class, C.class);
        test(String.class, C1.class);
        test(String.class, C2.class);
        test(String.class, C3.class);
        test(String.class, C4.class);
        test(String.class, C5.class);
        test(String.class, C6.class);
        test(String.class, C7.class);
        test(String.class, C8.class);
        test(String.class, C9.class);
    }

    private static void test(Class<?> type, Class<?> bean) throws Exception {
        for (MethodDescriptor md : Introspector.getBeanInfo(bean).getMethodDescriptors()) {
            if (md.getName().equals("getFoo")) {
                if (type != md.getMethod().getReturnType()) {
                    throw new Error("unexpected type");
                }
            }
        }
    }

    public interface A {
        public Object getFoo();
    }

    public class C implements A {
        @Override
        public String getFoo() {
            return null;
        }
    }

    public class C1 implements A {
        @Override
        public String getFoo() {
            return null;
        }

        public String getFoo1() {
            return null;
        }
    }

    public class C2 implements A {
        @Override
        public String getFoo() {
            return null;
        }

        public String getFoo1() {
            return null;
        }

        public String getFoo2() {
            return null;
        }
    }

    public class C3 implements A {
        @Override
        public String getFoo() {
            return null;
        }

        public String getFoo1() {
            return null;
        }

        public String getFoo2() {
            return null;
        }

        public String getFoo3() {
            return null;
        }
    }

    public class C4 implements A {
        @Override
        public String getFoo() {
            return null;
        }

        public String getFoo1() {
            return null;
        }

        public String getFoo2() {
            return null;
        }

        public String getFoo3() {
            return null;
        }

        public String getFoo4() {
            return null;
        }
    }

    public class C5 implements A {
        @Override
        public String getFoo() {
            return null;
        }

        public String getFoo1() {
            return null;
        }

        public String getFoo2() {
            return null;
        }

        public String getFoo3() {
            return null;
        }

        public String getFoo4() {
            return null;
        }

        public String getFoo5() {
            return null;
        }
    }

    public class C6 implements A {
        @Override
        public String getFoo() {
            return null;
        }

        public String getFoo1() {
            return null;
        }

        public String getFoo2() {
            return null;
        }

        public String getFoo3() {
            return null;
        }

        public String getFoo4() {
            return null;
        }

        public String getFoo5() {
            return null;
        }

        public String getFoo6() {
            return null;
        }
    }

    public class C7 implements A {
        @Override
        public String getFoo() {
            return null;
        }

        public String getFoo1() {
            return null;
        }

        public String getFoo2() {
            return null;
        }

        public String getFoo3() {
            return null;
        }

        public String getFoo4() {
            return null;
        }

        public String getFoo5() {
            return null;
        }

        public String getFoo6() {
            return null;
        }

        public String getFoo7() {
            return null;
        }
    }

    public class C8 implements A {
        @Override
        public String getFoo() {
            return null;
        }

        public String getFoo1() {
            return null;
        }

        public String getFoo2() {
            return null;
        }

        public String getFoo3() {
            return null;
        }

        public String getFoo4() {
            return null;
        }

        public String getFoo5() {
            return null;
        }

        public String getFoo6() {
            return null;
        }

        public String getFoo7() {
            return null;
        }

        public String getFoo8() {
            return null;
        }
    }

    public class C9 implements A {
        @Override
        public String getFoo() {
            return null;
        }

        public String getFoo1() {
            return null;
        }

        public String getFoo2() {
            return null;
        }

        public String getFoo3() {
            return null;
        }

        public String getFoo4() {
            return null;
        }

        public String getFoo5() {
            return null;
        }

        public String getFoo6() {
            return null;
        }

        public String getFoo7() {
            return null;
        }

        public String getFoo8() {
            return null;
        }

        public String getFoo9() {
            return null;
        }
    }
}
