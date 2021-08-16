/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6504896 8028415
 * @summary TreeMaker.Literal(Object) does not support Booleans
 * @modules jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.tree.TreeMaker;
import static com.sun.tools.javac.code.TypeTag.*;

public class MakeLiteralTest {
    public static void main(String... args) throws Exception {
        new MakeLiteralTest().run();
    }

    void run() throws Exception {
        Context context = new Context();
        JavacFileManager.preRegister(context);
        Symtab syms = Symtab.instance(context);
        maker = TreeMaker.instance(context);
        types = Types.instance(context);

        test("abc",                     CLASS,      syms.stringType,    "abc");
        test(Boolean.FALSE,             BOOLEAN,    syms.booleanType,   Integer.valueOf(0));
        test(Boolean.TRUE,              BOOLEAN,    syms.booleanType,   Integer.valueOf(1));
        test(Byte.valueOf((byte) 1),    BYTE,       syms.byteType,      Byte.valueOf((byte) 1));
        test(Character.valueOf('a'),    CHAR,       syms.charType,      Integer.valueOf('a'));
        test(Double.valueOf(1d),        DOUBLE,     syms.doubleType,    Double.valueOf(1d));
        test(Float.valueOf(1f),         FLOAT,      syms.floatType,     Float.valueOf(1f));
        test(Integer.valueOf(1),        INT,        syms.intType,       Integer.valueOf(1));
        test(Long.valueOf(1),           LONG,       syms.longType,      Long.valueOf(1));
        test(Short.valueOf((short) 1),  SHORT,      syms.shortType,     Short.valueOf((short) 1));

        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    void test(Object value, TypeTag tag, Type type, Object constValue) {
        JCLiteral l = maker.Literal(value);
        if (!l.type.hasTag(tag))
            error("unexpected tag: " + l.getTag() + ": expected: " + tag);
        if (!types.isSameType(l.type, type))
            error("unexpected type: " + l.type + ": expected: " + type);
        if (l.type.constValue().getClass() != constValue.getClass()
                || !constValue.equals(l.type.constValue()))  {
            error("unexpected const value: "
                    + l.type.constValue().getClass() + " " + l.type.constValue()
                    + ": expected:" + constValue.getClass() + " " + constValue);
        }
        if (l.getValue().getClass() != value.getClass()
                || !value.equals(l.getValue()))  {
            error("unexpected const value: "
                    + l.getValue().getClass() + " " + l.type.constValue()
                    + ": expected:" + value.getClass() + " " + value);
        }
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    TreeMaker maker;
    Types types;
    int errors;
}
