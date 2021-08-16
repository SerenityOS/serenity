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

import java.io.InputStream;
import javax.xml.stream.XMLResolver;
import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * A Catalog Resolver that implements SAX {@link org.xml.sax.EntityResolver},
 * StAX {@link javax.xml.stream.XMLResolver},
 * DOM LS {@link org.w3c.dom.ls.LSResourceResolver} used by Schema Validation, and
 * Transform {@link javax.xml.transform.URIResolver}, and resolves
 * external references using catalogs.
 * <p>
 * The <a href="https://www.oasis-open.org/committees/download.php/14809/xml-catalogs.html">
 * Catalog Standard</a> distinguished {@code external identifiers} from {@code uri entries}
 * as being used to solely identify DTDs, while {@code uri entries} for
 * other resources such as stylesheets and schema. The Java APIs, such as
 * {@link javax.xml.stream.XMLResolver} and {@link org.w3c.dom.ls.LSResourceResolver}
 * however, make no such distinction.
 * In consistent with the existing Java API, this CatalogResolver recognizes a
 * system identifier as a URI and will search both {@code system} and {@code uri}
 * entries in a catalog in order to find a matching entry.
 * <p>
 * The search is started in the current catalog. If a match is found,
 * no further attempt will be made. Only if there is no match in the current
 * catalog, will alternate catalogs including delegate and next catalogs be considered.
 *
 * <h2>Search Order</h2>
 * The resolver will first search the system-type of entries with the specified
 * {@code systemId}. The system entries include {@code system},
 * {@code rewriteSystem} and {@code systemSuffix} entries.
 * <p>
 * If no match is found, {@code public} entries may be searched in accordance with
 * the {@code prefer} attribute.
 * <p>
 * <b>The {@code prefer} attribute</b>: if the {@code prefer} is public,
 * and there is no match found through the system entries, {@code public} entries
 * will be considered. If it is not specified, the {@code prefer} is public
 * by default (Note that by the OASIS standard, system entries will always
 * be considered before public entries. Prefer public means that public entries
 * will be matched when both system and public identifiers are specified.
 * In general therefore, prefer public is recommended.)
 * <p>
 * If no match is found with the {@code systemId} and {@code public} identifier,
 * the resolver will continue searching {@code uri} entries
 * with the specified {@code systemId} or {@code href}. The {@code uri} entries
 * include {@code uri}, {@code rewriteURI}, and {@code uriSuffix} entries.
 *
 * <h2>Error Handling</h2>
 * The interfaces that the CatalogResolver extend specified checked exceptions, including:
 * <ul>
 * <li>
 * {@link org.xml.sax.SAXException} and {@link java.io.IOException} by
 * {@link org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)}
 * </li>
 * <li>
 * {@link javax.xml.stream.XMLStreamException} by
 * {@link javax.xml.stream.XMLResolver#resolveEntity(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
 * </li>
 * <li>
 * {@link javax.xml.transform.TransformerException} by
 * {@link javax.xml.transform.URIResolver#resolve(java.lang.String, java.lang.String)}
 * </li>
 * </ul>
 * <p>
 * The CatalogResolver however, will throw {@link javax.xml.catalog.CatalogException}
 * only when {@code javax.xml.catalog.resolve} is specified as {@code strict}.
 * For applications that expect to handle the checked Exceptions, it may be
 * necessary to use a custom resolver to wrap the CatalogResolver or implement it
 * with a {@link javax.xml.catalog.Catalog} object.
 *
 * @since 9
 */
public interface CatalogResolver extends EntityResolver, XMLResolver,
        URIResolver, LSResourceResolver {

    /**
     * Implements {@link org.xml.sax.EntityResolver}. The method searches through
     * the catalog entries in the main and alternative catalogs to attempt to find
     * a match with the specified {@code publicId} or systemId.
     *
     * @param publicId the public identifier of the external entity being
     * referenced, or null if none was supplied
     *
     * @param systemId the system identifier of the external entity being
     * referenced. A system identifier is required on all external entities. XML
     * requires a system identifier on all external entities, so this value is
     * always specified.
     *
     * @return a {@link org.xml.sax.InputSource} object if a mapping is found.
     * If no mapping is found, returns a {@link org.xml.sax.InputSource} object
     * containing an empty {@link java.io.Reader} if the
     * {@code javax.xml.catalog.resolve} property is set to {@code ignore};
     * returns null if the
     * {@code javax.xml.catalog.resolve} property is set to {@code continue}.
     *
     * @throws CatalogException if no mapping is found and
     * {@code javax.xml.catalog.resolve} is specified as {@code strict}
     */
    @Override
    public InputSource resolveEntity(String publicId, String systemId);


    /**
     * Implements URIResolver. The method searches through the catalog entries
     * in the main and alternative catalogs to attempt to find a match
     * with the specified {@code href} attribute. The {@code href} attribute will
     * be used literally, with no attempt to be made absolute to the {@code base}.
     * <p>
     * If the value is a URN, the {@code href} attribute is recognized as a
     * {@code publicId}, and used to search {@code public} entries.
     * If the value is a URI, it is taken as a {@code systemId}, and used to
     * search both {@code system} and {@code uri} entries.
     *
     *
     * @param href the href attribute that specifies the URI of a style sheet,
     * which may be relative or absolute
     * @param base The base URI against which the href attribute will be made
     * absolute if the absolute URI is required
     *
     * @return a {@link javax.xml.transform.Source} object if a mapping is found.
     * If no mapping is found, returns an empty {@link javax.xml.transform.Source}
     * object if the {@code javax.xml.catalog.resolve} property is set to
     * {@code ignore};
     * returns a {@link javax.xml.transform.Source} object with the original URI
     * (href, or href resolved with base if base is not null) if the
     * {@code javax.xml.catalog.resolve} property is set to {@code continue}.
     *
     * @throws CatalogException if no mapping is found and
     * {@code javax.xml.catalog.resolve} is specified as {@code strict}
     */
    @Override
    public Source resolve(String href, String base);

    /**
     * Implements {@link javax.xml.stream.XMLResolver}. For the purpose of resolving
     * {@code publicId} and {@code systemId}, this method is equivalent to
     * {@link #resolveEntity(java.lang.String, java.lang.String) }.
     * <p>
     * The {@code systemId} will be used literally, with no attempt to be made
     * absolute to the {@code baseUri}. The {@code baseUri} and {@code namespace}
     * are not used in the search for a match in a catalog. However, a relative
     * {@code systemId} in an xml source may have been made absolute by the parser
     * with the {@code baseURI}, thus making it unable to find a {@code system} entry.
     * In such a case, a {@code systemSuffix} entry is recommended over a
     * {@code system} entry.
     *
     * @param publicId the public identifier of the external entity being
     * referenced, or null if none was supplied
     *
     * @param systemId the system identifier of the external entity being
     * referenced. A system identifier is required on all external entities. XML
     * requires a system identifier on all external entities, so this value is
     * always specified.
     * @param baseUri  the absolute base URI, not used by the CatalogResolver
     * @param namespace the namespace of the entity to resolve, not used by the
     * CatalogResolver.
     *
     * @return an {@link java.io.InputStream} object if a mapping is found; null
     * if no mapping is found and the {@code javax.xml.catalog.resolve} property
     * is set to {@code continue} or {@code ignore}. Note that for XMLResolver,
     * it is not possible to ignore a reference, {@code ignore} is therefore
     * treated the same as {@code continue}.
     *
     * @throws CatalogException if no mapping is found and
     * {@code javax.xml.catalog.resolve} is specified as {@code strict}
     */
    @Override
    public InputStream resolveEntity(String publicId, String systemId,
            String baseUri, String namespace);

    /**
     * Implements {@link org.w3c.dom.ls.LSResourceResolver}. For the purpose of
     * resolving {@code publicId} and {@code systemId}, this method is equivalent
     * to {@link #resolveEntity(java.lang.String, java.lang.String) }.
     * <p>
     * The {@code systemId} will be used literally, with no attempt to be made
     * absolute to the {@code baseUri}. The {@code baseUri}, {@code namespaceUri}
     * and {@code type} are not used in the search for a match in a catalog.
     * However, a relative {@code systemId} in a source may have been made absolute
     * by the parser with the {@code baseURI}, thus making it unable to find a
     * {@code system} entry. In such a case, a {@code systemSuffix} entry is
     * recommended over a {@code system} entry.
     *
     * @param type  the type of the resource being resolved,
     * not used by the CatalogResolver
     * @param namespaceUri  the namespace of the resource being resolved,
     * not used by the CatalogResolver
     * @param publicId  the public identifier of the external entity being
     *   referenced, or {@code null} if no public identifier was
     *   supplied or if the resource is not an entity.
     * @param systemId  the system identifier, a URI reference of the
     *   external resource being referenced
     * @param baseUri  the absolute base URI, not used by the CatalogResolver
     *
     * @return a {@link org.w3c.dom.ls.LSInput} object if a mapping is found; null
     * if no mapping is found and the {@code javax.xml.catalog.resolve} property
     * is set to {@code continue} or {@code ignore}. Note that for
     * {@link org.w3c.dom.ls.LSResourceResolver}, it is not possible to ignore a
     * reference, {@code ignore} is therefore treated the same as {@code continue}.
     *
     * @throws CatalogException if no mapping is found and
     * {@code javax.xml.catalog.resolve} is specified as {@code strict}
     */
    @Override
    public LSInput resolveResource(String type, String namespaceUri,
            String publicId, String systemId, String baseUri);

}
