/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl.xs.traversers;

import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeValueException;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaGrammar;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaNamespaceSupport;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.XSAttributeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSGrammarBucket;
import com.sun.org.apache.xerces.internal.impl.xs.XSWildcardDecl;
import com.sun.org.apache.xerces.internal.impl.xs.util.XInt;
import com.sun.org.apache.xerces.internal.impl.xs.util.XIntPool;
import com.sun.org.apache.xerces.internal.util.DOMUtil;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityManager;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

/**
 * Class <code>XSAttributeCheck</code> is used to check the validity of attributes
 * appearing in the schema document. It
 * - reports an error for invalid element (invalid namespace, invalid name)
 * - reports an error for invalid attribute (invalid namespace, invalid name)
 * - reports an error for invalid attribute value
 * - return compiled values for attriute values
 * - provide default value for missing optional attributes
 * - provide default value for incorrect attribute values
 *
 * But it's the caller's responsibility to check whether a required attribute
 * is present.
 *
 * Things need revisiting:
 * - Whether to return non-schema attributes/values
 * - Do we need to update NamespaceScope and ErrorReporter when reset()?
 * - Should have the datatype validators return compiled value
 * - use symbol table instead of many maps
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 * @LastModified: Nov 2017
 */

public class XSAttributeChecker {

    // REVISIT: only local element and attribute are different from others.
    //          it's possible to have either name or ref. all the others
    //          are only allowed to have one of name or ref, or neither of them.
    //          we'd better move such checking to the traverser.
    private static final String ELEMENT_N = "element_n";
    private static final String ELEMENT_R = "element_r";
    private static final String ATTRIBUTE_N = "attribute_n";
    private static final String ATTRIBUTE_R = "attribute_r";

    private static       int ATTIDX_COUNT           = 0;
    public static final int ATTIDX_ABSTRACT        = ATTIDX_COUNT++;
    public static final int ATTIDX_AFORMDEFAULT    = ATTIDX_COUNT++;
    public static final int ATTIDX_BASE            = ATTIDX_COUNT++;
    public static final int ATTIDX_BLOCK           = ATTIDX_COUNT++;
    public static final int ATTIDX_BLOCKDEFAULT    = ATTIDX_COUNT++;
    public static final int ATTIDX_DEFAULT         = ATTIDX_COUNT++;
    public static final int ATTIDX_EFORMDEFAULT    = ATTIDX_COUNT++;
    public static final int ATTIDX_FINAL           = ATTIDX_COUNT++;
    public static final int ATTIDX_FINALDEFAULT    = ATTIDX_COUNT++;
    public static final int ATTIDX_FIXED           = ATTIDX_COUNT++;
    public static final int ATTIDX_FORM            = ATTIDX_COUNT++;
    public static final int ATTIDX_ID              = ATTIDX_COUNT++;
    public static final int ATTIDX_ITEMTYPE        = ATTIDX_COUNT++;
    public static final int ATTIDX_MAXOCCURS       = ATTIDX_COUNT++;
    public static final int ATTIDX_MEMBERTYPES     = ATTIDX_COUNT++;
    public static final int ATTIDX_MINOCCURS       = ATTIDX_COUNT++;
    public static final int ATTIDX_MIXED           = ATTIDX_COUNT++;
    public static final int ATTIDX_NAME            = ATTIDX_COUNT++;
    public static final int ATTIDX_NAMESPACE       = ATTIDX_COUNT++;
    public static final int ATTIDX_NAMESPACE_LIST  = ATTIDX_COUNT++;
    public static final int ATTIDX_NILLABLE        = ATTIDX_COUNT++;
    public static final int ATTIDX_NONSCHEMA       = ATTIDX_COUNT++;
    public static final int ATTIDX_PROCESSCONTENTS = ATTIDX_COUNT++;
    public static final int ATTIDX_PUBLIC          = ATTIDX_COUNT++;
    public static final int ATTIDX_REF             = ATTIDX_COUNT++;
    public static final int ATTIDX_REFER           = ATTIDX_COUNT++;
    public static final int ATTIDX_SCHEMALOCATION  = ATTIDX_COUNT++;
    public static final int ATTIDX_SOURCE          = ATTIDX_COUNT++;
    public static final int ATTIDX_SUBSGROUP       = ATTIDX_COUNT++;
    public static final int ATTIDX_SYSTEM          = ATTIDX_COUNT++;
    public static final int ATTIDX_TARGETNAMESPACE = ATTIDX_COUNT++;
    public static final int ATTIDX_TYPE            = ATTIDX_COUNT++;
    public static final int ATTIDX_USE             = ATTIDX_COUNT++;
    public static final int ATTIDX_VALUE           = ATTIDX_COUNT++;
    public static final int ATTIDX_ENUMNSDECLS     = ATTIDX_COUNT++;
    public static final int ATTIDX_VERSION         = ATTIDX_COUNT++;
    public static final int ATTIDX_XML_LANG        = ATTIDX_COUNT++;
    public static final int ATTIDX_XPATH           = ATTIDX_COUNT++;
    public static final int ATTIDX_FROMDEFAULT     = ATTIDX_COUNT++;
    //public static final int ATTIDX_OTHERVALUES     = ATTIDX_COUNT++;
    public static final int ATTIDX_ISRETURNED      = ATTIDX_COUNT++;

    private static final XIntPool fXIntPool = new XIntPool();
    // constants to return
    private static final XInt INT_QUALIFIED      = fXIntPool.getXInt(SchemaSymbols.FORM_QUALIFIED);
    private static final XInt INT_UNQUALIFIED    = fXIntPool.getXInt(SchemaSymbols.FORM_UNQUALIFIED);
    private static final XInt INT_EMPTY_SET      = fXIntPool.getXInt(XSConstants.DERIVATION_NONE);
    private static final XInt INT_ANY_STRICT     = fXIntPool.getXInt(XSWildcardDecl.PC_STRICT);
    private static final XInt INT_ANY_LAX        = fXIntPool.getXInt(XSWildcardDecl.PC_LAX);
    private static final XInt INT_ANY_SKIP       = fXIntPool.getXInt(XSWildcardDecl.PC_SKIP);
    private static final XInt INT_ANY_ANY        = fXIntPool.getXInt(XSWildcardDecl.NSCONSTRAINT_ANY);
    private static final XInt INT_ANY_LIST       = fXIntPool.getXInt(XSWildcardDecl.NSCONSTRAINT_LIST);
    private static final XInt INT_ANY_NOT        = fXIntPool.getXInt(XSWildcardDecl.NSCONSTRAINT_NOT);
    private static final XInt INT_USE_OPTIONAL   = fXIntPool.getXInt(SchemaSymbols.USE_OPTIONAL);
    private static final XInt INT_USE_REQUIRED   = fXIntPool.getXInt(SchemaSymbols.USE_REQUIRED);
    private static final XInt INT_USE_PROHIBITED = fXIntPool.getXInt(SchemaSymbols.USE_PROHIBITED);
    private static final XInt INT_WS_PRESERVE    = fXIntPool.getXInt(XSSimpleType.WS_PRESERVE);
    private static final XInt INT_WS_REPLACE     = fXIntPool.getXInt(XSSimpleType.WS_REPLACE);
    private static final XInt INT_WS_COLLAPSE    = fXIntPool.getXInt(XSSimpleType.WS_COLLAPSE);
    private static final XInt INT_UNBOUNDED      = fXIntPool.getXInt(SchemaSymbols.OCCURRENCE_UNBOUNDED);

    // used to store the map from element name to attribute list
    // for 14 global elements
    private static final Map<String, Container> fEleAttrsMapG = new HashMap<>(29);
    // for 39 local elememnts
    private static final Map<String, Container> fEleAttrsMapL = new HashMap<>(79);

    // used to initialize fEleAttrsMap
    // step 1: all possible data types
    // DT_??? >= 0 : validate using a validator, which is initialized staticly
    // DT_??? <  0 : validate directly, which is done in "validate()"

    protected static final int DT_ANYURI           = 0;
    protected static final int DT_ID               = 1;
    protected static final int DT_QNAME            = 2;
    protected static final int DT_STRING           = 3;
    protected static final int DT_TOKEN            = 4;
    protected static final int DT_NCNAME           = 5;
    protected static final int DT_XPATH            = 6;
    protected static final int DT_XPATH1           = 7;
    protected static final int DT_LANGUAGE         = 8;

    // used to store extra datatype validators
    protected static final int DT_COUNT            = DT_LANGUAGE + 1;
    private static final XSSimpleType[] fExtraDVs = new XSSimpleType[DT_COUNT];
    static {
        // step 5: register all datatype validators for new types
        SchemaGrammar grammar = SchemaGrammar.SG_SchemaNS;
        // anyURI
        fExtraDVs[DT_ANYURI] = (XSSimpleType)grammar.getGlobalTypeDecl(SchemaSymbols.ATTVAL_ANYURI);
        // ID
        fExtraDVs[DT_ID] = (XSSimpleType)grammar.getGlobalTypeDecl(SchemaSymbols.ATTVAL_ID);
        // QName
        fExtraDVs[DT_QNAME] = (XSSimpleType)grammar.getGlobalTypeDecl(SchemaSymbols.ATTVAL_QNAME);
        // string
        fExtraDVs[DT_STRING] = (XSSimpleType)grammar.getGlobalTypeDecl(SchemaSymbols.ATTVAL_STRING);
        // token
        fExtraDVs[DT_TOKEN] = (XSSimpleType)grammar.getGlobalTypeDecl(SchemaSymbols.ATTVAL_TOKEN);
        // NCName
        fExtraDVs[DT_NCNAME] = (XSSimpleType)grammar.getGlobalTypeDecl(SchemaSymbols.ATTVAL_NCNAME);
        // xpath = a subset of XPath expression
        fExtraDVs[DT_XPATH] = fExtraDVs[DT_STRING];
        // xpath = a subset of XPath expression
        fExtraDVs[DT_XPATH] = fExtraDVs[DT_STRING];
        // language
        fExtraDVs[DT_LANGUAGE] = (XSSimpleType)grammar.getGlobalTypeDecl(SchemaSymbols.ATTVAL_LANGUAGE);
    }

    protected static final int DT_BLOCK            = -1;
    protected static final int DT_BLOCK1           = -2;
    protected static final int DT_FINAL            = -3;
    protected static final int DT_FINAL1           = -4;
    protected static final int DT_FINAL2           = -5;
    protected static final int DT_FORM             = -6;
    protected static final int DT_MAXOCCURS        = -7;
    protected static final int DT_MAXOCCURS1       = -8;
    protected static final int DT_MEMBERTYPES      = -9;
    protected static final int DT_MINOCCURS1       = -10;
    protected static final int DT_NAMESPACE        = -11;
    protected static final int DT_PROCESSCONTENTS  = -12;
    protected static final int DT_USE              = -13;
    protected static final int DT_WHITESPACE       = -14;
    protected static final int DT_BOOLEAN          = -15;
    protected static final int DT_NONNEGINT        = -16;
    protected static final int DT_POSINT           = -17;

    static {
        // step 2: all possible attributes for all elements
        int attCount = 0;
        int ATT_ABSTRACT_D          = attCount++;
        int ATT_ATTRIBUTE_FD_D      = attCount++;
        int ATT_BASE_R              = attCount++;
        int ATT_BASE_N              = attCount++;
        int ATT_BLOCK_N             = attCount++;
        int ATT_BLOCK1_N            = attCount++;
        int ATT_BLOCK_D_D           = attCount++;
        int ATT_DEFAULT_N           = attCount++;
        int ATT_ELEMENT_FD_D        = attCount++;
        int ATT_FINAL_N             = attCount++;
        int ATT_FINAL1_N            = attCount++;
        int ATT_FINAL_D_D           = attCount++;
        int ATT_FIXED_N             = attCount++;
        int ATT_FIXED_D             = attCount++;
        int ATT_FORM_N              = attCount++;
        int ATT_ID_N                = attCount++;
        int ATT_ITEMTYPE_N          = attCount++;
        int ATT_MAXOCCURS_D         = attCount++;
        int ATT_MAXOCCURS1_D        = attCount++;
        int ATT_MEMBER_T_N          = attCount++;
        int ATT_MINOCCURS_D         = attCount++;
        int ATT_MINOCCURS1_D        = attCount++;
        int ATT_MIXED_D             = attCount++;
        int ATT_MIXED_N             = attCount++;
        int ATT_NAME_R              = attCount++;
        int ATT_NAMESPACE_D         = attCount++;
        int ATT_NAMESPACE_N         = attCount++;
        int ATT_NILLABLE_D          = attCount++;
        int ATT_PROCESS_C_D         = attCount++;
        int ATT_PUBLIC_R            = attCount++;
        int ATT_REF_R               = attCount++;
        int ATT_REFER_R             = attCount++;
        int ATT_SCHEMA_L_R          = attCount++;
        int ATT_SCHEMA_L_N          = attCount++;
        int ATT_SOURCE_N            = attCount++;
        int ATT_SUBSTITUTION_G_N    = attCount++;
        int ATT_SYSTEM_N            = attCount++;
        int ATT_TARGET_N_N          = attCount++;
        int ATT_TYPE_N              = attCount++;
        int ATT_USE_D               = attCount++;
        int ATT_VALUE_NNI_N         = attCount++;
        int ATT_VALUE_PI_N          = attCount++;
        int ATT_VALUE_STR_N         = attCount++;
        int ATT_VALUE_WS_N          = attCount++;
        int ATT_VERSION_N           = attCount++;
        int ATT_XML_LANG            = attCount++;
        int ATT_XPATH_R             = attCount++;
        int ATT_XPATH1_R            = attCount++;

        // step 3: store all these attributes in an array
        OneAttr[] allAttrs = new OneAttr[attCount];
        allAttrs[ATT_ABSTRACT_D]        =   new OneAttr(SchemaSymbols.ATT_ABSTRACT,
                                                        DT_BOOLEAN,
                                                        ATTIDX_ABSTRACT,
                                                        Boolean.FALSE);
        allAttrs[ATT_ATTRIBUTE_FD_D]    =   new OneAttr(SchemaSymbols.ATT_ATTRIBUTEFORMDEFAULT,
                                                        DT_FORM,
                                                        ATTIDX_AFORMDEFAULT,
                                                        INT_UNQUALIFIED);
        allAttrs[ATT_BASE_R]            =   new OneAttr(SchemaSymbols.ATT_BASE,
                                                        DT_QNAME,
                                                        ATTIDX_BASE,
                                                        null);
        allAttrs[ATT_BASE_N]            =   new OneAttr(SchemaSymbols.ATT_BASE,
                                                        DT_QNAME,
                                                        ATTIDX_BASE,
                                                        null);
        allAttrs[ATT_BLOCK_N]           =   new OneAttr(SchemaSymbols.ATT_BLOCK,
                                                        DT_BLOCK,
                                                        ATTIDX_BLOCK,
                                                        null);
        allAttrs[ATT_BLOCK1_N]          =   new OneAttr(SchemaSymbols.ATT_BLOCK,
                                                        DT_BLOCK1,
                                                        ATTIDX_BLOCK,
                                                        null);
        allAttrs[ATT_BLOCK_D_D]         =   new OneAttr(SchemaSymbols.ATT_BLOCKDEFAULT,
                                                        DT_BLOCK,
                                                        ATTIDX_BLOCKDEFAULT,
                                                        INT_EMPTY_SET);
        allAttrs[ATT_DEFAULT_N]         =   new OneAttr(SchemaSymbols.ATT_DEFAULT,
                                                        DT_STRING,
                                                        ATTIDX_DEFAULT,
                                                        null);
        allAttrs[ATT_ELEMENT_FD_D]      =   new OneAttr(SchemaSymbols.ATT_ELEMENTFORMDEFAULT,
                                                        DT_FORM,
                                                        ATTIDX_EFORMDEFAULT,
                                                        INT_UNQUALIFIED);
        allAttrs[ATT_FINAL_N]           =   new OneAttr(SchemaSymbols.ATT_FINAL,
                                                        DT_FINAL,
                                                        ATTIDX_FINAL,
                                                        null);
        allAttrs[ATT_FINAL1_N]          =   new OneAttr(SchemaSymbols.ATT_FINAL,
                                                        DT_FINAL1,
                                                        ATTIDX_FINAL,
                                                        null);
        allAttrs[ATT_FINAL_D_D]         =   new OneAttr(SchemaSymbols.ATT_FINALDEFAULT,
                                                        DT_FINAL2,
                                                        ATTIDX_FINALDEFAULT,
                                                        INT_EMPTY_SET);
        allAttrs[ATT_FIXED_N]           =   new OneAttr(SchemaSymbols.ATT_FIXED,
                                                        DT_STRING,
                                                        ATTIDX_FIXED,
                                                        null);
        allAttrs[ATT_FIXED_D]           =   new OneAttr(SchemaSymbols.ATT_FIXED,
                                                        DT_BOOLEAN,
                                                        ATTIDX_FIXED,
                                                        Boolean.FALSE);
        allAttrs[ATT_FORM_N]            =   new OneAttr(SchemaSymbols.ATT_FORM,
                                                        DT_FORM,
                                                        ATTIDX_FORM,
                                                        null);
        allAttrs[ATT_ID_N]              =   new OneAttr(SchemaSymbols.ATT_ID,
                                                        DT_ID,
                                                        ATTIDX_ID,
                                                        null);
        allAttrs[ATT_ITEMTYPE_N]        =   new OneAttr(SchemaSymbols.ATT_ITEMTYPE,
                                                        DT_QNAME,
                                                        ATTIDX_ITEMTYPE,
                                                        null);
        allAttrs[ATT_MAXOCCURS_D]       =   new OneAttr(SchemaSymbols.ATT_MAXOCCURS,
                                                        DT_MAXOCCURS,
                                                        ATTIDX_MAXOCCURS,
                                                        fXIntPool.getXInt(1));
        allAttrs[ATT_MAXOCCURS1_D]      =   new OneAttr(SchemaSymbols.ATT_MAXOCCURS,
                                                        DT_MAXOCCURS1,
                                                        ATTIDX_MAXOCCURS,
                                                        fXIntPool.getXInt(1));
        allAttrs[ATT_MEMBER_T_N]        =   new OneAttr(SchemaSymbols.ATT_MEMBERTYPES,
                                                        DT_MEMBERTYPES,
                                                        ATTIDX_MEMBERTYPES,
                                                        null);
        allAttrs[ATT_MINOCCURS_D]       =   new OneAttr(SchemaSymbols.ATT_MINOCCURS,
                                                        DT_NONNEGINT,
                                                        ATTIDX_MINOCCURS,
                                                        fXIntPool.getXInt(1));
        allAttrs[ATT_MINOCCURS1_D]      =   new OneAttr(SchemaSymbols.ATT_MINOCCURS,
                                                        DT_MINOCCURS1,
                                                        ATTIDX_MINOCCURS,
                                                        fXIntPool.getXInt(1));
        allAttrs[ATT_MIXED_D]           =   new OneAttr(SchemaSymbols.ATT_MIXED,
                                                        DT_BOOLEAN,
                                                        ATTIDX_MIXED,
                                                        Boolean.FALSE);
        allAttrs[ATT_MIXED_N]           =   new OneAttr(SchemaSymbols.ATT_MIXED,
                                                        DT_BOOLEAN,
                                                        ATTIDX_MIXED,
                                                        null);
        allAttrs[ATT_NAME_R]            =   new OneAttr(SchemaSymbols.ATT_NAME,
                                                        DT_NCNAME,
                                                        ATTIDX_NAME,
                                                        null);
        allAttrs[ATT_NAMESPACE_D]       =   new OneAttr(SchemaSymbols.ATT_NAMESPACE,
                                                        DT_NAMESPACE,
                                                        ATTIDX_NAMESPACE,
                                                        INT_ANY_ANY);
        allAttrs[ATT_NAMESPACE_N]       =   new OneAttr(SchemaSymbols.ATT_NAMESPACE,
                                                        DT_ANYURI,
                                                        ATTIDX_NAMESPACE,
                                                        null);
        allAttrs[ATT_NILLABLE_D]        =   new OneAttr(SchemaSymbols.ATT_NILLABLE,
                                                        DT_BOOLEAN,
                                                        ATTIDX_NILLABLE,
                                                        Boolean.FALSE);
        allAttrs[ATT_PROCESS_C_D]       =   new OneAttr(SchemaSymbols.ATT_PROCESSCONTENTS,
                                                        DT_PROCESSCONTENTS,
                                                        ATTIDX_PROCESSCONTENTS,
                                                        INT_ANY_STRICT);
        allAttrs[ATT_PUBLIC_R]          =   new OneAttr(SchemaSymbols.ATT_PUBLIC,
                                                        DT_TOKEN,
                                                        ATTIDX_PUBLIC,
                                                        null);
        allAttrs[ATT_REF_R]             =   new OneAttr(SchemaSymbols.ATT_REF,
                                                        DT_QNAME,
                                                        ATTIDX_REF,
                                                        null);
        allAttrs[ATT_REFER_R]           =   new OneAttr(SchemaSymbols.ATT_REFER,
                                                        DT_QNAME,
                                                        ATTIDX_REFER,
                                                        null);
        allAttrs[ATT_SCHEMA_L_R]        =   new OneAttr(SchemaSymbols.ATT_SCHEMALOCATION,
                                                        DT_ANYURI,
                                                        ATTIDX_SCHEMALOCATION,
                                                        null);
        allAttrs[ATT_SCHEMA_L_N]        =   new OneAttr(SchemaSymbols.ATT_SCHEMALOCATION,
                                                        DT_ANYURI,
                                                        ATTIDX_SCHEMALOCATION,
                                                        null);
        allAttrs[ATT_SOURCE_N]          =   new OneAttr(SchemaSymbols.ATT_SOURCE,
                                                        DT_ANYURI,
                                                        ATTIDX_SOURCE,
                                                        null);
        allAttrs[ATT_SUBSTITUTION_G_N]  =   new OneAttr(SchemaSymbols.ATT_SUBSTITUTIONGROUP,
                                                        DT_QNAME,
                                                        ATTIDX_SUBSGROUP,
                                                        null);
        allAttrs[ATT_SYSTEM_N]          =   new OneAttr(SchemaSymbols.ATT_SYSTEM,
                                                        DT_ANYURI,
                                                        ATTIDX_SYSTEM,
                                                        null);
        allAttrs[ATT_TARGET_N_N]        =   new OneAttr(SchemaSymbols.ATT_TARGETNAMESPACE,
                                                        DT_ANYURI,
                                                        ATTIDX_TARGETNAMESPACE,
                                                        null);
        allAttrs[ATT_TYPE_N]            =   new OneAttr(SchemaSymbols.ATT_TYPE,
                                                        DT_QNAME,
                                                        ATTIDX_TYPE,
                                                        null);
        allAttrs[ATT_USE_D]             =   new OneAttr(SchemaSymbols.ATT_USE,
                                                        DT_USE,
                                                        ATTIDX_USE,
                                                        INT_USE_OPTIONAL);
        allAttrs[ATT_VALUE_NNI_N]       =   new OneAttr(SchemaSymbols.ATT_VALUE,
                                                        DT_NONNEGINT,
                                                        ATTIDX_VALUE,
                                                        null);
        allAttrs[ATT_VALUE_PI_N]        =   new OneAttr(SchemaSymbols.ATT_VALUE,
                                                        DT_POSINT,
                                                        ATTIDX_VALUE,
                                                        null);
        allAttrs[ATT_VALUE_STR_N]       =   new OneAttr(SchemaSymbols.ATT_VALUE,
                                                        DT_STRING,
                                                        ATTIDX_VALUE,
                                                        null);
        allAttrs[ATT_VALUE_WS_N]        =   new OneAttr(SchemaSymbols.ATT_VALUE,
                                                        DT_WHITESPACE,
                                                        ATTIDX_VALUE,
                                                        null);
        allAttrs[ATT_VERSION_N]         =   new OneAttr(SchemaSymbols.ATT_VERSION,
                                                        DT_TOKEN,
                                                        ATTIDX_VERSION,
                                                        null);
        allAttrs[ATT_XML_LANG]          =   new OneAttr(SchemaSymbols.ATT_XML_LANG,
                                                        DT_LANGUAGE,
                                                        ATTIDX_XML_LANG,
                                                        null);
        allAttrs[ATT_XPATH_R]           =   new OneAttr(SchemaSymbols.ATT_XPATH,
                                                        DT_XPATH,
                                                        ATTIDX_XPATH,
                                                        null);
        allAttrs[ATT_XPATH1_R]          =   new OneAttr(SchemaSymbols.ATT_XPATH,
                                                        DT_XPATH1,
                                                        ATTIDX_XPATH,
                                                        null);

        // step 4: for each element, make a list of possible attributes
        Container attrList;

        // for element "attribute" - global
        attrList = Container.getContainer(5);
        // default = string
        attrList.put(SchemaSymbols.ATT_DEFAULT, allAttrs[ATT_DEFAULT_N]);
        // fixed = string
        attrList.put(SchemaSymbols.ATT_FIXED, allAttrs[ATT_FIXED_N]);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // name = NCName
        attrList.put(SchemaSymbols.ATT_NAME, allAttrs[ATT_NAME_R]);
        // type = QName
        attrList.put(SchemaSymbols.ATT_TYPE, allAttrs[ATT_TYPE_N]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_ATTRIBUTE, attrList);

        // for element "attribute" - local name
        attrList = Container.getContainer(7);
        // default = string
        attrList.put(SchemaSymbols.ATT_DEFAULT, allAttrs[ATT_DEFAULT_N]);
        // fixed = string
        attrList.put(SchemaSymbols.ATT_FIXED, allAttrs[ATT_FIXED_N]);
        // form = (qualified | unqualified)
        attrList.put(SchemaSymbols.ATT_FORM, allAttrs[ATT_FORM_N]);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // name = NCName
        attrList.put(SchemaSymbols.ATT_NAME, allAttrs[ATT_NAME_R]);
        // type = QName
        attrList.put(SchemaSymbols.ATT_TYPE, allAttrs[ATT_TYPE_N]);
        // use = (optional | prohibited | required) : optional
        attrList.put(SchemaSymbols.ATT_USE, allAttrs[ATT_USE_D]);
        fEleAttrsMapL.put(ATTRIBUTE_N, attrList);

        // for element "attribute" - local ref
        attrList = Container.getContainer(5);
        // default = string
        attrList.put(SchemaSymbols.ATT_DEFAULT, allAttrs[ATT_DEFAULT_N]);
        // fixed = string
        attrList.put(SchemaSymbols.ATT_FIXED, allAttrs[ATT_FIXED_N]);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // ref = QName
        attrList.put(SchemaSymbols.ATT_REF, allAttrs[ATT_REF_R]);
        // use = (optional | prohibited | required) : optional
        attrList.put(SchemaSymbols.ATT_USE, allAttrs[ATT_USE_D]);
        fEleAttrsMapL.put(ATTRIBUTE_R, attrList);

        // for element "element" - global
        attrList = Container.getContainer(10);
        // abstract = boolean : false
        attrList.put(SchemaSymbols.ATT_ABSTRACT, allAttrs[ATT_ABSTRACT_D]);
        // block = (#all | List of (extension | restriction | substitution))
        attrList.put(SchemaSymbols.ATT_BLOCK, allAttrs[ATT_BLOCK_N]);
        // default = string
        attrList.put(SchemaSymbols.ATT_DEFAULT, allAttrs[ATT_DEFAULT_N]);
        // final = (#all | List of (extension | restriction))
        attrList.put(SchemaSymbols.ATT_FINAL, allAttrs[ATT_FINAL_N]);
        // fixed = string
        attrList.put(SchemaSymbols.ATT_FIXED, allAttrs[ATT_FIXED_N]);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // name = NCName
        attrList.put(SchemaSymbols.ATT_NAME, allAttrs[ATT_NAME_R]);
        // nillable = boolean : false
        attrList.put(SchemaSymbols.ATT_NILLABLE, allAttrs[ATT_NILLABLE_D]);
        // substitutionGroup = QName
        attrList.put(SchemaSymbols.ATT_SUBSTITUTIONGROUP, allAttrs[ATT_SUBSTITUTION_G_N]);
        // type = QName
        attrList.put(SchemaSymbols.ATT_TYPE, allAttrs[ATT_TYPE_N]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_ELEMENT, attrList);

        // for element "element" - local name
        attrList = Container.getContainer(10);
        // block = (#all | List of (extension | restriction | substitution))
        attrList.put(SchemaSymbols.ATT_BLOCK, allAttrs[ATT_BLOCK_N]);
        // default = string
        attrList.put(SchemaSymbols.ATT_DEFAULT, allAttrs[ATT_DEFAULT_N]);
        // fixed = string
        attrList.put(SchemaSymbols.ATT_FIXED, allAttrs[ATT_FIXED_N]);
        // form = (qualified | unqualified)
        attrList.put(SchemaSymbols.ATT_FORM, allAttrs[ATT_FORM_N]);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // maxOccurs = (nonNegativeInteger | unbounded)  : 1
        attrList.put(SchemaSymbols.ATT_MAXOCCURS, allAttrs[ATT_MAXOCCURS_D]);
        // minOccurs = nonNegativeInteger : 1
        attrList.put(SchemaSymbols.ATT_MINOCCURS, allAttrs[ATT_MINOCCURS_D]);
        // name = NCName
        attrList.put(SchemaSymbols.ATT_NAME, allAttrs[ATT_NAME_R]);
        // nillable = boolean : false
        attrList.put(SchemaSymbols.ATT_NILLABLE, allAttrs[ATT_NILLABLE_D]);
        // type = QName
        attrList.put(SchemaSymbols.ATT_TYPE, allAttrs[ATT_TYPE_N]);
        fEleAttrsMapL.put(ELEMENT_N, attrList);

        // for element "element" - local ref
        attrList = Container.getContainer(4);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // maxOccurs = (nonNegativeInteger | unbounded)  : 1
        attrList.put(SchemaSymbols.ATT_MAXOCCURS, allAttrs[ATT_MAXOCCURS_D]);
        // minOccurs = nonNegativeInteger : 1
        attrList.put(SchemaSymbols.ATT_MINOCCURS, allAttrs[ATT_MINOCCURS_D]);
        // ref = QName
        attrList.put(SchemaSymbols.ATT_REF, allAttrs[ATT_REF_R]);
        fEleAttrsMapL.put(ELEMENT_R, attrList);

        // for element "complexType" - global
        attrList = Container.getContainer(6);
        // abstract = boolean : false
        attrList.put(SchemaSymbols.ATT_ABSTRACT, allAttrs[ATT_ABSTRACT_D]);
        // block = (#all | List of (extension | restriction))
        attrList.put(SchemaSymbols.ATT_BLOCK, allAttrs[ATT_BLOCK1_N]);
        // final = (#all | List of (extension | restriction))
        attrList.put(SchemaSymbols.ATT_FINAL, allAttrs[ATT_FINAL_N]);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // mixed = boolean : false
        attrList.put(SchemaSymbols.ATT_MIXED, allAttrs[ATT_MIXED_D]);
        // name = NCName
        attrList.put(SchemaSymbols.ATT_NAME, allAttrs[ATT_NAME_R]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_COMPLEXTYPE, attrList);

        // for element "notation" - global
        attrList = Container.getContainer(4);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // name = NCName
        attrList.put(SchemaSymbols.ATT_NAME, allAttrs[ATT_NAME_R]);
        // public = A public identifier, per ISO 8879
        attrList.put(SchemaSymbols.ATT_PUBLIC, allAttrs[ATT_PUBLIC_R]);
        // system = anyURI
        attrList.put(SchemaSymbols.ATT_SYSTEM, allAttrs[ATT_SYSTEM_N]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_NOTATION, attrList);


        // for element "complexType" - local
        attrList = Container.getContainer(2);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // mixed = boolean : false
        attrList.put(SchemaSymbols.ATT_MIXED, allAttrs[ATT_MIXED_D]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_COMPLEXTYPE, attrList);

        // for element "simpleContent" - local
        attrList = Container.getContainer(1);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_SIMPLECONTENT, attrList);

        // for element "restriction" - local
        attrList = Container.getContainer(2);
        // base = QName
        attrList.put(SchemaSymbols.ATT_BASE, allAttrs[ATT_BASE_N]);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_RESTRICTION, attrList);

        // for element "extension" - local
        attrList = Container.getContainer(2);
        // base = QName
        attrList.put(SchemaSymbols.ATT_BASE, allAttrs[ATT_BASE_R]);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_EXTENSION, attrList);

        // for element "attributeGroup" - local ref
        attrList = Container.getContainer(2);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // ref = QName
        attrList.put(SchemaSymbols.ATT_REF, allAttrs[ATT_REF_R]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_ATTRIBUTEGROUP, attrList);

        // for element "anyAttribute" - local
        attrList = Container.getContainer(3);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // namespace = ((##any | ##other) | List of (anyURI | (##targetNamespace | ##local)) )  : ##any
        attrList.put(SchemaSymbols.ATT_NAMESPACE, allAttrs[ATT_NAMESPACE_D]);
        // processContents = (lax | skip | strict) : strict
        attrList.put(SchemaSymbols.ATT_PROCESSCONTENTS, allAttrs[ATT_PROCESS_C_D]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_ANYATTRIBUTE, attrList);

        // for element "complexContent" - local
        attrList = Container.getContainer(2);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // mixed = boolean
        attrList.put(SchemaSymbols.ATT_MIXED, allAttrs[ATT_MIXED_N]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_COMPLEXCONTENT, attrList);

        // for element "attributeGroup" - global
        attrList = Container.getContainer(2);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // name = NCName
        attrList.put(SchemaSymbols.ATT_NAME, allAttrs[ATT_NAME_R]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_ATTRIBUTEGROUP, attrList);

        // for element "group" - global
        attrList = Container.getContainer(2);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // name = NCName
        attrList.put(SchemaSymbols.ATT_NAME, allAttrs[ATT_NAME_R]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_GROUP, attrList);

        // for element "group" - local ref
        attrList = Container.getContainer(4);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // maxOccurs = (nonNegativeInteger | unbounded)  : 1
        attrList.put(SchemaSymbols.ATT_MAXOCCURS, allAttrs[ATT_MAXOCCURS_D]);
        // minOccurs = nonNegativeInteger : 1
        attrList.put(SchemaSymbols.ATT_MINOCCURS, allAttrs[ATT_MINOCCURS_D]);
        // ref = QName
        attrList.put(SchemaSymbols.ATT_REF, allAttrs[ATT_REF_R]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_GROUP, attrList);

        // for element "all" - local
        attrList = Container.getContainer(3);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // maxOccurs = 1 : 1
        attrList.put(SchemaSymbols.ATT_MAXOCCURS, allAttrs[ATT_MAXOCCURS1_D]);
        // minOccurs = (0 | 1) : 1
        attrList.put(SchemaSymbols.ATT_MINOCCURS, allAttrs[ATT_MINOCCURS1_D]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_ALL, attrList);

        // for element "choice" - local
        attrList = Container.getContainer(3);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // maxOccurs = (nonNegativeInteger | unbounded)  : 1
        attrList.put(SchemaSymbols.ATT_MAXOCCURS, allAttrs[ATT_MAXOCCURS_D]);
        // minOccurs = nonNegativeInteger : 1
        attrList.put(SchemaSymbols.ATT_MINOCCURS, allAttrs[ATT_MINOCCURS_D]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_CHOICE, attrList);
        // for element "sequence" - local
        fEleAttrsMapL.put(SchemaSymbols.ELT_SEQUENCE, attrList);

        // for element "any" - local
        attrList = Container.getContainer(5);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // maxOccurs = (nonNegativeInteger | unbounded)  : 1
        attrList.put(SchemaSymbols.ATT_MAXOCCURS, allAttrs[ATT_MAXOCCURS_D]);
        // minOccurs = nonNegativeInteger : 1
        attrList.put(SchemaSymbols.ATT_MINOCCURS, allAttrs[ATT_MINOCCURS_D]);
        // namespace = ((##any | ##other) | List of (anyURI | (##targetNamespace | ##local)) )  : ##any
        attrList.put(SchemaSymbols.ATT_NAMESPACE, allAttrs[ATT_NAMESPACE_D]);
        // processContents = (lax | skip | strict) : strict
        attrList.put(SchemaSymbols.ATT_PROCESSCONTENTS, allAttrs[ATT_PROCESS_C_D]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_ANY, attrList);

        // for element "unique" - local
        attrList = Container.getContainer(2);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // name = NCName
        attrList.put(SchemaSymbols.ATT_NAME, allAttrs[ATT_NAME_R]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_UNIQUE, attrList);
        // for element "key" - local
        fEleAttrsMapL.put(SchemaSymbols.ELT_KEY, attrList);

        // for element "keyref" - local
        attrList = Container.getContainer(3);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // name = NCName
        attrList.put(SchemaSymbols.ATT_NAME, allAttrs[ATT_NAME_R]);
        // refer = QName
        attrList.put(SchemaSymbols.ATT_REFER, allAttrs[ATT_REFER_R]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_KEYREF, attrList);

        // for element "selector" - local
        attrList = Container.getContainer(2);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // xpath = a subset of XPath expression
        attrList.put(SchemaSymbols.ATT_XPATH, allAttrs[ATT_XPATH_R]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_SELECTOR, attrList);

        // for element "field" - local
        attrList = Container.getContainer(2);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // xpath = a subset of XPath expression
        attrList.put(SchemaSymbols.ATT_XPATH, allAttrs[ATT_XPATH1_R]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_FIELD, attrList);

        // for element "annotation" - global
        attrList = Container.getContainer(1);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_ANNOTATION, attrList);
        // for element "annotation" - local
        fEleAttrsMapL.put(SchemaSymbols.ELT_ANNOTATION, attrList);

        // for element "appinfo" - local
        attrList = Container.getContainer(1);
        // source = anyURI
        attrList.put(SchemaSymbols.ATT_SOURCE, allAttrs[ATT_SOURCE_N]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_APPINFO, attrList);
        fEleAttrsMapL.put(SchemaSymbols.ELT_APPINFO, attrList);

        // for element "documentation" - local
        attrList = Container.getContainer(2);
        // source = anyURI
        attrList.put(SchemaSymbols.ATT_SOURCE, allAttrs[ATT_SOURCE_N]);
        // xml:lang = language
        attrList.put(SchemaSymbols.ATT_XML_LANG, allAttrs[ATT_XML_LANG]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_DOCUMENTATION, attrList);
        fEleAttrsMapL.put(SchemaSymbols.ELT_DOCUMENTATION, attrList);

        // for element "simpleType" - global
        attrList = Container.getContainer(3);
        // final = (#all | List of (list | union | restriction))
        attrList.put(SchemaSymbols.ATT_FINAL, allAttrs[ATT_FINAL1_N]);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // name = NCName
        attrList.put(SchemaSymbols.ATT_NAME, allAttrs[ATT_NAME_R]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_SIMPLETYPE, attrList);

        // for element "simpleType" - local
        attrList = Container.getContainer(2);
        // final = (#all | List of (list | union | restriction))
        attrList.put(SchemaSymbols.ATT_FINAL, allAttrs[ATT_FINAL1_N]);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_SIMPLETYPE, attrList);

        // for element "restriction" - local
        // already registered for complexType

        // for element "list" - local
        attrList = Container.getContainer(2);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // itemType = QName
        attrList.put(SchemaSymbols.ATT_ITEMTYPE, allAttrs[ATT_ITEMTYPE_N]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_LIST, attrList);

        // for element "union" - local
        attrList = Container.getContainer(2);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // memberTypes = List of QName
        attrList.put(SchemaSymbols.ATT_MEMBERTYPES, allAttrs[ATT_MEMBER_T_N]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_UNION, attrList);

        // for element "schema" - global
        attrList = Container.getContainer(8);
        // attributeFormDefault = (qualified | unqualified) : unqualified
        attrList.put(SchemaSymbols.ATT_ATTRIBUTEFORMDEFAULT, allAttrs[ATT_ATTRIBUTE_FD_D]);
        // blockDefault = (#all | List of (extension | restriction | substitution))  : ''
        attrList.put(SchemaSymbols.ATT_BLOCKDEFAULT, allAttrs[ATT_BLOCK_D_D]);
        // elementFormDefault = (qualified | unqualified) : unqualified
        attrList.put(SchemaSymbols.ATT_ELEMENTFORMDEFAULT, allAttrs[ATT_ELEMENT_FD_D]);
        // finalDefault = (#all | List of (extension | restriction | list | union))  : ''
        attrList.put(SchemaSymbols.ATT_FINALDEFAULT, allAttrs[ATT_FINAL_D_D]);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // targetNamespace = anyURI
        attrList.put(SchemaSymbols.ATT_TARGETNAMESPACE, allAttrs[ATT_TARGET_N_N]);
        // version = token
        attrList.put(SchemaSymbols.ATT_VERSION, allAttrs[ATT_VERSION_N]);
        // xml:lang = language
        attrList.put(SchemaSymbols.ATT_XML_LANG, allAttrs[ATT_XML_LANG]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_SCHEMA, attrList);

        // for element "include" - global
        attrList = Container.getContainer(2);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // schemaLocation = anyURI
        attrList.put(SchemaSymbols.ATT_SCHEMALOCATION, allAttrs[ATT_SCHEMA_L_R]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_INCLUDE, attrList);
        // for element "redefine" - global
        fEleAttrsMapG.put(SchemaSymbols.ELT_REDEFINE, attrList);

        // for element "import" - global
        attrList = Container.getContainer(3);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // namespace = anyURI
        attrList.put(SchemaSymbols.ATT_NAMESPACE, allAttrs[ATT_NAMESPACE_N]);
        // schemaLocation = anyURI
        attrList.put(SchemaSymbols.ATT_SCHEMALOCATION, allAttrs[ATT_SCHEMA_L_N]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_IMPORT, attrList);

        // for element "length" - local
        attrList = Container.getContainer(3);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // value = nonNegativeInteger
        attrList.put(SchemaSymbols.ATT_VALUE, allAttrs[ATT_VALUE_NNI_N]);
        // fixed = boolean : false
        attrList.put(SchemaSymbols.ATT_FIXED, allAttrs[ATT_FIXED_D]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_LENGTH, attrList);
        // for element "minLength" - local
        fEleAttrsMapL.put(SchemaSymbols.ELT_MINLENGTH, attrList);
        // for element "maxLength" - local
        fEleAttrsMapL.put(SchemaSymbols.ELT_MAXLENGTH, attrList);
        // for element "fractionDigits" - local
        fEleAttrsMapL.put(SchemaSymbols.ELT_FRACTIONDIGITS, attrList);

        // for element "totalDigits" - local
        attrList = Container.getContainer(3);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // value = positiveInteger
        attrList.put(SchemaSymbols.ATT_VALUE, allAttrs[ATT_VALUE_PI_N]);
        // fixed = boolean : false
        attrList.put(SchemaSymbols.ATT_FIXED, allAttrs[ATT_FIXED_D]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_TOTALDIGITS, attrList);

        // for element "pattern" - local
        attrList = Container.getContainer(2);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // value = string
        attrList.put(SchemaSymbols.ATT_VALUE, allAttrs[ATT_VALUE_STR_N]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_PATTERN, attrList);

        // for element "enumeration" - local
        attrList = Container.getContainer(2);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // value = anySimpleType
        attrList.put(SchemaSymbols.ATT_VALUE, allAttrs[ATT_VALUE_STR_N]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_ENUMERATION, attrList);

        // for element "whiteSpace" - local
        attrList = Container.getContainer(3);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // value = preserve | replace | collapse
        attrList.put(SchemaSymbols.ATT_VALUE, allAttrs[ATT_VALUE_WS_N]);
        // fixed = boolean : false
        attrList.put(SchemaSymbols.ATT_FIXED, allAttrs[ATT_FIXED_D]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_WHITESPACE, attrList);

        // for element "maxInclusive" - local
        attrList = Container.getContainer(3);
        // id = ID
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[ATT_ID_N]);
        // value = anySimpleType
        attrList.put(SchemaSymbols.ATT_VALUE, allAttrs[ATT_VALUE_STR_N]);
        // fixed = boolean : false
        attrList.put(SchemaSymbols.ATT_FIXED, allAttrs[ATT_FIXED_D]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_MAXINCLUSIVE, attrList);
        // for element "maxExclusive" - local
        fEleAttrsMapL.put(SchemaSymbols.ELT_MAXEXCLUSIVE, attrList);
        // for element "minInclusive" - local
        fEleAttrsMapL.put(SchemaSymbols.ELT_MININCLUSIVE, attrList);
        // for element "minExclusive" - local
        fEleAttrsMapL.put(SchemaSymbols.ELT_MINEXCLUSIVE, attrList);
    }

    // used to resolver namespace prefixes
    protected XSDHandler fSchemaHandler = null;

    // used to store symbols.
    protected SymbolTable fSymbolTable = null;

    // used to store the mapping from processed element to attributes
    protected Map<String, List<String>> fNonSchemaAttrs = new HashMap<>();

    // temprory vector, used to hold the namespace list
    protected List<String> fNamespaceList = new ArrayList<>();

    // whether this attribute appeared in the current element
    protected boolean[] fSeen = new boolean[ATTIDX_COUNT];
    private static boolean[] fSeenTemp = new boolean[ATTIDX_COUNT];

    // constructor. Sets fErrorReproter and get datatype validators
    public XSAttributeChecker(XSDHandler schemaHandler) {
        fSchemaHandler = schemaHandler;
    }

    public void reset(SymbolTable symbolTable) {
        fSymbolTable = symbolTable;
        fNonSchemaAttrs.clear();
    }

    /**
     * Check whether the specified element conforms to the attributes restriction
     * an array of attribute values is returned. the caller must call
     * <code>returnAttrArray</code> to return that array.
     *
     * @param element    which element to check
     * @param isGlobal   whether a child of &lt;schema&gt; or &lt;redefine&gt;
     * @param schemaDoc  the document where the element lives in
     * @return           an array containing attribute values
     */
    public Object[] checkAttributes(Element element, boolean isGlobal,
                                    XSDocumentInfo schemaDoc) {
        return checkAttributes(element, isGlobal, schemaDoc, false);
    }

    /**
     * Check whether the specified element conforms to the attributes restriction
     * an array of attribute values is returned. the caller must call
     * <code>returnAttrArray</code> to return that array. This method also takes
     * an extra parameter: if the element is "enumeration", whether to make a
     * copy of the namespace context, so that the value can be resolved as a
     * QName later.
     *
     * @param element      which element to check
     * @param isGlobal     whether a child of &lt;schema&gt; or &lt;redefine&gt;
     * @param schemaDoc    the document where the element lives in
     * @param enumAsQName  whether to tread enumeration value as QName
     * @return             an array containing attribute values
     */
    @SuppressWarnings("unchecked")
    public Object[] checkAttributes(Element element, boolean isGlobal,
                                    XSDocumentInfo schemaDoc, boolean enumAsQName) {
        if (element == null)
            return null;

        // get all attributes
        Attr[] attrs = DOMUtil.getAttrs(element);

        // update NamespaceSupport
        resolveNamespace(element, attrs, schemaDoc.fNamespaceSupport);

        String uri = DOMUtil.getNamespaceURI(element);
        String elName = DOMUtil.getLocalName(element);

        if (!SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(uri)) {
            reportSchemaError("s4s-elt-schema-ns", new Object[] {elName}, element);
        }

        Map<String, Container> eleAttrsMap = fEleAttrsMapG;
        String lookupName = elName;

        // REVISIT: only local element and attribute are different from others.
        //          it's possible to have either name or ref. all the others
        //          are only allowed to have one of name or ref, or neither of them.
        //          we'd better move such checking to the traverser.
        if (!isGlobal) {
            eleAttrsMap = fEleAttrsMapL;
            if (elName.equals(SchemaSymbols.ELT_ELEMENT)) {
                if (DOMUtil.getAttr(element, SchemaSymbols.ATT_REF) != null)
                    lookupName = ELEMENT_R;
                else
                    lookupName = ELEMENT_N;
            } else if (elName.equals(SchemaSymbols.ELT_ATTRIBUTE)) {
                if (DOMUtil.getAttr(element, SchemaSymbols.ATT_REF) != null)
                    lookupName = ATTRIBUTE_R;
                else
                    lookupName = ATTRIBUTE_N;
            }
        }

        // get desired attribute list of this element
        Container attrList = eleAttrsMap.get(lookupName);
        if (attrList == null) {
            // should never gets here.
            // when this method is called, the call already knows that
            // the element can appear.
            reportSchemaError ("s4s-elt-invalid", new Object[] {elName}, element);
            return null;
        }

        Object[] attrValues = getAvailableArray();
        long fromDefault = 0;

        // clear the "seen" flag.
        System.arraycopy(fSeenTemp, 0, fSeen, 0, ATTIDX_COUNT);

        // traverse all attributes
        int length = attrs.length;
        Attr sattr = null;
        for (int i = 0; i < length; i++) {
            sattr = attrs[i];
            // get the attribute name/value
            //String attrName = DOMUtil.getLocalName(sattr);
            String attrName = sattr.getName();
            String attrURI = DOMUtil.getNamespaceURI(sattr);
            String attrVal = DOMUtil.getValue(sattr);

            if (attrName.startsWith("xml")) {
                String attrPrefix = DOMUtil.getPrefix(sattr);
                // we don't want to add namespace declarations to the non-schema attributes
                if ("xmlns".equals(attrPrefix) || "xmlns".equals(attrName)) {
                    continue;
                }
                // Both <schema> and <documentation> may have an xml:lang attribute.
                // Set the URI for this attribute to null so that we process it
                // like any other schema attribute.
                else if (SchemaSymbols.ATT_XML_LANG.equals(attrName) &&
                        (SchemaSymbols.ELT_SCHEMA.equals(elName) ||
                                SchemaSymbols.ELT_DOCUMENTATION.equals(elName))) {
                    attrURI = null;
                }
            }

            // for attributes with namespace prefix
            //
            if (attrURI != null && attrURI.length() != 0) {
                // attributes with schema namespace are not allowed
                // and not allowed on "document" and "appInfo"
                if (attrURI.equals(SchemaSymbols.URI_SCHEMAFORSCHEMA)) {
                    reportSchemaError ("s4s-att-not-allowed", new Object[] {elName, attrName}, element);
                }
                else {
                    List<String> temp;
                    if(attrValues[ATTIDX_NONSCHEMA] == null) {
                        // these are usually small
                        temp = new ArrayList<>(4);
                    } else {
                        temp = (List<String>)attrValues[ATTIDX_NONSCHEMA];
                    }
                    temp.add(attrName);
                    temp.add(attrVal);
                    attrValues[ATTIDX_NONSCHEMA] = temp;
                    // for attributes from other namespace
                    // store them in a list, and TRY to validate them after
                    // schema traversal (because it's "lax")
                    //otherValues.put(attrName, attrVal);
                    // REVISIT:  actually use this some day...
                    // String attrRName = attrURI + "," + attrName;
                    // List<String> values = (ArrayList<String>)fNonSchemaAttrs.get(attrRName);
                    // if (values == null) {
                        // values = new ArrayList<>();
                        // values.add(attrName);
                        // values.add(elName);
                        // values.add(attrVal);
                        // fNonSchemaAttrs.put(attrRName, values);
                    // }
                    // else {
                        // values.add(elName);
                        // values.add(attrVal);
                    // }
                }
                continue;
            }

            // check whether this attribute is allowed
            OneAttr oneAttr = attrList.get(attrName);
            if (oneAttr == null) {
                reportSchemaError ("s4s-att-not-allowed",
                                   new Object[] {elName, attrName},
                                   element);
                continue;
            }

            // we've seen this attribute
            fSeen[oneAttr.valueIndex] = true;

            // check the value against the datatype
            try {
                // no checking on string needs to be done here.
                // no checking on xpath needs to be done here.
                // xpath values are validated in xpath parser
                if (oneAttr.dvIndex >= 0) {
                    if (oneAttr.dvIndex != DT_STRING &&
                        oneAttr.dvIndex != DT_XPATH &&
                        oneAttr.dvIndex != DT_XPATH1) {
                        XSSimpleType dv = fExtraDVs[oneAttr.dvIndex];
                        Object avalue = dv.validate(attrVal, schemaDoc.fValidationContext, null);
                        // kludge to handle chameleon includes/redefines...
                        if (oneAttr.dvIndex == DT_QNAME) {
                            QName qname = (QName)avalue;
                            if(qname.prefix == XMLSymbols.EMPTY_STRING && qname.uri == null && schemaDoc.fIsChameleonSchema)
                                qname.uri = schemaDoc.fTargetNamespace;
                        }
                        attrValues[oneAttr.valueIndex] = avalue;
                    } else {
                        attrValues[oneAttr.valueIndex] = attrVal;
                    }
                }
                else {
                    attrValues[oneAttr.valueIndex] = validate(attrValues, attrName, attrVal, oneAttr.dvIndex, schemaDoc);
                }
            } catch (InvalidDatatypeValueException ide) {
                reportSchemaError ("s4s-att-invalid-value",
                                   new Object[] {elName, attrName, ide.getMessage()},
                                   element);
                if (oneAttr.dfltValue != null)
                    //attrValues.put(attrName, oneAttr.dfltValue);
                    attrValues[oneAttr.valueIndex] = oneAttr.dfltValue;
            }

            // For "enumeration", and type is possible to be a QName, we need
            // to return namespace context for later QName resolution.
            if (elName.equals(SchemaSymbols.ELT_ENUMERATION) && enumAsQName) {
                attrValues[ATTIDX_ENUMNSDECLS] = new SchemaNamespaceSupport(schemaDoc.fNamespaceSupport);
            }
        }

        // apply default values
        OneAttr[] reqAttrs = attrList.values;
        for (int i = 0; i < reqAttrs.length; i++) {
            OneAttr oneAttr = reqAttrs[i];

            // if the attribute didn't apprear, and
            // if the attribute is optional with default value, apply it
            if (oneAttr.dfltValue != null && !fSeen[oneAttr.valueIndex]) {
                //attrValues.put(oneAttr.name, oneAttr.dfltValue);
                attrValues[oneAttr.valueIndex] = oneAttr.dfltValue;
                fromDefault |= (1<<oneAttr.valueIndex);
            }
        }

        attrValues[ATTIDX_FROMDEFAULT] = fromDefault;
        //attrValues[ATTIDX_OTHERVALUES] = otherValues;

        // Check that minOccurs isn't greater than maxOccurs.
        // p-props-correct 2.1
        if (attrValues[ATTIDX_MAXOCCURS] != null) {
            int min = ((XInt)attrValues[ATTIDX_MINOCCURS]).intValue();
            int max = ((XInt)attrValues[ATTIDX_MAXOCCURS]).intValue();
            if (max != SchemaSymbols.OCCURRENCE_UNBOUNDED) {

                // maxOccurLimit is only check in secure mode
                if (fSchemaHandler.fSecurityManager != null) {
                    String localName = element.getLocalName();

                // The maxOccurs restriction no longer applies to elements
                    // and wildcards in a sequence in which they are the only
                    // particle. These are now validated using a constant
                    // space algorithm. The restriction still applies to all
                    // other cases.

                    // Determine if constant-space algorithm can be applied
                    final boolean optimize =
                            (localName.equals("element") || localName.equals("any")) &&
                            (element.getNextSibling() == null) &&
                            (element.getPreviousSibling() == null) &&
                            (element.getParentNode().getLocalName().equals("sequence"));

                    if (!optimize) {
                    //Revisit :: IMO this is not right place to check
                    // maxOccurNodeLimit.
                    int maxOccurNodeLimit = fSchemaHandler.fSecurityManager.getLimit(XMLSecurityManager.Limit.MAX_OCCUR_NODE_LIMIT);
                    if (max > maxOccurNodeLimit && !fSchemaHandler.fSecurityManager.isNoLimit(maxOccurNodeLimit)) {
                        reportSchemaFatalError("MaxOccurLimit", new Object[] {maxOccurNodeLimit}, element);

                        // reset max values in case processing continues on error
                        attrValues[ATTIDX_MAXOCCURS] = fXIntPool.getXInt(maxOccurNodeLimit);
                                                //new Integer(maxOccurNodeLimit);
                        max = maxOccurNodeLimit;
                    }
                }
                }

                if (min > max) {
                    reportSchemaError ("p-props-correct.2.1",
                                       new Object[] {elName, attrValues[ATTIDX_MINOCCURS], attrValues[ATTIDX_MAXOCCURS]},
                                       element);
                    attrValues[ATTIDX_MINOCCURS] = attrValues[ATTIDX_MAXOCCURS];
                }
            }
        }

        return attrValues;
    }

    private Object validate(Object[] attrValues, String attr, String ivalue, int dvIndex,
                            XSDocumentInfo schemaDoc) throws InvalidDatatypeValueException {
        if (ivalue == null)
            return null;

        // To validate these types, we don't actually need to normalize the
        // strings. We only need to remove the whitespace from both ends.
        // In some special cases (list types), StringTokenizer can correctly
        // process the un-normalized whitespace.

        String value = XMLChar.trim(ivalue);
        Object retValue = null;
        List<QName> memberType;
        int choice;

        switch (dvIndex) {
        case DT_BOOLEAN:
            if (value.equals(SchemaSymbols.ATTVAL_FALSE) ||
                value.equals(SchemaSymbols.ATTVAL_FALSE_0)) {
                retValue = Boolean.FALSE;
            } else if (value.equals(SchemaSymbols.ATTVAL_TRUE) ||
                       value.equals(SchemaSymbols.ATTVAL_TRUE_1)) {
                retValue = Boolean.TRUE;
            } else {
                throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{value, "boolean"});
            }
            break;
        case DT_NONNEGINT:
            try {
                if (value.length() > 0 && value.charAt(0) == '+')
                    value = value.substring(1);
                retValue = fXIntPool.getXInt(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{value, "nonNegativeInteger"});
            }
            if (((XInt)retValue).intValue() < 0)
                throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{value, "nonNegativeInteger"});
            break;
        case DT_POSINT:
            try {
                if (value.length() > 0 && value.charAt(0) == '+')
                    value = value.substring(1);
                retValue = fXIntPool.getXInt(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{value, "positiveInteger"});
            }
            if (((XInt)retValue).intValue() <= 0)
                throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{value, "positiveInteger"});
            break;
        case DT_BLOCK:
            // block = (#all | List of (extension | restriction | substitution))
            choice = 0;
            if (value.equals (SchemaSymbols.ATTVAL_POUNDALL)) {
                choice = XSConstants.DERIVATION_SUBSTITUTION|XSConstants.DERIVATION_EXTENSION|
                         XSConstants.DERIVATION_RESTRICTION;
            }
            else {
                StringTokenizer t = new StringTokenizer(value, " \n\t\r");
                while (t.hasMoreTokens()) {
                    String token = t.nextToken ();

                    if (token.equals (SchemaSymbols.ATTVAL_EXTENSION)) {
                        choice |= XSConstants.DERIVATION_EXTENSION;
                    }
                    else if (token.equals (SchemaSymbols.ATTVAL_RESTRICTION)) {
                        choice |= XSConstants.DERIVATION_RESTRICTION;
                    }
                    else if (token.equals (SchemaSymbols.ATTVAL_SUBSTITUTION)) {
                        choice |= XSConstants.DERIVATION_SUBSTITUTION;
                    }
                    else {
                        throw new InvalidDatatypeValueException(
                                "cvc-datatype-valid.1.2.3", new Object[]{value,
                                    "(#all | List of (extension | restriction | substitution))"});
                    }
                }
            }
            retValue = fXIntPool.getXInt(choice);
            break;
        case DT_BLOCK1:
        case DT_FINAL:
            // block = (#all | List of (extension | restriction))
            // final = (#all | List of (extension | restriction))
            choice = 0;
            if (value.equals (SchemaSymbols.ATTVAL_POUNDALL)) {
                //choice = SchemaSymbols.EXTENSION|SchemaSymbols.RESTRICTION;
                // REVISIT: if #all, then make the result the combination of
                //          everything: substitution/externsion/restriction/list/union.
                //          would this be a problem?
                // the reason doing so is that when final/blockFinal on <schema>
                // is #all, it's not always the same as the conbination of those
                // values allowed by final/blockFinal.
                // for example, finalDefault="#all" is not always the same as
                // finalDefault="extension restriction".
                // if finalDefault="#all", final on any simple type would be
                // "extension restriction list union".
                choice = XSConstants.DERIVATION_SUBSTITUTION|XSConstants.DERIVATION_EXTENSION|
                         XSConstants.DERIVATION_RESTRICTION|XSConstants.DERIVATION_LIST|
                         XSConstants.DERIVATION_UNION;
            }
            else {
                StringTokenizer t = new StringTokenizer(value, " \n\t\r");
                while (t.hasMoreTokens()) {
                    String token = t.nextToken ();

                    if (token.equals (SchemaSymbols.ATTVAL_EXTENSION)) {
                        choice |= XSConstants.DERIVATION_EXTENSION;
                    }
                    else if (token.equals (SchemaSymbols.ATTVAL_RESTRICTION)) {
                        choice |= XSConstants.DERIVATION_RESTRICTION;
                    }
                    else {
                        throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.3", new Object[]{value, "(#all | List of (extension | restriction))"});
                    }
                }
            }
            retValue = fXIntPool.getXInt(choice);
            break;
        case DT_FINAL1:
            // final = (#all | List of (list | union | restriction))
            choice = 0;
            if (value.equals (SchemaSymbols.ATTVAL_POUNDALL)) {
                //choice = SchemaSymbols.RESTRICTION|SchemaSymbols.LIST|
                //         SchemaSymbols.UNION;
                // REVISIT: if #all, then make the result the combination of
                //          everything: substitution/externsion/restriction/list/union.
                //          would this be a problem?
                // same reason as above DT_BLOCK1/DT_FINAL
                choice = XSConstants.DERIVATION_SUBSTITUTION|XSConstants.DERIVATION_EXTENSION|
                         XSConstants.DERIVATION_RESTRICTION|XSConstants.DERIVATION_LIST|
                         XSConstants.DERIVATION_UNION;
            }
            else {
                StringTokenizer t = new StringTokenizer(value, " \n\t\r");
                while (t.hasMoreTokens()) {
                    String token = t.nextToken ();

                    if (token.equals (SchemaSymbols.ATTVAL_LIST)) {
                        choice |= XSConstants.DERIVATION_LIST;
                    }
                    else if (token.equals (SchemaSymbols.ATTVAL_UNION)) {
                        choice |= XSConstants.DERIVATION_UNION;
                    }
                    else if (token.equals (SchemaSymbols.ATTVAL_RESTRICTION)) {
                        choice |= XSConstants.DERIVATION_RESTRICTION;
                    }
                    else {
                        throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.3", new Object[]{value, "(#all | List of (list | union | restriction))"});
                    }
                }
            }
            retValue = fXIntPool.getXInt(choice);
            break;
        case DT_FINAL2:
            // finalDefault = (#all | List of (extension | restriction | list | union))
            choice = 0;
            if (value.equals (SchemaSymbols.ATTVAL_POUNDALL)) {
                //choice = SchemaSymbols.RESTRICTION|SchemaSymbols.LIST|
                //         SchemaSymbols.UNION;
                // REVISIT: if #all, then make the result the combination of
                //          everything: substitution/externsion/restriction/list/union.
                //          would this be a problem?
                // same reason as above DT_BLOCK1/DT_FINAL
                choice = XSConstants.DERIVATION_SUBSTITUTION|XSConstants.DERIVATION_EXTENSION|
                         XSConstants.DERIVATION_RESTRICTION|XSConstants.DERIVATION_LIST|
                         XSConstants.DERIVATION_UNION;
            }
            else {
                StringTokenizer t = new StringTokenizer(value, " \n\t\r");
                while (t.hasMoreTokens()) {
                    String token = t.nextToken ();

                    if (token.equals (SchemaSymbols.ATTVAL_EXTENSION)) {
                        choice |= XSConstants.DERIVATION_EXTENSION;
                    }
                    else if (token.equals (SchemaSymbols.ATTVAL_RESTRICTION)) {
                        choice |= XSConstants.DERIVATION_RESTRICTION;
                    }
                    else if (token.equals (SchemaSymbols.ATTVAL_LIST)) {
                        choice |= XSConstants.DERIVATION_LIST;
                    }
                    else if (token.equals (SchemaSymbols.ATTVAL_UNION)) {
                        choice |= XSConstants.DERIVATION_UNION;
                    }
                    else {
                        throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.3", new Object[]{value, "(#all | List of (extension | restriction | list | union))"});
                    }
                }
            }
            retValue = fXIntPool.getXInt(choice);
            break;
        case DT_FORM:
            // form = (qualified | unqualified)
            if (value.equals (SchemaSymbols.ATTVAL_QUALIFIED))
                retValue = INT_QUALIFIED;
            else if (value.equals (SchemaSymbols.ATTVAL_UNQUALIFIED))
                retValue = INT_UNQUALIFIED;
            else
                throw new InvalidDatatypeValueException("cvc-enumeration-valid",
                                                        new Object[]{value, "(qualified | unqualified)"});
            break;
        case DT_MAXOCCURS:
            // maxOccurs = (nonNegativeInteger | unbounded)
            if (value.equals(SchemaSymbols.ATTVAL_UNBOUNDED)) {
                retValue = INT_UNBOUNDED;
            } else {
                try {
                    retValue = validate(attrValues, attr, value, DT_NONNEGINT, schemaDoc);
                } catch (NumberFormatException e) {
                    throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.3", new Object[]{value, "(nonNegativeInteger | unbounded)"});
                }
            }
            break;
        case DT_MAXOCCURS1:
            // maxOccurs = 1
            if (value.equals("1"))
                retValue = fXIntPool.getXInt(1);
            else
                throw new InvalidDatatypeValueException("cvc-enumeration-valid",
                                                        new Object[]{value, "(1)"});
            break;
        case DT_MEMBERTYPES:
            // memberTypes = List of QName
            memberType = new ArrayList<>();
            try {
                StringTokenizer t = new StringTokenizer(value, " \n\t\r");
                while (t.hasMoreTokens()) {
                    String token = t.nextToken ();
                    QName qname = (QName)fExtraDVs[DT_QNAME].validate(token, schemaDoc.fValidationContext, null);
                    // kludge to handle chameleon includes/redefines...
                    if(qname.prefix == XMLSymbols.EMPTY_STRING && qname.uri == null && schemaDoc.fIsChameleonSchema)
                        qname.uri = schemaDoc.fTargetNamespace;
                    memberType.add(qname);
                }
                retValue = memberType;
            }
            catch (InvalidDatatypeValueException ide) {
                throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.2", new Object[]{value, "(List of QName)"});
            }
            break;
        case DT_MINOCCURS1:
            // minOccurs = (0 | 1)
            if (value.equals("0"))
                retValue = fXIntPool.getXInt(0);
            else if (value.equals("1"))
                retValue = fXIntPool.getXInt(1);
            else
                throw new InvalidDatatypeValueException("cvc-enumeration-valid",
                                                        new Object[]{value, "(0 | 1)"});
            break;
        case DT_NAMESPACE:
            // namespace = ((##any | ##other) | List of (anyURI | (##targetNamespace | ##local)) )
            if (value.equals(SchemaSymbols.ATTVAL_TWOPOUNDANY)) {
                // ##any
                retValue = INT_ANY_ANY;
            } else if (value.equals(SchemaSymbols.ATTVAL_TWOPOUNDOTHER)) {
                // ##other
                retValue = INT_ANY_NOT;
                String[] list = new String[2];
                list[0] = schemaDoc.fTargetNamespace;
                list[1] = null;
                attrValues[ATTIDX_NAMESPACE_LIST] = list;
            } else {
                // list
                retValue = INT_ANY_LIST;

                fNamespaceList.clear();

                // tokenize
                StringTokenizer tokens = new StringTokenizer(value, " \n\t\r");
                String token;
                String tempNamespace;
                try {
                    while (tokens.hasMoreTokens()) {
                        token = tokens.nextToken();
                        if (token.equals(SchemaSymbols.ATTVAL_TWOPOUNDLOCAL)) {
                            tempNamespace = null;
                        } else if (token.equals(SchemaSymbols.ATTVAL_TWOPOUNDTARGETNS)) {
                            tempNamespace = schemaDoc.fTargetNamespace;
                        } else {
                            // we have found namespace URI here
                            // need to add it to the symbol table
                            fExtraDVs[DT_ANYURI].validate(token, schemaDoc.fValidationContext, null);
                            tempNamespace = fSymbolTable.addSymbol(token);
                        }

                        //check for duplicate namespaces in the list
                        if (!fNamespaceList.contains(tempNamespace)) {
                            fNamespaceList.add(tempNamespace);
                        }
                    }
                } catch (InvalidDatatypeValueException ide) {
                    throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.3",
                            new Object[]{value,
                                "((##any | ##other) | List of (anyURI | (##targetNamespace | ##local)) )"});
                }

                // convert the vector to an array
                int num = fNamespaceList.size();
                String[] list = new String[num];
                list = fNamespaceList.toArray(list);
                attrValues[ATTIDX_NAMESPACE_LIST] = list;
            }
            break;
        case DT_PROCESSCONTENTS:
            // processContents = (lax | skip | strict)
            if (value.equals (SchemaSymbols.ATTVAL_STRICT))
                retValue = INT_ANY_STRICT;
            else if (value.equals (SchemaSymbols.ATTVAL_LAX))
                retValue = INT_ANY_LAX;
            else if (value.equals (SchemaSymbols.ATTVAL_SKIP))
                retValue = INT_ANY_SKIP;
            else
                throw new InvalidDatatypeValueException("cvc-enumeration-valid",
                                                        new Object[]{value, "(lax | skip | strict)"});
            break;
        case DT_USE:
            // use = (optional | prohibited | required)
            if (value.equals (SchemaSymbols.ATTVAL_OPTIONAL))
                retValue = INT_USE_OPTIONAL;
            else if (value.equals (SchemaSymbols.ATTVAL_REQUIRED))
                retValue = INT_USE_REQUIRED;
            else if (value.equals (SchemaSymbols.ATTVAL_PROHIBITED))
                retValue = INT_USE_PROHIBITED;
            else
                throw new InvalidDatatypeValueException("cvc-enumeration-valid",
                                                        new Object[]{value, "(optional | prohibited | required)"});
            break;
        case DT_WHITESPACE:
            // value = preserve | replace | collapse
            if (value.equals (SchemaSymbols.ATTVAL_PRESERVE))
                retValue = INT_WS_PRESERVE;
            else if (value.equals (SchemaSymbols.ATTVAL_REPLACE))
                retValue = INT_WS_REPLACE;
            else if (value.equals (SchemaSymbols.ATTVAL_COLLAPSE))
                retValue = INT_WS_COLLAPSE;
            else
                throw new InvalidDatatypeValueException("cvc-enumeration-valid",
                                                        new Object[]{value, "(preserve | replace | collapse)"});
            break;
        }

        return retValue;
    }

    void reportSchemaFatalError (String key, Object[] args, Element ele) {
        fSchemaHandler.reportSchemaFatalError(key, args, ele);
    }

    void reportSchemaError (String key, Object[] args, Element ele) {
        fSchemaHandler.reportSchemaError(key, args, ele);
    }

    // validate attriubtes from non-schema namespaces
    // REVISIT: why we store the attributes in this way? why not just a list
    //          of structure {element node, attr name/qname, attr value)?
    // REVISIT: pass the proper element node to reportSchemaError
    public void checkNonSchemaAttributes(XSGrammarBucket grammarBucket) {
        // for all attributes
        XSAttributeDecl attrDecl;
        for (Map.Entry<String, List<String>> entry : fNonSchemaAttrs.entrySet()) {
            // get name, uri, localpart
            String attrRName = entry.getKey();
            String attrURI = attrRName.substring(0,attrRName.indexOf(','));
            String attrLocal = attrRName.substring(attrRName.indexOf(',')+1);
            // find associated grammar
            SchemaGrammar sGrammar = grammarBucket.getGrammar(attrURI);
            if (sGrammar == null) {
                continue;
            }
            // and get the datatype validator, if there is one
            attrDecl = sGrammar.getGlobalAttributeDecl(attrLocal);
            if (attrDecl == null) {
                continue;
            }
            XSSimpleType dv = (XSSimpleType)attrDecl.getTypeDefinition();
            if (dv == null) {
                continue;
            }

            // get all values appeared with this attribute name
            List<String> values = entry.getValue();
            String elName;
            String attrName = values.get(0);
            // for each of the values
            int count = values.size();
            for (int i = 1; i < count; i += 2) {
                elName = values.get(i);
                try {
                    // and validate it using the XSSimpleType
                    // REVISIT: what would be the proper validation context?
                    //          guess we need to save that in the vectors too.
                    dv.validate(values.get(i+1), null, null);
                } catch(InvalidDatatypeValueException ide) {
                    reportSchemaError ("s4s-att-invalid-value",
                                       new Object[] {elName, attrName, ide.getMessage()},
                                       null);
                }
            }
        }
    }

    // normalize the string according to the whiteSpace facet
    public static String normalize(String content, short ws) {
        int len = content == null ? 0 : content.length();
        if (len == 0 || ws == XSSimpleType.WS_PRESERVE)
            return content;

        StringBuilder sb = new StringBuilder();
        if (ws == XSSimpleType.WS_REPLACE) {
            char ch;
            // when it's replace, just replace #x9, #xa, #xd by #x20
            for (int i = 0; i < len; i++) {
                ch = content.charAt(i);
                if (ch != 0x9 && ch != 0xa && ch != 0xd)
                    sb.append(ch);
                else
                    sb.append((char)0x20);
            }
        } else {
            char ch;
            int i;
            boolean isLeading = true;
            // when it's collapse
            for (i = 0; i < len; i++) {
                ch = content.charAt(i);
                // append real characters, so we passed leading ws
                if (ch != 0x9 && ch != 0xa && ch != 0xd && ch != 0x20) {
                    sb.append(ch);
                    isLeading = false;
                }
                else {
                    // for whitespaces, we skip all following ws
                    for (; i < len-1; i++) {
                        ch = content.charAt(i+1);
                        if (ch != 0x9 && ch != 0xa && ch != 0xd && ch != 0x20)
                            break;
                    }
                    // if it's not a leading or tailing ws, then append a space
                    if (i < len - 1 && !isLeading)
                        sb.append((char)0x20);
                }
            }
        }

        return sb.toString();
    }

    // the following part implements an attribute-value-array pool.
    // when checkAttribute is called, it calls getAvailableArray to get
    // an array from the pool; when the caller is done with the array,
    // it calls returnAttrArray to return that array to the pool.

    // initial size of the array pool. 10 is big enough
    static final int INIT_POOL_SIZE = 10;
    // the incremental size of the array pool
    static final int INC_POOL_SIZE  = 10;
    // the array pool
    Object[][] fArrayPool = new Object[INIT_POOL_SIZE][ATTIDX_COUNT];
    // used to clear the returned array
    // I think System.arrayCopy is more efficient than setting 35 fields to null
    private static Object[] fTempArray = new Object[ATTIDX_COUNT];
    // current position of the array pool (# of arrays not returned)
    int fPoolPos = 0;

    // get the next available array
    protected Object[] getAvailableArray() {
        // if no array left in the pool, increase the pool size
        if (fArrayPool.length == fPoolPos) {
            // increase size
            fArrayPool = new Object[fPoolPos+INC_POOL_SIZE][];
            // initialize each *new* array
            for (int i = fPoolPos; i < fArrayPool.length; i++)
                fArrayPool[i] = new Object[ATTIDX_COUNT];
        }
        // get the next available one
        Object[] retArray = fArrayPool[fPoolPos];
        // clear it from the pool. this is for GC: if a caller forget to
        // return the array, we want that array to be GCed.
        fArrayPool[fPoolPos++] = null;
        // to make sure that one array is not returned twice, we use
        // the last entry to indicate whether an array is already returned
        // now set it to false.
        System.arraycopy(fTempArray, 0, retArray, 0, ATTIDX_COUNT-1);
        retArray[ATTIDX_ISRETURNED] = Boolean.FALSE;

        return retArray;
    }

    // return an array back to the pool
    @SuppressWarnings("unchecked")
    public void returnAttrArray(Object[] attrArray, XSDocumentInfo schemaDoc) {
        // pop the namespace context
        if (schemaDoc != null)
            schemaDoc.fNamespaceSupport.popContext();

        // if 1. the pool is full; 2. the array is null;
        // 3. the array is of wrong size; 4. the array is already returned
        // then we can't accept this array to be returned
        if (fPoolPos == 0 ||
            attrArray == null ||
            attrArray.length != ATTIDX_COUNT ||
                ((Boolean)attrArray[ATTIDX_ISRETURNED])) {
            return;
        }

        // mark this array as returned
        attrArray[ATTIDX_ISRETURNED] = Boolean.TRUE;
        // better clear nonschema vector
        if(attrArray[ATTIDX_NONSCHEMA] != null)
            ((List<String>)attrArray[ATTIDX_NONSCHEMA]).clear();
        // and put it into the pool
        fArrayPool[--fPoolPos] = attrArray;
    }

    public void resolveNamespace(Element element, Attr[] attrs,
                                 SchemaNamespaceSupport nsSupport) {
        // push the namespace context
        nsSupport.pushContext();

        // search for new namespace bindings
        int length = attrs.length;
        Attr sattr = null;
        String rawname, prefix, uri;
        for (int i = 0; i < length; i++) {
            sattr = attrs[i];
            rawname = DOMUtil.getName(sattr);
            prefix = null;
            if (rawname.equals(XMLSymbols.PREFIX_XMLNS))
                prefix = XMLSymbols.EMPTY_STRING;
            else if (rawname.startsWith("xmlns:"))
                prefix = fSymbolTable.addSymbol(DOMUtil.getLocalName(sattr));
            if (prefix != null) {
                uri = fSymbolTable.addSymbol(DOMUtil.getValue(sattr));
                nsSupport.declarePrefix(prefix, uri.length()!=0 ? uri : null);
            }
        }
    }
}

class OneAttr {
    // name of the attribute
    public String name;
    // index of the datatype validator
    public int dvIndex;
    // whether it's optional, and has default value
    public int valueIndex;
    // the default value of this attribute
    public Object dfltValue;

    public OneAttr(String name, int dvIndex, int valueIndex, Object dfltValue) {
        this.name = name;
        this.dvIndex = dvIndex;
        this.valueIndex = valueIndex;
        this.dfltValue = dfltValue;
    }
}

abstract class Container {
    static final int THRESHOLD = 5;
    static Container getContainer(int size) {
        if (size > THRESHOLD)
            return new LargeContainer(size);
        else
            return new SmallContainer(size);
    }
    abstract void put(String key, OneAttr value);
    abstract OneAttr get(String key);

    OneAttr[] values;
    int pos = 0;
}

class SmallContainer extends Container {
    String[] keys;
    SmallContainer(int size) {
        keys = new String[size];
        values = new OneAttr[size];
    }
    void put(String key, OneAttr value) {
        keys[pos] = key;
        values[pos++] = value;
    }
    OneAttr get(String key) {
        for (int i = 0; i < pos; i++) {
            if (keys[i].equals(key)) {
                return values[i];
            }
        }
        return null;
    }
}

class LargeContainer extends Container {
    Map<String, OneAttr> items;
    LargeContainer(int size) {
        items = new HashMap<>(size*2+1);
        values = new OneAttr[size];
    }
    void put(String key, OneAttr value) {
        items.put(key, value);
        values[pos++] = value;
    }
    OneAttr get(String key) {
        OneAttr ret = items.get(key);
        return ret;
    }
}
