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

/**
 * @test
 * @compile Basic.java
 * @run testng p.Basic
 * @summary Basic test for java.lang.Class::getPackageName
 */

package p;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import static org.testng.Assert.*;

public class Basic {


    // -- member classes --

    static class Nested {
        static class Foo { }
    }

    Class<?> getNestedClass1() {
        return Nested.class;
    }
    Class<?> getNestedClass2() {
        return Nested.Foo.class;
    }

    class Inner {
        class Foo { }
    }

    Class<?> getInnerClass1() {
        return Inner.class;
    }
    Class<?> getInnerClass2() {
        return Inner.Foo.class;
    }

    // -- local and anonymous classes --

    Class<?> getLocalClass1() {
        class Local { }
        return Local.class;
    }

    Class<?> getLocalClass2() {
        class Local {
            class Foo { }
        }
        return Local.Foo.class;
    }

    Class<?> getLocalClass3() {
        class Local {
            final Class<?> c;
            Local() {
                class Foo { }
                this.c = Foo.class;
            }
            Class<?> get() {
                return c;
            }
        }
        return new Local().get();
    }

    Class<?> getAnonymousClass1() {
        Runnable r = new Runnable() { public void run() { } };
        return r.getClass();
    }

    Class<?> getAnonymousClass2() {
        class Local {
            Class<?> get() {
                Runnable r = new Runnable() { public void run() { } };
                return r.getClass();
            }
        }
        return new Local().get();
    }

    Class<?> getAnonymousClass3() {
        Runnable r = () -> { };
        return r.getClass();
    }

    Class<?> getAnonymousClass4() {
        class Local {
            Class<?> get() {
                Runnable r = () -> { };
                return r.getClass();
            }
        }
        return new Local().get();
    }

    Class<?> getAnonymousClass5() {
        class Local {
            final Class<?> c;
            Local() {
                Runnable r = new Runnable() { public void run() { } };
                this.c = r.getClass();
            }
            Class<?> get() {
                return c;
            }
        }
        return new Local().get();
    }

    Class<?> getAnonymousClass6() {
        class Local {
            final Class<?> c;
            Local() {
                Runnable r = () -> { };
                this.c = r.getClass();
            }
            Class<?> get() {
                return c;
            }
        }
        return new Local().get();
    }

    static final String TEST_PACKAGE = Basic.class.getPackage().getName();

    @DataProvider(name = "classes")
    public Object[][] classes() {
        return new Object[][] {

            { Basic.class,                  TEST_PACKAGE },
            { Basic[].class,                TEST_PACKAGE },
            { Basic[][].class,              TEST_PACKAGE },

            { getNestedClass1(),            TEST_PACKAGE },
            { getNestedClass2(),            TEST_PACKAGE },
            { getInnerClass1(),             TEST_PACKAGE },
            { getInnerClass2(),             TEST_PACKAGE },

            { getLocalClass1(),             TEST_PACKAGE },
            { getLocalClass2(),             TEST_PACKAGE },
            { getLocalClass3(),             TEST_PACKAGE },

            { getAnonymousClass1(),         TEST_PACKAGE },
            { getAnonymousClass2(),         TEST_PACKAGE },
            { getAnonymousClass3(),         TEST_PACKAGE },
            { getAnonymousClass4(),         TEST_PACKAGE },
            { getAnonymousClass5(),         TEST_PACKAGE },
            { getAnonymousClass6(),         TEST_PACKAGE },

            { Object.class,                 "java.lang" },
            { Object[].class,               "java.lang" },
            { Object[][].class,             "java.lang" },

            { int.class,                    "java.lang" },
            { int[].class,                  "java.lang" },
            { int[][].class,                "java.lang" },

            { void.class,                   "java.lang" },

        };
    }

    @Test(dataProvider = "classes")
    public void testPackageName(Class<?> type, String expected) {
        assertEquals(type.getPackageName(), expected);
    }

}
