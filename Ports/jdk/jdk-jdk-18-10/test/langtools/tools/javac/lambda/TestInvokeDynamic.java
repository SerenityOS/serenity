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

/*
 * @test
 * @bug 7194586 8003280 8006694 8010404 8129962
 * @summary Add lambda tests
 *  Add back-end support for invokedynamic
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.jvm
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main TestInvokeDynamic
 */

import java.io.IOException;
import java.io.InputStream;

import javax.tools.JavaFileObject;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
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
import com.sun.tools.javac.code.Symbol.MethodHandleSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.jvm.PoolConstant.LoadableConstant;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.util.Names;

import combo.ComboParameter;
import combo.ComboTestHelper;
import combo.ComboInstance;
import combo.ComboTask.Result;

public class TestInvokeDynamic extends ComboInstance<TestInvokeDynamic> {

    enum StaticArgumentKind implements ComboParameter {
        STRING("Hello!", "String", "Ljava/lang/String;") {
            @Override
            boolean check(CPInfo cpInfo) throws Exception {
                return (cpInfo instanceof CONSTANT_String_info) &&
                        ((CONSTANT_String_info)cpInfo).getString()
                        .equals(value);
            }
        },
        CLASS(null, "Class<?>", "Ljava/lang/Class;") {
            @Override
            boolean check(CPInfo cpInfo) throws Exception {
                return (cpInfo instanceof CONSTANT_Class_info) &&
                        ((CONSTANT_Class_info)cpInfo).getName()
                        .equals("java/lang/String");
            }
        },
        INTEGER(1, "int", "I") {
            @Override
            boolean check(CPInfo cpInfo) throws Exception {
                return (cpInfo instanceof CONSTANT_Integer_info) &&
                        ((CONSTANT_Integer_info)cpInfo).value ==
                        ((Integer)value).intValue();
            }
        },
        LONG(1L, "long", "J") {
            @Override
            boolean check(CPInfo cpInfo) throws Exception {
                return (cpInfo instanceof CONSTANT_Long_info) &&
                        ((CONSTANT_Long_info)cpInfo).value ==
                        ((Long)value).longValue();
            }
        },
        FLOAT(1.0f, "float", "F") {
            @Override
            boolean check(CPInfo cpInfo) throws Exception {
                return (cpInfo instanceof CONSTANT_Float_info) &&
                        ((CONSTANT_Float_info)cpInfo).value ==
                        ((Float)value).floatValue();
            }
        },
        DOUBLE(1.0, "double","D") {
            @Override
            boolean check(CPInfo cpInfo) throws Exception {
                return (cpInfo instanceof CONSTANT_Double_info) &&
                        ((CONSTANT_Double_info)cpInfo).value ==
                        ((Double)value).doubleValue();
            }
        },
        METHOD_HANDLE(null, "MethodHandle", "Ljava/lang/invoke/MethodHandle;") {
            @Override
            boolean check(CPInfo cpInfo) throws Exception {
                if (!(cpInfo instanceof CONSTANT_MethodHandle_info))
                    return false;
                CONSTANT_MethodHandle_info handleInfo =
                        (CONSTANT_MethodHandle_info)cpInfo;
                return handleInfo.getCPRefInfo().getClassName().equals("Array") &&
                        handleInfo.reference_kind == RefKind.REF_invokeVirtual &&
                        handleInfo.getCPRefInfo()
                        .getNameAndTypeInfo().getName().equals("clone") &&
                        handleInfo.getCPRefInfo()
                        .getNameAndTypeInfo().getType().equals("()Ljava/lang/Object;");
            }
        },
        METHOD_TYPE(null, "MethodType", "Ljava/lang/invoke/MethodType;") {
            @Override
            boolean check(CPInfo cpInfo) throws Exception {
                return (cpInfo instanceof CONSTANT_MethodType_info) &&
                        ((CONSTANT_MethodType_info)cpInfo).getType()
                        .equals("()Ljava/lang/Object;");
            }
        };

        Object value;
        String sourceTypeStr;
        String bytecodeTypeStr;

        StaticArgumentKind(Object value, String sourceTypeStr,
                String bytecodeTypeStr) {
            this.value = value;
            this.sourceTypeStr = sourceTypeStr;
            this.bytecodeTypeStr = bytecodeTypeStr;
        }

        abstract boolean check(CPInfo cpInfo) throws Exception;

        LoadableConstant getValue(Symtab syms) {
            switch (this) {
                case STRING:
                    return LoadableConstant.String((String)value);
                case INTEGER:
                    return LoadableConstant.Int((Integer)value);
                case LONG:
                    return LoadableConstant.Long((Long)value);
                case FLOAT:
                    return LoadableConstant.Float((Float)value);
                case DOUBLE:
                    return LoadableConstant.Double((Double)value);
                case CLASS:
                    return (ClassType)syms.stringType;
                case METHOD_HANDLE:
                    return syms.arrayCloneMethod.asHandle();
                case METHOD_TYPE:
                    return ((MethodType)syms.arrayCloneMethod.type);
                default:
                    throw new AssertionError();
            }
        }

        @Override
        public String expand(String optParameter) {
            return sourceTypeStr;
        }
    }

    enum StaticArgumentsArity implements ComboParameter {
        ZERO(0, ""),
        ONE(1, ",#{SARG[0]} s1"),
        TWO(2, ",#{SARG[0]} s1, #{SARG[1]} s2"),
        THREE(3, ",#{SARG[0]} s1, #{SARG[1]} s2, #{SARG[2]} s3");

        int arity;
        String argsTemplate;

        StaticArgumentsArity(int arity, String argsTemplate) {
            this.arity = arity;
            this.argsTemplate = argsTemplate;
        }

        @Override
        public String expand(String optParameter) {
            return argsTemplate;
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<TestInvokeDynamic>()
                .withFilter(TestInvokeDynamic::redundantTestFilter)
                .withDimension("SARGS", (x, arity) -> x.arity = arity, StaticArgumentsArity.values())
                .withArrayDimension("SARG", (x, arg, idx) -> x.saks[idx] = arg, 3, StaticArgumentKind.values())
                .run(TestInvokeDynamic::new);
    }

    StaticArgumentsArity arity;
    StaticArgumentKind[] saks = new StaticArgumentKind[3];

    boolean redundantTestFilter() {
        for (int i = arity.arity ; i < saks.length ; i++) {
            if (saks[i].ordinal() != 0) {
                return false;
            }
        }
        return true;
    }

    final String source_template =
                "import java.lang.invoke.*;\n" +
                "class Test {\n" +
                "   void m() { }\n" +
                "   void test() {\n" +
                "      Object o = this; // marker statement \n" +
                "      m();\n" +
                "   }\n" +
                "}\n" +
                "class Bootstrap {\n" +
                "   public static CallSite bsm(MethodHandles.Lookup lookup, " +
                "String name, MethodType methodType #{SARGS}) {\n" +
                "       return null;\n" +
                "   }\n" +
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
                        return new Indifier(syms, names, types);
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
                if (i.getMnemonic().equals("invokedynamic")) {
                    CONSTANT_InvokeDynamic_info indyInfo =
                         (CONSTANT_InvokeDynamic_info)cf
                            .constant_pool.get(i.getShort(1));
                    bsmIdx = indyInfo.bootstrap_method_attr_index;
                    if (!indyInfo.getNameAndTypeInfo().getType().equals("()V")) {
                        fail("type mismatch for CONSTANT_InvokeDynamic_info");
                        return;
                    }
                }
            }
            if (bsmIdx == -1) {
                fail("Missing invokedynamic in generated code");
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

            if (bsm_spec.bootstrap_arguments.length != arity.arity) {
                fail("Bad number of static invokedynamic args " +
                        "in BootstrapMethod attribute");
                return;
            }

            for (int i = 0 ; i < arity.arity ; i++) {
                if (!saks[i].check(cf.constant_pool
                        .get(bsm_spec.bootstrap_arguments[i]))) {
                    fail("Bad static argument value " + saks[i]);
                    return;
                }
            }

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

            if (!bsm_ref.getClassInfo().getName().equals("Bootstrap")) {
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
            if (lnt.line_number_table_length != 3) {
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
        buf.append("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;");
        for (int i = 0 ; i < arity.arity ; i++) {
            buf.append(saks[i].bytecodeTypeStr);
        }
        buf.append(")Ljava/lang/invoke/CallSite;");
        return buf.toString();
    }

    class Indifier extends TreeScanner<Void, Void> implements TaskListener {

        MethodHandleSymbol bsm;
        Symtab syms;
        Names names;
        Types types;

        Indifier(Symtab syms, Names names, Types types) {
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
        public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
            super.visitMethodInvocation(node, p);
            JCMethodInvocation apply = (JCMethodInvocation)node;
            JCIdent ident = (JCIdent)apply.meth;
            Symbol oldSym = ident.sym;
            if (!oldSym.isConstructor()) {
                LoadableConstant[] staticArgs = new LoadableConstant[arity.arity];
                for (int i = 0; i < arity.arity ; i++) {
                    staticArgs[i] = saks[i].getValue(syms);
                }
                ident.sym = new Symbol.DynamicMethodSymbol(oldSym.name,
                        oldSym.owner, bsm, oldSym.type, staticArgs);
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
