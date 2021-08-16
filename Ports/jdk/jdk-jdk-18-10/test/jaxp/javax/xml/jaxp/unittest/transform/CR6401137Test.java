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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6401137
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.CR6401137Test
 * @run testng/othervm transform.CR6401137Test
 * @summary Test transform certain xsl.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class CR6401137Test {

    @Test
    public final void testTransform() {

        try {
            String str = new String();
            ByteArrayOutputStream byte_stream = new ByteArrayOutputStream();
            File inputFile = new File(getClass().getResource("CR6401137.xml").getPath());
            FileReader in = new FileReader(inputFile);
            int c;

            while ((c = in.read()) != -1) {
                str = str + new Character((char) c).toString();
            }

            in.close();

            System.out.println(str);
            byte buf[] = str.getBytes();
            byte_stream.write(buf);
            String style_sheet_uri = "CR6401137.xsl";
            byte[] xml_byte_array = byte_stream.toByteArray();
            InputStream xml_input_stream = new ByteArrayInputStream(xml_byte_array);

            Source xml_source = new StreamSource(xml_input_stream);

            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            StreamSource source = new StreamSource(getClass().getResourceAsStream(style_sheet_uri));
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
}
