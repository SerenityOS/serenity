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

package com.sun.xml.internal.stream.dtd.nonvalidating;

import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDTDContentModelSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDTDSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A DTD grammar. This class implements the XNI handler interfaces
 * for DTD information so that it can build the approprate validation
 * structures automatically from the callbacks.
 *
 * @author Eric Ye, IBM
 * @author Jeffrey Rodriguez, IBM
 * @author Andy Clark, IBM
 * @author Neil Graham, IBM
 *
 */
public class DTDGrammar {


    /** Top level scope (-1). */
    public static final int TOP_LEVEL_SCOPE = -1;

    // private

    /** Chunk shift (8). */
    private static final int CHUNK_SHIFT = 8; // 2^8 = 256

    /** Chunk size (1 << CHUNK_SHIFT). */
    private static final int CHUNK_SIZE = (1 << CHUNK_SHIFT);

    /** Chunk mask (CHUNK_SIZE - 1). */
    private static final int CHUNK_MASK = CHUNK_SIZE - 1;

    /** Initial chunk count (1 << (10 - CHUNK_SHIFT)). */
    private static final int INITIAL_CHUNK_COUNT = (1 << (10 - CHUNK_SHIFT)); // 2^10 = 1k

    /** List flag (0x80). */
    private static final short LIST_FLAG = 0x80;

    /** List mask (~LIST_FLAG). */
    private static final short LIST_MASK = ~LIST_FLAG;

    // debugging

    /** Debug DTDGrammar. */
    private static final boolean DEBUG = false;

    //
    // Data
    //

    protected XMLDTDSource fDTDSource = null;
    protected XMLDTDContentModelSource fDTDContentModelSource = null;

    /** Current element index. */
    protected int fCurrentElementIndex;

    /** Current attribute index. */
    protected int fCurrentAttributeIndex;

    /** fReadingExternalDTD */
    protected boolean fReadingExternalDTD = false;

    /** Symbol table. */
    private final SymbolTable fSymbolTable;
    private final ArrayList<XMLNotationDecl> notationDecls = new ArrayList<>();

    // element declarations

    /** Number of element declarations. */
    private int fElementDeclCount = 0;

    /** Element declaration name. */
    private QName fElementDeclName[][] = new QName[INITIAL_CHUNK_COUNT][];

    /**
     * Element declaration type.
     * @see XMLElementDecl
     */
    private short fElementDeclType[][] = new short[INITIAL_CHUNK_COUNT][];


    /** First attribute declaration of an element declaration. */
    private int fElementDeclFirstAttributeDeclIndex[][] = new int[INITIAL_CHUNK_COUNT][];

    /** Last attribute declaration of an element declaration. */
    private int fElementDeclLastAttributeDeclIndex[][] = new int[INITIAL_CHUNK_COUNT][];

    // attribute declarations

    /** Number of attribute declarations. */
    private int fAttributeDeclCount = 0 ;

    /** Attribute declaration name. */
    private QName fAttributeDeclName[][] = new QName[INITIAL_CHUNK_COUNT][];

    /**
     * Attribute declaration type.
     * @see XMLAttributeDecl
     */
    private short fAttributeDeclType[][] = new short[INITIAL_CHUNK_COUNT][];

    /** Attribute declaration enumeration values. */
    private String[] fAttributeDeclEnumeration[][] = new String[INITIAL_CHUNK_COUNT][][];
    private short fAttributeDeclDefaultType[][] = new short[INITIAL_CHUNK_COUNT][];
    private String fAttributeDeclDefaultValue[][] = new String[INITIAL_CHUNK_COUNT][];
    private String fAttributeDeclNonNormalizedDefaultValue[][] = new String[INITIAL_CHUNK_COUNT][];
    private int fAttributeDeclNextAttributeDeclIndex[][] = new int[INITIAL_CHUNK_COUNT][];

    /** Element index mapping table. */
    private final Map<String, Integer> fElementIndexMap = new HashMap<>();

    /** Temporary qualified name. */
    private final QName fQName = new QName();

    /** Temporary Attribute decl. */
    protected XMLAttributeDecl fAttributeDecl = new XMLAttributeDecl();

    /** Element declaration. */
    private XMLElementDecl fElementDecl = new XMLElementDecl();

    /** Simple type. */
    private XMLSimpleType fSimpleType = new XMLSimpleType();


    /** table of XMLElementDecl   */
    Map<String, XMLElementDecl> fElementDeclTab = new HashMap<>();

    /** Default constructor. */
    public DTDGrammar(SymbolTable symbolTable) {
        fSymbolTable = symbolTable;
    }

    public int getAttributeDeclIndex(int elementDeclIndex, String attributeDeclName) {
        if (elementDeclIndex == -1) {
            return -1;
        }
        int attDefIndex = getFirstAttributeDeclIndex(elementDeclIndex);
        while (attDefIndex != -1) {
            getAttributeDecl(attDefIndex, fAttributeDecl);

            if (fAttributeDecl.name.rawname == attributeDeclName
            || attributeDeclName.equals(fAttributeDecl.name.rawname) ) {
                return attDefIndex;
            }
            attDefIndex = getNextAttributeDeclIndex(attDefIndex);
        }
        return -1;
    }

    /**
     * The start of the DTD.
     *
     * @param locator  The document locator, or null if the document
     *                 location cannot be reported during the parsing of
     *                 the document DTD. However, it is <em>strongly</em>
     *                 recommended that a locator be supplied that can
     *                 at least report the base system identifier of the
     *                 DTD.
     *
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startDTD(XMLLocator locator, Augmentations augs) throws XNIException {
    } // startDTD(XMLLocator)

    // startExternalSubset(Augmentations)

    // endExternalSubset(Augmentations)

    /**
     * An element declaration.
     *
     * @param name         The name of the element.
     * @param contentModel The element content model.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void elementDecl(String name, String contentModel, Augmentations augs)
    throws XNIException {

        XMLElementDecl tmpElementDecl = fElementDeclTab.get(name) ;
        if ( tmpElementDecl != null ) {
            if (tmpElementDecl.type == -1) {
                fCurrentElementIndex = getElementDeclIndex(name);
            }
            else {
                // duplicate element, ignored.
                return;
            }
        }
        else {
            fCurrentElementIndex = createElementDecl();//create element decl
        }

        XMLElementDecl elementDecl       = new XMLElementDecl();
        QName          elementName       = new QName(null, name, name, null);

        elementDecl.name.setValues(elementName);
        elementDecl.scope= -1;
        if (contentModel.equals("EMPTY")) {
            elementDecl.type = XMLElementDecl.TYPE_EMPTY;
        }
        else if (contentModel.equals("ANY")) {
            elementDecl.type = XMLElementDecl.TYPE_ANY;
        }
        else if (contentModel.startsWith("(") ) {
            if (contentModel.indexOf("#PCDATA") > 0 ) {
                elementDecl.type = XMLElementDecl.TYPE_MIXED;
            }
            else {
                elementDecl.type = XMLElementDecl.TYPE_CHILDREN;
            }
        }


        //add(or set) this elementDecl to the local cache
        this.fElementDeclTab.put(name, elementDecl );

        fElementDecl = elementDecl;


        if ( DEBUG ) {
            System.out.println(  "name = " + fElementDecl.name.localpart );
            System.out.println(  "Type = " + fElementDecl.type );
        }

        setElementDecl(fCurrentElementIndex, fElementDecl );//set internal structure

        int chunk = fCurrentElementIndex >> CHUNK_SHIFT;
        ensureElementDeclCapacity(chunk);
    }

    /**
     * An attribute declaration.
     *
     * @param elementName   The name of the element that this attribute
     *                      is associated with.
     * @param attributeName The name of the attribute.
     * @param type          The attribute type. This value will be one of
     *                      the following: "CDATA", "ENTITY", "ENTITIES",
     *                      "ENUMERATION", "ID", "IDREF", "IDREFS",
     *                      "NMTOKEN", "NMTOKENS", or "NOTATION".
     * @param enumeration   If the type has the value "ENUMERATION", this
     *                      array holds the allowed attribute values;
     *                      otherwise, this array is null.
     * @param defaultType   The attribute default type. This value will be
     *                      one of the following: "#FIXED", "#IMPLIED",
     *                      "#REQUIRED", or null.
     * @param defaultValue  The attribute default value, or null if no
     *                      default value is specified.
     * @param nonNormalizedDefaultValue  The attribute default value with no normalization
     *                      performed, or null if no default value is specified.
     *
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void attributeDecl(String elementName, String attributeName,
    String type, String[] enumeration,
    String defaultType, XMLString defaultValue,
    XMLString nonNormalizedDefaultValue, Augmentations augs) throws XNIException {

        if (type != XMLSymbols.fCDATASymbol && defaultValue != null) {
            normalizeDefaultAttrValue(defaultValue);
        }

        if ( this.fElementDeclTab.containsKey(elementName)) {
            //if ElementDecl has already being created in the Grammar then remove from table,
            //this.fElementDeclTab.remove( (String) elementName );
        }
        // then it is forward reference to a element decl, create the elementDecl first.
        else {
            fCurrentElementIndex = createElementDecl();//create element decl

            XMLElementDecl elementDecl       = new XMLElementDecl();
            elementDecl.name.setValues(null, elementName, elementName, null);

            elementDecl.scope= -1;

            //add(or set) this elementDecl to the local cache
            this.fElementDeclTab.put(elementName, elementDecl );

            //set internal structure
            setElementDecl(fCurrentElementIndex, elementDecl );
        }

        //Get Grammar index to grammar array
        int elementIndex       = getElementDeclIndex(elementName);

        //return, when more than one definition is provided for the same attribute of given element type
        //only the first declaration is binding and later declarations are ignored
        if (getAttributeDeclIndex(elementIndex, attributeName) != -1) {
            return;
        }

        fCurrentAttributeIndex = createAttributeDecl();// Create current Attribute Decl

        fSimpleType.clear();
        if ( defaultType != null ) {
            if ( defaultType.equals( "#FIXED") ) {
                fSimpleType.defaultType = XMLSimpleType.DEFAULT_TYPE_FIXED;
            } else if ( defaultType.equals( "#IMPLIED") ) {
                fSimpleType.defaultType = XMLSimpleType.DEFAULT_TYPE_IMPLIED;
            } else if ( defaultType.equals( "#REQUIRED") ) {
                fSimpleType.defaultType = XMLSimpleType.DEFAULT_TYPE_REQUIRED;
            }
        }
        if ( DEBUG ) {
            System.out.println("defaultvalue = " + defaultValue.toString() );
        }
        fSimpleType.defaultValue = defaultValue!=null ? defaultValue.toString() : null;
        fSimpleType.nonNormalizedDefaultValue = nonNormalizedDefaultValue!=null ?
                nonNormalizedDefaultValue.toString() : null;
        fSimpleType.enumeration       = enumeration;

        if (type.equals("CDATA")) {
            fSimpleType.type = XMLSimpleType.TYPE_CDATA;
        }
        else if ( type.equals("ID") ) {
            fSimpleType.type = XMLSimpleType.TYPE_ID;
        }
        else if ( type.startsWith("IDREF") ) {
            fSimpleType.type = XMLSimpleType.TYPE_IDREF;
            if (type.indexOf("S") > 0) {
                fSimpleType.list = true;
            }
        }
        else if (type.equals("ENTITIES")) {
            fSimpleType.type = XMLSimpleType.TYPE_ENTITY;
            fSimpleType.list = true;
        }
        else if (type.equals("ENTITY")) {
            fSimpleType.type = XMLSimpleType.TYPE_ENTITY;
        }
        else if (type.equals("NMTOKENS")) {
            fSimpleType.type = XMLSimpleType.TYPE_NMTOKEN;
            fSimpleType.list = true;
        }
        else if (type.equals("NMTOKEN")) {
            fSimpleType.type = XMLSimpleType.TYPE_NMTOKEN;
        }
        else if (type.startsWith("NOTATION") ) {
            fSimpleType.type = XMLSimpleType.TYPE_NOTATION;
        }
        else if (type.startsWith("ENUMERATION") ) {
            fSimpleType.type = XMLSimpleType.TYPE_ENUMERATION;
        }
        else {
            // REVISIT: Report error message. -Ac
            System.err.println("!!! unknown attribute type "+type);
        }
        // REVISIT: The datatype should be stored with the attribute value
        //          and not special-cased in the XMLValidator. -Ac
        //fSimpleType.datatypeValidator = fDatatypeValidatorFactory.createDatatypeValidator(type, null, facets, fSimpleType.list);

        fQName.setValues(null, attributeName, attributeName, null);
        fAttributeDecl.setValues( fQName, fSimpleType, false );

        setAttributeDecl(elementIndex, fCurrentAttributeIndex, fAttributeDecl);

        int chunk = fCurrentAttributeIndex >> CHUNK_SHIFT;
        ensureAttributeDeclCapacity(chunk);
    } // attributeDecl(String,String,String,String[],String,XMLString,XMLString, Augmentations)

    /** Returns the symbol table. */
    public SymbolTable getSymbolTable() {
        return fSymbolTable;
    } // getSymbolTable():SymbolTable

    /**
     * Returns the index of the first element declaration. This index
     * is then used to query more information about the element declaration.
     *
     * @see #getNextElementDeclIndex
     * @see #getElementDecl
     */
    public int getFirstElementDeclIndex() {
        return fElementDeclCount >= 0 ? 0 : -1;
    } // getFirstElementDeclIndex():int

    /**
     * Returns the next index of the element declaration following the
     * specified element declaration.
     *
     * @param elementDeclIndex The element declaration index.
     */
    public int getNextElementDeclIndex(int elementDeclIndex) {
        return elementDeclIndex < fElementDeclCount - 1
        ? elementDeclIndex + 1 : -1;
    } // getNextElementDeclIndex(int):int

    /**
     * getElementDeclIndex
     *
     * @param elementDeclName
     *
     * @return index of the elementDeclName in scope
     */
    public int getElementDeclIndex(String elementDeclName) {
        Integer mapping = fElementIndexMap.get(elementDeclName);
        if (mapping == null) {
            mapping = -1;
        }
        //System.out.println("getElementDeclIndex("+elementDeclName+") -> "+mapping);
        return mapping;
    } // getElementDeclIndex(String):int

    /** Returns the element decl index.
     * @param elementDeclQName qualilfied name of the element
     */
    public int getElementDeclIndex(QName elementDeclQName) {
        return getElementDeclIndex(elementDeclQName.rawname);
    } // getElementDeclIndex(QName):int

    /** make separate function for getting contentSpecType of element.
     * we can avoid setting of the element values.
     */

    public short getContentSpecType(int elementIndex){
        if (elementIndex < 0 || elementIndex >= fElementDeclCount) {
            return -1 ;
        }

        int chunk = elementIndex >> CHUNK_SHIFT;
        int index = elementIndex &  CHUNK_MASK;

        if(fElementDeclType[chunk][index] == -1){
            return -1 ;
        }
        else{
            return (short) (fElementDeclType[chunk][index] & LIST_MASK);
        }
    }

    /**
     * getElementDecl
     *
     * @param elementDeclIndex
     * @param elementDecl The values of this structure are set by this call.
     *
     * @return True if find the element, False otherwise.
     */
    public boolean getElementDecl(int elementDeclIndex,
    XMLElementDecl elementDecl) {

        if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
            return false;
        }

        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex &  CHUNK_MASK;

        elementDecl.name.setValues(fElementDeclName[chunk][index]);

        if (fElementDeclType[chunk][index] == -1) {
            elementDecl.type                    = -1;
            elementDecl.simpleType.list = false;
        } else {
            elementDecl.type            = (short) (fElementDeclType[chunk][index] & LIST_MASK);
            elementDecl.simpleType.list = (fElementDeclType[chunk][index] & LIST_FLAG) != 0;
        }

        elementDecl.simpleType.defaultType       = -1;
        elementDecl.simpleType.defaultValue      = null;
        return true;

    }

    // REVISIT: Make this getAttributeDeclCount/getAttributeDeclAt. -Ac

    /**
     * getFirstAttributeDeclIndex
     *
     * @param elementDeclIndex
     *
     * @return index of the first attribute for element declaration elementDeclIndex
     */
    public int getFirstAttributeDeclIndex(int elementDeclIndex) {
        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex &  CHUNK_MASK;

        return  fElementDeclFirstAttributeDeclIndex[chunk][index];
    } // getFirstAttributeDeclIndex

    /**
     * getNextAttributeDeclIndex
     *
     * @param attributeDeclIndex
     *
     * @return index of the next attribute of the attribute at attributeDeclIndex
     */
    public int getNextAttributeDeclIndex(int attributeDeclIndex) {
        int chunk = attributeDeclIndex >> CHUNK_SHIFT;
        int index = attributeDeclIndex &  CHUNK_MASK;

        return fAttributeDeclNextAttributeDeclIndex[chunk][index];
    }

    /**
     * getAttributeDecl
     *
     * @param attributeDeclIndex
     * @param attributeDecl The values of this structure are set by this call.
     *
     * @return true if getAttributeDecl was able to fill in the value of attributeDecl
     */
    public boolean getAttributeDecl(int attributeDeclIndex, XMLAttributeDecl attributeDecl) {
        if (attributeDeclIndex < 0 || attributeDeclIndex >= fAttributeDeclCount) {
            return false;
        }
        int chunk = attributeDeclIndex >> CHUNK_SHIFT;
        int index = attributeDeclIndex & CHUNK_MASK;

        attributeDecl.name.setValues(fAttributeDeclName[chunk][index]);

        short attributeType;
        boolean isList;

        if (fAttributeDeclType[chunk][index] == -1) {

            attributeType = -1;
            isList = false;
        } else {
            attributeType = (short) (fAttributeDeclType[chunk][index] & LIST_MASK);
            isList = (fAttributeDeclType[chunk][index] & LIST_FLAG) != 0;
        }
        attributeDecl.simpleType.setValues(attributeType,fAttributeDeclName[chunk][index].localpart,
        fAttributeDeclEnumeration[chunk][index],
        isList, fAttributeDeclDefaultType[chunk][index],
        fAttributeDeclDefaultValue[chunk][index],
        fAttributeDeclNonNormalizedDefaultValue[chunk][index]);
        return true;

    } // getAttributeDecl


    /**
     * Returns whether the given attribute is of type CDATA or not
     *
     * @param elName The element name.
     * @param atName The attribute name.
     *
     * @return true if the attribute is of type CDATA
     */
    public boolean isCDATAAttribute(QName elName, QName atName) {
        int elDeclIdx = getElementDeclIndex(elName);
        if (getAttributeDecl(elDeclIdx, fAttributeDecl)
        && fAttributeDecl.simpleType.type != XMLSimpleType.TYPE_CDATA){
            return false;
        }
        return true;
    }



    public void printElements(  ) {
        int elementDeclIndex = 0;
        XMLElementDecl elementDecl = new XMLElementDecl();
        while (getElementDecl(elementDeclIndex++, elementDecl)) {

            System.out.println("element decl: "+elementDecl.name+
            ", "+ elementDecl.name.rawname  );

        }
    }

    public void printAttributes(int elementDeclIndex) {
        int attributeDeclIndex = getFirstAttributeDeclIndex(elementDeclIndex);
        System.out.print(elementDeclIndex);
        System.out.print(" [");
        while (attributeDeclIndex != -1) {
            System.out.print(' ');
            System.out.print(attributeDeclIndex);
            printAttribute(attributeDeclIndex);
            attributeDeclIndex = getNextAttributeDeclIndex(attributeDeclIndex);
            if (attributeDeclIndex != -1) {
                System.out.print(",");
            }
        }
        System.out.println(" ]");
    }


    protected int createElementDecl() {
        int chunk = fElementDeclCount >> CHUNK_SHIFT;
        int index = fElementDeclCount & CHUNK_MASK;
        ensureElementDeclCapacity(chunk);
        fElementDeclName[chunk][index]                    = new QName();
        fElementDeclType[chunk][index]                    = -1;
        fElementDeclFirstAttributeDeclIndex[chunk][index] = -1;
        fElementDeclLastAttributeDeclIndex[chunk][index]  = -1;
        return fElementDeclCount++;
    }

    protected void setElementDecl(int elementDeclIndex, XMLElementDecl elementDecl) {
        if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
            return;
        }
        int     chunk       = elementDeclIndex >> CHUNK_SHIFT;
        int     index       = elementDeclIndex &  CHUNK_MASK;

        int     scope       = elementDecl.scope;


        fElementDeclName[chunk][index].setValues(elementDecl.name);
        fElementDeclType[chunk][index]                  = elementDecl.type;



        if (elementDecl.simpleType.list  == true ) {
            fElementDeclType[chunk][index] |= LIST_FLAG;
        }

        fElementIndexMap.put(elementDecl.name.rawname, elementDeclIndex);
    }




    protected void setFirstAttributeDeclIndex(int elementDeclIndex, int newFirstAttrIndex){

        if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
            return;
        }

        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex &  CHUNK_MASK;

        fElementDeclFirstAttributeDeclIndex[chunk][index] = newFirstAttrIndex;
    }


    protected int createAttributeDecl() {
        int chunk = fAttributeDeclCount >> CHUNK_SHIFT;
        int index = fAttributeDeclCount & CHUNK_MASK;

        ensureAttributeDeclCapacity(chunk);
        fAttributeDeclName[chunk][index]                    = new QName();
        fAttributeDeclType[chunk][index]                    = -1;
        fAttributeDeclEnumeration[chunk][index]             = null;
        fAttributeDeclDefaultType[chunk][index]             = XMLSimpleType.DEFAULT_TYPE_IMPLIED;
        fAttributeDeclDefaultValue[chunk][index]            = null;
        fAttributeDeclNonNormalizedDefaultValue[chunk][index]            = null;
        fAttributeDeclNextAttributeDeclIndex[chunk][index]  = -1;
        return fAttributeDeclCount++;
    }


    protected void setAttributeDecl(int elementDeclIndex, int attributeDeclIndex,
    XMLAttributeDecl attributeDecl) {
        int attrChunk = attributeDeclIndex >> CHUNK_SHIFT;
        int attrIndex = attributeDeclIndex &  CHUNK_MASK;
        fAttributeDeclName[attrChunk][attrIndex].setValues(attributeDecl.name);
        fAttributeDeclType[attrChunk][attrIndex]  =  attributeDecl.simpleType.type;

        if (attributeDecl.simpleType.list) {
            fAttributeDeclType[attrChunk][attrIndex] |= LIST_FLAG;
        }
        fAttributeDeclEnumeration[attrChunk][attrIndex]  =  attributeDecl.simpleType.enumeration;
        fAttributeDeclDefaultType[attrChunk][attrIndex]  =  attributeDecl.simpleType.defaultType;

        fAttributeDeclDefaultValue[attrChunk][attrIndex] = attributeDecl.simpleType.defaultValue;
        fAttributeDeclNonNormalizedDefaultValue[attrChunk][attrIndex] = attributeDecl.simpleType.nonNormalizedDefaultValue;

        int elemChunk     = elementDeclIndex >> CHUNK_SHIFT;
        int elemIndex     = elementDeclIndex &  CHUNK_MASK;
        int index         = fElementDeclFirstAttributeDeclIndex[elemChunk][elemIndex];
        while (index != -1) {
            if (index == attributeDeclIndex) {
                break;
            }
            attrChunk = index >> CHUNK_SHIFT;
            attrIndex = index & CHUNK_MASK;
            index = fAttributeDeclNextAttributeDeclIndex[attrChunk][attrIndex];
        }
        if (index == -1) {
            if (fElementDeclFirstAttributeDeclIndex[elemChunk][elemIndex] == -1) {
                fElementDeclFirstAttributeDeclIndex[elemChunk][elemIndex] = attributeDeclIndex;
            } else {
                index = fElementDeclLastAttributeDeclIndex[elemChunk][elemIndex];
                attrChunk = index >> CHUNK_SHIFT;
                attrIndex = index & CHUNK_MASK;
                fAttributeDeclNextAttributeDeclIndex[attrChunk][attrIndex] = attributeDeclIndex;
            }
            fElementDeclLastAttributeDeclIndex[elemChunk][elemIndex] = attributeDeclIndex;
        }
    }

    public void notationDecl(String name, XMLResourceIdentifier identifier,
    Augmentations augs) throws XNIException {


        XMLNotationDecl  notationDecl = new XMLNotationDecl();
        notationDecl.setValues(name,identifier.getPublicId(),identifier.getLiteralSystemId(),
        identifier.getBaseSystemId());
        notationDecls.add(notationDecl);
    }

    public List<XMLNotationDecl> getNotationDecls() {
        return notationDecls;
    }

    //
    // Private methods
    //
    private void printAttribute(int attributeDeclIndex) {

        XMLAttributeDecl attributeDecl = new XMLAttributeDecl();
        if (getAttributeDecl(attributeDeclIndex, attributeDecl)) {
            System.out.print(" { ");
            System.out.print(attributeDecl.name.localpart);
            System.out.print(" }");
        }

    } // printAttribute(int)



    private void ensureElementDeclCapacity(int chunk) {
        if (chunk >= fElementDeclName.length) {

            fElementDeclName = resize(fElementDeclName, fElementDeclName.length * 2);
            fElementDeclType = resize(fElementDeclType, fElementDeclType.length * 2);
            fElementDeclFirstAttributeDeclIndex = resize(fElementDeclFirstAttributeDeclIndex, fElementDeclFirstAttributeDeclIndex.length * 2);
            fElementDeclLastAttributeDeclIndex = resize(fElementDeclLastAttributeDeclIndex, fElementDeclLastAttributeDeclIndex.length * 2);
        }
        else if (fElementDeclName[chunk] != null) {
            return;
        }

        fElementDeclName[chunk] = new QName[CHUNK_SIZE];
        fElementDeclType[chunk] = new short[CHUNK_SIZE];
        fElementDeclFirstAttributeDeclIndex[chunk] = new int[CHUNK_SIZE];
        fElementDeclLastAttributeDeclIndex[chunk] = new int[CHUNK_SIZE];
        return;
    }

    private void ensureAttributeDeclCapacity(int chunk) {

        if (chunk >= fAttributeDeclName.length) {
            fAttributeDeclName = resize(fAttributeDeclName, fAttributeDeclName.length * 2);
            fAttributeDeclType = resize(fAttributeDeclType, fAttributeDeclType.length * 2);
            fAttributeDeclEnumeration = resize(fAttributeDeclEnumeration, fAttributeDeclEnumeration.length * 2);
            fAttributeDeclDefaultType = resize(fAttributeDeclDefaultType, fAttributeDeclDefaultType.length * 2);
            fAttributeDeclDefaultValue = resize(fAttributeDeclDefaultValue, fAttributeDeclDefaultValue.length * 2);
            fAttributeDeclNonNormalizedDefaultValue = resize(fAttributeDeclNonNormalizedDefaultValue, fAttributeDeclNonNormalizedDefaultValue.length * 2);
            fAttributeDeclNextAttributeDeclIndex = resize(fAttributeDeclNextAttributeDeclIndex, fAttributeDeclNextAttributeDeclIndex.length * 2);
        }
        else if (fAttributeDeclName[chunk] != null) {
            return;
        }

        fAttributeDeclName[chunk] = new QName[CHUNK_SIZE];
        fAttributeDeclType[chunk] = new short[CHUNK_SIZE];
        fAttributeDeclEnumeration[chunk] = new String[CHUNK_SIZE][];
        fAttributeDeclDefaultType[chunk] = new short[CHUNK_SIZE];
        fAttributeDeclDefaultValue[chunk] = new String[CHUNK_SIZE];
        fAttributeDeclNonNormalizedDefaultValue[chunk] = new String[CHUNK_SIZE];
        fAttributeDeclNextAttributeDeclIndex[chunk] = new int[CHUNK_SIZE];
        return;
    }


    // resize chunks

    private static short[][] resize(short array[][], int newsize) {
        short newarray[][] = new short[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static int[][] resize(int array[][], int newsize) {
        int newarray[][] = new int[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static QName[][] resize(QName array[][], int newsize) {
        QName newarray[][] = new QName[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static String[][] resize(String array[][], int newsize) {
        String newarray[][] = new String[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static String[][][] resize(String array[][][], int newsize) {
        String newarray[][][] = new String[newsize] [][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    /**
     * Normalize the attribute value of a non CDATA default attribute
     * collapsing sequences of space characters (x20)
     *
     * @param value The value to normalize
     * @return Whether the value was changed or not.
     */
    private boolean normalizeDefaultAttrValue(XMLString value) {

        int oldLength = value.length;

        boolean skipSpace = true; // skip leading spaces
        int current = value.offset;
        int end = value.offset + value.length;
        for (int i = value.offset; i < end; i++) {
            if (value.ch[i] == ' ') {
                if (!skipSpace) {
                    // take the first whitespace as a space and skip the others
                    value.ch[current++] = ' ';
                    skipSpace = true;
                }
                else {
                    // just skip it.
                }
            }
            else {
                // simply shift non space chars if needed
                if (current != i) {
                    value.ch[current] = value.ch[i];
                }
                current++;
                skipSpace = false;
            }
        }
        if (current != end) {
            if (skipSpace) {
                // if we finished on a space trim it
                current--;
            }
            // set the new value length
            value.length = current - value.offset;
            return true;
        }
        return false;
    }
    public void endDTD(Augmentations augs) throws XNIException {

    }
}
