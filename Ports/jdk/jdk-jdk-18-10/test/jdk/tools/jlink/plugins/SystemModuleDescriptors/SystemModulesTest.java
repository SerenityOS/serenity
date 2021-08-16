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

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.*;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jdk.internal.access.JavaLangModuleAccess;
import jdk.internal.access.SharedSecrets;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8142968 8173381
 * @modules java.base/jdk.internal.access
 * @modules java.base/jdk.internal.module
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @build ModuleTargetHelper
 * @run testng SystemModulesTest
 * @summary Verify the properties of ModuleDescriptor created
 *          by SystemModules
 */

public class SystemModulesTest {
    private static final JavaLangModuleAccess JLMA =
        SharedSecrets.getJavaLangModuleAccess();
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String OS_ARCH = System.getProperty("os.arch");
    //  system modules containing no package
    private static final Set<String> EMPTY_MODULES =
        Set.of("java.se", "jdk.jdwp.agent", "jdk.pack");

    @Test
    public void testSystemModules() {
        Path jimage = Paths.get(System.getProperty("java.home"), "lib", "modules");
        if (Files.notExists(jimage))
            return;

        ModuleFinder.ofSystem().findAll().stream()
                    .forEach(this::checkAttributes);
    }

    // JMOD files are created with OS name and arch matching the bundle name
    private boolean checkOSName(String name) {
        if (OS_NAME.startsWith("Windows")) {
            return name.equals("windows");
        }

        switch (OS_NAME) {
            case "Linux":
                return name.equals("linux");
            case "Mac OS X":
                return name.equals("macos");
            default:
                // skip validation on unknown platform
                System.out.println("Skip checking OS name in ModuleTarget: " + name);
                return true;
        }
    }

    private boolean checkOSArch(String name) {
        if (name.equals(OS_ARCH))
            return true;

        switch (OS_ARCH) {
            case "i386":
            case "x86":
                return name.equals("x86");
            case "amd64":
            case "x86_64":
                return name.equals("amd64");
            default:
                // skip validation on unknown platform
                System.out.println("Skip checking OS arch in ModuleTarget: " + name);
                return true;
        }
    }

    private void checkAttributes(ModuleReference modRef) {
        try {
            ModuleTargetHelper.ModuleTarget mt = ModuleTargetHelper.read(modRef);
            String[] values = mt.targetPlatform().split("-");
            assertTrue(checkOSName(values[0]));
            assertTrue(checkOSArch(values[1]));
        } catch (IOException exp) {
            throw new UncheckedIOException(exp);
        }
    }

    /**
     * Verify ModuleDescriptor contains unmodifiable sets
     */
    @Test
    public void testUnmodifableDescriptors() throws Exception {
        ModuleFinder.ofSystem().findAll()
                    .stream()
                    .map(ModuleReference::descriptor)
                    .forEach(this::testModuleDescriptor);
    }

    private void testModuleDescriptor(ModuleDescriptor md) {
        assertUnmodifiable(md.packages(), "package");
        assertUnmodifiable(md.requires(),
                           JLMA.newRequires(Set.of(Requires.Modifier.TRANSITIVE),
                                            "require", null));
        for (Requires req : md.requires()) {
            assertUnmodifiable(req.modifiers(), Requires.Modifier.TRANSITIVE);
        }

        assertUnmodifiable(md.exports(), JLMA.newExports(Set.of(), "export", Set.of()));
        for (Exports exp : md.exports()) {
            assertUnmodifiable(exp.modifiers(), Exports.Modifier.SYNTHETIC);
            assertUnmodifiable(exp.targets(), "target");
        }

        assertUnmodifiable(md.opens(), JLMA.newOpens(Set.of(), "open", Set.of()));
        for (Opens opens : md.opens()) {
            assertUnmodifiable(opens.modifiers(), Opens.Modifier.SYNTHETIC);
            assertUnmodifiable(opens.targets(), "target");
        }

        assertUnmodifiable(md.uses(), "use");

        assertUnmodifiable(md.provides(),
                           JLMA.newProvides("provide", List.of("provide")));
        for (Provides provides : md.provides()) {
            assertUnmodifiable(provides.providers(), "provide");
        }

    }

    private <T> void assertUnmodifiable(Set<T> set, T dummy) {
        try {
            set.add(dummy);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // pass
        } catch (Exception e) {
            fail("Should throw UnsupportedOperationException");
        }
    }

    private <T> void assertUnmodifiable(List<T> list, T dummy) {
        try {
            list.add(dummy);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // pass
        } catch (Exception e) {
            fail("Should throw UnsupportedOperationException");
        }
    }

    private <T, V> void assertUnmodifiable(Map<T, V> set, T dummyKey, V dummyValue) {
        try {
            set.put(dummyKey, dummyValue);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // pass
        } catch (Exception e) {
            fail("Should throw UnsupportedOperationException");
        }
    }

}
