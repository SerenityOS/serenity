/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import jdk.tools.jlink.plugin.Plugin;
import jdk.tools.jlink.plugin.PluginException;
import jdk.tools.jlink.internal.PluginRepository;
import jdk.tools.jlink.internal.TaskHelper;
import jdk.tools.jlink.internal.plugins.PluginsResourceBundle;
import tests.Helper;
import tests.JImageGenerator;
import tests.JImageValidator;
import tests.Result;

/*
 * @test
 * @bug 8152143 8152704 8155649 8165804 8185841 8176841 8190918
 *      8179071 8202537 8221432 8222098 8251317 8258794
 * @summary IncludeLocalesPlugin tests
 * @author Naoto Sato
 * @requires (vm.compMode != "Xcomp" & os.maxMemory >= 2g)
 * @library ../../lib
 * @modules java.base/jdk.internal.jimage
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.internal.plugins
 *          jdk.jlink/jdk.tools.jlink.plugin
 *          jdk.jlink/jdk.tools.jmod
 *          jdk.jlink/jdk.tools.jimage
 *          jdk.compiler
 * @build tests.*
 * @build tools.jlink.plugins.GetAvailableLocales
 * @run main/othervm/timeout=180 -Xmx1g IncludeLocalesPluginTest
 */
public class IncludeLocalesPluginTest {

    private final static String moduleName = "IncludeLocalesTest";
    private static Helper helper;
    private final static int INCLUDE_LOCALES_OPTION = 0;
    private final static int ADDMODS_OPTION         = 1;
    private final static int EXPECTED_LOCATIONS     = 2;
    private final static int UNEXPECTED_PATHS       = 3;
    private final static int AVAILABLE_LOCALES      = 4;
    private final static int ERROR_MESSAGE          = 5;

    private static int errors;

    private final static Object[][] testData = {
        // Test data should include:
        //  - --include-locales command line option
        //  - --add-modules command line option values
        //  - List of required resources in the result image
        //  - List of resources that should not exist in the result image
        //  - List of available locales in the result image
        //  - Error message

        // without --include-locales option: should include all locales
        {
            "",
            "jdk.localedata",
            List.of(
                "/jdk.localedata/sun/text/resources/ext/FormatData_en_GB.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_th.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_zh.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_001.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_th.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_zh.class"),
            List.of(),
            Arrays.stream(Locale.getAvailableLocales())
                  // "(root)" for Locale.ROOT rather than ""
                  .map(loc -> loc.equals(Locale.ROOT) ? "(root)" : loc.toString())
                  .collect(Collectors.toList()),
            "",
        },

        // Asterisk works exactly the same as above
        {
            "--include-locales=*",
            "jdk.localedata",
            List.of(
                "/jdk.localedata/sun/text/resources/ext/FormatData_en_GB.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_th.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_zh.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_001.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_th.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_zh.class"),
            List.of(),
            Arrays.stream(Locale.getAvailableLocales())
                  // "(root)" for Locale.ROOT rather than ""
                  .map(loc -> loc.equals(Locale.ROOT) ? "(root)" : loc.toString())
                  .collect(Collectors.toList()),
            "",
        },

        // World English/Spanish in Latin America
        {
            "--include-locales=en-001,es-419",
            "jdk.localedata",
            List.of(
                "/jdk.localedata/sun/text/resources/ext/FormatData_en_AU.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_es.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_es_AR.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_001.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_150.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_AT.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_es.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_es_419.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_es_AR.class"),
            List.of(
                "/jdk.localedata/sun/text/resources/ext/LineBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/thai_dict",
                "/jdk.localedata/sun/text/resources/ext/WordBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/BreakIteratorInfo_th.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_th.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_th.class"),
            List.of(
                "(root)", "en", "en_US", "en_US_#Latn", "en_US_POSIX", "en_001", "en_150", "en_AG", "en_AI",
                "en_AT", "en_AU", "en_BB", "en_BE", "en_BM", "en_BS", "en_BW", "en_BZ",
                "en_CA", "en_CC", "en_CH", "en_CK", "en_CM", "en_CX", "en_CY", "en_DE",
                "en_DG", "en_DK", "en_DM", "en_ER", "en_FI", "en_FJ", "en_FK", "en_FM",
                "en_GB", "en_GD", "en_GG", "en_GH", "en_GI", "en_GM", "en_GY", "en_HK",
                "en_IE", "en_IL", "en_IM", "en_IN", "en_IO", "en_JE", "en_JM", "en_KE",
                "en_KI", "en_KN", "en_KY", "en_LC", "en_LR", "en_LS", "en_MG", "en_MO",
                "en_MS", "en_MT", "en_MU", "en_MW", "en_MY", "en_NA", "en_NF", "en_NG",
                "en_NL", "en_NR", "en_NU", "en_NZ", "en_PG", "en_PH", "en_PK", "en_PN",
                "en_PW", "en_RW", "en_SB", "en_SC", "en_SD", "en_SE", "en_SG", "en_SH",
                "en_SI", "en_SL", "en_SS", "en_SX", "en_SZ", "en_TC", "en_TK", "en_TO",
                "en_TT", "en_TV", "en_TZ", "en_UG", "en_VC", "en_VG", "en_VU", "en_WS",
                "en_ZA", "en_ZM", "en_ZW", "es", "es_419", "es_AR", "es_BO", "es_BR", "es_BZ",
                "es_CL", "es_CO", "es_CR", "es_CU", "es_DO", "es_EC", "es_GT", "es_HN",
                "es_MX", "es_NI", "es_PA", "es_PE", "es_PR", "es_PY", "es_SV", "es_US",
                "es_UY", "es_VE"),
            "",
        },

        // All English and Japanese locales
        {
            "--include-locales=en,ja",
            "jdk.localedata",
            List.of(
                "/jdk.localedata/sun/text/resources/ext/FormatData_en_GB.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_001.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_ja.class"),
            List.of(
                "/jdk.localedata/sun/text/resources/ext/LineBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/thai_dict",
                "/jdk.localedata/sun/text/resources/ext/WordBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/BreakIteratorInfo_th.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_th.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_zh.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_th.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_zh.class"),
            List.of(
                "(root)", "en", "en_001", "en_150", "en_AE", "en_AG", "en_AI", "en_AS", "en_AT",
                "en_AU", "en_BB", "en_BE", "en_BI", "en_BM", "en_BS", "en_BW", "en_BZ",
                "en_CA", "en_CC", "en_CH", "en_CK", "en_CM", "en_CX", "en_CY", "en_DE",
                "en_DG", "en_DK", "en_DM", "en_ER", "en_FI", "en_FJ", "en_FK", "en_FM",
                "en_GB", "en_GD", "en_GG", "en_GH", "en_GI", "en_GM", "en_GU", "en_GY",
                "en_HK", "en_IE", "en_IL", "en_IM", "en_IN", "en_IO", "en_JE", "en_JM",
                "en_KE", "en_KI", "en_KN", "en_KY", "en_LC", "en_LR", "en_LS", "en_MG",
                "en_MH", "en_MO", "en_MP", "en_MS", "en_MT", "en_MU", "en_MW", "en_MY",
                "en_NA", "en_NF", "en_NG", "en_NL", "en_NR", "en_NU", "en_NZ", "en_PG",
                "en_PH", "en_PK", "en_PN", "en_PR", "en_PW", "en_RW", "en_SB", "en_SC",
                "en_SD", "en_SE", "en_SG", "en_SH", "en_SI", "en_SL", "en_SS", "en_SX",
                "en_SZ", "en_TC", "en_TK", "en_TO", "en_TT", "en_TV", "en_TZ", "en_UG",
                "en_UM", "en_US", "en_US_#Latn", "en_US_POSIX", "en_VC", "en_VG", "en_VI", "en_VU",
                "en_WS", "en_ZA", "en_ZM", "en_ZW", "ja", "ja_JP", "ja_JP_#Jpan",
                "ja_JP_JP_#u-ca-japanese"),
            "",
        },

        // All locales in Austria
        {
            "--include-locales=*-AT",
            "jdk.localedata",
            List.of(
                "/jdk.localedata/sun/text/resources/ext/FormatData_de.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_de_AT.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_de.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_de_AT.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_001.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_150.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_AT.class"),
            List.of(
                "/jdk.localedata/sun/text/resources/ext/LineBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/thai_dict",
                "/jdk.localedata/sun/text/resources/ext/WordBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/BreakIteratorInfo_th.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_en_GB.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_th.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_th.class"),
            List.of(
                "(root)", "en", "en_001", "en_150", "en_AT", "en_US", "en_US_#Latn", "en_US_POSIX",
                "de", "de_AT"),
            "",
        },

        // All locales in India
        {
            "--include-locales=*-IN",
            "jdk.localedata",
            List.of(
                "/jdk.localedata/sun/text/resources/ext/FormatData_en_IN.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_hi_IN.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_001.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_IN.class"),
            List.of(
                "/jdk.localedata/sun/text/resources/ext/LineBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/thai_dict",
                "/jdk.localedata/sun/text/resources/ext/WordBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/BreakIteratorInfo_th.class",
                "/jdk.localedata/sun/text/resources/ext/BreakIteratorResources_th.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_en_GB.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_th.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_zh.class",
                "/jdk.localedata/sun/util/resources/cldr/ext/CalendarData_as_IN.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_th.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_zh.class"),
            List.of(
                "(root)", "as", "as_IN", "as_IN_#Beng", "bn", "bn_IN", "bo", "bo_IN", "brx", "brx_IN",
                "brx_IN_#Deva", "ccp", "ccp_IN", "doi", "doi_IN", "doi_IN_#Deva", "en", "en_001", "en_IN",
                "en_US", "en_US_#Latn", "en_US_POSIX", "gu", "gu_IN", "gu_IN_#Gujr", "hi", "hi_IN",
                "hi_IN_#Deva", "kn", "kn_IN", "kn_IN_#Knda", "kok", "kok_IN", "kok_IN_#Deva", "ks_IN_#Arab",
                "ks_IN", "ks__#Arab", "ks", "mai", "mai_IN", "mai_IN_#Deva", "mni", "mni__#Beng",
                "mni_IN", "mni_IN_#Beng", "ml", "ml_IN", "ml_IN_#Mlym", "mr", "mr_IN", "mr_IN_#Deva", "ne",
                "ne_IN", "or", "or_IN", "or_IN_#Orya", "pa", "pa__#Guru", "pa_IN", "pa_IN_#Guru", "sa",
                "sa_IN", "sa_IN_#Deva", "sat", "sat__#Olck", "sat_IN", "sat_IN_#Olck", "sd", "sd__#Deva",
                "sd_IN", "sd_IN_#Deva", "ta", "ta_IN", "ta_IN_#Taml", "te", "te_IN", "te_IN_#Telu", "ur_IN", "ur"),
            "",
        },

        // Thai
        {
            "--include-locales=th",
            "jdk.localedata",
            List.of(
                "/jdk.localedata/sun/text/resources/ext/LineBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/thai_dict",
                "/jdk.localedata/sun/text/resources/ext/WordBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/BreakIteratorInfo_th.class",
                "/jdk.localedata/sun/text/resources/ext/BreakIteratorResources_th.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_th.class"),
            List.of(
                "/jdk.localedata/sun/text/resources/ext/FormatData_en_GB.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_zh.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_001.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_zh.class"),
            List.of(
                "(root)", "en", "en_US", "en_US_#Latn", "en_US_POSIX", "th", "th_TH",
                "th_TH_#Thai", "th_TH_TH_#u-nu-thai"),
            "",
        },

        // Hong Kong
        {
            "--include-locales=zh-HK",
            "jdk.localedata",
            List.of(
                "/jdk.localedata/sun/text/resources/ext/FormatData_zh.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_zh_HK.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_zh_TW.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_zh.class"),
            List.of(
                "/jdk.localedata/sun/text/resources/ext/LineBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/thai_dict",
                "/jdk.localedata/sun/text/resources/ext/WordBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/BreakIteratorInfo_th.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_en_GB.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_th.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_zh_CN.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_001.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_th.class"),
            List.of(
                "(root)", "en", "en_US", "en_US_#Latn", "en_US_POSIX", "zh", "zh__#Hans", "zh__#Hant",
                "zh_HK", "zh_HK_#Hans", "zh_HK_#Hant"),
            "",
        },

        // Simplified Chinese
        {
            "--include-locales=zh-Hans",
            "jdk.localedata",
            List.of(
                "/jdk.localedata/sun/text/resources/ext/FormatData_zh.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_zh_CN.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_zh_SG.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_zh.class"),
            List.of(
                "/jdk.localedata/sun/text/resources/ext/LineBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/thai_dict",
                "/jdk.localedata/sun/text/resources/ext/WordBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/BreakIteratorInfo_th.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_en_GB.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_th.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_001.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_th.class"),
            List.of(
                "(root)", "en", "en_US", "en_US_#Latn", "en_US_POSIX", "zh", "zh__#Hans", "zh_CN",
                "zh_CN_#Hans", "zh_HK", "zh_HK_#Hans", "zh_MO", "zh_MO_#Hans", "zh_SG", "zh_SG_#Hans"),
            "",
        },

        // Norwegian
        {
            "--include-locales=nb,nn,no",
            "jdk.localedata",
            List.of(
                "/jdk.localedata/sun/text/resources/ext/FormatData_no.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_no_NO.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_no_NO_NY.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_nb.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_nn.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_no.class"),
            List.of(
                "/jdk.localedata/sun/text/resources/ext/LineBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/thai_dict",
                "/jdk.localedata/sun/text/resources/ext/WordBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/BreakIteratorInfo_th.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_en_GB.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_th.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_001.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_th.class"),
            List.of(
                "(root)", "en", "en_US", "en_US_#Latn", "en_US_POSIX", "nb", "nb_NO",
                "nb_NO_#Latn", "nb_SJ", "nn", "nn_NO", "nn_NO_#Latn", "no", "no_NO", "no_NO_NY",
                "no_NO_#Latn"),
            "",
        },

        // Hebrew/Indonesian/Yiddish
        {
            "--include-locales=he,id,yi",
            "jdk.localedata",
            List.of(
                "/jdk.localedata/sun/text/resources/ext/FormatData_he.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_he_IL.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_id.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_id_ID.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_he.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_id.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_yi.class"),
            List.of(
                "/jdk.localedata/sun/text/resources/ext/LineBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/thai_dict",
                "/jdk.localedata/sun/text/resources/ext/WordBreakIteratorData_th",
                "/jdk.localedata/sun/text/resources/ext/BreakIteratorInfo_th.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_en_GB.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_th.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_001.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_th.class"),
            List.of(
                "(root)", "en", "en_US", "en_US_#Latn", "en_US_POSIX", "id", "id_ID",
                "id_ID_#Latn", "he", "he_IL", "he_IL_#Hebr", "yi", "yi_001", "yi_001_#Hebr"),
            "",
        },

        // Langtag including extensions. Should be ignored.
        {
            "--include-locales=en,ja-u-nu-thai",
            "jdk.localedata",
            List.of(
                "/jdk.localedata/sun/text/resources/ext/FormatData_en_GB.class",
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_en_001.class"),
            List.of(
                "/jdk.localedata/sun/text/resources/cldr/ext/FormatData_ja.class",
                "/jdk.localedata/sun/text/resources/ext/FormatData_th.class"),
            List.of(
                "(root)", "en", "en_001", "en_150", "en_AE", "en_AG", "en_AI", "en_AS", "en_AT",
                "en_AU", "en_BB", "en_BE", "en_BI", "en_BM", "en_BS", "en_BW", "en_BZ",
                "en_CA", "en_CC", "en_CH", "en_CK", "en_CM", "en_CX", "en_CY", "en_DE",
                "en_DG", "en_DK", "en_DM", "en_ER", "en_FI", "en_FJ", "en_FK", "en_FM",
                "en_GB", "en_GD", "en_GG", "en_GH", "en_GI", "en_GM", "en_GU", "en_GY",
                "en_HK", "en_IE", "en_IL", "en_IM", "en_IN", "en_IO", "en_JE", "en_JM",
                "en_KE", "en_KI", "en_KN", "en_KY", "en_LC", "en_LR", "en_LS", "en_MG",
                "en_MH", "en_MO", "en_MP", "en_MS", "en_MT", "en_MU", "en_MW", "en_MY",
                "en_NA", "en_NF", "en_NG", "en_NL", "en_NR", "en_NU", "en_NZ", "en_PG",
                "en_PH", "en_PK", "en_PN", "en_PR", "en_PW", "en_RW", "en_SB", "en_SC",
                "en_SD", "en_SE", "en_SG", "en_SH", "en_SI", "en_SL", "en_SS", "en_SX",
                "en_SZ", "en_TC", "en_TK", "en_TO", "en_TT", "en_TV", "en_TZ", "en_UG",
                "en_UM", "en_US", "en_US_#Latn", "en_US_POSIX", "en_VC", "en_VG", "en_VI", "en_VU",
                "en_WS", "en_ZA", "en_ZM", "en_ZW"),
            "",
        },

        // Error case: No matching locales
        {
            "--include-locales=xyz",
            "jdk.localedata",
            null,
            null,
            null,
            new PluginException(String.format(
                PluginsResourceBundle.getMessage("include-locales.nomatchinglocales"), "xyz"))
                .getMessage(),
        },

        // Error case: Invalid argument
        {
            "--include-locales=en,zh_HK",
            "jdk.localedata",
            null,
            null,
            null,
            new PluginException(String.format(
                PluginsResourceBundle.getMessage("include-locales.invalidtag"), "zh_hk"))
                .getMessage(),
        },

        // Error case: jdk.localedata is not added
        {
            "--include-locales=en-US",
            "java.base",
            null,
            null,
            null,
            new PluginException(
                PluginsResourceBundle.getMessage("include-locales.localedatanotfound"))
                .getMessage(),
        },
    };

    public static void main(String[] args) throws Exception {
        helper = Helper.newHelper();
        if (helper == null) {
            System.err.println("Test not run");
            return;
        }
        helper.generateDefaultModules();

        for (Object[] data : testData) {
            // create image for each test data
            Result result;
            if (data[INCLUDE_LOCALES_OPTION].toString().isEmpty()) {
                System.out.println("Invoking jlink with no --include-locales option");
                result = JImageGenerator.getJLinkTask()
                    .modulePath(helper.defaultModulePath())
                    .output(helper.createNewImageDir(moduleName))
                    .addMods((String) data[ADDMODS_OPTION])
                    .call();
            } else {
                System.out.println("Invoking jlink with \"" + data[INCLUDE_LOCALES_OPTION] + "\"");
                result = JImageGenerator.getJLinkTask()
                    .modulePath(helper.defaultModulePath())
                    .output(helper.createNewImageDir(moduleName))
                    .addMods((String) data[ADDMODS_OPTION])
                    .option((String) data[INCLUDE_LOCALES_OPTION])
                    .call();
            }

            String errorMsg = (String) data[ERROR_MESSAGE];
            if (errorMsg.isEmpty()) {
                Path image = result.assertSuccess();

                // test locale data entries
                testLocaleDataEntries(image,
                    (List<String>) data[EXPECTED_LOCATIONS],
                    (List<String>) data[UNEXPECTED_PATHS]);

                // test available locales
                testAvailableLocales(image, (List<String>) data[AVAILABLE_LOCALES]);
            } else {
                result.assertFailure(new TaskHelper(TaskHelper.JLINK_BUNDLE)
                    .getMessage("error.prefix") + " " +errorMsg);
                System.out.println("\tExpected failure: " + result.getMessage());
            }
        }

        if (errors > 0) {
            throw new RuntimeException("Test failed");
        }
    }

    private static void testLocaleDataEntries(Path image, List<String> expectedLocations,
                        List<String> unexpectedPaths) throws Exception {
        System.out.println("testLocaleDataEntries:");
        try {
            JImageValidator.validate(
                image.resolve("lib").resolve("modules"),
                expectedLocations, unexpectedPaths);
        } catch (Exception e) {
            System.out.println("\tFailed with: " + e);
            e.printStackTrace();
            errors++;
        }
    }

    private static void testAvailableLocales(Path image, List<String> availableLocales) throws Exception {
        System.out.println("testAvailableLocales:");
        Path launcher = image.resolve("bin/java" +
            (System.getProperty("os.name").startsWith("Windows") ? ".exe" : ""));
        List<String> args = new ArrayList<>(availableLocales.size() + 2);
        args.add(launcher.toString());
        args.add("GetAvailableLocales");
        args.addAll(availableLocales);
        Process proc = new ProcessBuilder(args).inheritIO().start();

        int len = Math.min(10, args.size());
        String command = args.subList(0, len).stream().collect(Collectors.joining(" "))
                         + (len < availableLocales.size() ? " ..." : "");

        int status = proc.waitFor();
        if (status == 0) {
            System.out.println("\tDone\t" + command);
        } else {
            System.out.println("\tExit " + status + "\t" + command);
            errors++;
        }
        System.out.println();
    }
}
