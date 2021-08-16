/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.datatransfer;

import sun.awt.AppContext;
import sun.datatransfer.DesktopDatatransferService;

import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorMap;
import java.util.LinkedHashSet;
import java.util.function.Supplier;

/**
 * Provides desktop services to the datatransfer module according to
 * {@code DesktopDatatransferService} interface.
 *
 * @author Petr Pchelko
 * @since 9
 */
public class DesktopDatatransferServiceImpl implements DesktopDatatransferService {

    private static final Object FLAVOR_MAP_KEY = new Object();

    @Override
    public void invokeOnEventThread(Runnable r) {
        EventQueue.invokeLater(r);
    }

    @Override
    public String getDefaultUnicodeEncoding() {
        DataTransferer dataTransferer = DataTransferer.getInstance();
        if (dataTransferer != null) {
            return dataTransferer.getDefaultUnicodeEncoding();
        }
        return null;
    }

    @Override
    public FlavorMap getFlavorMap(Supplier<FlavorMap> supplier) {
        AppContext context = AppContext.getAppContext();
        FlavorMap fm = (FlavorMap) context.get(FLAVOR_MAP_KEY);
        if (fm == null) {
            fm = supplier.get();
            context.put(FLAVOR_MAP_KEY, fm);
        }
        return fm;
    }

    @Override
    public boolean isDesktopPresent() {
        return true;
    }

    @Override
    public LinkedHashSet<DataFlavor> getPlatformMappingsForNative(String nat) {
        DataTransferer instance = DataTransferer.getInstance();
        return instance != null ? instance.getPlatformMappingsForNative(nat) : new LinkedHashSet<>();
    }

    @Override
    public LinkedHashSet<String> getPlatformMappingsForFlavor(DataFlavor df) {
        DataTransferer instance = DataTransferer.getInstance();
        return instance != null ? instance.getPlatformMappingsForFlavor(df) : new LinkedHashSet<>();
    }

    @Override
    public void registerTextFlavorProperties(String nat, String charset, String eoln, String terminators) {
        DataTransferer instance = DataTransferer.getInstance();
        if (instance != null) {
            instance.registerTextFlavorProperties(nat, charset, eoln, terminators);
        }
    }
}
