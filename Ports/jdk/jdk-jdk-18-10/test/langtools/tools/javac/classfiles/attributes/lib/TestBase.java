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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;

import toolbox.JavacTask;
import toolbox.ToolBox;

/**
 * Base class for class file attribute tests.
 * Contains methods for compiling generated sources in memory,
 * for reading files from disk and a lot of assert* methods.
 */
public class TestBase {

    public static final String LINE_SEPARATOR = System.lineSeparator();
    public static final boolean isDumpOfSourceEnabled = Boolean.getBoolean("dump.src");

    private <S> InMemoryFileManager compile(
            List<String> options,
            Function<S, ? extends JavaFileObject> src2JavaFileObject,
            List<S> sources)
            throws IOException, CompilationException {

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<? extends JavaFileObject> src = sources.stream()
                .map(src2JavaFileObject)
                .collect(Collectors.toList());

        DiagnosticCollector<? super JavaFileObject> dc = new DiagnosticCollector<>();
        try (InMemoryFileManager fileManager
                     = new InMemoryFileManager(compiler.getStandardFileManager(null, null, null))) {
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, dc, options, null, src);
            boolean success = task.call();
            if (!success) {
                String errorMessage = dc.getDiagnostics().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining("\n"));
                throw new CompilationException("Compilation Error\n\n" + errorMessage);
            }
            return fileManager;
        }
    }

    /**
     * Compiles sources in memory.
     *
     * @param sources to compile
     * @return in-memory file manager which contains class files and class loader
     */
    public InMemoryFileManager compile(String... sources)
            throws IOException, CompilationException {
        return compile(Collections.emptyList(), sources);
    }

    /**
     * Compiles sources in memory.
     *
     * @param options compiler options.
     * @param sources sources to compile.
     * @return in-memory file manager which contains class files and class loader.
     */
    public InMemoryFileManager compile(List<String> options, String... sources)
            throws IOException, CompilationException {
        return compile(options, ToolBox.JavaSource::new, Arrays.asList(sources));
    }

    /**
     * Compiles sources in memory.
     *
     * @param sources sources[i][0] - name of file, sources[i][1] - sources.
     * @return in-memory file manager which contains class files and class loader.
     */
    public InMemoryFileManager compile(String[]... sources) throws IOException,
            CompilationException {
        return compile(Collections.emptyList(), sources);
    }

    /**
     * Compiles sources in memory.
     *
     * @param options compiler options
     * @param sources sources[i][0] - name of file, sources[i][1] - sources.
     * @return in-memory file manager which contains class files and class loader.
     */
    public InMemoryFileManager compile(List<String> options, String[]... sources)
            throws IOException, CompilationException {
        return compile(options, src -> new ToolBox.JavaSource(src[0], src[1]), Arrays.asList(sources));
    }

    /**
     * Returns class file that is read from {@code is}.
     *
     * @param is an input stream
     * @return class file that is read from {@code is}
     * @throws IOException if I/O error occurs
     * @throws ConstantPoolException if constant pool error occurs
     */
    public ClassFile readClassFile(InputStream is) throws IOException, ConstantPoolException {
        return ClassFile.read(is);
    }

    /**
     * Returns class file that is read from {@code fileObject}.
     *
     * @param fileObject a file object
     * @return class file that is read from {@code fileObject}
     * @throws IOException if I/O error occurs
     * @throws ConstantPoolException if constant pool error occurs
     */
    public ClassFile readClassFile(JavaFileObject fileObject) throws IOException, ConstantPoolException {
        try (InputStream is = fileObject.openInputStream()) {
            return readClassFile(is);
        }
    }

    /**
     * Returns class file that corresponds to {@code clazz}.
     *
     * @param clazz a class
     * @return class file that is read from {@code clazz}
     * @throws IOException if I/O error occurs
     * @throws ConstantPoolException if constant pool error occurs
     */
    public ClassFile readClassFile(Class<?> clazz) throws IOException, ConstantPoolException {
        return readClassFile(getClassFile(clazz));
    }

    /**
     * Returns class file that corresponds to {@code className}.
     *
     * @param className a class name
     * @return class file that is read from {@code className}
     * @throws IOException if I/O error occurs
     * @throws ConstantPoolException if constant pool error occurs
     */
    public ClassFile readClassFile(String className) throws IOException, ConstantPoolException {
        return readClassFile(getClassFile(className + ".class"));
    }

    /**
     * Returns class file that is read from {@code file}.
     *
     * @param file a file
     * @return class file that is read from {@code file}
     * @throws IOException if I/O error occurs
     * @throws ConstantPoolException if constant pool error occurs
     */
    public ClassFile readClassFile(File file) throws IOException, ConstantPoolException {
        try (InputStream is = new FileInputStream(file)) {
            return readClassFile(is);
        }
    }

    public void assertEquals(Object actual, Object expected, String message) {
        if (!Objects.equals(actual, expected))
            throw new AssertionFailedException(String.format("%s%nGot: %s, Expected: %s",
                    message, actual, expected));
    }

    public void assertNull(Object actual, String message) {
        assertEquals(actual, null, message);
    }

    public void assertNotNull(Object actual, String message) {
        if (Objects.isNull(actual)) {
            throw new AssertionFailedException(message + " : Expected not null value");
        }
    }

    public void assertTrue(boolean actual, String message) {
        assertEquals(actual, true, message);
    }

    public void assertFalse(boolean actual, String message) {
        assertEquals(actual, false, message);
    }

    public void assertContains(Set<?> found, Set<?> expected, String message) {
        Set<?> copy = new HashSet<>(expected);
        copy.removeAll(found);
        assertTrue(found.containsAll(expected), message + " : " + copy);
    }

    public void writeToFile(Path path, String source) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(source);
        }
    }

    public void writeToFileIfEnabled(Path path, String source) throws IOException {
        if (isDumpOfSourceEnabled) {
            writeToFile(path, source);
        } else {
            System.err.println("Source dumping disabled. To enable, run the test with '-Ddump.src=true'");
        }
    }

    public File getSourceDir() {
        return new File(System.getProperty("test.src", "."));
    }

    public File getClassDir() {
        return new File(System.getProperty("test.classes", TestBase.class.getResource(".").getPath()));
    }

    public File getSourceFile(String fileName) {
        return new File(getSourceDir(), fileName);
    }

    public File getClassFile(String fileName) {
        return new File(getClassDir(), fileName);
    }

    public File getClassFile(Class clazz) {
        return getClassFile(clazz.getName().replace(".", "/") + ".class");
    }

    /**
     * Prints message to standard error. New lines are converted to system dependent NL.
     *
     * @param message string to print.
     */
    public void echo(String message) {
        printf(message + "\n");
    }

    /**
     * Substitutes args in template and prints result to standard error.
     * New lines are converted to system dependent NL.
     *
     * @param template template in standard String.format(...) format.
     * @param args arguments to substitute in template.
     */
    public void printf(String template, Object... args) {
        System.err.printf(String.format(template, args).replace("\n", LINE_SEPARATOR));
    }

    public static class CompilationException extends Exception {

        public CompilationException(String message) {
            super(message);
        }
    }

    public static class AssertionFailedException extends RuntimeException {
        public AssertionFailedException(String message) {
            super(message);
        }
    }
}
