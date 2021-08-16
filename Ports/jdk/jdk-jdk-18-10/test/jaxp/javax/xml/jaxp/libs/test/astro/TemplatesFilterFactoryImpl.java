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
import static test.astro.AstroConstants.DECXSL;
import static test.astro.AstroConstants.RAURIXSL;
import static test.astro.AstroConstants.STYPEXSL;
import static test.astro.AstroConstants.TOPTEMPLINCXSL;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/*
 * Implementation of the filter factory interface that utilizes
 * a TemplatesHandler and creates Templates from the stylesheets.
 * The Templates objects are then used to create a TransformerHandler
 * a.k.a Filter which is returned to the caller.
 * This factory uses a Uri resolver which is registered with the
 * Transformer factory.
 *
 */
public class TemplatesFilterFactoryImpl extends AbstractFilterFactory {
    private final URIResolver uriResolver = new TemplatesFilterFactoryURIResolver();

    @Override
    protected String getRAXsl() {
        return RAURIXSL;
    }

    @Override
    protected String getDECXsl() {
        return DECXSL;
    }

    @Override
    protected String getRADECXsl() {
        return DECXSL;
    }

    @Override
    protected String getStellarXsl() {
        return STYPEXSL;
    }

    @Override
    protected TransformerHandler getTransformerHandler(String xslFileName) throws SAXException, ParserConfigurationException,
            TransformerConfigurationException, IOException {
        SAXTransformerFactory factory = (SAXTransformerFactory) TransformerFactory.newInstance();
        factory.setURIResolver(uriResolver);

        TemplatesHandler templatesHandler = factory.newTemplatesHandler();

        SAXParserFactory pFactory = SAXParserFactory.newInstance();
        pFactory.setNamespaceAware(true);

        XMLReader xmlreader = pFactory.newSAXParser().getXMLReader();

        // create the stylesheet input source
        InputSource xslSrc = new InputSource(xslFileName);

        xslSrc.setSystemId(filenameToURL(xslFileName));
        // hook up the templates handler as the xsl content handler
        xmlreader.setContentHandler(templatesHandler);
        // call parse on the xsl input source

        xmlreader.parse(xslSrc);

        // extract the Templates object created from the xsl input source
        return factory.newTransformerHandler(templatesHandler.getTemplates());
    }

    /*
     * Uri resolver used to resolve stylesheet used by the Templates filter
     * factory.
     */
    private static class TemplatesFilterFactoryURIResolver implements URIResolver {
        public Source resolve(String href, String base) throws TransformerException {
            if ("http://astro.com/stylesheets/topleveltemplate".equals(href)) {
                StreamSource ss = new StreamSource(TOPTEMPLINCXSL);
                ss.setSystemId(filenameToURL(TOPTEMPLINCXSL));
                return ss;
            } else {
                return null;
            }
        }
    }
}
