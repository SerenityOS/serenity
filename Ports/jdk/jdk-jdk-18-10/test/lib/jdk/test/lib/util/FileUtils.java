/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jdk.test.lib.Platform;

import com.sun.management.UnixOperatingSystemMXBean;

/**
 * Common library for various test file utility functions.
 */
public final class FileUtils {
    private static final boolean IS_WINDOWS = Platform.isWindows();
    private static final int RETRY_DELETE_MILLIS = IS_WINDOWS ? 500 : 0;
    private static final int MAX_RETRY_DELETE_TIMES = IS_WINDOWS ? 15 : 0;
    private static volatile boolean nativeLibLoaded;

    /**
     * Deletes a file, retrying if necessary.
     *
     * @param path  the file to delete
     *
     * @throws NoSuchFileException
     *         if the file does not exist (optional specific exception)
     * @throws DirectoryNotEmptyException
     *         if the file is a directory and could not otherwise be deleted
     *         because the directory is not empty (optional specific exception)
     * @throws IOException
     *         if an I/O error occurs
     */
    public static void deleteFileWithRetry(Path path) throws IOException {
        try {
            deleteFileWithRetry0(path);
        } catch (InterruptedException x) {
            throw new IOException("Interrupted while deleting.", x);
        }
    }

    /**
     * Deletes a file, retrying if necessary.
     * No exception thrown if file doesn't exist.
     *
     * @param path  the file to delete
     *
     * @throws NoSuchFileException
     *         if the file does not exist (optional specific exception)
     * @throws DirectoryNotEmptyException
     *         if the file is a directory and could not otherwise be deleted
     *         because the directory is not empty (optional specific exception)
     * @throws IOException
     *         if an I/O error occurs
     */
    public static void deleteFileIfExistsWithRetry(Path path) throws IOException {
        try {
            if (!Files.notExists(path)) {
                deleteFileWithRetry0(path);
            }
        } catch (InterruptedException x) {
            throw new IOException("Interrupted while deleting.", x);
        }
    }

    private static void deleteFileWithRetry0(Path path)
            throws IOException, InterruptedException {
        int times = 0;
        IOException ioe = null;
        while (true) {
            try {
                Files.delete(path);
                // Checks for absence of the file. Semantics of Files.exists() is not the same.
                while (!Files.notExists(path)) {
                    times++;
                    if (times > MAX_RETRY_DELETE_TIMES) {
                        throw new IOException("File still exists after " + times + " waits.");
                    }
                    Thread.sleep(RETRY_DELETE_MILLIS);
                }
                break;
            } catch (NoSuchFileException | DirectoryNotEmptyException x) {
                throw x;
            } catch (IOException x) {
                // Backoff/retry in case another process is accessing the file
                times++;
                if (ioe == null) {
                    ioe = x;
                } else {
                    ioe.addSuppressed(x);
                }

                if (times > MAX_RETRY_DELETE_TIMES) {
                    throw ioe;
                }
                Thread.sleep(RETRY_DELETE_MILLIS);
            }
        }
    }

    /**
     * Deletes a directory and its subdirectories, retrying if necessary.
     *
     * @param dir  the directory to delete
     *
     * @throws  IOException
     *          If an I/O error occurs. Any such exceptions are caught
     *          internally. If only one is caught, then it is re-thrown.
     *          If more than one exception is caught, then the second and
     *          following exceptions are added as suppressed exceptions of the
     *          first one caught, which is then re-thrown.
     */
    public static void deleteFileTreeWithRetry(Path dir) throws IOException {
        IOException ioe = null;
        final List<IOException> excs = deleteFileTreeUnchecked(dir);
        if (!excs.isEmpty()) {
            ioe = excs.remove(0);
            for (IOException x : excs) {
                ioe.addSuppressed(x);
            }
        }
        if (ioe != null) {
            throw ioe;
        }
    }

    public static List<IOException> deleteFileTreeUnchecked(Path dir) {
        final List<IOException> excs = new ArrayList<>();
        try {
            java.nio.file.Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        deleteFileWithRetry0(file);
                    } catch (IOException x) {
                        excs.add(x);
                    } catch (InterruptedException x) {
                        excs.add(new IOException("Interrupted while deleting.", x));
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    try {
                        deleteFileWithRetry0(dir);
                    } catch (IOException x) {
                        excs.add(x);
                    } catch (InterruptedException x) {
                        excs.add(new IOException("Interrupted while deleting.", x));
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    excs.add(exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException x) {
            excs.add(x);
        }
        return excs;
    }

    /**
     * Checks whether all file systems are accessible. This is performed
     * by checking free disk space on all mounted file systems via a
     * separate, spawned process. File systems are considered to be
     * accessible if this process completes successfully before a given
     * fixed duration has elapsed.
     *
     * @implNote On Unix this executes the {@code df} command in a separate
     * process and on Windows always returns {@code true}.
     */
    public static boolean areFileSystemsAccessible() throws IOException {
        boolean areFileSystemsAccessible = true;
        if (!IS_WINDOWS) {
            // try to check whether 'df' hangs
            System.out.println("\n--- df output ---");
            System.out.flush();
            Process proc = new ProcessBuilder("df").inheritIO().start();
            try {
                proc.waitFor(90, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
            try {
                int exitValue = proc.exitValue();
                if (exitValue != 0) {
                    System.err.printf("df process exited with %d != 0%n",
                        exitValue);
                    areFileSystemsAccessible = false;
                }
            } catch (IllegalThreadStateException ignored) {
                System.err.println("df command apparently hung");
                areFileSystemsAccessible = false;
            }
        }
        return areFileSystemsAccessible;
    }

    /**
     * Checks whether all file systems are accessible and there are no
     * duplicate mount points. This is performed by checking free disk
     * space on all mounted file systems via a separate, spawned process.
     * File systems are considered to be accessible if this process completes
     * successfully before a given fixed duration has elapsed.
     *
     * @implNote On Unix this executes the {@code df} command in a separate
     * process and on Windows always returns {@code true}.
     *
     * @return whether file systems appear to be accessible and duplicate-free
     */
    public static boolean areMountPointsAccessibleAndUnique() {
        if (IS_WINDOWS) return true;

        final AtomicBoolean areMountPointsOK = new AtomicBoolean(true);
        Thread thr = new Thread(() -> {
            try {
                Process proc = new ProcessBuilder("df").start();
                BufferedReader reader = new BufferedReader
                    (new InputStreamReader(proc.getInputStream()));
                // Skip the first line as it is the "df" output header.
                if (reader.readLine() != null ) {
                    Set mountPoints = new HashSet();
                    String mountPoint = null;
                    while ((mountPoint = reader.readLine()) != null) {
                        if (!mountPoints.add(mountPoint)) {
                            System.err.printf
                                ("Config error: duplicate mount point %s%n",
                                mountPoint);
                            areMountPointsOK.set(false);
                            break;
                        }
                    }
                }

                try {
                    proc.waitFor(90, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
                try {
                    int exitValue = proc.exitValue();
                    if (exitValue != 0) {
                        System.err.printf("df process exited with %d != 0%n",
                            exitValue);
                        areMountPointsOK.set(false);
                    }
                } catch (IllegalThreadStateException ignored) {
                    System.err.println("df command apparently hung");
                    areMountPointsOK.set(false);
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            };
        });

        final AtomicReference throwableReference =
            new AtomicReference<Throwable>();
        thr.setUncaughtExceptionHandler(
            new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread t, Throwable e) {
                    throwableReference.set(e);
                }
            });

        thr.start();
        try {
            thr.join(120*1000L);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }

        Throwable uncaughtException = (Throwable)throwableReference.get();
        if (uncaughtException != null) {
            throw new RuntimeException(uncaughtException);
        }

        if (thr.isAlive()) {
            throw new RuntimeException("df thread did not join in time");
        }

        return areMountPointsOK.get();
    }

    /**
     * List the open file descriptors (if supported by the 'lsof' command).
     * @param ps a printStream to send the output to
     * @throws UncheckedIOException if an error occurs
     */
    public static void listFileDescriptors(PrintStream ps) {

        Optional<String[]> lsof = Arrays.stream(lsCommands)
                .filter(args -> Files.isExecutable(Path.of(args[0])))
                .findFirst();
        lsof.ifPresent(args -> {
            try {
                ps.printf("Open File Descriptors:%n");
                long pid = ProcessHandle.current().pid();
                ProcessBuilder pb = new ProcessBuilder(args[0], args[1], Integer.toString((int) pid));
                pb.redirectErrorStream(true);   // combine stderr and stdout
                pb.redirectOutput(Redirect.PIPE);

                Process p = pb.start();
                Instant start = Instant.now();
                p.getInputStream().transferTo(ps);

                try {
                    int timeout = 10;
                    if (!p.waitFor(timeout, TimeUnit.SECONDS)) {
                        System.out.printf("waitFor timed out: %d%n", timeout);
                    }
                } catch (InterruptedException ie) {
                    throw new IOException("interrupted", ie);
                }
                ps.println();
            } catch (IOException ioe) {
                throw new UncheckedIOException("error listing file descriptors", ioe);
            }
        });
    }

    // Return the current process handle count
    public static long getProcessHandleCount() {
        if (IS_WINDOWS) {
            if (!nativeLibLoaded) {
                System.loadLibrary("FileUtils");
                nativeLibLoaded = true;
            }
            return getWinProcessHandleCount();
        } else {
            return ((UnixOperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean()).getOpenFileDescriptorCount();
        }
    }

    private static native long getWinProcessHandleCount();

    // Possible command locations and arguments
    static String[][] lsCommands = new String[][] {
            {"/usr/bin/lsof", "-p"},
            {"/usr/sbin/lsof", "-p"},
            {"/bin/lsof", "-p"},
            {"/sbin/lsof", "-p"},
            {"/usr/local/bin/lsof", "-p"},
    };
}
