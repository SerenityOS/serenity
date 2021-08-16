/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4221795 8174270
 * @summary Confirm Normalizer's fundamental behavior
 * @modules java.base/sun.text java.base/jdk.internal.icu.text
 * @library /java/text/testlib
 * @compile -XDignore.symbol.file NormalizerAPITest.java
 * @run main/timeout=30 NormalizerAPITest
 */

import java.text.Normalizer;
import java.nio.CharBuffer;


/*
 * Tests around null/"" arguments for public methods.
 *
 * You may think that so elaborate testing for such a part is not necessary.
 * But I actually detected a bug by this program during my porting work.
 */
public class NormalizerAPITest extends IntlTest {

    //
    // Shortcuts
    //

    /*
     * Normalization forms
     */
    static final Normalizer.Form NFC  = Normalizer.Form.NFC;
    static final Normalizer.Form NFD  = Normalizer.Form.NFD;
    static final Normalizer.Form NFKC = Normalizer.Form.NFKC;
    static final Normalizer.Form NFKD = Normalizer.Form.NFKD;
    static final Normalizer.Form[] forms = {NFC, NFD, NFKC, NFKD};

    static final Normalizer.Form NULL = null;

    /*
     * Option
     */
    static final int[] options = {
        0x00,
        sun.text.Normalizer.UNICODE_3_2,
        jdk.internal.icu.text.NormalizerBase.UNICODE_3_2,
        jdk.internal.icu.text.NormalizerBase.UNICODE_LATEST,
    };

    static final String nonNullStr = "testdata";


    public static void main(String[] args) throws Exception {
        new NormalizerAPITest().run(args);
    }

    /*
     * Check if normalize(null) throws NullPointerException as expected.
     */
    public void Test_NullPointerException_java_normalize() {
        boolean error = false;

        /* Check null as String to be normalized */
        for (int i = 0; i < forms.length; i++) {
            try {
                String s = Normalizer.normalize(null, forms[i]);
                error = true;
            }
            catch (NullPointerException e) {
            }
        }

        /* Check null as a Normalization form */
        try {
            String s = Normalizer.normalize(nonNullStr, NULL);
            error = true;
        }
        catch (NullPointerException e) {
        }

        if (error) {
             errln("normalize(null) should throw NullPointerException.");
        }
    }

    /*
     * Check if normalize(null) throws NullPointerException as expected.
     */
    public void Test_NullPointerException_sun_normalize() {
        boolean error = false;

        for (int j = 0; j < options.length; j++) {
            for (int i = 0; i < forms.length; i++) {
                /* Check null as a String to be normalized */
                try {
                    String s = sun.text.Normalizer.normalize(null, forms[i], options[j]);
                    error = true;
                }
                catch (NullPointerException e) {
                }
            }

            /* Check null as a Normalization form */
            try {
                String s = sun.text.Normalizer.normalize(nonNullStr, NULL, options[j]);
                error = true;
            }
            catch (NullPointerException e) {
            }
        }

        if (error) {
             errln("normalize(null) should throw NullPointerException.");
        }
    }

    /*
     * Check if isNormalized(null) throws NullPointerException as expected.
     */
    public void Test_NullPointerException_java_isNormalized() {
        boolean error = false;

        for (int i = 0; i < forms.length; i++) {
            try {
                /* Check null as a String to be scanned */
                boolean b = Normalizer.isNormalized(null, forms[i]);
                error = true;
            }
            catch (NullPointerException e) {
            }
        }

        /* Check null as a String to be scanned */
        try {
            boolean b = Normalizer.isNormalized(nonNullStr, NULL);
            error = true;
        }

        catch (NullPointerException e) {
        }
        if (error) {
             errln("isNormalized(null) should throw NullPointerException.");
        }
    }

    /*
     * Check if isNormalized(null) throws NullPointerException as expected.
     */
    public void Test_NullPointerException_sun_isNormalized() {
        boolean error = false;

        for (int j = 0; j < options.length; j++) {
            for (int i = 0; i < forms.length; i++) {
                try {
                    /* Check null as a String to be scanned */
                    boolean b = sun.text.Normalizer.isNormalized(null, forms[i], options[j]);
                    error = true;
                }
                catch (NullPointerException e) {
                }
            }

            /* Check null as a String to be scanned */
            try {
                boolean b = sun.text.Normalizer.isNormalized(nonNullStr, NULL, options[j]);
                error = true;
            }
            catch (NullPointerException e) {
            }
        }

        if (error) {
             errln("isNormalized(null) should throw NullPointerException.");
        }
    }

    /*
     * Check if isNormalized("") doesn't throw NullPointerException and returns
     * "" as expected.
     */
    public void Test_No_NullPointerException_java_normalize() {
        boolean error = false;

        for (int i = 0; i < forms.length; i++) {
            try {
                String s = Normalizer.normalize("", forms[i]);
                if (!s.equals("")) {
                    error = true;
                }
            }
            catch (NullPointerException e) {
                error = true;
            }
        }

        if (error) {
             errln("normalize() for String(\"\") should return \"\".");
        }
    }

    /*
     * Check if isNormalized("") doesn't throw NullPointerException and returns
     * "" as expected.
     */
    public void Test_No_NullPointerException_sun_normalize() {
        boolean error = false;

        for (int j = 0; j < options.length; j++) {
            for (int i = 0; i < forms.length; i++) {
                try {
                    String s = sun.text.Normalizer.normalize("", forms[i], options[j]);
                    if (!s.equals("")) {
                        error = true;
                    }
                }
                catch (NullPointerException e) {
                    error = true;
                }
            }
        }
        if (error) {
             errln("normalize() for String(\"\") should return \"\".");
        }
    }

    /*
     * Check if isNormalized("") doesn't throw NullPointerException and returns
     * "" as expected.
     */
    public void Test_No_NullPointerException_java_isNormalized() {
        boolean error = false;

        for (int i = 0; i < forms.length; i++) {
            try {
                boolean b = Normalizer.isNormalized("", forms[i]);
                if (!b) {
                    error = true;
                }
            }
            catch (NullPointerException e) {
                error = true;
            }
        }
        if (error) {
             errln("isNormalized() for String(\"\") should not return true.");
        }
    }

    /*
     * Check if isNormalized("") doesn't throw NullPointerException and returns
     * "" as expected.
     */
    public void Test_No_NullPointerException_sun_isNormalized() {
        boolean error = false;

        for (int j = 0; j < options.length; j++) {
            for (int i = 0; i < forms.length; i++) {
                try {
                    boolean b = sun.text.Normalizer.isNormalized("", forms[i], options[j]);
                    if (!b) {
                        error = true;
                    }
                }
                catch (NullPointerException e) {
                    error = true;
                }
            }
        }
        if (error) {
             errln("isNormalized() for String(\"\") should not return true.");
        }
    }

    /*
     * Check if normalize() and isNormalized() work as expected for every
     * known class which implement CharSequence Interface.
     */
    public void Test_CharSequence() {

        check_CharSequence(String.valueOf(inputData),
                           String.valueOf(outputData));

        check_CharSequence(new StringBuffer(original),
                           new StringBuffer(expected));

        check_CharSequence(new StringBuilder(original),
                           new StringBuilder(expected));

        check_CharSequence(CharBuffer.wrap(inputData),
                           CharBuffer.wrap(outputData));
    }


    void check_CharSequence(CharSequence in, CharSequence expected) {
        String out = Normalizer.normalize(in, NFD);
        if (!out.equals(expected.toString())) {
            errln("java.text.Normalizer.normalize(" +
                  in.getClass().getSimpleName() + ") failed.");
        }
        out = sun.text.Normalizer.normalize(in, NFD,
                             jdk.internal.icu.text.NormalizerBase.UNICODE_LATEST);
        if (!out.equals(expected.toString())) {
            errln("sun.text.Normalizer.normalize(" +
                  in.getClass().getSimpleName() + ") failed.");
        }

        if (!Normalizer.isNormalized(expected, NFD)) {
            errln("java.text.Normalizer.isNormalize(" +
                  in.getClass().getSimpleName() + ") failed.");
        }
        if (!sun.text.Normalizer.isNormalized(expected, NFD,
                           jdk.internal.icu.text.NormalizerBase.UNICODE_LATEST)) {
            errln("sun.text.Normalizer.isNormalize(" +
                  in.getClass().getSimpleName() + ") failed.");
        }
    }

    static final char[] inputData  = {'T', 's', 'c', 'h', 'u', '\u1e9b'};
    static final char[] outputData = {'T', 's', 'c', 'h', 'u', '\u017f', '\u0307'};
    static final String original   = String.valueOf(inputData);
    static final String expected   = String.valueOf(outputData);
}
