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

import static catalog.CatalogTestUtils.catalogUriResolver;
import static catalog.ResolutionChecker.checkNoUriMatch;
import static catalog.ResolutionChecker.checkUriResolution;

import javax.xml.catalog.CatalogResolver;
import javax.xml.catalog.CatalogException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.UriSuffixTest
 * @run testng/othervm catalog.UriSuffixTest
 * @summary Get matched URIs from rewriteURI entries.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class UriSuffixTest {

    @Test(dataProvider = "uri-matchedUri")
    public void testMatch(String uri, String matchedUri) {
        checkUriResolution(createResolver(), uri, matchedUri);
    }

    @DataProvider(name = "uri-matchedUri")
    public Object[][] dataOnMatch() {
        return new Object[][] {
                // The matched URI of the specified URI reference is defined in
                // a uriSuffix entry. The match is an absolute path.
                { "http://remote/dtd/alice/docAlice.dtd",
                        "http://local/dtd/docAliceUS.dtd" },

                // The matched URI of the specified URI reference is defined in
                // a uriSuffix entry. The match isn't an absolute path.
                { "http://remote/dtd/bob/docBob.dtd",
                        "http://local/base/dtd/docBobUS.dtd" },

                // The matched URI of the specified URI reference is defined in
                // a uriSuffix entry. The match isn't an absolute path, and the
                // uriSuffix entry defines alternative base. So the returned
                // URI should include the alternative base.
                { "http://remote/dtd/carl/cdocCarl.dtd",
                        "http://local/carlBase/dtd/docCarlUS.dtd" },

                // The catalog file defines two uriSuffix entries, and both of
                // them match the specified URI reference. But the first matched
                // URI should be returned.
                { "http://remote/dtd/david/docDavid.dtd",
                        "http://local/base/dtd/docDavidUS1.dtd" },

                // The catalog file defines two uriSuffix entries, and both
                // of them match the specified URI reference. But the longest
                // match should be used.
                { "http://remote/dtd/ella/docElla.dtd",
                        "http://local/base/dtd/docEllaUS.dtd" } };
    }

    /*
     * If no match is found, a CatalogException should be thrown.
     */
    @Test(expectedExceptions = CatalogException.class)
    public void testNoMatch() {
        checkNoUriMatch(createResolver());
    }

    private CatalogResolver createResolver() {
        return catalogUriResolver("uriSuffix.xml");
    }
}
