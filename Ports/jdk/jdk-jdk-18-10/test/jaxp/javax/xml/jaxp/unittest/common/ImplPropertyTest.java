/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package common;

import com.sun.org.apache.xerces.internal.utils.XMLSecurityManager.Limit;
import java.util.EnumSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;
import jdk.xml.internal.JdkProperty.ImplPropMap;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.XMLReader;


/*
 * @test
 * @bug 8265248
 * @modules java.xml/com.sun.org.apache.xerces.internal.utils
 * @modules java.xml/jdk.xml.internal
 * @run testng common.ImplPropertyTest
 * @summary Verifies Implementation-specific Features and Properties as specified
 * in the java.xml module summary.
 */
public class ImplPropertyTest {

    private final DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();
    private final XMLInputFactory xif = XMLInputFactory.newDefaultFactory();
    private final SchemaFactory sf = SchemaFactory.newDefaultInstance();


    private final XPathFactory xf = XPathFactory.newDefaultInstance();

    // as in the Processors table in java.xml module summary
    private enum Processor {
        DOM,
        SAX,
        XMLREADER,
        StAX,
        VALIDATION,
        TRANSFORM,
        XSLTC,
        DOMLS,
        XPATH
    };

    /**
     * Verifies both the new and legacy property names. This test runs two cases:
     * a. sets legacy property first;
     * b. sets new property first. Note the new property name is the same as that
     * of the System property as of JDK 17.
     * In both test cases, the expected return value shall be equal to the value
     * set with the new property name.
     * @throws Exception if the test fails
     */
    @Test
    public void testLimits() throws Exception {
        // Supported processors for Limits
        Set<Processor> pLimit = EnumSet.of(Processor.DOM, Processor.SAX, Processor.XMLREADER,
                Processor.StAX, Processor.VALIDATION, Processor.TRANSFORM);

        for (Limit limit : Limit.values()) {
            for (Processor p : pLimit) {
                testProperties(p, limit.apiProperty(), 100, limit.systemProperty(), 200, true);
            }
        }
    }

    // Supported processor for isStandalone: DOMLS
    @Test
    public void testIsStandalone() throws Exception {
        testProperties(Processor.DOMLS, ImplPropMap.ISSTANDALONE.qName(), true,
                ImplPropMap.ISSTANDALONE.systemProperty(), false, true);
    }

    // Supported processor for xsltcIsStandalone: XSLTC Serializer
    @Test
    public void testXSLTCIsStandalone() throws Exception {
        testProperties(Processor.XSLTC, ImplPropMap.XSLTCISSTANDALONE.qName(), "no",
                ImplPropMap.XSLTCISSTANDALONE.systemProperty(), "yes", true);
        testProperties(Processor.XSLTC, ImplPropMap.XSLTCISSTANDALONE.qNameOld(), "no",
                ImplPropMap.XSLTCISSTANDALONE.systemProperty(), "yes", true);
    }

    // Supported processor for cdataChunkSize: SAX and StAX
    @Test
    public void testCData() throws Exception {
        // Supported processors for CDATA
        Set<Processor> pCData = EnumSet.of(Processor.SAX, Processor.XMLREADER,
                Processor.StAX);
        ImplPropMap CDATA = ImplPropMap.CDATACHUNKSIZE;
        for (Processor p : pCData) {
            testProperties(p, CDATA.qName(), 100, CDATA.systemProperty(), 200, false);
        }
    }

    // Supported processor for extensionClassLoader: Transform
    @Test
    public void testExtensionClassLoader() throws Exception {
        ImplPropMap ECL = ImplPropMap.EXTCLSLOADER;
        TestCL cl1 = new TestCL("testClassLoader1");
        TestCL cl2 = new TestCL("testClassLoader2");
        testProperties(Processor.TRANSFORM, ECL.qNameOld(), cl1, ECL.qName(), cl2, true);
    }

    // Supported processor for feature enableExtensionFunctions: Transform, XPath
    @Test
    public void testEnableExtensionFunctions() throws Exception {
        Set<Processor> pEEF = EnumSet.of(Processor.TRANSFORM, Processor.XPATH);
        ImplPropMap EEF = ImplPropMap.ENABLEEXTFUNC;
        for (Processor p : pEEF) {
            testFeatures(p, EEF.qName(), true, EEF.systemProperty(), false, EEF.isNameDiffer());
        }
    }

    // Supported processor for feature overrideDefaultParser: Transform, Validation, XPath
    @Test
    public void testOverrideDefaultParser() throws Exception {
        Set<Processor> pEEF = EnumSet.of(Processor.TRANSFORM, Processor.VALIDATION, Processor.XPATH);
        ImplPropMap ODP = ImplPropMap.OVERRIDEPARSER;
        for (Processor p : pEEF) {
            testFeatures(p, ODP.qName(), true, ODP.systemProperty(), false, ODP.isNameDiffer());
        }
    }

    // Supported processor for feature resetSymbolTable: SAX
    @Test
    public void testResetSymbolTable() throws Exception {
        ImplPropMap RST = ImplPropMap.RESETSYMBOLTABLE;
        testFeatures(Processor.SAX, RST.qName(), true, RST.systemProperty(), false, RST.isNameDiffer());
    }

    /**
     * Tests properties. Two assertions:
     * (1) verifies the old property is still supported;
     * (2) verifies the new property name takes preference.
     *
     * @param processor the processor to be tested
     * @param name1 the old property name
     * @param value1 the value to be set with name1
     * @param name2 the new property name
     * @param value2 the value to be set with name2
     * @param differ a flag indicating whether name1 and name2 differ
     * @throws Exception if the test fails
     */
    private void testProperties(Processor processor, String name1, Object value1,
            String name2, Object value2, boolean differ)
            throws Exception {

        Object ret1 = null;
        Object ret2 = null;
        switch (processor) {
            case DOM:
                dbf.setAttribute(name1, value1);
                ret1 = dbf.getAttribute(name1);
                if (differ) {
                    dbf.setAttribute(name2, value2);
                    dbf.setAttribute(name1, value1);
                    ret2 = dbf.getAttribute(name2);
                }
                break;
            case SAX:
                SAXParser sp = SAXParserFactory.newDefaultInstance().newSAXParser();
                sp.setProperty(name1, value1);
                ret1 = sp.getProperty(name1);
                if (differ) {
                    sp.setProperty(name2, value2);
                    sp.setProperty(name1, value1);
                    ret2 = sp.getProperty(name2);
                }
                break;
            case XMLREADER:
                XMLReader reader = SAXParserFactory.newDefaultInstance().newSAXParser().getXMLReader();
                reader.setProperty(name1, value1);
                ret1 = reader.getProperty(name1);
                if (differ) {
                    reader.setProperty(name2, value2);
                    reader.setProperty(name1, value1);
                    ret2 = reader.getProperty(name2);
                }
                break;
            case StAX:
                xif.setProperty(name1, value1);
                ret1 = xif.getProperty(name1);
                if (differ) {
                    xif.setProperty(name2, value2);
                    xif.setProperty(name1, value1);
                    ret2 = xif.getProperty(name2);
                }
                break;
            case VALIDATION:
                sf.setProperty(name1, value1);
                ret1 = sf.getProperty(name1);
                if (differ) {
                    sf.setProperty(name2, value2);
                    sf.setProperty(name1, value1);
                    ret2 = sf.getProperty(name2);
                }
                break;
            case TRANSFORM:
                TransformerFactory tf = TransformerFactory.newDefaultInstance();
                tf.setAttribute(name1, value1);
                ret1 = tf.getAttribute(name1);
                if (differ) {
                    tf.setAttribute(name2, value2);
                    tf.setAttribute(name1, value1);
                    ret2 = tf.getAttribute(name2);
                }
                break;
            case XSLTC:
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(name1, (String)value1);
                ret1 = transformer.getOutputProperty(name1);
                if (differ) {
                    transformer.setOutputProperty(name2, (String)value2);
                    transformer.setOutputProperty(name1, (String)value1);
                    ret2 = transformer.getOutputProperty(name2);
                }
                break;
            case DOMLS:
                DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
                DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
                LSSerializer serializer = impl.createLSSerializer();
                DOMConfiguration domConfig = serializer.getDomConfig();
                domConfig.setParameter(name1, value1);
                ret1 = domConfig.getParameter(name1);
                if (differ) {
                    domConfig.setParameter(name2, value2);
                    domConfig.setParameter(name1, value1);
                    ret2 = domConfig.getParameter(name2);
                }
                break;
            case XPATH:
                break;
        }
        if ((value1 instanceof Integer) && ret1 instanceof String) {
            ret1 = Integer.parseInt((String)ret1);
            ret2 = Integer.parseInt((String)ret2);
        }

        // name1 is set, expected return value: value1 (set with the old name)
        Assert.assertEquals(ret1, value1);
        if (differ) {
            // if both are set, expected value: value2 (set with the new name)
            Assert.assertEquals(ret2, value2);
        }
    }

    private void testFeatures(Processor processor, String name1, boolean value1,
            String name2, boolean value2, boolean differ)
            throws Exception {
        boolean ret1 = false, ret2 = false;
        switch (processor) {
            case DOM:
                dbf.setFeature(name1, value1);
                Assert.assertEquals(dbf.getFeature(name1), value1);
                if (differ) {
                    dbf.setFeature(name2, value2);
                    dbf.setFeature(name1, value1);
                    Assert.assertEquals(dbf.getFeature(name2), value2);
                }
                return;
            case SAX:
                SAXParserFactory spf = SAXParserFactory.newDefaultInstance();
                spf.setFeature(name1, value1);
                Assert.assertEquals(spf.getFeature(name1), value1);
                if (differ) {
                    spf.setFeature(name2, value2);
                    spf.setFeature(name1, value1);
                    Assert.assertEquals(spf.getFeature(name2), value2);
                }
                return;
            case VALIDATION:
                sf.setFeature(name1, value1);
                Assert.assertEquals(sf.getFeature(name1), value1);
                if (differ) {
                    sf.setFeature(name2, value2);
                    sf.setFeature(name1, value1);
                    Assert.assertEquals(sf.getFeature(name2), value2);
                }
                return;
            case TRANSFORM:
                TransformerFactory tf = TransformerFactory.newDefaultInstance();
                tf.setFeature(name1, value1);
                Assert.assertEquals(tf.getFeature(name1), value1);
                if (differ) {
                    tf.setFeature(name2, value2);
                    tf.setFeature(name1, value1);
                    Assert.assertEquals(tf.getFeature(name2), value2);
                }
                return;
            case XPATH:
                xf.setFeature(name1, value1);
                Assert.assertEquals(xf.getFeature(name1), value1);
                if (differ) {
                    xf.setFeature(name2, value2);
                    xf.setFeature(name1, value1);
                    Assert.assertEquals(xf.getFeature(name2), value2);
                }
                return;
        }

        Assert.fail("Failed setting features for : " + processor);
    }


    class TestCL extends ClassLoader {
        String name;
        public TestCL(String name) {
            this.name = name;
        }

        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException( name );
        }
    }
}
