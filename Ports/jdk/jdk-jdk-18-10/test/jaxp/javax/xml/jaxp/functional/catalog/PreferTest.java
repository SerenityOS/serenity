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
import static catalog.ResolutionChecker.checkExtIdResolution;

import javax.xml.catalog.CatalogResolver;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.PreferTest
 * @run testng/othervm catalog.PreferTest
 * @summary Get matched URIs from system and public family entries, which
 *          specify the prefer attribute. It tests how does the prefer attribute
 *          affect the resolution procedure. The test rule is based on OASIS
 *          Standard V1.1 section 4.1.1. "The prefer attribute".
 */
@Listeners({jaxp.library.FilePolicy.class})
public class PreferTest {

    @Test(dataProvider = "publicId-systemId-matchedUri")
    public void testPrefer(String publicId, String systemId,
            String expected) {
        checkExtIdResolution(createResolver(), publicId, systemId, expected);
    }

    @DataProvider(name = "publicId-systemId-matchedUri")
    public Object[][] data() {
        return new Object[][] {
                // The prefer attribute is public. Both of the specified public
                // id and system id have matches in the catalog file. But
                // finally, the returned URI is the system match.
                { "-//REMOTE//DTD ALICE DOCALICE XML//EN",
                        "http://remote/dtd/alice/docAlice.dtd",
                        "http://local/base/dtd/docAliceSys.dtd" },

                // The prefer attribute is public, and only the specified system
                // id has match. The returned URI is the system match.
                { "-//REMOTE//DTD ALICE DOCALICEDUMMY XML//EN",
                        "http://remote/dtd/alice/docAlice.dtd",
                        "http://local/base/dtd/docAliceSys.dtd"},

                // The prefer attribute is public, and only the specified public
                // id has match. The returned URI is the system match.
                { "-//REMOTE//DTD ALICE DOCALICE XML//EN",
                        "http://remote/dtd/alice/docAliceDummy.dtd",
                        "http://local/base/dtd/docAlicePub.dtd" },

                // The prefer attribute is system, and both of the specified
                // system id and public id have matches. But the returned URI is
                // the system match.
                { "-//REMOTE//DTD BOB DOCBOB XML//EN",
                        "http://remote/dtd/bob/docBob.dtd",
                        "http://local/base/dtd/docBobSys.dtd" },

                // The prefer attribute is system, and only system id has match.
                // The returned URI is the system match.
                { "-//REMOTE//DTD BOB DOCBOBDUMMY XML//EN",
                        "http://remote/dtd/bob/docBob.dtd",
                        "http://local/base/dtd/docBobSys.dtd" } };
    }

    private CatalogResolver createResolver() {
        return catalogResolver("prefer.xml");
    }
}
