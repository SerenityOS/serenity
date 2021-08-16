/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.xs.util.StringListImpl;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSNamedMap4Types;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSNamedMapImpl;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSObjectListImpl;
import com.sun.org.apache.xerces.internal.util.SymbolHash;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.xs.StringList;
import com.sun.org.apache.xerces.internal.xs.XSAttributeDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSAttributeGroupDefinition;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSElementDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSIDCDefinition;
import com.sun.org.apache.xerces.internal.xs.XSModel;
import com.sun.org.apache.xerces.internal.xs.XSModelGroupDefinition;
import com.sun.org.apache.xerces.internal.xs.XSNamedMap;
import com.sun.org.apache.xerces.internal.xs.XSNamespaceItem;
import com.sun.org.apache.xerces.internal.xs.XSNamespaceItemList;
import com.sun.org.apache.xerces.internal.xs.XSNotationDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSObject;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Implements XSModel:  a read-only interface that represents an XML Schema,
 * which could be components from different namespaces.
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 *
 * @LastModified: Oct 2017
 */
@SuppressWarnings("unchecked") // method <T>toArray(T[])
public final class XSModelImpl extends AbstractList<XSNamespaceItem> implements XSModel, XSNamespaceItemList {

    // the max index / the max value of XSObject type
    private static final short MAX_COMP_IDX = XSTypeDefinition.SIMPLE_TYPE;
    private static final boolean[] GLOBAL_COMP = {false,    // null
                                                  true,     // attribute
                                                  true,     // element
                                                  true,     // type
                                                  false,    // attribute use
                                                  true,     // attribute group
                                                  true,     // group
                                                  false,    // model group
                                                  false,    // particle
                                                  false,    // wildcard
                                                  true,    // idc
                                                  true,     // notation
                                                  false,    // annotation
                                                  false,    // facet
                                                  false,    // multi value facet
                                                  true,     // complex type
                                                  true      // simple type
                                                 };

    // number of grammars/namespaces stored here
    private final int fGrammarCount;
    // all target namespaces
    private final String[] fNamespaces;
    // all schema grammar objects (for each namespace)
    private final SchemaGrammar[] fGrammarList;
    // a map from namespace to schema grammar
    private final SymbolHash fGrammarMap;
    // a map from element declaration to its substitution group
    private final SymbolHash fSubGroupMap;

    // store a certain kind of components from all namespaces
    private final XSNamedMap[] fGlobalComponents;
    // store a certain kind of components from one namespace
    private final XSNamedMap[][] fNSComponents;

    // a string list of all the target namespaces.
    private final StringList fNamespacesList;
    // store all annotations
    private XSObjectList fAnnotations = null;

    // whether there is any IDC in this XSModel
    private final boolean fHasIDC;

   /**
    * Construct an XSModelImpl, by storing some grammars and grammars imported
    * by them to this object.
    *
    * @param grammars   the array of schema grammars
    */
    public XSModelImpl(SchemaGrammar[] grammars) {
        this(grammars, Constants.SCHEMA_VERSION_1_0);
    }

    public XSModelImpl(SchemaGrammar[] grammars, short s4sVersion) {
        // copy namespaces/grammars from the array to our arrays
        int len = grammars.length;
        final int initialSize = Math.max(len+1, 5);
        String[] namespaces = new String[initialSize];
        SchemaGrammar[] grammarList = new SchemaGrammar[initialSize];
        boolean hasS4S = false;
        for (int i = 0; i < len; i++) {
            final SchemaGrammar sg = grammars[i];
            final String tns = sg.getTargetNamespace();
            namespaces[i] = tns;
            grammarList[i] = sg;
            if (tns == SchemaSymbols.URI_SCHEMAFORSCHEMA) {
                hasS4S = true;
            }
        }
        // If a schema for the schema namespace isn't included, include it here.
        if (!hasS4S) {
            namespaces[len] = SchemaSymbols.URI_SCHEMAFORSCHEMA;
            grammarList[len++] = SchemaGrammar.getS4SGrammar(s4sVersion);
        }

        SchemaGrammar sg1, sg2;
        List<SchemaGrammar> gs;
        int i, j, k;
        // and recursively get all imported grammars, add them to our arrays
        for (i = 0; i < len; i++) {
            // get the grammar
            sg1 = grammarList[i];
            gs = sg1.getImportedGrammars();
            // for each imported grammar
            for (j = gs == null ? -1 : gs.size() - 1; j >= 0; j--) {
                sg2 = gs.get(j);
                // check whether this grammar is already in the list
                for (k = 0; k < len; k++) {
                    if (sg2 == grammarList[k]) {
                        break;
                    }
                }
                // if it's not, add it to the list
                if (k == len) {
                    // ensure the capacity of the arrays
                    if (len == grammarList.length) {
                        String[] newSA = new String[len*2];
                        System.arraycopy(namespaces, 0, newSA, 0, len);
                        namespaces = newSA;
                        SchemaGrammar[] newGA = new SchemaGrammar[len*2];
                        System.arraycopy(grammarList, 0, newGA, 0, len);
                        grammarList = newGA;
                    }
                    namespaces[len] = sg2.getTargetNamespace();
                    grammarList[len] = sg2;
                    len++;
                }
            }
        }

        fNamespaces = namespaces;
        fGrammarList = grammarList;

        boolean hasIDC = false;
        // establish the mapping from namespace to grammars
        fGrammarMap = new SymbolHash(len*2);
        for (i = 0; i < len; i++) {
            fGrammarMap.put(null2EmptyString(fNamespaces[i]), fGrammarList[i]);
            // update the idc field
            if (fGrammarList[i].hasIDConstraints()) {
                hasIDC = true;
            }
        }

        fHasIDC = hasIDC;
        fGrammarCount = len;
        fGlobalComponents = new XSNamedMap[MAX_COMP_IDX+1];
        fNSComponents = new XSNamedMap[len][MAX_COMP_IDX+1];
        fNamespacesList = new StringListImpl(fNamespaces, fGrammarCount);

        // build substitution groups
        fSubGroupMap = buildSubGroups();
    }

    private SymbolHash buildSubGroups_Org() {
        SubstitutionGroupHandler sgHandler = new SubstitutionGroupHandler(null);
        for (int i = 0 ; i < fGrammarCount; i++) {
            sgHandler.addSubstitutionGroup(fGrammarList[i].getSubstitutionGroups());
        }

        final XSNamedMap elements = getComponents(XSConstants.ELEMENT_DECLARATION);
        final int len = elements.getLength();
        final SymbolHash subGroupMap = new SymbolHash(len*2);
        XSElementDecl head;
        XSElementDeclaration[] subGroup;
        for (int i = 0; i < len; i++) {
            head = (XSElementDecl)elements.item(i);
            subGroup = sgHandler.getSubstitutionGroup(head);
            subGroupMap.put(head, subGroup.length > 0 ?
                    new XSObjectListImpl(subGroup, subGroup.length) : XSObjectListImpl.EMPTY_LIST);
        }
        return subGroupMap;
    }

    private SymbolHash buildSubGroups() {
        SubstitutionGroupHandler sgHandler = new SubstitutionGroupHandler(null);
        for (int i = 0 ; i < fGrammarCount; i++) {
            sgHandler.addSubstitutionGroup(fGrammarList[i].getSubstitutionGroups());
        }

        final XSObjectListImpl elements = getGlobalElements();
        final int len = elements.getLength();
        final SymbolHash subGroupMap = new SymbolHash(len*2);
        XSElementDecl head;
        XSElementDeclaration[] subGroup;
        for (int i = 0; i < len; i++) {
            head = (XSElementDecl)elements.item(i);
            subGroup = sgHandler.getSubstitutionGroup(head);
            subGroupMap.put(head, subGroup.length > 0 ?
                    new XSObjectListImpl(subGroup, subGroup.length) : XSObjectListImpl.EMPTY_LIST);
        }
        return subGroupMap;
    }

    private XSObjectListImpl getGlobalElements() {
        final SymbolHash[] tables = new SymbolHash[fGrammarCount];
        int length = 0;

        for (int i = 0; i < fGrammarCount; i++) {
            tables[i] = fGrammarList[i].fAllGlobalElemDecls;
            length += tables[i].getLength();
        }

        if (length == 0) {
            return XSObjectListImpl.EMPTY_LIST;
        }

        final XSObject[] components = new XSObject[length];

        int start = 0;
        for (int i = 0; i < fGrammarCount; i++) {
            tables[i].getValues(components, start);
            start += tables[i].getLength();
        }

        return new XSObjectListImpl(components, length);
    }

    /**
     * Convenience method. Returns a list of all namespaces that belong to
     * this schema.
     * @return A list of all namespaces that belong to this schema or
     *   <code>null</code> if all components don't have a targetNamespace.
     */
    public StringList getNamespaces() {
        return fNamespacesList;
    }

    /**
     * A set of namespace schema information information items (of type
     * <code>XSNamespaceItem</code>), one for each namespace name which
     * appears as the target namespace of any schema component in the schema
     * used for that assessment, and one for absent if any schema component
     * in the schema had no target namespace. For more information see
     * schema information.
     */
    public XSNamespaceItemList getNamespaceItems() {
        return this;
    }

    /**
     * Returns a list of top-level components, i.e. element declarations,
     * attribute declarations, etc.
     * @param objectType The type of the declaration, i.e.
     *   <code>ELEMENT_DECLARATION</code>. Note that
     *   <code>XSTypeDefinition.SIMPLE_TYPE</code> and
     *   <code>XSTypeDefinition.COMPLEX_TYPE</code> can also be used as the
     *   <code>objectType</code> to retrieve only complex types or simple
     *   types, instead of all types.
     * @return  A list of top-level definitions of the specified type in
     *   <code>objectType</code> or an empty <code>XSNamedMap</code> if no
     *   such definitions exist.
     */
    public synchronized XSNamedMap getComponents(short objectType) {
        if (objectType <= 0 || objectType > MAX_COMP_IDX ||
            !GLOBAL_COMP[objectType]) {
            return XSNamedMapImpl.EMPTY_MAP;
        }

        SymbolHash[] tables = new SymbolHash[fGrammarCount];
        // get all hashtables from all namespaces for this type of components
        if (fGlobalComponents[objectType] == null) {
            for (int i = 0; i < fGrammarCount; i++) {
                switch (objectType) {
                case XSConstants.TYPE_DEFINITION:
                case XSTypeDefinition.COMPLEX_TYPE:
                case XSTypeDefinition.SIMPLE_TYPE:
                    tables[i] = fGrammarList[i].fGlobalTypeDecls;
                    break;
                case XSConstants.ATTRIBUTE_DECLARATION:
                    tables[i] = fGrammarList[i].fGlobalAttrDecls;
                    break;
                case XSConstants.ELEMENT_DECLARATION:
                    tables[i] = fGrammarList[i].fGlobalElemDecls;
                    break;
                case XSConstants.ATTRIBUTE_GROUP:
                    tables[i] = fGrammarList[i].fGlobalAttrGrpDecls;
                    break;
                case XSConstants.MODEL_GROUP_DEFINITION:
                    tables[i] = fGrammarList[i].fGlobalGroupDecls;
                    break;
                case XSConstants.NOTATION_DECLARATION:
                    tables[i] = fGrammarList[i].fGlobalNotationDecls;
                    break;
                case XSConstants.IDENTITY_CONSTRAINT:
                    tables[i] = fGrammarList[i].fGlobalIDConstraintDecls;
                    break;
                }
            }
            // for complex/simple types, create a special implementation,
            // which take specific types out of the hash table
            if (objectType == XSTypeDefinition.COMPLEX_TYPE ||
                objectType == XSTypeDefinition.SIMPLE_TYPE) {
                fGlobalComponents[objectType] = new XSNamedMap4Types(fNamespaces, tables, fGrammarCount, objectType);
            }
            else {
                fGlobalComponents[objectType] = new XSNamedMapImpl(fNamespaces, tables, fGrammarCount);
            }
        }

        return fGlobalComponents[objectType];
    }

    /**
     * Convenience method. Returns a list of top-level component declarations
     * that are defined within the specified namespace, i.e. element
     * declarations, attribute declarations, etc.
     * @param objectType The type of the declaration, i.e.
     *   <code>ELEMENT_DECLARATION</code>.
     * @param namespace The namespace to which the declaration belongs or
     *   <code>null</code> (for components with no target namespace).
     * @return  A list of top-level definitions of the specified type in
     *   <code>objectType</code> and defined in the specified
     *   <code>namespace</code> or an empty <code>XSNamedMap</code>.
     */
    public synchronized XSNamedMap getComponentsByNamespace(short objectType,
                                                            String namespace) {
        if (objectType <= 0 || objectType > MAX_COMP_IDX ||
            !GLOBAL_COMP[objectType]) {
            return XSNamedMapImpl.EMPTY_MAP;
        }

        // try to find the grammar
        int i = 0;
        if (namespace != null) {
            for (; i < fGrammarCount; ++i) {
                if (namespace.equals(fNamespaces[i])) {
                    break;
                }
            }
        }
        else {
            for (; i < fGrammarCount; ++i) {
                if (fNamespaces[i] == null) {
                    break;
                }
            }
        }
        if (i == fGrammarCount) {
            return XSNamedMapImpl.EMPTY_MAP;
        }

        // get the hashtable for this type of components
        if (fNSComponents[i][objectType] == null) {
            SymbolHash table = null;
            switch (objectType) {
            case XSConstants.TYPE_DEFINITION:
            case XSTypeDefinition.COMPLEX_TYPE:
            case XSTypeDefinition.SIMPLE_TYPE:
                table = fGrammarList[i].fGlobalTypeDecls;
                break;
            case XSConstants.ATTRIBUTE_DECLARATION:
                table = fGrammarList[i].fGlobalAttrDecls;
                break;
            case XSConstants.ELEMENT_DECLARATION:
                table = fGrammarList[i].fGlobalElemDecls;
                break;
            case XSConstants.ATTRIBUTE_GROUP:
                table = fGrammarList[i].fGlobalAttrGrpDecls;
                break;
            case XSConstants.MODEL_GROUP_DEFINITION:
                table = fGrammarList[i].fGlobalGroupDecls;
                break;
            case XSConstants.NOTATION_DECLARATION:
                table = fGrammarList[i].fGlobalNotationDecls;
                break;
            case XSConstants.IDENTITY_CONSTRAINT:
                table = fGrammarList[i].fGlobalIDConstraintDecls;
                break;
            }

            // for complex/simple types, create a special implementation,
            // which take specific types out of the hash table
            if (objectType == XSTypeDefinition.COMPLEX_TYPE ||
                objectType == XSTypeDefinition.SIMPLE_TYPE) {
                fNSComponents[i][objectType] = new XSNamedMap4Types(namespace, table, objectType);
            }
            else {
                fNSComponents[i][objectType] = new XSNamedMapImpl(namespace, table);
            }
        }

        return fNSComponents[i][objectType];
    }

    /**
     * Convenience method. Returns a top-level simple or complex type
     * definition.
     * @param name The name of the definition.
     * @param namespace The namespace of the definition, otherwise null.
     * @return An <code>XSTypeDefinition</code> or null if such definition
     *   does not exist.
     */
    public XSTypeDefinition getTypeDefinition(String name,
                                              String namespace) {
        SchemaGrammar sg = (SchemaGrammar)fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return (XSTypeDefinition)sg.fGlobalTypeDecls.get(name);
    }

    /**
     * Convenience method. Returns a top-level simple or complex type
     * definition.
     * @param name The name of the definition.
     * @param namespace The namespace of the definition, otherwise null.
     * @param loc The schema location where the component was defined
     * @return An <code>XSTypeDefinition</code> or null if such definition
     *   does not exist.
     */
    public XSTypeDefinition getTypeDefinition(String name,
                                              String namespace,
                                              String loc) {
        SchemaGrammar sg = (SchemaGrammar)fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return sg.getGlobalTypeDecl(name, loc);
    }

    /**
     * Convenience method. Returns a top-level attribute declaration.
     * @param name The name of the declaration.
     * @param namespace The namespace of the definition, otherwise null.
     * @return A top-level attribute declaration or null if such declaration
     *   does not exist.
     */
    public XSAttributeDeclaration getAttributeDeclaration(String name,
                                                   String namespace) {
        SchemaGrammar sg = (SchemaGrammar)fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return (XSAttributeDeclaration)sg.fGlobalAttrDecls.get(name);
    }

    /**
     * Convenience method. Returns a top-level attribute declaration.
     * @param name The name of the declaration.
     * @param namespace The namespace of the definition, otherwise null.
     * @param loc The schema location where the component was defined
     * @return A top-level attribute declaration or null if such declaration
     *   does not exist.
     */
    public XSAttributeDeclaration getAttributeDeclaration(String name,
                                                   String namespace,
                                                   String loc) {
        SchemaGrammar sg = (SchemaGrammar)fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return sg.getGlobalAttributeDecl(name, loc);
    }

    /**
     * Convenience method. Returns a top-level element declaration.
     * @param name The name of the declaration.
     * @param namespace The namespace of the definition, otherwise null.
     * @return A top-level element declaration or null if such declaration
     *   does not exist.
     */
    public XSElementDeclaration getElementDeclaration(String name,
                                               String namespace) {
        SchemaGrammar sg = (SchemaGrammar)fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return (XSElementDeclaration)sg.fGlobalElemDecls.get(name);
    }

    /**
     * Convenience method. Returns a top-level element declaration.
     * @param name The name of the declaration.
     * @param namespace The namespace of the definition, otherwise null.
     * @param loc The schema location where the component was defined
     * @return A top-level element declaration or null if such declaration
     *   does not exist.
     */
    public XSElementDeclaration getElementDeclaration(String name,
                                               String namespace,
                                               String loc) {
        SchemaGrammar sg = (SchemaGrammar)fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return sg.getGlobalElementDecl(name, loc);
    }

    /**
     * Convenience method. Returns a top-level attribute group definition.
     * @param name The name of the definition.
     * @param namespace The namespace of the definition, otherwise null.
     * @return A top-level attribute group definition or null if such
     *   definition does not exist.
     */
    public XSAttributeGroupDefinition getAttributeGroup(String name,
                                                        String namespace) {
        SchemaGrammar sg = (SchemaGrammar)fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return (XSAttributeGroupDefinition)sg.fGlobalAttrGrpDecls.get(name);
    }

    /**
     * Convenience method. Returns a top-level attribute group definition.
     * @param name The name of the definition.
     * @param namespace The namespace of the definition, otherwise null.
     * @param loc The schema location where the component was defined
     * @return A top-level attribute group definition or null if such
     *   definition does not exist.
     */
    public XSAttributeGroupDefinition getAttributeGroup(String name,
                                                        String namespace,
                                                        String loc) {
        SchemaGrammar sg = (SchemaGrammar)fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return sg.getGlobalAttributeGroupDecl(name, loc);
    }

    /**
     * Convenience method. Returns a top-level model group definition.
     *
     * @param name      The name of the definition.
     * @param namespace The namespace of the definition, otherwise null.
     * @return A top-level model group definition definition or null if such
     *         definition does not exist.
     */
    public XSModelGroupDefinition getModelGroupDefinition(String name,
                                                          String namespace) {
        SchemaGrammar sg = (SchemaGrammar)fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return (XSModelGroupDefinition)sg.fGlobalGroupDecls.get(name);
    }

    /**
     * Convenience method. Returns a top-level model group definition.
     *
     * @param name      The name of the definition.
     * @param namespace The namespace of the definition, otherwise null.
     * @param loc The schema location where the component was defined
     * @return A top-level model group definition definition or null if such
     *         definition does not exist.
     */
    public XSModelGroupDefinition getModelGroupDefinition(String name,
                                                          String namespace,
                                                          String loc) {
        SchemaGrammar sg = (SchemaGrammar)fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return sg.getGlobalGroupDecl(name, loc);
    }

    /**
     * Convenience method. Returns a top-level model group definition.
     *
     * @param name      The name of the definition.
     * @param namespace The namespace of the definition, otherwise null.
     * @return A top-level model group definition definition or null if such
     *         definition does not exist.
     */
    public XSIDCDefinition getIDCDefinition(String name, String namespace) {
        SchemaGrammar sg = (SchemaGrammar)fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return (XSIDCDefinition)sg.fGlobalIDConstraintDecls.get(name);
    }

    /**
     * Convenience method. Returns a top-level model group definition.
     *
     * @param name      The name of the definition.
     * @param namespace The namespace of the definition, otherwise null.
     * @param loc The schema location where the component was defined
     * @return A top-level model group definition definition or null if such
     *         definition does not exist.
     */
    public XSIDCDefinition getIDCDefinition(String name, String namespace,
                                                          String loc) {
        SchemaGrammar sg = (SchemaGrammar)fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return sg.getIDConstraintDecl(name, loc);
    }


    /**
     * @see org.apache.xerces.xs.XSModel#getNotationDeclaration(String, String)
     */
    public XSNotationDeclaration getNotationDeclaration(String name,
                                                 String namespace) {
        SchemaGrammar sg = (SchemaGrammar)fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return (XSNotationDeclaration)sg.fGlobalNotationDecls.get(name);
    }

    public XSNotationDeclaration getNotationDeclaration(String name,
                                                 String namespace,
                                                 String loc) {
        SchemaGrammar sg = (SchemaGrammar)fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return sg.getGlobalNotationDecl(name, loc);
    }

    /**
     *  [annotations]: a set of annotations if it exists, otherwise an empty
     * <code>XSObjectList</code>.
     */
    public synchronized XSObjectList getAnnotations() {
        if (fAnnotations != null) {
            return fAnnotations;
        }

        // do this in two passes to avoid inaccurate array size
        int totalAnnotations = 0;
        for (int i = 0; i < fGrammarCount; i++) {
            totalAnnotations += fGrammarList[i].fNumAnnotations;
        }
        if (totalAnnotations == 0) {
            fAnnotations = XSObjectListImpl.EMPTY_LIST;
            return fAnnotations;
        }
        XSAnnotationImpl [] annotations = new XSAnnotationImpl [totalAnnotations];
        int currPos = 0;
        for (int i = 0; i < fGrammarCount; i++) {
            SchemaGrammar currGrammar = fGrammarList[i];
            if (currGrammar.fNumAnnotations > 0) {
                System.arraycopy(currGrammar.fAnnotations, 0, annotations, currPos, currGrammar.fNumAnnotations);
                currPos += currGrammar.fNumAnnotations;
            }
        }
        fAnnotations = new XSObjectListImpl(annotations, annotations.length);
        return fAnnotations;
    }

    private static final String null2EmptyString(String str) {
        return str == null ? XMLSymbols.EMPTY_STRING : str;
    }

    /**
     * REVISIT: to expose identity constraints from XSModel.
     * For now, we only expose whether there are any IDCs.
     * We also need to add these methods to the public
     * XSModel interface.
     */
    public boolean hasIDConstraints() {
        return fHasIDC;
    }

    /**
     * Convenience method. Returns a list containing the members of the
     * substitution group for the given <code>XSElementDeclaration</code>
     * or an empty <code>XSObjectList</code> if the substitution group
     * contains no members.
     * @param head The substitution group head.
     * @return A list containing the members of the substitution group
     *  for the given <code>XSElementDeclaration</code> or an empty
     *  <code>XSObjectList</code> if the substitution group contains
     *  no members.
     */
    public XSObjectList getSubstitutionGroup(XSElementDeclaration head) {
        return (XSObjectList)fSubGroupMap.get(head);
    }

    //
    // XSNamespaceItemList methods
    //

    /**
     * The number of <code>XSNamespaceItem</code>s in the list. The range of
     * valid child object indices is 0 to <code>length-1</code> inclusive.
     */
    public int getLength() {
        return fGrammarCount;
    }

    /**
     * Returns the <code>index</code>th item in the collection or
     * <code>null</code> if <code>index</code> is greater than or equal to
     * the number of objects in the list. The index starts at 0.
     * @param index  index into the collection.
     * @return  The <code>XSNamespaceItem</code> at the <code>index</code>th
     *   position in the <code>XSNamespaceItemList</code>, or
     *   <code>null</code> if the index specified is not valid.
     */
    public XSNamespaceItem item(int index) {
        if (index < 0 || index >= fGrammarCount) {
            return null;
        }
        return fGrammarList[index];
    }

    //
    // java.util.List methods
    //

    public XSNamespaceItem get(int index) {
        if (index >= 0 && index < fGrammarCount) {
            return fGrammarList[index];
        }
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    public int size() {
        return getLength();
    }

    public Iterator<XSNamespaceItem> iterator() {
        return listIterator0(0);
    }

    public ListIterator<XSNamespaceItem> listIterator() {
        return listIterator0(0);
    }

    public ListIterator<XSNamespaceItem> listIterator(int index) {
        if (index >= 0 && index < fGrammarCount) {
            return listIterator0(index);
        }
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    private ListIterator<XSNamespaceItem> listIterator0(int index) {
        return new XSNamespaceItemListIterator(index);
    }

    public Object[] toArray() {
        Object[] a = new Object[fGrammarCount];
        toArray0(a);
        return a;
    }

    public Object[] toArray(Object[] a) {
        if (a.length < fGrammarCount) {
            Class<?> arrayClass = a.getClass();
            Class<?> componentType = arrayClass.getComponentType();
            a = (Object[]) Array.newInstance(componentType, fGrammarCount);
        }
        toArray0(a);
        if (a.length > fGrammarCount) {
            a[fGrammarCount] = null;
        }
        return a;
    }

    private void toArray0(Object[] a) {
        if (fGrammarCount > 0) {
            System.arraycopy(fGrammarList, 0, a, 0, fGrammarCount);
        }
    }

    private final class XSNamespaceItemListIterator implements ListIterator<XSNamespaceItem> {
        private int index;
        public XSNamespaceItemListIterator(int index) {
            this.index = index;
        }
        public boolean hasNext() {
            return (index < fGrammarCount);
        }
        public XSNamespaceItem next() {
            if (index < fGrammarCount) {
                return fGrammarList[index++];
            }
            throw new NoSuchElementException();
        }
        public boolean hasPrevious() {
            return (index > 0);
        }
        public XSNamespaceItem previous() {
            if (index > 0) {
                return fGrammarList[--index];
            }
            throw new NoSuchElementException();
        }
        public int nextIndex() {
            return index;
        }
        public int previousIndex() {
            return index - 1;
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
        public void set(XSNamespaceItem o) {
            throw new UnsupportedOperationException();
        }
        public void add(XSNamespaceItem o) {
            throw new UnsupportedOperationException();
        }
    }

} // class XSModelImpl
