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
public class XMLErrorResources_pt_BR extends ListResourceBundle
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
      "Fun\u00E7\u00E3o n\u00E3o suportada!"},

    { ER_CANNOT_OVERWRITE_CAUSE,
      "N\u00E3o \u00E9 poss\u00EDvel substituir a causa"},

    { ER_NO_DEFAULT_IMPL,
      "Nenhuma implementa\u00E7\u00E3o padr\u00E3o encontrada "},

    { ER_CHUNKEDINTARRAY_NOT_SUPPORTED,
      "ChunkedIntArray({0}) n\u00E3o suportado atualmente"},

    { ER_OFFSET_BIGGER_THAN_SLOT,
      "Deslocamento maior que o slot"},

    { ER_COROUTINE_NOT_AVAIL,
      "Co-rotina n\u00E3o dispon\u00EDvel, id={0}"},

    { ER_COROUTINE_CO_EXIT,
      "CoroutineManager recebeu a solicita\u00E7\u00E3o co_exit()"},

    { ER_COJOINROUTINESET_FAILED,
      "Falha em co_joinCoroutineSet()"},

    { ER_COROUTINE_PARAM,
      "Erro no par\u00E2metro da co-rotina ({0})"},

    { ER_PARSER_DOTERMINATE_ANSWERS,
      "\nINESPERADO: Parser doTerminate responde {0}"},

    { ER_NO_PARSE_CALL_WHILE_PARSING,
      "o parsing n\u00E3o pode ser chamado durante o parsing"},

    { ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Erro: iterador digitado para o eixo {0} n\u00E3o implementado"},

    { ER_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Erro: iterador para o eixo {0} n\u00E3o implementado "},

    { ER_ITERATOR_CLONE_NOT_SUPPORTED,
      "clonagem do iterador n\u00E3o suportada"},

    { ER_UNKNOWN_AXIS_TYPE,
      "Tipo transversal de eixo desconhecido: {0}"},

    { ER_AXIS_NOT_SUPPORTED,
      "Transversor de eixo n\u00E3o suportado: {0}"},

    { ER_NO_DTMIDS_AVAIL,
      "N\u00E3o h\u00E1 mais IDs de DTM dispon\u00EDveis"},

    { ER_NOT_SUPPORTED,
      "N\u00E3o suportado: {0}"},

    { ER_NODE_NON_NULL,
      "O n\u00F3 deve ser n\u00E3o-nulo para getDTMHandleFromNode"},

    { ER_COULD_NOT_RESOLVE_NODE,
      "N\u00E3o foi poss\u00EDvel resolver o n\u00F3 para um handle"},

    { ER_STARTPARSE_WHILE_PARSING,
       "startParse n\u00E3o pode ser chamado durante o parsing"},

    { ER_STARTPARSE_NEEDS_SAXPARSER,
       "startParse requer um SAXParser n\u00E3o nulo"},

    { ER_COULD_NOT_INIT_PARSER,
       "n\u00E3o foi poss\u00EDvel inicializar o parser com"},

    { ER_EXCEPTION_CREATING_POOL,
       "exce\u00E7\u00E3o ao criar a nova inst\u00E2ncia do pool"},

    { ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
       "O caminho cont\u00E9m uma sequ\u00EAncia inv\u00E1lida de caracteres de escape"},

    { ER_SCHEME_REQUIRED,
       "O esquema \u00E9 obrigat\u00F3rio!"},

    { ER_NO_SCHEME_IN_URI,
       "Nenhum esquema encontrado no URI: {0}"},

    { ER_NO_SCHEME_INURI,
       "Nenhum esquema encontrado no URI"},

    { ER_PATH_INVALID_CHAR,
       "O caminho cont\u00E9m um caractere inv\u00E1lido: {0}"},

    { ER_SCHEME_FROM_NULL_STRING,
       "N\u00E3o \u00E9 poss\u00EDvel definir o esquema de uma string nula"},

    { ER_SCHEME_NOT_CONFORMANT,
       "O esquema n\u00E3o \u00E9 compat\u00EDvel."},

    { ER_HOST_ADDRESS_NOT_WELLFORMED,
       "O host n\u00E3o \u00E9 um endere\u00E7o correto"},

    { ER_PORT_WHEN_HOST_NULL,
       "A porta n\u00E3o pode ser definida quando o host \u00E9 nulo"},

    { ER_INVALID_PORT,
       "N\u00FAmero de porta inv\u00E1lido"},

    { ER_FRAG_FOR_GENERIC_URI,
       "O fragmento s\u00F3 pode ser definido para um URI gen\u00E9rico"},

    { ER_FRAG_WHEN_PATH_NULL,
       "O fragmento n\u00E3o pode ser definido quando o caminho \u00E9 nulo"},

    { ER_FRAG_INVALID_CHAR,
       "O fragmento cont\u00E9m um caractere inv\u00E1lido"},

    { ER_PARSER_IN_USE,
      "O parser j\u00E1 est\u00E1 sendo usado"},

    { ER_CANNOT_CHANGE_WHILE_PARSING,
      "N\u00E3o \u00E9 poss\u00EDvel alterar {0} {1} durante o parsing"},

    { ER_SELF_CAUSATION_NOT_PERMITTED,
      "Autoaverigua\u00E7\u00E3o n\u00E3o permitida"},

    { ER_NO_USERINFO_IF_NO_HOST,
      "As informa\u00E7\u00F5es do usu\u00E1rio n\u00E3o podem ser especificadas se o host n\u00E3o tiver sido especificado"},

    { ER_NO_PORT_IF_NO_HOST,
      "A porta n\u00E3o pode ser especificada se o host n\u00E3o tiver sido especificado"},

    { ER_NO_QUERY_STRING_IN_PATH,
      "A string de consulta n\u00E3o pode ser especificada no caminho nem na string de consulta"},

    { ER_NO_FRAGMENT_STRING_IN_PATH,
      "O fragmento n\u00E3o pode ser especificado no caminho nem no fragmento"},

    { ER_CANNOT_INIT_URI_EMPTY_PARMS,
      "N\u00E3o \u00E9 poss\u00EDvel inicializar o URI com par\u00E2metros vazios"},

    { ER_METHOD_NOT_SUPPORTED,
      "M\u00E9todo ainda n\u00E3o suportado "},

    { ER_INCRSAXSRCFILTER_NOT_RESTARTABLE,
      "IncrementalSAXSource_Filter atualmente n\u00E3o reinicializ\u00E1vel"},

    { ER_XMLRDR_NOT_BEFORE_STARTPARSE,
      "XMLReader n\u00E3o anterior \u00E0 solicita\u00E7\u00E3o de startParse"},

    { ER_AXIS_TRAVERSER_NOT_SUPPORTED,
      "Transversor de eixo n\u00E3o suportado: {0}"},

    { ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER,
      "ListingErrorHandler criado com PrintWriter nulo!"},

    { ER_SYSTEMID_UNKNOWN,
      "SystemId Desconhecido"},

    { ER_LOCATION_UNKNOWN,
      "Localiza\u00E7\u00E3o de erro desconhecida"},

    { ER_PREFIX_MUST_RESOLVE,
      "O prefixo deve ser resolvido para um namespace: {0}"},

    { ER_CREATEDOCUMENT_NOT_SUPPORTED,
      "createDocument() n\u00E3o suportado no XPathContext!"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT,
      "O filho do atributo n\u00E3o tem um documento do propriet\u00E1rio!"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT,
      "O filho do atributo n\u00E3o tem um elemento do documento do propriet\u00E1rio!"},

    { ER_CANT_OUTPUT_TEXT_BEFORE_DOC,
      "Advert\u00EAncia: n\u00E3o pode haver texto antes do elemento do documento! Ignorando..."},

    { ER_CANT_HAVE_MORE_THAN_ONE_ROOT,
      "N\u00E3o pode ter mais de uma raiz em um DOM!"},

    { ER_ARG_LOCALNAME_NULL,
       "O argumento 'localName' \u00E9 nulo"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The localname is the portion after the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_LOCALNAME_INVALID,
       "Localname em QNAME deve ser um NCName v\u00E1lido"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The prefix is the portion before the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_PREFIX_INVALID,
       "O prefixo em QNAME deve ser um NCName v\u00E1lido"},

    { ER_NAME_CANT_START_WITH_COLON,
      "O nome n\u00E3o pode come\u00E7ar com dois pontos"},

    { "BAD_CODE", "O par\u00E2metro para createMessage estava fora dos limites"},
    { "FORMAT_FAILED", "Exce\u00E7\u00E3o gerada durante a chamada messageFormat"},
    { "line", "N\u00B0 da Linha"},
    { "column","N\u00B0 da Coluna"},

    {ER_SERIALIZER_NOT_CONTENTHANDLER,
      "A classe ''{0}'' do serializador n\u00E3o implementa org.xml.sax.ContentHandler."},

    {ER_RESOURCE_COULD_NOT_FIND,
      "N\u00E3o foi poss\u00EDvel encontrar o recurso [ {0} ].\n {1}" },

    {ER_RESOURCE_COULD_NOT_LOAD,
      "O recurso [ {0} ] n\u00E3o foi carregado: {1} \n {2} \t {3}" },

    {ER_BUFFER_SIZE_LESSTHAN_ZERO,
      "Tamanho do buffer <=0" },

    {ER_INVALID_UTF16_SURROGATE,
      "Foi detectado um substituto de UTF-16 inv\u00E1lido: {0} ?" },

    {ER_OIERROR,
      "Erro de E/S" },

    {ER_ILLEGAL_ATTRIBUTE_POSITION,
      "N\u00E3o \u00E9 poss\u00EDvel adicionar o atributo {0} depois dos n\u00F3s filhos ou antes que um elemento seja produzido. O atributo ser\u00E1 ignorado."},

      /*
       * Note to translators:  The stylesheet contained a reference to a
       * namespace prefix that was undefined.  The value of the substitution
       * text is the name of the prefix.
       */
    {ER_NAMESPACE_PREFIX,
      "O namespace do prefixo ''{0}'' n\u00E3o foi declarado." },
      /*
       * Note to translators:  This message is reported if the stylesheet
       * being processed attempted to construct an XML document with an
       * attribute in a place other than on an element.  The substitution text
       * specifies the name of the attribute.
       */
    {ER_STRAY_ATTRIBUTE,
      "Atributo ''{0}'' fora do elemento." },

      /*
       * Note to translators:  As with the preceding message, a namespace
       * declaration has the form of an attribute and is only permitted to
       * appear on an element.  The substitution text {0} is the namespace
       * prefix and {1} is the URI that was being used in the erroneous
       * namespace declaration.
       */
    {ER_STRAY_NAMESPACE,
      "Declara\u00E7\u00E3o de namespace ''{0}''=''{1}'' fora do elemento." },

    {ER_COULD_NOT_LOAD_RESOURCE,
      "N\u00E3o foi poss\u00EDvel carregar ''{0}'' (verificar CLASSPATH); usando agora apenas os padr\u00F5es"},

    { ER_ILLEGAL_CHARACTER,
       "Tentativa de exibir um caractere de valor integral {0} que n\u00E3o est\u00E1 representado na codifica\u00E7\u00E3o de sa\u00EDda especificada de {1}."},

    {ER_COULD_NOT_LOAD_METHOD_PROPERTY,
      "N\u00E3o foi poss\u00EDvel carregar o arquivo de propriedade ''{0}'' para o m\u00E9todo de sa\u00EDda ''{1}'' (verificar CLASSPATH)" }


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
