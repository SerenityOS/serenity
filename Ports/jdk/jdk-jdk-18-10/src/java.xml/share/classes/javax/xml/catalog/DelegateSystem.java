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
package javax.xml.catalog;

import java.net.URI;

/**
 * Represents a delegateSystem entry.
 *
 * @since 9
 */
final class DelegateSystem extends AltCatalog {
    String systemIdStartString;

    /**
     * Construct a delegateSystem entry.
     * @param systemIdStartString The systemIdStartString attribute.
     * @param catalog The catalog attribute.
     */
    public DelegateSystem(String base, String systemIdStartString, String catalog) {
        super(CatalogEntryType.DELEGATESYSTEM, base);
        setSystemIdStartString(systemIdStartString);
        setCatalog(catalog);
    }

    /**
     * Set the systemIdStartString attribute.
     * @param systemIdStartString The systemIdStartString attribute value.
     */
    public void setSystemIdStartString (String systemIdStartString) {
        CatalogMessages.reportNPEOnNull("systemIdStartString", systemIdStartString);
        this.systemIdStartString = Normalizer.normalizeURI(systemIdStartString);
        setMatchId(this.systemIdStartString);
    }

    /**
     * Get the systemIdStartString attribute.
     * @return The systemIdStartString
     */
    public String getSystemIdStartString () {
        return systemIdStartString;
    }

    /**
     * Try to match the specified systemId with the entry.
     *
     * @param systemId The systemId to be matched.
     * @return The URI of the catalog.
     */
    @Override
    public String match(String systemId) {
        return match(systemId, 0);
    }

    /**
     * Matches the specified publicId with the entry. Return the match if it
     * is successful and the length of the systemIdStartString is longer than the
     * longest of any previous match.
     *
     * @param systemId The systemId to be matched.
     * @param currentMatch The length of systemIdStartString of previous match if any.
     * @return The replacement URI if the match is successful, null if not.
     */
    @Override
    public URI matchURI(String systemId, int currentMatch) {
        if (systemIdStartString.length() <= systemId.length() &&
                systemIdStartString.equals(systemId.substring(0, systemIdStartString.length()))) {
            if (currentMatch < systemIdStartString.length()) {
                return catalogURI;
            }
        }
        return null;
    }

}
