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
import static catalog.ResolutionChecker.expectExceptionOnPubId;

import javax.xml.catalog.CatalogException;
import javax.xml.catalog.CatalogResolver;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.DelegatePublicTest
 * @run testng/othervm catalog.DelegatePublicTest
 * @summary Get matched URIs from DelegatePublic entries.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class DelegatePublicTest {

    @Test(dataProvider = "publicId-matchedUri")
    public void testMatch(String publicId, String matchedUri) {
        checkPubIdResolution(createResolver(), publicId, matchedUri);
    }

    @DataProvider(name = "publicId-matchedUri")
    public Object[][] dataOnMatch() {
        return new Object[][] {
                // The matched URI of the specified public id is defined in
                // a delegate catalog file of the current catalog file.
                { "-//REMOTE//DTD ALICE DOCALICE XML//EN",
                        "http://local/dtd/docAlicePub.dtd" },

                // The current catalog file defines two delegatePublic entries
                // with the same publicIdStartString, and both of them match the
                // specified public id. But the matched URI should be in the
                // delegate catalog file, which is defined in the upper
                // delegatePublic entry.
                { "-//REMOTE//DTD BOB DOCBOB XML//EN",
                        "http://local/base/dtd/bob/docBobPub.dtd" },

                // The current catalog file defines two delegatePublic entries,
                // and both of them match the specified public id. But the
                // matched URI should be in the delegate catalog file, which is
                // defined in the longest matched delegatePublic entry.
                { "-//REMOTE//DTD CARL DOCCARL XML//EN",
                        "http://local/base/dtd/carl/docCarlPub.dtd" } };
    }

    @Test(dataProvider = "publicId-expectedExceptionClass")
    public void testException(String publicId,
            Class<? extends Throwable> expectedExceptionClass) {
        expectExceptionOnPubId(createResolver(), publicId,
                expectedExceptionClass);
    }

    @DataProvider(name = "publicId-expectedExceptionClass")
    public Object[][] dataOnException() {
        return new Object[][] {
                // The matched delegatePublic entry of the specified public id
                // defines a non-existing delegate catalog file. That should
                // raise a RuntimeException.
                { "-//REMOTE//DTD DAVID DOCDAVID XML//EN",
                        RuntimeException.class },

                // There's no match of the specified public id in the catalog
                // structure. That should raise a CatalogException.
                { "-//REMOTE//DTD GHOST DOCGHOST XML//EN",
                        CatalogException.class } };
    }

    private CatalogResolver createResolver() {
        return catalogResolver("delegatePublic.xml");
    }
}
