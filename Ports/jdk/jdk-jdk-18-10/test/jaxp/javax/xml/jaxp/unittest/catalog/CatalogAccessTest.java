/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.catalog.CatalogResolver;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import static jaxp.library.JAXPTestUtilities.tryRunWithAllPerm;

/*
 * @test
 * @bug 8171243
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.CatalogAccessTest
 * @summary the Catalog API grants no privilege to external resources. This test
 * verifies that SecurityException will be thrown if access to resources is denied
 * by the security manager.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class CatalogAccessTest {
    static final CatalogFeatures FEATURES = CatalogFeatures.builder().
            with(CatalogFeatures.Feature.PREFER, "system").build();

    /*
     * Verifies that the SecurityException is thrown if access to the resource is
     * denied by the security manager.
     */
    @Test(dataProvider = "accessTest", expectedExceptions = SecurityException.class)
    public void testSecurity(String cfile, String sysId, String pubId) throws Exception {
        CatalogResolver cr = CatalogManager.catalogResolver(FEATURES, URI.create(cfile));
        InputSource is = cr.resolveEntity(pubId, sysId);
        Assert.fail("Failed to throw SecurityException");
    }

    /*
        DataProvider: used for SecurityException testing
        Data columns:
        catalog uri, systemId, publicId
     */
    @DataProvider(name = "accessTest")
    Object[][] getDataForAccessTest() throws Exception {
        String systemId = "http://www.sys00test.com/rewrite.dtd";
        String publicId = "PUB-404";
        String urlFile = tryRunWithAllPerm(() ->
                getClass().getResource("rewriteSystem_id.xml").toExternalForm());
        return new Object[][]{
            {urlFile, systemId, publicId}
        };
    }
}
