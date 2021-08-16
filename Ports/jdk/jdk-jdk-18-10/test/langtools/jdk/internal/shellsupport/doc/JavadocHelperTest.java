/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8131019 8189778 8190552
 * @summary Test JavadocHelper
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/jdk.internal.shellsupport.doc
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask
 * @run testng/timeout=900/othervm JavadocHelperTest
 */

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import jdk.internal.shellsupport.doc.JavadocHelper;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test
public class JavadocHelperTest {

    public void testJavadoc() throws Exception {
        doTestJavadoc("",
                      t -> t.getElements().getTypeElement("test.Super"),
                      "Top level. ");
        doTestJavadoc("",
                      t -> getFirstMethod(t, "test.Super"),
                      " javadoc1\n" +
                      "\n" +
                      " @param p1 param1\n" +
                      " @param p2 param2\n" +
                      " @param p3 param3\n" +
                      " @throws IllegalStateException exc1\n" +
                      " @throws IllegalArgumentException exc2\n" +
                      " @throws IllegalAccessException exc3\n" +
                      " @return value\n");
    }

    private Element getFirstMethod(JavacTask task, String typeName) {
        return ElementFilter.methodsIn(task.getElements().getTypeElement(typeName).getEnclosedElements()).get(0);
    }

    private Function<JavacTask, Element> getSubTest = t -> getFirstMethod(t, "test.Sub");

    public void testInheritNoJavadoc() throws Exception {
        doTestJavadoc("",
                      getSubTest,
                      " javadoc1\n" +
                      "\n" +
                      " @param p1 param1\n" +
                      " @param p2 param2\n" +
                      " @param p3 param3\n" +
                      " @throws IllegalStateException exc1\n" +
                      " @throws IllegalArgumentException exc2\n" +
                      " @throws IllegalAccessException exc3\n" +
                      " @return value\n");
    }

    public void testInheritFull() throws Exception {
        doTestJavadoc("    /**\n" +
                      "     * Prefix {@inheritDoc} suffix.\n" +
                      "     *\n" +
                      "     * @param p1 prefix {@inheritDoc} suffix\n" +
                      "     * @param p2 prefix {@inheritDoc} suffix\n" +
                      "     * @param p3 prefix {@inheritDoc} suffix\n" +
                      "     * @throws IllegalStateException prefix {@inheritDoc} suffix\n" +
                      "     * @throws IllegalArgumentException prefix {@inheritDoc} suffix\n" +
                      "     * @throws IllegalAccessException prefix {@inheritDoc} suffix\n" +
                      "     * @return prefix {@inheritDoc} suffix\n" +
                      "     */\n",
                      getSubTest,
                      " Prefix javadoc1 suffix.\n" +
                      "\n" +
                      " @param p1 prefix param1 suffix\n" +
                      " @param p2 prefix param2 suffix\n" +
                      " @param p3 prefix param3 suffix\n" +
                      " @throws IllegalStateException prefix exc1 suffix\n" +
                      " @throws IllegalArgumentException prefix exc2 suffix\n" +
                      " @throws IllegalAccessException prefix exc3 suffix\n" +
                      " @return prefix value suffix\n");
    }

    public void testInheritMissingParam() throws Exception {
        doTestJavadoc("    /**\n" +
                      "     * Prefix {@inheritDoc} suffix.\n" +
                      "     *\n" +
                      "     * @param p1 prefix {@inheritDoc} suffix\n" +
                      "     * @param p3 prefix {@inheritDoc} suffix\n" +
                      "     * @throws IllegalStateException prefix {@inheritDoc} suffix\n" +
                      "     * @throws IllegalArgumentException prefix {@inheritDoc} suffix\n" +
                      "     * @throws IllegalAccessException prefix {@inheritDoc} suffix\n" +
                      "     * @return prefix {@inheritDoc} suffix\n" +
                      "     */\n",
                      getSubTest,
                      " Prefix javadoc1 suffix.\n" +
                      "\n" +
                      " @param p1 prefix param1 suffix\n" +
                      "@param p2 param2\n" +
                      " @param p3 prefix param3 suffix\n" +
                      " @throws IllegalStateException prefix exc1 suffix\n" +
                      " @throws IllegalArgumentException prefix exc2 suffix\n" +
                      " @throws IllegalAccessException prefix exc3 suffix\n" +
                      " @return prefix value suffix\n");
    }

    public void testInheritMissingFirstParam() throws Exception {
        doTestJavadoc("    /**\n" +
                      "     * Prefix {@inheritDoc} suffix.\n" +
                      "     *\n" +
                      "     * @param p2 prefix {@inheritDoc} suffix\n" +
                      "     * @param p3 prefix {@inheritDoc} suffix\n" +
                      "     * @throws IllegalStateException prefix {@inheritDoc} suffix\n" +
                      "     * @throws IllegalArgumentException prefix {@inheritDoc} suffix\n" +
                      "     * @throws IllegalAccessException prefix {@inheritDoc} suffix\n" +
                      "     * @return prefix {@inheritDoc} suffix\n" +
                      "     */\n",
                      getSubTest,
                      " Prefix javadoc1 suffix.\n" +
                      "\n" +
                      "@param p1 param1\n" +
                      " @param p2 prefix param2 suffix\n" +
                      " @param p3 prefix param3 suffix\n" +
                      " @throws IllegalStateException prefix exc1 suffix\n" +
                      " @throws IllegalArgumentException prefix exc2 suffix\n" +
                      " @throws IllegalAccessException prefix exc3 suffix\n" +
                      " @return prefix value suffix\n");
    }

    public void testInheritMissingThrows() throws Exception {
        doTestJavadoc("    /**\n" +
                      "     * Prefix {@inheritDoc} suffix.\n" +
                      "     *\n" +
                      "     * @param p1 prefix {@inheritDoc} suffix\n" +
                      "     * @param p2 prefix {@inheritDoc} suffix\n" +
                      "     * @param p3 prefix {@inheritDoc} suffix\n" +
                      "     * @throws IllegalStateException prefix {@inheritDoc} suffix\n" +
                      "     * @throws IllegalAccessException prefix {@inheritDoc} suffix\n" +
                      "     * @return prefix {@inheritDoc} suffix\n" +
                      "     */\n",
                      getSubTest,
                      " Prefix javadoc1 suffix.\n" +
                      "\n" +
                      " @param p1 prefix param1 suffix\n" +
                      " @param p2 prefix param2 suffix\n" +
                      " @param p3 prefix param3 suffix\n" +
                      " @throws IllegalStateException prefix exc1 suffix\n" +
                      "@throws java.lang.IllegalArgumentException exc2\n" +
                      " @throws IllegalAccessException prefix exc3 suffix\n" +
                      " @return prefix value suffix\n");
    }

    public void testInheritMissingReturn() throws Exception {
        doTestJavadoc("    /**\n" +
                      "     * Prefix {@inheritDoc} suffix.\n" +
                      "     *\n" +
                      "     * @param p1 prefix {@inheritDoc} suffix\n" +
                      "     * @param p2 prefix {@inheritDoc} suffix\n" +
                      "     * @param p3 prefix {@inheritDoc} suffix\n" +
                      "     * @throws IllegalStateException prefix {@inheritDoc} suffix\n" +
                      "     * @throws IllegalArgumentException prefix {@inheritDoc} suffix\n" +
                      "     * @throws IllegalAccessException prefix {@inheritDoc} suffix\n" +
                      "     */\n",
                      getSubTest,
                      " Prefix javadoc1 suffix.\n" +
                      "\n" +
                      " @param p1 prefix param1 suffix\n" +
                      " @param p2 prefix param2 suffix\n" +
                      " @param p3 prefix param3 suffix\n" +
                      " @throws IllegalStateException prefix exc1 suffix\n" +
                      " @throws IllegalArgumentException prefix exc2 suffix\n" +
                      " @throws IllegalAccessException prefix exc3 suffix\n" +
                      "@return value\n");
    }

    public void testInheritAllButOne() throws Exception {
        doTestJavadoc("    /**\n" +
                      "     * @throws IllegalArgumentException {@inheritDoc}\n" +
                      "     */\n",
                      getSubTest,
                      "javadoc1\n" +
                      "@param p1 param1\n" +
                      "@param p2 param2\n" +
                      "@param p3 param3\n" +
                      "@throws java.lang.IllegalStateException exc1\n" +
                      " @throws IllegalArgumentException exc2\n" +
                      "@throws java.lang.IllegalAccessException exc3\n" +
                      "@return value\n");
    }

    public void testInheritEmpty() throws Exception {
        doTestJavadoc("    /**\n" +
                      "     */\n",
                      "    /**@param p1\n" +
                      "     * @param p2\n" +
                      "     * @param p3\n" +
                      "     * @throws IllegalStateException\n" +
                      "     * @throws IllegalArgumentException\n" +
                      "     * @throws IllegalAccessException\n" +
                      "     * @return\n" +
                      "     */\n",
                      getSubTest,
                      "\n" +
                      "@param p1 \n" +
                      "@param p2 \n" +
                      "@param p3 \n" +
                      "@throws java.lang.IllegalStateException \n" +
                      "@throws java.lang.IllegalArgumentException \n" +
                      "@throws java.lang.IllegalAccessException \n" +
                      "@return \n");
    }

    public void testEmptyValue() throws Exception {
        doTestJavadoc("    /**\n" +
                      "     */\n",
                      "    /**@param p1 {@value}\n" +
                      "     * @param p2\n" +
                      "     * @param p3\n" +
                      "     * @throws IllegalStateException\n" +
                      "     * @throws IllegalArgumentException\n" +
                      "     * @throws IllegalAccessException\n" +
                      "     * @return\n" +
                      "     */\n",
                      getSubTest,
                      "\n" +
                      "@param p1 {@value}\n" +
                      "@param p2 \n" +
                      "@param p3 \n" +
                      "@throws java.lang.IllegalStateException \n" +
                      "@throws java.lang.IllegalArgumentException \n" +
                      "@throws java.lang.IllegalAccessException \n" +
                      "@return \n");
    }

    public void testShortComment() throws Exception {
        doTestJavadoc("    /**Test.*/\n",
                      getSubTest,
                      "Test." +
                      "@param p1 param1\n" +
                      "@param p2 param2\n" +
                      "@param p3 param3\n" +
                      "@throws java.lang.IllegalStateException exc1\n" +
                      "@throws java.lang.IllegalArgumentException exc2\n" +
                      "@throws java.lang.IllegalAccessException exc3\n" +
                      "@return value\n");
    }

    private void doTestJavadoc(String origJavadoc, Function<JavacTask, Element> getElement, String expectedJavadoc) throws Exception {
        doTestJavadoc(origJavadoc,
                      "    /**\n" +
                      "     * javadoc1\n" +
                      "     *\n" +
                      "     * @param p1 param1\n" +
                      "     * @param p2 param2\n" +
                      "     * @param p3 param3\n" +
                      "     * @throws IllegalStateException exc1\n" +
                      "     * @throws IllegalArgumentException exc2\n" +
                      "     * @throws IllegalAccessException exc3\n" +
                      "     * @return value\n" +
                      "     */\n",
                      getElement, expectedJavadoc);
    }

    private void doTestJavadoc(String origJavadoc,
                               String superJavadoc,
                               Function<JavacTask, Element> getElement,
                               String expectedJavadoc) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String subClass =
                "package test;\n" +
                "public class Sub extends Super {\n" +
                origJavadoc +
                "    public String test(int p1, int p2, int p3) throws IllegalStateException, IllegalArgumentException, IllegalAccessException { return null;} \n" +
                "}\n";
        String superClass =
                "package test;\n" +
                "/**Top level." +
                " */\n" +
                "public class Super {\n" +
                superJavadoc +
                "    public String test(int p1, int p2, int p3) throws IllegalStateException, IllegalArgumentException, IllegalAccessException { return null;} \n" +
                "}\n";

        Path srcZip = Paths.get("src.zip");

        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(srcZip))) {
            out.putNextEntry(new JarEntry("test/Sub.java"));
            out.write(subClass.getBytes());
            out.putNextEntry(new JarEntry("test/Super.java"));
            out.write(superClass.getBytes());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        DiagnosticListener<? super JavaFileObject> noErrors = d -> {
            if (d.getKind() == Kind.ERROR) {
                throw new AssertionError(d.getMessage(null));
            }
        };

        assertTrue(compiler.getTask(null, null, noErrors, Arrays.asList("-d", "."), null, Arrays.asList(new JFOImpl("Super", superClass), new JFOImpl("Sub", subClass))).call());

        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            fm.setLocationFromPaths(StandardLocation.CLASS_PATH, Arrays.asList(Paths.get(".").toAbsolutePath()));
            JavacTask task = (JavacTask) compiler.getTask(null, fm, noErrors, null, null, null);

            Element el = getElement.apply(task);

            try (JavadocHelper helper = JavadocHelper.create(task, Arrays.asList(srcZip))) {
                String javadoc = helper.getResolvedDocComment(el);

                assertEquals(javadoc, expectedJavadoc);
            }
        }
    }

    private static final class JFOImpl extends SimpleJavaFileObject {

        private final String code;

        public JFOImpl(String name, String code) throws URISyntaxException {
            super(new URI("mem:///" + name + ".java"), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return code;
        }

    }

    public void testAllDocs() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticListener<? super JavaFileObject> noErrors = d -> {
            if (d.getKind() == Kind.ERROR) {
                throw new AssertionError(d.getMessage(null));
            }
        };

        List<Path> sources = new ArrayList<>();
        Path home = Paths.get(System.getProperty("java.home"));
        Path srcZip = home.resolve("lib").resolve("src.zip");
        if (Files.isReadable(srcZip)) {
            URI uri = URI.create("jar:" + srcZip.toUri());
            try (FileSystem zipFO = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                Path root = zipFO.getRootDirectories().iterator().next();

                //modular format:
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
                    for (Path p : ds) {
                        if (Files.isDirectory(p)) {
                            sources.add(p);
                        }
                    }
                }
                try (StandardJavaFileManager fm =
                        compiler.getStandardFileManager(null, null, null)) {
                    JavacTask task =
                            (JavacTask) compiler.getTask(null, fm, noErrors, null, null, null);
                    task.getElements().getTypeElement("java.lang.Object");
                    for (ModuleElement me : task.getElements().getAllModuleElements()) {
                        List<ExportsDirective> exports =
                                ElementFilter.exportsIn(me.getDirectives());
                        for (ExportsDirective ed : exports) {
                            try (JavadocHelper helper = JavadocHelper.create(task, sources)) {
                                List<? extends Element> content =
                                        ed.getPackage().getEnclosedElements();
                                for (TypeElement clazz : ElementFilter.typesIn(content)) {
                                    for (Element el : clazz.getEnclosedElements()) {
                                        helper.getResolvedDocComment(el);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
