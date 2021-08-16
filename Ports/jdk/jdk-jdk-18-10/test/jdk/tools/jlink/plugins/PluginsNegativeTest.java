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
 * @summary Negative test for ImagePluginStack.
 * @author Andrei Eremeev
 * @modules jdk.jlink/jdk.tools.jlink
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.plugin
 * @run main/othervm PluginsNegativeTest
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jdk.tools.jlink.internal.ImagePluginConfiguration;
import jdk.tools.jlink.internal.Jlink;
import jdk.tools.jlink.internal.Jlink.PluginsConfiguration;
import jdk.tools.jlink.internal.PluginRepository;
import jdk.tools.jlink.internal.ImagePluginStack;
import jdk.tools.jlink.internal.ResourcePoolManager;
import jdk.tools.jlink.plugin.Plugin;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.plugin.ResourcePoolEntry;

public class PluginsNegativeTest {

    public static void main(String[] args) throws Exception {
        new PluginsNegativeTest().test();
    }

    public void test() throws Exception {
        testDuplicateBuiltInProviders();
        testUnknownProvider();
        PluginRepository.registerPlugin(new CustomPlugin("plugin"));
        testEmptyInputResource();
        testEmptyOutputResource();
    }

    private void testDuplicateBuiltInProviders() {
        List<Plugin> javaPlugins = new ArrayList<>();
        javaPlugins.addAll(PluginRepository.getPlugins(ModuleLayer.boot()));
        for (Plugin javaPlugin : javaPlugins) {
            System.out.println("Registered plugin: " + javaPlugin.getName());
        }
        for (Plugin javaPlugin : javaPlugins) {
            String pluginName = javaPlugin.getName();
            try {
                PluginRepository.registerPlugin(new CustomPlugin(pluginName));
                try {
                    PluginRepository.getPlugin(pluginName, ModuleLayer.boot());
                    throw new AssertionError("Exception is not thrown for duplicate plugin: " + pluginName);
                } catch (Exception ignored) {
                }
            } finally {
                PluginRepository.unregisterPlugin(pluginName);
            }
        }
    }

    private void testUnknownProvider() {
        if (PluginRepository.getPlugin("unknown", ModuleLayer.boot()) != null) {
            throw new AssertionError("Exception expected for unknown plugin name");
        }
    }

    private static Plugin createPlugin(String name) {
        return Jlink.newPlugin(name, Collections.emptyMap(), null);
    }

    private void testEmptyOutputResource() throws Exception {
        List<Plugin> plugins = new ArrayList<>();
        plugins.add(createPlugin("plugin"));
        ImagePluginStack stack = ImagePluginConfiguration.parseConfiguration(new PluginsConfiguration(plugins,
                null, null));
        ResourcePoolManager inResources = new ResourcePoolManager();
        inResources.add(ResourcePoolEntry.create("/aaa/bbb/A", new byte[10]));
        try {
            stack.visitResources(inResources);
            throw new AssertionError("Exception expected when output resource is empty");
        } catch (Exception ignored) {
        }
    }

    private void testEmptyInputResource() throws Exception {
        List<Plugin> plugins = new ArrayList<>();
        plugins.add(createPlugin("plugin"));
        ImagePluginStack stack = ImagePluginConfiguration.parseConfiguration(new PluginsConfiguration(plugins,
                null, null));
        ResourcePoolManager inResources = new ResourcePoolManager();
        ResourcePool outResources = stack.visitResources(inResources);
        if (!outResources.isEmpty()) {
            throw new AssertionError("Output resource is not empty");
        }
    }

    public static class CustomPlugin implements Plugin {

        private final String name;

        CustomPlugin(String name) {
            this.name = name;
        }

        @Override
        public ResourcePool transform(ResourcePool inResources, ResourcePoolBuilder outResources) {
            // don't add anything to the builder
            return outResources.build();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public void configure(Map<String, String> config) {

        }
    }
}
