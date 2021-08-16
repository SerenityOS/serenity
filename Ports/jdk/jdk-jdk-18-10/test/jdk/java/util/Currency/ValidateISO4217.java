/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4691089 4819436 4942982 5104960 6544471 6627549 7066203 7195759
 *      8039317 8074350 8074351 8145952 8187946 8193552 8202026 8204269
 *      8208746 8209775 8264792
 * @summary Validate ISO 4217 data for Currency class.
 * @modules java.base/java.util:open
 *          jdk.localedata
 */

/*
 * ############################################################################
 *
 *  ValidateISO4217 is a tool to detect differences between the latest ISO 4217
 *  data and and Java's currency data which is based on ISO 4217.
 *  If there is a difference, the following file which includes currency data
 *  may need to be updated.
 *      src/share/classes/java/util/CurrencyData.properties
 *
 * ############################################################################
 *
 * 1) Make a golden-data file.
 *      From BSi's ISO4217 data (TABLE A1.doc), extract four (or eight, if currency is changing)
 *      fields and save as ./tablea1.txt.
 *        <Country code>\t<Currency code>\t<Numeric code>\t<Minor unit>[\t<Cutover Date>\t<new Currency code>\t<new Numeric code>\t<new Minor unit>]
 *      The Cutover Date is given in SimpleDateFormat's 'yyyy-MM-dd-HH-mm-ss' format in the GMT time zone.
 *
 * 2) Compile ValidateISO4217.java
 *
 * 3) Execute ValidateISO4217 as follows:
 *      java ValidateISO4217
 */

import java.io.*;
import java.text.*;
import java.util.*;

public class ValidateISO4217 {

    static final int ALPHA_NUM = 26;

    static final byte UNDEFINED = 0;
    static final byte DEFINED = 1;
    static final byte SKIPPED = 2;

    /* input files */
    static final String datafile = "tablea1.txt";

    /* alpha2-code table */
    static byte[] codes = new byte[ALPHA_NUM * ALPHA_NUM];

    static final String[][] additionalCodes = {
        /* Defined in ISO 4217 list, but don't have code and minor unit info. */
        {"AQ", "", "", "0"},    // Antarctica

        /*
         * Defined in ISO 4217 list, but don't have code and minor unit info in
         * it. On the other hand, both code and minor unit are defined in
         * .properties file. I don't know why, though.
         */
        {"GS", "GBP", "826", "2"},      // South Georgia And The South Sandwich Islands

        /* Not defined in ISO 4217 list, but defined in .properties file. */
        {"AX", "EUR", "978", "2"},      // \u00c5LAND ISLANDS
        {"PS", "ILS", "376", "2"},      // Palestinian Territory, Occupied

        /* Not defined in ISO 4217 list, but added in ISO 3166 country code list */
        {"JE", "GBP", "826", "2"},      // Jersey
        {"GG", "GBP", "826", "2"},      // Guernsey
        {"IM", "GBP", "826", "2"},      // Isle of Man
        {"BL", "EUR", "978", "2"},      // Saint Barthelemy
        {"MF", "EUR", "978", "2"},      // Saint Martin

        /* Defined neither in ISO 4217 nor ISO 3166 list */
        {"XK", "EUR", "978", "2"},      // Kosovo
    };

    /* Codes that are obsolete, do not have related country */
    static final String otherCodes =
        "ADP-AFA-ATS-AYM-AZM-BEF-BGL-BOV-BYB-BYR-CHE-CHW-CLF-COU-CUC-CYP-"
        + "DEM-EEK-ESP-FIM-FRF-GHC-GRD-GWP-IEP-ITL-LTL-LUF-LVL-MGF-MRO-MTL-MXV-MZM-NLG-"
        + "PTE-ROL-RUR-SDD-SIT-SKK-SRG-STD-TMM-TPE-TRL-VEF-UYI-USN-USS-VEB-"
        + "XAG-XAU-XBA-XBB-XBC-XBD-XDR-XFO-XFU-XPD-XPT-XSU-XTS-XUA-XXX-"
        + "YUM-ZMK-ZWD-ZWN-ZWR";

    static boolean err = false;

    static Set<Currency> testCurrencies = new HashSet<Currency>();

    public static void main(String[] args) throws Exception {
        CheckDataVersion.check();
        test1();
        test2();
        getAvailableCurrenciesTest();

        if (err) {
            throw new RuntimeException("Failed: Validation ISO 4217 data");
        }
    }

    static void test1() throws Exception {

        try (FileReader fr = new FileReader(new File(System.getProperty("test.src", "."), datafile));
             BufferedReader in = new BufferedReader(fr))
        {
            String line;
            SimpleDateFormat format = null;

            while ((line = in.readLine()) != null) {
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }

                StringTokenizer tokens = new StringTokenizer(line, "\t");
                String country = tokens.nextToken();
                if (country.length() != 2) {
                    continue;
                }

                String currency;
                String numeric;
                String minorUnit;
                int tokensCount = tokens.countTokens();
                if (tokensCount < 3) {
                    currency = "";
                    numeric = "0";
                    minorUnit = "0";
                } else {
                    currency = tokens.nextToken();
                    numeric = tokens.nextToken();
                    minorUnit = tokens.nextToken();
                    testCurrencies.add(Currency.getInstance(currency));

                    // check for the cutover
                    if (tokensCount > 3) {
                        if (format == null) {
                            format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
                            format.setTimeZone(TimeZone.getTimeZone("GMT"));
                            format.setLenient(false);
                        }
                        if (format.parse(tokens.nextToken()).getTime() <
                            System.currentTimeMillis()) {
                            currency = tokens.nextToken();
                            numeric = tokens.nextToken();
                            minorUnit = tokens.nextToken();
                            testCurrencies.add(Currency.getInstance(currency));
                        }
                    }
                }
                int index = toIndex(country);
                testCountryCurrency(country, currency, Integer.parseInt(numeric),
                    Integer.parseInt(minorUnit), index);
            }
        }

        for (int i = 0; i < additionalCodes.length; i++) {
            int index = toIndex(additionalCodes[i][0]);
            if (additionalCodes[i][1].length() != 0) {
                testCountryCurrency(additionalCodes[i][0], additionalCodes[i][1],
                    Integer.parseInt(additionalCodes[i][2]),
                    Integer.parseInt(additionalCodes[i][3]), index);
                testCurrencies.add(Currency.getInstance(additionalCodes[i][1]));
            } else {
                codes[index] = SKIPPED;
            }
        }
    }

    static int toIndex(String s) {
        return ((s.charAt(0) - 'A') * ALPHA_NUM + s.charAt(1) - 'A');
    }

    static void testCountryCurrency(String country, String currencyCode,
                                int numericCode, int digits, int index) {
        if (currencyCode.length() == 0) {
            return;
        }
        testCurrencyDefined(currencyCode, numericCode, digits);

        Locale loc = new Locale("", country);
        try {
            Currency currency = Currency.getInstance(loc);
            if (!currency.getCurrencyCode().equals(currencyCode)) {
                System.err.println("Error: [" + country + ":" +
                    loc.getDisplayCountry() + "] expected: " + currencyCode +
                    ", got: " + currency.getCurrencyCode());
                err = true;
            }

            if (codes[index] != UNDEFINED) {
                System.out.println("Warning: [" + country + ":" +
                    loc.getDisplayCountry() +
                    "] multiple definitions. currency code=" + currencyCode);
            }
            codes[index] = DEFINED;
        }
        catch (Exception e) {
            System.err.println("Error: " + e + ": Country=" + country);
            err = true;
        }
    }

    static void testCurrencyDefined(String currencyCode, int numericCode, int digits) {
        try {
            Currency currency = currency = Currency.getInstance(currencyCode);

            if (currency.getNumericCode() != numericCode) {
                System.err.println("Error: [" + currencyCode + "] expected: " +
                    numericCode + "; got: " + currency.getNumericCode());
                err = true;
            }

            if (currency.getDefaultFractionDigits() != digits) {
                System.err.println("Error: [" + currencyCode + "] expected: " +
                    digits + "; got: " + currency.getDefaultFractionDigits());
                err = true;
            }
        }
        catch (Exception e) {
            System.err.println("Error: " + e + ": Currency code=" +
                currencyCode);
            err = true;
        }
    }

    static void test2() {
        for (int i = 0; i < ALPHA_NUM; i++) {
            for (int j = 0; j < ALPHA_NUM; j++) {
                char[] code = new char[2];
                code[0] = (char)('A'+ i);
                code[1] = (char)('A'+ j);
                String country = new String(code);
                boolean ex;

                if (codes[toIndex(country)] == UNDEFINED) {
                    ex = false;
                    try {
                        Currency.getInstance(new Locale("", country));
                    }
                    catch (IllegalArgumentException e) {
                        ex = true;
                    }
                    if (!ex) {
                        System.err.println("Error: This should be an undefined code and throw IllegalArgumentException: " +
                            country);
                        err = true;
                    }
                } else if (codes[toIndex(country)] == SKIPPED) {
                    Currency cur = null;
                    try {
                        cur = Currency.getInstance(new Locale("", country));
                    }
                    catch (Exception e) {
                        System.err.println("Error: " + e + ": Country=" +
                            country);
                        err = true;
                    }
                    if (cur != null) {
                        System.err.println("Error: Currency.getInstance() for an this locale should return null: " +
                            country);
                        err = true;
                    }
                }
            }
        }
    }

    /**
     * This test depends on test1(), where 'testCurrencies' set is constructed
     */
    static void getAvailableCurrenciesTest() {
        Set<Currency> jreCurrencies = Currency.getAvailableCurrencies();

        // add otherCodes
        StringTokenizer st = new StringTokenizer(otherCodes, "-");
        while (st.hasMoreTokens()) {
            testCurrencies.add(Currency.getInstance(st.nextToken()));
        }

        if (!testCurrencies.containsAll(jreCurrencies)) {
            System.err.print("Error: getAvailableCurrencies() returned extra currencies than expected: ");
            jreCurrencies.removeAll(testCurrencies);
            for (Currency c : jreCurrencies) {
                System.err.print(" "+c);
            }
            System.err.println();
            err = true;
        }
    }
}
