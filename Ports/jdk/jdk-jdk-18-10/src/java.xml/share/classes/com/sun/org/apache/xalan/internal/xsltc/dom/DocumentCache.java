/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
/*
 * $Id: DocumentCache.java,v 1.2.4.1 2005/09/06 06:15:22 pvedula Exp $
 */

package com.sun.org.apache.xalan.internal.xsltc.dom;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.DOMCache;
import com.sun.org.apache.xalan.internal.xsltc.DOMEnhancedForDTM;
import com.sun.org.apache.xalan.internal.xsltc.Translet;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xalan.internal.xsltc.runtime.BasisLibrary;
import com.sun.org.apache.xalan.internal.xsltc.runtime.Constants;
import com.sun.org.apache.xml.internal.utils.SystemIDResolver;
import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * @author Morten Jorgensen
 */
public final class DocumentCache implements DOMCache {

    private int       _size;
    private Map<String, CachedDocument> _references;
    private String[]  _URIs;
    private int       _count;
    private int       _current;
    private SAXParser _parser;
    private XMLReader _reader;
    private XSLTCDTMManager _dtmManager;

    private static final int REFRESH_INTERVAL = 1000;

    /*
     * Inner class containing a DOMImpl object and DTD handler
     */
    public final class CachedDocument {

        // Statistics data
        private long _firstReferenced;
        private long _lastReferenced;
        private long _accessCount;
        private long _lastModified;
        private long _lastChecked;
        private long _buildTime;

        // DOM and DTD handler references
        private DOMEnhancedForDTM _dom = null;

        /**
         * Constructor - load document and initialise statistics
         */
        public CachedDocument(String uri) {
            // Initialise statistics variables
            final long stamp = System.currentTimeMillis();
            _firstReferenced = stamp;
            _lastReferenced  = stamp;
            _accessCount     = 0;
            loadDocument(uri);

            _buildTime = System.currentTimeMillis() - stamp;
        }

        /**
         * Loads the document and updates build-time (latency) statistics
         */
        public void loadDocument(String uri) {

            try {
                final long stamp = System.currentTimeMillis();
                _dom = (DOMEnhancedForDTM)_dtmManager.getDTM(
                                 new SAXSource(_reader, new InputSource(uri)),
                                 false, null, true, false);
                _dom.setDocumentURI(uri);

                // The build time can be used for statistics for a better
                // priority algorithm (currently round robin).
                final long thisTime = System.currentTimeMillis() - stamp;
                if (_buildTime > 0)
                    _buildTime = (_buildTime + thisTime) >>> 1;
                else
                    _buildTime = thisTime;
            }
            catch (Exception e) {
                _dom = null;
            }
        }

        public DOM getDocument()       { return(_dom); }

        public long getFirstReferenced()   { return(_firstReferenced); }

        public long getLastReferenced()    { return(_lastReferenced); }

        public long getAccessCount()       { return(_accessCount); }

        public void incAccessCount()       { _accessCount++; }

        public long getLastModified()      { return(_lastModified); }

        public void setLastModified(long t){ _lastModified = t; }

        public long getLatency()           { return(_buildTime); }

        public long getLastChecked()       { return(_lastChecked); }

        public void setLastChecked(long t) { _lastChecked = t; }

        public long getEstimatedSize() {
            if (_dom != null)
                return(_dom.getSize() << 5); // ???
            else
                return(0);
        }

    }

    /**
     * DocumentCache constructor
     */
    public DocumentCache(int size) throws SAXException {
        this(size, null);
        try {
            _dtmManager = XSLTCDTMManager.createNewDTMManagerInstance();
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    /**
     * DocumentCache constructor
     */
    public DocumentCache(int size, XSLTCDTMManager dtmManager) throws SAXException {
        _dtmManager = dtmManager;
        _count = 0;
        _current = 0;
        _size  = size;
        _references = new HashMap<>(_size+2);
        _URIs = new String[_size];

        try {
            // Create a SAX parser and get the XMLReader object it uses
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            try {
                factory.setFeature(Constants.NAMESPACE_FEATURE,true);
            }
            catch (Exception e) {
                factory.setNamespaceAware(true);
            }
            _parser = factory.newSAXParser();
            _reader = _parser.getXMLReader();
        }
        catch (ParserConfigurationException e) {
            BasisLibrary.runTimeError(BasisLibrary.NAMESPACES_SUPPORT_ERR);
        }
    }

    /**
     * Returns the time-stamp for a document's last update
     */
    private final long getLastModified(String uri) {
        try {
            URL url = new URL(uri);
            URLConnection connection = url.openConnection();
            long timestamp = connection.getLastModified();
            // Check for a "file:" URI (courtesy of Brian Ewins)
            if (timestamp == 0){ // get 0 for local URI
                if ("file".equals(url.getProtocol())){
                    File localfile = Paths.get(url.toURI()).toFile();
                    timestamp = localfile.lastModified();
                }
            }
            return(timestamp);
        }
        // Brutal handling of all exceptions
        catch (Exception e) {
            return(System.currentTimeMillis());
        }
    }

    /**
     *
     */
    private CachedDocument lookupDocument(String uri) {
        return(_references.get(uri));
    }

    /**
     *
     */
    private synchronized void insertDocument(String uri, CachedDocument doc) {
        if (_count < _size) {
            // Insert out URI in circular buffer
            _URIs[_count++] = uri;
            _current = 0;
        }
        else {
            // Remove oldest URI from reference map
            _references.remove(_URIs[_current]);
            // Insert our URI in circular buffer
            _URIs[_current] = uri;
            if (++_current >= _size) _current = 0;
        }
        _references.put(uri, doc);
    }

    /**
     *
     */
    private synchronized void replaceDocument(String uri, CachedDocument doc) {
        if (doc == null)
            insertDocument(uri, doc);
        else
            _references.put(uri, doc);
    }

    /**
     * Returns a document either by finding it in the cache or
     * downloading it and putting it in the cache.
     */
    @Override
    public DOM retrieveDocument(String baseURI, String href, Translet trs) {
        CachedDocument doc;

    String uri = href;
    if (baseURI != null && !baseURI.equals("")) {
        try {
            uri = SystemIDResolver.getAbsoluteURI(uri, baseURI);
        } catch (TransformerException te) {
            // ignore
        }
    }

        // Try to get the document from the cache first
        if ((doc = lookupDocument(uri)) == null) {
            doc = new CachedDocument(uri);
            if (doc == null) return null; // better error handling needed!!!
            doc.setLastModified(getLastModified(uri));
            insertDocument(uri, doc);
        }
        // If the document is in the cache we must check if it is still valid
        else {
            long now = System.currentTimeMillis();
            long chk = doc.getLastChecked();
            doc.setLastChecked(now);
            // Has the modification time for this file been checked lately?
            if (now > (chk + REFRESH_INTERVAL)) {
                doc.setLastChecked(now);
                long last = getLastModified(uri);
                // Reload document if it has been modified since last download
                if (last > doc.getLastModified()) {
                    doc = new CachedDocument(uri);
                    if (doc == null) return null;
                    doc.setLastModified(getLastModified(uri));
                    replaceDocument(uri, doc);
                }
            }

        }

        // Get the references to the actual DOM and DTD handler
        final DOM dom = doc.getDocument();

        // The dom reference may be null if the URL pointed to a
        // non-existing document
        if (dom == null) return null;

        doc.incAccessCount(); // For statistics

        final AbstractTranslet translet = (AbstractTranslet)trs;

        // Give the translet an early opportunity to extract any
        // information from the DOM object that it would like.
        translet.prepassDocument(dom);

        return(doc.getDocument());
    }

    /**
     * Outputs the cache statistics
     */
    public void getStatistics(PrintWriter out) {
        out.println("<h2>DOM cache statistics</h2><center><table border=\"2\">"+
                    "<tr><td><b>Document URI</b></td>"+
                    "<td><center><b>Build time</b></center></td>"+
                    "<td><center><b>Access count</b></center></td>"+
                    "<td><center><b>Last accessed</b></center></td>"+
                    "<td><center><b>Last modified</b></center></td></tr>");

        for (int i=0; i<_count; i++) {
            CachedDocument doc = _references.get(_URIs[i]);
            out.print("<tr><td><a href=\""+_URIs[i]+"\">"+
                      "<font size=-1>"+_URIs[i]+"</font></a></td>");
            out.print("<td><center>"+doc.getLatency()+"ms</center></td>");
            out.print("<td><center>"+doc.getAccessCount()+"</center></td>");
            out.print("<td><center>"+(new Date(doc.getLastReferenced()))+
                      "</center></td>");
            out.print("<td><center>"+(new Date(doc.getLastModified()))+
                      "</center></td>");
            out.println("</tr>");
        }

        out.println("</table></center>");
    }
}
