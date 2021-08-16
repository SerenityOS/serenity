/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a group entry.
 *
 * @since 9
 */
class GroupEntry extends BaseEntry {
    static final int ATTRIBUTE_PREFER = 0;
    static final int ATTRIBUTE_DEFFER = 1;
    static final int ATTRIBUTE_RESOLUTION = 2;

    //Unmodifiable features when the Catalog is created
    CatalogFeatures features;

    //Value of the prefer attribute
    boolean isPreferPublic = true;

    //The parent of the catalog instance
    CatalogImpl parent = null;

    //The catalog instance this group belongs to
    CatalogImpl catalog;

    //A list of all entries in a catalog or group
    List<BaseEntry> entries = new ArrayList<>();

    //loaded delegated catalog by system id
    Map<String, CatalogImpl> delegateCatalogs = new HashMap<>();

    //A list of all loaded Catalogs, including this, and next catalogs
    Map<String, CatalogImpl> loadedCatalogs = new HashMap<>();

    /*
     A list of Catalog Ids that have already been searched in a matching
     operation. Check this list before constructing new Catalog to avoid circular
     reference.
     */
    List<String> catalogsSearched = new ArrayList<>();

    //A flag to indicate whether the current match is a system or uri
    boolean isInstantMatch = false;

    //A match of a rewrite type
    String rewriteMatch = null;

    //The length of the longest match of a rewrite type
    int longestRewriteMatch = 0;

    //A match of a suffix type
    String suffixMatch = null;

    //The length of the longest match of a suffix type
    int longestSuffixMatch = 0;

    //Indicate whether a system entry has been searched
    boolean systemEntrySearched = false;

    /**
     * PreferType represents possible values of the prefer property
     */
    public static enum PreferType {
        PUBLIC("public"),
        SYSTEM("system");

        final String literal;

        PreferType(String literal) {
            this.literal = literal;
        }

        public boolean prefer(String prefer) {
            return literal.equals(prefer);
        }
    }

    /**
     * PreferType represents possible values of the resolve property
     */
    public static enum ResolveType {
        STRICT(CatalogFeatures.RESOLVE_STRICT),
        CONTINUE(CatalogFeatures.RESOLVE_CONTINUE),
        IGNORE(CatalogFeatures.RESOLVE_IGNORE);

        final String literal;

        ResolveType(String literal) {
            this.literal = literal;
        }

        static public ResolveType getType(String resolveType) {
            for (ResolveType type : ResolveType.values()) {
                if (type.isType(resolveType)) {
                    return type;
                }
            }
            return null;
        }

        public boolean isType(String type) {
            return literal.equals(type);
        }
    }

    /**
     * Constructs a GroupEntry
     *
     * @param type the type of the entry
     * @param parent the parent Catalog
     */
    public GroupEntry(CatalogEntryType type, CatalogImpl parent) {
        super(type);
        this.parent = parent;
    }

    /**
     * Constructs a group entry.
     *
     * @param base The baseURI attribute
     * @param attributes The attributes
     */
    public GroupEntry(String base, String... attributes) {
        this(null, base, attributes);
    }

    /**
     * Resets the group entry to its initial state.
     */
    public void reset() {
        isInstantMatch = false;
        rewriteMatch = null;
        longestRewriteMatch = 0;
        suffixMatch = null;
        longestSuffixMatch = 0;
        systemEntrySearched = false;
    }
    /**
     * Constructs a group entry.
     * @param catalog the catalog this GroupEntry belongs to
     * @param base the baseURI attribute
     * @param attributes the attributes
     */
    public GroupEntry(CatalogImpl catalog, String base, String... attributes) {
        super(CatalogEntryType.GROUP, base);
        setPrefer(attributes[ATTRIBUTE_PREFER]);
        this.catalog = catalog;
    }

    /**
     * Sets the catalog for this GroupEntry.
     *
     * @param catalog the catalog this GroupEntry belongs to
     */
    void setCatalog(CatalogImpl catalog) {
        this.catalog = catalog;
    }

    /**
     * Adds an entry.
     *
     * @param entry The entry to be added.
     */
    public void addEntry(BaseEntry entry) {
        entries.add(entry);
    }

    /**
     * Sets the prefer property. If the value is null or empty, or any String
     * other than the defined, it will be assumed as the default value.
     *
     * @param value The value of the prefer attribute
     */
    public final void setPrefer(String value) {
        isPreferPublic = PreferType.PUBLIC.prefer(value);
    }

    /**
     * Queries the prefer attribute
     *
     * @return true if the prefer attribute is set to system, false if not.
     */
    public boolean isPreferPublic() {
        return isPreferPublic;
    }

    /**
     * Attempt to find a matching entry in the catalog by systemId.
     *
     * <p>
     * The method searches through the system-type entries, including system,
     * rewriteSystem, systemSuffix, delegateSystem, and group entries in the
     * current catalog in order to find a match.
     *
     *
     * @param systemId The system identifier of the external entity being
     * referenced.
     *
     * @return a URI string if a mapping is found, or null otherwise.
     */
    public String matchSystem(String systemId) {
        systemEntrySearched = true;
        String match = null;
        for (BaseEntry entry : entries) {
            switch (entry.type) {
                case SYSTEM:
                    match = ((SystemEntry) entry).match(systemId);
                    //if there's a matching system entry, use it
                    if (match != null) {
                        isInstantMatch = true;
                        return match;
                    }
                    break;
                case REWRITESYSTEM:
                    match = ((RewriteSystem) entry).match(systemId, longestRewriteMatch);
                    if (match != null) {
                        rewriteMatch = match;
                        longestRewriteMatch = ((RewriteSystem) entry).getSystemIdStartString().length();
                    }
                    break;
                case SYSTEMSUFFIX:
                    match = ((SystemSuffix) entry).match(systemId, longestSuffixMatch);
                    if (match != null) {
                        suffixMatch = match;
                        longestSuffixMatch = ((SystemSuffix) entry).getSystemIdSuffix().length();
                    }
                    break;
                case GROUP:
                    GroupEntry grpEntry = (GroupEntry) entry;
                    match = grpEntry.matchSystem(systemId);
                    if (grpEntry.isInstantMatch) {
                        //use it if there is a match of the system type
                        return match;
                    } else if (grpEntry.longestRewriteMatch > longestRewriteMatch) {
                        longestRewriteMatch = grpEntry.longestRewriteMatch;
                        rewriteMatch = match;
                    } else if (grpEntry.longestSuffixMatch > longestSuffixMatch) {
                        longestSuffixMatch = grpEntry.longestSuffixMatch;
                        suffixMatch = match;
                    }
                    break;
            }
        }

        if (longestRewriteMatch > 0) {
            return rewriteMatch;
        } else if (longestSuffixMatch > 0) {
            return suffixMatch;
        }

        //if no single match is found, try delegates
        return matchDelegate(CatalogEntryType.DELEGATESYSTEM, systemId);
    }

    /**
     * Attempt to find a matching entry in the catalog by publicId.
     *
     * <p>
     * The method searches through the public-type entries, including public,
     * delegatePublic, and group entries in the current catalog in order to find
     * a match.
     *
     *
     * @param publicId The public identifier of the external entity being
     * referenced.
     *
     * @return a URI string if a mapping is found, or null otherwise.
     */
    public String matchPublic(String publicId) {
        /*
           When both public and system identifiers are specified, and prefer is
        not public (that is, system), only system entry will be used.
        */
        if (!isPreferPublic && systemEntrySearched) {
            return null;
        }
        //match public entries
        String match = null;
        for (BaseEntry entry : entries) {
            switch (entry.type) {
                case PUBLIC:
                    match = ((PublicEntry) entry).match(publicId);
                    break;
                case URI:
                    match = ((UriEntry) entry).match(publicId);
                    break;
                case GROUP:
                    match = ((GroupEntry) entry).matchPublic(publicId);
                    break;
            }
            if (match != null) {
                return match;
            }
        }

        //if no single match is found, try delegates
        return matchDelegate(CatalogEntryType.DELEGATEPUBLIC, publicId);
    }

    /**
     * Attempt to find a matching entry in the catalog by the uri element.
     *
     * <p>
     * The method searches through the uri-type entries, including uri,
     * rewriteURI, uriSuffix, delegateURI and group entries in the current
     * catalog in order to find a match.
     *
     *
     * @param uri The URI reference of a resource.
     *
     * @return a URI string if a mapping is found, or null otherwise.
     */
    public String matchURI(String uri) {
        String match = null;
        for (BaseEntry entry : entries) {
            switch (entry.type) {
                case URI:
                    match = ((UriEntry) entry).match(uri);
                    if (match != null) {
                        isInstantMatch = true;
                        return match;
                    }
                    break;
                case REWRITEURI:
                    match = ((RewriteUri) entry).match(uri, longestRewriteMatch);
                    if (match != null) {
                        rewriteMatch = match;
                        longestRewriteMatch = ((RewriteUri) entry).getURIStartString().length();
                    }
                    break;
                case URISUFFIX:
                    match = ((UriSuffix) entry).match(uri, longestSuffixMatch);
                    if (match != null) {
                        suffixMatch = match;
                        longestSuffixMatch = ((UriSuffix) entry).getURISuffix().length();
                    }
                    break;
                case GROUP:
                    GroupEntry grpEntry = (GroupEntry) entry;
                    match = grpEntry.matchURI(uri);
                    if (grpEntry.isInstantMatch) {
                        //use it if there is a match of the uri type
                        return match;
                    } else if (grpEntry.longestRewriteMatch > longestRewriteMatch) {
                        rewriteMatch = match;
                        longestRewriteMatch = grpEntry.longestRewriteMatch;
                    } else if (grpEntry.longestSuffixMatch > longestSuffixMatch) {
                        suffixMatch = match;
                        longestSuffixMatch = grpEntry.longestSuffixMatch;
                    }
                    break;
            }
        }

        if (longestRewriteMatch > 0) {
            return rewriteMatch;
        } else if (longestSuffixMatch > 0) {
            return suffixMatch;
        }

        //if no single match is found, try delegates
        return matchDelegate(CatalogEntryType.DELEGATEURI, uri);
    }

    /**
     * Matches delegatePublic or delegateSystem against the specified id
     *
     * @param type the type of the Catalog entry
     * @param id the system or public id to be matched
     * @return the URI string if a mapping is found, or null otherwise.
     */
    private String matchDelegate(CatalogEntryType type, String id) {
        String match = null;
        int longestMatch = 0;
        URI catalogId = null;
        URI temp;

        //Check delegate types in the current catalog
        for (BaseEntry entry : entries) {
            if (entry.type == type) {
                if (type == CatalogEntryType.DELEGATESYSTEM) {
                    temp = ((DelegateSystem)entry).matchURI(id, longestMatch);
                } else if (type == CatalogEntryType.DELEGATEPUBLIC) {
                    temp = ((DelegatePublic)entry).matchURI(id, longestMatch);
                } else {
                    temp = ((DelegateUri)entry).matchURI(id, longestMatch);
                }
                if (temp != null) {
                    longestMatch = entry.getMatchId().length();
                    catalogId = temp;
                }
            }
        }

        //Check delegate Catalogs
        if (catalogId != null) {
            Catalog delegateCatalog = loadDelegateCatalog(catalog, catalogId);

            if (delegateCatalog != null) {
                if (type == CatalogEntryType.DELEGATESYSTEM) {
                    match = delegateCatalog.matchSystem(id);
                } else if (type == CatalogEntryType.DELEGATEPUBLIC) {
                    match = delegateCatalog.matchPublic(id);
                } else {
                    match = delegateCatalog.matchURI(id);
                }
            }
        }

        return match;
    }

    /**
     * Loads all delegate catalogs.
     *
     * @param parent the parent catalog of the delegate catalogs
     */
    void loadDelegateCatalogs(CatalogImpl parent) {
        entries.stream()
                .filter((entry) -> (entry.type == CatalogEntryType.DELEGATESYSTEM ||
                        entry.type == CatalogEntryType.DELEGATEPUBLIC ||
                        entry.type == CatalogEntryType.DELEGATEURI))
                .map((entry) -> (AltCatalog)entry)
                .forEach((altCatalog) -> {
                        loadDelegateCatalog(parent, altCatalog.getCatalogURI());
        });
    }

    /**
     * Loads a delegate catalog by the catalogId specified.
     *
     * @param parent the parent catalog of the delegate catalog
     * @param catalogURI the URI to the catalog
     */
    Catalog loadDelegateCatalog(CatalogImpl parent, URI catalogURI) {
        CatalogImpl delegateCatalog = null;
        if (catalogURI != null) {
            String catalogId = catalogURI.toASCIIString();
            if (verifyCatalogFile(parent, catalogURI)) {
                delegateCatalog = getLoadedCatalog(catalogId);
                if (delegateCatalog == null) {
                    delegateCatalog = new CatalogImpl(parent, features, catalogURI);
                    delegateCatalog.load();
                    delegateCatalogs.put(catalogId, delegateCatalog);
                }
            }
        }

        return delegateCatalog;
    }

    /**
     * Returns a previously loaded Catalog object if found.
     *
     * @param catalogId The systemId of a catalog
     * @return a Catalog object previously loaded, or null if none in the saved
     * list
     */
    CatalogImpl getLoadedCatalog(String catalogId) {
        CatalogImpl c = null;

        //check delegate Catalogs
        c = delegateCatalogs.get(catalogId);
        if (c == null) {
            //check other loaded Catalogs
            c = loadedCatalogs.get(catalogId);
        }

        return c;
    }


    /**
     * Verifies that the catalog file represented by the catalogId exists. If it
     * doesn't, returns false to ignore it as specified in the Catalog
     * specification, section 8. Resource Failures.
     * <p>
     * Verifies that the catalog represented by the catalogId has not been
     * searched or is not circularly referenced.
     *
     * @param parent the parent of the catalog to be loaded
     * @param catalogURI the URI to the catalog
     * @throws CatalogException if circular reference is found.
     * @return true if the catalogId passed verification, false otherwise
     */
    final boolean verifyCatalogFile(CatalogImpl parent, URI catalogURI) {
        if (catalogURI == null) {
            return false;
        }

        //Ignore it if it doesn't exist
        if (Util.isFileUri(catalogURI) &&
                !Util.isFileUriExist(catalogURI, false)) {
            return false;
        }

        String catalogId = catalogURI.toASCIIString();
        if (catalogsSearched.contains(catalogId) || isCircular(parent, catalogId)) {
            CatalogMessages.reportRunTimeError(CatalogMessages.ERR_CIRCULAR_REFERENCE,
                    new Object[]{CatalogMessages.sanitize(catalogId)});
        }

        return true;
    }

    /**
     * Checks whether the catalog is circularly referenced
     *
     * @param parent the parent of the catalog to be loaded
     * @param systemId the system identifier of the catalog to be loaded
     * @return true if is circular, false otherwise
     */
    boolean isCircular(CatalogImpl parent, String systemId) {
        // first, check the parent of the catalog to be loaded
        if (parent == null) {
            return false;
        }

        if (parent.systemId.equals(systemId)) {
            return true;
        }

       // next, check parent's parent
        return parent.isCircular(parent.parent, systemId);
    }
}
