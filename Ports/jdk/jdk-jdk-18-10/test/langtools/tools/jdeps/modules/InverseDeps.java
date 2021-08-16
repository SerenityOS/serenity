/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests split packages
 * @library ../lib
 * @build CompilerUtils JdepsUtil
 * @modules java.logging
 *          jdk.jdeps/com.sun.tools.jdeps
 *          jdk.unsupported
 * @run testng InverseDeps
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.tools.jdeps.Archive;
import com.sun.tools.jdeps.InverseDepsAnalyzer;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;


public class InverseDeps {
    private static final String TEST_SRC = System.getProperty("test.src");
    private static final String TEST_CLASSES = System.getProperty("test.classes");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path LIBS_DIR = Paths.get("libs");

    private static final Set<String> modules = new LinkedHashSet(
        List.of("unsafe", "mIV", "mV", "mVI", "mVII")
    );

    /**
     * Compiles classes used by the test
     */
    @BeforeTest
    public void compileAll() throws Exception {
        CompilerUtils.cleanDir(MODS_DIR);

        for (String mn : modules) {
            // compile a module
            assertTrue(CompilerUtils.compileModule(SRC_DIR, MODS_DIR, mn));

            // create JAR files with no module-info.class
            Path root = MODS_DIR.resolve(mn);

            try (Stream<Path> stream = Files.walk(root, Integer.MAX_VALUE)) {
                Stream<Path> entries = stream.filter(f -> {
                    String fn = f.getFileName().toString();
                    return fn.endsWith(".class") && !fn.equals("module-info.class");
                });
                JdepsUtil.createJar(LIBS_DIR.resolve(mn + ".jar"), root, entries);
            }
        }
    }
    @DataProvider(name = "jdkModules")
    public Object[][] jdkModules() {
        return new Object[][]{
            // --require and a subset of dependencies
            { "jdk.compiler", new String[][] {
                    new String[] {"jdk.compiler", "jdk.jshell"},
                    new String[] {"jdk.compiler", "jdk.javadoc"},
                }
            },
            { "java.compiler", new String[][] {
                    new String[] {"java.compiler", "jdk.jshell"},
                    new String[] {"java.compiler", "jdk.compiler", "jdk.jshell"},
                    new String[] {"java.compiler", "jdk.compiler"},
                    new String[] {"java.compiler", "jdk.compiler", "jdk.javadoc"},
                    new String[] {"java.compiler", "java.se"},
                }
            },
        };
    }

    @Test(dataProvider = "jdkModules")
    public void testJDKModule(String moduleName, String[][] expected) throws Exception {
        // this invokes the jdeps launcher so that all system modules are observable
        JdepsRunner jdeps = JdepsRunner.run(
            "--inverse", "--require", moduleName
        );
        List<String> output = Arrays.stream(jdeps.output())
            .map(s -> s.trim())
            .collect(Collectors.toList());

        // verify the dependences
        assertTrue(Arrays.stream(expected)
                         .map(path -> Arrays.stream(path)
                         .collect(Collectors.joining(" <- ")))
                         .anyMatch(output::contains));
    }


    @DataProvider(name = "testrequires")
    public Object[][] expected1() {
        return new Object[][] {
            // --require and result
            { "java.sql", new String[][] {
                    new String[] { "java.sql", "mV" },
                }
            },
            { "java.compiler", new String[][] {
                    new String[] { "java.compiler", "mV" },
                    new String[] { "java.compiler", "mIV", "mV" },
                }
            },
            { "java.logging", new String[][]{
                    new String[] {"java.logging", "mV"},
                    new String[] {"java.logging", "mIV", "mV"},
                    new String[] {"java.logging", "java.sql", "mV"},
                }
            },
            { "jdk.unsupported", new String[][] {
                    new String[] {"jdk.unsupported", "unsafe", "mVI", "mVII"},
                    new String[] {"jdk.unsupported", "unsafe", "mVII"}
                }
            },
        };
    }

    @Test(dataProvider = "testrequires")
    public void testrequires(String name, String[][] expected) throws Exception {
        String cmd1 = String.format("jdeps --inverse --module-path %s --require %s --add-modules %s%n",
                MODS_DIR, name, modules.stream().collect(Collectors.joining(",")));

        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd1)) {
            jdeps.appModulePath(MODS_DIR.toString())
                .addmods(modules)
                .requires(Set.of(name));

            runJdeps(jdeps, expected);
        }

        String cmd2 = String.format("jdeps --inverse --module-path %s --require %s" +
            " --add-modules ALL-MODULE-PATH%n", LIBS_DIR, name);

            // automatic module
        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd2)) {
            jdeps.appModulePath(MODS_DIR.toString())
                .addmods(Set.of("ALL-MODULE-PATH"))
                .requires(Set.of(name));

            runJdeps(jdeps, expected);
        }
    }

    @DataProvider(name = "testpackage")
    public Object[][] expected2() {
        return new Object[][] {
            // -package and result
            { "p4", new String[][] {
                        new String[] { "mIV", "mV"},
                    }
            },
            { "javax.tools", new String[][] {
                        new String[] {"java.compiler", "mV"},
                        new String[] {"java.compiler", "mIV", "mV"},
                    }
            },
            { "sun.misc", new String[][] {
                        new String[] {"jdk.unsupported", "unsafe", "mVI", "mVII"},
                        new String[] {"jdk.unsupported", "unsafe", "mVII"}
                    }
            }
        };
    }

    @Test(dataProvider = "testpackage")
    public void testpackage(String name, String[][] expected) throws Exception {
        String cmd = String.format("jdeps --inverse --module-path %s -package %s --add-modules %s%n",
            MODS_DIR, name, modules.stream().collect(Collectors.joining(",")));
        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd)) {
            jdeps.appModulePath(MODS_DIR.toString())
                .addmods(modules)
                .matchPackages(Set.of(name));

            runJdeps(jdeps, expected);
        }
    }

    @DataProvider(name = "testregex")
    public Object[][] expected3() {
        return new Object[][] {
            // -regex and result
            { "org.safe.Lib", new String[][] {
                    new String[] { "unsafe", "mVII"},
                    new String[] { "unsafe", "mVI", "mVII"},
                }
            },
            { "java.util.logging.*|org.safe.Lib", new String[][] {
                    new String[] { "unsafe", "mVII"},
                    new String[] { "unsafe", "mVI", "mVII"},
                    new String[] { "java.logging", "mV"},
                }
            }
        };
    }

    @Test(dataProvider = "testregex")
    public void testregex(String name, String[][] expected) throws Exception {
        String cmd = String.format("jdeps --inverse --module-path %s -regex %s --add-modules %s%n",
                MODS_DIR, name, modules.stream().collect(Collectors.joining(",")));

        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd)) {
            jdeps.appModulePath(MODS_DIR.toString())
                .addmods(modules)
                .regex(name);

            runJdeps(jdeps, expected);
        }
    }

    @DataProvider(name = "classpath")
    public Object[][] expected4() {
        return new Object[][] {
            // -regex and result
            { "sun.misc.Unsafe", new String[][] {
                    new String[] {"jdk.unsupported", "unsafe.jar", "mVI.jar", "mVII.jar"},
                    new String[] {"jdk.unsupported", "unsafe.jar", "mVII.jar"}
                }
            },
            { "org.safe.Lib", new String[][] {
                    new String[] { "unsafe.jar", "mVII.jar"},
                    new String[] { "unsafe.jar", "mVI.jar", "mVII.jar"},
                }
            },
            { "java.util.logging.*|org.safe.Lib", new String[][] {
                    new String[] { "unsafe.jar", "mVII.jar"},
                    new String[] { "unsafe.jar", "mVI.jar", "mVII.jar"},
                    new String[] { "java.logging", "mV.jar"},
                }
            }
        };
    }

    @Test(dataProvider = "classpath")
    public void testClassPath(String name, String[][] expected) throws Exception {
        // -classpath
        String cpath = modules.stream()
            .filter(mn -> !mn.equals("mVII"))
            .map(mn -> LIBS_DIR.resolve(mn + ".jar").toString())
            .collect(Collectors.joining(File.pathSeparator));

        Path jarfile = LIBS_DIR.resolve("mVII.jar");

        String cmd1 = String.format("jdeps --inverse -classpath %s -regex %s %s%n",
            cpath, name, jarfile);
        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd1)) {
            jdeps.verbose("-verbose:class")
                .addClassPath(cpath)
                .regex(name).addRoot(jarfile);
            runJdeps(jdeps, expected);
        }

        // all JAR files on the command-line arguments
        Set<Path> paths = modules.stream()
                                 .map(mn -> LIBS_DIR.resolve(mn + ".jar"))
                                 .collect(Collectors.toSet());
        String cmd2 = String.format("jdeps --inverse -regex %s %s%n", name, paths);
        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd2)) {
            jdeps.verbose("-verbose:class").regex(name);
            paths.forEach(jdeps::addRoot);
            runJdeps(jdeps, expected);
        }
    }

    private void runJdeps(JdepsUtil.Command jdeps, String[][] expected)  throws Exception {
        InverseDepsAnalyzer analyzer = jdeps.getInverseDepsAnalyzer();

        assertTrue(analyzer.run());

        // get the inverse transitive dependences
        List<String[]> paths = analyzer.inverseDependences().stream()
            .map(deque -> deque.stream()
                               .map(Archive::getName)
                               .collect(Collectors.toList()).toArray(new String[0]))
            .collect(Collectors.toList());

        jdeps.dumpOutput(System.err);
        paths.forEach(path -> System.err.println(Arrays.stream(path)
                .collect(Collectors.joining(" <- "))));

        // verify the dependences
        assertEquals(paths.size(), expected.length);

        for (int i=0; i < paths.size(); i++) {
            String[] path = paths.get(i);
            boolean noneMatched = Arrays.stream(expected)
                    .filter(array -> array.length == path.length)
                    .noneMatch(array -> Arrays.equals(array, path));
            if (noneMatched)
                System.err.format("Expected: %s found: %s%n",
                                  Arrays.stream(expected)
                                      .map(Arrays::toString)
                                      .collect(Collectors.joining(", ")),
                    Arrays.toString(path));

            assertFalse(noneMatched);
        }
    }

}
