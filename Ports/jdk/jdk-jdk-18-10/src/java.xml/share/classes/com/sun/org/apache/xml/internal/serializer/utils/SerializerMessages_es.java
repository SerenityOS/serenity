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
public class SerializerMessages_es extends ListResourceBundle {

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
                "La clave de mensaje ''{0}'' no est\u00E1 en la clase de mensaje ''{1}''" },

            {   MsgKey.BAD_MSGFORMAT,
                "Fallo de formato del mensaje ''{0}'' en la clase de mensaje ''{1}''." },

            {   MsgKey.ER_SERIALIZER_NOT_CONTENTHANDLER,
                "La clase de serializador ''{0}'' no implanta org.xml.sax.ContentHandler." },

            {   MsgKey.ER_RESOURCE_COULD_NOT_FIND,
                    "No se ha encontrado el recurso [ {0} ].\n {1}" },

            {   MsgKey.ER_RESOURCE_COULD_NOT_LOAD,
                    "No se ha podido cargar el recurso [ {0} ]: {1} \n {2} \t {3}" },

            {   MsgKey.ER_BUFFER_SIZE_LESSTHAN_ZERO,
                    "Tama\u00F1o de buffer menor o igual que 0" },

            {   MsgKey.ER_INVALID_UTF16_SURROGATE,
                    "\u00BFSe ha detectado un sustituto UTF-16 no v\u00E1lido: {0}?" },

            {   MsgKey.ER_OIERROR,
                "Error de E/S" },

            {   MsgKey.ER_ILLEGAL_ATTRIBUTE_POSITION,
                "No se puede agregar el atributo {0} despu\u00E9s de nodos secundarios o antes de que se produzca un elemento. Se ignorar\u00E1 el atributo." },

            /*
             * Note to translators:  The stylesheet contained a reference to a
             * namespace prefix that was undefined.  The value of the substitution
             * text is the name of the prefix.
             */
            {   MsgKey.ER_NAMESPACE_PREFIX,
                "No se ha declarado el espacio de nombres para el prefijo ''{0}''." },

            /*
             * Note to translators:  This message is reported if the stylesheet
             * being processed attempted to construct an XML document with an
             * attribute in a place other than on an element.  The substitution text
             * specifies the name of the attribute.
             */
            {   MsgKey.ER_STRAY_ATTRIBUTE,
                "El atributo ''{0}'' est\u00E1 fuera del elemento." },

            /*
             * Note to translators:  As with the preceding message, a namespace
             * declaration has the form of an attribute and is only permitted to
             * appear on an element.  The substitution text {0} is the namespace
             * prefix and {1} is the URI that was being used in the erroneous
             * namespace declaration.
             */
            {   MsgKey.ER_STRAY_NAMESPACE,
                "Declaraci\u00F3n del espacio de nombres ''{0}''=''{1}'' fuera del elemento." },

            {   MsgKey.ER_COULD_NOT_LOAD_RESOURCE,
                "No se ha podido cargar ''{0}'' (compruebe la CLASSPATH), ahora s\u00F3lo se est\u00E1n utilizando los valores por defecto" },

            {   MsgKey.ER_ILLEGAL_CHARACTER,
                "Intento de realizar la salida del car\u00E1cter del valor integral {0}, que no est\u00E1 representado en la codificaci\u00F3n de salida de {1}." },

            {   MsgKey.ER_COULD_NOT_LOAD_METHOD_PROPERTY,
                "No se ha podido cargar el archivo de propiedades ''{0}'' para el m\u00E9todo de salida ''{1}'' (compruebe la CLASSPATH)" },

            {   MsgKey.ER_INVALID_PORT,
                "N\u00FAmero de puerto no v\u00E1lido" },

            {   MsgKey.ER_PORT_WHEN_HOST_NULL,
                "No se puede definir el puerto si el host es nulo" },

            {   MsgKey.ER_HOST_ADDRESS_NOT_WELLFORMED,
                "El formato de la direcci\u00F3n de host no es correcto" },

            {   MsgKey.ER_SCHEME_NOT_CONFORMANT,
                "El esquema no es v\u00E1lido." },

            {   MsgKey.ER_SCHEME_FROM_NULL_STRING,
                "No se puede definir un esquema a partir de una cadena nula" },

            {   MsgKey.ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
                "La ruta de acceso contiene una secuencia de escape no v\u00E1lida" },

            {   MsgKey.ER_PATH_INVALID_CHAR,
                "La ruta de acceso contiene un car\u00E1cter no v\u00E1lido: {0}" },

            {   MsgKey.ER_FRAG_INVALID_CHAR,
                "El fragmento contiene un car\u00E1cter no v\u00E1lido" },

            {   MsgKey.ER_FRAG_WHEN_PATH_NULL,
                "No se puede definir el fragmento si la ruta de acceso es nula" },

            {   MsgKey.ER_FRAG_FOR_GENERIC_URI,
                "S\u00F3lo se puede definir el fragmento para un URI gen\u00E9rico" },

            {   MsgKey.ER_NO_SCHEME_IN_URI,
                "No se ha encontrado un esquema en el URI" },

            {   MsgKey.ER_CANNOT_INIT_URI_EMPTY_PARMS,
                "No se puede inicializar el URI con par\u00E1metros vac\u00EDos" },

            {   MsgKey.ER_NO_FRAGMENT_STRING_IN_PATH,
                "No se puede especificar el fragmento en la ruta de acceso y en el fragmento" },

            {   MsgKey.ER_NO_QUERY_STRING_IN_PATH,
                "No se puede especificar la cadena de consulta en la ruta de acceso y en la cadena de consulta" },

            {   MsgKey.ER_NO_PORT_IF_NO_HOST,
                "No se puede especificar el puerto si no se ha especificado el host" },

            {   MsgKey.ER_NO_USERINFO_IF_NO_HOST,
                "No se puede especificar la informaci\u00F3n de usuario si no se ha especificado el host" },

            {   MsgKey.ER_XML_VERSION_NOT_SUPPORTED,
                "Advertencia: es necesario que la versi\u00F3n del documento de salida sea ''{0}''. Esta versi\u00F3n de XML no est\u00E1 soportada. La versi\u00F3n del documento de salida ser\u00E1 ''1.0''." },

            {   MsgKey.ER_SCHEME_REQUIRED,
                "Se necesita un esquema." },

            /*
             * Note to translators:  The words 'Properties' and
             * 'SerializerFactory' in this message are Java class names
             * and should not be translated.
             */
            {   MsgKey.ER_FACTORY_PROPERTY_MISSING,
                "El objeto de propiedades transferido a SerializerFactory no tiene una propiedad ''{0}''." },

            {   MsgKey.ER_ENCODING_NOT_SUPPORTED,
                "Advertencia: el tiempo de ejecuci\u00F3n de Java no soporta la codificaci\u00F3n ''{0}''." },

             {MsgKey.ER_FEATURE_NOT_FOUND,
             "No se reconoce el par\u00E1metro ''{0}''."},

             {MsgKey.ER_FEATURE_NOT_SUPPORTED,
             "Se reconoce el par\u00E1metro ''{0}'' pero no se puede definir el valor solicitado."},

             {MsgKey.ER_STRING_TOO_LONG,
             "La cadena resultante es demasiado larga para que quepa en una cadena DOM: ''{0}''."},

             {MsgKey.ER_TYPE_MISMATCH_ERR,
             "El tipo de valor para este nombre de par\u00E1metro no es compatible con el tipo de valor esperado. "},

             {MsgKey.ER_NO_OUTPUT_SPECIFIED,
             "El destino de salida en el que se deb\u00EDan escribir los datos era nulo."},

             {MsgKey.ER_UNSUPPORTED_ENCODING,
             "Se ha encontrado una codificaci\u00F3n no soportada."},

             {MsgKey.ER_UNABLE_TO_SERIALIZE_NODE,
             "El nodo no se ha podido serializar."},

             {MsgKey.ER_CDATA_SECTIONS_SPLIT,
             "La secci\u00F3n CDATA contiene uno o m\u00E1s marcadores de terminaci\u00F3n ']]>'."},

             {MsgKey.ER_WARNING_WF_NOT_CHECKED,
                 "No se ha podido crear una instancia del comprobador de formato correcto. El par\u00E1metro de formato correcto se ha definido en true pero no se puede realizar la comprobaci\u00F3n de formato correcto."
             },

             {MsgKey.ER_WF_INVALID_CHARACTER,
                 "El nodo ''{0}'' contiene caracteres XML no v\u00E1lidos."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_COMMENT,
                 "Se ha encontrado un car\u00E1cter XML (Unicode: 0x{0}) no v\u00E1lido en el comentario."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_PI,
                 "Se ha encontrado un car\u00E1cter XML (Unicode: 0x{0}) no v\u00E1lido en los datos de la instrucci\u00F3n de procesamiento."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_CDATA,
                 "Se ha encontrado un car\u00E1cter XML (Unicode: 0x{0}) no v\u00E1lido en el contenido de la secci\u00F3n CDATA."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_TEXT,
                 "Se ha encontrado un car\u00E1cter XML (Unicode: 0x{0}) no v\u00E1lido en los datos de caracteres del nodo."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                 "Se ha encontrado un car\u00E1cter XML no v\u00E1lido en el nodo {0} denominado ''{1}''."
             },

             { MsgKey.ER_WF_DASH_IN_COMMENT,
                 "La cadena \"--\" no est\u00E1 permitida en los comentarios."
             },

             {MsgKey.ER_WF_LT_IN_ATTVAL,
                 "El valor del atributo \"{1}\" asociado a un tipo de elemento \"{0}\" no debe contener el car\u00E1cter ''<''."
             },

             {MsgKey.ER_WF_REF_TO_UNPARSED_ENT,
                 "La referencia de entidad no analizada \"&{0};\" no est\u00E1 permitida."
             },

             {MsgKey.ER_WF_REF_TO_EXTERNAL_ENT,
                 "La referencia de entidad externa \"&{0};\" no est\u00E1 permitida en un valor de atributo."
             },

             {MsgKey.ER_NS_PREFIX_CANNOT_BE_BOUND,
                 "El prefijo \"{0}\" no se puede enlazar al espacio de nombres \"{1}\"."
             },

             {MsgKey.ER_NULL_LOCAL_ELEMENT_NAME,
                 "El nombre local del elemento \"{0}\" es nulo."
             },

             {MsgKey.ER_NULL_LOCAL_ATTR_NAME,
                 "El nombre local del atributo \"{0}\" es nulo."
             },

             { MsgKey.ER_ELEM_UNBOUND_PREFIX_IN_ENTREF,
                 "El texto de sustituci\u00F3n del nodo de entidad \"{0}\" contiene un nodo de elemento \"{1}\"con un prefijo no enlazado \"{2}\"."
             },

             { MsgKey.ER_ATTR_UNBOUND_PREFIX_IN_ENTREF,
                 "El texto de sustituci\u00F3n del nodo de entidad \"{0}\" contiene un nodo de atributo \"{1}\"con un prefijo no enlazado \"{2}\"."
             },

             { MsgKey.ER_WRITING_INTERNAL_SUBSET,
                 "Se ha producido un error al escribir el subjuego interno."
             },

        };

        return contents;
    }
}
