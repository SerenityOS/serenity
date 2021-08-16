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
import static catalog.ResolutionChecker.checkNoMatch;
import static catalog.ResolutionChecker.checkSysIdResolution;

import javax.xml.catalog.CatalogException;
import javax.xml.catalog.CatalogResolver;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.RewriteSystemTest
 * @run testng/othervm catalog.RewriteSystemTest
 * @summary Get matched URIs from rewriteSystem entries.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class RewriteSystemTest {

    @Test(dataProvider = "systemId-matchedUri")
    public void testMatch(String systemId, String matchedUri) {
        checkSysIdResolution(createResolver(), systemId, matchedUri);
    }

    @DataProvider(name = "systemId-matchedUri")
    public Object[][] dataOnMatch() {
        return new Object[][] {
                // The matched URI of the specified system id is defined in a
                // rewriteSystem entry. The match is an absolute path.
                { "http://remote/dtd/alice/docAlice.dtd",
                        "http://local/dtd/alice/rs/docAlice.dtd" },

                // The matched URI of the specified system id is defined in a
                // rewriteSystem entry. The match isn't an absolute path.
                { "http://remote/dtd/bob/docBob.dtd",
                        "http://local/base/dtd/bob/rs/docBob.dtd" },

                // The matched URI of the specified system id is defined in a
                // rewriteSystem entry. The match isn't an absolute path, and
                // the rewriteSystem entry defines alternative base. So the
                // returned URI should include the alternative base.
                { "http://remote/dtd/carl/docCarl.dtd",
                        "http://local/carlBase/dtd/carl/rs/docCarl.dtd" },

                // The catalog file defines two rewriteSystem entries, and both
                // of them match the specified system id. But the first matched
                // URI should be returned.
                { "http://remote/dtd/david/docDavid.dtd",
                        "http://local/base/dtd/david1/rs/docDavid.dtd" },

                // The catalog file defines two rewriteSystem entries, and both
                // of them match the specified system id. But the longest match
                // should be used.
                { "http://remote/dtd/ella/docElla.dtd",
                        "http://local/base/dtd/ella/rs/docElla.dtd" } };
    }

    /*
     * If no match is found, a CatalogException should be thrown.
     */
    @Test(expectedExceptions = CatalogException.class)
    public void testNoMatch() {
        checkNoMatch(createResolver());
    }

    private CatalogResolver createResolver() {
        return catalogResolver("rewriteSystem.xml");
    }
}
