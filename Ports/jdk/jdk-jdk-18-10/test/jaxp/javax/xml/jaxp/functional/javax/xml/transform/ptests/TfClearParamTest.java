/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
package javax.xml.transform.ptests;

import static javax.xml.transform.ptests.TransformerTestConst.XML_DIR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Class containing the test cases for SAXParserFactory API
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.transform.ptests.TfClearParamTest
 * @run testng/othervm javax.xml.transform.ptests.TfClearParamTest
 */
@Listeners({jaxp.library.FilePolicy.class})
public class TfClearParamTest {
    /**
     * Test style-sheet file name.
     */
    private final String XSL_FILE = XML_DIR + "cities.xsl";

    /**
     * Long parameter name embedded with a URI.
     */
    private final String LONG_PARAM_NAME = "{http://xyz.foo.com/yada/baz.html}foo";

    /**
     * Short parameter name.
     */
    private final String SHORT_PARAM_NAME = "foo";

    /**
     * Parameter value.
     */
    private final String PARAM_VALUE = "xyz";

    /**
     * Obtains transformer's parameter with the same name that set before. Value
     * should be same as set one.
     * @throws TransformerConfigurationException If for some reason the
     *         TransformerHandler can not be created.
     */
    @Test
    public void clear01() throws TransformerConfigurationException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setParameter(LONG_PARAM_NAME, PARAM_VALUE);
        assertEquals(transformer.getParameter(LONG_PARAM_NAME).toString(), PARAM_VALUE);
    }

    /**
     * Obtains transformer's parameter with the a name that wasn't set before.
     * Null is expected.
     * @throws TransformerConfigurationException If for some reason the
     *         TransformerHandler can not be created.
     */
    @Test
    public void clear02() throws TransformerConfigurationException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setParameter(LONG_PARAM_NAME, PARAM_VALUE);
        transformer.clearParameters();
        assertNull(transformer.getParameter(LONG_PARAM_NAME));
    }

    /**
     * Obtains transformer's parameter with a short name that set before. Value
     * should be same as set one.
     * @throws TransformerConfigurationException If for some reason the
     *         TransformerHandler can not be created.
     */
    @Test
    public void clear03() throws TransformerConfigurationException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer transformer = tfactory.newTransformer();

        transformer.setParameter(SHORT_PARAM_NAME, PARAM_VALUE);
        assertEquals(transformer.getParameter(SHORT_PARAM_NAME).toString(), PARAM_VALUE);
    }

    /**
     * Obtains transformer's parameter with a short name that set with an integer
     * object before. Value should be same as the set integer object.
     * @throws TransformerConfigurationException If for some reason the
     *         TransformerHandler can not be created.
     */
    @Test
    public void clear04() throws TransformerConfigurationException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();

        int intObject = 5;
        transformer.setParameter(SHORT_PARAM_NAME, intObject);
        assertEquals(transformer.getParameter(SHORT_PARAM_NAME), intObject);
    }

    /**
     * Obtains transformer's parameter whose initiated with a stream source with
     * the a name that set before. Value should be same as set one.
     * @throws TransformerConfigurationException If for some reason the
     *         TransformerHandler can not be created.
     */
    @Test
    public void clear05() throws TransformerConfigurationException {
        Transformer transformer = TransformerFactory.newInstance().
                newTransformer(new StreamSource(new File(XSL_FILE)));

        transformer.setParameter(LONG_PARAM_NAME, PARAM_VALUE);
        assertEquals(transformer.getParameter(LONG_PARAM_NAME), PARAM_VALUE);
    }

    /**
     * Obtains transformer's parameter whose initiated with a stream source with
     * the a name that wasn't set before. Null is expected.
     * @throws TransformerConfigurationException If for some reason the
     *         TransformerHandler can not be created.
     */
    @Test
    public void clear06() throws TransformerConfigurationException {
        Transformer transformer = TransformerFactory.newInstance().
                newTransformer(new StreamSource(new File(XSL_FILE)));
        transformer.setParameter(LONG_PARAM_NAME, PARAM_VALUE);
        transformer.clearParameters();
        assertNull(transformer.getParameter(LONG_PARAM_NAME));
    }

    /**
     * Obtains transformer's parameter whose initiated with a sax source with
     * the a name that set before. Value should be same as set one.
     * @throws Exception If any errors occur.
     */
    @Test
    public void clear07() throws Exception {
        try (FileInputStream fis = new FileInputStream(XSL_FILE)) {
            SAXSource saxSource = new SAXSource();
            saxSource.setInputSource(new InputSource(fis));

            Transformer transformer = TransformerFactory.newInstance().newTransformer(saxSource);
            transformer.setParameter(LONG_PARAM_NAME, PARAM_VALUE);
            assertEquals(transformer.getParameter(LONG_PARAM_NAME), PARAM_VALUE);
        }
    }

    /**
     * Obtains transformer's parameter whose initiated with a sax source with
     * the a name that wasn't set before. Null is expected.
     * @throws Exception If any errors occur.
     */
    @Test
    public void clear08() throws Exception {
        try (FileInputStream fis = new FileInputStream(XSL_FILE)) {
            SAXSource saxSource = new SAXSource();
            saxSource.setInputSource(new InputSource(fis));

            Transformer transformer = TransformerFactory.newInstance().newTransformer(saxSource);
            transformer.setParameter(LONG_PARAM_NAME, PARAM_VALUE);
            transformer.clearParameters();
            assertNull(transformer.getParameter(LONG_PARAM_NAME));
        }
    }

    /**
     * Obtains transformer's parameter whose initiated with a dom source with
     * the a name that set before. Value should be same as set one.
     * @throws Exception If any errors occur.
     */
    @Test
    public void clear09() throws Exception {
        TransformerFactory tfactory = TransformerFactory.newInstance();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(new File(XSL_FILE));
        DOMSource domSource = new DOMSource((Node)document);

        Transformer transformer = tfactory.newTransformer(domSource);

        transformer.setParameter(LONG_PARAM_NAME, PARAM_VALUE);
        assertEquals(transformer.getParameter(LONG_PARAM_NAME), PARAM_VALUE);
    }

    /**
     * Obtains transformer's parameter whose initiated with a dom source with
     * the a name that wasn't set before. Null is expected.
     * @throws Exception If any errors occur.
     */
    @Test
    public void clear10() throws Exception {
        TransformerFactory tfactory = TransformerFactory.newInstance();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(new File(XSL_FILE));
        DOMSource domSource = new DOMSource((Node)document);

        Transformer transformer = tfactory.newTransformer(domSource);
        transformer.setParameter(LONG_PARAM_NAME, PARAM_VALUE);
        transformer.clearParameters();
        assertNull(transformer.getParameter(LONG_PARAM_NAME));
    }
}
