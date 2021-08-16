/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package catalog;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.catalog.CatalogResolver;
import jaxp.library.JAXPTestUtilities;

/*
 * Utilities for testing XML Catalog API.
 */
final class CatalogTestUtils {

    /* catalog files */
    static final String CATALOG_PUBLIC = "public.xml";
    static final String CATALOG_SYSTEM = "system.xml";
    static final String CATALOG_URI = "uri.xml";

    /* features */
    static final String FEATURE_FILES = "javax.xml.catalog.files";
    static final String FEATURE_PREFER = "javax.xml.catalog.prefer";
    static final String FEATURE_DEFER = "javax.xml.catalog.defer";
    static final String FEATURE_RESOLVE = "javax.xml.catalog.resolve";

    /* values of prefer feature */
    static final String PREFER_SYSTEM = "system";
    static final String PREFER_PUBLIC = "public";

    /* values of defer feature */
    static final String DEFER_TRUE = "true";
    static final String DEFER_FALSE = "false";

    /* values of resolve feature */
    static final String RESOLVE_STRICT = "strict";
    static final String RESOLVE_CONTINUE = "continue";
    static final String RESOLVE_IGNORE = "ignore";

    private static final String JAXP_PROPS = "jaxp.properties";
    private static final String JAXP_PROPS_BAK = JAXP_PROPS + ".bak";

    private CatalogTestUtils() { }

    /* ********** create resolver ********** */

    /*
     * Creates CatalogResolver with a set of catalogs.
     */
    static CatalogResolver catalogResolver(String... catalogName) {
        return catalogResolver(CatalogFeatures.defaults(), catalogName);
    }

    /*
     * Creates CatalogResolver with a feature and a set of catalogs.
     */
    static CatalogResolver catalogResolver(CatalogFeatures features,
            String... catalogName) {
        return (catalogName == null) ?
                CatalogManager.catalogResolver(features) :
                CatalogManager.catalogResolver(features, getCatalogPaths(catalogName));
    }

    /*
     * Creates catalogUriResolver with a set of catalogs.
     */
    static CatalogResolver catalogUriResolver(String... catalogName) {
        return catalogUriResolver(CatalogFeatures.defaults(), catalogName);
    }

    /*
     * Creates catalogUriResolver with a feature and a set of catalogs.
     */
    static CatalogResolver catalogUriResolver(
            CatalogFeatures features, String... catalogName) {
        return (catalogName == null) ?
                CatalogManager.catalogResolver(features) :
                CatalogManager.catalogResolver(features, getCatalogPaths(catalogName));
    }

    // Gets the paths of the specified catalogs.
    private static URI[] getCatalogPaths(String... catalogNames) {
        return catalogNames == null
                ? null
                : Stream.of(catalogNames).map(
                        catalogName -> getCatalogPath(catalogName)).collect(
                                Collectors.toList()).toArray(new URI[0]);
    }

    // Gets the paths of the specified catalogs.
    static URI getCatalogPath(String catalogName) {
        return catalogName == null
                ? null
                : Paths.get(JAXPTestUtilities.getPathByClassName(CatalogTestUtils.class, "catalogFiles")
                        + catalogName).toUri();
    }

    /* ********** jaxp.properties ********** */

    /*
     * Generates jaxp.properties with the specified content,
     * takes a backup if possible.
     */
    static void generateJAXPProps(String content) throws IOException {
        Path filePath = getJAXPPropsPath();
        Path bakPath = filePath.resolveSibling(JAXP_PROPS_BAK);
        System.out.println("Creating new file " + filePath +
            ", saving old version to " + bakPath + ".");
        if (Files.exists(filePath) && !Files.exists(bakPath)) {
            Files.move(filePath, bakPath);
        }

        Files.write(filePath, content.getBytes());
    }

    /*
     * Deletes jaxp.properties, restoring backup if possible.
     */
    static void deleteJAXPProps() throws IOException {
        Path filePath = getJAXPPropsPath();
        Path bakPath = filePath.resolveSibling(JAXP_PROPS_BAK);
        System.out.println("Removing file " + filePath +
                ", restoring old version from " + bakPath + ".");
        Files.delete(filePath);
        if (Files.exists(bakPath)) {
            Files.move(bakPath, filePath);
        }
    }

    /*
     * Gets the path of jaxp.properties.
     */
    private static Path getJAXPPropsPath() {
        return Paths.get(System.getProperty("java.home") + File.separator
                + "conf" + File.separator + JAXP_PROPS);
    }

    /*
     * Creates the content of properties file with the specified
     * property-value pairs.
     */
    static String createPropsContent(Map<String, String> props) {
        return props.entrySet().stream().map(
                entry -> String.format("%s=%s%n", entry.getKey(),
                        entry.getValue())).reduce(
                                (line1, line2) -> line1 + line2).get();
    }
}
