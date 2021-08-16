/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8160286 8243666 8217527
 * @summary Test the recording and checking of module hashes
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.base/jdk.internal.module
 *          jdk.compiler
 *          jdk.jartool
 *          jdk.jlink
 * @build jdk.test.lib.compiler.ModuleInfoMaker
 *        jdk.test.lib.compiler.CompilerUtils
 * @run testng HashesTest
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.module.ModuleInfo;
import jdk.internal.module.ModuleHashes;
import jdk.internal.module.ModulePath;

import jdk.test.lib.compiler.ModuleInfoMaker;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;
import static java.lang.module.ModuleDescriptor.Requires.Modifier.*;

public class HashesTest {
    static final ToolProvider JMOD_TOOL = ToolProvider.findFirst("jmod")
        .orElseThrow(() ->
            new RuntimeException("jmod tool not found")
        );
    static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
        .orElseThrow(() ->
            new RuntimeException("jar tool not found")
        );

    static final AtomicInteger counter = new AtomicInteger(0);

    private Path mods;
    private Path lib;
    private ModuleInfoMaker builder;

    @BeforeMethod
    public void setTestPath() throws IOException {
        Path dest = Path.of("test" + counter.addAndGet(1));
        if (Files.exists(dest)) {
            deleteDirectory(dest);
        }
        this.mods = dest.resolve("mods");
        this.lib = dest.resolve("lib");
        this.builder = new ModuleInfoMaker(dest.resolve("src"));

        Files.createDirectories(lib);
        Files.createDirectories(mods);
    }

    @Test
    public void test() throws IOException {
        // create modules for test cases
        makeModule("m2");
        makeModule("m3");
        makeModule("m1", "m2", "m3");

        makeModule("org.bar", TRANSITIVE, "m1");
        makeModule("org.foo", TRANSITIVE, "org.bar");

        // create JMOD for m1, m2, m3
        makeJmod("m2");
        makeJmod("m3");

        // no hash is recorded since m1 has outgoing edges
        jmodHashModules("m1", ".*");

        // no hash is recorded in m1, m2, m3
        assertNull(hashes("m1"));
        assertNull(hashes("m2"));
        assertNull(hashes("m3"));

        // hash m1 in m2
        jmodHashModules("m2",  "m1");
        checkHashes("m2", Set.of("m1"));

        // hash m1 in m2
        jmodHashModules("m2",  ".*");
        checkHashes("m2", Set.of("m1"));

        // create m2.jmod with no hash
        makeJmod("m2");
        // run jmod hash command to hash m1 in m2 and m3
        runJmod(List.of("hash", "--module-path", lib.toString(),
                        "--hash-modules", ".*"));
        checkHashes("m2", Set.of("m1"));
        checkHashes("m3", Set.of("m1"));

        // check transitive requires
        makeJmod("org.bar");
        makeJmod("org.foo");

        jmodHashModules("org.bar", "org.*");
        checkHashes("org.bar", Set.of("org.foo"));

        jmodHashModules( "m3", ".*");
        checkHashes("m3", Set.of("org.foo", "org.bar", "m1"));
    }

    @Test
    public void multiBaseModules() throws IOException {
        /*
         * y2 -----------> y1
         *    |______
         *    |      |
         *    V      V
         *    z3 -> z2
         *    |      |
         *    |      V
         *    |---> z1
         */

        makeModule("z1");
        makeModule("z2", "z1");
        makeModule("z3", "z1", "z2");

        makeModule("y1");
        makeModule("y2", "y1", "z2", "z3");

        Set<String> ys = Set.of("y1", "y2");
        Set<String> zs = Set.of("z1", "z2", "z3");

        // create JMOD files
        Stream.concat(ys.stream(), zs.stream()).forEach(this::makeJmod);

        // run jmod hash command
        runJmod(List.of("hash", "--module-path", lib.toString(),
                        "--hash-modules", ".*"));

        /*
         * z1 and y1 are the modules with hashes recorded.
         */
        checkHashes("y1", Set.of("y2"));
        checkHashes("z1", Set.of("z2", "z3", "y2"));
        Stream.concat(ys.stream(), zs.stream())
              .filter(mn -> !mn.equals("y1") && !mn.equals("z1"))
              .forEach(mn -> assertNull(hashes(mn)));
    }

    @Test
    public void mixJmodAndJarFile() throws IOException {
        /*
         * j3 -----------> j2
         *    |______
         *    |      |
         *    V      V
         *    m3 -> m2
         *    |      |
         *    |      V
         *    |---> m1 -> j1 -> jdk.jlink
         */

        makeModule("j1");
        makeModule("j2");
        makeModule("m1", "j1");
        makeModule("m2", "m1");
        makeModule("m3", "m1", "m2");

        makeModule("j3", "j2", "m2", "m3");

        Set<String> jars = Set.of("j1", "j2", "j3");
        Set<String> jmods = Set.of("m1", "m2", "m3");

        // create JMOD and JAR files
        jars.forEach(this::makeJar);
        jmods.forEach(this::makeJmod);

        // run jmod hash command
        runJmod(List.of("hash", "--module-path", lib.toString(),
                        "--hash-modules", "^j.*|^m.*"));

        /*
         * j1 and j2 are the modules with hashes recorded.
         */
        checkHashes("j2", Set.of("j3"));
        checkHashes("j1", Set.of("m1", "m2", "m3", "j3"));
        Stream.concat(jars.stream(), jmods.stream())
              .filter(mn -> !mn.equals("j1") && !mn.equals("j2"))
              .forEach(mn -> assertNull(hashes(mn)));
    }

    @Test
    public void upgradeableModule() throws IOException {
        Path mpath = Paths.get(System.getProperty("java.home"), "jmods");
        if (!Files.exists(mpath)) {
            return;
        }

        makeModule("m1");
        makeModule("java.compiler", "m1");
        makeModule("m2", "java.compiler");

        makeJmod("m1");
        makeJmod("m2");
        makeJmod("java.compiler",
                    "--module-path",
                    lib.toString() + File.pathSeparator + mpath,
                    "--hash-modules", "java\\.(?!se)|^m.*");

        checkHashes("java.compiler",  Set.of("m2"));
    }

    @Test
    public void testImageJmods() throws IOException {
        Path mpath = Paths.get(System.getProperty("java.home"), "jmods");
        if (!Files.exists(mpath)) {
            return;
        }

        makeModule("m1", "jdk.compiler", "jdk.attach");
        makeModule("m2", "m1");
        makeModule("m3", "java.compiler");

        makeJmod("m1");
        makeJmod("m2");

        runJmod(List.of("hash",
                        "--module-path",
                        mpath.toString() + File.pathSeparator + lib.toString(),
                        "--hash-modules", ".*"));

        validateImageJmodsTest(mpath);
    }

    @Test
    public void testImageJmods1() throws IOException {
        Path mpath = Paths.get(System.getProperty("java.home"), "jmods");
        if (!Files.exists(mpath)) {
            return;
        }

        makeModule("m1", "jdk.compiler", "jdk.attach");
        makeModule("m2", "m1");
        makeModule("m3", "java.compiler");

        makeJar("m2");
        makeJar("m1",
                    "--module-path",
                    mpath.toString() + File.pathSeparator + lib.toString(),
                    "--hash-modules", ".*");
        validateImageJmodsTest(mpath);
    }

    @Test
    public void testReproducibibleHash() throws Exception {
        makeModule("m4");
        makeModule("m3", "m4");
        makeModule("m2");
        makeModule("m1", "m2", "m3");

        // create JMOD files and run jmod hash
        List.of("m1", "m2", "m3", "m4").forEach(this::makeJmod);
        Map<String, ModuleHashes> hashes1 = runJmodHash();

        // sleep a bit to be confident that the hashes aren't dependent on timestamps
        Thread.sleep(2000);

        // (re)create JMOD files and run jmod hash
        List.of("m1", "m2", "m3", "m4").forEach(this::makeJmod);
        Map<String, ModuleHashes> hashes2 = runJmodHash();

        // hashes should be equal
        assertEquals(hashes1, hashes2);
    }

    @Test
    public void testHashModulesPattern() throws IOException {
        // create modules for test cases
        makeModule("m1");
        makeModule("m2", "m1");
        makeModule("m3");
        makeModule("m4", "m1", "m3");
        List.of("m1", "m2", "m3", "m4").forEach(this::makeJmod);

        // compute hash for the target jmod (m1.jmod) with different regex
        // 1) --hash-module "m2"
        Path jmod = lib.resolve("m1.jmod");
        runJmod("hash",
                "--module-path", lib.toString(),
                "--hash-modules", "m2", jmod.toString());
        assertEquals(moduleHashes().keySet(), Set.of("m1"));
        checkHashes("m1", Set.of("m2"));

        // 2) --hash-module "m2|m4"
        runJmod("hash",
                "--module-path", lib.toString(),
                "--hash-modules", "m2|m4", jmod.toString());
        assertEquals(moduleHashes().keySet(), Set.of("m1"));
        checkHashes("m1", Set.of("m2", "m4"));

        // 3) --hash-module ".*"
        runJmod("hash",
                "--module-path", lib.toString(),
                "--hash-modules", ".*", jmod.toString());
        assertEquals(moduleHashes().keySet(), Set.of("m1"));
        checkHashes("m1", Set.of("m2", "m4"));

        // target jmod is not specified
        // compute hash for all modules in the library
        runJmod("hash",
                "--module-path", lib.toString(),
                "--hash-modules", ".*");
        assertEquals(moduleHashes().keySet(), Set.of("m1", "m3"));
        checkHashes("m1", Set.of("m2", "m4"));
        checkHashes("m3", Set.of("m4"));
    }

    private void validateImageJmodsTest(Path mpath)
        throws IOException
    {
        // hash is recorded in m1 and not any other packaged modules on module path
        checkHashes("m1", Set.of("m2"));
        assertNull(hashes("m2"));

        // should not override any JDK packaged modules
        ModuleFinder finder = ModulePath.of(Runtime.version(), true, mpath);
        assertNull(hashes(finder, "jdk.compiler"));
        assertNull(hashes(finder, "jdk.attach"));
    }

    private void checkHashes(String mn, Set<String> hashModules) {
        ModuleHashes hashes = hashes(mn);
        assertEquals(hashModules, hashes.names());
    }

    private ModuleHashes hashes(String name) {
        ModuleFinder finder = ModulePath.of(Runtime.version(), true, lib);
        return hashes(finder, name);
    }

    private ModuleHashes hashes(ModuleFinder finder, String name) {
        ModuleReference mref = finder.find(name).orElseThrow(RuntimeException::new);
        try {
            try (ModuleReader reader = mref.open();
                 InputStream in = reader.open("module-info.class").get()) {
                ModuleHashes hashes = ModuleInfo.read(in, null).recordedHashes();
                System.out.format("hashes in module %s %s%n", name,
                    (hashes != null) ? "present" : "absent");
                if (hashes != null) {
                    hashes.names().stream().sorted().forEach(n ->
                        System.out.format("  %s %s%n", n, toHex(hashes.hashFor(n)))
                    );
                }
                return hashes;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String toHex(byte[] ba) {
        StringBuilder sb = new StringBuilder(ba.length);
        for (byte b: ba) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException
            {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException
            {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }


    private void makeModule(String mn, String... deps) throws IOException {
        makeModule(mn, null, deps);
    }

    private void makeModule(String mn, ModuleDescriptor.Requires.Modifier mod, String... deps)
        throws IOException
    {
        if (mod != null && mod != TRANSITIVE && mod != STATIC) {
            throw new IllegalArgumentException(mod.toString());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("module ")
          .append(mn)
          .append(" {")
          .append("\n");
        Arrays.stream(deps)
              .forEach(req -> {
                  sb.append("    requires ");
                  if (mod != null) {
                      sb.append(mod.toString().toLowerCase())
                        .append(" ");
                  }
                  sb.append(req)
                    .append(";\n");
              });
        sb.append("}\n");
        builder.writeJavaFiles(mn, sb.toString());
        builder.compile(mn, mods);
    }

    private void jmodHashModules(String moduleName, String hashModulesPattern) {
        makeJmod(moduleName, "--module-path", lib.toString(),
                 "--hash-modules", hashModulesPattern);
    }

    private void makeJmod(String moduleName, String... options) {
        Path mclasses = mods.resolve(moduleName);
        Path outfile = lib.resolve(moduleName + ".jmod");
        List<String> args = new ArrayList<>();
        args.add("create");
        Collections.addAll(args, options);
        Collections.addAll(args, "--class-path", mclasses.toString(),
                           outfile.toString());

        if (Files.exists(outfile)) {
            try {
                Files.delete(outfile);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        runJmod(args);
    }

    /**
     * Execute jmod hash on the modules in the lib directory. Returns a map of
     * the modules, with the module name as the key, for the modules that have
     * a ModuleHashes class file attribute.
     */
    private Map<String, ModuleHashes> runJmodHash() {
        runJmod("hash",
                "--module-path", lib.toString(),
                "--hash-modules", ".*");
        return moduleHashes();
    }

    private Map<String, ModuleHashes> moduleHashes() {
        return ModulePath.of(Runtime.version(), true, lib)
                .findAll()
                .stream()
                .map(ModuleReference::descriptor)
                .map(ModuleDescriptor::name)
                .filter(mn -> hashes(mn) != null)
                .collect(Collectors.toMap(mn -> mn, this::hashes));
    }

    private static void runJmod(List<String> args) {
        runJmod(args.toArray(new String[args.size()]));
    }

    private static void runJmod(String... args) {
        int rc = JMOD_TOOL.run(System.out, System.out, args);
        System.out.println("jmod " + Arrays.stream(args).collect(Collectors.joining(" ")));
        if (rc != 0) {
            throw new AssertionError("jmod failed: rc = " + rc);
        }
    }

    private void makeJar(String moduleName, String... options) {
        Path mclasses = mods.resolve(moduleName);
        Path outfile = lib.resolve(moduleName + ".jar");
        List<String> args = new ArrayList<>();
        Stream.concat(Stream.of("--create",
                                "--file=" + outfile.toString()),
                      Arrays.stream(options))
              .forEach(args::add);
        args.add("-C");
        args.add(mclasses.toString());
        args.add(".");

        if (Files.exists(outfile)) {
            try {
                Files.delete(outfile);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        int rc = JAR_TOOL.run(System.out, System.out, args.toArray(new String[args.size()]));
        System.out.println("jar " + args.stream().collect(Collectors.joining(" ")));
        if (rc != 0) {
            throw new AssertionError("jar failed: rc = " + rc);
        }
    }
}
