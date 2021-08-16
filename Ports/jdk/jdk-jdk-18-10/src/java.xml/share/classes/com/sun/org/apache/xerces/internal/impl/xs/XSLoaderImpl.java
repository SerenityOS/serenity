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

package com.sun.org.apache.xerces.internal.impl.xs;

import com.sun.org.apache.xerces.internal.impl.xs.util.XSGrammarPool;
import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xni.grammars.XSGrammar;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import com.sun.org.apache.xerces.internal.xs.LSInputList;
import com.sun.org.apache.xerces.internal.xs.StringList;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSLoader;
import com.sun.org.apache.xerces.internal.xs.XSModel;
import com.sun.org.apache.xerces.internal.xs.XSNamedMap;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMStringList;
import org.w3c.dom.ls.LSInput;

/**
 * <p>An implementation of XSLoader which wraps XMLSchemaLoader.</p>
 *
 * @xerces.internal
 *
 * @author Michael Glavassevich, IBM
 *
 */
public final class XSLoaderImpl implements XSLoader, DOMConfiguration {

    /**
     * Grammar pool. Need this to prevent us from
     * getting two grammars from the same namespace.
     */
    private final XSGrammarPool fGrammarPool = new XSGrammarMerger();

    /** Schema loader. **/
    private final XMLSchemaLoader fSchemaLoader = new XMLSchemaLoader();

    /**
     * No-args constructor.
     */
    public XSLoaderImpl() {
        fSchemaLoader.setProperty(XMLSchemaLoader.XMLGRAMMAR_POOL, fGrammarPool);
    }

    /**
     *  The configuration of a document. It maintains a table of recognized
     * parameters. Using the configuration, it is possible to change the
     * behavior of the load methods. The configuration may support the
     * setting of and the retrieval of the following non-boolean parameters
     * defined on the <code>DOMConfiguration</code> interface:
     * <code>error-handler</code> (<code>DOMErrorHandler</code>) and
     * <code>resource-resolver</code> (<code>LSResourceResolver</code>).
     * <br> The following list of boolean parameters is defined:
     * <dl>
     * <dt>
     * <code>"validate"</code></dt>
     * <dd>
     * <dl>
     * <dt><code>true</code></dt>
     * <dd>[required] (default) Validate an XML
     * Schema during loading. If validation errors are found, the error
     * handler is notified. </dd>
     * <dt><code>false</code></dt>
     * <dd>[optional] Do not
     * report errors during the loading of an XML Schema document. </dd>
     * </dl></dd>
     * </dl>
     */
    public DOMConfiguration getConfig() {
        return this;
    }

    /**
     * Parses the content of XML Schema documents specified as the list of URI
     * references. If the URI contains a fragment identifier, the behavior
     * is not defined by this specification.
     * @param uriList The list of URI locations.
     * @return An XSModel representing the schema documents.
     */
    public XSModel loadURIList(StringList uriList) {
        int length = uriList.getLength();
        try {
            fGrammarPool.clear();
            for (int i = 0; i < length; ++i) {
                fSchemaLoader.loadGrammar(new XMLInputSource(null, uriList.item(i), null, false));
            }
            return fGrammarPool.toXSModel();
        }
        catch (Exception e) {
            fSchemaLoader.reportDOMFatalError(e);
            return null;
        }
    }

    /**
     *  Parses the content of XML Schema documents specified as a list of
     * <code>LSInput</code>s.
     * @param is  The list of <code>LSInput</code>s from which the XML
     *   Schema documents are to be read.
     * @return An XSModel representing the schema documents.
     */
    public XSModel loadInputList(LSInputList is) {
        final int length = is.getLength();
        try {
            fGrammarPool.clear();
            for (int i = 0; i < length; ++i) {
                fSchemaLoader.loadGrammar(fSchemaLoader.dom2xmlInputSource(is.item(i)));
            }
            return fGrammarPool.toXSModel();
        }
        catch (Exception e) {
            fSchemaLoader.reportDOMFatalError(e);
            return null;
        }
    }

    /**
     * Parse an XML Schema document from a location identified by a URI
     * reference. If the URI contains a fragment identifier, the behavior is
     * not defined by this specification.
     * @param uri The location of the XML Schema document to be read.
     * @return An XSModel representing this schema.
     */
    public XSModel loadURI(String uri) {
        try {
            fGrammarPool.clear();
            return ((XSGrammar) fSchemaLoader.loadGrammar(new XMLInputSource(null, uri, null, false))).toXSModel();
        }
        catch (Exception e){
            fSchemaLoader.reportDOMFatalError(e);
            return null;
        }
    }

    /**
     *  Parse an XML Schema document from a resource identified by a
     * <code>LSInput</code> .
     * @param is  The <code>LSInput</code> from which the source
     *   document is to be read.
     * @return An XSModel representing this schema.
     */
    public XSModel load(LSInput is) {
        try {
            fGrammarPool.clear();
            return ((XSGrammar) fSchemaLoader.loadGrammar(fSchemaLoader.dom2xmlInputSource(is))).toXSModel();
        }
        catch (Exception e) {
            fSchemaLoader.reportDOMFatalError(e);
            return null;
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.dom3.DOMConfiguration#setParameter(java.lang.String, java.lang.Object)
     */
    public void setParameter(String name, Object value) throws DOMException {
        fSchemaLoader.setParameter(name, value);
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.dom3.DOMConfiguration#getParameter(java.lang.String)
     */
    public Object getParameter(String name) throws DOMException {
        return fSchemaLoader.getParameter(name);
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.dom3.DOMConfiguration#canSetParameter(java.lang.String, java.lang.Object)
     */
    public boolean canSetParameter(String name, Object value) {
        return fSchemaLoader.canSetParameter(name, value);
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.dom3.DOMConfiguration#getParameterNames()
     */
    public DOMStringList getParameterNames() {
        return fSchemaLoader.getParameterNames();
    }

    /**
     * Grammar pool which merges grammars from the same namespace into one. This eliminates
     * duplicate named components. It doesn't ensure that the grammar is consistent, however
     * this no worse than than the behaviour of XMLSchemaLoader alone when used as an XSLoader.
     */
    private static final class XSGrammarMerger extends XSGrammarPool {

        public XSGrammarMerger () {}

        public void putGrammar(Grammar grammar) {
            SchemaGrammar cachedGrammar =
                toSchemaGrammar(super.getGrammar(grammar.getGrammarDescription()));
            if (cachedGrammar != null) {
                SchemaGrammar newGrammar = toSchemaGrammar(grammar);
                if (newGrammar != null) {
                    mergeSchemaGrammars(cachedGrammar, newGrammar);
                }
            }
            else {
                super.putGrammar(grammar);
            }
        }

        private SchemaGrammar toSchemaGrammar (Grammar grammar) {
            return (grammar instanceof SchemaGrammar) ? (SchemaGrammar) grammar : null;
        }

        private void mergeSchemaGrammars(SchemaGrammar cachedGrammar, SchemaGrammar newGrammar) {

            /** Add new top-level element declarations. **/
            XSNamedMap map = newGrammar.getComponents(XSConstants.ELEMENT_DECLARATION);
            int length = map.getLength();
            for (int i = 0; i < length; ++i) {
                XSElementDecl decl = (XSElementDecl) map.item(i);
                if (cachedGrammar.getGlobalElementDecl(decl.getName()) == null) {
                    cachedGrammar.addGlobalElementDecl(decl);
                }
            }

            /** Add new top-level attribute declarations. **/
            map = newGrammar.getComponents(XSConstants.ATTRIBUTE_DECLARATION);
            length = map.getLength();
            for (int i = 0; i < length; ++i) {
                XSAttributeDecl decl = (XSAttributeDecl) map.item(i);
                if (cachedGrammar.getGlobalAttributeDecl(decl.getName()) == null) {
                    cachedGrammar.addGlobalAttributeDecl(decl);
                }
            }

            /** Add new top-level type definitions. **/
            map = newGrammar.getComponents(XSConstants.TYPE_DEFINITION);
            length = map.getLength();
            for (int i = 0; i < length; ++i) {
                XSTypeDefinition decl = (XSTypeDefinition) map.item(i);
                if (cachedGrammar.getGlobalTypeDecl(decl.getName()) == null) {
                    cachedGrammar.addGlobalTypeDecl(decl);
                }
            }

            /** Add new top-level attribute group definitions. **/
            map = newGrammar.getComponents(XSConstants.ATTRIBUTE_GROUP);
            length = map.getLength();
            for (int i = 0; i < length; ++i) {
                XSAttributeGroupDecl decl = (XSAttributeGroupDecl) map.item(i);
                if (cachedGrammar.getGlobalAttributeGroupDecl(decl.getName()) == null) {
                    cachedGrammar.addGlobalAttributeGroupDecl(decl);
                }
            }

            /** Add new top-level model group definitions. **/
            map = newGrammar.getComponents(XSConstants.MODEL_GROUP);
            length = map.getLength();
            for (int i = 0; i < length; ++i) {
                XSGroupDecl decl = (XSGroupDecl) map.item(i);
                if (cachedGrammar.getGlobalGroupDecl(decl.getName()) == null) {
                    cachedGrammar.addGlobalGroupDecl(decl);
                }
            }

            /** Add new top-level notation declarations. **/
            map = newGrammar.getComponents(XSConstants.NOTATION_DECLARATION);
            length = map.getLength();
            for (int i = 0; i < length; ++i) {
                XSNotationDecl decl = (XSNotationDecl) map.item(i);
                if (cachedGrammar.getGlobalNotationDecl(decl.getName()) == null) {
                    cachedGrammar.addGlobalNotationDecl(decl);
                }
            }

            /**
             * Add all annotations. Since these components are not named it's
             * possible we'll add duplicate components. There isn't much we can
             * do. It's no worse than XMLSchemaLoader when used as an XSLoader.
             */
            XSObjectList annotations = newGrammar.getAnnotations();
            length = annotations.getLength();
            for (int i = 0; i < length; ++i) {
                cachedGrammar.addAnnotation((XSAnnotationImpl) annotations.item(i));
            }

        }

        public boolean containsGrammar(XMLGrammarDescription desc) {
            return false;
        }

        public Grammar getGrammar(XMLGrammarDescription desc) {
            return null;
        }

        public Grammar retrieveGrammar(XMLGrammarDescription desc) {
            return null;
        }

        public Grammar [] retrieveInitialGrammarSet (String grammarType) {
            return new Grammar[0];
        }
    }
}
