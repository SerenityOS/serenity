/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.tools.classfile.TypeAnnotation.TargetType.*;

/*
 * @test
 * @bug 8042451 8044009 8044010
 * @summary Test population of reference info for nested types
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @ignore 8057687 emit correct byte code an attributes for type annotations
 * @compile -g Driver.java ReferenceInfoUtil.java NestedTypes.java
 * @run main Driver NestedTypes
 */
public class NestedTypes {

    // method parameters

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {}, paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {1, 0}, paramIndex = 0)
    public String testParam1() {
        return "void test(@TA Outer.@TB Inner a) { }";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0}, paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 1, 0}, paramIndex = 0)
    public String testParam1b() {
        return "void test(List<@TA Outer.@TB Inner> a) { }";
    }

    // TODO: the tests that use @TA Map.Entry should fail, as
    // Map cannot be annotated.
    // We need some tests for the fully qualified name syntax.
    /*
    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {}, paramIndex = 0)
    public String testParam1c() {
        return "void test(java.util.@TA Map.Entry a) { }";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {}, paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {1, 0}, paramIndex = 0)
    })
    public String testParam1d() {
        return "void test(java.util.@TA Map.@TB Entry a) { }";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0}, paramIndex = 0)
    public String testParam1e() {
        return "void test(List<java.util.@TA Map.Entry> a) { }";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {3, 0}, paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {3, 0, 1, 0}, paramIndex = 0)
    })
    public String testParam1f() {
        return "void test(List<java.util.@TA Map. @TB Entry> a) { }";
    }
    */

    @TADescription(annotation = "TB", type = METHOD_FORMAL_PARAMETER,
           genericLocation = {3, 0}, paramIndex = 0)
    public String testParam1g() {
        return "void test(List<java.util.Map. @TB Entry> a) { }";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {}, paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {1, 0}, paramIndex = 0)
    public String testParam2() {
        return "void test(@TA GOuter<String,String>.@TB GInner<String,String> a) { }";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0}, paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 1, 0}, paramIndex = 0)
    public String testParam2b() {
        return "void test(List<@TA GOuter<String,String>.@TB GInner<String,String>> a) { }";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0}, paramIndex = 0)
    @TADescription(annotation = "TC", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0}, paramIndex = 0)
    @TADescription(annotation = "TD", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "TE", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0}, paramIndex = 0)
    @TADescription(annotation = "TF", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "TG", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0}, paramIndex = 0)
    @TADescription(annotation = "TH", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 0}, paramIndex = 0)
    @TADescription(annotation = "TI", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 1}, paramIndex = 0)
    @TADescription(annotation = "TJ", type = METHOD_FORMAL_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "TK", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0}, paramIndex = 0)
    public String testParam3() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " void test(@TA Outer . @TB GInner<@TC List<@TD Object @TE[] @TF[]>>. @TG GInner2<@TH Integer, @TI Object> @TJ[] @TK[] a) { }\n" +
                "}";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0}, paramIndex = 0)
    @TADescription(annotation = "TC", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0}, paramIndex = 0)
    @TADescription(annotation = "TD", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "TE", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0}, paramIndex = 0)
    @TADescription(annotation = "TF", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "TG", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0}, paramIndex = 0)
    @TADescription(annotation = "TH", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 0}, paramIndex = 0)
    @TADescription(annotation = "TI", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 1}, paramIndex = 0)
    @TADescription(annotation = "TJ", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0}, paramIndex = 0)
    @TADescription(annotation = "TK", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0}, paramIndex = 0)
    public String testParam4() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " void test(List<@TA Outer . @TB GInner<@TC List<@TD Object @TE[] @TF[]>>. @TG GInner2<@TH Integer, @TI Object> @TJ[] @TK[]> a) { }\n" +
                "}";
    }


    // Local variables

    @TADescription(annotation = "TA", type = LOCAL_VARIABLE,
            genericLocation = {},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TB", type = LOCAL_VARIABLE,
            genericLocation = {1, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    public String testLocal1a() {
        return "void test() { @TA Outer.@TB Inner a = null; }";
    }

    @TADescription(annotation = "TA", type = LOCAL_VARIABLE,
            genericLocation = {},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    public String testLocal1b() {
        return "void test() { @TA Outer.Inner a = null; }";
    }

    @TADescription(annotation = "TB", type = LOCAL_VARIABLE,
            genericLocation = {1, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    public String testLocal1c() {
        return "void test() { Outer.@TB Inner a = null; }";
    }

    @TADescription(annotation = "TA", type = LOCAL_VARIABLE,
            genericLocation = {},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TB", type = LOCAL_VARIABLE,
            genericLocation = {1, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    public String testLocal2() {
        return "void test() { @TA GOuter<String,String>.@TB GInner<String,String> a = null; }";
    }

    @TADescription(annotation = "TA", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TB", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TC", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TD", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TE", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TF", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TG", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TH", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TI", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 1},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TJ", type = LOCAL_VARIABLE,
            genericLocation = {},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TK", type = LOCAL_VARIABLE,
            genericLocation = {0, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    public String testLocal3() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " void test() { @TA Outer . @TB GInner<@TC List<@TD Object @TE[] @TF[]>>. @TG GInner2<@TH Integer, @TI Object> @TJ[] @TK[] a = null; }\n" +
                "}";
    }


    @TADescription(annotation = "TA", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TB", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TC", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TD", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TE", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TF", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TG", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TH", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TI", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 1},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TJ", type = LOCAL_VARIABLE,
            genericLocation = {3, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "TK", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    public String testLocal4() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " void test() { List<@TA Outer . @TB GInner<@TC List<@TD Object @TE[] @TF[]>>. @TG GInner2<@TH Integer, @TI Object> @TJ[] @TK[]> a = null; }\n" +
                "}";
    }


    // fields

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {})
    @TADescription(annotation = "TB", type = FIELD,
            genericLocation = {1, 0})
    public String testField1a() {
        return "@TA Outer.@TB Inner a;";
    }

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {})
    public String testField1b() {
        return "@TA Outer.Inner a;";
    }

    @TADescription(annotation = "TB", type = FIELD,
            genericLocation = {1, 0})
    public String testField1c() {
        return "Outer.@TB Inner a;";
    }

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {})
    @TADescription(annotation = "TB", type = FIELD,
            genericLocation = {1, 0})
    public String testField2() {
        return "@TA GOuter<String,String>.@TB GInner<String,String> a;";
    }

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {0, 0, 0, 0})
    @TADescription(annotation = "TB", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "TC", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0})
    @TADescription(annotation = "TD", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "TE", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0})
    @TADescription(annotation = "TF", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0})
    @TADescription(annotation = "TG", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0})
    @TADescription(annotation = "TH", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 0})
    @TADescription(annotation = "TI", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 1})
    @TADescription(annotation = "TJ", type = FIELD)
    @TADescription(annotation = "TK", type = FIELD,
            genericLocation = {0, 0})
    public String testField3() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " @TA Outer . @TB GInner<@TC List<@TD Object @TE[] @TF[]>>. @TG GInner2<@TH Integer, @TI Object> @TJ[] @TK[] a;\n" +
                "}";
    }

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "TB", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "TC", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0})
    @TADescription(annotation = "TD", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "TE", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0})
    @TADescription(annotation = "TF", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0})
    @TADescription(annotation = "TG", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0})
    @TADescription(annotation = "TH", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 0})
    @TADescription(annotation = "TI", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 1})
    @TADescription(annotation = "TJ", type = FIELD,
            genericLocation = {3, 0})
    @TADescription(annotation = "TK", type = FIELD,
            genericLocation = {3, 0, 0, 0})
    public String testField4() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " List<@TA Outer . @TB GInner<@TC List<@TD Object @TE[] @TF[]>>. @TG GInner2<@TH Integer, @TI Object> @TJ[] @TK[]> a;\n" +
                "}";
    }


    // return types

    @TADescription(annotation = "TA", type = METHOD_RETURN,
            genericLocation = {})
    @TADescription(annotation = "TB", type = METHOD_RETURN,
            genericLocation = {1, 0})
    public String testReturn1() {
        return "@TA Outer.@TB Inner test() { return null; }";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN,
            genericLocation = {})
    @TADescription(annotation = "TB", type = METHOD_RETURN,
            genericLocation = {1, 0})
    public String testReturn2() {
        return "@TA GOuter<String,String>.@TB GInner<String,String> test() { return null; }";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0})
    @TADescription(annotation = "TB", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "TC", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0})
    @TADescription(annotation = "TD", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "TE", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0})
    @TADescription(annotation = "TF", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0})
    @TADescription(annotation = "TG", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0})
    @TADescription(annotation = "TH", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 0})
    @TADescription(annotation = "TI", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 1})
    @TADescription(annotation = "TJ", type = METHOD_RETURN)
    @TADescription(annotation = "TK", type = METHOD_RETURN,
            genericLocation = {0, 0})
    public String testReturn3() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " @TA Outer . @TB GInner<@TC List<@TD Object @TE[] @TF[]>>. @TG GInner2<@TH Integer, @TI Object> @TJ[] @TK[] test() { return null; }\n" +
                "}";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "TB", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "TC", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0})
    @TADescription(annotation = "TD", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "TE", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0})
    @TADescription(annotation = "TF", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0})
    @TADescription(annotation = "TG", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0})
    @TADescription(annotation = "TH", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 0})
    @TADescription(annotation = "TI", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 1})
    @TADescription(annotation = "TJ", type = METHOD_RETURN,
            genericLocation = {3, 0})
    @TADescription(annotation = "TK", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0})
    public String testReturn4() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " List<@TA Outer . @TB GInner<@TC List<@TD Object @TE[] @TF[]>>. @TG GInner2<@TH Integer, @TI Object> @TJ[] @TK[]> test() { return null; }\n" +
                "}";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN,
                genericLocation = {3, 0})
    @TADescription(annotation = "TB", type = METHOD_RETURN,
                genericLocation = {3, 0, 3, 0})
    @TADescription(annotation = "TC", type = METHOD_RETURN,
                genericLocation = {3, 0, 3, 1})
    @TADescription(annotation = "TD", type = METHOD_RETURN,
                genericLocation = {3, 0, 3, 1, 3, 0})
    @TADescription(annotation = "TE", type = METHOD_RETURN,
                genericLocation = {3, 0, 1, 0})
    @TADescription(annotation = "TF", type = METHOD_RETURN,
                genericLocation = {3, 0, 1, 0, 3, 0})
    @TADescription(annotation = "TG", type = METHOD_RETURN,
                genericLocation = {3, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "TH", type = METHOD_RETURN,
                genericLocation = {3, 0, 1, 0, 3, 0, 3, 0})
    @TADescription(annotation = "TI", type = METHOD_RETURN,
                genericLocation = {3, 0, 1, 0, 3, 0, 3, 0, 0, 0})
    @TADescription(annotation = "TJ", type = METHOD_RETURN,
                genericLocation = {3, 0, 1, 0, 1, 0})
    public String testReturn5() {
        return "class GOuter<A, B> {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " List<@TA GOuter<@TB String, @TC List<@TD Object>> . @TE GInner<@TF List<@TG Object @TH[] @TI[]>>. @TJ GInner2<String, String>> test() { return null; }\n" +
                "}";
    }


    // type parameters

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {}, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {1, 0}, paramIndex = 0, boundIndex = 0)
    public String testTypeparam1() {
        return "<X extends @TA Outer.@TB Inner> X test() { return null; }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {}, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {1, 0}, paramIndex = 0, boundIndex = 0)
    public String testTypeparam2() {
        return "<X extends @TA GOuter<String,String>.@TB GInner<String,String>> X test() { return null; }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {},
                paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {1, 0},
                paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TC", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {1, 0, 3, 0},
                paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TD", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {1, 0, 3, 0, 3, 0, 0, 0, 0, 0},
                paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TE", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {1, 0, 3, 0, 3, 0},
                paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TF", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {1, 0, 3, 0, 3, 0, 0, 0},
                paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TG", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {1, 0, 1, 0},
                paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TH", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {1, 0, 1, 0, 3, 0},
                paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TI", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {1, 0, 1, 0, 3, 1},
                paramIndex = 0, boundIndex = 0)
    public String testTypeparam3() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " <X extends @TA Outer . @TB GInner<@TC List<@TD Object @TE[] @TF[]>>. @TG GInner2<@TH Integer, @TI Object>> X test() { return null; }\n" +
                "}";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 0, 0, 0, 0},
                paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 0, 0, 0, 0, 1, 0},
                paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TC", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0},
                paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TD", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0},
                paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TE", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0},
                paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TF", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0},
                paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TG", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0},
                paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TH", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 0},
                paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TI", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 1},
                paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TJ", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0},
                paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TK", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 0, 0},
                paramIndex = 0, boundIndex = 1)
    public String testTypeparam4() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " <X extends List<@TA Outer . @TB GInner<@TC List<@TD Object @TE[] @TF[]>>. @TG GInner2<@TH Integer, @TI Object> @TJ[] @TK[]>> X test() { return null; }\n" +
                "}";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 3, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TC", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 3, 1}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TD", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 3, 1, 3, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TE", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 1, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TF", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 1, 0, 3, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TG", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TH", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 1, 0, 3, 0, 3, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TI", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 1, 0, 3, 0, 3, 0, 0, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TJ", type = METHOD_TYPE_PARAMETER_BOUND,
                genericLocation = {3, 0, 1, 0, 1, 0}, paramIndex = 0, boundIndex = 1)
    public String testTypeparam5() {
        return "class GOuter<A, B> {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " <X extends List<@TA GOuter<@TB String, @TC List<@TD Object>> . @TE GInner<@TF List<@TG Object @TH[] @TI[]>>. @TJ GInner2<String, String>>> X test() { return null; }\n" +
                "}";
    }

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {3, 0, 1, 0})
    public String testUses1a() {
        return "class %TEST_CLASS_NAME% { class Inner {}    List<@TA Inner> f; }";
    }

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {3, 0})
    public String testUses1b() {
        return "class %TEST_CLASS_NAME% { class Inner {}    List<@TA %TEST_CLASS_NAME%.Inner> f; }";
    }

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {3, 0, 1, 0, 1, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUses2a() {
        return "class %TEST_CLASS_NAME% { class Inner { class Inner2{}    List<@TA Inner2> f; }}";
    }

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {3, 0, 1, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUses2b() {
        return "class %TEST_CLASS_NAME% { class Inner { class Inner2{}    List<@TA Inner.Inner2> f; }}";
    }

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {3, 0, 1, 0, 1, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUses2c() {
        return "class %TEST_CLASS_NAME% { class Inner { class Inner2{}    List<Inner.@TA Inner2> f; }}";
    }

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {3, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUses2d() {
        return "class %TEST_CLASS_NAME%{ class Inner { class Inner2{}    List<@TA %TEST_CLASS_NAME%.Inner.Inner2> f; }}";
    }

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {3, 0, 1, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUses2e() {
        return "class %TEST_CLASS_NAME% { class Inner { class Inner2{}    List<%TEST_CLASS_NAME%.@TA Inner.Inner2> f; }}";
    }

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {3, 0, 1, 0, 1, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUses2f() {
        return "class %TEST_CLASS_NAME% { class Inner { class Inner2{}    List<%TEST_CLASS_NAME%.Inner.@TA Inner2> f; }}";
    }

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {3, 0, 1, 0, 1, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUses3a() {
        return "class %TEST_CLASS_NAME% { class Inner<A, B> { class Inner2<C, D>{}\n" +
                "    List<%TEST_CLASS_NAME%.Inner.@TA Inner2> f; }}";
    }

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {3, 0, 1, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUses3b() {
        return "class %TEST_CLASS_NAME% { class Inner<A, B> { class Inner2<C, D>{}\n" +
                "    List<%TEST_CLASS_NAME%.@TA Inner.Inner2> f; }}";
    }

    @TADescription(annotation = "TA", type = FIELD,
                genericLocation = {})
    @TADescription(annotation = "TB", type = FIELD,
                genericLocation = {3, 0})
    public String testUses4() {
        return "class %TEST_CLASS_NAME% { static class TInner {}\n" +
                "    @TA TInner f; \n" +
                "    List<@TB TInner> g; }";
    }

    @TADescription(annotation = "TA", type = FIELD,
            genericLocation = {3, 0, 1, 0, 3, 1})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUses3c() {
        return "class %TEST_CLASS_NAME% { class Inner<A, B> { class Inner2<C, D>{}\n" +
                "    List<%TEST_CLASS_NAME%.Inner<String, @TA Object>.Inner2<%TEST_CLASS_NAME%, %TEST_CLASS_NAME%>> f; }}";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER, paramIndex=0)
    public String testFullyQualified1() {
        return "void testme(java.security.@TA ProtectionDomain protectionDomain) {}";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER, paramIndex=0,
            genericLocation = {3, 0})
    public String testFullyQualified2() {
        return "void testme(List<java.security.@TA ProtectionDomain> protectionDomain) {}";
    }

    @TADescription(annotation = "TA", type = LOCAL_VARIABLE,
                genericLocation = {},
                lvarOffset = ReferenceInfoUtil.IGNORE_VALUE,
                lvarLength = ReferenceInfoUtil.IGNORE_VALUE,
                lvarIndex = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = LOCAL_VARIABLE,
                genericLocation = {1, 0},
                lvarOffset = ReferenceInfoUtil.IGNORE_VALUE,
                lvarLength = ReferenceInfoUtil.IGNORE_VALUE,
                lvarIndex = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TC", type = LOCAL_VARIABLE,
                // Only classes count, not methods.
                genericLocation = {1, 0, 1, 0},
                lvarOffset = ReferenceInfoUtil.IGNORE_VALUE,
                lvarLength = ReferenceInfoUtil.IGNORE_VALUE,
                lvarIndex = ReferenceInfoUtil.IGNORE_VALUE)
    @TestClass("Outer$Inner")
    public String testMethodNesting1() {
        return "class Outer {\n" +
                " class Inner {\n" +
                "  void foo() {\n" +
                "    class MInner {}\n" +
                "    @TA Outer . @TB Inner l1 = null;\n" +
                "    @TC MInner l2 = null;\n" +
                "  }\n" +
                "}}\n";
    }

    @TADescription(annotation = "TA", type = NEW,
                genericLocation = {},
                offset = 0)
    @TADescription(annotation = "TB", type = NEW,
                genericLocation = {1, 0},
                offset = 0)
    @TADescription(annotation = "TC", type = NEW,
                // Only classes count, not methods.
                genericLocation = {1, 0, 1, 0},
                offset = 12)
    @TestClass("Outer$Inner")
    public String testMethodNesting2() {
        return "class Outer {\n" +
                " class Inner {\n" +
                "  void foo() {\n" +
                "    class MInner {}\n" +
                "    Object o1 = new @TA Outer . @TB Inner();" +
                "    Object o2 = new @TC MInner();\n" +
                "  }\n" +
                "}}\n";
    }

    @TADescription(annotation = "TA", type = CLASS_EXTENDS,
                genericLocation = {}, typeIndex = -1)
    @TADescription(annotation = "TB", type = CLASS_EXTENDS,
                genericLocation = {3, 0}, typeIndex = -1)
    @TADescription(annotation = "TC", type = CLASS_EXTENDS,
                genericLocation = {3, 1}, typeIndex = -1)
    @TADescription(annotation = "TD", type = CLASS_EXTENDS,
                genericLocation = {1, 0}, typeIndex = -1)
    @TADescription(annotation = "TE", type = CLASS_EXTENDS,
                genericLocation = {1, 0, 3, 0}, typeIndex = -1)
    @TADescription(annotation = "TF", type = CLASS_EXTENDS,
                genericLocation = {1, 0, 3, 1}, typeIndex = -1)
    @TestClass("GOuter$GInner$Test")
    public String testExtends1() {
        return "class GOuter<A, B> {\n" +
                "  class GInner<X, Y> {\n" +
                "    class Test extends @TA GOuter<@TB String, @TC String>.@TD GInner<@TE String, @TF String> {}" +
                "  }" +
                "}";
    }

    @TADescription(annotation = "TA", type = CLASS_TYPE_PARAMETER,
                genericLocation = {}, paramIndex = 0)
    @TADescription(annotation = "TB", type = CLASS_TYPE_PARAMETER_BOUND,
                genericLocation = {}, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TC", type = FIELD,
                genericLocation = {})
    @TADescription(annotation = "TD", type = FIELD,
                genericLocation = {3, 0})
    @TestClass("%TEST_CLASS_NAME%$1Nested")
    public String testNestedInMethod1() {
        return "class %TEST_CLASS_NAME% {\n" +
                "  void foobar() {\n" +
                "    class Nested<@TA X extends @TB Object> {\n" +
                "      @TC List<@TD Object> f;\n" +
                "    }\n" +
                "  }" +
                "}";
    }

    @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {}, paramIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {1, 0}, paramIndex = 0)
    public String testParamRepeatableAnnotation1() {
        return "void test(@RTA @RTA Outer.@RTB @RTB Inner a) { }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0}, paramIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 1, 0}, paramIndex = 0)
    public String testParamRepeatableAnnotation1b() {
        return "void test(List<@RTA @RTA Outer.@RTB @RTB Inner> a) { }";
    }

    @TADescription(annotation = "RTBs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0}, paramIndex = 0)
    public String testParamRepeatableAnnotation1g() {
        return "void test(List<java.util.Map. @RTB @RTB Entry> a) { }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {}, paramIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {1, 0}, paramIndex = 0)
    public String testParamRepeatableAnnotation2() {
        return "void test(@RTA @RTA GOuter<String,String>.@RTB @RTB GInner<String,String> a) { }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0}, paramIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 1, 0}, paramIndex = 0)
    public String testParamRepeatableAnnotation2b() {
        return "void test(List<@RTA @RTA GOuter<String,String>.@RTB @RTB GInner<String,String>> a) { }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0}, paramIndex = 0)
    @TADescription(annotation = "RTCs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0}, paramIndex = 0)
    @TADescription(annotation = "RTDs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "RTEs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0}, paramIndex = 0)
    @TADescription(annotation = "RTFs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "RTGs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0}, paramIndex = 0)
    @TADescription(annotation = "RTHs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 0}, paramIndex = 0)
    @TADescription(annotation = "RTIs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 1}, paramIndex = 0)
    @TADescription(annotation = "RTJs", type = METHOD_FORMAL_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "RTKs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {0, 0}, paramIndex = 0)
    public String testParamRepeatableAnnotation3() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " void test(@RTA @RTA Outer . @RTB @RTB GInner<@RTC @RTC List<@RTD @RTD Object" +
                " @RTE @RTE[] @RTF @RTF[]>>. @RTG @RTG GInner2<@RTH @RTH Integer, @RTI @RTI Object>" +
                " @RTJ @RTJ[] @RTK @RTK[] a) { }\n" +
                "}";
    }

    @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0}, paramIndex = 0)
    @TADescription(annotation = "RTCs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0}, paramIndex = 0)
    @TADescription(annotation = "RTDs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "RTEs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0}, paramIndex = 0)
    @TADescription(annotation = "RTFs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "RTGs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0}, paramIndex = 0)
    @TADescription(annotation = "RTHs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 0}, paramIndex = 0)
    @TADescription(annotation = "RTIs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 1}, paramIndex = 0)
    @TADescription(annotation = "RTJs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0}, paramIndex = 0)
    @TADescription(annotation = "RTKs", type = METHOD_FORMAL_PARAMETER,
            genericLocation = {3, 0, 0, 0}, paramIndex = 0)
    public String testParamRepeatableAnnotation4() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " void test(List<@RTA @RTA Outer . @RTB @RTB GInner<@RTC @RTC List<@RTD @RTD Object" +
                " @RTE @RTE[] @RTF @RTF[]>>. @RTG @RTG GInner2<@RTH @RTH Integer," +
                " @RTI @RTI Object> @RTJ @RTJ[] @RTK @RTK[]> a) { }\n" +
                "}";
    }

    // Local variables

    @TADescription(annotation = "RTAs", type = LOCAL_VARIABLE,
            genericLocation = {},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTBs", type = LOCAL_VARIABLE,
            genericLocation = {1, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    public String testLocalRepeatableAnnotation1a() {
        return "void test() { @RTA @RTA Outer.@RTB @RTB Inner a = null; }";
    }

    @TADescription(annotation = "RTAs", type = LOCAL_VARIABLE,
            genericLocation = {},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    public String testLocalRepeatableAnnotation1b() {
        return "void test() { @RTA @RTA Outer.Inner a = null; }";
    }

    @TADescription(annotation = "RTBs", type = LOCAL_VARIABLE,
            genericLocation = {1, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    public String testLocalRepeatableAnnotation1c() {
        return "void test() { Outer.@RTB @RTB Inner a = null; }";
    }

    @TADescription(annotation = "RTAs", type = LOCAL_VARIABLE,
            genericLocation = {},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTBs", type = LOCAL_VARIABLE,
            genericLocation = {1, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    public String testLocalRepeatableAnnotation2() {
        return "void test() { @RTA @RTA GOuter<String,String>.@RTB @RTB GInner<String,String> a = null; }";
    }

    @TADescription(annotation = "RTAs", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTBs", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTCs", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTDs", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTEs", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTFs", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTGs", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTHs", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTIs", type = LOCAL_VARIABLE,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 1},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTJs", type = LOCAL_VARIABLE,
            genericLocation = {},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTKs", type = LOCAL_VARIABLE,
            genericLocation = {0, 0},
            lvarOffset = {5}, lvarLength = {1}, lvarIndex = {1})
    public String testLocalRepeatableAnnotation3() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " void test() { @RTA @RTA Outer . @RTB @RTB GInner<@RTC @RTC List<@RTD @RTD Object" +
                " @RTE @RTE[] @RTF @RTF[]>>. @RTG @RTG GInner2<@RTH @RTH Integer, @RTI @RTI Object>" +
                " @RTJ @RTJ[] @RTK @RTK[] a = null; }\n" +
                "}";
    }


    @TADescription(annotation = "RTAs", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTBs", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTCs", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTDs", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTEs", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTFs", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTGs", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTHs", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTIs", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 1},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTJs", type = LOCAL_VARIABLE,
            genericLocation = {3, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    @TADescription(annotation = "RTKs", type = LOCAL_VARIABLE,
            genericLocation = {3, 0, 0, 0},
            lvarOffset = {2}, lvarLength = {1}, lvarIndex = {1})
    public String testLocalRepeatableAnnotation4() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " void test() { List<@RTA @RTA Outer . @RTB @RTB GInner<@RTC @RTC List<@RTD @RTD" +
                " Object @RTE @RTE [] @RTF @RTF []>>. @RTG @RTG GInner2<@RTH @RTH" +
                " Integer, @RTI @RTI Object> @RTJ @RTJ [] @RTK @RTK []> a = null; }\n" +
                "}";
    }


    // fields

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {})
    @TADescription(annotation = "RTBs", type = FIELD,
            genericLocation = {1, 0})
    public String testFieldRepeatableAnnotation1a() {
        return "@RTA @RTA Outer.@RTB @RTB Inner a;";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {})
    public String testFieldRepeatableAnnotation1b() {
        return "@RTA @RTA Outer.Inner a;";
    }

    @TADescription(annotation = "RTBs", type = FIELD,
            genericLocation = {1, 0})
    public String testFieldRepeatableAnnotation1c() {
        return "Outer.@RTB @RTB Inner a;";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {})
    @TADescription(annotation = "RTBs", type = FIELD,
            genericLocation = {1, 0})
    public String testFieldRepeatableAnnotation2() {
        return "@RTA @RTA GOuter<String,String>.@RTB @RTB GInner<String,String> a;";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {0, 0, 0, 0})
    @TADescription(annotation = "RTBs", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "RTCs", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0})
    @TADescription(annotation = "RTDs", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "RTEs", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0})
    @TADescription(annotation = "RTFs", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0})
    @TADescription(annotation = "RTGs", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0})
    @TADescription(annotation = "RTHs", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 0})
    @TADescription(annotation = "RTIs", type = FIELD,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 1})
    @TADescription(annotation = "RTJs", type = FIELD)
    @TADescription(annotation = "RTKs", type = FIELD,
            genericLocation = {0, 0})
    public String testFieldRepeatableAnnotation3() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " @RTA @RTA Outer . @RTB @RTB GInner<@RTC @RTC List<@RTD @RTD Object @RTE @RTE[] @RTF @RTF[]>>." +
                " @RTG @RTG GInner2<@RTH @RTH Integer, @RTI @RTI Object> @RTJ @RTJ[] @RTK @RTK[] a;\n" +
                "}";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "RTBs", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "RTCs", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0})
    @TADescription(annotation = "RTDs", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "RTEs", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0})
    @TADescription(annotation = "RTFs", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0})
    @TADescription(annotation = "RTGs", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0})
    @TADescription(annotation = "RTHs", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 0})
    @TADescription(annotation = "RTIs", type = FIELD,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 1})
    @TADescription(annotation = "RTJs", type = FIELD,
            genericLocation = {3, 0})
    @TADescription(annotation = "RTKs", type = FIELD,
            genericLocation = {3, 0, 0, 0})
    public String testFieldRepeatableAnnotation4() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " List<@RTA @RTA Outer . @RTB @RTB GInner<@RTC @RTC List<@RTD @RTD Object" +
                " @RTE @RTE[] @RTF @RTF[]>>. @RTG @RTG GInner2<@RTH @RTH Integer," +
                " @RTI @RTI Object> @RTJ @RTJ[] @RTK @RTK[]> a;\n" +
                "}";
    }


    // return types

    @TADescription(annotation = "RTAs", type = METHOD_RETURN,
            genericLocation = {})
    @TADescription(annotation = "RTBs", type = METHOD_RETURN,
            genericLocation = {1, 0})
    public String testReturnRepeatableAnnotation1() {
        return "@RTA @RTA Outer.@RTB @RTB Inner test() { return null; }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN,
            genericLocation = {})
    @TADescription(annotation = "RTBs", type = METHOD_RETURN,
            genericLocation = {1, 0})
    public String testReturnRepeatableAnnotation2() {
        return "@RTA @RTA GOuter<String,String>." +
                "@RTB @RTB GInner<String,String> test() { return null; }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0})
    @TADescription(annotation = "RTBs", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "RTCs", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0})
    @TADescription(annotation = "RTDs", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "RTEs", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0})
    @TADescription(annotation = "RTFs", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0})
    @TADescription(annotation = "RTGs", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0})
    @TADescription(annotation = "RTHs", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 0})
    @TADescription(annotation = "RTIs", type = METHOD_RETURN,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0, 3, 1})
    @TADescription(annotation = "RTJs", type = METHOD_RETURN)
    @TADescription(annotation = "RTKs", type = METHOD_RETURN,
            genericLocation = {0, 0})
    public String testReturnRepeatableAnnotation3() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " @RTA @RTA Outer . @RTB @RTB GInner<@RTC @RTC List<@RTD @RTD Object @RTE @RTE[]" +
                " @RTF @RTF[]>>. @RTG @RTG GInner2<@RTH @RTH Integer," +
                " @RTI @RTI Object> @RTJ @RTJ[] @RTK @RTK[] test() { return null; }\n" +
                "}";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "RTBs", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "RTCs", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0})
    @TADescription(annotation = "RTDs", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "RTEs", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0})
    @TADescription(annotation = "RTFs", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0})
    @TADescription(annotation = "RTGs", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0})
    @TADescription(annotation = "RTHs", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 0})
    @TADescription(annotation = "RTIs", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 1})
    @TADescription(annotation = "RTJs", type = METHOD_RETURN,
            genericLocation = {3, 0})
    @TADescription(annotation = "RTKs", type = METHOD_RETURN,
            genericLocation = {3, 0, 0, 0})
    public String testReturnRepeatableAnnotation4() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " List<@RTA @RTA Outer . @RTB @RTB GInner<@RTC @RTC List<@RTD @RTD Object" +
                " @RTE @RTE[] @RTF @RTF[]>>. @RTG @RTG GInner2<@RTH @RTH Integer," +
                " @RTI @RTI Object> @RTJ @RTJ[] @RTK @RTK[]> test() { return null; }\n" +
                "}";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN,
            genericLocation = {3, 0})
    @TADescription(annotation = "RTBs", type = METHOD_RETURN,
            genericLocation = {3, 0, 3, 0})
    @TADescription(annotation = "RTCs", type = METHOD_RETURN,
            genericLocation = {3, 0, 3, 1})
    @TADescription(annotation = "RTDs", type = METHOD_RETURN,
            genericLocation = {3, 0, 3, 1, 3, 0})
    @TADescription(annotation = "RTEs", type = METHOD_RETURN,
            genericLocation = {3, 0, 1, 0})
    @TADescription(annotation = "RTFs", type = METHOD_RETURN,
            genericLocation = {3, 0, 1, 0, 3, 0})
    @TADescription(annotation = "RTGs", type = METHOD_RETURN,
            genericLocation = {3, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "RTHs", type = METHOD_RETURN,
            genericLocation = {3, 0, 1, 0, 3, 0, 3, 0})
    @TADescription(annotation = "RTIs", type = METHOD_RETURN,
            genericLocation = {3, 0, 1, 0, 3, 0, 3, 0, 0, 0})
    @TADescription(annotation = "RTJs", type = METHOD_RETURN,
            genericLocation = {3, 0, 1, 0, 1, 0})
    public String testReturnRepeatableAnnotation5() {
        return "class GOuter<A, B> {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " List<@RTA @RTA GOuter<@RTB @RTB String, @RTC @RTC List<@RTD @RTD Object>> ." +
                " @RTE @RTE GInner<@RTF @RTF List<@RTG @RTG Object @RTH @RTH[] @RTI @RTI[]>>." +
                " @RTJ @RTJ GInner2<String, String>> test() { return null; }\n" +
                "}";
    }


    // type parameters

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {}, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {1, 0}, paramIndex = 0, boundIndex = 0)
    public String testTypeparamRepeatableAnnotation1() {
        return "<X extends @RTA @RTA Outer.@RTB @RTB Inner> X test() { return null; }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {}, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {1, 0}, paramIndex = 0, boundIndex = 0)
    public String testTypeparamRepeatableAnnotation2() {
        return "<X extends @RTA @RTA GOuter<String,String>.@RTB @RTB GInner<String,String>> X test() { return null; }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {},
            paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {1, 0},
            paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTCs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {1, 0, 3, 0},
            paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTDs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {1, 0, 3, 0, 3, 0, 0, 0, 0, 0},
            paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTEs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {1, 0, 3, 0, 3, 0},
            paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTFs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {1, 0, 3, 0, 3, 0, 0, 0},
            paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTGs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {1, 0, 1, 0},
            paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTHs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {1, 0, 1, 0, 3, 0},
            paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTIs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {1, 0, 1, 0, 3, 1},
            paramIndex = 0, boundIndex = 0)
    public String testTypeparamRepeatableAnnotation3() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " <X extends @RTA @RTA Outer . @RTB @RTB GInner<@RTC @RTC List<@RTD @RTD Object" +
                " @RTE @RTE[] @RTF @RTF[]>>. @RTG @RTG GInner2<@RTH @RTH Integer," +
                " @RTI @RTI Object>> X test() { return null; }\n" +
                "}";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 0, 0, 0, 0},
            paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0},
            paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTCs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0},
            paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTDs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0},
            paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTEs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0},
            paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTFs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 3, 0, 3, 0, 0, 0},
            paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTGs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0},
            paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTHs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 0},
            paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTIs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3, 1},
            paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTJs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0},
            paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTKs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 0, 0},
            paramIndex = 0, boundIndex = 1)
    public String testTypeparamRepeatableAnnotation4() {
        return "class Outer {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " <X extends List<@RTA @RTA Outer . @RTB @RTB GInner<@RTC @RTC List<@RTD @RTD Object" +
                " @RTE @RTE[] @RTF @RTF[]>>. @RTG @RTG GInner2<@RTH @RTH Integer," +
                " @RTI @RTI Object> @RTJ @RTJ[] @RTK @RTK[]>> X test() { return null; }\n" +
                "}";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 3, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTCs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 3, 1}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTDs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 3, 1, 3, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTEs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 1, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTFs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 1, 0, 3, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTGs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTHs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 1, 0, 3, 0, 3, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTIs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 1, 0, 3, 0, 3, 0, 0, 0}, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTJs", type = METHOD_TYPE_PARAMETER_BOUND,
            genericLocation = {3, 0, 1, 0, 1, 0}, paramIndex = 0, boundIndex = 1)
    public String testTypeparamRepeatableAnnotation5() {
        return "class GOuter<A, B> {\n" +
                " class GInner<X> {\n" +
                "  class GInner2<Y, Z> {}\n" +
                "}}\n\n" +
                "class %TEST_CLASS_NAME% {\n" +
                " <X extends List<@RTA @RTA GOuter<@RTB @RTB String, @RTC @RTC List<@RTD @RTD Object>> ." +
                " @RTE @RTE GInner<@RTF @RTF List<@RTG @RTG Object @RTH @RTH[] @RTI @RTI[]>>." +
                " @RTJ @RTJ GInner2<String, String>>> X test() { return null; }\n" +
                "}";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {3, 0, 1, 0})
    public String testUsesRepeatableAnnotation1a() {
        return "class %TEST_CLASS_NAME% { class Inner {}    List<@RTA @RTA Inner> f; }";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {3, 0})
    public String testUsesRepeatableAnnotation1b() {
        return "class %TEST_CLASS_NAME% { class Inner {}    List<@RTA @RTA %TEST_CLASS_NAME%.Inner> f; }";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {3, 0, 1, 0, 1, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUsesRepeatableAnnotation2a() {
        return "class %TEST_CLASS_NAME% { class Inner { class Inner2{}    List<@RTA @RTA Inner2> f; }}";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {3, 0, 1, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUsesRepeatableAnnotation2b() {
        return "class %TEST_CLASS_NAME% { class Inner { class Inner2{}    List<@RTA @RTA Inner.Inner2> f; }}";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {3, 0, 1, 0, 1, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUsesRepeatableAnnotation2c() {
        return "class %TEST_CLASS_NAME% { class Inner { class Inner2{}    List<Inner.@RTA @RTA Inner2> f; }}";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {3, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUsesRepeatableAnnotation2d() {
        return "class %TEST_CLASS_NAME%{ class Inner { class Inner2{}" +
                "    List<@RTA @RTA %TEST_CLASS_NAME%.Inner.Inner2> f; }}";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {3, 0, 1, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUsesRepeatableAnnotation2e() {
        return "class %TEST_CLASS_NAME% { class Inner { class Inner2{}" +
                "    List<%TEST_CLASS_NAME%.@RTA @RTA Inner.Inner2> f; }}";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {3, 0, 1, 0, 1, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUsesRepeatableAnnotation2f() {
        return "class %TEST_CLASS_NAME% { class Inner { class Inner2{}" +
                "    List<%TEST_CLASS_NAME%.Inner.@RTA @RTA Inner2> f; }}";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {3, 0, 1, 0, 1, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUsesRepeatableAnnotation3a() {
        return "class %TEST_CLASS_NAME% { class Inner<A, B> { class Inner2<C, D>{}\n" +
                "    List<%TEST_CLASS_NAME%.Inner.@RTA @RTA Inner2> f; }}";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {3, 0, 1, 0})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUsesRepeatableAnnotation3b() {
        return "class %TEST_CLASS_NAME% { class Inner<A, B> { class Inner2<C, D>{}\n" +
                "    List<%TEST_CLASS_NAME%.@RTA @RTA Inner.Inner2> f; }}";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {})
    @TADescription(annotation = "RTBs", type = FIELD,
            genericLocation = {3, 0})
    public String testUsesRepeatableAnnotation4() {
        return "class %TEST_CLASS_NAME% { static class TInner {}\n" +
                "    @RTA @RTA TInner f; \n" +
                "    List<@RTB @RTB TInner> g; }";
    }

    @TADescription(annotation = "RTAs", type = FIELD,
            genericLocation = {3, 0, 1, 0, 3, 1})
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String testUsesRepeatableAnnotation3c() {
        return "class %TEST_CLASS_NAME% { class Inner<A, B> { class Inner2<C, D>{}\n" +
                "    List<%TEST_CLASS_NAME%.Inner<String," +
                " @RTA @RTA Object>.Inner2<%TEST_CLASS_NAME%, %TEST_CLASS_NAME%>> f; }}";
    }

    @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER, paramIndex=0)
    public String testFullyQualifiedRepeatableAnnotation1() {
        return "void testme(java.security.@RTA @RTA ProtectionDomain protectionDomain) {}";
    }

    @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER, paramIndex=0,
            genericLocation = {3, 0})
    public String testFullyQualifiedRepeatableAnnotation2() {
        return "void testme(List<java.security.@RTA @RTA ProtectionDomain> protectionDomain) {}";
    }

    @TADescription(annotation = "RTAs", type = LOCAL_VARIABLE,
            genericLocation = {},
            lvarOffset = ReferenceInfoUtil.IGNORE_VALUE,
            lvarLength = ReferenceInfoUtil.IGNORE_VALUE,
            lvarIndex = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = LOCAL_VARIABLE,
            genericLocation = {1, 0},
            lvarOffset = ReferenceInfoUtil.IGNORE_VALUE,
            lvarLength = ReferenceInfoUtil.IGNORE_VALUE,
            lvarIndex = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTCs", type = LOCAL_VARIABLE,
            // Only classes count, not methods.
            genericLocation = {1, 0, 1, 0},
            lvarOffset = ReferenceInfoUtil.IGNORE_VALUE,
            lvarLength = ReferenceInfoUtil.IGNORE_VALUE,
            lvarIndex = ReferenceInfoUtil.IGNORE_VALUE)
    @TestClass("Outer$Inner")
    public String testMethodNestingRepeatableAnnotation1() {
        return "class Outer {\n" +
                " class Inner {\n" +
                "  void foo() {\n" +
                "    class MInner {}\n" +
                "    @RTA @RTA Outer . @RTB @RTB Inner l1 = null;\n" +
                "    @RTC @RTC MInner l2 = null;\n" +
                "  }\n" +
                "}}\n";
    }

    @TADescription(annotation = "RTAs", type = NEW,
            genericLocation = {},
            offset = 0)
    @TADescription(annotation = "RTBs", type = NEW,
            genericLocation = {1, 0},
            offset = 0)
    @TADescription(annotation = "RTCs", type = NEW,
            // Only classes count, not methods.
            genericLocation = {1, 0, 1, 0},
            offset = 12)
    @TestClass("Outer$Inner")
    public String testMethodNestingRepeatableAnnotation2() {
        return "class Outer {\n" +
                " class Inner {\n" +
                "  void foo() {\n" +
                "    class MInner {}\n" +
                "    Object o1 = new @RTA @RTA Outer . @RTB @RTB Inner();" +
                "    Object o2 = new @RTC @RTC MInner();\n" +
                "  }\n" +
                "}}\n";
    }

    @TADescription(annotation = "RTAs", type = CLASS_EXTENDS,
            genericLocation = {}, typeIndex = -1)
    @TADescription(annotation = "RTBs", type = CLASS_EXTENDS,
            genericLocation = {3, 0}, typeIndex = -1)
    @TADescription(annotation = "RTCs", type = CLASS_EXTENDS,
            genericLocation = {3, 1}, typeIndex = -1)
    @TADescription(annotation = "RTDs", type = CLASS_EXTENDS,
            genericLocation = {1, 0}, typeIndex = -1)
    @TADescription(annotation = "RTEs", type = CLASS_EXTENDS,
            genericLocation = {1, 0, 3, 0}, typeIndex = -1)
    @TADescription(annotation = "RTFs", type = CLASS_EXTENDS,
            genericLocation = {1, 0, 3, 1}, typeIndex = -1)
    @TestClass("GOuter$GInner$Test")
    public String testExtendsRepeatableAnnotation1() {
        return "class GOuter<A, B> {\n" +
                "  class GInner<X, Y> {\n" +
                "    class Test extends @RTA @RTA GOuter<@RTB @RTB String," +
                " @RTC @RTC String>.@RTD @RTD GInner<@RTE @RTE String, @RTF @RTF String> {}" +
                "  }" +
                "}";
    }

    @TADescription(annotation = "RTAs", type = CLASS_TYPE_PARAMETER,
            genericLocation = {}, paramIndex = 0)
    @TADescription(annotation = "RTBs", type = CLASS_TYPE_PARAMETER_BOUND,
            genericLocation = {}, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTCs", type = FIELD,
            genericLocation = {})
    @TADescription(annotation = "RTDs", type = FIELD,
            genericLocation = {3, 0})
    @TestClass("%TEST_CLASS_NAME%$1Nested")
    public String testNestedInMethodRepeatableAnnotation1() {
        return "class %TEST_CLASS_NAME% {\n" +
                "  void foobar() {\n" +
                "    class Nested<@RTA @RTA X extends @RTB @RTB Object> {\n" +
                "      @RTC @RTC List<@RTD @RTD Object> f;\n" +
                "    }\n" +
                "  }" +
                "}";
    }
}
