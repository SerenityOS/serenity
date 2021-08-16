/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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
package test.astro;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static test.astro.AstroConstants.DEC_MAX;
import static test.astro.AstroConstants.DEC_MIN;
import static test.astro.AstroConstants.JAXP_SCHEMA_LANGUAGE;
import static test.astro.AstroConstants.JAXP_SCHEMA_SOURCE;
import static test.astro.AstroConstants.RA_MAX;
import static test.astro.AstroConstants.RA_MIN;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/*
 * AstroProcessor is to carry out the user's query with filters and produce a table of
 * star records that match the criterion, and finally output with HTML format.
 */
public class AstroProcessor {
    private String catalogFileName;

    private FilterFactory ffact;
    private InputSourceFactory isfact;

    private SAXParserFactory spf;

    /*
     * Constructor for the Main astro class.
     *
     * @param fFactClass the class of the FilterFactory implementation
     *
     * @param catalogfilename the name of the XML input document (database)
     *
     * @param isFactClass the class of the Input Source Factory implementation
     */
    public AstroProcessor(Class<FilterFactory> fFactClass, String catalogFileName, Class<InputSourceFactory> isFactClass) throws Exception {
        // create the Filter Factory instance...

        ffact = fFactClass.newInstance();

        // create the Input Source Instance

        isfact = isFactClass.newInstance();

        spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        spf.setValidating(true);

        // All XML Readers are required to recognize these two:
        spf.setFeature("http://xml.org/sax/features/namespaces", true);
        spf.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

        // Other features...
        spf.setFeature("http://xml.org/sax/features/validation", true);
        spf.setFeature("http://apache.org/xml/features/validation/schema", true);
        spf.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);

        this.catalogFileName = catalogFileName;
    }

    /*
     * Sets the star stellar type query.
     *
     * @param arg stellar type string, can be a substring.
     */
    public TransformerHandler getStellarTypeFilter(String arg) throws TransformerConfigurationException, SAXException, ParserConfigurationException,
            IOException {
        String stellarType = null;
        if (arg != null && arg.length() != 0) {
            stellarType = arg; // set value of query
        } else {
            throw new IllegalArgumentException("\n  Stellar type string of length zero found.");
        }

        return ffact.newStellarTypeFilter(stellarType);
    }

    /*
     * Sets the right ascension parameters for a query. Parameters are validated
     * to be in the range of 0h and 24h inclusive.
     *
     * @param min minimum right ascension in hours.
     *
     * @param max maximum right ascension in hours.
     */
    public TransformerHandler getRAFilter(double min, double max) throws TransformerConfigurationException, SAXException, ParserConfigurationException,
            IOException {
        double raMin = RA_MIN; // hours
        double raMax = RA_MAX; // hours
        if (min < max) {
            if ((min >= RA_MIN && min <= RA_MAX) && (max >= RA_MIN && max <= RA_MAX)) {
                raMin = min; // set value of query
                raMax = max; // set value of query

            }
        } else {
            throw new IllegalArgumentException("min must be less than max.\n" + "min=" + min + ", max=" + max);
        }

        return ffact.newRAFilter(raMin, raMax);
    }

    /*
     * Sets the right ascension and dec parameters for a query. Parameters are
     * validated to be in the range of ra 0h and 24h and dec -90 to +90
     * inclusive.
     *
     * @param rmin minimum right ascension in hours.
     *
     * @param rmax maximum right ascension in hours.
     *
     * @param dmin minimum declination in degs.
     *
     * @param dmax maximum declination in degs.
     */
    public TransformerHandler getRADECFilter(double rmin, double rmax, double dmin, double dmax) throws TransformerConfigurationException, SAXException,
            ParserConfigurationException, IOException {
        double raMin = RA_MIN; // hours
        double raMax = RA_MAX; // hours
        double decMin = DEC_MIN; // degrees
        double decMax = DEC_MAX; // degrees
        if (rmin < rmax && dmin < dmax) {
            if ((rmin >= RA_MIN && rmin <= RA_MAX) && (rmax >= RA_MIN && rmax <= RA_MAX)) {
                raMin = rmin; // set value of query
                raMax = rmax; // set value of query
            }
            if ((dmin >= DEC_MIN && dmin <= DEC_MAX) && (dmax >= DEC_MIN && dmax <= DEC_MAX)) {
                decMin = dmin; // set value of query
                decMax = dmax; // set value of query
            }

        } else {
            throw new IllegalArgumentException("min must be less than max.\n" + "rmin=" + rmin + ", rmax=" + rmax + ", dmin=" + dmin + ", dmax=" + dmax);
        }

        return ffact.newRADECFilter(raMin, raMax, decMin, decMax);
    }

    /*
     * Sets the declination parameters for a query. Parameters are validated to
     * be in the range of -90 and +90 degrees inclusive.
     *
     * @param min minimum declination in degrees.
     *
     * @param max maximum declination in degrees.
     */
    public TransformerHandler getDecFilter(double min, double max) throws TransformerConfigurationException, SAXException, ParserConfigurationException,
            IOException {
        double decMin = DEC_MIN; // degrees
        double decMax = DEC_MAX; // degrees
        if (min < max) {
            if ((min >= DEC_MIN && min <= DEC_MAX) && (max >= DEC_MIN && max <= DEC_MAX)) {
                decMin = min; // set value of query
                decMax = max; // set value of query
            }
        } else {
            throw new IllegalArgumentException("min must be less than max.\n" + "min=" + min + ", max=" + max);
        }

        return ffact.newDECFilter(decMin, decMax);
    }

    /*
     * Runs the filter process against the astronomical database.
     *
     * @throws Exception
     */
    public void process(String outputfile, TransformerHandler... filters) throws Exception {
        XMLReader catparser = getXMLReader();

        File catalogfile = new File(catalogFileName);
        InputSource catsrc = isfact.newInputSource(catalogfile.getPath());

        TransformerHandler outfilter = ffact.newHTMLOutput();
        // create an array from the Vector of filters...

        // hook the filters up to each other, there may be zero filters
        int nfilters = filters.length;
        if (nfilters != 0) {
            TransformerHandler prev = null;
            for (int i = 0; i < filters.length; i++) {
                TransformerHandler curr = filters[i];
                if (prev != null) {
                    prev.setResult(new SAXResult(curr));
                }
                prev = curr;
            }
            // hook up the last filter to the output filter
            prev.setResult(new SAXResult(outfilter));
            // hook up the catalog parser to the first filter...
            catparser.setContentHandler(filters[0]);
        } else {
            // There are no query filters,
            // hook up the catalog parser directly to output filter...
            catparser.setContentHandler(outfilter);
        }
        // hook up the output filter to the output file or std out
        if (outputfile != null) {
            outfilter.setResult(new StreamResult(outputfile));
        } else {
            outfilter.setResult(new StreamResult(System.out));
        }

        catparser.parse(catsrc);
    }

    private XMLReader getXMLReader() throws Exception {
        SAXParser parser = spf.newSAXParser();
        parser.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA_NS_URI);
        parser.setProperty(JAXP_SCHEMA_SOURCE, "catalog.xsd");
        XMLReader catparser = parser.getXMLReader();
        catparser.setErrorHandler(new CatalogErrorHandler());
        return catparser;
    }

    /*
     * Error Handler for the parsing of the XML astronomical catalog.
     */
    private static class CatalogErrorHandler implements ErrorHandler {
        private String getParseExceptionInfo(SAXParseException spe) {
            String systemId = spe.getSystemId();
            if (systemId == null) {
                systemId = "null";
            }
            String info = "Catalog URI=" + systemId + " Line=" + spe.getLineNumber() + ": " + spe.getMessage();
            return info;
        }

        public void warning(SAXParseException spe) throws SAXException {
            String message = "Warning: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }

        public void error(SAXParseException spe) throws SAXException {
            String message = "Error: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }

        public void fatalError(SAXParseException spe) throws SAXException {
            String message = "Fatal Error: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }
    }
}
