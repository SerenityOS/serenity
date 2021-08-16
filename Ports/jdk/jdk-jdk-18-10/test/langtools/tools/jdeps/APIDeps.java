/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8015912 8029216 8048063 8050804
 * @summary Test -apionly and -jdkinternals options
 * @library lib
 * @modules java.base/sun.security.x509
 *          java.management
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jdeps/com.sun.tools.jdeps
 * @run main APIDeps
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class APIDeps {
    public static void main(String... args) throws Exception {
        int errors = 0;
        errors += new APIDeps().run();
        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    private static final Path dest = Paths.get(System.getProperty("test.classes", "."), "tmp");
    private static final String[] srcDirs = new String[] {
            "m", "b", "c", "d", "e", "f", "g"
    };
    void setup(Path dest) throws IOException {
        CompilerUtils.cleanDir(dest);
        Files.createDirectories(dest);
        Path testsrc = Paths.get(System.getProperty("test.src"));
        List<String> options = new ArrayList<>();

        // jdk.jdeps is a service provider module so needs to be explicitly included
        options.add("--add-modules=jdk.jdeps");

        // add --add-exports
        String testModules = System.getProperty("test.modules", "");
        List<String> addExports = new ArrayList<>();
        for (String s : testModules.split("\\s+")) {
            if (s.isEmpty()) continue;
            if (s.indexOf('/') != -1)
                addExports.add("--add-exports=" + s.trim() + "=ALL-UNNAMED");
        }
        options.addAll(addExports);

        for (String dir : srcDirs) {
            Path source = testsrc.resolve(dir);
            boolean ok = CompilerUtils.compile(source, dest, options.toArray(new String[0]));
            if (!ok) {
                throw new RuntimeException("compilation fails");
            }
        }
    }

    int run() throws IOException {
        // compile classes in a separate directory for analysis
        setup(dest);

        File testDir = dest.toFile();
        String testDirBasename = testDir.toPath().getFileName().toString();
        File mDir = new File(testDir, "m");
        // all dependencies
        test(new File(mDir, "Bar.class"),
             new String[] {"java.lang.Object", "java.lang.String",
                           "java.util.Set", "java.util.HashSet",
                           "java.lang.management.ManagementFactory",
                           "java.lang.management.RuntimeMXBean",
                           "b.B", "c.C", "d.D", "f.F", "g.G"},
             new String[] {"compact1", "compact3", testDirBasename},
             new String[] {"-classpath", testDir.getPath(), "-verbose", "-P"});
        test(new File(mDir, "Foo.class"),
             new String[] {"c.I", "e.E", "f.F"},
             new String[] {testDirBasename},
             new String[] {"-classpath", testDir.getPath(), "-verbose:class", "-P"});
        test(new File(mDir, "Foo.class"),
             new String[] {"c.I", "e.E", "f.F", "m.Bar"},
             new String[] {testDirBasename},
             new String[] {"-classpath", testDir.getPath(), "-verbose:class", "-filter:none", "-P"});
        test(new File(mDir, "Gee.class"),
             new String[] {"g.G", "sun.security.x509.X509CertInfo", "com.sun.tools.classfile.ClassFile",
                           "com.sun.management.ThreadMXBean", "com.sun.source.tree.BinaryTree"},
             new String[] {testDirBasename, "JDK internal API", "compact3", ""},
             new String[] {"-classpath", testDir.getPath(), "-verbose", "-P"});

        // -jdkinternals
        test(new File(mDir, "Gee.class"),
             new String[] {"sun.security.x509.X509CertInfo", "com.sun.tools.classfile.ClassFile"},
             new String[] {"JDK internal API"},
             new String[] {"-jdkinternals", "-quiet"});
        // -jdkinternals parses all classes on -classpath and the input arguments
        test(new File(mDir, "Gee.class"),
             new String[] {"com.sun.tools.classfile.ClassFile",
                           "sun.security.x509.X509CertInfo"},
             new String[] {"JDK internal API"},
             // use -classpath tmp/a with no use of JDK internal API
             new String[] {"-classpath", dest.resolve("a").toString(), "-jdkinternals", "-quiet"});

        // parse only APIs
        test(mDir,
             new String[] {"java.lang.Object", "java.lang.String",
                           "java.util.Set",
                           "c.C", "d.D", "c.I", "e.E"},
             new String[] {"compact1", testDirBasename},
             new String[] {"-classpath", testDir.getPath(), "-verbose:class", "-P", "-apionly"});

        test(mDir,
             new String[] {"java.lang.Object", "java.lang.String",
                           "java.util.Set",
                           "c.C", "d.D", "c.I", "e.E", "m.Bar"},
             new String[] {"compact1", testDirBasename, mDir.getName()},
             new String[] {"-classpath", testDir.getPath(), "-verbose", "-P", "--api-only"});
        return errors;
    }

    void test(File file, String[] expect, String[] profiles) {
        test(file, expect, profiles, new String[0]);
    }

    void test(File file, String[] expect, String[] profiles, String[] options) {
        List<String> args = new ArrayList<>(Arrays.asList(options));
        if (file != null) {
            args.add(file.getPath());
        }
        checkResult("api-dependencies", expect, profiles,
                    jdeps(args.toArray(new String[0])));
    }

    Map<String,String> jdeps(String... args) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        System.err.println("jdeps " + Arrays.stream(args)
            .collect(Collectors.joining(" ")));
        int rc = com.sun.tools.jdeps.Main.run(args, pw);
        pw.close();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        if (rc != 0)
            throw new Error("jdeps failed: rc=" + rc);
        return findDeps(out);
    }

    // Pattern used to parse lines
    private static Pattern linePattern = Pattern.compile(".*\r?\n");
    private static Pattern pattern = Pattern.compile("\\s+ -> (\\S+) +(.*)");

    // Use the linePattern to break the given String into lines, applying
    // the pattern to each line to see if we have a match
    private static Map<String,String> findDeps(String out) {
        Map<String,String> result = new HashMap<>();
        Matcher lm = linePattern.matcher(out);  // Line matcher
        Matcher pm = null;                      // Pattern matcher
        int lines = 0;
        while (lm.find()) {
            lines++;
            CharSequence cs = lm.group();       // The current line
            if (pm == null)
                pm = pattern.matcher(cs);
            else
                pm.reset(cs);
            if (pm.find())
                result.put(pm.group(1), pm.group(2).trim());
            if (lm.end() == out.length())
                break;
        }
        return result;
    }

    void checkResult(String label, String[] expect, Collection<String> found) {
        List<String> list = Arrays.asList(expect);
        if (!isEqual(list, found))
            error("Unexpected " + label + " found: '" + found + "', expected: '" + list + "'");
    }

    void checkResult(String label, String[] expect, String[] profiles, Map<String,String> result) {
        // check the dependencies
        checkResult(label, expect, result.keySet());
        // check profile information
        Set<String> values = new TreeSet<>();
        String internal = "JDK internal API";
        for (String s: result.values()) {
            if (s.startsWith(internal)){
                values.add(internal);
            } else {
                values.add(s);
            }
        }
        checkResult(label, profiles, values);
    }

    boolean isEqual(List<String> expected, Collection<String> found) {
        if (expected.size() != found.size())
            return false;

        List<String> list = new ArrayList<>(found);
        list.removeAll(expected);
        return list.isEmpty();
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;

}
