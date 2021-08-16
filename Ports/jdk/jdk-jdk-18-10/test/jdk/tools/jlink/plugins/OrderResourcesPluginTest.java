/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test sorter plugin
 * @author Jean-Francois Denise
 * @modules jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.internal.plugins
 *          jdk.jlink/jdk.tools.jlink.plugin
 * @run main OrderResourcesPluginTest
 */

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jdk.tools.jlink.internal.ResourcePoolManager;
import jdk.tools.jlink.internal.plugins.OrderResourcesPlugin;
import jdk.tools.jlink.plugin.ResourcePoolEntry;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.Plugin;

public class OrderResourcesPluginTest {

    public static void main(String[] args) throws Exception {
        new OrderResourcesPluginTest().test();
    }

    public void test() throws Exception {
        ResourcePoolEntry[] array = {
                ResourcePoolEntry.create("/module1/toto1.class", new byte[0]),
                ResourcePoolEntry.create("/module2/toto2.class", new byte[0]),
                ResourcePoolEntry.create("/module3/toto3.class", new byte[0]),
                ResourcePoolEntry.create("/module3/toto3/module-info.class", new byte[0]),
                ResourcePoolEntry.create("/zazou/toto.class", new byte[0]),
                ResourcePoolEntry.create("/module4/zazou.class", new byte[0]),
                ResourcePoolEntry.create("/module5/toto5.class", new byte[0]),
                ResourcePoolEntry.create("/module6/toto6/module-info.class", new byte[0])
        };

        ResourcePoolEntry[] sorted = {
                ResourcePoolEntry.create("/zazou/toto.class", new byte[0]),
                ResourcePoolEntry.create("/module3/toto3/module-info.class", new byte[0]),
                ResourcePoolEntry.create("/module6/toto6/module-info.class", new byte[0]),
                ResourcePoolEntry.create("/module1/toto1.class", new byte[0]),
                ResourcePoolEntry.create("/module2/toto2.class", new byte[0]),
                ResourcePoolEntry.create("/module3/toto3.class", new byte[0]),
                ResourcePoolEntry.create("/module4/zazou.class", new byte[0]),
                ResourcePoolEntry.create("/module5/toto5.class", new byte[0])
        };

        ResourcePoolEntry[] sorted2 = {
            ResourcePoolEntry.create("/module5/toto5.class", new byte[0]),
            ResourcePoolEntry.create("/module6/toto6/module-info.class", new byte[0]),
            ResourcePoolEntry.create("/module4/zazou.class", new byte[0]),
            ResourcePoolEntry.create("/module3/toto3.class", new byte[0]),
            ResourcePoolEntry.create("/module3/toto3/module-info.class", new byte[0]),
            ResourcePoolEntry.create("/module1/toto1.class", new byte[0]),
            ResourcePoolEntry.create("/module2/toto2.class", new byte[0]),
            ResourcePoolEntry.create("/zazou/toto.class", new byte[0])
        };

        ResourcePoolManager resources = new ResourcePoolManager();
        for (ResourcePoolEntry r : array) {
            resources.add(r);
        }

        {
            ResourcePoolManager out = new ResourcePoolManager();
            Map<String, String> config = new HashMap<>();
            config.put("order-resources", "/zazou/**,**/module-info.class");
            Plugin p = new OrderResourcesPlugin();
            p.configure(config);
            ResourcePool resPool = p.transform(resources.resourcePool(), out.resourcePoolBuilder());
            check(out.entries().collect(Collectors.toList()), sorted);
        }

        {
            // Order of resources in the file, then un-ordered resources.
            File order = new File("resources.order");
            order.createNewFile();
            StringBuilder builder = new StringBuilder();
            // 5 first resources come from file
            for (int i = 0; i < 5; i++) {
                String path = sorted2[i].path();
                int index = path.indexOf('/', 1);
                path = path.substring(index + 1, path.length() - ".class".length());
                builder.append(path).append("\n");
            }
            Files.write(order.toPath(), builder.toString().getBytes());

            ResourcePoolManager out = new ResourcePoolManager();
            Map<String, String> config = new HashMap<>();
            config.put("order-resources", "@" + order.getAbsolutePath());
            Plugin p = new OrderResourcesPlugin();
            p.configure(config);
            ResourcePool resPool = p.transform(resources.resourcePool(), out.resourcePoolBuilder());
            check(out.entries().collect(Collectors.toList()), sorted2);

        }
    }

    private void check(Collection<ResourcePoolEntry> outResources,
            ResourcePoolEntry[] sorted) {
        if (outResources.size() != sorted.length) {
            throw new AssertionError("Wrong number of resources:\n"
                    + "expected: " + Arrays.toString(sorted) + ",\n"
                    + "     got: " + outResources);
        }
        int i = 0;
        for (ResourcePoolEntry r : outResources) {
            System.err.println("Resource: " + r);
            if (!sorted[i].path().equals(r.path())) {
                throw new AssertionError("Resource not properly sorted, difference at: " + i + "\n"
                        + "expected: " + Arrays.toString(sorted) + ",\n"
                        + "     got: " + outResources);
            }
            i++;
        }
    }
}
