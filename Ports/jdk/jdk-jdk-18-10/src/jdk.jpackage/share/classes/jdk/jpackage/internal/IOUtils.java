/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.jpackage.internal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import  java.util.concurrent.atomic.AtomicReference;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * IOUtils
 *
 * A collection of static utility methods.
 */
public class IOUtils {

    public static void deleteRecursive(Path directory) throws IOException {
        final AtomicReference<IOException> exception = new AtomicReference<>();

        if (!Files.exists(directory)) {
            return;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                            BasicFileAttributes attr) throws IOException {
                if (Platform.getPlatform() == Platform.WINDOWS) {
                    Files.setAttribute(file, "dos:readonly", false);
                }
                try {
                    Files.delete(file);
                } catch (IOException ex) {
                    exception.compareAndSet(null, ex);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                            BasicFileAttributes attr) throws IOException {
                if (Platform.getPlatform() == Platform.WINDOWS) {
                    Files.setAttribute(dir, "dos:readonly", false);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e)
                            throws IOException {
                try {
                    Files.delete(dir);
                } catch (IOException ex) {
                    exception.compareAndSet(null, ex);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        if (exception.get() != null) {
            throw exception.get();
        }
    }

    public static void copyRecursive(Path src, Path dest) throws IOException {
        copyRecursive(src, dest, List.of());
    }

    public static void copyRecursive(Path src, Path dest,
            final List<String> excludes) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir,
                    final BasicFileAttributes attrs) throws IOException {
                if (excludes.contains(dir.toFile().getName())) {
                    return FileVisitResult.SKIP_SUBTREE;
                } else {
                    Files.createDirectories(dest.resolve(src.relativize(dir)));
                    return FileVisitResult.CONTINUE;
                }
            }

            @Override
            public FileVisitResult visitFile(final Path file,
                    final BasicFileAttributes attrs) throws IOException {
                if (!excludes.contains(file.toFile().getName())) {
                    Files.copy(file, dest.resolve(src.relativize(file)));
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void copyFile(Path sourceFile, Path destFile)
            throws IOException {
        Files.createDirectories(getParent(destFile));

        Files.copy(sourceFile, destFile,
                   StandardCopyOption.REPLACE_EXISTING,
                   StandardCopyOption.COPY_ATTRIBUTES);
    }

    public static boolean exists(Path path) {
        if (path == null) {
            return false;
        }

        return Files.exists(path);
    }

    // run "launcher paramfile" in the directory where paramfile is kept
    public static void run(String launcher, Path paramFile)
            throws IOException {
        if (IOUtils.exists(paramFile)) {
            ProcessBuilder pb =
                    new ProcessBuilder(launcher,
                        getFileName(paramFile).toString());
            pb = pb.directory(getParent(paramFile).toFile());
            exec(pb);
        }
    }

    public static void exec(ProcessBuilder pb)
            throws IOException {
        exec(pb, false, null, false, Executor.INFINITE_TIMEOUT);
    }

    // timeout in seconds. -1 will be return if process timeouts.
    public static void exec(ProcessBuilder pb, long timeout)
            throws IOException {
        exec(pb, false, null, false, timeout);
    }

    // See JDK-8236282
    // Reading output from some processes (currently known "hdiutil attach")
    // might hang even if process already exited. Only possible workaround found
    // in "hdiutil attach" case is to redirect the output to a temp file and then
    // read this file back.
    public static void exec(ProcessBuilder pb, boolean writeOutputToFile)
            throws IOException {
        exec(pb, false, null, writeOutputToFile, Executor.INFINITE_TIMEOUT);
    }

    static void exec(ProcessBuilder pb, boolean testForPresenceOnly,
            PrintStream consumer) throws IOException {
        exec(pb, testForPresenceOnly, consumer, false, Executor.INFINITE_TIMEOUT);
    }

    static void exec(ProcessBuilder pb, boolean testForPresenceOnly,
            PrintStream consumer, boolean writeOutputToFile, long timeout)
            throws IOException {
        List<String> output = new ArrayList<>();
        Executor exec = Executor.of(pb).setWriteOutputToFile(writeOutputToFile)
                .setTimeout(timeout).setOutputConsumer(lines -> {
            lines.forEach(output::add);
            if (consumer != null) {
                output.forEach(consumer::println);
            }
        });

        if (testForPresenceOnly) {
            exec.execute();
        } else {
            exec.executeExpectSuccess();
        }
    }

    public static int getProcessOutput(List<String> result, String... args)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(args);

        final Process p = pb.start();

        List<String> list = new ArrayList<>();

        final BufferedReader in =
                new BufferedReader(new InputStreamReader(p.getInputStream()));
        final BufferedReader err =
                new BufferedReader(new InputStreamReader(p.getErrorStream()));

        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    list.add(line);
                }
            } catch (IOException ioe) {
                Log.verbose(ioe);
            }

            try {
                String line;
                while ((line = err.readLine()) != null) {
                    Log.error(line);
                }
            } catch (IOException ioe) {
                  Log.verbose(ioe);
            }
        });
        t.setDaemon(true);
        t.start();

        int ret = p.waitFor();
        Log.verbose(pb.command(), list, ret, IOUtils.getPID(p));

        result.clear();
        result.addAll(list);

        return ret;
    }

    static void writableOutputDir(Path outdir) throws PackagerException {
        if (!Files.isDirectory(outdir)) {
            try {
                Files.createDirectories(outdir);
            } catch (IOException ex) {
                throw new PackagerException("error.cannot-create-output-dir",
                    outdir.toAbsolutePath().toString());
            }
        }

        if (!Files.isWritable(outdir)) {
            throw new PackagerException("error.cannot-write-to-output-dir",
                    outdir.toAbsolutePath().toString());
        }
    }

    public static Path replaceSuffix(Path path, String suffix) {
        Path parent = path.getParent();
        String filename = getFileName(path).toString().replaceAll("\\.[^.]*$", "")
                + Optional.ofNullable(suffix).orElse("");
        return parent != null ? parent.resolve(filename) : Path.of(filename);
    }

    public static Path addSuffix(Path path, String suffix) {
        Path parent = path.getParent();
        String filename = getFileName(path).toString() + suffix;
        return parent != null ? parent.resolve(filename) : Path.of(filename);
    }

    public static String getSuffix(Path path) {
        String filename = replaceSuffix(getFileName(path), null).toString();
        return getFileName(path).toString().substring(filename.length());
    }

    @FunctionalInterface
    public static interface XmlConsumer {
        void accept(XMLStreamWriter xml) throws IOException, XMLStreamException;
    }

    public static void createXml(Path dstFile, XmlConsumer xmlConsumer) throws
            IOException {
        XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
        Files.createDirectories(getParent(dstFile));
        try (Writer w = Files.newBufferedWriter(dstFile)) {
            // Wrap with pretty print proxy
            XMLStreamWriter xml = (XMLStreamWriter) Proxy.newProxyInstance(
                    XMLStreamWriter.class.getClassLoader(), new Class<?>[]{
                XMLStreamWriter.class}, new PrettyPrintHandler(
                    xmlFactory.createXMLStreamWriter(w)));

            xml.writeStartDocument();
            xmlConsumer.accept(xml);
            xml.writeEndDocument();
            xml.flush();
            xml.close();
        } catch (XMLStreamException ex) {
            throw new IOException(ex);
        } catch (IOException ex) {
            throw ex;
        }
    }

    public static Path getParent(Path p) {
        Path parent = p.getParent();
        if (parent == null) {
            IllegalArgumentException iae =
                    new IllegalArgumentException(p.toString());
            Log.verbose(iae);
            throw iae;
        }
        return parent;
    }

    public static Path getFileName(Path p) {
        Path filename = p.getFileName();
        if (filename == null) {
            IllegalArgumentException iae =
                    new IllegalArgumentException(p.toString());
            Log.verbose(iae);
            throw iae;
        }
        return filename;
    }

    public static long getPID(Process p) {
        try {
            return p.pid();
        } catch (UnsupportedOperationException ex) {
            Log.verbose(ex); // Just log exception and ignore it. This method
                             // is used for verbose output, so not a problem
                             // if unsupported.
            return -1;
        }
    }

    private static class PrettyPrintHandler implements InvocationHandler {

        PrettyPrintHandler(XMLStreamWriter target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws
                Throwable {
            switch (method.getName()) {
                case "writeStartElement":
                    // update state of parent node
                    if (depth > 0) {
                        hasChildElement.put(depth - 1, true);
                    }
                    // reset state of current node
                    hasChildElement.put(depth, false);
                    // indent for current depth
                    target.writeCharacters(EOL);
                    target.writeCharacters(repeat(depth, INDENT));
                    depth++;
                    break;
                case "writeEndElement":
                    depth--;
                    if (hasChildElement.get(depth) == true) {
                        target.writeCharacters(EOL);
                        target.writeCharacters(repeat(depth, INDENT));
                    }
                    break;
                case "writeProcessingInstruction":
                case "writeEmptyElement":
                    // update state of parent node
                    if (depth > 0) {
                        hasChildElement.put(depth - 1, true);
                    }
                    // indent for current depth
                    target.writeCharacters(EOL);
                    target.writeCharacters(repeat(depth, INDENT));
                    break;
                default:
                    break;
            }
            method.invoke(target, args);
            return null;
        }

        private static String repeat(int d, String s) {
            StringBuilder sb = new StringBuilder();
            while (d-- > 0) {
                sb.append(s);
            }
            return sb.toString();
        }

        private final XMLStreamWriter target;
        private int depth = 0;
        private final Map<Integer, Boolean> hasChildElement = new HashMap<>();
        private static final String INDENT = "  ";
        private static final String EOL = "\n";
    }
}
