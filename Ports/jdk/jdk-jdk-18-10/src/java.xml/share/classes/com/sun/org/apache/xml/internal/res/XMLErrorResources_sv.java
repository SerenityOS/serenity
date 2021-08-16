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
public class XMLErrorResources_sv extends ListResourceBundle
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
      "Funktionen st\u00F6ds inte!"},

    { ER_CANNOT_OVERWRITE_CAUSE,
      "Orsak kan inte skrivas \u00F6ver"},

    { ER_NO_DEFAULT_IMPL,
      "Hittade ingen standardimplementering "},

    { ER_CHUNKEDINTARRAY_NOT_SUPPORTED,
      "ChunkedIntArray({0}) underst\u00F6ds f\u00F6r n\u00E4rvarande inte"},

    { ER_OFFSET_BIGGER_THAN_SLOT,
      "Offset st\u00F6rre \u00E4n plats"},

    { ER_COROUTINE_NOT_AVAIL,
      "Sidorutin \u00E4r inte tillg\u00E4nglig, id={0}"},

    { ER_COROUTINE_CO_EXIT,
      "CoroutineManager har tagit emot co_exit()-beg\u00E4ran"},

    { ER_COJOINROUTINESET_FAILED,
      "co_joinCoroutineSet() utf\u00F6rdes inte"},

    { ER_COROUTINE_PARAM,
      "Parameterfel f\u00F6r sidorutin ({0})"},

    { ER_PARSER_DOTERMINATE_ANSWERS,
      "\nUNEXPECTED: Parsersvar {0} f\u00F6r doTerminate"},

    { ER_NO_PARSE_CALL_WHILE_PARSING,
      "parse f\u00E5r inte anropas medan tolkning sker"},

    { ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Fel: typad iterator f\u00F6r axeln {0} har inte implementerats"},

    { ER_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Fel: iterator f\u00F6r axeln {0} har inte implementerats "},

    { ER_ITERATOR_CLONE_NOT_SUPPORTED,
      "Iteratorklon underst\u00F6ds inte"},

    { ER_UNKNOWN_AXIS_TYPE,
      "Ok\u00E4nd axeltraverstyp: {0}"},

    { ER_AXIS_NOT_SUPPORTED,
      "Axeltravers underst\u00F6ds inte: {0}"},

    { ER_NO_DTMIDS_AVAIL,
      "Inga fler DTM-id:n \u00E4r tillg\u00E4ngliga"},

    { ER_NOT_SUPPORTED,
      "Underst\u00F6ds inte: {0}"},

    { ER_NODE_NON_NULL,
      "Nod m\u00E5ste vara icke-null f\u00F6r getDTMHandleFromNode"},

    { ER_COULD_NOT_RESOLVE_NODE,
      "Kunde inte matcha noden med en referens"},

    { ER_STARTPARSE_WHILE_PARSING,
       "startParse f\u00E5r inte anropas medan tolkning sker"},

    { ER_STARTPARSE_NEEDS_SAXPARSER,
       "startParse beh\u00F6ver en SAXParser som \u00E4r icke-null"},

    { ER_COULD_NOT_INIT_PARSER,
       "kunde inte initiera parser med"},

    { ER_EXCEPTION_CREATING_POOL,
       "undantag skapar ny instans f\u00F6r pool"},

    { ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
       "S\u00F6kv\u00E4gen inneh\u00E5ller en ogiltig escape-sekvens"},

    { ER_SCHEME_REQUIRED,
       "Schema kr\u00E4vs!"},

    { ER_NO_SCHEME_IN_URI,
       "Schema saknas i URI: {0}"},

    { ER_NO_SCHEME_INURI,
       "Schema saknas i URI"},

    { ER_PATH_INVALID_CHAR,
       "S\u00F6kv\u00E4gen inneh\u00E5ller ett ogiltigt tecken: {0}"},

    { ER_SCHEME_FROM_NULL_STRING,
       "Kan inte st\u00E4lla in schema fr\u00E5n null-str\u00E4ng"},

    { ER_SCHEME_NOT_CONFORMANT,
       "Schemat \u00E4r inte likformigt."},

    { ER_HOST_ADDRESS_NOT_WELLFORMED,
       "V\u00E4rd \u00E4r inte en v\u00E4lformulerad adress"},

    { ER_PORT_WHEN_HOST_NULL,
       "Port kan inte st\u00E4llas in n\u00E4r v\u00E4rd \u00E4r null"},

    { ER_INVALID_PORT,
       "Ogiltigt portnummer"},

    { ER_FRAG_FOR_GENERIC_URI,
       "Fragment kan bara st\u00E4llas in f\u00F6r en allm\u00E4n URI"},

    { ER_FRAG_WHEN_PATH_NULL,
       "Fragment kan inte st\u00E4llas in n\u00E4r s\u00F6kv\u00E4g \u00E4r null"},

    { ER_FRAG_INVALID_CHAR,
       "Fragment inneh\u00E5ller ett ogiltigt tecken"},

    { ER_PARSER_IN_USE,
      "Parser anv\u00E4nds redan"},

    { ER_CANNOT_CHANGE_WHILE_PARSING,
      "Kan inte \u00E4ndra {0} {1} medan tolkning sker"},

    { ER_SELF_CAUSATION_NOT_PERMITTED,
      "Sj\u00E4lvorsakande inte till\u00E5ten"},

    { ER_NO_USERINFO_IF_NO_HOST,
      "Anv\u00E4ndarinfo f\u00E5r inte anges om v\u00E4rden inte \u00E4r angiven"},

    { ER_NO_PORT_IF_NO_HOST,
      "Port f\u00E5r inte anges om v\u00E4rden inte \u00E4r angiven"},

    { ER_NO_QUERY_STRING_IN_PATH,
      "Fr\u00E5gestr\u00E4ng kan inte anges i b\u00E5de s\u00F6kv\u00E4gen och fr\u00E5gestr\u00E4ngen"},

    { ER_NO_FRAGMENT_STRING_IN_PATH,
      "Fragment kan inte anges i b\u00E5de s\u00F6kv\u00E4gen och fragmentet"},

    { ER_CANNOT_INIT_URI_EMPTY_PARMS,
      "Kan inte initiera URI med tomma parametrar"},

    { ER_METHOD_NOT_SUPPORTED,
      "Metoden st\u00F6ds \u00E4nnu inte "},

    { ER_INCRSAXSRCFILTER_NOT_RESTARTABLE,
      "IncrementalSAXSource_Filter kan f\u00F6r n\u00E4rvarande inte startas om"},

    { ER_XMLRDR_NOT_BEFORE_STARTPARSE,
      "XMLReader inte f\u00F6re startParse-beg\u00E4ran"},

    { ER_AXIS_TRAVERSER_NOT_SUPPORTED,
      "Axeltravers underst\u00F6ds inte: {0}"},

    { ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER,
      "ListingErrorHandler skapad med null PrintWriter!"},

    { ER_SYSTEMID_UNKNOWN,
      "SystemId ok\u00E4nt"},

    { ER_LOCATION_UNKNOWN,
      "Platsen f\u00F6r felet \u00E4r ok\u00E4nd"},

    { ER_PREFIX_MUST_RESOLVE,
      "Prefix m\u00E5ste matchas till en namnrymd: {0}"},

    { ER_CREATEDOCUMENT_NOT_SUPPORTED,
      "createDocument() st\u00F6ds inte i XPathContext!"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT,
      "Underordnat attribut har inget \u00E4gardokument!"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT,
      "Underordnat attribut har inget \u00E4gardokumentelement!"},

    { ER_CANT_OUTPUT_TEXT_BEFORE_DOC,
      "Varning: utdatatext kan inte skrivas ut f\u00F6re dokumentelement! Ignoreras..."},

    { ER_CANT_HAVE_MORE_THAN_ONE_ROOT,
      "En DOM kan inte ha fler \u00E4n en rot!"},

    { ER_ARG_LOCALNAME_NULL,
       "Argumentet 'localName' \u00E4r null"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The localname is the portion after the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_LOCALNAME_INVALID,
       "Localname i QNAME b\u00F6r vara giltigt NCName"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The prefix is the portion before the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_PREFIX_INVALID,
       "Prefix i QNAME b\u00F6r vara giltigt NCName"},

    { ER_NAME_CANT_START_WITH_COLON,
      "Namnet kan inte b\u00F6rja med kolon"},

    { "BAD_CODE", "Parameter f\u00F6r createMessage ligger utanf\u00F6r gr\u00E4nsv\u00E4rdet"},
    { "FORMAT_FAILED", "Undantag utl\u00F6st vid messageFormat-anrop"},
    { "line", "Rad nr"},
    { "column","Kolumn nr"},

    {ER_SERIALIZER_NOT_CONTENTHANDLER,
      "Serializerklassen ''{0}'' implementerar inte org.xml.sax.ContentHandler."},

    {ER_RESOURCE_COULD_NOT_FIND,
      "Resursen [ {0} ] kunde inte h\u00E4mtas.\n {1}" },

    {ER_RESOURCE_COULD_NOT_LOAD,
      "Resursen [ {0} ] kunde inte laddas: {1} \n {2} \t {3}" },

    {ER_BUFFER_SIZE_LESSTHAN_ZERO,
      "Buffertstorlek <=0" },

    {ER_INVALID_UTF16_SURROGATE,
      "Ogiltigt UTF-16-surrogat uppt\u00E4ckt: {0} ?" },

    {ER_OIERROR,
      "IO-fel" },

    {ER_ILLEGAL_ATTRIBUTE_POSITION,
      "Kan inte l\u00E4gga till attributet {0} efter underordnade noder eller innan ett element har skapats. Attributet ignoreras."},

      /*
       * Note to translators:  The stylesheet contained a reference to a
       * namespace prefix that was undefined.  The value of the substitution
       * text is the name of the prefix.
       */
    {ER_NAMESPACE_PREFIX,
      "Namnrymd f\u00F6r prefix ''{0}'' har inte deklarerats." },
      /*
       * Note to translators:  This message is reported if the stylesheet
       * being processed attempted to construct an XML document with an
       * attribute in a place other than on an element.  The substitution text
       * specifies the name of the attribute.
       */
    {ER_STRAY_ATTRIBUTE,
      "Attributet ''{0}'' finns utanf\u00F6r elementet." },

      /*
       * Note to translators:  As with the preceding message, a namespace
       * declaration has the form of an attribute and is only permitted to
       * appear on an element.  The substitution text {0} is the namespace
       * prefix and {1} is the URI that was being used in the erroneous
       * namespace declaration.
       */
    {ER_STRAY_NAMESPACE,
      "Namnrymdsdeklarationen ''{0}''=''{1}'' finns utanf\u00F6r element." },

    {ER_COULD_NOT_LOAD_RESOURCE,
      "Kunde inte ladda ''{0}'' (kontrollera CLASSPATH), anv\u00E4nder nu enbart standardv\u00E4rden"},

    { ER_ILLEGAL_CHARACTER,
       "F\u00F6rs\u00F6k att skriva utdatatecken med integralv\u00E4rdet {0} som inte \u00E4r representerat i angiven utdatakodning av {1}."},

    {ER_COULD_NOT_LOAD_METHOD_PROPERTY,
      "Kunde inte ladda egenskapsfilen ''{0}'' f\u00F6r utdatametoden ''{1}'' (kontrollera CLASSPATH)" }


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
