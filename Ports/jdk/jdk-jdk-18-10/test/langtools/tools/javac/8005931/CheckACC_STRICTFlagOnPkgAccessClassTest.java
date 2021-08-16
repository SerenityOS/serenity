/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005931
 * @summary javac doesn't set ACC_STRICT for classes with package access
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main CheckACC_STRICTFlagOnPkgAccessClassTest
 */

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import com.sun.source.util.JavacTask;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Descriptor;
import com.sun.tools.classfile.Descriptor.InvalidDescriptor;
import com.sun.tools.classfile.Method;

import static com.sun.tools.classfile.AccessFlags.ACC_STRICT;

public class CheckACC_STRICTFlagOnPkgAccessClassTest {

    private static final String AssertionErrorMessage =
        "All methods should have the ACC_STRICT access flag " +
        "please check output";
    private static final String CompilationErrorMessage =
        "Error thrown when compiling the following source:\n";
    private static final String offendingMethodErrorMessage =
        "Method %s of class %s doesn't have the ACC_STRICT access flag";

    JavaSource source = new JavaSource();

    private List<String> errors = new ArrayList<>();

    public static void main(String[] args)
            throws IOException, ConstantPoolException, InvalidDescriptor {
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        new CheckACC_STRICTFlagOnPkgAccessClassTest().run(comp);
    }

    private void run(JavaCompiler comp)
            throws IOException, ConstantPoolException, InvalidDescriptor {
        compile(comp);
        check();
        if (errors.size() > 0) {
            for (String error: errors) {
                System.err.println(error);
            }
            throw new AssertionError(AssertionErrorMessage);
        }
    }

    private void compile(JavaCompiler comp) {
        JavacTask ct = (JavacTask)comp.getTask(null, null, null,
                                               List.of("--release", "16"), null,
                Arrays.asList(source));
        try {
            if (!ct.call()) {
                throw new AssertionError(CompilationErrorMessage +
                        source.getCharContent(true));
            }
        } catch (Throwable ex) {
            throw new AssertionError(CompilationErrorMessage +
                    source.getCharContent(true));
        }
    }

    void check()
        throws
            IOException,
            ConstantPoolException,
            Descriptor.InvalidDescriptor {
        ClassFile classFileToCheck = ClassFile.read(new File("Test.class"));

        for (Method method : classFileToCheck.methods) {
            if ((method.access_flags.flags & ACC_STRICT) == 0) {
                errors.add(String.format(offendingMethodErrorMessage,
                        method.getName(classFileToCheck.constant_pool),
                        classFileToCheck.getName()));
            }
        }
    }

    class JavaSource extends SimpleJavaFileObject {

        String source = "strictfp class Test {" +
                "    Test(){}" +
                "    void m(){}" +
                "}";

        public JavaSource() {
            super(URI.create("Test.java"), JavaFileObject.Kind.SOURCE);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
