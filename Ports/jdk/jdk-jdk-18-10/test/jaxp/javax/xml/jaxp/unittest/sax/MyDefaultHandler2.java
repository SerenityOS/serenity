/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package sax;

import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.Attributes2Impl;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.ext.Locator2;
import org.xml.sax.ext.Locator2Impl;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderAdapter;

public class MyDefaultHandler2 extends DefaultHandler2 {
    Locator2Impl locator = new Locator2Impl();
    StringBuffer currentValue = new StringBuffer();
    String version = "customVersion";
    String encoding = "customEncoding";

    public void setDocumentLocator(Locator locator) {
        this.locator = new Locator2Impl((Locator2) locator);
        this.locator.setXMLVersion(version);
        this.locator.setEncoding(encoding);
    }

    public void startDocument() throws SAXException {
        super.startDocument();
        System.out.println("startDocument() is invoked");
        System.out.println(locator.getXMLVersion());
        System.out.println(locator.getEncoding());
    }

    public void attributeDecl(String ename, String aname, String type, String mode, String value) throws SAXException {
        super.attributeDecl(ename, aname, type, mode, value);
        System.out.println("attributeDecl() is invoked for attr :" + aname);
    }

    public void elementDecl(String name, String model) throws SAXException {
        super.elementDecl(name, model);
        System.out.println("elementDecl() is invoked for element : " + name);
    }

    public void internalEntityDecl(String name, String value) throws SAXException {
        super.internalEntityDecl(name, value);
        System.out.println("internalEntityDecl() is invoked for entity : " + name);
    }

    public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
        super.externalEntityDecl(name, publicId, systemId);
        System.out.println("externalEntityDecl() is invoked for entity : " + name);
    }

    public void comment(char[] ch, int start, int length) throws SAXException {
        super.comment(ch, start, length);
        System.out.println(new String(ch, start, length));
    }

    public void endDocument() throws SAXException {
        super.endDocument();
        System.out.println("\nendDocument() is invoked");
    }

    public void startCDATA() throws SAXException {
        super.startCDATA();
        System.out.println("startCDATA() is invoked");
    }

    public void endCDATA() throws SAXException {
        super.endCDATA();
        System.out.println("endCDATA() is invoked");
    }

    public void startEntity(String name) throws SAXException {
        super.startEntity(name);
        // System.out.println("startEntity() is invoked for entity : " + name) ;
    }

    public void endEntity(String name) throws SAXException {
        super.endEntity(name);
        // System.out.println("endEntity() is invoked for entity : " + name) ;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);
        if (qName.equals("toy")) {
            Attributes2Impl impl = new Attributes2Impl();
            impl.setAttributes(attributes);
            System.out.println("\ntoy id=" + impl.getValue("id"));
        } else if (qName.equals("price") || qName.equals("name")) {
            System.out.print("       " + qName + " : ");
            currentValue = new StringBuffer();
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if (qName.equals("price") || qName.equals("name")) {
            System.out.print(currentValue.toString());
        }
    }

    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        super.startDTD(name, publicId, systemId);
        System.out.println("startDTD() is invoked");
    }

    public void endDTD() throws SAXException {
        super.endDTD();
        System.out.println("endDTD() is invoked");
    }

    public void characters(char[] ch, int start, int length) {
        // System.out.println(start + " " + length) ;
        currentValue.append(ch, start, length);
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        System.out.println("resolveEntity(publicId, systemId) is invoked");
        return super.resolveEntity(publicId, systemId);
    }

    public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId) throws SAXException, IOException {
        System.out.println("resolveEntity(name, publicId, baseURI, systemId) is invoked");
        return super.resolveEntity(name, publicId, baseURI, systemId);
    }

    public InputSource getExternalSubset(String name, String baseURI) throws SAXException, IOException {
        System.out.println("getExternalSubset() is invoked");
        return super.getExternalSubset(name, baseURI);
    }

    public void startPrefixMapping(String prefix, String uri) {
        System.out.println("startPrefixMapping() is invoked for " + prefix + " : " + uri);
        try {
            new XMLReaderAdapter().startPrefixMapping(prefix, uri);
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    public void endPrefixMapping(String prefix) {
        System.out.println("\nendPrefixMapping() is invoked for " + prefix);
        try {
            new XMLReaderAdapter().endPrefixMapping(prefix);
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    public void skippedEntity(String name) {
        try {
            System.out.println("skippedEntity() is invoked for : " + name);
            new XMLReaderAdapter().skippedEntity(name);
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    public void error(SAXParseException e) throws SAXException {
        System.out.println("error() is invoked for in ErrorHandler");
        new XMLFilterImpl().warning(e);
    }

    public void fatalError(SAXParseException e) throws SAXException {
        System.out.println("fatalError() is invoked for in ErrorHandler");
        new XMLFilterImpl().warning(e);
    }

    public void warning(SAXParseException e) throws SAXException {
        System.out.println("warning() is invoked for in ErrorHandler");
        new XMLFilterImpl().warning(e);
    }

}
