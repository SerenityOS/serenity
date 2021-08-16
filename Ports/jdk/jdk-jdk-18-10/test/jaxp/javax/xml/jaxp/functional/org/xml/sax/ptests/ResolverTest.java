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
package org.xml.sax.ptests;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static jaxp.library.JAXPTestUtilities.USER_DIR;
import static jaxp.library.JAXPTestUtilities.compareWithGold;
import static org.testng.Assert.assertTrue;
import static org.xml.sax.ptests.SAXTestConst.GOLDEN_DIR;
import static org.xml.sax.ptests.SAXTestConst.XML_DIR;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Entity resolver should be invoked in XML parse. This test verifies parsing
 * process by checking the output with golden file.
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.xml.sax.ptests.ResolverTest
 * @run testng/othervm org.xml.sax.ptests.ResolverTest
 */
@Test
@Listeners({jaxp.library.FilePolicy.class})
public class ResolverTest {
    /**
     * Unit test for entityResolver setter.
     *
     * @throws Exception If any errors occur.
     */
    public void testResolver() throws Exception {
        String outputFile = USER_DIR + "EntityResolver.out";
        String goldFile = GOLDEN_DIR + "EntityResolverGF.out";
        String xmlFile = XML_DIR + "publish.xml";

        Files.copy(Paths.get(XML_DIR + "publishers.dtd"),
                Paths.get(USER_DIR + "publishers.dtd"), REPLACE_EXISTING);
        Files.copy(Paths.get(XML_DIR + "familytree.dtd"),
                Paths.get(USER_DIR + "familytree.dtd"), REPLACE_EXISTING);

        try(FileInputStream instream = new FileInputStream(xmlFile);
                MyEntityResolver eResolver = new MyEntityResolver(outputFile)) {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setEntityResolver(eResolver);
            InputSource is = new InputSource(instream);
            xmlReader.parse(is);
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }
}

/**
 * Simple entity resolver to write every entity to an output file.
 */
class MyEntityResolver extends XMLFilterImpl implements AutoCloseable {
    /**
     * FileWriter to write string to output file.
     */
    private final BufferedWriter bWriter;

    /**
     * Initiate FileWriter when construct a MyContentHandler.
     * @param outputFileName output file name.
     * @throws SAXException creation of FileWriter failed.
     */
    MyEntityResolver(String outputFileName) throws SAXException {
        super();
        try {
            bWriter = new BufferedWriter(new FileWriter(outputFileName));
        } catch (IOException ex) {
            throw new SAXException(ex);
        }
    }

    /**
     * Write In resolveEntity tag along with publicid and systemId when meet
     * resolveEntity event.
     * @throws IOException error happen when writing file.
     */
    @Override
    public InputSource resolveEntity(String publicid, String systemid)
            throws SAXException, IOException {
        String str = "In resolveEntity.." + " " + publicid + " " + getFileName(systemid);
        bWriter.write( str, 0,str.length());
        bWriter.newLine();
        return super.resolveEntity(publicid, systemid);
    }

    /**
     * Flush the content and close the file.
     * @throws IOException error happen when writing file or closing file.
     */
    @Override
    public void close() throws IOException {
        bWriter.flush();
        bWriter.close();
    }

    private String getFileName(String systemid) {
        try {
            return Paths.get(new URI(systemid)).getFileName().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
