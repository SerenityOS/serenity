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

import static catalog.CatalogTestUtils.CATALOG_PUBLIC;
import static catalog.CatalogTestUtils.CATALOG_SYSTEM;
import static catalog.CatalogTestUtils.CATALOG_URI;
import static catalog.CatalogTestUtils.catalogResolver;
import static catalog.CatalogTestUtils.catalogUriResolver;
import static catalog.ResolutionChecker.checkSysIdResolution;
import static catalog.ResolutionChecker.checkUriResolution;

import javax.xml.catalog.CatalogException;
import javax.xml.catalog.CatalogResolver;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.LoadCatalogTest
 * @run testng/othervm catalog.LoadCatalogTest
 * @summary When catalog resolver loads catalog files, the current catalog file
 *          and the catalog files specified by the nextCatalog entries may not
 *          accessible. This case tests how does the resolver handle this issue.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class LoadCatalogTest {

    private static final String CATALOG_LOADCATALOGFILES = "loadCatalogFiles.xml";
    private static final String CATALOG_DUMMY = "dummy.xml";

    private static final String ID_ALICE = "http://remote/dtd/alice/docAlice.dtd";
    private static final String ID_ALICE_URI = "http://remote/dtd/uri/alice/docAlice.dtd";
    private static final String ID_DUMMY = "http://remote/dtd/doc.dtd";

    @Test(dataProvider = "entityResolver")
    public void testMatchOnEntityResolver(CatalogResolver resolver) {
        checkSysIdResolution(resolver, ID_ALICE,
                "http://local/dtd/docAliceSys.dtd");
    }

    @DataProvider(name = "entityResolver")
    public Object[][] entityResolver() {
        return new Object[][] {
                // This EntityResolver loads multiple catalog files one by one.
                // All of the files are available.
                { catalogResolver(CATALOG_PUBLIC, CATALOG_URI,
                        CATALOG_SYSTEM) },

                // This EntityResolver loads multiple catalog files one by one,
                // but the middle one isn't existing.
                { catalogResolver(CATALOG_PUBLIC, CATALOG_DUMMY,
                        CATALOG_SYSTEM) } };
    }

    @Test(dataProvider = "uriResolver")
    public void testMatchOnUriResolver(CatalogResolver resolver) {
        checkUriResolution(resolver, ID_ALICE_URI,
                "http://local/dtd/docAliceURI.dtd");
    }

    @DataProvider(name = "uriResolver")
    public Object[][] uriResolver() {
        return new Object[][] {
                // This URIResolver loads multiple catalog files one by one.
                // All of the files are available.
                { catalogUriResolver(CATALOG_PUBLIC, CATALOG_SYSTEM,
                        CATALOG_URI) },

                // This URIResolver loads multiple catalog files one by one,
                // but the middle one isn't existing.
                { catalogUriResolver(CATALOG_PUBLIC, CATALOG_DUMMY,
                        CATALOG_URI) } };
    }

    @Test(dataProvider = "catalogName",
            expectedExceptions = CatalogException.class)
    public void testExceptionOnEntityResolver(String[] catalogName) {
        catalogResolver(catalogName).resolveEntity(null, ID_DUMMY);
    }

    @Test(dataProvider = "catalogName",
            expectedExceptions = CatalogException.class)
    public void testExceptionOnUriResolver(String[] catalogName) {
        catalogUriResolver(catalogName).resolve(ID_DUMMY, null);
    }

    @DataProvider(name = "catalogName")
    public Object[][] catalogName() {
        return new Object[][] {
                // This catalog file set includes null catalog files.
                { (String[]) null },

                // This catalog file set includes one catalog file, but this
                // catalog defines a non-existing next catalog.
                { new String[] { CATALOG_LOADCATALOGFILES } } };
    }
}
