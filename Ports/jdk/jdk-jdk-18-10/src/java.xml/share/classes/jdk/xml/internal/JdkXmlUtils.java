/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.xml.internal;

import com.sun.org.apache.xalan.internal.utils.XMLSecurityManager;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl;
import com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl;
import com.sun.org.apache.xerces.internal.util.ParserConfigurationSettings;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogFeatures.Feature;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import static jdk.xml.internal.JdkConstants.OVERRIDE_PARSER;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * Constants for use across JAXP processors.
 */
public class JdkXmlUtils {
    private static final String DOM_FACTORY_ID = "javax.xml.parsers.DocumentBuilderFactory";
    private static final String SAX_FACTORY_ID = "javax.xml.parsers.SAXParserFactory";
    private static final String SAX_DRIVER = "org.xml.sax.driver";

    /**
     * Xerces features
     */
    public static final String NAMESPACES_FEATURE =
        Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE;
    public static final String NAMESPACE_PREFIXES_FEATURE =
        Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACE_PREFIXES_FEATURE;


    /**
     * Catalog features
     */
    public final static String USE_CATALOG = XMLConstants.USE_CATALOG;
    public final static String SP_USE_CATALOG = "javax.xml.useCatalog";
    public final static String CATALOG_FILES = CatalogFeatures.Feature.FILES.getPropertyName();
    public final static String CATALOG_DEFER = CatalogFeatures.Feature.DEFER.getPropertyName();
    public final static String CATALOG_PREFER = CatalogFeatures.Feature.PREFER.getPropertyName();
    public final static String CATALOG_RESOLVE = CatalogFeatures.Feature.RESOLVE.getPropertyName();



    /**
     * Default value of USE_CATALOG. This will read the System property
     */
    public static final boolean USE_CATALOG_DEFAULT
            = SecuritySupport.getJAXPSystemProperty(Boolean.class, SP_USE_CATALOG, "true");


    /**
     * The system-default factory
     */
    private static final SAXParserFactory defaultSAXFactory = getSAXFactory(false);

    /**
     * Returns the value.
     *
     * @param value the specified value
     * @param defValue the default value
     * @return the value, or the default value if the value is null
     */
    public static int getValue(Object value, int defValue) {
        if (value == null) {
            return defValue;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            return Integer.parseInt(String.valueOf(value));
        } else {
            throw new IllegalArgumentException("Unexpected class: "
                    + value.getClass());
        }
    }

    /**
     * Sets the XMLReader instance with the specified property if the the
     * property is supported, ignores error if not, issues a warning if so
     * requested.
     *
     * @param reader an XMLReader instance
     * @param property the name of the property
     * @param value the value of the property
     * @param warn a flag indicating whether a warning should be issued
     */
    public static void setXMLReaderPropertyIfSupport(XMLReader reader, String property,
            Object value, boolean warn) {
        try {
            reader.setProperty(property, value);
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            if (warn) {
                XMLSecurityManager.printWarning(reader.getClass().getName(),
                        property, e);
            }
        }
    }

    /**
     * Returns the value of a Catalog feature by the property name.
     *
     * @param features a CatalogFeatures instance
     * @param name the name of a Catalog feature
     * @return the value of a Catalog feature, null if the name does not match
     * any feature supported by the Catalog.
     */
    public static String getCatalogFeature(CatalogFeatures features, String name) {
        for (Feature feature : Feature.values()) {
            if (feature.getPropertyName().equals(name)) {
                return features.get(feature);
            }
        }
        return null;
    }

    /**
     * Creates an instance of a CatalogFeatures.
     *
     * @param defer the defer property defined in CatalogFeatures
     * @param file the file path to a catalog
     * @param prefer the prefer property defined in CatalogFeatures
     * @param resolve the resolve property defined in CatalogFeatures
     * @return a {@link javax.xml.transform.Source} object
     */
    public static CatalogFeatures getCatalogFeatures(String defer, String file,
            String prefer, String resolve) {

        CatalogFeatures.Builder builder = CatalogFeatures.builder();
        if (file != null) {
            builder = builder.with(CatalogFeatures.Feature.FILES, file);
        }
        if (prefer != null) {
            builder = builder.with(CatalogFeatures.Feature.PREFER, prefer);
        }
        if (defer != null) {
            builder = builder.with(CatalogFeatures.Feature.DEFER, defer);
        }
        if (resolve != null) {
            builder = builder.with(CatalogFeatures.Feature.RESOLVE, resolve);
        }

        return builder.build();
    }

    /**
     * Passing on the CatalogFeatures settings from one Xerces configuration
     * object to another.
     *
     * @param config1 a Xerces configuration object
     * @param config2 a Xerces configuration object
     */
    public static void catalogFeaturesConfig2Config(XMLComponentManager config1,
            ParserConfigurationSettings config2) {
        boolean supportCatalog = true;
        boolean useCatalog = config1.getFeature(XMLConstants.USE_CATALOG);
        try {
            config2.setFeature(JdkXmlUtils.USE_CATALOG, useCatalog);
        } catch (XMLConfigurationException e) {
            supportCatalog = false;
        }

        if (supportCatalog && useCatalog) {
            try {
                for (CatalogFeatures.Feature f : CatalogFeatures.Feature.values()) {
                    config2.setProperty(f.getPropertyName(), config1.getProperty(f.getPropertyName()));
                }
            } catch (XMLConfigurationException e) {
                //shall not happen for internal settings
            }
        }
    }

    /**
     * Passing on the CatalogFeatures settings from a Xerces configuration
     * object to an XMLReader.
     *
     * @param config a Xerces configuration object
     * @param reader an XMLReader
     */
    public static void catalogFeaturesConfig2Reader(XMLComponentManager config, XMLReader reader) {
        boolean supportCatalog = true;
        boolean useCatalog = config.getFeature(XMLConstants.USE_CATALOG);
        try {
            reader.setFeature(JdkXmlUtils.USE_CATALOG, useCatalog);
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            supportCatalog = false;
        }

        if (supportCatalog && useCatalog) {
            try {
                for (CatalogFeatures.Feature f : CatalogFeatures.Feature.values()) {
                    reader.setProperty(f.getPropertyName(), config.getProperty(f.getPropertyName()));
                }
            } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
                //shall not happen for internal settings
            }
        }
    }

    /**
     * Returns an XMLReader instance. If overrideDefaultParser is requested, use
     * SAXParserFactory or XMLReaderFactory, otherwise use the system-default
     * SAXParserFactory to locate an XMLReader.
     *
     * @param overrideDefaultParser a flag indicating whether a 3rd party's
     * parser implementation may be used to override the system-default one
     * @param secureProcessing a flag indicating whether secure processing is
     * requested
     * @param useXMLReaderFactory a flag indicating when the XMLReader should be
     * created using XMLReaderFactory. True is a compatibility mode that honors
     * the property org.xml.sax.driver (see JDK-6490921).
     * @return an XMLReader instance
     */
    public static XMLReader getXMLReader(boolean overrideDefaultParser,
            boolean secureProcessing) {
        SAXParserFactory saxFactory;
        XMLReader reader = null;
        String spSAXDriver = SecuritySupport.getSystemProperty(SAX_DRIVER);
        if (spSAXDriver != null) {
            reader = getXMLReaderWXMLReaderFactory();
        } else if (overrideDefaultParser) {
            reader = getXMLReaderWSAXFactory(overrideDefaultParser);
        }

        if (reader != null) {
            if (secureProcessing) {
                try {
                    reader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, secureProcessing);
                } catch (SAXException e) {
                    XMLSecurityManager.printWarning(reader.getClass().getName(),
                            XMLConstants.FEATURE_SECURE_PROCESSING, e);
                }
            }
            try {
                reader.setFeature(NAMESPACES_FEATURE, true);
                reader.setFeature(NAMESPACE_PREFIXES_FEATURE, false);
            } catch (SAXException se) {
                // older version of a parser
            }
            return reader;
        }

        // use the system-default
        saxFactory = defaultSAXFactory;

        try {
            reader = saxFactory.newSAXParser().getXMLReader();
        } catch (ParserConfigurationException | SAXException ex) {
            // shall not happen with the system-default reader
        }
        return reader;
    }

    /**
     * Creates a system-default DOM Document.
     *
     * @return a DOM Document instance
     */
    public static Document getDOMDocument() {
        try {
            DocumentBuilderFactory dbf = JdkXmlUtils.getDOMFactory(false);
            return dbf.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException pce) {
            // can never happen with the system-default configuration
        }
        return null;
    }

    /**
     * Returns a DocumentBuilderFactory instance.
     *
     * @param overrideDefaultParser a flag indicating whether the system-default
     * implementation may be overridden. If the system property of the
     * DOM factory ID is set, override is always allowed.
     *
     * @return a DocumentBuilderFactory instance.
     */
    @SuppressWarnings("removal")
    public static DocumentBuilderFactory getDOMFactory(boolean overrideDefaultParser) {
        boolean override = overrideDefaultParser;
        String spDOMFactory = SecuritySupport.getJAXPSystemProperty(DOM_FACTORY_ID);

        if (spDOMFactory != null && System.getSecurityManager() == null) {
            override = true;
        }
        DocumentBuilderFactory dbf
                = !override
                        ? new DocumentBuilderFactoryImpl()
                        : DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        // false is the default setting. This step here is for compatibility
        dbf.setValidating(false);
        return dbf;
    }

    /**
     * Returns a SAXParserFactory instance.
     *
     * @param overrideDefaultParser a flag indicating whether the system-default
     * implementation may be overridden. If the system property of the
     * DOM factory ID is set, override is always allowed.
     *
     * @return a SAXParserFactory instance.
     */
    @SuppressWarnings("removal")
    public static SAXParserFactory getSAXFactory(boolean overrideDefaultParser) {
        boolean override = overrideDefaultParser;
        String spSAXFactory = SecuritySupport.getJAXPSystemProperty(SAX_FACTORY_ID);
        if (spSAXFactory != null && System.getSecurityManager() == null) {
            override = true;
        }

        SAXParserFactory factory
                = !override
                        ? new SAXParserFactoryImpl()
                        : SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory;
    }

    public static SAXTransformerFactory getSAXTransformFactory(boolean overrideDefaultParser) {
        SAXTransformerFactory tf = overrideDefaultParser
                ? (SAXTransformerFactory) SAXTransformerFactory.newInstance()
                : (SAXTransformerFactory) new TransformerFactoryImpl();
        try {
            tf.setFeature(OVERRIDE_PARSER, overrideDefaultParser);
        } catch (TransformerConfigurationException ex) {
            // ignore since it'd never happen with the JDK impl.
        }
        return tf;
    }

    /**
     * Returns the character to be used to quote the input content. Between
     * single and double quotes, this method returns the one that is not found
     * in the input. Returns double quote by default.
     *
     * @param s the input string
     * @return returns the quote not found in the input
     */
    public static char getQuoteChar(String s) {
        if (s != null && s.indexOf('"') > -1) {
            return '\'';
        } else {
            return '"';
        }
    }

    private static XMLReader getXMLReaderWSAXFactory(boolean overrideDefaultParser) {
        SAXParserFactory saxFactory = getSAXFactory(overrideDefaultParser);
        try {
            return saxFactory.newSAXParser().getXMLReader();
        } catch (ParserConfigurationException | SAXException ex) {
            return getXMLReaderWXMLReaderFactory();
        }
    }

    @SuppressWarnings("deprecation")
    private static XMLReader getXMLReaderWXMLReaderFactory() {
        try {
            return org.xml.sax.helpers.XMLReaderFactory.createXMLReader();
        } catch (SAXException ex1) {
        }
        return null;
    }
}
