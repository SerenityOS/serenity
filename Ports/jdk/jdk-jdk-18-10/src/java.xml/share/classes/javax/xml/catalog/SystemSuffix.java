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

import java.net.URL;

/**
 * Represents a systemSuffix entry.
 *
 * @since 9
 */
final class SystemSuffix extends BaseEntry {
    String systemIdSuffix;
    URL uri;

    /**
     * Construct a systemSuffix entry.
     * @param systemIdSuffix The systemIdSuffix attribute.
     * @param uri The uri attribute.
     */
    public SystemSuffix(String base, String systemIdSuffix, String uri) {
        super(CatalogEntryType.SYSTEMSUFFIX, base);
        setSystemIdSuffix(systemIdSuffix);
        setURI(uri);
    }

    /**
     * Set the systemIdSuffix attribute.
     * @param systemIdSuffix The systemIdSuffix attribute value.
     */
    public void setSystemIdSuffix(String systemIdSuffix) {
        CatalogMessages.reportNPEOnNull("systemIdSuffix", systemIdSuffix);
        this.systemIdSuffix = Normalizer.normalizeURI(systemIdSuffix);
    }

    /**
     * Set the uri attribute. If the value of the uri attribute is relative, it
     * must be made absolute with respect to the base URI currently in effect.
     * The URI reference should not include a fragment identifier.
     * @param uri The uri attribute value.
     */
    public void setURI(String uri) {
        this.uri = verifyURI("setURI", baseURI, uri);
    }

    /**
     * Get the systemIdSuffix attribute.
     * @return The systemIdSuffix
     */
    public String getSystemIdSuffix  () {
        return systemIdSuffix;
    }
    /**
     * Get the uri attribute.
     * @return The uri attribute value.
     */
    public URL getURI() {
        return uri;
    }

    /**
     * Try to match the specified systemId with the entry. Return the match if it
     * is successful and the length of the systemIdSuffix is longer than the longest
     * of any previous match.
     *
     * @param systemId The systemId to be matched.
     * @param currentMatch The length of systemIdSuffix of previous match if any.
     * @return The replacement URI if the match is successful, null if not.
     */
    @Override
    public String match(String systemId, int currentMatch) {
        if (systemId.endsWith(systemIdSuffix)) {
            if (currentMatch < systemIdSuffix.length()) {
                return uri.toString();
            }
        }
        return null;
    }

    /**
     * Try to match the specified systemId with the entry.
     *
     * @param systemId The systemId to be matched.
     * @return The replacement URI if the match is successful, null if not.
     */
    @Override
    public String match(String systemId) {
        return match(systemId, 0);
    }
}
