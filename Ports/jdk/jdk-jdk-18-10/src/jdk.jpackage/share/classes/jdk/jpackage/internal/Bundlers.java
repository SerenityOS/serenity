/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Bundlers
 *
 * The interface implemented by BasicBundlers
 */
public interface Bundlers {

    /**
     * This convenience method will call
     * {@link #createBundlersInstance(ClassLoader)}
     * with the classloader that this Bundlers is loaded from.
     *
     * @return an instance of Bundlers loaded and configured from
     *         the current ClassLoader.
     */
    public static Bundlers createBundlersInstance() {
        return createBundlersInstance(Bundlers.class.getClassLoader());
    }

    /**
     * This convenience method will automatically load a Bundlers instance
     * from either META-INF/services or the default
     * {@link BasicBundlers} if none are found in
     * the services meta-inf.
     *
     * After instantiating the bundlers instance it will load the default
     * bundlers via {@link #loadDefaultBundlers()} as well as requesting
     * the services loader to load any other bundelrs via
     * {@link #loadBundlersFromServices(ClassLoader)}.

     *
     * @param servicesClassLoader the classloader to search for
     *                            META-INF/service registered bundlers
     * @return an instance of Bundlers loaded and configured from
     *         the specified ClassLoader
     */
    public static Bundlers createBundlersInstance(
            ClassLoader servicesClassLoader) {
        ServiceLoader<Bundlers> bundlersLoader =
                ServiceLoader.load(Bundlers.class, servicesClassLoader);
        Bundlers bundlers = null;
        Iterator<Bundlers> iter = bundlersLoader.iterator();
        if (iter.hasNext()) {
            bundlers = iter.next();
        }
        if (bundlers == null) {
            bundlers = new BasicBundlers();
        }

        bundlers.loadBundlersFromServices(servicesClassLoader);
        return bundlers;
    }

    /**
     * Returns all of the preconfigured, requested, and manually
     * configured bundlers loaded with this instance.
     *
     * @return  a read-only collection of the requested bundlers
     */
    Collection<Bundler> getBundlers();

    /**
     * Returns all of the preconfigured, requested, and manually
     * configured bundlers loaded with this instance that are of
     * a specific BundleType, such as disk images, installers, or
     * remote installers.
     *
     * @return a read-only collection of the requested bundlers
     */
    Collection<Bundler> getBundlers(String type);

    /**
     * Loads bundlers from the META-INF/services directly.
     *
     * This method is called from the
     * {@link #createBundlersInstance(ClassLoader)}
     * and {@link #createBundlersInstance()} methods.
     */
    void loadBundlersFromServices(ClassLoader cl);

}
