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

package com.sun.org.apache.xml.internal.res;


import java.util.ListResourceBundle;

/**
 * Set up error messages.
 * We build a two dimensional array of message keys and
 * message strings. In order to add a new message here,
 * you need to first add a String constant. And you need
 * to enter key, value pair as part of the contents
 * array. You also need to update MAX_CODE for error strings
 * and MAX_WARNING for warnings ( Needed for only information
 * purpose )
 */
public class XMLErrorResources_de extends ListResourceBundle
{

/*
 * This file contains error and warning messages related to Xalan Error
 * Handling.
 *
 *  General notes to translators:
 *
 *  1) Xalan (or more properly, Xalan-interpretive) and XSLTC are names of
 *     components.
 *     XSLT is an acronym for "XML Stylesheet Language: Transformations".
 *     XSLTC is an acronym for XSLT Compiler.
 *
 *  2) A stylesheet is a description of how to transform an input XML document
 *     into a resultant XML document (or HTML document or text).  The
 *     stylesheet itself is described in the form of an XML document.
 *
 *  3) A template is a component of a stylesheet that is used to match a
 *     particular portion of an input document and specifies the form of the
 *     corresponding portion of the output document.
 *
 *  4) An element is a mark-up tag in an XML document; an attribute is a
 *     modifier on the tag.  For example, in <elem attr='val' attr2='val2'>
 *     "elem" is an element name, "attr" and "attr2" are attribute names with
 *     the values "val" and "val2", respectively.
 *
 *  5) A namespace declaration is a special attribute that is used to associate
 *     a prefix with a URI (the namespace).  The meanings of element names and
 *     attribute names that use that prefix are defined with respect to that
 *     namespace.
 *
 *  6) "Translet" is an invented term that describes the class file that
 *     results from compiling an XML stylesheet into a Java class.
 *
 *  7) XPath is a specification that describes a notation for identifying
 *     nodes in a tree-structured representation of an XML document.  An
 *     instance of that notation is referred to as an XPath expression.
 *
 */

  /** Maximum error messages, this is needed to keep track of the number of messages.    */
  public static final int MAX_CODE = 61;

  /** Maximum warnings, this is needed to keep track of the number of warnings.          */
  public static final int MAX_WARNING = 0;

  /** Maximum misc strings.   */
  public static final int MAX_OTHERS = 4;

  /** Maximum total warnings and error messages.          */
  public static final int MAX_MESSAGES = MAX_CODE + MAX_WARNING + 1;


  /*
   * Message keys
   */
  public static final String ER_FUNCTION_NOT_SUPPORTED = "ER_FUNCTION_NOT_SUPPORTED";
  public static final String ER_CANNOT_OVERWRITE_CAUSE = "ER_CANNOT_OVERWRITE_CAUSE";
  public static final String ER_NO_DEFAULT_IMPL = "ER_NO_DEFAULT_IMPL";
  public static final String ER_CHUNKEDINTARRAY_NOT_SUPPORTED = "ER_CHUNKEDINTARRAY_NOT_SUPPORTED";
  public static final String ER_OFFSET_BIGGER_THAN_SLOT = "ER_OFFSET_BIGGER_THAN_SLOT";
  public static final String ER_COROUTINE_NOT_AVAIL = "ER_COROUTINE_NOT_AVAIL";
  public static final String ER_COROUTINE_CO_EXIT = "ER_COROUTINE_CO_EXIT";
  public static final String ER_COJOINROUTINESET_FAILED = "ER_COJOINROUTINESET_FAILED";
  public static final String ER_COROUTINE_PARAM = "ER_COROUTINE_PARAM";
  public static final String ER_PARSER_DOTERMINATE_ANSWERS = "ER_PARSER_DOTERMINATE_ANSWERS";
  public static final String ER_NO_PARSE_CALL_WHILE_PARSING = "ER_NO_PARSE_CALL_WHILE_PARSING";
  public static final String ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED = "ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED";
  public static final String ER_ITERATOR_AXIS_NOT_IMPLEMENTED = "ER_ITERATOR_AXIS_NOT_IMPLEMENTED";
  public static final String ER_ITERATOR_CLONE_NOT_SUPPORTED = "ER_ITERATOR_CLONE_NOT_SUPPORTED";
  public static final String ER_UNKNOWN_AXIS_TYPE = "ER_UNKNOWN_AXIS_TYPE";
  public static final String ER_AXIS_NOT_SUPPORTED = "ER_AXIS_NOT_SUPPORTED";
  public static final String ER_NO_DTMIDS_AVAIL = "ER_NO_DTMIDS_AVAIL";
  public static final String ER_NOT_SUPPORTED = "ER_NOT_SUPPORTED";
  public static final String ER_NODE_NON_NULL = "ER_NODE_NON_NULL";
  public static final String ER_COULD_NOT_RESOLVE_NODE = "ER_COULD_NOT_RESOLVE_NODE";
  public static final String ER_STARTPARSE_WHILE_PARSING = "ER_STARTPARSE_WHILE_PARSING";
  public static final String ER_STARTPARSE_NEEDS_SAXPARSER = "ER_STARTPARSE_NEEDS_SAXPARSER";
  public static final String ER_COULD_NOT_INIT_PARSER = "ER_COULD_NOT_INIT_PARSER";
  public static final String ER_EXCEPTION_CREATING_POOL = "ER_EXCEPTION_CREATING_POOL";
  public static final String ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE = "ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE";
  public static final String ER_SCHEME_REQUIRED = "ER_SCHEME_REQUIRED";
  public static final String ER_NO_SCHEME_IN_URI = "ER_NO_SCHEME_IN_URI";
  public static final String ER_NO_SCHEME_INURI = "ER_NO_SCHEME_INURI";
  public static final String ER_PATH_INVALID_CHAR = "ER_PATH_INVALID_CHAR";
  public static final String ER_SCHEME_FROM_NULL_STRING = "ER_SCHEME_FROM_NULL_STRING";
  public static final String ER_SCHEME_NOT_CONFORMANT = "ER_SCHEME_NOT_CONFORMANT";
  public static final String ER_HOST_ADDRESS_NOT_WELLFORMED = "ER_HOST_ADDRESS_NOT_WELLFORMED";
  public static final String ER_PORT_WHEN_HOST_NULL = "ER_PORT_WHEN_HOST_NULL";
  public static final String ER_INVALID_PORT = "ER_INVALID_PORT";
  public static final String ER_FRAG_FOR_GENERIC_URI ="ER_FRAG_FOR_GENERIC_URI";
  public static final String ER_FRAG_WHEN_PATH_NULL = "ER_FRAG_WHEN_PATH_NULL";
  public static final String ER_FRAG_INVALID_CHAR = "ER_FRAG_INVALID_CHAR";
  public static final String ER_PARSER_IN_USE = "ER_PARSER_IN_USE";
  public static final String ER_CANNOT_CHANGE_WHILE_PARSING = "ER_CANNOT_CHANGE_WHILE_PARSING";
  public static final String ER_SELF_CAUSATION_NOT_PERMITTED = "ER_SELF_CAUSATION_NOT_PERMITTED";
  public static final String ER_NO_USERINFO_IF_NO_HOST = "ER_NO_USERINFO_IF_NO_HOST";
  public static final String ER_NO_PORT_IF_NO_HOST = "ER_NO_PORT_IF_NO_HOST";
  public static final String ER_NO_QUERY_STRING_IN_PATH = "ER_NO_QUERY_STRING_IN_PATH";
  public static final String ER_NO_FRAGMENT_STRING_IN_PATH = "ER_NO_FRAGMENT_STRING_IN_PATH";
  public static final String ER_CANNOT_INIT_URI_EMPTY_PARMS = "ER_CANNOT_INIT_URI_EMPTY_PARMS";
  public static final String ER_METHOD_NOT_SUPPORTED ="ER_METHOD_NOT_SUPPORTED";
  public static final String ER_INCRSAXSRCFILTER_NOT_RESTARTABLE = "ER_INCRSAXSRCFILTER_NOT_RESTARTABLE";
  public static final String ER_XMLRDR_NOT_BEFORE_STARTPARSE = "ER_XMLRDR_NOT_BEFORE_STARTPARSE";
  public static final String ER_AXIS_TRAVERSER_NOT_SUPPORTED = "ER_AXIS_TRAVERSER_NOT_SUPPORTED";
  public static final String ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER = "ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER";
  public static final String ER_SYSTEMID_UNKNOWN = "ER_SYSTEMID_UNKNOWN";
  public static final String ER_LOCATION_UNKNOWN = "ER_LOCATION_UNKNOWN";
  public static final String ER_PREFIX_MUST_RESOLVE = "ER_PREFIX_MUST_RESOLVE";
  public static final String ER_CREATEDOCUMENT_NOT_SUPPORTED = "ER_CREATEDOCUMENT_NOT_SUPPORTED";
  public static final String ER_CHILD_HAS_NO_OWNER_DOCUMENT = "ER_CHILD_HAS_NO_OWNER_DOCUMENT";
  public static final String ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT = "ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT";
  public static final String ER_CANT_OUTPUT_TEXT_BEFORE_DOC = "ER_CANT_OUTPUT_TEXT_BEFORE_DOC";
  public static final String ER_CANT_HAVE_MORE_THAN_ONE_ROOT = "ER_CANT_HAVE_MORE_THAN_ONE_ROOT";
  public static final String ER_ARG_LOCALNAME_NULL = "ER_ARG_LOCALNAME_NULL";
  public static final String ER_ARG_LOCALNAME_INVALID = "ER_ARG_LOCALNAME_INVALID";
  public static final String ER_ARG_PREFIX_INVALID = "ER_ARG_PREFIX_INVALID";
  public static final String ER_NAME_CANT_START_WITH_COLON = "ER_NAME_CANT_START_WITH_COLON";

  // Message keys used by the serializer
  public static final String ER_RESOURCE_COULD_NOT_FIND = "ER_RESOURCE_COULD_NOT_FIND";
  public static final String ER_RESOURCE_COULD_NOT_LOAD = "ER_RESOURCE_COULD_NOT_LOAD";
  public static final String ER_BUFFER_SIZE_LESSTHAN_ZERO = "ER_BUFFER_SIZE_LESSTHAN_ZERO";
  public static final String ER_INVALID_UTF16_SURROGATE = "ER_INVALID_UTF16_SURROGATE";
  public static final String ER_OIERROR = "ER_OIERROR";
  public static final String ER_NAMESPACE_PREFIX = "ER_NAMESPACE_PREFIX";
  public static final String ER_STRAY_ATTRIBUTE = "ER_STRAY_ATTIRBUTE";
  public static final String ER_STRAY_NAMESPACE = "ER_STRAY_NAMESPACE";
  public static final String ER_COULD_NOT_LOAD_RESOURCE = "ER_COULD_NOT_LOAD_RESOURCE";
  public static final String ER_COULD_NOT_LOAD_METHOD_PROPERTY = "ER_COULD_NOT_LOAD_METHOD_PROPERTY";
  public static final String ER_SERIALIZER_NOT_CONTENTHANDLER = "ER_SERIALIZER_NOT_CONTENTHANDLER";
  public static final String ER_ILLEGAL_ATTRIBUTE_POSITION = "ER_ILLEGAL_ATTRIBUTE_POSITION";
  public static final String ER_ILLEGAL_CHARACTER = "ER_ILLEGAL_CHARACTER";

  /*
   * Now fill in the message text.
   * Then fill in the message text for that message code in the
   * array. Use the new error code as the index into the array.
   */

  // Error messages...

  /** The lookup table for error messages.   */
  private static final Object[][] contents = {

  /** Error message ID that has a null message, but takes in a single object.    */
    {"ER0000" , "{0}" },

    { ER_FUNCTION_NOT_SUPPORTED,
      "Funktion nicht unterst\u00FCtzt."},

    { ER_CANNOT_OVERWRITE_CAUSE,
      "Ursache kann nicht \u00FCberschrieben werden"},

    { ER_NO_DEFAULT_IMPL,
      "Keine Standardimplementierung gefunden "},

    { ER_CHUNKEDINTARRAY_NOT_SUPPORTED,
      "ChunkedIntArray({0}) derzeit nicht unterst\u00FCtzt"},

    { ER_OFFSET_BIGGER_THAN_SLOT,
      "Offset gr\u00F6\u00DFer als Slot"},

    { ER_COROUTINE_NOT_AVAIL,
      "Coroutine nicht verf\u00FCgbar; ID={0}"},

    { ER_COROUTINE_CO_EXIT,
      "CoroutineManager hat co_exit()-Anforderung erhalten"},

    { ER_COJOINROUTINESET_FAILED,
      "co_joinCoroutineSet() nicht erfolgreich"},

    { ER_COROUTINE_PARAM,
      "Coroutine-Parameterfehler ({0})"},

    { ER_PARSER_DOTERMINATE_ANSWERS,
      "\nUNEXPECTED: Parser doTerminate antwortet {0}"},

    { ER_NO_PARSE_CALL_WHILE_PARSING,
      "\"parse\" darf w\u00E4hrend des Parsing nicht aufgerufen werden"},

    { ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Fehler: Typisierter Iterator f\u00FCr Achse {0} nicht implementiert"},

    { ER_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Fehler: Iterator f\u00FCr Achse {0} nicht implementiert "},

    { ER_ITERATOR_CLONE_NOT_SUPPORTED,
      "Iteratorclone nicht unterst\u00FCtzt"},

    { ER_UNKNOWN_AXIS_TYPE,
      "Unbekannter Achsendurchlauftyp: {0}"},

    { ER_AXIS_NOT_SUPPORTED,
      "Achsen-Traverser nicht unterst\u00FCtzt: {0}"},

    { ER_NO_DTMIDS_AVAIL,
      "Keine weiteren DTM-IDs verf\u00FCgbar"},

    { ER_NOT_SUPPORTED,
      "Nicht unterst\u00FCtzt: {0}"},

    { ER_NODE_NON_NULL,
      "Knoten darf nicht null sein f\u00FCr getDTMHandleFromNode"},

    { ER_COULD_NOT_RESOLVE_NODE,
      "Knoten konnte nicht in Handle aufgel\u00F6st werden"},

    { ER_STARTPARSE_WHILE_PARSING,
       "\"startParse\" darf w\u00E4hrend des Parsing nicht aufgerufen werden"},

    { ER_STARTPARSE_NEEDS_SAXPARSER,
       "startParse erfordert einen SAXParser ungleich null"},

    { ER_COULD_NOT_INIT_PARSER,
       "Parser konnte nicht initialisiert werden mit"},

    { ER_EXCEPTION_CREATING_POOL,
       "Ausnahme beim Erstellen einer neuen Instanz f\u00FCr Pool"},

    { ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
       "Pfad enth\u00E4lt eine ung\u00FCltige Escapesequenz"},

    { ER_SCHEME_REQUIRED,
       "Schema ist erforderlich."},

    { ER_NO_SCHEME_IN_URI,
       "Kein Schema gefunden in URI: {0}"},

    { ER_NO_SCHEME_INURI,
       "Kein Schema gefunden in URI"},

    { ER_PATH_INVALID_CHAR,
       "Pfad enth\u00E4lt ung\u00FCltiges Zeichen: {0}"},

    { ER_SCHEME_FROM_NULL_STRING,
       "Schema kann nicht von Nullzeichenfolge festgelegt werden"},

    { ER_SCHEME_NOT_CONFORMANT,
       "Schema ist nicht konform."},

    { ER_HOST_ADDRESS_NOT_WELLFORMED,
       "Host ist keine wohlgeformte Adresse"},

    { ER_PORT_WHEN_HOST_NULL,
       "Port kann nicht festgelegt werden, wenn der Host null ist"},

    { ER_INVALID_PORT,
       "Ung\u00FCltige Portnummer"},

    { ER_FRAG_FOR_GENERIC_URI,
       "Fragment kann nur f\u00FCr einen generischen URI festgelegt werden"},

    { ER_FRAG_WHEN_PATH_NULL,
       "Fragment kann nicht festgelegt werden, wenn der Pfad null ist"},

    { ER_FRAG_INVALID_CHAR,
       "Fragment enth\u00E4lt ein ung\u00FCltiges Zeichen"},

    { ER_PARSER_IN_USE,
      "Parser wird bereits verwendet"},

    { ER_CANNOT_CHANGE_WHILE_PARSING,
      "{0} {1} kann w\u00E4hrend Parsing nicht ge\u00E4ndert werden"},

    { ER_SELF_CAUSATION_NOT_PERMITTED,
      "Selbstkausalit\u00E4t nicht zul\u00E4ssig"},

    { ER_NO_USERINFO_IF_NO_HOST,
      "Benutzerinformationen k\u00F6nnen nicht angegeben werden, wenn der Host nicht angegeben wurde"},

    { ER_NO_PORT_IF_NO_HOST,
      "Port kann nicht angegeben werden, wenn der Host nicht angegeben wurde"},

    { ER_NO_QUERY_STRING_IN_PATH,
      "Abfragezeichenfolge kann nicht im Pfad und in der Abfragezeichenfolge angegeben werden"},

    { ER_NO_FRAGMENT_STRING_IN_PATH,
      "Fragment kann nicht im Pfad und im Fragment angegeben werden"},

    { ER_CANNOT_INIT_URI_EMPTY_PARMS,
      "URI kann nicht mit leeren Parametern initialisiert werden"},

    { ER_METHOD_NOT_SUPPORTED,
      "Methode noch nicht unterst\u00FCtzt "},

    { ER_INCRSAXSRCFILTER_NOT_RESTARTABLE,
      "IncrementalSAXSource_Filter kann derzeit nicht neu gestartet werden"},

    { ER_XMLRDR_NOT_BEFORE_STARTPARSE,
      "XMLReader nicht vor startParse-Anforderung"},

    { ER_AXIS_TRAVERSER_NOT_SUPPORTED,
      "Achsen-Traverser nicht unterst\u00FCtzt: {0}"},

    { ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER,
      "ListingErrorHandler mit Null-PrintWriter erstellt."},

    { ER_SYSTEMID_UNKNOWN,
      "SystemId unbekannt"},

    { ER_LOCATION_UNKNOWN,
      "Fehlerposition unbekannt"},

    { ER_PREFIX_MUST_RESOLVE,
      "Pr\u00E4fix muss in Namespace aufgel\u00F6st werden: {0}"},

    { ER_CREATEDOCUMENT_NOT_SUPPORTED,
      "createDocument() nicht in XPathContext unterst\u00FCtzt."},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT,
      "Untergeordnetes Attribut hat kein Eigent\u00FCmerdokument."},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT,
      "Untergeordnetes Attribut hat kein Eigent\u00FCmerdokumentelement."},

    { ER_CANT_OUTPUT_TEXT_BEFORE_DOC,
      "Warnung: Text kann nicht vor Dokumentelement ausgegeben werden. Wird ignoriert..."},

    { ER_CANT_HAVE_MORE_THAN_ONE_ROOT,
      "Mehrere Roots f\u00FCr ein DOM nicht zul\u00E4ssig."},

    { ER_ARG_LOCALNAME_NULL,
       "Argument \"localName\" ist null"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The localname is the portion after the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_LOCALNAME_INVALID,
       "Localname in QNAME muss ein g\u00FCltiger NCName sein"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The prefix is the portion before the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_PREFIX_INVALID,
       "Pr\u00E4fix in QNAME muss ein g\u00FCltiger NCName sein"},

    { ER_NAME_CANT_START_WITH_COLON,
      "Name darf nicht mit einem Doppelpunkt beginnen"},

    { "BAD_CODE", "Parameter f\u00FCr createMessage war au\u00DFerhalb des g\u00FCltigen Bereichs"},
    { "FORMAT_FAILED", "Ausnahme bei messageFormat-Aufruf ausgel\u00F6st"},
    { "line", "Zeilennummer"},
    { "column","Spaltennummer"},

    {ER_SERIALIZER_NOT_CONTENTHANDLER,
      "Serializer-Klasse \"{0}\" implementiert org.xml.sax.ContentHandler nicht."},

    {ER_RESOURCE_COULD_NOT_FIND,
      "Ressource [ {0} ] konnte nicht gefunden werden.\n {1}" },

    {ER_RESOURCE_COULD_NOT_LOAD,
      "Ressource [ {0} ] konnte nicht geladen werden: {1} \n {2} \t {3}" },

    {ER_BUFFER_SIZE_LESSTHAN_ZERO,
      "Puffergr\u00F6\u00DFe <=0" },

    {ER_INVALID_UTF16_SURROGATE,
      "Ung\u00FCltige UTF-16-Ersetzung festgestellt: {0}?" },

    {ER_OIERROR,
      "I/O-Fehler" },

    {ER_ILLEGAL_ATTRIBUTE_POSITION,
      "Attribut {0} kann nicht nach untergeordneten Knoten oder vor dem Erstellen eines Elements hinzugef\u00FCgt werden. Attribut wird ignoriert."},

      /*
       * Note to translators:  The stylesheet contained a reference to a
       * namespace prefix that was undefined.  The value of the substitution
       * text is the name of the prefix.
       */
    {ER_NAMESPACE_PREFIX,
      "Namespace f\u00FCr Pr\u00E4fix \"{0}\" wurde nicht deklariert." },
      /*
       * Note to translators:  This message is reported if the stylesheet
       * being processed attempted to construct an XML document with an
       * attribute in a place other than on an element.  The substitution text
       * specifies the name of the attribute.
       */
    {ER_STRAY_ATTRIBUTE,
      "Attribut \"{0}\" au\u00DFerhalb des Elements." },

      /*
       * Note to translators:  As with the preceding message, a namespace
       * declaration has the form of an attribute and is only permitted to
       * appear on an element.  The substitution text {0} is the namespace
       * prefix and {1} is the URI that was being used in the erroneous
       * namespace declaration.
       */
    {ER_STRAY_NAMESPACE,
      "Namespace-Deklaration {0}={1} au\u00DFerhalb des Elements." },

    {ER_COULD_NOT_LOAD_RESOURCE,
      "\"{0}\" konnte nicht geladen werden (CLASSPATH pr\u00FCfen). Die Standardwerte werden verwendet"},

    { ER_ILLEGAL_CHARACTER,
       "Versuch, Zeichen mit Integralwert {0} auszugeben, das nicht in der speziellen Ausgabecodierung von {1} dargestellt wird."},

    {ER_COULD_NOT_LOAD_METHOD_PROPERTY,
      "Property-Datei \"{0}\" konnte f\u00FCr Ausgabemethode \"{1}\" nicht geladen werden (CLASSPATH pr\u00FCfen)" }


  };

  /**
   * Get the association list.
   *
   * @return The association list.
   */

    protected Object[][] getContents() {
        return contents;
    }

}
