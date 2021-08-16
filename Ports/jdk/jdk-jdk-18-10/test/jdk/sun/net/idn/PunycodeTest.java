/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Unit test for jdk.internal.icu.impl.Punycode
 * @bug 4737170 8174270
 * @modules java.base/jdk.internal.icu.impl
 * @compile -XDignore.symbol.file PunycodeTest.java
 * @run main/othervm -ea PunycodeTest
 * @author Edward Wang
 */

import java.util.Scanner;
import jdk.internal.icu.impl.Punycode;

/**
 * unit test for Punycode that is also originated from the sample code
 * provided in rfc3492.txt
 */
public class PunycodeTest {

    /* For testing, we'll just set some compile-time limits rather than */
    /* use malloc(), and set a compile-time option rather than using a  */
    /* command-line option.                                             */

    static final int  unicode_max_length = 256;
    static final int  ace_max_length = 256;

    static final String too_big =
            "input or output is too large, recompile with larger limits\n";
    static final String invalid_input = "invalid input\n";
    static final String overflow = "arithmetic overflow\n";
    static final String io_error = "I/O error\n";

    /* The following string is used to convert printable */
    /* characters between ASCII and the native charset:  */

    static void fail(String msg, String input) {
        System.out.println(msg+" input: "+input);
        throw new RuntimeException(msg+" input: "+input);
    }


    public int testCount = 0;

    private int input_length, j;
    private int output_length[] = new int[1];
    private boolean case_flags[] = new boolean[unicode_max_length];

    public String testEncoding(String inputS) {
        char input[] = new char[unicode_max_length];
        int codept = 0;
        char uplus[] = new char[2];
        StringBuffer output;
        int c;

        /* Read the input code points: */

        input_length = 0;

        Scanner sc = new Scanner(inputS);

        while (sc.hasNext()) {  // need to stop at end of line
            try {
                String next = sc.next();
                uplus[0] = next.charAt(0);
                uplus[1] = next.charAt(1);
                codept = Integer.parseInt(next.substring(2), 16);
            } catch (Exception ex) {
                fail(invalid_input, inputS);
            }

            if (uplus[1] != '+' || codept > Integer.MAX_VALUE) {
                fail(invalid_input, inputS);
            }

            if (input_length == unicode_max_length) fail(too_big, inputS);

            if (uplus[0] == 'u') case_flags[input_length] = false;
            else if (uplus[0] == 'U') case_flags[input_length] = true;
            else fail(invalid_input, inputS);

            input[input_length++] = (char)codept;
        }

        /* Encode: */

        output_length[0] = ace_max_length;
        try {
            output = Punycode.encode((new StringBuffer()).append(input, 0, input_length), case_flags);
        } catch (Exception e) {
            fail(invalid_input, inputS);
            // never reach here, just to make compiler happy
            return null;
        }

        testCount++;
        return output.toString();
    }

    public String testDecoding(String inputS) {
        char input[] = new char[0];
        int pp;
        StringBuffer output;

        /* Read the Punycode input string and convert to ASCII: */

        if (inputS.length() <= ace_max_length+2) {
            input = inputS.toCharArray();
        } else {
            fail(invalid_input, inputS);
        }
        input_length = input.length;

        /* Decode: */

        output_length[0] = unicode_max_length;
        try {
            output = Punycode.decode((new StringBuffer()).append(input, 0, input_length), case_flags);
        } catch (Exception e) {
            fail(invalid_input, inputS);
            // never reach here, just to make compiler happy
            return null;
        }

        /* Output the result: */
        StringBuffer result = new StringBuffer();
        for (j = 0;  j < output.length();  ++j) {
            result.append(String.format("%s+%04X ",
                    case_flags[j] ? "U" : "u",
                    (int)output.charAt(j) ));
        }

        testCount++;
        return result.substring(0, result.length() - 1);
    }

  // test data from rfc3492
  static String[][] testdata = {
    {"(A) Arabic (Egyptian):",
     "u+0644 u+064A u+0647 u+0645 u+0627 u+0628 u+062A u+0643 u+0644 "+
     "u+0645 u+0648 u+0634 u+0639 u+0631 u+0628 u+064A u+061F",
     "egbpdaj6bu4bxfgehfvwxn"},
    {"(B) Chinese (simplified):",
     "u+4ED6 u+4EEC u+4E3A u+4EC0 u+4E48 u+4E0D u+8BF4 u+4E2D u+6587",
     "ihqwcrb4cv8a8dqg056pqjye"},
    {"(C) Chinese (traditional):",
    "u+4ED6 u+5011 u+7232 u+4EC0 u+9EBD u+4E0D u+8AAA u+4E2D u+6587",
    "ihqwctvzc91f659drss3x8bo0yb"},
    {"(D) Czech: Pro<ccaron>prost<ecaron>nemluv<iacute><ccaron>esky",
    "U+0050 u+0072 u+006F u+010D u+0070 u+0072 u+006F u+0073 u+0074 "+
     "u+011B u+006E u+0065 u+006D u+006C u+0075 u+0076 u+00ED u+010D "+
     "u+0065 u+0073 u+006B u+0079",
    "Proprostnemluvesky-uyb24dma41a"},
    {"(E) Hebrew:",
    "u+05DC u+05DE u+05D4 u+05D4 u+05DD u+05E4 u+05E9 u+05D5 u+05D8 "+
     "u+05DC u+05D0 u+05DE u+05D3 u+05D1 u+05E8 u+05D9 u+05DD u+05E2 "+
     "u+05D1 u+05E8 u+05D9 u+05EA",
    "4dbcagdahymbxekheh6e0a7fei0b"},
    {"(F) Hindi (Devanagari):",
    "u+092F u+0939 u+0932 u+094B u+0917 u+0939 u+093F u+0928 u+094D "+
     "u+0926 u+0940 u+0915 u+094D u+092F u+094B u+0902 u+0928 u+0939 "+
     "u+0940 u+0902 u+092C u+094B u+0932 u+0938 u+0915 u+0924 u+0947 "+
     "u+0939 u+0948 u+0902",
    "i1baa7eci9glrd9b2ae1bj0hfcgg6iyaf8o0a1dig0cd"},
    {"(G) Japanese (kanji and hiragana):",
    "u+306A u+305C u+307F u+3093 u+306A u+65E5 u+672C u+8A9E u+3092 "+
     "u+8A71 u+3057 u+3066 u+304F u+308C u+306A u+3044 u+306E u+304B",
    "n8jok5ay5dzabd5bym9f0cm5685rrjetr6pdxa"},
    {"(H) Korean (Hangul syllables):",
    "u+C138 u+ACC4 u+C758 u+BAA8 u+B4E0 u+C0AC u+B78C u+B4E4 u+C774 "+
     "u+D55C u+AD6D u+C5B4 u+B97C u+C774 u+D574 u+D55C u+B2E4 u+BA74 "+
     "u+C5BC u+B9C8 u+B098 u+C88B u+C744 u+AE4C",
    "989aomsvi5e83db1d2a355cv1e0vak1dwrv93d5xbh15a0dt30a5j"+
    "psd879ccm6fea98c"},
    {"(I) Russian (Cyrillic):",
    "U+043F u+043E u+0447 u+0435 u+043C u+0443 u+0436 u+0435 u+043E "+
     "u+043D u+0438 u+043D u+0435 u+0433 u+043E u+0432 u+043E u+0440 "+
     "u+044F u+0442 u+043F u+043E u+0440 u+0443 u+0441 u+0441 u+043A "+
     "u+0438",
    "b1abfaaepdrnnbgefbaDotcwatmq2g4l"},
    {"(J) Spanish: Porqu<eacute>nopuedensimplementehablarenEspa<ntilde>ol",
    "U+0050 u+006F u+0072 u+0071 u+0075 u+00E9 u+006E u+006F u+0070 "+
     "u+0075 u+0065 u+0064 u+0065 u+006E u+0073 u+0069 u+006D u+0070 "+
     "u+006C u+0065 u+006D u+0065 u+006E u+0074 u+0065 u+0068 u+0061 "+
     "u+0062 u+006C u+0061 u+0072 u+0065 u+006E U+0045 u+0073 u+0070 "+
     "u+0061 u+00F1 u+006F u+006C",
    "PorqunopuedensimplementehablarenEspaol-fmd56a"},
    {"(K) Vietnamese:"+
     "T<adotbelow>isaoh<odotbelow>kh<ocirc>ngth<ecirchookabove>ch"+
     "<ihookabove>n<oacute>iti<ecircacute>ngVi<ecircdotbelow>t",
    "U+0054 u+1EA1 u+0069 u+0073 u+0061 u+006F u+0068 u+1ECD u+006B "+
     "u+0068 u+00F4 u+006E u+0067 u+0074 u+0068 u+1EC3 u+0063 u+0068 "+
     "u+1EC9 u+006E u+00F3 u+0069 u+0074 u+0069 u+1EBF u+006E u+0067 "+
     "U+0056 u+0069 u+1EC7 u+0074",
    "TisaohkhngthchnitingVit-kjcr8268qyxafd2f1b9g"},
    {"(L) 3<nen>B<gumi><kinpachi><sensei>",
     "u+0033 u+5E74 U+0042 u+7D44 u+91D1 u+516B u+5148 u+751F",
    "3B-ww4c5e180e575a65lsy2b"},
    {"(M) <amuro><namie>-with-SUPER-MONKEYS",
     "u+5B89 u+5BA4 u+5948 u+7F8E u+6075 u+002D u+0077 u+0069 u+0074 "+
     "u+0068 u+002D U+0053 U+0055 U+0050 U+0045 U+0052 u+002D U+004D "+
     "U+004F U+004E U+004B U+0045 U+0059 U+0053",
    "-with-SUPER-MONKEYS-pc58ag80a8qai00g7n9n"},
    {"(N) Hello-Another-Way-<sorezore><no><basho>",
    "U+0048 u+0065 u+006C u+006C u+006F u+002D U+0041 u+006E u+006F "+
     "u+0074 u+0068 u+0065 u+0072 u+002D U+0057 u+0061 u+0079 u+002D "+
     "u+305D u+308C u+305E u+308C u+306E u+5834 u+6240",
    "Hello-Another-Way--fc4qua05auwb3674vfr0b"},
    {"(O) <hitotsu><yane><no><shita>2",
     "u+3072 u+3068 u+3064 u+5C4B u+6839 u+306E u+4E0B u+0032",
    "2-u9tlzr9756bt3uc0v"},
    {"(P) Maji<de>Koi<suru>5<byou><mae>",
     "U+004D u+0061 u+006A u+0069 u+3067 U+004B u+006F u+0069 u+3059 "+
     "u+308B u+0035 u+79D2 u+524D",
    "MajiKoi5-783gue6qz075azm5e"},
    {"(Q) <pafii>de<runba>",
     "u+30D1 u+30D5 u+30A3 u+30FC u+0064 u+0065 u+30EB u+30F3 u+30D0",
    "de-jg4avhby1noc0d"},
    {"(R) <sono><supiido><de>",
     "u+305D u+306E u+30B9 u+30D4 u+30FC u+30C9 u+3067",
    "d9juau41awczczp"},
    {"(S) -> $1.00 <-",
     "u+002D u+003E u+0020 u+0024 u+0031 u+002E u+0030 u+0030 u+0020 "+
     "u+003C u+002D",
    "-> $1.00 <--"},
  };

  public static void main(String[] argv) throws Exception {
      PunycodeTest mytest = new PunycodeTest();
      for (int i = 0; i < testdata.length; i++) {
          String encodeResult = mytest.testEncoding(testdata[i][1]);
          String decodeResult = mytest.testDecoding(testdata[i][2]);

          checkResult(encodeResult, testdata[i][2]);
          checkResult(decodeResult, testdata[i][1]);
      }

      System.out.println("Test cases: " + mytest.testCount);
  }

  public static void checkResult(String actual, String expected) {
      if (!actual.equals(expected)) {
          System.out.printf("\n%15s: %s\n", "FAILED", actual);
          System.out.printf("%15s: %s\n\n", "should be", expected);
          throw new RuntimeException("Punycode test failed.");
      } else {
          System.out.printf("%15s: %s\n", "SUCCEEDED", actual);
      }
  }

}
