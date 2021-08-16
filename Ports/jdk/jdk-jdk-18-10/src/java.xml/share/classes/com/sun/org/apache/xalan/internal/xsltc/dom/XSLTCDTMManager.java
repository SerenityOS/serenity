/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xalan.internal.xsltc.dom;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stax.StAXSource;

import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.dtm.ref.DTMDefaultBase;
import com.sun.org.apache.xml.internal.dtm.DTMException;
import com.sun.org.apache.xml.internal.dtm.DTMWSFilter;
import com.sun.org.apache.xml.internal.dtm.ref.DTMManagerDefault;
import com.sun.org.apache.xml.internal.res.XMLErrorResources;
import com.sun.org.apache.xml.internal.res.XMLMessages;
import com.sun.org.apache.xml.internal.utils.SystemIDResolver;
import com.sun.org.apache.xalan.internal.xsltc.trax.DOM2SAX;
import com.sun.org.apache.xalan.internal.xsltc.trax.StAXEvent2SAX;
import com.sun.org.apache.xalan.internal.xsltc.trax.StAXStream2SAX;

import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * The default implementation for the DTMManager.
 */
public class XSLTCDTMManager extends DTMManagerDefault
{

    /** Set this to true if you want a dump of the DTM after creation */
    private static final boolean DUMPTREE = false;

    /** Set this to true if you want basic diagnostics */
    private static final boolean DEBUG = false;

    /**
     * Constructor DTMManagerDefault
     *
     */
    public XSLTCDTMManager()
    {
        super();
    }

    /**
     * Obtain a new instance of a <code>DTMManager</code>.
     * This static method creates a new factory instance.
     * The current implementation just returns a new XSLTCDTMManager instance.
     */
    public static XSLTCDTMManager newInstance()
    {
        return new XSLTCDTMManager();
    }

    /**
     * Creates a new instance of the XSLTC DTM Manager service.
     * Creates a new instance of the default class
     * <code>com.sun.org.apache.xalan.internal.xsltc.dom.XSLTCDTMManager</code>.
     */
      public static XSLTCDTMManager createNewDTMManagerInstance() {
         return newInstance();
      }

    /**
     * Get an instance of a DTM, loaded with the content from the
     * specified source.  If the unique flag is true, a new instance will
     * always be returned.  Otherwise it is up to the DTMManager to return a
     * new instance or an instance that it already created and may be being used
     * by someone else.
     * (I think more parameters will need to be added for error handling, and
     * entity resolution).
     *
     * @param source the specification of the source object.
     * @param unique true if the returned DTM must be unique, probably because it
     * is going to be mutated.
     * @param whiteSpaceFilter Enables filtering of whitespace nodes, and may
     *                         be null.
     * @param incremental true if the DTM should be built incrementally, if
     *                    possible.
     * @param doIndexing true if the caller considers it worth it to use
     *                   indexing schemes.
     *
     * @return a non-null DTM reference.
     */
    @Override
    public DTM getDTM(Source source, boolean unique,
                      DTMWSFilter whiteSpaceFilter, boolean incremental,
                      boolean doIndexing)
    {
        return getDTM(source, unique, whiteSpaceFilter, incremental,
                      doIndexing, false, 0, true, false);
    }

    /**
     * Get an instance of a DTM, loaded with the content from the
     * specified source.  If the unique flag is true, a new instance will
     * always be returned.  Otherwise it is up to the DTMManager to return a
     * new instance or an instance that it already created and may be being used
     * by someone else.
     * (I think more parameters will need to be added for error handling, and
     * entity resolution).
     *
     * @param source the specification of the source object.
     * @param unique true if the returned DTM must be unique, probably because it
     * is going to be mutated.
     * @param whiteSpaceFilter Enables filtering of whitespace nodes, and may
     *                         be null.
     * @param incremental true if the DTM should be built incrementally, if
     *                    possible.
     * @param doIndexing true if the caller considers it worth it to use
     *                   indexing schemes.
     * @param buildIdIndex true if the id index table should be built.
     *
     * @return a non-null DTM reference.
     */
    public DTM getDTM(Source source, boolean unique,
                      DTMWSFilter whiteSpaceFilter, boolean incremental,
                      boolean doIndexing, boolean buildIdIndex)
    {
        return getDTM(source, unique, whiteSpaceFilter, incremental,
                      doIndexing, false, 0, buildIdIndex, false);
    }

    /**
     * Get an instance of a DTM, loaded with the content from the
     * specified source.  If the unique flag is true, a new instance will
     * always be returned.  Otherwise it is up to the DTMManager to return a
     * new instance or an instance that it already created and may be being used
     * by someone else.
     * (I think more parameters will need to be added for error handling, and
     * entity resolution).
     *
     * @param source the specification of the source object.
     * @param unique true if the returned DTM must be unique, probably because it
     * is going to be mutated.
     * @param whiteSpaceFilter Enables filtering of whitespace nodes, and may
     *                         be null.
     * @param incremental true if the DTM should be built incrementally, if
     *                    possible.
     * @param doIndexing true if the caller considers it worth it to use
     *                   indexing schemes.
     * @param buildIdIndex true if the id index table should be built.
     * @param newNameTable true if we want to use a separate ExpandedNameTable
     *                     for this DTM.
     *
     * @return a non-null DTM reference.
     */
  public DTM getDTM(Source source, boolean unique,
                    DTMWSFilter whiteSpaceFilter, boolean incremental,
                    boolean doIndexing, boolean buildIdIndex,
                    boolean newNameTable)
  {
    return getDTM(source, unique, whiteSpaceFilter, incremental,
                  doIndexing, false, 0, buildIdIndex, newNameTable);
  }

  /**
     * Get an instance of a DTM, loaded with the content from the
     * specified source.  If the unique flag is true, a new instance will
     * always be returned.  Otherwise it is up to the DTMManager to return a
     * new instance or an instance that it already created and may be being used
     * by someone else.
     * (I think more parameters will need to be added for error handling, and
     * entity resolution).
     *
     * @param source the specification of the source object.
     * @param unique true if the returned DTM must be unique, probably because it
     * is going to be mutated.
     * @param whiteSpaceFilter Enables filtering of whitespace nodes, and may
     *                         be null.
     * @param incremental true if the DTM should be built incrementally, if
     *                    possible.
     * @param doIndexing true if the caller considers it worth it to use
     *                   indexing schemes.
     * @param hasUserReader true if <code>source</code> is a
     *                      <code>SAXSource</code> object that has an
     *                      <code>XMLReader</code>, that was specified by the
     *                      user.
     * @param size  Specifies initial size of tables that represent the DTM
     * @param buildIdIndex true if the id index table should be built.
     *
     * @return a non-null DTM reference.
     */
    public DTM getDTM(Source source, boolean unique,
                      DTMWSFilter whiteSpaceFilter, boolean incremental,
                      boolean doIndexing, boolean hasUserReader, int size,
                      boolean buildIdIndex)
    {
      return getDTM(source, unique, whiteSpaceFilter, incremental,
                    doIndexing, hasUserReader, size,
                    buildIdIndex, false);
  }

  /**
     * Get an instance of a DTM, loaded with the content from the
     * specified source.  If the unique flag is true, a new instance will
     * always be returned.  Otherwise it is up to the DTMManager to return a
     * new instance or an instance that it already created and may be being used
     * by someone else.
     * (I think more parameters will need to be added for error handling, and
     * entity resolution).
     *
     * @param source the specification of the source object.
     * @param unique true if the returned DTM must be unique, probably because it
     * is going to be mutated.
     * @param whiteSpaceFilter Enables filtering of whitespace nodes, and may
     *                         be null.
     * @param incremental true if the DTM should be built incrementally, if
     *                    possible.
     * @param doIndexing true if the caller considers it worth it to use
     *                   indexing schemes.
     * @param hasUserReader true if <code>source</code> is a
     *                      <code>SAXSource</code> object that has an
     *                      <code>XMLReader</code>, that was specified by the
     *                      user.
     * @param size  Specifies initial size of tables that represent the DTM
     * @param buildIdIndex true if the id index table should be built.
     * @param newNameTable true if we want to use a separate ExpandedNameTable
     *                     for this DTM.
     *
     * @return a non-null DTM reference.
     */
  public DTM getDTM(Source source, boolean unique,
                    DTMWSFilter whiteSpaceFilter, boolean incremental,
                    boolean doIndexing, boolean hasUserReader, int size,
                    boolean buildIdIndex, boolean newNameTable)
  {
        if(DEBUG && null != source) {
            System.out.println("Starting "+
                         (unique ? "UNIQUE" : "shared")+
                         " source: "+source.getSystemId());
        }

        int dtmPos = getFirstFreeDTMID();
        int documentID = dtmPos << IDENT_DTM_NODE_BITS;

        if ((null != source) && source instanceof StAXSource) {
            final StAXSource staxSource = (StAXSource)source;
            StAXEvent2SAX staxevent2sax = null;
            StAXStream2SAX staxStream2SAX = null;
            if (staxSource.getXMLEventReader() != null) {
                final XMLEventReader xmlEventReader = staxSource.getXMLEventReader();
                staxevent2sax = new StAXEvent2SAX(xmlEventReader);
            } else if (staxSource.getXMLStreamReader() != null) {
                final XMLStreamReader xmlStreamReader = staxSource.getXMLStreamReader();
                staxStream2SAX = new StAXStream2SAX(xmlStreamReader);
            }

            SAXImpl dtm;

            if (size <= 0) {
                dtm = new SAXImpl(this, source, documentID,
                                  whiteSpaceFilter, null, doIndexing,
                                  DTMDefaultBase.DEFAULT_BLOCKSIZE,
                                  buildIdIndex, newNameTable);
            } else {
                dtm = new SAXImpl(this, source, documentID,
                                  whiteSpaceFilter, null, doIndexing,
                                  size, buildIdIndex, newNameTable);
            }

            dtm.setDocumentURI(source.getSystemId());

            addDTM(dtm, dtmPos, 0);

            try {
                if (staxevent2sax != null) {
                    staxevent2sax.setContentHandler(dtm);
                    staxevent2sax.parse();
                }
                else if (staxStream2SAX != null) {
                    staxStream2SAX.setContentHandler(dtm);
                    staxStream2SAX.parse();
                }

            }
            catch (RuntimeException re) {
                throw re;
            }
            catch (Exception e) {
                throw new com.sun.org.apache.xml.internal.utils.WrappedRuntimeException(e);
            }

            return dtm;
        }else if ((null != source) && source instanceof DOMSource) {
            final DOMSource domsrc = (DOMSource) source;
            final org.w3c.dom.Node node = domsrc.getNode();
            final DOM2SAX dom2sax = new DOM2SAX(node);

            SAXImpl dtm;

            if (size <= 0) {
                dtm = new SAXImpl(this, source, documentID,
                                  whiteSpaceFilter, null, doIndexing,
                                  DTMDefaultBase.DEFAULT_BLOCKSIZE,
                                  buildIdIndex, newNameTable);
            } else {
                dtm = new SAXImpl(this, source, documentID,
                                  whiteSpaceFilter, null, doIndexing,
                                  size, buildIdIndex, newNameTable);
            }

            dtm.setDocumentURI(source.getSystemId());

            addDTM(dtm, dtmPos, 0);

            dom2sax.setContentHandler(dtm);

            try {
                dom2sax.parse();
            }
            catch (RuntimeException re) {
                throw re;
            }
            catch (Exception e) {
                throw new com.sun.org.apache.xml.internal.utils.WrappedRuntimeException(e);
            }

            return dtm;
        }
        else
        {
            boolean isSAXSource = (null != source)
                                  ? (source instanceof SAXSource) : true;
            boolean isStreamSource = (null != source)
                                  ? (source instanceof StreamSource) : false;

            if (isSAXSource || isStreamSource) {
                XMLReader reader;
                InputSource xmlSource;

                if (null == source) {
                    xmlSource = null;
                    reader = null;
                    hasUserReader = false;  // Make sure the user didn't lie
                }
                else {
                    reader = getXMLReader(source);
                    xmlSource = SAXSource.sourceToInputSource(source);

                    String urlOfSource = xmlSource.getSystemId();

                    if (null != urlOfSource) {
                        try {
                            urlOfSource = SystemIDResolver.getAbsoluteURI(urlOfSource);
                        }
                        catch (Exception e) {
                            // %REVIEW% Is there a better way to send a warning?
                            System.err.println("Can not absolutize URL: " + urlOfSource);
                        }

                        xmlSource.setSystemId(urlOfSource);
                    }
                }

                // Create the basic SAX2DTM.
                SAXImpl dtm;
                if (size <= 0) {
                    dtm = new SAXImpl(this, source, documentID, whiteSpaceFilter,
                                      null, doIndexing,
                                      DTMDefaultBase.DEFAULT_BLOCKSIZE,
                                      buildIdIndex, newNameTable);
                } else {
                    dtm = new SAXImpl(this, source, documentID, whiteSpaceFilter,
                            null, doIndexing, size, buildIdIndex, newNameTable);
                }

                // Go ahead and add the DTM to the lookup table.  This needs to be
                // done before any parsing occurs. Note offset 0, since we've just
                // created a new DTM.
                addDTM(dtm, dtmPos, 0);

                if (null == reader) {
                    // Then the user will construct it themselves.
                    return dtm;
                }

                reader.setContentHandler(dtm.getBuilder());

                if (!hasUserReader || null == reader.getDTDHandler()) {
                    reader.setDTDHandler(dtm);
                }

                if(!hasUserReader || null == reader.getErrorHandler()) {
                    reader.setErrorHandler(dtm);
                }

                try {
                    reader.setProperty("http://xml.org/sax/properties/lexical-handler", dtm);
                }
                catch (SAXNotRecognizedException e){}
                catch (SAXNotSupportedException e){}

                try {
                    reader.parse(xmlSource);
                }
                catch (RuntimeException re) {
                    throw re;
                }
                catch (Exception e) {
                    throw new com.sun.org.apache.xml.internal.utils.WrappedRuntimeException(e);
                } finally {
                    if (!hasUserReader) {
                        releaseXMLReader(reader);
                    }
                }

                if (DUMPTREE) {
                    System.out.println("Dumping SAX2DOM");
                    dtm.dumpDTM(System.err);
                }

                return dtm;
            }
            else {
                // It should have been handled by a derived class or the caller
                // made a mistake.
                throw new DTMException(XMLMessages.createXMLMessage(XMLErrorResources.ER_NOT_SUPPORTED, new Object[]{source}));
            }
        }
    }
}
