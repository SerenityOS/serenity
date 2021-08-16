/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorMap;
import java.util.LinkedHashSet;
import java.util.function.Supplier;

/**
 * Contains services which desktop provides to the datatransfer system to enrich
 * it's functionality.
 *
 * @author Petr Pchelko
 * @since 9
 */
public interface DesktopDatatransferService {

    /**
     * If desktop is present - invokes a {@code Runnable} on the event dispatch
     * thread. Otherwise invokes a {@code run()} method directly.
     *
     * @param  r a {@code Runnable} to invoke
     */
    void invokeOnEventThread(Runnable r);

    /**
     * Get a platform-dependent default unicode encoding to use in datatransfer
     * system.
     *
     * @return default unicode encoding
     */
    String getDefaultUnicodeEncoding();

    /**
     * Takes an appropriate {@code FlavorMap} from the desktop. If no
     * appropriate table is found - uses a provided supplier to instantiate a
     * table. If the desktop is absent - creates and returns a system singleton.
     *
     * @param  supplier a constructor that should be used to create a new
     *         instance of the {@code FlavorMap}
     * @return a {@code FlavorMap}
     */
    FlavorMap getFlavorMap(Supplier<FlavorMap> supplier);

    /**
     * Checks if desktop is present.
     *
     * @return {@code true} is the desktop is present
     */
    boolean isDesktopPresent();

    /**
     * Returns platform-specific mappings for the specified native format. If
     * there are no platform-specific mappings for this native, the method
     * returns an empty {@code Set}.
     *
     * @param  nat a native format to return flavors for
     * @return set of platform-specific mappings for a native format
     */
    LinkedHashSet<DataFlavor> getPlatformMappingsForNative(String nat);

    /**
     * Returns platform-specific mappings for the specified flavor. If there are
     * no platform-specific mappings for this flavor, the method returns an
     * empty {@code Set}.
     *
     * @param  df {@code DataFlavor} to return mappings for
     * @return set of platform-specific mappings for a {@code DataFlavor}
     */
    LinkedHashSet<String> getPlatformMappingsForFlavor(DataFlavor df);

    /**
     * This method is called for text flavor mappings established while parsing
     * the default flavor mappings file. It stores the "eoln" and "terminators"
     * parameters which are not officially part of the MIME type. They are MIME
     * parameters specific to the flavormap.properties file format.
     */
    void registerTextFlavorProperties(String nat, String charset,
                                      String eoln, String terminators);
}
