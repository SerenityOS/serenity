/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import jdk.tools.jlink.plugin.Plugin;
import jdk.tools.jlink.plugin.PluginException;

/**
 *
 * Plugin Providers repository. Plugin Providers are
 * retrieved thanks to the ServiceLoader mechanism.
 */
public final class PluginRepository {

    private PluginRepository() {
    }

    private static final Map<String, Plugin> registeredPlugins = new HashMap<>();

    /**
     * Retrieves the plugin associated to the passed name. If multiple providers
     * exist for the same name,
     * then an exception is thrown.
     * @param name The plugin provider name.
     * @param pluginsLayer
     * @return A provider or null if not found.
     */
    public static Plugin getPlugin(String name,
            ModuleLayer pluginsLayer) {
        return getPlugin(Plugin.class, name, pluginsLayer);
    }

    /**
     * Build plugin for the passed name.
     *
     * @param config Optional config.
     * @param name Non null name.
     * @param pluginsLayer
     * @return A plugin or null if no plugin found.
     */
    public static Plugin newPlugin(Map<String, String> config, String name,
            ModuleLayer pluginsLayer) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(pluginsLayer);
        Plugin plugin = getPlugin(name, pluginsLayer);
        if (plugin != null) {
            try {
                plugin.configure(config);
            } catch (IllegalArgumentException e) {
                if (JlinkTask.DEBUG) {
                    System.err.println("Plugin " + plugin.getName() + " threw exception with config: " + config);
                    e.printStackTrace();
                }
                throw e;
            }
        }
        return plugin;
    }

    /**
     * Explicit registration of a plugin in the repository. Used by unit tests
     * @param plugin The plugin to register.
     */
    public synchronized static void registerPlugin(Plugin plugin) {
        Objects.requireNonNull(plugin);
        registeredPlugins.put(plugin.getName(), plugin);
    }

    /**
     * Explicit unregistration of a plugin in the repository. Used by unit
     * tests
     *
     * @param name Plugin name
     */
    public synchronized static void unregisterPlugin(String name) {
        Objects.requireNonNull(name);
        registeredPlugins.remove(name);
    }

    public static List<Plugin> getPlugins(ModuleLayer pluginsLayer) {
        return getPlugins(Plugin.class, pluginsLayer);
    }

    private static <T extends Plugin> T getPlugin(Class<T> clazz, String name,
            ModuleLayer pluginsLayer) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(pluginsLayer);
        T provider = null;
        List<T> javaProviders = getPlugins(clazz, pluginsLayer);
        for(T factory : javaProviders) {
            if (factory.getName().equals(name)) {
                if (provider != null) {
                    throw new PluginException("Multiple plugin "
                            + "for the name " + name);
                }
                provider = factory;
            }
        }
        return provider;
    }

    /**
     * The plugins accessible in the current context.
     *
     * @param pluginsLayer
     * @return The list of plugins.
     */
    private static <T extends Plugin> List<T> getPlugins(Class<T> clazz, ModuleLayer pluginsLayer) {
        Objects.requireNonNull(pluginsLayer);
        List<T> factories = new ArrayList<>();
        try {
            Iterator<T> providers
                    = ServiceLoader.load(pluginsLayer, clazz).iterator();
            while (providers.hasNext()) {
                factories.add(providers.next());
            }
            registeredPlugins.values().stream().forEach((fact) -> {
                if (clazz.isInstance(fact)) {
                    @SuppressWarnings("unchecked")
                    T trans = (T) fact;
                    factories.add(trans);
                }
            });
            return factories;
        } catch (Exception ex) {
            throw new PluginException(ex);
        }
    }

}
