/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test;

import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/*
 * Main class to verify if ModuleDescriptor carries the correct version
 */
public class Main {
    static final Map<String, String> nameToVersion = new HashMap<>();

    // jdk.test.Main $count $module-name... $version...
    public static void main(String... args) throws Exception {
        int count = args.length > 0 ? Integer.valueOf(args[0]) : 0;
        if (count < 1 || args.length != count*2+1) {
            throw new IllegalArgumentException(Arrays.toString(args));
        }

        List<String> modules = List.of(Arrays.copyOfRange(args, 1, 1+count));
        List<String> versions = List.of(Arrays.copyOfRange(args, 1+count, args.length));
        for (int i=0; i < modules.size(); i++) {
            System.out.format("module %s expects %s version%n",
                              modules.get(i), versions.get(i));
            nameToVersion.put(modules.get(i), versions.get(i));
        }

        FileSystem fs = FileSystems.newFileSystem(URI.create("jrt:/"),
                                                  Collections.emptyMap());
        // check the module descriptor of a system module
        for (int i=0; i < modules.size(); i++) {
            String mn = modules.get(i);
            Module module = ModuleLayer.boot().findModule(mn).orElseThrow(
                () -> new RuntimeException(mn + " not found")
            );

            // check ModuleDescriptor from the run-time
            validate(module.getDescriptor());

            // check module-info.class in the image
            Path path = fs.getPath("/", "modules", modules.get(i), "module-info.class");
            validate(ModuleDescriptor.read(Files.newInputStream(path)));
        }
    }

    static void validate(ModuleDescriptor descriptor) {
        checkVersion(descriptor.name(), descriptor.version());
        descriptor.requires()
            .stream()
            .filter(r -> !r.name().equals("java.base"))
            .forEach(r -> checkVersion(r.name(), r.compiledVersion()));
    }

    static void checkVersion(String mn, Optional<ModuleDescriptor.Version> version) {
        boolean matched;
        String v = nameToVersion.get(mn);
        if (version.isPresent()) {
            matched = version.get().toString().equals(v);
        } else {
            // 0 indicate no version
            matched = v.equals("0");
        }

        if (!matched) {
            throw new RuntimeException(mn + " mismatched version " + version
                + " expected: " + v);
        }
    }
}
