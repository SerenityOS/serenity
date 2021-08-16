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

package toolbox;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.JavaFileManager.Location;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Utility methods and classes for writing jtreg tests for
 * javac, javah, javap, and sjavac. (For javadoc support,
 * see JavadocTester.)
 *
 * <p>There is support for common file operations similar to
 * shell commands like cat, cp, diff, mv, rm, grep.
 *
 * <p>There is also support for invoking various tools, like
 * javac, javah, javap, jar, java and other JDK tools.
 *
 * <p><em>File separators</em>: for convenience, many operations accept strings
 * to represent filenames. On all platforms on which JDK is supported,
 * "/" is a legal filename component separator. In particular, even
 * on Windows, where the official file separator is "\", "/" is a legal
 * alternative. It is therefore recommended that any client code using
 * strings to specify filenames should use "/".
 *
 * @author Vicente Romero (original)
 * @author Jonathan Gibbons (revised)
 */
public class ToolBox {
    /** The platform line separator. */
    public static final String lineSeparator = System.getProperty("line.separator");
    /** The platform OS name. */
    public static final String osName = System.getProperty("os.name");

    /** The location of the class files for this test, or null if not set. */
    public static final String testClasses = System.getProperty("test.classes");
    /** The location of the source files for this test, or null if not set. */
    public static final String testSrc = System.getProperty("test.src");
    /** The location of the test JDK for this test, or null if not set. */
    public static final String testJDK = System.getProperty("test.jdk");
    /** The timeout factor for slow systems. */
    public static final float timeoutFactor;
    static {
        String ttf = System.getProperty("test.timeout.factor");
        timeoutFactor = (ttf == null) ? 1.0f : Float.valueOf(ttf);
    }

    /** The current directory. */
    public static final Path currDir = Paths.get(".");

    /** The stream used for logging output. */
    public PrintStream out = System.err;

    /**
     * Checks if the host OS is some version of Windows.
     * @return true if the host OS is some version of Windows
     */
    public static boolean isWindows() {
        return osName.toLowerCase(Locale.ENGLISH).startsWith("windows");
    }

    /**
     * Splits a string around matches of the given regular expression.
     * If the string is empty, an empty list will be returned.
     * @param text the string to be split
     * @param sep  the delimiting regular expression
     * @return the strings between the separators
     */
    public List<String> split(String text, String sep) {
        if (text.isEmpty())
            return Collections.emptyList();
        return Arrays.asList(text.split(sep));
    }

    /**
     * Checks if two lists of strings are equal.
     * @param l1 the first list of strings to be compared
     * @param l2 the second list of strings to be compared
     * @throws Error if the lists are not equal
     */
    public void checkEqual(List<String> l1, List<String> l2) throws Error {
        if (!Objects.equals(l1, l2)) {
            // l1 and l2 cannot both be null
            if (l1 == null)
                throw new Error("comparison failed: l1 is null");
            if (l2 == null)
                throw new Error("comparison failed: l2 is null");
            // report first difference
            for (int i = 0; i < Math.min(l1.size(), l2.size()); i++) {
                String s1 = l1.get(i);
                String s2 = l2.get(i);
                if (!Objects.equals(s1, s2)) {
                    throw new Error("comparison failed, index " + i +
                            ", (" + s1 + ":" + s2 + ")");
                }
            }
            throw new Error("comparison failed: l1.size=" + l1.size() + ", l2.size=" + l2.size());
        }
    }

    /**
     * Filters a list of strings according to the given regular expression,
     * returning the strings that match the regular expression.
     * @param regex the regular expression
     * @param lines the strings to be filtered
     * @return the strings matching the regular expression
     */
    public List<String> grep(String regex, List<String> lines) {
        return grep(Pattern.compile(regex), lines, true);
    }

    /**
     * Filters a list of strings according to the given regular expression,
     * returning the strings that match the regular expression.
     * @param pattern the regular expression
     * @param lines the strings to be filtered
     * @return the strings matching the regular expression
     */
    public List<String> grep(Pattern pattern, List<String> lines) {
        return grep(pattern, lines, true);
    }

    /**
     * Filters a list of strings according to the given regular expression,
     * returning either the strings that match or the strings that do not match.
     * @param regex the regular expression
     * @param lines the strings to be filtered
     * @param match if true, return the lines that match; otherwise if false, return the lines that do not match.
     * @return the strings matching(or not matching) the regular expression
     */
    public List<String> grep(String regex, List<String> lines, boolean match) {
        return grep(Pattern.compile(regex), lines, match);
    }

    /**
     * Filters a list of strings according to the given regular expression,
     * returning either the strings that match or the strings that do not match.
     * @param pattern the regular expression
     * @param lines the strings to be filtered
     * @param match if true, return the lines that match; otherwise if false, return the lines that do not match.
     * @return the strings matching(or not matching) the regular expression
     */
    public List<String> grep(Pattern pattern, List<String> lines, boolean match) {
        return lines.stream()
                .filter(s -> pattern.matcher(s).find() == match)
                .collect(Collectors.toList());
    }

    /**
     * Copies a file.
     * If the given destination exists and is a directory, the copy is created
     * in that directory.  Otherwise, the copy will be placed at the destination,
     * possibly overwriting any existing file.
     * <p>Similar to the shell "cp" command: {@code cp from to}.
     * @param from the file to be copied
     * @param to where to copy the file
     * @throws IOException if any error occurred while copying the file
     */
    public void copyFile(String from, String to) throws IOException {
        copyFile(Paths.get(from), Paths.get(to));
    }

    /**
     * Copies a file.
     * If the given destination exists and is a directory, the copy is created
     * in that directory.  Otherwise, the copy will be placed at the destination,
     * possibly overwriting any existing file.
     * <p>Similar to the shell "cp" command: {@code cp from to}.
     * @param from the file to be copied
     * @param to where to copy the file
     * @throws IOException if an error occurred while copying the file
     */
    public void copyFile(Path from, Path to) throws IOException {
        if (Files.isDirectory(to)) {
            to = to.resolve(from.getFileName());
        } else {
            Files.createDirectories(to.getParent());
        }
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Creates one of more directories.
     * For each of the series of paths, a directory will be created,
     * including any necessary parent directories.
     * <p>Similar to the shell command: {@code mkdir -p paths}.
     * @param paths the directories to be created
     * @throws IOException if an error occurred while creating the directories
     */
    public void createDirectories(String... paths) throws IOException {
        if (paths.length == 0)
            throw new IllegalArgumentException("no directories specified");
        for (String p : paths)
            Files.createDirectories(Paths.get(p));
    }

    /**
     * Creates one or more directories.
     * For each of the series of paths, a directory will be created,
     * including any necessary parent directories.
     * <p>Similar to the shell command: {@code mkdir -p paths}.
     * @param paths the directories to be created
     * @throws IOException if an error occurred while creating the directories
     */
    public void createDirectories(Path... paths) throws IOException {
        if (paths.length == 0)
            throw new IllegalArgumentException("no directories specified");
        for (Path p : paths)
            Files.createDirectories(p);
    }

    /**
     * Deletes one or more files, awaiting confirmation that the files
     * no longer exist. Any directories to be deleted must be empty.
     * <p>Similar to the shell command: {@code rm files}.
     * @param files the names of the files to be deleted
     * @throws IOException if an error occurred while deleting the files
     */
    public void deleteFiles(String... files) throws IOException {
        deleteFiles(List.of(files).stream().map(Paths::get).collect(Collectors.toList()));
    }

    /**
     * Deletes one or more files, awaiting confirmation that the files
     * no longer exist. Any directories to be deleted must be empty.
     * <p>Similar to the shell command: {@code rm files}.
     * @param paths the paths for the files to be deleted
     * @throws IOException if an error occurred while deleting the files
     */
    public void deleteFiles(Path... paths) throws IOException {
        deleteFiles(List.of(paths));
    }

    /**
     * Deletes one or more files, awaiting confirmation that the files
     * no longer exist. Any directories to be deleted must be empty.
     * <p>Similar to the shell command: {@code rm files}.
     * @param paths the paths for the files to be deleted
     * @throws IOException if an error occurred while deleting the files
     */
    public void deleteFiles(List<Path> paths) throws IOException {
        if (paths.isEmpty())
            throw new IllegalArgumentException("no files specified");
        IOException ioe = null;
        for (Path path : paths) {
            ioe = deleteFile(path, ioe);
        }
        if (ioe != null) {
            throw ioe;
        }
        ensureDeleted(paths);
    }

    /**
     * Deletes all content of a directory (but not the directory itself),
     * awaiting confirmation that the content has been deleted.
     * @param root the directory to be cleaned
     * @throws IOException if an error occurs while cleaning the directory
     */
    public void cleanDirectory(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IOException(root + " is not a directory");
        }
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            private IOException ioe = null;
            // for each directory we visit, maintain a list of the files that we try to delete
            private Deque<List<Path>> dirFiles = new LinkedList<>();

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes a) throws IOException {
                ioe = deleteFile(file, ioe);
                dirFiles.peekFirst().add(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes a) throws IOException {
                if (!dir.equals(root)) {
                    dirFiles.peekFirst().add(dir);
                }
                dirFiles.addFirst(new ArrayList<>());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                if (e != null) {
                    throw e;
                }
                if (ioe != null) {
                    throw ioe;
                }
                ensureDeleted(dirFiles.removeFirst());
                if (!dir.equals(root)) {
                    ioe = deleteFile(dir, ioe);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Internal method to delete a file, using {@code Files.delete}.
     * It does not wait to confirm deletion, nor does it retry.
     * If an exception occurs it is either returned or added to the set of
     * suppressed exceptions for an earlier exception.
     * @param path the path for the file to be deleted
     * @param ioe the earlier exception, or null
     * @return the earlier exception or an exception that occurred while
     *  trying to delete the file
     */
    private IOException deleteFile(Path path, IOException ioe) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            if (ioe == null) {
                ioe = e;
            } else {
                ioe.addSuppressed(e);
            }
        }
        return ioe;
    }

    /**
     * Wait until it is confirmed that a set of files have been deleted.
     * @param paths the paths for the files to be deleted
     * @throws IOException if a file has not been deleted
     */
    private void ensureDeleted(Collection<Path> paths)
            throws IOException {
        for (Path path : paths) {
            ensureDeleted(path);
        }
    }

    /**
     * Wait until it is confirmed that a file has been deleted.
     * @param path the path for the file to be deleted
     * @throws IOException if problems occur while deleting the file
     */
    private void ensureDeleted(Path path) throws IOException {
        long startTime = System.currentTimeMillis();
        do {
            // Note: Files.notExists is not the same as !Files.exists
            if (Files.notExists(path)) {
                return;
            }
            System.gc(); // allow finalizers and cleaners to run
            try {
                Thread.sleep(RETRY_DELETE_MILLIS);
            } catch (InterruptedException e) {
                throw new IOException("Interrupted while waiting for file to be deleted: " + path, e);
            }
        } while ((System.currentTimeMillis() - startTime) <= MAX_RETRY_DELETE_MILLIS);

        throw new IOException("File not deleted: " + path);
    }

    private static final int RETRY_DELETE_MILLIS = isWindows() ? (int)(500 * timeoutFactor): 0;
    private static final int MAX_RETRY_DELETE_MILLIS = isWindows() ? (int)(15 * 1000 * timeoutFactor) : 0;

    /**
     * Moves a file.
     * If the given destination exists and is a directory, the file will be moved
     * to that directory.  Otherwise, the file will be moved to the destination,
     * possibly overwriting any existing file.
     * <p>Similar to the shell "mv" command: {@code mv from to}.
     * @param from the file to be moved
     * @param to where to move the file
     * @throws IOException if an error occurred while moving the file
     */
    public void moveFile(String from, String to) throws IOException {
        moveFile(Paths.get(from), Paths.get(to));
    }

    /**
     * Moves a file.
     * If the given destination exists and is a directory, the file will be moved
     * to that directory.  Otherwise, the file will be moved to the destination,
     * possibly overwriting any existing file.
     * <p>Similar to the shell "mv" command: {@code mv from to}.
     * @param from the file to be moved
     * @param to where to move the file
     * @throws IOException if an error occurred while moving the file
     */
    public void moveFile(Path from, Path to) throws IOException {
        if (Files.isDirectory(to)) {
            to = to.resolve(from.getFileName());
        } else {
            Files.createDirectories(to.getParent());
        }
        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Reads the lines of a file.
     * The file is read using the default character encoding.
     * @param path the file to be read
     * @return the lines of the file
     * @throws IOException if an error occurred while reading the file
     */
    public List<String> readAllLines(String path) throws IOException {
        return readAllLines(path, null);
    }

    /**
     * Reads the lines of a file.
     * The file is read using the default character encoding.
     * @param path the file to be read
     * @return the lines of the file
     * @throws IOException if an error occurred while reading the file
     */
    public List<String> readAllLines(Path path) throws IOException {
        return readAllLines(path, null);
    }

    /**
     * Reads the lines of a file using the given encoding.
     * @param path the file to be read
     * @param encoding the encoding to be used to read the file
     * @return the lines of the file.
     * @throws IOException if an error occurred while reading the file
     */
    public List<String> readAllLines(String path, String encoding) throws IOException {
        return readAllLines(Paths.get(path), encoding);
    }

    /**
     * Reads the lines of a file using the given encoding.
     * @param path the file to be read
     * @param encoding the encoding to be used to read the file
     * @return the lines of the file
     * @throws IOException if an error occurred while reading the file
     */
    public List<String> readAllLines(Path path, String encoding) throws IOException {
        return Files.readAllLines(path, getCharset(encoding));
    }

    private Charset getCharset(String encoding) {
        return (encoding == null) ? Charset.defaultCharset() : Charset.forName(encoding);
    }

    /**
     * Find .java files in one or more directories.
     * <p>Similar to the shell "find" command: {@code find paths -name \*.java}.
     * @param paths the directories in which to search for .java files
     * @return the .java files found
     * @throws IOException if an error occurred while searching for files
     */
    public Path[] findJavaFiles(Path... paths) throws IOException {
        return findFiles(".java", paths);
    }

    /**
     * Find files matching the file extension, in one or more directories.
     * <p>Similar to the shell "find" command: {@code find paths -name \*.ext}.
     * @param fileExtension the extension to search for
     * @param paths the directories in which to search for files
     * @return the files matching the file extension
     * @throws IOException if an error occurred while searching for files
     */
    public Path[] findFiles(String fileExtension, Path... paths) throws IOException {
        Set<Path> files = new TreeSet<>();  // use TreeSet to force a consistent order
        for (Path p : paths) {
            Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    if (file.getFileName().toString().endsWith(fileExtension)) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return files.toArray(new Path[files.size()]);
    }

    /**
     * Writes a file containing the given content.
     * Any necessary directories for the file will be created.
     * @param path where to write the file
     * @param content the content for the file
     * @throws IOException if an error occurred while writing the file
     */
    public void writeFile(String path, String content) throws IOException {
        writeFile(Paths.get(path), content);
    }

    /**
     * Writes a file containing the given content.
     * Any necessary directories for the file will be created.
     * @param path where to write the file
     * @param content the content for the file
     * @throws IOException if an error occurred while writing the file
     */
    public void writeFile(Path path, String content) throws IOException {
        Path dir = path.getParent();
        if (dir != null)
            Files.createDirectories(dir);
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write(content);
        }
    }

    /**
     * Writes one or more files containing Java source code.
     * For each file to be written, the filename will be inferred from the
     * given base directory, the package declaration (if present) and from the
     * the name of the first class, interface or enum declared in the file.
     * <p>For example, if the base directory is /my/dir/ and the content
     * contains "package p; class C { }", the file will be written to
     * /my/dir/p/C.java.
     * <p>Note: the content is analyzed using regular expressions;
     * errors can occur if any contents have initial comments that might trip
     * up the analysis.
     * @param dir the base directory
     * @param contents the contents of the files to be written
     * @throws IOException if an error occurred while writing any of the files.
     */
    public void writeJavaFiles(Path dir, String... contents) throws IOException {
        if (contents.length == 0)
            throw new IllegalArgumentException("no content specified for any files");
        for (String c : contents) {
            new JavaSource(c).write(dir);
        }
    }

    /**
     * Returns the path for the binary of a JDK tool within {@link testJDK}.
     * @param tool the name of the tool
     * @return the path of the tool
     */
    public Path getJDKTool(String tool) {
        return Paths.get(testJDK, "bin", tool);
    }

    /**
     * Returns a string representing the contents of an {@code Iterable} as a list.
     * @param <T> the type parameter of the {@code Iterable}
     * @param items the iterable
     * @return the string
     */
    <T> String toString(Iterable<T> items) {
        return StreamSupport.stream(items.spliterator(), false)
                .map(Objects::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }


    /**
     * An in-memory Java source file.
     * It is able to extract the file name from simple source text using
     * regular expressions.
     */
    public static class JavaSource extends SimpleJavaFileObject {
        private final String source;

        /**
         * Creates a in-memory file object for Java source code.
         * @param className the name of the class
         * @param source the source text
         */
        public JavaSource(String className, String source) {
            super(URI.create(className), JavaFileObject.Kind.SOURCE);
            this.source = source;
        }

        /**
         * Creates a in-memory file object for Java source code.
         * The name of the class will be inferred from the source code.
         * @param source the source text
         */
        public JavaSource(String source) {
            super(URI.create(getJavaFileNameFromSource(source)),
                    JavaFileObject.Kind.SOURCE);
            this.source = source;
        }

        /**
         * Writes the source code to a file in the current directory.
         * @throws IOException if there is a problem writing the file
         */
        public void write() throws IOException {
            write(currDir);
        }

        /**
         * Writes the source code to a file in a specified directory.
         * @param dir the directory
         * @throws IOException if there is a problem writing the file
         */
        public void write(Path dir) throws IOException {
            Path file = dir.resolve(getJavaFileNameFromSource(source));
            Files.createDirectories(file.getParent());
            try (BufferedWriter out = Files.newBufferedWriter(file)) {
                out.write(source.replace("\n", lineSeparator));
            }
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }

        private static Pattern commentPattern =
                Pattern.compile("(?s)(\\s+//.*?\n|/\\*.*?\\*/)");
        private static Pattern modulePattern =
                Pattern.compile("module\\s+((?:\\w+\\.)*)");
        private static Pattern packagePattern =
                Pattern.compile("package\\s+(((?:\\w+\\.)*)(?:\\w+))");
        private static Pattern classPattern =
                Pattern.compile("(?:public\\s+)?(?:class|enum|interface|record)\\s+(\\w+)");

        /**
         * Extracts the Java file name from the class declaration.
         * This method is intended for simple files and uses regular expressions.
         * Comments in the source are stripped before looking for the
         * declarations from which the name is derived.
         */
        static String getJavaFileNameFromSource(String source) {
            StringBuilder sb = new StringBuilder();
            Matcher matcher = commentPattern.matcher(source);
            int start = 0;
            while (matcher.find()) {
                sb.append(source.substring(start, matcher.start()));
                start = matcher.end();
            }
            sb.append(source.substring(start));
            source = sb.toString();

            String packageName = null;

            matcher = modulePattern.matcher(source);
            if (matcher.find())
                return "module-info.java";

            matcher = packagePattern.matcher(source);
            if (matcher.find()) {
                packageName = matcher.group(1).replace(".", "/");
                validateName(packageName);
            }

            matcher = classPattern.matcher(source);
            if (matcher.find()) {
                String className = matcher.group(1) + ".java";
                validateName(className);
                return (packageName == null) ? className : packageName + "/" + className;
            } else if (packageName != null) {
                return packageName + "/package-info.java";
            } else {
                throw new Error("Could not extract the java class " +
                        "name from the provided source");
            }
        }
    }

    /**
     * Extracts the Java file name from the class declaration.
     * This method is intended for simple files and uses regular expressions,
     * so comments matching the pattern can make the method fail.
     * @deprecated This is a legacy method for compatibility with ToolBox v1.
     *      Use {@link JavaSource#getName JavaSource.getName} instead.
     * @param source the source text
     * @return the Java file name inferred from the source
     */
    @Deprecated
    public static String getJavaFileNameFromSource(String source) {
        return JavaSource.getJavaFileNameFromSource(source);
    }

    private static final Set<String> RESERVED_NAMES = Set.of(
        "con", "prn", "aux", "nul",
        "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8",  "com9",
        "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8",  "lpt9"
    );

    /**Validate if a given name is a valid file name
     * or path name on known platforms.
     */
    public static void validateName(String name) {
        for (String part : name.split("\\.|/|\\\\")) {
            if (RESERVED_NAMES.contains(part.toLowerCase(Locale.US))) {
                throw new IllegalArgumentException("Name: " + name + " is" +
                                                   "a reserved name on Windows, " +
                                                   "and will not work!");
            }
        }
    }
    /**
     * A memory file manager, for saving generated files in memory.
     * The file manager delegates to a separate file manager for listing and
     * reading input files.
     */
    public static class MemoryFileManager extends ForwardingJavaFileManager {
        private interface Content {
            byte[] getBytes();
            String getString();
        }

        /**
         * Maps binary class names to generated content.
         */
        private final Map<Location, Map<String, Content>> files;

        /**
         * Construct a memory file manager which stores output files in memory,
         * and delegates to a default file manager for input files.
         */
        public MemoryFileManager() {
            this(ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null));
        }

        /**
         * Construct a memory file manager which stores output files in memory,
         * and delegates to a specified file manager for input files.
         * @param fileManager the file manager to be used for input files
         */
        public MemoryFileManager(JavaFileManager fileManager) {
            super(fileManager);
            files = new HashMap<>();
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location,
                                                   String name,
                                                   JavaFileObject.Kind kind,
                                                   FileObject sibling)
        {
            return new MemoryFileObject(location, name, kind);
        }

        /**
         * Returns the set of names of files that have been written to a given
         * location.
         * @param location the location
         * @return the set of file names
         */
        public Set<String> getFileNames(Location location) {
            Map<String, Content> filesForLocation = files.get(location);
            return (filesForLocation == null)
                ? Collections.emptySet() : filesForLocation.keySet();
        }

        /**
         * Returns the content written to a file in a given location,
         * or null if no such file has been written.
         * @param location the location
         * @param name the name of the file
         * @return the content as an array of bytes
         */
        public byte[] getFileBytes(Location location, String name) {
            Content content = getFile(location, name);
            return (content == null) ? null : content.getBytes();
        }

        /**
         * Returns the content written to a file in a given location,
         * or null if no such file has been written.
         * @param location the location
         * @param name the name of the file
         * @return the content as a string
         */
        public String getFileString(Location location, String name) {
            Content content = getFile(location, name);
            return (content == null) ? null : content.getString();
        }

        private Content getFile(Location location, String name) {
            Map<String, Content> filesForLocation = files.get(location);
            return (filesForLocation == null) ? null : filesForLocation.get(name);
        }

        private void save(Location location, String name, Content content) {
            Map<String, Content> filesForLocation = files.get(location);
            if (filesForLocation == null)
                files.put(location, filesForLocation = new HashMap<>());
            filesForLocation.put(name, content);
        }

        /**
         * A writable file object stored in memory.
         */
        private class MemoryFileObject extends SimpleJavaFileObject {
            private final Location location;
            private final String name;

            /**
             * Constructs a memory file object.
             * @param name binary name of the class to be stored in this file object
             */
            MemoryFileObject(Location location, String name, JavaFileObject.Kind kind) {
                super(URI.create("mfm:///" + name.replace('.','/') + kind.extension),
                      Kind.CLASS);
                this.location = location;
                this.name = name;
            }

            @Override
            public OutputStream openOutputStream() {
                return new FilterOutputStream(new ByteArrayOutputStream()) {
                    @Override
                    public void close() throws IOException {
                        out.close();
                        byte[] bytes = ((ByteArrayOutputStream) out).toByteArray();
                        save(location, name, new Content() {
                            @Override
                            public byte[] getBytes() {
                                return bytes;
                            }
                            @Override
                            public String getString() {
                                return new String(bytes);
                            }

                        });
                    }
                };
            }

            @Override
            public Writer openWriter() {
                return new FilterWriter(new StringWriter()) {
                    @Override
                    public void close() throws IOException {
                        out.close();
                        String text = ((StringWriter) out).toString();
                        save(location, name, new Content() {
                            @Override
                            public byte[] getBytes() {
                                return text.getBytes();
                            }
                            @Override
                            public String getString() {
                                return text;
                            }

                        });
                    }
                };
            }
        }
    }
}

