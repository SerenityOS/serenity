/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.module.ModuleDescriptor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import jdk.internal.jimage.decompressor.CompressedResourceHeader;
import jdk.internal.module.Resources;
import jdk.internal.module.ModuleInfo;
import jdk.internal.module.ModuleInfo.Attributes;
import jdk.internal.module.ModuleTarget;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.plugin.ResourcePoolEntry;
import jdk.tools.jlink.plugin.ResourcePoolModule;
import jdk.tools.jlink.plugin.ResourcePoolModuleView;
import jdk.tools.jlink.plugin.PluginException;

/**
 * A manager for pool of resources.
 */
public class ResourcePoolManager {
    // utility to read Module Attributes of the given ResourcePoolModule
    static Attributes readModuleAttributes(ResourcePoolModule mod) {
        String p = "/" + mod.name() + "/module-info.class";
        Optional<ResourcePoolEntry> content = mod.findEntry(p);
        if (!content.isPresent()) {
              throw new PluginException("module-info.class not found for " +
                  mod.name() + " module");
        }
        ByteBuffer bb = ByteBuffer.wrap(content.get().contentBytes());
        try {
            return ModuleInfo.read(bb, null);
        } catch (RuntimeException re) {
            throw new RuntimeException("module info cannot be read for " + mod.name(), re);
        }
    }

    /**
     * Returns true if a resource has an effective package.
     */
    public static boolean isNamedPackageResource(String path) {
        return (path.endsWith(".class") && !path.endsWith("module-info.class")) ||
                Resources.canEncapsulate(path);
    }

    class ResourcePoolModuleImpl implements ResourcePoolModule {

        final Map<String, ResourcePoolEntry> moduleContent = new LinkedHashMap<>();
        // lazily initialized
        private ModuleDescriptor descriptor;
        private ModuleTarget target;

        final String name;

        private ResourcePoolModuleImpl(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Optional<ResourcePoolEntry> findEntry(String path) {
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (!path.startsWith("/" + name + "/")) {
                path = "/" + name + path; // path already starts with '/'
            }
            return Optional.ofNullable(moduleContent.get(path));
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
                Attributes attr = readModuleAttributes(this);
                this.descriptor = attr.descriptor();
                this.target = attr.target();
            }
        }

        @Override
        public Set<String> packages() {
            Set<String> pkgs = new HashSet<>();
            moduleContent.values().stream()
                .filter(m -> m.type() == ResourcePoolEntry.Type.CLASS_OR_RESOURCE)
                .forEach(res -> {
                    String name = ImageFileCreator.resourceName(res.path());
                    if (isNamedPackageResource(name)) {
                        String pkg = ImageFileCreator.toPackage(name);
                        if (!pkg.isEmpty()) {
                            pkgs.add(pkg);
                        }
                    }
                });
            return pkgs;
        }

        @Override
        public String toString() {
            return name();
        }

        @Override
        public Stream<ResourcePoolEntry> entries() {
            return moduleContent.values().stream();
        }

        @Override
        public int entryCount() {
            return moduleContent.values().size();
        }
    }

    public class ResourcePoolImpl implements ResourcePool {
        @Override
        public ResourcePoolModuleView moduleView() {
            return ResourcePoolManager.this.moduleView();
        }

        @Override
        public Stream<ResourcePoolEntry> entries() {
            return ResourcePoolManager.this.entries();
        }

        @Override
        public int entryCount() {
            return ResourcePoolManager.this.entryCount();
        }

        @Override
        public Optional<ResourcePoolEntry> findEntry(String path) {
            return ResourcePoolManager.this.findEntry(path);
        }

        @Override
        public Optional<ResourcePoolEntry> findEntryInContext(String path, ResourcePoolEntry context) {
            return ResourcePoolManager.this.findEntryInContext(path, context);
        }

        @Override
        public boolean contains(ResourcePoolEntry data) {
            return ResourcePoolManager.this.contains(data);
        }

        @Override
        public boolean isEmpty() {
            return ResourcePoolManager.this.isEmpty();
        }

        @Override
        public ByteOrder byteOrder() {
            return ResourcePoolManager.this.byteOrder();
        }

        public StringTable getStringTable() {
            return ResourcePoolManager.this.getStringTable();
        }
    }

    class ResourcePoolBuilderImpl implements ResourcePoolBuilder {
        private boolean built;

        @Override
        public void add(ResourcePoolEntry data) {
            if (built) {
                throw new IllegalStateException("resource pool already built!");
            }
            ResourcePoolManager.this.add(data);
        }

        @Override
        public ResourcePool build() {
            built = true;
            return ResourcePoolManager.this.resourcePool();
        }
    }

    class ResourcePoolModuleViewImpl implements ResourcePoolModuleView {
        @Override
        public Optional<ResourcePoolModule> findModule(String name) {
            return ResourcePoolManager.this.findModule(name);
        }

        @Override
        public Stream<ResourcePoolModule> modules() {
            return ResourcePoolManager.this.modules();
        }

        @Override
        public int moduleCount() {
            return ResourcePoolManager.this.moduleCount();
        }
    }

    private final Map<String, ResourcePoolEntry> resources = new LinkedHashMap<>();
    private final Map<String, ResourcePoolModule> modules = new LinkedHashMap<>();
    private final ByteOrder order;
    private final StringTable table;
    private final ResourcePool poolImpl;
    private final ResourcePoolBuilder poolBuilderImpl;
    private final ResourcePoolModuleView moduleViewImpl;

    public ResourcePoolManager() {
        this(ByteOrder.nativeOrder());
    }

    public ResourcePoolManager(ByteOrder order) {
        this(order, new StringTable() {

            @Override
            public int addString(String str) {
                return -1;
            }

            @Override
            public String getString(int id) {
                return null;
            }
        });
    }

    public ResourcePoolManager(ByteOrder order, StringTable table) {
        this.order = Objects.requireNonNull(order);
        this.table = Objects.requireNonNull(table);
        this.poolImpl = new ResourcePoolImpl();
        this.poolBuilderImpl = new ResourcePoolBuilderImpl();
        this.moduleViewImpl = new ResourcePoolModuleViewImpl();
    }

    public ResourcePool resourcePool() {
        return poolImpl;
    }

    public ResourcePoolBuilder resourcePoolBuilder() {
        return poolBuilderImpl;
    }

    public ResourcePoolModuleView moduleView() {
        return moduleViewImpl;
    }

    /**
     * Add a ResourcePoolEntry.
     *
     * @param data The ResourcePoolEntry to add.
     */
    public void add(ResourcePoolEntry data) {
        Objects.requireNonNull(data);
        if (resources.get(data.path()) != null) {
            throw new PluginException("Resource " + data.path()
                    + " already present");
        }
        String modulename = data.moduleName();
        ResourcePoolModuleImpl m = (ResourcePoolModuleImpl)modules.get(modulename);
        if (m == null) {
            m = new ResourcePoolModuleImpl(modulename);
            modules.put(modulename, m);
        }
        resources.put(data.path(), data);
        m.moduleContent.put(data.path(), data);
    }

    /**
     * Retrieves the module for the provided name.
     *
     * @param name The module name
     * @return the module of matching name, if found
     */
    public Optional<ResourcePoolModule> findModule(String name) {
        Objects.requireNonNull(name);
        return Optional.ofNullable(modules.get(name));
    }

    /**
     * The stream of modules contained in this ResourcePool.
     *
     * @return The stream of modules.
     */
    public Stream<ResourcePoolModule> modules() {
        return modules.values().stream();
    }

    /**
     * Return the number of ResourcePoolModule count in this ResourcePool.
     *
     * @return the module count.
     */
    public int moduleCount() {
        return modules.size();
    }

    /**
     * Get all ResourcePoolEntry contained in this ResourcePool instance.
     *
     * @return The stream of ResourcePoolModuleEntries.
     */
    public Stream<ResourcePoolEntry> entries() {
        return resources.values().stream();
    }

    /**
     * Return the number of ResourcePoolEntry count in this ResourcePool.
     *
     * @return the entry count.
     */
    public int entryCount() {
        return resources.values().size();
    }

    /**
     * Get the ResourcePoolEntry for the passed path.
     *
     * @param path A data path
     * @return A ResourcePoolEntry instance or null if the data is not found
     */
    public Optional<ResourcePoolEntry> findEntry(String path) {
        Objects.requireNonNull(path);
        return Optional.ofNullable(resources.get(path));
    }

    /**
     * Get the ResourcePoolEntry for the passed path restricted to supplied context.
     *
     * @param path A data path
     * @param context A context of the search
     * @return A ResourcePoolEntry instance or null if the data is not found
     */
    public Optional<ResourcePoolEntry> findEntryInContext(String path, ResourcePoolEntry context) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(context);
        ResourcePoolModule module = modules.get(context.moduleName());
        Objects.requireNonNull(module);
        Optional<ResourcePoolEntry> entry = module.findEntry(path);
        // Navigating other modules via requires and exports is problematic
        // since we cannot construct the runtime model of loaders and layers.
        return entry;
     }

    /**
     * Check if the ResourcePool contains the given ResourcePoolEntry.
     *
     * @param data The module data to check existence for.
     * @return The module data or null if not found.
     */
    public boolean contains(ResourcePoolEntry data) {
        Objects.requireNonNull(data);
        return findEntry(data.path()).isPresent();
    }

    /**
     * Check if the ResourcePool contains some content at all.
     *
     * @return True, no content, false otherwise.
     */
    public boolean isEmpty() {
        return resources.isEmpty();
    }

    /**
     * The ByteOrder currently in use when generating the jimage file.
     *
     * @return The ByteOrder.
     */
    public ByteOrder byteOrder() {
        return order;
    }

    public StringTable getStringTable() {
        return table;
    }

    /**
     * A resource that has been compressed.
     */
    public static final class CompressedModuleData extends ByteArrayResourcePoolEntry {

        final long uncompressed_size;

        private CompressedModuleData(String module, String path,
                byte[] content, long uncompressed_size) {
            super(module, path, ResourcePoolEntry.Type.CLASS_OR_RESOURCE, content);
            this.uncompressed_size = uncompressed_size;
        }

        public long getUncompressedSize() {
            return uncompressed_size;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof CompressedModuleData)) {
                return false;
            }
            CompressedModuleData f = (CompressedModuleData) other;
            return f.path().equals(path());
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    public static CompressedModuleData newCompressedResource(ResourcePoolEntry original,
            ByteBuffer compressed,
            String plugin, String pluginConfig, StringTable strings,
            ByteOrder order) {
        Objects.requireNonNull(original);
        Objects.requireNonNull(compressed);
        Objects.requireNonNull(plugin);

        boolean isTerminal = !(original instanceof CompressedModuleData);
        long uncompressed_size = original.contentLength();
        if (original instanceof CompressedModuleData) {
            CompressedModuleData comp = (CompressedModuleData) original;
            uncompressed_size = comp.getUncompressedSize();
        }
        int nameOffset = strings.addString(plugin);
        int configOffset = -1;
        if (pluginConfig != null) {
            configOffset = strings.addString(plugin);
        }
        CompressedResourceHeader rh
                = new CompressedResourceHeader(compressed.limit(), original.contentLength(),
                        nameOffset, configOffset, isTerminal);
        // Merge header with content;
        byte[] h = rh.getBytes(order);
        ByteBuffer bb = ByteBuffer.allocate(compressed.limit() + h.length);
        bb.order(order);
        bb.put(h);
        bb.put(compressed);
        byte[] contentWithHeader = bb.array();

        CompressedModuleData compressedResource
                = new CompressedModuleData(original.moduleName(), original.path(),
                        contentWithHeader, uncompressed_size);
        return compressedResource;
    }
}
