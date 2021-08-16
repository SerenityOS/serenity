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
 * @summary Test a pool containing jimage resources and classes.
 * @author Jean-Francois Denise
 * @modules jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.plugin
 * @run build ResourcePoolTest
 * @run main ResourcePoolTest
 */

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import jdk.tools.jlink.internal.ResourcePoolManager;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolModule;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolEntry;

public class ResourcePoolTest {

    public static void main(String[] args) throws Exception {
        new ResourcePoolTest().test();
    }

    public void test() throws Exception {
        checkResourceAdding();
        checkResourceVisitor();
        checkResourcesAfterCompression();
    }

    private static final String SUFFIX = "END";

    private void checkResourceVisitor() throws Exception {
        ResourcePoolManager input = new ResourcePoolManager();
        for (int i = 0; i < 1000; ++i) {
            String module = "/module" + (i / 10);
            String resourcePath = module + "/java/package" + i;
            byte[] bytes = resourcePath.getBytes();
            input.add(ResourcePoolEntry.create(resourcePath, bytes));
        }
        ResourcePoolManager output = new ResourcePoolManager();
        ResourceVisitor visitor = new ResourceVisitor();
        input.resourcePool().transformAndCopy(visitor, output.resourcePoolBuilder());
        if (visitor.getAmountBefore() == 0) {
            throw new AssertionError("Resources not found");
        }
        if (visitor.getAmountBefore() != input.entryCount()) {
            throw new AssertionError("Number of visited resources. Expected: " +
                    visitor.getAmountBefore() + ", got: " + input.entryCount());
        }
        if (visitor.getAmountAfter() != output.entryCount()) {
            throw new AssertionError("Number of added resources. Expected: " +
                    visitor.getAmountAfter() + ", got: " + output.entryCount());
        }
        output.entries().forEach(outResource -> {
            String path = outResource.path().replaceAll(SUFFIX + "$", "");
            if (!input.findEntry(path).isPresent()) {
                throw new AssertionError("Unknown resource: " + path);
            }
        });
    }

    private static class ResourceVisitor implements Function<ResourcePoolEntry, ResourcePoolEntry> {

        private int amountBefore;
        private int amountAfter;

        @Override
        public ResourcePoolEntry apply(ResourcePoolEntry resource) {
            int index = ++amountBefore % 3;
            switch (index) {
                case 0:
                    ++amountAfter;
                    return ResourcePoolEntry.create(resource.path() + SUFFIX,
                            resource.type(), resource.contentBytes());
                case 1:
                    ++amountAfter;
                    return resource.copyWithContent(resource.contentBytes());
            }
            return null;
        }

        public int getAmountAfter() {
            return amountAfter;
        }

        public int getAmountBefore() {
            return amountBefore;
        }
    }

    private void checkResourceAdding() {
        List<String> samples = new ArrayList<>();
        samples.add("java.base");
        samples.add("java/lang/Object");
        samples.add("java.base");
        samples.add("java/lang/String");
        samples.add("java.management");
        samples.add("javax/management/ObjectName");
        test(samples, (resources, module, path) -> {
            try {
                resources.add(ResourcePoolEntry.create(path, new byte[0]));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        test(samples, (resources, module, path) -> {
            try {
                resources.add(ResourcePoolManager.
                        newCompressedResource(ResourcePoolEntry.create(path, new byte[0]),
                                ByteBuffer.allocate(99), "bitcruncher", null,
                                ((ResourcePoolManager)resources).getStringTable(), ByteOrder.nativeOrder()));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private void test(List<String> samples, ResourceAdder adder) {
        if (samples.isEmpty()) {
            throw new AssertionError("No sample to test");
        }
        ResourcePoolManager resources = new ResourcePoolManager();
        Set<String> modules = new HashSet<>();
        for (int i = 0; i < samples.size(); i++) {
            String module = samples.get(i);
            modules.add(module);
            i++;
            String clazz = samples.get(i);
            String path = "/" + module + "/" + clazz + ".class";
            adder.add(resources, module, path);
        }
        for (int i = 0; i < samples.size(); i++) {
            String module = samples.get(i);
            i++;
            String clazz = samples.get(i);
            String path = "/" + module + "/" + clazz + ".class";
            Optional<ResourcePoolEntry> res = resources.findEntry(path);
            if (!res.isPresent()) {
                throw new AssertionError("Resource not found " + path);
            }
            checkModule(resources.resourcePool(), res.get());
            if (resources.findEntry(clazz).isPresent()) {
                throw new AssertionError("Resource found " + clazz);
            }
        }
        if (resources.entryCount() != samples.size() / 2) {
            throw new AssertionError("Invalid number of resources");
        }
    }

    private void checkModule(ResourcePool resources, ResourcePoolEntry res) {
        Optional<ResourcePoolModule> optMod = resources.moduleView().findModule(res.moduleName());
        if (!optMod.isPresent()) {
            throw new AssertionError("No module " + res.moduleName());
        }
        ResourcePoolModule m = optMod.get();
        if (!m.name().equals(res.moduleName())) {
            throw new AssertionError("Not right module name " + res.moduleName());
        }
        if (!m.findEntry(res.path()).isPresent()) {
            throw new AssertionError("resource " + res.path()
                    + " not in module " + m.name());
        }
    }

    private void checkResourcesAfterCompression() throws Exception {
        ResourcePoolManager resources1 = new ResourcePoolManager();
        ResourcePoolEntry res1 = ResourcePoolEntry.create("/module1/toto1", new byte[0]);
        ResourcePoolEntry res2 = ResourcePoolEntry.create("/module2/toto1", new byte[0]);
        resources1.add(res1);
        resources1.add(res2);

        checkResources(resources1, res1, res2);
        ResourcePoolManager resources2 = new ResourcePoolManager();
        ResourcePoolEntry res3 = ResourcePoolEntry.create("/module2/toto1", new byte[7]);
        resources2.add(res3);
        resources2.add(ResourcePoolManager.newCompressedResource(res1,
                ByteBuffer.allocate(7), "zip", null, resources1.getStringTable(),
                ByteOrder.nativeOrder()));
        checkResources(resources2, res1, res2);
    }

    private void checkResources(ResourcePoolManager resources, ResourcePoolEntry... expected) {
        List<String> modules = new ArrayList();
        resources.modules().forEach(m -> {
            modules.add(m.name());
        });
        for (ResourcePoolEntry res : expected) {
            if (!resources.contains(res)) {
                throw new AssertionError("Resource not found: " + res);
            }

            if (!resources.findEntry(res.path()).isPresent()) {
                throw new AssertionError("Resource not found: " + res);
            }

            if (!modules.contains(res.moduleName())) {
                throw new AssertionError("Module not found: " + res.moduleName());
            }

            if (!resources.contains(res)) {
                throw new AssertionError("Resources not found: " + res);
            }

            try {
                resources.add(res);
                throw new AssertionError(res + " already present, but an exception is not thrown");
            } catch (Exception ex) {
                // Expected
            }
        }

        try {
            resources.add(ResourcePoolEntry.create("/module2/toto1", new byte[0]));
            throw new AssertionError("ResourcePool is read-only, but an exception is not thrown");
        } catch (Exception ex) {
            // Expected
        }
    }

    interface ResourceAdder {
        void add(ResourcePoolManager resources, String module, String path);
    }
}
