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
public class XMLErrorResources_cs extends ListResourceBundle
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

  /*
   * Now fill in the message text.
   * Then fill in the message text for that message code in the
   * array. Use the new error code as the index into the array.
   */

  // Error messages...
  private static final Object[][] _contents = new Object[][] {

  /** Error message ID that has a null message, but takes in a single object.    */
    {"ER0000" , "{0}" },

    { ER_FUNCTION_NOT_SUPPORTED,
      "Nepodporovan\u00e1 funkce!"},

    { ER_CANNOT_OVERWRITE_CAUSE,
      "P\u0159\u00ed\u010dinu nelze p\u0159epsat"},

    { ER_NO_DEFAULT_IMPL,
      "Nebyla nalezena v\u00fdchoz\u00ed implementace. "},

    { ER_CHUNKEDINTARRAY_NOT_SUPPORTED,
      "Funkce ChunkedIntArray({0}) nen\u00ed aktu\u00e1ln\u011b podporov\u00e1na."},

    { ER_OFFSET_BIGGER_THAN_SLOT,
      "Offset je v\u011bt\u0161\u00ed ne\u017e slot."},

    { ER_COROUTINE_NOT_AVAIL,
      "Spole\u010dn\u00e1 rutina nen\u00ed k dispozici, id={0}"},

    { ER_COROUTINE_CO_EXIT,
      "Funkce CoroutineManager obdr\u017eela po\u017eadavek co_exit()"},

    { ER_COJOINROUTINESET_FAILED,
      "Selhala funkce co_joinCoroutineSet()"},

    { ER_COROUTINE_PARAM,
      "Chyba parametru spole\u010dn\u00e9 rutiny ({0})"},

    { ER_PARSER_DOTERMINATE_ANSWERS,
      "\nNeo\u010dek\u00e1van\u00e9: odpov\u011bdi funkce analyz\u00e1toru doTerminate {0}"},

    { ER_NO_PARSE_CALL_WHILE_PARSING,
      "b\u011bhem anal\u00fdzy nelze volat analyz\u00e1tor"},

    { ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Chyba: zadan\u00fd iter\u00e1tor osy {0} nen\u00ed implementov\u00e1n"},

    { ER_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Chyba: zadan\u00fd iter\u00e1tor osy {0} nen\u00ed implementov\u00e1n "},

    { ER_ITERATOR_CLONE_NOT_SUPPORTED,
      "Nepodporovan\u00fd klon iter\u00e1toru."},

    { ER_UNKNOWN_AXIS_TYPE,
      "Nezn\u00e1m\u00fd typ osy pr\u016fchodu: {0}"},

    { ER_AXIS_NOT_SUPPORTED,
      "Nepodporovan\u00e1 osa pr\u016fchodu: {0}"},

    { ER_NO_DTMIDS_AVAIL,
      "\u017d\u00e1dn\u00e1 dal\u0161\u00ed ID DTM nejsou k dispozici"},

    { ER_NOT_SUPPORTED,
      "Nepodporov\u00e1no: {0}"},

    { ER_NODE_NON_NULL,
      "Uzel pou\u017eit\u00fd ve funkci getDTMHandleFromNode mus\u00ed m\u00edt hodnotu not-null"},

    { ER_COULD_NOT_RESOLVE_NODE,
      "Uzel nelze p\u0159elo\u017eit do manipul\u00e1toru"},

    { ER_STARTPARSE_WHILE_PARSING,
       "B\u011bhem anal\u00fdzy nelze volat funkci startParse."},

    { ER_STARTPARSE_NEEDS_SAXPARSER,
       "Funkce startParse vy\u017eaduje SAXParser s hodnotou not-null."},

    { ER_COULD_NOT_INIT_PARSER,
       "nelze inicializovat analyz\u00e1tor s: "},

    { ER_EXCEPTION_CREATING_POOL,
       "v\u00fdjimka p\u0159i vytv\u00e1\u0159en\u00ed nov\u00e9 instance spole\u010dn\u00e9 oblasti"},

    { ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
       "Cesta obsahuje neplatnou escape sekvenci"},

    { ER_SCHEME_REQUIRED,
       "Je vy\u017eadov\u00e1no sch\u00e9ma!"},

    { ER_NO_SCHEME_IN_URI,
       "V URI nebylo nalezeno \u017e\u00e1dn\u00e9 sch\u00e9ma: {0}"},

    { ER_NO_SCHEME_INURI,
       "V URI nebylo nalezeno \u017e\u00e1dn\u00e9 sch\u00e9ma"},

    { ER_PATH_INVALID_CHAR,
       "Cesta obsahuje neplatn\u00fd znak: {0}"},

    { ER_SCHEME_FROM_NULL_STRING,
       "Nelze nastavit sch\u00e9ma \u0159et\u011bzce s hodnotou null."},

    { ER_SCHEME_NOT_CONFORMANT,
       "Sch\u00e9ma nevyhovuje."},

    { ER_HOST_ADDRESS_NOT_WELLFORMED,
       "Adresa hostitele m\u00e1 nespr\u00e1vn\u00fd form\u00e1t."},

    { ER_PORT_WHEN_HOST_NULL,
       "M\u00e1-li hostitel hodnotu null, nelze nastavit port."},

    { ER_INVALID_PORT,
       "Neplatn\u00e9 \u010d\u00edslo portu."},

    { ER_FRAG_FOR_GENERIC_URI,
       "Fragment lze nastavit jen u generick\u00e9ho URI."},

    { ER_FRAG_WHEN_PATH_NULL,
       "M\u00e1-li cesta hodnotu null, nelze nastavit fragment."},

    { ER_FRAG_INVALID_CHAR,
       "Fragment obsahuje neplatn\u00fd znak."},

    { ER_PARSER_IN_USE,
      "Analyz\u00e1tor se ji\u017e pou\u017e\u00edv\u00e1."},

    { ER_CANNOT_CHANGE_WHILE_PARSING,
      "B\u011bhem anal\u00fdzy nelze m\u011bnit {0} {1}."},

    { ER_SELF_CAUSATION_NOT_PERMITTED,
      "Zp\u016fsoben\u00ed sama sebe (self-causation) nen\u00ed povoleno"},

    { ER_NO_USERINFO_IF_NO_HOST,
      "Nen\u00ed-li ur\u010den hostitel, nelze zadat \u00fadaje o u\u017eivateli."},

    { ER_NO_PORT_IF_NO_HOST,
      "Nen\u00ed-li ur\u010den hostitel, nelze zadat port."},

    { ER_NO_QUERY_STRING_IN_PATH,
      "V \u0159et\u011bzci cesty a dotazu nelze zadat \u0159et\u011bzec dotazu."},

    { ER_NO_FRAGMENT_STRING_IN_PATH,
      "Fragment nelze ur\u010dit z\u00e1rove\u0148 v cest\u011b i ve fragmentu."},

    { ER_CANNOT_INIT_URI_EMPTY_PARMS,
      "URI nelze inicializovat s pr\u00e1zdn\u00fdmi parametry."},

    { ER_METHOD_NOT_SUPPORTED,
      "Prozat\u00edm nepodporovan\u00e1 metoda. "},

    { ER_INCRSAXSRCFILTER_NOT_RESTARTABLE,
      "Filtr IncrementalSAXSource_Filter nelze aktu\u00e1ln\u011b znovu spustit."},

    { ER_XMLRDR_NOT_BEFORE_STARTPARSE,
      "P\u0159ed po\u017eadavkem startParse nen\u00ed XMLReader."},

    { ER_AXIS_TRAVERSER_NOT_SUPPORTED,
      "Nepodporovan\u00e1 osa pr\u016fchodu: {0}"},

    { ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER,
      "Prvek ListingErrorHandler byl vytvo\u0159en s funkc\u00ed PrintWriter s hodnotou null!"},

    { ER_SYSTEMID_UNKNOWN,
      "Nezn\u00e1m\u00fd identifik\u00e1tor SystemId"},

    { ER_LOCATION_UNKNOWN,
      "Chyba se vyskytla na nezn\u00e1m\u00e9m m\u00edst\u011b"},

    { ER_PREFIX_MUST_RESOLVE,
      "P\u0159edponu mus\u00ed b\u00fdt mo\u017eno p\u0159elo\u017eit do oboru n\u00e1zv\u016f: {0}"},

    { ER_CREATEDOCUMENT_NOT_SUPPORTED,
      "Funkce XPathContext nepodporuje funkci createDocument()!"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT,
      "Potomek atributu nem\u00e1 dokument vlastn\u00edka!"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT,
      "Potomek atributu nem\u00e1 prvek dokumentu vlastn\u00edka!"},

    { ER_CANT_OUTPUT_TEXT_BEFORE_DOC,
      "Varov\u00e1n\u00ed: v\u00fdstup textu nem\u016f\u017ee p\u0159edch\u00e1zet prvku dokumentu! Ignorov\u00e1no..."},

    { ER_CANT_HAVE_MORE_THAN_ONE_ROOT,
      "DOM nem\u016f\u017ee m\u00edt n\u011bkolik ko\u0159en\u016f!"},

    { ER_ARG_LOCALNAME_NULL,
       "Argument 'localName' m\u00e1 hodnotu null"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The localname is the portion after the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_LOCALNAME_INVALID,
       "Hodnota Localname ve funkci QNAME by m\u011bla b\u00fdt platn\u00fdm prvkem NCName"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The prefix is the portion before the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_PREFIX_INVALID,
       "P\u0159edpona ve funkci QNAME by m\u011bla b\u00fdt platn\u00fdm prvkem NCName"},

    { "BAD_CODE", "Parametr funkce createMessage je mimo limit"},
    { "FORMAT_FAILED", "P\u0159i vol\u00e1n\u00ed funkce messageFormat do\u0161lo k v\u00fdjimce"},
    { "line", "\u0158\u00e1dek #"},
    { "column","Sloupec #"},

    {ER_SERIALIZER_NOT_CONTENTHANDLER,
      "T\u0159\u00edda serializace ''{0}'' neimplementuje org.xml.sax.ContentHandler."},

    {ER_RESOURCE_COULD_NOT_FIND,
      "Nelze naj\u00edt zdroj [ {0} ].\n {1}" },

    {ER_RESOURCE_COULD_NOT_LOAD,
      "Nelze zav\u00e9st zdroj [ {0} ]: {1} \n {2} \t {3}" },

    {ER_BUFFER_SIZE_LESSTHAN_ZERO,
      "Velikost vyrovn\u00e1vac\u00ed pam\u011bti <=0" },

    {ER_INVALID_UTF16_SURROGATE,
      "Byla zji\u0161t\u011bna neplatn\u00e1 n\u00e1hrada UTF-16: {0} ?" },

    {ER_OIERROR,
      "Chyba vstupu/v\u00fdstupu" },

    {ER_ILLEGAL_ATTRIBUTE_POSITION,
      "Nelze p\u0159idat atribut {0} po uzlech potomk\u016f ani p\u0159ed t\u00edm, ne\u017e je vytvo\u0159en prvek. Atribut bude ignorov\u00e1n."},

      /*
       * Note to translators:  The stylesheet contained a reference to a
       * namespace prefix that was undefined.  The value of the substitution
       * text is the name of the prefix.
       */
    {ER_NAMESPACE_PREFIX,
      "Obor n\u00e1zv\u016f pro p\u0159edponu ''{0}'' nebyl deklarov\u00e1n." },
      /*
       * Note to translators:  This message is reported if the stylesheet
       * being processed attempted to construct an XML document with an
       * attribute in a place other than on an element.  The substitution text
       * specifies the name of the attribute.
       */
    {ER_STRAY_ATTRIBUTE,
      "Atribut ''{0}'' je vn\u011b prvku." },

      /*
       * Note to translators:  As with the preceding message, a namespace
       * declaration has the form of an attribute and is only permitted to
       * appear on an element.  The substitution text {0} is the namespace
       * prefix and {1} is the URI that was being used in the erroneous
       * namespace declaration.
       */
    {ER_STRAY_NAMESPACE,
      "Deklarace oboru n\u00e1zv\u016f ''{0}''=''{1}'' je vn\u011b prvku." },

    {ER_COULD_NOT_LOAD_RESOURCE,
      "Nelze zav\u00e9st ''{0}'' (zkontrolujte prom\u011bnnou CLASSPATH), proto se pou\u017e\u00edvaj\u00ed pouze v\u00fdchoz\u00ed hodnoty"},

    {ER_COULD_NOT_LOAD_METHOD_PROPERTY,
      "Nelze na\u010d\u00edst soubor vlastnost\u00ed ''{0}'' pro v\u00fdstupn\u00ed metodu ''{1}'' (zkontrolujte prom\u011bnnou CLASSPATH)." }


  };

  /**
   * Get the lookup table for error messages
   *
   * @return The association list.
   */
  public Object[][] getContents()
  {
    return _contents;
  }

}
