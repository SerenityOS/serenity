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

import com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import static javax.xml.catalog.BaseEntry.CatalogEntryType;
import static javax.xml.catalog.CatalogFeatures.DEFER_TRUE;
import javax.xml.catalog.CatalogFeatures.Feature;
import static javax.xml.catalog.CatalogMessages.formatMessage;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 * Implementation of the Catalog.
 *
 * @since 9
 */
class CatalogImpl extends GroupEntry implements Catalog {

    //Catalog level, 0 means the top catalog
    int level = 0;

    //Value of the defer attribute to determine if alternative catalogs are read
    boolean isDeferred = true;

    //Value of the resolve attribute
    ResolveType resolveType = ResolveType.STRICT;

    //indicate whether the Catalog is empty
    boolean isEmpty;

    //Current parsed Catalog
    int current = 0;

    //System Id for this catalog
    String systemId;

    /*
     A list of catalog entry files from the input, excluding the current catalog.
     URIs in the List are verified during input validation or property retrieval.
     */
    List<String> inputFiles;

    //A list of catalogs specified using the nextCatalog element
    List<NextCatalog> nextCatalogs;

    //reuse the parser
    SAXParser parser;

    /**
     * Construct a Catalog with specified URI.
     *
     * @param f the features object
     * @param uris the uri(s) to one or more catalogs
     * @throws CatalogException If an error happens while parsing the specified
     * catalog file.
     */
    public CatalogImpl(CatalogFeatures f, URI... uris) throws CatalogException {
        this(null, f, uris);
    }

    /**
     * Construct a Catalog with specified URI.
     *
     * @param parent The parent catalog
     * @param f the features object
     * @param uris the uri(s) to one or more catalogs
     * @throws CatalogException If an error happens while parsing the specified
     * catalog file.
     */
    public CatalogImpl(CatalogImpl parent, CatalogFeatures f, URI... uris) throws CatalogException {
        super(CatalogEntryType.CATALOG, parent);
        if (f == null) {
            throw new NullPointerException(
                    formatMessage(CatalogMessages.ERR_NULL_ARGUMENT, new Object[]{"CatalogFeatures"}));
        }

        init(f);

        //Path of catalog files
        String[] catalogFile = null;
        if (level == 0 && uris.length == 0) {
            String files = features.get(Feature.FILES);
            if (files != null) {
                catalogFile = files.split(";");
            }
        } else {
            catalogFile = new String[uris.length];
            for (int i=0; i<uris.length; i++) {
                catalogFile[i] = uris[i].toASCIIString();
            }
        }

        /*
         In accordance with 8. Resource Failures of the Catalog spec, missing
         Catalog entry files are to be ignored.
         */
        if ((catalogFile != null && catalogFile.length > 0)) {
            int start = 0;
            URI uri = null;
            for (String temp : catalogFile) {
                uri = URI.create(temp);
                start++;
                if (verifyCatalogFile(null, uri)) {
                    systemId = temp;
                    try {
                        baseURI = new URL(systemId);
                    } catch (MalformedURLException e) {
                        CatalogMessages.reportRunTimeError(CatalogMessages.ERR_INVALID_PATH,
                                new Object[]{temp}, e);
                    }
                    break;
                }
            }

            //Save the rest of input files as alternative catalogs
            if (level == 0 && catalogFile.length > start) {
                inputFiles = new ArrayList<>();
                for (int i = start; i < catalogFile.length; i++) {
                    if (catalogFile[i] != null) {
                        inputFiles.add(catalogFile[i]);
                    }
                }
            }
        }
    }

    /**
     * Loads the catalog
     */
    void load() {
        if (systemId != null) {
            parse(systemId);
        }

        setCatalog(this);

        //save this catalog before loading the next
        loadedCatalogs.put(systemId, this);

        //Load delegate and alternative catalogs if defer is false.
        if (!isDeferred()) {
           loadDelegateCatalogs(this);
           loadNextCatalogs();
        }
    }

    private void init(CatalogFeatures f) {
        if (parent == null) {
            level = 0;
        } else {
            level = parent.level + 1;
            this.loadedCatalogs = parent.loadedCatalogs;
            this.catalogsSearched = parent.catalogsSearched;
        }
        if (f == null) {
            this.features = CatalogFeatures.defaults();
        } else {
            this.features = f;
        }
        setPrefer(features.get(Feature.PREFER));
        setDeferred(features.get(Feature.DEFER));
        setResolve(features.get(Feature.RESOLVE));
    }

    /**
     * Resets the Catalog instance to its initial state.
     */
    @Override
    public void reset() {
        super.reset();
        current = 0;
        if (level == 0) {
            catalogsSearched.clear();
        }
        entries.stream().filter((entry) -> (entry.type == CatalogEntryType.GROUP)).forEach((entry) -> {
            ((GroupEntry) entry).reset();
        });
    }

    /**
     * Returns whether this Catalog instance is the top (main) Catalog.
     *
     * @return true if the instance is the top Catalog, false otherwise
     */
    boolean isTop() {
        return level == 0;
    }

    /**
     * Gets the parent of this catalog.
     *
     * @returns The parent catalog
     */
    public Catalog getParent() {
        return this.parent;
    }

    /**
     * Sets the defer property. If the value is null or empty, or any String
     * other than the defined, it will be assumed as the default value.
     *
     * @param value The value of the defer attribute
     */
    public final void setDeferred(String value) {
        isDeferred = DEFER_TRUE.equals(value);
    }

    /**
     * Queries the defer attribute
     *
     * @return true if the prefer attribute is set to system, false if not.
     */
    public boolean isDeferred() {
        return isDeferred;
    }

    /**
     * Sets the resolve property. If the value is null or empty, or any String
     * other than the defined, it will be assumed as the default value.
     *
     * @param value The value of the resolve attribute
     */
    public final void setResolve(String value) {
        resolveType = ResolveType.getType(value);
    }

    /**
     * Gets the value of the resolve attribute
     *
     * @return The value of the resolve attribute
     */
    public final ResolveType getResolve() {
        return resolveType;
    }

    /**
     * Marks the Catalog as being searched already.
     */
    void markAsSearched() {
        catalogsSearched.add(systemId);
    }

    /**
     * Parses the catalog.
     *
     * @param systemId The systemId of the catalog
     * @throws CatalogException if parsing the catalog failed
     */
    private void parse(String systemId) {
        if (parser == null) {
            parser = getParser();
        }

        try {
            CatalogReader reader = new CatalogReader(this, parser);
            parser.parse(systemId, reader);
        } catch (SAXException | IOException ex) {
            CatalogMessages.reportRunTimeError(CatalogMessages.ERR_PARSING_FAILED, ex);
        }
    }

    /**
     * Returns a SAXParser instance
     * @return a SAXParser instance
     * @throws CatalogException if constructing a SAXParser failed
     */
    private SAXParser getParser() {
        SAXParser p = null;
        try {
            SAXParserFactory spf = new SAXParserFactoryImpl();
            spf.setNamespaceAware(true);
            spf.setValidating(false);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            p = spf.newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            CatalogMessages.reportRunTimeError(CatalogMessages.ERR_PARSING_FAILED, e);
        }
        return p;
    }

    /**
     * Indicate that the catalog is empty
     *
     * @return True if the catalog is empty; False otherwise.
     */
    public boolean isEmpty() {
        return isEmpty;
    }

    @Override
    public Stream<Catalog> catalogs() {
        Iterator<Catalog> iter = new Iterator<Catalog>() {
            Catalog nextCatalog = null;

            //Current index of the input files
            int inputFilesIndex = 0;

            //Next catalog
            int nextCatalogIndex = 0;

            @Override
            public boolean hasNext() {
                if (nextCatalog != null) {
                    return true;
                } else {
                    nextCatalog = nextCatalog();
                    return (nextCatalog != null);
                }
            }

            @Override
            public Catalog next() {
                if (nextCatalog != null || hasNext()) {
                    Catalog catalog = nextCatalog;
                    nextCatalog = null;
                    return catalog;
                } else {
                    throw new NoSuchElementException();
                }
            }

            /**
             * Returns the next alternative catalog.
             *
             * @return the next catalog if any
             */
            private Catalog nextCatalog() {
                Catalog c = null;

                //Check those specified in nextCatalogs
                if (nextCatalogs != null) {
                    while (c == null && nextCatalogIndex < nextCatalogs.size()) {
                        c = getCatalog(catalog,
                                nextCatalogs.get(nextCatalogIndex++).getCatalogURI());
                    }
                }

                //Check the input list
                if (c == null && inputFiles != null) {
                    while (c == null && inputFilesIndex < inputFiles.size()) {
                        c = getCatalog(null,
                                URI.create(inputFiles.get(inputFilesIndex++)));
                    }
                }

                return c;
            }
        };

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                iter, Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    /**
     * Adds the catalog to the nextCatalog list
     *
     * @param catalog a catalog specified in a nextCatalog entry
     */
    void addNextCatalog(NextCatalog catalog) {
        if (catalog == null) {
            return;
        }

        if (nextCatalogs == null) {
            nextCatalogs = new ArrayList<>();
        }

        nextCatalogs.add(catalog);
    }

    /**
     * Loads all alternative catalogs.
     */
    void loadNextCatalogs() {
        //loads catalogs specified in nextCatalogs
        if (nextCatalogs != null) {
            nextCatalogs.stream().forEach((next) -> {
                getCatalog(this, next.getCatalogURI());
            });
        }

        //loads catalogs from the input list
        if (inputFiles != null) {
            inputFiles.stream().forEach((uri) -> {
                getCatalog(null, URI.create(uri));
            });
        }
    }

    /**
     * Returns a Catalog object by the specified path.
     *
     * @param parent the parent catalog for the alternative catalogs to be loaded.
     * It will be null if the ones to be loaded are from the input list.
     * @param uri the path to a catalog
     * @return a Catalog object
     */
    Catalog getCatalog(CatalogImpl parent, URI uri) {
        if (uri == null) {
            return null;
        }

        CatalogImpl c = null;

        if (verifyCatalogFile(parent, uri)) {
            c = getLoadedCatalog(uri.toASCIIString());
            if (c == null) {
                c = new CatalogImpl(this, features, uri);
                c.load();
            }
        }
        return c;
    }

    /**
     * Saves a loaded Catalog.
     *
     * @param catalogId the catalogId associated with the Catalog object
     * @param c the Catalog to be saved
     */
    void saveLoadedCatalog(String catalogId, CatalogImpl c) {
        loadedCatalogs.put(catalogId, c);
    }

    /**
     * Returns a count of all loaded catalogs, including delegate catalogs.
     *
     * @return a count of all loaded catalogs
     */
    int loadedCatalogCount() {
        return loadedCatalogs.size();
    }
}
