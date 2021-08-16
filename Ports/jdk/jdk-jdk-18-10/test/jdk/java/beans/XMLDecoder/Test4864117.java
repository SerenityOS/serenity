/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4864117
 * @summary Tests XMLDecoder within another DefaultHandler for SAX parser
 * @author Sergey Malenkov
 */

import java.beans.XMLDecoder;
import java.beans.ExceptionListener;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public final class Test4864117 extends DefaultHandler implements ExceptionListener {
    private static final String TEST = "test";
    private static final String DATA
            = "<test>\n"
            + " <void property=\"message\">\n"
            + "  <string>Hello, world!</string>\n"
            + " </void>\n"
            + "</test>";

    public static void main(String[] args) {
        Test4864117 test = new Test4864117();
        InputStream input = new ByteArrayInputStream(DATA.getBytes());
        Exception error = null;
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(input, test);
        }
        catch (ParserConfigurationException exception) {
            error = exception;
        }
        catch (SAXException exception) {
            error = exception.getException();
            if (error == null) {
                error = exception;
            }
        }
        catch (IOException exception) {
            error = exception;
        }
        if (error != null) {
            throw new Error("unexpected error", error);
        }
        test.print('?', test.getMessage());
    }

    private String message;

    public String getMessage() {
        if (this.message == null) {
            throw new Error("owner's method is not called");
        }
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
        print(':', this.message);
    }

    // DefaultHandler implementation

    private DefaultHandler handler;
    private int depth;

    @Override
    public void startDocument() throws SAXException {
        this.handler = XMLDecoder.createHandler(this, this, null);
        this.handler.startDocument();
    }

    @Override
    public void endDocument() {
        this.handler = null;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        print('>', qName);
        if (this.depth > 0) {
            this.handler.startElement(uri, localName, qName, attributes);
        } else if (!TEST.equals(qName)) {
            throw new SAXException("unexpected element name: " + qName);
        }
        this.depth++;
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        this.depth--;
        print('<', qName);
        if (this.depth > 0) {
            this.handler.endElement(uri, localName, qName);
        } else if (!TEST.equals(qName)) {
            throw new SAXException("unexpected element name: " + qName);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        this.handler.characters(ch, start, length);
    }

    public void exceptionThrown(Exception exception) {
        throw new Error("unexpected exception", exception);
    }

    private void print(char ch, String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.depth; i++) sb.append(' ');
        sb.append(ch).append(' ').append(name);
        System.out.println(sb.toString());
    }
}
