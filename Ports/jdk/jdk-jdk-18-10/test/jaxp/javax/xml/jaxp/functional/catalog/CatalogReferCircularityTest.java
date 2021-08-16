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

import javax.xml.catalog.CatalogException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.CatalogReferCircularityTest
 * @run testng/othervm catalog.CatalogReferCircularityTest
 * @summary Via nextCatalog entry, the catalog reference chain may be
 *          a (partial) closed circuit. For instance, a catalog may use itself
 *          as an additional catalog specified in its own nextCatalog entry.
 *          This case tests if the implementation handles this issue.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class CatalogReferCircularityTest {

    @Test(dataProvider = "catalogName",
            expectedExceptions = CatalogException.class)
    public void testReferCircularity(String catalogFile) {
        catalogResolver(catalogFile).resolveEntity(null,
                "http://remote/dtd/ghost/docGhost.dtd");
    }

    @DataProvider(name = "catalogName")
    public Object[][] catalogName() {
        return new Object[][] {
                // This catalog defines itself as next catalog.
                { "catalogReferCircle-itself.xml" },

                // This catalog defines catalogReferCircle-right.xml as its next
                // catalog. And catalogReferCircle-right.xml also defines
                // catalogReferCircle-left.xml as its next catalog, too.
                { "catalogReferCircle-left.xml" } };
    }
}
