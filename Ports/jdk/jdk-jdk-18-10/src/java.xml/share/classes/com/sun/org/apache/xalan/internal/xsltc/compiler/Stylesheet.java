/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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
/*
 * $Id: Stylesheet.java,v 1.5 2005/09/28 13:48:16 pvedula Exp $
 */

package com.sun.org.apache.xalan.internal.xsltc.compiler;

import com.sun.org.apache.bcel.internal.generic.ANEWARRAY;
import com.sun.org.apache.bcel.internal.generic.BasicType;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.FieldGen;
import com.sun.org.apache.bcel.internal.generic.GETFIELD;
import com.sun.org.apache.bcel.internal.generic.GETSTATIC;
import com.sun.org.apache.bcel.internal.generic.INVOKEINTERFACE;
import com.sun.org.apache.bcel.internal.generic.INVOKESPECIAL;
import com.sun.org.apache.bcel.internal.generic.INVOKESTATIC;
import com.sun.org.apache.bcel.internal.generic.INVOKEVIRTUAL;
import com.sun.org.apache.bcel.internal.generic.ISTORE;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.LocalVariableGen;
import com.sun.org.apache.bcel.internal.generic.NEW;
import com.sun.org.apache.bcel.internal.generic.NEWARRAY;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.sun.org.apache.bcel.internal.generic.PUTFIELD;
import com.sun.org.apache.bcel.internal.generic.PUTSTATIC;
import com.sun.org.apache.bcel.internal.generic.TargetLostException;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.utils.SystemIDResolver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 * @LastModified: Oct 2017
 */
public final class Stylesheet extends SyntaxTreeNode {

    /**
     * XSLT version defined in the stylesheet.
     */
    private String _version;

    /**
     * Internal name of this stylesheet used as a key into the symbol table.
     */
    private QName _name;

    /**
     * A URI that represents the system ID for this stylesheet.
     */
    private String _systemId;

    /**
     * A reference to the parent stylesheet or null if topmost.
     */
    private Stylesheet _parentStylesheet;

    /**
     * Contains global variables and parameters defined in the stylesheet.
     */
    private List<VariableBase> _globals = new ArrayList<>();

    /**
     * Used to cache the result returned by <code>hasLocalParams()</code>.
     */
    private Boolean _hasLocalParams = null;

    /**
     * The name of the class being generated.
     */
    private String _className;

    /**
      * Contains all templates defined in this stylesheet
      */
    private final List<Template> _templates = new ArrayList<>();

    /**
     * Used to cache result of <code>getAllValidTemplates()</code>. Only
     * set in top-level stylesheets that include/import other stylesheets.
     */
    private List<Template> _allValidTemplates = null;

    /**
     * Counter to generate unique mode suffixes.
     */
    private int _nextModeSerial = 1;

    /**
     * Mapping between mode names and Mode instances.
     */
    private final Map<String, Mode> _modes = new HashMap<>();

    /**
     * A reference to the default Mode object.
     */
    private Mode _defaultMode;

    /**
     * Mapping between extension URIs and their prefixes.
     */
    private final Map<String, String> _extensions = new HashMap<>();

    /**
     * Reference to the stylesheet from which this stylesheet was
     * imported (if any).
     */
    public Stylesheet _importedFrom = null;

    /**
     * Reference to the stylesheet from which this stylesheet was
     * included (if any).
     */
    public Stylesheet _includedFrom = null;

    /**
     * Array of all the stylesheets imported or included from this one.
     */
    private List<Stylesheet> _includedStylesheets = null;

    /**
     * Import precendence for this stylesheet.
     */
    private int _importPrecedence = 1;

    /**
     * Minimum precendence of any descendant stylesheet by inclusion or
     * importation.
     */
    private int _minimumDescendantPrecedence = -1;

    /**
     * Mapping between key names and Key objects (needed by Key/IdPattern).
     */
    private Map<String, Key> _keys = new HashMap<>();

    /**
     * A reference to the SourceLoader set by the user (a URIResolver
     * if the JAXP API is being used).
     */
    private SourceLoader _loader = null;

    /**
     * Flag indicating if format-number() is called.
     */
    private boolean _numberFormattingUsed = false;

    /**
     * Flag indicating if this is a simplified stylesheets. A template
     * matching on "/" must be added in this case.
     */
    private boolean _simplified = false;

    /**
     * Flag indicating if multi-document support is needed.
     */
    private boolean _multiDocument = false;

    /**
     * Flag indicating if nodset() is called.
     */
    private boolean _callsNodeset = false;

    /**
     * Flag indicating if id() is called.
     */
    private boolean _hasIdCall = false;

    /**
     * Set to true to enable template inlining optimization.
     * @see XSLTC#_templateInlining
     */
    private boolean _templateInlining = false;

    /**
     * A reference to the last xsl:output object found in the styleshet.
     */
    private Output  _lastOutputElement = null;

    /**
     * Output properties for this stylesheet.
     */
    private Properties _outputProperties = null;

    /**
     * Output method for this stylesheet (must be set to one of
     * the constants defined below).
     */
    private int _outputMethod = UNKNOWN_OUTPUT;

    // Output method constants
    public static final int UNKNOWN_OUTPUT = 0;
    public static final int XML_OUTPUT     = 1;
    public static final int HTML_OUTPUT    = 2;
    public static final int TEXT_OUTPUT    = 3;

    /**
     * Return the output method
     */
    public int getOutputMethod() {
        return _outputMethod;
    }

    /**
     * Check and set the output method
     */
    private void checkOutputMethod() {
        if (_lastOutputElement != null) {
            String method = _lastOutputElement.getOutputMethod();
            if (method != null) {
                if (method.equals("xml"))
                    _outputMethod = XML_OUTPUT;
                else if (method.equals("html"))
                    _outputMethod = HTML_OUTPUT;
                else if (method.equals("text"))
                    _outputMethod = TEXT_OUTPUT;
            }
        }
    }

    public boolean getTemplateInlining() {
        return _templateInlining;
    }

    public void setTemplateInlining(boolean flag) {
        _templateInlining = flag;
    }

    public boolean isSimplified() {
        return(_simplified);
    }

    public void setSimplified() {
        _simplified = true;
    }

    public void setHasIdCall(boolean flag) {
        _hasIdCall = flag;
    }

    public void setOutputProperty(String key, String value) {
        if (_outputProperties == null) {
            _outputProperties = new Properties();
        }
        _outputProperties.setProperty(key, value);
    }

    public void setOutputProperties(Properties props) {
        _outputProperties = props;
    }

    public Properties getOutputProperties() {
        return _outputProperties;
    }

    public Output getLastOutputElement() {
        return _lastOutputElement;
    }

    public void setMultiDocument(boolean flag) {
        _multiDocument = flag;
    }

    public boolean isMultiDocument() {
        return _multiDocument;
    }

    public void setCallsNodeset(boolean flag) {
        if (flag) setMultiDocument(flag);
        _callsNodeset = flag;
    }

    public boolean callsNodeset() {
        return _callsNodeset;
    }

    public void numberFormattingUsed() {
        _numberFormattingUsed = true;
        /*
         * Fix for bug 23046, if the stylesheet is included, set the
         * numberFormattingUsed flag to the parent stylesheet too.
         * AbstractTranslet.addDecimalFormat() will be inlined once for the
         * outer most stylesheet.
         */
        Stylesheet parent = getParentStylesheet();
        if (null != parent) parent.numberFormattingUsed();
    }

    public void setImportPrecedence(final int precedence) {
        // Set import precedence for this stylesheet
        _importPrecedence = precedence;

        // Set import precedence for all included stylesheets
        final Iterator<SyntaxTreeNode> elements = elements();
        while (elements.hasNext()) {
            SyntaxTreeNode child = elements.next();
            if (child instanceof Include) {
                Stylesheet included = ((Include)child).getIncludedStylesheet();
                if (included != null && included._includedFrom == this) {
                    included.setImportPrecedence(precedence);
                }
            }
        }

        // Set import precedence for the stylesheet that imported this one
        if (_importedFrom != null) {
            if (_importedFrom.getImportPrecedence() < precedence) {
                final Parser parser = getParser();
                final int nextPrecedence = parser.getNextImportPrecedence();
                _importedFrom.setImportPrecedence(nextPrecedence);
            }
        }
        // Set import precedence for the stylesheet that included this one
        else if (_includedFrom != null) {
            if (_includedFrom.getImportPrecedence() != precedence)
                _includedFrom.setImportPrecedence(precedence);
        }
    }

    public int getImportPrecedence() {
        return _importPrecedence;
    }

    /**
     * Get the minimum of the precedence of this stylesheet, any stylesheet
     * imported by this stylesheet and any include/import descendant of this
     * stylesheet.
     */
    public int getMinimumDescendantPrecedence() {
        if (_minimumDescendantPrecedence == -1) {
            // Start with precedence of current stylesheet as a basis.
            int min = getImportPrecedence();

            // Recursively examine all imported/included stylesheets.
            final int inclImpCount = (_includedStylesheets != null)
                                          ? _includedStylesheets.size()
                                          : 0;

            for (int i = 0; i < inclImpCount; i++) {
                int prec = (_includedStylesheets.get(i)).getMinimumDescendantPrecedence();

                if (prec < min) {
                    min = prec;
                }
            }

            _minimumDescendantPrecedence = min;
        }
        return _minimumDescendantPrecedence;
    }

    public boolean checkForLoop(String systemId) {
        // Return true if this stylesheet includes/imports itself
        if (_systemId != null && _systemId.equals(systemId)) {
            return true;
        }
        // Then check with any stylesheets that included/imported this one
        if (_parentStylesheet != null)
            return _parentStylesheet.checkForLoop(systemId);
        // Otherwise OK
        return false;
    }

    public void setParser(Parser parser) {
        super.setParser(parser);
        _name = makeStylesheetName("__stylesheet_");
    }

    public void setParentStylesheet(Stylesheet parent) {
        _parentStylesheet = parent;
    }

    public Stylesheet getParentStylesheet() {
        return _parentStylesheet;
    }

    public void setImportingStylesheet(Stylesheet parent) {
        _importedFrom = parent;
        parent.addIncludedStylesheet(this);
    }

    public void setIncludingStylesheet(Stylesheet parent) {
        _includedFrom = parent;
        parent.addIncludedStylesheet(this);
    }

    public void addIncludedStylesheet(Stylesheet child) {
        if (_includedStylesheets == null) {
            _includedStylesheets = new ArrayList<>();
        }
        _includedStylesheets.add(child);
    }

    public void setSystemId(String systemId) {
        if (systemId != null) {
            _systemId = SystemIDResolver.getAbsoluteURI(systemId);
        }
    }

    public String getSystemId() {
        return _systemId;
    }

    public void setSourceLoader(SourceLoader loader) {
        _loader = loader;
    }

    public SourceLoader getSourceLoader() {
        return _loader;
    }

    private QName makeStylesheetName(String prefix) {
        return getParser().getQName(prefix+getXSLTC().nextStylesheetSerial());
    }

    /**
     * Returns true if this stylesheet has global vars or params.
     */
    public boolean hasGlobals() {
        return _globals.size() > 0;
    }

    /**
     * Returns true if at least one template in the stylesheet has params
     * defined. Uses the variable <code>_hasLocalParams</code> to cache the
     * result.
     */
    public boolean hasLocalParams() {
        if (_hasLocalParams == null) {
           List<Template> templates = getAllValidTemplates();
            final int n = templates.size();
            for (int i = 0; i < n; i++) {
                final Template template = templates.get(i);
                if (template.hasParams()) {
                    _hasLocalParams = Boolean.TRUE;
                    return true;
                }
            }
            _hasLocalParams = Boolean.FALSE;
            return false;
        }
        else {
            return _hasLocalParams.booleanValue();
        }
    }

    /**
     * Adds a single prefix mapping to this syntax tree node.
     * @param prefix Namespace prefix.
     * @param uri Namespace URI.
     */
    protected void addPrefixMapping(String prefix, String uri) {
        if (prefix.equals(EMPTYSTRING) && uri.equals(XHTML_URI)) return;
        super.addPrefixMapping(prefix, uri);
    }

    /**
     * Store extension URIs
     */
    private void extensionURI(String prefixes, SymbolTable stable) {
        if (prefixes != null) {
            StringTokenizer tokens = new StringTokenizer(prefixes);
            while (tokens.hasMoreTokens()) {
                final String prefix = tokens.nextToken();
                final String uri = lookupNamespace(prefix);
                if (uri != null) {
                    _extensions.put(uri, prefix);
                }
            }
        }
    }

    public boolean isExtension(String uri) {
        return (_extensions.get(uri) != null);
    }

    public void declareExtensionPrefixes(Parser parser) {
        final SymbolTable stable = parser.getSymbolTable();
        final String extensionPrefixes = getAttribute("extension-element-prefixes");
        extensionURI(extensionPrefixes, stable);
    }

    /**
     * Parse the version and uri fields of the stylesheet and add an
     * entry to the symbol table mapping the name <tt>__stylesheet_</tt>
     * to an instance of this class.
     */
    public void parseContents(Parser parser) {
        final SymbolTable stable = parser.getSymbolTable();

        /*
        // Make sure the XSL version set in this stylesheet
        if ((_version == null) || (_version.equals(EMPTYSTRING))) {
            reportError(this, parser, ErrorMsg.REQUIRED_ATTR_ERR,"version");
        }
        // Verify that the version is 1.0 and nothing else
        else if (!_version.equals("1.0")) {
            reportError(this, parser, ErrorMsg.XSL_VERSION_ERR, _version);
        }
        */

        // Add the implicit mapping of 'xml' to the XML namespace URI
        addPrefixMapping("xml", "http://www.w3.org/XML/1998/namespace");

        // Report and error if more than one stylesheet defined
        final Stylesheet sheet = stable.addStylesheet(_name, this);
        if (sheet != null) {
            // Error: more that one stylesheet defined
            ErrorMsg err = new ErrorMsg(ErrorMsg.MULTIPLE_STYLESHEET_ERR,this);
            parser.reportError(Constants.ERROR, err);
        }

        // If this is a simplified stylesheet we must create a template that
        // grabs the root node of the input doc ( <xsl:template match="/"/> ).
        // This template needs the current element (the one passed to this
        // method) as its only child, so the Template class has a special
        // method that handles this (parseSimplified()).
        if (_simplified) {
            stable.excludeURI(XSLT_URI);
            Template template = new Template();
            template.parseSimplified(this, parser);
        }
        // Parse the children of this node
        else {
            parseOwnChildren(parser);
        }
    }

    /**
     * Parse all direct children of the <xsl:stylesheet/> element.
     */
    public final void parseOwnChildren(Parser parser) {
        final SymbolTable stable = parser.getSymbolTable();
        final String excludePrefixes = getAttribute("exclude-result-prefixes");
        final String extensionPrefixes = getAttribute("extension-element-prefixes");

        // Exclude XSLT uri
        stable.pushExcludedNamespacesContext();
        stable.excludeURI(Constants.XSLT_URI);
        stable.excludeNamespaces(excludePrefixes);
        stable.excludeNamespaces(extensionPrefixes);

        final List<SyntaxTreeNode> contents = getContents();
        final int count = contents.size();

        // We have to scan the stylesheet element's top-level elements for
        // variables and/or parameters before we parse the other elements
        for (int i = 0; i < count; i++) {
            SyntaxTreeNode child = contents.get(i);
            if ((child instanceof VariableBase) ||
                (child instanceof NamespaceAlias)) {
                parser.getSymbolTable().setCurrentNode(child);
                child.parseContents(parser);
            }
        }

        // Now go through all the other top-level elements...
        for (int i = 0; i < count; i++) {
            SyntaxTreeNode child = contents.get(i);
            if (!(child instanceof VariableBase) &&
                !(child instanceof NamespaceAlias)) {
                parser.getSymbolTable().setCurrentNode(child);
                child.parseContents(parser);
            }

            // All template code should be compiled as methods if the
            // <xsl:apply-imports/> element was ever used in this stylesheet
            if (!_templateInlining && (child instanceof Template)) {
                Template template = (Template)child;
                String name = "template$dot$" + template.getPosition();
                template.setName(parser.getQName(name));
            }
        }

        stable.popExcludedNamespacesContext();
    }

    public void processModes() {
        if (_defaultMode == null)
            _defaultMode = new Mode(null, this, Constants.EMPTYSTRING);
        _defaultMode.processPatterns(_keys);
        _modes.values().stream().forEach((mode) -> {
            mode.processPatterns(_keys);
        });
    }

    private void compileModes(ClassGenerator classGen) {
        _defaultMode.compileApplyTemplates(classGen);
        _modes.values().stream().forEach((mode) -> {
            mode.compileApplyTemplates(classGen);
        });
    }

    public Mode getMode(QName modeName) {
        if (modeName == null) {
            if (_defaultMode == null) {
                _defaultMode = new Mode(null, this, Constants.EMPTYSTRING);
            }
            return _defaultMode;
        }
        else {
            Mode mode = _modes.get(modeName.getStringRep());
            if (mode == null) {
                final String suffix = Integer.toString(_nextModeSerial++);
                _modes.put(modeName.getStringRep(), mode = new Mode(modeName, this, suffix));
            }
            return mode;
        }
    }

    /**
     * Type check all the children of this node.
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        final int count = _globals.size();
        for (int i = 0; i < count; i++) {
            final VariableBase var = _globals.get(i);
            var.typeCheck(stable);
        }
        return typeCheckContents(stable);
    }

    /**
     * Translate the stylesheet into JVM bytecodes.
     */
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        translate();
    }

    private void addDOMField(ClassGenerator classGen) {
        final FieldGen fgen = new FieldGen(ACC_PUBLIC,
                                           Util.getJCRefType(DOM_INTF_SIG),
                                           DOM_FIELD,
                                           classGen.getConstantPool());
        classGen.addField(fgen.getField());
    }

    /**
     * Add a static field
     */
    private void addStaticField(ClassGenerator classGen, String type,
                                String name)
    {
        final FieldGen fgen = new FieldGen(ACC_PROTECTED|ACC_STATIC,
                                           Util.getJCRefType(type),
                                           name,
                                           classGen.getConstantPool());
        classGen.addField(fgen.getField());

    }

    /**
     * Translate the stylesheet into JVM bytecodes.
     */
    public void translate() {
        _className = getXSLTC().getClassName();

        // Define a new class by extending TRANSLET_CLASS
        final ClassGenerator classGen =
            new ClassGenerator(_className,
                               TRANSLET_CLASS,
                               Constants.EMPTYSTRING,
                               ACC_PUBLIC | ACC_SUPER,
                               null, this);

        addDOMField(classGen);

        // Compile transform() to initialize parameters, globals & output
        // and run the transformation
        compileTransform(classGen);

        // Translate all non-template elements and filter out all templates
        final Iterator<SyntaxTreeNode> elements = elements();
        while (elements.hasNext()) {
            SyntaxTreeNode element = elements.next();
            // xsl:template
            if (element instanceof Template) {
                // Separate templates by modes
                final Template template = (Template)element;
                //_templates.add(template);
                getMode(template.getModeName()).addTemplate(template);
            }
            // xsl:attribute-set
            else if (element instanceof AttributeSet) {
                ((AttributeSet)element).translate(classGen, null);
            }
            else if (element instanceof Output) {
                // save the element for later to pass to compileConstructor
                Output output = (Output)element;
                if (output.enabled()) _lastOutputElement = output;
            }
            else {
                // Global variables and parameters are handled elsewhere.
                // Other top-level non-template elements are ignored. Literal
                // elements outside of templates will never be output.
            }
        }

        checkOutputMethod();
        processModes();
        compileModes(classGen);
        compileStaticInitializer(classGen);
        compileConstructor(classGen, _lastOutputElement);

        if (!getParser().errorsFound()) {
            getXSLTC().dumpClass(classGen.getJavaClass());
        }
    }

    /**
     * Compile the namesArray, urisArray and typesArray into
     * the static initializer. They are read-only from the
     * translet. All translet instances can share a single
     * copy of this informtion.
     */
    private void compileStaticInitializer(ClassGenerator classGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = new InstructionList();

        final MethodGenerator staticConst =
            new MethodGenerator(ACC_PUBLIC|ACC_STATIC,
                                com.sun.org.apache.bcel.internal.generic.Type.VOID,
                                null, null, "<clinit>",
                                _className, il, cpg);

        addStaticField(classGen, "[" + STRING_SIG, STATIC_NAMES_ARRAY_FIELD);
        addStaticField(classGen, "[" + STRING_SIG, STATIC_URIS_ARRAY_FIELD);
        addStaticField(classGen, "[I", STATIC_TYPES_ARRAY_FIELD);
        addStaticField(classGen, "[" + STRING_SIG, STATIC_NAMESPACE_ARRAY_FIELD);
        // Create fields of type char[] that will contain literal text from
        // the stylesheet.
        final int charDataFieldCount = getXSLTC().getCharacterDataCount();
        for (int i = 0; i < charDataFieldCount; i++) {
            addStaticField(classGen, STATIC_CHAR_DATA_FIELD_SIG,
                           STATIC_CHAR_DATA_FIELD+i);
        }

        // Put the names array into the translet - used for dom/translet mapping
        final List<String> namesIndex = getXSLTC().getNamesIndex();
        int size = namesIndex.size();
        String[] namesArray = new String[size];
        String[] urisArray = new String[size];
        int[] typesArray = new int[size];

        int index;
        for (int i = 0; i < size; i++) {
            String encodedName = namesIndex.get(i);
            if ((index = encodedName.lastIndexOf(':')) > -1) {
                urisArray[i] = encodedName.substring(0, index);
            }

            index = index + 1;
            if (encodedName.charAt(index) == '@') {
                typesArray[i] = DTM.ATTRIBUTE_NODE;
                index++;
            } else if (encodedName.charAt(index) == '?') {
                typesArray[i] = DTM.NAMESPACE_NODE;
                index++;
            } else {
                typesArray[i] = DTM.ELEMENT_NODE;
            }

            if (index == 0) {
                namesArray[i] = encodedName;
            }
            else {
                namesArray[i] = encodedName.substring(index);
            }
        }

        staticConst.markChunkStart();
        il.append(new PUSH(cpg, size));
        il.append(new ANEWARRAY(cpg.addClass(STRING)));
        int namesArrayRef = cpg.addFieldref(_className,
                                            STATIC_NAMES_ARRAY_FIELD,
                                            NAMES_INDEX_SIG);
        il.append(new PUTSTATIC(namesArrayRef));
        staticConst.markChunkEnd();

        for (int i = 0; i < size; i++) {
            final String name = namesArray[i];
            staticConst.markChunkStart();
            il.append(new GETSTATIC(namesArrayRef));
            il.append(new PUSH(cpg, i));
            il.append(new PUSH(cpg, name));
            il.append(AASTORE);
            staticConst.markChunkEnd();
        }

        staticConst.markChunkStart();
        il.append(new PUSH(cpg, size));
        il.append(new ANEWARRAY(cpg.addClass(STRING)));
        int urisArrayRef = cpg.addFieldref(_className,
                                           STATIC_URIS_ARRAY_FIELD,
                                           URIS_INDEX_SIG);
        il.append(new PUTSTATIC(urisArrayRef));
        staticConst.markChunkEnd();

        for (int i = 0; i < size; i++) {
            final String uri = urisArray[i];
            staticConst.markChunkStart();
            il.append(new GETSTATIC(urisArrayRef));
            il.append(new PUSH(cpg, i));
            il.append(new PUSH(cpg, uri));
            il.append(AASTORE);
            staticConst.markChunkEnd();
        }

        staticConst.markChunkStart();
        il.append(new PUSH(cpg, size));
        il.append(new NEWARRAY(BasicType.INT));
        int typesArrayRef = cpg.addFieldref(_className,
                                            STATIC_TYPES_ARRAY_FIELD,
                                            TYPES_INDEX_SIG);
        il.append(new PUTSTATIC(typesArrayRef));
        staticConst.markChunkEnd();

        for (int i = 0; i < size; i++) {
            final int nodeType = typesArray[i];
            staticConst.markChunkStart();
            il.append(new GETSTATIC(typesArrayRef));
            il.append(new PUSH(cpg, i));
            il.append(new PUSH(cpg, nodeType));
            il.append(IASTORE);
        }

        // Put the namespace names array into the translet
        final List<String> namespaces = getXSLTC().getNamespaceIndex();
        staticConst.markChunkStart();
        il.append(new PUSH(cpg, namespaces.size()));
        il.append(new ANEWARRAY(cpg.addClass(STRING)));
        int namespaceArrayRef = cpg.addFieldref(_className,
                                                STATIC_NAMESPACE_ARRAY_FIELD,
                                                NAMESPACE_INDEX_SIG);
        il.append(new PUTSTATIC(namespaceArrayRef));
        staticConst.markChunkEnd();

        for (int i = 0; i < namespaces.size(); i++) {
            final String ns = namespaces.get(i);
            staticConst.markChunkStart();
            il.append(new GETSTATIC(namespaceArrayRef));
            il.append(new PUSH(cpg, i));
            il.append(new PUSH(cpg, ns));
            il.append(AASTORE);
            staticConst.markChunkEnd();
        }

        // Grab all the literal text in the stylesheet and put it in a char[]
        final int charDataCount = getXSLTC().getCharacterDataCount();
        final int toCharArray = cpg.addMethodref(STRING, "toCharArray", "()[C");
        for (int i = 0; i < charDataCount; i++) {
            staticConst.markChunkStart();
            il.append(new PUSH(cpg, getXSLTC().getCharacterData(i)));
            il.append(new INVOKEVIRTUAL(toCharArray));
            il.append(new PUTSTATIC(cpg.addFieldref(_className,
                                               STATIC_CHAR_DATA_FIELD+i,
                                               STATIC_CHAR_DATA_FIELD_SIG)));
            staticConst.markChunkEnd();
        }

        il.append(RETURN);

        classGen.addMethod(staticConst);

    }

    /**
     * Compile the translet's constructor
     */
    private void compileConstructor(ClassGenerator classGen, Output output) {

        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = new InstructionList();

        final MethodGenerator constructor =
            new MethodGenerator(ACC_PUBLIC,
                                com.sun.org.apache.bcel.internal.generic.Type.VOID,
                                null, null, "<init>",
                                _className, il, cpg);

        // Call the constructor in the AbstractTranslet superclass
        il.append(classGen.loadTranslet());
        il.append(new INVOKESPECIAL(cpg.addMethodref(TRANSLET_CLASS,
                                                     "<init>", "()V")));

        constructor.markChunkStart();
        il.append(classGen.loadTranslet());
        il.append(new GETSTATIC(cpg.addFieldref(_className,
                                                STATIC_NAMES_ARRAY_FIELD,
                                                NAMES_INDEX_SIG)));
        il.append(new PUTFIELD(cpg.addFieldref(TRANSLET_CLASS,
                                               NAMES_INDEX,
                                               NAMES_INDEX_SIG)));

        il.append(classGen.loadTranslet());
        il.append(new GETSTATIC(cpg.addFieldref(_className,
                                                STATIC_URIS_ARRAY_FIELD,
                                                URIS_INDEX_SIG)));
        il.append(new PUTFIELD(cpg.addFieldref(TRANSLET_CLASS,
                                               URIS_INDEX,
                                               URIS_INDEX_SIG)));
        constructor.markChunkEnd();

        constructor.markChunkStart();
        il.append(classGen.loadTranslet());
        il.append(new GETSTATIC(cpg.addFieldref(_className,
                                                STATIC_TYPES_ARRAY_FIELD,
                                                TYPES_INDEX_SIG)));
        il.append(new PUTFIELD(cpg.addFieldref(TRANSLET_CLASS,
                                               TYPES_INDEX,
                                               TYPES_INDEX_SIG)));
        constructor.markChunkEnd();

        constructor.markChunkStart();
        il.append(classGen.loadTranslet());
        il.append(new GETSTATIC(cpg.addFieldref(_className,
                                                STATIC_NAMESPACE_ARRAY_FIELD,
                                                NAMESPACE_INDEX_SIG)));
        il.append(new PUTFIELD(cpg.addFieldref(TRANSLET_CLASS,
                                               NAMESPACE_INDEX,
                                               NAMESPACE_INDEX_SIG)));
        constructor.markChunkEnd();

        constructor.markChunkStart();
        il.append(classGen.loadTranslet());
        il.append(new PUSH(cpg, AbstractTranslet.CURRENT_TRANSLET_VERSION));
        il.append(new PUTFIELD(cpg.addFieldref(TRANSLET_CLASS,
                                               TRANSLET_VERSION_INDEX,
                                               TRANSLET_VERSION_INDEX_SIG)));
        constructor.markChunkEnd();

        if (_hasIdCall) {
            constructor.markChunkStart();
            il.append(classGen.loadTranslet());
            il.append(new PUSH(cpg, Boolean.TRUE));
            il.append(new PUTFIELD(cpg.addFieldref(TRANSLET_CLASS,
                                                   HASIDCALL_INDEX,
                                                   HASIDCALL_INDEX_SIG)));
            constructor.markChunkEnd();
        }

        // Compile in code to set the output configuration from <xsl:output>
        if (output != null) {
            // Set all the output settings files in the translet
            constructor.markChunkStart();
            output.translate(classGen, constructor);
            constructor.markChunkEnd();
        }

        // Compile default decimal formatting symbols.
        // This is an implicit, nameless xsl:decimal-format top-level element.
        if (_numberFormattingUsed) {
            constructor.markChunkStart();
            DecimalFormatting.translateDefaultDFS(classGen, constructor);
            constructor.markChunkEnd();
        }

        il.append(RETURN);

        classGen.addMethod(constructor);
    }

    /**
     * Compile a topLevel() method into the output class. This method is
     * called from transform() to handle all non-template top-level elements.
     * Returns the signature of the topLevel() method.
     *
     * Global variables/params and keys are first sorted to resolve
     * dependencies between them. The XSLT 1.0 spec does not allow a key
     * to depend on a variable. However, for compatibility with Xalan
     * interpretive, that type of dependency is allowed. Note also that
     * the buildKeys() method is still generated as it is used by the
     * LoadDocument class, but it no longer called from transform().
     */
    private String compileTopLevel(ClassGenerator classGen) {

        final ConstantPoolGen cpg = classGen.getConstantPool();

        final com.sun.org.apache.bcel.internal.generic.Type[] argTypes = {
            Util.getJCRefType(DOM_INTF_SIG),
            Util.getJCRefType(NODE_ITERATOR_SIG),
            Util.getJCRefType(TRANSLET_OUTPUT_SIG)
        };

        final String[] argNames = {
            DOCUMENT_PNAME, ITERATOR_PNAME, TRANSLET_OUTPUT_PNAME
        };

        final InstructionList il = new InstructionList();

        final MethodGenerator toplevel =
            new MethodGenerator(ACC_PUBLIC,
                                com.sun.org.apache.bcel.internal.generic.Type.VOID,
                                argTypes, argNames,
                                "topLevel", _className, il,
                                classGen.getConstantPool());

        toplevel.addException("com.sun.org.apache.xalan.internal.xsltc.TransletException");

        // Define and initialize 'current' variable with the root node
        final LocalVariableGen current =
            toplevel.addLocalVariable("current",
                                      com.sun.org.apache.bcel.internal.generic.Type.INT,
                                      null, null);

        final int setFilter = cpg.addInterfaceMethodref(DOM_INTF,
                               "setFilter",
                               "(Lcom/sun/org/apache/xalan/internal/xsltc/StripFilter;)V");

        final int gitr = cpg.addInterfaceMethodref(DOM_INTF,
                                                        "getIterator",
                                                        "()"+NODE_ITERATOR_SIG);
        il.append(toplevel.loadDOM());
        il.append(new INVOKEINTERFACE(gitr, 1));
        il.append(toplevel.nextNode());
        current.setStart(il.append(new ISTORE(current.getIndex())));

        // Create a new list containing variables/params + keys
        List<SyntaxTreeNode> varDepElements = new ArrayList<>(_globals);
        Iterator<SyntaxTreeNode> elements = elements();
        while (elements.hasNext()) {
            SyntaxTreeNode element = elements.next();
            if (element instanceof Key) {
                varDepElements.add(element);
            }
        }

        // Determine a partial order for the variables/params and keys
        varDepElements = resolveDependencies(varDepElements);

        // Translate vars/params and keys in the right order
        final int count = varDepElements.size();
        for (int i = 0; i < count; i++) {
            final TopLevelElement tle = (TopLevelElement) varDepElements.get(i);
            tle.translate(classGen, toplevel);
            if (tle instanceof Key) {
                final Key key = (Key) tle;
                _keys.put(key.getName(), key);
            }
        }

        // Compile code for other top-level elements
       List<Whitespace.WhitespaceRule> whitespaceRules = new ArrayList<>();
        elements = elements();
        while (elements.hasNext()) {
            SyntaxTreeNode element = elements.next();
            // xsl:decimal-format
            if (element instanceof DecimalFormatting) {
                ((DecimalFormatting)element).translate(classGen,toplevel);
            }
            // xsl:strip/preserve-space
            else if (element instanceof Whitespace) {
                whitespaceRules.addAll(((Whitespace)element).getRules());
            }
        }

        // Translate all whitespace strip/preserve rules
        if (whitespaceRules.size() > 0) {
            Whitespace.translateRules(whitespaceRules,classGen);
        }

        if (classGen.containsMethod(STRIP_SPACE, STRIP_SPACE_PARAMS) != null) {
            il.append(toplevel.loadDOM());
            il.append(classGen.loadTranslet());
            il.append(new INVOKEINTERFACE(setFilter, 2));
        }

        il.append(RETURN);

        // Compute max locals + stack and add method to class
        classGen.addMethod(toplevel);

        return("("+DOM_INTF_SIG+NODE_ITERATOR_SIG+TRANSLET_OUTPUT_SIG+")V");
    }

    /**
     * This method returns a vector with variables/params and keys in the
     * order in which they are to be compiled for initialization. The order
     * is determined by analyzing the dependencies between them. The XSLT 1.0
     * spec does not allow a key to depend on a variable. However, for
     * compatibility with Xalan interpretive, that type of dependency is
     * allowed and, therefore, consider to determine the partial order.
     */
    private List<SyntaxTreeNode> resolveDependencies(List<SyntaxTreeNode> input) {
        List<SyntaxTreeNode> result = new ArrayList<>();
        while (input.size() > 0) {
            boolean changed = false;
            for (int i = 0; i < input.size(); ) {
                final TopLevelElement vde = (TopLevelElement) input.get(i);
                final List<SyntaxTreeNode> dep = vde.getDependencies();
                if (dep == null || result.containsAll(dep)) {
                    result.add(vde);
                    input.remove(i);
                    changed = true;
                }
                else {
                    i++;
                }
            }

            // If nothing was changed in this pass then we have a circular ref
            if (!changed) {
                ErrorMsg err = new ErrorMsg(ErrorMsg.CIRCULAR_VARIABLE_ERR,
                                            input.toString(), this);
                getParser().reportError(Constants.ERROR, err);
                return(result);
            }
        }

        return result;
    }

    /**
     * Compile a buildKeys() method into the output class. Note that keys
     * for the input document are created in topLevel(), not in this method.
     * However, we still need this method to create keys for documents loaded
     * via the XPath document() function.
     */
    private String compileBuildKeys(ClassGenerator classGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();

        final com.sun.org.apache.bcel.internal.generic.Type[] argTypes = {
            Util.getJCRefType(DOM_INTF_SIG),
            Util.getJCRefType(NODE_ITERATOR_SIG),
            Util.getJCRefType(TRANSLET_OUTPUT_SIG),
            com.sun.org.apache.bcel.internal.generic.Type.INT
        };

        final String[] argNames = {
            DOCUMENT_PNAME, ITERATOR_PNAME, TRANSLET_OUTPUT_PNAME, "current"
        };

        final InstructionList il = new InstructionList();

        final MethodGenerator buildKeys =
            new MethodGenerator(ACC_PUBLIC,
                                com.sun.org.apache.bcel.internal.generic.Type.VOID,
                                argTypes, argNames,
                                "buildKeys", _className, il,
                                classGen.getConstantPool());

        buildKeys.addException("com.sun.org.apache.xalan.internal.xsltc.TransletException");

        final Iterator<SyntaxTreeNode> elements = elements();
        while (elements.hasNext()) {
            // xsl:key
            final SyntaxTreeNode element = elements.next();
            if (element instanceof Key) {
                final Key key = (Key)element;
                key.translate(classGen, buildKeys);
                _keys.put(key.getName(),key);
            }
        }

        il.append(RETURN);

        // Compute max locals + stack and add method to class
        buildKeys.stripAttributes(true);
        buildKeys.setMaxLocals();
        buildKeys.setMaxStack();
        buildKeys.removeNOPs();

        classGen.addMethod(buildKeys.getMethod());

        return("("+DOM_INTF_SIG+NODE_ITERATOR_SIG+TRANSLET_OUTPUT_SIG+"I)V");
    }

    /**
     * Compile transform() into the output class. This method is used to
     * initialize global variables and global parameters. The current node
     * is set to be the document's root node.
     */
    private void compileTransform(ClassGenerator classGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();

        /*
         * Define the the method transform with the following signature:
         * void transform(DOM, NodeIterator, HandlerBase)
         */
        final com.sun.org.apache.bcel.internal.generic.Type[] argTypes =
            new com.sun.org.apache.bcel.internal.generic.Type[3];
        argTypes[0] = Util.getJCRefType(DOM_INTF_SIG);
        argTypes[1] = Util.getJCRefType(NODE_ITERATOR_SIG);
        argTypes[2] = Util.getJCRefType(TRANSLET_OUTPUT_SIG);

        final String[] argNames = new String[3];
        argNames[0] = DOCUMENT_PNAME;
        argNames[1] = ITERATOR_PNAME;
        argNames[2] = TRANSLET_OUTPUT_PNAME;

        final InstructionList il = new InstructionList();
        final MethodGenerator transf =
            new MethodGenerator(ACC_PUBLIC,
                                com.sun.org.apache.bcel.internal.generic.Type.VOID,
                                argTypes, argNames,
                                "transform",
                                _className,
                                il,
                                classGen.getConstantPool());
        transf.addException("com.sun.org.apache.xalan.internal.xsltc.TransletException");

        // call resetPrefixIndex at the beginning of transform
        final int check = cpg.addMethodref(BASIS_LIBRARY_CLASS, "resetPrefixIndex", "()V");
        il.append(new INVOKESTATIC(check));

        // Define and initialize current with the root node
        final LocalVariableGen current =
            transf.addLocalVariable("current",
                                    com.sun.org.apache.bcel.internal.generic.Type.INT,
                                    null, null);
        final String applyTemplatesSig = classGen.getApplyTemplatesSig();
        final int applyTemplates = cpg.addMethodref(getClassName(),
                                                    "applyTemplates",
                                                    applyTemplatesSig);
        final int domField = cpg.addFieldref(getClassName(),
                                             DOM_FIELD,
                                             DOM_INTF_SIG);

        // push translet for PUTFIELD
        il.append(classGen.loadTranslet());
        // prepare appropriate DOM implementation

        if (isMultiDocument()) {
            il.append(new NEW(cpg.addClass(MULTI_DOM_CLASS)));
            il.append(DUP);
        }

        il.append(classGen.loadTranslet());
        il.append(transf.loadDOM());
        il.append(new INVOKEVIRTUAL(cpg.addMethodref(TRANSLET_CLASS,
                                                     "makeDOMAdapter",
                                                     "("+DOM_INTF_SIG+")"+
                                                     DOM_ADAPTER_SIG)));
        // DOMAdapter is on the stack

        if (isMultiDocument()) {
            final int init = cpg.addMethodref(MULTI_DOM_CLASS,
                                              "<init>",
                                              "("+DOM_INTF_SIG+")V");
            il.append(new INVOKESPECIAL(init));
            // MultiDOM is on the stack
        }

        //store to _dom variable
        il.append(new PUTFIELD(domField));

        // continue with globals initialization
        final int gitr = cpg.addInterfaceMethodref(DOM_INTF,
                                                        "getIterator",
                                                        "()"+NODE_ITERATOR_SIG);
        il.append(transf.loadDOM());
        il.append(new INVOKEINTERFACE(gitr, 1));
        il.append(transf.nextNode());
        current.setStart(il.append(new ISTORE(current.getIndex())));

        // Transfer the output settings to the output post-processor
        il.append(classGen.loadTranslet());
        il.append(transf.loadHandler());
        final int index = cpg.addMethodref(TRANSLET_CLASS,
                                           "transferOutputSettings",
                                           "("+OUTPUT_HANDLER_SIG+")V");
        il.append(new INVOKEVIRTUAL(index));

        /*
         * Compile buildKeys() method. Note that this method is not
         * invoked here as keys for the input document are now created
         * in topLevel(). However, this method is still needed by the
         * LoadDocument class.
         */
        final String keySig = compileBuildKeys(classGen);
        final int keyIdx = cpg.addMethodref(getClassName(),
                                               "buildKeys", keySig);

        // Look for top-level elements that need handling
        final Iterator<SyntaxTreeNode> toplevel = elements();
        if (_globals.size() > 0 || toplevel.hasNext()) {
            // Compile method for handling top-level elements
            final String topLevelSig = compileTopLevel(classGen);
            // Get a reference to that method
            final int topLevelIdx = cpg.addMethodref(getClassName(),
                                                     "topLevel",
                                                     topLevelSig);
            // Push all parameters on the stack and call topLevel()
            il.append(classGen.loadTranslet()); // The 'this' pointer
            il.append(classGen.loadTranslet());
            il.append(new GETFIELD(domField));  // The DOM reference
            il.append(transf.loadIterator());
            il.append(transf.loadHandler());    // The output handler
            il.append(new INVOKEVIRTUAL(topLevelIdx));
        }

        // start document
        il.append(transf.loadHandler());
        il.append(transf.startDocument());

        // push first arg for applyTemplates
        il.append(classGen.loadTranslet());
        // push translet for GETFIELD to get DOM arg
        il.append(classGen.loadTranslet());
        il.append(new GETFIELD(domField));
        // push remaining 2 args
        il.append(transf.loadIterator());
        il.append(transf.loadHandler());
        il.append(new INVOKEVIRTUAL(applyTemplates));
        // endDocument
        il.append(transf.loadHandler());
        il.append(transf.endDocument());

        il.append(RETURN);

        // Compute max locals + stack and add method to class
        classGen.addMethod(transf);

    }

    /**
     * Peephole optimization: Remove sequences of [ALOAD, POP].
     */
    private void peepHoleOptimization(MethodGenerator methodGen) {
        final String pattern = "`aload'`pop'`instruction'";
        final InstructionList il = methodGen.getInstructionList();
        final InstructionFinder find = new InstructionFinder(il);
        for(Iterator<InstructionHandle[]> iter=find.search(pattern); iter.hasNext(); ) {
            InstructionHandle[] match = iter.next();
            try {
                il.delete(match[0], match[1]);
            }
            catch (TargetLostException e) {
                // TODO: move target down into the list
            }
        }
    }

    public int addParam(Param param) {
        _globals.add(param);
        return _globals.size() - 1;
    }

    public int addVariable(Variable global) {
        _globals.add(global);
        return _globals.size() - 1;
    }

    public void display(int indent) {
        indent(indent);
        Util.println("Stylesheet");
        displayContents(indent + IndentIncrement);
    }

    // do we need this wrapper ?????
    public String getNamespace(String prefix) {
        return lookupNamespace(prefix);
    }

    public String getClassName() {
        return _className;
    }

    public List<Template> getTemplates() {
        return _templates;
    }

    public List<Template> getAllValidTemplates() {
        // Return templates if no imported/included stylesheets
        if (_includedStylesheets == null) {
            return _templates;
        }

        // Is returned value cached?
        if (_allValidTemplates == null) {
            List<Template> templates = new ArrayList<>();
            templates.addAll(_templates);
            for (Stylesheet included : _includedStylesheets) {
                templates.addAll(included.getAllValidTemplates());
            }
            //templates.addAll(_templates);

            // Cache results in top-level stylesheet only
            if (_parentStylesheet != null) {
                return templates;
            }
            _allValidTemplates = templates;
         }

        return _allValidTemplates;
    }

    protected void addTemplate(Template template) {
        _templates.add(template);
    }
}
