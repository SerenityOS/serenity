/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xml.internal.utils;

import com.sun.org.apache.xalan.internal.utils.XMLSecurityManager;
import java.util.HashMap;
import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkXmlFeatures;
import jdk.xml.internal.JdkXmlUtils;
import jdk.xml.internal.SecuritySupport;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * Creates XMLReader objects and caches them for re-use.
 * This class follows the singleton pattern.
 *
 * @LastModified: May 2021
 */
public class XMLReaderManager {

    private static final XMLReaderManager m_singletonManager =
                                                     new XMLReaderManager();
    private static final String property = "org.xml.sax.driver";

    /**
     * Cache of XMLReader objects
     */
    private ThreadLocal<ReaderWrapper> m_readers;

    /**
     * Keeps track of whether an XMLReader object is in use.
     */
    private HashMap<XMLReader, Boolean> m_inUse;

    private boolean m_overrideDefaultParser;

    private boolean _secureProcessing;
     /**
     * protocols allowed for external DTD references in source file and/or stylesheet.
     */
    private String _accessExternalDTD = JdkConstants.EXTERNAL_ACCESS_DEFAULT;

    private XMLSecurityManager _xmlSecurityManager;

    //Catalog Feature
    private boolean _useCatalog;
    private CatalogFeatures _catalogFeatures;

    private int _cdataChunkSize;

    /**
     * Hidden constructor
     */
    private XMLReaderManager() {
    }

    /**
     * Retrieves the singleton reader manager
     */
    public static XMLReaderManager getInstance(boolean overrideDefaultParser) {
        m_singletonManager.setOverrideDefaultParser(overrideDefaultParser);
        return m_singletonManager;
    }

    /**
     * Retrieves a cached XMLReader for this thread, or creates a new
     * XMLReader, if the existing reader is in use.  When the caller no
     * longer needs the reader, it must release it with a call to
     * {@link #releaseXMLReader}.
     */
    public synchronized XMLReader getXMLReader() throws SAXException {
        XMLReader reader;

        if (m_readers == null) {
            // When the m_readers.get() method is called for the first time
            // on a thread, a new XMLReader will automatically be created.
            m_readers = new ThreadLocal<>();
        }

        if (m_inUse == null) {
            m_inUse = new HashMap<>();
        }

        /**
         * Constructs a new XMLReader if:
         * (1) the cached reader for this thread is in use, or
         * (2) the requirement for overriding has changed,
         * (3) the cached reader isn't an instance of the class set in the
         * 'org.xml.sax.driver' property
         *
         * otherwise, returns the cached reader
         */
        ReaderWrapper rw = m_readers.get();
        boolean threadHasReader = (rw != null);
        reader = threadHasReader ? rw.reader : null;
        String factory = SecuritySupport.getSystemProperty(property);
        if (threadHasReader && m_inUse.get(reader) != Boolean.TRUE &&
                (rw.overrideDefaultParser == m_overrideDefaultParser) &&
                ( factory == null || reader.getClass().getName().equals(factory))) {
            m_inUse.put(reader, Boolean.TRUE);
        } else {
            reader = JdkXmlUtils.getXMLReader(m_overrideDefaultParser, _secureProcessing);

            // Cache the XMLReader if this is the first time we've created
            // a reader for this thread.
            if (!threadHasReader) {
                m_readers.set(new ReaderWrapper(reader, m_overrideDefaultParser));
                m_inUse.put(reader, Boolean.TRUE);
            }
        }

        //reader is cached, but this property might have been reset
        JdkXmlUtils.setXMLReaderPropertyIfSupport(reader, XMLConstants.ACCESS_EXTERNAL_DTD,
                _accessExternalDTD, true);

        JdkXmlUtils.setXMLReaderPropertyIfSupport(reader, JdkConstants.CDATA_CHUNK_SIZE,
                _cdataChunkSize, false);

        String lastProperty = "";
        try {
            if (_xmlSecurityManager != null) {
                for (XMLSecurityManager.Limit limit : XMLSecurityManager.Limit.values()) {
                    lastProperty = limit.apiProperty();
                    reader.setProperty(lastProperty,
                            _xmlSecurityManager.getLimitValueAsString(limit));
                }
                if (_xmlSecurityManager.printEntityCountInfo()) {
                    lastProperty = JdkConstants.JDK_DEBUG_LIMIT;
                    reader.setProperty(lastProperty, JdkConstants.JDK_YES);
                }
            }
        } catch (SAXException se) {
            XMLSecurityManager.printWarning(reader.getClass().getName(), lastProperty, se);
        }

        boolean supportCatalog = true;
        try {
            reader.setFeature(JdkXmlUtils.USE_CATALOG, _useCatalog);
        }
        catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            supportCatalog = false;
        }

        if (supportCatalog && _useCatalog && _catalogFeatures != null) {
            try {
                for (CatalogFeatures.Feature f : CatalogFeatures.Feature.values()) {
                    reader.setProperty(f.getPropertyName(), _catalogFeatures.get(f));
                }
            } catch (SAXNotRecognizedException e) {
                //shall not happen for internal settings
            }
        }
        return reader;
    }

    /**
     * Mark the cached XMLReader as available.  If the reader was not
     * actually in the cache, do nothing.
     *
     * @param reader The XMLReader that's being released.
     */
    public synchronized void releaseXMLReader(XMLReader reader) {
        // If the reader that's being released is the cached reader
        // for this thread, remove it from the m_isUse list.
        ReaderWrapper rw = m_readers.get();
        if (rw.reader == reader && reader != null) {
            m_inUse.remove(reader);
        }
    }
    /**
     * Return the state of the services mechanism feature.
     */
    public boolean overrideDefaultParser() {
        return m_overrideDefaultParser;
    }

    /**
     * Set the state of the services mechanism feature.
     */
    public void setOverrideDefaultParser(boolean flag) {
        m_overrideDefaultParser = flag;
    }

    /**
     * Set feature
     */
    public void setFeature(String name, boolean value) {
        if (name.equals(XMLConstants.FEATURE_SECURE_PROCESSING)) {
            _secureProcessing = value;
        } else if (XMLConstants.USE_CATALOG.equals(name)) {
            _useCatalog = value;
        }
    }

    /**
     * Get property value
     */
    public Object getProperty(String name) {
        if (name.equals(XMLConstants.ACCESS_EXTERNAL_DTD)) {
            return _accessExternalDTD;
        } else if (name.equals(JdkConstants.SECURITY_MANAGER)) {
            return _xmlSecurityManager;
        }
        return null;
    }

    /**
     * Set property.
     */
    public void setProperty(String name, Object value) {
        if (name.equals(XMLConstants.ACCESS_EXTERNAL_DTD)) {
            _accessExternalDTD = (String)value;
        } else if (name.equals(JdkConstants.SECURITY_MANAGER)) {
            _xmlSecurityManager = (XMLSecurityManager)value;
        } else if (JdkXmlFeatures.CATALOG_FEATURES.equals(name)) {
            _catalogFeatures = (CatalogFeatures)value;
        } else if (JdkConstants.CDATA_CHUNK_SIZE.equals(name)) {
            _cdataChunkSize = JdkXmlUtils.getValue(value, _cdataChunkSize);
        }
    }

    class ReaderWrapper {
        XMLReader reader;
        boolean overrideDefaultParser;

        public ReaderWrapper(XMLReader reader, boolean overrideDefaultParser) {
            this.reader = reader;
            this.overrideDefaultParser = overrideDefaultParser;
        }
    }
}
