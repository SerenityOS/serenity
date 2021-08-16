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

package com.sun.org.apache.xalan.internal.templates;


/**
 * Primary constants used in the TransformerImpl classes.
 * @xsl.usage advanced
 */
public class Constants extends com.sun.org.apache.xml.internal.utils.Constants
{

  /**
   * IDs for XSL element types. These are associated
   * with the string literals in the TransformerImpl class.
   * Don't change the numbers. NOTE THAT THESE ARE NOT IN
   * ALPHABETICAL ORDER!
   * (It's a pity Java doesn't have a real Enumerated Mnemonic
   * datatype... or a C-like preprocessor in lieu thereof which
   * could be used to generate and maintain synch between these lists.)
   */
  public static final int ELEMNAME_UNDEFINED = -1, ELEMNAME_WITHPARAM = 2,
                          ELEMNAME_ADDATTRIBUTE = 4, ELEMNAME_ANCHOR = 22,

  //  ELEMNAME_ANCHOR_PATTERN = 23,
  ELEMNAME_APPLY_TEMPLATES = 50, ELEMNAME_USE = 34, ELEMNAME_CHILDREN = 6,
                                 ELEMNAME_CHOOSE = 37, ELEMNAME_COMMENT = 59,  // my own
                                 ELEMNAME_CONSTRUCT = 7,  // my own
                                 ELEMNAME_CONTENTS = 8, ELEMNAME_COPY = 9,
                                 ELEMNAME_COPY_OF = 74,
                                 ELEMNAME_DECIMALFORMAT = 83,
                                 ELEMNAME_DEFINEATTRIBUTESET = 40,

  //  ELEMNAME_DEFINECONSTANT = 29,
  //  ELEMNAME_DEFINEMACRO = 10,
  ELEMNAME_DEFINESCRIPT = 11, ELEMNAME_DISPLAYIF = 12,  // my own
                              ELEMNAME_EMPTY = 14, ELEMNAME_EVAL = 15,
                              ELEMNAME_EXPECTEDCHILDREN = 16,
                              ELEMNAME_EXTENSION = 54,
                              ELEMNAME_EXTENSIONHANDLER = 63,
                              ELEMNAME_FOREACH = 28, ELEMNAME_KEY = 31,
                              ELEMNAME_IF = 36, ELEMNAME_IMPORT = 26,
                              ELEMNAME_INCLUDE = 27,
                              ELEMNAME_CALLTEMPLATE = 17,
                              ELEMNAME_PARAMVARIABLE = 41,
                              ELEMNAME_NUMBER = 35, ELEMNAME_NSALIAS = 84,
                              ELEMNAME_OTHERWISE = 39, ELEMNAME_PI = 58,
                              ELEMNAME_PRESERVESPACE = 33,
                              ELEMNAME_REMOVEATTRIBUTE = 5,
                              ELEMNAME_TEMPLATE = 19, ELEMNAME_SORT = 64,
                              ELEMNAME_STRIPSPACE = 32,
                              ELEMNAME_STYLESHEET = 25, ELEMNAME_TEXT = 42,
                              ELEMNAME_VALUEOF = 30, ELEMNAME_WHEN = 38,

  // Pattern by example support
  ELEMNAME_ROOT = 44, ELEMNAME_ANY = 45, ELEMNAME_ELEMENT = 46,
                      ELEMNAME_TARGETELEMENT = 47, ELEMNAME_ATTRIBUTE = 48,
                      ELEMNAME_TARGETATTRIBUTE = 49, ELEMNAME_URL = 52,  // my own
                      ELEMNAME_CALL = 55,  // my own

  //  ELEMNAME_WITHPARAM = 56,
  ELEMNAME_FALLBACK = 57,  // my own
  ELEMNAME_TARGETPI = 60,  // my own
  ELEMNAME_TARGETCOMMENT = 61,  // my own
  ELEMNAME_TARGETTEXT = 62,  // my own
  ELEMNAME_CSSSTYLECONVERSION = 65,  // my own
  ELEMNAME_COUNTER = 66, ELEMNAME_COUNTERS = 67,
  ELEMNAME_COUNTERINCREMENT = 68, ELEMNAME_COUNTERRESET = 69,
  ELEMNAME_COUNTERSCOPE = 71, ELEMNAME_APPLY_IMPORTS = 72,
  ELEMNAME_VARIABLE = 73, ELEMNAME_MESSAGE = 75, ELEMNAME_LOCALE = 76,
  ELEMNAME_LITERALRESULT = 77, ELEMNAME_TEXTLITERALRESULT = 78,
  ELEMNAME_EXTENSIONCALL = 79, ELEMNAME_EXTENSIONDECL = 85,
  ELEMNAME_EXTENSIONSCRIPT = 86, ELEMNAME_OUTPUT = 80,
  ELEMNAME_COMPONENT = 81, ELEMNAME_SCRIPT = 82;

  // Next free number: 90 (88 and 89 used for EXSLT elements);

  /**
   * Literals for XSL element names.  Note that there are more
   * names than IDs, because some names map to the same ID.
   */
  public static final String
          ELEMNAME_ANCHOR_STRING = "anchor",
      ELEMNAME_ANY_STRING = "any",  // pattern-by-example support
      ELEMNAME_APPLY_IMPORTS_STRING = "apply-imports",
      ELEMNAME_APPLY_TEMPLATES_STRING = "apply-templates",
      ELEMNAME_ARG_STRING = "arg",
      ELEMNAME_ATTRIBUTESET_STRING = "attribute-set",
      ELEMNAME_ATTRIBUTE_STRING = "attribute",  // pattern-by-example support
      ELEMNAME_CALLTEMPLATEARG_STRING = "invoke-arg",
      ELEMNAME_CALLTEMPLATE_STRING = "call-template",
      ELEMNAME_CALL_STRING = "call",
      ELEMNAME_CHILDREN_STRING = "children",
      ELEMNAME_CHOOSE_STRING = "choose",
      ELEMNAME_COMMENT_STRING = "comment",
      ELEMNAME_COMPONENT_STRING = "component",
      ELEMNAME_CONSTRUCT_STRING = "construct",  // my own
      ELEMNAME_CONTENTS_STRING = "contents",
      ELEMNAME_COPY_OF_STRING ="copy-of",
      ELEMNAME_COPY_STRING = "copy",
      ELEMNAME_COUNTERINCREMENT_STRING = "counter-increment",
      ELEMNAME_COUNTERRESET_STRING = "counter-reset",
      ELEMNAME_COUNTERSCOPE_STRING = "counter-scope",
      ELEMNAME_COUNTERS_STRING = "counters",
      ELEMNAME_COUNTER_STRING = "counter",
      ELEMNAME_CSSSTYLECONVERSION_STRING = "css-style-conversion",
      ELEMNAME_DECIMALFORMAT_STRING = "decimal-format",
      ELEMNAME_DISPLAYIF_STRING = "display-if",  // my own
      ELEMNAME_ELEMENT_STRING = "element",  // pattern-by-example support
      ELEMNAME_EMPTY_STRING = "empty",
      ELEMNAME_EVAL_STRING = "eval",
      ELEMNAME_EXPECTEDCHILDREN_STRING = "expectedchildren",
      ELEMNAME_EXTENSIONHANDLER_STRING = "code-dispatcher",
      ELEMNAME_EXTENSION_STRING = "functions",
      ELEMNAME_FALLBACK_STRING = "fallback",
      ELEMNAME_FOREACH_STRING = "for-each",
      ELEMNAME_IF_STRING = "if",
      ELEMNAME_IMPORT_STRING = "import",
      ELEMNAME_INCLUDE_STRING = "include",
      ELEMNAME_KEY_STRING = "key",
      ELEMNAME_LOCALE_STRING = "locale",
      ELEMNAME_MESSAGE_STRING = "message",
      ELEMNAME_NSALIAS_STRING = "namespace-alias",
      ELEMNAME_NUMBER_STRING = "number",
      ELEMNAME_OTHERWISE_STRING = "otherwise",
      ELEMNAME_OUTPUT_STRING = "output",
      ELEMNAME_PARAMVARIABLE_STRING = "param",
      ELEMNAME_PI_OLD_STRING = "pi",
      ELEMNAME_PI_STRING = "processing-instruction",
      ELEMNAME_PRESERVESPACE_STRING = "preserve-space",
      ELEMNAME_ROOT_STRING = "root",  // pattern-by-example support
      ELEMNAME_SCRIPT_STRING = "script",
      ELEMNAME_SORT_STRING = "sort",
      ELEMNAME_STRIPSPACE_STRING = "strip-space",
      ELEMNAME_STYLESHEET_STRING = "stylesheet",
      ELEMNAME_TARGETATTRIBUTE_STRING = "target-attribute",  // pattern-by-example support
      ELEMNAME_TARGETCOMMENT_STRING = "target-comment",
      ELEMNAME_TARGETELEMENT_STRING = "target-element",  // pattern-by-example support
      ELEMNAME_TARGETPI_STRING = "target-pi",
      ELEMNAME_TARGETTEXT_STRING = "target-text",
      ELEMNAME_TEMPLATE_STRING = "template",
      ELEMNAME_TEXT_STRING = "text",
      ELEMNAME_TRANSFORM_STRING = "transform",
      ELEMNAME_URL_STRING = "uri",  // pattern-by-example support
      ELEMNAME_USE_STRING = "use",
      ELEMNAME_VALUEOF_STRING = "value-of",
      ELEMNAME_VARIABLE_STRING = "variable",
      ELEMNAME_WHEN_STRING = "when",
      ELEMNAME_WITHPARAM_STRING = "with-param";

  /**
   * Literals for EXSLT function elements.
   */
  public static final String
    EXSLT_ELEMNAME_FUNCTION_STRING = "function",
    EXSLT_ELEMNAME_FUNCRESULT_STRING = "result";
  public static final int
    EXSLT_ELEMNAME_FUNCTION = 88,
    EXSLT_ELEMNAME_FUNCRESULT = 89;


  /**
   * Literals for XSL attribute names.  Note that there may be more
   * names than IDs, because some names may map to the same ID.
   */
  public static final String
          ATTRNAME_AMOUNT = "amount",
      ATTRNAME_ANCESTOR = "ancestor",
      ATTRNAME_ARCHIVE = "archive",
      ATTRNAME_ATTRIBUTE = "attribute",
      ATTRNAME_ATTRIBUTE_SET = "attribute-set",
      ATTRNAME_CASEORDER = "case-order",
      ATTRNAME_CLASS = "class",
      ATTRNAME_CLASSID = "classid",
      ATTRNAME_CODEBASE = "codebase",
      ATTRNAME_CODETYPE = "type",
      ATTRNAME_CONDITION = "condition",
      ATTRNAME_COPYTYPE = "copy-type",
      ATTRNAME_COUNT = "count",
      ATTRNAME_DATATYPE = "data-type",
      ATTRNAME_DECIMALSEPARATOR = "decimal-separator",
      ATTRNAME_DEFAULT = "default",
      ATTRNAME_DEFAULTSPACE = "default-space",
      ATTRNAME_DEPTH = "with-children",
      ATTRNAME_DIGIT = "digit",
      ATTRNAME_DIGITGROUPSEP = "digit-group-sep",
      ATTRNAME_DISABLE_OUTPUT_ESCAPING = "disable-output-escaping",
      ATTRNAME_ELEMENT = "element",
      ATTRNAME_ELEMENTS = "elements",
      ATTRNAME_EXCLUDE_RESULT_PREFIXES ="exclude-result-prefixes",
      ATTRNAME_EXPR = "expr",
      ATTRNAME_EXTENSIONELEMENTPREFIXES = "extension-element-prefixes",
      ATTRNAME_FORMAT = "format",
      ATTRNAME_FROM = "from",
      ATTRNAME_GROUPINGSEPARATOR = "grouping-separator",
      ATTRNAME_GROUPINGSIZE = "grouping-size",
      ATTRNAME_HREF = "href",
      ATTRNAME_ID = "id",
      ATTRNAME_IMPORTANCE = "importance",
      ATTRNAME_INDENTRESULT = "indent-result",
      ATTRNAME_INFINITY = "infinity",
      ATTRNAME_LANG = "lang",
      ATTRNAME_LETTERVALUE = "letter-value",
      ATTRNAME_LEVEL = "level",
      ATTRNAME_MATCH = "match",
      ATTRNAME_METHOD = "calls",
      ATTRNAME_MINUSSIGN = "minus-sign",
      ATTRNAME_MODE = "mode",
      ATTRNAME_NAME = "name",
      ATTRNAME_NAMESPACE = "namespace",
      ATTRNAME_NAN = "NaN",
      ATTRNAME_NDIGITSPERGROUP = "n-digits-per-group",
      ATTRNAME_NS = "ns",
      ATTRNAME_ONLY = "only",
      ATTRNAME_ORDER = "order",
      ATTRNAME_OUTPUT_CDATA_SECTION_ELEMENTS = "cdata-section-elements",
      ATTRNAME_OUTPUT_DOCTYPE_PUBLIC = "doctype-public",
      ATTRNAME_OUTPUT_DOCTYPE_SYSTEM = "doctype-system",
      ATTRNAME_OUTPUT_ENCODING = "encoding",
      ATTRNAME_OUTPUT_INDENT = "indent",
      ATTRNAME_OUTPUT_MEDIATYPE = "media-type",
      ATTRNAME_OUTPUT_METHOD = "method",  // qname,
      ATTRNAME_OUTPUT_OMITXMLDECL = "omit-xml-declaration",
      ATTRNAME_OUTPUT_STANDALONE = "standalone",
      ATTRNAME_OUTPUT_VERSION = "version",
      ATTRNAME_PATTERNSEPARATOR = "pattern-separator",
      ATTRNAME_PERCENT = "percent",
      ATTRNAME_PERMILLE = "per-mille",
      ATTRNAME_PRIORITY = "priority",
      ATTRNAME_REFID = "refID",
      ATTRNAME_RESULTNS = "result-ns",
      ATTRNAME_RESULT_PREFIX = "result-prefix",
      ATTRNAME_SELECT = "select",
      ATTRNAME_SEQUENCESRC = "sequence-src",
      ATTRNAME_STYLE = "style",
      ATTRNAME_STYLESHEET_PREFIX = "stylesheet-prefix",
      ATTRNAME_TERMINATE = "terminate",
      ATTRNAME_TEST = "test",
      ATTRNAME_TOSTRING = "to-string",
      ATTRNAME_TYPE = "type",
      ATTRNAME_USE = "use",
      ATTRNAME_USEATTRIBUTESETS = "use-attribute-sets",
      ATTRNAME_VALUE = "value",
      ATTRNAME_VERSION = "version",
      ATTRNAME_XMLNS = "xmlns:", // namespace declaration prefix -- NOT an attribute by itself
      ATTRNAME_XMLNSDEF = "xmlns", // default namespace
      ATTRNAME_XMLSPACE = "xml:space",
      ATTRNAME_ZERODIGIT = "zero-digit";

  /** IDs for XSL attribute types. These are associated
   * with the string literals in the TransformerImpl class.
   * Don't change the numbers. NOTE THAT THESE ARE NOT IN
   * ALPHABETICAL ORDER!
   */
  public static final int TATTRNAME_OUTPUT_METHOD = 1, TATTRNAME_AMOUNT = 2,
                          TATTRNAME_ANCESTOR = 3, TATTRNAME_ARCHIVE = 4,
                          TATTRNAME_ATTRIBUTE = 5,
                          TATTRNAME_ATTRIBUTE_SET = 6,
                          TATTRNAME_CASEORDER = 7, TATTRNAME_CLASS = 8,
                          TATTRNAME_CLASSID = 9, TATTRNAME_CODEBASE = 10,
                          TATTRNAME_CODETYPE = 11, TATTRNAME_CONDITION = 12,
                          TATTRNAME_COPYTYPE = 13, TATTRNAME_COUNT = 14,
                          TATTRNAME_DATATYPE = 15, TATTRNAME_DEFAULT = 16,
                          TATTRNAME_DEFAULTSPACE = 17, TATTRNAME_DEPTH = 18,
                          TATTRNAME_DIGITGROUPSEP = 19,
                          TATTRNAME_DISABLE_OUTPUT_ESCAPING = 20,
                          TATTRNAME_ELEMENT = 21, TATTRNAME_ELEMENTS = 22,
                          TATTRNAME_EXPR = 23,
                          TATTRNAME_EXTENSIONELEMENTPREFIXES = 24,
                          TATTRNAME_FORMAT = 25, TATTRNAME_FROM = 26,
                          TATTRNAME_GROUPINGSEPARATOR = 27,
                          TATTRNAME_GROUPINGSIZE = 28, TATTRNAME_HREF = 29,
                          TATTRNAME_ID = 30, TATTRNAME_IMPORTANCE = 31,
                          TATTRNAME_INDENTRESULT = 32, TATTRNAME_LANG = 33,
                          TATTRNAME_LETTERVALUE = 34, TATTRNAME_LEVEL = 35,
                          TATTRNAME_MATCH = 36, TATTRNAME_METHOD = 37,
                          TATTRNAME_MODE = 38, TATTRNAME_NAME = 39,
                          TATTRNAME_NAMESPACE = 40,
                          TATTRNAME_NDIGITSPERGROUP = 41, TATTRNAME_NS = 42,
                          TATTRNAME_ONLY = 43, TATTRNAME_ORDER = 44,
                          TATTRNAME_OUTPUT_CDATA_SECTION_ELEMENTS = 45,
                          TATTRNAME_OUTPUT_DOCTYPE_PUBLIC = 46,
                          TATTRNAME_OUTPUT_DOCTYPE_SYSTEM = 47,
                          TATTRNAME_OUTPUT_ENCODING = 48,
                          TATTRNAME_OUTPUT_INDENT = 49,
                          TATTRNAME_OUTPUT_MEDIATYPE = 50,
                          TATTRNAME_OUTPUT_STANDALONE = 51,
                          TATTRNAME_OUTPUT_VERSION = 52,
                          TATTRNAME_OUTPUT_OMITXMLDECL = 53,
                          TATTRNAME_PRIORITY = 54, TATTRNAME_REFID = 55,
                          TATTRNAME_RESULTNS = 56, TATTRNAME_SELECT = 57,
                          TATTRNAME_SEQUENCESRC = 58, TATTRNAME_STYLE = 59,
                          TATTRNAME_TEST = 60, TATTRNAME_TOSTRING = 61,
                          TATTRNAME_TYPE = 62, TATTRNAME_USE = 63,
                          TATTRNAME_USEATTRIBUTESETS = 64,
                          TATTRNAME_VALUE = 65, TATTRNAME_XMLNSDEF = 66,
                          TATTRNAME_XMLNS = 67, TATTRNAME_XMLSPACE = 68,
                          TATTRNAME_EXCLUDE_RESULT_PREFIXES = 69;

  /** Mnemonics for the possible values of the xsl:output element's
   * method= attribute:
   * <ul>
   * <li>ATTRVAL_OUTPUT_METHOD_XML = Use an XML formatter to
   * produce the output document (basic XSLT operation).</li>
   * <li>ATTRVAL_OUTPUT_METHOD_HTML: Use an HTML formatter to
   * produce the output document. When generating HTML documents,
   * this may yield better results; it does things like escaping
   * characters in href attributes.</li>
   * </li>ATTRVAL_OUTPUT_METHOD_TEXT:  Use a Text formatter to
   * produce the output document. Generally the right choice if your
   * stylesheet wants to take over _all_ the details of formatting,
   * most often when producing something that isn't an XML or HTML
   * document.</li>
   * </ul>
   * */
  public static final String ATTRVAL_OUTPUT_METHOD_HTML = "html",
                             ATTRVAL_OUTPUT_METHOD_XML = "xml",
                             ATTRVAL_OUTPUT_METHOD_TEXT = "text";


  /* For space-att*/
  public static final int ATTRVAL_PRESERVE = 1, ATTRVAL_STRIP = 2;


  /** For indent-result          */
  public static final boolean ATTRVAL_YES = true, ATTRVAL_NO = false;


  /** For letter-value attribute (part of conversion attributes).          */
  public static final String ATTRVAL_ALPHABETIC = "alphabetic",
                             ATTRVAL_OTHER = "other",
                             ATTRVAL_TRADITIONAL = "traditional";


  /** For level attribute in xsl:number.          */
  public static final String ATTRVAL_SINGLE = "single",
                             ATTRVAL_MULTI = "multiple", ATTRVAL_ANY = "any";


  /** For Stylesheet-prefix and result-prefix in xsl:namespace-alias          */
  public static final String ATTRVAL_DEFAULT_PREFIX = "#default";

 /** Integer equivalents for above        */
  public static final int NUMBERLETTER_ALPHABETIC = 1, NUMBERLETTER_TRADITIONAL = 2;

  /** Integer equivelents for above        */
  public static final int NUMBERLEVEL_SINGLE = 1, NUMBERLEVEL_MULTI = 2,
                          NUMBERLEVEL_ANY = 3, MAX_MULTI_COUNTING_DEPTH = 32;


  /** some stuff for my patterns-by-example         */
  public static final String ATTRVAL_THIS = ".", ATTRVAL_PARENT = "..",
                             ATTRVAL_ANCESTOR = "ancestor", ATTRVAL_ID = "id";


  /** Stuff for sorting      */
  public static final String ATTRVAL_DATATYPE_TEXT = "text",
                             ATTRVAL_DATATYPE_NUMBER = "number",
                             ATTRVAL_ORDER_ASCENDING = "ascending",
                             ATTRVAL_ORDER_DESCENDING = "descending",
                             ATTRVAL_CASEORDER_UPPER = "upper-first",
                             ATTRVAL_CASEORDER_LOWER = "lower-first";

 /** Integer equivelents for DATATYPE attribute */
  public static final int SORTDATATYPE_TEXT = 1, SORTDATATYPE_NUMBER = 2;

  /** Integer equivelents for ORDER attribute */
  public static final int SORTORDER_ASCENDING = 1, SORTORDER_DESCENDING = 2;

  /** Integer equivelents for CASE-ORDER attribute */
  public static final int SORTCASEORDER_UPPERFIRST = 1, SORTCASEORDER_LOWERFIRST = 2;

  /** some stuff for Decimal-format       */
  public static final String ATTRVAL_INFINITY = "Infinity",
                             ATTRVAL_NAN = "NaN",
                             DEFAULT_DECIMAL_FORMAT = "#default";


  /** temp dummy         */
  public static final String ATTRNAME_XXXX = "XXXX";
}
