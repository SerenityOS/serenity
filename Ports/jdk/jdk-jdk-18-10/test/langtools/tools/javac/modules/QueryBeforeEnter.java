/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8208184
 * @summary tests for module resolution
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main QueryBeforeEnter
 */

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Arrays;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

// import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.Main;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class QueryBeforeEnter extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        QueryBeforeEnter t = new QueryBeforeEnter();
        t.runTests();
    }

    @Test
    public void testEmpty(Path base) throws Exception {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        com.sun.source.util.JavacTask task =
            (com.sun.source.util.JavacTask) javaCompiler.getTask(null, null, null, null, null, null);
        TypeElement jlString = task.getElements().getTypeElement("java.lang.String");

        assertNotNull(jlString);
    }

    @Test
    public void testUnnamed(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        tb.writeJavaFiles(m1,
                          "module m1x { exports m1x; }",
                          "package m1x; public class M1 {}");

        Path m2 = moduleSrc.resolve("m2x");

        tb.writeJavaFiles(m2,
                          "module m2x { exports m2x; }",
                          "package m2x; public class M2 {}");

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(modulePath)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll();

        Path cpSrc = base.resolve("cp-src");

        tb.writeJavaFiles(cpSrc,
                          "package cp; public class CP {}");

        Path cp = base.resolve("cp");

        Files.createDirectories(cp);

        new JavacTask(tb)
                .outdir(cp)
                .files(findJavaFiles(cpSrc))
                .run()
                .writeAll();

        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "package test; public class Test1 {}",
                          "package test; public class Test2 {}");

        Path out = base.resolve("out");

        Files.createDirectories(out);

        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = javaCompiler.getStandardFileManager(null, null, null)) {
            com.sun.source.util.JavacTask task =
                (com.sun.source.util.JavacTask) javaCompiler.getTask(null,
                                                              null,
                                                              d -> { throw new IllegalStateException(d.toString()); },
                                                              Arrays.asList("--module-path", modulePath.toString(),
                                                                            "--class-path", cp.toString(),
                                                                            "-sourcepath", src.toString()),
                                                              null,
                                                              fm.getJavaFileObjects(src.resolve("test").resolve("Test2.java")));
            assertNotNull(task.getElements().getTypeElement("java.lang.String"));
            assertNotNull(task.getElements().getTypeElement("javax.tools.ToolProvider"));
            assertNull(task.getElements().getTypeElement("m1x.M1"));
            assertNull(task.getElements().getTypeElement("m2x.M2"));
            assertNotNull(task.getElements().getTypeElement("cp.CP"));
            assertNotNull(task.getElements().getTypeElement("test.Test1"));
            assertNotNull(task.getElements().getTypeElement("test.Test2"));
            assertNotNull(task.getElements().getModuleElement("java.base"));
            assertNotNull(task.getElements().getModuleElement("java.compiler"));
            assertNull(task.getElements().getModuleElement("m1x"));
            assertNull(task.getElements().getModuleElement("m2x"));
        }
    }

    @Test
    public void testSingleNamed(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        tb.writeJavaFiles(m1,
                          "module m1x { exports m1x; }",
                          "package m1x; public class M1 {}");

        Path m2 = moduleSrc.resolve("m2x");

        tb.writeJavaFiles(m2,
                          "module m2x { exports m2x; }",
                          "package m2x; public class M2 {}");

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(modulePath)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll();

        Path cpSrc = base.resolve("cp-src");

        tb.writeJavaFiles(cpSrc,
                          "package cp; public class CP {}");

        Path cp = base.resolve("cp");

        Files.createDirectories(cp);

        new JavacTask(tb)
                .outdir(cp)
                .files(findJavaFiles(cpSrc))
                .run()
                .writeAll();

        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "module test { requires java.base; requires m1x; } ",
                          "package test; public class Test {}");

        Path out = base.resolve("out");

        Files.createDirectories(out);

        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = javaCompiler.getStandardFileManager(null, null, null)) {
            com.sun.source.util.JavacTask task =
                (com.sun.source.util.JavacTask) javaCompiler.getTask(null,
                                                              null,
                                                              d -> { throw new IllegalStateException(d.toString()); },
                                                              Arrays.asList("--module-path", modulePath.toString(),
                                                                            "--class-path", cp.toString(),
                                                                            "-sourcepath", src.toString()),
                                                              null,
                                                              fm.getJavaFileObjects(findJavaFiles(src)));
            assertNotNull(task.getElements().getTypeElement("java.lang.String"));
            assertNull(task.getElements().getTypeElement("javax.tools.ToolProvider"));
            assertNotNull(task.getElements().getTypeElement("m1x.M1"));
            assertNull(task.getElements().getTypeElement("m2x.M2"));
            assertNotNull(task.getElements().getTypeElement("test.Test"));
            assertNotNull(task.getElements().getModuleElement("java.base"));
            assertNull(task.getElements().getModuleElement("java.compiler"));
            assertNotNull(task.getElements().getModuleElement("m1x"));
            assertNull(task.getElements().getModuleElement("m2x"));
            assertNotNull(task.getElements().getModuleElement("test"));
        }
    }

    @Test
    public void testMultiModule(Path base) throws Exception {
        Path modulePathSrc = base.resolve("module-path-src");
        Path m1 = modulePathSrc.resolve("m1x");

        tb.writeJavaFiles(m1,
                          "module m1x { exports m1x; }",
                          "package m1x; public class M1 {}");

        Path m2 = modulePathSrc.resolve("m2x");

        tb.writeJavaFiles(m2,
                          "module m2x { exports m2x; }",
                          "package m2x; public class M2 {}");

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        new JavacTask(tb)
                .options("--module-source-path", modulePathSrc.toString())
                .outdir(modulePath)
                .files(findJavaFiles(modulePathSrc))
                .run()
                .writeAll();

        Path cpSrc = base.resolve("cp-src");

        tb.writeJavaFiles(cpSrc,
                          "package cp; public class CP {}");

        Path cp = base.resolve("cp");

        Files.createDirectories(cp);

        new JavacTask(tb)
                .outdir(cp)
                .files(findJavaFiles(cpSrc))
                .run()
                .writeAll();

        Path moduleSrc = base.resolve("module-src");
        Path m3 = moduleSrc.resolve("m3x");

        tb.writeJavaFiles(m3,
                          "module m3x { requires m1x; exports m3x; }",
                          "package m3x; public class M3 {  }");

        Path m4 = moduleSrc.resolve("m4x");

        tb.writeJavaFiles(m4,
                          "module m4x { exports m4x; }",
                          "package m4x; public class M4 {}");

        Path out = base.resolve("out");

        Files.createDirectories(out);

        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = javaCompiler.getStandardFileManager(null, null, null)) {
            com.sun.source.util.JavacTask task =
                (com.sun.source.util.JavacTask) javaCompiler.getTask(null,
                                                              null,
                                                              d -> { throw new IllegalStateException(d.toString()); },
                                                              Arrays.asList("--module-path", modulePath.toString(),
                                                                            "--class-path", cp.toString(),
                                                                            "--module-source-path", moduleSrc.toString(),
                                                                            "-d", out.toString()),
                                                              null,
                                                              fm.getJavaFileObjects(findJavaFiles(moduleSrc)));
            assertNotNull(task.getElements().getTypeElement("java.lang.String"));
            assertNull(task.getElements().getTypeElement("javax.tools.ToolProvider"));
            assertNotNull(task.getElements().getTypeElement("m1x.M1"));
            assertNull(task.getElements().getTypeElement("m2x.M2"));
            assertNotNull(task.getElements().getTypeElement("m3x.M3"));
            assertNotNull(task.getElements().getTypeElement("m4x.M4"));
            assertNotNull(task.getElements().getModuleElement("java.base"));
            assertNull(task.getElements().getModuleElement("java.compiler"));
            assertNotNull(task.getElements().getModuleElement("m1x"));
            assertNull(task.getElements().getModuleElement("m2x"));
            assertNotNull(task.getElements().getModuleElement("m3x"));
            assertNotNull(task.getElements().getModuleElement("m4x"));
        }
    }

    @Test
    public void testTooSoon(Path base) throws Exception {
        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "package test; public class Test {}");

        Path out = base.resolve("out");

        Files.createDirectories(out);

        Path reg = base.resolve("reg");
        Path regFile = reg.resolve("META-INF").resolve("services").resolve(Plugin.class.getName());

        Files.createDirectories(regFile.getParent());

        try (OutputStream regOut = Files.newOutputStream(regFile)) {
            regOut.write(PluginImpl.class.getName().getBytes());
        }

        String processorPath = System.getProperty("test.class.path") + File.pathSeparator + reg.toString();

        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        Path testSource = src.resolve("test").resolve("Test.java");
        try (StandardJavaFileManager fm = javaCompiler.getStandardFileManager(null, null, null)) {
            com.sun.source.util.JavacTask task =
                (com.sun.source.util.JavacTask) javaCompiler.getTask(null,
                                                              null,
                                                              d -> { throw new IllegalStateException(d.toString()); },
                                                              Arrays.asList("--processor-path", processorPath,
                                                                            "-processor", AP.class.getName(),
                                                                            "-Xplugin:test"),
                                                              null,
                                                              fm.getJavaFileObjects(testSource));
            task.call();
        }

        Main.compile(new String[] {"--processor-path", processorPath,
                                   "-Xplugin:test",
                                   testSource.toString()});
    }

    public static class PluginImpl implements Plugin {

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public void init(com.sun.source.util.JavacTask task, String... args) {
            task.addTaskListener(new TaskListener() {
                boolean wasEntered;
                @Override
                public void started(TaskEvent e) {
                    switch (e.getKind()) {
                        case COMPILATION: case PARSE:
                            shouldFail(e.getKind());
                            break;
                        case ANNOTATION_PROCESSING: case ENTER:
                            if (wasEntered) {
                                shouldPass(e.getKind());
                            } else {
                                shouldFail(e.getKind());
                            }
                            break;
                        default:
                            shouldPass(e.getKind());
                            break;
                    }
                }
                @Override
                public void finished(TaskEvent e) {
                    switch (e.getKind()) {
                        case PARSE:
                            shouldFail(e.getKind());
                            break;
                        case ENTER:
                            wasEntered = true;
                            //fall-through:
                        default:
                            shouldPass(e.getKind());
                            break;
                    }
                }
                private void shouldFail(TaskEvent.Kind kind) {
                    try {
                        task.getElements().getTypeElement("java.lang.String");
                        throw new AssertionError("Expected exception not observed; kind=" + kind.name());
                    } catch (IllegalStateException ex) {
                        //correct
                    }
                }
                private void shouldPass(TaskEvent.Kind kind) {
                    assertNotNull(task.getElements().getTypeElement("java.lang.String"));
                }
            });

        }

    }

    @SupportedAnnotationTypes("*")
    public static final class AP extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @Test
    public void testBrokenModule(Path base) throws Exception {
        Map<String, String> sourceFileName2Content = new HashMap<>();

        sourceFileName2Content.put("module-info.java", "module test { requires unknown.; } ");
        sourceFileName2Content.put("test/Test.java", "package test; public class Test {}");

        Path out = base.resolve("out");

        Files.createDirectories(out);

        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = javaCompiler.getStandardFileManager(null, null, null)) {
            com.sun.source.util.JavacTask task =
                (com.sun.source.util.JavacTask) javaCompiler.getTask(null,
                                                              new TestMemoryFileManager(fm, sourceFileName2Content),
                                                              null,
                                                              Arrays.asList("-d", out.toString()),
                                                              null,
                                                              null);
            task.getElements().getTypeElement("test.Test");
        }
    }

    private static final class TestMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {

        private final Map<String, String> sourceFileName2Content;

        public TestMemoryFileManager(JavaFileManager fileManager, Map<String, String> sourceFileName2Content) {
            super(fileManager);
            this.sourceFileName2Content = sourceFileName2Content;
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
            if (location == StandardLocation.SOURCE_PATH) {
                List<JavaFileObject> result = new ArrayList<>();
                String dir = packageName.replace('.', '/') + "/";

                for (Entry<String, String> e : sourceFileName2Content.entrySet()) {
                    if (e.getKey().startsWith(dir) &&
                        !e.getKey().substring(dir.length()).contains("/")) {
                        try {
                            result.add(new SourceFileObject(e.getKey(), e.getValue()));
                        } catch (URISyntaxException ex) {
                            throw new IOException(ex);
                        }
                    }
                }

                return result;
            }
            return super.list(location, packageName, kinds, recurse);
        }

        @Override
        public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
            if (location == StandardLocation.SOURCE_PATH) {
                String path = className.replace('.', '/') + ".java";
                String code = sourceFileName2Content.get(path);

                if (code == null) return null;

                try {
                    return new SourceFileObject(path, code);
                } catch (URISyntaxException ex) {
                    throw new IOException(ex);
                }
            }
            return super.getJavaFileForInput(location, className, kind);
        }

        @Override
        public boolean hasLocation(Location location) {
            return super.hasLocation(location) || location == StandardLocation.SOURCE_PATH;
        }

        @Override
        public boolean contains(Location location, FileObject fo) throws IOException {
            if (location == StandardLocation.SOURCE_PATH) {
                return fo instanceof SourceFileObject;
            }
            return super.contains(location, fo);
        }

        @Override
        public String inferBinaryName(Location location, JavaFileObject file) {
            if (location == StandardLocation.SOURCE_PATH) {
                String path = ((SourceFileObject) file).path;
                String fileName = path.substring(path.lastIndexOf('/'));
                return fileName.substring(0, fileName.length() - ".java".length());
            }
            return super.inferBinaryName(location, file);
        }

    }

    private static final class SourceFileObject extends SimpleJavaFileObject {
        private final String path;
        private final String code;

        public SourceFileObject(String path, String code) throws URISyntaxException {
            super(new URI("mem://" + path), Kind.SOURCE);
            this.path = path;
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return code;
        }

        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            return path.endsWith(simpleName + kind.extension);
        }

    }

    private static void assertNotNull(Object actual) {
        if (actual == null) {
            throw new AssertionError("unexpected null!");
        }
    }

    private static void assertNull(Object actual) {
        if (actual != null) {
            throw new AssertionError("unexpected non null!");
        }
    }

}
