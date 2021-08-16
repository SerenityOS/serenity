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

import static jaxp.library.JAXPTestUtilities.filenameToURL;
import static test.astro.AstroConstants.DECENTXSL;
import static test.astro.AstroConstants.DECXSL;
import static test.astro.AstroConstants.RAENTXSL;
import static test.astro.AstroConstants.STYPEXSL;
import static test.astro.AstroConstants.TOPTEMPLXSL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/*
 * Implementation of the filter factory interface that utilizes SAX
 * sources rather than Stream sources. This factory utilizes a
 * SAX parser factory and XMLReader to read in the stylesheets used
 * to create the filters for the query pipeline.
 * The XMLReader has been equipped with an entity resolver
 * SAXFilterFactoryEntityResolver, it is used to resolve external
 * entities in two specialized stylesheets, ra-ent.xsl (derived from
 * ra.xsl) and dec-ent.xsl (derived from dec.xsl).
 *
 */
public class SAXFilterFactoryImpl extends SourceFilterFactory {
    private EntityResolver entityResolver;

    public SAXFilterFactoryImpl() {
        super();
        entityResolver = new SAXFilterFactoryEntityResolver();
    }

    @Override
    protected Source getSource(String xslFileName) throws SAXException, ParserConfigurationException {
        SAXSource saxsource = new SAXSource(new InputSource(filenameToURL(xslFileName)));
        saxsource.setXMLReader(getXMLReader());
        return saxsource;
    }

    @Override
    protected String getRAXsl() {
        return RAENTXSL;
    }

    @Override
    protected String getDECXsl() {
        return DECENTXSL;
    }

    @Override
    protected String getRADECXsl() {
        return DECXSL;
    }

    @Override
    protected String getStellarXsl() {
        return STYPEXSL;
    }

    /*
     * Entity resolver implementation that is used in the SAXFilterFactory
     * implementation for handling external entities that appear in two
     * specialized stylesheets, 'ra-ent.xsl' and 'dec-ent.xsl'. Both of these
     * stylesheets contain an external entity reference to the top level
     * stylesheet template, which is stored in a separate file,
     * 'toptemplate.xsl'.
     */
    private static class SAXFilterFactoryEntityResolver implements EntityResolver {
        public InputSource resolveEntity(String publicid, String sysId) {
            if (sysId.equals("http://astro.com/stylesheets/toptemplate")) {
                InputSource retval = new InputSource(TOPTEMPLXSL);
                retval.setSystemId(filenameToURL(TOPTEMPLXSL));
                return retval;
            } else {
                return null; // use default behavior
            }
        }
    }

    private XMLReader getXMLReader() throws SAXException, ParserConfigurationException {
        SAXParserFactory pfactory = SAXParserFactory.newInstance();
        pfactory.setNamespaceAware(true);
        // pfactory.setValidating(true);
        XMLReader xmlreader = pfactory.newSAXParser().getXMLReader();
        // entity resolver is used in stylesheets ra-ent.xsl,
        // dec-ent.xsl. Other stylehsheets will not use it
        // since they do not contain ext entities.
        xmlreader.setEntityResolver(entityResolver);
        return xmlreader;
    }
}
