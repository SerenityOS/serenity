/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ReflectPermission;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.PropertyPermission;
import java.util.concurrent.Callable;

import jdk.internal.module.Modules;
import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.FlightRecorderListener;
import jdk.jfr.FlightRecorderPermission;
import jdk.jfr.Recording;
import jdk.jfr.internal.consumer.FileAccess;

/**
 * Contains JFR code that does
 * {@link AccessController#doPrivileged(PrivilegedAction)}
 */
public final class SecuritySupport {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Module JFR_MODULE = Event.class.getModule();
    public  static final SafePath JFC_DIRECTORY = getPathInProperty("java.home", "lib/jfr");
    public static final FileAccess PRIVILEGED = new Privileged();
    static final SafePath USER_HOME = getPathInProperty("user.home", null);
    static final SafePath JAVA_IO_TMPDIR = getPathInProperty("java.io.tmpdir", null);

    static {
        // ensure module java.base can read module jdk.jfr as early as possible
        addReadEdge(Object.class);
        addHandlerExport(Object.class);
        addEventsExport(Object.class);
        addInstrumentExport(Object.class);
    }

    static final class SecureRecorderListener implements FlightRecorderListener {

        @SuppressWarnings("removal")
        private final AccessControlContext context;
        private final FlightRecorderListener changeListener;

        SecureRecorderListener(@SuppressWarnings("removal") AccessControlContext context, FlightRecorderListener changeListener) {
            this.context = Objects.requireNonNull(context);
            this.changeListener = Objects.requireNonNull(changeListener);
        }

        @SuppressWarnings("removal")
        @Override
        public void recordingStateChanged(Recording recording) {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                try {
                    changeListener.recordingStateChanged(recording);
                } catch (Throwable t) {
                    // Prevent malicious user to propagate exception callback in the wrong context
                    Logger.log(LogTag.JFR, LogLevel.WARN, "Unexpected exception in listener " + changeListener.getClass()+ " at recording state change");
                }
                return null;
            }, context);
        }

        @SuppressWarnings("removal")
        @Override
        public void recorderInitialized(FlightRecorder recorder) {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                try  {
                    changeListener.recorderInitialized(recorder);
                } catch (Throwable t) {
                    // Prevent malicious user to propagate exception callback in the wrong context
                    Logger.log(LogTag.JFR, LogLevel.WARN, "Unexpected exception in listener " + changeListener.getClass()+ " when initializing FlightRecorder");
                }
                return null;
            }, context);
        }

        public FlightRecorderListener getChangeListener() {
            return changeListener;
        }
    }

    private static final class DirectoryCleaner extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            Files.delete(path);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc != null) {
                throw exc;
            }
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Path created by the default file provider,and not
     * a malicious provider.
     *
     */
    public static final class SafePath implements Comparable<SafePath> {
        private final Path path;
        private final String text;

        public SafePath(Path p) {
            // sanitize
            text = p.toString();
            path = Paths.get(text);
        }

        public SafePath(String path) {
            this(Paths.get(path));
        }

        public Path toPath() {
            return path;
        }

        public File toFile() {
            return path.toFile();
        }

        @Override
        public String toString() {
            return text;
        }

        @Override
        public int compareTo(SafePath that) {
            return that.text.compareTo(this.text);
        }

        @Override
        public boolean equals(Object other) {
            if(other != null && other instanceof SafePath s){
                return this.toPath().equals(s.toPath());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.toPath().hashCode();
        }
    }

    private interface RunnableWithCheckedException {
        public void run() throws Exception;
    }

    private interface CallableWithoutCheckException<T> {
        public T call();
    }

    @SuppressWarnings("removal")
    private static <U> U doPrivilegedIOWithReturn(Callable<U> function) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<U>() {
                @Override
                public U run() throws Exception {
                    return function.call();
                }
            }, null);
        } catch (PrivilegedActionException e) {
            Throwable t = e.getCause();
            if (t instanceof IOException) {
                throw (IOException) t;
            }
            throw new IOException("Unexpected error during I/O operation. " + t.getMessage(), t);
        }
    }

    private static void doPriviligedIO(RunnableWithCheckedException function) throws IOException {
        doPrivilegedIOWithReturn(() -> {
            function.run();
            return null;
        });
    }

    @SuppressWarnings("removal")
    private static void doPrivileged(Runnable function, Permission... perms) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                function.run();
                return null;
            }
        }, null, perms);
    }

    @SuppressWarnings("removal")
    private static void doPrivileged(Runnable function) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                function.run();
                return null;
            }
        });
    }

    @SuppressWarnings("removal")
    private static <T> T doPrivilegedWithReturn(CallableWithoutCheckException<T> function, Permission... perms) {
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            @Override
            public T run() {
                return function.call();
            }
        }, null, perms);
    }

    public static List<SafePath> getPredefinedJFCFiles() {
        List<SafePath> list = new ArrayList<>();
        try {
            Iterator<Path> pathIterator = doPrivilegedIOWithReturn(() -> {
                return Files.newDirectoryStream(JFC_DIRECTORY.toPath(), "*").iterator();
            });
            while (pathIterator.hasNext()) {
                Path path = pathIterator.next();
                if (path.toString().endsWith(".jfc")) {
                    list.add(new SafePath(path));
                }
            }
        } catch (IOException ioe) {
            Logger.log(LogTag.JFR, LogLevel.WARN, "Could not access .jfc-files in " + JFC_DIRECTORY + ", " + ioe.getMessage());
        }
        return list;
    }

    static void makeVisibleToJFR(Class<?> clazz) {
        Module classModule = clazz.getModule();
        Modules.addReads(JFR_MODULE, classModule);
        if (clazz.getPackage() != null) {
            String packageName = clazz.getPackage().getName();
            Modules.addExports(classModule, packageName, JFR_MODULE);
            Modules.addOpens(classModule, packageName, JFR_MODULE);
        }
    }

    /**
     * Adds a qualified export of the internal.jdk.jfr.internal.handlers package
     * (for EventHandler)
     */
    static void addHandlerExport(Class<?> clazz) {
        Modules.addExports(JFR_MODULE, Utils.HANDLERS_PACKAGE_NAME, clazz.getModule());
    }

    static void addEventsExport(Class<?> clazz) {
        Modules.addExports(JFR_MODULE, Utils.EVENTS_PACKAGE_NAME, clazz.getModule());
    }

    static void addInstrumentExport(Class<?> clazz) {
        Modules.addExports(JFR_MODULE, Utils.INSTRUMENT_PACKAGE_NAME, clazz.getModule());
    }

    static void addReadEdge(Class<?> clazz) {
        Modules.addReads(clazz.getModule(), JFR_MODULE);
    }

    public static void registerEvent(Class<? extends jdk.internal.event.Event> eventClass) {
        doPrivileged(() ->  MetadataRepository.getInstance().register(eventClass), new FlightRecorderPermission(Utils.REGISTER_EVENT));
    }

    public static void registerMirror(Class<? extends Event> eventClass) {
        doPrivileged(() ->  MetadataRepository.getInstance().registerMirror(eventClass), new FlightRecorderPermission(Utils.REGISTER_EVENT));
    }

    public static void setProperty(String propertyName, String value) {
        doPrivileged(() -> System.setProperty(propertyName, value), new PropertyPermission(propertyName, "write"));
    }

    static boolean getBooleanProperty(String propertyName) {
        return doPrivilegedWithReturn(() -> Boolean.getBoolean(propertyName), new PropertyPermission(propertyName, "read"));
    }

    private static SafePath getPathInProperty(String prop, String subPath) {
        return doPrivilegedWithReturn(() -> {
            String path = System.getProperty(prop);
            if (path == null) {
                return null;
            }
            File file = subPath == null ? new File(path) : new File(path, subPath);
            return new SafePath(file.getAbsolutePath());
        }, new PropertyPermission("*", "read"));
    }

    // Called by JVM during initialization of JFR
    static Thread createRecorderThread(ThreadGroup systemThreadGroup, ClassLoader contextClassLoader) {
        // The thread should have permission = new Permission[0], and not "modifyThreadGroup" and "modifyThread" on the stack,
        // but it's hard circumvent if we are going to pass in system thread group in the constructor
        Thread thread = doPrivilegedWithReturn(() -> new Thread(systemThreadGroup, "JFR Recorder Thread"), new RuntimePermission("modifyThreadGroup"), new RuntimePermission("modifyThread"));
        doPrivileged(() -> thread.setContextClassLoader(contextClassLoader), new RuntimePermission("setContextClassLoader"), new RuntimePermission("modifyThread"));
        return thread;
    }

    static void registerShutdownHook(Thread shutdownHook) {
        doPrivileged(() -> Runtime.getRuntime().addShutdownHook(shutdownHook), new RuntimePermission("shutdownHooks"));
    }

    static void setUncaughtExceptionHandler(Thread thread, Thread.UncaughtExceptionHandler eh) {
        doPrivileged(() -> thread.setUncaughtExceptionHandler(eh), new RuntimePermission("modifyThread"));
    }

    static void moveReplace(SafePath from, SafePath to) throws IOException {
        doPrivilegedIOWithReturn(() -> Files.move(from.toPath(), to.toPath()));
    }

    static void clearDirectory(SafePath safePath) throws IOException {
        doPriviligedIO(() -> Files.walkFileTree(safePath.toPath(), new DirectoryCleaner()));
    }

    static SafePath toRealPath(SafePath safePath) throws IOException {
        return new SafePath(doPrivilegedIOWithReturn(() -> safePath.toPath().toRealPath()));
    }

    static boolean existDirectory(SafePath directory) throws IOException {
        return doPrivilegedIOWithReturn(() -> Files.exists(directory.toPath()));
    }

    static RandomAccessFile createRandomAccessFile(SafePath path) throws Exception {
        return doPrivilegedIOWithReturn(() -> new RandomAccessFile(path.toPath().toFile(), "rw"));
    }

    public static InputStream newFileInputStream(SafePath safePath) throws IOException {
        return doPrivilegedIOWithReturn(() -> Files.newInputStream(safePath.toPath()));
    }

    public static long getFileSize(SafePath safePath) throws IOException {
        return doPrivilegedIOWithReturn(() -> Files.size(safePath.toPath()));
    }

    static SafePath createDirectories(SafePath safePath) throws IOException {
        Path p = doPrivilegedIOWithReturn(() -> Files.createDirectories(safePath.toPath()));
        return new SafePath(p);
    }

    public static boolean exists(SafePath safePath) throws IOException {
        // Files.exist(path) is allocation intensive
        return doPrivilegedIOWithReturn(() -> safePath.toPath().toFile().exists());
    }

    public static boolean isDirectory(SafePath safePath) throws IOException {
        return doPrivilegedIOWithReturn(() -> Files.isDirectory(safePath.toPath()));
    }

    static void delete(SafePath localPath) throws IOException {
        doPriviligedIO(() -> Files.delete(localPath.toPath()));
    }

    static boolean isWritable(SafePath safePath) throws IOException {
        return doPrivilegedIOWithReturn(() -> Files.isWritable(safePath.toPath()));
    }

    static void deleteOnExit(SafePath safePath) {
        doPrivileged(() -> safePath.toPath().toFile().deleteOnExit());
    }

    static ReadableByteChannel newFileChannelToRead(SafePath safePath) throws IOException {
        return doPrivilegedIOWithReturn(() -> FileChannel.open(safePath.toPath(), StandardOpenOption.READ));
    }

    public static InputStream getResourceAsStream(String name) throws IOException {
        return doPrivilegedIOWithReturn(() -> SecuritySupport.class.getResourceAsStream(name));
    }

    public static Reader newFileReader(SafePath safePath) throws FileNotFoundException, IOException {
        return doPrivilegedIOWithReturn(() -> Files.newBufferedReader(safePath.toPath()));
    }

    static void touch(SafePath path) throws IOException {
        doPriviligedIO(() -> new RandomAccessFile(path.toPath().toFile(), "rw").close());
    }

    static void setAccessible(Method method) {
        doPrivileged(() -> method.setAccessible(true), new ReflectPermission("suppressAccessChecks"));
    }

    static void setAccessible(Field field) {
        doPrivileged(() -> field.setAccessible(true), new ReflectPermission("suppressAccessChecks"));
    }

    static void setAccessible(Constructor<?> constructor) {
        doPrivileged(() -> constructor.setAccessible(true), new ReflectPermission("suppressAccessChecks"));
    }

    @SuppressWarnings("removal")
    static void ensureClassIsInitialized(Class<?> clazz) {
        try {
            MethodHandles.Lookup lookup;
            if (System.getSecurityManager() == null) {
                lookup = MethodHandles.privateLookupIn(clazz, LOOKUP);
            } else {
                lookup = AccessController.doPrivileged(new PrivilegedExceptionAction<>() {
                    @Override
                    public MethodHandles.Lookup run() throws IllegalAccessException {
                        return MethodHandles.privateLookupIn(clazz, LOOKUP);
                    }
                }, null, new ReflectPermission("suppressAccessChecks"));
            }
            lookup.ensureInitialized(clazz);
        } catch (IllegalAccessException e) {
            throw new InternalError(e);
        } catch (PrivilegedActionException e) {
            throw new InternalError(e.getCause());
        }
    }

    @SuppressWarnings("removal")
    static Class<?> defineClass(Class<?> lookupClass, byte[] bytes) {
        return AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
            @Override
            public Class<?> run() {
                try {
                    return MethodHandles.privateLookupIn(lookupClass, LOOKUP).defineClass(bytes);
                } catch (IllegalAccessException e) {
                    throw new InternalError(e);
                }
            }
        });
    }

    public static Thread createThreadWitNoPermissions(String threadName, Runnable runnable) {
        return doPrivilegedWithReturn(() -> new Thread(runnable, threadName), new Permission[0]);
    }

    public static void setDaemonThread(Thread t, boolean daemon) {
      doPrivileged(()-> t.setDaemon(daemon), new RuntimePermission("modifyThread"));
    }

    public static SafePath getAbsolutePath(SafePath path) throws IOException {
        return new SafePath(doPrivilegedIOWithReturn((()-> path.toPath().toAbsolutePath())));
    }

    private static final class Privileged extends FileAccess {
        @Override
        public RandomAccessFile openRAF(File f, String mode) throws IOException {
            return doPrivilegedIOWithReturn( () -> new RandomAccessFile(f, mode));
        }

        @Override
        public  DirectoryStream<Path> newDirectoryStream(Path directory)  throws IOException  {
            return doPrivilegedIOWithReturn( () -> Files.newDirectoryStream(directory));
        }

        @Override
        public  String getAbsolutePath(File f) throws IOException {
            return doPrivilegedIOWithReturn( () -> f.getAbsolutePath());
        }
        @Override
        public long length(File f) throws IOException {
            return doPrivilegedIOWithReturn( () -> f.length());
        }

        @Override
        public  long fileSize(Path p) throws IOException {
            return doPrivilegedIOWithReturn( () -> Files.size(p));
        }

        @Override
        public boolean exists(Path p) throws IOException {
            return doPrivilegedIOWithReturn( () -> Files.exists(p));
        }

        @Override
        public boolean isDirectory(Path p) {
            return doPrivilegedWithReturn( () -> Files.isDirectory(p));
        }

        @Override
        public FileTime getLastModified(Path p) throws IOException {
            // Timestamp only needed when examining repository for other JVMs,
            // in which case an unprivileged mode should be used.
            throw new InternalError("Should not reach here");
        }
    }


}
