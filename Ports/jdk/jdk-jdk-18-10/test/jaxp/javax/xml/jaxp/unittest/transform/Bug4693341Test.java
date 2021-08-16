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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 4693341
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.Bug4693341Test
 * @run testng/othervm transform.Bug4693341Test
 * @summary Test transform with external dtd.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug4693341Test {
    // save dtd file to current working directory to avoid writing into source repository
    public void copyDTDtoWorkDir() throws IOException {
        try (FileInputStream dtdres = new FileInputStream(getClass().getResource("Bug4693341.dtd").getPath());
             FileOutputStream dtdwork = new FileOutputStream(USER_DIR + "Bug4693341.dtd");) {
            int n;
            byte[] buffer = new byte[1024];
            while((n = dtdres.read(buffer)) > -1) {
                dtdwork.write(buffer, 0, n);
            }
        }
    }

    @Test
    public void test() {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();

            copyDTDtoWorkDir();

            File outf = new File(USER_DIR + "Bug4693341.out");
            StreamResult result = new StreamResult(new FileOutputStream(outf));

            String in = getClass().getResource("Bug4693341.xml").getPath();
            File file = new File(in);
            StreamSource source = new StreamSource(new FileInputStream(file), ("file://" + in));

            transformer.transform(source, result);

            //URL inputsource = new URL("file", "", golden);
            URL output = new URL("file", "", outf.getPath());

            // error happens when trying to parse output
            String systemId = output.toExternalForm();
            System.out.println("systemId: " + systemId);
            InputSource is = new InputSource(systemId);
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(is, new DefaultHandler());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
}
