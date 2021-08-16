/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sun.org.apache.xml.internal.serializer.utils;

import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * An instance of this class is a ListResourceBundle that
 * has the required getContents() method that returns
 * an array of message-key/message associations.
 * <p>
 * The message keys are defined in {@link MsgKey}. The
 * messages that those keys map to are defined here.
 * <p>
 * The messages in the English version are intended to be
 * translated.
 *
 * This class is not a public API, it is only public because it is
 * used in com.sun.org.apache.xml.internal.serializer.
 *
 * @xsl.usage internal
 */
public class SerializerMessages_pt_BR extends ListResourceBundle {

    /*
     * This file contains error and warning messages related to
     * Serializer Error Handling.
     *
     *  General notes to translators:

     *  1) A stylesheet is a description of how to transform an input XML document
     *     into a resultant XML document (or HTML document or text).  The
     *     stylesheet itself is described in the form of an XML document.

     *
     *  2) An element is a mark-up tag in an XML document; an attribute is a
     *     modifier on the tag.  For example, in <elem attr='val' attr2='val2'>
     *     "elem" is an element name, "attr" and "attr2" are attribute names with
     *     the values "val" and "val2", respectively.
     *
     *  3) A namespace declaration is a special attribute that is used to associate
     *     a prefix with a URI (the namespace).  The meanings of element names and
     *     attribute names that use that prefix are defined with respect to that
     *     namespace.
     *
     *
     */

    /** The lookup table for error messages.   */
    public Object[][] getContents() {
        Object[][] contents = new Object[][] {
            {   MsgKey.BAD_MSGKEY,
                "A chave de mensagem ''{0}'' n\u00E3o est\u00E1 na classe de mensagem ''{1}''" },

            {   MsgKey.BAD_MSGFORMAT,
                "Houve falha no formato da mensagem ''{0}'' na classe de mensagem ''{1}''." },

            {   MsgKey.ER_SERIALIZER_NOT_CONTENTHANDLER,
                "A classe ''{0}'' do serializador n\u00E3o implementa org.xml.sax.ContentHandler." },

            {   MsgKey.ER_RESOURCE_COULD_NOT_FIND,
                    "N\u00E3o foi poss\u00EDvel encontrar o recurso [ {0} ].\n {1}" },

            {   MsgKey.ER_RESOURCE_COULD_NOT_LOAD,
                    "O recurso [ {0} ] n\u00E3o foi carregado: {1} \n {2} \t {3}" },

            {   MsgKey.ER_BUFFER_SIZE_LESSTHAN_ZERO,
                    "Tamanho do buffer <=0" },

            {   MsgKey.ER_INVALID_UTF16_SURROGATE,
                    "Foi detectado um substituto de UTF-16 inv\u00E1lido: {0} ?" },

            {   MsgKey.ER_OIERROR,
                "Erro de E/S" },

            {   MsgKey.ER_ILLEGAL_ATTRIBUTE_POSITION,
                "N\u00E3o \u00E9 poss\u00EDvel adicionar o atributo {0} depois dos n\u00F3s filhos ou antes que um elemento seja produzido. O atributo ser\u00E1 ignorado." },

            /*
             * Note to translators:  The stylesheet contained a reference to a
             * namespace prefix that was undefined.  The value of the substitution
             * text is the name of the prefix.
             */
            {   MsgKey.ER_NAMESPACE_PREFIX,
                "O namespace do prefixo ''{0}'' n\u00E3o foi declarado." },

            /*
             * Note to translators:  This message is reported if the stylesheet
             * being processed attempted to construct an XML document with an
             * attribute in a place other than on an element.  The substitution text
             * specifies the name of the attribute.
             */
            {   MsgKey.ER_STRAY_ATTRIBUTE,
                "Atributo ''{0}'' fora do elemento." },

            /*
             * Note to translators:  As with the preceding message, a namespace
             * declaration has the form of an attribute and is only permitted to
             * appear on an element.  The substitution text {0} is the namespace
             * prefix and {1} is the URI that was being used in the erroneous
             * namespace declaration.
             */
            {   MsgKey.ER_STRAY_NAMESPACE,
                "Declara\u00E7\u00E3o de namespace ''{0}''=''{1}'' fora do elemento." },

            {   MsgKey.ER_COULD_NOT_LOAD_RESOURCE,
                "N\u00E3o foi poss\u00EDvel carregar ''{0}'' (verificar CLASSPATH); usando agora apenas os padr\u00F5es" },

            {   MsgKey.ER_ILLEGAL_CHARACTER,
                "Tentativa de exibir um caractere de valor integral {0} que n\u00E3o est\u00E1 representado na codifica\u00E7\u00E3o de sa\u00EDda especificada de {1}." },

            {   MsgKey.ER_COULD_NOT_LOAD_METHOD_PROPERTY,
                "N\u00E3o foi poss\u00EDvel carregar o arquivo de propriedade ''{0}'' para o m\u00E9todo de sa\u00EDda ''{1}'' (verificar CLASSPATH)" },

            {   MsgKey.ER_INVALID_PORT,
                "N\u00FAmero de porta inv\u00E1lido" },

            {   MsgKey.ER_PORT_WHEN_HOST_NULL,
                "A porta n\u00E3o pode ser definida quando o host \u00E9 nulo" },

            {   MsgKey.ER_HOST_ADDRESS_NOT_WELLFORMED,
                "O host n\u00E3o \u00E9 um endere\u00E7o correto" },

            {   MsgKey.ER_SCHEME_NOT_CONFORMANT,
                "O esquema n\u00E3o \u00E9 compat\u00EDvel." },

            {   MsgKey.ER_SCHEME_FROM_NULL_STRING,
                "N\u00E3o \u00E9 poss\u00EDvel definir o esquema de uma string nula" },

            {   MsgKey.ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
                "O caminho cont\u00E9m uma sequ\u00EAncia inv\u00E1lida de caracteres de escape" },

            {   MsgKey.ER_PATH_INVALID_CHAR,
                "O caminho cont\u00E9m um caractere inv\u00E1lido: {0}" },

            {   MsgKey.ER_FRAG_INVALID_CHAR,
                "O fragmento cont\u00E9m um caractere inv\u00E1lido" },

            {   MsgKey.ER_FRAG_WHEN_PATH_NULL,
                "O fragmento n\u00E3o pode ser definido quando o caminho \u00E9 nulo" },

            {   MsgKey.ER_FRAG_FOR_GENERIC_URI,
                "O fragmento s\u00F3 pode ser definido para um URI gen\u00E9rico" },

            {   MsgKey.ER_NO_SCHEME_IN_URI,
                "Nenhum esquema encontrado no URI" },

            {   MsgKey.ER_CANNOT_INIT_URI_EMPTY_PARMS,
                "N\u00E3o \u00E9 poss\u00EDvel inicializar o URI com par\u00E2metros vazios" },

            {   MsgKey.ER_NO_FRAGMENT_STRING_IN_PATH,
                "O fragmento n\u00E3o pode ser especificado no caminho nem no fragmento" },

            {   MsgKey.ER_NO_QUERY_STRING_IN_PATH,
                "A string de consulta n\u00E3o pode ser especificada no caminho nem na string de consulta" },

            {   MsgKey.ER_NO_PORT_IF_NO_HOST,
                "A porta n\u00E3o pode ser especificada se o host n\u00E3o tiver sido especificado" },

            {   MsgKey.ER_NO_USERINFO_IF_NO_HOST,
                "As informa\u00E7\u00F5es do usu\u00E1rio n\u00E3o podem ser especificadas se o host n\u00E3o tiver sido especificado" },

            {   MsgKey.ER_XML_VERSION_NOT_SUPPORTED,
                "Advert\u00EAncia: a vers\u00E3o do documento de sa\u00EDda deve ser obrigatoriamente ''{0}''. Esta vers\u00E3o do XML n\u00E3o \u00E9 suportada. A vers\u00E3o do documento de sa\u00EDda ser\u00E1 ''1.0''." },

            {   MsgKey.ER_SCHEME_REQUIRED,
                "O esquema \u00E9 obrigat\u00F3rio!" },

            /*
             * Note to translators:  The words 'Properties' and
             * 'SerializerFactory' in this message are Java class names
             * and should not be translated.
             */
            {   MsgKey.ER_FACTORY_PROPERTY_MISSING,
                "O objeto Properties especificado para a SerializerFactory n\u00E3o tem uma propriedade ''{0}''." },

            {   MsgKey.ER_ENCODING_NOT_SUPPORTED,
                "Advert\u00EAncia: a codifica\u00E7\u00E3o ''{0}'' n\u00E3o \u00E9 suportada pelo Java runtime." },

             {MsgKey.ER_FEATURE_NOT_FOUND,
             "O par\u00E2metro ''{0}'' n\u00E3o \u00E9 reconhecido."},

             {MsgKey.ER_FEATURE_NOT_SUPPORTED,
             "O par\u00E2metro ''{0}'' \u00E9 reconhecido, mas o valor solicitado n\u00E3o pode ser definido."},

             {MsgKey.ER_STRING_TOO_LONG,
             "A string resultante \u00E9 muito longa para se ajustar a uma DOMString: ''{0}''."},

             {MsgKey.ER_TYPE_MISMATCH_ERR,
             "O tipo de valor do nome deste par\u00E2metro \u00E9 incompat\u00EDvel com o tipo de valor esperado."},

             {MsgKey.ER_NO_OUTPUT_SPECIFIED,
             "O destino da sa\u00EDda dos dados a serem gravados era nulo."},

             {MsgKey.ER_UNSUPPORTED_ENCODING,
             "Uma codifica\u00E7\u00E3o n\u00E3o suportada foi encontrada."},

             {MsgKey.ER_UNABLE_TO_SERIALIZE_NODE,
             "N\u00E3o foi poss\u00EDvel serializar o n\u00F3."},

             {MsgKey.ER_CDATA_SECTIONS_SPLIT,
             "A Se\u00E7\u00E3o CDATA cont\u00E9m um ou mais marcadores de t\u00E9rmino ']]>'."},

             {MsgKey.ER_WARNING_WF_NOT_CHECKED,
                 "N\u00E3o foi poss\u00EDvel criar uma inst\u00E2ncia do verificador de Formato Correto. O par\u00E2metro formatado corretamente foi definido como verdadeiro, mas a verifica\u00E7\u00E3o de formato correto n\u00E3o pode ser executada."
             },

             {MsgKey.ER_WF_INVALID_CHARACTER,
                 "O n\u00F3 ''{0}'' cont\u00E9m caracteres XML inv\u00E1lidos."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_COMMENT,
                 "Um caractere XML inv\u00E1lido (Unicode: 0x{0}) foi encontrado no coment\u00E1rio."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_PI,
                 "Um caractere XML inv\u00E1lido (Unicode: 0x{0}) foi encontrado nos dados da instru\u00E7\u00E3o de processamento."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_CDATA,
                 "Um caractere XML inv\u00E1lido (Unicode: 0x {0}) foi encontrado no conte\u00FAdo da Se\u00E7\u00E3o CDATA."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_TEXT,
                 "Um caractere XML inv\u00E1lido (Unicode: 0x {0}) foi encontrado no conte\u00FAdo dos dados de caracteres do n\u00F3."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                 "Um ou mais caracteres XML inv\u00E1lidos foram encontrados no n\u00F3 {0} chamado ''{1}''."
             },

             { MsgKey.ER_WF_DASH_IN_COMMENT,
                 "A string \"--\" n\u00E3o \u00E9 permitida nos coment\u00E1rios."
             },

             {MsgKey.ER_WF_LT_IN_ATTVAL,
                 "O valor do atributo \"{1}\" associado a um tipo de elemento \"{0}\" n\u00E3o deve conter o caractere ''<''."
             },

             {MsgKey.ER_WF_REF_TO_UNPARSED_ENT,
                 "A refer\u00EAncia da entidade n\u00E3o submetida a parsing \"&{0};\" n\u00E3o \u00E9 permitida."
             },

             {MsgKey.ER_WF_REF_TO_EXTERNAL_ENT,
                 "A refer\u00EAncia da entidade externa \"&{0};\" n\u00E3o \u00E9 permitida em um valor do atributo."
             },

             {MsgKey.ER_NS_PREFIX_CANNOT_BE_BOUND,
                 "O prefixo \"{0}\" n\u00E3o pode ser vinculado ao namespace \"{1}\"."
             },

             {MsgKey.ER_NULL_LOCAL_ELEMENT_NAME,
                 "O nome local do elemento \"{0}\" \u00E9 nulo."
             },

             {MsgKey.ER_NULL_LOCAL_ATTR_NAME,
                 "O nome local do atributo \"{0}\" \u00E9 nulo."
             },

             { MsgKey.ER_ELEM_UNBOUND_PREFIX_IN_ENTREF,
                 "O texto de substitui\u00E7\u00E3o do n\u00F3 \"{0}\" de entidade cont\u00E9m um n\u00F3 \"{1}\" de elemento com um prefixo desvinculado \"{2}\"."
             },

             { MsgKey.ER_ATTR_UNBOUND_PREFIX_IN_ENTREF,
                 "O texto de substitui\u00E7\u00E3o do n\u00F3 \"{0}\" de entidade cont\u00E9m um n\u00F3 \"{1}\" de atributo com um prefixo desvinculado \"{2}\"."
             },

             { MsgKey.ER_WRITING_INTERNAL_SUBSET,
                 "Ocorreu um erro ao gravar o subconjunto interno."
             },

        };

        return contents;
    }
}
