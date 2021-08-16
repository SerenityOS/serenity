/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8011738
 * @author  sogoel
 * @summary Code translation test for Lambda expressions, method references
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main ByteCodeTest
 */

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.BootstrapMethods_attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.ConstantPool.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ByteCodeTest {

    static boolean IS_DEBUG = false;
    public static void main(String[] args) {
        File classFile = null;
        int err = 0;
        boolean verifyResult = false;
        for(TestCases tc : TestCases.values()) {
            classFile = getCompiledFile(tc.name(), tc.srcCode);
            if(classFile == null) { // either testFile or classFile was not created
               err++;
            } else {
                verifyResult = verifyClassFileAttributes(classFile, tc);
                if(!verifyResult)
                    System.out.println("Bootstrap class file attributes did not match for " + tc.name());
            }
        }
        if(err > 0)
            throw new RuntimeException("Found " + err + " found");
        else
            System.out.println("Test passed");
    }

    private static boolean verifyClassFileAttributes(File classFile, TestCases tc) {
        ClassFile c = null;
        try {
            c = ClassFile.read(classFile);
        } catch (IOException | ConstantPoolException e) {
            e.printStackTrace();
        }
        ConstantPoolVisitor cpv = new ConstantPoolVisitor(c, c.constant_pool.size());
        Map<Integer, String> hm = cpv.getBSMMap();

        List<String> expectedValList = tc.getExpectedArgValues();
        expectedValList.add(tc.bsmSpecifier.specifier);
        if(!(hm.values().containsAll(new HashSet<String>(expectedValList)))) {
            System.out.println("Values do not match");
            return false;
        }
        return true;
    }

    private static File getCompiledFile(String fname, String srcString) {
        File testFile = null, classFile = null;
        boolean isTestFileCreated = true;

        try {
            testFile = writeTestFile(fname+".java", srcString);
        } catch(IOException ioe) {
            isTestFileCreated = false;
            System.err.println("fail to write" + ioe);
        }

        if(isTestFileCreated) {
            try {
                classFile = compile(testFile);
            } catch (Error err) {
                System.err.println("fail compile. Source:\n" + srcString);
                throw err;
            }
        }
        return classFile;
    }

    static File writeTestFile(String fname, String source) throws IOException {
        File f = new File(fname);
          PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
          out.println(source);
          out.close();
          return f;
    }

    static File compile(File f) {
        int rc = com.sun.tools.javac.Main.compile(new String[] {
                "-g", f.getPath() });
        if (rc != 0)
                throw new Error("compilation failed. rc=" + rc);
            String path = f.getPath();
            return new File(path.substring(0, path.length() - 5) + ".class");
    }

    static void debugln(String str) {
        if(IS_DEBUG)
            System.out.println(str);
    }

    enum BSMSpecifier {
        SPECIFIER1("REF_invokeStatic java/lang/invoke/LambdaMetafactory metaFactory " +
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
                "Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)" +
                "Ljava/lang/invoke/CallSite;"),
        SPECIFIER2("REF_invokeStatic java/lang/invoke/LambdaMetafactory altMetaFactory " +
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
                        "[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;");

        String specifier;
        private BSMSpecifier(String specifier) {
            this.specifier = specifier;
        }
    }

    enum TestCases {
        // Single line lambda expression
        TC1("class TC1 {\n" +
            "    public static void main(String[] args) {\n" +
            "        Object o = (Runnable) () -> { System.out.println(\"hi\");};\n" +
            "    }\n"+
            "}", BSMSpecifier.SPECIFIER1) {

            @Override
            List<String> getExpectedArgValues() {
                List<String> valList = new ArrayList<>();
                valList.add("REF_invokeInterface java/lang/Runnable run ()V");
                valList.add("REF_invokeStatic TC1 lambda$0 ()V");
                valList.add("()V");
                return valList;
            }
        },

        // Lambda expression in a for loop
        TC2("import java.util.*;\n" +
            "public class TC2 {\n" +
            "    void TC2_test() {\n" +
            "        List<String> list = new ArrayList<>();\n" +
            "        list.add(\"A\");\n" +
            "        list.add(\"B\");\n" +
            "        list.stream().forEach( s -> { System.out.println(s); } );\n" +
            "    }\n" +
            "    public static void main(String[] args) {\n" +
            "        new TC2().TC2_test();\n" +
            "    }\n" +
            "}", BSMSpecifier.SPECIFIER1) {

            @Override
            List<String> getExpectedArgValues() {
                List<String> valList = new ArrayList<>();
                valList.add("REF_invokeInterface java/util/function/Consumer accept (Ljava/lang/Object;)V");
                valList.add("REF_invokeStatic TC2 lambda$0 (Ljava/lang/String;)V");
                valList.add("(Ljava/lang/String;)V");
                return valList;
            }
        },

        // Lambda initializer
        TC3("class TC3 {\n" +
            "    interface SAM {\n" +
            "        void m(int i);\n" +
            "    }\n" +
            "    SAM lambda_03 = (int pos) -> { };\n" +
            "}", BSMSpecifier.SPECIFIER1) {

            @Override
            List<String> getExpectedArgValues() {
                List<String> valList = new ArrayList<>();
                valList.add("REF_invokeInterface TC3$SAM m (I)V");
                valList.add("REF_invokeStatic TC3 lambda$0 (I)V");
                valList.add("(I)V");
                return valList;
            }
        },

        // Array initializer
        TC4("class TC4 {\n" +
            "    interface Block<T> {\n" +
            "        void m(T t);\n" +
            "     }\n" +
            "     void test1() {\n" +
            "         Block<?>[] arr1 =  { t -> { }, t -> { } };\n" +
            "     }\n" +
            "}", BSMSpecifier.SPECIFIER1) {

            @Override
            List<String> getExpectedArgValues() {
                List<String> valList = new ArrayList<>();
                valList.add("REF_invokeInterface TC4$Block m (Ljava/lang/Object;)V");
                valList.add("REF_invokeStatic TC4 lambda$0 (Ljava/lang/Object;)V");
                valList.add("(Ljava/lang/Object;)V");
                valList.add("REF_invokeStatic TC4 lambda$1 (Ljava/lang/Object;)V");
                return valList;
            }
        },

        //Lambda expression as a method arg
        TC5("class TC5 {\n"+
            "    interface MapFun<T,R> {  R m( T n); }\n" +
            "    void meth( MapFun<String,Integer> mf ) {\n" +
            "        assert( mf.m(\"four\") == 4);\n" +
            "    }\n"+
            "    void test(Integer i) {\n" +
            "        meth(s -> { Integer len = s.length(); return len; } );\n" +
            "    }\n"+
            "}", BSMSpecifier.SPECIFIER1) {

            @Override
            List<String> getExpectedArgValues() {
                List<String> valList = new ArrayList<>();
                valList.add("REF_invokeInterface TC5$MapFun m (Ljava/lang/Object;)Ljava/lang/Object;");
                valList.add("REF_invokeStatic TC5 lambda$0 (Ljava/lang/String;)Ljava/lang/Integer;");
                valList.add("(Ljava/lang/String;)Ljava/lang/Integer;");
                return valList;
            }
        },

        //Inner class of Lambda expression
        TC6("class TC6 {\n" +
            "    interface MapFun<T, R> {  R m( T n); }\n" +
            "    MapFun<Class<?>,String> cs;\n" +
            "    void test() {\n" +
            "        cs = c -> {\n" +
            "                 class innerClass   {\n" +
            "                    Class<?> icc;\n" +
            "                    innerClass(Class<?> _c) { icc = _c; }\n" +
            "                    String getString() { return icc.toString(); }\n" +
            "                  }\n" +
            "             return new innerClass(c).getString();\n"+
            "             };\n" +
            "    }\n" +
            "}\n", BSMSpecifier.SPECIFIER1) {

            @Override
            List<String> getExpectedArgValues() {
                List<String> valList = new ArrayList<>();
                valList.add("REF_invokeInterface TC6$MapFun m (Ljava/lang/Object;)Ljava/lang/Object;");
                valList.add("REF_invokeSpecial TC6 lambda$0 (Ljava/lang/Class;)Ljava/lang/String;");
                valList.add("(Ljava/lang/Class;)Ljava/lang/String;");
                return valList;
            }
        },

        // Method reference
        TC7("class TC7 {\n" +
            "    static interface SAM {\n" +
            "       void m(Integer i);\n" +
            "    }\n" +
            "    void m(Integer i) {}\n" +
            "       SAM s = this::m;\n" +
            "}\n", BSMSpecifier.SPECIFIER1) {

            @Override
            List<String> getExpectedArgValues() {
                List<String> valList = new ArrayList<>();
                valList.add("REF_invokeInterface TC7$SAM m (Ljava/lang/Integer;)V");
                valList.add("REF_invokeVirtual TC7 m (Ljava/lang/Integer;)V");
                valList.add("(Ljava/lang/Integer;)V");
                return valList;
            }
        },

        // Constructor reference
        TC8("public class TC8 {\n" +
            "    static interface A {Fee<String> m();}\n" +
            "    static class Fee<T> {\n" +
            "        private T t;\n" +
            "        public Fee() {}\n" +
            "    }\n" +
            "    public static void main(String[] args) {\n" +
            "        A a = Fee<String>::new; \n" +
            "    }\n" +
            "}\n", BSMSpecifier.SPECIFIER1) {

            @Override
            List<String> getExpectedArgValues() {
                List<String> valList = new ArrayList<>();
                valList.add("REF_invokeInterface TC8$A m ()LTC8$Fee;");
                valList.add("REF_newInvokeSpecial TC8$Fee <init> ()V");
                valList.add("()LTC8$Fee;");
                return valList;
            }
        },

        // Recursive lambda expression
        TC9("class TC9 {\n" +
            "    interface Recursive<T, R> { T apply(R n); };\n" +
            "    Recursive<Integer,Integer> factorial;\n" +
            "    void test(Integer j) {\n" +
            "        factorial = i -> { return i == 0 ? 1 : i * factorial.apply( i - 1 ); };\n" +
            "    }\n" +
            "}\n", BSMSpecifier.SPECIFIER1) {

            @Override
            List<String> getExpectedArgValues() {
                List<String> valList = new ArrayList<>();
                valList.add("REF_invokeInterface TC9$Recursive apply (Ljava/lang/Object;)Ljava/lang/Object;");
                valList.add("REF_invokeSpecial TC9 lambda$0 (Ljava/lang/Integer;)Ljava/lang/Integer;");
                valList.add("(Ljava/lang/Integer;)Ljava/lang/Integer;");
                return valList;
            }
        },

        //Serializable Lambda
        TC10("import java.io.Serializable;\n" +
              "class TC10 {\n" +
              "    interface Foo { int m(); }\n" +
              "    public static void main(String[] args) {\n" +
              "        Foo f1 = (Foo & Serializable)() -> 3;\n" +
              "    }\n" +
              "}\n", BSMSpecifier.SPECIFIER2) {

            @Override
            List<String> getExpectedArgValues() {
                List<String> valList = new ArrayList<>();
                valList.add("REF_invokeStatic java/lang/invoke/LambdaMetafactory altMetaFactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;");
                valList.add("REF_invokeInterface TC10$Foo m ()I");
                valList.add("REF_invokeStatic TC10 lambda$main$3231c38a$0 ()I");
                valList.add("()I");
                valList.add("1");
                return valList;
            }
        };

        String srcCode;
        BSMSpecifier bsmSpecifier;

        TestCases(String src, BSMSpecifier bsmSpecifier) {
            this.srcCode = src;
            // By default, all test cases will have bootstrap method specifier as Lambda.MetaFactory
            // For serializable lambda test cases, bootstrap method specifier changed to altMetaFactory
            this.bsmSpecifier = bsmSpecifier;
        }

        List<String> getExpectedArgValues() {
            return null;
        }

        void setSrcCode(String src) {
            srcCode = src;
        }
    }

    static class ConstantPoolVisitor implements ConstantPool.Visitor<String, Integer> {
        final List<String> slist;
        final ClassFile cf;
        final ConstantPool cfpool;
        final Map<Integer, String> bsmMap;


        public ConstantPoolVisitor(ClassFile cf, int size) {
            slist = new ArrayList<>(size);
            for (int i = 0 ; i < size; i++) {
                slist.add(null);
            }
            this.cf = cf;
            this.cfpool = cf.constant_pool;
            bsmMap = readBSM();
        }

        public Map<Integer, String> getBSMMap() {
            return Collections.unmodifiableMap(bsmMap);
        }

        public String visit(CPInfo c, int index) {
            return c.accept(this, index);
        }

        private Map<Integer, String> readBSM() {
            BootstrapMethods_attribute bsmAttr =
                    (BootstrapMethods_attribute) cf.getAttribute(Attribute.BootstrapMethods);
            if (bsmAttr != null) {
                Map<Integer, String> out =
                        new HashMap<>(bsmAttr.bootstrap_method_specifiers.length);
                for (BootstrapMethods_attribute.BootstrapMethodSpecifier bsms :
                        bsmAttr.bootstrap_method_specifiers) {
                    int index = bsms.bootstrap_method_ref;
                    try {
                        String value = slist.get(index);
                        if (value == null) {
                            value = visit(cfpool.get(index), index);
                            debugln("[SG]: index " + index);
                            debugln("[SG]: value " + value);
                            slist.set(index, value);
                            out.put(index, value);
                        }
                        for (int idx : bsms.bootstrap_arguments) {
                            value = slist.get(idx);
                            if (value == null) {
                                value = visit(cfpool.get(idx), idx);
                                debugln("[SG]: idx " + idx);
                                debugln("[SG]: value " + value);
                                slist.set(idx, value);
                                out.put(idx, value);
                            }
                        }
                    } catch (InvalidIndex ex) {
                        ex.printStackTrace();
                    }
                }
                return out;
            }
            return new HashMap<>(0);
        }

        @Override
        public String visitClass(CONSTANT_Class_info c, Integer p) {

            String value = slist.get(p);
            if (value == null) {
                try {
                    value = visit(cfpool.get(c.name_index), c.name_index);
                    slist.set(p, value);
                } catch (ConstantPoolException ex) {
                    ex.printStackTrace();
                }
            }
            return value;
        }

        @Override
        public String visitDouble(CONSTANT_Double_info c, Integer p) {

            String value = slist.get(p);
            if (value == null) {
                value = Double.toString(c.value);
                slist.set(p, value);
            }
            return value;
        }

        @Override
        public String visitFieldref(CONSTANT_Fieldref_info c, Integer p) {

        String value = slist.get(p);
            if (value == null) {
                try {
                    value = visit(cfpool.get(c.class_index), c.class_index);
                    value = value.concat(" " + visit(cfpool.get(c.name_and_type_index),
                                         c.name_and_type_index));
                    slist.set(p, value);
                } catch (ConstantPoolException ex) {
                    ex.printStackTrace();
                }
            }
            return value;
        }

        @Override
        public String visitFloat(CONSTANT_Float_info c, Integer p) {

            String value = slist.get(p);
            if (value == null) {
                value = Float.toString(c.value);
                slist.set(p, value);
            }
            return value;
        }

        @Override
        public String visitInteger(CONSTANT_Integer_info cnstnt, Integer p) {

            String value = slist.get(p);
            if (value == null) {
                value = Integer.toString(cnstnt.value);
                slist.set(p, value);
            }
            return value;
        }

        @Override
        public String visitInterfaceMethodref(CONSTANT_InterfaceMethodref_info c,
                                              Integer p) {

            String value = slist.get(p);
            if (value == null) {
                try {
                    value = visit(cfpool.get(c.class_index), c.class_index);
                    value = value.concat(" " +
                                         visit(cfpool.get(c.name_and_type_index),
                                         c.name_and_type_index));
                    slist.set(p, value);
                } catch (ConstantPoolException ex) {
                    ex.printStackTrace();
                }
            }
            return value;
        }

        @Override
        public String visitInvokeDynamic(CONSTANT_InvokeDynamic_info c, Integer p) {

            String value = slist.get(p);
            if (value == null) {
                try {
                    value = bsmMap.get(c.bootstrap_method_attr_index) + " "
                            + visit(cfpool.get(c.name_and_type_index), c.name_and_type_index);
                    slist.set(p, value);
                } catch (ConstantPoolException ex) {
                    ex.printStackTrace();
                }
            }
            return value;
        }

        @Override
        public String visitDynamicConstant(CONSTANT_Dynamic_info c, Integer p) {

            String value = slist.get(p);
            if (value == null) {
                try {
                    value = bsmMap.get(c.bootstrap_method_attr_index) + " "
                            + visit(cfpool.get(c.name_and_type_index), c.name_and_type_index);
                    slist.set(p, value);
                } catch (ConstantPoolException ex) {
                    ex.printStackTrace();
                }
            }
            return value;
        }

        @Override
        public String visitLong(CONSTANT_Long_info c, Integer p) {

            String value = slist.get(p);
            if (value == null) {
                value = Long.toString(c.value);
                slist.set(p, value);
            }
            return value;
        }

        @Override
        public String visitNameAndType(CONSTANT_NameAndType_info c, Integer p) {

            String value = slist.get(p);
            if (value == null) {
                try {
                    value = visit(cfpool.get(c.name_index), c.name_index);
                    value = value.concat(" " +
                            visit(cfpool.get(c.type_index), c.type_index));
                    slist.set(p, value);
                } catch (InvalidIndex ex) {
                    ex.printStackTrace();
                }
            }
            return value;
        }

        @Override
        public String visitMethodref(CONSTANT_Methodref_info c, Integer p) {

            String value = slist.get(p);
            if (value == null) {
                try {
                    value = visit(cfpool.get(c.class_index), c.class_index);
                    value = value.concat(" " +
                                         visit(cfpool.get(c.name_and_type_index),
                                         c.name_and_type_index));
                    slist.set(p, value);
                } catch (ConstantPoolException ex) {
                    ex.printStackTrace();
                }
            }
            return value;
        }

        @Override
        public String visitMethodHandle(CONSTANT_MethodHandle_info c, Integer p) {

        String value = slist.get(p);
            if (value == null) {
                try {
                    value = c.reference_kind.name();
                    value = value.concat(" "
                            + visit(cfpool.get(c.reference_index), c.reference_index));
                    slist.set(p, value);
                } catch (ConstantPoolException ex) {
                    ex.printStackTrace();
                }
            }
            return value;
        }

        @Override
        public String visitMethodType(CONSTANT_MethodType_info c, Integer p) {

            String value = slist.get(p);
            if (value == null) {
                try {
                    value = visit(cfpool.get(c.descriptor_index), c.descriptor_index);
                    slist.set(p, value);
                } catch (ConstantPoolException ex) {
                    ex.printStackTrace();
                }
            }
            return value;
        }

        @Override
        public String visitModule(CONSTANT_Module_info c, Integer p) {

            String value = slist.get(p);
            if (value == null) {
                try {
                    value = visit(cfpool.get(c.name_index), c.name_index);
                    slist.set(p, value);
                } catch (ConstantPoolException ex) {
                    ex.printStackTrace();
                }
            }
            return value;
        }

        @Override
        public String visitPackage(CONSTANT_Package_info c, Integer p) {

            String value = slist.get(p);
            if (value == null) {
                try {
                    value = visit(cfpool.get(c.name_index), c.name_index);
                    slist.set(p, value);
                } catch (ConstantPoolException ex) {
                    ex.printStackTrace();
                }
            }
            return value;
        }

        @Override
        public String visitString(CONSTANT_String_info c, Integer p) {

            try {
                String value = slist.get(p);
                if (value == null) {
                    value = c.getString();
                    slist.set(p, value);
                }
                return value;
            } catch (ConstantPoolException ex) {
                throw new RuntimeException("Fatal error", ex);
            }
        }

        @Override
        public String  visitUtf8(CONSTANT_Utf8_info cnstnt, Integer p) {

            String value = slist.get(p);
            if (value == null) {
                value = cnstnt.value;
                slist.set(p, value);
            }
            return value;
        }
    }
}

