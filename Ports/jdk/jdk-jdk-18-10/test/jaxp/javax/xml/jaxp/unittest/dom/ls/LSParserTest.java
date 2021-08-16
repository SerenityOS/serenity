/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package dom.ls;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSResourceResolver;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.ls.LSParserTest
 * @run testng/othervm dom.ls.LSParserTest
 * @summary Test LSParser's DOMConfiguration for supported properties.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class LSParserTest {

    @Test
    public void testDOMConfiguration() {

        final DOMErrorHandler handler = new DOMErrorHandler() {
            public boolean handleError(final DOMError error) {
                return false;
            }
        };

        final LSResourceResolver resolver = new LSResourceResolver() {
            public LSInput resolveResource(final String type, final String namespaceURI, final String publicId, final String systemId, final String baseURI) {
                return null;
            }
        };

        final Object[][] values = {
                // parameter, value
                { "canonical-form", Boolean.FALSE }, { "cdata-sections", Boolean.FALSE }, { "cdata-sections", Boolean.TRUE },
                { "check-character-normalization", Boolean.FALSE }, { "comments", Boolean.FALSE }, { "comments", Boolean.TRUE },
                { "datatype-normalization", Boolean.FALSE }, { "entities", Boolean.FALSE }, { "entities", Boolean.TRUE }, { "error-handler", handler },
                { "infoset", Boolean.TRUE }, { "namespaces", Boolean.TRUE }, { "namespace-declarations", Boolean.TRUE },
                { "namespace-declarations", Boolean.FALSE }, { "normalize-characters", Boolean.FALSE }, { "split-cdata-sections", Boolean.TRUE },
                { "split-cdata-sections", Boolean.FALSE }, { "validate", Boolean.FALSE }, { "validate-if-schema", Boolean.FALSE },
                { "well-formed", Boolean.TRUE }, { "element-content-whitespace", Boolean.TRUE },

                { "charset-overrides-xml-encoding", Boolean.TRUE }, { "charset-overrides-xml-encoding", Boolean.FALSE }, { "disallow-doctype", Boolean.FALSE },
                { "ignore-unknown-character-denormalizations", Boolean.TRUE }, { "resource-resolver", resolver }, { "resource-resolver", null },
                { "supported-media-types-only", Boolean.FALSE }, };

        DOMImplementation domImpl = null;
        try {
            domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException parserConfigurationException) {
            parserConfigurationException.printStackTrace();
            Assert.fail(parserConfigurationException.toString());
        }

        DOMImplementationLS lsImpl = (DOMImplementationLS) domImpl.getFeature("LS", "3.0");

        LSParser lsParser = lsImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);

        DOMConfiguration config = lsParser.getDomConfig();

        for (int i = values.length; --i >= 0;) {
            Object val = values[i][1];
            String param = (String) values[i][0];
            try {
                config.setParameter(param, val);
                Object returned = config.getParameter(param);
                Assert.assertEquals(val, returned, "'" + param + "' is set to " + returned + ", but expected " + val);
                System.out.println("set '" + param + "'" + " to '" + val + "'" + " and returned '" + returned + "'");
            } catch (DOMException e) {
                String settings = "setting '" + param + "' to " + val;
                System.err.println(settings);
                e.printStackTrace();
                Assert.fail(settings + ", " + e.toString());
            }
        }
    }
}
