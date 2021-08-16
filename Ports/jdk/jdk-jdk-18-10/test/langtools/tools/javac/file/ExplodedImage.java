/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8067138
 * @summary Verify that compiling against the exploded JDK image works, and that Locations close
 *          the directory streams properly when working with exploded JDK image.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox ExplodedImage
 * @run main/othervm ExplodedImage modules/* testDirectoryStreamClosed
 * @run main/othervm ExplodedImage modules/* testCanCompileAgainstExplodedImage
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.code.Symbol.ClassSymbol;

import toolbox.ToolBox;

public class ExplodedImage {
    public static void main(String... args) throws IOException {
        new ExplodedImage().run(args);
    }

    void run(String... args) throws IOException {
        switch (args[0]) {
            case "testDirectoryStreamClosed":
                testDirectoryStreamClosed(args[1]);
                break;
            case "testCanCompileAgainstExplodedImage":
                testCanCompileAgainstExplodedImage(args[1]);
                break;
        }
    }

    void testDirectoryStreamClosed(String loc) throws IOException {
        System.err.println("testDirectoryStreamClosed(" + loc + ")");
        Path javaHome = prepareJavaHome();
        Path targetPath = javaHome.resolve(loc.replace("*", "/java.base").replace("/", sep));
        Path testClass = targetPath.resolve(("java/lang/" + TEST_FILE).replace("/", sep));
        Files.createDirectories(testClass.getParent());
        Files.createFile(testClass);
        System.setProperty("java.home", javaHome.toString());

        for (int i = 0; i < REPEATS; i++) {
            try (StandardJavaFileManager fm = javaCompiler.getStandardFileManager(null, null, null)) {
                Iterable<JavaFileObject> javaLangContent =
                        fm.list(StandardLocation.PLATFORM_CLASS_PATH,
                                "java.lang",
                                EnumSet.allOf(JavaFileObject.Kind.class),
                                false);
                boolean found = false;

                for (JavaFileObject fo : javaLangContent) {
                    if (!fo.getName().endsWith(TEST_FILE)) {
                        throw new IllegalStateException("Wrong file: " + fo);
                    }
                    found = true;
                }

                if (!found)
                    throw new IllegalStateException("Could not find the expected file!");
            }
        }

        System.err.println("finished.");
    }
    //where:
        static final String TEST_FILE = "ExplodedImageTestFile.class";
        static final int REPEATS = 16 * 1024 + 1;

    void testCanCompileAgainstExplodedImage(String loc) throws IOException {
        System.err.println("testCanCompileAgainstExplodedImage(" + loc + ")");
        Path javaHome = prepareJavaHome();
        Path targetPath = javaHome.resolve(loc.replace("*", "/java.base").replace("/", sep));
        try (StandardJavaFileManager fm = javaCompiler.getStandardFileManager(null, null, null)) {
            for (String pack : REQUIRED_PACKAGES) {
                Iterable<JavaFileObject> content = fm.list(StandardLocation.PLATFORM_CLASS_PATH,
                                                           pack,
                                                           EnumSet.allOf(JavaFileObject.Kind.class),
                                                           false);

                for (JavaFileObject jfo : content) {
                    String name = jfo.getName();
                    int lastSlash = name.lastIndexOf('/');
                    name = lastSlash >= 0 ? name.substring(lastSlash + 1) : name;
                    Path target = targetPath.resolve(pack.replace(".", sep) + sep + name);
                    Files.createDirectories(target.getParent());
                    try (InputStream in = jfo.openInputStream()) {
                        Files.copy(in, target);
                    }
                }
            }
        }

        System.setProperty("java.home", javaHome.toString());

        try (StandardJavaFileManager fm = javaCompiler.getStandardFileManager(null, null, null)) {
            DiagnosticListener<JavaFileObject> noErrors = d -> {
                if (d.getKind() == Diagnostic.Kind.ERROR)
                    throw new IllegalStateException("Unexpected error: " + d);
            };
            ToolBox.JavaSource inputFile =
                    new ToolBox.JavaSource("import java.util.List; class Test { List l; }");
            List<JavaFileObject> inputFiles = Arrays.asList(inputFile);
            boolean result =
                    javaCompiler.getTask(null, fm, noErrors, null, null, inputFiles).call();
            if (!result) {
                throw new IllegalStateException("Could not compile correctly!");
            }
            JavacTask task =
                    (JavacTask) javaCompiler.getTask(null, fm, noErrors, null, null, inputFiles);
            task.parse();
            TypeElement juList = task.getElements().getTypeElement("java.util.List");
            if (juList == null)
                throw new IllegalStateException("Cannot resolve java.util.List!");
            URI listSource = ((ClassSymbol) juList).classfile.toUri();
            if (!listSource.toString().startsWith(javaHome.toUri().toString()))
                throw new IllegalStateException(  "Did not load java.util.List from correct place, " +
                                                  "actual location: " + listSource.toString() +
                                                "; expected prefix: " + javaHome.toUri());
        }

        System.err.println("finished.");
    }
    //where:
        static final String[] REQUIRED_PACKAGES = {"java.lang", "java.io", "java.util"};

    Path prepareJavaHome() throws IOException {
        Path javaHome = new File("javahome").getAbsoluteFile().toPath();
        delete(javaHome);
        Files.createDirectory(javaHome);
        return javaHome;
    }

    String sep = FileSystems.getDefault().getSeparator();
    JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
    String originalJavaHome = System.getProperty("java.home");

    void delete(Path p) throws IOException {
        if (!Files.exists(p))
            return ;
        if (Files.isDirectory(p)) {
            try (DirectoryStream<Path> dir = Files.newDirectoryStream(p)) {
                for (Path child : dir) {
                    delete(child);
                }
            }
        }
        Files.delete(p);
    }
}
