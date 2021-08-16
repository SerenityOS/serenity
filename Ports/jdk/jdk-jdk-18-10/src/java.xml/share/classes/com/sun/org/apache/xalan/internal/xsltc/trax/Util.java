/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xalan.internal.xsltc.trax;

import com.sun.org.apache.xalan.internal.utils.XMLSecurityManager;
import com.sun.org.apache.xalan.internal.xsltc.compiler.XSLTC;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import java.io.InputStream;
import java.io.Reader;
import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogFeatures.Feature;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkXmlFeatures;
import jdk.xml.internal.JdkXmlUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * @author Santiago Pericas-Geertsen
 *
 * Added Catalog Support for URI resolution
 *
 * @LastModified: May 2021
 */
@SuppressWarnings("deprecation") //org.xml.sax.helpers.XMLReaderFactory
public final class Util {
    private static final String property = "org.xml.sax.driver";

    public static String baseName(String name) {
        return com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util.baseName(name);
    }

    public static String noExtName(String name) {
        return com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util.noExtName(name);
    }

    public static String toJavaName(String name) {
        return com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util.toJavaName(name);
    }

    /**
     * Creates a SAX2 InputSource object from a TrAX Source object
     */
    public static InputSource getInputSource(XSLTC xsltc, Source source)
        throws TransformerConfigurationException
    {
        InputSource input = null;

        String systemId = source.getSystemId();

        try {
            // Try to get InputSource from SAXSource input
            if (source instanceof SAXSource) {
                final SAXSource sax = (SAXSource)source;
                input = sax.getInputSource();
                // Pass the SAX parser to the compiler
                try {
                    XMLReader reader = sax.getXMLReader();

                    if (reader == null) {
                        boolean overrideDefaultParser = xsltc.getFeature(
                                JdkXmlFeatures.XmlFeature.JDK_OVERRIDE_PARSER);
                        reader = JdkXmlUtils.getXMLReader(overrideDefaultParser,
                                xsltc.isSecureProcessing());
                    } else {
                        // compatibility for legacy applications
                        reader.setFeature
                            (JdkXmlUtils.NAMESPACES_FEATURE,true);
                        reader.setFeature
                            (JdkXmlUtils.NAMESPACE_PREFIXES_FEATURE,false);
                    }

                    JdkXmlUtils.setXMLReaderPropertyIfSupport(reader, XMLConstants.ACCESS_EXTERNAL_DTD,
                            xsltc.getProperty(XMLConstants.ACCESS_EXTERNAL_DTD), true);

                    JdkXmlUtils.setXMLReaderPropertyIfSupport(reader, JdkConstants.CDATA_CHUNK_SIZE,
                            xsltc.getProperty(JdkConstants.CDATA_CHUNK_SIZE), false);

                    String lastProperty = "";
                    try {
                        XMLSecurityManager securityManager =
                                (XMLSecurityManager)xsltc.getProperty(JdkConstants.SECURITY_MANAGER);
                        if (securityManager != null) {
                            for (XMLSecurityManager.Limit limit : XMLSecurityManager.Limit.values()) {
                                lastProperty = limit.apiProperty();
                                reader.setProperty(lastProperty,
                                        securityManager.getLimitValueAsString(limit));
                            }
                            if (securityManager.printEntityCountInfo()) {
                                lastProperty = JdkConstants.JDK_DEBUG_LIMIT;
                                reader.setProperty(lastProperty, JdkConstants.JDK_YES);
                            }
                        }
                    } catch (SAXException se) {
                        XMLSecurityManager.printWarning(reader.getClass().getName(), lastProperty, se);
                    }

                    boolean supportCatalog = true;
                    boolean useCatalog = xsltc.getFeature(JdkXmlFeatures.XmlFeature.USE_CATALOG);
                    try {
                        reader.setFeature(JdkXmlUtils.USE_CATALOG, useCatalog);
                    }
                    catch (SAXNotRecognizedException | SAXNotSupportedException e) {
                        supportCatalog = false;
                    }

                    if (supportCatalog & useCatalog) {
                        try {
                            CatalogFeatures cf = (CatalogFeatures)xsltc.getProperty(JdkXmlFeatures.CATALOG_FEATURES);
                            if (cf != null) {
                                for (Feature f : CatalogFeatures.Feature.values()) {
                                    reader.setProperty(f.getPropertyName(), cf.get(f));
                                }
                            }
                        } catch (SAXNotRecognizedException e) {
                            //shall not happen for internal settings
                        }
                    }

                    xsltc.setXMLReader(reader);
                }catch (SAXNotRecognizedException snre ) {
                  throw new TransformerConfigurationException
                       ("SAXNotRecognizedException ",snre);
                }catch (SAXNotSupportedException snse ) {
                  throw new TransformerConfigurationException
                       ("SAXNotSupportedException ",snse);
                }

            }
            // handle  DOMSource
            else if (source instanceof DOMSource) {
                final DOMSource domsrc = (DOMSource)source;
                final Document dom = (Document)domsrc.getNode();
                final DOM2SAX dom2sax = new DOM2SAX(dom);
                xsltc.setXMLReader(dom2sax);

                // Try to get SAX InputSource from DOM Source.
                input = SAXSource.sourceToInputSource(source);
                if (input == null){
                    input = new InputSource(domsrc.getSystemId());
                }
            }

            // handle StAXSource
            else if (source instanceof StAXSource) {
                final StAXSource staxSource = (StAXSource)source;
                StAXEvent2SAX staxevent2sax = null;
                StAXStream2SAX staxStream2SAX = null;
                if (staxSource.getXMLEventReader() != null) {
                    final XMLEventReader xmlEventReader = staxSource.getXMLEventReader();
                    staxevent2sax = new StAXEvent2SAX(xmlEventReader);
                    xsltc.setXMLReader(staxevent2sax);
                } else if (staxSource.getXMLStreamReader() != null) {
                    final XMLStreamReader xmlStreamReader = staxSource.getXMLStreamReader();
                    staxStream2SAX = new StAXStream2SAX(xmlStreamReader);
                    xsltc.setXMLReader(staxStream2SAX);
                }

                // get sax InputSource from StAXSource
                input = SAXSource.sourceToInputSource(source);
                if (input == null){
                    input = new InputSource(staxSource.getSystemId());
                }
            }

            // Try to get InputStream or Reader from StreamSource
            else if (source instanceof StreamSource) {
                final StreamSource stream = (StreamSource)source;
                final InputStream istream = stream.getInputStream();
                final Reader reader = stream.getReader();
                xsltc.setXMLReader(null);     // Clear old XML reader

                // Create InputSource from Reader or InputStream in Source
                if (istream != null) {
                    input = new InputSource(istream);
                }
                else if (reader != null) {
                    input = new InputSource(reader);
                }
                else {
                    input = new InputSource(systemId);
                }
            }
            else {
                ErrorMsg err = new ErrorMsg(ErrorMsg.JAXP_UNKNOWN_SOURCE_ERR);
                throw new TransformerConfigurationException(err.toString());
            }
            input.setSystemId(systemId);
        }
        catch (NullPointerException e) {
            ErrorMsg err = new ErrorMsg(ErrorMsg.JAXP_NO_SOURCE_ERR,
                                        "TransformerFactory.newTemplates()");
            throw new TransformerConfigurationException(err.toString());
        }
        catch (SecurityException e) {
            ErrorMsg err = new ErrorMsg(ErrorMsg.FILE_ACCESS_ERR, systemId);
            throw new TransformerConfigurationException(err.toString());
        }
        return input;
    }

}
