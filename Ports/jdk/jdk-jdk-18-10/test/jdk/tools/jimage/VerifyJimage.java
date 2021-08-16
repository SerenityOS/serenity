/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.jimage.BasicImageReader;
import jdk.internal.jimage.ImageLocation;

/*
 * @test
 * @summary Verify jimage
 * @modules java.base/jdk.internal.jimage
 * @run main/othervm --add-modules ALL-SYSTEM VerifyJimage
 */

/**
 * This test runs in two modes:
 * (1) No argument: it verifies the jimage by loading all classes in the runtime
 * (2) path of exploded modules: it compares bytes of each file in the exploded
 *     module with the entry in jimage
 *
 * FIXME: exception thrown when findLocation from jimage by multiple threads
 * -Djdk.test.threads=<n> to specify the number of threads.
 */
public class VerifyJimage {
    private static final String MODULE_INFO = "module-info.class";
    private static final Deque<String> failed = new ConcurrentLinkedDeque<>();

    public static void main(String... args) throws Exception {

        String home = System.getProperty("java.home");
        Path bootimagePath = Paths.get(home, "lib", "modules");
        if (Files.notExists(bootimagePath)) {
             System.out.println("Test skipped, not an images build");
             return;
        }

        long start = System.nanoTime();
        int numThreads = Integer.getInteger("jdk.test.threads", 1);
        JImageReader reader = newJImageReader();
        VerifyJimage verify = new VerifyJimage(reader, numThreads);
        if (args.length == 0) {
            // load classes from jimage
            verify.loadClasses();
        } else {
            Path dir = Paths.get(args[0]);
            if (Files.notExists(dir) || !Files.isDirectory(dir)) {
                throw new RuntimeException("Invalid argument: " + dir);
            }
            verify.compareExplodedModules(dir);
        }
        verify.waitForCompletion();
        long end = System.nanoTime();
        int entries = reader.entries();
        System.out.format("%d entries %d files verified: %d ms %d errors%n",
                          entries, verify.count.get(),
                          TimeUnit.NANOSECONDS.toMillis(end - start), failed.size());
        for (String f : failed) {
            System.err.println(f);
        }
        if (!failed.isEmpty()) {
            throw new AssertionError("Test failed");
        }
    }

    private final AtomicInteger count = new AtomicInteger(0);
    private final JImageReader reader;
    private final ExecutorService pool;

    VerifyJimage(JImageReader reader, int numThreads) {
        this.reader = reader;
        this.pool = Executors.newFixedThreadPool(numThreads);
    }

    private void waitForCompletion() throws InterruptedException {
        pool.shutdown();
        pool.awaitTermination(20, TimeUnit.SECONDS);
    }

    private void compareExplodedModules(Path dir) throws IOException {
        System.out.println("comparing jimage with " + dir);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path mdir : stream) {
                if (Files.isDirectory(mdir)) {
                    pool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Files.find(mdir, Integer.MAX_VALUE, (Path p, BasicFileAttributes attr)
                                           -> !Files.isDirectory(p) &&
                                              !mdir.relativize(p).toString().startsWith("_") &&
                                              !p.getFileName().toString().equals("MANIFEST.MF"))
                                     .forEach(p -> compare(mdir, p, reader));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    });
                }
            }
        }
    }

    private final List<String> BOOT_RESOURCES = Arrays.asList(
        "java.base/META-INF/services/java.nio.file.spi.FileSystemProvider"
    );
    private final List<String> EXT_RESOURCES = Arrays.asList(
        "jdk.zipfs/META-INF/services/java.nio.file.spi.FileSystemProvider"
    );
    private final List<String> APP_RESOURCES = Arrays.asList(
        "jdk.hotspot.agent/META-INF/services/com.sun.jdi.connect.Connector",
        "jdk.jdi/META-INF/services/com.sun.jdi.connect.Connector"
    );

    private void compare(Path mdir, Path p, JImageReader reader) {
        String entry = p.getFileName().toString().equals(MODULE_INFO)
                ? mdir.getFileName().toString() + "/" + MODULE_INFO
                : mdir.relativize(p).toString().replace(File.separatorChar, '/');

        count.incrementAndGet();
        String file = mdir.getFileName().toString() + "/" + entry;
        if (APP_RESOURCES.contains(file)) {
            // skip until the service config file is merged
            System.out.println("Skipped " + file);
            return;
        }

        if (reader.findLocation(entry) != null) {
            reader.compare(entry, p);
        }
    }

    private void loadClasses() {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        Stream.of(reader.getEntryNames())
              .filter(this::accept)
              .map(this::toClassName)
              .forEach(cn -> {
                  count.incrementAndGet();
                  try {
                      System.out.println("Loading " + cn);
                      Class.forName(cn, false, loader);
                  } catch (VerifyError ve) {
                      System.err.println("VerifyError for " + cn);
                      failed.add(reader.imageName() + ": " + cn + " not verified: " + ve.getMessage());
                  } catch (ClassNotFoundException e) {
                      failed.add(reader.imageName() + ": " + cn + " not found");
                  }
              });
    }

    private String toClassName(String entry) {
        int index = entry.indexOf('/', 1);
        return entry.substring(index + 1, entry.length())
                    .replaceAll("\\.class$", "").replace('/', '.');
    }

    private static Set<String> EXCLUDED_MODULES =
        Set.of("javafx.deploy", "jdk.deploy", "jdk.plugin", "jdk.javaws",
            // All JVMCI packages other than jdk.vm.ci.services are dynamically
            // exported to jdk.internal.vm.compiler
            "jdk.internal.vm.compiler"
        );

    private boolean accept(String entry) {
        int index = entry.indexOf('/', 1);
        String mn = index > 1 ? entry.substring(1, index) : "";
        // filter deployment modules

        if (mn.isEmpty() || EXCLUDED_MODULES.contains(mn)) {
            return false;
        }
        return entry.endsWith(".class") && !entry.endsWith(MODULE_INFO);
    }

    private static JImageReader newJImageReader() throws IOException {
        String home = System.getProperty("java.home");
        Path jimage = Paths.get(home, "lib", "modules");
        System.out.println("opened " + jimage);
        return new JImageReader(jimage);
    }

    static class JImageReader extends BasicImageReader {
        final Path jimage;
        JImageReader(Path p) throws IOException {
            super(p);
            this.jimage = p;
        }

        String imageName() {
            return jimage.getFileName().toString();
        }

        int entries() {
            return getHeader().getTableLength();
        }

        void compare(String entry, Path p) {
            try {
                byte[] bytes = Files.readAllBytes(p);
                byte[] imagebytes = getResource(entry);
                if (!Arrays.equals(bytes, imagebytes)) {
                    failed.add(imageName() + ": bytes differs than " + p.toString());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
