/*
 * Copyright (c) 1999, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4160195 7026346
 * @summary Check for correct detection of XML content type, including BOM streams
 */

import java.io.*;
import java.net.*;


public class GetXmlContentType {

    static final String XML_MIME_TYPE = "application/xml";
    static final String XML_HEADER = "<?xml";
    static int passed, failed;

    // guess type from content and filename
    static final String goodFiles [] = {
        "xml1",         // US-ASCII and supersets
        "xml2.xml",     // typed inferred from filename
        "xml3",         // UTF-16 little endian (partial)
        "xml4"          // UTF-16 big endian (partial)
        };

    // some common non-XML examples
    static final String badFiles [] = {
        "not-xml1",
        "not-xml2"
        };

    public static void main(String[] args) throws Exception {
        contentTypeFromFile();
        contentTypeFromBOMStream();

        if (failed > 0)
            throw new RuntimeException (
                "Test failed; passed = " + passed + ", failed = " + failed);
    }

    static void contentTypeFromFile() throws Exception {
        // POSITIVE tests:  good data --> good result

        for (String goodFile : goodFiles) {
            String result = getUrlContentType(goodFile);

            if (!XML_MIME_TYPE.equals(result)) {
                System.out.println("Wrong MIME type: " + goodFile + " --> " + result);
                failed++;
            } else {
                passed++;
            }
        }

        // NEGATIVE tests:  bad data --> correct diagnostic
        for (String badFile : badFiles) {
            String result = getUrlContentType(badFile);

            if (XML_MIME_TYPE.equals(result)) {
                System.out.println("Wrong MIME type: " + badFile + " --> " + result);
                failed++;
            } else {
                passed++;
            }
        }
    }

    static String getUrlContentType(String name) throws IOException {
        File file = new File(System.getProperty("test.src", "."), "xml");
        URL u = new URL("file:"
                         + file.getCanonicalPath()
                         + file.separator
                         + name);
        URLConnection conn = u.openConnection();

        return conn.getContentType();
    }

    static void contentTypeFromBOMStream() throws Exception {
        final String[] encodings = new  String[]
                {"UTF-8", "UTF-16BE", "UTF-16LE", "UTF-32BE", "UTF-32LE"};
        for (String encoding : encodings) {
             try (InputStream is = new ByteArrayInputStream(toBOMBytes(encoding))) {
                 String mime = URLConnection.guessContentTypeFromStream(is);
                 if ( !XML_MIME_TYPE.equals(mime) ) {
                     System.out.println("Wrong MIME type: " + encoding + " --> " + mime);
                     failed++;
                 } else {
                     passed++;
                 }
             }
         }
    }

    static byte[] toBOMBytes(String encoding) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        switch (encoding) {
            case "UTF-8" :
                bos.write(new  byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
                break;
            case "UTF-16BE" :
                bos.write(new  byte[] { (byte) 0xFE, (byte) 0xFF });
                break;
            case "UTF-16LE" :
                bos.write(new  byte[] { (byte) 0xFF, (byte) 0xFE });
                break;
            case "UTF-32BE" :
                bos.write(new  byte[] { (byte) 0x00, (byte) 0x00,
                                        (byte) 0xFE, (byte) 0xFF });
                break;
            case "UTF-32LE" :
                bos.write(new  byte[] { (byte) 0xFF, (byte) 0xFE,
                                        (byte) 0x00, (byte) 0x00 });
        }

        bos.write(XML_HEADER.getBytes(encoding));
        return bos.toByteArray();
    }
}
