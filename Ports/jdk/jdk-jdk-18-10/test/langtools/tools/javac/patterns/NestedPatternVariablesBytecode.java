/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8268748
 * @summary Javac generates error opcodes when using nest pattern variables
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main NestedPatternVariablesBytecode
 */

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.Instruction;

import toolbox.JavacTask;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class NestedPatternVariablesBytecode extends TestRunner {
    private static final String JAVA_VERSION = System.getProperty("java.specification.version");
    private static final String TEST_METHOD = "test";

    ToolBox tb;
    ClassFile cf;

    public NestedPatternVariablesBytecode() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String[] args) throws Exception {
        NestedPatternVariablesBytecode t = new NestedPatternVariablesBytecode();
        t.runTests();
    }

    @Test
    public void testNestedPatternVariablesBytecode() throws Exception {
        String code = """
                class NestedPatterVariablesTest {
                    String test(Object o) {
                        if (o instanceof (CharSequence cs && cs instanceof String s)) {
                            return s;
                        }
                        return null;
                    }
                }""";
        Path curPath = Path.of(".");
        new JavacTask(tb)
                .options("--enable-preview", "-source", JAVA_VERSION)
                .sources(code)
                .outdir(curPath)
                .run();

        cf = ClassFile.read(curPath.resolve("NestedPatterVariablesTest.class"));
        Method testMethod = Arrays.stream(cf.methods)
                                  .filter(m -> isTestMethod(m))
                                  .findAny()
                                  .get();
        Code_attribute code_attribute = (Code_attribute) testMethod.attributes.get(Attribute.Code);

        List<String> actualCode = getCodeInstructions(code_attribute);
        List<String> expectedCode = Arrays.asList(
                "aload_1", "instanceof", "ifeq", "aload_1", "checkcast", "astore_2", "aload_2", "instanceof",
                "ifeq", "aload_2", "checkcast", "astore_3", "aload_3", "areturn", "aconst_null", "areturn");
        tb.checkEqual(expectedCode, actualCode);
    }

    boolean isTestMethod(Method m) {
        try {
            return TEST_METHOD.equals(m.getName(cf.constant_pool));
        } catch (ConstantPoolException e) {
            throw new IllegalStateException(e);
        }
    }

    List<String> getCodeInstructions(Code_attribute code) {
        return StreamSupport.stream(code.getInstructions().spliterator(), false)
                .map(Instruction::getMnemonic)
                .toList();
    }
}
