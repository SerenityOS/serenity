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

import java.io.StringWriter;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 7098746
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.CR7098746Test
 * @run testng/othervm transform.CR7098746Test
 * @summary Test transforming as expected.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class CR7098746Test {

    @Test
    public final void testTransform() {

        try {

            String inFilename = "CR7098746.xml";
            String xslFilename = "CR7098746.xsl";

            StringWriter sw = new StringWriter();
            // Create transformer factory
            TransformerFactory factory = TransformerFactory.newInstance();
            // set the translet name
            // factory.setAttribute("translet-name", "myTranslet");

            // set the destination directory
            // factory.setAttribute("destination-directory", "c:\\temp");
            // factory.setAttribute("generate-translet", Boolean.TRUE);

            // Use the factory to create a template containing the xsl file
            Templates template = factory.newTemplates(new StreamSource(getClass().getResourceAsStream(xslFilename)));
            // Use the template to create a transformer
            Transformer xformer = template.newTransformer();
            // Prepare the input and output files
            Source source = new StreamSource(getClass().getResourceAsStream(inFilename));
            // Result result = new StreamResult(new
            // FileOutputStream(outFilename));
            Result result = new StreamResult(sw);
            // Apply the xsl file to the source file and write the result to the
            // output file
            xformer.transform(source, result);

            String out = sw.toString();
            if (out.indexOf("<p>") < 0) {
                Assert.fail(out);
            }
        } catch (Exception e) {
            // unexpected failure
            e.printStackTrace();
            Assert.fail(e.toString());
        }
    }
}
