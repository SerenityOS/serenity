/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static catalog.CatalogTestUtils.CATALOG_SYSTEM;
import static catalog.CatalogTestUtils.CATALOG_URI;
import static catalog.CatalogTestUtils.catalogResolver;
import static catalog.CatalogTestUtils.catalogUriResolver;
import static catalog.ResolutionChecker.checkSysIdResolution;
import static catalog.ResolutionChecker.checkUriResolution;

import javax.xml.catalog.CatalogException;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.ValidateCatalogTest
 * @run testng/othervm catalog.ValidateCatalogTest
 * @summary A legal catalog file must be well-formed XML, the root element
 *          must be catalog, and the naming space of the root element must be
 *          urn:oasis:names:tc:entity:xmlns:xml:catalog.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class ValidateCatalogTest {

    private static final String CATALOG_WRONGROOT = "validateCatalog-wrongRoot.xml";
    private static final String CATALOG_MALFORMED = "validateCatalog-malformed.xml";

    /*
     * EntityResolver tries to load catalog with wrong root,
     * it should throw CatalogException.
     */
    @Test(expectedExceptions = CatalogException.class)
    public void validateWrongRootCatalogOnEntityResolver() {
        catalogResolver(CATALOG_WRONGROOT);
    }

    /*
     * URIResolver tries to load catalog with wrong root,
     * it should throw CatalogException.
     */
    @Test(expectedExceptions = CatalogException.class)
    public void validateWrongRootCatalogOnUriResolver() {
        catalogUriResolver(CATALOG_WRONGROOT);
    }

    /*
     * EntityResolver tries to load malformed catalog,
     * it should throw RuntimeException.
     */
    @Test(expectedExceptions = RuntimeException.class)
    public void validateMalformedCatalogOnEntityResolver() {
        catalogResolver(CATALOG_MALFORMED);
    }

    /*
     * UriResolver tries to load malformed catalog,
     * it should throw RuntimeException.
     */
    @Test(expectedExceptions = RuntimeException.class)
    public void validateMalformedCatalogOnUriResolver() {
        catalogUriResolver(CATALOG_MALFORMED);
    }

    /*
     * Resolver should ignore the catalog which doesn't declare the correct
     * naming space.
     */
    @Test
    public void validateWrongNamingSpaceCatalog() {
        String catalogName = "validateCatalog-noNamingSpace.xml";
        checkSysIdResolution(catalogResolver(catalogName, CATALOG_SYSTEM),
                "http://remote/dtd/alice/docAlice.dtd",
                "http://local/dtd/docAliceSys.dtd");
        checkUriResolution(catalogUriResolver(catalogName, CATALOG_URI),
                "http://remote/dtd/uri/alice/docAlice.dtd",
                "http://local/dtd/docAliceURI.dtd");
    }
}
