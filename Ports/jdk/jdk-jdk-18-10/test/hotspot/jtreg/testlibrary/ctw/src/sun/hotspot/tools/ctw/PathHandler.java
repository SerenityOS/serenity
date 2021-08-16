/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.hotspot.tools.ctw;

import java.io.Closeable;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handler for a path, responsible for processing classes in the path.
 */
public class PathHandler implements Closeable {
    public static abstract class PathEntry implements Closeable {
        private final ClassLoader loader = new PathEntryClassLoader(this::findByteCode);

        /**
         * returns bytecode for the class
         * @param name binary name of the class
         * @return bytecode of the class or null if handler does not have any
         * code for this name
         */
        protected abstract byte[] findByteCode(String name);

        protected final Path root;

        /**
         * @param root path entry root
         * @throws NullPointerException if {@code root} is {@code null}
         */
        protected PathEntry(Path root) {
            Objects.requireNonNull(root, "root can not be null");
            this.root = root.normalize();
        }

        /**
         * @return classloader which will be used to define classes
         */
        protected final ClassLoader loader() {
            return loader;
        }

        /**
         * @return stream of all classes in the specified path.
         */
        protected abstract Stream<String> classes();

        /**
         * @return string description of the specific path.
         */
        protected abstract String description();

        public void close() { }

    }

    private static class PathEntryClassLoader extends java.lang.ClassLoader {
        private final Function<String, byte[]> findByteCode;

        private PathEntryClassLoader(Function<String, byte[]> findByteCode) {
            this.findByteCode = findByteCode;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] code = findByteCode.apply(name);
            if (code == null) {
                return super.findClass(name);
            } else {
                return defineClass(name, code, 0, code.length);
            }
        }
    }

    private static final AtomicLong CLASS_COUNT = new AtomicLong(0L);
    private static volatile boolean CLASSES_LIMIT_REACHED = false;
    private static final Pattern JAR_IN_DIR_PATTERN
            = Pattern.compile("^(.*[/\\\\])?\\*$");

    /**
     * Factory method. Constructs list of handlers for {@code path}.
     *
     * @param path     the path to process
     * @throws NullPointerException if {@code path} or {@code executor} is
     *                              {@code null}
     */
    public static List<PathHandler> create(String path) {
        Objects.requireNonNull(path);
        Matcher matcher = JAR_IN_DIR_PATTERN.matcher(path);
        if (matcher.matches()) {
            path = matcher.group(1);
            path = path.isEmpty() ? "." : path;
            return ClassPathJarInDirEntry.create(Paths.get(path));
        } else if (path.startsWith("modules:")) {
            Path modules = FileSystems.getFileSystem(URI.create("jrt:/"))
                                      .getPath("modules");
            return Arrays.stream(path.substring("modules:".length())
                                     .split(","))
                         .map(modules::resolve)
                         .map(ClassPathDirEntry::new)
                         .map(PathHandler::new)
                         .collect(Collectors.toList());
        } else {
            path = path.isEmpty() ? "." : path;
            Path p = Paths.get(path);
            PathEntry entry;
            if (isJarFile(p)) {
                entry = new ClassPathJarEntry(p);
            } else if (isListFile(p)) {
                entry = new ClassesListInFile(p);
            } else if (isJimageFile(p)) {
                entry = new ClassPathJimageEntry(p);
            } else {
                entry = new ClassPathDirEntry(p);
            }
            return Collections.singletonList(new PathHandler(entry));
        }
    }

    private static boolean isJarFile(Path path) {
        if (Files.isRegularFile(path)) {
            String name = path.toString();
            return Utils.endsWithIgnoreCase(name, ".zip")
                    || Utils.endsWithIgnoreCase(name, ".jar");
        }
        return false;
    }

    private static boolean isJimageFile(Path path) {
        String filename = path.getFileName().toString();
        return Files.isRegularFile(path)
                && ("modules".equals(filename)
                || Utils.endsWithIgnoreCase(filename, ".jimage"));
    }

    private static boolean isListFile(Path path) {
        if (Files.isRegularFile(path)) {
            String name = path.toString();
            return Utils.endsWithIgnoreCase(name, ".lst");
        }
        return false;
    }

    private final PathEntry entry;
    protected PathHandler(PathEntry entry) {
        Objects.requireNonNull(entry);
        this.entry = entry;
    }


    @Override
    public void close() {
        entry.close();
    }

    /**
     * Processes all classes in the specified path.
     * @param executor executor used for process task invocation
     */
    public final void process(Executor executor) {
        CompileTheWorld.OUT.println(entry.description());
        entry.classes()
             .distinct()
             .forEach(s -> processClass(s, executor));
    }

    /**
     * @return count of all classes in the specified path.
     */
    public long classCount() {
        return entry.classes().count();
    }


    /**
     * Processes specified class.
     * @param name fully qualified name of class to process
     */
    protected final void processClass(String name, Executor executor) {
        Objects.requireNonNull(name);
        if (isFinished()) {
            return;
        }
        long id = CLASS_COUNT.incrementAndGet();
        if (id > Utils.COMPILE_THE_WORLD_STOP_AT) {
            CLASSES_LIMIT_REACHED = true;
            return;
        }
        if (id >= Utils.COMPILE_THE_WORLD_START_AT) {
            Class<?> aClass;
            Thread.currentThread().setContextClassLoader(entry.loader());
            try {
                CompileTheWorld.OUT.println(String.format("[%d]\t%s", id, name));
                aClass = entry.loader().loadClass(name);
                Compiler.compileClass(aClass, id, executor);
            } catch (Throwable e) {
                CompileTheWorld.OUT.println(String.format("[%d]\t%s\tWARNING skipped: %s",
                        id, name, e));
                e.printStackTrace(CompileTheWorld.ERR);
            }
        }
    }

    /**
     * @return count of processed classes
     */
    public static long getProcessedClassCount() {
        long id = CLASS_COUNT.get();
        if (id < Utils.COMPILE_THE_WORLD_START_AT) {
            return 0;
        }
        if (id > Utils.COMPILE_THE_WORLD_STOP_AT) {
            return Utils.COMPILE_THE_WORLD_STOP_AT - Utils.COMPILE_THE_WORLD_START_AT + 1;
        }
        return id - Utils.COMPILE_THE_WORLD_START_AT + 1;
    }

    /**
     * @return {@code true} if classes limit is reached and processing should be stopped
     */
    public static boolean isFinished() {
        return CLASSES_LIMIT_REACHED;
    }

}

