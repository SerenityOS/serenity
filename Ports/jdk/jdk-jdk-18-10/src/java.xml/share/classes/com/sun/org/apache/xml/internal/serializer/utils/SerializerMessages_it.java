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
public class SerializerMessages_it extends ListResourceBundle {

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
                "La chiave di messaggio ''{0}'' non si trova nella classe messaggio ''{1}''" },

            {   MsgKey.BAD_MSGFORMAT,
                "Formato di messaggio ''{0}'' in classe messaggio ''{1}'' non riuscito." },

            {   MsgKey.ER_SERIALIZER_NOT_CONTENTHANDLER,
                "La classe serializzatore ''{0}'' non implementa org.xml.sax.ContentHandler." },

            {   MsgKey.ER_RESOURCE_COULD_NOT_FIND,
                    "Risorsa [ {0} ] non trovata.\n {1}" },

            {   MsgKey.ER_RESOURCE_COULD_NOT_LOAD,
                    "Impossibile caricare la risorsa [ {0} ]: {1} \n {2} \t {3}" },

            {   MsgKey.ER_BUFFER_SIZE_LESSTHAN_ZERO,
                    "Dimensione buffer <=0" },

            {   MsgKey.ER_INVALID_UTF16_SURROGATE,
                    "Rilevato surrogato UTF-16 non valido: {0}?" },

            {   MsgKey.ER_OIERROR,
                "Errore di I/O" },

            {   MsgKey.ER_ILLEGAL_ATTRIBUTE_POSITION,
                "Impossibile aggiungere l''attributo {0} dopo i nodi figlio o prima che sia prodotto un elemento. L''attributo verr\u00E0 ignorato." },

            /*
             * Note to translators:  The stylesheet contained a reference to a
             * namespace prefix that was undefined.  The value of the substitution
             * text is the name of the prefix.
             */
            {   MsgKey.ER_NAMESPACE_PREFIX,
                "Lo spazio di nomi per il prefisso ''{0}'' non \u00E8 stato dichiarato." },

            /*
             * Note to translators:  This message is reported if the stylesheet
             * being processed attempted to construct an XML document with an
             * attribute in a place other than on an element.  The substitution text
             * specifies the name of the attribute.
             */
            {   MsgKey.ER_STRAY_ATTRIBUTE,
                "Attributo ''{0}'' al di fuori dell''elemento." },

            /*
             * Note to translators:  As with the preceding message, a namespace
             * declaration has the form of an attribute and is only permitted to
             * appear on an element.  The substitution text {0} is the namespace
             * prefix and {1} is the URI that was being used in the erroneous
             * namespace declaration.
             */
            {   MsgKey.ER_STRAY_NAMESPACE,
                "Dichiarazione dello spazio di nomi ''{0}''=''{1}'' al di fuori dell''elemento." },

            {   MsgKey.ER_COULD_NOT_LOAD_RESOURCE,
                "Impossibile caricare ''{0}'' (verificare CLASSPATH); verranno utilizzati i valori predefiniti" },

            {   MsgKey.ER_ILLEGAL_CHARACTER,
                "Tentativo di eseguire l''output di un carattere di valore integrale {0} non rappresentato nella codifica di output {1} specificata." },

            {   MsgKey.ER_COULD_NOT_LOAD_METHOD_PROPERTY,
                "Impossibile caricare il file delle propriet\u00E0 ''{0}'' per il metodo di emissione ''{1}'' (verificare CLASSPATH)" },

            {   MsgKey.ER_INVALID_PORT,
                "Numero di porta non valido" },

            {   MsgKey.ER_PORT_WHEN_HOST_NULL,
                "La porta non pu\u00F2 essere impostata se l'host \u00E8 nullo" },

            {   MsgKey.ER_HOST_ADDRESS_NOT_WELLFORMED,
                "Host non \u00E8 un indirizzo corretto" },

            {   MsgKey.ER_SCHEME_NOT_CONFORMANT,
                "Lo schema non \u00E8 conforme." },

            {   MsgKey.ER_SCHEME_FROM_NULL_STRING,
                "Impossibile impostare lo schema da una stringa nulla" },

            {   MsgKey.ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
                "Il percorso contiene sequenza di escape non valida" },

            {   MsgKey.ER_PATH_INVALID_CHAR,
                "Il percorso contiene un carattere non valido: {0}" },

            {   MsgKey.ER_FRAG_INVALID_CHAR,
                "Il frammento contiene un carattere non valido" },

            {   MsgKey.ER_FRAG_WHEN_PATH_NULL,
                "Il frammento non pu\u00F2 essere impostato se il percorso \u00E8 nullo" },

            {   MsgKey.ER_FRAG_FOR_GENERIC_URI,
                "Il frammento pu\u00F2 essere impostato solo per un URI generico" },

            {   MsgKey.ER_NO_SCHEME_IN_URI,
                "Nessuno schema trovato nell'URI" },

            {   MsgKey.ER_CANNOT_INIT_URI_EMPTY_PARMS,
                "Impossibile inizializzare l'URI con i parametri vuoti" },

            {   MsgKey.ER_NO_FRAGMENT_STRING_IN_PATH,
                "Il frammento non pu\u00F2 essere specificato sia nel percorso che nel frammento" },

            {   MsgKey.ER_NO_QUERY_STRING_IN_PATH,
                "La stringa di query non pu\u00F2 essere specificata nella stringa di percorso e query." },

            {   MsgKey.ER_NO_PORT_IF_NO_HOST,
                "La porta non pu\u00F2 essere specificata se l'host non \u00E8 specificato" },

            {   MsgKey.ER_NO_USERINFO_IF_NO_HOST,
                "Userinfo non pu\u00F2 essere specificato se l'host non \u00E8 specificato" },

            {   MsgKey.ER_XML_VERSION_NOT_SUPPORTED,
                "Avvertenza: la versione del documento di output deve essere ''{0}''. Questa versione di XML non \u00E8 supportata. La versione del documento di output sar\u00E0 ''1.0''." },

            {   MsgKey.ER_SCHEME_REQUIRED,
                "Lo schema \u00E8 obbligatorio." },

            /*
             * Note to translators:  The words 'Properties' and
             * 'SerializerFactory' in this message are Java class names
             * and should not be translated.
             */
            {   MsgKey.ER_FACTORY_PROPERTY_MISSING,
                "L''oggetto Properties passato a SerializerFactory non dispone di una propriet\u00E0 ''{0}''." },

            {   MsgKey.ER_ENCODING_NOT_SUPPORTED,
                "Avvertenza: la codifica ''{0}'' non \u00E8 supportata da Java Runtime." },

             {MsgKey.ER_FEATURE_NOT_FOUND,
             "Il parametro {0} non \u00E8 riconosciuto."},

             {MsgKey.ER_FEATURE_NOT_SUPPORTED,
             "Il parametro ''{0}'' \u00E8 stato riconosciuto, ma non \u00E8 possibile impostare il valore richiesto."},

             {MsgKey.ER_STRING_TOO_LONG,
             "La stringa risultante \u00E8 troppo lunga per adattarsi in DOMString: ''{0}''."},

             {MsgKey.ER_TYPE_MISMATCH_ERR,
             "Il tipo di valore per questo nome parametro non \u00E8 compatibile con il tipo di valore previsto. "},

             {MsgKey.ER_NO_OUTPUT_SPECIFIED,
             "La destinazione di output per i dati da scrivere \u00E8 nulla."},

             {MsgKey.ER_UNSUPPORTED_ENCODING,
             "\u00C8 stata rilevata una codifica non supportata."},

             {MsgKey.ER_UNABLE_TO_SERIALIZE_NODE,
             "Impossibile serializzare il nodo."},

             {MsgKey.ER_CDATA_SECTIONS_SPLIT,
             "La sezione CDATA contiene uno o pi\u00F9 indicatori di fine ']]>'."},

             {MsgKey.ER_WARNING_WF_NOT_CHECKED,
                 "Impossibile creare un'istanza dello strumento di controllo della correttezza del formato. Il parametro con formato valido \u00E8 impostato su true, ma non \u00E8 possibile eseguire il controllo della correttezza del formato."
             },

             {MsgKey.ER_WF_INVALID_CHARACTER,
                 "Il nodo ''{0}'' contiene caratteri XML non validi."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_COMMENT,
                 "\u00C8 stato trovato un carattere XML non valido (Unicode: 0x{0}) nel commento."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_PI,
                 "\u00C8 stato trovato un carattere XML non valido (Unicode: 0x{0}) nei dati dell''istruzione di elaborazione."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_CDATA,
                 "\u00C8 stato trovato un carattere XML non valido (Unicode: 0x{0}) nei contenuti della sezione CDATA."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_TEXT,
                 "\u00C8 stato trovato un carattere XML non valido (Unicode: 0x{0}) nel contenuto dei dati carattere del nodo."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                 "\u00C8 stato trovato un carattere o caratteri XML non validi nel nodo {0} denominato ''{1}''."
             },

             { MsgKey.ER_WF_DASH_IN_COMMENT,
                 "La stringa \"--\" non \u00E8 consentita nei commenti."
             },

             {MsgKey.ER_WF_LT_IN_ATTVAL,
                 "Il valore dell''attributo \"{1}\" associato a un tipo di elemento \"{0}\" non deve essere contenere il carattere ''<''."
             },

             {MsgKey.ER_WF_REF_TO_UNPARSED_ENT,
                 "Il riferimento di entit\u00E0 non analizzata \"&{0};\" non \u00E8 consentito."
             },

             {MsgKey.ER_WF_REF_TO_EXTERNAL_ENT,
                 "Il riferimento di entit\u00E0 esterna \"&{0};\" non \u00E8 consentito in un valore di attributo."
             },

             {MsgKey.ER_NS_PREFIX_CANNOT_BE_BOUND,
                 "Impossibile associare il prefisso \"{0}\" allo spazio di nomi \"{1}\"."
             },

             {MsgKey.ER_NULL_LOCAL_ELEMENT_NAME,
                 "Il nome locale dell''elemento \"{0}\" \u00E8 nullo."
             },

             {MsgKey.ER_NULL_LOCAL_ATTR_NAME,
                 "Il nome locale dell''attributo \"{0}\" \u00E8 nullo."
             },

             { MsgKey.ER_ELEM_UNBOUND_PREFIX_IN_ENTREF,
                 "Il testo di sostituzione del nodo entit\u00E0 \"{0}\" contiene un nodo elemento \"{1}\" con un prefisso non associato \"{2}\"."
             },

             { MsgKey.ER_ATTR_UNBOUND_PREFIX_IN_ENTREF,
                 "Il testo di sostituzione del nodo entit\u00E0 \"{0}\" contiene un nodo attributo \"{1}\" con un prefisso non associato \"{2}\"."
             },

             { MsgKey.ER_WRITING_INTERNAL_SUBSET,
                 "Si \u00E8 verificato un errore durante la scrittura del subset interno."
             },

        };

        return contents;
    }
}
