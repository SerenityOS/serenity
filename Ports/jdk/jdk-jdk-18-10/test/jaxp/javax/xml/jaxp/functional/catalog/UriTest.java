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

import static catalog.CatalogTestUtils.CATALOG_URI;
import static catalog.CatalogTestUtils.RESOLVE_CONTINUE;
import static catalog.CatalogTestUtils.catalogUriResolver;
import static catalog.ResolutionChecker.checkNoUriMatch;
import static catalog.ResolutionChecker.checkUriResolution;

import javax.xml.catalog.CatalogResolver;
import javax.xml.catalog.CatalogException;
import javax.xml.catalog.CatalogFeatures;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.UriTest
 * @run testng/othervm catalog.UriTest
 * @summary Get matched URIs from uri entries.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class UriTest {

    @Test(dataProvider = "uri-matchedUri")
    public void testMatch(String uri, String matchedUri) {
        checkUriResolution(createResolver(), uri, matchedUri);
    }

    @DataProvider(name = "uri-matchedUri")
    public Object[][] dataOnMatch() {
        return new Object[][] {
                // The matched URI of the specified URI reference is defined in
                // a uri entry. The match is an absolute path.
                { "http://remote/dtd/uri/alice/docAlice.dtd",
                        "http://local/dtd/docAliceURI.dtd" },

                // The matched URI of the specified URI reference is defined in
                // a uri entry. But the match isn't an absolute path, so the
                // returned URI should include the base, which is defined by the
                // catalog file, as the prefix.
                { "http://remote/dtd/bob/docBob.dtd",
                        "http://local/base/dtd/docBobURI.dtd" },

                // The catalog file defines two uri entries, and both of them
                // match the specified URI reference. But the first matched URI
                // should be returned.
                { "http://remote/dtd/david/docDavid.dtd",
                        "http://local/base/dtd/docDavidURI1.dtd" } };
    }

    /*
     * Specify base location via method CatalogResolver.resolve(href, base).
     */
    @Test
    public void testSpecifyBaseByAPI() {
        checkUriResolution(createResolver(),
                "http://remote/dtd/carl/docCarl.dtd",
                "http://local/carlBase/dtd/docCarlURI.dtd");

        CatalogResolver continueResolver = catalogUriResolver(
                CatalogFeatures.builder().with(CatalogFeatures.Feature.RESOLVE,
                        RESOLVE_CONTINUE).build(), CATALOG_URI);
        checkUriResolution(continueResolver, "docCarl.dtd",
                "http://local/alternativeBase/dtd/",
                "http://local/alternativeBase/dtd/docCarl.dtd");
    }

    /*
     * If no match is found, a CatalogException should be thrown.
     */
    @Test(expectedExceptions = CatalogException.class)
    public void testNoMatch() {
        checkNoUriMatch(createResolver());
    }

    private CatalogResolver createResolver() {
        return catalogUriResolver(CATALOG_URI);
    }
}
