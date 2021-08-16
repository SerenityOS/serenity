/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

package p1;

import java.io.InputStream;
import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import jdk.internal.module.ModuleInfo;
import jdk.internal.module.ModuleInfo.Attributes;

public class Main {
    private static boolean hasModuleTarget(InputStream in) throws IOException {
        ModuleInfo.Attributes attrs = ModuleInfo.read(in, null);
        return attrs.target() != null;
    }

    public static void main(String... args) throws Exception {
        // load another package
        p2.T.test();

        // validate the module descriptor
        validate(Main.class.getModule());

        // validate the Moduletarget attribute for java.base
        FileSystem fs = FileSystems.newFileSystem(URI.create("jrt:/"),
                                                  Collections.emptyMap());
        Path path = fs.getPath("/", "modules", "java.base", "module-info.class");
        try (InputStream in = Files.newInputStream(path)) {
            if (! hasModuleTarget(in)) {
                throw new RuntimeException("Missing ModuleTarget for java.base");
            }
        }
    }

    static void validate(Module module) throws IOException {
        ModuleDescriptor md = module.getDescriptor();

        // read m1/module-info.class
        FileSystem fs = FileSystems.newFileSystem(URI.create("jrt:/"),
                                                  Collections.emptyMap());
        Path path = fs.getPath("/", "modules", module.getName(), "module-info.class");
        ModuleDescriptor md1 = ModuleDescriptor.read(Files.newInputStream(path));


        // check the module descriptor of a system module and read from jimage
        checkPackages(md.packages(), "p1", "p2");
        checkPackages(md1.packages(), "p1", "p2");

        try (InputStream in = Files.newInputStream(path)) {
            checkModuleTargetAttribute(in, "p1");
        }
    }

    static void checkPackages(Set<String> pkgs, String... expected) {
        if (!pkgs.equals(Set.of(expected))) {
            throw new RuntimeException(pkgs + " expected: " + Set.of(expected));
        }
    }

    static void checkModuleTargetAttribute(InputStream in, String modName) throws IOException {
        if (hasModuleTarget(in)) {
            throw new RuntimeException("ModuleTarget present for " + modName);
        }
    }
}
