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
public class XMLErrorResources_ca extends ListResourceBundle
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
      "Aquesta funci\u00f3 no t\u00e9 suport. "},

    { ER_CANNOT_OVERWRITE_CAUSE,
      "No es pot sobreescriure una causa "},

    { ER_NO_DEFAULT_IMPL,
      "No s'ha trobat cap implementaci\u00f3 per defecte "},

    { ER_CHUNKEDINTARRAY_NOT_SUPPORTED,
      "En l''actualitat ChunkedIntArray({0}) no t\u00e9 suport "},

    { ER_OFFSET_BIGGER_THAN_SLOT,
      "El despla\u00e7ament \u00e9s m\u00e9s gran que la ranura "},

    { ER_COROUTINE_NOT_AVAIL,
      "Coroutine no est\u00e0 disponible, id={0} "},

    { ER_COROUTINE_CO_EXIT,
      "CoroutineManager ha rebut una petici\u00f3 co_exit() "},

    { ER_COJOINROUTINESET_FAILED,
      "S'ha produ\u00eft un error a co_joinCoroutineSet() "},

    { ER_COROUTINE_PARAM,
      "Error de par\u00e0metre coroutine ({0}) "},

    { ER_PARSER_DOTERMINATE_ANSWERS,
      "\nUNEXPECTED: doTerminate de l''analitzador respon {0}"},

    { ER_NO_PARSE_CALL_WHILE_PARSING,
      "L'an\u00e0lisi no es pot cridar mentre s'est\u00e0 duent a terme "},

    { ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Error: l''iterador de tipus de l''eix {0} no s''ha implementat "},

    { ER_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Error: l''iterador de l''eix {0} no s''ha implementat "},

    { ER_ITERATOR_CLONE_NOT_SUPPORTED,
      "El clonatge de l'iterador no t\u00e9 suport "},

    { ER_UNKNOWN_AXIS_TYPE,
      "Tipus de commutaci\u00f3 de l''eix desconeguda: {0} "},

    { ER_AXIS_NOT_SUPPORTED,
      "La commutaci\u00f3 de l''eix no t\u00e9 suport: {0} "},

    { ER_NO_DTMIDS_AVAIL,
      "No hi ha m\u00e9s ID de DTM disponibles "},

    { ER_NOT_SUPPORTED,
      "No t\u00e9 suport: {0} "},

    { ER_NODE_NON_NULL,
      "El node no ha de ser nul per a getDTMHandleFromNode "},

    { ER_COULD_NOT_RESOLVE_NODE,
      "No s'ha pogut resoldre el node en un manejador "},

    { ER_STARTPARSE_WHILE_PARSING,
       "startParse no es pot cridar mentre s'est\u00e0 duent a terme l'an\u00e0lisi "},

    { ER_STARTPARSE_NEEDS_SAXPARSER,
       "startParse necessita un SAXParser que no sigui nul "},

    { ER_COULD_NOT_INIT_PARSER,
       "No s'ha pogut inicialitzar l'analitzador amb "},

    { ER_EXCEPTION_CREATING_POOL,
       "S'ha produ\u00eft una excepci\u00f3 en crear una nova inst\u00e0ncia de l'agrupaci\u00f3 "},

    { ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
       "La via d'acc\u00e9s cont\u00e9 una seq\u00fc\u00e8ncia d'escapament no v\u00e0lida "},

    { ER_SCHEME_REQUIRED,
       "Es necessita l'esquema "},

    { ER_NO_SCHEME_IN_URI,
       "No s''ha trobat cap esquema a l''URI: {0} "},

    { ER_NO_SCHEME_INURI,
       "No s'ha trobat cap esquema a l'URI "},

    { ER_PATH_INVALID_CHAR,
       "La via d'acc\u00e9s cont\u00e9 un car\u00e0cter no v\u00e0lid {0} "},

    { ER_SCHEME_FROM_NULL_STRING,
       "No es pot establir un esquema des d'una cadena nul\u00b7la "},

    { ER_SCHEME_NOT_CONFORMANT,
       "L'esquema no t\u00e9 conformitat. "},

    { ER_HOST_ADDRESS_NOT_WELLFORMED,
       "El sistema principal no t\u00e9 una adre\u00e7a ben formada "},

    { ER_PORT_WHEN_HOST_NULL,
       "El port no es pot establir quan el sistema principal \u00e9s nul "},

    { ER_INVALID_PORT,
       "N\u00famero de port no v\u00e0lid "},

    { ER_FRAG_FOR_GENERIC_URI,
       "El fragment nom\u00e9s es pot establir per a un URI gen\u00e8ric "},

    { ER_FRAG_WHEN_PATH_NULL,
       "El fragment no es pot establir si la via d'acc\u00e9s \u00e9s nul\u00b7la "},

    { ER_FRAG_INVALID_CHAR,
       "El fragment cont\u00e9 un car\u00e0cter no v\u00e0lid "},

    { ER_PARSER_IN_USE,
      "L'analitzador ja s'est\u00e0 utilitzant "},

    { ER_CANNOT_CHANGE_WHILE_PARSING,
      "No es pot modificar {0} {1} mentre es du a terme l''an\u00e0lisi "},

    { ER_SELF_CAUSATION_NOT_PERMITTED,
      "La causalitat pr\u00f2pia no est\u00e0 permesa. "},

    { ER_NO_USERINFO_IF_NO_HOST,
      "No es pot especificar informaci\u00f3 de l'usuari si no s'especifica el sistema principal "},

    { ER_NO_PORT_IF_NO_HOST,
      "No es pot especificar el port si no s'especifica el sistema principal "},

    { ER_NO_QUERY_STRING_IN_PATH,
      "No es pot especificar una cadena de consulta en la via d'acc\u00e9s i la cadena de consulta "},

    { ER_NO_FRAGMENT_STRING_IN_PATH,
      "No es pot especificar un fragment tant en la via d'acc\u00e9s com en el fragment "},

    { ER_CANNOT_INIT_URI_EMPTY_PARMS,
      "No es pot inicialitzar l'URI amb par\u00e0metres buits "},

    { ER_METHOD_NOT_SUPPORTED,
      "Aquest m\u00e8tode encara no t\u00e9 suport "},

    { ER_INCRSAXSRCFILTER_NOT_RESTARTABLE,
      "Ara mateix no es pot reiniciar IncrementalSAXSource_Filter "},

    { ER_XMLRDR_NOT_BEFORE_STARTPARSE,
      "XMLReader no es pot produir abans de la petici\u00f3 d'startParse "},

    { ER_AXIS_TRAVERSER_NOT_SUPPORTED,
      "La commutaci\u00f3 de l''eix no t\u00e9 suport: {0} "},

    { ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER,
      "S''ha creat ListingErrorHandler amb PrintWriter nul "},

    { ER_SYSTEMID_UNKNOWN,
      "ID del sistema (SystemId) desconegut "},

    { ER_LOCATION_UNKNOWN,
      "Ubicaci\u00f3 de l'error desconeguda"},

    { ER_PREFIX_MUST_RESOLVE,
      "El prefix s''ha de resoldre en un espai de noms: {0} "},

    { ER_CREATEDOCUMENT_NOT_SUPPORTED,
      "createDocument() no t\u00e9 suport a XPathContext "},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT,
      "El subordinat de l'atribut no t\u00e9 un document de propietari. "},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT,
      "El subordinat de l'atribut no t\u00e9 un element de document de propietari. "},

    { ER_CANT_OUTPUT_TEXT_BEFORE_DOC,
      "Av\u00eds: no es pot produir text abans de l'element de document. Es passa per alt. "},

    { ER_CANT_HAVE_MORE_THAN_ONE_ROOT,
      "No hi pot haver m\u00e9s d'una arrel en un DOM. "},

    { ER_ARG_LOCALNAME_NULL,
       "L'argument 'localName' \u00e9s nul. "},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The localname is the portion after the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_LOCALNAME_INVALID,
       "El nom local de QNAME ha de ser un NCName v\u00e0lid. "},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The prefix is the portion before the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_PREFIX_INVALID,
       "El prefix de QNAME ha de ser un NCName v\u00e0lid. "},

    { "BAD_CODE", "El par\u00e0metre de createMessage estava fora dels l\u00edmits. "},
    { "FORMAT_FAILED", "S'ha generat una excepci\u00f3 durant la crida messageFormat. "},
    { "line", "L\u00ednia n\u00fam. "},
    { "column","Columna n\u00fam. "},

    {ER_SERIALIZER_NOT_CONTENTHANDLER,
      "La classe de serialitzador ''{0}'' no implementa org.xml.sax.ContentHandler."},

    {ER_RESOURCE_COULD_NOT_FIND,
      "No s''ha trobat el recurs [ {0} ].\n {1}" },

    {ER_RESOURCE_COULD_NOT_LOAD,
      "El recurs [ {0} ] no s''ha pogut carregar: {1} \n {2} \t {3}" },

    {ER_BUFFER_SIZE_LESSTHAN_ZERO,
      "Grand\u00e0ria del buffer <=0 " },

    {ER_INVALID_UTF16_SURROGATE,
      "S''ha detectat un suplent UTF-16 no v\u00e0lid: {0} ? " },

    {ER_OIERROR,
      "Error d'E/S " },

    {ER_ILLEGAL_ATTRIBUTE_POSITION,
      "No es pot afegir l''atribut {0} despr\u00e9s dels nodes subordinats o abans que es produeixi un element. Es passar\u00e0 per alt l''atribut. "},

      /*
       * Note to translators:  The stylesheet contained a reference to a
       * namespace prefix that was undefined.  The value of the substitution
       * text is the name of the prefix.
       */
    {ER_NAMESPACE_PREFIX,
      "L''espai de noms del prefix ''{0}'' no s''ha declarat." },
      /*
       * Note to translators:  This message is reported if the stylesheet
       * being processed attempted to construct an XML document with an
       * attribute in a place other than on an element.  The substitution text
       * specifies the name of the attribute.
       */
    {ER_STRAY_ATTRIBUTE,
      "L''atribut ''{0}'' es troba fora de l''element." },

      /*
       * Note to translators:  As with the preceding message, a namespace
       * declaration has the form of an attribute and is only permitted to
       * appear on an element.  The substitution text {0} is the namespace
       * prefix and {1} is the URI that was being used in the erroneous
       * namespace declaration.
       */
    {ER_STRAY_NAMESPACE,
      "La declaraci\u00f3 d''espai de noms ''{0}''=''{1}'' es troba fora de l''element." },

    {ER_COULD_NOT_LOAD_RESOURCE,
      "No s''ha pogut carregar ''{0}'' (comproveu la CLASSPATH); ara s''estan fent servir els valors per defecte."},

    {ER_COULD_NOT_LOAD_METHOD_PROPERTY,
      "No s''ha pogut carregar el fitxer de propietats ''{0}'' del m\u00e8tode de sortida ''{1}'' (comproveu la CLASSPATH)" }


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
