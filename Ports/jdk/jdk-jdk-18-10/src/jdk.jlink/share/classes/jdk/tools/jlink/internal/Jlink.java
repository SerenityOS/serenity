/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jdk.tools.jlink.builder.ImageBuilder;
import jdk.tools.jlink.plugin.Plugin;
import jdk.tools.jlink.plugin.PluginException;

/**
 * API to call jlink.
 */
public final class Jlink {

    /**
     * Create a plugin.
     *
     * @param name Plugin name
     * @param configuration Plugin configuration.
     * @param pluginsLayer Plugins Layer. null means boot layer.
     * @return A new plugin or null if plugin is unknown.
     */
    public static Plugin newPlugin(String name,
            Map<String, String> configuration, ModuleLayer pluginsLayer) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(configuration);
        pluginsLayer = pluginsLayer == null ? ModuleLayer.boot() : pluginsLayer;
        return PluginRepository.newPlugin(configuration, name, pluginsLayer);
    }

    /**
     * A complete plugin configuration. Instances of this class are used to
     * configure jlink.
     */
    public static final class PluginsConfiguration {

        private final List<Plugin> plugins;
        private final ImageBuilder imageBuilder;
        private final String lastSorterPluginName;

        /**
         * Empty plugins configuration.
         */
        public PluginsConfiguration() {
            this(Collections.emptyList());
        }

        /**
         * Plugins configuration.
         *
         * @param plugins List of plugins.
         */
        public PluginsConfiguration(List<Plugin> plugins) {
            this(plugins, null, null);
        }

        /**
         * Plugins configuration with a last sorter and an ImageBuilder. No
         * sorting can occur after the last sorter plugin. The ImageBuilder is
         * in charge to layout the image content on disk.
         *
         * @param plugins List of transformer plugins.
         * @param imageBuilder Image builder.
         * @param lastSorterPluginName Name of last sorter plugin, no sorting
         * can occur after it.
         */
        public PluginsConfiguration(List<Plugin> plugins,
                ImageBuilder imageBuilder, String lastSorterPluginName) {
            this.plugins = plugins == null ? Collections.emptyList()
                    : plugins;
            this.imageBuilder = imageBuilder;
            this.lastSorterPluginName = lastSorterPluginName;
        }

        /**
         * @return the plugins
         */
        public List<Plugin> getPlugins() {
            return plugins;
        }

        /**
         * @return the imageBuilder
         */
        public ImageBuilder getImageBuilder() {
            return imageBuilder;
        }

        /**
         * @return the lastSorterPluginName
         */
        public String getLastSorterPluginName() {
            return lastSorterPluginName;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("imagebuilder=").append(imageBuilder).append("\n");
            StringBuilder pluginsBuilder = new StringBuilder();
            for (Plugin p : plugins) {
                pluginsBuilder.append(p).append(",");
            }
            builder.append("plugins=").append(pluginsBuilder).append("\n");
            builder.append("lastsorter=").append(lastSorterPluginName).append("\n");

            return builder.toString();
        }
    }

    /**
     * Jlink configuration. Instances of this class are used to configure jlink.
     */
    public static final class JlinkConfiguration {

        private final Path output;
        private final Set<String> modules;
        private final ByteOrder endian;
        private final ModuleFinder finder;

        /**
         * jlink configuration,
         *
         * @param output Output directory, must not exist.
         * @param modules The possibly-empty set of root modules to resolve
         * @param endian Jimage byte order. Native order by default
         * @param finder the ModuleFinder for this configuration
         */
        public JlinkConfiguration(Path output,
                                  Set<String> modules,
                                  ByteOrder endian,
                                  ModuleFinder finder) {
            this.output = output;
            this.modules = Objects.requireNonNull(modules);
            this.endian = Objects.requireNonNull(endian);
            this.finder = finder;
        }

        /**
         * @return the byte ordering
         */
        public ByteOrder getByteOrder() {
            return endian;
        }

        /**
         * @return the output
         */
        public Path getOutput() {
            return output;
        }

        /**
         * @return the modules
         */
        public Set<String> getModules() {
            return modules;
        }

        /**
         * Returns {@link ModuleFinder} that finds all observable modules
         * for this jlink configuration.
         */
        public ModuleFinder finder() {
            return finder;
        }

        /**
         * Returns a {@link Configuration} of the given module path,
         * root modules with full service binding.
         */
        public Configuration resolveAndBind()
        {
            return Configuration.empty().resolveAndBind(finder,
                                                        ModuleFinder.of(),
                                                        modules);
        }

        /**
         * Returns a {@link Configuration} of the given module path,
         * root modules with no service binding.
         */
        public Configuration resolve()
        {
            return Configuration.empty().resolve(finder,
                                                 ModuleFinder.of(),
                                                 modules);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            builder.append("output=").append(output).append("\n");
            StringBuilder modsBuilder = new StringBuilder();
            for (String p : modules) {
                modsBuilder.append(p).append(",");
            }
            builder.append("modules=").append(modsBuilder).append("\n");
            builder.append("endian=").append(endian).append("\n");
            return builder.toString();
        }
    }

    /**
     * Jlink instance constructor, if a security manager is set, the jlink
     * permission is checked.
     */
    @SuppressWarnings("removal")
    public Jlink() {
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().
                    checkPermission(new JlinkPermission("jlink"));
        }
    }

    /**
     * Build the image.
     *
     * @param config Jlink config, must not be null.
     * @throws PluginException
     */
    public void build(JlinkConfiguration config) {
        build(config, null);
    }

    /**
     * Build the image with a plugin configuration.
     *
     * @param config Jlink config, must not be null.
     * @param pluginsConfig Plugins config, can be null
     * @throws PluginException
     */
    public void build(JlinkConfiguration config, PluginsConfiguration pluginsConfig) {
        Objects.requireNonNull(config);
        if (pluginsConfig == null) {
            pluginsConfig = new PluginsConfiguration();
        }

        // add all auto-enabled plugins from boot layer
        pluginsConfig = addAutoEnabledPlugins(pluginsConfig);

        try {
            JlinkTask.createImage(config, pluginsConfig);
        } catch (Exception ex) {
            throw new PluginException(ex);
        }
    }

    private PluginsConfiguration addAutoEnabledPlugins(PluginsConfiguration pluginsConfig) {
        List<Plugin> plugins = new ArrayList<>(pluginsConfig.getPlugins());
        List<Plugin> bootPlugins = PluginRepository.getPlugins(ModuleLayer.boot());
        for (Plugin bp : bootPlugins) {
            if (Utils.isAutoEnabled(bp)) {
                try {
                    bp.configure(Collections.emptyMap());
                } catch (IllegalArgumentException e) {
                    if (JlinkTask.DEBUG) {
                        System.err.println("Plugin " + bp.getName() + " threw exception with config: {}");
                        e.printStackTrace();
                    }
                    throw e;
                }
                plugins.add(bp);
            }
        }
        return new PluginsConfiguration(plugins, pluginsConfig.getImageBuilder(),
            pluginsConfig.getLastSorterPluginName());
    }

    /**
     * Post process the image with a plugin configuration.
     *
     * @param image Existing image.
     * @param plugins Plugins cannot be null
     */
    public void postProcess(ExecutableImage image, List<Plugin> plugins) {
        Objects.requireNonNull(image);
        Objects.requireNonNull(plugins);
        try {
            JlinkTask.postProcessImage(image, plugins);
        } catch (Exception ex) {
            throw new PluginException(ex);
        }
    }
}
