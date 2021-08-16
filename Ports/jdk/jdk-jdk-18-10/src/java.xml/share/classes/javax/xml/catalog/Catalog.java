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

import java.util.stream.Stream;

/**
 * The Catalog class represents an entity Catalog as defined by
 * <a
 * href="https://www.oasis-open.org/committees/download.php/14809/xml-catalogs.html">
 * XML Catalogs, OASIS Standard V1.1, 7 October 2005</a>.
 * <p>
 * A catalog is an XML file that contains a root {@code catalog} entry with a list
 * of catalog entries. The entries can also be grouped with a {@code group} entry.
 * The catalog and group entries may specify {@code prefer} and {@code xml:base}
 * attributes that set preference of public or system type of entries and base URI
 * to resolve relative URIs.
 *
 * <p>
 * A catalog can be used in two situations:
 * <ul>
 * <li>Locate the external resources with a public or system identifier;
 * </li>
 * <li>Locate an alternate URI reference with a URI.
 * </li>
 * </ul>
 * <p>
 * For case 1, the standard defines 6 External Identifier Entries:<br>
 * {@code public, system, rewriteSystem, systemSuffix, delegatePublic, and
 * delegateSystem}.
 * <p>
 * While for case 2, it defines 4 URI Entries:<br>
 * {@code uri, rewriteURI, uriSuffix and delegateURI}.
 * <p>
 * In addition to the above entry types, a catalog may define nextCatalog
 * entries to add additional catalog entry files.
 *
 * @since 9
 */
public interface Catalog {

    /**
     * Attempts to find a matching entry in the catalog by systemId.
     *
     * <p>
     * The method searches through the system-type entries, including {@code system,
     * rewriteSystem, systemSuffix, delegateSystem}, and {@code group} entries in the
     * current catalog in order to find a match.
     * <p>
     * Resolution follows the steps listed below: <br>
     * <ul>
     * <li>If a matching {@code system} entry exists, it is returned immediately.</li>
     * <li>If more than one {@code rewriteSystem} entry matches, the matching entry with
     * the longest normalized {@code systemIdStartString} value is returned.</li>
     * <li>If more than one {@code systemSuffix} entry matches, the matching entry
     * with the longest normalized {@code systemIdSuffix} value is returned.</li>
     * <li>If more than one {@code delegateSystem} entry matches, the matching entry
     * with the longest matching {@code systemIdStartString} value is returned.</li>
     * </ul>
     *
     * @param systemId the system identifier of the entity to be matched
     *
     * @return a URI string if a mapping is found, or null otherwise
     */
    public String matchSystem(String systemId);

    /**
     * Attempts to find a matching entry in the catalog by publicId. The method
     * searches through the public-type entries, including {@code public,
     * delegatePublic}, and {@code group} entries in the current catalog in order to find
     * a match.
     * <p>
     * Refer to the description about <a href="CatalogFeatures.html#PREFER">
     * Feature PREFER in the table Catalog Features</a> in class
     * {@link CatalogFeatures}. Public entries are only considered if the
     * {@code prefer} is {@code public} and {@code system} entries are not found.
     * <p>
     * Resolution follows the steps listed below: <br>
     * <ul>
     * <li>If a matching {@code public} entry is found, it is returned immediately.</li>
     * <li>If more than one {@code delegatePublic} entry matches, the matching entry
     * with the longest matching {@code publicIdStartString} value is returned.</li>
     * </ul>
     *
     * @param publicId the public identifier of the entity to be matched
     * @see CatalogFeatures.Feature
     * @return a URI string if a mapping is found, or null otherwise
     */
    public String matchPublic(String publicId);

    /**
     * Attempts to find a matching entry in the catalog by the uri element.
     *
     * <p>
     * The method searches through the uri-type entries, including {@code uri,
     * rewriteURI, uriSuffix, delegateURI} and {@code group} entries in the current
     * catalog in order to find a match.
     *
     * <p>
     * Resolution follows the steps listed below: <br>
     * <ul>
     * <li>If a matching {@code uri} entry is found, it is returned immediately.</li>
     * <li>If more than one {@code rewriteURI} entry matches, the matching entry with
     * the longest normalized {@code uriStartString} value is returned.</li>
     * <li>If more than one {@code uriSuffix} entry matches, the matching entry with
     * the longest normalized {@code uriSuffix} value is returned.</li>
     * <li>If more than one {@code delegatePublic} entry matches, the matching entry
     * with the longest matching {@code uriStartString} value is returned.</li>
     * </ul>
     *
     * @param uri the URI reference of the entity to be matched
     *
     * @return a URI string if a mapping is found, or null otherwise
     */
    public String matchURI(String uri);

    /**
     * Returns a sequential Stream of alternative Catalogs specified using the
     * {@code nextCatalog} entries in the current catalog, and as the input of
     * catalog files excluding the current catalog (that is, the first in the
     * input list) when the Catalog object is created by the {@link CatalogManager}.
     * <p>
     * The order of Catalogs in the returned stream is the same as the order
     * in which the corresponding {@code nextCatalog} entries appear in the
     * current catalog. The alternative catalogs from the input file list are
     * appended to the end of the stream in the order they are entered.
     *
     * @return a sequential Stream of Catalogs
     */
    public Stream<Catalog> catalogs();

}
