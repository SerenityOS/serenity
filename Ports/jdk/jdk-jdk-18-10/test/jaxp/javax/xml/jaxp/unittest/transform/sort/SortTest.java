/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package transform.sort;

import static jaxp.library.JAXPTestUtilities.getSystemProperty;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8193830
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.sort.SortTest
 * @run testng/othervm transform.sort.SortTest
 * @summary verify xsl:sort lang attribute
 */
@Listeners({jaxp.library.FilePolicy.class})
public class SortTest {

    static final String LAND_EN = "en";
    static final String LAND_PL = "pl";
    static final String LAND_RU = "ru";

    String filepath;
    String slash = "";

    @BeforeClass
    public void setUpClass() throws Exception {
        String file1 = getClass().getResource("sort-alphabet-english.xml").getFile();
        if (getSystemProperty("os.name").contains("Windows")) {
            filepath = file1.substring(1, file1.lastIndexOf("/") + 1);
            slash = "/";
        } else {
            filepath = file1.substring(0, file1.lastIndexOf("/") + 1);
        }
    }

    /*
     * DataProvider fields:
     * lang, xml, xsl, gold file
     */
    @DataProvider(name = "parameters")
    public Object[][] getParameters() {

        return new Object[][]{
            {LAND_EN, "sort-alphabet-english.xml", "sort-alphabet-english.xsl", "sort-alphabet-english.out"},
            {LAND_PL, "sort-alphabet-polish.xml", "sort-alphabet-polish.xsl", "sort-alphabet-polish.out"},
            {LAND_RU, "sort-alphabet-russian.xml", "sort-alphabet-russian.xsl", "sort-alphabet-russian.out"},};
    }

    @Test(dataProvider = "parameters")
    public final void testTransform(String lang, String xml, String xsl, String gold)
            throws Exception {

        StringWriter sw = new StringWriter();
        // Create transformer factory
        TransformerFactory factory = TransformerFactory.newInstance();

        // Use the factory to create a template containing the xsl file
        Templates template = factory.newTemplates(new StreamSource(getClass().getResourceAsStream(xsl)));
        // Use the template to create a transformer
        Transformer xformer = template.newTransformer();
        xformer.setParameter("lang", lang);
        // Prepare the input and output files
        Source source = new StreamSource(getClass().getResourceAsStream(xml));

        /*
             * The following may be used to produce gold files.
             * Using however the original gold files, and compare without considering
             * the format
         */
        //String output = getClass().getResource(gold).getPath();
        //Result result = new StreamResult(new FileOutputStream(output));
        // use the following to verify the output against the pre-generated one
        Result result = new StreamResult(sw);

        // Apply the xsl file to the source file and write the result to the
        // output file
        xformer.transform(source, result);

        String out = sw.toString();

        List<String> lines = Files.readAllLines(Paths.get(filepath + gold));
        String[] resultLines = out.split("\n");
        int i = 0;

        // the purpose is to test sort, so ignore the format of the output
        for (String line : lines) {
            Assert.assertEquals(resultLines[i++].trim(), line.trim());
        }
    }
}
