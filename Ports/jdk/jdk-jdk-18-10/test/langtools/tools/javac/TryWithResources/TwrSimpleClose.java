/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8194978
 * @summary Verify than an appropriate number of close method invocations is generated.
 * @library /tools/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox TwrSimpleClose
 * @run main TwrSimpleClose
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Methodref_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_NameAndType_info;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.Opcode;

import toolbox.ToolBox;

public class TwrSimpleClose {

    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    public static void main(String... args) throws Exception {
        new TwrSimpleClose().run();
    }

    void run() throws Exception {
        run("try (Test t = new Test()) { System.err.println(0); }", 2);
        run("try (Test t = new Test()) {\n" +
            "    if (t.hashCode() == 42)\n" +
            "        return;\n" +
            "    System.err.println(0);\n" +
            "}\n", 3);
    }

    void run(String trySpec, int expectedCloseCount) throws Exception {
        String template = "public class Test implements AutoCloseable {\n" +
                          "    void t(int i) {\n" +
                          "        TRY\n" +
                          "    }\n" +
                          "    public void close() { }\n" +
                          "}\n";
        String code = template.replace("TRY", trySpec);
        int closeCount = 0;

        try (StandardJavaFileManager sfm = compiler.getStandardFileManager(null, null, null);
             JFMImpl fm = new JFMImpl(sfm)) {
            Iterable<ToolBox.JavaSource> files = Arrays.asList(new ToolBox.JavaSource(code));
            JavacTask task = (JavacTask) compiler.getTask(null, fm, null, null, null, files);
            task.call();

            if (fm.classBytes.size() != 1) {
                throw new AssertionError();
            }

            byte[] data = fm.classBytes.values().iterator().next();
            ClassFile cf = ClassFile.read(new ByteArrayInputStream(data));

            for (Method m : cf.methods) {
                Code_attribute codeAttr = (Code_attribute) m.attributes.map.get(Attribute.Code);
                for (Instruction i : codeAttr.getInstructions()) {
                    if (i.getOpcode() == Opcode.INVOKEVIRTUAL) {
                        CONSTANT_Methodref_info method =
                                (CONSTANT_Methodref_info) cf.constant_pool.get(i.getShort(1));
                        CONSTANT_NameAndType_info nameAndType =
                                cf.constant_pool.getNameAndTypeInfo(method.name_and_type_index);
                        if ("close".equals(nameAndType.getName())) {
                            closeCount++;
                        }
                    }
                }
            }
            if (expectedCloseCount != closeCount) {
                throw new IllegalStateException("expected close count: " + expectedCloseCount +
                                                "; actual: " + closeCount + "; code:\n" + code);
            }
        }
    }

    private static final class JFMImpl extends ForwardingJavaFileManager<JavaFileManager> {

        private final Map<String, byte[]> classBytes = new HashMap<>();

        public JFMImpl(JavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind,
                                                   FileObject sibling) throws IOException {
            try {
                return new SimpleJavaFileObject(new URI("mem://" + className + ".class"), kind) {
                    @Override
                    public OutputStream openOutputStream() throws IOException {
                        return new ByteArrayOutputStream() {
                            @Override
                            public void close() throws IOException {
                                super.close();
                                classBytes.put(className, toByteArray());
                            }
                        };
                    }
                };
            } catch (URISyntaxException ex) {
                throw new IOException(ex);
            }
        }
    }

}
