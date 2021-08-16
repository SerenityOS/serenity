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
 * Represents a system entry.
 *
 * @since 9
 */
final class SystemEntry extends BaseEntry {
    String systemId;
    URL uri;

    /**
     * Construct a system entry.
     *
     * @param systemId The systemId attribute.
     * @param uri The uri attribute.
     */
    public SystemEntry(String base, String systemId, String uri) {
        super(CatalogEntryType.SYSTEM, base);
        setSystemId(systemId);
        setURI(uri);
    }

    /**
     * Set the systemId attribute.
     * @param systemId The systemId attribute value.
     */
    public void setSystemId(String systemId) {
        CatalogMessages.reportNPEOnNull("systemId", systemId);
        this.systemId = Normalizer.normalizeURI(systemId);
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
     * Get the systemId attribute.
     * @return The systemId
     */
    public String getSystemId() {
        return systemId;
    }
    /**
     * Get the uri attribute.
     * @return The uri attribute value.
     */
    public URL getURI() {
        return uri;
    }

    /**
     * Try to match the specified string with the entry
     *
     * @param systemId The systemId to be matched
     * @return The replacement URI if the match is successful, null if not.
     */
    @Override
    public String match(String systemId) {
        if (this.systemId.equals(systemId)) {
            return uri.toString();
        }
        return null;
    }
}
