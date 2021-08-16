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
import static test.astro.AstroConstants.RAXSL;
import static test.astro.AstroConstants.STYPEXSL;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/*
 * Implementation of the filter factory interface that utilizes DOM
 * sources rather than Stream or SAX sources. This factory utilizes a
 * DocumentBuilder to read in the stylesheets and create the DOM used
 * to create the filters for the query pipeline.
 *
 */
public class DOMFilterFactoryImpl extends SourceFilterFactory {
    @Override
    protected Source getSource(String xslFileName) throws SAXException, ParserConfigurationException, IOException {
        Document document = getStylesheetDOM(xslFileName);
        return new DOMSource(document);
    }


    @Override
    protected String getRAXsl() {
        return RAXSL;
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

    private Document getStylesheetDOM(String xslfilename) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().parse(filenameToURL(xslfilename));
    }
}
