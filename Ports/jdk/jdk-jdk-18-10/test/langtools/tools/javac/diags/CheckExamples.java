/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6968063 7127924
 * @summary provide examples of code that generate diagnostics
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.resources:open
 *          jdk.compiler/com.sun.tools.javac.util
 * @build Example CheckExamples DocCommentProcessor
 * @run main/othervm CheckExamples
 */

/*
 *      See CR 7127924 for info on why othervm is used.
 */

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Check invariants for a set of examples.
 * -- each example should exactly declare the keys that will be generated when
 *      it is run.
 * -- together, the examples should cover the set of resource keys in the
 *      compiler.properties bundle. A list of exceptions may be given in the
 *      not-yet.txt file. Entries on the not-yet.txt list should not be
 *      covered by examples.
 * When new keys are added to the resource bundle, it is strongly recommended
 * that corresponding new examples be added here, if at all practical, instead
 * of simply and lazily being added to the not-yet.txt list.
 */
public class CheckExamples {
    /**
     * Standard entry point.
     */
    public static void main(String... args) throws Exception {
        boolean jtreg = (System.getProperty("test.src") != null);
        Path tmpDir;
        boolean deleteOnExit;
        if (jtreg) {
            // use standard jtreg scratch directory: the current directory
            tmpDir = Paths.get(System.getProperty("user.dir"));
            deleteOnExit = false;
        } else {
            tmpDir = Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")),
                    CheckExamples.class.getName());
            deleteOnExit = true;
        }
        Example.setTempDir(tmpDir.toFile());

        try {
            new CheckExamples().run();
        } finally {
            if (deleteOnExit) {
                clean(tmpDir);
            }
        }
    }

    /**
     * Run the test.
     */
    void run() throws Exception {
        Set<Example> examples = getExamples();

        Set<String> notYetList = getNotYetList();
        Set<String> declaredKeys = new TreeSet<String>();
        for (Example e: examples) {
            Set<String> e_decl = e.getDeclaredKeys();
            Set<String> e_actual = e.getActualKeys();
            for (String k: e_decl) {
                if (!e_actual.contains(k))
                    error("Example " + e + " declares key " + k + " but does not generate it");
            }
            for (String k: e_actual) {
                if (!e_decl.contains(k))
                    error("Example " + e + " generates key " + k + " but does not declare it");
            }
            for (String k: e.getDeclaredKeys()) {
                if (notYetList.contains(k))
                    error("Example " + e + " declares key " + k + " which is also on the \"not yet\" list");
                declaredKeys.add(k);
            }
        }

        Module jdk_compiler = ModuleLayer.boot().findModule("jdk.compiler").get();
        ResourceBundle b =
            ResourceBundle.getBundle("com.sun.tools.javac.resources.compiler", jdk_compiler);
        Set<String> resourceKeys = new TreeSet<String>(b.keySet());

        for (String dk: declaredKeys) {
            if (!resourceKeys.contains(dk))
                error("Key " + dk + " is declared in tests but is not a valid key in resource bundle");
        }

        for (String nk: notYetList) {
            if (!resourceKeys.contains(nk))
                error("Key " + nk + " is declared in not-yet list but is not a valid key in resource bundle");
        }

        for (String rk: resourceKeys) {
            if (!declaredKeys.contains(rk) && !notYetList.contains(rk))
                error("Key " + rk + " is declared in resource bundle but is not in tests or not-yet list");
        }

        System.err.println(examples.size() + " examples checked");
        System.err.println(notYetList.size() + " keys on not-yet list");

        Counts declaredCounts = new Counts(declaredKeys);
        Counts resourceCounts = new Counts(resourceKeys);
        List<String> rows = new ArrayList<String>(Arrays.asList(Counts.prefixes));
        rows.add("other");
        rows.add("total");
        System.err.println();
        System.err.println(String.format("%-14s %15s %15s %4s",
                "prefix", "#keys in tests", "#keys in javac", "%"));
        for (String p: rows) {
            int d = declaredCounts.get(p);
            int r = resourceCounts.get(p);
            System.err.print(String.format("%-14s %15d %15d", p, d, r));
            if (r != 0)
                System.err.print(String.format(" %3d%%", (d * 100) / r));
            System.err.println();
        }

        if (errors > 0)
            throw new Exception(errors + " errors occurred.");
    }

    /**
     * Get the complete set of examples to be checked.
     */
    Set<Example> getExamples() {
        Set<Example> results = new TreeSet<Example>();
        File testSrc = new File(System.getProperty("test.src"));
        File examples = new File(testSrc, "examples");
        for (File f: examples.listFiles()) {
            if (isValidExample(f))
                results.add(new Example(f));
        }
        return results;
    }

    boolean isValidExample(File f) {
        return (f.isDirectory() && f.list().length > 0) ||
                (f.isFile() && f.getName().endsWith(".java"));
    }

    /**
     * Get the contents of the "not-yet" list.
     */
    Set<String> getNotYetList() {
        Set<String> results = new TreeSet<String>();
        File testSrc = new File(System.getProperty("test.src"));
        File notYetList = new File(testSrc, "examples.not-yet.txt");
        try {
            String[] lines = read(notYetList).split("[\r\n]");
            for (String line: lines) {
                int hash = line.indexOf("#");
                if (hash != -1)
                    line = line.substring(0, hash).trim();
                if (line.matches("[A-Za-z0-9-_.]+"))
                    results.add(line);
            }
        } catch (IOException e) {
            throw new Error(e);
        }
        return results;
    }

    /**
     * Read the contents of a file.
     */
    String read(File f) throws IOException {
        byte[] bytes = new byte[(int) f.length()];
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        try {
            in.readFully(bytes);
        } finally {
            in.close();
        }
        return new String(bytes);
    }

    /**
     * Report an error.
     */
    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;

    /**
     * Clean the contents of a directory.
     */
    static void clean(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) Files.delete(dir);
                return super.postVisitDirectory(dir, exc);
            }
        });
    }

    static class Counts {
        static String[] prefixes = {
            "compiler.err.",
            "compiler.warn.",
            "compiler.note.",
            "compiler.misc."
        };

        Counts(Set<String> keys) {
            nextKey:
            for (String k: keys) {
                for (String p: prefixes) {
                    if (k.startsWith(p)) {
                        inc(p);
                        continue nextKey;
                    }
                }
                inc("other");
            }
            table.put("total", keys.size());
        }

        int get(String p) {
             Integer i = table.get(p);
             return (i == null ? 0 : i);
        }

        void inc(String p) {
            Integer i = table.get(p);
            table.put(p, (i == null ? 1 : i + 1));
        }

        Map<String,Integer> table = new HashMap<String,Integer>();
    };
}
