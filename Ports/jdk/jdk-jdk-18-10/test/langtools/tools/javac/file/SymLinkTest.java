/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8178017 8181897
 * @summary JDK 9 change to symlink handling causes misleading
 *      class.public.should.be.in.file diagnostic and SourceFile
 *      attribute content
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.JavacTask toolbox.TestRunner toolbox.ToolBox
 * @run main SymLinkTest
 */

import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.SourceFile_attribute;
import toolbox.JavacTask;
import toolbox.TestRunner;
import toolbox.TestRunner.Test;
import toolbox.ToolBox;

public class SymLinkTest extends TestRunner {
    public static void main(String... args) throws Exception {
        new SymLinkTest().runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    private final ToolBox tb = new ToolBox();

    public SymLinkTest() {
        super(System.err);
    }

    @Test
    public void testgetKind(Path base) throws Exception {
        test(base, "SOURCE");
    }

    @Test
    public void testSymLink(Path base) throws Exception {
        test(base, "SOURCE.java");
    }

    void test(Path base, String name) throws Exception{
        Path file = base.resolve(name);
        Path javaFile = base.resolve("HelloWorld.java");
        tb.writeFile(file,
                "public class HelloWorld {\n"
                + "    public static void main(String... args) {\n"
                + "        System.err.println(\"Hello World!\");\n"
                + "    }\n"
                + "}");

        try {
            Files.createSymbolicLink(javaFile, file.getFileName());
        } catch (FileSystemException fse) {
            System.err.println("warning: test passes vacuously, sym-link could not be created");
            System.err.println(fse.getMessage());
            return;
        }

        Path classes = Files.createDirectories(base.resolve("classes"));
        new JavacTask(tb)
            .outdir(classes)
            .files(javaFile)
            .run()
            .writeAll();

        ClassFile cf = ClassFile.read(classes.resolve("HelloWorld.class"));
        SourceFile_attribute sf = (SourceFile_attribute) cf.attributes.get(Attribute.SourceFile);
        String sourceFile = sf.getSourceFile(cf.constant_pool);

        if (!"HelloWorld.java".equals(sourceFile)) {
            throw new AssertionError("Unexpected SourceFile attribute value: " + sourceFile);
        }
    }
}

