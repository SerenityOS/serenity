/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7199823
 * @summary javac generates inner class that can't be verified
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main InnerClassCannotBeVerified
 */

import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import javax.tools.JavaFileObject;
import java.net.URI;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler;
import com.sun.source.util.JavacTask;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import java.io.File;
import java.io.IOException;

public class InnerClassCannotBeVerified {

    enum CompilationKind {
        PRE_NESTMATES("-source", "10", "-target", "10"),
        POST_NESTMATES();

        String[] opts;

        CompilationKind(String... opts) {
            this.opts = opts;
        }
    }

    private static final String errorMessage =
            "Compile error while compiling the following source:\n";

    public static void main(String... args) throws Exception {
        new InnerClassCannotBeVerified().run();
    }

    void run() throws Exception {
        for (CompilationKind ck : CompilationKind.values()) {
            File file = new File("Test$1.class");
            if (file.exists()) {
                file.delete();
            }
            JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
            JavaSource source = new JavaSource();
            JavacTask ct = (JavacTask)comp.getTask(null, null, null,
                    Arrays.asList(ck.opts), null, Arrays.asList(source));
            try {
                if (!ct.call()) {
                    throw new AssertionError(errorMessage +
                            source.getCharContent(true));
                }
            } catch (Throwable ex) {
                throw new AssertionError(errorMessage +
                        source.getCharContent(true));
            }
            check(ck);
        }
    }

    private void check(CompilationKind ck) throws IOException, ConstantPoolException {
        try {
            File file = new File("Test$1.class");
            ClassFile classFile = ClassFile.read(file);
            if (ck == CompilationKind.POST_NESTMATES) {
                throw new AssertionError("Unexpected constructor tag class!");
            }
            boolean inheritsFromObject =
                    classFile.getSuperclassName().equals("java/lang/Object");
            boolean implementsNoInterface = classFile.interfaces.length == 0;
            boolean noMethods = classFile.methods.length == 0;
            if (!(inheritsFromObject &&
                    implementsNoInterface &&
                    noMethods)) {
                throw new AssertionError("The inner classes reused as " +
                        "access constructor tag for this code must be empty");
            }
        } catch (NoSuchFileException ex) {
            if (ck == CompilationKind.PRE_NESTMATES) {
                throw new AssertionError("Constructor tag class missing!");
            }
        }
    }

    class JavaSource extends SimpleJavaFileObject {

        String internalSource =
                              "public class Test {\n" +
                              "    private static class Foo {}\n" +
                              "    public static void main(String[] args){ \n" +
                              "        new Foo();\n" +
                              "        if(false) {\n" +
                              "            new Runnable() {\n" +
                              "                @Override\n" +
                              "                public void run() {\n" +
                              "                    System.out.println();\n" +
                              "                }\n" +
                              "            }.run();\n" +
                              "        }\n" +
                              "   }\n" +
                              "}";
        public JavaSource() {
            super(URI.create("Test.java"), JavaFileObject.Kind.SOURCE);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return internalSource;
        }
    }
}
