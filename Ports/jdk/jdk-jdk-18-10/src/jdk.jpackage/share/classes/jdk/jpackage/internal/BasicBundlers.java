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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * BasicBundlers
 *
 * A basic bundlers collection that loads the default bundlers.
 * Loads the common bundlers.
 * <UL>
 *     <LI>Windows file image</LI>
 *     <LI>Mac .app</LI>
 *     <LI>Linux file image</LI>
 *     <LI>Windows MSI</LI>
 *     <LI>Windows EXE</LI>
 *     <LI>Mac DMG</LI>
 *     <LI>Mac PKG</LI>
 *     <LI>Linux DEB</LI>
 *     <LI>Linux RPM</LI>
 *
 * </UL>
 */
public class BasicBundlers implements Bundlers {

    boolean defaultsLoaded = false;

    private final Collection<Bundler> bundlers = new CopyOnWriteArrayList<>();

    @Override
    public Collection<Bundler> getBundlers() {
        return Collections.unmodifiableCollection(bundlers);
    }

    @Override
    public Collection<Bundler> getBundlers(String type) {
        if (type == null) return Collections.emptySet();
        switch (type) {
            case "NONE":
                return Collections.emptySet();
            case "ALL":
                return getBundlers();
            default:
                return Arrays.asList(getBundlers().stream()
                        .filter(b -> type.equalsIgnoreCase(b.getBundleType()))
                        .toArray(Bundler[]::new));
        }
    }

    // Loads bundlers from the META-INF/services direct
    @Override
    public void loadBundlersFromServices(ClassLoader cl) {
        ServiceLoader<Bundler> loader = ServiceLoader.load(Bundler.class, cl);
        for (Bundler aLoader : loader) {
            bundlers.add(aLoader);
        }
    }
}
