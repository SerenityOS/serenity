/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static jaxp.library.JAXPTestUtilities.setSystemProperty;

import static catalog.CatalogTestUtils.FEATURE_FILES;
import static catalog.CatalogTestUtils.catalogResolver;
import static catalog.CatalogTestUtils.catalogUriResolver;
import static catalog.CatalogTestUtils.getCatalogPath;
import static catalog.ResolutionChecker.checkSysIdResolution;
import static catalog.ResolutionChecker.checkUriResolution;
import static javax.xml.catalog.CatalogFeatures.builder;
import static javax.xml.catalog.CatalogFeatures.Feature.FILES;

import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogResolver;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.SpecifyCatalogTest
 * @run testng/othervm catalog.SpecifyCatalogTest
 * @summary This case tests how to specify the catalog files.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class SpecifyCatalogTest {

    private static final String ID_URI = "http://remote/dtd/uri/doc.dtd";
    private static final String ID_SYS = "http://remote/dtd/sys/doc.dtd";

    private static final CatalogFeatures FILES_FEATURE = createFeature(
            "specifyCatalog-feature.xml");

    /*
     * CatalogResolver specifies catalog via feature javax.xml.catalog.files.
     */
    @Test
    public void specifyCatalogOnEntityResolver() {
        checkSysIdResolution(catalogResolver(FILES_FEATURE, (String[]) null),
                ID_SYS, "http://local/base/dtd/docFeatureSys.dtd");
    }

    /*
     * CatalogResolver specifies catalog via feature javax.xml.catalog.files.
     */
    @Test
    public void specifyCatalogOnUriResolver() {
        checkUriResolution(catalogUriResolver(FILES_FEATURE, (String[]) null),
                ID_URI, "http://local/base/dtd/docFeatureURI.dtd");
    }

    /*
     * Resolver specifies catalog via system property javax.xml.catalog.files.
     */
    @Test
    public void specifyCatalogViaSysProps() {
        setSystemProperty(FEATURE_FILES,
                getCatalogPath("specifyCatalog-sysProps.xml").toASCIIString());

        checkResolutionOnEntityResolver(catalogResolver((String[]) null),
                "http://local/base/dtd/docSysPropsSys.dtd");
        checkResolutionOnEntityResolver(
                catalogResolver(FILES_FEATURE, "specifyCatalog-api.xml"),
                "http://local/base/dtd/docAPISys.dtd");

        checkResolutionOnUriResolver(catalogUriResolver((String[]) null),
                "http://local/base/dtd/docSysPropsURI.dtd");
        checkResolutionOnUriResolver(
                catalogUriResolver(FILES_FEATURE, "specifyCatalog-api.xml"),
                "http://local/base/dtd/docAPIURI.dtd");
    }

    private void checkResolutionOnEntityResolver(CatalogResolver resolver,
            String matchedUri) {
        checkSysIdResolution(resolver, ID_SYS, matchedUri);
    }

    private void checkResolutionOnUriResolver(CatalogResolver resolver,
            String matchedUri) {
        checkUriResolution(resolver, ID_URI, matchedUri);
    }

    private static CatalogFeatures createFeature(String catalogName) {
        return builder().with(FILES, getCatalogPath(catalogName).toASCIIString()).build();
    }
}
