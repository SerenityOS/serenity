/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Commonly used constants.
 *
 * @xerces.internal
 *
 * @author Andy Clark, IBM
 *
 * @LastModified: May 2021
 */
public final class Constants {

    //
    // Constants
    //
    // Schema Types:
    public static final String NS_XMLSCHEMA = "http://www.w3.org/2001/XMLSchema".intern();
    public static final String NS_DTD = "http://www.w3.org/TR/REC-xml".intern();

    // Schema Versions:
    public static final String W3C_XML_SCHEMA10_NS_URI = "http://www.w3.org/XML/XMLSchema/v1.0".intern();

    // Schema features
    public static final String SUN_SCHEMA_FEATURE_PREFIX = "http://java.sun.com/xml/schema/features/";
    public static final String SUN_REPORT_IGNORED_ELEMENT_CONTENT_WHITESPACE = "report-ignored-element-content-whitespace";

    //stax properties

    public static final String ZEPHYR_PROPERTY_PREFIX = "http://java.sun.com/xml/stream/properties/" ;
    public static final String STAX_PROPERTIES = "stax-properties" ;
    public static final String STAX_ENTITY_RESOLVER_PROPERTY = "internal/stax-entity-resolver";
    public static final String STAX_REPORT_CDATA_EVENT = "report-cdata-event";
    public static final String READER_IN_DEFINED_STATE = ZEPHYR_PROPERTY_PREFIX + "reader-in-defined-state" ;
    public static final String ADD_NAMESPACE_DECL_AS_ATTRIBUTE = "add-namespacedecl-as-attrbiute";
    public static final String ESCAPE_CHARACTERS = "escapeCharacters";
    public static final String REUSE_INSTANCE = "reuse-instance" ;

    //DOM properties
    public static final String SUN_DOM_PROPERTY_PREFIX = "http://java.sun.com/xml/dom/properties/" ;
    public static final String SUN_DOM_ANCESTOR_CHECCK = "ancestor-check";

    /**
     * If true, ignore DOCTYPE declaration as if it wasn't present at all.
     * Note that this is a violation of the XML recommendation.
     * The full property name is prefixed by {@link #ZEPHYR_PROPERTY_PREFIX}.
     */
    public static final String IGNORE_EXTERNAL_DTD = "ignore-external-dtd";

    // sax features

    /** SAX feature prefix ("http://xml.org/sax/features/"). */
    public static final String SAX_FEATURE_PREFIX = "http://xml.org/sax/features/";

    public static final String NAMESPACES_FEATURE = "namespaces";

    /** Namespace prefixes feature ("namespace-prefixes"). */
    public static final String NAMESPACE_PREFIXES_FEATURE = "namespace-prefixes";

    /** String interning feature ("string-interning"). */
    public static final String STRING_INTERNING_FEATURE = "string-interning";

    /** Validation feature ("validation"). */
    public static final String VALIDATION_FEATURE = "validation";

    /** External general entities feature ("external-general-entities "). */
    public static final String EXTERNAL_GENERAL_ENTITIES_FEATURE = "external-general-entities";

    /** External parameter entities feature ("external-parameter-entities "). */
    public static final String EXTERNAL_PARAMETER_ENTITIES_FEATURE = "external-parameter-entities";

    /** Lexical handler parameter entities feature ("lexical-handler/parameter-entities"). */
    public static final String LEXICAL_HANDLER_PARAMETER_ENTITIES_FEATURE = "lexical-handler/parameter-entities";

    /** Is standalone feature ("is-standalone"). */
    public static final String IS_STANDALONE_FEATURE = "is-standalone";

    /** Resolve DTD URIs feature ("resolve-dtd-uris"). */
    public static final String RESOLVE_DTD_URIS_FEATURE = "resolve-dtd-uris";

    /** Use Attributes2 feature ("use-attributes2"). */
    public static final String USE_ATTRIBUTES2_FEATURE = "use-attributes2";

    /** Use Locator2 feature ("use-locator2"). */
    public static final String USE_LOCATOR2_FEATURE = "use-locator2";

    /** Use EntityResolver2 feature ("use-entity-resolver2"). */
    public static final String USE_ENTITY_RESOLVER2_FEATURE = "use-entity-resolver2";

    /** Unicode normalization checking feature ("unicode-normalization-checking"). */
    public static final String UNICODE_NORMALIZATION_CHECKING_FEATURE = "unicode-normalization-checking";

    /** xmlns URIs feature ("xmlns-uris"). */
    public static final String XMLNS_URIS_FEATURE = "xmlns-uris";

    /** XML 1.1 feature ("xml-1.1"). */
    public static final String XML_11_FEATURE = "xml-1.1";

    /** Allow unparsed entity and notation declaration events to be sent after the end DTD event ("allow-dtd-events-after-endDTD") */
    public static final String ALLOW_DTD_EVENTS_AFTER_ENDDTD_FEATURE = "allow-dtd-events-after-endDTD";

    // sax properties

    /** SAX property prefix ("http://xml.org/sax/properties/"). */
    public static final String SAX_PROPERTY_PREFIX = "http://xml.org/sax/properties/";

    /** Declaration handler property ("declaration-handler"). */
    public static final String DECLARATION_HANDLER_PROPERTY = "declaration-handler";

    /** Lexical handler property ("lexical-handler"). */
    public static final String LEXICAL_HANDLER_PROPERTY = "lexical-handler";

    /** DOM node property ("dom-node"). */
    public static final String DOM_NODE_PROPERTY = "dom-node";

    /** XML string property ("xml-string"). */
    public static final String XML_STRING_PROPERTY = "xml-string";

    public static final String FEATURE_SECURE_PROCESSING = "http://javax.xml.XMLConstants/feature/secure-processing";

    /** Document XML version property ("document-xml-version"). */
    public static final String DOCUMENT_XML_VERSION_PROPERTY = "document-xml-version";


    //
    // JAXP properties
    //

    /** JAXP property prefix ("http://java.sun.com/xml/jaxp/properties/"). */
    public static final String JAXP_PROPERTY_PREFIX =
        "http://java.sun.com/xml/jaxp/properties/";

    /** JAXP schemaSource property: when used internally may include DTD sources (DOM) */
    public static final String SCHEMA_SOURCE = "schemaSource";

    /** JAXP schemaSource language: when used internally may include DTD namespace (DOM) */
    public static final String SCHEMA_LANGUAGE = "schemaLanguage";

    /** JAXP Standard property prefix ("http://javax.xml.XMLConstants/property/"). */
    public static final String JAXPAPI_PROPERTY_PREFIX =
        "http://javax.xml.XMLConstants/property/";

    //
    // DOM features
    //

    /** Comments feature ("include-comments"). */
    public static final String INCLUDE_COMMENTS_FEATURE = "include-comments";

    /** Create cdata nodes feature ("create-cdata-nodes"). */
    public static final String CREATE_CDATA_NODES_FEATURE = "create-cdata-nodes";

    /** Feature id: load as infoset. */
    public static final String LOAD_AS_INFOSET = "load-as-infoset";


    //
    // Constants: DOM Level 3 feature ids
    //

    public static final String DOM_CANONICAL_FORM = "canonical-form";
    public static final String DOM_CDATA_SECTIONS ="cdata-sections";

    public static final String DOM_COMMENTS = "comments";

    // REVISIT: this feature seems to have no effect for Xerces

    public static final String DOM_CHARSET_OVERRIDES_XML_ENCODING =
    "charset-overrides-xml-encoding";

    public static final String DOM_DATATYPE_NORMALIZATION = "datatype-normalization";

    public static final String DOM_ENTITIES = "entities";
    public static final String DOM_INFOSET = "infoset";
    public static final String DOM_NAMESPACES = "namespaces";
    public static final String DOM_NAMESPACE_DECLARATIONS = "namespace-declarations";
    public static final String DOM_SUPPORTED_MEDIATYPES_ONLY =
        "supported-media-types-only";

    public static final String DOM_VALIDATE_IF_SCHEMA = "validate-if-schema";
    public static final String DOM_VALIDATE = "validate";
    public static final String DOM_ELEMENT_CONTENT_WHITESPACE =
        "element-content-whitespace";

    // DOM Level 3 features defined in Core:
    public static final String DOM_DISCARD_DEFAULT_CONTENT = "discard-default-content";
    public static final String DOM_NORMALIZE_CHARACTERS    = "normalize-characters";
    public static final String DOM_CHECK_CHAR_NORMALIZATION  = "check-character-normalization";
    public static final String DOM_WELLFORMED  = "well-formed";
    public static final String DOM_SPLIT_CDATA = "split-cdata-sections";

    // Load and Save
    public static final String DOM_FORMAT_PRETTY_PRINT = "format-pretty-print";
    public static final String DOM_XMLDECL = "xml-declaration";
    public static final String DOM_UNKNOWNCHARS = "unknown-characters";
    public static final String DOM_CERTIFIED =  "certified";
    public static final String DOM_DISALLOW_DOCTYPE =  "disallow-doctype";
    public static final String DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS =  "ignore-unknown-character-denormalizations";

    // DOM Properties
    public static final String DOM_RESOURCE_RESOLVER = "resource-resolver";
    public static final String DOM_ERROR_HANDLER = "error-handler";
    public static final String DOM_SCHEMA_TYPE = "schema-type";
    public static final String DOM_SCHEMA_LOCATION = "schema-location";
    public static final String DOM_ANCESTOR_CHECCK = "ancestor-check";
    // XSModel
    public static final String DOM_PSVI = "psvi";


    // xerces features

    /** Xerces features prefix ("http://apache.org/xml/features/"). */
    public static final String XERCES_FEATURE_PREFIX = "http://apache.org/xml/features/";

    /** Schema validation feature ("validation/schema"). */
    public static final String SCHEMA_VALIDATION_FEATURE = "validation/schema";

    /** Expose schema normalized values */
    public static final String SCHEMA_NORMALIZED_VALUE = "validation/schema/normalized-value";

    /** Send schema default value via characters() */
    public static final String SCHEMA_ELEMENT_DEFAULT = "validation/schema/element-default";

    /** Schema full constraint checking ("validation/schema-full-checking"). */
    public static final String SCHEMA_FULL_CHECKING = "validation/schema-full-checking";

    /** Augment Post-Schema-Validation-Infoset */
    public static final String SCHEMA_AUGMENT_PSVI = "validation/schema/augment-psvi";

    /** Dynamic validation feature ("validation/dynamic"). */
    public static final String DYNAMIC_VALIDATION_FEATURE = "validation/dynamic";

    /** Warn on duplicate attribute declaration feature ("validation/warn-on-duplicate-attdef"). */
    public static final String WARN_ON_DUPLICATE_ATTDEF_FEATURE = "validation/warn-on-duplicate-attdef";

    /** Warn on undeclared element feature ("validation/warn-on-undeclared-elemdef"). */
    public static final String WARN_ON_UNDECLARED_ELEMDEF_FEATURE = "validation/warn-on-undeclared-elemdef";

    /** Warn on duplicate entity declaration feature ("warn-on-duplicate-entitydef"). */
    public static final String WARN_ON_DUPLICATE_ENTITYDEF_FEATURE = "warn-on-duplicate-entitydef";

    /** Allow Java encoding names feature ("allow-java-encodings"). */
    public static final String ALLOW_JAVA_ENCODINGS_FEATURE = "allow-java-encodings";

    /** Disallow DOCTYPE declaration feature ("disallow-doctype-decl"). */
    public static final String DISALLOW_DOCTYPE_DECL_FEATURE = "disallow-doctype-decl";

    /** Continue after fatal error feature ("continue-after-fatal-error"). */
    public static final String CONTINUE_AFTER_FATAL_ERROR_FEATURE = "continue-after-fatal-error";

    /** Load dtd grammar when nonvalidating feature ("nonvalidating/load-dtd-grammar"). */
    public static final String LOAD_DTD_GRAMMAR_FEATURE = "nonvalidating/load-dtd-grammar";

    /** Load external dtd when nonvalidating feature ("nonvalidating/load-external-dtd"). */
    public static final String LOAD_EXTERNAL_DTD_FEATURE = "nonvalidating/load-external-dtd";

    /** Defer node expansion feature ("dom/defer-node-expansion"). */
    public static final String DEFER_NODE_EXPANSION_FEATURE = "dom/defer-node-expansion";

    /** Create entity reference nodes feature ("dom/create-entity-ref-nodes"). */
    public static final String CREATE_ENTITY_REF_NODES_FEATURE = "dom/create-entity-ref-nodes";

    /** Include ignorable whitespace feature ("dom/include-ignorable-whitespace"). */
    public static final String INCLUDE_IGNORABLE_WHITESPACE = "dom/include-ignorable-whitespace";

    /** Default attribute values feature ("validation/default-attribute-values"). */
    public static final String DEFAULT_ATTRIBUTE_VALUES_FEATURE = "validation/default-attribute-values";

    /** Validate content models feature ("validation/validate-content-models"). */
    public static final String VALIDATE_CONTENT_MODELS_FEATURE = "validation/validate-content-models";

    /** Validate datatypes feature ("validation/validate-datatypes"). */
    public static final String VALIDATE_DATATYPES_FEATURE = "validation/validate-datatypes";

    /** Balance syntax trees feature ("validation/balance-syntax-trees"). */
    public static final String BALANCE_SYNTAX_TREES = "validation/balance-syntax-trees";

    /** Notify character references feature (scanner/notify-char-refs"). */
    public static final String NOTIFY_CHAR_REFS_FEATURE = "scanner/notify-char-refs";

    /** Notify built-in (&amp;amp;, etc.) references feature (scanner/notify-builtin-refs"). */
    public static final String NOTIFY_BUILTIN_REFS_FEATURE = "scanner/notify-builtin-refs";

    /** Standard URI conformant feature ("standard-uri-conformant"). */
    public static final String STANDARD_URI_CONFORMANT_FEATURE = "standard-uri-conformant";

    /** Generate synthetic annotations feature ("generate-synthetic-annotations"). */
    public static final String GENERATE_SYNTHETIC_ANNOTATIONS_FEATURE = "generate-synthetic-annotations";

    /** Validate annotations feature ("validate-annotations"). */
    public static final String VALIDATE_ANNOTATIONS_FEATURE = "validate-annotations";


        /** Honour all schemaLocations feature ("honour-all-schemaLocations"). */
    public static final String HONOUR_ALL_SCHEMALOCATIONS_FEATURE = "honour-all-schemaLocations";

    /** Namespace growth feature ("namespace-growth"). */
    public static final String NAMESPACE_GROWTH_FEATURE = "namespace-growth";

    /** Tolerate duplicates feature ("internal/tolerate-duplicates"). */
    public static final String TOLERATE_DUPLICATES_FEATURE = "internal/tolerate-duplicates";

    /** XInclude processing feature ("xinclude"). */
    public static final String XINCLUDE_FEATURE = "xinclude";

    /** XInclude fixup base URIs feature ("xinclude/fixup-base-uris"). */
    public static final String XINCLUDE_FIXUP_BASE_URIS_FEATURE = "xinclude/fixup-base-uris";

    /** XInclude fixup language feature ("xinclude/fixup-language"). */
    public static final String XINCLUDE_FIXUP_LANGUAGE_FEATURE = "xinclude/fixup-language";

     /**
     * Feature to ignore xsi:type attributes on elements during validation,
     * until a global element declaration is found. ("validation/schema/ignore-xsi-type-until-elemdecl")
     * If this feature is on when validating a document, then beginning at the validation root
     * element, xsi:type attributes are ignored until a global element declaration is
     * found for an element.  Once a global element declaration has been found, xsi:type
     * attributes will start being processed for the sub-tree beginning at the element for
     * which the declaration was found.
     *
     * Suppose an element A has two element children, B and C.
     *
     * If a global element declaration is found for A, xsi:type attributes on A, B and C,
     * and all of B and C's descendents, will be processed.
     *
     * If no global element declaration is found for A or B, but one is found for C,
     * then xsi:type attributes will be ignored on A and B (and any descendents of B,
     * until a global element declaration is found), but xsi:type attributes will be
     * processed for C and all of C's descendents.
     *
     * Once xsi:type attributes stop being ignored for a subtree, they do not start
     * being ignored again, even if more elements are encountered for which no global
     * element declaration can be found.
     */
    public static final String IGNORE_XSI_TYPE_FEATURE = "validation/schema/ignore-xsi-type-until-elemdecl";

    /** Perform checking of ID/IDREFs ("validation/id-idref-checking") */
    public static final String ID_IDREF_CHECKING_FEATURE = "validation/id-idref-checking";

    /** Feature to ignore errors caused by identity constraints ("validation/identity-constraint-checking") */
    public static final String IDC_CHECKING_FEATURE = "validation/identity-constraint-checking";

    /** Feature to ignore errors caused by unparsed entities ("validation/unparsed-entity-checking") */
    public static final String UNPARSED_ENTITY_CHECKING_FEATURE = "validation/unparsed-entity-checking";

    /**
     * Internal feature. When set to true the schema validator will only use
     * schema components from the grammar pool provided.
     */
    public static final String USE_GRAMMAR_POOL_ONLY_FEATURE = "internal/validation/schema/use-grammar-pool-only";

    /** Internal performance related feature:
     * false - the parser settings (features/properties) have not changed between 2 parses
     * true - the parser settings have changed between 2 parses
     * NOTE: this feature should only be set by the parser configuration.
     */
    public static final String PARSER_SETTINGS = "internal/parser-settings";


    /** Feature to make XML Processor XInclude Aware */
    public static final String XINCLUDE_AWARE = "xinclude-aware";

    /** Ignore xsi:schemaLocation and xsi:noNamespaceSchemaLocation. */
    public static final String IGNORE_SCHEMA_LOCATION_HINTS = "validation/schema/ignore-schema-location-hints";

    /**
     * When true, the schema processor will change characters events
     * to ignorableWhitespaces events, when characters are expected to
     * only contain ignorable whitespaces.
     */
    public static final String CHANGE_IGNORABLE_CHARACTERS_INTO_IGNORABLE_WHITESPACES =
        "validation/change-ignorable-characters-into-ignorable-whitespaces";

    // xerces properties

    /** Xerces properties prefix ("http://apache.org/xml/properties/"). */
    public static final String XERCES_PROPERTY_PREFIX = "http://apache.org/xml/properties/";

    /** Current element node property ("dom/current-element-node"). */
    public static final String CURRENT_ELEMENT_NODE_PROPERTY = "dom/current-element-node";

    /** Document class name property ("dom/document-class-name"). */
    public static final String DOCUMENT_CLASS_NAME_PROPERTY = "dom/document-class-name";

    /** Symbol table property ("internal/symbol-table"). */
    public static final String SYMBOL_TABLE_PROPERTY = "internal/symbol-table";

    /** Error reporter property ("internal/error-reporter"). */
    public static final String ERROR_REPORTER_PROPERTY = "internal/error-reporter";

    /** Error handler property ("internal/error-handler"). */
    public static final String ERROR_HANDLER_PROPERTY = "internal/error-handler";

    /** XInclude handler property ("internal/xinclude-handler"). */
    public static final String XINCLUDE_HANDLER_PROPERTY = "internal/xinclude-handler";

    /** XPointer handler property ("internal/xpointer-handler"). */
    public static final String XPOINTER_HANDLER_PROPERTY = "internal/xpointer-handler";

    /** Entity manager property ("internal/entity-manager"). */
    public static final String ENTITY_MANAGER_PROPERTY = "internal/entity-manager";
    /** Input buffer size property ("input-buffer-size"). */
    public static final String BUFFER_SIZE_PROPERTY = "input-buffer-size";

    /** Security manager property ("security-manager"). */
    public static final String SECURITY_MANAGER_PROPERTY = "security-manager";

    /** Locale property ("locale"). */
    public static final String LOCALE_PROPERTY = "locale";

    /** property identifier: security manager. */
    public static final String SECURITY_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY;


    public static final String ENTITY_RESOLVER_PROPERTY = "internal/entity-resolver";

    /** Grammar pool property ("internal/grammar-pool"). */
    public static final String XMLGRAMMAR_POOL_PROPERTY = "internal/grammar-pool";

    /** Datatype validator factory ("internal/datatype-validator-factory"). */
    public static final String DATATYPE_VALIDATOR_FACTORY_PROPERTY = "internal/datatype-validator-factory";

    /** Document scanner property ("internal/document-scanner"). */
    public static final String DOCUMENT_SCANNER_PROPERTY = "internal/document-scanner";

    /** DTD scanner property ("internal/dtd-scanner"). */
    public static final String DTD_SCANNER_PROPERTY = "internal/dtd-scanner";

    /** DTD processor property ("internal/dtd-processor"). */
    public static final String DTD_PROCESSOR_PROPERTY = "internal/dtd-processor";

    /** Validator property ("internal/validator"). */
    public static final String VALIDATOR_PROPERTY = "internal/validator";

    /** Validator property ("internal/validator/dtd"). */
    public static final String DTD_VALIDATOR_PROPERTY = "internal/validator/dtd";

    /** Validator property ("internal/validator/schema"). */
    public static final String SCHEMA_VALIDATOR_PROPERTY = "internal/validator/schema";

    /** No namespace schema location property ("schema/external-schemaLocation"). */
    public static final String SCHEMA_LOCATION = "schema/external-schemaLocation";

    /** Schema location property ("schema/external-noNamespaceSchemaLocation"). */
    public static final String SCHEMA_NONS_LOCATION = "schema/external-noNamespaceSchemaLocation";

    /** Namespace binder property ("internal/namespace-binder"). */
    public static final String NAMESPACE_BINDER_PROPERTY = "internal/namespace-binder";

    /** Namespace context property ("internal/namespace-context"). */
    public static final String NAMESPACE_CONTEXT_PROPERTY = "internal/namespace-context";

    /** Validation manager property ("internal/validation-manager"). */
    public static final String VALIDATION_MANAGER_PROPERTY = "internal/validation-manager";

    /** Schema type for the root element in a document ("validation/schema/root-type-definition"). */
    public static final String ROOT_TYPE_DEFINITION_PROPERTY = "validation/schema/root-type-definition";

    /** Schema element declaration for the root element in a document ("validation/schema/root-element-declaration"). */
    public static final String ROOT_ELEMENT_DECLARATION_PROPERTY = "validation/schema/root-element-declaration";

    /** XPointer Schema property ("xpointer-schema"). */
    public static final String XPOINTER_SCHEMA_PROPERTY = "xpointer-schema";

    /** Schema element declaration for the root element in a document ("internal/validation/schema/dv-factory"). */
    public static final String SCHEMA_DV_FACTORY_PROPERTY = "internal/validation/schema/dv-factory";


    // general constants

    /** Element PSVI is stored in augmentations using string "ELEMENT_PSVI" */
    public final static String ELEMENT_PSVI = "ELEMENT_PSVI";

    /** Attribute PSVI is stored in augmentations using string "ATTRIBUTE_PSVI" */
    public final static String ATTRIBUTE_PSVI = "ATTRIBUTE_PSVI";

    /**
     * Boolean indicating whether an attribute is declared in the DTD is stored
     * in augmentations using the string "ATTRIBUTE_DECLARED". The absence of this
     * augmentation indicates that the attribute was not declared in the DTD.
     */
    public final static String ATTRIBUTE_DECLARED = "ATTRIBUTE_DECLARED";


    /**
     * {@link org.w3c.dom.TypeInfo} associated with current element/attribute
     * is stored in augmentations using this string as the key.
     *
     * This will ultimately controls {@link com.sun.org.apache.xerces.internal.parsers.AbstractDOMParser}
     * regarding what object the DOM will return from
     * {@link org.w3c.dom.Attr#getSchemaTypeInfo()} and
     * {@link org.w3c.dom.Element#getSchemaTypeInfo()} and
     */
    public final static String TYPEINFO = "org.w3c.dom.TypeInfo";

    /**
     * Whether an attribute is an id or not is stored in augmentations
     * using this string as the key. The value is {@link Boolean#TRUE}
     * or {@link Boolean#FALSE}.
     *
     * This will ultimately controls {@link com.sun.org.apache.xerces.internal.parsers.AbstractDOMParser}
     * about whether it will mark an attribute as ID or not.
     */
    public final static String ID_ATTRIBUTE = "ID_ATTRIBUTE";

    // XML version constants

    /**
     * Boolean indicating whether an entity referenced in the document has
     * not been read is stored in augmentations using the string "ENTITY_SKIPPED".
     * The absence of this augmentation indicates that the entity had a
     * declaration and was expanded.
     */
    public final static String ENTITY_SKIPPED = "ENTITY_SKIPPED";

    /**
     * Boolean indicating whether a character is a probable white space
     * character (ch <= 0x20) that was the replacement text of a character
     * reference is stored in augmentations using the string "CHAR_REF_PROBABLE_WS".
     * The absence of this augmentation indicates that the character is not
     * probable white space and/or was not included from a character reference.
     */
    public final static String CHAR_REF_PROBABLE_WS = "CHAR_REF_PROBABLE_WS";

    /** Boolean indicating if this entity is the last opened entity.
     *
     *@see com.sun.org.apache.xerces.internal.impl.XMLEntityManager#endEntity()
     *@see com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl#endEntity()
     *@see com.sun.org.apache.xerces.internal.impl.XMLDTDScannerImpl#endEntity()
     */
    public final static String LAST_ENTITY = "LAST_ENTITY";

    // XML version constants
    public final static short XML_VERSION_ERROR = -1;
    public final static short XML_VERSION_1_0 = 1;
    public final static short XML_VERSION_1_1 = 2;



    // DOM related constants
    public final static String ANONYMOUS_TYPE_NAMESPACE =
        "http://apache.org/xml/xmlschema/1.0/anonymousTypes";



    // Constant to enable Schema 1.1 support
    public final static boolean SCHEMA_1_1_SUPPORT = false;
    public final static short SCHEMA_VERSION_1_0          = 1;
    public final static short SCHEMA_VERSION_1_0_EXTENDED = 2;

    // private

    /** SAX features. */
    private static final String[] fgSAXFeatures = {
            NAMESPACES_FEATURE,
            NAMESPACE_PREFIXES_FEATURE,
            STRING_INTERNING_FEATURE,
            VALIDATION_FEATURE,
            EXTERNAL_GENERAL_ENTITIES_FEATURE,
            EXTERNAL_PARAMETER_ENTITIES_FEATURE,
    };

    /** SAX properties. */
    private static final String[] fgSAXProperties = {
            DECLARATION_HANDLER_PROPERTY,
            LEXICAL_HANDLER_PROPERTY,
            DOM_NODE_PROPERTY,
            XML_STRING_PROPERTY,
    };

    /** Xerces features. */
    private static final String[] fgXercesFeatures = {

        SCHEMA_VALIDATION_FEATURE,
        SCHEMA_FULL_CHECKING,
        DYNAMIC_VALIDATION_FEATURE,
        WARN_ON_DUPLICATE_ATTDEF_FEATURE,
        WARN_ON_UNDECLARED_ELEMDEF_FEATURE,
        ALLOW_JAVA_ENCODINGS_FEATURE,
        CONTINUE_AFTER_FATAL_ERROR_FEATURE,
        LOAD_DTD_GRAMMAR_FEATURE,
        LOAD_EXTERNAL_DTD_FEATURE,
        //DEFER_NODE_EXPANSION_FEATURE,
        CREATE_ENTITY_REF_NODES_FEATURE,
        XINCLUDE_AWARE,
        INCLUDE_IGNORABLE_WHITESPACE,
        //GRAMMAR_ACCESS_FEATURE,
        DEFAULT_ATTRIBUTE_VALUES_FEATURE,
        VALIDATE_CONTENT_MODELS_FEATURE,
        VALIDATE_DATATYPES_FEATURE,
        BALANCE_SYNTAX_TREES,
        NOTIFY_CHAR_REFS_FEATURE,
        NOTIFY_BUILTIN_REFS_FEATURE,
        DISALLOW_DOCTYPE_DECL_FEATURE,
        STANDARD_URI_CONFORMANT_FEATURE,
        GENERATE_SYNTHETIC_ANNOTATIONS_FEATURE,
        VALIDATE_ANNOTATIONS_FEATURE,
        HONOUR_ALL_SCHEMALOCATIONS_FEATURE,
        XINCLUDE_FEATURE,
        XINCLUDE_FIXUP_BASE_URIS_FEATURE,
        XINCLUDE_FIXUP_LANGUAGE_FEATURE,
        IGNORE_XSI_TYPE_FEATURE,
        ID_IDREF_CHECKING_FEATURE,
        IDC_CHECKING_FEATURE,
        UNPARSED_ENTITY_CHECKING_FEATURE,
        NAMESPACE_GROWTH_FEATURE,
        TOLERATE_DUPLICATES_FEATURE,
    };

    /** Xerces properties. */
    private static final String[] fgXercesProperties = {
            CURRENT_ELEMENT_NODE_PROPERTY,
            DOCUMENT_CLASS_NAME_PROPERTY,
            SYMBOL_TABLE_PROPERTY,
            ERROR_HANDLER_PROPERTY,
            ERROR_REPORTER_PROPERTY,
            ENTITY_MANAGER_PROPERTY,
            ENTITY_RESOLVER_PROPERTY,
            XMLGRAMMAR_POOL_PROPERTY,
            DATATYPE_VALIDATOR_FACTORY_PROPERTY,
            DOCUMENT_SCANNER_PROPERTY,
            DTD_SCANNER_PROPERTY,
            VALIDATOR_PROPERTY,
            SCHEMA_LOCATION,
            SCHEMA_NONS_LOCATION,
            VALIDATION_MANAGER_PROPERTY,
            BUFFER_SIZE_PROPERTY,
            SECURITY_MANAGER_PROPERTY,
            ROOT_TYPE_DEFINITION_PROPERTY,
            ROOT_ELEMENT_DECLARATION_PROPERTY,
            LOCALE_PROPERTY,
            SCHEMA_DV_FACTORY_PROPERTY,
    };

    /** Empty enumeration. */
    private static final Enumeration<Object> fgEmptyEnumeration = new ArrayEnumeration(new Object[] {});

    //
    // Constructors
    //

    /** This class cannot be instantiated. */
    private Constants() {}

    //
    // Public methods
    //

    // sax

    /** Returns an enumeration of the SAX features. */
    public static Enumeration<Object> getSAXFeatures() {
        return fgSAXFeatures.length > 0
        ? new ArrayEnumeration(fgSAXFeatures) : fgEmptyEnumeration;
    } // getSAXFeatures():Enumeration

    /** Returns an enumeration of the SAX properties. */
    public static Enumeration<Object> getSAXProperties() {
        return fgSAXProperties.length > 0
        ? new ArrayEnumeration(fgSAXProperties) : fgEmptyEnumeration;
    } // getSAXProperties():Enumeration

    // xerces

    /** Returns an enumeration of the Xerces features. */
    public static Enumeration<Object> getXercesFeatures() {
        return fgXercesFeatures.length > 0
        ? new ArrayEnumeration(fgXercesFeatures) : fgEmptyEnumeration;
    } // getXercesFeatures():Enumeration

    /** Returns an enumeration of the Xerces properties. */
    public static Enumeration<Object> getXercesProperties() {
        return fgXercesProperties.length > 0
        ? new ArrayEnumeration(fgXercesProperties) : fgEmptyEnumeration;
    } // getXercesProperties():Enumeration

    //
    // Classes
    //

    /**
     * An array enumeration.
     *
     * @author Andy Clark, IBM
     */
    static class ArrayEnumeration
    implements Enumeration<Object> {

        //
        // Data
        //

        /** Array. */
        private Object[] array;

        /** Index. */
        private int index;

        //
        // Constructors
        //

        /** Constructs an array enumeration. */
        public ArrayEnumeration(Object[] array) {
            this.array = array;
        } // <init>(Object[])

        //
        // Enumeration methods
        //

        /**
         * Tests if this enumeration contains more elements.
         *
         * @return  <code>true</code> if this enumeration contains more elements;
         *          <code>false</code> otherwise.
         * @since   JDK1.0
         */
        public boolean hasMoreElements() {
            return index < array.length;
        } // hasMoreElement():boolean

        /**
         * Returns the next element of this enumeration.
         *
         * @return     the next element of this enumeration.
         * @exception  NoSuchElementException  if no more elements exist.
         * @since      JDK1.0
         */
        public Object nextElement() {
            if (index < array.length) {
                return array[index++];
            }
            throw new NoSuchElementException();
        } // nextElement():Object

    } // class ArrayEnumeration

    //
    // MAIN
    //

    /** Prints all of the constants to standard output. */
    public static void main(String[] argv) {

        print("SAX features:", SAX_FEATURE_PREFIX, fgSAXFeatures);
        print("SAX properties:", SAX_PROPERTY_PREFIX, fgSAXProperties);
        print("Xerces features:", XERCES_FEATURE_PREFIX, fgXercesFeatures);
        print("Xerces properties:", XERCES_PROPERTY_PREFIX, fgXercesProperties);

    } // main(String[])

    /** Prints a list of features/properties. */
    private static void print(String header, String prefix, Object[] array) {
        System.out.print(header);
        if (array.length > 0) {
            System.out.println();
            for (int i = 0; i < array.length; i++) {
                System.out.print("  ");
                System.out.print(prefix);
                System.out.println(array[i]);
            }
        }
        else {
            System.out.println(" none.");
        }
    } // print(String,String,Object[])

} // class Constants
