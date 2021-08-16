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

import static catalog.CatalogTestUtils.PREFER_PUBLIC;
import static catalog.CatalogTestUtils.PREFER_SYSTEM;
import static catalog.CatalogTestUtils.catalogResolver;
import static javax.xml.catalog.CatalogFeatures.Feature.PREFER;

import javax.xml.catalog.CatalogException;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogResolver;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.PreferFeatureTest
 * @run testng/othervm catalog.PreferFeatureTest
 * @summary This case tests how does the feature affect the catalog resolution,
 *          and tests the priority between this feature and attribute prefer
 *          in catalog file.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class PreferFeatureTest {

    @Test(dataProvider = "prefer-publicId-systemId",
            expectedExceptions = CatalogException.class)
    public void testPreferFeature(String prefer, String systemId,
            String publicId) {
        createResolver(prefer).resolveEntity(systemId, publicId);
    }

    @DataProvider(name = "prefer-publicId-systemId")
    public Object[][] data() {
        return new Object[][] {
                // The feature prefer is system. There's a match for the
                // specified public id, and no match for the specified system id.
                // But the resolver cannot find the expected match, and raises a
                // CatalogException.
                { PREFER_SYSTEM, "-//REMOTE//DTD ALICE DOCALICE XML//EN",
                        "http://remote/dtd/alice/docAliceDummy.dtd" },

                // The feature prefer is public, and the prefer attribute of a
                // group entry is system. There's a match for the specified
                // public id, and no match for the specified system id. But the
                // resolver still cannot find the expected match, and raises a
                // CatalogException.
                { PREFER_PUBLIC, "-//REMOTE//DTD BOB DOCBOB XML//EN",
                         "http://remote/dtd/bob/docBobDummy.dtd"} };
    }

    private CatalogResolver createResolver(String prefer) {
        return catalogResolver(
                CatalogFeatures.builder().with(PREFER, prefer).build(),
                "preferFeature.xml");
    }
}
