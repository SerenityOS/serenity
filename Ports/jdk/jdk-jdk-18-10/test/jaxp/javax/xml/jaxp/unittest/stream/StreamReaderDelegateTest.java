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

package stream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.StreamReaderDelegateTest
 * @run testng/othervm stream.StreamReaderDelegateTest
 * @summary Test StreamReaderDelegate.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class StreamReaderDelegateTest {

    /**
     * Tested xml file looks as below: <?xml version="1.0" standalone="no" ?>
     * <ns1:foo attr1="defaultAttr1" ns1:attr1="ns1Attr1" ns2:attr1="ns2Attr1"
     * attr2="defaultAttr2" attr3="defaultAttr3" xmlns:ns1="http://ns1.java.com"
     * xmlns:ns2="http://ns2.java.com"> <!--description--> content text
     * <![CDATA[<greeting>Hello</greeting>]]> other content </ns1:foo>
     **/
    @Test
    public void testAttribute() {
        StreamReaderDelegate delegate = null;
        try {
            System.out.println("===in testAttribute()===");
            XMLInputFactory ifac = XMLInputFactory.newFactory();
            XMLStreamReader reader = ifac.createXMLStreamReader(new FileInputStream(new File(getClass().getResource("testfile1.xml").getFile())));
            delegate = new StreamReaderDelegate(reader);

            Assert.assertTrue(delegate.standaloneSet());
            Assert.assertFalse(delegate.isStandalone());
            while (delegate.hasNext()) {
                delegate.next();
                if (delegate.getEventType() == XMLStreamConstants.START_ELEMENT || delegate.getEventType() == XMLStreamConstants.ATTRIBUTE) {
                    if (delegate.getLocalName().equals("foo")) {
                        Assert.assertTrue(delegate.getAttributeCount() == 5);
                        Assert.assertTrue(delegate.getAttributeType(1) == "CDATA");

                        Assert.assertTrue(delegate.getAttributeValue(0).equals("defaultAttr1"));
                        Assert.assertTrue(delegate.getAttributeValue(delegate.getAttributeCount() - 2).equals("defaultAttr2"));
                        Assert.assertTrue(delegate.getAttributeValue(delegate.getAttributeCount() - 1).equals("defaultAttr3"));

                        Assert.assertTrue(delegate.getAttributeValue("http://ns1.java.com", "attr1").equals("ns1Attr1"));
                        Assert.assertTrue(delegate.getAttributeValue("http://ns2.java.com", "attr1").equals("ns2Attr1"));

                        Assert.assertTrue(delegate.getAttributeValue(null, "attr2").equals("defaultAttr2"));
                        Assert.assertTrue(delegate.getAttributeValue(null, "attr3").equals("defaultAttr3"));

                        Assert.assertTrue(delegate.getAttributeNamespace(0) == null);
                        Assert.assertTrue(delegate.getAttributeNamespace(1).equals("http://ns1.java.com"));
                        Assert.assertTrue(delegate.getAttributePrefix(1).equals("ns1"));
                        Assert.assertTrue(delegate.getAttributeName(1).toString()
                                .equals("{" + delegate.getAttributeNamespace(1) + "}" + delegate.getAttributeLocalName(1)));
                        Assert.assertTrue(delegate.getAttributeLocalName(1).equals("attr1"));

                        // negative test. Should return null for out of
                        // attribute array index
                        Assert.assertTrue(delegate.getAttributeNamespace(delegate.getAttributeCount()) == null);
                        Assert.assertTrue(delegate.getAttributePrefix(delegate.getAttributeCount()) == null);
                        Assert.assertTrue(delegate.getAttributeName(delegate.getAttributeCount()) == null);
                        Assert.assertTrue(delegate.getAttributeLocalName(delegate.getAttributeCount()) == null);
                        Assert.assertTrue(delegate.getAttributeType(delegate.getAttributeCount()) == null);
                    }
                } else {
                    try {
                        delegate.getAttributeCount();
                    } catch (IllegalStateException e) {
                        System.out.println("expected exception for incorrect event type");
                    }
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail("FileNotFoundException in testAttribute()");
        } catch (XMLStreamException e) {
            e.printStackTrace();
            System.out.println(delegate.getLocation());
            Assert.fail("XMLStreamException in testAttribute()");
        } catch (FactoryConfigurationError e) {
            e.printStackTrace();
            Assert.fail("FactoryConfigurationError in testAttribute()");
        } finally {
            try {
                delegate.close();
            } catch (XMLStreamException e) {
                e.printStackTrace();
                Assert.fail("XMLStreamException in testAttribute()");
            }
        }
    }

    /**
     * Tested xml file looks as below: <?xml version="1.0" encoding="UTF-8"?>
     * <ns1:foo xmlns:ns="http://ns1.java.com" xmlns:ns1="http://ns1.java.com"
     * xmlns:ns2="http://ns2.java.com" > <!--description-->content text
     * <![CDATA[<greeting>Hello</greeting>]]> other content </ns1:foo>
     **/
    @Test
    public void testNamespace() {
        StreamReaderDelegate delegate = null;
        try {
            System.out.println("===in testNamespace()===");
            XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(
                    new FileInputStream(new File(getClass().getResource("testfile2.xml").getFile())));
            delegate = new StreamReaderDelegate();
            delegate.setParent(reader);
            while (delegate.hasNext()) {
                delegate.next();
                if (delegate.getEventType() == XMLStreamConstants.START_ELEMENT || delegate.getEventType() == XMLStreamConstants.ATTRIBUTE) {

                    if (delegate.getName().getLocalPart().equals("foo")) {
                        Assert.assertTrue(("{" + delegate.getNamespaceURI(delegate.getPrefix()) + "}" + delegate.getLocalName()).equals(delegate.getName()
                                .toString()));
                        System.out.println(delegate.getLocation());

                        Assert.assertTrue(delegate.getNamespaceCount() == 3);
                        Assert.assertTrue(delegate.getNamespaceURI().equals("http://ns1.java.com"));
                        Assert.assertTrue(delegate.getNamespaceURI(2).equals("http://ns2.java.com"));
                        Assert.assertTrue(delegate.getNamespaceURI("ns").equals("http://ns1.java.com"));

                        Assert.assertTrue(delegate.getNamespacePrefix(1).equals("ns1"));

                        NamespaceContext nsCtx = delegate.getNamespaceContext();
                        nsCtx.getNamespaceURI("ns");
                        Iterator prefixes = nsCtx.getPrefixes("http://ns1.java.com");
                        boolean hasns = false;
                        boolean hasns1 = false;
                        while (prefixes.hasNext()) {
                            String prefix = (String) prefixes.next();
                            if (prefix.equals("ns")) {
                                hasns = true;
                            } else if (prefix.equals("ns1")) {
                                hasns1 = true;
                            }
                        }
                        Assert.assertTrue(hasns && hasns1);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail("FileNotFoundException in testNamespace()");
        } catch (XMLStreamException e) {
            e.printStackTrace();
            System.out.println(delegate.getLocation());
            Assert.fail("XMLStreamException in testNamespace()");
        } catch (FactoryConfigurationError e) {
            e.printStackTrace();
            Assert.fail("FactoryConfigurationError in testNamespace()");
        } finally {
            try {
                delegate.close();
            } catch (XMLStreamException e) {
                e.printStackTrace();
                Assert.fail("XMLStreamException in testNamespace()");
            }
        }
    }

    /**
     * <?xml version="1.0" encoding="utf-8" ?> <ns1:foo
     * xmlns:ns1="http://ns1.java.com" xmlns:ns2="http://ns2.java.com">
     * <!--description--> content text <![CDATA[<greeting>Hello</greeting>]]>
     * other content </ns1:foo>
     **/
    @Test
    public void testText() {
        String property = "javax.xml.stream.isCoalescing";
        System.out.println("===in testText()====");
        StreamReaderDelegate delegate = null;
        try {
            XMLInputFactory ifac = XMLInputFactory.newFactory();
            ifac.setProperty(property, Boolean.TRUE);
            XMLStreamReader reader = ifac.createXMLStreamReader(new FileInputStream(new File(getClass().getResource("testfile3.xml").getFile())), "iso8859-1");
            delegate = new StreamReaderDelegate();
            delegate.setParent(reader);

            Assert.assertTrue(delegate.getParent().equals(reader));
            Assert.assertTrue(delegate.getProperty(property).equals(Boolean.TRUE));
            Assert.assertTrue(delegate.getCharacterEncodingScheme().equalsIgnoreCase("utf-8"));
            Assert.assertTrue(delegate.getEncoding().equalsIgnoreCase("iso8859-1"));
            Assert.assertTrue(delegate.getVersion().equals("1.0"));
            while (delegate.hasNext()) {
                delegate.next();
                if (delegate.getEventType() == XMLStreamConstants.CHARACTERS) {
                    char[] target1 = new char[delegate.getTextLength()];
                    delegate.getTextCharacters(delegate.getTextStart(), target1, 0, target1.length);
                    char[] target2 = delegate.getTextCharacters();

                    Assert.assertTrue(delegate.getText().trim().equals(new String(target1).trim()));
                    Assert.assertTrue(delegate.getText().trim().equals(new String(target2).trim()));
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail("FileNotFoundException in testText()");
        } catch (XMLStreamException e) {
            e.printStackTrace();
            System.out.println(delegate.getLocation());
            Assert.fail("XMLStreamException in testText()");
        } catch (FactoryConfigurationError e) {
            e.printStackTrace();
            Assert.fail("FactoryConfigurationError in testText()");
        } finally {
            try {
                delegate.close();
            } catch (XMLStreamException e) {
                e.printStackTrace();
                Assert.fail("XMLStreamException in testText()");
            }
        }
    }

    @Test
    public void testWhiteSpace() {
        System.out.println("===in testWhiteSpace()===");
        StreamReaderDelegate delegate = null;
        try {
            XMLInputFactory ifac = XMLInputFactory.newFactory();
            ifac.setProperty("javax.xml.stream.isCoalescing", Boolean.TRUE);
            XMLStreamReader reader = ifac.createXMLStreamReader(new FileInputStream(new File(getClass().getResource("testfile4.xml").getFile())));

            delegate = new StreamReaderDelegate();
            delegate.setParent(reader);
            while (delegate.hasNext()) {
                int i = delegate.next();
                switch (i) {
                    case XMLStreamConstants.CHARACTERS: {
                        Assert.assertTrue(delegate.isCharacters());
                        Assert.assertTrue(delegate.hasText());
                        Assert.assertTrue(delegate.isWhiteSpace());
                        break;
                    }
                    case XMLStreamConstants.START_ELEMENT: {
                        Assert.assertTrue(delegate.isStartElement());
                        Assert.assertTrue(delegate.isAttributeSpecified(0));
                        Assert.assertTrue(delegate.hasName());
                        delegate.require(XMLStreamConstants.START_ELEMENT, delegate.getNamespaceURI(), delegate.getLocalName());
                        break;
                    }
                    case XMLStreamConstants.END_ELEMENT: {
                        Assert.assertTrue(delegate.isEndElement());
                        Assert.assertTrue(delegate.hasName());
                        delegate.require(XMLStreamConstants.END_ELEMENT, delegate.getNamespaceURI(), delegate.getLocalName());
                        break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail("FileNotFoundException in testWhiteSpace()");
        } catch (XMLStreamException e) {
            e.printStackTrace();
            System.out.println(delegate.getLocation());
            Assert.fail("XMLStreamException in testWhiteSpace()");
        } catch (FactoryConfigurationError e) {
            e.printStackTrace();
            Assert.fail("FactoryConfigurationError in testWhiteSpace()");
        } finally {
            try {
                delegate.close();
            } catch (XMLStreamException e) {
                e.printStackTrace();
                Assert.fail("XMLStreamException in testWhitespace()");
            }
        }

    }

    @Test
    public void testElementText() {
        System.out.println("===in testElementText()===");
        StreamReaderDelegate delegate = null;
        try {
            XMLInputFactory ifac = XMLInputFactory.newFactory();
            XMLStreamReader reader = ifac.createXMLStreamReader(new FileInputStream(new File(getClass().getResource("toys.xml").getFile())));

            delegate = new StreamReaderDelegate();
            delegate.setParent(reader);
            while (delegate.hasNext()) {
                if (delegate.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    if (delegate.getLocalName().equals("name") || delegate.getLocalName().equals("price")) {
                        System.out.println(delegate.getElementText());
                    }
                    delegate.nextTag();
                } else {
                    delegate.next();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail("FileNotFoundException in testElementText()");
        } catch (XMLStreamException e) {
            e.printStackTrace();
            System.out.println(delegate.getLocation());
            Assert.fail("XMLStreamException in testElementText()");
        } catch (FactoryConfigurationError e) {
            e.printStackTrace();
            Assert.fail("FactoryConfigurationError in testElementText()");
        } finally {
            try {
                delegate.close();
            } catch (XMLStreamException e) {
                e.printStackTrace();
                Assert.fail("XMLStreamException in testElementText()");
            }
        }
    }

    @Test
    public void testPITargetAndData() {
        System.out.println("===in testPITargetAndData()===");
        StreamReaderDelegate delegate = null;
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            String PITarget = "soffice";
            String PIData = "WebservicesArchitecture";
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<?" + PITarget + " " + PIData + "?>" + "<foo></foo>";
            InputStream is = new java.io.ByteArrayInputStream(xml.getBytes());
            XMLStreamReader sr = xif.createXMLStreamReader(is);
            delegate = new StreamReaderDelegate(sr);
            while (delegate.hasNext()) {
                int eventType = delegate.next();
                if (eventType == XMLStreamConstants.PROCESSING_INSTRUCTION) {
                    String target = delegate.getPITarget();
                    String data = delegate.getPIData();
                    Assert.assertTrue(target.equals(PITarget));
                    Assert.assertTrue(data.equals(PIData));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Exception in testPITargetAndData()");
        } finally {
            try {
                delegate.close();
            } catch (XMLStreamException e) {
                e.printStackTrace();
                Assert.fail("XMLStreamException in testPITargetAndData()");
            }
        }
    }
}
