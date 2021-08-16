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

/*
 * @test
 * @bug 7026941
 * @summary path options ignored when reusing filemanager across tasks
 * @modules java.compiler
 *          jdk.compiler
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import static javax.tools.StandardLocation.*;

/**
 * Test for combinations of using javac command-line options and fileManager setLocation
 * calls to affect the locations available in the fileManager.
 *
 * Using a single Java compiler and file manager, for each of the standard locations,
 * a series of operations is performed, using either compiler options or setLocation
 * calls. Each operation includes a compilation, and then a check for the value of
 * the standard location available in the file manager.
 *
 * The operations generate and use unique files to minimize the possibility of false
 * positive results.
 */
public class TestSearchPaths {

    public static void main(String... args) throws Exception {
        TestSearchPaths t = new TestSearchPaths();
        t.run();
    }

    void run() throws Exception {
        compiler = ToolProvider.getSystemJavaCompiler();
        fileManager = compiler.getStandardFileManager(null, null, null);
        try {
            // basic output path
            testClassOutput();

            // basic search paths
            testClassPath();
            testSourcePath();
            testPlatformClassPath();

            // annotation processing
            testAnnotationProcessorPath();
            testSourceOutput();

            // javah equivalent
            testNativeHeaderOutput();

            // future-proof: guard against new StandardLocations being added
            if (!tested.equals(EnumSet.allOf(StandardLocation.class))) {
                // FIXME: need to update for JDK 9 locations
                // error("not all standard locations have been tested");
                out.println("not yet tested: " + EnumSet.complementOf(tested));
            }

            if (errors > 0) {
                throw new Exception(errors + " errors occurred");
            }
        } finally {
            fileManager.close();
        }
    }

    void testClassOutput() throws IOException {
        String test = "testClassOutput";
        System.err.println("test: " + test);

        for (int i = 1; i <= 5; i++) {
            File classes = createDir(test + "/" + i + "/classes");
            List<String> options;
            switch (i) {
                default:
                    options = getOptions("-d", classes.getPath());
                    break;

                case 3:
                    setLocation(CLASS_OUTPUT, classes);
                    options = null;
                    break;
            }
            List<JavaFileObject> sources = getSources("class C" + i + " { }");
            callTask(options, sources);
            checkPath(CLASS_OUTPUT, Mode.EQUALS, classes);
            checkFile(CLASS_OUTPUT, "C" + i + ".class");
        }

        tested.add(CLASS_OUTPUT);
    }

    void testClassPath() throws IOException {
        String test = "testClassPath";
        System.err.println("test: " + test);

        for (int i = 1; i <= 5; i++) {
            File classes = createDir(test + "/" + i + "/classes");
            File classpath = new File("testClassOutput/" + i + "/classes");
            List<String> options;
            switch (i) {
                default:
                    options = getOptions("-d", classes.getPath(), "-classpath", classpath.getPath());
                    break;

                case 3:
                    setLocation(CLASS_PATH, classpath);
                    options = getOptions("-d", classes.getPath());
                    break;

                case 4:
                    options = getOptions("-d", classes.getPath(), "-cp", classpath.getPath());
                    break;
            }
            List<JavaFileObject> sources = getSources("class D" + i + " { C" + i + " c; }");
            callTask(options, sources);
            checkPath(CLASS_PATH, Mode.EQUALS, classpath);
            checkFile(CLASS_OUTPUT, "D" + i + ".class");
        }

        tested.add(CLASS_PATH);
        System.err.println();
    }

    void testSourcePath() throws IOException {
        String test = "testSourcePath";
        System.err.println("test: " + test);
        setLocation(CLASS_PATH); // empty

        for (int i = 1; i <= 5; i++) {
            File src = createDir(test + "/" + i + "/src");
            writeFile(src, "C" + i + ".java", "class C" + i + "{ }");
            File classes = createDir(test + "/" + i + "/classes");
            File srcpath = src;
            List<String> options;
            switch (i) {
                default:
                    options = getOptions("-d", classes.getPath(), "-sourcepath", srcpath.getPath());
                    break;

                case 3:
                    setLocation(SOURCE_PATH, srcpath);
                    options = getOptions("-d", classes.getPath());
                    break;
            }
            List<JavaFileObject> sources = getSources("class D" + i + " { C" + i + " c; }");
            callTask(options, sources);
            checkPath(SOURCE_PATH, Mode.EQUALS, srcpath);
            checkFile(CLASS_OUTPUT, "D" + i + ".class");
        }

        tested.add(SOURCE_PATH);
        System.err.println();
    }

    void testPlatformClassPath() throws IOException {
        String test = "testPlatformClassPath";
        System.err.println("test: " + test);

        List<File> defaultPath = getLocation(PLATFORM_CLASS_PATH);
        StringBuilder sb = new StringBuilder();
        for (File f: defaultPath) {
            if (sb.length() > 0)
                sb.append(File.pathSeparator);
            sb.append(f);
        }
        String defaultPathString = sb.toString();

        setLocation(CLASS_PATH); // empty
        setLocation(SOURCE_PATH); // empty

        // Use -source 8 -target 8 to enable use of platform class path options
        // FIXME: temporarily exclude cases referring to default bootclasspath
        // for (int i = 1; i <= 10; i++) {
        int[] cases = new int[] { 1, 2, 4, 5, 6, 7 };
        for (int i : cases) {
            File classes = createDir(test + "/" + i + "/classes");
            File testJars = createDir(test + "/" + i + "/testJars");
            File testClasses = createDir(test + "/" + i + "/testClasses");
            callTask(getOptions("-d", testClasses.getPath()), getSources("class C" + i + " { }"));

            List<String> options;
            Mode mode;
            List<File> match;
            String reference = "C" + i + " c;";

            File jar;

            switch (i) {
                case 1:
                    options = getOptions("-d", classes.getPath(),
                        "-source", "8", "-target", "8",
                        "-Xbootclasspath/p:" + testClasses);
                    mode = Mode.STARTS_WITH;
                    match = Arrays.asList(testClasses);
                    break;

                case 2:
                    // the default values for -extdirs and -endorseddirs come after the bootclasspath;
                    // so to check -Xbootclasspath/a: we specify empty values for those options.
                    options = getOptions("-d", classes.getPath(),
                        "-source", "8", "-target", "8",
                        "-Xbootclasspath/a:" + testClasses,
                        "-extdirs", "",
                        "-endorseddirs", "");
                    mode = Mode.ENDS_WITH;
                    match = Arrays.asList(testClasses);
                    break;

                case 3:
                    options = getOptions("-d", classes.getPath(), "-Xbootclasspath:" + defaultPathString);
                    mode = Mode.EQUALS;
                    match = defaultPath;
                    reference = "";
                    break;

                case 4:
                    fileManager.setLocation(PLATFORM_CLASS_PATH, null);
                    jar = new File(testJars, "j" + i + ".jar");
                    writeJar(jar, testClasses, "C" + i + ".class");
                    options = getOptions("-d", classes.getPath(),
                        "-source", "8", "-target", "8",
                        "-endorseddirs", testJars.getPath());
                    mode = Mode.CONTAINS;
                    match = Arrays.asList(jar);
                    break;

                case 5:
                    fileManager.setLocation(PLATFORM_CLASS_PATH, null);
                    jar = new File(testJars, "j" + i + ".jar");
                    writeJar(jar, testClasses, "C" + i + ".class");
                    options = getOptions("-d", classes.getPath(),
                        "-source", "8", "-target", "8",
                        "-Djava.endorsed.dirs=" + testJars.getPath());
                    mode = Mode.CONTAINS;
                    match = Arrays.asList(jar);
                    break;

                case 6:
                    fileManager.setLocation(PLATFORM_CLASS_PATH, null);
                    jar = new File(testJars, "j" + i + ".jar");
                    writeJar(jar, testClasses, "C" + i + ".class");
                    options = getOptions("-d", classes.getPath(),
                        "-source", "8", "-target", "8",
                        "-extdirs", testJars.getPath());
                    mode = Mode.CONTAINS;
                    match = Arrays.asList(jar);
                    break;

                case 7:
                    fileManager.setLocation(PLATFORM_CLASS_PATH, null);
                    jar = new File(testJars, "j" + i + ".jar");
                    writeJar(jar, testClasses, "C" + i + ".class");
                    options = getOptions("-d", classes.getPath(),
                        "-source", "8", "-target", "8",
                        "-Djava.ext.dirs=" + testJars.getPath());
                    mode = Mode.CONTAINS;
                    match = Arrays.asList(jar);
                    break;

                case 8:
                    setLocation(PLATFORM_CLASS_PATH, defaultPath);
                    options = getOptions("-d", classes.getPath());
                    mode = Mode.EQUALS;
                    match = defaultPath;
                    reference = "";
                    break;

                default:
                    options = getOptions("-d", classes.getPath(),
                        "-source", "8", "-target", "8",
                        "-bootclasspath", defaultPathString);
                    mode = Mode.EQUALS;
                    match = defaultPath;
                    reference = "";
                    break;
            }
            List<JavaFileObject> sources = getSources("class D" + i + " { " + reference + " }");

            callTask(options, sources);
            checkPath(PLATFORM_CLASS_PATH, mode, match);
            checkFile(CLASS_OUTPUT, "D" + i + ".class");
        }

        tested.add(PLATFORM_CLASS_PATH);
        System.err.println();
    }

    void testAnnotationProcessorPath() throws IOException {
        String test = "testAnnotationProcessorPath";
        System.err.println("test: " + test);

        fileManager.setLocation(PLATFORM_CLASS_PATH, null);

        String template =
                "import java.util.*;\n"
                + "import javax.annotation.processing.*;\n"
                + "import javax.lang.model.*;\n"
                + "import javax.lang.model.element.*;\n"
                + "@SupportedAnnotationTypes(\"*\")\n"
                + "public class A%d extends AbstractProcessor {\n"
                + "    public boolean process(Set<? extends TypeElement> annos, RoundEnvironment rEnv) {\n"
                + "        return true;\n"
                + "    }\n"
                + "    public SourceVersion getSupportedSourceVersion() {\n"
                + "        return SourceVersion.latest();\n"
                + "    }\n"
                + "}";

        for (int i = 1; i <= 5; i++) {
            File classes = createDir(test + "/" + i + "/classes");
            File annodir = createDir(test + "/" + i + "/processors");
            callTask(getOptions("-d", annodir.getPath()), getSources(String.format(template, i)));
            File annopath = annodir;
            List<String> options;
            switch (i) {
                default:
                    options = getOptions("-d", classes.getPath(),
                            "-processorpath", annopath.getPath(),
                            "-processor", "A" + i);
                    break;

                case 3:
                    setLocation(ANNOTATION_PROCESSOR_PATH, annopath);
                    options = getOptions("-d", classes.getPath(),
                            "-processor", "A" + i);
                    break;
            }
            List<JavaFileObject> sources = getSources("class D" + i + " { }");
            callTask(options, sources);
            checkPath(ANNOTATION_PROCESSOR_PATH, Mode.EQUALS, annopath);
            checkFile(CLASS_OUTPUT, "D" + i + ".class");
        }

        tested.add(ANNOTATION_PROCESSOR_PATH);
        System.err.println();
    }

    void testSourceOutput() throws IOException {
        String test = "testAnnotationProcessorPath";
        System.err.println("test: " + test);

        String source =
                "import java.io.*;\n"
                + "import java.util.*;\n"
                + "import javax.annotation.processing.*;\n"
                + "import javax.lang.model.*;\n"
                + "import javax.lang.model.element.*;\n"
                + "import javax.tools.*;\n"
                + "@SupportedOptions(\"name\")\n"
                + "@SupportedAnnotationTypes(\"*\")\n"
                + "public class A extends AbstractProcessor {\n"
                + "    int round = 0;\n"
                + "    public boolean process(Set<? extends TypeElement> annos, RoundEnvironment rEnv) {\n"
                + "        if (round++ == 0) try {\n"
                + "            String name = processingEnv.getOptions().get(\"name\");\n"
                + "            JavaFileObject fo = processingEnv.getFiler().createSourceFile(name);\n"
                + "            try (Writer out = fo.openWriter()) {\n"
                + "                out.write(\"class \" + name + \" { }\");\n"
                + "            }\n"
                + "        } catch (IOException e) { throw new Error(e); }\n"
                + "        return true;\n"
                + "    }\n"
                + "    public SourceVersion getSupportedSourceVersion() {\n"
                + "        return SourceVersion.latest();\n"
                + "    }\n"
                + "}";

        File annodir = createDir(test + "/processors");
        callTask(getOptions("-d", annodir.getPath()), getSources(source));
        setLocation(ANNOTATION_PROCESSOR_PATH, annodir);

        for (int i = 1; i <= 5; i++) {
            File classes = createDir(test + "/" + i + "/classes");
            File genSrc = createDir(test + "/" + "/genSrc");
            List<String> options;
            switch (i) {
                default:
                    options = getOptions("-d", classes.getPath(),
                            "-processor", "A", "-Aname=G" + i,
                            "-s", genSrc.getPath());
                    break;

                case 3:
                    setLocation(SOURCE_OUTPUT, genSrc);
                    options = getOptions("-d", classes.getPath(),
                            "-processor", "A", "-Aname=G" + i);
                    break;
            }
            List<JavaFileObject> sources = getSources("class D" + i + " { }");
            callTask(options, sources);
            checkPath(SOURCE_OUTPUT, Mode.EQUALS, genSrc);
            checkFile(CLASS_OUTPUT, "D" + i + ".class");
            checkFile(CLASS_OUTPUT, "G" + i + ".class");
        }
        tested.add(SOURCE_OUTPUT);
        System.err.println();
    }

    void testNativeHeaderOutput() throws IOException {
        String test = "testNativeHeaderOutput";
        System.err.println("test: " + test);

        for (int i = 1; i <= 5; i++) {
            File classes = createDir(test + "/" + i + "/classes");
            File headers = createDir(test + "/" + i + "/hdrs");
            List<String> options;
            switch (i) {
                default:
                    options = getOptions("-d", classes.getPath(), "-h", headers.getPath());
                    break;

                case 3:
                    setLocation(NATIVE_HEADER_OUTPUT, headers);
                    options = getOptions("-d", classes.getPath());
                    break;
            }
            List<JavaFileObject> sources = getSources("class C" + i + " { native void m(); }");
            callTask(options, sources);
            checkPath(NATIVE_HEADER_OUTPUT, Mode.EQUALS, headers);
            checkFile(NATIVE_HEADER_OUTPUT, "C" + i + ".h");
        }

        tested.add(StandardLocation.NATIVE_HEADER_OUTPUT);
        System.err.println();
    }

    List<String> getOptions(String... args) {
        return Arrays.asList(args);
    }

    List<JavaFileObject> getSources(String... sources) {
        List<JavaFileObject> list = new ArrayList<>();
        for (String s: sources)
            list.add(getSource(s));
        return list;
    }

    JavaFileObject getSource(final String source) {
        return new SimpleJavaFileObject(getURIFromSource(source), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
    }

    void callTask(List<String> options, List<JavaFileObject> files) {
        out.print("compile: ");
        if (options != null) {
            for (String o: options) {
                if (o.length() > 64) {
                    o = o.substring(0, 32) + "..." + o.substring(o.length() - 32);
                }
                out.print(" " + o);
            }
        }
        for (JavaFileObject f: files)
            out.print(" " + f.getName());
        out.println();
        CompilationTask t = compiler.getTask(out, fileManager, null, options, null, files);
        boolean ok = t.call();
        if (!ok)
            error("compilation failed");
    }

    enum Mode { EQUALS, CONTAINS, STARTS_WITH, ENDS_WITH };

    void checkFile(StandardLocation l, String path) {
        if (!l.isOutputLocation()) {
            error("Not an output location: " + l);
            return;
        }

        List<File> files = getLocation(l);
        if (files == null) {
            error("location is unset: " + l);
            return;
        }

        if (files.size() != 1)
            error("unexpected number of entries on " + l + ": " + files.size());

        File f = new File(files.get(0), path);
        if (!f.exists())
            error("file not found: " + f);
    }

    void checkPath(StandardLocation l, Mode m, File expect) {
        checkPath(l, m, Arrays.asList(expect));
    }

    void checkPath(StandardLocation l, Mode m, List<File> expect) {
        List<File> files = getLocation(l);
        if (files == null) {
            error("location is unset: " + l);
            return;
        }

        switch (m) {
            case EQUALS:
                if (!Objects.equals(files, expect)) {
                    error("location does not match the expected files: " + l);
                    out.println("found:  " + files);
                    out.println("expect: " + expect);
                }
                break;

            case CONTAINS:
                int containsIndex = Collections.indexOfSubList(files, expect);
                if (containsIndex == -1) {
                    error("location does not contain the expected files: " + l);
                    out.println("found:  " + files);
                    out.println("expect: " + expect);
                }
            break;

            case STARTS_WITH:
                int startsIndex = Collections.indexOfSubList(files, expect);
                if (startsIndex != 0) {
                    error("location does not start with the expected files: " + l);
                    out.println("found:  " + files);
                    out.println("expect: " + expect);
                }
            break;

            case ENDS_WITH:
                int endsIndex = Collections.lastIndexOfSubList(files, expect);
                if (endsIndex != files.size() - expect.size()) {
                    error("location does not end with the expected files: " + l);
                    out.println("found:  " + files);
                    out.println("expect: " + expect);
                }
            break;

        }
    }

    List<File> getLocation(StandardLocation l) {
        Iterable<? extends File> iter = fileManager.getLocation(l);
        if (iter == null)
            return null;
        List<File> files = new ArrayList<>();
        for (File f: iter)
            files.add(f);
        return files;
    }

    void setLocation(StandardLocation l, File... files) throws IOException {
        fileManager.setLocation(l, Arrays.asList(files));
    }

    void setLocation(StandardLocation l, List<File> files) throws IOException {
        fileManager.setLocation(l, files);
    }

    void writeFile(File dir, String path, String body) throws IOException {
        try (FileWriter w = new FileWriter(new File(dir, path))) {
            w.write(body);
        }
    }

    void writeJar(File jar, File dir, String... entries) throws IOException {
        try (JarOutputStream j = new JarOutputStream(Files.newOutputStream(jar.toPath()))) {
            for (String entry: entries) {
                j.putNextEntry(new JarEntry(entry));
                j.write(Files.readAllBytes(dir.toPath().resolve(entry)));
            }
        }
    }

    private static final Pattern packagePattern
            = Pattern.compile("package\\s+(((?:\\w+\\.)*)(?:\\w+))");
    private static final Pattern classPattern
            = Pattern.compile("(?:public\\s+)?(?:class|enum|interface)\\s+(\\w+)");


    private static URI getURIFromSource(String source) {
        String packageName = null;

        Matcher matcher = packagePattern.matcher(source);
        if (matcher.find()) {
            packageName = matcher.group(1).replace(".", "/");
        }

        matcher = classPattern.matcher(source);
        if (matcher.find()) {
            String className = matcher.group(1);
            String path = ((packageName == null) ? "" : packageName + "/") + className + ".java";
            return URI.create("myfo:///" + path);
        } else {
            throw new Error("Could not extract the java class "
                    + "name from the provided source");
        }
    }

    File createDir(String path) {
        File dir = new File(path);
        dir.mkdirs();
        return dir;
    }

    JavaCompiler compiler;
    StandardJavaFileManager fileManager;

    /**
     * Map for recording which standard locations have been tested.
     */
    EnumSet<StandardLocation> tested = EnumSet.noneOf(StandardLocation.class);

    /**
     * Logging stream. Used directly with test and for getTask calls.
     */
    final PrintWriter out = new PrintWriter(System.err, true);

    /**
     * Count of errors so far.
     */
    int errors;

    void error(String message) {
        errors++;
        out.println("Error: " + message);
    }
}
