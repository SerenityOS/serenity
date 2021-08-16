/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.jfr.internal.jfc;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import jdk.internal.org.xml.sax.InputSource;
import jdk.internal.org.xml.sax.SAXException;
import jdk.internal.util.xml.SAXParser;
import jdk.internal.util.xml.impl.SAXParserImpl;
import jdk.jfr.Configuration;

import jdk.jfr.internal.PrivateAccess;

/**
 * Parses a JDK Flight Recorder Configuration file (.jfc)
 */
final class JFCParser {
    static final String FILE_EXTENSION = ".jfc";
    private static final int MAXIMUM_FILE_SIZE = 1024 * 1024;

    public static Configuration createConfiguration(String name, Reader reader) throws IOException, ParseException {
        return createConfiguration(name, readContent(reader));
    }

    public static Configuration createConfiguration(String name, String content) throws IOException, ParseException {
        try {
            JFCParserHandler ch = new JFCParserHandler();
            parseXML(content, ch);
            return PrivateAccess.getInstance().newConfiguration(name, ch.label, ch.description, ch.provider, ch.settings, content);
        } catch (IllegalArgumentException iae) {
            throw new ParseException(iae.getMessage(), -1);
        } catch (SAXException e) {
            ParseException pe =  new ParseException("Error reading JFC file. " + e.getMessage(), -1);
            pe.initCause(e);
            throw pe;
        }
    }

    private static void parseXML(String content, JFCParserHandler ch) throws SAXException, IOException {
        CharArrayReader r = new CharArrayReader(content.toCharArray());
        SAXParser parser = new SAXParserImpl();
        parser.parse(new InputSource(r), ch);
    }

    private static String readContent(Reader r) throws IOException {
        CharArrayWriter writer = new CharArrayWriter(1024);
        int count = 0;
        int ch;
        while ((ch = r.read()) != -1) {
            writer.write(ch);
            count++;
            if (count >= MAXIMUM_FILE_SIZE) {
                throw new IOException("Presets with more than " + MAXIMUM_FILE_SIZE + " characters can't be read.");
            }
        }
        return new String(writer.toCharArray());
    }
}
