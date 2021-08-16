/*
 * Copyright 2014 Google, Inc.  All Rights Reserved.
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

package parse.jdk7156085;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.testng.annotations.Test;

/**
 * JDK-7156085: ArrayIndexOutOfBoundsException throws in UTF8Reader of SAXParser
 * https://bugs.openjdk.java.net/browse/JDK-7156085
 *
 * XERCESJ-1257: buffer overflow in UTF8Reader for characters out of BMP
 * https://issues.apache.org/jira/browse/XERCESJ-1257
 */
public class UTF8ReaderBug {
    @Test
    public void shouldAcceptSupplementaryCharacters() throws Throwable {
        StringBuilder b = new StringBuilder("<xml>");
        for(int i = 5; i < 8223; i++) {
            b.append(' ');
        }
        // Add surrogate characters which overflow the buffer. This shows the need to place an
        // overflow check at --
        // com.sun.org.apache.xerces.internal.impl.io.UTF8Reader.read(UTF8Reader.java:544)
        b.append("\uD835\uDC37");
        b.append("</xml>");
        sendToParser(b.toString());
    }

    private static void sendToParser(String b) throws Throwable {
        byte[] input = b.getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(input);

        SAXParserFactory  spf = SAXParserFactory.newInstance();
        SAXParser p = spf.newSAXParser();
        p.parse(new InputSource(in), new DefaultHandler());
    }
}
