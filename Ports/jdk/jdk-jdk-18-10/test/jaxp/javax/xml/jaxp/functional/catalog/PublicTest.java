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
import static catalog.CatalogTestUtils.catalogResolver;
import static catalog.ResolutionChecker.checkNoMatch;
import static catalog.ResolutionChecker.checkPubIdResolution;

import javax.xml.catalog.CatalogException;
import javax.xml.catalog.CatalogResolver;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.PublicTest
 * @run testng/othervm catalog.PublicTest
 * @summary Get matched URIs from public entries.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class PublicTest {

    @Test(dataProvider = "publicId-matchedUri")
    public void testPublic(String publicId, String matchedUri) {
        checkPubIdResolution(createResolver(), publicId, matchedUri);
    }

    @DataProvider(name = "publicId-matchedUri")
    public Object[][] data() {
        return new Object[][] {
                // The matched URI of the specified public id is defined in a
                // public entry. The match is an absolute path.
                { "-//REMOTE//DTD ALICE DOCALICE XML//EN",
                        "http://local/dtd/docAlicePub.dtd" },

                // The matched URI of the specified public id is defined in a
                // public entry. But the match isn't an absolute path, so the
                // returned URI should include the base, which is defined by the
                // catalog file, as the prefix.
                { "-//REMOTE//DTD BOB DOCBOB XML//EN",
                        "http://local/base/dtd/docBobPub.dtd" },

                // The matched URI of the specified public id is defined in a
                // public entry. The match isn't an absolute path, and the
                // public entry defines alternative base. So the returned URI
                // should include the alternative base.
                { "-//REMOTE//DTD CARL DOCCARL XML//EN",
                        "http://local/carlBase/dtd/docCarlPub.dtd" },

                // The catalog file defines two public entries, and both of them
                // match the specified public id. But the first matched URI
                // should be returned.
                { "-//REMOTE//DTD DAVID DOCDAVID XML//EN",
                        "http://local/base/dtd/docDavidPub1.dtd" } };
    }

    /*
     * If no match is found, a CatalogException should be thrown.
     */
    @Test(expectedExceptions = CatalogException.class)
    public void testNoMatch() {
        checkNoMatch(createResolver());
    }

    private CatalogResolver createResolver() {
        return catalogResolver(CATALOG_PUBLIC);
    }
}
