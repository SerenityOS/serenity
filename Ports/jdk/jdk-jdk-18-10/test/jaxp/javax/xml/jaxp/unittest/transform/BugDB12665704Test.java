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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6935697
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.BugDB12665704Test
 * @run testng/othervm transform.BugDB12665704Test
 * @summary Test Transformer can compile large xsl file.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class BugDB12665704Test {

    @Test
    public final void testTransform() {

        try {
            String str = new String();
            ByteArrayOutputStream byte_stream = new ByteArrayOutputStream();
            File inputFile = new File(getClass().getResource("BugDB12665704.xml").getPath());
            FileReader in = new FileReader(inputFile);
            int c;

            while ((c = in.read()) != -1) {
                str = str + new Character((char) c).toString();
            }

            in.close();

            System.out.println(str);
            byte buf[] = str.getBytes();
            byte_stream.write(buf);
            String style_sheet_uri = "BugDB12665704.xsl";
            byte[] xml_byte_array = byte_stream.toByteArray();
            InputStream xml_input_stream = new ByteArrayInputStream(xml_byte_array);

            Source xml_source = new StreamSource(xml_input_stream);

            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            StreamSource source = new StreamSource(getClass().getResource(style_sheet_uri).toString());
            transformer = tFactory.newTransformer(source);

            ByteArrayOutputStream result_output_stream = new ByteArrayOutputStream();
            Result result = new StreamResult(result_output_stream);
            transformer.transform(xml_source, result);
            result_output_stream.close();

            // expected success
        } catch (Exception e) {
            // unexpected failure
            e.printStackTrace();
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testSAPTransform() {
        StringWriter out = new StringWriter();
        try {
            String xml = getXML(getClass().getResource("BugDB12665704.xml").getPath());
            getTransformer().transform(new StreamSource(new StringReader(xml)), new StreamResult(out));
        } catch (TransformerConfigurationException ex) {
            // Trace.dump(xslt);
            // Trace.dump(xml);
            System.err.println("can't process xslt: " + ex.getMessage() + " (" + ex + ")");
        } catch (TransformerException ex) {
            // Trace.dump(xslt);
            // Trace.dump(xml);
            System.err.println("can't process xml: " + ex.getMessage() + " (" + ex + ")");
        } catch (Exception ex) {
            // Trace.dump(xslt);
            // Trace.dump(xml);
            System.err.println("can't create processor: " + ex.getMessage() + " (" + ex + ")");
        }
    }

    Transformer getTransformer() {
        Transformer transformer = null;
        try {
            InputStream xin = this.getClass().getResourceAsStream("BugDB12665704.xsl");
            StreamSource xslt = new StreamSource(xin);
            TransformerFactory fc = TransformerFactory.newInstance();
            transformer = fc.newTransformer(xslt);

        } catch (Exception e) {
            // unexpected failure
            e.printStackTrace();
            Assert.fail(e.toString());
        }

        return transformer;
    }

    String getXML(String sourceFile) throws IOException {
        BufferedReader inputStream = null;
        StringBuilder sb = new StringBuilder();
        try {
            inputStream = new BufferedReader(new FileReader(sourceFile));
            String l;

            while ((l = inputStream.readLine()) != null) {
                sb.append(l);
            }

        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return sb.toString();
    }
}
