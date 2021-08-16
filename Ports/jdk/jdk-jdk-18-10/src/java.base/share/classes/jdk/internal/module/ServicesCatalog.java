/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.module;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Provides;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import jdk.internal.loader.ClassLoaderValue;

/**
 * A <em>services catalog</em>. Each {@code ClassLoader} and {@code Layer} has
 * an optional {@code ServicesCatalog} for modules that provide services.
 *
 * @apiNote This class will be replaced once the ServiceLoader is further
 *          specified
 */
public final class ServicesCatalog {

    /**
     * Represents a service provider in the services catalog.
     */
    public static final class ServiceProvider {
        private final Module module;
        private final String providerName;

        public ServiceProvider(Module module, String providerName) {
            this.module = module;
            this.providerName = providerName;
        }

        public Module module() {
            return module;
        }

        public String providerName() {
            return providerName;
        }

        @Override
        public int hashCode() {
            return Objects.hash(module, providerName);
        }

        @Override
        public boolean equals(Object ob) {
            if (!(ob instanceof ServiceProvider))
                return false;
            ServiceProvider that = (ServiceProvider)ob;
            return Objects.equals(this.module, that.module)
                    && Objects.equals(this.providerName, that.providerName);
        }
    }

    // service name -> list of providers
    private final Map<String, List<ServiceProvider>> map = new ConcurrentHashMap<>(32);

    private ServicesCatalog() { }

    /**
     * Creates a ServicesCatalog that supports concurrent registration
     * and lookup
     */
    public static ServicesCatalog create() {
        return new ServicesCatalog();
    }

    /**
     * Adds service providers for the given service type.
     */
    private void addProviders(String service, ServiceProvider ... providers) {
        List<ServiceProvider> list = map.get(service);
        if (list == null) {
            list = new CopyOnWriteArrayList<>(providers);
            List<ServiceProvider> prev = map.putIfAbsent(service, list);
            if (prev != null) {
                // someone else got there
                prev.addAll(list);
            }
        } else {
            if (providers.length == 1) {
                list.add(providers[0]);
            } else {
                list.addAll(Arrays.asList(providers));
            }
        }
    }

    /**
     * Registers the providers in the given module in this services catalog.
     */
    public void register(Module module) {
        ModuleDescriptor descriptor = module.getDescriptor();
        for (Provides provides : descriptor.provides()) {
            String service = provides.service();
            List<String> providerNames = provides.providers();
            int count = providerNames.size();
            ServiceProvider[] providers = new ServiceProvider[count];
            for (int i = 0; i < count; i++) {
                providers[i] = new ServiceProvider(module, providerNames.get(i));
            }
            addProviders(service, providers);
        }
    }

    /**
     * Adds a provider in the given module to this services catalog.
     *
     * @apiNote This method is for use by java.lang.instrument
     */
    public void addProvider(Module module, Class<?> service, Class<?> impl) {
        addProviders(service.getName(), new ServiceProvider(module, impl.getName()));
    }

    /**
     * Returns the (possibly empty) list of service providers that implement
     * the given service type.
     */
    public List<ServiceProvider> findServices(String service) {
        return map.getOrDefault(service, List.of());
    }

    /**
     * Returns the ServicesCatalog for the given class loader or {@code null}
     * if there is none.
     */
    public static ServicesCatalog getServicesCatalogOrNull(ClassLoader loader) {
        return CLV.get(loader);
    }

    /**
     * Returns the ServicesCatalog for the given class loader, creating it if
     * needed.
     */
    public static ServicesCatalog getServicesCatalog(ClassLoader loader) {
        // CLV.computeIfAbsent(loader, (cl, clv) -> create());
        ServicesCatalog catalog = CLV.get(loader);
        if (catalog == null) {
            catalog = create();
            ServicesCatalog previous = CLV.putIfAbsent(loader, catalog);
            if (previous != null) catalog = previous;
        }
        return catalog;
    }

    /**
     * Associates the given ServicesCatalog with the given class loader.
     */
    public static void putServicesCatalog(ClassLoader loader, ServicesCatalog catalog) {
        ServicesCatalog previous = CLV.putIfAbsent(loader, catalog);
        if (previous != null) {
            throw new InternalError();
        }
    }

    // the ServicesCatalog registered to a class loader
    private static final ClassLoaderValue<ServicesCatalog> CLV = new ClassLoaderValue<>();
}
