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
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.SystemFamilyTest
 * @run testng/othervm catalog.SystemFamilyTest
 * @summary Get matched URIs from system, rewriteSystem, systemSuffix and
 *          delegateSystem entries. It tests the resolution priorities among
 *          the system family entries. The test rule is based on OASIS
 *          Standard V1.1 section 7.1.2. "Resolution of External Identifiers".
 */
@Listeners({jaxp.library.FilePolicy.class})
public class SystemFamilyTest {

    @Test(dataProvider = "systemId-matchedUri")
    public void testMatch(String systemId, String matchedUri) {
        checkSysIdResolution(createResolver(), systemId, matchedUri);
    }

    @DataProvider(name = "systemId-matchedUri")
    public Object[][] dataOnMatch() {
        return new Object[][] {
                // The matched URI of the specified system id is defined in a
                // system entry.
                { "http://remote/dtd/alice/docAlice.dtd",
                        "http://local/base/dtd/docAliceSys.dtd" },

                // The matched URI of the specified system id is defined in a
                // rewriteSystem entry.
                { "http://remote/dtd/bob/docBob.dtd",
                        "http://local/base/dtd/rs/docBob.dtd" },

                // The matched URI of the specified system id is defined in a
                // systemSuffix entry.
                { "http://remote/dtd/carl/docCarl.dtd",
                         "http://local/base/dtd/docCarlSS.dtd" } };
    }

    /*
     * If no match is found, a CatalogException should be thrown.
     */
    @Test(expectedExceptions = CatalogException.class)
    public void testNoMatch() {
        checkNoMatch(createResolver());
    }

    private CatalogResolver createResolver() {
        return catalogResolver("systemFamily.xml");
    }
}
