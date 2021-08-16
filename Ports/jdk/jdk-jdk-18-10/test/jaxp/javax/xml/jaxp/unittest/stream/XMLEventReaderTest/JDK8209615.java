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

package stream.XMLEventReaderTest;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import javax.xml.stream.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/*
 * @test
 * @bug 8209615
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLEventReaderTest.JDK8209615
 * @run testng/othervm stream.XMLEventReaderTest.JDK8209615
 * @summary Verifies that the parser continues parsing the character data
 */
@Listeners({jaxp.library.BasePolicy.class})
public class JDK8209615 {

    /**
     * Verifies that the parser parses the xml successfully. Before the patch,
     * the parser failed with the following error message:
     * The element type "Data" must be terminated by the matching end-tag "</Data>".
     *
     * @throws Exception if the parser fails to parse the xml
     */
    @Test
    public void testParseCData() throws Exception {

        String xml = "<Export generator=\"Cache\" version=\"25\">\r\n"
            + "<Class name=\"DSVRDemo.ConditionalBannerLIDR\">\r\n" + "<Description>\r\n" + "</Description>\r\n"
            + "<Parameter name=\"XSLTMODE\">\r\n" + "</Parameter>\r\n" + "<Parameter name=\"DSSHOME\">\r\n"
            + "</Parameter>\r\n" + "<Parameter name=\"DSSCLASS\">\r\n" + "</Parameter>\r\n"
            + "<XData name=\"ReportModel\">\r\n" + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n"
            + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n"
            + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n" + "{t:\"box\",gc:\r\n"
            + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n"
            + "{t:\"txt\",gc:\r\n" + "{t:\"box\",gc:\r\n" + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n"
            + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n"
            + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n"
            + "{t:\"txt\",gc:\r\n" + "{t:\"txt\",gc:\r\n" + "<Data><![CDATA[\r\n" + "<pageheader>\r\n"
            + "<![CDATA[\r\n" + "</svg:text>\r\n"
            + "<svg:text x=\"0\" y=\"30.333333333333336\" style=\"font-size:10;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;\" >\r\n"
            + "Run by:\r\n" + "</svg:text>\r\n" + "<xsl:value-of select=\"@Genre\"/>\r\n" + "</svg:text>\r\n"
            + "<svg:text x=\"0\" y=\"19.333333333333336\" style=\"font-size:10;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;\" >\r\n"
            + "Genre:\r\n" + "</svg:text>\r\n"
            + "<svg:rect x=\"70\" y=\"0\" width=\"86\" height=\"11\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"70\" y=\"8.333333333333334\" style=\"font-size:10;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;\" >\r\n"
            + "</svg:text>\r\n"
            + "<svg:rect x=\"0\" y=\"0\" width=\"70\" height=\"11\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"0\" y=\"8.333333333333334\" style=\"font-size:10;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;\" >\r\n"
            + "Run time:\r\n" + "</svg:text>\r\n"
            + "<svg:line x1=\"0\" y1=\"34\" x2=\"466\" y2=\"34\" style=\"stroke:#000000;stroke-width:2;stroke-opacity:1\" />\r\n"
            + "</svg:svg>\r\n" + "</fo:instream-foreign-object>\r\n" + "</fo:block>\r\n" + "]]]]><![CDATA[>\r\n"
            + "<!-- end of PAGE HEADER -->\r\n" + "<pagefooter>\r\n" + "<!-- PAGE FOOTER -->\r\n" + "<write>\r\n"
            + "<![CDATA[\r\n" + "<fo:block font-size=\"0pt\" >\r\n" + "<fo:instream-foreign-object>\r\n"
            + "<svg:svg width=\"468pt\" height=\"37pt\" viewBox=\"0 0 468 37\" >\r\n"
            + "<svg:line x1=\"0\" y1=\"1\" x2=\"466\" y2=\"1\" style=\"stroke:#000000;stroke-width:2;stroke-opacity:1\" />\r\n"
            + "<svg:rect x=\"70\" y=\"2\" width=\"86\" height=\"11\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<xsl:value-of select=\"@runBy\"/>\r\n" + "</svg:text>\r\n"
            + "<svg:rect x=\"0\" y=\"2\" width=\"70\" height=\"11\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"0\" y=\"10.333333333333334\" style=\"font-size:10;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;\" >\r\n"
            + "</svg:text>\r\n"
            + "<svg:rect x=\"70\" y=\"13\" width=\"86\" height=\"11\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "</svg:text>\r\n"
            + "<svg:text x=\"0\" y=\"21.333333333333336\" style=\"font-size:10;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;\" >\r\n"
            + "</svg:text>\r\n"
            + "<svg:rect x=\"70\" y=\"24\" width=\"86\" height=\"11\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"70\" y=\"32.333333333333336\" style=\"font-size:10;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;\" >\r\n"
            + "</svg:text>\r\n"
            + "<svg:rect x=\"0\" y=\"24\" width=\"70\" height=\"11\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"0\" y=\"32.333333333333336\" style=\"font-size:10;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;\" >\r\n"
            + "Run time:\r\n" + "</svg:text>\r\n"
            + "<svg:rect x=\"312\" y=\"23\" width=\"156\" height=\"12\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"468\" y=\"31.333333333333336\" style=\"font-size:10;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;text-anchor:end;\" >\r\n"
            + "Annotated Box Office\r\n" + "</svg:text>\r\n" + "</fo:instream-foreign-object>\r\n"
            + "</fo:block>\r\n" + "<!-- end of PAGE FOOTER -->\r\n" + "</pagefooter>\r\n" + "<body>\r\n"
            + "<write>\r\n" + "<![CDATA[\r\n" + "<fo:block font-size=\"0pt\" >\r\n"
            + "<svg:svg width=\"468pt\" height=\"205pt\" viewBox=\"0 0 468 205\" >\r\n"
            + "<svg:rect x=\"1\" y=\"73\" width=\"466\" height=\"126\" style=\"fill-opacity:1;fill:#ffffff;stroke:#000000;stroke-width:2;stroke-opacity:1\" />\r\n"
            + "<svg:rect x=\"0\" y=\"74\" width=\"468\" height=\"44\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "Annotated Box Office\r\n" + "</svg:text>\r\n"
            + "<svg:rect x=\"246\" y=\"118\" width=\"222\" height=\"16\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"246\" y=\"129.66666666666666\" style=\"font-size:14;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;\" >\r\n"
            + "</svg:text>\r\n"
            + "<svg:rect x=\"0\" y=\"118\" width=\"222\" height=\"16\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"222\" y=\"129.66666666666666\" style=\"font-size:14;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;text-anchor:end;\" >\r\n"
            + "Run by:\r\n" + "</svg:text>\r\n"
            + "<svg:rect x=\"246\" y=\"134\" width=\"222\" height=\"16\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "</svg:text>\r\n"
            + "<svg:text x=\"222\" y=\"145.66666666666666\" style=\"font-size:14;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;text-anchor:end;\" >\r\n"
            + "Genre:\r\n"
            + "<svg:rect x=\"246\" y=\"150\" width=\"222\" height=\"16\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"246\" y=\"161.66666666666666\" style=\"font-size:14;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;\" >\r\n"
            + "<xsl:value-of select=\"@runTime\"/>\r\n"
            + "<svg:rect x=\"0\" y=\"150\" width=\"222\" height=\"16\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "Run time:\r\n" + "</svg:text>\r\n"
            + "<svg:rect x=\"246\" y=\"166\" width=\"222\" height=\"16\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"246\" y=\"177.66666666666666\" style=\"font-size:14;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;\" >\r\n"
            + "<xsl:value-of select=\"TotalTicketsSold\"/>\r\n" + "</svg:text>\r\n"
            + "<svg:rect x=\"0\" y=\"166\" width=\"222\" height=\"16\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"222\" y=\"177.66666666666666\" style=\"font-size:14;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;text-anchor:end;\" >\r\n"
            + "Total tickets sold:\r\n" + "</svg:text>\r\n"
            + "<svg:rect x=\"246\" y=\"182\" width=\"222\" height=\"16\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"246\" y=\"193.66666666666666\" style=\"font-size:14;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;\" >\r\n"
            + "<xsl:value-of select=\"TotalFilms\"/>\r\n" + "</svg:text>\r\n"
            + "<svg:rect x=\"0\" y=\"182\" width=\"222\" height=\"16\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"222\" y=\"193.66666666666666\" style=\"font-size:14;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;text-anchor:end;\" >\r\n"
            + "Total films:\r\n" + "</svg:text>\r\n" + "</svg:svg>\r\n" + "]]]]><![CDATA[>\r\n" + "</write>\r\n"
            + "<pagebreak/>\r\n" + "<!-- end of REPORT HEADER -->\r\n" + "<group name='FilmsByGenre' >\r\n"
            + "<!-- GROUP HEADER (FilmsByGenre) -->\r\n" + "<write>\r\n" + "<![CDATA[\r\n"
            + "<fo:block font-size=\"0pt\" >\r\n" + "<fo:instream-foreign-object>\r\n"
            + "<svg:svg width=\"468pt\" height=\"74pt\" viewBox=\"0 0 468 74\" >\r\n"
            + "<svg:text x=\"78\" y=\"55\" style=\"font-size:12;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;\" >\r\n"
            + "<xsl:value-of select=\"TitleCount\"/>\r\n" + "</svg:text>\r\n"
            + "<svg:rect x=\"0\" y=\"45\" width=\"78\" height=\"14\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"0\" y=\"55\" style=\"font-size:12;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;\" >\r\n"
            + "Title count:\r\n" + "</svg:text>\r\n"
            + "<svg:rect x=\"156\" y=\"24\" width=\"156\" height=\"21\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<xsl:value-of select=\"@CategoryName\"/>\r\n" + "</svg:text>\r\n"
            + "<svg:text x=\"468\" y=\"55\" style=\"font-size:12;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;text-anchor:end;\" >\r\n"
            + "<svg:rect x=\"312\" y=\"45\" width=\"78\" height=\"14\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"390\" y=\"55\" style=\"font-size:12;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;text-anchor:end;\" >\r\n"
            + "</svg:text>\r\n"
            + "<svg:line x1=\"0\" y1=\"60\" x2=\"466\" y2=\"60\" style=\"stroke:#000000;stroke-width:2;stroke-opacity:1\" />\r\n"
            + "</svg:svg>\r\n" + "</fo:instream-foreign-object>\r\n" + "</fo:block>\r\n" + "]]]]><![CDATA[>\r\n"
            + "</write>\r\n" + "<group name='FilmDetail' >\r\n" + "<!-- REPORT BODY (FilmDetail) -->\r\n"
            + "<write>\r\n" + "<![CDATA[\r\n" + "<fo:block font-size=\"0pt\">\r\n"
            + "<fo:instream-foreign-object>\r\n"
            + "<svg:svg width=\"468pt\" height=\"71pt\" viewBox=\"0 0 468 71\" >\r\n"
            + "<svg:rect x=\"3\" y=\"4\" width=\"257\" height=\"16\" style=\"fill:#ffffff;fill-opacity:0;stroke-width:0;\" />\r\n"
            + "<svg:text x=\"3\" y=\"14\" style=\"font-size:12;font-family:sans-serif;fill:#000000;fill-opacity:1;font-weight:bold;\" >\r\n"
            + "</svg:text>\r\n" + "</report>]]></Data>\r\n" + "</XData>\r\n" + "</Class>\r\n" + "</Export>\r\n";
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            stax(inputStream);
        }

    }

    private static void stax(InputStream input) throws XMLStreamException, FactoryConfigurationError {
        XMLEventReader eventReader = XMLInputFactory.newInstance()
            .createXMLEventReader(input);
        ((Iterator<?>) eventReader).forEachRemaining(ignored -> {
        });
    }
}
