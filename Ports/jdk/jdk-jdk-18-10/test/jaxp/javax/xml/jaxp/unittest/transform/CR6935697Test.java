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

import java.io.FileOutputStream;

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
 * @bug 6935697
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.CR6935697Test
 * @run testng/othervm transform.CR6935697Test
 * @summary Test XSLT can parse the certain xsl.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class CR6935697Test {

    @Test
    public final void testTransform() {

        try {

            String inFilename = "CR6935697.xml";
            String xslFilename = "CR6935697.xsl";
            String outFilename = "CR6935697.out";

            // Create transformer factory
            TransformerFactory factory = TransformerFactory.newInstance();
            // Use the factory to create a template containing the xsl file
            Templates template = factory.newTemplates(new StreamSource(getClass().getResourceAsStream(xslFilename)));
            // Use the template to create a transformer
            Transformer xformer = template.newTransformer();
            // Prepare the input and output files
            Source source = new StreamSource(getClass().getResourceAsStream(inFilename));
            Result result = new StreamResult(new FileOutputStream(USER_DIR + outFilename));
            // Apply the xsl file to the source file and write the result to the
            // output file
            xformer.transform(source, result);

        } catch (Exception e) {
            // unexpected failure
            e.printStackTrace();
            Assert.fail(e.toString());
        }
    }
}
