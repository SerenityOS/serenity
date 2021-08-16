/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl.dtd;

import com.sun.org.apache.xerces.internal.impl.dtd.models.CMAny;
import com.sun.org.apache.xerces.internal.impl.dtd.models.CMBinOp;
import com.sun.org.apache.xerces.internal.impl.dtd.models.CMLeaf;
import com.sun.org.apache.xerces.internal.impl.dtd.models.CMNode;
import com.sun.org.apache.xerces.internal.impl.dtd.models.CMUniOp;
import com.sun.org.apache.xerces.internal.impl.dtd.models.ContentModelValidator;
import com.sun.org.apache.xerces.internal.impl.dtd.models.DFAContentModel;
import com.sun.org.apache.xerces.internal.impl.dtd.models.MixedContentModel;
import com.sun.org.apache.xerces.internal.impl.dtd.models.SimpleContentModel;
import com.sun.org.apache.xerces.internal.impl.dv.DatatypeValidator;
import com.sun.org.apache.xerces.internal.impl.validation.EntityState;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLDTDContentModelHandler;
import com.sun.org.apache.xerces.internal.xni.XMLDTDHandler;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDTDContentModelSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDTDSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A DTD grammar. This class implements the XNI handler interfaces
 * for DTD information so that it can build the appropriate validation
 * structures automatically from the callbacks.
 *
 * @xerces.internal
 *
 * @author Eric Ye, IBM
 * @author Jeffrey Rodriguez, IBM
 * @author Andy Clark, IBM
 * @author Neil Graham, IBM
 *
 * @LastModified: Feb 2020
 */
public class DTDGrammar
    implements XMLDTDHandler, XMLDTDContentModelHandler, EntityState, Grammar {

    //
    // Constants
    //

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
    private SymbolTable fSymbolTable;

    // The XMLDTDDescription with which this Grammar is associated
    protected XMLDTDDescription fGrammarDescription = null;

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

    /**
     * Element declaration content spec index. This index value is used
     * to refer to the content spec information tables.
     */
    private int fElementDeclContentSpecIndex[][] = new int[INITIAL_CHUNK_COUNT][];

    /**
     * Element declaration content model validator. This validator is
     * constructed from the content spec nodes.
     */
    private ContentModelValidator fElementDeclContentModelValidator[][] = new ContentModelValidator[INITIAL_CHUNK_COUNT][];

    /** First attribute declaration of an element declaration. */
    private int fElementDeclFirstAttributeDeclIndex[][] = new int[INITIAL_CHUNK_COUNT][];

    /** Last attribute declaration of an element declaration. */
    private int fElementDeclLastAttributeDeclIndex[][] = new int[INITIAL_CHUNK_COUNT][];

    // attribute declarations

    /** Number of attribute declarations. */
    private int fAttributeDeclCount = 0 ;

    /** Attribute declaration name. */
    private QName fAttributeDeclName[][] = new QName[INITIAL_CHUNK_COUNT][];

    // is this grammar immutable?  (fully constructed and not changeable)
    private boolean fIsImmutable = false;

    /**
     * Attribute declaration type.
     * @see XMLAttributeDecl
     */
    private short fAttributeDeclType[][] = new short[INITIAL_CHUNK_COUNT][];

    /** Attribute declaration enumeration values. */
    private String[] fAttributeDeclEnumeration[][] = new String[INITIAL_CHUNK_COUNT][][];
    private short fAttributeDeclDefaultType[][] = new short[INITIAL_CHUNK_COUNT][];
    private DatatypeValidator fAttributeDeclDatatypeValidator[][] = new DatatypeValidator[INITIAL_CHUNK_COUNT][];
    private String fAttributeDeclDefaultValue[][] = new String[INITIAL_CHUNK_COUNT][];
    private String fAttributeDeclNonNormalizedDefaultValue[][] = new String[INITIAL_CHUNK_COUNT][];
    private int fAttributeDeclNextAttributeDeclIndex[][] = new int[INITIAL_CHUNK_COUNT][];

    // content specs

    // here saves the content spec binary trees for element decls,
    // each element with a content model will hold a pointer which is
    // the index of the head node of the content spec tree.

    private int fContentSpecCount = 0;
    private short fContentSpecType[][] = new short[INITIAL_CHUNK_COUNT][];
    private Object fContentSpecValue[][] = new Object[INITIAL_CHUNK_COUNT][];
    private Object fContentSpecOtherValue[][] = new Object[INITIAL_CHUNK_COUNT][];

    // entities

    private int fEntityCount = 0;
    private String fEntityName[][] = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fEntityValue = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fEntityPublicId = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fEntitySystemId = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fEntityBaseSystemId = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fEntityNotation = new String[INITIAL_CHUNK_COUNT][];
    private byte[][] fEntityIsPE = new byte[INITIAL_CHUNK_COUNT][];
    private byte[][] fEntityInExternal = new byte[INITIAL_CHUNK_COUNT][];

    // notations

    private int fNotationCount = 0;
    private String fNotationName[][] = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fNotationPublicId = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fNotationSystemId = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fNotationBaseSystemId = new String[INITIAL_CHUNK_COUNT][];

    // other information

    /** Element index mapping table. */
    private final Map<String, Integer> fElementIndexMap = new HashMap<>();

    /** Entity index mapping table. */
    private final Map<String, Integer> fEntityIndexMap = new HashMap<>();

    /** Notation index mapping table. */
    private final Map<String, Integer> fNotationIndexMap = new HashMap<>();

    // temp variables

    /** Mixed. */
    private boolean fMixed;

    /** Temporary qualified name. */
    private final QName fQName = new QName();

    /** Temporary qualified name. */
    private final QName fQName2 = new QName();

    /** Temporary Attribute decl. */
    protected final XMLAttributeDecl fAttributeDecl = new XMLAttributeDecl();

    // for buildSyntaxTree method

    private int fLeafCount = 0;
    private int fEpsilonIndex = -1;

    /** Element declaration. */
    private XMLElementDecl fElementDecl = new XMLElementDecl();

    /** Entity declaration. */
    private XMLEntityDecl fEntityDecl = new XMLEntityDecl();

    /** Simple type. */
    private XMLSimpleType fSimpleType = new XMLSimpleType();

    /** Content spec node. */
    private XMLContentSpec fContentSpec = new XMLContentSpec();

    /** table of XMLElementDecl   */
    Map<String, XMLElementDecl> fElementDeclTab = new HashMap<>();

    /** Children content model operation stack. */
    private short[] fOpStack = null;

    /** Children content model index stack. */
    private int[] fNodeIndexStack = null;

    /** Children content model previous node index stack. */
    private int[] fPrevNodeIndexStack = null;

    /** Stack depth   */
    private int fDepth = 0;

    /** Entity stack. */
    private boolean[] fPEntityStack = new boolean[4];
    private int fPEDepth = 0;

    // additional fields(columns) for the element Decl pool in the Grammar

    /** flag if the elementDecl is External. */
    private int fElementDeclIsExternal[][] = new int[INITIAL_CHUNK_COUNT][];


    // additional fields(columns) for the attribute Decl pool in the Grammar

    /** flag if the AttributeDecl is External. */
    private int fAttributeDeclIsExternal[][] = new int[INITIAL_CHUNK_COUNT][];

    // for mixedElement method

    int valueIndex            = -1;
    int prevNodeIndex         = -1;
    int nodeIndex             = -1;

    //
    // Constructors
    //

    /** Default constructor. */
    public DTDGrammar(SymbolTable symbolTable, XMLDTDDescription desc) {
        fSymbolTable = symbolTable;
        fGrammarDescription = desc;
    } // <init>(SymbolTable)

    // Grammar methods

    // return the XMLDTDDescription object with which this is associated
    public XMLGrammarDescription getGrammarDescription() {
        return fGrammarDescription;
    } // getGrammarDescription():  XMLGrammarDescription

    //
    // Public methods
    //

    /**
     * Returns true if the specified element declaration is external.
     *
     * @param elementDeclIndex The element declaration index.
     */
    public boolean getElementDeclIsExternal(int elementDeclIndex) {

        if (elementDeclIndex < 0) {
            return false;
        }

        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex & CHUNK_MASK;
        return (fElementDeclIsExternal[chunk][index] != 0);

    } // getElementDeclIsExternal(int):boolean

    /**
     * Returns true if the specified attribute declaration is external.
     *
     * @param attributeDeclIndex Attribute declaration index.
     */
    public boolean getAttributeDeclIsExternal(int attributeDeclIndex) {

        if (attributeDeclIndex < 0) {
            return false;
        }

        int chunk = attributeDeclIndex >> CHUNK_SHIFT;
        int index = attributeDeclIndex & CHUNK_MASK;
        return (fAttributeDeclIsExternal[chunk][index] != 0);
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
    } // getAttributeDeclIndex (int,QName)

    //
    // XMLDTDHandler methods
    //

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
        //Initialize stack
        fOpStack = null;
        fNodeIndexStack = null;
        fPrevNodeIndexStack = null;
    } // startDTD(XMLLocator)

    /**
     * This method notifies of the start of an entity. The DTD has the
     * pseudo-name of "[dtd]" and parameter entity names start with '%'.
     * <p>
     * <strong>Note:</strong> Since the DTD is an entity, the handler
     * will be notified of the start of the DTD entity by calling the
     * startParameterEntity method with the entity name "[dtd]" <em>before</em> calling
     * the startDTD method.
     *
     * @param name     The name of the parameter entity.
     * @param identifier The resource identifier.
     * @param encoding The auto-detected IANA encoding name of the entity
     *                 stream. This value will be null in those situations
     *                 where the entity encoding is not auto-detected (e.g.
     *                 internal parameter entities).
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startParameterEntity(String name,
                                     XMLResourceIdentifier identifier,
                                     String encoding,
                                     Augmentations augs) throws XNIException {

        // keep track of this entity before fEntityDepth is increased
        if (fPEDepth == fPEntityStack.length) {
            boolean[] entityarray = new boolean[fPEntityStack.length * 2];
            System.arraycopy(fPEntityStack, 0, entityarray, 0, fPEntityStack.length);
            fPEntityStack = entityarray;
        }
        fPEntityStack[fPEDepth] = fReadingExternalDTD;
        fPEDepth++;

    } // startParameterEntity(String,XMLResourceIdentifier,String,Augmentations)

    /**
     * The start of the DTD external subset.
     *
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startExternalSubset(XMLResourceIdentifier identifier,
                                    Augmentations augs) throws XNIException {
        fReadingExternalDTD = true;
    } // startExternalSubset(Augmentations)

    /**
     * This method notifies the end of an entity. The DTD has the pseudo-name
     * of "[dtd]" and parameter entity names start with '%'.
     * <p>
     * <strong>Note:</strong> Since the DTD is an entity, the handler
     * will be notified of the end of the DTD entity by calling the
     * endEntity method with the entity name "[dtd]" <em>after</em> calling
     * the endDTD method.
     *
     * @param name The name of the entity.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endParameterEntity(String name, Augmentations augs) throws XNIException {
        // redundant check as this method can only be called after parsing a PE
        // incomplete or truncated DTD get caught before reaching this method
        if (fPEDepth > 0) {
            fPEDepth--;
            fReadingExternalDTD = fPEntityStack[fPEDepth];
        }

    } // endParameterEntity(String,Augmentations)

    /**
     * The end of the DTD external subset.
     *
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endExternalSubset(Augmentations augs) throws XNIException {
        fReadingExternalDTD = false;
    } // endExternalSubset(Augmentations)

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

        // check if it is already defined
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

        fQName.setValues(null, name, name, null);

        elementDecl.name.setValues(fQName);

        elementDecl.contentModelValidator = null;
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
        addContentSpecToElement(elementDecl);

        if ( DEBUG ) {
            System.out.println(  "name = " + fElementDecl.name.localpart );
            System.out.println(  "Type = " + fElementDecl.type );
        }

        setElementDecl(fCurrentElementIndex, fElementDecl );//set internal structure

        int chunk = fCurrentElementIndex >> CHUNK_SHIFT;
        int index = fCurrentElementIndex & CHUNK_MASK;
        ensureElementDeclCapacity(chunk);
        fElementDeclIsExternal[chunk][index] = (fReadingExternalDTD || fPEDepth > 0) ? 1 : 0;

    } // elementDecl(String,String)

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

        if ( this.fElementDeclTab.containsKey(elementName) ) {
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
        fSimpleType.defaultValue      = defaultValue!=null ?  defaultValue.toString() : null;
        fSimpleType.nonNormalizedDefaultValue      = nonNormalizedDefaultValue!=null ?  nonNormalizedDefaultValue.toString() : null;
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
        int index = fCurrentAttributeIndex & CHUNK_MASK;
        ensureAttributeDeclCapacity(chunk);
        fAttributeDeclIsExternal[chunk][index] = (fReadingExternalDTD || fPEDepth > 0) ?  1 : 0;

    } // attributeDecl(String,String,String,String[],String,XMLString,XMLString, Augmentations)

    /**
     * An internal entity declaration.
     *
     * @param name The name of the entity. Parameter entity names start with
     *             '%', whereas the name of a general entity is just the
     *             entity name.
     * @param text The value of the entity.
     * @param nonNormalizedText The non-normalized value of the entity. This
     *             value contains the same sequence of characters that was in
     *             the internal entity declaration, without any entity
     *             references expanded.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void internalEntityDecl(String name, XMLString text,
                                   XMLString nonNormalizedText,
                                   Augmentations augs) throws XNIException {

        int entityIndex = getEntityDeclIndex(name);
        if( entityIndex == -1){
            entityIndex = createEntityDecl();
            boolean isPE = name.startsWith("%");
            boolean inExternal = (fReadingExternalDTD || fPEDepth > 0);
            XMLEntityDecl  entityDecl = new XMLEntityDecl();
            entityDecl.setValues(name,null,null, null, null,
                                 text.toString(), isPE, inExternal);

            setEntityDecl(entityIndex, entityDecl);
        }

    } // internalEntityDecl(String,XMLString,XMLString)

    /**
     * An external entity declaration.
     *
     * @param name     The name of the entity. Parameter entity names start
     *                 with '%', whereas the name of a general entity is just
     *                 the entity name.
     * @param identifier    An object containing all location information
     *                      pertinent to this external entity declaration.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void externalEntityDecl(String name,
                                   XMLResourceIdentifier identifier,
                                   Augmentations augs) throws XNIException {

        int entityIndex = getEntityDeclIndex(name);
        if( entityIndex == -1){
            entityIndex = createEntityDecl();
            boolean isPE = name.startsWith("%");
            boolean inExternal = (fReadingExternalDTD || fPEDepth > 0);

            XMLEntityDecl  entityDecl = new XMLEntityDecl();
            entityDecl.setValues(name, identifier.getPublicId(), identifier.getLiteralSystemId(),
                                identifier.getBaseSystemId(),
                                null, null, isPE, inExternal);

            setEntityDecl(entityIndex, entityDecl);
        }
    } // externalEntityDecl(String, XMLResourceIdentifier, Augmentations)

    /**
     * An unparsed entity declaration.
     *
     * @param name     The name of the entity.
     * @param identifier    An object containing all location information
     *                      pertinent to this entity.
     * @param notation The name of the notation.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void unparsedEntityDecl(String name, XMLResourceIdentifier identifier,
                                   String notation,
                                   Augmentations augs) throws XNIException {

        XMLEntityDecl  entityDecl = new XMLEntityDecl();
        boolean isPE = name.startsWith("%");
        boolean inExternal = (fReadingExternalDTD || fPEDepth > 0);

        entityDecl.setValues(name,identifier.getPublicId(),identifier.getLiteralSystemId(),
                            identifier.getBaseSystemId(), notation,
                            null, isPE, inExternal);
        int entityIndex = getEntityDeclIndex(name);
        if (entityIndex == -1) {
            entityIndex = createEntityDecl();
            setEntityDecl(entityIndex, entityDecl);
        }

    } // unparsedEntityDecl(String,StringXMLResourceIdentifier,Augmentations)

    /**
     * A notation declaration
     *
     * @param name     The name of the notation.
     * @param identifier    An object containing all location information
     *                      pertinent to this notation.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void notationDecl(String name, XMLResourceIdentifier identifier,
                             Augmentations augs) throws XNIException {

        XMLNotationDecl  notationDecl = new XMLNotationDecl();
        notationDecl.setValues(name,identifier.getPublicId(),identifier.getLiteralSystemId(),
                identifier.getBaseSystemId());
        int notationIndex = getNotationDeclIndex(name);
        if (notationIndex == -1) {
            notationIndex = createNotationDecl();
            setNotationDecl(notationIndex, notationDecl);
        }

    } // notationDecl(String,XMLResourceIdentifier,Augmentations)

    /**
     * The end of the DTD.
     *
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endDTD(Augmentations augs) throws XNIException {
        fIsImmutable = true;
        // make sure our description contains useful stuff...
        if (fGrammarDescription.getRootName() == null) {
            // we don't know what the root is; so use possibleRoots...
            int chunk, index = 0;
            String currName = null;
            final int size = fElementDeclCount;
            List<String> elements = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                chunk = i >> CHUNK_SHIFT;
                index = i & CHUNK_MASK;
                currName = fElementDeclName[chunk][index].rawname;
                elements.add(currName);
            }
            fGrammarDescription.setPossibleRoots(elements);
        }
    } // endDTD()

    // sets the source of this handler
    public void setDTDSource(XMLDTDSource source) {
        fDTDSource = source;
    } // setDTDSource(XMLDTDSource)

    // returns the source of this handler
    public XMLDTDSource getDTDSource() {
        return fDTDSource;
    } // getDTDSource():  XMLDTDSource

    // no-op methods

    /**
     * Notifies of the presence of a TextDecl line in an entity. If present,
     * this method will be called immediately following the startEntity call.
     * <p>
     * <strong>Note:</strong> This method is only called for external
     * parameter entities referenced in the DTD.
     *
     * @param version  The XML version, or null if not specified.
     * @param encoding The IANA encoding name of the entity.
     *
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void textDecl(String version, String encoding, Augmentations augs)
        throws XNIException {}

    /**
     * A comment.
     *
     * @param text The text in the comment.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by application to signal an error.
     */
    public void comment(XMLString text, Augmentations augs) throws XNIException {}

    /**
     * A processing instruction. Processing instructions consist of a
     * target name and, optionally, text data. The data is only meaningful
     * to the application.
     * <p>
     * Typically, a processing instruction's data will contain a series
     * of pseudo-attributes. These pseudo-attributes follow the form of
     * element attributes but are <strong>not</strong> parsed or presented
     * to the application as anything other than text. The application is
     * responsible for parsing the data.
     *
     * @param target The target.
     * @param data   The data or null if none specified.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void processingInstruction(String target, XMLString data,
                                      Augmentations augs) throws XNIException {}

    /**
     * The start of an attribute list.
     *
     * @param elementName The name of the element that this attribute
     *                    list is associated with.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startAttlist(String elementName, Augmentations augs)
        throws XNIException {}

    /**
     * The end of an attribute list.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endAttlist(Augmentations augs) throws XNIException {}

    /**
     * The start of a conditional section.
     *
     * @param type The type of the conditional section. This value will
     *             either be CONDITIONAL_INCLUDE or CONDITIONAL_IGNORE.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     *
     * @see XMLDTDHandler#CONDITIONAL_INCLUDE
     * @see XMLDTDHandler#CONDITIONAL_IGNORE
     */
    public void startConditional(short type, Augmentations augs)
        throws XNIException {}

    /**
     * Characters within an IGNORE conditional section.
     *
     * @param text The ignored text.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     */
    public void ignoredCharacters(XMLString text, Augmentations augs)
        throws XNIException {}

    /**
     * The end of a conditional section.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endConditional(Augmentations augs) throws XNIException {}

    //
    // XMLDTDContentModelHandler methods
    //

    // set content model source
    public void setDTDContentModelSource(XMLDTDContentModelSource source) {
        fDTDContentModelSource = source;
    }

    // get content model source
    public XMLDTDContentModelSource getDTDContentModelSource() {
        return fDTDContentModelSource;
    }

    /**
     * The start of a content model. Depending on the type of the content
     * model, specific methods may be called between the call to the
     * startContentModel method and the call to the endContentModel method.
     *
     * @param elementName The name of the element.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startContentModel(String elementName, Augmentations augs)
        throws XNIException {

        XMLElementDecl elementDecl = this.fElementDeclTab.get(elementName);
        if ( elementDecl != null ) {
            fElementDecl = elementDecl;
        }
        fDepth = 0;
        initializeContentModelStack();

    } // startContentModel(String)

    /**
     * A start of either a mixed or children content model. A mixed
     * content model will immediately be followed by a call to the
     * <code>pcdata()</code> method. A children content model will
     * contain additional groups and/or elements.
     *
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     *
     * @see #any
     * @see #empty
     */
    public void startGroup(Augmentations augs) throws XNIException {
        fDepth++;
        initializeContentModelStack();
        fMixed = false;
    } // startGroup()

    /**
     * The appearance of "#PCDATA" within a group signifying a
     * mixed content model. This method will be the first called
     * following the content model's <code>startGroup()</code>.
     *
     *@param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     *
     * @see #startGroup
     */
    public void pcdata(Augmentations augs) throws XNIException {
        fMixed = true;
    } // pcdata()

    /**
     * A referenced element in a mixed or children content model.
     *
     * @param elementName The name of the referenced element.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void element(String elementName, Augmentations augs) throws XNIException {
        if (fMixed) {
            if (fNodeIndexStack[fDepth] == -1 ) {
                fNodeIndexStack[fDepth] = addUniqueLeafNode(elementName);
            }
            else {
                fNodeIndexStack[fDepth] = addContentSpecNode(XMLContentSpec.CONTENTSPECNODE_CHOICE,
                                                             fNodeIndexStack[fDepth],
                                                             addUniqueLeafNode(elementName));
            }
        }
        else {
            fNodeIndexStack[fDepth] = addContentSpecNode(XMLContentSpec.CONTENTSPECNODE_LEAF, elementName);
        }
    } // element(String)

    /**
     * The separator between choices or sequences of a mixed or children
     * content model.
     *
     * @param separator The type of children separator.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     *
     * @see org.apache.xerces.xni.XMLDTDContentModelHandler#SEPARATOR_CHOICE
     * @see org.apache.xerces.xni.XMLDTDContentModelHandler#SEPARATOR_SEQUENCE
     */
    public void separator(short separator, Augmentations augs) throws XNIException {

        if (!fMixed) {
            if (fOpStack[fDepth] != XMLContentSpec.CONTENTSPECNODE_SEQ && separator == XMLDTDContentModelHandler.SEPARATOR_CHOICE ) {
                if (fPrevNodeIndexStack[fDepth] != -1) {
                    fNodeIndexStack[fDepth] = addContentSpecNode(fOpStack[fDepth], fPrevNodeIndexStack[fDepth], fNodeIndexStack[fDepth]);
                }
                fPrevNodeIndexStack[fDepth] = fNodeIndexStack[fDepth];
                fOpStack[fDepth] = XMLContentSpec.CONTENTSPECNODE_CHOICE;
            } else if (fOpStack[fDepth] != XMLContentSpec.CONTENTSPECNODE_CHOICE && separator == XMLDTDContentModelHandler.SEPARATOR_SEQUENCE) {
                if (fPrevNodeIndexStack[fDepth] != -1) {
                    fNodeIndexStack[fDepth] = addContentSpecNode(fOpStack[fDepth], fPrevNodeIndexStack[fDepth], fNodeIndexStack[fDepth]);
                }
                fPrevNodeIndexStack[fDepth] = fNodeIndexStack[fDepth];
                fOpStack[fDepth] = XMLContentSpec.CONTENTSPECNODE_SEQ;
            }
        }

    } // separator(short)

    /**
     * The occurrence count for a child in a children content model or
     * for the mixed content model group.
     *
     * @param occurrence The occurrence count for the last element
     *                   or group.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     *
     * @see org.apache.xerces.xni.XMLDTDContentModelHandler#OCCURS_ZERO_OR_ONE
     * @see org.apache.xerces.xni.XMLDTDContentModelHandler#OCCURS_ZERO_OR_MORE
     * @see org.apache.xerces.xni.XMLDTDContentModelHandler#OCCURS_ONE_OR_MORE
     */
    public void occurrence(short occurrence, Augmentations augs) throws XNIException {

        if (!fMixed) {
            if (occurrence == XMLDTDContentModelHandler.OCCURS_ZERO_OR_ONE ) {
                fNodeIndexStack[fDepth] = addContentSpecNode(XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE, fNodeIndexStack[fDepth], -1);
            } else if ( occurrence == XMLDTDContentModelHandler.OCCURS_ZERO_OR_MORE ) {
                fNodeIndexStack[fDepth] = addContentSpecNode(XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE, fNodeIndexStack[fDepth], -1 );
            } else if ( occurrence == XMLDTDContentModelHandler.OCCURS_ONE_OR_MORE) {
                fNodeIndexStack[fDepth] = addContentSpecNode(XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE, fNodeIndexStack[fDepth], -1 );
            }
        }

    } // occurrence(short)

    /**
     * The end of a group for mixed or children content models.
     *
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endGroup(Augmentations augs) throws XNIException {

        if (!fMixed) {
            if (fPrevNodeIndexStack[fDepth] != -1) {
                fNodeIndexStack[fDepth] = addContentSpecNode(fOpStack[fDepth], fPrevNodeIndexStack[fDepth], fNodeIndexStack[fDepth]);
            }
            int nodeIndex = fNodeIndexStack[fDepth--];
            fNodeIndexStack[fDepth] = nodeIndex;
        }

    } // endGroup()

    // no-op methods

    /**
     * A content model of ANY.
     *
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     *
     * @see #empty
     * @see #startGroup
     */
    public void any(Augmentations augs) throws XNIException {}

    /**
     * A content model of EMPTY.
     *
     * @param augs Additional information that may include infoset
     *                      augmentations.
     * @throws XNIException Thrown by handler to signal an error.
     *
     * @see #any
     * @see #startGroup
     */
    public void empty(Augmentations augs) throws XNIException {}

    /**
     * The end of a content model.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endContentModel(Augmentations augs) throws XNIException {}

    //
    // Grammar methods
    //

    /** Returns true if this grammar is namespace aware. */
    public boolean isNamespaceAware() {
        return false;
    } // isNamespaceAware():boolean

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

                }//getContentSpecType

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

        /* Validators are null until we add that code */
        if (elementDecl.type == XMLElementDecl.TYPE_CHILDREN || elementDecl.type == XMLElementDecl.TYPE_MIXED) {
            elementDecl.contentModelValidator = getElementContentModelValidator(elementDeclIndex);
        }

        elementDecl.simpleType.datatypeValidator = null;
        elementDecl.simpleType.defaultType       = -1;
        elementDecl.simpleType.defaultValue      = null;

        return true;

    } // getElementDecl(int,XMLElementDecl):boolean

    QName getElementDeclName(int elementDeclIndex) {
        if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
            return null;
        }
        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex &  CHUNK_MASK;
        return fElementDeclName[chunk][index];
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
    } // getNextAttributeDeclIndex

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
                                           fAttributeDeclNonNormalizedDefaultValue[chunk][index],
                                           fAttributeDeclDatatypeValidator[chunk][index]);
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

    /**
     * getEntityDeclIndex
     *
     * @param entityDeclName
     *
     * @return the index of the EntityDecl
     */
    public int getEntityDeclIndex(String entityDeclName) {
        if (entityDeclName == null || fEntityIndexMap.get(entityDeclName) == null) {
            return -1;
        }

        return fEntityIndexMap.get(entityDeclName);
    } // getEntityDeclIndex

    /**
     * getEntityDecl
     *
     * @param entityDeclIndex
     * @param entityDecl
     *
     * @return true if getEntityDecl was able to fill entityDecl with the contents of the entity
     * with index entityDeclIndex
     */
    public boolean getEntityDecl(int entityDeclIndex, XMLEntityDecl entityDecl) {
        if (entityDeclIndex < 0 || entityDeclIndex >= fEntityCount) {
            return false;
        }
        int chunk = entityDeclIndex >> CHUNK_SHIFT;
        int index = entityDeclIndex & CHUNK_MASK;

        entityDecl.setValues(fEntityName[chunk][index],
                             fEntityPublicId[chunk][index],
                             fEntitySystemId[chunk][index],
                             fEntityBaseSystemId[chunk][index],
                             fEntityNotation[chunk][index],
                             fEntityValue[chunk][index],
                             fEntityIsPE[chunk][index] == 0 ? false : true ,
                             fEntityInExternal[chunk][index] == 0 ? false : true );

        return true;
    } // getEntityDecl

    /**
     * getNotationDeclIndex
     *
     * @param notationDeclName
     *
     * @return the index if found a notation with the name, otherwise -1.
     */
    public int getNotationDeclIndex(String notationDeclName) {
        if (notationDeclName == null || fNotationIndexMap.get(notationDeclName) == null) {
            return -1;
        }

        return fNotationIndexMap.get(notationDeclName);
    } // getNotationDeclIndex

    /**
     * getNotationDecl
     *
     * @param notationDeclIndex
     * @param notationDecl
     *
     * @return return true of getNotationDecl can fill notationDecl with information about
     * the notation at notationDeclIndex.
     */
    public boolean getNotationDecl(int notationDeclIndex, XMLNotationDecl notationDecl) {
        if (notationDeclIndex < 0 || notationDeclIndex >= fNotationCount) {
            return false;
        }
        int chunk = notationDeclIndex >> CHUNK_SHIFT;
        int index = notationDeclIndex & CHUNK_MASK;

        notationDecl.setValues(fNotationName[chunk][index],
                               fNotationPublicId[chunk][index],
                               fNotationSystemId[chunk][index],
                               fNotationBaseSystemId[chunk][index]);

        return true;

    } // getNotationDecl

    /**
     * getContentSpec
     *
     * @param contentSpecIndex
     * @param contentSpec
     *
     * @return true if find the requested contentSpec node, false otherwise
     */
    public boolean getContentSpec(int contentSpecIndex, XMLContentSpec contentSpec) {
        if (contentSpecIndex < 0 || contentSpecIndex >= fContentSpecCount )
            return false;

        int chunk = contentSpecIndex >> CHUNK_SHIFT;
        int index = contentSpecIndex & CHUNK_MASK;

        contentSpec.type       = fContentSpecType[chunk][index];
        contentSpec.value      = fContentSpecValue[chunk][index];
        contentSpec.otherValue = fContentSpecOtherValue[chunk][index];
        return true;
    }

    /**
     * Returns the index to the content spec for the given element
     * declaration, or <code>-1</code> if the element declaration
     * index was invalid.
     */
    public int getContentSpecIndex(int elementDeclIndex) {
        if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
            return -1;
        }
        final int chunk = elementDeclIndex >> CHUNK_SHIFT;
        final int index = elementDeclIndex & CHUNK_MASK;
        return fElementDeclContentSpecIndex[chunk][index];
    }

    /**
     * getContentSpecAsString
     *
     * @param elementDeclIndex
     *
     * @return String
     */
    public String getContentSpecAsString(int elementDeclIndex){

        if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
            return null;
        }

        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex &  CHUNK_MASK;

        int contentSpecIndex = fElementDeclContentSpecIndex[chunk][index];

        // lookup content spec node
        XMLContentSpec contentSpec = new XMLContentSpec();

        if (getContentSpec(contentSpecIndex, contentSpec)) {

            // build string
            StringBuffer str = new StringBuffer();
            int    parentContentSpecType = contentSpec.type & 0x0f;
            int    nextContentSpec;
            switch (parentContentSpecType) {
                case XMLContentSpec.CONTENTSPECNODE_LEAF: {
                    str.append('(');
                    if (contentSpec.value == null && contentSpec.otherValue == null) {
                        str.append("#PCDATA");
                    }
                    else {
                        str.append(contentSpec.value);
                    }
                    str.append(')');
                    break;
                }
                case XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE: {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    nextContentSpec = contentSpec.type;

                    if (nextContentSpec == XMLContentSpec.CONTENTSPECNODE_LEAF) {
                        str.append('(');
                        str.append(contentSpec.value);
                        str.append(')');
                    } else if( nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE  ||
                        nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE  ||
                        nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE ) {
                        str.append('(' );
                        appendContentSpec(contentSpec, str,
                                          true, parentContentSpecType );
                        str.append(')');
                    } else {
                        appendContentSpec(contentSpec, str,
                                          true, parentContentSpecType );
                    }
                    str.append('?');
                    break;
                }
                case XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE: {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    nextContentSpec = contentSpec.type;

                    if ( nextContentSpec == XMLContentSpec.CONTENTSPECNODE_LEAF) {
                        str.append('(');
                        if (contentSpec.value == null && contentSpec.otherValue == null) {
                            str.append("#PCDATA");
                        }
                        else if (contentSpec.otherValue != null) {
                            str.append("##any:uri=").append(contentSpec.otherValue);
                        }
                        else if (contentSpec.value == null) {
                            str.append("##any");
                        }
                        else {
                            appendContentSpec(contentSpec, str,
                                              true, parentContentSpecType );
                        }
                        str.append(')');
                    } else if( nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE  ||
                        nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE  ||
                        nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE ) {
                        str.append('(' );
                        appendContentSpec(contentSpec, str,
                                          true, parentContentSpecType );
                        str.append(')');
                    } else {
                        appendContentSpec(contentSpec, str,
                                          true, parentContentSpecType );
                    }
                    str.append('*');
                    break;
                }
                case XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE: {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    nextContentSpec = contentSpec.type;

                    if ( nextContentSpec == XMLContentSpec.CONTENTSPECNODE_LEAF) {
                        str.append('(');
                        if (contentSpec.value == null && contentSpec.otherValue == null) {
                            str.append("#PCDATA");
                        }
                        else if (contentSpec.otherValue != null) {
                            str.append("##any:uri=").append(contentSpec.otherValue);
                        }
                        else if (contentSpec.value == null) {
                            str.append("##any");
                        }
                        else {
                            str.append(contentSpec.value);
                        }
                        str.append(')');
                    } else if( nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE  ||
                        nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE  ||
                        nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE ) {
                        str.append('(' );
                        appendContentSpec(contentSpec, str,
                                          true, parentContentSpecType );
                        str.append(')');
                    } else {
                        appendContentSpec(contentSpec, str,
                                          true, parentContentSpecType);
                    }
                    str.append('+');
                    break;
                }
                case XMLContentSpec.CONTENTSPECNODE_CHOICE:
                case XMLContentSpec.CONTENTSPECNODE_SEQ: {
                    appendContentSpec(contentSpec, str,
                                      true, parentContentSpecType );
                    break;
                }
                case XMLContentSpec.CONTENTSPECNODE_ANY: {
                    str.append("##any");
                    if (contentSpec.otherValue != null) {
                        str.append(":uri=");
                        str.append(contentSpec.otherValue);
                    }
                    break;
                }
                case XMLContentSpec.CONTENTSPECNODE_ANY_OTHER: {
                    str.append("##other:uri=");
                    str.append(contentSpec.otherValue);
                    break;
                }
                case XMLContentSpec.CONTENTSPECNODE_ANY_LOCAL: {
                    str.append("##local");
                    break;
                }
                default: {
                    str.append("???");
                }

            } // switch type

            // return string
            return str.toString();
        }

        // not found
        return null;

    } // getContentSpecAsString(int):String

    // debugging

    public void printElements(  ) {
        int elementDeclIndex = 0;
        XMLElementDecl elementDecl = new XMLElementDecl();
        while (getElementDecl(elementDeclIndex++, elementDecl)) {

            System.out.println("element decl: "+elementDecl.name+
                               ", "+ elementDecl.name.rawname  );

            //                   ", "+ elementDecl.contentModelValidator.toString());
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

    //
    // Protected methods
    //

    /**
     * Adds the content spec to the given element declaration.
     */
    protected void addContentSpecToElement(XMLElementDecl elementDecl) {
        if ((fDepth == 0 || (fDepth == 1 && elementDecl.type == XMLElementDecl.TYPE_MIXED)) &&
                fNodeIndexStack != null) {
            if (elementDecl.type == XMLElementDecl.TYPE_MIXED) {
                int pcdata = addUniqueLeafNode(null);
                if (fNodeIndexStack[0] == -1) {
                    fNodeIndexStack[0] = pcdata;
                }
                else {
                    fNodeIndexStack[0] = addContentSpecNode(XMLContentSpec.CONTENTSPECNODE_CHOICE,
                            pcdata, fNodeIndexStack[0]);
                }
            }
            setContentSpecIndex(fCurrentElementIndex, fNodeIndexStack[fDepth]);
        }
    }

    /**
     * getElementContentModelValidator
     *
     * @param elementDeclIndex
     *
     * @return its ContentModelValidator if any.
     */
    protected ContentModelValidator getElementContentModelValidator(int elementDeclIndex) {

        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex & CHUNK_MASK;

        ContentModelValidator contentModel    =  fElementDeclContentModelValidator[chunk][index];

        // If we have one, just return that. Otherwise, gotta create one
        if (contentModel != null) {
            return contentModel;
        }

        int contentType = fElementDeclType[chunk][index];
        if (contentType == XMLElementDecl.TYPE_SIMPLE) {
            return null;
        }

        // Get the type of content this element has
        int contentSpecIndex = fElementDeclContentSpecIndex[chunk][index];

        /***
        if ( contentSpecIndex == -1 )
            return null;
        /***/

        XMLContentSpec  contentSpec = new XMLContentSpec();
        getContentSpec( contentSpecIndex, contentSpec );

        // And create the content model according to the spec type
        if ( contentType == XMLElementDecl.TYPE_MIXED ) {
            //
            //  Just create a mixel content model object. This type of
            //  content model is optimized for mixed content validation.
            //
            ChildrenList children = new ChildrenList();
            contentSpecTree(contentSpecIndex, contentSpec, children);
            contentModel = new MixedContentModel(children.qname,
                                                 children.type,
                                                 0, children.length,
                                                 false);
        } else if (contentType == XMLElementDecl.TYPE_CHILDREN) {
            //  This method will create an optimal model for the complexity
            //  of the element's defined model. If its simple, it will create
            //  a SimpleContentModel object. If its a simple list, it will
            //  create a SimpleListContentModel object. If its complex, it
            //  will create a DFAContentModel object.
            //
            contentModel = createChildModel(contentSpecIndex);
        } else {
            throw new RuntimeException("Unknown content type for a element decl "
                                     + "in getElementContentModelValidator() in AbstractDTDGrammar class");
        }

        // Add the new model to the content model for this element
        fElementDeclContentModelValidator[chunk][index] = contentModel;

        return contentModel;

    } // getElementContentModelValidator(int):ContentModelValidator

   protected int createElementDecl() {
      int chunk = fElementDeclCount >> CHUNK_SHIFT;
      int index = fElementDeclCount & CHUNK_MASK;
      ensureElementDeclCapacity(chunk);
      fElementDeclName[chunk][index]                    = new QName();
      fElementDeclType[chunk][index]                    = -1;
      fElementDeclContentModelValidator[chunk][index]   = null;
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

      fElementDeclName[chunk][index].setValues(elementDecl.name);
      fElementDeclType[chunk][index]                  = elementDecl.type;

      fElementDeclContentModelValidator[chunk][index] = elementDecl.contentModelValidator;

      if (elementDecl.simpleType.list  == true ) {
         fElementDeclType[chunk][index] |= LIST_FLAG;
      }

      fElementIndexMap.put(elementDecl.name.rawname, elementDeclIndex);
   }




   protected void putElementNameMapping(QName name, int scope,
                                        int elementDeclIndex) {
   }

   protected void setFirstAttributeDeclIndex(int elementDeclIndex, int newFirstAttrIndex){

      if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
         return;
      }

      int chunk = elementDeclIndex >> CHUNK_SHIFT;
      int index = elementDeclIndex &  CHUNK_MASK;

      fElementDeclFirstAttributeDeclIndex[chunk][index] = newFirstAttrIndex;
   }

   protected void setContentSpecIndex(int elementDeclIndex, int contentSpecIndex){

      if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
         return;
      }

      int chunk = elementDeclIndex >> CHUNK_SHIFT;
      int index = elementDeclIndex &  CHUNK_MASK;

      fElementDeclContentSpecIndex[chunk][index] = contentSpecIndex;
   }


   protected int createAttributeDecl() {
      int chunk = fAttributeDeclCount >> CHUNK_SHIFT;
      int index = fAttributeDeclCount & CHUNK_MASK;

      ensureAttributeDeclCapacity(chunk);
      fAttributeDeclName[chunk][index]                    = new QName();
      fAttributeDeclType[chunk][index]                    = -1;
      fAttributeDeclDatatypeValidator[chunk][index]       = null;
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
      fAttributeDeclDatatypeValidator[attrChunk][attrIndex] =  attributeDecl.simpleType.datatypeValidator;

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

   protected int createContentSpec() {
      int chunk = fContentSpecCount >> CHUNK_SHIFT;
      int index = fContentSpecCount & CHUNK_MASK;

      ensureContentSpecCapacity(chunk);
      fContentSpecType[chunk][index]       = -1;
      fContentSpecValue[chunk][index]      = null;
      fContentSpecOtherValue[chunk][index] = null;

      return fContentSpecCount++;
   }

   protected void setContentSpec(int contentSpecIndex, XMLContentSpec contentSpec) {
      int   chunk = contentSpecIndex >> CHUNK_SHIFT;
      int   index = contentSpecIndex & CHUNK_MASK;

      fContentSpecType[chunk][index]       = contentSpec.type;
      fContentSpecValue[chunk][index]      = contentSpec.value;
      fContentSpecOtherValue[chunk][index] = contentSpec.otherValue;
   }


   protected int createEntityDecl() {
       int chunk = fEntityCount >> CHUNK_SHIFT;
       int index = fEntityCount & CHUNK_MASK;

      ensureEntityDeclCapacity(chunk);
      fEntityIsPE[chunk][index] = 0;
      fEntityInExternal[chunk][index] = 0;

      return fEntityCount++;
   }

   protected void setEntityDecl(int entityDeclIndex, XMLEntityDecl entityDecl) {
       int chunk = entityDeclIndex >> CHUNK_SHIFT;
       int index = entityDeclIndex & CHUNK_MASK;

       fEntityName[chunk][index] = entityDecl.name;
       fEntityValue[chunk][index] = entityDecl.value;
       fEntityPublicId[chunk][index] = entityDecl.publicId;
       fEntitySystemId[chunk][index] = entityDecl.systemId;
       fEntityBaseSystemId[chunk][index] = entityDecl.baseSystemId;
       fEntityNotation[chunk][index] = entityDecl.notation;
       fEntityIsPE[chunk][index] = entityDecl.isPE ? (byte)1 : (byte)0;
       fEntityInExternal[chunk][index] = entityDecl.inExternal ? (byte)1 : (byte)0;

       fEntityIndexMap.put(entityDecl.name, entityDeclIndex);
   }

   protected int createNotationDecl() {
       int chunk = fNotationCount >> CHUNK_SHIFT;
       ensureNotationDeclCapacity(chunk);
       return fNotationCount++;
   }

   protected void setNotationDecl(int notationDeclIndex, XMLNotationDecl notationDecl) {
       int chunk = notationDeclIndex >> CHUNK_SHIFT;
       int index = notationDeclIndex & CHUNK_MASK;

       fNotationName[chunk][index] = notationDecl.name;
       fNotationPublicId[chunk][index] = notationDecl.publicId;
       fNotationSystemId[chunk][index] = notationDecl.systemId;
       fNotationBaseSystemId[chunk][index] = notationDecl.baseSystemId;

       fNotationIndexMap.put(notationDecl.name, notationDeclIndex);
   }

    /**
     * Create an XMLContentSpec for a single non-leaf
     *
     * @param nodeType the type of XMLContentSpec to create - from XMLContentSpec.CONTENTSPECNODE_*
     * @param nodeValue handle to an XMLContentSpec
     * @return handle to the newly create XMLContentSpec
     */
    protected int addContentSpecNode(short nodeType, String nodeValue) {

        // create content spec node
        int contentSpecIndex = createContentSpec();

        // set content spec node values
        fContentSpec.setValues(nodeType, nodeValue, null);
        setContentSpec(contentSpecIndex, fContentSpec);

        // return index
        return contentSpecIndex;

    } // addContentSpecNode(short,String):int

    /**
     * create an XMLContentSpec for a leaf
     *
     * @param   elementName  the name (Element) for the node
     * @return handle to the newly create XMLContentSpec
     */
    protected int addUniqueLeafNode(String elementName) {

        // create content spec node
        int contentSpecIndex = createContentSpec();

        // set content spec node values
        fContentSpec.setValues( XMLContentSpec.CONTENTSPECNODE_LEAF,
                                elementName, null);
        setContentSpec(contentSpecIndex, fContentSpec);

        // return index
        return contentSpecIndex;

    } // addUniqueLeafNode(String):int

    /**
     * Create an XMLContentSpec for a two child leaf
     *
     * @param nodeType the type of XMLContentSpec to create - from XMLContentSpec.CONTENTSPECNODE_*
     * @param leftNodeIndex handle to an XMLContentSpec
     * @param rightNodeIndex handle to an XMLContentSpec
     * @return handle to the newly create XMLContentSpec
     */
    protected int addContentSpecNode(short nodeType,
                                     int leftNodeIndex, int rightNodeIndex) {

        // create content spec node
        int contentSpecIndex = createContentSpec();

        // set content spec node values
        int[] leftIntArray  = new int[1];
        int[] rightIntArray = new int[1];

        leftIntArray[0]      = leftNodeIndex;
        rightIntArray[0]    = rightNodeIndex;
        fContentSpec.setValues(nodeType, leftIntArray, rightIntArray);
        setContentSpec(contentSpecIndex, fContentSpec);

        // return index
        return contentSpecIndex;

    } // addContentSpecNode(short,int,int):int

    /** Initialize content model stack. */
    protected void initializeContentModelStack() {

        if (fOpStack == null) {
            fOpStack = new short[8];
            fNodeIndexStack = new int[8];
            fPrevNodeIndexStack = new int[8];
        } else if (fDepth == fOpStack.length) {
            short[] newStack = new short[fDepth * 2];
            System.arraycopy(fOpStack, 0, newStack, 0, fDepth);
            fOpStack = newStack;
            int[]   newIntStack = new int[fDepth * 2];
            System.arraycopy(fNodeIndexStack, 0, newIntStack, 0, fDepth);
            fNodeIndexStack = newIntStack;
            newIntStack = new int[fDepth * 2];
            System.arraycopy(fPrevNodeIndexStack, 0, newIntStack, 0, fDepth);
            fPrevNodeIndexStack = newIntStack;
        }
        fOpStack[fDepth] = -1;
        fNodeIndexStack[fDepth] = -1;
        fPrevNodeIndexStack[fDepth] = -1;

    } // initializeContentModelStack()

    boolean isImmutable() {
        return fIsImmutable;
    }

    //
    // Private methods
    //

    private void appendContentSpec(XMLContentSpec contentSpec,
                                   StringBuffer str, boolean parens,
                                   int parentContentSpecType ) {

        int thisContentSpec = contentSpec.type & 0x0f;
        switch (thisContentSpec) {
            case XMLContentSpec.CONTENTSPECNODE_LEAF: {
                if (contentSpec.value == null && contentSpec.otherValue == null) {
                    str.append("#PCDATA");
                }
                else if (contentSpec.value == null && contentSpec.otherValue != null) {
                    str.append("##any:uri=").append(contentSpec.otherValue);
                }
                else if (contentSpec.value == null) {
                    str.append("##any");
                }
                else {
                    str.append(contentSpec.value);
                }
                break;
            }
            case XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE: {
                if (parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE  ||
                    parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE ||
                    parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE ) {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    str.append('(');
                    appendContentSpec(contentSpec, str, true, thisContentSpec );
                    str.append(')');
                }
                else {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    appendContentSpec( contentSpec, str, true, thisContentSpec );
                }
                str.append('?');
                break;
            }
            case XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE: {
                if (parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE ||
                    parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE ||
                    parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE ) {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    str.append('(');
                    appendContentSpec(contentSpec, str, true, thisContentSpec);
                    str.append(')' );
                }
                else {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    appendContentSpec(contentSpec, str, true, thisContentSpec);
                }
                str.append('*');
                break;
            }
            case XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE: {
                if (parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE   ||
                    parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE  ||
                    parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE ) {

                    str.append('(');
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    appendContentSpec(contentSpec, str, true, thisContentSpec);
                    str.append(')' );
                }
                else {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    appendContentSpec(contentSpec, str, true, thisContentSpec);
                }
                str.append('+');
                break;
            }
            case XMLContentSpec.CONTENTSPECNODE_CHOICE:
            case XMLContentSpec.CONTENTSPECNODE_SEQ: {
                if (parens) {
                    str.append('(');
                }
                int type = contentSpec.type;
                int otherValue = ((int[])contentSpec.otherValue)[0];
                getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                appendContentSpec(contentSpec, str, contentSpec.type != type, thisContentSpec);
                if (type == XMLContentSpec.CONTENTSPECNODE_CHOICE) {
                    str.append('|');
                }
                else {
                    str.append(',');
                }
                getContentSpec(otherValue, contentSpec);
                appendContentSpec(contentSpec, str, true, thisContentSpec);
                if (parens) {
                    str.append(')');
                }
                break;
            }
            case XMLContentSpec.CONTENTSPECNODE_ANY: {
                str.append("##any");
                if (contentSpec.otherValue != null) {
                    str.append(":uri=");
                    str.append(contentSpec.otherValue);
                }
                break;
            }
            case XMLContentSpec.CONTENTSPECNODE_ANY_OTHER: {
                str.append("##other:uri=");
                str.append(contentSpec.otherValue);
                break;
            }
            case XMLContentSpec.CONTENTSPECNODE_ANY_LOCAL: {
                str.append("##local");
                break;
            }
            default: {
                str.append("???");
                break;
            }

        } // switch type

    } // appendContentSpec(XMLContentSpec.Provider,StringPool,XMLContentSpec,StringBuffer,boolean)

    // debugging

    private void printAttribute(int attributeDeclIndex) {

        XMLAttributeDecl attributeDecl = new XMLAttributeDecl();
        if (getAttributeDecl(attributeDeclIndex, attributeDecl)) {
            System.out.print(" { ");
            System.out.print(attributeDecl.name.localpart);
            System.out.print(" }");
        }

    } // printAttribute(int)

    // content models

    /**
     * When the element has a 'CHILDREN' model, this method is called to
     * create the content model object. It looks for some special case simple
     * models and creates SimpleContentModel objects for those. For the rest
     * it creates the standard DFA style model.
     */
    private synchronized ContentModelValidator createChildModel(int contentSpecIndex) {

        //
        //  Get the content spec node for the element we are working on.
        //  This will tell us what kind of node it is, which tells us what
        //  kind of model we will try to create.
        //
        XMLContentSpec contentSpec = new XMLContentSpec();
        getContentSpec(contentSpecIndex, contentSpec);

        if ((contentSpec.type & 0x0f ) == XMLContentSpec.CONTENTSPECNODE_ANY ||
            (contentSpec.type & 0x0f ) == XMLContentSpec.CONTENTSPECNODE_ANY_OTHER ||
            (contentSpec.type & 0x0f ) == XMLContentSpec.CONTENTSPECNODE_ANY_LOCAL) {
            // let fall through to build a DFAContentModel
        }

        else if (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_LEAF) {
            //
            //  Check that the left value is not -1, since any content model
            //  with PCDATA should be MIXED, so we should not have gotten here.
            //
            if (contentSpec.value == null && contentSpec.otherValue == null)
                throw new RuntimeException("ImplementationMessages.VAL_NPCD");

            //
            //  Its a single leaf, so its an 'a' type of content model, i.e.
            //  just one instance of one element. That one is definitely a
            //  simple content model.
            //

            fQName.setValues(null, (String)contentSpec.value,
                              (String)contentSpec.value, (String)contentSpec.otherValue);
            return new SimpleContentModel(contentSpec.type, fQName, null);
        } else if ((contentSpec.type == XMLContentSpec.CONTENTSPECNODE_CHOICE)
                    ||  (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_SEQ)) {
            //
            //  Lets see if both of the children are leafs. If so, then it
            //  it has to be a simple content model
            //
            XMLContentSpec contentSpecLeft  = new XMLContentSpec();
            XMLContentSpec contentSpecRight = new XMLContentSpec();

            getContentSpec( ((int[])contentSpec.value)[0], contentSpecLeft);
            getContentSpec( ((int[])contentSpec.otherValue)[0], contentSpecRight);

            if ((contentSpecLeft.type == XMLContentSpec.CONTENTSPECNODE_LEAF)
                 &&  (contentSpecRight.type == XMLContentSpec.CONTENTSPECNODE_LEAF)) {
                //
                //  Its a simple choice or sequence, so we can do a simple
                //  content model for it.
                //
                fQName.setValues(null, (String)contentSpecLeft.value,
                                  (String)contentSpecLeft.value, (String)contentSpecLeft.otherValue);
                fQName2.setValues(null, (String)contentSpecRight.value,
                                  (String)contentSpecRight.value, (String)contentSpecRight.otherValue);
                return new SimpleContentModel(contentSpec.type, fQName, fQName2);
            }
        } else if ((contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE)
                    ||  (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE)
                    ||  (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE)) {
            //
            //  Its a repetition, so see if its one child is a leaf. If so
            //  its a repetition of a single element, so we can do a simple
            //  content model for that.
            //
            XMLContentSpec contentSpecLeft = new XMLContentSpec();
            getContentSpec(((int[])contentSpec.value)[0], contentSpecLeft);

            if (contentSpecLeft.type == XMLContentSpec.CONTENTSPECNODE_LEAF) {
                //
                //  It is, so we can create a simple content model here that
                //  will check for this repetition. We pass -1 for the unused
                //  right node.
                //
                fQName.setValues(null, (String)contentSpecLeft.value,
                                  (String)contentSpecLeft.value, (String)contentSpecLeft.otherValue);
                return new SimpleContentModel(contentSpec.type, fQName, null);
            }
        } else {
            throw new RuntimeException("ImplementationMessages.VAL_CST");
        }

        //
        //  Its not a simple content model, so here we have to create a DFA
        //  for this element. So we create a DFAContentModel object. He
        //  encapsulates all of the work to create the DFA.
        //

        fLeafCount = 0;
        //int leafCount = countLeaves(contentSpecIndex);
        fLeafCount = 0;
        CMNode cmn    = buildSyntaxTree(contentSpecIndex, contentSpec);

        // REVISIT: has to be fLeafCount because we convert x+ to x,x*, one more leaf
        return new DFAContentModel(  cmn, fLeafCount, false);

    } // createChildModel(int):ContentModelValidator

    private final CMNode buildSyntaxTree(int startNode,
                                         XMLContentSpec contentSpec) {

        // We will build a node at this level for the new tree
        CMNode nodeRet = null;
        getContentSpec(startNode, contentSpec);
        if ((contentSpec.type & 0x0f) == XMLContentSpec.CONTENTSPECNODE_ANY) {
            //nodeRet = new CMAny(contentSpec.type, -1, fLeafCount++);
            nodeRet = new CMAny(contentSpec.type, (String)contentSpec.otherValue, fLeafCount++);
        }
        else if ((contentSpec.type & 0x0f) == XMLContentSpec.CONTENTSPECNODE_ANY_OTHER) {
            nodeRet = new CMAny(contentSpec.type, (String)contentSpec.otherValue, fLeafCount++);
        }
        else if ((contentSpec.type & 0x0f) == XMLContentSpec.CONTENTSPECNODE_ANY_LOCAL) {
            nodeRet = new CMAny(contentSpec.type, null, fLeafCount++);
        }
        //
        //  If this node is a leaf, then its an easy one. We just add it
        //  to the tree.
        //
        else if (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_LEAF) {
            //
            //  Create a new leaf node, and pass it the current leaf count,
            //  which is its DFA state position. Bump the leaf count after
            //  storing it. This makes the positions zero based since we
            //  store first and then increment.
            //
            fQName.setValues(null, (String)contentSpec.value,
                              (String)contentSpec.value, (String)contentSpec.otherValue);
            nodeRet = new CMLeaf(fQName, fLeafCount++);
        }
        else {
            //
            //  Its not a leaf, so we have to recurse its left and maybe right
            //  nodes. Save both values before we recurse and trash the node.
            final int leftNode = ((int[])contentSpec.value)[0];
            final int rightNode = ((int[])contentSpec.otherValue)[0];

            if ((contentSpec.type == XMLContentSpec.CONTENTSPECNODE_CHOICE)
                ||  (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_SEQ)) {
                //
                //  Recurse on both children, and return a binary op node
                //  with the two created sub nodes as its children. The node
                //  type is the same type as the source.
                //

                nodeRet = new CMBinOp( contentSpec.type, buildSyntaxTree(leftNode, contentSpec)
                                       , buildSyntaxTree(rightNode, contentSpec));
            }
            else if (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE) {
                nodeRet = new CMUniOp( contentSpec.type, buildSyntaxTree(leftNode, contentSpec));
            }
            else if (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE
                  || contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE
                  || contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE) {
                nodeRet = new CMUniOp(contentSpec.type, buildSyntaxTree(leftNode, contentSpec));
            }
            else {
                throw new RuntimeException("ImplementationMessages.VAL_CST");
            }
        }
        // And return our new node for this level
        return nodeRet;
    }

    /**
     * Build a vector of valid QNames from Content Spec
     * table.
     *
     * @param contentSpecIndex
     *               Content Spec index
     * @param vectorQName
     *               Array of QName
     * @exception RuntimeException
     */
    private void contentSpecTree(int contentSpecIndex,
                                 XMLContentSpec contentSpec,
                                 ChildrenList children) {

        // Handle any and leaf nodes
        getContentSpec( contentSpecIndex, contentSpec);
        if ( contentSpec.type == XMLContentSpec.CONTENTSPECNODE_LEAF ||
            (contentSpec.type & 0x0f) == XMLContentSpec.CONTENTSPECNODE_ANY ||
            (contentSpec.type & 0x0f) == XMLContentSpec.CONTENTSPECNODE_ANY_LOCAL ||
            (contentSpec.type & 0x0f) == XMLContentSpec.CONTENTSPECNODE_ANY_OTHER) {

            // resize arrays, if needed
            if (children.length == children.qname.length) {
                QName[] newQName = new QName[children.length * 2];
                System.arraycopy(children.qname, 0, newQName, 0, children.length);
                children.qname = newQName;
                int[] newType = new int[children.length * 2];
                System.arraycopy(children.type, 0, newType, 0, children.length);
                children.type = newType;
            }

            // save values and return length
            children.qname[children.length] = new QName(null, (String)contentSpec.value,
                                                     (String) contentSpec.value,
                                                     (String) contentSpec.otherValue);
            children.type[children.length] = contentSpec.type;
            children.length++;
            return;
        }

        //
        //  Its not a leaf, so we have to recurse its left and maybe right
        //  nodes. Save both values before we recurse and trash the node.
        //
        final int leftNode = contentSpec.value != null
                           ? ((int[])(contentSpec.value))[0] : -1;
        int rightNode = -1 ;
        if (contentSpec.otherValue != null )
            rightNode = ((int[])(contentSpec.otherValue))[0];
        else
            return;

        if (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_CHOICE ||
            contentSpec.type == XMLContentSpec.CONTENTSPECNODE_SEQ) {
            contentSpecTree(leftNode, contentSpec, children);
            contentSpecTree(rightNode, contentSpec, children);
            return;
        }

        if (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE ||
            contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE ||
            contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE) {
            contentSpecTree(leftNode, contentSpec, children);
            return;
        }

        // error
        throw new RuntimeException("Invalid content spec type seen in contentSpecTree() method of AbstractDTDGrammar class : "+contentSpec.type);

    } // contentSpecTree(int,XMLContentSpec,ChildrenList)

    // ensure capacity

    private void ensureElementDeclCapacity(int chunk) {
        if (chunk >= fElementDeclName.length) {
            fElementDeclIsExternal = resize(fElementDeclIsExternal,
                                     fElementDeclIsExternal.length * 2);

            fElementDeclName = resize(fElementDeclName, fElementDeclName.length * 2);
            fElementDeclType = resize(fElementDeclType, fElementDeclType.length * 2);
            fElementDeclContentModelValidator = resize(fElementDeclContentModelValidator, fElementDeclContentModelValidator.length * 2);
            fElementDeclContentSpecIndex = resize(fElementDeclContentSpecIndex,fElementDeclContentSpecIndex.length * 2);
            fElementDeclFirstAttributeDeclIndex = resize(fElementDeclFirstAttributeDeclIndex, fElementDeclFirstAttributeDeclIndex.length * 2);
            fElementDeclLastAttributeDeclIndex = resize(fElementDeclLastAttributeDeclIndex, fElementDeclLastAttributeDeclIndex.length * 2);
        }
        else if (fElementDeclName[chunk] != null) {
            return;
        }

        fElementDeclIsExternal[chunk] = new int[CHUNK_SIZE];
        fElementDeclName[chunk] = new QName[CHUNK_SIZE];
        fElementDeclType[chunk] = new short[CHUNK_SIZE];
        fElementDeclContentModelValidator[chunk] = new ContentModelValidator[CHUNK_SIZE];
        fElementDeclContentSpecIndex[chunk] = new int[CHUNK_SIZE];
        fElementDeclFirstAttributeDeclIndex[chunk] = new int[CHUNK_SIZE];
        fElementDeclLastAttributeDeclIndex[chunk] = new int[CHUNK_SIZE];
        return;
    }

    private void ensureAttributeDeclCapacity(int chunk) {

        if (chunk >= fAttributeDeclName.length) {
            fAttributeDeclIsExternal = resize(fAttributeDeclIsExternal,
                                       fAttributeDeclIsExternal.length * 2);
            fAttributeDeclName = resize(fAttributeDeclName, fAttributeDeclName.length * 2);
            fAttributeDeclType = resize(fAttributeDeclType, fAttributeDeclType.length * 2);
            fAttributeDeclEnumeration = resize(fAttributeDeclEnumeration, fAttributeDeclEnumeration.length * 2);
            fAttributeDeclDefaultType = resize(fAttributeDeclDefaultType, fAttributeDeclDefaultType.length * 2);
            fAttributeDeclDatatypeValidator = resize(fAttributeDeclDatatypeValidator, fAttributeDeclDatatypeValidator.length * 2);
            fAttributeDeclDefaultValue = resize(fAttributeDeclDefaultValue, fAttributeDeclDefaultValue.length * 2);
            fAttributeDeclNonNormalizedDefaultValue = resize(fAttributeDeclNonNormalizedDefaultValue, fAttributeDeclNonNormalizedDefaultValue.length * 2);
            fAttributeDeclNextAttributeDeclIndex = resize(fAttributeDeclNextAttributeDeclIndex, fAttributeDeclNextAttributeDeclIndex.length * 2);
        }
        else if (fAttributeDeclName[chunk] != null) {
            return;
        }

        fAttributeDeclIsExternal[chunk] = new int[CHUNK_SIZE];
        fAttributeDeclName[chunk] = new QName[CHUNK_SIZE];
        fAttributeDeclType[chunk] = new short[CHUNK_SIZE];
        fAttributeDeclEnumeration[chunk] = new String[CHUNK_SIZE][];
        fAttributeDeclDefaultType[chunk] = new short[CHUNK_SIZE];
        fAttributeDeclDatatypeValidator[chunk] = new DatatypeValidator[CHUNK_SIZE];
        fAttributeDeclDefaultValue[chunk] = new String[CHUNK_SIZE];
        fAttributeDeclNonNormalizedDefaultValue[chunk] = new String[CHUNK_SIZE];
        fAttributeDeclNextAttributeDeclIndex[chunk] = new int[CHUNK_SIZE];
        return;
    }

    private void ensureEntityDeclCapacity(int chunk) {
        if (chunk >= fEntityName.length) {
            fEntityName = resize(fEntityName, fEntityName.length * 2);
            fEntityValue = resize(fEntityValue, fEntityValue.length * 2);
            fEntityPublicId = resize(fEntityPublicId, fEntityPublicId.length * 2);
            fEntitySystemId = resize(fEntitySystemId, fEntitySystemId.length * 2);
            fEntityBaseSystemId = resize(fEntityBaseSystemId, fEntityBaseSystemId.length * 2);
            fEntityNotation = resize(fEntityNotation, fEntityNotation.length * 2);
            fEntityIsPE = resize(fEntityIsPE, fEntityIsPE.length * 2);
            fEntityInExternal = resize(fEntityInExternal, fEntityInExternal.length * 2);
        }
        else if (fEntityName[chunk] != null) {
            return;
        }

        fEntityName[chunk] = new String[CHUNK_SIZE];
        fEntityValue[chunk] = new String[CHUNK_SIZE];
        fEntityPublicId[chunk] = new String[CHUNK_SIZE];
        fEntitySystemId[chunk] = new String[CHUNK_SIZE];
        fEntityBaseSystemId[chunk] = new String[CHUNK_SIZE];
        fEntityNotation[chunk] = new String[CHUNK_SIZE];
        fEntityIsPE[chunk] = new byte[CHUNK_SIZE];
        fEntityInExternal[chunk] = new byte[CHUNK_SIZE];
        return;
    }

    private void ensureNotationDeclCapacity(int chunk) {
        if (chunk >= fNotationName.length) {
            fNotationName = resize(fNotationName, fNotationName.length * 2);
            fNotationPublicId = resize(fNotationPublicId, fNotationPublicId.length * 2);
            fNotationSystemId = resize(fNotationSystemId, fNotationSystemId.length * 2);
            fNotationBaseSystemId = resize(fNotationBaseSystemId, fNotationBaseSystemId.length * 2);
        }
        else if (fNotationName[chunk] != null) {
            return;
        }

        fNotationName[chunk] = new String[CHUNK_SIZE];
        fNotationPublicId[chunk] = new String[CHUNK_SIZE];
        fNotationSystemId[chunk] = new String[CHUNK_SIZE];
        fNotationBaseSystemId[chunk] = new String[CHUNK_SIZE];
        return;
    }

    private void ensureContentSpecCapacity(int chunk) {
        if (chunk >= fContentSpecType.length) {
            fContentSpecType = resize(fContentSpecType, fContentSpecType.length * 2);
            fContentSpecValue = resize(fContentSpecValue, fContentSpecValue.length * 2);
            fContentSpecOtherValue = resize(fContentSpecOtherValue, fContentSpecOtherValue.length * 2);
        }
        else if (fContentSpecType[chunk] != null) {
            return;
        }

        fContentSpecType[chunk] = new short[CHUNK_SIZE];
        fContentSpecValue[chunk] = new Object[CHUNK_SIZE];
        fContentSpecOtherValue[chunk] = new Object[CHUNK_SIZE];
        return;
    }

    //
    // Private static methods
    //

    // resize chunks

    private static byte[][] resize(byte array[][], int newsize) {
        byte newarray[][] = new byte[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

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

    private static DatatypeValidator[][] resize(DatatypeValidator array[][], int newsize) {
        DatatypeValidator newarray[][] = new DatatypeValidator[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static ContentModelValidator[][] resize(ContentModelValidator array[][], int newsize) {
        ContentModelValidator newarray[][] = new ContentModelValidator[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static Object[][] resize(Object array[][], int newsize) {
        Object newarray[][] = new Object[newsize][];
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

    //
    // Classes
    //

    /**
     * Children list for <code>contentSpecTree</code> method.
     *
     * @xerces.internal
     *
     * @author Eric Ye, IBM
     */
    private static class ChildrenList {

        //
        // Data
        //

        /** Length. */
        public int length = 0;

        // NOTE: The following set of data is mutually exclusive. It is
        //       written this way because Java doesn't have a native
        //       union data structure. -Ac

        /** Left and right children names. */
        public QName[] qname = new QName[2];

        /** Left and right children types. */
        public int[] type = new int[2];

        //
        // Constructors
        //

        public ChildrenList () {}

    } // class ChildrenList

    //
    // EntityState methods
    //
    public boolean isEntityDeclared (String name){
        return (getEntityDeclIndex(name)!=-1)?true:false;
    }

    public boolean isEntityUnparsed (String name){
        int entityIndex = getEntityDeclIndex(name);
        if (entityIndex >-1) {
            int chunk = entityIndex >> CHUNK_SHIFT;
            int index = entityIndex & CHUNK_MASK;
            //for unparsed entity notation!=null
            return (fEntityNotation[chunk][index]!=null)?true:false;
        }
        return false;
    }
} // class DTDGrammar
