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

package test.gaptest;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static jaxp.library.JAXPTestUtilities.USER_DIR;
import static jaxp.library.JAXPTestUtilities.compareDocumentWithGold;
import static org.testng.Assert.assertTrue;
import static test.gaptest.GapTestConst.GOLDEN_DIR;
import static test.gaptest.GapTestConst.XML_DIR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 4693341
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow test.gaptest.Bug4693341
 * @run testng/othervm test.gaptest.Bug4693341
 * @summary test transforming to stream with external dtd
 */

@Listeners({jaxp.library.FilePolicy.class})
public class Bug4693341 {

    @Test
    public void test() throws TransformerException, ParserConfigurationException, SAXException, IOException {

        Transformer transformer = TransformerFactory.newInstance().newTransformer();

        String out = USER_DIR + "Bug4693341.out";
        StreamResult result = new StreamResult(new File(out));

        String in = XML_DIR + "Bug4693341.xml";
        String golden = GOLDEN_DIR + "Bug4693341.xml";
        File file = new File(in);
        StreamSource source = new StreamSource(file);
        System.out.println(source.getSystemId());

        Files.copy(Paths.get(XML_DIR + "Bug4693341.dtd"),
                Paths.get(USER_DIR + "Bug4693341.dtd"), REPLACE_EXISTING);

        transformer.transform(source, result);

        assertTrue(compareDocumentWithGold(golden, out));
    }

}
