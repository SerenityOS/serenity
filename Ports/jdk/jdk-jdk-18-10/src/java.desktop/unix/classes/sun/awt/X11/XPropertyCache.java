/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.X11;

import java.util.*;

/**
 * Implements abstract X window property caching mechanism.  The
 * caching is performed using storeCache method, the cached data can
 * be retrieved using getCacheEntry method.
 *
 * NOTE: current caching is disabled because of the big variate of
 * uncovered access to properties/changes of properties.  Once the
 * access to properites is rewritten using general mechanisms, caching
 * will be enabled.
 */
public class XPropertyCache {

    static class PropertyCacheEntry {
        private final int format;
        private final int numberOfItems;
        private final long bytesAfter;
        private final long data;
        private final int dataLength;
        public PropertyCacheEntry(int format, int numberOfItems, long bytesAfter, long data, int dataLength) {
            this.format = format;
            this.numberOfItems = numberOfItems;
            this.bytesAfter = bytesAfter;
            this.data = XlibWrapper.unsafe.allocateMemory(dataLength);
            this.dataLength = dataLength;
            XlibWrapper.memcpy(this.data, data, dataLength);
        }

        public int getFormat() {
            return format;
        }

        public int getNumberOfItems() {
            return numberOfItems;
        }

        public long getBytesAfter() {
            return bytesAfter;
        }

        public long getData() {
            return data;
        }

        public int getDataLength() {
            return dataLength;
        }
    }

    private static Map<Long, Map<XAtom, PropertyCacheEntry>> windowToMap = new HashMap<Long, Map<XAtom, PropertyCacheEntry>>();

    public static boolean isCached(long window, XAtom property) {
        Map<XAtom, PropertyCacheEntry> entryMap = windowToMap.get(window);
        if (entryMap != null) {
            return entryMap.containsKey(property);
        } else {
            return false;
        }
    }

    public static PropertyCacheEntry getCacheEntry(long window, XAtom property) {
        Map<XAtom, PropertyCacheEntry> entryMap = windowToMap.get(window);
        if (entryMap != null) {
            return entryMap.get(property);
        } else {
            return null;
        }
    }

    public static void storeCache(PropertyCacheEntry entry, long window, XAtom property) {
        Map<XAtom, PropertyCacheEntry> entryMap = windowToMap.get(window);
        if (entryMap == null) {
            entryMap = new HashMap<XAtom, PropertyCacheEntry>();
            windowToMap.put(window, entryMap);
        }
        entryMap.put(property, entry);
    }

    public static void clearCache(long window) {
        windowToMap.remove(window);
    }

    public static void clearCache(long window, XAtom property) {
        Map<XAtom, PropertyCacheEntry> entryMap = windowToMap.get(window);
        if (entryMap != null) {
            entryMap.remove(property);
        }
    }

    public static boolean isCachingSupported() {
        // Currently - unsupported
        return false;
    }
}
