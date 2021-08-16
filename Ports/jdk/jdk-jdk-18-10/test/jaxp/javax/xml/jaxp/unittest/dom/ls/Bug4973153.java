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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 4973153
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.ls.Bug4973153
 * @run testng/othervm dom.ls.Bug4973153
 * @summary Test LSSerialiser.setEncoding() raises 'unsupported-encoding' error if encoding is invalid.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug4973153 {

    DOMImplementationLS implLS = null;
    public String xml1 = "<?xml version=\"1.0\"?><ROOT><ELEMENT1></ELEMENT1><ELEMENT2></ELEMENT2></ROOT>";

    @Test
    public void testOne() {
        LSParser db = createLSParser();
        if (db == null) {
            System.out.println("Unable to create LSParser !");
            return;
        }
        LSSerializer dw = createLSSerializer();
        if (dw == null) {
            System.out.println("Unable to create LSSerializer!");
            return;
        }

        DOMErrorHandlerImpl eh = new DOMErrorHandlerImpl();
        dw.getDomConfig().setParameter("error-handler", eh);
        Document doc = db.parse(getXml1Source());

        Output out = new Output();
        out.setByteStream(new ByteArrayOutputStream());
        out.setEncoding("WrOnG_EnCoDiNg");
        try {
            if (dw.write(doc, out)) {
                System.out.println("Expected result value - false");
                return;
            }
        } catch (Exception ex) {
            // This is bad.
        }
        if (!eh.WrongEncodingErrorReceived) {
            Assert.fail("'unsupported-encoding' error was expected ");
            return;
        }
        System.out.println("OKAY");
        return;
    }

    @BeforeMethod
    public void setUp() {
        Document doc = null;
        DocumentBuilder parser = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            parser = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        StringBufferInputStream is = new StringBufferInputStream(xml1);
        try {
            doc = parser.parse(is);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DOMImplementation impl = doc.getImplementation();
        implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
    }

    @AfterMethod
    public void tearDown() {
        implLS = null;
    }

    public LSParser createLSParser() {
        return implLS.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, "http://www.w3.org/2001/XMLSchema");
    }

    public LSSerializer createLSSerializer() {
        return implLS.createLSSerializer();
    }

    public LSInput createLSInput() {
        return implLS.createLSInput();
    }

    public LSInput getXml1Source() {
        LSInput src = createLSInput();
        src.setStringData(xml1);
        return src;
    }
}

class Output implements LSOutput {
    OutputStream bs;
    Writer cs;
    String sId;
    String enc;

    public Output() {
        bs = null;
        cs = null;
        sId = null;
        enc = "UTF-8";
    }

    public OutputStream getByteStream() {
        return bs;
    }

    public void setByteStream(OutputStream byteStream) {
        bs = byteStream;
    }

    public Writer getCharacterStream() {
        return cs;
    }

    public void setCharacterStream(Writer characterStream) {
        cs = characterStream;
    }

    public String getSystemId() {
        return sId;
    }

    public void setSystemId(String systemId) {
        sId = systemId;
    }

    public String getEncoding() {
        return enc;
    }

    public void setEncoding(String encoding) {
        enc = encoding;
    }
}

class DOMErrorHandlerImpl implements DOMErrorHandler {
    boolean NoOutputSpecifiedErrorReceived = false;
    boolean WrongEncodingErrorReceived = false;

    public boolean handleError(DOMError error) {
        if ("no-output-specified".equalsIgnoreCase(error.getType())) {
            NoOutputSpecifiedErrorReceived = true;
        } else if ("unsupported-encoding".equalsIgnoreCase(error.getType())) {
            WrongEncodingErrorReceived = true;
        }
        return true;
    }
}
