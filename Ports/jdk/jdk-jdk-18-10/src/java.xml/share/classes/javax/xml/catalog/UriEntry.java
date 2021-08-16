/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Represents a uri entry.
 *
 * @since 9
 */
final class UriEntry extends BaseEntry {
    String name;
    URL uri;

    /**
     * Construct a uri entry.
     * @param name The name attribute.
     * @param uri The uri attribute.
     */
    public UriEntry(String base, String name, String uri) {
        super(CatalogEntryType.URI, base);
        setName(name);
        setURI(uri);
    }

    /**
     * Set the name attribute.
     * @param name The name attribute value.
     */
    public void setName(String name) {
        CatalogMessages.reportNPEOnNull("name", name);
        if (name.startsWith(Util.PUBLICID_PREFIX) || name.startsWith(Util.PUBLICID_PREFIX_ALT)) {
            this.name = Normalizer.normalizePublicId(name);
        } else {
            this.name = Normalizer.normalizeURI(name);
        }
    }

    /**
     * Set the uri attribute. If the value of the uri attribute is relative, it
     * must be made absolute with respect to the base URI currently in effect.
     *
     * @param uri The uri attribute value.
     */
    public void setURI(String uri) {
        this.uri = verifyURI("setURI", baseURI, uri);
    }

    /**
     * Get the name attribute.
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the uri attribute.
     * @return The uri attribute value.
     */
    public URL getURI() {
        return uri;
    }

    @Override
    public String match(String name) {
        if (this.name.equals(name)) {
            return uri.toString();
        }
        return null;
    }
}
