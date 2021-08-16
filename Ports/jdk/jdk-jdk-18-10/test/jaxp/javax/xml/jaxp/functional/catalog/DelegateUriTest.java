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
import static catalog.ResolutionChecker.checkUriResolution;
import static catalog.ResolutionChecker.expectExceptionOnUri;

import javax.xml.catalog.CatalogResolver;
import javax.xml.catalog.CatalogException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.DelegateUriTest
 * @run testng/othervm catalog.DelegateUriTest
 * @summary Get matched URIs from delegateURI entries.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class DelegateUriTest {

    @Test(dataProvider = "uri-matchedUri")
    public void testMatch(String uri, String matchedUri) {
        checkUriResolution(createResolver(), uri, matchedUri);
    }

    @DataProvider(name = "uri-matchedUri")
    public Object[][] data() {
        return new Object[][] {
                // The matched URI of the specified URI reference is defined in
                // a delegate catalog file of the current catalog file.
                { "http://remote/dtd/alice/docAlice.dtd",
                        "http://local/base/dtd/alice/docAliceDU.dtd" },

                // The current catalog file defines two delegateURI entries
                // with the same uriStartString, and both of them match the
                // specified URI reference. But the matched URI should be in
                // the delegate catalog file, which is defined in the upper
                // delegateURI entry.
                { "http://remote/dtd/bob/docBob.dtd",
                        "http://local/base/dtd/bob/docBobDU.dtd" },

                // The current catalog file defines two delegateURI entries,
                // and both of them match the specified URI reference. But the
                // matched URI should be in the delegate catalog file, which is
                // defined in the longest matched delegateURI entry.
                { "http://remote/dtd/carl/docCarl.dtd",
                        "http://local/base/dtd/carl/docCarlDU.dtd"} };
    }

    @Test(dataProvider = "uri-expectedExceptionClass")
    public void testException(String uri,
            Class<? extends Throwable> expectedExceptionClass) {
        expectExceptionOnUri(createResolver(), uri, expectedExceptionClass);
    }

    @DataProvider(name = "uri-expectedExceptionClass")
    public Object[][] dataOnException() {
        return new Object[][] {
                // The matched delegateURI entry of the specified URI reference
                // defines a non-existing delegate catalog file. That should
                // raise a RuntimeException.
                { "http://remote/dtd/david/docDavidDU.dtd",
                        RuntimeException.class },

                // There's no match of the specified URI reference in the
                // catalog structure. That should raise a CatalogException.
                { "http://ghost/xml/dtd/ghost/docGhostDS.dtd",
                        CatalogException.class } };
    }

    private CatalogResolver createResolver() {
        return catalogUriResolver("delegateUri.xml");
    }
}
