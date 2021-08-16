/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.org.objectweb.asm.*;

/*
 * @test
 * @summary Test scenarios for constant pool CONSTANT_Module and CONSTANT_Package
 *          types, for class file versions 53 and 52, when ACC_MODULE is set and
 *          not set in the access_flags.
 * @bug 8175383
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @compile -XDignore.symbol.file ConstModule.java
 * @run main ConstModule
 */

public class ConstModule {

    static final int ACC_MODULE = 0x8000;
    static final boolean MODULE_TEST = true;
    static final boolean PACKAGE_TEST = false;
    static final boolean CFE_EXCEPTION = true;
    static final boolean NCDFE_EXCEPTION = false;

    public static void main(String[] args) throws Exception {

        // Test that the JVM throws CFE for constant pool CONSTANT_Module type, for
        // class file version 53, when ACC_MODULE is not set in the access_flags.
        ConstModule.write_and_load(Opcodes.V9,
            Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT + Opcodes.ACC_SYNTHETIC,
            "jdk.fooMod", "FooMod", MODULE_TEST, CFE_EXCEPTION);

        // Test that the JVM throws NCDFE for constant pool CONSTANT_Module type,
        // for class file version 53, when ACC_MODULE is set in the access_flags.
        ConstModule.write_and_load(Opcodes.V9,
            Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT + Opcodes.ACC_SYNTHETIC + ACC_MODULE,
            "jdk.fooModACC", "FooModACC", MODULE_TEST, NCDFE_EXCEPTION);

        // Test that the JVM throws CFE for constant pool CONSTANT_Module type, for
        // class file version 52, even when ACC_MODULE is set in the access_flags.
        ConstModule.write_and_load(Opcodes.V1_8,
            Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT + Opcodes.ACC_SYNTHETIC + ACC_MODULE,
            "jdk.fooModACC52", "FooModACC52", MODULE_TEST, CFE_EXCEPTION);

        // Test that the JVM throws CFE for constant pool CONSTANT_Package type, for
        // class file version 53, when ACC_MODULE is not set in the access_flags.
        ConstModule.write_and_load(Opcodes.V9,
            Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT + Opcodes.ACC_SYNTHETIC,
            "jdk.fooPkg", "FooPkg", PACKAGE_TEST, CFE_EXCEPTION);

        // Test that the JVM throws NCDFE for constant pool CONSTANT_Package type,
        // for class file version 53, when ACC_MODULE is set in the access_flags.
        ConstModule.write_and_load(Opcodes.V9,
            Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT + Opcodes.ACC_SYNTHETIC + ACC_MODULE,
            "jdk.fooModACC", "FooModACC", PACKAGE_TEST, NCDFE_EXCEPTION);

        // Test that the JVM throws CFE for constant pool CONSTANT_Package type, for
        // class file version 52, even when ACC_MODULE is set in the access_flags.
        ConstModule.write_and_load(Opcodes.V1_8,
            Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT + Opcodes.ACC_SYNTHETIC + ACC_MODULE,
            "jdk.fooModACC52", "FooModACC52", PACKAGE_TEST, CFE_EXCEPTION);

    }

    public static void write_and_load(int version,
                                      int access_flags,
                                      String attr,
                                      String class_name,
                                      boolean module_test,
                                      boolean throwCFE) throws Exception {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(version,
                 access_flags,
                 class_name,
                 null,
                 "java/lang/Object",
                 null);

        if (module_test)
            cw.visitAttribute(new TestModuleAttribute(attr));
        else
            cw.visitAttribute(new TestPackageAttribute(attr));

        cw.visitEnd();
        byte[] bytes = cw.toByteArray();


        ClassLoader loader = new ClassLoader(ConstModule.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String cn)throws ClassNotFoundException {
                if (cn.equals(class_name)) {
                    try {
                        Class superClass = super.defineClass(cn, bytes, 0, bytes.length);
                        throw new RuntimeException("Expected ClassFormatError not thrown");
                    } catch (java.lang.ClassFormatError e) {
                       if (!throwCFE) {
                           throw new RuntimeException("Unexpected ClassFormatError exception: " + e.getMessage());
                       }
                       if (module_test && !e.getMessage().contains(
                           "Unknown constant tag 19 in class file")) {
                           throw new RuntimeException("Wrong ClassFormatError exception: " + e.getMessage());
                       } else if (!module_test && !e.getMessage().contains(
                           "Unknown constant tag 20 in class file")) {
                           throw new RuntimeException("Wrong ClassFormatError exception: " + e.getMessage());
                       }
                    } catch (java.lang.NoClassDefFoundError f) {
                       if (throwCFE) {
                           throw new RuntimeException("Unexpected NoClassDefFoundError exception: " + f.getMessage());
                       }
                       if (!f.getMessage().contains(
                           "is not a class because access_flag ACC_MODULE is set")) {
                           throw new RuntimeException("Wrong NoClassDefFoundError exception: " + f.getMessage());
                       }
                    }
                } else {
                    throw new ClassNotFoundException(cn);
                }
                return null;
            }
        };

        Class<?> clazz = loader.loadClass(class_name);
    }

    /**
     * ConstModuleAttr attribute.
     *
     * <pre> {@code
     *
     * MainClass_attribute {
     *   // index to CONSTANT_utf8_info structure in constant pool representing
     *   // the string "ConstModuleAttr"
     *   u2 attribute_name_index;
     *   u4 attribute_length;
     *
     *   // index to CONSTANT_Module_info structure
     *   u2 module_name_index
     * }
     *
     * } </pre>
     */
    public static class TestModuleAttribute extends Attribute {
        private final String moduleName;

        public TestModuleAttribute(String moduleName) {
            super("ConstModuleAttr");
            this.moduleName = moduleName;
        }

        public TestModuleAttribute() {
            this(null);
        }

        @Override
        protected Attribute read(ClassReader cr,
                                 int off,
                                 int len,
                                 char[] buf,
                                 int codeOff,
                                 Label[] labels)
        {
            String mn = cr.readModule(off, buf);
            off += 2;
            return new TestModuleAttribute(mn);
        }

        @Override
        protected ByteVector write(ClassWriter cw,
                                   byte[] code,
                                   int len,
                                   int maxStack,
                                   int maxLocals)
        {
            ByteVector attr = new ByteVector();
            attr.putShort(cw.newModule(moduleName));
            return attr;
        }
    }

    /**
     * ConstPackageAttr attribute.
     *
     * <pre> {@code
     *
     * MainClass_attribute {
     *   // index to CONSTANT_utf8_info structure in constant pool representing
     *   // the string "ConstPackageAttr"
     *   u2 attribute_name_index;
     *   u4 attribute_length;
     *
     *   // index to CONSTANT_Package_info structure
     *   u2 module_name_index
     * }
     *
     * } </pre>
     */
    public static class TestPackageAttribute extends Attribute {
        private final String packageName;

        public TestPackageAttribute(String packageName) {
            super("ConstPackageAttr");
            this.packageName = packageName;
        }

        public TestPackageAttribute() {
            this(null);
        }

        @Override
        protected Attribute read(ClassReader cr,
                                 int off,
                                 int len,
                                 char[] buf,
                                 int codeOff,
                                 Label[] labels)
        {
            String mn = cr.readPackage(off, buf);
            off += 2;
            return new TestPackageAttribute(mn);
        }

        @Override
        protected ByteVector write(ClassWriter cw,
                                   byte[] code,
                                   int len,
                                   int maxStack,
                                   int maxLocals)
        {
            ByteVector attr = new ByteVector();
            attr.putShort(cw.newPackage(packageName));
            return attr;
        }
    }
}
