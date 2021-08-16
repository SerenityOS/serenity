/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.module;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import build.tools.module.GenModuleInfoSource.Statement;

/**
 * Sanity test for GenModuleInfoSource tool
 */
public class ModuleInfoExtraTest {
    private static final Path DIR = Paths.get("gen-module-info-test");
    private static boolean verbose = false;
    public static void main(String... args) throws Exception {
        if (args.length != 0)
            verbose = true;

        GenModuleInfoSource.verbose = verbose;
        ModuleInfoExtraTest test = new ModuleInfoExtraTest("m", "m1", "m2", "m3");
        test.testModuleInfo();
        test.errorCases();
    }

    String[] moduleInfo = new String[] {
        "module m {",
        "    requires m1;",
        "    requires transitive m2;",
        "    exports p",
        "    to",
        "               // comment ... ",
        "    /* comment */ m1",
        ",",
        "       m2,m3",
        "  ;",
        "    exports q to m1;",
        "    provides s with /*   ",
        "    comment */ impl     ;    // comment",
        "    provides s1",
        "       with  ",
        "       impl1, impl2;",
        "}"
    };

    String[] moduleInfoExtra = new String[] {
        "exports q",
        "to",
        "   m2 // comment",
        "   /* comment */;",
        "   ;",
        "opens p.q ",
        "   to /* comment */ m3",
        "   , // m1",
        "   /* comment */ m4; uses p.I",
        ";   provides s1 with impl3;"
    };

    final Builder builder;
    ModuleInfoExtraTest(String name, String... modules) {
        this.builder = new Builder(name).modules(modules);
    }


    void testModuleInfo() throws IOException {
        GenModuleInfoSource source = builder.sourceFile(moduleInfo).build();
        Set<String> targetsP = new HashSet<>();
        targetsP.add("m1");
        targetsP.add("m2");
        targetsP.add("m3");

        Set<String> targetsQ = new HashSet<>();
        targetsQ.add("m1");

        Set<String> providerS = new HashSet<>();
        providerS.add("impl");

        Set<String> providerS1 = new HashSet<>();
        providerS1.add("impl1");
        providerS1.add("impl2");

        Set<String> opensPQ = new HashSet<>();

        check(source, targetsP, targetsQ, opensPQ, providerS, providerS1);

        // augment with extra
        Path file = DIR.resolve("extra");
        Files.write(file, Arrays.asList(moduleInfoExtra));
        source = builder.build(file);

        targetsQ.add("m2");
        providerS1.add("impl3");

        opensPQ.add("m3");
        check(source, targetsP, targetsQ, opensPQ, providerS, providerS1);
    }

    void check(GenModuleInfoSource source,
               Set<String> targetsP,
               Set<String> targetsQ,
               Set<String> opensPQ,
               Set<String> providerS,
               Set<String> providerS1) {
        if (verbose)
            source.moduleInfo.print(new PrintWriter(System.out, true));

        Statement export = source.moduleInfo.exports.get("p");
        if (!export.targets.equals(targetsP)) {
            throw new Error("unexpected: " + export);
        }

        export = source.moduleInfo.exports.get("q");
        if (!export.targets.equals(targetsQ)) {
            throw new Error("unexpected: " + export);
        }

        Statement provides = source.moduleInfo.provides.get("s");
        if (!provides.targets.equals(providerS)) {
            throw new Error("unexpected: " + provides);
        }

        provides = source.moduleInfo.provides.get("s1");
        if (!provides.targets.equals(providerS1)) {
            throw new Error("unexpected: " + provides);
        }
    }

    final Map<String[], String> badModuleInfos = Map.of(
        new String[] {
            "module x {",
            "   exports p1 to ",
            "           m1",
            "}"
        },                      ".*, line .*, missing semicolon.*",
        new String[] {
            "module x ",
            "   exports p1;"
        },                      ".*, line .*, missing \\{.*",
        new String[] {
            "module x {",
            "   requires m1;",
            "   requires",
            "}"
        },                      ".*, line .*, <identifier> missing.*",
        new String[] {
            "module x {",
            "   requires transitive m1",
            "}"
        },                      ".*, line .*, missing semicolon.*",
        new String[] {
            "module x {",
            "   exports p1 to m1;",
            "   exports p1 to m2;",
            "}"
        },                      ".*, line .*, multiple exports p1.*"
    );

    final Map<String[], String> badExtraFiles = Map.of(
            new String[] {
                "requires m2;"     // not allowed
            },                      ".*, line .*, cannot declare requires .*",
            new String[] {
                "exports p1 to m1;",
                "exports p2"            // missing semicolon
            },                      ".*, line .*, reach end of file.*",
            new String[] {
                "exports to m1;"        // missing <identifier>
            },                      ".*, line .*, <identifier> missing.*",
            new String[] {
                "exports p3 to m1;",
                "    m2, m3;"           // missing keyword
            },                      ".*, line .*, missing keyword.*",
            new String[] {
                "provides s with impl1;",   // typo ; should be ,
                "   impl2, impl3;"
            },                      ".*, line .*, missing keyword.*",
            new String[] {
                "uses s3",                  // missing semicolon
                "provides s3 with impl1,",
                "   impl2, impl3;"
            },                      ".*, line .*, missing semicolon.*",
            new String[] {
                "opens p1 to m1,, m2;"     // missing identifier
            },                      ".*, line .*, <identifier> missing.*"
    );

    final Map<String[], String> duplicates = Map.of(
            new String[] {
                "   exports p1 to m1, m2;",
                "   exports p1 to m3;",
            },                      ".*, line .*, multiple exports p1.*",
            new String[] {
                "   opens p1 to m1, m2;",
                "   exports p1 to m3;",
                "   opens p1 to m3;"
            },                      ".*, line .*, multiple opens p1.*",
            new String[] {
                "   uses s;",
                "   uses s;"
            },                      ".*, line .*, multiple uses s.*",
            new String[] {
                "   provides s with impl1;",
                "   provides s with impl2, impl3;"
            },                      ".*, line .*, multiple provides s.*"
    );

    void errorCases() {
        badModuleInfos.entrySet().stream().forEach(e -> badModuleInfoFile(e.getKey(), e.getValue()));
        badExtraFiles.entrySet().stream().forEach(e -> badExtraFile(e.getKey(), e.getValue()));
        duplicates.entrySet().stream().forEach(e -> badExtraFile(e.getKey(), e.getValue()));
    }

    void badModuleInfoFile(String[] lines, String regex)  {
        Builder builder = new Builder("x").modules("m1", "m2", "m3");
        try {
            GenModuleInfoSource source = builder.sourceFile(lines).build();
            throw new RuntimeException("Expected error: " + Arrays.toString(lines));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (Error e) {
            if (!e.getMessage().matches(regex)) {
                throw e;
            }
        }
    }

    void badExtraFile(String[] extras, String regex)  {
        Path file = DIR.resolve("test1");
        try {
            Files.write(file, Arrays.asList(extras));
            builder.build(file);
            Files.deleteIfExists(file);
            throw new RuntimeException("Expected error: " + Arrays.toString(extras));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (Error e) {
            if (!e.getMessage().matches(regex)) {
                throw e;
            }
        }
    }

    static class Builder {
        final String moduleName;
        final Path sourceFile;
        final Set<String> modules = new HashSet<>();
        public Builder(String name) {
            this.moduleName = name;
            this.sourceFile = DIR.resolve(name).resolve("module-info.java");
        }

        public Builder modules(String... names) {
            Arrays.stream(names).forEach(modules::add);
            return this;
        }

        public Builder sourceFile(String... lines) throws IOException {
            Files.createDirectories(sourceFile.getParent());
            try (BufferedWriter bw = Files.newBufferedWriter(sourceFile);
                 PrintWriter writer = new PrintWriter(bw)) {
                for (String l : lines) {
                    writer.println(l);
                }
            }
            return this;
        }

        public GenModuleInfoSource build() throws IOException {
            return build(Collections.emptyList());
        }

        public GenModuleInfoSource build(Path extraFile) throws IOException {
            return build(Collections.singletonList(extraFile));
        }

        public GenModuleInfoSource build(List<Path> extraFiles) throws IOException {
            return new GenModuleInfoSource(sourceFile, extraFiles, modules);
        }
    }

}
