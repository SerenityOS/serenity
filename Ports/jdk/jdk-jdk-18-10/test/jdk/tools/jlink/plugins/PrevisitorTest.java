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
 * @summary Test previsitor
 * @author Andrei Eremeev
 * @modules jdk.jlink/jdk.tools.jlink
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.plugin
 * @run main/othervm PrevisitorTest
 */
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jdk.tools.jlink.internal.ImagePluginConfiguration;
import jdk.tools.jlink.internal.Jlink;
import jdk.tools.jlink.internal.PluginRepository;
import jdk.tools.jlink.internal.ImagePluginStack;
import jdk.tools.jlink.internal.ResourcePoolManager;
import jdk.tools.jlink.internal.ResourcePoolManager.ResourcePoolImpl;
import jdk.tools.jlink.internal.ResourcePrevisitor;
import jdk.tools.jlink.internal.StringTable;
import jdk.tools.jlink.plugin.Plugin;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.plugin.ResourcePoolEntry;

public class PrevisitorTest {

    public static void main(String[] args) throws Exception {
        new PrevisitorTest().test();
    }

    private static Plugin createPlugin(String name) {
        return Jlink.newPlugin(name, Collections.emptyMap(), null);
    }

    public void test() throws Exception {
        CustomPlugin plugin = new CustomPlugin();
        PluginRepository.registerPlugin(plugin);
        List<Plugin> plugins = new ArrayList<>();
        plugins.add(createPlugin(CustomPlugin.NAME));
        ImagePluginStack stack = ImagePluginConfiguration.parseConfiguration(new Jlink.PluginsConfiguration(plugins,
                null, null));
        ResourcePoolManager inResources = new ResourcePoolManager(ByteOrder.nativeOrder(), new CustomStringTable());
        inResources.add(ResourcePoolEntry.create("/aaa/bbb/res1.class", new byte[90]));
        inResources.add(ResourcePoolEntry.create("/aaa/bbb/res2.class", new byte[90]));
        inResources.add(ResourcePoolEntry.create("/aaa/bbb/res3.class", new byte[90]));
        inResources.add(ResourcePoolEntry.create("/aaa/ddd/res1.class", new byte[90]));
        inResources.add(ResourcePoolEntry.create("/aaa/res1.class", new byte[90]));
        ResourcePool outResources = stack.visitResources(inResources);
        Collection<String> input = inResources.entries()
                .map(Object::toString)
                .collect(Collectors.toList());
        Collection<String> output = outResources.entries()
                .map(Object::toString)
                .collect(Collectors.toList());
        if (!input.equals(output)) {
            throw new AssertionError("Input and output resources differ: input: "
                    + input + ", output: " + output);
        }
    }

    private static class CustomStringTable implements StringTable {

        private final List<String> strings = new ArrayList<>();

        @Override
        public int addString(String str) {
            strings.add(str);
            return strings.size() - 1;
        }

        @Override
        public String getString(int id) {
            return strings.get(id);
        }

        public int size() {
            return strings.size();
        }
    }

    private static class CustomPlugin implements Plugin, ResourcePrevisitor {

        private static String NAME = "plugin";

        private boolean isPrevisitCalled = false;

        @Override
        public ResourcePool transform(ResourcePool inResources, ResourcePoolBuilder outResources) {
            if (!isPrevisitCalled) {
                throw new AssertionError("Previsit was not called");
            }
            CustomStringTable table = (CustomStringTable)((ResourcePoolImpl)inResources).getStringTable();
            if (table.size() == 0) {
                throw new AssertionError("Table is empty");
            }
            Map<String, Integer> count = new HashMap<>();
            for (int i = 0; i < table.size(); ++i) {
                String s = table.getString(i);
                Optional<ResourcePoolEntry> e = inResources.findEntry(s);
                if (e.isPresent()) {
                    throw new AssertionError();
                }
                count.compute(s, (k, c) -> 1 + (c == null ? 0 : c));
            }
            count.forEach((k, v) -> {
                if (v != 1) {
                    throw new AssertionError("Expected one entry in the table, got: " + v + " for " + k);
                }
            });
            inResources.entries().forEach(r -> {
                outResources.add(r);
            });

            return outResources.build();
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public void previsit(ResourcePool resources, StringTable strings) {
            isPrevisitCalled = true;
            resources.entries().forEach(r -> {
                String s = r.path();
                int lastIndexOf = s.lastIndexOf('/');
                if (lastIndexOf >= 0) {
                    strings.addString(s.substring(0, lastIndexOf));
                }
            });
        }
    }
}
