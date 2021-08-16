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
public class XMLErrorResources_es extends ListResourceBundle
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
      "Funci\u00F3n no soportada."},

    { ER_CANNOT_OVERWRITE_CAUSE,
      "No se puede sobrescribir la causa"},

    { ER_NO_DEFAULT_IMPL,
      "No se ha encontrado la implantaci\u00F3n por defecto "},

    { ER_CHUNKEDINTARRAY_NOT_SUPPORTED,
      "ChunkedIntArray({0}) no est\u00E1 soportado actualmente"},

    { ER_OFFSET_BIGGER_THAN_SLOT,
      "El desplazamiento es mayor que la ranura"},

    { ER_COROUTINE_NOT_AVAIL,
      "Corrutina no disponible, id={0}"},

    { ER_COROUTINE_CO_EXIT,
      "CoroutineManager ha recibido la solicitud co_exit()"},

    { ER_COJOINROUTINESET_FAILED,
      "Fallo de co_joinCoroutineSet()"},

    { ER_COROUTINE_PARAM,
      "Error de par\u00E1metro de corrutina ({0})"},

    { ER_PARSER_DOTERMINATE_ANSWERS,
      "\nINESPERADO: respuestas doTerminate del analizador {0}"},

    { ER_NO_PARSE_CALL_WHILE_PARSING,
      "no se puede realizar un an\u00E1lisis mientras se lleva a cabo otro"},

    { ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Error: el iterador introducido para el eje {0} no se ha implantado"},

    { ER_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Error: el iterador para el eje {0} no se ha implantado "},

    { ER_ITERATOR_CLONE_NOT_SUPPORTED,
      "La clonaci\u00F3n del iterador no est\u00E1 soportada"},

    { ER_UNKNOWN_AXIS_TYPE,
      "Tipo de recorrido de eje desconocido: {0}"},

    { ER_AXIS_NOT_SUPPORTED,
      "Traverser de eje no soportado: {0}"},

    { ER_NO_DTMIDS_AVAIL,
      "No hay m\u00E1s identificadores de DTM disponibles"},

    { ER_NOT_SUPPORTED,
      "No soportado: {0}"},

    { ER_NODE_NON_NULL,
      "El nodo debe ser no nulo para getDTMHandleFromNode"},

    { ER_COULD_NOT_RESOLVE_NODE,
      "No se ha podido resolver el nodo en un identificador"},

    { ER_STARTPARSE_WHILE_PARSING,
       "startParse no puede llamarse durante el an\u00E1lisis"},

    { ER_STARTPARSE_NEEDS_SAXPARSER,
       "startParse necesita un SAXParser no nulo"},

    { ER_COULD_NOT_INIT_PARSER,
       "no se ha podido inicializar el analizador con"},

    { ER_EXCEPTION_CREATING_POOL,
       "excepci\u00F3n al crear la nueva instancia para el pool"},

    { ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
       "La ruta de acceso contiene una secuencia de escape no v\u00E1lida"},

    { ER_SCHEME_REQUIRED,
       "Se necesita un esquema."},

    { ER_NO_SCHEME_IN_URI,
       "No se ha encontrado un esquema en el URI: {0}"},

    { ER_NO_SCHEME_INURI,
       "No se ha encontrado un esquema en el URI"},

    { ER_PATH_INVALID_CHAR,
       "La ruta de acceso contiene un car\u00E1cter no v\u00E1lido: {0}"},

    { ER_SCHEME_FROM_NULL_STRING,
       "No se puede definir un esquema a partir de una cadena nula"},

    { ER_SCHEME_NOT_CONFORMANT,
       "El esquema no es v\u00E1lido."},

    { ER_HOST_ADDRESS_NOT_WELLFORMED,
       "El formato de la direcci\u00F3n de host no es correcto"},

    { ER_PORT_WHEN_HOST_NULL,
       "No se puede definir el puerto si el host es nulo"},

    { ER_INVALID_PORT,
       "N\u00FAmero de puerto no v\u00E1lido"},

    { ER_FRAG_FOR_GENERIC_URI,
       "S\u00F3lo se puede definir el fragmento para un URI gen\u00E9rico"},

    { ER_FRAG_WHEN_PATH_NULL,
       "No se puede definir el fragmento si la ruta de acceso es nula"},

    { ER_FRAG_INVALID_CHAR,
       "El fragmento contiene un car\u00E1cter no v\u00E1lido"},

    { ER_PARSER_IN_USE,
      "El analizador ya se est\u00E1 utilizando"},

    { ER_CANNOT_CHANGE_WHILE_PARSING,
      "No se puede cambiar {0} {1} durante el an\u00E1lisis"},

    { ER_SELF_CAUSATION_NOT_PERMITTED,
      "La autocausalidad no est\u00E1 permitida"},

    { ER_NO_USERINFO_IF_NO_HOST,
      "No se puede especificar la informaci\u00F3n de usuario si no se ha especificado el host"},

    { ER_NO_PORT_IF_NO_HOST,
      "No se puede especificar el puerto si no se ha especificado el host"},

    { ER_NO_QUERY_STRING_IN_PATH,
      "No se puede especificar la cadena de consulta en la ruta de acceso y en la cadena de consulta"},

    { ER_NO_FRAGMENT_STRING_IN_PATH,
      "No se puede especificar el fragmento en la ruta de acceso y en el fragmento"},

    { ER_CANNOT_INIT_URI_EMPTY_PARMS,
      "No se puede inicializar el URI con par\u00E1metros vac\u00EDos"},

    { ER_METHOD_NOT_SUPPORTED,
      "M\u00E9todo no soportado a\u00FAn "},

    { ER_INCRSAXSRCFILTER_NOT_RESTARTABLE,
      "IncrementalSAXSource_Filter no se puede reiniciar actualmente"},

    { ER_XMLRDR_NOT_BEFORE_STARTPARSE,
      "XMLReader no anterior a la solicitud startParse"},

    { ER_AXIS_TRAVERSER_NOT_SUPPORTED,
      "Traverser de eje no soportado: {0}"},

    { ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER,
      "ListingErrorHandler se ha creado con un valor de PrintWriter nulo"},

    { ER_SYSTEMID_UNKNOWN,
      "Identificador de Sistema Desconocido"},

    { ER_LOCATION_UNKNOWN,
      "Ubicaci\u00F3n del error desconocida"},

    { ER_PREFIX_MUST_RESOLVE,
      "El prefijo se debe resolver en un espacio de nombres: {0}"},

    { ER_CREATEDOCUMENT_NOT_SUPPORTED,
      "createDocument() no soportado en XPathContext"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT,
      "El atributo child no tiene un documento de propietario."},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT,
      "El atributo child no tiene un elemento de documento de propietario."},

    { ER_CANT_OUTPUT_TEXT_BEFORE_DOC,
      "Advertencia: no se puede realizar la salida de texto antes del elemento del documento. Ignorando..."},

    { ER_CANT_HAVE_MORE_THAN_ONE_ROOT,
      "No se puede tener m\u00E1s de una ra\u00EDz en un DOM."},

    { ER_ARG_LOCALNAME_NULL,
       "El argumento 'localName' es nulo"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The localname is the portion after the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_LOCALNAME_INVALID,
       "El nombre local de QNAME debe ser un NCName v\u00E1lido"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The prefix is the portion before the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_PREFIX_INVALID,
       "El prefijo de QNAME debe ser un NCName v\u00E1lido"},

    { ER_NAME_CANT_START_WITH_COLON,
      "El nombre no puede empezar con dos puntos"},

    { "BAD_CODE", "El par\u00E1metro para crear un mensaje est\u00E1 fuera de los l\u00EDmites"},
    { "FORMAT_FAILED", "Se ha emitido una excepci\u00F3n durante la llamada a messageFormat"},
    { "line", "N\u00BA de L\u00EDnea"},
    { "column","N\u00BA de Columna"},

    {ER_SERIALIZER_NOT_CONTENTHANDLER,
      "La clase de serializador ''{0}'' no implanta org.xml.sax.ContentHandler."},

    {ER_RESOURCE_COULD_NOT_FIND,
      "No se ha encontrado el recurso [ {0} ].\n {1}" },

    {ER_RESOURCE_COULD_NOT_LOAD,
      "No se ha podido cargar el recurso [ {0} ]: {1} \n {2} \t {3}" },

    {ER_BUFFER_SIZE_LESSTHAN_ZERO,
      "Tama\u00F1o de buffer menor o igual que 0" },

    {ER_INVALID_UTF16_SURROGATE,
      "\u00BFSe ha detectado un sustituto UTF-16 no v\u00E1lido: {0}?" },

    {ER_OIERROR,
      "Error de ES" },

    {ER_ILLEGAL_ATTRIBUTE_POSITION,
      "No se puede agregar el atributo {0} despu\u00E9s de nodos secundarios o antes de que se produzca un elemento. Se ignorar\u00E1 el atributo."},

      /*
       * Note to translators:  The stylesheet contained a reference to a
       * namespace prefix that was undefined.  The value of the substitution
       * text is the name of the prefix.
       */
    {ER_NAMESPACE_PREFIX,
      "No se ha declarado el espacio de nombres para el prefijo ''{0}''." },
      /*
       * Note to translators:  This message is reported if the stylesheet
       * being processed attempted to construct an XML document with an
       * attribute in a place other than on an element.  The substitution text
       * specifies the name of the attribute.
       */
    {ER_STRAY_ATTRIBUTE,
      "El atributo ''{0}'' est\u00E1 fuera del elemento." },

      /*
       * Note to translators:  As with the preceding message, a namespace
       * declaration has the form of an attribute and is only permitted to
       * appear on an element.  The substitution text {0} is the namespace
       * prefix and {1} is the URI that was being used in the erroneous
       * namespace declaration.
       */
    {ER_STRAY_NAMESPACE,
      "Declaraci\u00F3n del espacio de nombres ''{0}''=''{1}'' fuera del elemento." },

    {ER_COULD_NOT_LOAD_RESOURCE,
      "No se ha podido cargar ''{0}'' (compruebe la CLASSPATH), ahora s\u00F3lo se est\u00E1n utilizando los valores por defecto"},

    { ER_ILLEGAL_CHARACTER,
       "Intento de realizar la salida del car\u00E1cter del valor integral {0}, que no est\u00E1 representado en la codificaci\u00F3n de salida de {1}."},

    {ER_COULD_NOT_LOAD_METHOD_PROPERTY,
      "No se ha podido cargar el archivo de propiedades ''{0}'' para el m\u00E9todo de salida ''{1}'' (compruebe la CLASSPATH)" }


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
