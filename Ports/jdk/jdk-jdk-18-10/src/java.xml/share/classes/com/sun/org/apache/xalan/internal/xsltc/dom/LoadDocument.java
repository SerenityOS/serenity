/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.DOMCache;
import com.sun.org.apache.xalan.internal.xsltc.DOMEnhancedForDTM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.dtm.DTMManager;
import com.sun.org.apache.xml.internal.dtm.ref.EmptyIterator;
import com.sun.org.apache.xml.internal.utils.SystemIDResolver;
import java.io.FileNotFoundException;
import javax.xml.transform.stream.StreamSource;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.SecuritySupport;

/**
 * @author Morten Jorgensen
 * @LastModified: May 2021
 */
public final class LoadDocument {

    private static final String NAMESPACE_FEATURE =
       "http://xml.org/sax/features/namespaces";

    /**
     * Interprets the arguments passed from the document() function (see
     * com/sun/org/apache/xalan/internal/xsltc/compiler/DocumentCall.java) and returns an
     * iterator containing the requested nodes. Builds a union-iterator if
     * several documents are requested.
     * 2 arguments arg1 and arg2.  document(Obj, node-set) call
     */
    public static DTMAxisIterator documentF(Object arg1, DTMAxisIterator arg2,
                            String xslURI, AbstractTranslet translet, DOM dom)
    throws TransletException {
        String baseURI = null;
        final int arg2FirstNode = arg2.next();
        if (arg2FirstNode == DTMAxisIterator.END) {
            //  the second argument node-set is empty
            return EmptyIterator.getInstance();
        } else {
            //System.err.println("arg2FirstNode name: "
            //                   + dom.getNodeName(arg2FirstNode )+"["
            //                   +Integer.toHexString(arg2FirstNode )+"]");
            baseURI = dom.getDocumentURI(arg2FirstNode);
            if (!SystemIDResolver.isAbsoluteURI(baseURI))
               baseURI = SystemIDResolver.getAbsoluteURIFromRelative(baseURI);
        }

        try {
            if (arg1 instanceof String) {
                if (((String)arg1).length() == 0) {
                    return document(xslURI, "", translet, dom);
                } else {
                    return document((String)arg1, baseURI, translet, dom);
                }
            } else if (arg1 instanceof DTMAxisIterator) {
                return document((DTMAxisIterator)arg1, baseURI, translet, dom);
            } else {
                final String err = "document("+arg1.toString()+")";
                throw new IllegalArgumentException(err);
            }
        } catch (Exception e) {
            throw new TransletException(e);
        }
    }
    /**
     * Interprets the arguments passed from the document() function (see
     * com/sun/org/apache/xalan/internal/xsltc/compiler/DocumentCall.java) and returns an
     * iterator containing the requested nodes. Builds a union-iterator if
     * several documents are requested.
     * 1 arguments arg.  document(Obj) call
     */
    public static DTMAxisIterator documentF(Object arg, String xslURI,
                    AbstractTranslet translet, DOM dom)
    throws TransletException {
        try {
            if (arg instanceof String) {
                if (xslURI == null )
                    xslURI = "";

                String baseURI = xslURI;
                if (!SystemIDResolver.isAbsoluteURI(xslURI))
                   baseURI = SystemIDResolver.getAbsoluteURIFromRelative(xslURI);

                String href = (String)arg;
                if (href.length() == 0) {
                    href = "";
                    // %OPT% Optimization to cache the stylesheet DOM.
                    // The stylesheet DOM is built once and cached
                    // in the Templates object.
                    TemplatesImpl templates = (TemplatesImpl)translet.getTemplates();
                    DOM sdom = null;
                    if (templates != null) {
                        sdom = templates.getStylesheetDOM();
                    }

                    // If the cached dom exists, we need to migrate it
                    // to the new DTMManager and create a DTMAxisIterator
                    // for the document.
                    if (sdom != null) {
                        return document(sdom, translet, dom);
                    }
                    else {
                        return document(href, baseURI, translet, dom, true);
                    }
                }
                else {
                    return document(href, baseURI, translet, dom);
                }
            } else if (arg instanceof DTMAxisIterator) {
                return document((DTMAxisIterator)arg, null, translet, dom);
            } else {
                final String err = "document("+arg.toString()+")";
                throw new IllegalArgumentException(err);
            }
        } catch (Exception e) {
            throw new TransletException(e);
        }
    }

    private static DTMAxisIterator document(String uri, String base,
                    AbstractTranslet translet, DOM dom)
        throws Exception
    {
        return document(uri, base, translet, dom, false);
    }

    private static DTMAxisIterator document(String uri, String base,
                    AbstractTranslet translet, DOM dom,
                    boolean cacheDOM)
    throws Exception
    {
        try {
        final String originalUri = uri;
        MultiDOM multiplexer = (MultiDOM)dom;

        // Prepend URI base to URI (from context)
        if (base != null && !base.equals("")) {
            uri = SystemIDResolver.getAbsoluteURI(uri, base);
        }

        // Return an empty iterator if the URI is clearly invalid
        // (to prevent some unncessary MalformedURL exceptions).
        if (uri == null || uri.equals("")) {
            return(EmptyIterator.getInstance());
        }

        // Check if this DOM has already been added to the multiplexer
        int mask = multiplexer.getDocumentMask(uri);
        if (mask != -1) {
            DOM newDom = ((DOMAdapter)multiplexer.getDOMAdapter(uri))
                                       .getDOMImpl();
            if (newDom instanceof DOMEnhancedForDTM) {
                return new SingletonIterator(((DOMEnhancedForDTM)newDom)
                                                               .getDocument(),
                                             true);
            }
        }

        // Check if we can get the DOM from a DOMCache
        DOMCache cache = translet.getDOMCache();
        DOM newdom;

        mask = multiplexer.nextMask(); // peek

        if (cache != null) {
            newdom = cache.retrieveDocument(base, originalUri, translet);
            if (newdom == null) {
                final Exception e = new FileNotFoundException(originalUri);
                throw new TransletException(e);
            }
        } else {
            String accessError = SecuritySupport.checkAccess(uri, translet.getAllowedProtocols(), JdkConstants.ACCESS_EXTERNAL_ALL);
            if (accessError != null) {
                ErrorMsg msg = new ErrorMsg(ErrorMsg.ACCESSING_XSLT_TARGET_ERR,
                        SecuritySupport.sanitizePath(uri), accessError);
                throw new Exception(msg.toString());
            }

            // Parse the input document and construct DOM object
            // Trust the DTMManager to pick the right parser and
            // set up the DOM correctly.
            XSLTCDTMManager dtmManager = (XSLTCDTMManager)multiplexer
                                                              .getDTMManager();
            DOMEnhancedForDTM enhancedDOM =
                    (DOMEnhancedForDTM) dtmManager.getDTM(new StreamSource(uri),
                                            false, null, true, false,
                                            translet.hasIdCall(), cacheDOM);
            newdom = enhancedDOM;

            // Cache the stylesheet DOM in the Templates object
            if (cacheDOM) {
                TemplatesImpl templates = (TemplatesImpl)translet.getTemplates();
                if (templates != null) {
                    templates.setStylesheetDOM(enhancedDOM);
                }
            }

            translet.prepassDocument(enhancedDOM);
            enhancedDOM.setDocumentURI(uri);
        }

        // Wrap the DOM object in a DOM adapter and add to multiplexer
        final DOMAdapter domAdapter = translet.makeDOMAdapter(newdom);
        multiplexer.addDOMAdapter(domAdapter);

        // Create index for any key elements
        translet.buildKeys(domAdapter, null, null, newdom.getDocument());

        // Return a singleton iterator containing the root node
        return new SingletonIterator(newdom.getDocument(), true);
        } catch (Exception e) {
            throw e;
        }
    }


    private static DTMAxisIterator document(DTMAxisIterator arg1,
                                            String baseURI,
                                            AbstractTranslet translet, DOM dom)
    throws Exception
    {
        UnionIterator union = new UnionIterator(dom);
        int node = DTM.NULL;

        while ((node = arg1.next()) != DTM.NULL) {
            String uri = dom.getStringValueX(node);
            //document(node-set) if true;  document(node-set,node-set) if false
            if (baseURI  == null) {
               baseURI = dom.getDocumentURI(node);
               if (!SystemIDResolver.isAbsoluteURI(baseURI))
                    baseURI = SystemIDResolver.getAbsoluteURIFromRelative(baseURI);
            }
            union.addIterator(document(uri, baseURI, translet, dom));
        }
        return(union);
    }

    /**
     * Create a DTMAxisIterator for the newdom. This is currently only
     * used to create an iterator for the cached stylesheet DOM.
     *
     * @param newdom the cached stylesheet DOM
     * @param translet the translet
     * @param the main dom (should be a MultiDOM)
     * @return a DTMAxisIterator from the document root
     */
    private static DTMAxisIterator document(DOM newdom,
                                            AbstractTranslet translet,
                                            DOM dom)
        throws Exception
    {
        DTMManager dtmManager = ((MultiDOM)dom).getDTMManager();
        // Need to migrate the cached DTM to the new DTMManager
        if (dtmManager != null && newdom instanceof DTM) {
            ((DTM)newdom).migrateTo(dtmManager);
        }

        translet.prepassDocument(newdom);

        // Wrap the DOM object in a DOM adapter and add to multiplexer
        final DOMAdapter domAdapter = translet.makeDOMAdapter(newdom);
        ((MultiDOM)dom).addDOMAdapter(domAdapter);

        // Create index for any key elements
        translet.buildKeys(domAdapter, null, null,
                           newdom.getDocument());

        // Return a singleton iterator containing the root node
        return new SingletonIterator(newdom.getDocument(), true);
    }

}
