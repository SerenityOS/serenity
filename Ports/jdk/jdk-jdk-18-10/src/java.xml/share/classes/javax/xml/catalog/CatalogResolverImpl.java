/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package javax.xml.catalog;

import com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Implements CatalogResolver.
 *
 * <p>
 * This class implements a SAX EntityResolver, StAX XMLResolver,
 * Schema Validation LSResourceResolver and Transform URIResolver.
 *
 *
 * @since 9
 */
final class CatalogResolverImpl implements CatalogResolver {
    Catalog catalog;

    /**
     * Construct an instance of the CatalogResolver from a Catalog.
     *
     * @param catalog A Catalog.
     */
    public CatalogResolverImpl(Catalog catalog) {
        this.catalog = catalog;
    }

    /*
       Implements the EntityResolver interface
    */
    @Override
    public InputSource resolveEntity(String publicId, String systemId) {
        //8150187: NPE expected if the system identifier is null for CatalogResolver
        CatalogMessages.reportNPEOnNull("systemId", systemId);

        //Normalize publicId and systemId
        systemId = Normalizer.normalizeURI(Util.getNotNullOrEmpty(systemId));
        publicId = Normalizer.normalizePublicId(Normalizer.decodeURN(Util.getNotNullOrEmpty(publicId)));

        //check whether systemId is a urn
        if (systemId != null && systemId.startsWith(Util.URN)) {
            systemId = Normalizer.decodeURN(systemId);
            if (publicId != null && !publicId.equals(systemId)) {
                systemId = null;
            } else {
                publicId = systemId;
                systemId = null;
            }
        }

        CatalogImpl c = (CatalogImpl)catalog;
        String resolvedSystemId = Util.resolve(c, publicId, systemId);

        if (resolvedSystemId != null) {
            return new InputSource(resolvedSystemId);
        }

        GroupEntry.ResolveType resolveType = ((CatalogImpl) catalog).getResolve();
        switch (resolveType) {
            case IGNORE:
                return new InputSource(new StringReader(""));
            case STRICT:
                CatalogMessages.reportError(CatalogMessages.ERR_NO_MATCH,
                        new Object[]{publicId, systemId});
        }

        //no action, allow the parser to continue
        return null;
    }

    /*
        Implements the URIResolver interface
    */
    CatalogResolverImpl entityResolver;

    @Override
    public Source resolve(String href, String base) {
        CatalogMessages.reportNPEOnNull("href", href);

        href = Util.getNotNullOrEmpty(href);
        base = Util.getNotNullOrEmpty(base);

        String result = null;
        CatalogImpl c = (CatalogImpl)catalog;
        String uri = Normalizer.normalizeURI(href);
        if (uri == null) {
            return null;
        }

        //check whether uri is a urn
        if (uri != null && uri.startsWith(Util.URN)) {
            String publicId = Normalizer.decodeURN(uri);
            if (publicId != null) {
                result = Util.resolve(c, publicId, null);
            }
        }

        //if no match with a public id, continue search for a URI
        if (result == null) {
            //remove fragment if any.
            int hashPos = uri.indexOf("#");
            if (hashPos >= 0) {
                uri = uri.substring(0, hashPos);
            }

            //search the current catalog
            result = Util.resolve(c, null, uri);
        }

        //Report error or return the URI as is when no match is found
        if (result == null) {
            GroupEntry.ResolveType resolveType = c.getResolve();
            switch (resolveType) {
                case IGNORE:
                    return new SAXSource(new InputSource(new StringReader("")));
                case STRICT:
                    CatalogMessages.reportError(CatalogMessages.ERR_NO_URI_MATCH,
                            new Object[]{href, base});
            }
            try {
                URL url = null;

                if (base == null) {
                    url = new URL(uri);
                    result = url.toString();
                } else {
                    URL baseURL = new URL(base);
                    url = (href.length() == 0 ? baseURL : new URL(baseURL, uri));
                    result = url.toString();
                }
            } catch (java.net.MalformedURLException mue) {
                    CatalogMessages.reportError(CatalogMessages.ERR_CREATING_URI,
                            new Object[]{href, base});
            }
        }

        SAXSource source = new SAXSource();
        source.setInputSource(new InputSource(result));
        setEntityResolver(source);
        return source;
    }

    /**
     * Establish an entityResolver for newly resolved URIs.
     * <p>
     * This is called from the URIResolver to set an EntityResolver on the SAX
     * parser to be used for new XML documents that are encountered as a result
     * of the document() function, xsl:import, or xsl:include. This is done
     * because the XSLT processor calls out to the SAXParserFactory itself to
     * create a new SAXParser to parse the new document. The new parser does not
     * automatically inherit the EntityResolver of the original (although
     * arguably it should). Quote from JAXP specification on Class
     * SAXTransformerFactory:
     * <p>
     * {@code If an application wants to set the ErrorHandler or EntityResolver
     * for an XMLReader used during a transformation, it should use a URIResolver
     * to return the SAXSource which provides (with getXMLReader) a reference to
     * the XMLReader}
     *
     */
    private void setEntityResolver(SAXSource source) {
        XMLReader reader = source.getXMLReader();
        if (reader == null) {
            SAXParserFactory spFactory = new SAXParserFactoryImpl();
            spFactory.setNamespaceAware(true);
            try {
                reader = spFactory.newSAXParser().getXMLReader();
            } catch (ParserConfigurationException | SAXException ex) {
                CatalogMessages.reportRunTimeError(CatalogMessages.ERR_PARSER_CONF, ex);
            }
        }
        if (entityResolver != null) {
            entityResolver = new CatalogResolverImpl(catalog);
        }
        reader.setEntityResolver(entityResolver);
        source.setXMLReader(reader);
    }

    @Override
    public InputStream resolveEntity(String publicId, String systemId, String baseUri, String namespace) {
        InputSource is = resolveEntity(publicId, systemId);

        if (is != null && !is.isEmpty()) {

            try {
                return new URL(is.getSystemId()).openStream();
            } catch (IOException ex) {
                //considered as no mapping.
            }

        }

        GroupEntry.ResolveType resolveType = ((CatalogImpl) catalog).getResolve();
        switch (resolveType) {
            case IGNORE:
                return null;
            case STRICT:
                CatalogMessages.reportError(CatalogMessages.ERR_NO_MATCH,
                        new Object[]{publicId, systemId});
        }

        //no action, allow the parser to continue
        return null;
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        InputSource is = resolveEntity(publicId, systemId);

        if (is != null && !is.isEmpty()) {
            return new LSInputImpl(is.getSystemId());
        }

        GroupEntry.ResolveType resolveType = ((CatalogImpl) catalog).getResolve();
        switch (resolveType) {
            case IGNORE:
                return null;
            case STRICT:
                CatalogMessages.reportError(CatalogMessages.ERR_NO_MATCH,
                        new Object[]{publicId, systemId});
        }

        //no action, allow the parser to continue
        return null;
    }

    /**
     * Implements LSInput. All that we need is the systemId since the Catalog
     * has already resolved it.
     */
    class LSInputImpl implements LSInput {

        private String systemId;

        public LSInputImpl(String systemId) {
            this.systemId = systemId;
        }

        @Override
        public Reader getCharacterStream() {
            return null;
        }

        @Override
        public void setCharacterStream(Reader characterStream) {
        }

        @Override
        public InputStream getByteStream() {
            return null;
        }

        @Override
        public void setByteStream(InputStream byteStream) {
        }

        @Override
        public String getStringData() {
            return null;
        }

        @Override
        public void setStringData(String stringData) {
        }

        @Override
        public String getSystemId() {
            return systemId;
        }

        @Override
        public void setSystemId(String systemId) {
            this.systemId = systemId;
        }

        @Override
        public String getPublicId() {
            return null;
        }

        @Override
        public void setPublicId(String publicId) {
        }

        @Override
        public String getBaseURI() {
            return null;
        }

        @Override
        public void setBaseURI(String baseURI) {
        }

        @Override
        public String getEncoding() {
            return null;
        }

        @Override
        public void setEncoding(String encoding) {
        }

        @Override
        public boolean getCertifiedText() {
            return false;
        }

        @Override
        public void setCertifiedText(boolean certifiedText) {
        }
    }

}
