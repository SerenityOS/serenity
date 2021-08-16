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
public class XMLErrorResources_tr extends ListResourceBundle
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
      "\u0130\u015flev desteklenmiyor!"},

    { ER_CANNOT_OVERWRITE_CAUSE,
      "Nedenin \u00fczerine yaz\u0131lamaz"},

    { ER_NO_DEFAULT_IMPL,
      "Varsay\u0131lan uygulama bulunamad\u0131"},

    { ER_CHUNKEDINTARRAY_NOT_SUPPORTED,
      "ChunkedIntArray({0}) \u015fu an desteklenmiyor"},

    { ER_OFFSET_BIGGER_THAN_SLOT,
      "G\u00f6reli konum yuvadan b\u00fcy\u00fck"},

    { ER_COROUTINE_NOT_AVAIL,
      "Coroutine kullan\u0131lam\u0131yor, id={0}"},

    { ER_COROUTINE_CO_EXIT,
      "CoroutineManager co_exit() iste\u011fi ald\u0131"},

    { ER_COJOINROUTINESET_FAILED,
      "co_joinCoroutineSet() ba\u015far\u0131s\u0131z oldu"},

    { ER_COROUTINE_PARAM,
      "Coroutine de\u011fi\u015ftirgesi hatas\u0131 ({0})"},

    { ER_PARSER_DOTERMINATE_ANSWERS,
      "\nBEKLENMEYEN: Parser doTerminate yan\u0131t\u0131 {0}"},

    { ER_NO_PARSE_CALL_WHILE_PARSING,
      "Ayr\u0131\u015ft\u0131rma s\u0131ras\u0131nda parse \u00e7a\u011fr\u0131lamaz"},

    { ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Hata: {0} ekseni i\u00e7in tip atanm\u0131\u015f yineleyici ger\u00e7ekle\u015ftirilmedi"},

    { ER_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Hata: {0} ekseni i\u00e7in yineleyici ger\u00e7ekle\u015ftirilmedi"},

    { ER_ITERATOR_CLONE_NOT_SUPPORTED,
      "Yineleyici e\u015fkopyas\u0131 desteklenmiyor"},

    { ER_UNKNOWN_AXIS_TYPE,
      "Bilinmeyen eksen dola\u015fma tipi: {0}"},

    { ER_AXIS_NOT_SUPPORTED,
      "Eksen dola\u015f\u0131c\u0131 desteklenmiyor: {0}"},

    { ER_NO_DTMIDS_AVAIL,
      "Kullan\u0131labilecek ba\u015fka DTM tan\u0131t\u0131c\u0131s\u0131 yok"},

    { ER_NOT_SUPPORTED,
      "Desteklenmiyor: {0}"},

    { ER_NODE_NON_NULL,
      "getDTMHandleFromNode i\u00e7in d\u00fc\u011f\u00fcm bo\u015f de\u011ferli olmamal\u0131d\u0131r"},

    { ER_COULD_NOT_RESOLVE_NODE,
      "D\u00fc\u011f\u00fcm tan\u0131t\u0131c\u0131 de\u011fere \u00e7\u00f6z\u00fclemedi"},

    { ER_STARTPARSE_WHILE_PARSING,
       "Ayr\u0131\u015ft\u0131rma s\u0131ras\u0131nda startParse \u00e7a\u011fr\u0131lamaz"},

    { ER_STARTPARSE_NEEDS_SAXPARSER,
       "startParse i\u00e7in bo\u015f de\u011ferli olmayan SAXParser gerekiyor"},

    { ER_COULD_NOT_INIT_PARSER,
       "Ayr\u0131\u015ft\u0131r\u0131c\u0131 bununla kullan\u0131ma haz\u0131rlanamad\u0131"},

    { ER_EXCEPTION_CREATING_POOL,
       "Havuz i\u00e7in yeni \u00f6rnek yarat\u0131l\u0131rken kural d\u0131\u015f\u0131 durum"},

    { ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
       "Yol ge\u00e7ersiz ka\u00e7\u0131\u015f dizisi i\u00e7eriyor"},

    { ER_SCHEME_REQUIRED,
       "\u015eema gerekli!"},

    { ER_NO_SCHEME_IN_URI,
       "URI i\u00e7inde \u015fema bulunamad\u0131: {0}"},

    { ER_NO_SCHEME_INURI,
       "URI i\u00e7inde \u015fema bulunamad\u0131"},

    { ER_PATH_INVALID_CHAR,
       "Yol ge\u00e7ersiz karakter i\u00e7eriyor: {0}"},

    { ER_SCHEME_FROM_NULL_STRING,
       "Bo\u015f de\u011ferli dizgiden \u015fema tan\u0131mlanamaz"},

    { ER_SCHEME_NOT_CONFORMANT,
       "\u015eema uyumlu de\u011fil."},

    { ER_HOST_ADDRESS_NOT_WELLFORMED,
       "Anasistem do\u011fru bi\u00e7imli bir adres de\u011fil"},

    { ER_PORT_WHEN_HOST_NULL,
       "Anasistem bo\u015f de\u011ferliyken kap\u0131 tan\u0131mlanamaz"},

    { ER_INVALID_PORT,
       "Kap\u0131 numaras\u0131 ge\u00e7ersiz"},

    { ER_FRAG_FOR_GENERIC_URI,
       "Par\u00e7a yaln\u0131zca soysal URI i\u00e7in tan\u0131mlanabilir"},

    { ER_FRAG_WHEN_PATH_NULL,
       "Yol bo\u015f de\u011ferliyken par\u00e7a tan\u0131mlanamaz"},

    { ER_FRAG_INVALID_CHAR,
       "Par\u00e7a ge\u00e7ersiz karakter i\u00e7eriyor"},

    { ER_PARSER_IN_USE,
      "Ayr\u0131\u015ft\u0131r\u0131c\u0131 kullan\u0131mda"},

    { ER_CANNOT_CHANGE_WHILE_PARSING,
      "Ayr\u0131\u015ft\u0131rma s\u0131ras\u0131nda {0} {1} de\u011fi\u015ftirilemez"},

    { ER_SELF_CAUSATION_NOT_PERMITTED,
      "\u00d6znedenselli\u011fe izin verilmez"},

    { ER_NO_USERINFO_IF_NO_HOST,
      "Anasistem belirtilmediyse kullan\u0131c\u0131 bilgisi belirtilemez"},

    { ER_NO_PORT_IF_NO_HOST,
      "Anasistem belirtilmediyse kap\u0131 belirtilemez"},

    { ER_NO_QUERY_STRING_IN_PATH,
      "Yol ve sorgu dizgisinde sorgu dizgisi belirtilemez"},

    { ER_NO_FRAGMENT_STRING_IN_PATH,
      "Par\u00e7a hem yolda, hem de par\u00e7ada belirtilemez"},

    { ER_CANNOT_INIT_URI_EMPTY_PARMS,
      "Bo\u015f de\u011fi\u015ftirgelerle URI kullan\u0131ma haz\u0131rlanamaz"},

    { ER_METHOD_NOT_SUPPORTED,
      "Y\u00f6ntem hen\u00fcz desteklenmiyor"},

    { ER_INCRSAXSRCFILTER_NOT_RESTARTABLE,
      "IncrementalSAXSource_Filter \u015fu an yeniden ba\u015flat\u0131labilir durumda de\u011fil"},

    { ER_XMLRDR_NOT_BEFORE_STARTPARSE,
      "XMLReader, startParse iste\u011finden \u00f6nce olmaz"},

    { ER_AXIS_TRAVERSER_NOT_SUPPORTED,
      "Eksen dola\u015f\u0131c\u0131 desteklenmiyor: {0}"},

    { ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER,
      "ListingErrorHandler bo\u015f de\u011ferli PrintWriter ile yarat\u0131ld\u0131!"},

    { ER_SYSTEMID_UNKNOWN,
      "SystemId bilinmiyor"},

    { ER_LOCATION_UNKNOWN,
      "Hata yeri bilinmiyor"},

    { ER_PREFIX_MUST_RESOLVE,
      "\u00d6nek bir ad alan\u0131na \u00e7\u00f6z\u00fclmelidir: {0}"},

    { ER_CREATEDOCUMENT_NOT_SUPPORTED,
      "XPathContext i\u00e7inde createDocument() desteklenmiyor!"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT,
      "\u00d6zniteli\u011fin alt \u00f6\u011fesinin iye belgesi yok!"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT,
      "\u00d6zniteli\u011fin alt \u00f6\u011fesinin iye belge \u00f6\u011fesi yok!"},

    { ER_CANT_OUTPUT_TEXT_BEFORE_DOC,
      "Uyar\u0131: Belge \u00f6\u011fesinden \u00f6nce metin \u00e7\u0131k\u0131\u015f\u0131 olamaz!  Yoksay\u0131l\u0131yor..."},

    { ER_CANT_HAVE_MORE_THAN_ONE_ROOT,
      "DOM \u00fczerinde birden fazla k\u00f6k olamaz!"},

    { ER_ARG_LOCALNAME_NULL,
       "'localName' ba\u011f\u0131ms\u0131z de\u011fi\u015ftirgesi bo\u015f de\u011ferli"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The localname is the portion after the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_LOCALNAME_INVALID,
       "QNAME i\u00e7indeki yerel ad (localname) ge\u00e7erli bir NCName olmal\u0131d\u0131r"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The prefix is the portion before the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_PREFIX_INVALID,
       "QNAME i\u00e7indeki \u00f6nek ge\u00e7erli bir NCName olmal\u0131d\u0131r"},

    { "BAD_CODE", "createMessage i\u00e7in kullan\u0131lan de\u011fi\u015ftirge s\u0131n\u0131rlar\u0131n d\u0131\u015f\u0131nda"},
    { "FORMAT_FAILED", "messageFormat \u00e7a\u011fr\u0131s\u0131 s\u0131ras\u0131nda kural d\u0131\u015f\u0131 durum yay\u0131nland\u0131"},
    { "line", "Sat\u0131r #"},
    { "column","Kolon #"},

    {ER_SERIALIZER_NOT_CONTENTHANDLER,
      "Diziselle\u015ftirici s\u0131n\u0131f\u0131 ''{0}'' org.xml.sax.ContentHandler i\u015flevini uygulam\u0131yor."},

    {ER_RESOURCE_COULD_NOT_FIND,
      "Kaynak [ {0} ] bulunamad\u0131.\n {1}" },

    {ER_RESOURCE_COULD_NOT_LOAD,
      "Kaynak [ {0} ] y\u00fckleyemedi: {1} \n {2} \t {3}" },

    {ER_BUFFER_SIZE_LESSTHAN_ZERO,
      "Arabellek b\u00fcy\u00fckl\u00fc\u011f\u00fc <=0" },

    {ER_INVALID_UTF16_SURROGATE,
      "UTF-16 yerine kullan\u0131lan de\u011fer ge\u00e7ersiz: {0} ?" },

    {ER_OIERROR,
      "G\u00c7 hatas\u0131" },

    {ER_ILLEGAL_ATTRIBUTE_POSITION,
      "Alt d\u00fc\u011f\u00fcmlerden sonra ya da bir \u00f6\u011fe \u00fcretilmeden \u00f6nce {0} \u00f6zniteli\u011fi eklenemez. \u00d6znitelik yoksay\u0131lacak."},

      /*
       * Note to translators:  The stylesheet contained a reference to a
       * namespace prefix that was undefined.  The value of the substitution
       * text is the name of the prefix.
       */
    {ER_NAMESPACE_PREFIX,
      "''{0}'' \u00f6nekine ili\u015fkin ad alan\u0131 bildirilmedi." },
      /*
       * Note to translators:  This message is reported if the stylesheet
       * being processed attempted to construct an XML document with an
       * attribute in a place other than on an element.  The substitution text
       * specifies the name of the attribute.
       */
    {ER_STRAY_ATTRIBUTE,
      "''{0}'' \u00f6zniteli\u011fi \u00f6\u011fenin d\u0131\u015f\u0131nda." },

      /*
       * Note to translators:  As with the preceding message, a namespace
       * declaration has the form of an attribute and is only permitted to
       * appear on an element.  The substitution text {0} is the namespace
       * prefix and {1} is the URI that was being used in the erroneous
       * namespace declaration.
       */
    {ER_STRAY_NAMESPACE,
      "''{0}''=''{1}'' ad alan\u0131 bildirimi \u00f6\u011fenin d\u0131\u015f\u0131nda." },

    {ER_COULD_NOT_LOAD_RESOURCE,
      "''{0}'' y\u00fcklenemedi (CLASSPATH de\u011fi\u015fkeninizi inceleyin), yaln\u0131zca varsay\u0131lanlar kullan\u0131l\u0131yor"},

    {ER_COULD_NOT_LOAD_METHOD_PROPERTY,
      "''{1}'' \u00e7\u0131k\u0131\u015f y\u00f6ntemi i\u00e7in ''{0}'' \u00f6zellik dosyas\u0131 y\u00fcklenemedi (CLASSPATH de\u011fi\u015fkenini inceleyin)" }


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
