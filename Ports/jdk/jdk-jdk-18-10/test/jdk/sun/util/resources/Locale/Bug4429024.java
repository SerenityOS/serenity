/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
/*
 * @test
 * @summary checking localised language/country names in finnish
 * @modules jdk.localedata
 * @bug 4429024 4964035 6558856 8008577
 * @run main/othervm -Djava.locale.providers=JRE,SPI Bug4429024
 */

import java.util.Locale;

public class Bug4429024 {

    public static void main(String[] args) throws Exception {

        int errors=0;

        String [][] fiLocales = {
                    { "ar", "arabia" },
                    { "ba", "baski" },
                    { "bg", "bulgaria" },
                    { "ca", "katalaani" },
                    { "cs", "tsekki" },
                    { "da", "tanska" },
                    { "de", "saksa" },
                    { "el", "kreikka" },
                    { "en", "englanti" },
                    { "es", "espanja" },
                    { "fi", "suomi" },
                    { "fr", "ranska" },
                    { "he", "heprea" },
                    { "hi", "hindi" },
                    { "it", "italia" },
                    { "ja", "japani" },
                    { "lt", "liettua" },
                    { "lv", "latvia" },
                    { "nl", "hollanti" },
                    { "no", "norja" },
                    { "pl", "puola" },
                    { "pt", "portugali" },
                    { "ru", "ven\u00e4j\u00e4" },
                    { "sv", "ruotsi" },
                    { "th", "thai" },
                    { "tr", "turkki" },
                    { "zh", "kiina" }
        };

        String[][] fiCountries = {
                    { "BE", "Belgia" },
                    { "BR", "Brasilia" },
                    { "CA", "Kanada" },
                    { "CH", "Sveitsi" },
                    { "CN", "Kiina" },
                    { "CZ", "Tsekin tasavalta" },
                    { "DE", "Saksa" },
                    { "DK", "Tanska" },
                    { "ES", "Espanja" },
                    { "FI", "Suomi" },
                    { "FR", "Ranska" },
                    { "GB", "Iso-Britannia" },
                    { "GR", "Kreikka" },
                    { "IE", "Irlanti" },
                    { "IT", "Italia" },
                    { "JP", "Japani" },
                    { "KR", "Korea" },
                    { "NL", "Alankomaat" },
                    { "NO", "Norja" },
                    { "PL", "Puola" },
                    { "PT", "Portugali" },
                    { "RU", "Ven\u00e4j\u00e4" },
                    { "SE", "Ruotsi" },
                    { "TR", "Turkki" },
                    { "US", "Yhdysvallat" }
        };

        for (int i=0; i < fiLocales.length; i++) {
            errors += getLanguage(fiLocales[i][0], fiLocales[i][1]);
        }

        for (int i=0; i < fiCountries.length; i++) {
            errors += getCountry(fiCountries[i][0], fiCountries[i][1]);
        }

        if(errors > 0){
            throw new RuntimeException();
        }
    };


        static int getLanguage(String inLang, String localizedName){

            Locale fiLocale = new Locale("fi", "FI");
            Locale inLocale = new Locale (inLang, "");

            if (!inLocale.getDisplayLanguage(fiLocale).equals(localizedName)){
                System.out.println("Language " + inLang +" should be \"" + localizedName  + "\", not \"" + inLocale.getDisplayLanguage(fiLocale) + "\"");
                return 1;
            }
            else{
                return 0;
            }
        }

    static int getCountry(String inCountry, String localizedName){

            Locale fiLocale = new Locale("fi", "FI");
            Locale inLocale = new Locale ("", inCountry);

            if (!inLocale.getDisplayCountry(fiLocale).equals(localizedName)){
                System.out.println("Country " + inCountry + " should be \"" + localizedName + "\", not \"" + inLocale.getDisplayCountry(fiLocale) + "\"");
                return 1;
            }
            else{
                return 0;
            }

        }
}
