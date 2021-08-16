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

/*
 * @test
 * @bug 8222949
 * @summary add condy support to javac's pool API
 * @library /tools/javac/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.jvm
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main TestConstantDynamic
 */

import java.io.IOException;
import java.io.InputStream;

import javax.tools.JavaFileObject;

import com.sun.source.tree.*;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreeScanner;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.BootstrapMethods_attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool.*;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.LineNumberTable_attribute;
import com.sun.tools.classfile.Method;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.jvm.PoolConstant.LoadableConstant;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.List;

import combo.ComboParameter;
import combo.ComboTestHelper;
import combo.ComboInstance;
import combo.ComboTask.Result;

public class TestConstantDynamic extends ComboInstance<TestConstantDynamic> {

    enum ConstantType implements ComboParameter {
        STRING("String", "Ljava/lang/String;"),
        CLASS("Class<?>", "Ljava/lang/Class;"),
        INTEGER("int", "I"),
        LONG("long", "J"),
        FLOAT("float", "F"),
        DOUBLE("double", "D"),
        METHOD_HANDLE("MethodHandle", "Ljava/lang/invoke/MethodHandle;"),
        METHOD_TYPE("MethodType", "Ljava/lang/invoke/MethodType;");

        String sourceTypeStr;
        String bytecodeTypeStr;

        ConstantType(String sourceTypeStr, String bytecodeTypeStr) {
            this.sourceTypeStr = sourceTypeStr;
            this.bytecodeTypeStr = bytecodeTypeStr;
        }

        @Override
        public String expand(String optParameter) {
            return sourceTypeStr;
        }
    }

    enum Value implements ComboParameter {
        STRING("\"Hello!\""),
        CLASS("null"),
        INTEGER("1"),
        LONG("1L"),
        FLOAT("1.0f"),
        DOUBLE("1.0"),
        METHOD_HANDLE("null"),
        METHOD_TYPE("null");

        String value;

        Value(String value) {
            this.value = value;
        }

        @Override
        public String expand(String optParameter) {
            return value;
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<TestConstantDynamic>()
                .withFilter(TestConstantDynamic::redundantTestFilter)
                .withDimension("TYPE", (x, type) -> x.type = type, ConstantType.values())
                .withDimension("VALUE", (x, value) -> x.value = value, Value.values())
                .run(TestConstantDynamic::new);
    }

    ConstantType type;
    Value value;

    boolean redundantTestFilter() {
        return type.name().equals(value.name());
    }

    final String source_template =
                "import java.lang.invoke.*;\n" +
                "import java.lang.invoke.MethodHandles.*;\n" +
                "class Test {\n" +
                "    static final #{TYPE} f = #{VALUE};\n" +

                "    static #{TYPE} bsm(MethodHandles.Lookup lookup, String name, Class<?> type) {\n" +
                "        return f;\n" +
                "    }\n" +

                "    static void test() {\n" +
                "        #{TYPE} i = f;\n" +
                "    }\n" +
                "}";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withOption("-g")
                .withSourceFromTemplate(source_template)
                .withListenerFactory(context -> {
                        Symtab syms = Symtab.instance(context);
                        Names names = Names.instance(context);
                        Types types = Types.instance(context);
                        return new Condifier(syms, names, types);
                    })
                .generate(this::verifyBytecode);
    }

    void verifyBytecode(Result<Iterable<? extends JavaFileObject>> res) {
        if (res.hasErrors()) {
            fail("Diags found when compiling instance: " + res.compilationInfo());
            return;
        }
        try (InputStream is = res.get().iterator().next().openInputStream()){
            ClassFile cf = ClassFile.read(is);
            Method testMethod = null;
            for (Method m : cf.methods) {
                if (m.getName(cf.constant_pool).equals("test")) {
                    testMethod = m;
                    break;
                }
            }
            if (testMethod == null) {
                fail("Test method not found");
                return;
            }
            Code_attribute ea =
                    (Code_attribute)testMethod.attributes.get(Attribute.Code);
            if (testMethod == null) {
                fail("Code attribute for test() method not found");
                return;
            }

            int bsmIdx = -1;

            for (Instruction i : ea.getInstructions()) {
                if (i.getMnemonic().equals("ldc")) {
                    CONSTANT_Dynamic_info condyInfo = (CONSTANT_Dynamic_info)cf.constant_pool.get(i.getByte(1));
                    bsmIdx = condyInfo.bootstrap_method_attr_index;
                    System.out.println("condyInfo.getNameAndTypeInfo().getType() " + condyInfo.getNameAndTypeInfo().getType());
                    if (!condyInfo.getNameAndTypeInfo().getType().equals(type.bytecodeTypeStr)) {
                        fail("type mismatch for CONSTANT_Dynamic_info");
                        return;
                    }
                }
            }


            if (bsmIdx == -1) {
                fail("Missing constantdynamic in generated code");
                return;
            }

            BootstrapMethods_attribute bsm_attr =
                    (BootstrapMethods_attribute)cf
                    .getAttribute(Attribute.BootstrapMethods);
            if (bsm_attr.bootstrap_method_specifiers.length != 1) {
                fail("Bad number of method specifiers " +
                        "in BootstrapMethods attribute");
                return;
            }
            BootstrapMethods_attribute.BootstrapMethodSpecifier bsm_spec =
                    bsm_attr.bootstrap_method_specifiers[0];

            CONSTANT_MethodHandle_info bsm_handle =
                    (CONSTANT_MethodHandle_info)cf.constant_pool
                    .get(bsm_spec.bootstrap_method_ref);

            if (bsm_handle.reference_kind != RefKind.REF_invokeStatic) {
                fail("Bad kind on boostrap method handle");
                return;
            }

            CONSTANT_Methodref_info bsm_ref =
                    (CONSTANT_Methodref_info)cf.constant_pool
                    .get(bsm_handle.reference_index);

            if (!bsm_ref.getClassInfo().getName().equals("Test")) {
                fail("Bad owner of boostrap method");
                return;
            }

            if (!bsm_ref.getNameAndTypeInfo().getName().equals("bsm")) {
                fail("Bad boostrap method name");
                return;
            }

            if (!bsm_ref.getNameAndTypeInfo()
                    .getType().equals(asBSMSignatureString())) {
                fail("Bad boostrap method type" +
                        bsm_ref.getNameAndTypeInfo().getType() + " " +
                        asBSMSignatureString());
                return;
            }

            LineNumberTable_attribute lnt =
                    (LineNumberTable_attribute)ea.attributes.get(Attribute.LineNumberTable);

            if (lnt == null) {
                fail("No LineNumberTable attribute");
                return;
            }
            if (lnt.line_number_table_length != 2) {
                fail("Wrong number of entries in LineNumberTable");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("error reading classfile: " + res.compilationInfo());
            return;
        }
    }

    String asBSMSignatureString() {
        StringBuilder buf = new StringBuilder();
        buf.append("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;");
        buf.append(")" + type.bytecodeTypeStr);
        return buf.toString();
    }

    class Condifier extends TreeScanner<Void, Void> implements TaskListener {

        MethodHandleSymbol bsm;
        Symtab syms;
        Names names;
        Types types;

        Condifier(Symtab syms, Names names, Types types) {
            this.syms = syms;
            this.names = names;
            this.types = types;
        }

        @Override
        public void started(TaskEvent e) {
            //do nothing
        }

        @Override
        public void finished(TaskEvent e) {
            if (e.getKind() == TaskEvent.Kind.ANALYZE) {
                scan(e.getCompilationUnit(), null);
            }
        }

        @Override
        public Void visitVariable(VariableTree node, Void p) {
            super.visitVariable(node, p);
            JCVariableDecl tree = (JCVariableDecl)node;
            VarSymbol v = tree.sym;
            if (tree.init != null && v.name.toString().equals("i")) {
                List<Type> bsm_staticArgs = List.of(syms.methodHandleLookupType,
                        syms.stringType,
                        syms.classType);
                Name bsmName = names.fromString("bsm");
                Symbol.DynamicVarSymbol dynSym = new Symbol.DynamicVarSymbol(bsmName,
                        syms.noSymbol,
                        bsm,
                        v.type,
                        new LoadableConstant[0]);
                ((JCIdent)tree.init).sym = dynSym;
                ((JCIdent)tree.init).name = bsmName;
            }
            return null;
        }

        @Override
        public Void visitMethod(MethodTree node, Void p) {
            super.visitMethod(node, p);
            if (node.getName().toString().equals("bsm")) {
                bsm = ((JCMethodDecl)node).sym.asHandle();
            }
            return null;
        }
    }
}
