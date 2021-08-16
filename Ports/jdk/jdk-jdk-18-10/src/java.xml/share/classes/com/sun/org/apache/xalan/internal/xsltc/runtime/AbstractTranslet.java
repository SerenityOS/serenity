/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xalan.internal.xsltc.runtime;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.DOMCache;
import com.sun.org.apache.xalan.internal.xsltc.DOMEnhancedForDTM;
import com.sun.org.apache.xalan.internal.xsltc.Translet;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.dom.DOMAdapter;
import com.sun.org.apache.xalan.internal.xsltc.dom.KeyIndex;
import com.sun.org.apache.xalan.internal.xsltc.runtime.output.TransletOutputHandlerFactory;
import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Templates;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkXmlUtils;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 * @author G. Todd Miller
 * @author John Howard, JohnH@schemasoft.com
 * @LastModified: May 2021
 */
public abstract class AbstractTranslet implements Translet {

    // These attributes are extracted from the xsl:output element. They also
    // appear as fields (with the same type, only public) in Output.java
    public String  _version = "1.0";
    public String  _method = null;
    public String  _encoding = "UTF-8";
    public boolean _omitHeader = false;
    public String  _standalone = null;
    //see JdkConstants.SP_XSLTC_IS_Standalone
    public boolean  _isStandalone = false;
    public String  _doctypePublic = null;
    public String  _doctypeSystem = null;
    public boolean _indent = false;
    public String  _mediaType = null;
    public List<String> _cdata = null;
    public int _indentamount = -1;

    public static final int FIRST_TRANSLET_VERSION = 100;
    public static final int VER_SPLIT_NAMES_ARRAY = 101;
    public static final int CURRENT_TRANSLET_VERSION = VER_SPLIT_NAMES_ARRAY;

    // Initialize Translet version field to base value.  A class that extends
    // AbstractTranslet may override this value to a more recent translet
    // version; if it doesn't override the value (because it was compiled
    // before the notion of a translet version was introduced, it will get
    // this default value).
    protected int transletVersion = FIRST_TRANSLET_VERSION;

    // DOM/translet handshaking - the arrays are set by the compiled translet
    protected String[] namesArray;
    protected String[] urisArray;
    protected int[]    typesArray;
    protected String[] namespaceArray;

    // The Templates object that is used to create this Translet instance
    protected Templates _templates = null;

    // Boolean flag to indicate whether this translet has id functions.
    protected boolean _hasIdCall = false;

    // TODO - these should only be instanciated when needed
    protected StringValueHandler stringValueHandler = new StringValueHandler();

    // Use one empty string instead of constantly instanciating String("");
    private final static String EMPTYSTRING = "";

    // This is the name of the index used for ID attributes
    private final static String ID_INDEX_NAME = "##id";

    private boolean _overrideDefaultParser;

    // The OutputStream for redirect function
    private FileOutputStream output = null;

    /**
     * protocols allowed for external references set by the stylesheet processing instruction, Document() function, Import and Include element.
     */
    private String _accessExternalStylesheet = JdkConstants.EXTERNAL_ACCESS_DEFAULT;

    /************************************************************************
     * Debugging
     ************************************************************************/
    public void printInternalState() {
        System.out.println("-------------------------------------");
        System.out.println("AbstractTranslet this = " + this);
        System.out.println("pbase = " + pbase);
        System.out.println("vframe = " + pframe);
        System.out.println("paramsStack.size() = " + paramsStack.size());
        System.out.println("namesArray.size = " + namesArray.length);
        System.out.println("namespaceArray.size = " + namespaceArray.length);
        System.out.println("");
        System.out.println("Total memory = " + Runtime.getRuntime().totalMemory());
    }

    /**
     * Wrap the initial input DOM in a dom adapter. This adapter is wrapped in
     * a DOM multiplexer if the document() function is used (handled by compiled
     * code in the translet - see compiler/Stylesheet.compileTransform()).
     */
    public final DOMAdapter makeDOMAdapter(DOM dom)
        throws TransletException {
        setRootForKeys(dom.getDocument());
        return new DOMAdapter(dom, namesArray, urisArray, typesArray, namespaceArray);
    }

    /************************************************************************
     * Parameter handling
     ************************************************************************/

    // Parameter's stack: <tt>pbase</tt> and <tt>pframe</tt> are used
    // to denote the current parameter frame.
    protected int pbase = 0, pframe = 0;
    protected List<Object> paramsStack = new ArrayList<>();

    /**
     * Push a new parameter frame.
     */
    public final void pushParamFrame() {
        paramsStack.add(pframe, pbase);
        pbase = ++pframe;
    }

    /**
     * Pop the topmost parameter frame.
     */
    public final void popParamFrame() {
        if (pbase > 0) {
            final int oldpbase = ((Integer)paramsStack.get(--pbase));
            for (int i = pframe - 1; i >= pbase; i--) {
                paramsStack.remove(i);
            }
            pframe = pbase; pbase = oldpbase;
        }
    }

    /**
     * Add a new global parameter if not already in the current frame.
     * To setParameters of the form {http://foo.bar}xyz
     * This needs to get mapped to an instance variable in the class
     * The mapping  created so that
     * the global variables in the generated class become
     * http$colon$$flash$$flash$foo$dot$bar$colon$xyz
     */
    public final Object addParameter(String name, Object value) {
        name = BasisLibrary.mapQNameToJavaName (name);
        return addParameter(name, value, false);
    }

    /**
     * Add a new global or local parameter if not already in the current frame.
     * The 'isDefault' parameter is set to true if the value passed is the
     * default value from the <xsl:parameter> element's select attribute or
     * element body.
     */
    public final Object addParameter(String name, Object value,
        boolean isDefault)
    {
        // Local parameters need to be re-evaluated for each iteration
        for (int i = pframe - 1; i >= pbase; i--) {
            final Parameter param = (Parameter) paramsStack.get(i);

            if (param._name.equals(name)) {
                // Only overwrite if current value is the default value and
                // the new value is _NOT_ the default value.
                if (param._isDefault || !isDefault) {
                    param._value = value;
                    param._isDefault = isDefault;
                    return value;
                }
                return param._value;
            }
        }

        // Add new parameter to parameter stack
        paramsStack.add(pframe++, new Parameter(name, value, isDefault));
        return value;
    }

    /**
     * Clears the parameter stack.
     */
    public void clearParameters() {
        pbase = pframe = 0;
        paramsStack.clear();
    }

    /**
     * Get the value of a parameter from the current frame or
     * <tt>null</tt> if undefined.
     */
    public final Object getParameter(String name) {

        name = BasisLibrary.mapQNameToJavaName (name);

        for (int i = pframe - 1; i >= pbase; i--) {
            final Parameter param = (Parameter)paramsStack.get(i);
            if (param._name.equals(name)) return param._value;
        }
        return null;
    }

    /************************************************************************
     * Message handling - implementation of <xsl:message>
     ************************************************************************/

    // Holds the translet's message handler - used for <xsl:message>.
    // The deault message handler dumps a string stdout, but anything can be
    // used, such as a dialog box for applets, etc.
    private MessageHandler _msgHandler = null;

    /**
     * Set the translet's message handler - must implement MessageHandler
     */
    public final void setMessageHandler(MessageHandler handler) {
        _msgHandler = handler;
    }

    /**
     * Pass a message to the message handler - used by Message class.
     */
    public final void displayMessage(String msg) {
        if (_msgHandler == null) {
            System.err.println(msg);
        }
        else {
            _msgHandler.displayMessage(msg);
        }
    }

    /************************************************************************
     * Decimal number format symbol handling
     ************************************************************************/

    // Contains decimal number formatting symbols used by FormatNumberCall
    public Map<String, DecimalFormat> _formatSymbols = null;

    /**
     * Adds a DecimalFormat object to the _formatSymbols map.
     * The entry is created with the input DecimalFormatSymbols.
     */
    public void addDecimalFormat(String name, DecimalFormatSymbols symbols) {
        // Instanciate map for formatting symbols if needed
        if (_formatSymbols == null) _formatSymbols = new HashMap<>();

        // The name cannot be null - use empty string instead
        if (name == null) name = EMPTYSTRING;

        // Construct a DecimalFormat object containing the symbols we got
        final DecimalFormat df = new DecimalFormat();
        if (symbols != null) {
            df.setDecimalFormatSymbols(symbols);
        }
        _formatSymbols.put(name, df);
    }

    /**
     * Retrieves a named DecimalFormat object from the _formatSymbols map.
     */
    public final DecimalFormat getDecimalFormat(String name) {

        if (_formatSymbols != null) {
            // The name cannot be null - use empty string instead
            if (name == null) name = EMPTYSTRING;

            DecimalFormat df = _formatSymbols.get(name);
            if (df == null) df = _formatSymbols.get(EMPTYSTRING);
            return df;
        }
        return(null);
    }

    /**
     * Give the translet an opportunity to perform a prepass on the document
     * to extract any information that it can store in an optimized form.
     *
     * Currently, it only extracts information about attributes of type ID.
     */
    public final void prepassDocument(DOM document) {
        setIndexSize(document.getSize());
        buildIDIndex(document);
    }

    /**
     * Leverages the Key Class to implement the XSLT id() function.
     * buildIdIndex creates the index (##id) that Key Class uses.
     * The index contains the element node index (int) and Id value (String).
     */
    private final void buildIDIndex(DOM document) {
        setRootForKeys(document.getDocument());

        if (document instanceof DOMEnhancedForDTM) {
            DOMEnhancedForDTM enhancedDOM = (DOMEnhancedForDTM)document;

            // If the input source is DOMSource, the KeyIndex table is not
            // built at this time. It will be built later by the lookupId()
            // and containsId() methods of the KeyIndex class.
            if (enhancedDOM.hasDOMSource()) {
                buildKeyIndex(ID_INDEX_NAME, document);
                return;
            }
            else {
                final Map<String, Integer> elementsByID = enhancedDOM.getElementsWithIDs();

                if (elementsByID == null) {
                    return;
                }

                // Given a Map of DTM nodes indexed by ID attribute values,
                // loop through the table copying information to a KeyIndex
                // for the mapping from ID attribute value to DTM node
                boolean hasIDValues = false;
                for (Map.Entry<String, Integer> entry : elementsByID.entrySet()) {
                    final int element = document.getNodeHandle(entry.getValue());
                    buildKeyIndex(ID_INDEX_NAME, element, entry.getKey());
                    hasIDValues = true;
                }

                if (hasIDValues) {
                    setKeyIndexDom(ID_INDEX_NAME, document);
                }
            }
        }
    }

    /**
     * After constructing the translet object, this method must be called to
     * perform any version-specific post-initialization that's required.
     */
    public final void postInitialization() {
        // If the version of the translet had just one namesArray, split
        // it into multiple fields.
        if (transletVersion < VER_SPLIT_NAMES_ARRAY) {
            int arraySize = namesArray.length;
            String[] newURIsArray = new String[arraySize];
            String[] newNamesArray = new String[arraySize];
            int[] newTypesArray = new int[arraySize];

            for (int i = 0; i < arraySize; i++) {
                String name = namesArray[i];
                int colonIndex = name.lastIndexOf(':');
                int lNameStartIdx = colonIndex+1;

                if (colonIndex > -1) {
                    newURIsArray[i] = name.substring(0, colonIndex);
                }

               // Distinguish attribute and element names.  Attribute has
               // @ before local part of name.
               if (name.charAt(lNameStartIdx) == '@') {
                   lNameStartIdx++;
                   newTypesArray[i] = DTM.ATTRIBUTE_NODE;
               } else if (name.charAt(lNameStartIdx) == '?') {
                   lNameStartIdx++;
                   newTypesArray[i] = DTM.NAMESPACE_NODE;
               } else {
                   newTypesArray[i] = DTM.ELEMENT_NODE;
               }
               newNamesArray[i] =
                          (lNameStartIdx == 0) ? name
                                               : name.substring(lNameStartIdx);
            }

            namesArray = newNamesArray;
            urisArray  = newURIsArray;
            typesArray = newTypesArray;
        }

        // Was translet compiled using a more recent version of the XSLTC
        // compiler than is known by the AbstractTranslet class?  If, so
        // and we've made it this far (which is doubtful), we should give up.
        if (transletVersion > CURRENT_TRANSLET_VERSION) {
            BasisLibrary.runTimeError(BasisLibrary.UNKNOWN_TRANSLET_VERSION_ERR,
                                      this.getClass().getName());
        }
    }

    /************************************************************************
     * Index(es) for <xsl:key> / key() / id()
     ************************************************************************/

    // Container for all indexes for xsl:key elements
    private Map<String, KeyIndex> _keyIndexes = null;
    private KeyIndex  _emptyKeyIndex = null;
    private int       _indexSize = 0;
    private int       _currentRootForKeys = 0;

    /**
     * This method is used to pass the largest DOM size to the translet.
     * Needed to make sure that the translet can index the whole DOM.
     */
    public void setIndexSize(int size) {
        if (size > _indexSize) _indexSize = size;
    }

    /**
     * Creates a KeyIndex object of the desired size - don't want to resize!!!
     */
    public KeyIndex createKeyIndex() {
        return(new KeyIndex(_indexSize));
    }

    /**
     * Adds a value to a key/id index
     *   @param name is the name of the index (the key or ##id)
     *   @param node is the node handle of the node to insert
     *   @param value is the value that will look up the node in the given index
     */
    public void buildKeyIndex(String name, int node, String value) {
        KeyIndex index = buildKeyIndexHelper(name);
        index.add(value, node, _currentRootForKeys);
    }

    /**
     * Create an empty KeyIndex in the DOM case
     *   @param name is the name of the index (the key or ##id)
     *   @param dom is the DOM
     */
    public void buildKeyIndex(String name, DOM dom) {
        KeyIndex index = buildKeyIndexHelper(name);
        index.setDom(dom, dom.getDocument());
    }

    /**
     * Return KeyIndex for the buildKeyIndex methods. Note the difference from the
     * public getKeyIndex method, this method creates a new Map if keyIndexes does
     * not exist.
     *
     * @param name the name of the index (the key or ##id)
     * @return a KeyIndex.
     */
    private KeyIndex buildKeyIndexHelper(String name) {
        if (_keyIndexes == null) _keyIndexes = new HashMap<>();

        KeyIndex index = _keyIndexes.get(name);
        if (index == null) {
            _keyIndexes.put(name, index = new KeyIndex(_indexSize));
        }
        return index;
    }

    /**
     * Returns the index for a given key (or id).
     * The index implements our internal iterator interface
     * @param name the name of the index (the key or ##id)
     * @return a KeyIndex.
     */
    public KeyIndex getKeyIndex(String name) {
        // Return an empty key index iterator if none are defined
        if (_keyIndexes == null) {
            return (_emptyKeyIndex != null)
                ? _emptyKeyIndex
                : (_emptyKeyIndex = new KeyIndex(1));
        }

        // Look up the requested key index
        final KeyIndex index = _keyIndexes.get(name);

        // Return an empty key index iterator if the requested index not found
        if (index == null) {
            return (_emptyKeyIndex != null)
                ? _emptyKeyIndex
                : (_emptyKeyIndex = new KeyIndex(1));
        }

        return(index);
    }

    private void setRootForKeys(int root) {
        _currentRootForKeys = root;
    }

    /**
     * This method builds key indexes - it is overridden in the compiled
     * translet in cases where the <xsl:key> element is used
     */
    public void buildKeys(DOM document, DTMAxisIterator iterator,
                          SerializationHandler handler,
                          int root) throws TransletException {

    }

    /**
     * This method builds key indexes - it is overridden in the compiled
     * translet in cases where the <xsl:key> element is used
     */
    public void setKeyIndexDom(String name, DOM document) {
        getKeyIndex(name).setDom(document, document.getDocument());
    }

    /************************************************************************
     * DOM cache handling
     ************************************************************************/

    // Hold the DOM cache (if any) used with this translet
    private DOMCache _domCache = null;

    /**
     * Sets the DOM cache used for additional documents loaded using the
     * document() function.
     */
    public void setDOMCache(DOMCache cache) {
        _domCache = cache;
    }

    /**
     * Returns the DOM cache used for this translet. Used by the LoadDocument
     * class (if present) when the document() function is used.
     */
    public DOMCache getDOMCache() {
        return(_domCache);
    }

    /************************************************************************
     * Multiple output document extension.
     * See compiler/TransletOutput for actual implementation.
     ************************************************************************/

    public SerializationHandler openOutputHandler(String filename, boolean append)
        throws TransletException
    {
        try {
            final TransletOutputHandlerFactory factory = TransletOutputHandlerFactory
                    .newInstance(_overrideDefaultParser, _msgHandler.getErrorListener());

            String dirStr = new File(filename).getParent();
            if ((null != dirStr) && (dirStr.length() > 0)) {
               File dir = new File(dirStr);
               dir.mkdirs();
            }

            output = new FileOutputStream(filename, append);
            factory.setEncoding(_encoding);
            factory.setOutputMethod(_method);
            factory.setOutputStream(new BufferedOutputStream(output));
            factory.setOutputType(TransletOutputHandlerFactory.STREAM);

            final SerializationHandler handler
                = factory.getSerializationHandler();

            transferOutputSettings(handler);
            handler.startDocument();
            return handler;
        }
        catch (Exception e) {
            throw new TransletException(e);
        }
    }

    public SerializationHandler openOutputHandler(String filename)
       throws TransletException
    {
       return openOutputHandler(filename, false);
    }

    public void closeOutputHandler(SerializationHandler handler) {
        try {
            handler.endDocument();
            handler.close();
            if (output != null) {
                output.close();
            }
        }
        catch (Exception e) {
            // what can you do?
        }
    }

    /************************************************************************
     * Native API transformation methods - _NOT_ JAXP/TrAX
     ************************************************************************/

    /**
     * Main transform() method - this is overridden by the compiled translet
     */
    public abstract void transform(DOM document, DTMAxisIterator iterator,
                                   SerializationHandler handler)
        throws TransletException;

    /**
     * Calls transform() with a given output handler
     */
    public final void transform(DOM document, SerializationHandler handler)
        throws TransletException {
        try {
            transform(document, document.getIterator(), handler);
        } finally {
            _keyIndexes = null;
        }
    }

    /**
     * Used by some compiled code as a shortcut for passing strings to the
     * output handler
     */
    public final void characters(final String string,
                                 SerializationHandler handler)
        throws TransletException {
        if (string != null) {
           //final int length = string.length();
           try {
               handler.characters(string);
           } catch (Exception e) {
               throw new TransletException(e);
           }
        }
    }

    /**
     * Add's a name of an element whose text contents should be output as CDATA
     */
    public void addCdataElement(String name) {
        if (_cdata == null) {
            _cdata = new ArrayList<>();
        }

        int lastColon = name.lastIndexOf(':');

        if (lastColon > 0) {
            String uri = name.substring(0, lastColon);
            String localName = name.substring(lastColon+1);
            _cdata.add(uri);
            _cdata.add(localName);
        } else {
            _cdata.add(null);
            _cdata.add(name);
        }
    }

    /**
     * Transfer the output settings to the output post-processor
     */
    protected void transferOutputSettings(SerializationHandler handler) {
        if (_method != null) {
            if (_method.equals("xml")) {
                if (_standalone != null) {
                    handler.setStandalone(_standalone);
                }
                if (_omitHeader) {
                    handler.setOmitXMLDeclaration(true);
                }
                handler.setCdataSectionElements(_cdata);
                if (_version != null) {
                    handler.setVersion(_version);
                }
                handler.setIndent(_indent);
                if (_indentamount >= 0)
                    handler.setIndentAmount(_indentamount);
                if (_doctypeSystem != null) {
                    handler.setDoctype(_doctypeSystem, _doctypePublic);
                }
                handler.setIsStandalone(_isStandalone);
            }
            else if (_method.equals("html")) {
                handler.setIndent(_indent);
                handler.setDoctype(_doctypeSystem, _doctypePublic);
                if (_mediaType != null) {
                    handler.setMediaType(_mediaType);
                }
            }
        }
        else {
            handler.setCdataSectionElements(_cdata);
            if (_version != null) {
                handler.setVersion(_version);
            }
            if (_standalone != null) {
                handler.setStandalone(_standalone);
            }
            if (_omitHeader) {
                handler.setOmitXMLDeclaration(true);
            }
            handler.setIndent(_indent);
            handler.setDoctype(_doctypeSystem, _doctypePublic);
            handler.setIsStandalone(_isStandalone);
        }
    }

    private Map<String, Class<?>> _auxClasses = null;

    public void addAuxiliaryClass(Class<?> auxClass) {
        if (_auxClasses == null) _auxClasses = new HashMap<>();
        _auxClasses.put(auxClass.getName(), auxClass);
    }

    public void setAuxiliaryClasses(Map<String, Class<?>> auxClasses) {
        _auxClasses = auxClasses;
    }

    public Class<?> getAuxiliaryClass(String className) {
        if (_auxClasses == null) return null;
        return((Class)_auxClasses.get(className));
    }

    // GTM added (see pg 110)
    public String[] getNamesArray() {
        return namesArray;
    }

    public String[] getUrisArray() {
        return urisArray;
    }

    public int[] getTypesArray() {
        return typesArray;
    }

    public String[] getNamespaceArray() {
        return namespaceArray;
    }

    public boolean hasIdCall() {
        return _hasIdCall;
    }

    public Templates getTemplates() {
        return _templates;
    }

    public void setTemplates(Templates templates) {
        _templates = templates;
    }
    /**
     * Return the state of the services mechanism feature.
     */
    public boolean overrideDefaultParser() {
        return _overrideDefaultParser;
    }

    /**
     * Set the state of the services mechanism feature.
     */
    public void setOverrideDefaultParser(boolean flag) {
        _overrideDefaultParser = flag;
    }

    /**
     * Return allowed protocols for accessing external stylesheet.
     */
    public String getAllowedProtocols() {
        return _accessExternalStylesheet;
    }

    /**
     * Set allowed protocols for accessing external stylesheet.
     */
    public void setAllowedProtocols(String protocols) {
        _accessExternalStylesheet = protocols;
    }

    /************************************************************************
     * DOMImplementation caching for basis library
     ************************************************************************/
    protected DOMImplementation _domImplementation = null;

    public Document newDocument(String uri, String qname)
        throws ParserConfigurationException
    {
        if (_domImplementation == null) {
            DocumentBuilderFactory dbf = JdkXmlUtils.getDOMFactory(_overrideDefaultParser);
            _domImplementation = dbf.newDocumentBuilder().getDOMImplementation();
        }
        return _domImplementation.createDocument(uri, qname, null);
    }
}
