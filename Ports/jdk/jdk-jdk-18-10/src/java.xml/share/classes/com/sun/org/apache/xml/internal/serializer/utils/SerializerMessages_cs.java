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
/*
 * $Id: SerializerMessages_cs.java,v 1.1.4.1 2005/09/08 11:03:12 suresh_emailid Exp $
 */

package com.sun.org.apache.xml.internal.serializer.utils;

import java.util.ListResourceBundle;

public class SerializerMessages_cs extends ListResourceBundle {
  public Object[][] getContents() {
    Object[][] contents =  new Object[][] {
        // BAD_MSGKEY needs translation
        // BAD_MSGFORMAT needs translation
      { MsgKey.ER_SERIALIZER_NOT_CONTENTHANDLER,
        "T\u0159\u00edda serializace ''{0}'' neimplementuje org.xml.sax.ContentHandler."},

      { MsgKey.ER_RESOURCE_COULD_NOT_FIND,
        "Nelze naj\u00edt zdroj [ {0} ].\n {1}"},

      { MsgKey.ER_RESOURCE_COULD_NOT_LOAD,
        "Nelze zav\u00e9st zdroj [ {0} ]: {1} \n {2} \n {3}"},

      { MsgKey.ER_BUFFER_SIZE_LESSTHAN_ZERO,
        "Velikost vyrovn\u00e1vac\u00ed pam\u011bti <=0"},

      { MsgKey.ER_INVALID_UTF16_SURROGATE,
        "Byla zji\u0161t\u011bna neplatn\u00e1 n\u00e1hrada UTF-16: {0} ?"},

      { MsgKey.ER_OIERROR,
        "Chyba vstupu/v\u00fdstupu"},

      { MsgKey.ER_ILLEGAL_ATTRIBUTE_POSITION,
        "Nelze p\u0159idat atribut {0} po uzlech potomk\u016f ani p\u0159ed t\u00edm, ne\u017e je vytvo\u0159en prvek. Atribut bude ignorov\u00e1n."},

      { MsgKey.ER_NAMESPACE_PREFIX,
        "Obor n\u00e1zv\u016f pro p\u0159edponu ''{0}'' nebyl deklarov\u00e1n."},

        // ER_STRAY_ATTRIBUTE needs translation
      { MsgKey.ER_STRAY_NAMESPACE,
        "Deklarace oboru n\u00e1zv\u016f ''{0}''=''{1}'' je vn\u011b prvku."},

      { MsgKey.ER_COULD_NOT_LOAD_RESOURCE,
        "Nelze zav\u00e9st ''{0}'' (zkontrolujte prom\u011bnnou CLASSPATH), proto se pou\u017e\u00edvaj\u00ed pouze v\u00fdchoz\u00ed hodnoty"},

        // ER_ILLEGAL_CHARACTER needs translation
      { MsgKey.ER_COULD_NOT_LOAD_METHOD_PROPERTY,
        "Nelze na\u010d\u00edst soubor vlastnost\u00ed ''{0}'' pro v\u00fdstupn\u00ed metodu ''{1}'' (zkontrolujte prom\u011bnnou CLASSPATH)."},

      { MsgKey.ER_INVALID_PORT,
        "Neplatn\u00e9 \u010d\u00edslo portu."},

      { MsgKey.ER_PORT_WHEN_HOST_NULL,
        "M\u00e1-li hostitel hodnotu null, nelze nastavit port."},

      { MsgKey.ER_HOST_ADDRESS_NOT_WELLFORMED,
        "Adresa hostitele m\u00e1 nespr\u00e1vn\u00fd form\u00e1t."},

      { MsgKey.ER_SCHEME_NOT_CONFORMANT,
        "Sch\u00e9ma nevyhovuje."},

      { MsgKey.ER_SCHEME_FROM_NULL_STRING,
        "Nelze nastavit sch\u00e9ma \u0159et\u011bzce s hodnotou null."},

      { MsgKey.ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
        "Cesta obsahuje neplatnou escape sekvenci"},

      { MsgKey.ER_PATH_INVALID_CHAR,
        "Cesta obsahuje neplatn\u00fd znak: {0}"},

      { MsgKey.ER_FRAG_INVALID_CHAR,
        "Fragment obsahuje neplatn\u00fd znak."},

      { MsgKey.ER_FRAG_WHEN_PATH_NULL,
        "M\u00e1-li cesta hodnotu null, nelze nastavit fragment."},

      { MsgKey.ER_FRAG_FOR_GENERIC_URI,
        "Fragment lze nastavit jen u generick\u00e9ho URI."},

      { MsgKey.ER_NO_SCHEME_IN_URI,
        "V URI nebylo nalezeno \u017e\u00e1dn\u00e9 sch\u00e9ma: {0}"},

      { MsgKey.ER_CANNOT_INIT_URI_EMPTY_PARMS,
        "URI nelze inicializovat s pr\u00e1zdn\u00fdmi parametry."},

      { MsgKey.ER_NO_FRAGMENT_STRING_IN_PATH,
        "Fragment nelze ur\u010dit z\u00e1rove\u0148 v cest\u011b i ve fragmentu."},

      { MsgKey.ER_NO_QUERY_STRING_IN_PATH,
        "V \u0159et\u011bzci cesty a dotazu nelze zadat \u0159et\u011bzec dotazu."},

      { MsgKey.ER_NO_PORT_IF_NO_HOST,
        "Nen\u00ed-li ur\u010den hostitel, nelze zadat port."},

      { MsgKey.ER_NO_USERINFO_IF_NO_HOST,
        "Nen\u00ed-li ur\u010den hostitel, nelze zadat \u00fadaje o u\u017eivateli."},

      { MsgKey.ER_SCHEME_REQUIRED,
        "Je vy\u017eadov\u00e1no sch\u00e9ma!"},

      /*
       * Note to translators:  The words 'Properties' and
       * 'SerializerFactory' in this message are Java class names
       * and should not be translated.
       */
      {   MsgKey.ER_FACTORY_PROPERTY_MISSING,
          "Objekt vlastnost\u00ed p\u0159edan\u00fd faktorii SerializerFactory neobsahuje vlastnost ''{0}''. " },

      {   MsgKey.ER_ENCODING_NOT_SUPPORTED,
          "Varov\u00e1n\u00ed: K\u00f3dov\u00e1n\u00ed ''{0}'' nen\u00ed v b\u011bhov\u00e9m prost\u0159ed\u00ed Java podporov\u00e1no." },

       {MsgKey.ER_FEATURE_NOT_FOUND,
       "Parametr ''{0}'' nebyl rozpozn\u00e1n."},

       {MsgKey.ER_FEATURE_NOT_SUPPORTED,
       "Parametr ''{0}'' byl rozpozn\u00e1n, ale nelze nastavit po\u017eadovanou hodnotu."},

       {MsgKey.ER_STRING_TOO_LONG,
       "V\u00fdsledn\u00fd \u0159et\u011bzec je p\u0159\u00edli\u0161 dlouh\u00fd pro \u0159et\u011bzec DOMString: ''{0}''."},

       {MsgKey.ER_TYPE_MISMATCH_ERR,
       "Typ hodnoty pro tento n\u00e1zev parametru nen\u00ed kompatibiln\u00ed s o\u010dek\u00e1van\u00fdm typem hodnoty."},

       {MsgKey.ER_NO_OUTPUT_SPECIFIED,
       "C\u00edlov\u00e9 um\u00edst\u011bn\u00ed v\u00fdstupu pro data ur\u010den\u00e1 k z\u00e1pisu je rovno hodnot\u011b Null. "},

       {MsgKey.ER_UNSUPPORTED_ENCODING,
       "Bylo nalezeno nepodporovan\u00e9 k\u00f3dov\u00e1n\u00ed."},

       {MsgKey.ER_UNABLE_TO_SERIALIZE_NODE,
       "Nelze prov\u00e9st serializaci uzlu. "},

       {MsgKey.ER_CDATA_SECTIONS_SPLIT,
       "Sekce CDATA obsahuje jednu nebo v\u00edce ukon\u010dovac\u00edch zna\u010dek ']]>'."},

       {MsgKey.ER_WARNING_WF_NOT_CHECKED,
           "Nelze vytvo\u0159it instanci modulu pro kontrolu spr\u00e1vn\u00e9ho utvo\u0159en\u00ed. Parametr spr\u00e1vn\u00e9ho utvo\u0159en\u00ed byl nastaven na hodnotu true, nepoda\u0159ilo se v\u0161ak zkontrolovat spr\u00e1vnost utvo\u0159en\u00ed. "
       },

       {MsgKey.ER_WF_INVALID_CHARACTER,
           "Uzel ''{0}'' obsahuje neplatn\u00e9 znaky XML. "
       },

       { MsgKey.ER_WF_INVALID_CHARACTER_IN_COMMENT,
           "V pozn\u00e1mce byl zji\u0161t\u011bn neplatn\u00fd znak XML (Unicode: 0x{0})."
       },

       { MsgKey.ER_WF_INVALID_CHARACTER_IN_PI,
           "V datech instrukce zpracov\u00e1n\u00ed byl nalezen neplatn\u00fd znak XML (Unicode: 0x{0})."
       },

       { MsgKey.ER_WF_INVALID_CHARACTER_IN_CDATA,
           "V odd\u00edlu CDATASection byl nalezen neplatn\u00fd znak XML (Unicode: 0x{0})."
       },

       { MsgKey.ER_WF_INVALID_CHARACTER_IN_TEXT,
           "V obsahu znakov\u00fdch dat uzlu byl nalezen neplatn\u00fd znak XML (Unicode: 0x{0})."
       },

       { MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
           "V objektu {0} s n\u00e1zvem ''{1}'' byl nalezen neplatn\u00fd znak XML. "
       },

       { MsgKey.ER_WF_DASH_IN_COMMENT,
           "V pozn\u00e1mk\u00e1ch nen\u00ed povolen \u0159et\u011bzec \"--\"."
       },

       {MsgKey.ER_WF_LT_IN_ATTVAL,
           "Hodnota atributu \"{1}\" souvisej\u00edc\u00edho s typem prvku \"{0}\" nesm\u00ed obsahovat znak ''<''."
       },

       {MsgKey.ER_WF_REF_TO_UNPARSED_ENT,
           "Odkaz na neanalyzovanou entitu \"&{0};\" nen\u00ed povolen."
       },

       {MsgKey.ER_WF_REF_TO_EXTERNAL_ENT,
           "Extern\u00ed odkaz na entitu \"&{0};\" nen\u00ed v hodnot\u011b atributu povolen."
       },

       {MsgKey.ER_NS_PREFIX_CANNOT_BE_BOUND,
           "P\u0159edpona \"{0}\" nesm\u00ed b\u00fdt v\u00e1zan\u00e1 k oboru n\u00e1zv\u016f \"{1}\"."
       },

       {MsgKey.ER_NULL_LOCAL_ELEMENT_NAME,
           "Lok\u00e1ln\u00ed n\u00e1zev prvku \"{0}\" m\u00e1 hodnotu Null. "
       },

       {MsgKey.ER_NULL_LOCAL_ATTR_NAME,
           "Lok\u00e1ln\u00ed n\u00e1zev atributu \"{0}\" m\u00e1 hodnotu Null. "
       },

       { MsgKey.ER_ELEM_UNBOUND_PREFIX_IN_ENTREF,
           "Nov\u00fd text uzlu entity \"{0}\" obsahuje uzel prvku \"{1}\" s nesv\u00e1zanou p\u0159edponou \"{2}\"."
       },

       { MsgKey.ER_ATTR_UNBOUND_PREFIX_IN_ENTREF,
           "Nov\u00fd text uzlu entity \"{0}\" obsahuje uzel atributu \"{1}\" s nesv\u00e1zanou p\u0159edponou \"{2}\". "
       },

    };
    return contents;
  }
}
