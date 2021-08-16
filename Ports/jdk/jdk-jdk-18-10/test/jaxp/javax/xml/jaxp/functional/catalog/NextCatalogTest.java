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

import static catalog.CatalogTestUtils.catalogResolver;
import static catalog.CatalogTestUtils.catalogUriResolver;
import static catalog.ResolutionChecker.checkPubIdResolution;
import static catalog.ResolutionChecker.checkSysIdResolution;
import static catalog.ResolutionChecker.checkUriResolution;

import javax.xml.catalog.CatalogResolver;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.NextCatalogTest
 * @run testng/othervm catalog.NextCatalogTest
 * @summary Get matched URIs from system, public and uri entries respectively,
 *          but some of the entries are defined in none-current catalog files.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class NextCatalogTest {

    private static final String CATALOG_NEXTCATALOGLEFT
            = "nextCatalog-left.xml";
    private static final String CATALOG_NEXTCATALOGRIGHT
            = "nextCatalog-right.xml";

    @Test(dataProvider = "systemId-matchedUri")
    public void testNextCatalogOnSysId(String sytemId, String matchedUri) {
        checkSysIdResolution(createEntityResolver(), sytemId, matchedUri);
    }

    @DataProvider(name = "systemId-matchedUri")
    public Object[][] dataOnSysId() {
        return new Object[][] {
                // This matched URI of the specified system id is defined in a
                // next catalog file.
                { "http://remote/dtd/sys/docAlice.dtd",
                        "http://local/base/dtd/docAliceNextLeftSys.dtd" },

                // There are two matches of the specified system id. One is in
                // the current catalog file, and the other is in a next catalog
                // file. But finally, the returned matched URI is the one in the
                // current catalog file.
                { "http://remote/dtd/sys/docBob.dtd",
                        "http://local/base/dtd/docBobLeftSys.dtd" },

                // The matched URI of the specified system id is defined in a
                // two-level next catalog file.
                { "http://remote/dtd/sys/docCarl.dtd",
                        "http://local/base/dtd/docCarlSys.dtd" },

                // Multiple catalog files, which are defined as next catalog,
                // have the matched system entries of the specified system id.
                // But finally, the returned matched URI is the first found.
                { "http://remote/dtd/sys/docDuplicate.dtd",
                        "http://local/base/dtd/docDuplicateLeftSys.dtd" } };
    }

    @Test(dataProvider = "publicId-matchedUri")
    public void testNextCatalogOnPubId(String publicId, String matchedUri) {
        checkPubIdResolution(createEntityResolver(), publicId, matchedUri);
    }

    @DataProvider(name = "publicId-matchedUri")
    public Object[][] dataOnPubId() {
        return new Object[][] {
                // This matched URI of the specified public id is defined in a
                // next catalog file.
                { "-//REMOTE//DTD ALICE DOCALICE XML//EN",
                        "http://local/base/dtd/docAliceNextLeftPub.dtd" },

                // There are two matches of the specified public id. One is in
                // the current catalog file, and the other is in a next catalog
                // file. But finally, the returned matched URI is the one in the
                // current catalog file.
                { "-//REMOTE//DTD BOB DOCBOB XML//EN",
                        "http://local/base/dtd/docBobLeftPub.dtd" },

                // The matched URI of the specified public id is defined in a
                // two-level next catalog file.
                { "-//REMOTE//DTD CARL DOCCARL XML//EN",
                        "http://local/base/dtd/docCarlPub.dtd" },

                // Multiple catalog files, which are defined as next catalog,
                // have the matched public entries of the specified public id.
                // But finally, the returned matched URI is the first found.
                { "-//REMOTE//DTD DUPLICATE DOCDUPLICATE XML//EN",
                        "http://local/base/dtd/docDuplicateLeftPub.dtd" } };
    }

    @Test(dataProvider = "uri-matchedUri")
    public void testNextCatalogOnUri(String uri, String matchedUri) {
        checkUriResolution(createUriResolver(), uri, matchedUri);
    }

    @DataProvider(name = "uri-matchedUri")
    public Object[][] dataOnUri() {
        return new Object[][] {
                // This matched URI of the specified URI reference is defined in
                // a next catalog file.
                { "http://remote/dtd/uri/docAlice.dtd",
                        "http://local/base/dtd/docAliceNextLeftURI.dtd" },

                // There are two matches of the specified URI reference. One is
                // in the current catalog file, and the other is in a next
                // catalog file. But finally, the returned matched URI is the
                // one in the current catalog file.
                { "http://remote/dtd/uri/docBob.dtd",
                        "http://local/base/dtd/docBobLeftURI.dtd" },

                // The matched URI of the specified URI reference is defined in
                // a two-level next catalog file.
                { "http://remote/dtd/uri/docCarl.dtd",
                        "http://local/base/dtd/docCarlURI.dtd" },

                // Multiple catalog files, which are defined as next catalog,
                // have the matched uri entries of the specified URI reference.
                // But finally, the returned matched URI is the first found.
                { "http://remote/dtd/uri/docDuplicate.dtd",
                        "http://local/base/dtd/docDuplicateLeftURI.dtd" } };
    }

    private CatalogResolver createEntityResolver() {
        return catalogResolver(CATALOG_NEXTCATALOGLEFT,
                CATALOG_NEXTCATALOGRIGHT);
    }

    private CatalogResolver createUriResolver() {
        return catalogUriResolver(CATALOG_NEXTCATALOGLEFT,
                CATALOG_NEXTCATALOGRIGHT);
    }
}
