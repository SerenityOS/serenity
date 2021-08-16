/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.util;

class LocaleISOData {
    /**
     * The 2- and 3-letter ISO 639 language codes.
     */
    static final String isoLanguageTable =
          "aa" + "aar"  // Afar
        + "ab" + "abk"  // Abkhazian
        + "ae" + "ave"  // Avestan
        + "af" + "afr"  // Afrikaans
        + "ak" + "aka"  // Akan
        + "am" + "amh"  // Amharic
        + "an" + "arg"  // Aragonese
        + "ar" + "ara"  // Arabic
        + "as" + "asm"  // Assamese
        + "av" + "ava"  // Avaric
        + "ay" + "aym"  // Aymara
        + "az" + "aze"  // Azerbaijani
        + "ba" + "bak"  // Bashkir
        + "be" + "bel"  // Belarusian
        + "bg" + "bul"  // Bulgarian
        + "bh" + "bih"  // Bihari
        + "bi" + "bis"  // Bislama
        + "bm" + "bam"  // Bambara
        + "bn" + "ben"  // Bengali
        + "bo" + "bod"  // Tibetan
        + "br" + "bre"  // Breton
        + "bs" + "bos"  // Bosnian
        + "ca" + "cat"  // Catalan
        + "ce" + "che"  // Chechen
        + "ch" + "cha"  // Chamorro
        + "co" + "cos"  // Corsican
        + "cr" + "cre"  // Cree
        + "cs" + "ces"  // Czech
        + "cu" + "chu"  // Church Slavic
        + "cv" + "chv"  // Chuvash
        + "cy" + "cym"  // Welsh
        + "da" + "dan"  // Danish
        + "de" + "deu"  // German
        + "dv" + "div"  // Divehi
        + "dz" + "dzo"  // Dzongkha
        + "ee" + "ewe"  // Ewe
        + "el" + "ell"  // Greek
        + "en" + "eng"  // English
        + "eo" + "epo"  // Esperanto
        + "es" + "spa"  // Spanish
        + "et" + "est"  // Estonian
        + "eu" + "eus"  // Basque
        + "fa" + "fas"  // Persian
        + "ff" + "ful"  // Fulah
        + "fi" + "fin"  // Finnish
        + "fj" + "fij"  // Fijian
        + "fo" + "fao"  // Faroese
        + "fr" + "fra"  // French
        + "fy" + "fry"  // Frisian
        + "ga" + "gle"  // Irish
        + "gd" + "gla"  // Scottish Gaelic
        + "gl" + "glg"  // Gallegan
        + "gn" + "grn"  // Guarani
        + "gu" + "guj"  // Gujarati
        + "gv" + "glv"  // Manx
        + "ha" + "hau"  // Hausa
        + "he" + "heb"  // Hebrew
        + "hi" + "hin"  // Hindi
        + "ho" + "hmo"  // Hiri Motu
        + "hr" + "hrv"  // Croatian
        + "ht" + "hat"  // Haitian
        + "hu" + "hun"  // Hungarian
        + "hy" + "hye"  // Armenian
        + "hz" + "her"  // Herero
        + "ia" + "ina"  // Interlingua
        + "id" + "ind"  // Indonesian
        + "ie" + "ile"  // Interlingue
        + "ig" + "ibo"  // Igbo
        + "ii" + "iii"  // Sichuan Yi
        + "ik" + "ipk"  // Inupiaq
        + "in" + "ind"  // Indonesian (old)
        + "io" + "ido"  // Ido
        + "is" + "isl"  // Icelandic
        + "it" + "ita"  // Italian
        + "iu" + "iku"  // Inuktitut
        + "iw" + "heb"  // Hebrew (old)
        + "ja" + "jpn"  // Japanese
        + "ji" + "yid"  // Yiddish (old)
        + "jv" + "jav"  // Javanese
        + "ka" + "kat"  // Georgian
        + "kg" + "kon"  // Kongo
        + "ki" + "kik"  // Kikuyu
        + "kj" + "kua"  // Kwanyama
        + "kk" + "kaz"  // Kazakh
        + "kl" + "kal"  // Greenlandic
        + "km" + "khm"  // Khmer
        + "kn" + "kan"  // Kannada
        + "ko" + "kor"  // Korean
        + "kr" + "kau"  // Kanuri
        + "ks" + "kas"  // Kashmiri
        + "ku" + "kur"  // Kurdish
        + "kv" + "kom"  // Komi
        + "kw" + "cor"  // Cornish
        + "ky" + "kir"  // Kirghiz
        + "la" + "lat"  // Latin
        + "lb" + "ltz"  // Luxembourgish
        + "lg" + "lug"  // Ganda
        + "li" + "lim"  // Limburgish
        + "ln" + "lin"  // Lingala
        + "lo" + "lao"  // Lao
        + "lt" + "lit"  // Lithuanian
        + "lu" + "lub"  // Luba-Katanga
        + "lv" + "lav"  // Latvian
        + "mg" + "mlg"  // Malagasy
        + "mh" + "mah"  // Marshallese
        + "mi" + "mri"  // Maori
        + "mk" + "mkd"  // Macedonian
        + "ml" + "mal"  // Malayalam
        + "mn" + "mon"  // Mongolian
        + "mo" + "mol"  // Moldavian
        + "mr" + "mar"  // Marathi
        + "ms" + "msa"  // Malay
        + "mt" + "mlt"  // Maltese
        + "my" + "mya"  // Burmese
        + "na" + "nau"  // Nauru
        + "nb" + "nob"  // Norwegian Bokm?l
        + "nd" + "nde"  // North Ndebele
        + "ne" + "nep"  // Nepali
        + "ng" + "ndo"  // Ndonga
        + "nl" + "nld"  // Dutch
        + "nn" + "nno"  // Norwegian Nynorsk
        + "no" + "nor"  // Norwegian
        + "nr" + "nbl"  // South Ndebele
        + "nv" + "nav"  // Navajo
        + "ny" + "nya"  // Nyanja
        + "oc" + "oci"  // Occitan
        + "oj" + "oji"  // Ojibwa
        + "om" + "orm"  // Oromo
        + "or" + "ori"  // Oriya
        + "os" + "oss"  // Ossetian
        + "pa" + "pan"  // Panjabi
        + "pi" + "pli"  // Pali
        + "pl" + "pol"  // Polish
        + "ps" + "pus"  // Pushto
        + "pt" + "por"  // Portuguese
        + "qu" + "que"  // Quechua
        + "rm" + "roh"  // Raeto-Romance
        + "rn" + "run"  // Rundi
        + "ro" + "ron"  // Romanian
        + "ru" + "rus"  // Russian
        + "rw" + "kin"  // Kinyarwanda
        + "sa" + "san"  // Sanskrit
        + "sc" + "srd"  // Sardinian
        + "sd" + "snd"  // Sindhi
        + "se" + "sme"  // Northern Sami
        + "sg" + "sag"  // Sango
        + "si" + "sin"  // Sinhalese
        + "sk" + "slk"  // Slovak
        + "sl" + "slv"  // Slovenian
        + "sm" + "smo"  // Samoan
        + "sn" + "sna"  // Shona
        + "so" + "som"  // Somali
        + "sq" + "sqi"  // Albanian
        + "sr" + "srp"  // Serbian
        + "ss" + "ssw"  // Swati
        + "st" + "sot"  // Southern Sotho
        + "su" + "sun"  // Sundanese
        + "sv" + "swe"  // Swedish
        + "sw" + "swa"  // Swahili
        + "ta" + "tam"  // Tamil
        + "te" + "tel"  // Telugu
        + "tg" + "tgk"  // Tajik
        + "th" + "tha"  // Thai
        + "ti" + "tir"  // Tigrinya
        + "tk" + "tuk"  // Turkmen
        + "tl" + "tgl"  // Tagalog
        + "tn" + "tsn"  // Tswana
        + "to" + "ton"  // Tonga
        + "tr" + "tur"  // Turkish
        + "ts" + "tso"  // Tsonga
        + "tt" + "tat"  // Tatar
        + "tw" + "twi"  // Twi
        + "ty" + "tah"  // Tahitian
        + "ug" + "uig"  // Uighur
        + "uk" + "ukr"  // Ukrainian
        + "ur" + "urd"  // Urdu
        + "uz" + "uzb"  // Uzbek
        + "ve" + "ven"  // Venda
        + "vi" + "vie"  // Vietnamese
        + "vo" + "vol"  // Volap?k
        + "wa" + "wln"  // Walloon
        + "wo" + "wol"  // Wolof
        + "xh" + "xho"  // Xhosa
        + "yi" + "yid"  // Yiddish
        + "yo" + "yor"  // Yoruba
        + "za" + "zha"  // Zhuang
        + "zh" + "zho"  // Chinese
        + "zu" + "zul"  // Zulu
        ;

    /**
     * The 2- and 3-letter ISO 3166 country codes.
     */
    static final String isoCountryTable =
          "AD" + "AND"  // Andorra, Principality of
        + "AE" + "ARE"  // United Arab Emirates
        + "AF" + "AFG"  // Afghanistan
        + "AG" + "ATG"  // Antigua and Barbuda
        + "AI" + "AIA"  // Anguilla
        + "AL" + "ALB"  // Albania, People's Socialist Republic of
        + "AM" + "ARM"  // Armenia
//      + "AN" + "ANT"  // Netherlands Antilles
        + "AO" + "AGO"  // Angola, Republic of
        + "AQ" + "ATA"  // Antarctica (the territory South of 60 deg S)
        + "AR" + "ARG"  // Argentina, Argentine Republic
        + "AS" + "ASM"  // American Samoa
        + "AT" + "AUT"  // Austria, Republic of
        + "AU" + "AUS"  // Australia, Commonwealth of
        + "AW" + "ABW"  // Aruba
        + "AX" + "ALA"  // \u00c5land Islands
        + "AZ" + "AZE"  // Azerbaijan, Republic of
        + "BA" + "BIH"  // Bosnia and Herzegovina
        + "BB" + "BRB"  // Barbados
        + "BD" + "BGD"  // Bangladesh, People's Republic of
        + "BE" + "BEL"  // Belgium, Kingdom of
        + "BF" + "BFA"  // Burkina Faso
        + "BG" + "BGR"  // Bulgaria, People's Republic of
        + "BH" + "BHR"  // Bahrain, Kingdom of
        + "BI" + "BDI"  // Burundi, Republic of
        + "BJ" + "BEN"  // Benin, People's Republic of
        + "BL" + "BLM"  // Saint Barth\u00e9lemy
        + "BM" + "BMU"  // Bermuda
        + "BN" + "BRN"  // Brunei Darussalam
        + "BO" + "BOL"  // Bolivia, Republic of
        + "BQ" + "BES"  // Bonaire, Sint Eustatius and Saba
        + "BR" + "BRA"  // Brazil, Federative Republic of
        + "BS" + "BHS"  // Bahamas, Commonwealth of the
        + "BT" + "BTN"  // Bhutan, Kingdom of
        + "BV" + "BVT"  // Bouvet Island (Bouvetoya)
        + "BW" + "BWA"  // Botswana, Republic of
        + "BY" + "BLR"  // Belarus
        + "BZ" + "BLZ"  // Belize
        + "CA" + "CAN"  // Canada
        + "CC" + "CCK"  // Cocos (Keeling) Islands
        + "CD" + "COD"  // Congo, Democratic Republic of
        + "CF" + "CAF"  // Central African Republic
        + "CG" + "COG"  // Congo, People's Republic of
        + "CH" + "CHE"  // Switzerland, Swiss Confederation
        + "CI" + "CIV"  // Cote D'Ivoire, Ivory Coast, Republic of the
        + "CK" + "COK"  // Cook Islands
        + "CL" + "CHL"  // Chile, Republic of
        + "CM" + "CMR"  // Cameroon, United Republic of
        + "CN" + "CHN"  // China, People's Republic of
        + "CO" + "COL"  // Colombia, Republic of
        + "CR" + "CRI"  // Costa Rica, Republic of
//      + "CS" + "SCG"  // Serbia and Montenegro
        + "CU" + "CUB"  // Cuba, Republic of
        + "CV" + "CPV"  // Cape Verde, Republic of
        + "CW" + "CUW"  // Cura\u00e7ao
        + "CX" + "CXR"  // Christmas Island
        + "CY" + "CYP"  // Cyprus, Republic of
        + "CZ" + "CZE"  // Czech Republic
        + "DE" + "DEU"  // Germany
        + "DJ" + "DJI"  // Djibouti, Republic of
        + "DK" + "DNK"  // Denmark, Kingdom of
        + "DM" + "DMA"  // Dominica, Commonwealth of
        + "DO" + "DOM"  // Dominican Republic
        + "DZ" + "DZA"  // Algeria, People's Democratic Republic of
        + "EC" + "ECU"  // Ecuador, Republic of
        + "EE" + "EST"  // Estonia
        + "EG" + "EGY"  // Egypt, Arab Republic of
        + "EH" + "ESH"  // Western Sahara
        + "ER" + "ERI"  // Eritrea
        + "ES" + "ESP"  // Spain, Spanish State
        + "ET" + "ETH"  // Ethiopia
        + "FI" + "FIN"  // Finland, Republic of
        + "FJ" + "FJI"  // Fiji, Republic of the Fiji Islands
        + "FK" + "FLK"  // Falkland Islands (Malvinas)
        + "FM" + "FSM"  // Micronesia, Federated States of
        + "FO" + "FRO"  // Faeroe Islands
        + "FR" + "FRA"  // France, French Republic
        + "GA" + "GAB"  // Gabon, Gabonese Republic
        + "GB" + "GBR"  // United Kingdom of Great Britain & N. Ireland
        + "GD" + "GRD"  // Grenada
        + "GE" + "GEO"  // Georgia
        + "GF" + "GUF"  // French Guiana
        + "GG" + "GGY"  // Guernsey
        + "GH" + "GHA"  // Ghana, Republic of
        + "GI" + "GIB"  // Gibraltar
        + "GL" + "GRL"  // Greenland
        + "GM" + "GMB"  // Gambia, Republic of the
        + "GN" + "GIN"  // Guinea, Revolutionary People's Rep'c of
        + "GP" + "GLP"  // Guadaloupe
        + "GQ" + "GNQ"  // Equatorial Guinea, Republic of
        + "GR" + "GRC"  // Greece, Hellenic Republic
        + "GS" + "SGS"  // South Georgia and the South Sandwich Islands
        + "GT" + "GTM"  // Guatemala, Republic of
        + "GU" + "GUM"  // Guam
        + "GW" + "GNB"  // Guinea-Bissau, Republic of
        + "GY" + "GUY"  // Guyana, Republic of
        + "HK" + "HKG"  // Hong Kong, Special Administrative Region of China
        + "HM" + "HMD"  // Heard and McDonald Islands
        + "HN" + "HND"  // Honduras, Republic of
        + "HR" + "HRV"  // Hrvatska (Croatia)
        + "HT" + "HTI"  // Haiti, Republic of
        + "HU" + "HUN"  // Hungary, Hungarian People's Republic
        + "ID" + "IDN"  // Indonesia, Republic of
        + "IE" + "IRL"  // Ireland
        + "IL" + "ISR"  // Israel, State of
        + "IM" + "IMN"  // Isle of Man
        + "IN" + "IND"  // India, Republic of
        + "IO" + "IOT"  // British Indian Ocean Territory (Chagos Archipelago)
        + "IQ" + "IRQ"  // Iraq, Republic of
        + "IR" + "IRN"  // Iran, Islamic Republic of
        + "IS" + "ISL"  // Iceland, Republic of
        + "IT" + "ITA"  // Italy, Italian Republic
        + "JE" + "JEY"  // Jersey
        + "JM" + "JAM"  // Jamaica
        + "JO" + "JOR"  // Jordan, Hashemite Kingdom of
        + "JP" + "JPN"  // Japan
        + "KE" + "KEN"  // Kenya, Republic of
        + "KG" + "KGZ"  // Kyrgyz Republic
        + "KH" + "KHM"  // Cambodia, Kingdom of
        + "KI" + "KIR"  // Kiribati, Republic of
        + "KM" + "COM"  // Comoros, Union of the
        + "KN" + "KNA"  // St. Kitts and Nevis
        + "KP" + "PRK"  // Korea, Democratic People's Republic of
        + "KR" + "KOR"  // Korea, Republic of
        + "KW" + "KWT"  // Kuwait, State of
        + "KY" + "CYM"  // Cayman Islands
        + "KZ" + "KAZ"  // Kazakhstan, Republic of
        + "LA" + "LAO"  // Lao People's Democratic Republic
        + "LB" + "LBN"  // Lebanon, Lebanese Republic
        + "LC" + "LCA"  // St. Lucia
        + "LI" + "LIE"  // Liechtenstein, Principality of
        + "LK" + "LKA"  // Sri Lanka, Democratic Socialist Republic of
        + "LR" + "LBR"  // Liberia, Republic of
        + "LS" + "LSO"  // Lesotho, Kingdom of
        + "LT" + "LTU"  // Lithuania
        + "LU" + "LUX"  // Luxembourg, Grand Duchy of
        + "LV" + "LVA"  // Latvia
        + "LY" + "LBY"  // Libyan Arab Jamahiriya
        + "MA" + "MAR"  // Morocco, Kingdom of
        + "MC" + "MCO"  // Monaco, Principality of
        + "MD" + "MDA"  // Moldova, Republic of
        + "ME" + "MNE"  // Montenegro, Republic of
        + "MF" + "MAF"  // Saint Martin
        + "MG" + "MDG"  // Madagascar, Republic of
        + "MH" + "MHL"  // Marshall Islands
        + "MK" + "MKD"  // Macedonia, the former Yugoslav Republic of
        + "ML" + "MLI"  // Mali, Republic of
        + "MM" + "MMR"  // Myanmar
        + "MN" + "MNG"  // Mongolia, Mongolian People's Republic
        + "MO" + "MAC"  // Macao, Special Administrative Region of China
        + "MP" + "MNP"  // Northern Mariana Islands
        + "MQ" + "MTQ"  // Martinique
        + "MR" + "MRT"  // Mauritania, Islamic Republic of
        + "MS" + "MSR"  // Montserrat
        + "MT" + "MLT"  // Malta, Republic of
        + "MU" + "MUS"  // Mauritius
        + "MV" + "MDV"  // Maldives, Republic of
        + "MW" + "MWI"  // Malawi, Republic of
        + "MX" + "MEX"  // Mexico, United Mexican States
        + "MY" + "MYS"  // Malaysia
        + "MZ" + "MOZ"  // Mozambique, People's Republic of
        + "NA" + "NAM"  // Namibia
        + "NC" + "NCL"  // New Caledonia
        + "NE" + "NER"  // Niger, Republic of the
        + "NF" + "NFK"  // Norfolk Island
        + "NG" + "NGA"  // Nigeria, Federal Republic of
        + "NI" + "NIC"  // Nicaragua, Republic of
        + "NL" + "NLD"  // Netherlands, Kingdom of the
        + "NO" + "NOR"  // Norway, Kingdom of
        + "NP" + "NPL"  // Nepal, Kingdom of
        + "NR" + "NRU"  // Nauru, Republic of
        + "NU" + "NIU"  // Niue, Republic of
        + "NZ" + "NZL"  // New Zealand
        + "OM" + "OMN"  // Oman, Sultanate of
        + "PA" + "PAN"  // Panama, Republic of
        + "PE" + "PER"  // Peru, Republic of
        + "PF" + "PYF"  // French Polynesia
        + "PG" + "PNG"  // Papua New Guinea
        + "PH" + "PHL"  // Philippines, Republic of the
        + "PK" + "PAK"  // Pakistan, Islamic Republic of
        + "PL" + "POL"  // Poland, Republic of Poland
        + "PM" + "SPM"  // St. Pierre and Miquelon
        + "PN" + "PCN"  // Pitcairn Island
        + "PR" + "PRI"  // Puerto Rico
        + "PS" + "PSE"  // Palestinian Territory, Occupied
        + "PT" + "PRT"  // Portugal, Portuguese Republic
        + "PW" + "PLW"  // Palau
        + "PY" + "PRY"  // Paraguay, Republic of
        + "QA" + "QAT"  // Qatar, State of
        + "RE" + "REU"  // Reunion
        + "RO" + "ROU"  // Romania, Socialist Republic of
        + "RS" + "SRB"  // Serbia, Republic of
        + "RU" + "RUS"  // Russian Federation
        + "RW" + "RWA"  // Rwanda, Rwandese Republic
        + "SA" + "SAU"  // Saudi Arabia, Kingdom of
        + "SB" + "SLB"  // Solomon Islands
        + "SC" + "SYC"  // Seychelles, Republic of
        + "SD" + "SDN"  // Sudan, Democratic Republic of the
        + "SE" + "SWE"  // Sweden, Kingdom of
        + "SG" + "SGP"  // Singapore, Republic of
        + "SH" + "SHN"  // St. Helena
        + "SI" + "SVN"  // Slovenia
        + "SJ" + "SJM"  // Svalbard & Jan Mayen Islands
        + "SK" + "SVK"  // Slovakia (Slovak Republic)
        + "SL" + "SLE"  // Sierra Leone, Republic of
        + "SM" + "SMR"  // San Marino, Republic of
        + "SN" + "SEN"  // Senegal, Republic of
        + "SO" + "SOM"  // Somalia, Somali Republic
        + "SR" + "SUR"  // Suriname, Republic of
        + "SS" + "SSD"  // South Sudan
        + "ST" + "STP"  // Sao Tome and Principe, Democratic Republic of
        + "SV" + "SLV"  // El Salvador, Republic of
        + "SX" + "SXM"  // Sint Maarten (Dutch part)
        + "SY" + "SYR"  // Syrian Arab Republic
        + "SZ" + "SWZ"  // Swaziland, Kingdom of
        + "TC" + "TCA"  // Turks and Caicos Islands
        + "TD" + "TCD"  // Chad, Republic of
        + "TF" + "ATF"  // French Southern Territories
        + "TG" + "TGO"  // Togo, Togolese Republic
        + "TH" + "THA"  // Thailand, Kingdom of
        + "TJ" + "TJK"  // Tajikistan
        + "TK" + "TKL"  // Tokelau (Tokelau Islands)
        + "TL" + "TLS"  // Timor-Leste, Democratic Republic of
        + "TM" + "TKM"  // Turkmenistan
        + "TN" + "TUN"  // Tunisia, Republic of
        + "TO" + "TON"  // Tonga, Kingdom of
        + "TR" + "TUR"  // Turkey, Republic of
        + "TT" + "TTO"  // Trinidad and Tobago, Republic of
        + "TV" + "TUV"  // Tuvalu
        + "TW" + "TWN"  // Taiwan, Province of China
        + "TZ" + "TZA"  // Tanzania, United Republic of
        + "UA" + "UKR"  // Ukraine
        + "UG" + "UGA"  // Uganda, Republic of
        + "UM" + "UMI"  // United States Minor Outlying Islands
        + "US" + "USA"  // United States of America
        + "UY" + "URY"  // Uruguay, Eastern Republic of
        + "UZ" + "UZB"  // Uzbekistan
        + "VA" + "VAT"  // Holy See (Vatican City State)
        + "VC" + "VCT"  // St. Vincent and the Grenadines
        + "VE" + "VEN"  // Venezuela, Bolivarian Republic of
        + "VG" + "VGB"  // British Virgin Islands
        + "VI" + "VIR"  // US Virgin Islands
        + "VN" + "VNM"  // Viet Nam, Socialist Republic of
        + "VU" + "VUT"  // Vanuatu
        + "WF" + "WLF"  // Wallis and Futuna Islands
        + "WS" + "WSM"  // Samoa, Independent State of
        + "YE" + "YEM"  // Yemen
        + "YT" + "MYT"  // Mayotte
        + "ZA" + "ZAF"  // South Africa, Republic of
        + "ZM" + "ZMB"  // Zambia, Republic of
        + "ZW" + "ZWE"  // Zimbabwe
        ;

    /**
     * Array to hold country codes for ISO3166-3.
     */
    static final String[] ISO3166_3 = {
        "AIDJ", "ANHH", "BQAQ", "BUMM", "BYAA", "CSHH", "CSXX", "CTKI", "DDDE",
        "DYBJ", "FQHH", "FXFR", "GEHH", "HVBF", "JTUM", "MIUM", "NHVU", "NQAQ",
        "NTHH", "PCHH", "PUUM", "PZPA", "RHZW", "SKIN", "SUHH", "TPTL", "VDVN",
        "WKUM", "YDYE", "YUCS", "ZRCD"
    };

    /**
     * This method computes a set of ISO3166-1 alpha-3 country codes from
     * existing isoCountryTable.
     */
    static Set<String> computeISO3166_1Alpha3Countries() {
        int tableLength = isoCountryTable.length();
        String[] isoTable = new String[tableLength / 5];
        for (int i = 0, index = 0; index < tableLength; i++, index += 5) {
            isoTable[i] = isoCountryTable.substring(index + 2, index + 5);
        }
        return Set.of(isoTable);
    }

    private LocaleISOData() {
    }
}
