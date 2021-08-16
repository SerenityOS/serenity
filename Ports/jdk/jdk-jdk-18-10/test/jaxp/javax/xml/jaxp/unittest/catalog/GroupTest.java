/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;
import java.nio.file.Paths;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.catalog.CatalogResolver;
import javax.xml.transform.Source;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8215330
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng catalog.GroupTest
 * @summary Tests catalog with Group entries.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class GroupTest extends CatalogSupportBase {

    String catalogGroup;
    /*
     * Initializing fields
     */
    @BeforeClass
    public void setUpClass() throws Exception {
        super.setUp();
        catalogGroup = Paths.get(filepath + "GroupTest.xml").toUri().toASCIIString();
    }

    /**
     * Tests catalog resolution with entries in a group.
     *
     * @param catalog the catalog to be used
     * @param uri an URI to be resolved by the catalog
     * @param expected the expected result string
     * @throws Exception
     */
    @Test(dataProvider = "data_group")
    public void testGroup(String catalog, String uri, String expected) throws Exception {
        CatalogResolver resolver = CatalogManager.catalogResolver(
                CatalogFeatures.defaults(), URI.create(catalog));

        Source src = resolver.resolve(uri, null);
        Assert.assertTrue(src.getSystemId().endsWith(expected), "uriSuffix match");
    }


    /*
       DataProvider: for testing catalogs with group entries
       Data: catalog file, uri, expected result string
     */
    @DataProvider(name = "data_group")
    public Object[][] getDataDOM() {
        return new Object[][]{
            {catalogGroup, "http://openjdk_java_net/xml/catalog/A/CommonFileA1.xml", "LocalFileA1.xml"},
            {catalogGroup, "http://openjdk_java_net/xml/catalog/B/CommonFileB1.xml", "LocalFileB1.xml"},
            {catalogGroup, "http://openjdk_java_net/xml/catalog/C/CommonFileC1.xml", "LocalFileC1.xml"},
        };
    }
}
