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
/*
 * $Id: Constants.java,v 1.7 2006/06/19 19:49:04 spericas Exp $
 */

package com.sun.org.apache.xalan.internal.xsltc.compiler;

import com.sun.org.apache.bcel.internal.generic.ArithmeticInstruction;
import com.sun.org.apache.bcel.internal.generic.ArrayInstruction;
import com.sun.org.apache.bcel.internal.generic.ConversionInstruction;
import com.sun.org.apache.bcel.internal.generic.Instruction;
import com.sun.org.apache.bcel.internal.generic.InstructionConst;
import com.sun.org.apache.bcel.internal.generic.LocalVariableInstruction;
import com.sun.org.apache.bcel.internal.generic.ReturnInstruction;
import com.sun.org.apache.bcel.internal.generic.StackInstruction;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
public interface Constants {
    public static final Instruction ACONST_NULL = InstructionConst.ACONST_NULL;
    public static final Instruction ATHROW = InstructionConst.ATHROW;
    public static final Instruction DCMPG = InstructionConst.DCMPG;
    public static final Instruction DCONST_0 = InstructionConst.DCONST_0;
    public static final Instruction ICONST_0 = InstructionConst.ICONST_0;
    public static final Instruction ICONST_1 = InstructionConst.ICONST_1;
    public static final Instruction NOP = InstructionConst.NOP;


    public static final StackInstruction DUP = InstructionConst.DUP;
    public static final StackInstruction DUP2 = InstructionConst.DUP2;
    public static final StackInstruction DUP_X1 = InstructionConst.DUP_X1;
    public static final StackInstruction DUP_X2 = InstructionConst.DUP_X2;
    public static final StackInstruction POP = InstructionConst.POP;
    public static final StackInstruction POP2 = InstructionConst.POP2;
    public static final StackInstruction SWAP = InstructionConst.SWAP;

    public static final LocalVariableInstruction ALOAD_0 = InstructionConst.ALOAD_0;
    public static final LocalVariableInstruction ALOAD_1 = InstructionConst.ALOAD_1;
    public static final LocalVariableInstruction ALOAD_2 = InstructionConst.ALOAD_2;
    public static final LocalVariableInstruction ILOAD_1 = InstructionConst.ILOAD_1;
    public static final LocalVariableInstruction ILOAD_2 = InstructionConst.ILOAD_2;

    public static final ArithmeticInstruction DADD = InstructionConst.DADD;
    public static final ArithmeticInstruction IXOR = InstructionConst.IXOR;

    public static final ArrayInstruction AASTORE = InstructionConst.AASTORE;
    public static final ArrayInstruction IASTORE = InstructionConst.IASTORE;

    public static final ConversionInstruction D2F = InstructionConst.D2F;
    public static final ConversionInstruction D2I = InstructionConst.D2I;
    public static final ConversionInstruction D2L = InstructionConst.D2L;
    public static final ConversionInstruction F2D = InstructionConst.F2D;
    public static final ConversionInstruction I2B = InstructionConst.I2B;
    public static final ConversionInstruction I2C = InstructionConst.I2C;
    public static final ConversionInstruction I2D = InstructionConst.I2D;
    public static final ConversionInstruction I2F = InstructionConst.I2F;
    public static final ConversionInstruction I2L = InstructionConst.I2L;
    public static final ConversionInstruction I2S = InstructionConst.I2S;
    public static final ConversionInstruction L2D = InstructionConst.L2D;
    public static final ConversionInstruction L2I = InstructionConst.L2I;


    public static final ReturnInstruction ARETURN = InstructionConst.ARETURN;
    public static final ReturnInstruction IRETURN = InstructionConst.IRETURN;
    public static final ReturnInstruction RETURN = InstructionConst.RETURN;



    // Error categories used to report errors to Parser.reportError()

    // Unexpected internal errors, such as null-ptr exceptions, etc.
    // Immediately terminates compilation, no translet produced
    public final int INTERNAL        = 0;
    // XSLT elements that are not implemented and unsupported ext.
    // Immediately terminates compilation, no translet produced
    public final int UNSUPPORTED     = 1;
    // Fatal error in the stylesheet input (parsing or content)
    // Immediately terminates compilation, no translet produced
    public final int FATAL           = 2;
    // Other error in the stylesheet input (parsing or content)
    // Does not terminate compilation, no translet produced
    public final int ERROR           = 3;
    // Other error in the stylesheet input (content errors only)
    // Does not terminate compilation, a translet is produced
    public final int WARNING         = 4;

    public static final String EMPTYSTRING = "";

    public static final String NAMESPACE_FEATURE =
        "http://xml.org/sax/features/namespaces";

    public static final String TRANSLET_INTF
        = "com.sun.org.apache.xalan.internal.xsltc.Translet";
    public static final String TRANSLET_INTF_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/Translet;";

    public static final String ATTRIBUTES_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/runtime/Attributes;";
    public static final String NODE_ITERATOR_SIG
        = "Lcom/sun/org/apache/xml/internal/dtm/DTMAxisIterator;";
    public static final String DOM_INTF_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/DOM;";
    public static final String DOM_IMPL_CLASS
        = "com/sun/org/apache/xalan/internal/xsltc/DOM"; // xml/dtm/ref/DTMDefaultBaseIterators"; //xalan/xsltc/dom/DOMImpl";
        public static final String SAX_IMPL_CLASS
        = "com/sun/org/apache/xalan/internal/xsltc/DOM/SAXImpl";
    public static final String DOM_IMPL_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/SAXImpl;"; //xml/dtm/ref/DTMDefaultBaseIterators"; //xalan/xsltc/dom/DOMImpl;";
        public static final String SAX_IMPL_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/SAXImpl;";
    public static final String DOM_ADAPTER_CLASS
        = "com/sun/org/apache/xalan/internal/xsltc/dom/DOMAdapter";
    public static final String DOM_ADAPTER_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/DOMAdapter;";
    public static final String MULTI_DOM_CLASS
        = "com.sun.org.apache.xalan.internal.xsltc.dom.MultiDOM";
    public static final String MULTI_DOM_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/MultiDOM;";

    public static final String STRING
        = "java.lang.String";

    public static final int ACC_PUBLIC
        = com.sun.org.apache.bcel.internal.Const.ACC_PUBLIC;
    public static final int ACC_SUPER
        = com.sun.org.apache.bcel.internal.Const.ACC_SUPER;
    public static final int ACC_FINAL
        = com.sun.org.apache.bcel.internal.Const.ACC_FINAL;
    public static final int ACC_PRIVATE
        = com.sun.org.apache.bcel.internal.Const.ACC_PRIVATE;
    public static final int ACC_PROTECTED
        = com.sun.org.apache.bcel.internal.Const.ACC_PROTECTED;
    public static final int ACC_STATIC
        = com.sun.org.apache.bcel.internal.Const.ACC_STATIC;

    public static final String MODULE_SIG
        = "Ljava/lang/Module;";
    public static final String CLASS_SIG
        = "Ljava/lang/Class;";
    public static final String STRING_SIG
        = "Ljava/lang/String;";
    public static final String STRING_BUFFER_SIG
        = "Ljava/lang/StringBuffer;";
    public static final String OBJECT_SIG
        = "Ljava/lang/Object;";
    public static final String DOUBLE_SIG
        = "Ljava/lang/Double;";
    public static final String INTEGER_SIG
        = "Ljava/lang/Integer;";
    public static final String COLLATOR_CLASS
        = "java/text/Collator";
    public static final String COLLATOR_SIG
        = "Ljava/text/Collator;";

    public static final String NODE
        = "int";
    public static final String NODE_ITERATOR
        = "com.sun.org.apache.xml.internal.dtm.DTMAxisIterator";
    public static final String NODE_ITERATOR_BASE
        = "com.sun.org.apache.xml.internal.dtm.ref.DTMAxisIteratorBase";
    public static final String SORT_ITERATOR
        = "com.sun.org.apache.xalan.internal.xsltc.dom.SortingIterator";
    public static final String SORT_ITERATOR_SIG
        = "Lcom.sun.org.apache.xalan.internal.xsltc.dom.SortingIterator;";
    public static final String NODE_SORT_RECORD
        = "com.sun.org.apache.xalan.internal.xsltc.dom.NodeSortRecord";
    public static final String NODE_SORT_FACTORY
        = "com/sun/org/apache/xalan/internal/xsltc/dom/NodeSortRecordFactory";
    public static final String NODE_SORT_RECORD_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/NodeSortRecord;";
    public static final String NODE_SORT_FACTORY_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/NodeSortRecordFactory;";
    public static final String LOCALE_CLASS
        = "java.util.Locale";
    public static final String LOCALE_SIG
        = "Ljava/util/Locale;";
    public static final String STRING_VALUE_HANDLER
        = "com.sun.org.apache.xalan.internal.xsltc.runtime.StringValueHandler";
    public static final String STRING_VALUE_HANDLER_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/runtime/StringValueHandler;";
    public static final String OUTPUT_HANDLER
        = "com/sun/org/apache/xml/internal/serializer/SerializationHandler";
    public static final String OUTPUT_HANDLER_SIG
        = "Lcom/sun/org/apache/xml/internal/serializer/SerializationHandler;";
    public static final String FILTER_INTERFACE
        = "com.sun.org.apache.xalan.internal.xsltc.dom.Filter";
    public static final String FILTER_INTERFACE_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/Filter;";
    public static final String UNION_ITERATOR_CLASS
        = "com.sun.org.apache.xalan.internal.xsltc.dom.UnionIterator";
    public static final String STEP_ITERATOR_CLASS
        = "com.sun.org.apache.xalan.internal.xsltc.dom.StepIterator";
    public static final String CACHED_NODE_LIST_ITERATOR_CLASS
        = "com.sun.org.apache.xalan.internal.xsltc.dom.CachedNodeListIterator";
    public static final String NTH_ITERATOR_CLASS
        = "com.sun.org.apache.xalan.internal.xsltc.dom.NthIterator";
    public static final String ABSOLUTE_ITERATOR
        = "com.sun.org.apache.xalan.internal.xsltc.dom.AbsoluteIterator";
    public static final String DUP_FILTERED_ITERATOR
        = "com.sun.org.apache.xalan.internal.xsltc.dom.DupFilterIterator";
    public static final String CURRENT_NODE_LIST_ITERATOR
        = "com.sun.org.apache.xalan.internal.xsltc.dom.CurrentNodeListIterator";
    public static final String CURRENT_NODE_LIST_FILTER
        = "com.sun.org.apache.xalan.internal.xsltc.dom.CurrentNodeListFilter";
    public static final String CURRENT_NODE_LIST_ITERATOR_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/CurrentNodeListIterator;";
    public static final String CURRENT_NODE_LIST_FILTER_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/CurrentNodeListFilter;";
    public static final String FILTER_STEP_ITERATOR
        = "com.sun.org.apache.xalan.internal.xsltc.dom.FilteredStepIterator";
    public static final String FILTER_ITERATOR
        = "com.sun.org.apache.xalan.internal.xsltc.dom.FilterIterator";
    public static final String SINGLETON_ITERATOR
        = "com.sun.org.apache.xalan.internal.xsltc.dom.SingletonIterator";
    public static final String MATCHING_ITERATOR
        = "com.sun.org.apache.xalan.internal.xsltc.dom.MatchingIterator";
    public static final String NODE_SIG
        = "I";
    public static final String GET_PARENT
        = "getParent";
    public static final String GET_PARENT_SIG
        = "(" + NODE_SIG + ")" + NODE_SIG;
    public static final String NEXT_SIG
        = "()" + NODE_SIG;
    public static final String NEXT
        = "next";
        public static final String NEXTID
        = "nextNodeID";
    public static final String MAKE_NODE
        = "makeNode";
    public static final String MAKE_NODE_LIST
        = "makeNodeList";
    public static final String GET_UNPARSED_ENTITY_URI
        = "getUnparsedEntityURI";
    public static final String STRING_TO_REAL
        = "stringToReal";
    public static final String STRING_TO_REAL_SIG
        = "(" + STRING_SIG + ")D";
    public static final String STRING_TO_INT
        = "stringToInt";
    public static final String STRING_TO_INT_SIG
        = "(" + STRING_SIG + ")I";

    public static final String XSLT_PACKAGE
        = "com.sun.org.apache.xalan.internal.xsltc";
    public static final String COMPILER_PACKAGE
        = XSLT_PACKAGE + ".compiler";
    public static final String RUNTIME_PACKAGE
        = XSLT_PACKAGE + ".runtime";
    public static final String TRANSLET_CLASS
        = RUNTIME_PACKAGE + ".AbstractTranslet";

    public static final String TRANSLET_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/runtime/AbstractTranslet;";
    public static final String UNION_ITERATOR_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/UnionIterator;";
    public static final String TRANSLET_OUTPUT_SIG
        = "Lcom/sun/org/apache/xml/internal/serializer/SerializationHandler;";
    public static final String MAKE_NODE_SIG
        = "(I)Lorg/w3c/dom/Node;";
    public static final String MAKE_NODE_SIG2
        = "(" + NODE_ITERATOR_SIG + ")Lorg/w3c/dom/Node;";
    public static final String MAKE_NODE_LIST_SIG
        = "(I)Lorg/w3c/dom/NodeList;";
    public static final String MAKE_NODE_LIST_SIG2
        = "(" + NODE_ITERATOR_SIG + ")Lorg/w3c/dom/NodeList;";

    public static final String STREAM_XML_OUTPUT
    = "com.sun.org.apache.xml.internal.serializer.ToXMLStream";

    public static final String OUTPUT_BASE
    = "com.sun.org.apache.xml.internal.serializer.SerializerBase";

    public static final String LOAD_DOCUMENT_CLASS
        = "com.sun.org.apache.xalan.internal.xsltc.dom.LoadDocument";

    public static final String KEY_INDEX_CLASS
        = "com/sun/org/apache/xalan/internal/xsltc/dom/KeyIndex";
    public static final String KEY_INDEX_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/KeyIndex;";

    public static final String KEY_INDEX_ITERATOR_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/KeyIndex$KeyIndexIterator;";
    public static final String DOM_INTF
        = "com.sun.org.apache.xalan.internal.xsltc.DOM";
    public static final String DOM_IMPL
        = "com.sun.org.apache.xalan.internal.xsltc.dom.SAXImpl";
    public static final String SAX_IMPL
        = "com.sun.org.apache.xalan.internal.xsltc.dom.SAXImpl";
    public static final String CLASS_CLASS
        = "java.lang.Class";
    public static final String MODULE_CLASS
        = "java.lang.Module";
    public static final String STRING_CLASS
        = "java.lang.String";
    public static final String OBJECT_CLASS
        = "java.lang.Object";
    public static final String BOOLEAN_CLASS
        = "java.lang.Boolean";
    public static final String STRING_BUFFER_CLASS
        = "java.lang.StringBuffer";
    public static final String STRING_WRITER
        = "java.io.StringWriter";
    public static final String WRITER_SIG
        = "Ljava/io/Writer;";

    public static final String TRANSLET_OUTPUT_BASE
        = "com.sun.org.apache.xalan.internal.xsltc.TransletOutputBase";
    // output interface
    public static final String TRANSLET_OUTPUT_INTERFACE
        = "com.sun.org.apache.xml.internal.serializer.SerializationHandler";
    public static final String BASIS_LIBRARY_CLASS
        = "com.sun.org.apache.xalan.internal.xsltc.runtime.BasisLibrary";
    public static final String ATTRIBUTE_LIST_IMPL_CLASS
        = "com.sun.org.apache.xalan.internal.xsltc.runtime.AttributeListImpl";
    public static final String DOUBLE_CLASS
        = "java.lang.Double";
    public static final String INTEGER_CLASS
        = "java.lang.Integer";
    public static final String RUNTIME_NODE_CLASS
        = "com.sun.org.apache.xalan.internal.xsltc.runtime.Node";
    public static final String MATH_CLASS
        = "java.lang.Math";

    public static final String BOOLEAN_VALUE
        = "booleanValue";
    public static final String BOOLEAN_VALUE_SIG
        = "()Z";
    public static final String INT_VALUE
        = "intValue";
    public static final String INT_VALUE_SIG
        = "()I";
    public static final String DOUBLE_VALUE
        = "doubleValue";
    public static final String DOUBLE_VALUE_SIG
        = "()D";

    public static final String DOM_PNAME
        = "dom";
    public static final String NODE_PNAME
        = "node";
    public static final String TRANSLET_OUTPUT_PNAME
        = "handler";
    public static final String ITERATOR_PNAME
        = "iterator";
    public static final String DOCUMENT_PNAME
        = "document";
    public static final String TRANSLET_PNAME
        = "translet";

    public static final String INVOKE_METHOD
        = "invokeMethod";
    public static final String GET_NODE_NAME
        = "getNodeNameX";
    public static final String CHARACTERSW
        = "characters";
    public static final String GET_CHILDREN
        = "getChildren";
    public static final String GET_TYPED_CHILDREN
        = "getTypedChildren";
    public static final String CHARACTERS
        = "characters";
    public static final String APPLY_TEMPLATES
        = "applyTemplates";
    public static final String GET_NODE_TYPE
        = "getNodeType";
    public static final String GET_NODE_VALUE
        = "getStringValueX";
    public static final String GET_ELEMENT_VALUE
        = "getElementValue";
    public static final String GET_ATTRIBUTE_VALUE
        = "getAttributeValue";
    public static final String HAS_ATTRIBUTE
        = "hasAttribute";
    public static final String ADD_ITERATOR
        = "addIterator";
    public static final String SET_START_NODE
        = "setStartNode";
    public static final String RESET
        = "reset";
    public static final String GET_MODULE
        = "getModule";
    public static final String FOR_NAME
        = "forName";
    public static final String ADD_READS
        = "addReads";

    public static final String GET_MODULE_SIG
        = "()" + MODULE_SIG;
    public static final String FOR_NAME_SIG
        = "(" + STRING_SIG + ")" + CLASS_SIG;
    public static final String ADD_READS_SIG
        = "(" + MODULE_SIG + ")" + MODULE_SIG;

    public static final String ATTR_SET_SIG
        = "(" + DOM_INTF_SIG  + NODE_ITERATOR_SIG + TRANSLET_OUTPUT_SIG + "I)V";

    public static final String GET_NODE_NAME_SIG
        = "(" + NODE_SIG + ")" + STRING_SIG;
    public static final String CHARACTERSW_SIG
        = "("  + STRING_SIG + TRANSLET_OUTPUT_SIG + ")V";
    public static final String CHARACTERS_SIG
        = "(" + NODE_SIG + TRANSLET_OUTPUT_SIG + ")V";
    public static final String GET_CHILDREN_SIG
        = "(" + NODE_SIG +")" + NODE_ITERATOR_SIG;
    public static final String GET_TYPED_CHILDREN_SIG
        = "(I)" + NODE_ITERATOR_SIG;
    public static final String GET_NODE_TYPE_SIG
        = "()S";
    public static final String GET_NODE_VALUE_SIG
        = "(I)" + STRING_SIG;
    public static final String GET_ELEMENT_VALUE_SIG
        = "(I)" + STRING_SIG;
    public static final String GET_ATTRIBUTE_VALUE_SIG
        = "(II)" + STRING_SIG;
    public static final String HAS_ATTRIBUTE_SIG
        = "(II)Z";
    public static final String GET_ITERATOR_SIG
        = "()" + NODE_ITERATOR_SIG;

    public static final String NAMES_INDEX
        = "namesArray";
    public static final String NAMES_INDEX_SIG
        = "[" + STRING_SIG;
    public static final String URIS_INDEX
       = "urisArray";
    public static final String URIS_INDEX_SIG
       = "[" + STRING_SIG;
    public static final String TYPES_INDEX
       = "typesArray";
    public static final String TYPES_INDEX_SIG
       = "[I";
    public static final String NAMESPACE_INDEX
        = "namespaceArray";
    public static final String NAMESPACE_INDEX_SIG
        = "[" + STRING_SIG;
    public static final String HASIDCALL_INDEX
        = "_hasIdCall";
    public static final String HASIDCALL_INDEX_SIG
        = "Z";
    public static final String TRANSLET_VERSION_INDEX
        = "transletVersion";
    public static final String TRANSLET_VERSION_INDEX_SIG
        = "I";

    public static final String DOM_FIELD
        = "_dom";
    public static final String STATIC_NAMES_ARRAY_FIELD
        = "_sNamesArray";
    public static final String STATIC_URIS_ARRAY_FIELD
        = "_sUrisArray";
    public static final String STATIC_TYPES_ARRAY_FIELD
        = "_sTypesArray";
    public static final String STATIC_NAMESPACE_ARRAY_FIELD
        = "_sNamespaceArray";
    public static final String STATIC_CHAR_DATA_FIELD
        = "_scharData";
    public static final String STATIC_CHAR_DATA_FIELD_SIG
        = "[C";
    public static final String FORMAT_SYMBOLS_FIELD
        = "format_symbols";

    public static final String ITERATOR_FIELD_SIG
        = NODE_ITERATOR_SIG;
    public static final String NODE_FIELD
        = "node";
    public static final String NODE_FIELD_SIG
        = "I";

    public static final String EMPTYATTR_FIELD
        = "EmptyAttributes";
    public static final String ATTRIBUTE_LIST_FIELD
        = "attributeList";
    public static final String CLEAR_ATTRIBUTES
        = "clear";
    public static final String ADD_ATTRIBUTE
        = "addAttribute";
    public static final String ATTRIBUTE_LIST_IMPL_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/runtime/AttributeListImpl;";
    public static final String CLEAR_ATTRIBUTES_SIG
        = "()" + ATTRIBUTE_LIST_IMPL_SIG;
    public static final String ADD_ATTRIBUTE_SIG
        = "(" + STRING_SIG + STRING_SIG + ")" + ATTRIBUTE_LIST_IMPL_SIG;

    public static final String ADD_ITERATOR_SIG
        = "(" + NODE_ITERATOR_SIG +")" + UNION_ITERATOR_SIG;

    public static final String ORDER_ITERATOR
        = "orderNodes";
    public static final String ORDER_ITERATOR_SIG
        = "("+NODE_ITERATOR_SIG+"I)"+NODE_ITERATOR_SIG;

    public static final String SET_START_NODE_SIG
        = "(" + NODE_SIG + ")" + NODE_ITERATOR_SIG;

    public static final String NODE_COUNTER
        = "com.sun.org.apache.xalan.internal.xsltc.dom.NodeCounter";
    public static final String NODE_COUNTER_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/NodeCounter;";
    public static final String DEFAULT_NODE_COUNTER
        = "com.sun.org.apache.xalan.internal.xsltc.dom.DefaultNodeCounter";
    public static final String DEFAULT_NODE_COUNTER_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/dom/DefaultNodeCounter;";
    public static final String TRANSLET_FIELD
        = "translet";
    public static final String TRANSLET_FIELD_SIG
        = TRANSLET_SIG;

    public static final String RESET_SIG
        = "()" + NODE_ITERATOR_SIG;
    public static final String GET_PARAMETER
        = "getParameter";
    public static final String ADD_PARAMETER
        = "addParameter";
    public static final String PUSH_PARAM_FRAME
        = "pushParamFrame";
    public static final String PUSH_PARAM_FRAME_SIG
        = "()V";
    public static final String POP_PARAM_FRAME
        = "popParamFrame";
    public static final String POP_PARAM_FRAME_SIG
        = "()V";
    public static final String GET_PARAMETER_SIG
        = "(" + STRING_SIG + ")" + OBJECT_SIG;
    public static final String ADD_PARAMETER_SIG
        = "(" + STRING_SIG + OBJECT_SIG + "Z)" + OBJECT_SIG;

    public static final String STRIP_SPACE
        = "stripSpace";
    public static final String STRIP_SPACE_INTF
        = "com/sun/org/apache/xalan/internal/xsltc/StripFilter";
    public static final String STRIP_SPACE_SIG
        = "Lcom/sun/org/apache/xalan/internal/xsltc/StripFilter;";
    public static final String STRIP_SPACE_PARAMS
        = "(Lcom/sun/org/apache/xalan/internal/xsltc/DOM;II)Z";

    public static final String GET_NODE_VALUE_ITERATOR
        = "getNodeValueIterator";
    public static final String GET_NODE_VALUE_ITERATOR_SIG
        = "("+NODE_ITERATOR_SIG+"I"+STRING_SIG+"Z)"+NODE_ITERATOR_SIG;

    public static final String GET_UNPARSED_ENTITY_URI_SIG
        = "("+STRING_SIG+")"+STRING_SIG;

    public static final int POSITION_INDEX = 2;
    public static final int LAST_INDEX     = 3;

    public static final String XMLNS_PREFIX = "xmlns";
    public static final String XMLNS_STRING = "xmlns:";
    public static final String XMLNS_URI
        = "http://www.w3.org/2000/xmlns/";
    public static final String XSLT_URI
        = "http://www.w3.org/1999/XSL/Transform";
    public static final String XHTML_URI
        = "http://www.w3.org/1999/xhtml";
    public static final String TRANSLET_URI
        = "http://xml.apache.org/xalan/xsltc";
    public static final String REDIRECT_URI
        = "http://xml.apache.org/xalan/redirect";
    public static final String FALLBACK_CLASS
        = "com.sun.org.apache.xalan.internal.xsltc.compiler.Fallback";

    public static final int RTF_INITIAL_SIZE = 32;

    // the API packages used by generated translet classes
    public static String[] PKGS_USED_BY_TRANSLET_CLASSES = {
        "com.sun.org.apache.xalan.internal.lib",
        "com.sun.org.apache.xalan.internal.xsltc",
        "com.sun.org.apache.xalan.internal.xsltc.runtime",
        "com.sun.org.apache.xalan.internal.xsltc.dom",
        "com.sun.org.apache.xml.internal.serializer",
        "com.sun.org.apache.xml.internal.dtm",
        "com.sun.org.apache.xml.internal.dtm.ref",
    };
}
