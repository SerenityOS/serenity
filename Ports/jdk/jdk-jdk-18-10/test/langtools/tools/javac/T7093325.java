/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7093325 8006694 8129962
 * @summary Redundant entry in bytecode exception table
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main T7093325
 */

import java.io.IOException;
import java.io.InputStream;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Method;

import javax.tools.JavaFileObject;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

public class T7093325 extends ComboInstance<T7093325> {

    enum StatementKind implements ComboParameter {
        NONE(null, false, false),
        THROW("throw new RuntimeException();", false, false),
        RETURN_NONEMPTY("System.out.println(); return;", true, false),
        RETURN_EMPTY("return;", true, true),
        APPLY("System.out.println();", true, false);

        String stmt;
        boolean canInline;
        boolean empty;

        StatementKind(String stmt, boolean canInline, boolean empty) {
            this.stmt = stmt;
            this.canInline = canInline;
            this.empty = empty;
        }

        @Override
        public String expand(String optParameter) {
            return stmt;
        }
    }

    enum CatchArity implements ComboParameter {
        NONE(""),
        ONE("catch (A a) { #{STMT[1]} }"),
        TWO("catch (B b) { #{STMT[2]} }"),
        THREE("catch (C c) { #{STMT[3]} }"),
        FOUR("catch (D d) { #{STMT[4]} }");

        String catchStr;

        CatchArity(String catchStr) {
            this.catchStr = catchStr;
        }

        @Override
        public String expand(String optParameter) {
            if (this.ordinal() == 0) {
                return catchStr;
            } else {
                return CatchArity.values()[this.ordinal() - 1].expand(optParameter) +
                        catchStr;
            }
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<T7093325>()
                .withFilter(T7093325::testFilter)
                .withDimension("CATCH", (x, ca) -> x.ca = ca, CatchArity.values())
                .withArrayDimension("STMT", (x, stmt, idx) -> x.stmts[idx] = stmt, 5, StatementKind.values())
                .run(T7093325::new);
    }

    /** instance decls **/

    CatchArity ca;
    StatementKind[] stmts = new StatementKind[5];

    boolean testFilter() {
        int lastPos = ca.ordinal() + 1;
        for (int i = 0; i < stmts.length ; i++) {
            boolean shouldBeSet = i < lastPos;
            boolean isSet = stmts[i] != StatementKind.NONE;
            if (shouldBeSet != isSet) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withSourceFromTemplate(source_template)
                .generate(this::verifyBytecode);
    }

    void verifyBytecode(Result<Iterable<? extends JavaFileObject>> result) {
        boolean lastInlined = false;
        boolean hasCode = false;
        int gapsCount = 0;
        for (int i = 0; i < ca.ordinal() + 1 ; i++) {
            lastInlined = stmts[i].canInline;
            hasCode = hasCode || !stmts[i].empty;
            if (lastInlined && hasCode) {
                hasCode = false;
                gapsCount++;
            }
        }
        if (!lastInlined) {
            gapsCount++;
        }

        try (InputStream is = result.get().iterator().next().openInputStream()) {
            ClassFile cf = ClassFile.read(is);
            if (cf == null) {
                fail("Classfile not found: " + result.compilationInfo());
                return;
            }

            Method test_method = null;
            for (Method m : cf.methods) {
                if (m.getName(cf.constant_pool).equals("test")) {
                    test_method = m;
                    break;
                }
            }

            if (test_method == null) {
                fail("Method test() not found in class Test" + result.compilationInfo());
                return;
            }

            Code_attribute code = null;
            for (Attribute a : test_method.attributes) {
                if (a.getName(cf.constant_pool).equals(Attribute.Code)) {
                    code = (Code_attribute)a;
                    break;
                }
            }

            if (code == null) {
                fail("Code attribute not found in method test()");
                return;
            }

            int actualGapsCount = 0;
            for (int i = 0; i < code.exception_table_length ; i++) {
                int catchType = code.exception_table[i].catch_type;
                if (catchType == 0) { //any
                    actualGapsCount++;
                }
            }

            if (actualGapsCount != gapsCount) {
                fail("Bad exception table for test()\n" +
                            "expected gaps: " + gapsCount + "\n" +
                            "found gaps: " + actualGapsCount + "\n" +
                        result.compilationInfo());
                return;
            }
        } catch (IOException | ConstantPoolException e) {
            e.printStackTrace();
            fail("error reading classfile: " + e);
        }

    }

    static final String source_template =
                "class Test {\n" +
                "   void test() {\n" +
                "   try { #{STMT[0]} } #{CATCH} finally { System.out.println(); }\n" +
                "   }\n" +
                "}\n" +
                "class A extends RuntimeException {} \n" +
                "class B extends RuntimeException {} \n" +
                "class C extends RuntimeException {} \n" +
                "class D extends RuntimeException {} \n" +
                "class E extends RuntimeException {}";
}
