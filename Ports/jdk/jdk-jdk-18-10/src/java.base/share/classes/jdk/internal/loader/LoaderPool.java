/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.loader;

import java.lang.module.Configuration;
import java.lang.module.ResolvedModule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A pool of class loaders.
 *
 * @see ModuleLayer#defineModulesWithManyLoaders
 */

public final class LoaderPool {

    // maps module names to class loaders
    private final Map<String, Loader> loaders;


    /**
     * Creates a pool of class loaders. Each module in the given configuration
     * is mapped to its own class loader in the pool. The class loader is
     * created with the given parent class loader as its parent.
     */
    public LoaderPool(Configuration cf,
                      List<ModuleLayer> parentLayers,
                      ClassLoader parentLoader)
    {
        Map<String, Loader> loaders = new HashMap<>();
        for (ResolvedModule resolvedModule : cf.modules()) {
            Loader loader = new Loader(resolvedModule, this, parentLoader);
            String mn = resolvedModule.name();
            loaders.put(mn, loader);
        }
        this.loaders = loaders;

        // complete the initialization
        loaders.values().forEach(l -> l.initRemotePackageMap(cf, parentLayers));
    }


    /**
     * Returns the class loader for the named module
     */
    public Loader loaderFor(String name) {
        Loader loader = loaders.get(name);
        assert loader != null;
        return loader;
    }

    /**
     * Returns a stream of the loaders in this pool.
     */
    public Stream<Loader> loaders() {
        return loaders.values().stream();
    }

}
