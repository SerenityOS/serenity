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
import static catalog.ResolutionChecker.checkPubIdResolution;

import javax.xml.catalog.CatalogResolver;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.UrnUnwrappingTest
 * @run testng/othervm catalog.UrnUnwrappingTest
 * @summary If the passed public identifier is started with "urn:publicid:",
 *          it has to be regarded as URN and normalized. And then the catalog
 *          resolver uses the normalized stuff to do matching.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class UrnUnwrappingTest {

    @Test(dataProvider = "urn-matchedUri")
    public void testUnwrapping(String urn, String matchedUri) {
        checkPubIdResolution(createResolver(), urn, matchedUri);
    }

    @DataProvider(name = "urn-matchedUri")
    public Object[][] data() {
        return new Object[][] {
                // The specified public id is URN format.
                { "urn:publicid:-:REMOTE:DTD+ALICE+DOCALICE+XML:EN",
                        "http://local/base/dtd/docAlicePub.dtd" },

                // The specified public id includes some special URN chars.
                { "urn:publicid:-:REMOTE:DTD+BOB+DOCBOB+;+%2B+%3A+%2F+%3B+%27"
                        + "+%3F+%23+%25:EN",
                        "http://local/base/dtd/docBobPub.dtd" } };
    }

    private CatalogResolver createResolver() {
        return catalogResolver("urnUnwrapping.xml");
    }
}
