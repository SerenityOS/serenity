/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.tools.jlink.internal;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.Stream;

import jdk.internal.jimage.decompressor.Decompressor;
import jdk.internal.module.ModuleInfo.Attributes;
import jdk.internal.module.ModuleTarget;
import jdk.tools.jlink.builder.ImageBuilder;
import jdk.tools.jlink.plugin.Plugin;
import jdk.tools.jlink.plugin.PluginException;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolEntry;
import jdk.tools.jlink.plugin.ResourcePoolModule;

/**
 * Plugins Stack. Plugins entry point to apply transformations onto resources
 * and files.
 */
public final class ImagePluginStack {

    public interface ImageProvider {

        ExecutableImage retrieve(ImagePluginStack stack) throws IOException;
    }

    public static final class OrderedResourcePoolManager extends ResourcePoolManager {
        class OrderedResourcePool extends ResourcePoolImpl {
            List<ResourcePoolEntry> getOrderedList() {
                return OrderedResourcePoolManager.this.getOrderedList();
            }
        }

        private final List<ResourcePoolEntry> orderedList = new ArrayList<>();
        private final ResourcePoolImpl poolImpl = new OrderedResourcePool();

        public OrderedResourcePoolManager(ByteOrder order, StringTable table) {
            super(order, table);
        }

        @Override
        public ResourcePool resourcePool() {
            return poolImpl;
        }

        /**
         * Add a resource.
         *
         * @param resource The Resource to add.
         */
        @Override
        public void add(ResourcePoolEntry resource) {
            super.add(resource);
            orderedList.add(resource);
        }

        List<ResourcePoolEntry> getOrderedList() {
            return Collections.unmodifiableList(orderedList);
        }
    }

    private final static class CheckOrderResourcePoolManager extends ResourcePoolManager {

        private final List<ResourcePoolEntry> orderedList;
        private int currentIndex;

        public CheckOrderResourcePoolManager(ByteOrder order, List<ResourcePoolEntry> orderedList, StringTable table) {
            super(order, table);
            this.orderedList = Objects.requireNonNull(orderedList);
        }

        /**
         * Add a resource.
         *
         * @param resource The Resource to add.
         */
        @Override
        public void add(ResourcePoolEntry resource) {
            ResourcePoolEntry ordered = orderedList.get(currentIndex);
            if (!resource.equals(ordered)) {
                throw new PluginException("Resource " + resource.path() + " not in the right order");
            }
            super.add(resource);
            currentIndex += 1;
        }
    }

    private static final class PreVisitStrings implements StringTable {

        private int currentid = 0;
        private final Map<String, Integer> stringsUsage = new HashMap<>();
        private final Map<String, Integer> stringsMap = new HashMap<>();
        private final Map<Integer, String> reverseMap = new HashMap<>();

        @Override
        public int addString(String str) {
            Objects.requireNonNull(str);
            Integer count = stringsUsage.get(str);
            if (count == null) {
                count = 0;
            }
            count += 1;
            stringsUsage.put(str, count);
            Integer id = stringsMap.get(str);
            if (id == null) {
                id = currentid;
                stringsMap.put(str, id);
                currentid += 1;
                reverseMap.put(id, str);
            }

            return id;
        }

        private List<String> getSortedStrings() {
            Stream<java.util.Map.Entry<String, Integer>> stream
                    = stringsUsage.entrySet().stream();
            // Remove strings that have a single occurence
            List<String> result = stream.sorted(Comparator.comparing(e -> e.getValue(),
                    Comparator.reverseOrder())).filter((e) -> {
                        return e.getValue() > 1;
                    }).map(java.util.Map.Entry::getKey).
                    toList();
            return result;
        }

        @Override
        public String getString(int id) {
            return reverseMap.get(id);
        }
    }

    private final ImageBuilder imageBuilder;
    private final Plugin lastSorter;
    private final List<Plugin> plugins = new ArrayList<>();
    private final List<ResourcePrevisitor> resourcePrevisitors = new ArrayList<>();
    private final boolean validate;

    public ImagePluginStack() {
        this(null, Collections.emptyList(), null);
    }

    public ImagePluginStack(ImageBuilder imageBuilder,
            List<Plugin> plugins,
            Plugin lastSorter) {
        this(imageBuilder, plugins, lastSorter, true);
    }

    public ImagePluginStack(ImageBuilder imageBuilder,
            List<Plugin> plugins,
            Plugin lastSorter,
            boolean validate) {
        this.imageBuilder = Objects.requireNonNull(imageBuilder);
        this.lastSorter = lastSorter;
        this.plugins.addAll(Objects.requireNonNull(plugins));
        plugins.stream().forEach((p) -> {
            Objects.requireNonNull(p);
            if (p instanceof ResourcePrevisitor) {
                resourcePrevisitors.add((ResourcePrevisitor) p);
            }
        });
        this.validate = validate;
    }

    public void operate(ImageProvider provider) throws Exception {
        ExecutableImage img = provider.retrieve(this);
        List<String> arguments = new ArrayList<>();
        plugins.stream()
                .filter(PostProcessor.class::isInstance)
                .map((plugin) -> ((PostProcessor)plugin).process(img))
                .filter((lst) -> (lst != null))
                .forEach((lst) -> {
                     arguments.addAll(lst);
                });
        img.storeLaunchArgs(arguments);
    }

    public DataOutputStream getJImageFileOutputStream() throws IOException {
        return imageBuilder.getJImageOutputStream();
    }

    public ImageBuilder getImageBuilder() {
        return imageBuilder;
    }

    /**
     * Resource Plugins stack entry point. All resources are going through all
     * the plugins.
     *
     * @param resources The set of resources to visit
     * @return The result of the visit.
     * @throws IOException
     */
    public ResourcePool visitResources(ResourcePoolManager resources)
            throws Exception {
        Objects.requireNonNull(resources);
        if (resources.isEmpty()) {
            return new ResourcePoolManager(resources.byteOrder(),
                    resources.getStringTable()).resourcePool();
        }
        PreVisitStrings previsit = new PreVisitStrings();
        resourcePrevisitors.stream().forEach((p) -> {
            p.previsit(resources.resourcePool(), previsit);
        });

        // Store the strings resulting from the previsit.
        List<String> sorted = previsit.getSortedStrings();
        sorted.stream().forEach((s) -> {
            resources.getStringTable().addString(s);
        });

        ResourcePool resPool = resources.resourcePool();
        List<ResourcePoolEntry> frozenOrder = null;
        for (Plugin p : plugins) {
            ResourcePoolManager resMgr = null;
            if (p == lastSorter) {
                if (frozenOrder != null) {
                    throw new Exception("Order of resources is already frozen. Plugin "
                            + p.getName() + " is badly located");
                }
                // Create a special Resource pool to compute the indexes.
                resMgr = new OrderedResourcePoolManager(resPool.byteOrder(),
                        resources.getStringTable());
            } else {// If we have an order, inject it
                if (frozenOrder != null) {
                    resMgr = new CheckOrderResourcePoolManager(resPool.byteOrder(),
                            frozenOrder, resources.getStringTable());
                } else {
                    resMgr = new ResourcePoolManager(resPool.byteOrder(),
                            resources.getStringTable());
                }
            }
            try {
                resPool = p.transform(resPool, resMgr.resourcePoolBuilder());
            } catch (PluginException pe) {
                if (JlinkTask.DEBUG) {
                    System.err.println("Plugin " + p.getName() + " threw exception during transform");
                    pe.printStackTrace();
                }
                throw pe;
            }
            if (resPool.isEmpty()) {
                throw new Exception("Invalid resource pool for plugin " + p);
            }
            if (resPool instanceof OrderedResourcePoolManager.OrderedResourcePool) {
                frozenOrder = ((OrderedResourcePoolManager.OrderedResourcePool)resPool).getOrderedList();
            }
        }

        return resPool;
    }

    /**
     * This pool wrap the original pool and automatically uncompress ResourcePoolEntry
     * if needed.
     */
    private class LastPoolManager extends ResourcePoolManager {
        private class LastModule implements ResourcePoolModule {

            final ResourcePoolModule module;
            // lazily initialized
            ModuleDescriptor descriptor;
            ModuleTarget target;

            LastModule(ResourcePoolModule module) {
                this.module = module;
            }

            @Override
            public String name() {
                return module.name();
            }

            @Override
            public Optional<ResourcePoolEntry> findEntry(String path) {
                Optional<ResourcePoolEntry> d = module.findEntry(path);
                return d.isPresent()? Optional.of(getUncompressed(d.get())) : Optional.empty();
            }

            @Override
            public ModuleDescriptor descriptor() {
                initModuleAttributes();
                return descriptor;
            }

            @Override
            public String targetPlatform() {
                initModuleAttributes();
                return target != null? target.targetPlatform() : null;
            }

            private void initModuleAttributes() {
                if (this.descriptor == null) {
                    Attributes attr = ResourcePoolManager.readModuleAttributes(this);
                    this.descriptor = attr.descriptor();
                    this.target = attr.target();
                }
            }

            @Override
            public Set<String> packages() {
                return module.packages();
            }

            @Override
            public String toString() {
                return name();
            }

            @Override
            public Stream<ResourcePoolEntry> entries() {
                List<ResourcePoolEntry> lst = new ArrayList<>();
                module.entries().forEach(md -> {
                    lst.add(getUncompressed(md));
                });
                return lst.stream();
            }

            @Override
            public int entryCount() {
                return module.entryCount();
            }
        }

        private final ResourcePool pool;
        Decompressor decompressor = new Decompressor();
        Collection<ResourcePoolEntry> content;

        LastPoolManager(ResourcePool pool) {
            this.pool = pool;
        }

        @Override
        public void add(ResourcePoolEntry resource) {
            throw new PluginException("pool is readonly");
        }

        @Override
        public Optional<ResourcePoolModule> findModule(String name) {
            Optional<ResourcePoolModule> module = pool.moduleView().findModule(name);
            return module.isPresent()? Optional.of(new LastModule(module.get())) : Optional.empty();
        }

        /**
         * The collection of modules contained in this pool.
         *
         * @return The collection of modules.
         */
        @Override
        public Stream<ResourcePoolModule> modules() {
            List<ResourcePoolModule> modules = new ArrayList<>();
            pool.moduleView().modules().forEach(m -> {
                modules.add(new LastModule(m));
            });
            return modules.stream();
        }

        @Override
        public int moduleCount() {
            return pool.moduleView().moduleCount();
        }

        /**
         * Get all resources contained in this pool instance.
         *
         * @return The stream of resources;
         */
        @Override
        public Stream<ResourcePoolEntry> entries() {
            if (content == null) {
                content = new ArrayList<>();
                pool.entries().forEach(md -> {
                    content.add(getUncompressed(md));
                });
            }
            return content.stream();
        }

        @Override
        public int entryCount() {
            return pool.entryCount();
        }

        /**
         * Get the resource for the passed path.
         *
         * @param path A resource path
         * @return A Resource instance if the resource is found
         */
        @Override
        public Optional<ResourcePoolEntry> findEntry(String path) {
            Objects.requireNonNull(path);
            Optional<ResourcePoolEntry> res = pool.findEntry(path);
            return res.isPresent()? Optional.of(getUncompressed(res.get())) : Optional.empty();
        }

        @Override
        public Optional<ResourcePoolEntry> findEntryInContext(String path, ResourcePoolEntry context) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(context);
            Optional<ResourcePoolEntry> res = pool.findEntryInContext(path, context);
            return res.map(this::getUncompressed);
        }

        @Override
        public boolean contains(ResourcePoolEntry res) {
            return pool.contains(res);
        }

        @Override
        public boolean isEmpty() {
            return pool.isEmpty();
        }

        @Override
        public ByteOrder byteOrder() {
            return pool.byteOrder();
        }

        private ResourcePoolEntry getUncompressed(ResourcePoolEntry res) {
            if (res != null) {
                if (res instanceof ResourcePoolManager.CompressedModuleData) {
                    try {
                        byte[] bytes = decompressor.decompressResource(byteOrder(),
                                (int offset) -> ((ResourcePoolImpl)pool).getStringTable().getString(offset),
                                res.contentBytes());
                        res = res.copyWithContent(bytes);
                    } catch (IOException ex) {
                        if (JlinkTask.DEBUG) {
                            System.err.println("IOException while reading resource: " + res.path());
                            ex.printStackTrace();
                        }
                        throw new PluginException(ex);
                    }
                }
            }
            return res;
        }
    }

    /**
     * Make the imageBuilder to store files.
     *
     * @param original
     * @param transformed
     * @param writer
     * @throws java.lang.Exception
     */
    public void storeFiles(ResourcePool original, ResourcePool transformed,
            BasicImageWriter writer)
            throws Exception {
        Objects.requireNonNull(original);
        Objects.requireNonNull(transformed);
        ResourcePool lastPool = new LastPoolManager(transformed).resourcePool();
        if (validate) {
            ResourcePoolConfiguration.validate(lastPool);
        }
        imageBuilder.storeFiles(lastPool);
    }

    public ExecutableImage getExecutableImage() throws IOException {
        return imageBuilder.getExecutableImage();
    }
}
