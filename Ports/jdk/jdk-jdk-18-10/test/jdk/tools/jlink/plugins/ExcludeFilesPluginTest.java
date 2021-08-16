/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test exclude files plugin
 * @author Jean-Francois Denise
 * @modules jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.internal.plugins
 *          jdk.jlink/jdk.tools.jlink.plugin
 * @run main ExcludeFilesPluginTest
 */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import jdk.tools.jlink.internal.ResourcePoolManager;

import jdk.tools.jlink.internal.plugins.ExcludeFilesPlugin;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolEntry;

public class ExcludeFilesPluginTest {
    public static void main(String[] args) throws Exception {
        new ExcludeFilesPluginTest().test();
    }

    public void test() throws Exception {
        checkFiles("**.jcov", "num/toto.jcov", "", true);
        checkFiles("**.jcov", "/toto.jcov", "", true);
        checkFiles("**.jcov", "toto.jcov/tutu/tata", "", false);
        checkFiles("/java.base/*.jcov", "toto.jcov", "java.base", true);
        checkFiles("/java.base/toto.jcov", "iti.jcov", "t/java.base", false);
        checkFiles("/java.base/*/toto.jcov", "toto.jcov", "java.base", false);
        checkFiles("/java.base/*/toto.jcov", "tutu/toto.jcov", "java.base", true);
        checkFiles("**/java.base/*/toto.jcov", "java.base/tutu/toto.jcov", "/tutu", true);

        checkFiles("/**$*.properties", "tutu/Toto$Titi.properties", "java.base", true);
        checkFiles("**$*.properties", "tutu/Toto$Titi.properties", "java.base", true);

        // Excluded files list in a file
        File order = new File("files.exc");
        order.createNewFile();
        Files.write(order.toPath(), "**.jcov".getBytes());
        checkFiles("@" + order.getAbsolutePath(), "/num/toto.jcov", "", true);
    }

    public void checkFiles(String s, String sample, String module, boolean exclude) throws Exception {
        Map<String, String> prop = new HashMap<>();
        ExcludeFilesPlugin fplug = new ExcludeFilesPlugin();
        prop.put(fplug.getName(), s);
        fplug.configure(prop);
        ResourcePoolManager files = new ResourcePoolManager();
        ResourcePoolManager fresult = new ResourcePoolManager();
        ResourcePoolEntry f = ResourcePoolEntry.create("/" + module + "/" + sample,
                ResourcePoolEntry.Type.CONFIG, new byte[0]);
        files.add(f);

        ResourcePool resPool = fplug.transform(files.resourcePool(), fresult.resourcePoolBuilder());

        if (exclude) {
            if (resPool.contains(f)) {
                throw new Exception(sample + " should be excluded by " + s);
            }
        } else {
            if (!resPool.contains(f)) {
                throw new Exception(sample + " shouldn't be excluded by " + s);
            }
        }
    }
}
