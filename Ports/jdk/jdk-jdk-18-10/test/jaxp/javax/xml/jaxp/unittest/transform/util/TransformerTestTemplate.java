/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

package transform.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/*
 * Template for Transformer tests
 */
public abstract class TransformerTestTemplate {
    private String xsl = null;
    private String sourceXml = null;

    public TransformerTestTemplate() {
        super();
    }

    public TransformerTestTemplate(String xsl, String sourceXml) {
        super();
        this.xsl = xsl;
        this.sourceXml = sourceXml;
    }

    public String getXsl() {
        return xsl;
    }

    public void setXsl(String xsl) {
        this.xsl = xsl;
    }

    public String getSourceXml() {
        return sourceXml;
    }

    public void setSourceXml(String sourceXml) {
        this.sourceXml = sourceXml;
    }

    public String fromInputStream(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            return br.lines().collect(Collectors.joining("\n"));
        }
    }

    // print an XML Snippet under a given title
    public void printSnippet(String title, String snippet) {
        StringBuilder div = new StringBuilder();
        for (int i = 0; i < title.length(); i++)
            div.append("=");
        System.out.println(title + "\n" + div + "\n" + snippet + "\n");
    }

    // pretty print a DOMResult
    public String prettyPrintDOMResult(DOMResult dr) throws ClassNotFoundException, InstantiationException,
        IllegalAccessException, ClassCastException
    {
        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        DOMImplementationLS domImplementationLS = (DOMImplementationLS)registry.getDOMImplementation("LS");
        StringWriter writer = new StringWriter();
        LSOutput formattedOutput = domImplementationLS.createLSOutput();
        formattedOutput.setCharacterStream(writer);
        LSSerializer domSerializer = domImplementationLS.createLSSerializer();
        domSerializer.getDomConfig().setParameter("format-pretty-print", true);
        domSerializer.getDomConfig().setParameter("xml-declaration", false);
        domSerializer.write(dr.getNode(), formattedOutput);
        return writer.toString();
    }

    public Transformer getTransformer() throws TransformerConfigurationException {
        return getTransformer(null);
    }

        // utility to obtain Transformer from TransformerFactory
    public Transformer getTransformer(TransformerFactory tf)
        throws TransformerConfigurationException
    {
        if (tf == null) {
            tf = TransformerFactory.newInstance();
        }

        if (xsl == null) {
            return tf.newTransformer();
        } else {
            return tf.newTransformer(
                new StreamSource(new ByteArrayInputStream(xsl.getBytes()))
            );
        }
    }

    // utility to obtain Templates from TransformerFactory
    public Templates getTemplates(TransformerFactory tf)
        throws TransformerConfigurationException
    {
        return tf.newTemplates(
            new StreamSource(new ByteArrayInputStream(xsl.getBytes()))
        );
    }

    // utility to construct StreamSource
    public StreamSource getStreamSource() {
        return new StreamSource(
            new ByteArrayInputStream(sourceXml.getBytes())
        );
    }

    // utility to construct DOMSource from DocumentBuilderFactory
    public DOMSource getDOMSource(DocumentBuilderFactory dbf)
        throws SAXException, IOException, ParserConfigurationException
    {
        return new DOMSource(
            dbf.newDocumentBuilder().parse(
                new InputSource(
                    new ByteArrayInputStream(sourceXml.getBytes())
                )
            )
        );
    }

    // utility to construct SAXSource from SAXParserFactory
    public SAXSource getSAXSource(SAXParserFactory spf)
        throws SAXException, ParserConfigurationException
    {
        return new SAXSource(
            spf.newSAXParser().getXMLReader(),
            new InputSource(new ByteArrayInputStream(sourceXml.getBytes()))
        );
    }

    // utility to construct StAXSource from XMLInputFactory
    public StAXSource getStAXSource(XMLInputFactory xif)
        throws XMLStreamException
    {
        return new StAXSource(
            xif.createXMLStreamReader(new StringReader(sourceXml))
        );
    }
}
