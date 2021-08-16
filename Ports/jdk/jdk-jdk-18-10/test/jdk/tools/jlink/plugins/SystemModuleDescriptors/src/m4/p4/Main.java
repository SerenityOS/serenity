/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package p4;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import jdk.internal.module.ModuleInfo;
import jdk.internal.module.ModuleInfo.Attributes;

public class Main {
    private static boolean hasModuleTarget(InputStream in) throws IOException {
        ModuleInfo.Attributes attrs = ModuleInfo.read(in, null);
        return attrs.target() != null;
    }

    private static boolean hasModuleTarget(String modName) throws IOException {
        FileSystem fs = FileSystems.newFileSystem(URI.create("jrt:/"), Map.of());
        Path path = fs.getPath("/", "modules", modName, "module-info.class");
        try (InputStream in = Files.newInputStream(path)) {
            return hasModuleTarget(in);
        }
    }

    // the system module plugin by default drops ModuleTarget attribute
    private static boolean expectModuleTarget = false;
    public static void main(String... args) throws IOException {
        if (args.length > 0) {
            if (!args[0].equals("retainModuleTarget")) {
                throw new IllegalArgumentException(args[0]);
            }

            expectModuleTarget = true;
        }

        // java.base is packaged with ModuleTarget
        if (!hasModuleTarget("java.base")) {
            throw new RuntimeException("ModuleTarget absent for java.base");
        }

        // verify module-info.class for m1 and m4
        checkModule("m1", "p1", "p2");
        checkModule("m4", "p4");
    }

    private static void checkModule(String mn, String... packages) throws IOException {
        // verify ModuleDescriptor from the runtime module
        ModuleDescriptor md = ModuleLayer.boot().findModule(mn).get()
                                   .getDescriptor();
        checkModuleDescriptor(md, packages);

        // verify ModuleDescriptor from module-info.class read from ModuleReader
        try (InputStream in = ModuleFinder.ofSystem().find(mn).get()
            .open().open("module-info.class").get()) {
            checkModuleDescriptor(ModuleDescriptor.read(in), packages);
        }

        // verify ModuleDescriptor from module-info.class read from jimage
        FileSystem fs = FileSystems.newFileSystem(URI.create("jrt:/"), Map.of());
        Path path = fs.getPath("/", "modules", mn, "module-info.class");
        checkModuleDescriptor(ModuleDescriptor.read(Files.newInputStream(path)), packages);
    }

    static void checkModuleDescriptor(ModuleDescriptor md, String... packages) throws IOException {
        String mainClass = md.name().replace('m', 'p') + ".Main";
        if (!md.mainClass().get().equals(mainClass)) {
            throw new RuntimeException(md.mainClass().toString());
        }

        // ModuleTarget attribute should be present
        if (!hasModuleTarget(md.name())) {
            throw new RuntimeException("ModuleTarget missing for " + md.name());
        }

        Set<String> pkgs = md.packages();
        if (!pkgs.equals(Set.of(packages))) {
            throw new RuntimeException(pkgs + " expected: " + Set.of(packages));
        }
    }
}
