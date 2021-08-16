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

package transform;

import static jaxp.library.JAXPTestUtilities.USER_DIR;

import java.io.File;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import transform.util.DOMUtil;
import transform.util.SAXUtil;
import transform.util.StAXUtil;
import transform.util.StreamUtil;

/*
 * @test
 * @bug 4892774
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.Bug4892774
 * @run testng/othervm transform.Bug4892774
 * @summary Test identity transformer with all possible types of Source and Result combinations for doucment version and encoding information.
 */

@Listeners({jaxp.library.FilePolicy.class})
public class Bug4892774 {

    private final String XML_FILE = "catalog.xml";
    private final String XML10_FILE = "catalog_10.xml"; // 1.0 version document
    private final String TEMP_FILE = USER_DIR + "tmp.xml";
    private final String EXPECTED_VERSION = "1.1";
    static private Transformer idTransform = null;

    private static DOMUtil domUtil = null;
    private static StreamUtil streamUtil = null;
    private static SAXUtil saxUtil = null;
    private static StAXUtil staxUtil = null;

    @BeforeMethod
    public void setUp() {
        File tmpFile = new File(TEMP_FILE);
        if (tmpFile.exists())
            tmpFile.delete();
        try {

            if (idTransform == null)
                idTransform = getIdTransformer();
            else
                idTransform.reset();

            initializeUtils();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured during setUp(): " + e.getMessage());
        }
    }

    @AfterMethod
    public void tearDown() {
        File tmpFile = new File(TEMP_FILE);
        if (tmpFile.exists())
            tmpFile.delete();
    }

    private void initializeUtils() throws Exception {
        if (domUtil == null)
            domUtil = (DOMUtil) TransformerUtilFactory.getUtil(TransformerUtilFactory.DOM);
        if (saxUtil == null)
            saxUtil = (SAXUtil) TransformerUtilFactory.getUtil(TransformerUtilFactory.SAX);
        if (streamUtil == null)
            streamUtil = (StreamUtil) TransformerUtilFactory.getUtil(TransformerUtilFactory.STREAM);
        if (staxUtil == null)
            staxUtil = (StAXUtil) TransformerUtilFactory.getUtil(TransformerUtilFactory.StAX);
    }

    @Test
    public void testDOM2DOM() {
        try {
            Source input = domUtil.prepareSource(this.getClass().getResourceAsStream(XML_FILE));
            DOMResult domResult = (DOMResult) domUtil.prepareResult();
            idTransform.transform(input, domResult);
            domUtil.checkResult(domResult, EXPECTED_VERSION);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    private Transformer getIdTransformer() throws Exception {
        return TransformerFactory.newInstance().newTransformer();
    }

    @Test
    public void testDOM2Stream() {
        try {

            Source input = domUtil.prepareSource(this.getClass().getResourceAsStream(XML_FILE));
            StreamResult strResult = (StreamResult) streamUtil.prepareResult();
            idTransform.transform(input, strResult);
            streamUtil.checkResult(strResult, EXPECTED_VERSION, "UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testDOM2SAX() {
        try {
            Source input = domUtil.prepareSource(this.getClass().getResourceAsStream(XML_FILE));
            SAXResult saxResult = (SAXResult) saxUtil.prepareResult();
            idTransform.transform(input, saxResult);
            saxUtil.checkResult(saxResult, EXPECTED_VERSION, "UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testDOM2StAX() {
        try {
            Source input = domUtil.prepareSource(this.getClass().getResourceAsStream(XML_FILE));
            StAXResult staxResult = (StAXResult) staxUtil.prepareResult();
            idTransform.transform(input, staxResult);
            staxUtil.checkResult(staxResult, EXPECTED_VERSION, "UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testDOM2StAXStream() {
        try {
            Source input = domUtil.prepareSource(this.getClass().getResourceAsStream(XML_FILE));
            StAXResult staxResult = (StAXResult) staxUtil.prepareStreamResult();
            idTransform.transform(input, staxResult);
            staxUtil.checkStreamResult(staxResult, EXPECTED_VERSION);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testSAX2DOM() {
        try {
            Source input = saxUtil.prepareSource(this.getClass().getResourceAsStream(XML_FILE));
            DOMResult domResult = (DOMResult) domUtil.prepareResult();
            idTransform.transform(input, domResult);
            domUtil.checkResult(domResult, EXPECTED_VERSION);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testSAX2SAX() {
        try {
            Source input = saxUtil.prepareSource(this.getClass().getResourceAsStream(XML_FILE));
            SAXResult saxResult = (SAXResult) saxUtil.prepareResult();
            idTransform.transform(input, saxResult);
            saxUtil.checkResult(saxResult, EXPECTED_VERSION, "UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testSAX2Stream() {
        try {
            Source input = saxUtil.prepareSource(this.getClass().getResourceAsStream(XML_FILE));
            StreamResult strResult = (StreamResult) streamUtil.prepareResult();
            idTransform.transform(input, strResult);
            streamUtil.checkResult(strResult, EXPECTED_VERSION, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testSAX2StAX() {
        try {
            Source input = saxUtil.prepareSource(this.getClass().getResourceAsStream(XML_FILE));
            StAXResult staxResult = (StAXResult) staxUtil.prepareResult();
            idTransform.transform(input, staxResult);
            staxUtil.checkResult(staxResult, EXPECTED_VERSION, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testSAX2StAXStream() {
        try {
            Source input = saxUtil.prepareSource(this.getClass().getResourceAsStream(XML_FILE));
            StAXResult staxResult = (StAXResult) staxUtil.prepareStreamResult();
            idTransform.transform(input, staxResult);
            staxUtil.checkStreamResult(staxResult, EXPECTED_VERSION);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testStream2DOM() {
        try {
            Source input = streamUtil.prepareSource(this.getClass().getResourceAsStream(XML_FILE));
            DOMResult domResult = (DOMResult) domUtil.prepareResult();
            idTransform.transform(input, domResult);
            domUtil.checkResult(domResult, EXPECTED_VERSION);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testStream2Stream() {
        try {
            Source input = streamUtil.prepareSource(this.getClass().getResourceAsStream(XML_FILE));
            StreamResult strResult = (StreamResult) streamUtil.prepareResult();
            idTransform.transform(input, strResult);
            streamUtil.checkResult(strResult, EXPECTED_VERSION, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testStream2Stax() {
        try {
            Source input = streamUtil.prepareSource(this.getClass().getResourceAsStream(XML_FILE));
            StAXResult staxResult = (StAXResult) staxUtil.prepareResult();
            idTransform.transform(input, staxResult);
            staxUtil.checkResult(staxResult, EXPECTED_VERSION, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testStream2StaxStream() {
        try {
            Source input = streamUtil.prepareSource(this.getClass().getResourceAsStream(XML_FILE));
            StAXResult staxResult = (StAXResult) staxUtil.prepareStreamResult();
            idTransform.transform(input, staxResult);
            staxUtil.checkStreamResult(staxResult, EXPECTED_VERSION);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testStream2SAX() {
        try {
            Source input = streamUtil.prepareSource(this.getClass().getResourceAsStream(XML_FILE));
            SAXResult saxResult = (SAXResult) saxUtil.prepareResult();
            idTransform.transform(input, saxResult);
            saxUtil.checkResult(saxResult, EXPECTED_VERSION, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testStAX2DOM() {
        try {
            Source input = staxUtil.prepareStreamSource(this.getClass().getResourceAsStream(XML10_FILE));
            DOMResult domResult = (DOMResult) domUtil.prepareResult();
            idTransform.transform(input, domResult);
            domUtil.checkResult(domResult, "1.0");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testStAX2Stream() {
        try {
            Source input = staxUtil.prepareStreamSource(this.getClass().getResourceAsStream(XML10_FILE));
            StreamResult strResult = (StreamResult) streamUtil.prepareResult();
            idTransform.transform(input, strResult);
            streamUtil.checkResult(strResult, "1.0", "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testStAX2StAX() {
        try {
            Source input = staxUtil.prepareStreamSource(this.getClass().getResourceAsStream(XML10_FILE));
            StAXResult staxResult = (StAXResult) staxUtil.prepareResult();
            idTransform.transform(input, staxResult);
            staxUtil.checkResult(staxResult, "1.0", "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testStAXEvent2DOM() {
        try {
            Source input = staxUtil.prepareSource(this.getClass().getResourceAsStream(XML10_FILE));
            DOMResult domResult = (DOMResult) domUtil.prepareResult();
            idTransform.transform(input, domResult);
            domUtil.checkResult(domResult, "1.0");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testStAXEvent2Stream() {
        try {
            Source input = staxUtil.prepareSource(this.getClass().getResourceAsStream(XML10_FILE));
            StreamResult strResult = (StreamResult) streamUtil.prepareResult();
            idTransform.transform(input, strResult);
            streamUtil.checkResult(strResult, "1.0", "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }
}
