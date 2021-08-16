/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * (C) Copyright IBM Corp. 1998 - All Rights Reserved
 *
 * Portions copyright (c) 2007 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies. Please refer to the file "copyright.html"
 * for further important copyright and licensing information.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */
/*
    @bug 4123370 4091969 4118731 4182108 4778440

    The "at-test" tag was removed from this file, because there's no way to
    run this test in an automated test harness.  It depends on having various
    different locales installed on the machine, and on Windows it depends
    on the user going to the "Regional Settings" control panel and changing
    the settings before running the test for each bug.  We can run this test
    manually from time to time to ensure that there has been no regression,
    but it's not automated. -- lwerner, 7/6/98

    INSTRUCTIONS FOR RUNNING THIS TEST
    ==================================
    This test is designed to check for problems in the JVM code that initializes the
    default Java locale (the locale returned by Locale.getDefault()) from the system
    locale settings (or from command-line arguments).  Since detecting a regression
    usually requires setting the environment up in some way prior to running the test,
    this is a manual test.

    The test simply prints out the internal ID and display name of the default Java locale,
    and the name of the default Java character encoding.  It passes if these are what
    you expect them to be, and fails if they're not.

    Bug #4091969:
        To test for bug #4091969, run this test on a Korean-localized version of
        Windows, with the default locale set to Korean.  You should get "ko_KR"
        as the default locale.

    Bug #4123370:
        One part of bug #4123370 duplicates bug #4091969, which is covered by the
        instructions above.

        To test the unique part of bug #4123370, use the "Regional Settings" control
        panel to set the currect locale to each of the different Spanish-language locales.
        Run this test once for each Spanish-language locale.  You should see the appropriate
        locale ID and name for each locale.  Both "Spanish - Traditional Sort" and
        "Spanish - Modern Sort" should produce "es_ES" and "Spanish (Spain)".

    Bug #4118731:
        The basic issue here was that we had changed so that calling getDisplayName()
        on a locale that didn't include a country code no longer included a country
        name (instead of picking a default country name, as before), which is the
        right answer.  The problem is we weren't always getting back a system default
        locale from Solaris that includes a country code, even though we should.

        To test this, set the system default locale to a locale that doesn't include
        a country code, such as "fr" or "de", using (in the C shell) "setenv LC_ALL fr"
        (or whatever the locale ID you want is).  Running PrintDefaultLocale should
        still produce a locale ID, and a locale display name, that include a country
        code (and country name).  [Remember to make sure the locale is actually installed
        first.]

        To test the specific complaint in the bug, use "setenv LC_ALL ja".  Also pay
        special attention to Solaris locale IDs that don't match the corresponding java
        locales, such as "su" (which should turn into "fi_FI"), "cz" (which should turn
        into "cs_CZ"), and "en_UK" (which should turn into "en_GB").

    Bug #4079167:
        Test this bug the same way you test bug #4118731.  Set the locale to each of
        the specified locale IDs (e.g., "setenv LC_ALL japanese"), and then run
        PrintDefaultLocale.  You should get the following results:
            Solaris ID  Java ID  Java display name     Encoding
            ==========  =======  ====================  ========
            japanese    ja_JP    Japanese (Japan)        --
            korean      ko_KR    Korean (South Korea)    --
            tchinese    zh_TW    Chinese (Taiwan)        --
            big5        zh_TW    Chinese (Taiwan)      Big5
        (Where "--" is marked for "encoding," the result isn't important-- it's the
        default encoding for that locale, which we don't test.  It should be something
        plausible.  Also note that this test presupposed you actually have locales
        with these names installed on your system.)

    Bug #4154559, 4778440:
        Set the locale to Norwegian (Bokmal) and Norwegian (Nynorsk) using the
        Regional Settings control panel on Windows.  For each setting, run this program.
        You should see no_NO and no_NO_NY, respectively.

    Bug #4182108:
        Test this bug the same way you test bug #4118731.  Set the locale to
        each of the specified locale IDs (e.g., "setenv LC_ALL japanese"), and
        then run PrintDefaultLocale.  You should get the following results:

            Solaris ID          Java ID     Encoding
            ==========          =======     ========
            cz                  cs_CZ       --
            su                  fi_FI       --
            fr.ISO8859-15       fr_FR       ISO8859-15
            fr.ISO8859-15@euro  fr_FR       ISO8859-15

        Where "--" is marked for "encoding," the result isn't important-- it's
        the default encoding for that locale, which we don't test.  It should be
        something plausible.  Also note that this test presupposed you actually
        have locales with these names installed on your system.

        As of this writing, there is a bug in Solaris or in the 8859-15/euro
        patch for 2.6 (Solaris patch 106842-01) which causes nl_langinfo() to
        return the wrong value for 8859-15 locales.  As a result, the encoding
        returned by this test is currency ISO8859-1 for 8859-15 locales.

    Bug #4778440, 5005601, 5074060, 5107154:
        Run the "deflocale" tool found in "data" directory (deflocale.sh on Unix,
        deflocale.exe on Windows), and check the following:

            4778440: Check that iw_IL is the default locale if the OS's locale is
            Hebrew,  and in_ID for Indonesian.
            5005601: For Norwegian locales, no_NO is selected for Bokmal, and no_NO_NY
            is selected for Nynorsk.
            5074060, 5107154: On Windows XP ServicePack 2, check the default locales for the
            following Windows locales.  Compare with the golden data (deflocale.win):
                Bengali - India
                Croatian - Bosnia and Herzegovina
                Bosnian - Bosnia and Herzegovina
                Serbian (Latin) - Bosnia and Herzegovina
                Serbian (Cyrillic) - Bosnia and Herzegovina
                Welsh - United Kingdom
                Maori - New Zealand
                Malayalam - India
                Maltese - Malta
                Quechua - Bolivia
                Quechua - Ecuador
                Quechua - Peru
                Setswana (Tswana) - South Africa
                isiXhosa (Xhosa) - South Africa
                isiZulu ( Zulu) - South Africa
                Sesotho sa Leboa (Northern Sotho) - South Africa
                Sami, Northern - Norway
                Sami, Northern - Sweden
                Sami, Northern - Finland
                Sami, Lule - Norway
                Sami, Lule - Sweden
                Sami, Southern - Norway
                Sami, Southern - Sweden
                Sami, Skolt - Finland
                Sami, Inari - Finland

    Bug # 6409997:
        Run the "deflocale.exe" tool found in "data" directory on Windows Vista.
        It contains the following new locales:
                Tajik (Cyrillic) (Tajikistan) - 1251
                Upper Sorbian (Germany) - 1252
                Turkmen (Turkmenistan) - 1250
                Oriya (India) - 0
                Assamese (India) - 0
                Tibetan (People's Republic of China) - 0
                Khmer (Cambodia) - 0
                Lao (Lao P.D.R.) - 0
                Sinhala (Sri Lanka) - 0
                Inuktitut (Canada) - 0
                Amharic (Ethiopia) - 0
                Hausa (Latin) (Nigeria) - 1252
                Yoruba (Nigeria) - 1252
                Bashkir (Russia) - 1251
                Greenlandic (Greenland) - 1252
                Igbo (Nigeria) - 1252
                Yi (People's Republic of China) - 0
                Breton (France) - 1252
                Uighur (People's Republic of China) - 1256
                Occitan (France) - 1252
                Corsican (France) - 1252
                Alsatian (France) - 1252
                Yakut (Russia) - 1251
                K'iche (Guatemala) - 1252
                Kinyarwanda (Rwanda) - 1252
                Wolof (Senegal) - 1252
                Dari (Afghanistan) - 1256
                Lower Sorbian (Germany) - 1252
                Bengali (Bangladesh) - 0
                Mongolian (Traditional Mongolian) (People's Republic of China) - 0
                Tamazight (Latin) (Algeria) - 1252
                English (India) - 1252
                English (Malaysia) - 1252
                English (Singapore) - 1252
                Spanish (United States) - 1252

*/
import java.nio.charset.Charset;
import java.util.Locale;

public class PrintDefaultLocale {
    public static void main(String[] args) {
        System.out.printf("default locale: ID: %s, Name: %s\n",
            Locale.getDefault().toString(),
            Locale.getDefault().getDisplayName(Locale.US));
        System.out.printf("display locale: ID: %s, Name: %s\n",
            Locale.getDefault(Locale.Category.DISPLAY).toString(),
            Locale.getDefault(Locale.Category.DISPLAY).getDisplayName(Locale.US));
        System.out.printf("format locale: ID: %s, Name: %s\n",
            Locale.getDefault(Locale.Category.FORMAT).toString(),
            Locale.getDefault(Locale.Category.FORMAT).getDisplayName(Locale.US));
        System.out.printf("default charset: %s\n", Charset.defaultCharset());
    }
}
