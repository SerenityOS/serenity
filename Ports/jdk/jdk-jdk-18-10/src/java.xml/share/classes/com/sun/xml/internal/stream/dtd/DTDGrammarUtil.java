/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.xml.internal.stream.dtd;
import com.sun.xml.internal.stream.dtd.nonvalidating.DTDGrammar;
import com.sun.xml.internal.stream.dtd.nonvalidating.XMLAttributeDecl;
import com.sun.xml.internal.stream.dtd.nonvalidating.XMLElementDecl;
import com.sun.xml.internal.stream.dtd.nonvalidating.XMLSimpleType;
import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import javax.xml.XMLConstants;

 /*
  * @author Eric Ye, IBM
  * @author Andy Clark, IBM
  * @author Jeffrey Rodriguez IBM
  * @author Neil Graham, IBM
  * @author Sunitha Reddy, Sun Microsystems
  */

public class DTDGrammarUtil {


    /** Property identifier: symbol table. */
    protected static final String SYMBOL_TABLE =
    Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

    protected static final String NAMESPACES =
    Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE;


    /** Compile to true to debug attributes. */
    private static final boolean DEBUG_ATTRIBUTES = false;

    /** Compile to true to debug element children. */
    private static final boolean DEBUG_ELEMENT_CHILDREN = false;

    protected DTDGrammar fDTDGrammar = null;
    /** Namespaces. */
    protected boolean fNamespaces;

    /** Symbol table. */
    protected SymbolTable fSymbolTable = null;

    /** Current element index. */
    private int fCurrentElementIndex = -1;

    /** Current content spec type. */
    private int fCurrentContentSpecType = -1;

    /** Content spec type stack. */
    private boolean[] fElementContentState = new boolean[8];

    /** Element depth. */
    private int fElementDepth = -1;

    /** True if inside of element content. */
    private boolean fInElementContent = false;

    /** Temporary atribute declaration. */
    private XMLAttributeDecl fTempAttDecl = new XMLAttributeDecl();

    /** Temporary qualified name. */
    private QName fTempQName = new QName();

    /** Temporary string buffers. */
    private StringBuilder fBuffer = new StringBuilder();

    private NamespaceContext fNamespaceContext = null;

    /** Default constructor. */
    public DTDGrammarUtil(SymbolTable symbolTable) {
        fSymbolTable = symbolTable;
    }

    public DTDGrammarUtil(DTDGrammar grammar, SymbolTable symbolTable) {
        fDTDGrammar = grammar;
        fSymbolTable = symbolTable;
    }

    public DTDGrammarUtil(DTDGrammar grammar, SymbolTable symbolTable,
            NamespaceContext namespaceContext) {
        fDTDGrammar = grammar;
        fSymbolTable = symbolTable;
        fNamespaceContext = namespaceContext;
    }

    /*
     * Resets the component. The component can query the component manager
     * about any features and properties that affect the operation of the
     * component.
     *
     * @param componentManager The component manager.
     *
     * @throws SAXException Thrown by component on finitialization error.
     *                      For example, if a feature or property is
     *                      required for the operation of the component, the
     *                      component manager may throw a
     *                      SAXNotRecognizedException or a
     *                      SAXNotSupportedException.
     */
    public void reset(XMLComponentManager componentManager)
    throws XMLConfigurationException {

        fDTDGrammar = null;
        fInElementContent = false;
        fCurrentElementIndex = -1;
        fCurrentContentSpecType = -1;
        fNamespaces = componentManager.getFeature(NAMESPACES, true);
        fSymbolTable = (SymbolTable) componentManager.getProperty(
                Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY);
        fElementDepth = -1;
    }


    /**
     * The start of an element.
     *
     * @param element    The name of the element.
     * @param attributes The element attributes.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startElement(QName element, XMLAttributes attributes)  throws XNIException {
        handleStartElement(element, attributes);
    }

    /**
     * The end of an element.
     *
     * @param element The name of the element.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endElement(QName element) throws XNIException {
        handleEndElement(element);
    }

    /**
     * The start of a CDATA section.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startCDATA(Augmentations augs) throws XNIException {
    }

    /**
     * The end of a CDATA section.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endCDATA(Augmentations augs) throws XNIException {
    }



    /** Add default attributes and validate. */
    public void addDTDDefaultAttrs(QName elementName, XMLAttributes attributes)
    throws XNIException {

        int elementIndex;
        elementIndex = fDTDGrammar.getElementDeclIndex(elementName);
        // is there anything to do?
        if (elementIndex == -1 || fDTDGrammar == null) {
            return;
        }

        //
        // Check after all specified attrs are scanned
        // (1) report error for REQUIRED attrs that are missing (V_TAGc)
        // (2) add default attrs (FIXED and NOT_FIXED)
        //
        int attlistIndex = fDTDGrammar.getFirstAttributeDeclIndex(elementIndex);

        while (attlistIndex != -1) {

            fDTDGrammar.getAttributeDecl(attlistIndex, fTempAttDecl);

            if (DEBUG_ATTRIBUTES) {
                if (fTempAttDecl != null) {
                    XMLElementDecl elementDecl = new XMLElementDecl();
                    fDTDGrammar.getElementDecl(elementIndex, elementDecl);
                    System.out.println("element: " + (elementDecl.name.localpart));
                    System.out.println("attlistIndex " + attlistIndex + "\n" +
                    "attName : '" + (fTempAttDecl.name.localpart) + "'\n"
                    + "attType : " + fTempAttDecl.simpleType.type + "\n"
                    + "attDefaultType : " + fTempAttDecl.simpleType.defaultType + "\n"
                    + "attDefaultValue : '" + fTempAttDecl.simpleType.defaultValue + "'\n"
                    + attributes.getLength() + "\n"
                    );
                }
            }
            String attPrefix = fTempAttDecl.name.prefix;
            String attLocalpart = fTempAttDecl.name.localpart;
            String attRawName = fTempAttDecl.name.rawname;
            String attType = getAttributeTypeName(fTempAttDecl);
            int attDefaultType = fTempAttDecl.simpleType.defaultType;
            String attValue = null;

            if (fTempAttDecl.simpleType.defaultValue != null) {
                attValue = fTempAttDecl.simpleType.defaultValue;
            }
            boolean specified = false;
            boolean required = attDefaultType == XMLSimpleType.DEFAULT_TYPE_REQUIRED;
            boolean cdata = attType == XMLSymbols.fCDATASymbol;

            if (!cdata || required || attValue != null) {

                //check whether attribute is a namespace declaration
                if (fNamespaceContext != null && attRawName.startsWith(XMLConstants.XMLNS_ATTRIBUTE)) {
                    String prefix = "";
                    int pos = attRawName.indexOf(':');
                    if (pos != -1) {
                        prefix = attRawName.substring(0, pos);
                    } else {
                        prefix = attRawName;
                    }
                    prefix = fSymbolTable.addSymbol(prefix);
                    if (!((com.sun.org.apache.xerces.internal.util.
                            NamespaceSupport) fNamespaceContext).
                            containsPrefixInCurrentContext(prefix)) {
                        fNamespaceContext.declarePrefix(prefix, attValue);
                    }
                    specified = true;
                } else {

                    int attrCount = attributes.getLength();
                    for (int i = 0; i < attrCount; i++) {
                        if (attributes.getQName(i) == attRawName) {
                            specified = true;
                            break;
                        }
                    }

                }

            }

            if (!specified) {
                if (attValue != null) {
                    if (fNamespaces) {
                        int index = attRawName.indexOf(':');
                        if (index != -1) {
                            attPrefix = attRawName.substring(0, index);
                            attPrefix = fSymbolTable.addSymbol(attPrefix);
                            attLocalpart = attRawName.substring(index + 1);
                            attLocalpart = fSymbolTable.addSymbol(attLocalpart);
                        }
                    }
                    fTempQName.setValues(attPrefix, attLocalpart, attRawName,
                            fTempAttDecl.name.uri);
                    int newAttr = attributes.addAttribute(fTempQName, attType,
                            attValue);
                }
            }
            attlistIndex = fDTDGrammar.getNextAttributeDeclIndex(attlistIndex);
        }

        // now iterate through the expanded attributes for
        // 1. if every attribute seen is declared in the DTD
        // 2. check if the VC: default_fixed holds
        // 3. validate every attribute.
        int attrCount = attributes.getLength();
        for (int i = 0; i < attrCount; i++) {
            String attrRawName = attributes.getQName(i);
            boolean declared = false;
            int position =
            fDTDGrammar.getFirstAttributeDeclIndex(elementIndex);
            while (position != -1) {
                fDTDGrammar.getAttributeDecl(position, fTempAttDecl);
                if (fTempAttDecl.name.rawname == attrRawName) {
                    // found the match att decl,
                    declared = true;
                    break;
                }
                position = fDTDGrammar.getNextAttributeDeclIndex(position);
            }
            if (!declared) {
                continue;
            }

            String type = getAttributeTypeName(fTempAttDecl);
            attributes.setType(i, type);

            boolean changedByNormalization = false;
            if (attributes.isSpecified(i) && type != XMLSymbols.fCDATASymbol) {
                changedByNormalization = normalizeAttrValue(attributes, i);
            }
        } // for all attributes

    } // addDTDDefaultAttrsAndValidate(int,XMLAttrList)


    /**
     * Normalize the attribute value of a non CDATA attributes collapsing
     * sequences of space characters (x20)
     *
     * @param attributes The list of attributes
     * @param index The index of the attribute to normalize
     */
    private boolean normalizeAttrValue(XMLAttributes attributes, int index) {
        // vars
        boolean leadingSpace = true;
        boolean spaceStart = false;
        boolean readingNonSpace = false;
        int count = 0;
        int eaten = 0;
        String attrValue = attributes.getValue(index);
        char[] attValue = new char[attrValue.length()];

        fBuffer.setLength(0);
        attrValue.getChars(0, attrValue.length(), attValue, 0);
        for (int i = 0; i < attValue.length; i++) {

            if (attValue[i] == ' ') {

                // now the tricky part
                if (readingNonSpace) {
                    spaceStart = true;
                    readingNonSpace = false;
                }

                if (spaceStart && !leadingSpace) {
                    spaceStart = false;
                    fBuffer.append(attValue[i]);
                    count++;
                } else {
                    if (leadingSpace || !spaceStart) {
                        eaten++;
                    }
                }

            } else {
                readingNonSpace = true;
                spaceStart = false;
                leadingSpace = false;
                fBuffer.append(attValue[i]);
                count++;
            }
        }

        // check if the last appended character is a space.
        if (count > 0 && fBuffer.charAt(count - 1) == ' ') {
            fBuffer.setLength(count - 1);

        }
        String newValue = fBuffer.toString();
        attributes.setValue(index, newValue);
        return !attrValue.equals(newValue);
    }



    /** convert attribute type from ints to strings */
    private String getAttributeTypeName(XMLAttributeDecl attrDecl) {

        switch (attrDecl.simpleType.type) {
            case XMLSimpleType.TYPE_ENTITY: {
                return attrDecl.simpleType.list ? XMLSymbols.fENTITIESSymbol :
                    XMLSymbols.fENTITYSymbol;
            }
            case XMLSimpleType.TYPE_ENUMERATION: {
                StringBuilder buffer = new StringBuilder();
                buffer.append('(');
                for (int i = 0; i < attrDecl.simpleType.enumeration.length; i++) {
                    if (i > 0) {
                        buffer.append("|");
                    }
                    buffer.append(attrDecl.simpleType.enumeration[i]);
                }
                buffer.append(')');
                return fSymbolTable.addSymbol(buffer.toString());
            }
            case XMLSimpleType.TYPE_ID: {
                return XMLSymbols.fIDSymbol;
            }
            case XMLSimpleType.TYPE_IDREF: {
                return attrDecl.simpleType.list ? XMLSymbols.fIDREFSSymbol :
                    XMLSymbols.fIDREFSymbol;
            }
            case XMLSimpleType.TYPE_NMTOKEN: {
                return attrDecl.simpleType.list ? XMLSymbols.fNMTOKENSSymbol :
                    XMLSymbols.fNMTOKENSymbol;
            }
            case XMLSimpleType.TYPE_NOTATION: {
                return XMLSymbols.fNOTATIONSymbol;
            }
        }
        return XMLSymbols.fCDATASymbol;

    }


    /** ensure element stack capacity */
    private void ensureStackCapacity(int newElementDepth) {
        if (newElementDepth == fElementContentState.length) {
            boolean[] newStack = new boolean[newElementDepth * 2];
            System.arraycopy(this.fElementContentState, 0, newStack, 0,
                    newElementDepth);
            fElementContentState = newStack;
        }
    }



    /** Handle element
     * @return true if validator is removed from the pipeline
     */
    protected void handleStartElement(QName element, XMLAttributes attributes) throws XNIException {

        if (fDTDGrammar == null) {
            fCurrentElementIndex = -1;
            fCurrentContentSpecType = -1;
            fInElementContent = false;
            return;
        } else {
            fCurrentElementIndex = fDTDGrammar.getElementDeclIndex(element);
            fCurrentContentSpecType = fDTDGrammar.getContentSpecType(
                    fCurrentElementIndex);
            //handleDTDDefaultAttrs(element,attributes);
            addDTDDefaultAttrs(element, attributes);
        }

        fInElementContent = fCurrentContentSpecType == XMLElementDecl.TYPE_CHILDREN;
        fElementDepth++;
        ensureStackCapacity(fElementDepth);
        fElementContentState[fElementDepth] = fInElementContent;
    }


    /** Handle end element. */
    protected void handleEndElement(QName element) throws XNIException {
        if (fDTDGrammar == null) return;
        fElementDepth--;
        if (fElementDepth < -1) {
            throw new RuntimeException("FWK008 Element stack underflow");
        }
        if (fElementDepth < 0) {
            fCurrentElementIndex = -1;
            fCurrentContentSpecType = -1;
            fInElementContent = false;
            return;
        }
        fInElementContent =  fElementContentState[fElementDepth];
    }

    public boolean isInElementContent() {
        return fInElementContent;
    }

    public boolean isIgnorableWhiteSpace(XMLString text) {
        if (isInElementContent()) {
            for (int i = text.offset; i < text.offset + text.length; i++) {
                if (!XMLChar.isSpace(text.ch[i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
