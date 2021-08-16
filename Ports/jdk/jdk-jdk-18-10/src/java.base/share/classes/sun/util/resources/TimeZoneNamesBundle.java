/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package sun.util.resources;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Set;

/**
 * Subclass of <code>ResourceBundle</code> with special
 * functionality for time zone names. The additional functionality:
 * <ul>
 * <li>Preserves the order of entries in the <code>getContents</code>
 *     array for the enumeration returned by <code>getKeys</code>.
 * <li>Inserts the time zone ID (the key of the bundle entries) into
 *     the string arrays returned by <code>handleGetObject</code>.
 * </ul>
 * All <code>TimeZoneNames</code> resource bundles must extend this
 * class and implement the <code>getContents</code> method.
 */
public abstract class TimeZoneNamesBundle extends OpenListResourceBundle {

    /**
     * Maps time zone IDs to locale-specific names.
     * The value returned is an array of five strings:
     * <ul>
     * <li>The time zone ID (same as the key, not localized).
     * <li>The long name of the time zone in standard time (localized).
     * <li>The short name of the time zone in standard time (localized).
     * <li>The long name of the time zone in daylight savings time (localized).
     * <li>The short name of the time zone in daylight savings time (localized).
     * <li>The long name of the time zone in generic form (localized).
     * <li>The short name of the time zone in generic form (localized).
     * </ul>
     * The localized names come from the subclasses's
     * <code>getContents</code> implementations, while the time zone
     * ID is inserted into the returned array by this method.
     */
    @Override
    public Object handleGetObject(String key) {
        Object val = super.handleGetObject(key);
        if (val instanceof String[]) {
            String[] contents = (String[]) val;
            int clen = contents.length;
            String[] tmpobj = new String[7];
            tmpobj[0] = key;
            System.arraycopy(contents, 0, tmpobj, 1, clen);
            return tmpobj;
        }
        return val;
    }

    /**
     * Use LinkedHashMap to preserve the order of bundle entries.
     */
    @Override
    protected <K, V> Map<K, V> createMap(int size) {
        return new LinkedHashMap<>(size);
    }

    /**
     * Use LinkedHashSet to preserve the key order.
     * @param <E> the type of elements
     * @return a Set
     */
    @Override
    protected <E> Set<E> createSet() {
        return new LinkedHashSet<>();
    }

    /**
     * Provides key/value mappings for a specific
     * resource bundle. Each entry of the array
     * returned must be an array with two elements:
     * <ul>
     * <li>The key, which must be a string.
     * <li>The value, which must be an array of
     *     four strings:
     *     <ul>
     *     <li>The long name of the time zone in standard time.
     *     <li>The short name of the time zone in standard time.
     *     <li>The long name of the time zone in daylight savings time.
     *     <li>The short name of the time zone in daylight savings time.
     *     </ul>
     * </ul>
     */
    protected abstract Object[][] getContents();
}
