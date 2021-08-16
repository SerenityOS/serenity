/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8054307
 * @summary Tests correctness of string related intrinsics and C2 optimizations.
 * @library /test/lib
 *
 * @run main/timeout=240 compiler.intrinsics.string.TestStringIntrinsics
 */

package compiler.intrinsics.string;

import jdk.test.lib.format.Format;
import jdk.test.lib.format.ArrayCodec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

public class TestStringIntrinsics {

    public enum Operation {
        ARR_EQUALS_B, ARR_EQUALS_C, EQUALS, COMPARE_TO, INDEX_OF, INDEX_OF_CON_U, INDEX_OF_CON_L,
        INDEX_OF_CON_UL, CONCAT, CONCAT_C, CONCAT_I, CONCAT_M, INDEX_OF_CHAR
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Test {
        Operation op();
        String constString() default "";
        String[] inStrings() default {};
        char[] inChars() default {};
        int[] inInts() default {};
        String[] outStrings() default {};
    }

    public static void main(String[] args) throws Exception {
        new TestStringIntrinsics().run();
    }

    public void run() throws Exception {
        // Build latin1 and UTF16 strings
        StringBuilder latin1Builder = new StringBuilder();
        for (int i = 0; i <= 255; ++i) {
            latin1Builder.append((char) i);
        }
        String latin1 = latin1Builder.toString();
        StringBuilder utf16Builder = new StringBuilder();
        for (int i = 0; i <= 10000; ++i) {
            utf16Builder.append((char) i);
        }
        String utf16 = utf16Builder.toString();

        // Invoke test methods
        for (Method m : TestStringIntrinsics.class.getMethods()) {
            if (m.isAnnotationPresent(Test.class)) {
                System.out.print("Checking " + m.getName() + "... ");
                Test antn = m.getAnnotation(Test.class);
                Operation op = antn.op();
                if (isStringConcatTest(op)) {
                    checkStringConcat(op, m, antn);
                } else {
                    checkIntrinsics(op, m, latin1, utf16, antn);
                }
                System.out.println("Done.");
            }
        }
    }

    private boolean isStringConcatTest(Operation op) {
        return op == Operation.CONCAT ||
               op == Operation.CONCAT_C ||
               op == Operation.CONCAT_I ||
               op == Operation.CONCAT_M;
    }

    /**
     * Checks correctness of the String.equals, String.compareTo and String.indexOf intrinsics.
     * -XX:SpecialStringEquals
     * -XX:SpecialStringCompareTo
     * -XX:SpecialStringIndexOf
     */
    private void checkIntrinsics(Operation op, Method m, String latin1, String utf16, Test antn) throws Exception {
        for (int i = 0; i < 50_000; ++i) {
            // Copy and permute latin1 and UTF16 string
            char[] arrL = latin1.toCharArray();
            int indexL = i % arrL.length;
            int mod = (arrL.length - arrL[indexL]);
            int incL = i % ((mod != 0) ? mod : 1);
            arrL[indexL] = (char) ((int) arrL[indexL] + incL);
            String latin1Copy = String.valueOf(arrL);

            char[] arrU = utf16.toCharArray();
            int indexU = i % arrU.length;
            mod = (arrU.length - arrU[indexU]);
            int incU = i % ((mod != 0) ? mod : 1);
            arrU[indexU] = (char) ((int) arrU[indexU] + incU);
            String utf16Copy = String.valueOf(arrU);

            switch (op) {
            case ARR_EQUALS_B:
                invokeAndCompareArrays(m, (incL == 0), latin1.getBytes("ISO-8859-1"), latin1Copy.getBytes("ISO-8859-1"));
                invokeAndCompareArrays(m, true, new byte[] {1, 2, 3}, new byte[] {1, 2, 3});
                invokeAndCompareArrays(m, true, new byte[] {1}, new byte[] {1});
                invokeAndCompareArrays(m, true, new byte[] {}, new byte[] {});
                break;
            case ARR_EQUALS_C:
                invokeAndCompareArrays(m, (incU == 0), utf16.toCharArray(), arrU);
                break;
            case EQUALS:
                invokeAndCheck(m, (incL == 0), latin1, latin1Copy);
                invokeAndCheck(m, false, latin1, "");
                invokeAndCheck(m, false, "", latin1);

                invokeAndCheck(m, (incU == 0), utf16, utf16Copy);
                invokeAndCheck(m, false, utf16, "");
                invokeAndCheck(m, false, "", utf16);

                invokeAndCheck(m, false, latin1, utf16);
                break;
            case COMPARE_TO:
                invokeAndCheck(m, -incL, latin1, latin1Copy);
                invokeAndCheck(m, latin1.length(), latin1, "");

                invokeAndCheck(m, -incU, utf16, utf16Copy);
                invokeAndCheck(m, utf16.length(), utf16, "");

                // Cross coder
                char cL = latin1.charAt(indexL);
                char cU = utf16.charAt(indexU);
                invokeAndCheck(m, cL - cU, latin1, latin1.replace(cL, cU));
                invokeAndCheck(m, cU - cL, utf16, utf16.replace(cU, cL));

                // Different lengths
                invokeAndCheck(m, 1, "ABCD", "ABC");
                invokeAndCheck(m, -1, "\uff21\uff22\uff23", "\uff21\uff22\uff23\uff24");
                invokeAndCheck(m, 1, "ABC\uff24", "ABC");
                invokeAndCheck(m, 3, "ABC\uff24\uff25\uff26", "ABC");
                invokeAndCheck(m, -1, "ABC","ABC\uff24");
                invokeAndCheck(m, -3, "ABC","ABC\uff24\uff25\uff26");
                break;
            case INDEX_OF:
                invokeAndCheck(m, indexL, latin1, latin1.substring(indexL), (indexL > 42) ? 42 : 0);
                invokeAndCheck(m, 0, latin1, "", 0);

                invokeAndCheck(m, indexU, utf16, utf16.substring(indexU), (indexU > 42) ? 42 : 0);
                invokeAndCheck(m, 0, utf16, "", 0);

                // Cross coder
                invokeAndCheck(m, -1, latin1.substring(0, indexL), utf16.substring(indexU), (indexL > 42) ? 42 : 0);
                // Skip latin1 chars in utf16 string
                int start = 256;
                int end = indexU > start ? indexU : start;
                invokeAndCheck(m, end-start, utf16.substring(start, end) + latin1.substring(indexL), latin1.substring(indexL), 0);
                break;
            case INDEX_OF_CON_L:
                invokeAndCheck(m, antn.constString(), latin1);
                break;
            case INDEX_OF_CON_U:
                invokeAndCheck(m, antn.constString(), utf16);
                break;
            case INDEX_OF_CON_UL:
                invokeAndCheck(m, antn.constString(), utf16);
                break;
            case INDEX_OF_CHAR:
                invokeAndCheck(m, 7, "abcdefg\uD800\uDC00", 65536, 0);
                invokeAndCheck(m, -1, "abcdefg\uD800\uDC01", 65536, 0);
                invokeAndCheck(m, -1, "abcdefg\uD800", 65536, 0);
                invokeAndCheck(m, 3, "abc\u0107", 263, 0);
                invokeAndCheck(m, -1, "abc\u0108", 263, 0);
                invokeAndCheck(m, 7, "abcdefg\u0107", 263, 0);
                invokeAndCheck(m, 7, "abcdefg\u0107", 263, -1);
                invokeAndCheck(m, 0, "\u0107", 263, 0);
                break;
            default:
                throw new RuntimeException("Unexpected operation.");
            }
        }
    }

    /**
     * Checks correctness of the C2 string concatenation optimization.
     * -XX:OptimizeStringConcat
     */
    private void checkStringConcat(Operation op, Method m, Test antn) throws Exception {
        for (int i = 0; i < 50_000; ++i) {
            String[] result = antn.outStrings();
            switch(op) {
            case CONCAT:
                String[] strs = antn.inStrings();
                for (int j = 0; j < strs.length; ++j) {
                    invokeAndCheck(m, result[j], strs[j]);
                }
                break;
            case CONCAT_C:
                char[] ch = antn.inChars();
                for (int j = 0; j < ch.length; ++j) {
                    invokeAndCheck(m, result[j], ch[j]);
                }
                break;
            case CONCAT_I:
                int[] k = antn.inInts();
                for (int j = 0; j < k.length; ++j) {
                    invokeAndCheck(m, result[j], k[j]);
                }
                break;
            case CONCAT_M:
                strs = antn.inStrings();
                ch = antn.inChars();
                k = antn.inInts();
                for (int j = 0; j < strs.length; ++j) {
                    invokeAndCheck(m, result[j], strs[j], ch[j], k[j]);
                }
                break;
            default:
                throw new RuntimeException("Unexpected operation.");
            }
        }
    }

    /**
     * Invokes method 'm' by passing arguments the two 'args' (which are supposed to be arrays)
     * checks if the returned value. In case of error and arrays being not equal, prints their difference.
     */
    private void invokeAndCompareArrays(Method m, boolean expectedResult, Object arg0, Object arg1) throws Exception {
        boolean result = (Boolean)m.invoke(null, arg0, arg1);
        if (expectedResult == result)
            return;

        String cause = String.format("Result: (%b) of '%s' is not equal to expected (%b)",
                        result, m.getName(), expectedResult);

        if (expectedResult == true) {
            System.err.println(cause);
            System.err.println(Format.arrayDiff(arg0, arg1));
        } else {
            System.err.println(cause);
            System.err.printf("First array argument: %n    %s%n", ArrayCodec.format(arg0));
        }

        throw new RuntimeException(cause);
    }

    /**
     * Invokes method 'm' by passing arguments 'args' and checks if the
     * returned value equals 'expectedResult'.
     */
    private void invokeAndCheck(Method m, Object expectedResult, Object... args) throws Exception {
        Object actualResult = m.invoke(null, args);
        if (!actualResult.equals(expectedResult)) {
            var nl = System.lineSeparator();
            StringBuilder msgBuilder = new StringBuilder();
            msgBuilder.append("Actual result of '" + m.getName() + "' is not equal to expected value." + nl);
            msgBuilder.append("Expected: " + Format.asLiteral(expectedResult) + nl);
            msgBuilder.append("Actual: " + Format.asLiteral(actualResult));

            for (int i = 0; i < args.length; i++) {
                msgBuilder.append(nl + "    Arg" + i + ": " + Format.asLiteral(args[i]));
            }

            final String message = msgBuilder.toString();
            System.err.println(message);
            throw new RuntimeException(message);
        }
    }

    /*
     * Constants
     */
    static final char charU = '\uff21';
    static final char charL = 'A';
    static final String emptyString = "";
    static final String stringL = "abcdefghijklmnop";
    static final String stringSmallL = "abc";
    static final String stringU = "\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28";
    static final String stringSmallU = "\u0f21\u0f22\u0f23";
    static final int constInt = 123;
    static final int constIntNeg = -123;

    /*
     * Arrays.equals
     */
    @Test(op = Operation.ARR_EQUALS_B)
    public static boolean arrayEqualsB(byte[] a, byte[] b) {
      return Arrays.equals(a, b);
    }

    @Test(op = Operation.ARR_EQUALS_C)
    public static boolean arrayEqualsC(char[] a, char[] b) {
      return Arrays.equals(a, b);
    }

    /*
     * String.equals
     */
    @Test(op = Operation.EQUALS)
    public static boolean equals(String a, String b) {
        return a.equals(b);
    }

    /*
     * String.compareTo
     */
    @Test(op = Operation.COMPARE_TO)
    public static int compareTo(String a, String b) {
        return a.compareTo(b);
    }

    /*
     * String.indexOf
     */
    @Test(op = Operation.INDEX_OF)
    public static int indexOf(String a, String b, int from) {
        return a.indexOf(b, from);
    }

    @Test(op = Operation.INDEX_OF_CON_U, constString = stringSmallU)
    public static String indexOfConstU(String a) {
        int result = a.indexOf(stringSmallU);
        return a.substring(result, result + stringSmallU.length());
    }

    @Test(op = Operation.INDEX_OF_CON_U, constString = stringU)
    public static String indexOfConstLargeU(String a) {
        int result = a.indexOf(stringU);
        return a.substring(result, result + stringU.length());
    }

    @Test(op = Operation.INDEX_OF_CON_U, constString = emptyString)
    public static String indexOfConstEmptyU(String a) {
        int result = a.indexOf(emptyString);
        return a.substring(result, result + emptyString.length());
    }

    @Test(op = Operation.INDEX_OF_CON_L, constString = stringSmallL)
    public static String indexOfConstL(String a) {
        int result = a.indexOf(stringSmallL);
        return a.substring(result, result + stringSmallL.length());
    }

    @Test(op = Operation.INDEX_OF_CON_L, constString = stringL)
    public static String indexOfConstLargeL(String a) {
        int result = a.indexOf(stringL);
        return a.substring(result, result + stringL.length());
    }

    @Test(op = Operation.INDEX_OF_CON_L, constString = emptyString)
    public static String indexOfConstEmptyL(String a) {
        int result = a.indexOf(emptyString);
        return a.substring(result, result + emptyString.length());
    }

    @Test(op = Operation.INDEX_OF_CON_UL, constString = stringSmallL)
    public static String indexOfConstUL(String a) {
        int result = a.indexOf(stringSmallL);
        return a.substring(result, result + stringSmallL.length());
    }

    @Test(op = Operation.INDEX_OF_CON_UL, constString = stringL)
    public static String indexOfConstLargeUL(String a) {
        int result = a.indexOf(stringL);
        return a.substring(result, result + stringL.length());
    }

    @Test(op = Operation.INDEX_OF_CHAR)
    public static int indexOfChar(String a, int ch, int from) {
        return a.indexOf(ch, from);
    }

    /*
     * String concatenation optimization
     */
    @Test(op = Operation.CONCAT, inStrings = {"ABC", "\uff21\uff22\uff23"}, outStrings = {"ABC", "\uff21\uff22\uff23"})
    public static String concatString(String a) {
        return new StringBuilder().append(a).toString();
    }

    @Test(op = Operation.CONCAT, inStrings = {""}, outStrings = {""})
    public static String concatStringEmpty(String a) {
        return new StringBuilder().toString();
    }

    @Test(op = Operation.CONCAT, inStrings = {""}, outStrings = {"null"})
    public static String concatStringNull(String a) {
        return new StringBuilder().append((String)null).toString();
    }

    @Test(op = Operation.CONCAT, inStrings = {"ABC", "\uff21\uff22\uff23"}, outStrings = {"abcdefghijklmnopABCabc", "abcdefghijklmnop\uff21\uff22\uff23abc"})
    public static String concatStringConstL(String a) {
        return new StringBuilder().append(stringL).append(a).append(stringSmallL).toString();
    }

    @Test(op = Operation.CONCAT, inStrings = {"ABC", "\uff21\uff22\uff23"}, outStrings = {"\u0f21\u0f22\u0f23ABC\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28", "\u0f21\u0f22\u0f23\uff21\uff22\uff23\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28"})
    public static String concatStringConstU(String a) {
        return new StringBuilder().append(stringSmallU).append(a).append(stringU).toString();
    }

    @Test(op = Operation.CONCAT_C, inChars = {'A', '\uff21'}, outStrings = {"A", "\uff21"})
    public static String concatChar(char a) {
        return new StringBuilder().append(a).toString();
    }

    @Test(op = Operation.CONCAT_C, inChars = {'A', '\uff21'}, outStrings = {"abcdefghijklmnopAabcA\uff21", "abcdefghijklmnop\uff21abcA\uff21"})
    public static String concatCharConstL(char a) {
        return new StringBuilder().append(stringL).append(a).append(stringSmallL).append(charL).append(charU).toString();
    }

    @Test(op = Operation.CONCAT_C, inChars = {'A', '\uff21'}, outStrings = {"\u0f21\u0f22\u0f23A\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28\uff21A", "\u0f21\u0f22\u0f23\uff21\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28\uff21A"})
    public static String concatCharConstU(char a) {
        return new StringBuilder().append(stringSmallU).append(a).append(stringU).append(charU).append(charL).toString();
    }

    @Test(op = Operation.CONCAT_I, inInts = {Integer.MIN_VALUE, -42, 42, Integer.MAX_VALUE}, outStrings = {"-2147483648", "-42", "42", "2147483647"})
    public static String concatInt(int a) {
        return new StringBuilder().append(a).toString();
    }

    @Test(op = Operation.CONCAT_I, inInts = {Integer.MIN_VALUE, -42, 42, Integer.MAX_VALUE}, outStrings = {"abcdefghijklmnop-2147483648abc123-123", "abcdefghijklmnop-42abc123-123", "abcdefghijklmnop42abc123-123", "abcdefghijklmnop2147483647abc123-123"})
    public static String concatIntConstL(int b) {
        return new StringBuilder().append(stringL).append(b).append(stringSmallL).append(constInt).append(constIntNeg).toString();
    }

    @Test(op = Operation.CONCAT_I, inInts = {Integer.MIN_VALUE, -42, 42, Integer.MAX_VALUE}, outStrings = {"\u0f21\u0f22\u0f23-2147483648\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28123-123", "\u0f21\u0f22\u0f23-42\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28123-123", "\u0f21\u0f22\u0f2342\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28123-123", "\u0f21\u0f22\u0f232147483647\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28123-123"})
    public static String concatIntConstU(int b) {
        return new StringBuilder().append(stringSmallU).append(b).append(stringU).append(constInt).append(constIntNeg).toString();
    }

    @Test(op = Operation.CONCAT, inStrings = {""}, outStrings = {"nullabcabcdefghijklmnopA123-123"})
    public static String concatConstL(String a) {
        return new StringBuilder().append((String)null).append(stringSmallL).append(stringL).append(charL).append(constInt).append(constIntNeg).toString();
    }

    @Test(op = Operation.CONCAT, inStrings = {""}, outStrings = {"nullabcabcdefghijklmnop\u0f21\u0f22\u0f23\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28A\uff21123-123"})
    public static String concatConstU(String a) {
        return new StringBuilder().append((String)null).append(stringSmallL).append(stringL).append(stringSmallU).append(stringU).append(charL).append(charU).append(constInt).append(constIntNeg).toString();
    }

    @Test(op = Operation.CONCAT_M,
          inStrings = {"ABCDEFG", "ABCDEFG", "\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28", "\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28"},
          inChars = {'A', '\uff21', 'A', '\uff21'},
          inInts = {Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE},
          outStrings = {"ABCDEFGA-2147483648nullabcdefghijklmnop123-123A\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28\uff21ABCDEFGA-2147483648null",
                        "ABCDEFG\uff212147483647nullabcdefghijklmnop123-123A\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28\uff21ABCDEFG\uff212147483647null",
                        "\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28A-2147483648nullabcdefghijklmnop123-123A\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28\uff21\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28A-2147483648null",
            "\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28\uff212147483647nullabcdefghijklmnop123-123A\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28\uff21\u0f21\u0f22\u0f23\u0f24\u0f25\u0f26\u0f27\u0f28\uff212147483647null"})
    public static String concatMixed(String a, char b, int c) {
        return new StringBuilder().append(a).append(b).append(c).append((String)null)
                .append(stringL).append(constInt).append(constIntNeg).append(charL).append(stringU).append(charU)
                .append(a).append(b).append(c).append((String)null).toString();
    }
}
