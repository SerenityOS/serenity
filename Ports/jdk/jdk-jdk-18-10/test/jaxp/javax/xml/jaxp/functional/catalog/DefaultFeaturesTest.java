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

import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogFeatures.Feature;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.DefaultFeaturesTest
 * @run testng/othervm catalog.DefaultFeaturesTest
 * @summary This case tests if the default feature values are expected.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class DefaultFeaturesTest {

    private CatalogFeatures defaultFeature;

    @BeforeClass
    public void init() {
        defaultFeature = CatalogFeatures.defaults();
    }

    @Test(dataProvider="feature-value")
    public void testDefaultFeatures(Feature feature, String expected) {
        String featureValue = defaultFeature.get(feature);
        if (expected != null) {
            Assert.assertEquals(featureValue, expected);
        } else {
            Assert.assertNull(featureValue);
        }
    }

    @DataProvider(name = "feature-value")
    public Object[][] data() {
        return new Object[][] {
                { Feature.FILES, null },
                { Feature.PREFER, CatalogTestUtils.PREFER_PUBLIC },
                { Feature.DEFER, CatalogTestUtils.DEFER_TRUE },
                { Feature.RESOLVE, CatalogTestUtils.RESOLVE_STRICT } };
    }
}
