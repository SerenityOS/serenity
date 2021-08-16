/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8233922
 * @modules java.base/jdk.internal.module
 * @library /test/lib
 * @build ServiceBinding TestBootLayer
 * @run testng ServiceBinding
 * @summary Test service binding with incubator modules
 */

import java.io.File;
import java.io.OutputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ResolvedModule;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream;

import static java.lang.module.ModuleDescriptor.newModule;

import jdk.internal.module.ModuleInfoWriter;
import jdk.internal.module.ModuleResolution;

import org.testng.annotations.Test;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

@Test
public class ServiceBinding {
    private static final Path HERE = Path.of(".");

    /**
     * module m1 uses p.S
     * (incubating) module m2 requires m1 provides p.S
     */
    public void test1() throws Exception {
        Path mlib = Files.createTempDirectory(HERE, "mlib");

        var m1 = newModule("m1").exports("p").uses("p.S").build();
        var m2 = newModule("m2").requires("m1").provides("p.S", List.of("impl.S1")).build();

        writeModule(mlib, m1);
        writeIncubatingModule(mlib, m2);

        // boot layer: root=m1, incubator module m2 should not be resolved
        testBootLayer(mlib, Set.of("m1"), Set.of("m1"), Set.of("m2"))
                .shouldNotMatch("WARNING:.*m2");

        // custom configuration: root=m1, incubator module m2 should be resolved
        testCustomConfiguration(mlib, Set.of("m1"), Set.of("m2"));
    }

    /**
     * module m1 uses p.S
     * (incubating) module m2 requires m1 provides P.S uses q.S
     * (incubating) module m3 requires m2 provides q.S
     */
    public void test2() throws Exception {
        Path mlib = Files.createTempDirectory("mlib");

        var m1 = newModule("m1").exports("p").uses("p.S").build();
        var m2 = newModule("m2")
                .requires("m1")
                .provides("p.S", List.of("impl.S1"))
                .exports("q")
                .uses("q.S")
                .build();
        var m3 = newModule("m3").requires("m2").provides("q.S", List.of("impl.S1")).build();

        writeModule(mlib, m1);
        writeIncubatingModule(mlib, m2);
        writeIncubatingModule(mlib, m3);

        // boot layer: root=m1, incubator modules m2 and m3 should not be resolved
        testBootLayer(mlib, Set.of("m1"), Set.of("m1"), Set.of("m2", "m3"))
                .shouldNotMatch("WARNING:.*m2")
                .shouldNotMatch("WARNING:.*m3");

        // boot layer: root=m2, incubator module m3 should not be resolved
        testBootLayer(mlib, Set.of("m2"), Set.of("m1", "m2"), Set.of("m3"))
                .shouldMatch("WARNING:.*m2")
                .shouldNotMatch("WARNING:.*m3");

        // custom configuration: root=m1, incubator modules m2 and m3 should be resolved
        testCustomConfiguration(mlib, Set.of("m1"), Set.of("m1", "m2", "m3"));

        // custom configuration: root=m2, incubator module m3 should be resolved
        testCustomConfiguration(mlib, Set.of("m2"), Set.of("m1", "m2", "m3"));
    }

    /**
     * Creates an exploded module on the file system.
     *
     * @param mlib the top-level module directory
     * @param descriptor the module descriptor of the module to write
     */
    void writeModule(Path mlib, ModuleDescriptor descriptor) throws Exception {
        writeModule(mlib, descriptor, false);
    }

    /**
     * Creates an exploded module on the file system. The module will be an
     * incubating module.
     *
     * @param mlib the top-level module directory
     * @param descriptor the module descriptor of the module to write
     */
    void writeIncubatingModule(Path mlib, ModuleDescriptor descriptor) throws Exception {
        writeModule(mlib, descriptor, true);
    }

    /**
     * Creates an exploded module on the file system.
     *
     * @param mlib the top-level module directory
     * @param descriptor the module descriptor of the module to write
     * @param incubating to create an incubating module
     */
    void writeModule(Path mlib, ModuleDescriptor descriptor, boolean incubating)
        throws Exception
    {
        // create ModuleResolution attribute if incubating module
        ModuleResolution mres = (incubating) ? ModuleResolution.empty().withIncubating() : null;
        String name = descriptor.name();

        // create directory for module
        Path dir = Files.createDirectory(mlib.resolve(name));

        // module-info.class
        try (OutputStream out = Files.newOutputStream(dir.resolve("module-info.class"))) {
            ModuleInfoWriter.write(descriptor, mres, out);
        }

        // create a dummy class file for each package
        for (String pn : descriptor.packages()) {
            Path subdir = dir.resolve(pn.replace('.', File.separatorChar));
            Files.createDirectories(subdir);
            Files.createFile(subdir.resolve("C.class"));
        }
    }

    /**
     * Run TestBootLayer in a child VM with the given module path and the
     * --add-modules option with additional root modules. TestBootLayer checks
     * the modules in the boot layer.
     *
     * @param mlib the module path
     * @param roots the modules to specify to --add-modules
     * @param expected the names of modules that should be in the boot layer
     * @param notExpected the names of modules that should not be in boot layer
     */
    OutputAnalyzer testBootLayer(Path mlib,
                                 Set<String> roots,
                                 Set<String> expected,
                                 Set<String> notExpected)
        throws Exception
    {
        var opts = Stream.of("-p", mlib.toString(),
                             "--add-modules", commaSeparated(roots),
                             "TestBootLayer", commaSeparated(expected), commaSeparated(notExpected));
        return ProcessTools.executeTestJava(opts.toArray(String[]::new))
                .outputTo(System.out)
                .errorTo(System.out)
                .shouldHaveExitValue(0);
    }

    /**
     * Creates a Configuration by resolving a set of root modules, with service
     * binding, then checks that the Configuration includes the expected modules.
     *
     * @param mlib the module path
     * @param roots the names of the root modules
     * @param expected the names of modules that should be in the configuration
     */
    void testCustomConfiguration(Path mlib, Set<String> roots, Set<String> expected) {
        ModuleFinder finder = ModuleFinder.of(mlib);
        Configuration cf = ModuleLayer.boot()
                .configuration()
                .resolveAndBind(finder, ModuleFinder.of(), roots);

        Set<String> modules = cf.modules().stream()
                .map(ResolvedModule::name)
                .collect(Collectors.toSet());

        expected.stream()
                .filter(mn -> !modules.contains(mn))
                .findAny()
                .ifPresent(mn -> {
                    throw new RuntimeException(mn + " not in configuration!!!");
                });
    }

    String commaSeparated(Set<String> s) {
        return s.stream().collect(Collectors.joining(","));
    }
}
