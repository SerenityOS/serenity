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

import java.io.StringReader;
import javax.xml.catalog.BaseEntry.CatalogEntryType;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * CatalogReader handles SAX events while parsing through a catalog file to
 * create a catalog object.
 *
 * @since 9
 */
class CatalogReader extends DefaultHandler implements EntityResolver, URIResolver {
    /** URI of the W3C XML Schema for OASIS XML Catalog files. */
    public static final String xmlCatalogXSD = "http://www.oasis-open.org/committees/entity/release/1.0/catalog.xsd";

    /** Public identifier for OASIS XML Catalog files. */
    public static final String xmlCatalogPubId = "-//OASIS//DTD XML Catalogs V1.0//EN";

    /**
     * The namespace name defined by the OASIS Standard
     */
    public static final String NAMESPACE_OASIS = "urn:oasis:names:tc:entity:xmlns:xml:catalog";

    //Indicate whether the root element has been found
    boolean seenRoot;

    //Indicate that the parser is in a group entry
    boolean inGroup;

    //The Catalog instance
    CatalogImpl catalog;

    //The parser for reading the catalog
    SAXParser parser;

    //The current catalog entry
    CatalogEntry catalogEntry;

    //The current group
    GroupEntry group;

    //The current entry
    BaseEntry entry;

    //remove this variable once 8136778 is committed
    boolean ignoreTheCatalog = false;

    /**
     * Constructs an instance with a Catalog object and parser.
     *
     * @param catalog The Catalog object that represents a catalog
     */
    @SuppressWarnings("unchecked")
    public CatalogReader(Catalog catalog, SAXParser parser) {
        this.catalog = (CatalogImpl) catalog;
        this.parser = parser;
    }

    @Override
    public void startElement(String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {

        //ignore the catalog if it's not compliant. See section 8, item 3 of the spec.
        if (ignoreTheCatalog) return;
        if (!NAMESPACE_OASIS.equals(namespaceURI)) {
//wait till 8136778 is committed
//            parser.stop();
            ignoreTheCatalog = true;
            return;
        }


        CatalogEntryType type = CatalogEntryType.getType(localName);
        if (type == null) {
            CatalogMessages.reportError(CatalogMessages.ERR_INVALID_ENTRY_TYPE,
                    new Object[]{localName});
        }
        if (type != CatalogEntryType.CATALOGENTRY) {
            if (!seenRoot) {
                CatalogMessages.reportError(CatalogMessages.ERR_INVALID_CATALOG);
            }
        }

        String base = atts.getValue("xml:base");
        if (base == null) {
            if (inGroup) {
                base = group.getBaseURI().toString();
            } else {
                if (type == CatalogEntryType.CATALOGENTRY) {
                    base = catalog.getBaseURI().toString();
                } else {
                    base = catalogEntry.getBaseURI().toString();
                }
            }
        } else {
            base = Normalizer.normalizeURI(base);
        }

        //parse the catalog and group entries
        if (type == CatalogEntryType.CATALOGENTRY
                || type == CatalogEntryType.GROUP) {
            String prefer = atts.getValue("prefer");
            if (prefer == null) {
                if (type == CatalogEntryType.CATALOGENTRY) {
                    //use the general setting
                    prefer = catalog.isPreferPublic() ?
                            CatalogFeatures.PREFER_PUBLIC : CatalogFeatures.PREFER_SYSTEM;
                } else {
                    //Group inherit from the catalog entry
                    prefer = catalogEntry.isPreferPublic() ?
                            CatalogFeatures.PREFER_PUBLIC : CatalogFeatures.PREFER_SYSTEM;
                }
            }

            if (type == CatalogEntryType.CATALOGENTRY) {
                seenRoot = true;
                if (catalog.isTop()) {
                    String defer = atts.getValue("defer");
                    String resolve = atts.getValue("resolve");
                    if (defer == null) {
                        defer = catalog.isDeferred() ?
                                CatalogFeatures.DEFER_TRUE : CatalogFeatures.DEFER_FALSE;
                    }
                    if (resolve == null) {
                        resolve = catalog.getResolve().literal;
                    }
                    //override property settings with those from the catalog file
                    catalog.setResolve(resolve);
                    catalog.setDeferred(defer);
                    catalogEntry = new CatalogEntry(base, prefer, defer, resolve);
                } else {
                    catalogEntry = new CatalogEntry(base, prefer);
                }
                catalog.setPrefer(prefer);
                return;
            } else {
                inGroup = true;
                group = new GroupEntry(catalog, base, prefer);
                catalog.addEntry(group);
                return;
            }
        }

        //parse entries other than the catalog and group entries
        switch (type) {
            case PUBLIC:
                entry = new PublicEntry(base, atts.getValue("publicId"), atts.getValue("uri"));
                break;
            case SYSTEM:
                entry = new SystemEntry(base, atts.getValue("systemId"), atts.getValue("uri"));
                break;
            case REWRITESYSTEM:
                entry = new RewriteSystem(base, atts.getValue("systemIdStartString"), atts.getValue("rewritePrefix"));
                break;
            case SYSTEMSUFFIX:
                entry = new SystemSuffix(base, atts.getValue("systemIdSuffix"), atts.getValue("uri"));
                break;
            case DELEGATEPUBLIC:
                entry = new DelegatePublic(base, atts.getValue("publicIdStartString"), atts.getValue("catalog"));
                break;
            case DELEGATESYSTEM:
                entry = new DelegateSystem(base, atts.getValue("systemIdStartString"), atts.getValue("catalog"));
                break;
            case URI:
                entry = new UriEntry(base, atts.getValue("name"), atts.getValue("uri"));
                break;
            case REWRITEURI:
                entry = new RewriteUri(base, atts.getValue("uriStartString"), atts.getValue("rewritePrefix"));
                break;
            case URISUFFIX:
                entry = new UriSuffix(base, atts.getValue("uriSuffix"), atts.getValue("uri"));
                break;
            case DELEGATEURI:
                entry = new DelegateUri(base, atts.getValue("uriStartString"), atts.getValue("catalog"));
                break;
            case NEXTCATALOG:
                entry = new NextCatalog(base, atts.getValue("catalog"));
                break;
        }

        if (type == CatalogEntryType.NEXTCATALOG) {
            catalog.addNextCatalog((NextCatalog) entry);
        } else if (inGroup) {
            group.addEntry(entry);
        } else {
            catalog.addEntry(entry);
        }

    }

    /**
     * Handles endElement event
     */
    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        if (ignoreTheCatalog) return;

        CatalogEntryType type = CatalogEntryType.getType(localName);
        if (type == CatalogEntryType.GROUP) {
            inGroup = false;
        }
    }


    /**
     * Skips external DTD since resolving external DTD is not required
     * by the specification.
     */
    @Override
    public InputSource resolveEntity(String publicId, String systemId) {
        return new InputSource(new StringReader(""));
    }

    /**
     * Skips external references since resolution is not required
     * by the specification.
     */
    @Override
    public Source resolve(String href, String base)
            throws TransformerException {
        return new SAXSource(new InputSource(new StringReader("")));
    }

}
