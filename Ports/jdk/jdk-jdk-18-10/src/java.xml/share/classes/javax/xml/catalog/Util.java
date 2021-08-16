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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import static javax.xml.catalog.CatalogFeatures.DEFER_FALSE;
import static javax.xml.catalog.CatalogFeatures.DEFER_TRUE;
import javax.xml.catalog.CatalogFeatures.Feature;
import static javax.xml.catalog.CatalogFeatures.PREFER_PUBLIC;
import static javax.xml.catalog.CatalogFeatures.PREFER_SYSTEM;
import static javax.xml.catalog.CatalogFeatures.RESOLVE_CONTINUE;
import static javax.xml.catalog.CatalogFeatures.RESOLVE_IGNORE;
import static javax.xml.catalog.CatalogFeatures.RESOLVE_STRICT;
import jdk.xml.internal.SecuritySupport;

/**
 *
 * @since 9
 */
class Util {

    final static String URN = "urn:publicid:";
    final static String PUBLICID_PREFIX = "-//";
    final static String PUBLICID_PREFIX_ALT = "+//";
    final static String SCHEME_FILE = "file";
    final static String SCHEME_JAR = "jar";
    final static String SCHEME_JARFILE = "jar:file:";

    /**
     * Finds an entry in the catalog that matches with the publicId or systemId.
     *
     * The resolution follows the following rules determined by the prefer
     * setting:
     *
     * prefer "system": attempts to resolve with a system entry; attempts to
     * resolve with a public entry when only publicId is specified.
     *
     * prefer "public": attempts to resolve with a system entry; attempts to
     * resolve with a public entry if no matching system entry is found.
     *
     * If no match is found, continue searching uri entries
     *
     * @param catalog the catalog
     * @param publicId the publicId
     * @param systemId the systemId
     * @return the resolved systemId if a match is found, null otherwise
     */
    static String resolve(CatalogImpl catalog, String publicId, String systemId) {
        String resolvedSystemId = null;

        //search the current catalog
        catalog.reset();
        if (systemId != null) {
            /*
             If a system identifier is specified, it is used no matter how
             prefer is set.
             */
            resolvedSystemId = catalog.matchSystem(systemId);
        }

        if (resolvedSystemId == null && publicId != null) {
            resolvedSystemId = catalog.matchPublic(publicId);
        }

        if (resolvedSystemId == null && systemId != null) {
            resolvedSystemId = catalog.matchURI(systemId);
        }

        //mark the catalog as having been searched before trying alternatives
        catalog.markAsSearched();

        //search alternative catalogs
        if (resolvedSystemId == null) {
            Iterator<Catalog> iter = catalog.catalogs().iterator();
            while (iter.hasNext()) {
                resolvedSystemId = resolve((CatalogImpl) iter.next(), publicId, systemId);
                if (resolvedSystemId != null) {
                    break;
                }

            }
        }

        return resolvedSystemId;
    }

    static void validateUrisSyntax(URI... uris) {
        for (URI uri : uris) {
            validateUriSyntax(uri);
        }
    }

    static void validateUrisSyntax(String... uris) {
        for (String uri : uris) {
            validateUriSyntax(URI.create(uri));
        }
    }

    /**
     * Validate that the URI must be absolute and a valid URL.
     *
     * Note that this method does not verify the existence of the resource. The
     * Catalog standard requires that such resources be ignored.
     *
     * @param uri
     * @throws IllegalArgumentException if the uri is not absolute and a valid
     * URL
     */
    static void validateUriSyntax(URI uri) {
        CatalogMessages.reportNPEOnNull("URI input", uri);

        if (!uri.isAbsolute()) {
            CatalogMessages.reportIAE(CatalogMessages.ERR_URI_NOTABSOLUTE,
                    new Object[]{uri}, null);
        }

        try {
            // check if the scheme was valid
            uri.toURL();
        } catch (MalformedURLException ex) {
            CatalogMessages.reportIAE(CatalogMessages.ERR_URI_NOTVALIDURL,
                    new Object[]{uri}, null);
        }
    }

    /**
     * Checks whether the URI is a file URI, including JAR file.
     *
     * @param uri the specified URI.
     * @return true if it is a file or JAR file URI, false otherwise
     */
    static boolean isFileUri(URI uri) {
        if (SCHEME_FILE.equals(uri.getScheme())
                || SCHEME_JAR.equals(uri.getScheme())) {
            return true;
        }
        return false;
    }

    /**
     * Verifies whether the file resource exists.
     *
     * @param uri the URI to locate the resource
     * @param openJarFile a flag to indicate whether a JAR file should be
     * opened. This operation may be expensive.
     * @return true if the resource exists, false otherwise.
     */
    static boolean isFileUriExist(URI uri, boolean openJarFile) {
        if (uri != null && uri.isAbsolute()) {
            if (null != uri.getScheme()) {
                switch (uri.getScheme()) {
                    case SCHEME_FILE:
                        String path = uri.getPath();
                        File f1 = new File(path);
                        if (f1.isFile()) {
                            return true;
                        }
                        break;
                    case SCHEME_JAR:
                        String tempUri = uri.toString();
                        int pos = tempUri.indexOf("!");
                        if (pos < 0) {
                            return false;
                        }
                        if (openJarFile) {
                            String jarFile = tempUri.substring(SCHEME_JARFILE.length(), pos);
                            String entryName = tempUri.substring(pos + 2);
                            try {
                                JarFile jf = new JarFile(jarFile);
                                JarEntry je = jf.getJarEntry(entryName);
                                if (je != null) {
                                    return true;
                                }
                            } catch (IOException ex) {
                                return false;
                            }
                        } else {
                            return true;
                        }
                        break;
                }
            }
        }
        return false;
    }

    /**
     * Find catalog file paths by reading the system property, and then
     * jaxp.properties if the system property is not specified.
     *
     * @param sysPropertyName the name of system property
     * @return the catalog file paths, or null if not found.
     */
    static String[] getCatalogFiles(String sysPropertyName) {
        String value = SecuritySupport.getJAXPSystemProperty(sysPropertyName);
        if (value != null && !value.isEmpty()) {
            return value.split(";");
        }
        return null;
    }

    /**
     * Checks whether the specified string is null or empty, returns the
     * original string with leading and trailing spaces removed if not.
     *
     * @param test the string to be tested
     * @return the original string with leading and trailing spaces removed, or
     * null if it is null or empty
     *
     */
    static String getNotNullOrEmpty(String test) {
        if (test == null) {
            return test;
        } else {
            String temp = test.trim();
            if (temp.length() == 0) {
                return null;
            } else {
                return temp;
            }
        }
    }

    /**
     * Validates the input for features.
     *
     * @param f the feature
     * @param value the value
     * @throws IllegalArgumentException if the value is invalid for the feature
     */
    static void validateFeatureInput(Feature f, String value) {
        CatalogMessages.reportNPEOnNull(f.name(), value);
        if (value.length() == 0) {
            CatalogMessages.reportIAE(CatalogMessages.ERR_INVALID_ARGUMENT,
                    new Object[]{value, f.name()}, null);
        }

        if (f == Feature.PREFER) {
            if (!value.equals(PREFER_SYSTEM) && !value.equals(PREFER_PUBLIC)) {
                CatalogMessages.reportIAE(CatalogMessages.ERR_INVALID_ARGUMENT,
                        new Object[]{value, Feature.PREFER.name()}, null);
            }
        } else if (f == Feature.DEFER) {
            if (!value.equals(DEFER_TRUE) && !value.equals(DEFER_FALSE)) {
                CatalogMessages.reportIAE(CatalogMessages.ERR_INVALID_ARGUMENT,
                        new Object[]{value, Feature.DEFER.name()}, null);
            }
        } else if (f == Feature.RESOLVE) {
            if (!value.equals(RESOLVE_STRICT) && !value.equals(RESOLVE_CONTINUE)
                    && !value.equals(RESOLVE_IGNORE)) {
                CatalogMessages.reportIAE(CatalogMessages.ERR_INVALID_ARGUMENT,
                        new Object[]{value, Feature.RESOLVE.name()}, null);
            }
        } else if (f == Feature.FILES) {
            Util.validateUrisSyntax(value.split(";"));
        }
    }
}
