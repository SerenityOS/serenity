/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xalan.internal.xsltc.compiler.util;

import java.util.ListResourceBundle;

/**
 * @author Morten Jorgensen
 */
public class ErrorMessages_pt_BR extends ListResourceBundle {

/*
 * XSLTC compile-time error messages.
 *
 * General notes to translators and definitions:
 *
 *   1) XSLTC is the name of the product.  It is an acronym for "XSLT Compiler".
 *      XSLT is an acronym for "XML Stylesheet Language: Transformations".
 *
 *   2) A stylesheet is a description of how to transform an input XML document
 *      into a resultant XML document (or HTML document or text).  The
 *      stylesheet itself is described in the form of an XML document.
 *
 *   3) A template is a component of a stylesheet that is used to match a
 *      particular portion of an input document and specifies the form of the
 *      corresponding portion of the output document.
 *
 *   4) An axis is a particular "dimension" in a tree representation of an XML
 *      document; the nodes in the tree are divided along different axes.
 *      Traversing the "child" axis, for instance, means that the program
 *      would visit each child of a particular node; traversing the "descendant"
 *      axis means that the program would visit the child nodes of a particular
 *      node, their children, and so on until the leaf nodes of the tree are
 *      reached.
 *
 *   5) An iterator is an object that traverses nodes in a tree along a
 *      particular axis, one at a time.
 *
 *   6) An element is a mark-up tag in an XML document; an attribute is a
 *      modifier on the tag.  For example, in <elem attr='val' attr2='val2'>
 *      "elem" is an element name, "attr" and "attr2" are attribute names with
 *      the values "val" and "val2", respectively.
 *
 *   7) A namespace declaration is a special attribute that is used to associate
 *      a prefix with a URI (the namespace).  The meanings of element names and
 *      attribute names that use that prefix are defined with respect to that
 *      namespace.
 *
 *   8) DOM is an acronym for Document Object Model.  It is a tree
 *      representation of an XML document.
 *
 *      SAX is an acronym for the Simple API for XML processing.  It is an API
 *      used inform an XML processor (in this case XSLTC) of the structure and
 *      content of an XML document.
 *
 *      Input to the stylesheet processor can come from an XML parser in the
 *      form of a DOM tree or through the SAX API.
 *
 *   9) DTD is a document type declaration.  It is a way of specifying the
 *      grammar for an XML file, the names and types of elements, attributes,
 *      etc.
 *
 *  10) XPath is a specification that describes a notation for identifying
 *      nodes in a tree-structured representation of an XML document.  An
 *      instance of that notation is referred to as an XPath expression.
 *
 *  11) Translet is an invented term that refers to the class file that contains
 *      the compiled form of a stylesheet.
 */

    // These message should be read from a locale-specific resource bundle
    /** Get the lookup table for error messages.
     *
     * @return The message lookup table.
     */
    public Object[][] getContents()
    {
      return new Object[][] {
        {ErrorMsg.MULTIPLE_STYLESHEET_ERR,
        "Mais de uma folha de estilos definida no mesmo arquivo."},

        /*
         * Note to translators:  The substitution text is the name of a
         * template.  The same name was used on two different templates in the
         * same stylesheet.
         */
        {ErrorMsg.TEMPLATE_REDEF_ERR,
        "O modelo ''{0}'' j\u00E1 foi definido nesta folha de estilos."},


        /*
         * Note to translators:  The substitution text is the name of a
         * template.  A reference to the template name was encountered, but the
         * template is undefined.
         */
        {ErrorMsg.TEMPLATE_UNDEF_ERR,
        "O modelo ''{0}'' n\u00E3o foi definido nesta folha de estilos."},

        /*
         * Note to translators:  The substitution text is the name of a variable
         * that was defined more than once.
         */
        {ErrorMsg.VARIABLE_REDEF_ERR,
        "A vari\u00E1vel ''{0}'' est\u00E1 definida v\u00E1rias vezes no mesmo escopo."},

        /*
         * Note to translators:  The substitution text is the name of a variable
         * or parameter.  A reference to the variable or parameter was found,
         * but it was never defined.
         */
        {ErrorMsg.VARIABLE_UNDEF_ERR,
        "Vari\u00E1vel ou par\u00E2metro ''{0}'' indefinido."},

        /*
         * Note to translators:  The word "class" here refers to a Java class.
         * Processing the stylesheet required a class to be loaded, but it could
         * not be found.  The substitution text is the name of the class.
         */
        {ErrorMsg.CLASS_NOT_FOUND_ERR,
        "N\u00E3o \u00E9 poss\u00EDvel localizar a classe ''{0}''."},

        /*
         * Note to translators:  The word "method" here refers to a Java method.
         * Processing the stylesheet required a reference to the method named by
         * the substitution text, but it could not be found.  "public" is the
         * Java keyword.
         */
        {ErrorMsg.METHOD_NOT_FOUND_ERR,
        "N\u00E3o \u00E9 poss\u00EDvel localizar o m\u00E9todo externo ''{0}'' (deve ser p\u00FAblico)."},

        /*
         * Note to translators:  The word "method" here refers to a Java method.
         * Processing the stylesheet required a reference to the method named by
         * the substitution text, but no method with the required types of
         * arguments or return type could be found.
         */
        {ErrorMsg.ARGUMENT_CONVERSION_ERR,
        "N\u00E3o \u00E9 poss\u00EDvel converter o argumento/tipo de retorno na chamada para o m\u00E9todo ''{0}''"},

        /*
         * Note to translators:  The file or URI named in the substitution text
         * is missing.
         */
        {ErrorMsg.FILE_NOT_FOUND_ERR,
        "Arquivo ou URI ''{0}'' n\u00E3o encontrado."},

        /*
         * Note to translators:  This message is displayed when the URI
         * mentioned in the substitution text is not well-formed syntactically.
         */
        {ErrorMsg.INVALID_URI_ERR,
        "URI inv\u00E1lido ''{0}''."},

        /*
         * Note to translators:  This message is displayed when the URI
         * mentioned in the substitution text is not well-formed syntactically.
         */
        {ErrorMsg.CATALOG_EXCEPTION,
        "JAXP08090001: O CatalogResolver foi ativado com o cat\u00E1logo \"{0}\", mas uma CatalogException foi retornada."},

        /*
         * Note to translators:  The file or URI named in the substitution text
         * exists but could not be opened.
         */
        {ErrorMsg.FILE_ACCESS_ERR,
        "N\u00E3o \u00E9 poss\u00EDvel abrir o arquivo ou o URI ''{0}''."},

        /*
         * Note to translators: <xsl:stylesheet> and <xsl:transform> are
         * keywords that should not be translated.
         */
        {ErrorMsg.MISSING_ROOT_ERR,
        "elemento <xsl:stylesheet> ou <xsl:transform> esperado."},

        /*
         * Note to translators:  The stylesheet contained a reference to a
         * namespace prefix that was undefined.  The value of the substitution
         * text is the name of the prefix.
         */
        {ErrorMsg.NAMESPACE_UNDEF_ERR,
        "O prefixo do namespace ''{0}'' n\u00E3o foi declarado."},

        /*
         * Note to translators:  The Java function named in the stylesheet could
         * not be found.
         */
        {ErrorMsg.FUNCTION_RESOLVE_ERR,
        "N\u00E3o \u00E9 poss\u00EDvel resolver a chamada para a fun\u00E7\u00E3o ''{0}''."},

        /*
         * Note to translators:  The substitution text is the name of a
         * function.  A literal string here means a constant string value.
         */
        {ErrorMsg.NEED_LITERAL_ERR,
        "O argumento para \"{0}'' deve ser uma string literal."},

        /*
         * Note to translators:  This message indicates there was a syntactic
         * error in the form of an XPath expression.  The substitution text is
         * the expression.
         */
        {ErrorMsg.XPATH_PARSER_ERR,
        "Erro durante o parsing da express\u00E3o XPath ''{0}''."},

        /*
         * Note to translators:  An element in the stylesheet requires a
         * particular attribute named by the substitution text, but that
         * attribute was not specified in the stylesheet.
         */
        {ErrorMsg.REQUIRED_ATTR_ERR,
        "O atributo obrigat\u00F3rio ''{0}'' n\u00E3o foi encontrado."},

        /*
         * Note to translators:  This message indicates that a character not
         * permitted in an XPath expression was encountered.  The substitution
         * text is the offending character.
         */
        {ErrorMsg.ILLEGAL_CHAR_ERR,
        "Caractere inv\u00E1lido ''{0}'' na express\u00E3o XPath."},

        /*
         * Note to translators:  A processing instruction is a mark-up item in
         * an XML document that request some behaviour of an XML processor.  The
         * form of the name of was invalid in this case, and the substitution
         * text is the name.
         */
        {ErrorMsg.ILLEGAL_PI_ERR,
        "Nome inv\u00E1lido ''{0}'' para instru\u00E7\u00E3o de processamento."},

        /*
         * Note to translators:  This message is reported if the stylesheet
         * being processed attempted to construct an XML document with an
         * attribute in a place other than on an element.  The substitution text
         * specifies the name of the attribute.
         */
        {ErrorMsg.STRAY_ATTRIBUTE_ERR,
        "Atributo ''{0}'' fora do elemento."},

        /*
         * Note to translators:  An attribute that wasn't recognized was
         * specified on an element in the stylesheet.  The attribute is named
         * by the substitution
         * text.
         */
        {ErrorMsg.ILLEGAL_ATTRIBUTE_ERR,
        "Atributo ''{0}'' inv\u00E1lido."},

        /*
         * Note to translators:  "import" and "include" are keywords that should
         * not be translated.  This messages indicates that the stylesheet
         * named in the substitution text imported or included itself either
         * directly or indirectly.
         */
        {ErrorMsg.CIRCULAR_INCLUDE_ERR,
        "Import/Include circular. Folha de estilos ''{0}'' j\u00E1 carregada."},

        /*
         * Note to translators:  "xsl:import" and "xsl:include" are keywords that
         * should not be translated.
         */
        {ErrorMsg.IMPORT_PRECEDE_OTHERS_ERR,
        "Os filhos do elemento xsl:import devem preceder todos os outros filhos de um elemento xsl:stylesheet, inclusive quaisquer filhos do elemento xsl:include."},

        /*
         * Note to translators:  A result-tree fragment is a portion of a
         * resulting XML document represented as a tree.  "<xsl:sort>" is a
         * keyword and should not be translated.
         */
        {ErrorMsg.RESULT_TREE_SORT_ERR,
        "Os fragmentos da \u00E1rvore n\u00E3o podem ser classificados (os elementos <xsl:sort> foram ignorados). Voc\u00EA deve classificar os n\u00F3s ao criar a \u00E1rvore de resultados."},

        /*
         * Note to translators:  A name can be given to a particular style to be
         * used to format decimal values.  The substitution text gives the name
         * of such a style for which more than one declaration was encountered.
         */
        {ErrorMsg.SYMBOLS_REDEF_ERR,
        "A formata\u00E7\u00E3o decimal ''{0}'' j\u00E1 foi definida."},

        /*
         * Note to translators:  The stylesheet version named in the
         * substitution text is not supported.
         */
        {ErrorMsg.XSL_VERSION_ERR,
        "A vers\u00E3o XSL \"{0}'' n\u00E3o \u00E9 suportada por XSLTC."},

        /*
         * Note to translators:  The definitions of one or more variables or
         * parameters depend on one another.
         */
        {ErrorMsg.CIRCULAR_VARIABLE_ERR,
        "Refer\u00EAncia de vari\u00E1vel/par\u00E2metro circulares ''{0}''."},

        /*
         * Note to translators:  The operator in an expresion with two operands was
         * not recognized.
         */
        {ErrorMsg.ILLEGAL_BINARY_OP_ERR,
        "Operador desconhecido para a express\u00E3o bin\u00E1ria."},

        /*
         * Note to translators:  This message is produced if a reference to a
         * function has too many or too few arguments.
         */
        {ErrorMsg.ILLEGAL_ARG_ERR,
        "Argumento(s) inv\u00E1lido(s) para a chamada da fun\u00E7\u00E3o."},

        /*
         * Note to translators:  "document()" is the name of function and must
         * not be translated.  A node-set is a set of the nodes in the tree
         * representation of an XML document.
         */
        {ErrorMsg.DOCUMENT_ARG_ERR,
        "O segundo argumento para a fun\u00E7\u00E3o document() deve ser um conjunto de n\u00F3s."},

        /*
         * Note to translators:  "<xsl:when>" and "<xsl:choose>" are keywords
         * and should not be translated.  This message describes a syntax error
         * in the stylesheet.
         */
        {ErrorMsg.MISSING_WHEN_ERR,
        "\u00C9 necess\u00E1rio, pelo menos, um elemento <xsl:when> em <xsl:choose>."},

        /*
         * Note to translators:  "<xsl:otherwise>" and "<xsl:choose>" are
         * keywords and should not be translated.  This message describes a
         * syntax error in the stylesheet.
         */
        {ErrorMsg.MULTIPLE_OTHERWISE_ERR,
        "\u00C9 permitido somente um elemento <xsl:otherwise> em <xsl:choose>."},

        /*
         * Note to translators:  "<xsl:otherwise>" and "<xsl:choose>" are
         * keywords and should not be translated.  This message describes a
         * syntax error in the stylesheet.
         */
        {ErrorMsg.STRAY_OTHERWISE_ERR,
        "<xsl:otherwise> s\u00F3 pode ser usado em <xsl:choose>."},

        /*
         * Note to translators:  "<xsl:when>" and "<xsl:choose>" are keywords
         * and should not be translated.  This message describes a syntax error
         * in the stylesheet.
         */
        {ErrorMsg.STRAY_WHEN_ERR,
        "<xsl:when> s\u00F3 pode ser usado em <xsl:choose>."},

        /*
         * Note to translators:  "<xsl:when>", "<xsl:otherwise>" and
         * "<xsl:choose>" are keywords and should not be translated.  This
         * message describes a syntax error in the stylesheet.
         */
        {ErrorMsg.WHEN_ELEMENT_ERR,
        "Somente os elementos <xsl:when> e <xsl:otherwise> s\u00E3o permitidos em <xsl:choose>."},

        /*
         * Note to translators:  "<xsl:attribute-set>" and "name" are keywords
         * that should not be translated.
         */
        {ErrorMsg.UNNAMED_ATTRIBSET_ERR,
        "<xsl:attribute-set> n\u00E3o encontrado no atributo 'name'."},

        /*
         * Note to translators:  An element in the stylesheet contained an
         * element of a type that it was not permitted to contain.
         */
        {ErrorMsg.ILLEGAL_CHILD_ERR,
        "Elemento filho inv\u00E1lido."},

        /*
         * Note to translators:  The stylesheet tried to create an element with
         * a name that was not a valid XML name.  The substitution text contains
         * the name.
         */
        {ErrorMsg.ILLEGAL_ELEM_NAME_ERR,
        "Voc\u00EA n\u00E3o pode chamar um elemento ''{0}''"},

        /*
         * Note to translators:  The stylesheet tried to create an attribute
         * with a name that was not a valid XML name.  The substitution text
         * contains the name.
         */
        {ErrorMsg.ILLEGAL_ATTR_NAME_ERR,
        "Voc\u00EA n\u00E3o pode chamar um atributo ''{0}''"},

        /*
         * Note to translators:  The children of the outermost element of a
         * stylesheet are referred to as top-level elements.  No text should
         * occur within that outermost element unless it is within a top-level
         * element.  This message indicates that that constraint was violated.
         * "<xsl:stylesheet>" is a keyword that should not be translated.
         */
        {ErrorMsg.ILLEGAL_TEXT_NODE_ERR,
        "Dados de texto fora do elemento <xsl:stylesheet> de n\u00EDvel superior."},

        /*
         * Note to translators:  JAXP is an acronym for the Java API for XML
         * Processing.  This message indicates that the XML parser provided to
         * XSLTC to process the XML input document had a configuration problem.
         */
        {ErrorMsg.SAX_PARSER_CONFIG_ERR,
        "Parser de JAXP n\u00E3o configurado corretamente"},

        /*
         * Note to translators:  The substitution text names the internal error
         * encountered.
         */
        {ErrorMsg.INTERNAL_ERR,
        "Erro interno-XSLTC irrecuper\u00E1vel: ''{0}''"},

        /*
         * Note to translators:  The stylesheet contained an element that was
         * not recognized as part of the XSL syntax.  The substitution text
         * gives the element name.
         */
        {ErrorMsg.UNSUPPORTED_XSL_ERR,
        "Elemento XSL n\u00E3o suportado ''{0}''."},

        /*
         * Note to translators:  The stylesheet referred to an extension to the
         * XSL syntax and indicated that it was defined by XSLTC, but XSTLC does
         * not recognized the particular extension named.  The substitution text
         * gives the extension name.
         */
        {ErrorMsg.UNSUPPORTED_EXT_ERR,
        "Extens\u00E3o de XSLTC n\u00E3o reconhecida ''{0}''."},

        /*
         * Note to translators:  The XML document given to XSLTC as a stylesheet
         * was not, in fact, a stylesheet.  XSLTC is able to detect that in this
         * case because the outermost element in the stylesheet has to be
         * declared with respect to the XSL namespace URI, but no declaration
         * for that namespace was seen.
         */
        {ErrorMsg.MISSING_XSLT_URI_ERR,
        "O documento de entrada n\u00E3o \u00E9 uma folha de estilos (o namespace XSL n\u00E3o foi declarado no elemento-raiz)."},

        /*
         * Note to translators:  XSLTC could not find the stylesheet document
         * with the name specified by the substitution text.
         */
        {ErrorMsg.MISSING_XSLT_TARGET_ERR,
        "N\u00E3o foi poss\u00EDvel localizar o alvo da folha de estilos ''{0}''."},

        /*
         * Note to translators:  access to the stylesheet target is denied
         */
        {ErrorMsg.ACCESSING_XSLT_TARGET_ERR,
        "N\u00E3o foi poss\u00EDvel ler o alvo ''{0}'' da folha de estilos, porque o acesso a ''{1}'' n\u00E3o \u00E9 permitido em virtude da restri\u00E7\u00E3o definida pela propriedade accessExternalStylesheet."},

        /*
         * Note to translators:  This message represents an internal error in
         * condition in XSLTC.  The substitution text is the class name in XSLTC
         * that is missing some functionality.
         */
        {ErrorMsg.NOT_IMPLEMENTED_ERR,
        "N\u00E3o implementado: ''{0}''."},

        /*
         * Note to translators:  The XML document given to XSLTC as a stylesheet
         * was not, in fact, a stylesheet.
         */
        {ErrorMsg.NOT_STYLESHEET_ERR,
        "O documento de entrada n\u00E3o cont\u00E9m uma folha de estilos XSL."},

        /*
         * Note to translators:  The element named in the substitution text was
         * encountered in the stylesheet but is not recognized.
         */
        {ErrorMsg.ELEMENT_PARSE_ERR,
        "N\u00E3o foi poss\u00EDvel fazer parsing do elemento ''{0}''"},

        /*
         * Note to translators:  "use", "<key>", "node", "node-set", "string"
         * and "number" are keywords in this context and should not be
         * translated.  This message indicates that the value of the "use"
         * attribute was not one of the permitted values.
         */
        {ErrorMsg.KEY_USE_ATTR_ERR,
        "O atributo use de <key> deve ser node, node-set, string ou number."},

        /*
         * Note to translators:  An XML document can specify the version of the
         * XML specification to which it adheres.  This message indicates that
         * the version specified for the output document was not valid.
         */
        {ErrorMsg.OUTPUT_VERSION_ERR,
        "A vers\u00E3o do documento XML de sa\u00EDda deve ser 1.0"},

        /*
         * Note to translators:  The operator in a comparison operation was
         * not recognized.
         */
        {ErrorMsg.ILLEGAL_RELAT_OP_ERR,
        "Opera\u00E7\u00E3o desconhecida para a express\u00E3o relacional"},

        /*
         * Note to translators:  An attribute set defines as a set of XML
         * attributes that can be added to an element in the output XML document
         * as a group.  This message is reported if the name specified was not
         * used to declare an attribute set.  The substitution text is the name
         * that is in error.
         */
        {ErrorMsg.ATTRIBSET_UNDEF_ERR,
        "Tentativa de usar um conjunto de atributos ''{0}'' n\u00E3o existente."},

        /*
         * Note to translators:  The term "attribute value template" is a term
         * defined by XSLT which describes the value of an attribute that is
         * determined by an XPath expression.  The message indicates that the
         * expression was syntactically incorrect; the substitution text
         * contains the expression that was in error.
         */
        {ErrorMsg.ATTR_VAL_TEMPLATE_ERR,
        "N\u00E3o \u00E9 poss\u00EDvel fazer parsing do modelo do valor do atributo ''{0}''."},

        /*
         * Note to translators:  ???
         */
        {ErrorMsg.UNKNOWN_SIG_TYPE_ERR,
        "Tipo de dados desconhecido na assinatura da classe ''{0}''."},

        /*
         * Note to translators:  The substitution text refers to data types.
         * The message is displayed if a value in a particular context needs to
         * be converted to type {1}, but that's not possible for a value of
         * type {0}.
         */
        {ErrorMsg.DATA_CONVERSION_ERR,
        "N\u00E3o \u00E9 poss\u00EDvel converter o tipo de dados ''{0}'' em ''{1}''."},

        /*
         * Note to translators:  "Templates" is a Java class name that should
         * not be translated.
         */
        {ErrorMsg.NO_TRANSLET_CLASS_ERR,
        "Este Templates n\u00E3o cont\u00E9m uma defini\u00E7\u00E3o de classe translet v\u00E1lida."},

        /*
         * Note to translators:  "Templates" is a Java class name that should
         * not be translated.
         */
        {ErrorMsg.NO_MAIN_TRANSLET_ERR,
        "Este Templates n\u00E3o cont\u00E9m uma classe com o nome ''{0}''."},

        /*
         * Note to translators:  The substitution text is the name of a class.
         */
        {ErrorMsg.TRANSLET_CLASS_ERR,
        "N\u00E3o foi poss\u00EDvel carregar a classe translet ''{0}''."},

        {ErrorMsg.TRANSLET_OBJECT_ERR,
        "Classe translet carregada, mas n\u00E3o \u00E9 poss\u00EDvel criar uma inst\u00E2ncia translet."},

        /*
         * Note to translators:  "ErrorListener" is a Java interface name that
         * should not be translated.  The message indicates that the user tried
         * to set an ErrorListener object on object of the class named in the
         * substitution text with "null" Java value.
         */
        {ErrorMsg.ERROR_LISTENER_NULL_ERR,
        "Tentativa de definir ErrorListener para ''{0}'' como nulo"},

        /*
         * Note to translators:  StreamSource, SAXSource and DOMSource are Java
         * interface names that should not be translated.
         */
        {ErrorMsg.JAXP_UNKNOWN_SOURCE_ERR,
        "Somente StreamSource, SAXSource e DOMSource s\u00E3o suportados por XSLTC"},

        /*
         * Note to translators:  "Source" is a Java class name that should not
         * be translated.  The substitution text is the name of Java method.
         */
        {ErrorMsg.JAXP_NO_SOURCE_ERR,
        "O objeto source especificado para ''{0}'' n\u00E3o tem conte\u00FAdo."},

        /*
         * Note to translators:  The message indicates that XSLTC failed to
         * compile the stylesheet into a translet (class file).
         */
        {ErrorMsg.JAXP_COMPILE_ERR,
        "N\u00E3o foi poss\u00EDvel compilar a folha de estilos"},

        /*
         * Note to translators:  "TransformerFactory" is a class name.  In this
         * context, an attribute is a property or setting of the
         * TransformerFactory object.  The substitution text is the name of the
         * unrecognised attribute.  The method used to retrieve the attribute is
         * "getAttribute", so it's not clear whether it would be best to
         * translate the term "attribute".
         */
        {ErrorMsg.JAXP_INVALID_ATTR_ERR,
        "TransformerFactory n\u00E3o reconhece o atributo ''{0}''."},

        {ErrorMsg.JAXP_INVALID_ATTR_VALUE_ERR,
        "Valor incorreto especificado para o atributo ''{0}''."},

        /*
         * Note to translators:  "setResult()" and "startDocument()" are Java
         * method names that should not be translated.
         */
        {ErrorMsg.JAXP_SET_RESULT_ERR,
        "setResult() deve ser chamado antes de startDocument()."},

        /*
         * Note to translators:  "Transformer" is a Java interface name that
         * should not be translated.  A Transformer object should contained a
         * reference to a translet object in order to be used for
         * transformations; this message is produced if that requirement is not
         * met.
         */
        {ErrorMsg.JAXP_NO_TRANSLET_ERR,
        "O Transformer n\u00E3o tem um objeto translet encapsulado."},

        /*
         * Note to translators:  The XML document that results from a
         * transformation needs to be sent to an output handler object; this
         * message is produced if that requirement is not met.
         */
        {ErrorMsg.JAXP_NO_HANDLER_ERR,
        "Nenhum handler de sa\u00EDda definido para o resultado da transforma\u00E7\u00E3o."},

        /*
         * Note to translators:  "Result" is a Java interface name in this
         * context.  The substitution text is a method name.
         */
        {ErrorMsg.JAXP_NO_RESULT_ERR,
        "O objeto result especificado para ''{0}'' \u00E9 inv\u00E1lido."},

        /*
         * Note to translators:  "Transformer" is a Java interface name.  The
         * user's program attempted to access an unrecognized property with the
         * name specified in the substitution text.  The method used to retrieve
         * the property is "getOutputProperty", so it's not clear whether it
         * would be best to translate the term "property".
         */
        {ErrorMsg.JAXP_UNKNOWN_PROP_ERR,
        "Tentativa de acessar a propriedade ''{0}'' do Transformer inv\u00E1lida."},

        /*
         * Note to translators:  SAX2DOM is the name of a Java class that should
         * not be translated.  This is an adapter in the sense that it takes a
         * DOM object and converts it to something that uses the SAX API.
         */
        {ErrorMsg.SAX2DOM_ADAPTER_ERR,
        "N\u00E3o foi poss\u00EDvel criar o adaptador SAX2DOM: ''{0}''."},

        /*
         * Note to translators:  "XSLTCSource.build()" is a Java method name.
         * "systemId" is an XML term that is short for "system identification".
         */
        {ErrorMsg.XSLTC_SOURCE_ERR,
        "XSLTCSource.build() chamado sem o systemId ser definido."},

        { ErrorMsg.ER_RESULT_NULL,
            "O resultado n\u00E3o deve ser nulo"},

        /*
         * Note to translators:  This message indicates that the value argument
         * of setParameter must be a valid Java Object.
         */
        {ErrorMsg.JAXP_INVALID_SET_PARAM_VALUE,
        "O valor do par\u00E2metro {0} deve ser um Objeto Java v\u00E1lido"},


        {ErrorMsg.COMPILE_STDIN_ERR,
        "A op\u00E7\u00E3o -i deve ser usada com a op\u00E7\u00E3o -o."},


        /*
         * Note to translators:  This message contains usage information for a
         * means of invoking XSLTC from the command-line.  The message is
         * formatted for presentation in English.  The strings <output>,
         * <directory>, etc. indicate user-specified argument values, and can
         * be translated - the argument <package> refers to a Java package, so
         * it should be handled in the same way the term is handled for JDK
         * documentation.
         */
        {ErrorMsg.COMPILE_USAGE_STR,
        "SINOPSE\n   java com.sun.org.apache.xalan.internal.xsltc.cmdline.Compile [-o <sa\u00EDda>]\n      [-d <diret\u00F3rio>] [-j <jarfile>] [-p <pacote>]\n      [-n] [-x] [-u] [-v] [-h] { <folha de estilos> | -i }\n\nOP\u00C7\u00D5ES\n   -o <sa\u00EDda>    atribui o nome <sa\u00EDda> ao translet\n                  gerado.  Por padr\u00E3o, o nome translet\n                  origina-se do nome <folha de estilos>.  Esta op\u00E7\u00E3o\n                  \u00E9 ignorada caso sejam compiladas v\u00E1rias folhas de estilos.\n   -d <diret\u00F3rio> especifica um diret\u00F3rio de destino para translet\n   -j <arquivo jar>   empacota as classes translet em um arquivo jar do\n                  nome especificado como <arquivo jar>\n   -p <pacote>   especifica um prefixo de nome do pacote para todas as classes\n                  translet geradas.\n   -n             permite a inclus\u00E3o do modelo na linha (comportamento padr\u00E3o melhor\n                  em m\u00E9dia).\n   -x             ativa a sa\u00EDda de mensagens de depura\u00E7\u00E3o adicionais\n   -u             interpreta os argumentos <folha de estilos> como URLs\n   -i             obriga o compilador a ler a folha de estilos de stdin\n   -v             imprime a vers\u00E3o do compilador\n   -h             imprime esta instru\u00E7\u00E3o de uso\n"},

        /*
         * Note to translators:  This message contains usage information for a
         * means of invoking XSLTC from the command-line.  The message is
         * formatted for presentation in English.  The strings <jarfile>,
         * <document>, etc. indicate user-specified argument values, and can
         * be translated - the argument <class> refers to a Java class, so it
         * should be handled in the same way the term is handled for JDK
         * documentation.
         */
        {ErrorMsg.TRANSFORM_USAGE_STR,
        "SINOPSE \n   java com.sun.org.apache.xalan.internal.xsltc.cmdline.Transforme [-j <arquivo jar>]\n      [-x] [-n <itera\u00E7\u00F5es>] {-u <url_documento> | <documento>}\n      <classe> [<par\u00E2m1>=<valor1> ...]\n\n   usa a <classe> translet para transformar um documento XML \n   especificado como <documento>. O translet <classe> est\u00E1 no\n   CLASSPATH do usu\u00E1rio ou no <arquivo jar> opcionalmente especificado.\nOP\u00C7\u00D5ES\n   -j <arquivo jar>    especifica um arquivo jar com base no qual ser\u00E1 carregado o translet\n   -x              ativa a sa\u00EDda de mensagens de depura\u00E7\u00E3o adicionais\n   -n <itera\u00E7\u00F5es> executa a transforma\u00E7\u00E3o <itera\u00E7\u00F5es> vezes e\n                   exibe as informa\u00E7\u00F5es de perfis\n   -u <url_documento> especifica o documento XML de entrada na forma de URL\n"},



        /*
         * Note to translators:  "<xsl:sort>", "<xsl:for-each>" and
         * "<xsl:apply-templates>" are keywords that should not be translated.
         * The message indicates that an xsl:sort element must be a child of
         * one of the other kinds of elements mentioned.
         */
        {ErrorMsg.STRAY_SORT_ERR,
        "<xsl:sort> s\u00F3 pode ser usado dentro de <xsl:for-each> ou <xsl:apply-templates>."},

        /*
         * Note to translators:  The message indicates that the encoding
         * requested for the output document was on that requires support that
         * is not available from the Java Virtual Machine being used to execute
         * the program.
         */
        {ErrorMsg.UNSUPPORTED_ENCODING,
        "A codifica\u00E7\u00E3o de sa\u00EDda ''{0}'' n\u00E3o \u00E9 suportada nesta JVM."},

        /*
         * Note to translators:  The message indicates that the XPath expression
         * named in the substitution text was not well formed syntactically.
         */
        {ErrorMsg.SYNTAX_ERR,
        "Erro de sintaxe em ''{0}''."},

        /*
         * Note to translators:  The substitution text is the name of a Java
         * class.  The term "constructor" here is the Java term.  The message is
         * displayed if XSLTC could not find a constructor for the specified
         * class.
         */
        {ErrorMsg.CONSTRUCTOR_NOT_FOUND,
        "N\u00E3o \u00E9 poss\u00EDvel localizar o construtor externo ''{0}''."},

        /*
         * Note to translators:  "static" is the Java keyword.  The substitution
         * text is the name of a function.  The first argument of that function
         * is not of the required type.
         */
        {ErrorMsg.NO_JAVA_FUNCT_THIS_REF,
        "O primeiro argumento para a fun\u00E7\u00E3o Java n\u00E3o static ''{0}'' n\u00E3o \u00E9 uma refer\u00EAncia de objeto v\u00E1lida."},

        /*
         * Note to translators:  An XPath expression was not of the type
         * required in a particular context.  The substitution text is the
         * expression that was in error.
         */
        {ErrorMsg.TYPE_CHECK_ERR,
        "Erro ao verificar o tipo de express\u00E3o ''{0}''."},

        /*
         * Note to translators:  An XPath expression was not of the type
         * required in a particular context.  However, the location of the
         * problematic expression is unknown.
         */
        {ErrorMsg.TYPE_CHECK_UNK_LOC_ERR,
        "Erro ao verificar o tipo de uma express\u00E3o em uma localiza\u00E7\u00E3o desconhecida."},

        /*
         * Note to translators:  The substitution text is the name of a command-
         * line option that was not recognized.
         */
        {ErrorMsg.ILLEGAL_CMDLINE_OPTION_ERR,
        "A op\u00E7\u00E3o da linha de comandos ''{0}'' n\u00E3o \u00E9 v\u00E1lida."},

        /*
         * Note to translators:  The substitution text is the name of a command-
         * line option.
         */
        {ErrorMsg.CMDLINE_OPT_MISSING_ARG_ERR,
        "A op\u00E7\u00E3o da linha de comandos ''{0}'' n\u00E3o encontrou um argumento obrigat\u00F3rio."},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text contains two error
         * messages.  The spacing before the second substitution text indents
         * it the same amount as the first in English.
         */
        {ErrorMsg.WARNING_PLUS_WRAPPED_MSG,
        "WARNING:  ''{0}''\n       :{1}"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text is an error message.
         */
        {ErrorMsg.WARNING_MSG,
        "WARNING:  ''{0}''"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text contains two error
         * messages.  The spacing before the second substitution text indents
         * it the same amount as the first in English.
         */
        {ErrorMsg.FATAL_ERR_PLUS_WRAPPED_MSG,
        "FATAL ERROR:  ''{0}''\n           :{1}"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text is an error message.
         */
        {ErrorMsg.FATAL_ERR_MSG,
        "FATAL ERROR:  ''{0}''"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text contains two error
         * messages.  The spacing before the second substitution text indents
         * it the same amount as the first in English.
         */
        {ErrorMsg.ERROR_PLUS_WRAPPED_MSG,
        "ERROR:  ''{0}''\n     :{1}"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text is an error message.
         */
        {ErrorMsg.ERROR_MSG,
        "ERROR:  ''{0}''"},

        /*
         * Note to translators:  The substitution text is the name of a class.
         */
        {ErrorMsg.TRANSFORM_WITH_TRANSLET_STR,
        "Transformar usando translet ''{0}'' "},

        /*
         * Note to translators:  The first substitution is the name of a class,
         * while the second substitution is the name of a jar file.
         */
        {ErrorMsg.TRANSFORM_WITH_JAR_STR,
        "Transformar usando translet ''{0}'' do arquivo jar ''{1}''"},

        /*
         * Note to translators:  "TransformerFactory" is the name of a Java
         * interface and must not be translated.  The substitution text is
         * the name of the class that could not be instantiated.
         */
        {ErrorMsg.COULD_NOT_CREATE_TRANS_FACT,
        "N\u00E3o foi poss\u00EDvel criar uma inst\u00E2ncia da classe TransformerFactory ''{0}''."},

        /*
         * Note to translators:  This message is produced when the user
         * specified a name for the translet class that contains characters
         * that are not permitted in a Java class name.  The substitution
         * text "{0}" specifies the name the user requested, while "{1}"
         * specifies the name the processor used instead.
         */
        {ErrorMsg.TRANSLET_NAME_JAVA_CONFLICT,
         "N\u00E3o foi poss\u00EDvel usar o nome ''{0}'' como o nome da classe translet, pois ele cont\u00E9m caracteres que n\u00E3o s\u00E3o permitidos no nome da classe Java. O nome ''{1}'' foi usado."},

        /*
         * Note to translators:  The following message is used as a header.
         * All the error messages are collected together and displayed beneath
         * this message.
         */
        {ErrorMsg.COMPILER_ERROR_KEY,
        "Erros do compilador:"},

        /*
         * Note to translators:  The following message is used as a header.
         * All the warning messages are collected together and displayed
         * beneath this message.
         */
        {ErrorMsg.COMPILER_WARNING_KEY,
        "Advert\u00EAncias do compilador:"},

        /*
         * Note to translators:  The following message is used as a header.
         * All the error messages that are produced when the stylesheet is
         * applied to an input document are collected together and displayed
         * beneath this message.  A 'translet' is the compiled form of a
         * stylesheet (see above).
         */
        {ErrorMsg.RUNTIME_ERROR_KEY,
        "Erros de translet:"},

        /*
         * Note to translators:  An attribute whose value is constrained to
         * be a "QName" or a list of "QNames" had a value that was incorrect.
         * 'QName' is an XML syntactic term that must not be translated.  The
         * substitution text contains the actual value of the attribute.
         */
        {ErrorMsg.INVALID_QNAME_ERR,
        "Um atributo cujo valor deve ser um QName ou uma lista de QNames separada por espa\u00E7os em branco tinha o valor ''{0}''"},

        /*
         * Note to translators:  An attribute whose value is required to
         * be an "NCName".
         * 'NCName' is an XML syntactic term that must not be translated.  The
         * substitution text contains the actual value of the attribute.
         */
        {ErrorMsg.INVALID_NCNAME_ERR,
        "Um atributo cujo valor deve ser um NCName tinha o valor ''{0}''"},

        /*
         * Note to translators:  An attribute with an incorrect value was
         * encountered.  The permitted value is one of the literal values
         * "xml", "html" or "text"; it is also permitted to have the form of
         * a QName that is not also an NCName.  The terms "method",
         * "xsl:output", "xml", "html" and "text" are keywords that must not
         * be translated.  The term "qname-but-not-ncname" is an XML syntactic
         * term.  The substitution text contains the actual value of the
         * attribute.
         */
        {ErrorMsg.INVALID_METHOD_IN_OUTPUT,
        "O atributo method de um elemento <xsl:output> tinha o valor ''{0}''. O valor deve ser um dos seguintes: ''xml'', ''html'', ''text'', ou qname, mas n\u00E3o ncname"},

        {ErrorMsg.JAXP_GET_FEATURE_NULL_NAME,
        "O nome do recurso n\u00E3o pode ser nulo em TransformerFactory.getFeature(Nome da string)."},

        {ErrorMsg.JAXP_SET_FEATURE_NULL_NAME,
        "O nome do recurso n\u00E3o pode ser nulo em TransformerFactory.setFeature(Nome da string, valor booliano)."},

        {ErrorMsg.JAXP_UNSUPPORTED_FEATURE,
        "N\u00E3o \u00E9 poss\u00EDvel definir o recurso ''{0}'' nesta TransformerFactory."},

        {ErrorMsg.JAXP_SECUREPROCESSING_FEATURE,
        "FEATURE_SECURE_PROCESSING: N\u00E3o \u00E9 poss\u00EDvel definir o recurso como falso quando o gerenciador de seguran\u00E7a est\u00E1 presente."},

        /*
         * Note to translators:  This message describes an internal error in the
         * processor.  The term "byte code" is a Java technical term for the
         * executable code in a Java method, and "try-catch-finally block"
         * refers to the Java keywords with those names.  "Outlined" is a
         * technical term internal to XSLTC and should not be translated.
         */
        {ErrorMsg.OUTLINE_ERR_TRY_CATCH,
         "Erro interno de XSLTC: o byte code gerado cont\u00E9m um bloco try-catch-finally e n\u00E3o pode ser outlined."},

        /*
         * Note to translators:  This message describes an internal error in the
         * processor.  The terms "OutlineableChunkStart" and
         * "OutlineableChunkEnd" are the names of classes internal to XSLTC and
         * should not be translated.  The message indicates that for every
         * "start" there must be a corresponding "end", and vice versa, and
         * that if one of a pair of "start" and "end" appears between another
         * pair of corresponding "start" and "end", then the other half of the
         * pair must also be between that same enclosing pair.
         */
        {ErrorMsg.OUTLINE_ERR_UNBALANCED_MARKERS,
         "Erro interno de XSLTC: os marcadores OutlineableChunkStart e OutlineableChunkEnd devem ser balanceados e aninhados corretamente."},

        /*
         * Note to translators:  This message describes an internal error in the
         * processor.  The term "byte code" is a Java technical term for the
         * executable code in a Java method.  The "method" that is being
         * referred to is a Java method in a translet that XSLTC is generating
         * in processing a stylesheet.  The "instruction" that is being
         * referred to is one of the instrutions in the Java byte code in that
         * method.  "Outlined" is a technical term internal to XSLTC and
         * should not be translated.
         */
        {ErrorMsg.OUTLINE_ERR_DELETED_TARGET,
         "Erro interno de XSLTC: ainda h\u00E1 refer\u00EAncia no m\u00E9todo original a uma instru\u00E7\u00E3o que fazia parte de um bloco de byte code que foi outlined."
        },


        /*
         * Note to translators:  This message describes an internal error in the
         * processor.  The "method" that is being referred to is a Java method
         * in a translet that XSLTC is generating.
         *
         */
        {ErrorMsg.OUTLINE_ERR_METHOD_TOO_BIG,
         "Erro interno de XSLTC: um m\u00E9todo no translet excede a limita\u00E7\u00E3o da M\u00E1quina Virtual Java quanto ao tamanho de um m\u00E9todo de de 64 kilobytes. Em geral, essa situa\u00E7\u00E3o \u00E9 causada por modelos de uma folha de estilos que s\u00E3o muito grandes. Tente reestruturar sua folha de estilos de forma a usar modelos menores."
        },

         {ErrorMsg.DESERIALIZE_TRANSLET_ERR, "Quando a seguran\u00E7a do Java est\u00E1 ativada, o suporte para desserializar TemplatesImpl fica desativado. Essa situa\u00E7\u00E3o pode ser corrigida definindo a propriedade do sistema jdk.xml.enableTemplatesImplDeserialization como true."}

    };

    }
}
