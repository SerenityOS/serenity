/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4313885 4926319 4927634 5032610 5032622 5049968 5059533 6223711 6277261 6269946 6288823
 *      8072722 8139414 8166261 8172695
 * @summary Basic tests of java.util.Scanner methods
 * @key randomness
 * @modules jdk.localedata
 * @run main/othervm ScanTest
 */

import java.io.*;
import java.math.*;
import java.nio.*;
import java.text.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.*;
import java.util.stream.*;

public class ScanTest {

    private static boolean failure = false;
    private static int failCount = 0;
    private static int NUM_SOURCE_TYPES = 2;
    private static File inputFile = new File(System.getProperty("test.src", "."), "input.txt");

    public static void main(String[] args) throws Exception {
        Locale defaultLocale = Locale.getDefault();
        try {
            // Before we have resource to improve the test to be ready for
            // arbitrary locale, force the default locale to be ROOT for now.
            Locale.setDefault(Locale.US);

            skipTest();
            findInLineTest();
            findWithinHorizonTest();
            findInEmptyLineTest();
            removeTest();
            fromFileTest();
            ioExceptionTest();
            matchTest();
            delimiterTest();
            boundaryDelimTest();
            useLocaleTest();
            closeTest();
            cacheTest();
            cacheTest2();
            nonASCIITest();
            resetTest();
            streamCloseTest();
            streamComodTest();
            outOfRangeRadixTest();

            for (int j = 0; j < NUM_SOURCE_TYPES; j++) {
                hasNextTest(j);
                nextTest(j);
                hasNextPatternTest(j);
                nextPatternTest(j);
                booleanTest(j);
                byteTest(j);
                shortTest(j);
                intTest(j);
                longTest(j);
                floatTest(j);
                doubleTest(j);
                integerPatternTest(j);
                floatPatternTest(j);
                bigIntegerPatternTest(j);
                bigDecimalPatternTest(j);
                hasNextLineTest(j);
                nextLineTest(j);
                singleDelimTest(j);
            }

            // Examples
            //example1();
            //example2();
            //example3();

            // Usage cases
            useCase1();
            useCase2();
            useCase3();
            useCase4();
            useCase5();

            if (failure)
                throw new RuntimeException("Failure in the scanning tests.");
            else
                System.err.println("OKAY: All tests passed.");
        } finally {
            // restore the default locale
            Locale.setDefault(defaultLocale);
        }
    }

    public static void useCase1() throws Exception {
        try (Scanner sc = new Scanner(inputFile)) {
            sc.findWithinHorizon("usage case 1", 0);
            String[] names = new String[4];
            for (int i=0; i<4; i++) {
                while (sc.hasNextFloat())
                    sc.nextFloat();
                names[i] = sc.next();
                sc.nextLine();
            }
            if (!names[0].equals("Frank"))
                failCount++;
            if (!names[1].equals("Joe"))
                failCount++;
            if (!names[2].equals("Mary"))
                failCount++;
            if (!names[3].equals("Michelle"))
                failCount++;
        }
        report("Use case 1");
    }

    public static void useCase2() throws Exception {
        try (Scanner sc = new Scanner(inputFile).useDelimiter("-")) {
            String testDataTag = sc.findWithinHorizon("usage case 2\n", 0);
            if (!testDataTag.equals("usage case 2\n"))
                failCount++;
            if (!sc.next().equals("cat"))
                failCount++;
            if (sc.nextInt() != 9)
                failCount++;
            if (!sc.next().equals("dog"))
                failCount++;
            if (sc.nextInt() != 6)
                failCount++;
            if (!sc.next().equals("pig"))
                failCount++;
            if (sc.nextInt() != 2)
                failCount++;
            if (!sc.next().equals(""))
                failCount++;
            if (sc.nextInt() != 5)
                failCount++;
        }
        report("Use case 2");
    }

    public static void useCase3() throws Exception {
        try (Scanner sc = new Scanner(inputFile)) {
            String testDataTag = sc.findWithinHorizon("usage case 3\n", 0);
            if (!testDataTag.equals("usage case 3\n"))
                failCount++;
            Pattern tagPattern = Pattern.compile("@[a-z]+");
            Pattern endPattern = Pattern.compile("\\*\\/");
            String tag;
            String end = sc.findInLine(endPattern);

            while (end == null) {
                if ((tag = sc.findInLine(tagPattern)) != null) {
                    String text = sc.nextLine();
                    text = text.substring(0, text.length() - 1);
                    //System.out.println(text);
                } else {
                    sc.nextLine();
                }
                end = sc.findInLine(endPattern);
            }
        }
        report("Use case 3");
    }

    public static void useCase4() throws Exception {
        try (Scanner sc = new Scanner(inputFile)) {
            String testDataTag = sc.findWithinHorizon("usage case 4\n", 0);
            if (!testDataTag.equals("usage case 4\n"))
                failCount++;

            // Read some text parts of four hrefs
            String[] expected = { "Diffs", "Sdiffs", "Old", "New" };
            for (int i=0; i<4; i++) {
                sc.findWithinHorizon("<a href", 1000);
                sc.useDelimiter("[<>\n]+");
                sc.next();
                String textOfRef = sc.next();
                if (!textOfRef.equals(expected[i]))
                    failCount++;
            }
            // Read some html tags using < and > as delimiters
            if (!sc.next().equals("/a"))
                failCount++;
            if (!sc.next().equals("b"))
                failCount++;

            // Scan some html tags using skip and next
            Pattern nonTagStart = Pattern.compile("[^<]+");
            Pattern tag = Pattern.compile("<[^>]+?>");
            Pattern spotAfterTag = Pattern.compile("(?<=>)");
            String[] expected2 = { "</b>", "<p>", "<ul>", "<li>" };
            sc.useDelimiter(spotAfterTag);
            int tagsFound = 0;
            while (tagsFound < 4) {
                if (!sc.hasNext(tag)) {
                    // skip text between tags
                    sc.skip(nonTagStart);
                }
                String tagContents = sc.next(tag);
                if (!tagContents.equals(expected2[tagsFound]))
                    failCount++;
                tagsFound++;
            }
        }

        report("Use case 4");
    }

    public static void useCase5() throws Exception {
        try (Scanner sc = new Scanner(inputFile)) {
            String testDataTag = sc.findWithinHorizon("usage case 5\n", 0);
            if (!testDataTag.equals("usage case 5\n"))
                failCount++;

            sc.findWithinHorizon("Share Definitions", 0);
            sc.nextLine();
            sc.next("\\[([a-z]+)\\]");
            String shareName = sc.match().group(1);
            if (!shareName.equals("homes"))
                failCount++;

            String[] keys = { "comment", "browseable", "writable", "valid users" };
            String[] vals = { "Home Directories", "no", "yes", "%S" };
            for (int i=0; i<4; i++) {
                sc.useDelimiter("=");
                String key = sc.next().trim();
                if (!key.equals(keys[i]))
                    failCount++;
                sc.skip("[ =]+");
                sc.useDelimiter("\n");
                String value = sc.next();
                if (!value.equals(vals[i]))
                    failCount++;
                sc.nextLine();
            }
        }

        report("Use case 5");
    }

    public static void nonASCIITest() throws Exception {
        String yourBasicTibetanNumberZero = "\u0f20";
        String yourBasicTibetanFloatingNumber = "\u0f23.\u0f27";
        String weirdMixtureOfTibetanAndASCII = "\u0f23.7";
        String weirdMixtureOfASCIIAndTibetan = "3.\u0f27";
        Scanner sc = new Scanner(yourBasicTibetanNumberZero);
        int i = sc.nextInt();
        if (i != 0)
            failCount++;
        sc = new Scanner(yourBasicTibetanFloatingNumber);
        float f = sc.nextFloat();
        if (f != Float.parseFloat("3.7"))
            failCount++;
        sc = new Scanner(weirdMixtureOfTibetanAndASCII);
        f = sc.nextFloat();
        if (f != Float.parseFloat("3.7"))
            failCount++;
        sc = new Scanner(weirdMixtureOfASCIIAndTibetan);
        f = sc.nextFloat();
        if (f != Float.parseFloat("3.7"))
            failCount++;
        report("Scanning non ASCII digits");
    }

    public static void findWithinHorizonTest() throws Exception {
        // Test with a string source
        Scanner sc = new Scanner("dog  cat     cat    dog     cat");
        try {
            sc.findWithinHorizon("dog", -1);
            failCount++;
        } catch (IllegalArgumentException iae) {
            // Correct result
        }
        if (sc.findWithinHorizon("dog", 2) != null)
            failCount++;
        if (!sc.findWithinHorizon("dog", 3).equals("dog"))
            failCount++;
        if (sc.findWithinHorizon("cat", 4) != null)
            failCount++;
        if (!sc.findWithinHorizon("cat", 5).equals("cat"))
            failCount++;
         if (sc.findWithinHorizon("cat", 7) != null)
            failCount++;
        if (sc.findWithinHorizon("dog", 7) != null)
            failCount++;
        if (!sc.findWithinHorizon("cat", 0).equals("cat"))
            failCount++;
        if (!sc.findWithinHorizon("dog", 0).equals("dog"))
            failCount++;
        if (!sc.findWithinHorizon("cat", 0).equals("cat"))
            failCount++;

        // Test with a stream source
        StutteringInputStream stutter = new StutteringInputStream();
        for (int index=0; index<stutter.length(); index++) {
            //System.out.println("index is now "+index);
            sc = new Scanner(stutter);
            String word = stutter.wordInIndex(index);
            if (word != null) {
                String result = sc.findWithinHorizon(word, index);
                if ((result == null) || (!result.equals(word)))
                    failCount++;
            }
            stutter.reset();
            word = stutter.wordBeyondIndex(index);
            sc = new Scanner(stutter);
            String result = sc.findWithinHorizon(word, index);
            if ((result != null) && (index > 0))
                failCount++;
            stutter.reset();
        }

        // We must loop to let StutteringInputStream do its magic
        for (int j=0; j<10; j++) {
            // An anchor at the end of stream should work
            stutter.reset();
            sc = new Scanner(stutter);
            String result = sc.findWithinHorizon("phant$", 0);
            if (!result.equals("phant"))
                failCount++;
            stutter.reset();
            sc = new Scanner(stutter);
            result = sc.findWithinHorizon("phant$", 54);
            if (!result.equals("phant"))
                failCount++;
            // An anchor at the end of horizon should not
            stutter.reset();
            sc = new Scanner(stutter);
            result = sc.findWithinHorizon("brummer$", 7);
            if (result != null)
                failCount++;
            // An anchor at start should work
            stutter.reset();
            sc = new Scanner(stutter);
            result = sc.findWithinHorizon("^brummer", 0);
            if (!result.equals("brummer"))
                failCount++;
        }

        report("Find to horizon test");
    }

    // StutteringInputStream returns 1 to 3 characters at a time
    static class StutteringInputStream implements Readable {
        StutteringInputStream() {
            text = "brummer hisser tort zardzard rantrant caimagator phant";
            datalen = 54;
        }
        StutteringInputStream(String text) {
            this.text = text;
            datalen = text.length();
        }
        Random generator = new Random();
        String text;
        int datalen;
        int index = 0;
        public int length() {
            return datalen;
        }
        public void reset() {
            index = 0;
        }
        public String wordInIndex(int index) {
            if (index < 7)  return null;
            if (index < 14) return "brummer";
            if (index < 19) return "hisser";
            if (index < 28) return "tort";
            if (index < 37) return "zardzard";
            if (index < 48) return "rantrant";
            return "caimagator";
        }
        public String wordBeyondIndex(int index) {
            if (index < 7)  return "brummer";
            if (index < 14) return "hisser";
            if (index < 19) return "tort";
            if (index < 28) return "zardzard";
            if (index < 37) return "rantrant";
            if (index < 48) return "caimagator";
            return "phantphant";
        }
        public int read(java.nio.CharBuffer target) throws IOException {
            if (index > datalen-1)
                return -1; // EOS
            int len = target.remaining();
            if (len > 4) // return 1 to 3 characters
                len = generator.nextInt(3) + 1;
            while ((index + len) > datalen)
                len--;
            for (int i=0; i<len; i++)
                target.put(text.charAt(index++));
            return len;
        }
    }

    public static void hasNextLineTest(int sourceType) throws Exception {
        Scanner sc = scannerFor("1\n2\n3 3\r\n4 4 4\r5", sourceType);
        if (!sc.hasNextLine()) failCount++;
        if (!sc.nextLine().equals("1")) failCount++;
        if (!sc.hasNextLine()) failCount++;
        if (sc.nextInt() != 2) failCount++;
        if (!sc.hasNextLine()) failCount++;
        if (!sc.nextLine().equals("")) failCount++;
        if (!sc.hasNextLine()) failCount++;
        if (sc.nextInt() != 3) failCount++;
        if (!sc.hasNextLine()) failCount++;
        if (!sc.nextLine().equals(" 3")) failCount++;
        if (!sc.hasNextLine()) failCount++;
        if (sc.nextInt() != 4) failCount++;
        if (!sc.hasNextLine()) failCount++;
        if (sc.nextInt() != 4) failCount++;
        if (!sc.hasNextLine()) failCount++;
        if (!sc.nextLine().equals(" 4")) failCount++;
        if (!sc.hasNextLine()) failCount++;
        if (!sc.nextLine().equals("5")) failCount++;
        if (sc.hasNextLine()) failCount++;
        sc = new Scanner("blah blah blah blah blah blah");
        if (!sc.hasNextLine()) failCount++;
        if (!sc.nextLine().equals("blah blah blah blah blah blah"))
           failCount++;
        if (sc.hasNextLine()) failCount++;

        // Go through all the lines in a file
        try (Scanner sc2 = new Scanner(inputFile)) {
            String lastLine = "blah";
            while (sc2.hasNextLine())
                lastLine = sc2.nextLine();
            if (!lastLine.equals("# Data for usage case 6")) failCount++;
        }

        report("Has next line test");
    }

    public static void nextLineTest(int sourceType) throws Exception {
        Scanner sc = scannerFor("1\n2\n3 3\r\n4 4 4\r5", sourceType);
        if (!sc.nextLine().equals("1"))
            failCount++;
        if (sc.nextInt() != 2)
            failCount++;
        if (!sc.nextLine().equals(""))
           failCount++;
        if (sc.nextInt() != 3)
            failCount++;
        if (!sc.nextLine().equals(" 3"))
           failCount++;
        if (sc.nextInt() != 4)
            failCount++;
        if (sc.nextInt() != 4)
            failCount++;
        if (!sc.nextLine().equals(" 4"))
           failCount++;
        if (!sc.nextLine().equals("5"))
           failCount++;
        sc = new Scanner("blah blah blah blah blah blah");
        if (!sc.nextLine().equals("blah blah blah blah blah blah"))
           failCount++;
        report("Next line test");
    }

    public static void singleDelimTest(int sourceType) throws Exception {
        Scanner sc = scannerFor("12 13  14   15    16     17      ",
                                sourceType);
        sc.useDelimiter(" ");
        for (int i=0; i<6; i++) {
            int j = sc.nextInt();
            if (j != 12 + i)
                failCount++;
            for (int k=0; k<i; k++) {
                String empty = sc.next();
                if (!empty.equals(""))
                    failCount++;
            }
        }
        report("Single delim test");
    }

    private static void append(StringBuilder sb, char c, int n) {
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
    }

    public static void boundaryDelimTest() throws Exception {
        // 8139414
        int i = 1019;
        StringBuilder sb = new StringBuilder();
        sb.append("--;");
        for (int j = 0; j < 1019; ++j) {
            sb.append(j%10);
        }
        sb.append("-;-");
        String text = sb.toString();
        try (Scanner scanner = new Scanner(text)) {
            scanner.useDelimiter("-;(-)?");
            while (scanner.hasNext()) {
                scanner.next();
            }
        } catch (NoSuchElementException e) {
            System.out.println("Caught NoSuchElementException " + e);
            e.printStackTrace();
            failCount++;
        }

        report("delim at boundary test");
    }

    /*
     * The hasNextPattern caches a match of a pattern called the regular cache
     * The hasNextType caches a match of that type called the type cache
     * Any next must clear the caches whether it uses them or not, because
     * it advances past a token thus invalidating any cached token; any
     * hasNext must set a cache to what it finds.
     */
    public static void cacheTest() throws Exception {
        // Test clearing of the type cache
        Scanner scanner = new Scanner("777 dog");
        scanner.hasNextInt();
        scanner.findInLine("777");
        try {
            scanner.nextInt();
            System.out.println("type cache not cleared by find");
            failCount++;
        } catch (InputMismatchException ime) {
            // Correct
        }

        scanner = new Scanner("777 dog");
        scanner.hasNextInt();
        scanner.skip("777");
        try {
            scanner.nextInt();
            System.out.println("type cache not cleared by skip");
            failCount++;
        } catch (InputMismatchException ime) {
            // Correct
        }

        // Test clearing of the regular cache
        scanner = new Scanner("777 dog");
        scanner.hasNext("777");
        scanner.findInLine("777");
        try {
            scanner.next("777");
            System.out.println("regular cache not cleared by find");
            failCount++;
        } catch (InputMismatchException ime) {
            // Correct
        }

        // Test two primitive next clearing of type cache
        scanner = new Scanner("777 dog");
        scanner.hasNextInt();
        scanner.nextLong();
        try {
            scanner.nextInt();
            System.out.println("type cache not cleared by primitive next");
            failCount++;
        } catch (InputMismatchException ime) {
            // Correct
        }

        // Test using both of them, type first
        scanner = new Scanner("777 dog");
        scanner.hasNext("777");
        scanner.nextInt();
        try {
            scanner.next("777");
            System.out.println("regular cache not cleared by primitive next");
            failCount++;
        } catch (InputMismatchException ime) {
            // Correct
        }

        // Test using both of them, regular first
        scanner = new Scanner("777 dog");
        scanner.hasNext("777");
        scanner.hasNextInt();
        scanner.next("777");
        try {
            scanner.nextInt();
            System.out.println("type cache not cleared by regular next");
            failCount++;
        } catch (InputMismatchException ime) {
            // Correct
        }
        report("Cache test");
    }

    /*
     * The hasNext<IntegerType>(radix) method caches a matched integer type
     * with specified radix for the next next<IntegerType>(radix) invoke.
     * The cache value should not be used if the next<IntegerType>(radix)
     * has different radix value with the last hasNext<IntegerType>(radix).
     */
    public static void cacheTest2() throws Exception {
        // Test clearing of the type cache
        Scanner scanner = new Scanner("10");
        scanner.hasNextByte(16);
        if (scanner.nextByte(10) != 10) {
            System.out.println("wrong radix cache is used");
            failCount++;
        }
        scanner = new Scanner("10");
        scanner.hasNextShort(16);
        if (scanner.nextShort(10) != 10) {
            System.out.println("wrong radix cache is used");
            failCount++;
        }
        scanner = new Scanner("10");
        scanner.hasNextInt(16);
        if (scanner.nextInt(10) != 10) {
            System.out.println("wrong radix cache is used");
            failCount++;
        }
        scanner = new Scanner("10");
        scanner.hasNextLong(16);
        if (scanner.nextLong(10) != 10) {
            System.out.println("wrong radix cache is used");
            failCount++;
        }
        scanner = new Scanner("10");
        scanner.hasNextBigInteger(16);
        if (scanner.nextBigInteger(10).intValue() != 10) {
            System.out.println("wrong radix cache is used");
            failCount++;
        }
        report("Cache test2");
    }


    public static void closeTest() throws Exception {
        Scanner sc = new Scanner("testing");
        sc.close();
        sc.ioException();
        sc.delimiter();
        sc.useDelimiter("blah");
        sc.useDelimiter(Pattern.compile("blah"));

        for (Consumer<Scanner> method : methodList) {
            try {
                method.accept(sc);
                failCount++;
            } catch (IllegalStateException ise) {
                // Correct
            }
        }

        report("Close test");
    }

    static List<Consumer<Scanner>> methodList = Arrays.asList(
        Scanner::hasNext,
        Scanner::next,
        sc -> sc.hasNext(Pattern.compile("blah")),
        sc -> sc.next(Pattern.compile("blah")),
        Scanner::hasNextBoolean,
        Scanner::nextBoolean,
        Scanner::hasNextByte,
        Scanner::nextByte,
        Scanner::hasNextShort,
        Scanner::nextShort,
        Scanner::hasNextInt,
        Scanner::nextInt,
        Scanner::hasNextLong,
        Scanner::nextLong,
        Scanner::hasNextFloat,
        Scanner::nextFloat,
        Scanner::hasNextDouble,
        Scanner::nextDouble,
        Scanner::hasNextBigInteger,
        Scanner::nextBigInteger,
        Scanner::hasNextBigDecimal,
        Scanner::nextBigDecimal,
        Scanner::hasNextLine,
        Scanner::tokens,
        sc -> sc.findAll(Pattern.compile("blah")),
        sc -> sc.findAll("blah")
    );

    public static void removeTest() throws Exception {
        Scanner sc = new Scanner("testing");
        try {
            sc.remove();
            failCount++;
        } catch (UnsupportedOperationException uoe) {
            // Correct result
        }
        report("Remove test");
    }

    public static void delimiterTest() throws Exception {
        Scanner sc = new Scanner("blah");
        Pattern test = sc.delimiter();
        if (!test.toString().equals("\\p{javaWhitespace}+"))
            failCount++;
        sc.useDelimiter("a");
        test = sc.delimiter();
        if (!test.toString().equals("a"))
            failCount++;
        sc.useDelimiter(Pattern.compile("b"));
        test = sc.delimiter();
        if (!test.toString().equals("b"))
            failCount++;
        report("Delimiter test");
    }

    public static void ioExceptionTest() throws Exception {
        Readable thrower = new ThrowingReadable();
        Scanner sc = new Scanner(thrower);
        try {
            sc.nextInt();
            failCount++;
        } catch (NoSuchElementException nsee) {
            // Correct result
        }
        Exception thrown = sc.ioException();
        String detail = thrown.getMessage();
        if (!detail.equals("ThrowingReadable always throws"))
            failCount++;

        report("IOException test");
    }

    public static void bigIntegerPatternTest(int sourceType) throws Exception {
        Scanner sc = scannerFor("23 9223372036854775817", sourceType);
        if (!sc.nextBigInteger().equals(BigInteger.valueOf(23)))
            failCount++;
        if (!sc.nextBigInteger().equals(new BigInteger(
            "9223372036854775817", 10)))
            failCount++;

        // Test another radix
        sc = new Scanner("4a4 4A4").useRadix(16);
        if (!sc.nextBigInteger().equals(new BigInteger("4a4", 16)))
            failCount++;
        if (!sc.nextBigInteger().equals(new BigInteger("4A4", 16)))
            failCount++;

        // Test alternating radices
        sc = new Scanner("12 4a4 14 4f4");
        if (!sc.nextBigInteger(10).equals(new BigInteger("12", 10)))
            failCount++;
        if (!sc.nextBigInteger(16).equals(new BigInteger("4a4", 16)))
            failCount++;
        if (!sc.nextBigInteger(10).equals(new BigInteger("14", 10)))
            failCount++;
        if (!sc.nextBigInteger(16).equals(new BigInteger("4f4", 16)))
            failCount++;

        // Test use of a lot of radices
        for (int i=2; i<17; i++) {
            sc = new Scanner("1111");
            if (!sc.nextBigInteger(i).equals(new BigInteger("1111", i)))
                failCount++;
        }

        report("BigInteger pattern");
    }

    public static void bigDecimalPatternTest(int sourceType) throws Exception {
        Scanner sc = scannerFor("23 45.99 -45,067.444 3.4e10", sourceType);
        if (!sc.nextBigDecimal().equals(BigDecimal.valueOf(23)))
            failCount++;
        if (!sc.nextBigDecimal().equals(new BigDecimal("45.99")))
            failCount++;
        if (!sc.nextBigDecimal().equals(new BigDecimal("-45067.444")))
            failCount++;
        if (!sc.nextBigDecimal().equals(new BigDecimal("3.4e10")))
            failCount++;
        report("BigDecimal pattern");
    }

    public static void integerPatternTest(int sourceType) throws Exception {
        String input =
            "1 22 f FF Z -3 -44 123 1,200 -123 -3,400,000 5,40 ,500 ";
        Scanner sc = scannerFor(input, sourceType);
        integerPatternBody(sc);
        CharBuffer cb = CharBuffer.wrap(input);
        sc = new Scanner(cb);
        integerPatternBody(sc);
        report("Integer pattern");
    }

    public static void integerPatternBody(Scanner sc) throws Exception {
        if (sc.nextInt() != 1)        failCount++;
        if (sc.nextShort() != 22)     failCount++;
        if (sc.nextShort(16) != 15)   failCount++;
        if (sc.nextShort(16) != 255)  failCount++;
        if (sc.nextShort(36) != 35)   failCount++;
        if (!sc.hasNextInt())         failCount++;
        if (sc.nextInt() != -3)       failCount++;
        if (sc.nextInt() != -44)      failCount++;
        if (sc.nextLong() != 123)     failCount++;
        if (!sc.hasNextInt())         failCount++;
        if (sc.nextInt() != 1200)     failCount++;
        if (sc.nextInt() != -123)     failCount++;
        if (sc.nextInt() != -3400000) failCount++;
        try {
            sc.nextInt();
            failCount++;
        } catch (InputMismatchException ime) {
            // Correct result
        }
        sc.next();
        try {
            sc.nextLong();
            failCount++;
        } catch (InputMismatchException ime) {
            // Correct result
        }
        sc.next();
        try {
            sc.next();
            failCount++;
        } catch (InputMismatchException ime) {
            failCount++;
        } catch (NoSuchElementException nse) {
            // Correct result
        }
    }

    public static void floatPatternTest(int sourceType) throws Exception {
        String input =
            "090.090 1 22.0 -3 -44.05 +.123 -.1234 -3400000 56,566.6 " +
            "Infinity +Infinity -Infinity NaN -NaN +NaN 5.4.0 5-.00 ++6.07";
        Scanner sc = scannerFor(input, sourceType);
        floatPatternBody(sc);
        CharBuffer cb = CharBuffer.wrap(input);
        sc = new Scanner(cb);
        floatPatternBody(sc);
        report("Float pattern");
    }

    public static void floatPatternBody(Scanner sc) throws Exception {
        if (sc.nextFloat() != 090.090f)                   failCount++;
        if (sc.nextFloat() != 1f)                         failCount++;
        if (sc.nextFloat() != 22.0f)                      failCount++;
        if (sc.nextDouble() != -3d)                       failCount++;
        if (sc.nextDouble() != -44.05d)                   failCount++;
        if (sc.nextFloat() != .123f)                      failCount++;
        if (sc.nextFloat() != -.1234f)                    failCount++;
        if (sc.nextDouble() != -3400000d)                 failCount++;
        if (sc.nextDouble() != 56566.6d)                  failCount++;
        if (sc.nextDouble() != Double.POSITIVE_INFINITY)  failCount++;
        if (sc.nextDouble() != Double.POSITIVE_INFINITY)  failCount++;
        if (sc.nextDouble() != Double.NEGATIVE_INFINITY)  failCount++;
        if (!Double.valueOf(sc.nextDouble()).isNaN())     failCount++;
        if (!Double.valueOf(sc.nextDouble()).isNaN())     failCount++;
        if (!Double.valueOf(sc.nextDouble()).isNaN())     failCount++;
        try {
            sc.nextFloat();
            failCount++;
        } catch (NoSuchElementException nse) {
            // Correct result
        }
        try {
            sc.nextDouble();
            failCount++;
        } catch (NoSuchElementException nse) {
            // Correct result
        }
        try {
            sc.nextDouble();
            failCount++;
        } catch (NoSuchElementException nse) {
            // Correct result
        }
    }

    public static void fromFileTest() throws Exception {
        File f = new File(System.getProperty("test.src", "."), "input.txt");
        try (Scanner sc = new Scanner(f)) {
            sc.useDelimiter("\n+");
            String testDataTag = sc.findWithinHorizon("fromFileTest", 0);
            if (!testDataTag.equals("fromFileTest"))
                failCount++;

            int count = 0;
            while (sc.hasNextLong()) {
                long blah = sc.nextLong();
                count++;
            }
            if (count != 7)
                failCount++;
        }
        report("From file");
    }

    private static void example1() throws Exception {
        Scanner s = new Scanner("1 fish 2 fish red fish blue fish");
        s.useDelimiter("\\s*fish\\s*");
        List <String> results = new ArrayList<String>();
        while (s.hasNext())
            results.add(s.next());
        System.out.println(results);
    }

    private static void example2() throws Exception {
        Scanner s = new Scanner("1 fish 2 fish red fish blue fish");
        s.useDelimiter("\\s*fish\\s*");
        System.out.println(s.nextInt());
        System.out.println(s.nextInt());
        System.out.println(s.next());
        System.out.println(s.next());
    }

    private static void example3() throws Exception {
        Scanner s = new Scanner("1 fish 2 fish red fish blue fish");
        s.findInLine("(\\d+) fish (\\d+) fish (\\w+) fish (\\w+)");
        for (int i=1; i<=s.match().groupCount(); i++)
            System.out.println(s.match().group(i));
    }

    private static void findInLineTest() throws Exception {
        Scanner s = new Scanner("abc def ghi jkl mno");
        Pattern letters = Pattern.compile("[a-z]+");
        Pattern frogs = Pattern.compile("frogs");
        String str = s.findInLine(letters);
        if (!str.equals("abc"))
            failCount++;
        if (!s.hasNext(letters))
            failCount++;
        try {
            str = s.findInLine(frogs);
        } catch (NoSuchElementException nsee) {
            // Correct
        }
        if (!s.hasNext())
            failCount++;
        if (!s.hasNext(letters))
            failCount++;
        str = s.findInLine(letters);
        if (!str.equals("def"))
            failCount++;

        report("Find patterns");
    }

    private static void findInEmptyLineTest() throws Exception {
        String eol = System.getProperty("line.separator");
        Scanner s = new Scanner("line 1" + eol + "" + eol + "line 3" + eol);
        int lineNo = 0;
        while (s.hasNextLine()) {
            lineNo++;
            s.findInLine("3");
            s.nextLine();
        }
        if (lineNo != 3)
            failCount++;
        report("findInEmptyLine test");
    }

    private static void matchTest() throws Exception {
        Scanner s = new Scanner("1 fish 2 fish red fish blue fish");
        s.findInLine("(\\d+) fish (\\d+) fish (\\w+) fish (\\w+)");

        MatchResult result = s.match();
        if (!result.group(1).equals("1"))
            failCount++;
        if (!result.group(2).equals("2"))
            failCount++;
        if (!result.group(3).equals("red"))
            failCount++;
        if (!result.group(4).equals("blue"))
            failCount++;

        report("Match patterns");
    }

    private static void skipTest() throws Exception {
        Scanner s = new Scanner("abc def ghi jkl mno");
        Pattern letters = Pattern.compile("[a-z]+");
        Pattern spaceLetters = Pattern.compile(" [a-z]+");
        Pattern frogs = Pattern.compile("frogs");
        try {
            s.skip(letters);
        } catch (NoSuchElementException ime) {
            failCount++;
        }
        String token = s.next(letters);
        if (!token.equals("def")) {
            System.out.println("expected def");
            System.out.println("I found "+token);
            failCount++;
        }
        try {
            s.skip(letters);
            failCount++;
        } catch (NoSuchElementException ime) {
            // Correct result
        }
        token = s.next(letters);
        if (!token.equals("ghi")) {
            System.out.println("expected ghi");
            System.out.println("I found "+token);
            failCount++;
        }
        try {
            s.skip(letters);
            failCount++;
        } catch (NoSuchElementException ime) {
            // Correct result because skip ignores delims
        }
        try {
            s.skip(spaceLetters);
        } catch (NoSuchElementException ime) {
            failCount++;
        }
        token = s.next(letters);
        if (!token.equals("mno")) {
            System.out.println("expected mno");
            System.out.println("I found "+token);
            failCount++;
        }
        try {
            s.skip(letters);
            failCount++;
        } catch (NoSuchElementException ime) {
            // Correct result
        }
        report("Skip patterns");
    }

    private static void byteTest(int sourceType) throws Exception {
        String input = " 3 0 00 b -B 012 44 -55 12 127 129 -131 dog 0x12";
        Scanner s = scannerFor(input, sourceType);
        if (!s.hasNextByte())          failCount++;
        if (s.nextByte() != (byte)3)   failCount++;
        if (!s.hasNextByte())          failCount++;
        if (s.nextByte() != (byte)0)   failCount++;
        if (!s.hasNextByte())          failCount++;
        if (s.nextByte() != (byte)0)   failCount++;
        if (!s.hasNextByte(16))        failCount++;
        if (s.nextByte(16) != (byte)11)failCount++;
        if (!s.hasNextByte(16))        failCount++;
        if (s.nextByte(16) != (byte)-11) failCount++;
        if (!s.hasNextByte())          failCount++;
        if (s.nextByte() != (byte)12)  failCount++;
        if (!s.hasNextByte())          failCount++;
        if (s.nextByte() != (byte)44)  failCount++;
        if (!s.hasNextByte())          failCount++;
        if (s.nextByte() != (byte)-55) failCount++;
        if (!s.hasNextByte())          failCount++;
        if (s.nextByte() != (byte)12)  failCount++;
        if (!s.hasNextByte())          failCount++;
        if (s.nextByte() != (byte)127) failCount++;
        if (s.hasNextByte())           failCount++;

        try {
            s.nextByte();
            failCount++;
        } catch (InputMismatchException ime) {
            // Correct result
        }
        if (s.hasNextByte())           failCount++;
        if (s.nextInt() != 129)        failCount++;
        if (s.hasNextByte())           failCount++;
        try {
            s.nextByte();
            failCount++;
        } catch (InputMismatchException ime) {
            // Correct result
        }
        if (s.nextInt() != -131)       failCount++;
        if (s.hasNextByte())           failCount++;
        try {
            s.nextByte();
            failCount++;
        } catch (InputMismatchException ime) {
            // Correct result
        }
        s.next(Pattern.compile("\\w+"));
        if (s.hasNextByte())
            failCount++;
        try {
            s.nextByte();
            failCount++;
        } catch (NoSuchElementException nsee) {
            // Correct result
        }
        s.next();
        if (s.hasNextByte())
            failCount++;
        try {
            byte bb = s.nextByte();
            failCount++;
        } catch (NoSuchElementException nsee) {
            // Correct result
        }
        report("Scan bytes");
    }

    private static void shortTest(int sourceType) throws Exception {
        String input = "  017 22 00E -34 44,333 -53999 0x19 dog";
        Scanner s = scannerFor(input, sourceType);
        if (!s.hasNextShort())             failCount++;
        if (s.nextShort() != (short)17)   failCount++;
        if (!s.hasNextShort())            failCount++;
        if (s.nextShort() != (short)22)   failCount++;
        if (!s.hasNextShort(16))          failCount++;
        if (s.nextShort(16) != (short)14) failCount++;
        if (!s.hasNextShort())            failCount++;
        if (s.nextShort() != (short)-34)  failCount++;
        for (int i=0; i<4; i++) {
            if (s.hasNextShort())
                failCount++;
            try {
                s.nextShort();
                failCount++;
            } catch (InputMismatchException ime) {
                // Correct result
            }
            s.next();
        }
        try {
            s.next();
            failCount++;
        } catch (InputMismatchException ime) {
            failCount++;
        } catch (NoSuchElementException nse) {
            // Correct result
        }
        report("Scan shorts");
    }

    private static void intTest(int sourceType) throws Exception {
        Scanner s = scannerFor(
            "22 022 C -34 0x80000000 -2147483649 dog ", sourceType);
        if (!s.hasNextInt())      failCount++;
        if (s.nextInt() != 22)    failCount++;
        if (!s.hasNextInt())      failCount++;
        if (s.nextInt() != 22)    failCount++;
        if (!s.hasNextInt(16))    failCount++;
        if (s.nextInt(16) != 12)  failCount++;
        if (!s.hasNextInt())      failCount++;
        if (s.nextInt() != -34)   failCount++;
        for (int i=0; i<3; i++) {
            if (s.hasNextInt())
                failCount++;
            try {
                s.nextInt();
                failCount++;
            } catch (InputMismatchException ime) {
                // Correct result
            }
            s.next();
        }
        try {
            s.next();
            failCount++;
        } catch (InputMismatchException ime) {
            failCount++;
        } catch (NoSuchElementException nse) {
            // Correct result
        }
        report("Scan ints");
    }

    private static void longTest(int sourceType) throws Exception {
        Scanner s = scannerFor(
        "022 9223372036854775807 0x8000000000000000 9223372036854775808 dog ",
              sourceType);
        if (!s.hasNextLong())                        failCount++;
        if (s.nextLong() != (long)22)                failCount++;
        if (!s.hasNextLong())                        failCount++;
        if (s.nextLong() != 9223372036854775807L)    failCount++;
        for (int i=0; i<3; i++) {
            if (s.hasNextLong())
                failCount++;
            try {
                s.nextLong();
                failCount++;
            } catch (InputMismatchException ime) {
                // Correct result
            }
            s.next();
        }
        try {
            s.next();
            failCount++;
        } catch (InputMismatchException ime) {
            failCount++;
        } catch (NoSuchElementException nse) {
            // Correct result
        }
        report("Scan longs");
    }

    private static void floatTest(int sourceType) throws Exception {
        Scanner s = scannerFor(
            "0 0. 0.0 2 2. 2.0 2.3 -2 -2.0 -2.3 -. 2-. 2..3", sourceType);
        if (!s.hasNextFloat())      failCount++;
        if (s.nextFloat() != 0f)    failCount++;
        if (!s.hasNextFloat())      failCount++;
        if (s.nextFloat() != 0f)    failCount++;
        if (!s.hasNextFloat())      failCount++;
        if (s.nextFloat() != 0f)    failCount++;
        if (!s.hasNextFloat())      failCount++;
        if (s.nextFloat() != 2f)    failCount++;
        if (!s.hasNextFloat())      failCount++;
        if (s.nextFloat() != 2f)    failCount++;
        if (!s.hasNextFloat())      failCount++;
        if (s.nextFloat() != 2f)    failCount++;
        if (!s.hasNextFloat())      failCount++;
        if (s.nextFloat() != 2.3f)  failCount++;
        if (!s.hasNextFloat())      failCount++;
        if (s.nextFloat() != -2f)   failCount++;
        if (!s.hasNextFloat())      failCount++;
        if (s.nextFloat() != -2f)   failCount++;
        if (!s.hasNextFloat())      failCount++;
        if (s.nextFloat() != -2.3f) failCount++;
        for (int i=0; i<3; i++) {
            if (s.hasNextLong())
                failCount++;
            try {
                s.nextFloat();
                failCount++;
            } catch (InputMismatchException ime) {
                // Correct result
            }
            s.next();
        }
        try {
            s.next();
            failCount++;
        } catch (InputMismatchException ime) {
            failCount++;
        } catch (NoSuchElementException nse) {
            // Correct result
        }
        report("Scan floats");
    }

    private static void doubleTest(int sourceType) throws Exception {
        Scanner s = scannerFor(
            "0 0. 0.0 2 2. 2.0 2.3 -2 -2.0 -2.3 -. 2-. 2..3", sourceType);
        if (!s.hasNextDouble())             failCount++;
        if (s.nextDouble() != 0d)           failCount++;
        if (!s.hasNextDouble())             failCount++;
        if (s.nextDouble() != 0d)           failCount++;
        if (!s.hasNextDouble())             failCount++;
        if (s.nextDouble() != 0d)           failCount++;
        if (!s.hasNextDouble())             failCount++;
        if (s.nextDouble() != 2d)           failCount++;
        if (!s.hasNextDouble())             failCount++;
        if (s.nextDouble() != 2d)           failCount++;
        if (!s.hasNextDouble())             failCount++;
        if (s.nextDouble() != 2d)           failCount++;
        if (!s.hasNextDouble())             failCount++;
        if (s.nextDouble() != 2.3d)         failCount++;
        if (!s.hasNextDouble())             failCount++;
        if (s.nextDouble() != -2d)          failCount++;
        if (!s.hasNextDouble())             failCount++;
        if (s.nextDouble() != -2d)          failCount++;
        if (!s.hasNextDouble())             failCount++;
        if (s.nextDouble() != -2.3d)        failCount++;
        for (int i=0; i<3; i++) {
            if (s.hasNextLong())
                failCount++;
            try {
                s.nextDouble();
                failCount++;
            } catch (InputMismatchException ime) {
                // Correct result
            }
            s.next();
        }
        try {
            s.next();
            failCount++;
        } catch (InputMismatchException ime) {
            failCount++;
        } catch (NoSuchElementException nse) {
            // Correct result
        }
        report("Scan doubles");
    }

    private static void booleanTest(int sourceType) throws Exception {
        Scanner s = scannerFor(
            " true false\t \r\n true FaLse \n  True Tru", sourceType);
        if (!s.nextBoolean())     failCount++;
        if (!s.hasNextBoolean())  failCount++;
        if (s.nextBoolean())      failCount++;
        if (!s.nextBoolean())     failCount++;
        if (s.nextBoolean())      failCount++;
        if (!s.nextBoolean())     failCount++;
        if (s.hasNextBoolean())   failCount++;
        try {
            s.nextBoolean();
            failCount++;
        } catch (NoSuchElementException nsee) {
            // Expected result
        }
        report("Scan booleans");
    }

    private static void hasNextTest(int sourceType) throws Exception {
        Scanner s = scannerFor(
            " blah blech\t blather  alongblatherindeed", sourceType);
        if (!s.hasNext())            failCount++;
        if (!s.hasNext())            failCount++;
        String result = s.next();
        if (!result.equals("blah"))  failCount++;
        if (!s.hasNext())            failCount++;
        if (!s.hasNext())            failCount++;
        result = s.next();
        if (!result.equals("blech")) failCount++;
        if (!s.hasNext())            failCount++;
        result = s.next();
        if (!result.equals("blather")) failCount++;
        if (!s.hasNext())              failCount++;
        if (!s.hasNext())              failCount++;
        result = s.next();
        if (!result.equals("alongblatherindeed")) failCount++;
        if (s.hasNext())                          failCount++;
        try {
            result = s.next();
            failCount++;
        } catch (NoSuchElementException nsee) {
            // Correct result
        }
        report("Has next test");
    }

    private static void nextTest(int sourceType) throws Exception {
        Scanner s = scannerFor(
            " blah blech\t blather  alongblatherindeed", sourceType);
        String result = (String)s.next();
        if (!result.equals("blah"))    failCount++;
        result = (String)s.next();
        if (!result.equals("blech"))   failCount++;
        result = (String)s.next();
        if (!result.equals("blather")) failCount++;
        result = (String)s.next();
        if (!result.equals("alongblatherindeed"))
            failCount++;
        try {
            result = (String)s.next();
            failCount++;
        } catch (NoSuchElementException nsee) {
            // Correct result
        }
        report("Next test");
    }

    private static void hasNextPatternTest(int sourceType) throws Exception {
        Scanner s = scannerFor(
            " blah blech\t blather  alongblatherindeed", sourceType);
        Pattern p1 = Pattern.compile("\\w+");
        Pattern p2 = Pattern.compile("blech");
        if (!s.hasNext(p1))    failCount++;
        if (!s.hasNext(p1))    failCount++;
        if (s.hasNext(p2))     failCount++;
        String result = (String)s.next();
        if (!result.equals("blah"))  failCount++;
        if (!s.hasNext(p1))          failCount++;
        if (!s.hasNext(p2))          failCount++;
        result = (String)s.next();
        if (!result.equals("blech")) failCount++;
        if (!s.hasNext(p1))          failCount++;
        if (s.hasNext(p2))           failCount++;
        result = (String)s.next();
        if (!result.equals("blather")) failCount++;
        if (!s.hasNext(p1))            failCount++;
        if (s.hasNext(p2))             failCount++;
        result = (String)s.next();
        if (!result.equals("alongblatherindeed")) failCount++;
        if (s.hasNext(p1))  failCount++;
        if (s.hasNext(p2))  failCount++;
        report("Has Next Pattern test");
    }

    private static void nextPatternTest(int sourceType) throws Exception {
        Scanner s = scannerFor(
            " blah blech\t blather  alongblatherindeed", sourceType);
        Pattern p1 = Pattern.compile("blah");
        Pattern p2 = Pattern.compile("blech");
        Pattern p3 = Pattern.compile("blather");
        Pattern p4 = Pattern.compile("alongblatherindeed");
        String result = null;
        try {
            result = (String)s.next(p2);
            failCount++;
        } catch (NoSuchElementException nsee) {
            // Correct result
        }
        result = (String)s.next(p1);
        if (!result.equals("blah"))
            failCount++;
        try {
            result = (String)s.next(p1);
            failCount++;
        } catch (NoSuchElementException nsee) {
            // Correct result
        }
        result = (String)s.next(p2);
        if (!result.equals("blech"))
            failCount++;
        try {
            result = (String)s.next(p4);
            failCount++;
        } catch (NoSuchElementException nsee) {
            // Correct result
        }
        result = (String)s.next(p3);
        if (!result.equals("blather"))
            failCount++;
        try {
            result = (String)s.next(p3);
            failCount++;
        } catch (NoSuchElementException nsee) {
            // Correct result
        }
        result = (String)s.next(p4);
        if (!result.equals("alongblatherindeed"))
            failCount++;
        try {
            result = (String)s.next();
            failCount++;
        } catch (NoSuchElementException nsee) {
            // Correct result
        }
        report("Next pattern test");
    }

    private static void useLocaleTest() throws Exception {
        Scanner s = new Scanner("334.65").useLocale(Locale.ENGLISH);
        if (!s.hasNextFloat())           failCount++;
        if (s.nextFloat() != 334.65f)    failCount++;

        s = new Scanner("334,65").useLocale(Locale.FRENCH);
        if (!s.hasNextFloat())           failCount++;
        if (s.nextFloat() != 334.65f)    failCount++;

        s = new Scanner("4.334,65").useLocale(Locale.GERMAN);
        if (!s.hasNextFloat())           failCount++;
        if (s.nextFloat() != 4334.65f)    failCount++;

        // Test case reported from India
        try {
            String Message = "123978.90 $";
            Locale locale = new Locale("hi","IN");
            NumberFormat form = NumberFormat.getInstance(locale);
            double myNumber = 1902.09;
            Scanner scanner = new Scanner(form.format(myNumber).toString());
            scanner.useLocale(locale);
            double d = scanner.nextDouble();
        } catch (InputMismatchException ime) {
            failCount++;
        }
        report("Use locale test");
    }

    public static void resetTest() throws Exception {
        Scanner sc = new Scanner("");
        int radix = sc.radix();
        Locale locale = sc.locale();
        Pattern delimiter = sc.delimiter();
        Pattern a = Pattern.compile("A");
        sc.useDelimiter(a);
        Locale dummy = new Locale("en", "US", "dummy");
        sc.useLocale(dummy);
        sc.useRadix(16);
        if (sc.radix() != 16 ||
            !sc.locale().equals(dummy) ||
            !sc.delimiter().pattern().equals(a.pattern())) {
            failCount++;
        } else {
            sc.reset();
            if (sc.radix() != radix ||
                !sc.locale().equals(locale) ||
                !sc.delimiter().pattern().equals(delimiter.pattern())) {
                failCount++;
            }
        }
        sc.close();
        report("Reset test");
    }

    static List<BiConsumer <Scanner, Integer>> methodWRList = Arrays.asList(
        (s, r) -> s.hasNextByte(r),
        (s, r) -> s.nextByte(r),
        (s, r) -> s.hasNextShort(r),
        (s, r) -> s.nextShort(r),
        (s, r) -> s.hasNextInt(r),
        (s, r) -> s.nextInt(r),
        (s, r) -> s.hasNextLong(r),
        (s, r) -> s.nextLong(r),
        (s, r) -> s.hasNextBigInteger(r),
        (s, r) -> s.nextBigInteger(r)
    );

    /*
     * Test that setting the radix to an out of range value triggers
     * an IllegalArgumentException
     */
    public static void outOfRangeRadixTest() throws Exception {
        int[] bad = new int[] { -1, 0,  1, 37, 38 };
        int[] good = IntStream.rangeClosed(Character.MIN_RADIX, Character.MAX_RADIX)
                              .toArray();

        methodWRList.stream().forEach( m -> {
            for (int r : bad) {
                try (Scanner sc = new Scanner("10 10 10 10")) {
                    m.accept(sc, r);
                    failCount++;
                } catch (IllegalArgumentException ise) {}
            }
        });
        methodWRList.stream().forEach( m -> {
            for (int r : good) {
                try (Scanner sc = new Scanner("10 10 10 10")) {
                    m.accept(sc, r);
                } catch (Exception x) {
                    failCount++;
                }
            }
        });
        report("Radix out of range test");
    }

    /*
     * Test that closing the stream also closes the underlying Scanner.
     * The cases of attempting to open streams on a closed Scanner are
     * covered by closeTest().
     */
    public static void streamCloseTest() throws Exception {
        Scanner sc;

        Scanner sc1 = new Scanner("xyzzy");
        sc1.tokens().close();
        try {
            sc1.hasNext();
            failCount++;
        } catch (IllegalStateException ise) {
            // Correct result
        }

        Scanner sc2 = new Scanner("a b c d e f");
        try {
            sc2.tokens()
               .peek(s -> sc2.close())
               .count();
        } catch (IllegalStateException ise) {
            // Correct result
        }

        Scanner sc3 = new Scanner("xyzzy");
        sc3.findAll("q").close();
        try {
            sc3.hasNext();
            failCount++;
        } catch (IllegalStateException ise) {
            // Correct result
        }

        try (Scanner sc4 = new Scanner(inputFile)) {
            sc4.findAll("[0-9]+")
               .peek(s -> sc4.close())
               .count();
            failCount++;
        } catch (IllegalStateException ise) {
            // Correct result
        }

        report("Streams Close test");
    }

    /*
     * Test ConcurrentModificationException
     */
    public static void streamComodTest() {
        try {
            Scanner sc = new Scanner("a b c d e f");
            sc.tokens()
              .peek(s -> sc.hasNext())
              .count();
            failCount++;
        } catch (ConcurrentModificationException cme) {
            // Correct result
        }

        try {
            Scanner sc = new Scanner("a b c d e f");
            Iterator<String> it = sc.tokens().iterator();
            it.next();
            sc.next();
            it.next();
            failCount++;
        } catch (ConcurrentModificationException cme) {
            // Correct result
        }

        try {
            String input = IntStream.range(0, 100)
                                    .mapToObj(String::valueOf)
                                    .collect(Collectors.joining(" "));
            Scanner sc = new Scanner(input);
            sc.findAll("[0-9]+")
              .peek(s -> sc.hasNext())
              .count();
            failCount++;
        } catch (ConcurrentModificationException cme) {
            // Correct result
        }

        try {
            String input = IntStream.range(0, 100)
                                    .mapToObj(String::valueOf)
                                    .collect(Collectors.joining(" "));
            Scanner sc = new Scanner(input);
            Iterator<MatchResult> it = sc.findAll("[0-9]+").iterator();
            it.next();
            sc.next();
            it.next();
            failCount++;
        } catch (ConcurrentModificationException cme) {
            // Correct result
        }

        report("Streams Comod test");
    }

    private static void report(String testName) {
        System.err.printf("%-30s: %s%n", testName,
                          (failCount == 0) ? "Passed" : String.format("Failed(%d)", failCount));

        if (failCount > 0)
            failure = true;
        failCount = 0;
    }

    static Scanner scannerFor(String input, int sourceType) {
        if (sourceType == 1)
            return new Scanner(input);
        else
            return new Scanner(new StutteringInputStream(input));
    }

    static class ThrowingReadable implements Readable {
        ThrowingReadable() {
        }
        public int read(java.nio.CharBuffer cb) throws IOException {
            throw new IOException("ThrowingReadable always throws");
        }
    }
}
