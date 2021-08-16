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

/*
 * @test
 * @bug 8168254
 * @summary Detect duplicated resources in packaged modules
 * @modules jdk.jlink/jdk.tools.jlink.builder
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.plugin
 * @run build ResourceDuplicateCheckTest
 * @run main ResourceDuplicateCheckTest
 */

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import jdk.tools.jlink.builder.DefaultImageBuilder;
import jdk.tools.jlink.internal.ResourcePoolEntryFactory;
import jdk.tools.jlink.internal.ResourcePoolManager;
import jdk.tools.jlink.plugin.PluginException;
import jdk.tools.jlink.plugin.ResourcePoolEntry;

public class ResourceDuplicateCheckTest {
    public static void main(String[] args) throws Exception {
        new ResourceDuplicateCheckTest().test();
    }

    public void test() throws Exception {
        ResourcePoolManager input = new ResourcePoolManager();
        // need java.base module info because OS name is retrieved from it from storeFiles
        input.add(ResourcePoolEntryFactory.create("/java.base/module-info.class",
                    ResourcePoolEntry.Type.CLASS_OR_RESOURCE, getJavaBaseModuleInfo()));

        // same NATIVE_CMD from two different modules
        input.add(newInMemoryImageFile("/com.acme/bin/myexec",
                    ResourcePoolEntry.Type.NATIVE_CMD, "mylib"));
        input.add(newInMemoryImageFile("/com.foo/bin/myexec",
                    ResourcePoolEntry.Type.NATIVE_CMD, "mylib"));
        Path root = Paths.get(System.getProperty("test.classes"));
        DefaultImageBuilder writer = new DefaultImageBuilder(root, Collections.emptyMap());
        try {
            writer.storeFiles(input.resourcePool());
        } catch (PluginException pe) {
            if (! pe.getMessage().contains("Duplicate resources:")) {
                throw new AssertionError("expected duplicate resources message");
            }
        }
    }

    private byte[] getJavaBaseModuleInfo() throws Exception {
        Path path = FileSystems.
                getFileSystem(URI.create("jrt:/")).
                getPath("/modules/java.base/module-info.class");
        return Files.readAllBytes(path);
    }

    private static ResourcePoolEntry newInMemoryImageFile(String path,
            ResourcePoolEntry.Type type, String content) {
        return ResourcePoolEntryFactory.create(path, type, content.getBytes());
    }
}
