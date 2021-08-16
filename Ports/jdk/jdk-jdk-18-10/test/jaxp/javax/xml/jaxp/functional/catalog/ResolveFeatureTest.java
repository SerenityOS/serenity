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

import static catalog.CatalogTestUtils.CATALOG_SYSTEM;
import static catalog.CatalogTestUtils.CATALOG_URI;
import static catalog.CatalogTestUtils.RESOLVE_CONTINUE;
import static catalog.CatalogTestUtils.RESOLVE_IGNORE;
import static catalog.CatalogTestUtils.RESOLVE_STRICT;
import static catalog.CatalogTestUtils.catalogResolver;
import static catalog.CatalogTestUtils.catalogUriResolver;
import static catalog.ResolutionChecker.checkSysIdResolution;
import static catalog.ResolutionChecker.checkUriResolution;
import static javax.xml.catalog.CatalogFeatures.builder;

import javax.xml.catalog.CatalogException;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogFeatures.Feature;
import javax.xml.catalog.CatalogResolver;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.ResolveFeatureTest
 * @run testng/othervm catalog.ResolveFeatureTest
 * @summary This case tests how does resolve feature affect the catalog
 *          resolution.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class ResolveFeatureTest {

    /*
     * For strict external identifier resolution, if no match is found,
     * it should throw CatalogException.
     */
    @Test(expectedExceptions = CatalogException.class)
    public void testStrictResolutionOnEntityResolver() {
        createEntityResolver(RESOLVE_STRICT).resolveEntity(null,
                "http://remote/dtd/alice/docAliceDummy.dtd");
    }

    /*
     * For strict URI reference resolution, if no match is found,
     * it should throw CatalogException.
     */
    @Test(expectedExceptions = CatalogException.class)
    public void testStrictResolutionOnUriResolver() {
        createUriResolver(RESOLVE_STRICT).resolve(
                "http://remote/dtd/alice/docAliceDummy.dtd", null);
    }

    /*
     * For continue external identifier resolution, if no match is found,
     * it should continue the process.
     */
    @Test
    public void testContinueResolutionOnEntityResolver() {
        CatalogResolver resolver = createEntityResolver(RESOLVE_CONTINUE);
        resolver.resolveEntity(null, "http://remote/dtd/bob/docBobDummy.dtd");
        checkSysIdResolution(resolver, "http://remote/dtd/bob/docBob.dtd",
                "http://local/base/dtd/docBobSys.dtd");
    }

    /*
     * For continue URI reference resolution, if no match is found,
     * it should continue the process.
     */
    @Test
    public void testContinueResolutionOnUriResolver() {
        CatalogResolver resolver = createUriResolver(RESOLVE_CONTINUE);
        resolver.resolve("http://remote/dtd/bob/docBobDummy.dtd", null);
        checkUriResolution(resolver, "http://remote/dtd/bob/docBob.dtd",
                "http://local/base/dtd/docBobURI.dtd");
    }

    /*
     * For ignore external identifier resolution, if no match is found,
     * it should break the process and return null.
     */
    @Test
    public void testIgnoreResolutionOnEntityResolver() {
        checkSysIdResolution(createEntityResolver(RESOLVE_IGNORE),
                "http://remote/dtd/carl/docCarlDummy.dtd", null);
    }

    /*
     * For ignore URI reference resolution, if no match is found,
     * it should break the process and return null.
     */
    @Test
    public void testIgnoreResolutionOnUriResolver() {
        checkUriResolution(createUriResolver(RESOLVE_IGNORE),
                "http://remote/dtd/carl/docCarlDummy.dtd", null);
    }

    private CatalogResolver createEntityResolver(String resolve) {
        return catalogResolver(createFeature(resolve), CATALOG_SYSTEM);
    }

    private CatalogResolver createUriResolver(String resolve) {
        return catalogUriResolver(createFeature(resolve), CATALOG_URI);
    }

    private CatalogFeatures createFeature(String resolve) {
        return builder().with(Feature.RESOLVE, resolve).build();
    }
}
