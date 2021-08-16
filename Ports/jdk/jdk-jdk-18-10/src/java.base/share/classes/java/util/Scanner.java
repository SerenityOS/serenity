/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.math.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.text.*;
import java.text.spi.NumberFormatProvider;
import java.util.function.Consumer;
import java.util.regex.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.ResourceBundleBasedAdapter;

/**
 * A simple text scanner which can parse primitive types and strings using
 * regular expressions.
 *
 * <p>A {@code Scanner} breaks its input into tokens using a
 * delimiter pattern, which by default matches whitespace. The resulting
 * tokens may then be converted into values of different types using the
 * various {@code next} methods.
 *
 * <p>For example, this code allows a user to read a number from
 * {@code System.in}:
 * <blockquote><pre>{@code
 *     Scanner sc = new Scanner(System.in);
 *     int i = sc.nextInt();
 * }</pre></blockquote>
 *
 * <p>As another example, this code allows {@code long} types to be
 * assigned from entries in a file {@code myNumbers}:
 * <blockquote><pre>{@code
 *      Scanner sc = new Scanner(new File("myNumbers"));
 *      while (sc.hasNextLong()) {
 *          long aLong = sc.nextLong();
 *      }
 * }</pre></blockquote>
 *
 * <p>The scanner can also use delimiters other than whitespace. This
 * example reads several items in from a string:
 * <blockquote><pre>{@code
 *     String input = "1 fish 2 fish red fish blue fish";
 *     Scanner s = new Scanner(input).useDelimiter("\\s*fish\\s*");
 *     System.out.println(s.nextInt());
 *     System.out.println(s.nextInt());
 *     System.out.println(s.next());
 *     System.out.println(s.next());
 *     s.close();
 * }</pre></blockquote>
 * <p>
 * prints the following output:
 * <blockquote><pre>{@code
 *     1
 *     2
 *     red
 *     blue
 * }</pre></blockquote>
 *
 * <p>The same output can be generated with this code, which uses a regular
 * expression to parse all four tokens at once:
 * <blockquote><pre>{@code
 *     String input = "1 fish 2 fish red fish blue fish";
 *     Scanner s = new Scanner(input);
 *     s.findInLine("(\\d+) fish (\\d+) fish (\\w+) fish (\\w+)");
 *     MatchResult result = s.match();
 *     for (int i=1; i<=result.groupCount(); i++)
 *         System.out.println(result.group(i));
 *     s.close();
 * }</pre></blockquote>
 *
 * <p>The <a id="default-delimiter">default whitespace delimiter</a> used
 * by a scanner is as recognized by {@link Character#isWhitespace(char)
 * Character.isWhitespace()}. The {@link #reset reset()}
 * method will reset the value of the scanner's delimiter to the default
 * whitespace delimiter regardless of whether it was previously changed.
 *
 * <p>A scanning operation may block waiting for input.
 *
 * <p>The {@link #next} and {@link #hasNext} methods and their
 * companion methods (such as {@link #nextInt} and
 * {@link #hasNextInt}) first skip any input that matches the delimiter
 * pattern, and then attempt to return the next token. Both {@code hasNext()}
 * and {@code next()} methods may block waiting for further input.  Whether a
 * {@code hasNext()} method blocks has no connection to whether or not its
 * associated {@code next()} method will block. The {@link #tokens} method
 * may also block waiting for input.
 *
 * <p>The {@link #findInLine findInLine()},
 * {@link #findWithinHorizon findWithinHorizon()},
 * {@link #skip skip()}, and {@link #findAll findAll()}
 * methods operate independently of the delimiter pattern. These methods will
 * attempt to match the specified pattern with no regard to delimiters in the
 * input and thus can be used in special circumstances where delimiters are
 * not relevant. These methods may block waiting for more input.
 *
 * <p>When a scanner throws an {@link InputMismatchException}, the scanner
 * will not pass the token that caused the exception, so that it may be
 * retrieved or skipped via some other method.
 *
 * <p>Depending upon the type of delimiting pattern, empty tokens may be
 * returned. For example, the pattern {@code "\\s+"} will return no empty
 * tokens since it matches multiple instances of the delimiter. The delimiting
 * pattern {@code "\\s"} could return empty tokens since it only passes one
 * space at a time.
 *
 * <p> A scanner can read text from any object which implements the {@link
 * java.lang.Readable} interface.  If an invocation of the underlying
 * readable's {@link java.lang.Readable#read read()} method throws an {@link
 * java.io.IOException} then the scanner assumes that the end of the input
 * has been reached.  The most recent {@code IOException} thrown by the
 * underlying readable can be retrieved via the {@link #ioException} method.
 *
 * <p>When a {@code Scanner} is closed, it will close its input source
 * if the source implements the {@link java.io.Closeable} interface.
 *
 * <p>A {@code Scanner} is not safe for multithreaded use without
 * external synchronization.
 *
 * <p>Unless otherwise mentioned, passing a {@code null} parameter into
 * any method of a {@code Scanner} will cause a
 * {@code NullPointerException} to be thrown.
 *
 * <p>A scanner will default to interpreting numbers as decimal unless a
 * different radix has been set by using the {@link #useRadix} method. The
 * {@link #reset} method will reset the value of the scanner's radix to
 * {@code 10} regardless of whether it was previously changed.
 *
 * <h2> <a id="localized-numbers">Localized numbers</a> </h2>
 *
 * <p> An instance of this class is capable of scanning numbers in the standard
 * formats as well as in the formats of the scanner's locale. A scanner's
 * <a id="initial-locale">initial locale </a>is the value returned by the {@link
 * java.util.Locale#getDefault(Locale.Category)
 * Locale.getDefault(Locale.Category.FORMAT)} method; it may be changed via the {@link
 * #useLocale useLocale()} method. The {@link #reset} method will reset the value of the
 * scanner's locale to the initial locale regardless of whether it was
 * previously changed.
 *
 * <p>The localized formats are defined in terms of the following parameters,
 * which for a particular locale are taken from that locale's {@link
 * java.text.DecimalFormat DecimalFormat} object, {@code df}, and its and
 * {@link java.text.DecimalFormatSymbols DecimalFormatSymbols} object,
 * {@code dfs}.
 *
 * <blockquote><dl>
 *     <dt><i>LocalGroupSeparator&nbsp;&nbsp;</i>
 *         <dd>The character used to separate thousands groups,
 *         <i>i.e.,</i>&nbsp;{@code dfs.}{@link
 *         java.text.DecimalFormatSymbols#getGroupingSeparator
 *         getGroupingSeparator()}
 *     <dt><i>LocalDecimalSeparator&nbsp;&nbsp;</i>
 *         <dd>The character used for the decimal point,
 *     <i>i.e.,</i>&nbsp;{@code dfs.}{@link
 *     java.text.DecimalFormatSymbols#getDecimalSeparator
 *     getDecimalSeparator()}
 *     <dt><i>LocalPositivePrefix&nbsp;&nbsp;</i>
 *         <dd>The string that appears before a positive number (may
 *         be empty), <i>i.e.,</i>&nbsp;{@code df.}{@link
 *         java.text.DecimalFormat#getPositivePrefix
 *         getPositivePrefix()}
 *     <dt><i>LocalPositiveSuffix&nbsp;&nbsp;</i>
 *         <dd>The string that appears after a positive number (may be
 *         empty), <i>i.e.,</i>&nbsp;{@code df.}{@link
 *         java.text.DecimalFormat#getPositiveSuffix
 *         getPositiveSuffix()}
 *     <dt><i>LocalNegativePrefix&nbsp;&nbsp;</i>
 *         <dd>The string that appears before a negative number (may
 *         be empty), <i>i.e.,</i>&nbsp;{@code df.}{@link
 *         java.text.DecimalFormat#getNegativePrefix
 *         getNegativePrefix()}
 *     <dt><i>LocalNegativeSuffix&nbsp;&nbsp;</i>
 *         <dd>The string that appears after a negative number (may be
 *         empty), <i>i.e.,</i>&nbsp;{@code df.}{@link
 *     java.text.DecimalFormat#getNegativeSuffix
 *     getNegativeSuffix()}
 *     <dt><i>LocalNaN&nbsp;&nbsp;</i>
 *         <dd>The string that represents not-a-number for
 *         floating-point values,
 *         <i>i.e.,</i>&nbsp;{@code dfs.}{@link
 *         java.text.DecimalFormatSymbols#getNaN
 *         getNaN()}
 *     <dt><i>LocalInfinity&nbsp;&nbsp;</i>
 *         <dd>The string that represents infinity for floating-point
 *         values, <i>i.e.,</i>&nbsp;{@code dfs.}{@link
 *         java.text.DecimalFormatSymbols#getInfinity
 *         getInfinity()}
 * </dl></blockquote>
 *
 * <h3> <a id="number-syntax">Number syntax</a> </h3>
 *
 * <p> The strings that can be parsed as numbers by an instance of this class
 * are specified in terms of the following regular-expression grammar, where
 * Rmax is the highest digit in the radix being used (for example, Rmax is 9 in base 10).
 *
 * <dl>
 *   <dt><i>NonAsciiDigit</i>:
 *       <dd>A non-ASCII character c for which
 *            {@link java.lang.Character#isDigit Character.isDigit}{@code (c)}
 *                        returns&nbsp;true
 *
 *   <dt><i>Non0Digit</i>:
 *       <dd>{@code [1-}<i>Rmax</i>{@code ] | }<i>NonASCIIDigit</i>
 *
 *   <dt><i>Digit</i>:
 *       <dd>{@code [0-}<i>Rmax</i>{@code ] | }<i>NonASCIIDigit</i>
 *
 *   <dt><i>GroupedNumeral</i>:
 *       <dd><code>(&nbsp;</code><i>Non0Digit</i>
 *                   <i>Digit</i>{@code ?
 *                   }<i>Digit</i>{@code ?}
 *       <dd>&nbsp;&nbsp;&nbsp;&nbsp;<code>(&nbsp;</code><i>LocalGroupSeparator</i>
 *                         <i>Digit</i>
 *                         <i>Digit</i>
 *                         <i>Digit</i>{@code  )+ )}
 *
 *   <dt><i>Numeral</i>:
 *       <dd>{@code ( ( }<i>Digit</i>{@code + )
 *               | }<i>GroupedNumeral</i>{@code  )}
 *
 *   <dt><a id="Integer-regex"><i>Integer</i>:</a>
 *       <dd>{@code ( [-+]? ( }<i>Numeral</i>{@code
 *                               ) )}
 *       <dd>{@code | }<i>LocalPositivePrefix</i> <i>Numeral</i>
 *                      <i>LocalPositiveSuffix</i>
 *       <dd>{@code | }<i>LocalNegativePrefix</i> <i>Numeral</i>
 *                 <i>LocalNegativeSuffix</i>
 *
 *   <dt><i>DecimalNumeral</i>:
 *       <dd><i>Numeral</i>
 *       <dd>{@code | }<i>Numeral</i>
 *                 <i>LocalDecimalSeparator</i>
 *                 <i>Digit</i>{@code *}
 *       <dd>{@code | }<i>LocalDecimalSeparator</i>
 *                 <i>Digit</i>{@code +}
 *
 *   <dt><i>Exponent</i>:
 *       <dd>{@code ( [eE] [+-]? }<i>Digit</i>{@code + )}
 *
 *   <dt><a id="Decimal-regex"><i>Decimal</i>:</a>
 *       <dd>{@code ( [-+]? }<i>DecimalNumeral</i>
 *                         <i>Exponent</i>{@code ? )}
 *       <dd>{@code | }<i>LocalPositivePrefix</i>
 *                 <i>DecimalNumeral</i>
 *                 <i>LocalPositiveSuffix</i>
 *                 <i>Exponent</i>{@code ?}
 *       <dd>{@code | }<i>LocalNegativePrefix</i>
 *                 <i>DecimalNumeral</i>
 *                 <i>LocalNegativeSuffix</i>
 *                 <i>Exponent</i>{@code ?}
 *
 *   <dt><i>HexFloat</i>:
 *       <dd>{@code [-+]? 0[xX][0-9a-fA-F]*\.[0-9a-fA-F]+
 *                 ([pP][-+]?[0-9]+)?}
 *
 *   <dt><i>NonNumber</i>:
 *       <dd>{@code NaN
 *                          | }<i>LocalNan</i>{@code
 *                          | Infinity
 *                          | }<i>LocalInfinity</i>
 *
 *   <dt><i>SignedNonNumber</i>:
 *       <dd>{@code ( [-+]? }<i>NonNumber</i>{@code  )}
 *       <dd>{@code | }<i>LocalPositivePrefix</i>
 *                 <i>NonNumber</i>
 *                 <i>LocalPositiveSuffix</i>
 *       <dd>{@code | }<i>LocalNegativePrefix</i>
 *                 <i>NonNumber</i>
 *                 <i>LocalNegativeSuffix</i>
 *
 *   <dt><a id="Float-regex"><i>Float</i></a>:
 *       <dd><i>Decimal</i>
 *           {@code | }<i>HexFloat</i>
 *           {@code | }<i>SignedNonNumber</i>
 *
 * </dl>
 * <p>Whitespace is not significant in the above regular expressions.
 *
 * @since   1.5
 */
public final class Scanner implements Iterator<String>, Closeable {

    // Internal buffer used to hold input
    private CharBuffer buf;

    // Size of internal character buffer
    private static final int BUFFER_SIZE = 1024; // change to 1024;

    // The index into the buffer currently held by the Scanner
    private int position;

    // Internal matcher used for finding delimiters
    private Matcher matcher;

    // Pattern used to delimit tokens
    private Pattern delimPattern;

    // Pattern found in last hasNext operation
    private Pattern hasNextPattern;

    // Position after last hasNext operation
    private int hasNextPosition;

    // Result after last hasNext operation
    private String hasNextResult;

    // The input source
    private Readable source;

    // Boolean is true if source is done
    private boolean sourceClosed = false;

    // Boolean indicating more input is required
    private boolean needInput = false;

    // Boolean indicating if a delim has been skipped this operation
    private boolean skipped = false;

    // A store of a position that the scanner may fall back to
    private int savedScannerPosition = -1;

    // A cache of the last primitive type scanned
    private Object typeCache = null;

    // Boolean indicating if a match result is available
    private boolean matchValid = false;

    // Boolean indicating if this scanner has been closed
    private boolean closed = false;

    // The current radix used by this scanner
    private int radix = 10;

    // The default radix for this scanner
    private int defaultRadix = 10;

    // The locale used by this scanner
    private Locale locale = null;

    // A cache of the last few recently used Patterns
    private PatternLRUCache patternCache = new PatternLRUCache(7);

    // A holder of the last IOException encountered
    private IOException lastException;

    // Number of times this scanner's state has been modified.
    // Generally incremented on most public APIs and checked
    // within spliterator implementations.
    int modCount;

    // A pattern for java whitespace
    private static Pattern WHITESPACE_PATTERN = Pattern.compile(
                                                "\\p{javaWhitespace}+");

    // A pattern for any token
    private static Pattern FIND_ANY_PATTERN = Pattern.compile("(?s).*");

    // A pattern for non-ASCII digits
    private static Pattern NON_ASCII_DIGIT = Pattern.compile(
        "[\\p{javaDigit}&&[^0-9]]");

    // Fields and methods to support scanning primitive types

    /**
     * Locale dependent values used to scan numbers
     */
    private String groupSeparator = "\\,";
    private String decimalSeparator = "\\.";
    private String nanString = "NaN";
    private String infinityString = "Infinity";
    private String positivePrefix = "";
    private String negativePrefix = "\\-";
    private String positiveSuffix = "";
    private String negativeSuffix = "";

    /**
     * Fields and an accessor method to match booleans
     */
    private static volatile Pattern boolPattern;
    private static final String BOOLEAN_PATTERN = "true|false";
    private static Pattern boolPattern() {
        Pattern bp = boolPattern;
        if (bp == null)
            boolPattern = bp = Pattern.compile(BOOLEAN_PATTERN,
                                          Pattern.CASE_INSENSITIVE);
        return bp;
    }

    /**
     * Fields and methods to match bytes, shorts, ints, and longs
     */
    private Pattern integerPattern;
    private String digits = "0123456789abcdefghijklmnopqrstuvwxyz";
    private String non0Digit = "[\\p{javaDigit}&&[^0]]";
    private int SIMPLE_GROUP_INDEX = 5;
    private String buildIntegerPatternString() {
        String radixDigits = digits.substring(0, radix);
        // \\p{javaDigit} is not guaranteed to be appropriate
        // here but what can we do? The final authority will be
        // whatever parse method is invoked, so ultimately the
        // Scanner will do the right thing
        String digit = "((?i)["+radixDigits+"\\p{javaDigit}])";
        String groupedNumeral = "("+non0Digit+digit+"?"+digit+"?("+
                                groupSeparator+digit+digit+digit+")+)";
        // digit++ is the possessive form which is necessary for reducing
        // backtracking that would otherwise cause unacceptable performance
        String numeral = "(("+ digit+"++)|"+groupedNumeral+")";
        String javaStyleInteger = "([-+]?(" + numeral + "))";
        String negativeInteger = negativePrefix + numeral + negativeSuffix;
        String positiveInteger = positivePrefix + numeral + positiveSuffix;
        return "("+ javaStyleInteger + ")|(" +
            positiveInteger + ")|(" +
            negativeInteger + ")";
    }
    private Pattern integerPattern() {
        if (integerPattern == null) {
            integerPattern = patternCache.forName(buildIntegerPatternString());
        }
        return integerPattern;
    }

    /**
     * Fields and an accessor method to match line separators
     */
    private static volatile Pattern separatorPattern;
    private static volatile Pattern linePattern;
    private static final String LINE_SEPARATOR_PATTERN =
                                           "\r\n|[\n\r\u2028\u2029\u0085]";
    private static final String LINE_PATTERN = ".*("+LINE_SEPARATOR_PATTERN+")|.+$";

    private static Pattern separatorPattern() {
        Pattern sp = separatorPattern;
        if (sp == null)
            separatorPattern = sp = Pattern.compile(LINE_SEPARATOR_PATTERN);
        return sp;
    }

    private static Pattern linePattern() {
        Pattern lp = linePattern;
        if (lp == null)
            linePattern = lp = Pattern.compile(LINE_PATTERN);
        return lp;
    }

    /**
     * Fields and methods to match floats and doubles
     */
    private Pattern floatPattern;
    private Pattern decimalPattern;
    private void buildFloatAndDecimalPattern() {
        // \\p{javaDigit} may not be perfect, see above
        String digit = "(([0-9\\p{javaDigit}]))";
        String exponent = "([eE][+-]?"+digit+"+)?";
        String groupedNumeral = "("+non0Digit+digit+"?"+digit+"?("+
                                groupSeparator+digit+digit+digit+")+)";
        // Once again digit++ is used for performance, as above
        String numeral = "(("+digit+"++)|"+groupedNumeral+")";
        String decimalNumeral = "("+numeral+"|"+numeral +
            decimalSeparator + digit + "*+|"+ decimalSeparator +
            digit + "++)";
        String nonNumber = "(NaN|"+nanString+"|Infinity|"+
                               infinityString+")";
        String positiveFloat = "(" + positivePrefix + decimalNumeral +
                            positiveSuffix + exponent + ")";
        String negativeFloat = "(" + negativePrefix + decimalNumeral +
                            negativeSuffix + exponent + ")";
        String decimal = "(([-+]?" + decimalNumeral + exponent + ")|"+
            positiveFloat + "|" + negativeFloat + ")";
        String hexFloat =
            "[-+]?0[xX][0-9a-fA-F]*\\.[0-9a-fA-F]+([pP][-+]?[0-9]+)?";
        String positiveNonNumber = "(" + positivePrefix + nonNumber +
                            positiveSuffix + ")";
        String negativeNonNumber = "(" + negativePrefix + nonNumber +
                            negativeSuffix + ")";
        String signedNonNumber = "(([-+]?"+nonNumber+")|" +
                                 positiveNonNumber + "|" +
                                 negativeNonNumber + ")";
        floatPattern = Pattern.compile(decimal + "|" + hexFloat + "|" +
                                       signedNonNumber);
        decimalPattern = Pattern.compile(decimal);
    }
    private Pattern floatPattern() {
        if (floatPattern == null) {
            buildFloatAndDecimalPattern();
        }
        return floatPattern;
    }
    private Pattern decimalPattern() {
        if (decimalPattern == null) {
            buildFloatAndDecimalPattern();
        }
        return decimalPattern;
    }

    // Constructors

    /**
     * Constructs a {@code Scanner} that returns values scanned
     * from the specified source delimited by the specified pattern.
     *
     * @param source A character source implementing the Readable interface
     * @param pattern A delimiting pattern
     */
    private Scanner(Readable source, Pattern pattern) {
        assert source != null : "source should not be null";
        assert pattern != null : "pattern should not be null";
        this.source = source;
        delimPattern = pattern;
        buf = CharBuffer.allocate(BUFFER_SIZE);
        buf.limit(0);
        matcher = delimPattern.matcher(buf);
        matcher.useTransparentBounds(true);
        matcher.useAnchoringBounds(false);
        useLocale(Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * Constructs a new {@code Scanner} that produces values scanned
     * from the specified source.
     *
     * @param  source A character source implementing the {@link Readable}
     *         interface
     */
    public Scanner(Readable source) {
        this(Objects.requireNonNull(source, "source"), WHITESPACE_PATTERN);
    }

    /**
     * Constructs a new {@code Scanner} that produces values scanned
     * from the specified input stream. Bytes from the stream are converted
     * into characters using the underlying platform's
     * {@linkplain java.nio.charset.Charset#defaultCharset() default charset}.
     *
     * @param  source An input stream to be scanned
     */
    public Scanner(InputStream source) {
        this(new InputStreamReader(source), WHITESPACE_PATTERN);
    }

    /**
     * Constructs a new {@code Scanner} that produces values scanned
     * from the specified input stream. Bytes from the stream are converted
     * into characters using the specified charset.
     *
     * @param  source An input stream to be scanned
     * @param charsetName The encoding type used to convert bytes from the
     *        stream into characters to be scanned
     * @throws IllegalArgumentException if the specified character set
     *         does not exist
     */
    public Scanner(InputStream source, String charsetName) {
        this(source, toCharset(charsetName));
    }

    /**
     * Constructs a new {@code Scanner} that produces values scanned
     * from the specified input stream. Bytes from the stream are converted
     * into characters using the specified charset.
     *
     * @param  source an input stream to be scanned
     * @param  charset the charset used to convert bytes from the file
     *         into characters to be scanned
     * @since  10
     */
    public Scanner(InputStream source, Charset charset) {
        this(makeReadable(Objects.requireNonNull(source, "source"), charset),
             WHITESPACE_PATTERN);
    }

    /**
     * Returns a charset object for the given charset name.
     * @throws NullPointerException          is csn is null
     * @throws IllegalArgumentException      if the charset is not supported
     */
    private static Charset toCharset(String csn) {
        Objects.requireNonNull(csn, "charsetName");
        try {
            return Charset.forName(csn);
        } catch (IllegalCharsetNameException|UnsupportedCharsetException e) {
            // IllegalArgumentException should be thrown
            throw new IllegalArgumentException(e);
        }
    }

    /*
     * This method is added so that null-check on charset can be performed before
     * creating InputStream as an existing test required it.
     */
    private static Readable makeReadable(Path source, Charset charset)
            throws IOException {
        Objects.requireNonNull(charset, "charset");
        return makeReadable(Files.newInputStream(source), charset);
    }

    private static Readable makeReadable(InputStream source, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        return new InputStreamReader(source, charset);
    }

    /**
     * Constructs a new {@code Scanner} that produces values scanned
     * from the specified file. Bytes from the file are converted into
     * characters using the underlying platform's
     * {@linkplain java.nio.charset.Charset#defaultCharset() default charset}.
     *
     * @param  source A file to be scanned
     * @throws FileNotFoundException if source is not found
     */
    public Scanner(File source) throws FileNotFoundException {
        this((ReadableByteChannel)(new FileInputStream(source).getChannel()));
    }

    /**
     * Constructs a new {@code Scanner} that produces values scanned
     * from the specified file. Bytes from the file are converted into
     * characters using the specified charset.
     *
     * @param  source A file to be scanned
     * @param charsetName The encoding type used to convert bytes from the file
     *        into characters to be scanned
     * @throws FileNotFoundException if source is not found
     * @throws IllegalArgumentException if the specified encoding is
     *         not found
     */
    public Scanner(File source, String charsetName)
        throws FileNotFoundException
    {
        this(Objects.requireNonNull(source), toDecoder(charsetName));
    }

    /**
     * Constructs a new {@code Scanner} that produces values scanned
     * from the specified file. Bytes from the file are converted into
     * characters using the specified charset.
     *
     * @param  source A file to be scanned
     * @param  charset The charset used to convert bytes from the file
     *         into characters to be scanned
     * @throws IOException
     *         if an I/O error occurs opening the source
     * @since  10
     */
    public Scanner(File source, Charset charset) throws IOException {
        this(Objects.requireNonNull(source), charset.newDecoder());
    }

    private Scanner(File source, CharsetDecoder dec)
        throws FileNotFoundException
    {
        this(makeReadable((ReadableByteChannel)(new FileInputStream(source).getChannel()), dec));
    }

    private static CharsetDecoder toDecoder(String charsetName) {
        Objects.requireNonNull(charsetName, "charsetName");
        try {
            return Charset.forName(charsetName).newDecoder();
        } catch (IllegalCharsetNameException|UnsupportedCharsetException unused) {
            throw new IllegalArgumentException(charsetName);
        }
    }

    private static Readable makeReadable(ReadableByteChannel source,
                                         CharsetDecoder dec) {
        return Channels.newReader(source, dec, -1);
    }

    private static Readable makeReadable(ReadableByteChannel source,
                                         Charset charset) {
        Objects.requireNonNull(charset, "charset");
        return Channels.newReader(source, charset);
    }

    /**
     * Constructs a new {@code Scanner} that produces values scanned
     * from the specified file. Bytes from the file are converted into
     * characters using the underlying platform's
     * {@linkplain java.nio.charset.Charset#defaultCharset() default charset}.
     *
     * @param   source
     *          the path to the file to be scanned
     * @throws  IOException
     *          if an I/O error occurs opening source
     *
     * @since   1.7
     */
    public Scanner(Path source)
        throws IOException
    {
        this(Files.newInputStream(source));
    }

    /**
     * Constructs a new {@code Scanner} that produces values scanned
     * from the specified file. Bytes from the file are converted into
     * characters using the specified charset.
     *
     * @param   source
     *          the path to the file to be scanned
     * @param   charsetName
     *          The encoding type used to convert bytes from the file
     *          into characters to be scanned
     * @throws  IOException
     *          if an I/O error occurs opening source
     * @throws  IllegalArgumentException
     *          if the specified encoding is not found
     * @since   1.7
     */
    public Scanner(Path source, String charsetName) throws IOException {
        this(Objects.requireNonNull(source), toCharset(charsetName));
    }

    /**
     * Constructs a new {@code Scanner} that produces values scanned
     * from the specified file. Bytes from the file are converted into
     * characters using the specified charset.
     *
     * @param   source
     *          the path to the file to be scanned
     * @param   charset
     *          the charset used to convert bytes from the file
     *          into characters to be scanned
     * @throws  IOException
     *          if an I/O error occurs opening the source
     * @since   10
     */
    public Scanner(Path source, Charset charset)  throws IOException {
        this(makeReadable(source, charset));
    }

    /**
     * Constructs a new {@code Scanner} that produces values scanned
     * from the specified string.
     *
     * @param  source A string to scan
     */
    public Scanner(String source) {
        this(new StringReader(source), WHITESPACE_PATTERN);
    }

    /**
     * Constructs a new {@code Scanner} that produces values scanned
     * from the specified channel. Bytes from the source are converted into
     * characters using the underlying platform's
     * {@linkplain java.nio.charset.Charset#defaultCharset() default charset}.
     *
     * @param  source A channel to scan
     */
    public Scanner(ReadableByteChannel source) {
        this(makeReadable(Objects.requireNonNull(source, "source")),
             WHITESPACE_PATTERN);
    }

    private static Readable makeReadable(ReadableByteChannel source) {
        return makeReadable(source, Charset.defaultCharset().newDecoder());
    }

    /**
     * Constructs a new {@code Scanner} that produces values scanned
     * from the specified channel. Bytes from the source are converted into
     * characters using the specified charset.
     *
     * @param  source A channel to scan
     * @param charsetName The encoding type used to convert bytes from the
     *        channel into characters to be scanned
     * @throws IllegalArgumentException if the specified character set
     *         does not exist
     */
    public Scanner(ReadableByteChannel source, String charsetName) {
        this(makeReadable(Objects.requireNonNull(source, "source"), toDecoder(charsetName)),
             WHITESPACE_PATTERN);
    }

    /**
     * Constructs a new {@code Scanner} that produces values scanned
     * from the specified channel. Bytes from the source are converted into
     * characters using the specified charset.
     *
     * @param source a channel to scan
     * @param charset the encoding type used to convert bytes from the
     *        channel into characters to be scanned
     * @since 10
     */
    public Scanner(ReadableByteChannel source, Charset charset) {
        this(makeReadable(Objects.requireNonNull(source, "source"), charset),
             WHITESPACE_PATTERN);
    }

    // Private primitives used to support scanning

    private void saveState() {
        savedScannerPosition = position;
    }

    private void revertState() {
        this.position = savedScannerPosition;
        savedScannerPosition = -1;
        skipped = false;
    }

    private boolean revertState(boolean b) {
        this.position = savedScannerPosition;
        savedScannerPosition = -1;
        skipped = false;
        return b;
    }

    private void cacheResult() {
        hasNextResult = matcher.group();
        hasNextPosition = matcher.end();
        hasNextPattern = matcher.pattern();
    }

    private void cacheResult(String result) {
        hasNextResult = result;
        hasNextPosition = matcher.end();
        hasNextPattern = matcher.pattern();
    }

    // Clears both regular cache and type cache
    private void clearCaches() {
        hasNextPattern = null;
        typeCache = null;
    }

    // Also clears both the regular cache and the type cache
    private String getCachedResult() {
        position = hasNextPosition;
        hasNextPattern = null;
        typeCache = null;
        return hasNextResult;
    }

    // Also clears both the regular cache and the type cache
    private void useTypeCache() {
        if (closed)
            throw new IllegalStateException("Scanner closed");
        position = hasNextPosition;
        hasNextPattern = null;
        typeCache = null;
    }

    // Tries to read more input. May block.
    private void readInput() {
        if (buf.limit() == buf.capacity())
            makeSpace();
        // Prepare to receive data
        int p = buf.position();
        buf.position(buf.limit());
        buf.limit(buf.capacity());

        int n = 0;
        try {
            n = source.read(buf);
        } catch (IOException ioe) {
            lastException = ioe;
            n = -1;
        }
        if (n == -1) {
            sourceClosed = true;
            needInput = false;
        }
        if (n > 0)
            needInput = false;
        // Restore current position and limit for reading
        buf.limit(buf.position());
        buf.position(p);
    }

    // After this method is called there will either be an exception
    // or else there will be space in the buffer
    private boolean makeSpace() {
        clearCaches();
        int offset = savedScannerPosition == -1 ?
            position : savedScannerPosition;
        buf.position(offset);
        // Gain space by compacting buffer
        if (offset > 0) {
            buf.compact();
            translateSavedIndexes(offset);
            position -= offset;
            buf.flip();
            return true;
        }
        // Gain space by growing buffer
        int newSize = buf.capacity() * 2;
        CharBuffer newBuf = CharBuffer.allocate(newSize);
        newBuf.put(buf);
        newBuf.flip();
        translateSavedIndexes(offset);
        position -= offset;
        buf = newBuf;
        matcher.reset(buf);
        return true;
    }

    // When a buffer compaction/reallocation occurs the saved indexes must
    // be modified appropriately
    private void translateSavedIndexes(int offset) {
        if (savedScannerPosition != -1)
            savedScannerPosition -= offset;
    }

    // If we are at the end of input then NoSuchElement;
    // If there is still input left then InputMismatch
    private void throwFor() {
        skipped = false;
        if ((sourceClosed) && (position == buf.limit()))
            throw new NoSuchElementException();
        else
            throw new InputMismatchException();
    }

    // Returns true if a complete token or partial token is in the buffer.
    // It is not necessary to find a complete token since a partial token
    // means that there will be another token with or without more input.
    private boolean hasTokenInBuffer() {
        matchValid = false;
        matcher.usePattern(delimPattern);
        matcher.region(position, buf.limit());
        // Skip delims first
        if (matcher.lookingAt()) {
            if (matcher.hitEnd() && !sourceClosed) {
                // more input might change the match of delims, in which
                // might change whether or not if there is token left in
                // buffer (don't update the "position" in this case)
                needInput = true;
                return false;
            }
            position = matcher.end();
        }
        // If we are sitting at the end, no more tokens in buffer
        if (position == buf.limit())
            return false;
        return true;
    }

    /*
     * Returns a "complete token" that matches the specified pattern
     *
     * A token is complete if surrounded by delims; a partial token
     * is prefixed by delims but not postfixed by them
     *
     * The position is advanced to the end of that complete token
     *
     * Pattern == null means accept any token at all
     *
     * Triple return:
     * 1. valid string means it was found
     * 2. null with needInput=false means we won't ever find it
     * 3. null with needInput=true means try again after readInput
     */
    private String getCompleteTokenInBuffer(Pattern pattern) {
        matchValid = false;
        // Skip delims first
        matcher.usePattern(delimPattern);
        if (!skipped) { // Enforcing only one skip of leading delims
            matcher.region(position, buf.limit());
            if (matcher.lookingAt()) {
                // If more input could extend the delimiters then we must wait
                // for more input
                if (matcher.hitEnd() && !sourceClosed) {
                    needInput = true;
                    return null;
                }
                // The delims were whole and the matcher should skip them
                skipped = true;
                position = matcher.end();
            }
        }

        // If we are sitting at the end, no more tokens in buffer
        if (position == buf.limit()) {
            if (sourceClosed)
                return null;
            needInput = true;
            return null;
        }
        // Must look for next delims. Simply attempting to match the
        // pattern at this point may find a match but it might not be
        // the first longest match because of missing input, or it might
        // match a partial token instead of the whole thing.

        // Then look for next delims
        matcher.region(position, buf.limit());
        boolean foundNextDelim = matcher.find();
        if (foundNextDelim && (matcher.end() == position)) {
            // Zero length delimiter match; we should find the next one
            // using the automatic advance past a zero length match;
            // Otherwise we have just found the same one we just skipped
            foundNextDelim = matcher.find();
        }
        if (foundNextDelim) {
            // In the rare case that more input could cause the match
            // to be lost and there is more input coming we must wait
            // for more input. Note that hitting the end is okay as long
            // as the match cannot go away. It is the beginning of the
            // next delims we want to be sure about, we don't care if
            // they potentially extend further.
            if (matcher.requireEnd() && !sourceClosed) {
                needInput = true;
                return null;
            }
            int tokenEnd = matcher.start();
            // There is a complete token.
            if (pattern == null) {
                // Must continue with match to provide valid MatchResult
                pattern = FIND_ANY_PATTERN;
            }
            //  Attempt to match against the desired pattern
            matcher.usePattern(pattern);
            matcher.region(position, tokenEnd);
            if (matcher.matches()) {
                String s = matcher.group();
                position = matcher.end();
                return s;
            } else { // Complete token but it does not match
                return null;
            }
        }

        // If we can't find the next delims but no more input is coming,
        // then we can treat the remainder as a whole token
        if (sourceClosed) {
            if (pattern == null) {
                // Must continue with match to provide valid MatchResult
                pattern = FIND_ANY_PATTERN;
            }
            // Last token; Match the pattern here or throw
            matcher.usePattern(pattern);
            matcher.region(position, buf.limit());
            if (matcher.matches()) {
                String s = matcher.group();
                position = matcher.end();
                return s;
            }
            // Last piece does not match
            return null;
        }

        // There is a partial token in the buffer; must read more
        // to complete it
        needInput = true;
        return null;
    }

    // Finds the specified pattern in the buffer up to horizon.
    // Returns true if the specified input pattern was matched,
    // and leaves the matcher field with the current match state.
    private boolean findPatternInBuffer(Pattern pattern, int horizon) {
        matchValid = false;
        matcher.usePattern(pattern);
        int bufferLimit = buf.limit();
        int horizonLimit = -1;
        int searchLimit = bufferLimit;
        if (horizon > 0) {
            horizonLimit = position + horizon;
            if (horizonLimit < bufferLimit)
                searchLimit = horizonLimit;
        }
        matcher.region(position, searchLimit);
        if (matcher.find()) {
            if (matcher.hitEnd() && (!sourceClosed)) {
                // The match may be longer if didn't hit horizon or real end
                if (searchLimit != horizonLimit) {
                     // Hit an artificial end; try to extend the match
                    needInput = true;
                    return false;
                }
                // The match could go away depending on what is next
                if ((searchLimit == horizonLimit) && matcher.requireEnd()) {
                    // Rare case: we hit the end of input and it happens
                    // that it is at the horizon and the end of input is
                    // required for the match.
                    needInput = true;
                    return false;
                }
            }
            // Did not hit end, or hit real end, or hit horizon
            position = matcher.end();
            return true;
        }

        if (sourceClosed)
            return false;

        // If there is no specified horizon, or if we have not searched
        // to the specified horizon yet, get more input
        if ((horizon == 0) || (searchLimit != horizonLimit))
            needInput = true;
        return false;
    }

    // Attempts to match a pattern anchored at the current position.
    // Returns true if the specified input pattern was matched,
    // and leaves the matcher field with the current match state.
    private boolean matchPatternInBuffer(Pattern pattern) {
        matchValid = false;
        matcher.usePattern(pattern);
        matcher.region(position, buf.limit());
        if (matcher.lookingAt()) {
            if (matcher.hitEnd() && (!sourceClosed)) {
                // Get more input and try again
                needInput = true;
                return false;
            }
            position = matcher.end();
            return true;
        }

        if (sourceClosed)
            return false;

        // Read more to find pattern
        needInput = true;
        return false;
    }

    // Throws if the scanner is closed
    private void ensureOpen() {
        if (closed)
            throw new IllegalStateException("Scanner closed");
    }

    // Public methods

    /**
     * Closes this scanner.
     *
     * <p> If this scanner has not yet been closed then if its underlying
     * {@linkplain java.lang.Readable readable} also implements the {@link
     * java.io.Closeable} interface then the readable's {@code close} method
     * will be invoked.  If this scanner is already closed then invoking this
     * method will have no effect.
     *
     * <p>Attempting to perform search operations after a scanner has
     * been closed will result in an {@link IllegalStateException}.
     *
     */
    public void close() {
        if (closed)
            return;
        if (source instanceof Closeable) {
            try {
                ((Closeable)source).close();
            } catch (IOException ioe) {
                lastException = ioe;
            }
        }
        sourceClosed = true;
        source = null;
        closed = true;
    }

    /**
     * Returns the {@code IOException} last thrown by this
     * {@code Scanner}'s underlying {@code Readable}. This method
     * returns {@code null} if no such exception exists.
     *
     * @return the last exception thrown by this scanner's readable
     */
    public IOException ioException() {
        return lastException;
    }

    /**
     * Returns the {@code Pattern} this {@code Scanner} is currently
     * using to match delimiters.
     *
     * @return this scanner's delimiting pattern.
     */
    public Pattern delimiter() {
        return delimPattern;
    }

    /**
     * Sets this scanner's delimiting pattern to the specified pattern.
     *
     * @param pattern A delimiting pattern
     * @return this scanner
     */
    public Scanner useDelimiter(Pattern pattern) {
        modCount++;
        delimPattern = pattern;
        return this;
    }

    /**
     * Sets this scanner's delimiting pattern to a pattern constructed from
     * the specified {@code String}.
     *
     * <p> An invocation of this method of the form
     * {@code useDelimiter(pattern)} behaves in exactly the same way as the
     * invocation {@code useDelimiter(Pattern.compile(pattern))}.
     *
     * <p> Invoking the {@link #reset} method will set the scanner's delimiter
     * to the <a href= "#default-delimiter">default</a>.
     *
     * @param pattern A string specifying a delimiting pattern
     * @return this scanner
     */
    public Scanner useDelimiter(String pattern) {
        modCount++;
        delimPattern = patternCache.forName(pattern);
        return this;
    }

    /**
     * Returns this scanner's locale.
     *
     * <p>A scanner's locale affects many elements of its default
     * primitive matching regular expressions; see
     * <a href= "#localized-numbers">localized numbers</a> above.
     *
     * @return this scanner's locale
     */
    public Locale locale() {
        return this.locale;
    }

    /**
     * Sets this scanner's locale to the specified locale.
     *
     * <p>A scanner's locale affects many elements of its default
     * primitive matching regular expressions; see
     * <a href= "#localized-numbers">localized numbers</a> above.
     *
     * <p>Invoking the {@link #reset} method will set the scanner's locale to
     * the <a href= "#initial-locale">initial locale</a>.
     *
     * @param locale A string specifying the locale to use
     * @return this scanner
     */
    public Scanner useLocale(Locale locale) {
        if (locale.equals(this.locale))
            return this;

        modCount++;
        this.locale = locale;

        DecimalFormat df = null;
        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(locale);
        if (nf instanceof DecimalFormat) {
             df = (DecimalFormat) nf;
        } else {

            // In case where NumberFormat.getNumberInstance() returns
            // other instance (non DecimalFormat) based on the provider
            // used and java.text.spi.NumberFormatProvider implementations,
            // DecimalFormat constructor is used to obtain the instance
            LocaleProviderAdapter adapter = LocaleProviderAdapter
                    .getAdapter(NumberFormatProvider.class, locale);
            if (!(adapter instanceof ResourceBundleBasedAdapter)) {
                adapter = LocaleProviderAdapter.getResourceBundleBased();
            }
            String[] all = adapter.getLocaleResources(locale)
                    .getNumberPatterns();
            df = new DecimalFormat(all[0], dfs);
        }

        // These must be literalized to avoid collision with regex
        // metacharacters such as dot or parenthesis
        groupSeparator =   "\\x{" + Integer.toHexString(dfs.getGroupingSeparator()) + "}";
        decimalSeparator = "\\x{" + Integer.toHexString(dfs.getDecimalSeparator()) + "}";

        // Quoting the nonzero length locale-specific things
        // to avoid potential conflict with metacharacters
        nanString = Pattern.quote(dfs.getNaN());
        infinityString = Pattern.quote(dfs.getInfinity());
        positivePrefix = df.getPositivePrefix();
        if (!positivePrefix.isEmpty())
            positivePrefix = Pattern.quote(positivePrefix);
        negativePrefix = df.getNegativePrefix();
        if (!negativePrefix.isEmpty())
            negativePrefix = Pattern.quote(negativePrefix);
        positiveSuffix = df.getPositiveSuffix();
        if (!positiveSuffix.isEmpty())
            positiveSuffix = Pattern.quote(positiveSuffix);
        negativeSuffix = df.getNegativeSuffix();
        if (!negativeSuffix.isEmpty())
            negativeSuffix = Pattern.quote(negativeSuffix);

        // Force rebuilding and recompilation of locale dependent
        // primitive patterns
        integerPattern = null;
        floatPattern = null;

        return this;
    }

    /**
     * Returns this scanner's default radix.
     *
     * <p>A scanner's radix affects elements of its default
     * number matching regular expressions; see
     * <a href= "#localized-numbers">localized numbers</a> above.
     *
     * @return the default radix of this scanner
     */
    public int radix() {
        return this.defaultRadix;
    }

    /**
     * Sets this scanner's default radix to the specified radix.
     *
     * <p>A scanner's radix affects elements of its default
     * number matching regular expressions; see
     * <a href= "#localized-numbers">localized numbers</a> above.
     *
     * <p>If the radix is less than {@link Character#MIN_RADIX Character.MIN_RADIX}
     * or greater than {@link Character#MAX_RADIX Character.MAX_RADIX}, then an
     * {@code IllegalArgumentException} is thrown.
     *
     * <p>Invoking the {@link #reset} method will set the scanner's radix to
     * {@code 10}.
     *
     * @param radix The radix to use when scanning numbers
     * @return this scanner
     * @throws IllegalArgumentException if radix is out of range
     */
    public Scanner useRadix(int radix) {
        if ((radix < Character.MIN_RADIX) || (radix > Character.MAX_RADIX))
            throw new IllegalArgumentException("radix:"+radix);

        if (this.defaultRadix == radix)
            return this;
        modCount++;
        this.defaultRadix = radix;
        // Force rebuilding and recompilation of radix dependent patterns
        integerPattern = null;
        return this;
    }

    // The next operation should occur in the specified radix but
    // the default is left untouched.
    private void setRadix(int radix) {
        if ((radix < Character.MIN_RADIX) || (radix > Character.MAX_RADIX))
            throw new IllegalArgumentException("radix:"+radix);

        if (this.radix != radix) {
            // Force rebuilding and recompilation of radix dependent patterns
            integerPattern = null;
            this.radix = radix;
        }
    }

    /**
     * Returns the match result of the last scanning operation performed
     * by this scanner. This method throws {@code IllegalStateException}
     * if no match has been performed, or if the last match was
     * not successful.
     *
     * <p>The various {@code next} methods of {@code Scanner}
     * make a match result available if they complete without throwing an
     * exception. For instance, after an invocation of the {@link #nextInt}
     * method that returned an int, this method returns a
     * {@code MatchResult} for the search of the
     * <a href="#Integer-regex"><i>Integer</i></a> regular expression
     * defined above. Similarly the {@link #findInLine findInLine()},
     * {@link #findWithinHorizon findWithinHorizon()}, and {@link #skip skip()}
     * methods will make a match available if they succeed.
     *
     * @return a match result for the last match operation
     * @throws IllegalStateException  If no match result is available
     */
    public MatchResult match() {
        if (!matchValid)
            throw new IllegalStateException("No match result available");
        return matcher.toMatchResult();
    }

    /**
     * <p>Returns the string representation of this {@code Scanner}. The
     * string representation of a {@code Scanner} contains information
     * that may be useful for debugging. The exact format is unspecified.
     *
     * @return  The string representation of this scanner
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("java.util.Scanner");
        sb.append("[delimiters=" + delimPattern + "]");
        sb.append("[position=" + position + "]");
        sb.append("[match valid=" + matchValid + "]");
        sb.append("[need input=" + needInput + "]");
        sb.append("[source closed=" + sourceClosed + "]");
        sb.append("[skipped=" + skipped + "]");
        sb.append("[group separator=" + groupSeparator + "]");
        sb.append("[decimal separator=" + decimalSeparator + "]");
        sb.append("[positive prefix=" + positivePrefix + "]");
        sb.append("[negative prefix=" + negativePrefix + "]");
        sb.append("[positive suffix=" + positiveSuffix + "]");
        sb.append("[negative suffix=" + negativeSuffix + "]");
        sb.append("[NaN string=" + nanString + "]");
        sb.append("[infinity string=" + infinityString + "]");
        return sb.toString();
    }

    /**
     * Returns true if this scanner has another token in its input.
     * This method may block while waiting for input to scan.
     * The scanner does not advance past any input.
     *
     * @return true if and only if this scanner has another token
     * @throws IllegalStateException if this scanner is closed
     * @see java.util.Iterator
     */
    public boolean hasNext() {
        ensureOpen();
        saveState();
        modCount++;
        while (!sourceClosed) {
            if (hasTokenInBuffer()) {
                return revertState(true);
            }
            readInput();
        }
        boolean result = hasTokenInBuffer();
        return revertState(result);
    }

    /**
     * Finds and returns the next complete token from this scanner.
     * A complete token is preceded and followed by input that matches
     * the delimiter pattern. This method may block while waiting for input
     * to scan, even if a previous invocation of {@link #hasNext} returned
     * {@code true}.
     *
     * @return the next token
     * @throws NoSuchElementException if no more tokens are available
     * @throws IllegalStateException if this scanner is closed
     * @see java.util.Iterator
     */
    public String next() {
        ensureOpen();
        clearCaches();
        modCount++;
        while (true) {
            String token = getCompleteTokenInBuffer(null);
            if (token != null) {
                matchValid = true;
                skipped = false;
                return token;
            }
            if (needInput)
                readInput();
            else
                throwFor();
        }
    }

    /**
     * The remove operation is not supported by this implementation of
     * {@code Iterator}.
     *
     * @throws UnsupportedOperationException if this method is invoked.
     * @see java.util.Iterator
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns true if the next token matches the pattern constructed from the
     * specified string. The scanner does not advance past any input.
     *
     * <p> An invocation of this method of the form {@code hasNext(pattern)}
     * behaves in exactly the same way as the invocation
     * {@code hasNext(Pattern.compile(pattern))}.
     *
     * @param pattern a string specifying the pattern to scan
     * @return true if and only if this scanner has another token matching
     *         the specified pattern
     * @throws IllegalStateException if this scanner is closed
     */
    public boolean hasNext(String pattern)  {
        return hasNext(patternCache.forName(pattern));
    }

    /**
     * Returns the next token if it matches the pattern constructed from the
     * specified string.  If the match is successful, the scanner advances
     * past the input that matched the pattern.
     *
     * <p> An invocation of this method of the form {@code next(pattern)}
     * behaves in exactly the same way as the invocation
     * {@code next(Pattern.compile(pattern))}.
     *
     * @param pattern a string specifying the pattern to scan
     * @return the next token
     * @throws NoSuchElementException if no such tokens are available
     * @throws IllegalStateException if this scanner is closed
     */
    public String next(String pattern)  {
        return next(patternCache.forName(pattern));
    }

    /**
     * Returns true if the next complete token matches the specified pattern.
     * A complete token is prefixed and postfixed by input that matches
     * the delimiter pattern. This method may block while waiting for input.
     * The scanner does not advance past any input.
     *
     * @param pattern the pattern to scan for
     * @return true if and only if this scanner has another token matching
     *         the specified pattern
     * @throws IllegalStateException if this scanner is closed
     */
    public boolean hasNext(Pattern pattern) {
        ensureOpen();
        if (pattern == null)
            throw new NullPointerException();
        hasNextPattern = null;
        saveState();
        modCount++;

        while (true) {
            if (getCompleteTokenInBuffer(pattern) != null) {
                matchValid = true;
                cacheResult();
                return revertState(true);
            }
            if (needInput)
                readInput();
            else
                return revertState(false);
        }
    }

    /**
     * Returns the next token if it matches the specified pattern. This
     * method may block while waiting for input to scan, even if a previous
     * invocation of {@link #hasNext(Pattern)} returned {@code true}.
     * If the match is successful, the scanner advances past the input that
     * matched the pattern.
     *
     * @param pattern the pattern to scan for
     * @return the next token
     * @throws NoSuchElementException if no more tokens are available
     * @throws IllegalStateException if this scanner is closed
     */
    public String next(Pattern pattern) {
        ensureOpen();
        if (pattern == null)
            throw new NullPointerException();

        modCount++;
        // Did we already find this pattern?
        if (hasNextPattern == pattern)
            return getCachedResult();
        clearCaches();

        // Search for the pattern
        while (true) {
            String token = getCompleteTokenInBuffer(pattern);
            if (token != null) {
                matchValid = true;
                skipped = false;
                return token;
            }
            if (needInput)
                readInput();
            else
                throwFor();
        }
    }

    /**
     * Returns true if there is another line in the input of this scanner.
     * This method may block while waiting for input. The scanner does not
     * advance past any input.
     *
     * @return true if there is a line separator in the remaining input
     * or if the input has other remaining characters
     * @throws IllegalStateException if this scanner is closed
     */
    public boolean hasNextLine() {
        saveState();

        modCount++;
        String result = findWithinHorizon(linePattern(), 0);
        if (result != null) {
            MatchResult mr = this.match();
            String lineSep = mr.group(1);
            if (lineSep != null) {
                result = result.substring(0, result.length() -
                                          lineSep.length());
                cacheResult(result);

            } else {
                cacheResult();
            }
        }
        revertState();
        return (result != null);
    }

    /**
     * Advances this scanner past the current line and returns the input
     * that was skipped.
     *
     * This method returns the rest of the current line, excluding any line
     * separator at the end. The position is set to the beginning of the next
     * line.
     *
     * <p>Since this method continues to search through the input looking
     * for a line separator, it may buffer all of the input searching for
     * the line to skip if no line separators are present.
     *
     * @return the line that was skipped
     * @throws NoSuchElementException if no line was found
     * @throws IllegalStateException if this scanner is closed
     */
    public String nextLine() {
        modCount++;
        if (hasNextPattern == linePattern())
            return getCachedResult();
        clearCaches();

        String result = findWithinHorizon(linePattern, 0);
        if (result == null)
            throw new NoSuchElementException("No line found");
        MatchResult mr = this.match();
        String lineSep = mr.group(1);
        if (lineSep != null)
            result = result.substring(0, result.length() - lineSep.length());
        if (result == null)
            throw new NoSuchElementException();
        else
            return result;
    }

    // Public methods that ignore delimiters

    /**
     * Attempts to find the next occurrence of a pattern constructed from the
     * specified string, ignoring delimiters.
     *
     * <p>An invocation of this method of the form {@code findInLine(pattern)}
     * behaves in exactly the same way as the invocation
     * {@code findInLine(Pattern.compile(pattern))}.
     *
     * @param pattern a string specifying the pattern to search for
     * @return the text that matched the specified pattern
     * @throws IllegalStateException if this scanner is closed
     */
    public String findInLine(String pattern) {
        return findInLine(patternCache.forName(pattern));
    }

    /**
     * Attempts to find the next occurrence of the specified pattern ignoring
     * delimiters. If the pattern is found before the next line separator, the
     * scanner advances past the input that matched and returns the string that
     * matched the pattern.
     * If no such pattern is detected in the input up to the next line
     * separator, then {@code null} is returned and the scanner's
     * position is unchanged. This method may block waiting for input that
     * matches the pattern.
     *
     * <p>Since this method continues to search through the input looking
     * for the specified pattern, it may buffer all of the input searching for
     * the desired token if no line separators are present.
     *
     * @param pattern the pattern to scan for
     * @return the text that matched the specified pattern
     * @throws IllegalStateException if this scanner is closed
     */
    public String findInLine(Pattern pattern) {
        ensureOpen();
        if (pattern == null)
            throw new NullPointerException();
        clearCaches();
        modCount++;
        // Expand buffer to include the next newline or end of input
        int endPosition = 0;
        saveState();
        while (true) {
            if (findPatternInBuffer(separatorPattern(), 0)) {
                endPosition = matcher.start();
                break; // up to next newline
            }
            if (needInput) {
                readInput();
            } else {
                endPosition = buf.limit();
                break; // up to end of input
            }
        }
        revertState();
        int horizonForLine = endPosition - position;
        // If there is nothing between the current pos and the next
        // newline simply return null, invoking findWithinHorizon
        // with "horizon=0" will scan beyond the line bound.
        if (horizonForLine == 0)
            return null;
        // Search for the pattern
        return findWithinHorizon(pattern, horizonForLine);
    }

    /**
     * Attempts to find the next occurrence of a pattern constructed from the
     * specified string, ignoring delimiters.
     *
     * <p>An invocation of this method of the form
     * {@code findWithinHorizon(pattern)} behaves in exactly the same way as
     * the invocation
     * {@code findWithinHorizon(Pattern.compile(pattern), horizon)}.
     *
     * @param pattern a string specifying the pattern to search for
     * @param horizon the search horizon
     * @return the text that matched the specified pattern
     * @throws IllegalStateException if this scanner is closed
     * @throws IllegalArgumentException if horizon is negative
     */
    public String findWithinHorizon(String pattern, int horizon) {
        return findWithinHorizon(patternCache.forName(pattern), horizon);
    }

    /**
     * Attempts to find the next occurrence of the specified pattern.
     *
     * <p>This method searches through the input up to the specified
     * search horizon, ignoring delimiters. If the pattern is found the
     * scanner advances past the input that matched and returns the string
     * that matched the pattern. If no such pattern is detected then the
     * null is returned and the scanner's position remains unchanged. This
     * method may block waiting for input that matches the pattern.
     *
     * <p>A scanner will never search more than {@code horizon} code
     * points beyond its current position. Note that a match may be clipped
     * by the horizon; that is, an arbitrary match result may have been
     * different if the horizon had been larger. The scanner treats the
     * horizon as a transparent, non-anchoring bound (see {@link
     * Matcher#useTransparentBounds} and {@link Matcher#useAnchoringBounds}).
     *
     * <p>If horizon is {@code 0}, then the horizon is ignored and
     * this method continues to search through the input looking for the
     * specified pattern without bound. In this case it may buffer all of
     * the input searching for the pattern.
     *
     * <p>If horizon is negative, then an IllegalArgumentException is
     * thrown.
     *
     * @param pattern the pattern to scan for
     * @param horizon the search horizon
     * @return the text that matched the specified pattern
     * @throws IllegalStateException if this scanner is closed
     * @throws IllegalArgumentException if horizon is negative
     */
    public String findWithinHorizon(Pattern pattern, int horizon) {
        ensureOpen();
        if (pattern == null)
            throw new NullPointerException();
        if (horizon < 0)
            throw new IllegalArgumentException("horizon < 0");
        clearCaches();
        modCount++;

        // Search for the pattern
        while (true) {
            if (findPatternInBuffer(pattern, horizon)) {
                matchValid = true;
                return matcher.group();
            }
            if (needInput)
                readInput();
            else
                break; // up to end of input
        }
        return null;
    }

    /**
     * Skips input that matches the specified pattern, ignoring delimiters.
     * This method will skip input if an anchored match of the specified
     * pattern succeeds.
     *
     * <p>If a match to the specified pattern is not found at the
     * current position, then no input is skipped and a
     * {@code NoSuchElementException} is thrown.
     *
     * <p>Since this method seeks to match the specified pattern starting at
     * the scanner's current position, patterns that can match a lot of
     * input (".*", for example) may cause the scanner to buffer a large
     * amount of input.
     *
     * <p>Note that it is possible to skip something without risking a
     * {@code NoSuchElementException} by using a pattern that can
     * match nothing, e.g., {@code sc.skip("[ \t]*")}.
     *
     * @param pattern a string specifying the pattern to skip over
     * @return this scanner
     * @throws NoSuchElementException if the specified pattern is not found
     * @throws IllegalStateException if this scanner is closed
     */
    public Scanner skip(Pattern pattern) {
        ensureOpen();
        if (pattern == null)
            throw new NullPointerException();
        clearCaches();
        modCount++;

        // Search for the pattern
        while (true) {
            if (matchPatternInBuffer(pattern)) {
                matchValid = true;
                position = matcher.end();
                return this;
            }
            if (needInput)
                readInput();
            else
                throw new NoSuchElementException();
        }
    }

    /**
     * Skips input that matches a pattern constructed from the specified
     * string.
     *
     * <p> An invocation of this method of the form {@code skip(pattern)}
     * behaves in exactly the same way as the invocation
     * {@code skip(Pattern.compile(pattern))}.
     *
     * @param pattern a string specifying the pattern to skip over
     * @return this scanner
     * @throws IllegalStateException if this scanner is closed
     */
    public Scanner skip(String pattern) {
        return skip(patternCache.forName(pattern));
    }

    // Convenience methods for scanning primitives

    /**
     * Returns true if the next token in this scanner's input can be
     * interpreted as a boolean value using a case insensitive pattern
     * created from the string "true|false".  The scanner does not
     * advance past the input that matched.
     *
     * @return true if and only if this scanner's next token is a valid
     *         boolean value
     * @throws IllegalStateException if this scanner is closed
     */
    public boolean hasNextBoolean()  {
        return hasNext(boolPattern());
    }

    /**
     * Scans the next token of the input into a boolean value and returns
     * that value. This method will throw {@code InputMismatchException}
     * if the next token cannot be translated into a valid boolean value.
     * If the match is successful, the scanner advances past the input that
     * matched.
     *
     * @return the boolean scanned from the input
     * @throws InputMismatchException if the next token is not a valid boolean
     * @throws NoSuchElementException if input is exhausted
     * @throws IllegalStateException if this scanner is closed
     */
    public boolean nextBoolean()  {
        clearCaches();
        return Boolean.parseBoolean(next(boolPattern()));
    }

    /**
     * Returns true if the next token in this scanner's input can be
     * interpreted as a byte value in the default radix using the
     * {@link #nextByte} method. The scanner does not advance past any input.
     *
     * @return true if and only if this scanner's next token is a valid
     *         byte value
     * @throws IllegalStateException if this scanner is closed
     */
    public boolean hasNextByte() {
        return hasNextByte(defaultRadix);
    }

    /**
     * Returns true if the next token in this scanner's input can be
     * interpreted as a byte value in the specified radix using the
     * {@link #nextByte} method. The scanner does not advance past any input.
     *
     * <p>If the radix is less than {@link Character#MIN_RADIX Character.MIN_RADIX}
     * or greater than {@link Character#MAX_RADIX Character.MAX_RADIX}, then an
     * {@code IllegalArgumentException} is thrown.
     *
     * @param radix the radix used to interpret the token as a byte value
     * @return true if and only if this scanner's next token is a valid
     *         byte value
     * @throws IllegalStateException if this scanner is closed
     * @throws IllegalArgumentException if the radix is out of range
     */
    public boolean hasNextByte(int radix) {
        setRadix(radix);
        boolean result = hasNext(integerPattern());
        if (result) { // Cache it
            try {
                String s = (matcher.group(SIMPLE_GROUP_INDEX) == null) ?
                    processIntegerToken(hasNextResult) :
                    hasNextResult;
                typeCache = Byte.parseByte(s, radix);
            } catch (NumberFormatException nfe) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Scans the next token of the input as a {@code byte}.
     *
     * <p> An invocation of this method of the form
     * {@code nextByte()} behaves in exactly the same way as the
     * invocation {@code nextByte(radix)}, where {@code radix}
     * is the default radix of this scanner.
     *
     * @return the {@code byte} scanned from the input
     * @throws InputMismatchException
     *         if the next token does not match the <i>Integer</i>
     *         regular expression, or is out of range
     * @throws NoSuchElementException if input is exhausted
     * @throws IllegalStateException if this scanner is closed
     */
    public byte nextByte() {
         return nextByte(defaultRadix);
    }

    /**
     * Scans the next token of the input as a {@code byte}.
     * This method will throw {@code InputMismatchException}
     * if the next token cannot be translated into a valid byte value as
     * described below. If the translation is successful, the scanner advances
     * past the input that matched.
     *
     * <p> If the next token matches the <a
     * href="#Integer-regex"><i>Integer</i></a> regular expression defined
     * above then the token is converted into a {@code byte} value as if by
     * removing all locale specific prefixes, group separators, and locale
     * specific suffixes, then mapping non-ASCII digits into ASCII
     * digits via {@link Character#digit Character.digit}, prepending a
     * negative sign (-) if the locale specific negative prefixes and suffixes
     * were present, and passing the resulting string to
     * {@link Byte#parseByte(String, int) Byte.parseByte} with the
     * specified radix.
     *
     * <p>If the radix is less than {@link Character#MIN_RADIX Character.MIN_RADIX}
     * or greater than {@link Character#MAX_RADIX Character.MAX_RADIX}, then an
     * {@code IllegalArgumentException} is thrown.
     *
     * @param radix the radix used to interpret the token as a byte value
     * @return the {@code byte} scanned from the input
     * @throws InputMismatchException
     *         if the next token does not match the <i>Integer</i>
     *         regular expression, or is out of range
     * @throws NoSuchElementException if input is exhausted
     * @throws IllegalStateException if this scanner is closed
     * @throws IllegalArgumentException if the radix is out of range
     */
    public byte nextByte(int radix) {
        // Check cached result
        if ((typeCache != null) && (typeCache instanceof Byte)
            && this.radix == radix) {
            byte val = ((Byte)typeCache).byteValue();
            useTypeCache();
            return val;
        }
        setRadix(radix);
        clearCaches();
        // Search for next byte
        try {
            String s = next(integerPattern());
            if (matcher.group(SIMPLE_GROUP_INDEX) == null)
                s = processIntegerToken(s);
            return Byte.parseByte(s, radix);
        } catch (NumberFormatException nfe) {
            position = matcher.start(); // don't skip bad token
            throw new InputMismatchException(nfe.getMessage());
        }
    }

    /**
     * Returns true if the next token in this scanner's input can be
     * interpreted as a short value in the default radix using the
     * {@link #nextShort} method. The scanner does not advance past any input.
     *
     * @return true if and only if this scanner's next token is a valid
     *         short value in the default radix
     * @throws IllegalStateException if this scanner is closed
     */
    public boolean hasNextShort() {
        return hasNextShort(defaultRadix);
    }

    /**
     * Returns true if the next token in this scanner's input can be
     * interpreted as a short value in the specified radix using the
     * {@link #nextShort} method. The scanner does not advance past any input.
     *
     * <p>If the radix is less than {@link Character#MIN_RADIX Character.MIN_RADIX}
     * or greater than {@link Character#MAX_RADIX Character.MAX_RADIX}, then an
     * {@code IllegalArgumentException} is thrown.
     *
     * @param radix the radix used to interpret the token as a short value
     * @return true if and only if this scanner's next token is a valid
     *         short value in the specified radix
     * @throws IllegalStateException if this scanner is closed
     * @throws IllegalArgumentException if the radix is out of range
     */
    public boolean hasNextShort(int radix) {
        setRadix(radix);
        boolean result = hasNext(integerPattern());
        if (result) { // Cache it
            try {
                String s = (matcher.group(SIMPLE_GROUP_INDEX) == null) ?
                    processIntegerToken(hasNextResult) :
                    hasNextResult;
                typeCache = Short.parseShort(s, radix);
            } catch (NumberFormatException nfe) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Scans the next token of the input as a {@code short}.
     *
     * <p> An invocation of this method of the form
     * {@code nextShort()} behaves in exactly the same way as the
     * invocation {@link #nextShort(int) nextShort(radix)}, where {@code radix}
     * is the default radix of this scanner.
     *
     * @return the {@code short} scanned from the input
     * @throws InputMismatchException
     *         if the next token does not match the <i>Integer</i>
     *         regular expression, or is out of range
     * @throws NoSuchElementException if input is exhausted
     * @throws IllegalStateException if this scanner is closed
     */
    public short nextShort() {
        return nextShort(defaultRadix);
    }

    /**
     * Scans the next token of the input as a {@code short}.
     * This method will throw {@code InputMismatchException}
     * if the next token cannot be translated into a valid short value as
     * described below. If the translation is successful, the scanner advances
     * past the input that matched.
     *
     * <p> If the next token matches the <a
     * href="#Integer-regex"><i>Integer</i></a> regular expression defined
     * above then the token is converted into a {@code short} value as if by
     * removing all locale specific prefixes, group separators, and locale
     * specific suffixes, then mapping non-ASCII digits into ASCII
     * digits via {@link Character#digit Character.digit}, prepending a
     * negative sign (-) if the locale specific negative prefixes and suffixes
     * were present, and passing the resulting string to
     * {@link Short#parseShort(String, int) Short.parseShort} with the
     * specified radix.
     *
     * <p>If the radix is less than {@link Character#MIN_RADIX Character.MIN_RADIX}
     * or greater than {@link Character#MAX_RADIX Character.MAX_RADIX}, then an
     * {@code IllegalArgumentException} is thrown.
     *
     * @param radix the radix used to interpret the token as a short value
     * @return the {@code short} scanned from the input
     * @throws InputMismatchException
     *         if the next token does not match the <i>Integer</i>
     *         regular expression, or is out of range
     * @throws NoSuchElementException if input is exhausted
     * @throws IllegalStateException if this scanner is closed
     * @throws IllegalArgumentException if the radix is out of range
     */
    public short nextShort(int radix) {
        // Check cached result
        if ((typeCache != null) && (typeCache instanceof Short)
            && this.radix == radix) {
            short val = ((Short)typeCache).shortValue();
            useTypeCache();
            return val;
        }
        setRadix(radix);
        clearCaches();
        // Search for next short
        try {
            String s = next(integerPattern());
            if (matcher.group(SIMPLE_GROUP_INDEX) == null)
                s = processIntegerToken(s);
            return Short.parseShort(s, radix);
        } catch (NumberFormatException nfe) {
            position = matcher.start(); // don't skip bad token
            throw new InputMismatchException(nfe.getMessage());
        }
    }

    /**
     * Returns true if the next token in this scanner's input can be
     * interpreted as an int value in the default radix using the
     * {@link #nextInt} method. The scanner does not advance past any input.
     *
     * @return true if and only if this scanner's next token is a valid
     *         int value
     * @throws IllegalStateException if this scanner is closed
     */
    public boolean hasNextInt() {
        return hasNextInt(defaultRadix);
    }

    /**
     * Returns true if the next token in this scanner's input can be
     * interpreted as an int value in the specified radix using the
     * {@link #nextInt} method. The scanner does not advance past any input.
     *
     * <p>If the radix is less than {@link Character#MIN_RADIX Character.MIN_RADIX}
     * or greater than {@link Character#MAX_RADIX Character.MAX_RADIX}, then an
     * {@code IllegalArgumentException} is thrown.
     *
     * @param radix the radix used to interpret the token as an int value
     * @return true if and only if this scanner's next token is a valid
     *         int value
     * @throws IllegalStateException if this scanner is closed
     * @throws IllegalArgumentException if the radix is out of range
     */
    public boolean hasNextInt(int radix) {
        setRadix(radix);
        boolean result = hasNext(integerPattern());
        if (result) { // Cache it
            try {
                String s = (matcher.group(SIMPLE_GROUP_INDEX) == null) ?
                    processIntegerToken(hasNextResult) :
                    hasNextResult;
                typeCache = Integer.parseInt(s, radix);
            } catch (NumberFormatException nfe) {
                result = false;
            }
        }
        return result;
    }

    /**
     * The integer token must be stripped of prefixes, group separators,
     * and suffixes, non ascii digits must be converted into ascii digits
     * before parse will accept it.
     */
    private String processIntegerToken(String token) {
        String result = token.replaceAll(""+groupSeparator, "");
        boolean isNegative = false;
        int preLen = negativePrefix.length();
        if ((preLen > 0) && result.startsWith(negativePrefix)) {
            isNegative = true;
            result = result.substring(preLen);
        }
        int sufLen = negativeSuffix.length();
        if ((sufLen > 0) && result.endsWith(negativeSuffix)) {
            isNegative = true;
            result = result.substring(result.length() - sufLen,
                                      result.length());
        }
        if (isNegative)
            result = "-" + result;
        return result;
    }

    /**
     * Scans the next token of the input as an {@code int}.
     *
     * <p> An invocation of this method of the form
     * {@code nextInt()} behaves in exactly the same way as the
     * invocation {@code nextInt(radix)}, where {@code radix}
     * is the default radix of this scanner.
     *
     * @return the {@code int} scanned from the input
     * @throws InputMismatchException
     *         if the next token does not match the <i>Integer</i>
     *         regular expression, or is out of range
     * @throws NoSuchElementException if input is exhausted
     * @throws IllegalStateException if this scanner is closed
     */
    public int nextInt() {
        return nextInt(defaultRadix);
    }

    /**
     * Scans the next token of the input as an {@code int}.
     * This method will throw {@code InputMismatchException}
     * if the next token cannot be translated into a valid int value as
     * described below. If the translation is successful, the scanner advances
     * past the input that matched.
     *
     * <p> If the next token matches the <a
     * href="#Integer-regex"><i>Integer</i></a> regular expression defined
     * above then the token is converted into an {@code int} value as if by
     * removing all locale specific prefixes, group separators, and locale
     * specific suffixes, then mapping non-ASCII digits into ASCII
     * digits via {@link Character#digit Character.digit}, prepending a
     * negative sign (-) if the locale specific negative prefixes and suffixes
     * were present, and passing the resulting string to
     * {@link Integer#parseInt(String, int) Integer.parseInt} with the
     * specified radix.
     *
     * <p>If the radix is less than {@link Character#MIN_RADIX Character.MIN_RADIX}
     * or greater than {@link Character#MAX_RADIX Character.MAX_RADIX}, then an
     * {@code IllegalArgumentException} is thrown.
     *
     * @param radix the radix used to interpret the token as an int value
     * @return the {@code int} scanned from the input
     * @throws InputMismatchException
     *         if the next token does not match the <i>Integer</i>
     *         regular expression, or is out of range
     * @throws NoSuchElementException if input is exhausted
     * @throws IllegalStateException if this scanner is closed
     * @throws IllegalArgumentException if the radix is out of range
     */
    public int nextInt(int radix) {
        // Check cached result
        if ((typeCache != null) && (typeCache instanceof Integer)
            && this.radix == radix) {
            int val = ((Integer)typeCache).intValue();
            useTypeCache();
            return val;
        }
        setRadix(radix);
        clearCaches();
        // Search for next int
        try {
            String s = next(integerPattern());
            if (matcher.group(SIMPLE_GROUP_INDEX) == null)
                s = processIntegerToken(s);
            return Integer.parseInt(s, radix);
        } catch (NumberFormatException nfe) {
            position = matcher.start(); // don't skip bad token
            throw new InputMismatchException(nfe.getMessage());
        }
    }

    /**
     * Returns true if the next token in this scanner's input can be
     * interpreted as a long value in the default radix using the
     * {@link #nextLong} method. The scanner does not advance past any input.
     *
     * @return true if and only if this scanner's next token is a valid
     *         long value
     * @throws IllegalStateException if this scanner is closed
     */
    public boolean hasNextLong() {
        return hasNextLong(defaultRadix);
    }

    /**
     * Returns true if the next token in this scanner's input can be
     * interpreted as a long value in the specified radix using the
     * {@link #nextLong} method. The scanner does not advance past any input.
     *
     * <p>If the radix is less than {@link Character#MIN_RADIX Character.MIN_RADIX}
     * or greater than {@link Character#MAX_RADIX Character.MAX_RADIX}, then an
     * {@code IllegalArgumentException} is thrown.
     *
     * @param radix the radix used to interpret the token as a long value
     * @return true if and only if this scanner's next token is a valid
     *         long value
     * @throws IllegalStateException if this scanner is closed
     * @throws IllegalArgumentException if the radix is out of range
     */
    public boolean hasNextLong(int radix) {
        setRadix(radix);
        boolean result = hasNext(integerPattern());
        if (result) { // Cache it
            try {
                String s = (matcher.group(SIMPLE_GROUP_INDEX) == null) ?
                    processIntegerToken(hasNextResult) :
                    hasNextResult;
                typeCache = Long.parseLong(s, radix);
            } catch (NumberFormatException nfe) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Scans the next token of the input as a {@code long}.
     *
     * <p> An invocation of this method of the form
     * {@code nextLong()} behaves in exactly the same way as the
     * invocation {@code nextLong(radix)}, where {@code radix}
     * is the default radix of this scanner.
     *
     * @return the {@code long} scanned from the input
     * @throws InputMismatchException
     *         if the next token does not match the <i>Integer</i>
     *         regular expression, or is out of range
     * @throws NoSuchElementException if input is exhausted
     * @throws IllegalStateException if this scanner is closed
     */
    public long nextLong() {
        return nextLong(defaultRadix);
    }

    /**
     * Scans the next token of the input as a {@code long}.
     * This method will throw {@code InputMismatchException}
     * if the next token cannot be translated into a valid long value as
     * described below. If the translation is successful, the scanner advances
     * past the input that matched.
     *
     * <p> If the next token matches the <a
     * href="#Integer-regex"><i>Integer</i></a> regular expression defined
     * above then the token is converted into a {@code long} value as if by
     * removing all locale specific prefixes, group separators, and locale
     * specific suffixes, then mapping non-ASCII digits into ASCII
     * digits via {@link Character#digit Character.digit}, prepending a
     * negative sign (-) if the locale specific negative prefixes and suffixes
     * were present, and passing the resulting string to
     * {@link Long#parseLong(String, int) Long.parseLong} with the
     * specified radix.
     *
     * <p>If the radix is less than {@link Character#MIN_RADIX Character.MIN_RADIX}
     * or greater than {@link Character#MAX_RADIX Character.MAX_RADIX}, then an
     * {@code IllegalArgumentException} is thrown.
     *
     * @param radix the radix used to interpret the token as an int value
     * @return the {@code long} scanned from the input
     * @throws InputMismatchException
     *         if the next token does not match the <i>Integer</i>
     *         regular expression, or is out of range
     * @throws NoSuchElementException if input is exhausted
     * @throws IllegalStateException if this scanner is closed
     * @throws IllegalArgumentException if the radix is out of range
     */
    public long nextLong(int radix) {
        // Check cached result
        if ((typeCache != null) && (typeCache instanceof Long)
            && this.radix == radix) {
            long val = ((Long)typeCache).longValue();
            useTypeCache();
            return val;
        }
        setRadix(radix);
        clearCaches();
        try {
            String s = next(integerPattern());
            if (matcher.group(SIMPLE_GROUP_INDEX) == null)
                s = processIntegerToken(s);
            return Long.parseLong(s, radix);
        } catch (NumberFormatException nfe) {
            position = matcher.start(); // don't skip bad token
            throw new InputMismatchException(nfe.getMessage());
        }
    }

    /**
     * The float token must be stripped of prefixes, group separators,
     * and suffixes, non ascii digits must be converted into ascii digits
     * before parseFloat will accept it.
     *
     * If there are non-ascii digits in the token these digits must
     * be processed before the token is passed to parseFloat.
     */
    private String processFloatToken(String token) {
        String result = token.replaceAll(groupSeparator, "");
        if (!decimalSeparator.equals("\\."))
            result = result.replaceAll(decimalSeparator, ".");
        boolean isNegative = false;
        int preLen = negativePrefix.length();
        if ((preLen > 0) && result.startsWith(negativePrefix)) {
            isNegative = true;
            result = result.substring(preLen);
        }
        int sufLen = negativeSuffix.length();
        if ((sufLen > 0) && result.endsWith(negativeSuffix)) {
            isNegative = true;
            result = result.substring(result.length() - sufLen,
                                      result.length());
        }
        if (result.equals(nanString))
            result = "NaN";
        if (result.equals(infinityString))
            result = "Infinity";
        if (isNegative)
            result = "-" + result;

        // Translate non-ASCII digits
        Matcher m = NON_ASCII_DIGIT.matcher(result);
        if (m.find()) {
            StringBuilder inASCII = new StringBuilder();
            for (int i=0; i<result.length(); i++) {
                char nextChar = result.charAt(i);
                if (Character.isDigit(nextChar)) {
                    int d = Character.digit(nextChar, 10);
                    if (d != -1)
                        inASCII.append(d);
                    else
                        inASCII.append(nextChar);
                } else {
                    inASCII.append(nextChar);
                }
            }
            result = inASCII.toString();
        }

        return result;
    }

    /**
     * Returns true if the next token in this scanner's input can be
     * interpreted as a float value using the {@link #nextFloat}
     * method. The scanner does not advance past any input.
     *
     * @return true if and only if this scanner's next token is a valid
     *         float value
     * @throws IllegalStateException if this scanner is closed
     */
    public boolean hasNextFloat() {
        setRadix(10);
        boolean result = hasNext(floatPattern());
        if (result) { // Cache it
            try {
                String s = processFloatToken(hasNextResult);
                typeCache = Float.valueOf(Float.parseFloat(s));
            } catch (NumberFormatException nfe) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Scans the next token of the input as a {@code float}.
     * This method will throw {@code InputMismatchException}
     * if the next token cannot be translated into a valid float value as
     * described below. If the translation is successful, the scanner advances
     * past the input that matched.
     *
     * <p> If the next token matches the <a
     * href="#Float-regex"><i>Float</i></a> regular expression defined above
     * then the token is converted into a {@code float} value as if by
     * removing all locale specific prefixes, group separators, and locale
     * specific suffixes, then mapping non-ASCII digits into ASCII
     * digits via {@link Character#digit Character.digit}, prepending a
     * negative sign (-) if the locale specific negative prefixes and suffixes
     * were present, and passing the resulting string to
     * {@link Float#parseFloat Float.parseFloat}. If the token matches
     * the localized NaN or infinity strings, then either "Nan" or "Infinity"
     * is passed to {@link Float#parseFloat(String) Float.parseFloat} as
     * appropriate.
     *
     * @return the {@code float} scanned from the input
     * @throws InputMismatchException
     *         if the next token does not match the <i>Float</i>
     *         regular expression, or is out of range
     * @throws NoSuchElementException if input is exhausted
     * @throws IllegalStateException if this scanner is closed
     */
    public float nextFloat() {
        // Check cached result
        if ((typeCache != null) && (typeCache instanceof Float)) {
            float val = ((Float)typeCache).floatValue();
            useTypeCache();
            return val;
        }
        setRadix(10);
        clearCaches();
        try {
            return Float.parseFloat(processFloatToken(next(floatPattern())));
        } catch (NumberFormatException nfe) {
            position = matcher.start(); // don't skip bad token
            throw new InputMismatchException(nfe.getMessage());
        }
    }

    /**
     * Returns true if the next token in this scanner's input can be
     * interpreted as a double value using the {@link #nextDouble}
     * method. The scanner does not advance past any input.
     *
     * @return true if and only if this scanner's next token is a valid
     *         double value
     * @throws IllegalStateException if this scanner is closed
     */
    public boolean hasNextDouble() {
        setRadix(10);
        boolean result = hasNext(floatPattern());
        if (result) { // Cache it
            try {
                String s = processFloatToken(hasNextResult);
                typeCache = Double.valueOf(Double.parseDouble(s));
            } catch (NumberFormatException nfe) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Scans the next token of the input as a {@code double}.
     * This method will throw {@code InputMismatchException}
     * if the next token cannot be translated into a valid double value.
     * If the translation is successful, the scanner advances past the input
     * that matched.
     *
     * <p> If the next token matches the <a
     * href="#Float-regex"><i>Float</i></a> regular expression defined above
     * then the token is converted into a {@code double} value as if by
     * removing all locale specific prefixes, group separators, and locale
     * specific suffixes, then mapping non-ASCII digits into ASCII
     * digits via {@link Character#digit Character.digit}, prepending a
     * negative sign (-) if the locale specific negative prefixes and suffixes
     * were present, and passing the resulting string to
     * {@link Double#parseDouble Double.parseDouble}. If the token matches
     * the localized NaN or infinity strings, then either "Nan" or "Infinity"
     * is passed to {@link Double#parseDouble(String) Double.parseDouble} as
     * appropriate.
     *
     * @return the {@code double} scanned from the input
     * @throws InputMismatchException
     *         if the next token does not match the <i>Float</i>
     *         regular expression, or is out of range
     * @throws NoSuchElementException if the input is exhausted
     * @throws IllegalStateException if this scanner is closed
     */
    public double nextDouble() {
        // Check cached result
        if ((typeCache != null) && (typeCache instanceof Double)) {
            double val = ((Double)typeCache).doubleValue();
            useTypeCache();
            return val;
        }
        setRadix(10);
        clearCaches();
        // Search for next float
        try {
            return Double.parseDouble(processFloatToken(next(floatPattern())));
        } catch (NumberFormatException nfe) {
            position = matcher.start(); // don't skip bad token
            throw new InputMismatchException(nfe.getMessage());
        }
    }

    // Convenience methods for scanning multi precision numbers

    /**
     * Returns true if the next token in this scanner's input can be
     * interpreted as a {@code BigInteger} in the default radix using the
     * {@link #nextBigInteger} method. The scanner does not advance past any
     * input.
     *
     * @return true if and only if this scanner's next token is a valid
     *         {@code BigInteger}
     * @throws IllegalStateException if this scanner is closed
     */
    public boolean hasNextBigInteger() {
        return hasNextBigInteger(defaultRadix);
    }

    /**
     * Returns true if the next token in this scanner's input can be
     * interpreted as a {@code BigInteger} in the specified radix using
     * the {@link #nextBigInteger} method. The scanner does not advance past
     * any input.
     *
     * <p>If the radix is less than {@link Character#MIN_RADIX Character.MIN_RADIX}
     * or greater than {@link Character#MAX_RADIX Character.MAX_RADIX}, then an
     * {@code IllegalArgumentException} is thrown.
     *
     * @param radix the radix used to interpret the token as an integer
     * @return true if and only if this scanner's next token is a valid
     *         {@code BigInteger}
     * @throws IllegalStateException if this scanner is closed
     * @throws IllegalArgumentException if the radix is out of range
     */
    public boolean hasNextBigInteger(int radix) {
        setRadix(radix);
        boolean result = hasNext(integerPattern());
        if (result) { // Cache it
            try {
                String s = (matcher.group(SIMPLE_GROUP_INDEX) == null) ?
                    processIntegerToken(hasNextResult) :
                    hasNextResult;
                typeCache = new BigInteger(s, radix);
            } catch (NumberFormatException nfe) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Scans the next token of the input as a {@link java.math.BigInteger
     * BigInteger}.
     *
     * <p> An invocation of this method of the form
     * {@code nextBigInteger()} behaves in exactly the same way as the
     * invocation {@code nextBigInteger(radix)}, where {@code radix}
     * is the default radix of this scanner.
     *
     * @return the {@code BigInteger} scanned from the input
     * @throws InputMismatchException
     *         if the next token does not match the <i>Integer</i>
     *         regular expression, or is out of range
     * @throws NoSuchElementException if the input is exhausted
     * @throws IllegalStateException if this scanner is closed
     */
    public BigInteger nextBigInteger() {
        return nextBigInteger(defaultRadix);
    }

    /**
     * Scans the next token of the input as a {@link java.math.BigInteger
     * BigInteger}.
     *
     * <p> If the next token matches the <a
     * href="#Integer-regex"><i>Integer</i></a> regular expression defined
     * above then the token is converted into a {@code BigInteger} value as if
     * by removing all group separators, mapping non-ASCII digits into ASCII
     * digits via the {@link Character#digit Character.digit}, and passing the
     * resulting string to the {@link
     * java.math.BigInteger#BigInteger(java.lang.String)
     * BigInteger(String, int)} constructor with the specified radix.
     *
     * <p>If the radix is less than {@link Character#MIN_RADIX Character.MIN_RADIX}
     * or greater than {@link Character#MAX_RADIX Character.MAX_RADIX}, then an
     * {@code IllegalArgumentException} is thrown.
     *
     * @param radix the radix used to interpret the token
     * @return the {@code BigInteger} scanned from the input
     * @throws InputMismatchException
     *         if the next token does not match the <i>Integer</i>
     *         regular expression, or is out of range
     * @throws NoSuchElementException if the input is exhausted
     * @throws IllegalStateException if this scanner is closed
     * @throws IllegalArgumentException if the radix is out of range
     */
    public BigInteger nextBigInteger(int radix) {
        // Check cached result
        if ((typeCache != null) && (typeCache instanceof BigInteger val)
            && this.radix == radix) {
            useTypeCache();
            return val;
        }
        setRadix(radix);
        clearCaches();
        // Search for next int
        try {
            String s = next(integerPattern());
            if (matcher.group(SIMPLE_GROUP_INDEX) == null)
                s = processIntegerToken(s);
            return new BigInteger(s, radix);
        } catch (NumberFormatException nfe) {
            position = matcher.start(); // don't skip bad token
            throw new InputMismatchException(nfe.getMessage());
        }
    }

    /**
     * Returns true if the next token in this scanner's input can be
     * interpreted as a {@code BigDecimal} using the
     * {@link #nextBigDecimal} method. The scanner does not advance past any
     * input.
     *
     * @return true if and only if this scanner's next token is a valid
     *         {@code BigDecimal}
     * @throws IllegalStateException if this scanner is closed
     */
    public boolean hasNextBigDecimal() {
        setRadix(10);
        boolean result = hasNext(decimalPattern());
        if (result) { // Cache it
            try {
                String s = processFloatToken(hasNextResult);
                typeCache = new BigDecimal(s);
            } catch (NumberFormatException nfe) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Scans the next token of the input as a {@link java.math.BigDecimal
     * BigDecimal}.
     *
     * <p> If the next token matches the <a
     * href="#Decimal-regex"><i>Decimal</i></a> regular expression defined
     * above then the token is converted into a {@code BigDecimal} value as if
     * by removing all group separators, mapping non-ASCII digits into ASCII
     * digits via the {@link Character#digit Character.digit}, and passing the
     * resulting string to the {@link
     * java.math.BigDecimal#BigDecimal(java.lang.String) BigDecimal(String)}
     * constructor.
     *
     * @return the {@code BigDecimal} scanned from the input
     * @throws InputMismatchException
     *         if the next token does not match the <i>Decimal</i>
     *         regular expression, or is out of range
     * @throws NoSuchElementException if the input is exhausted
     * @throws IllegalStateException if this scanner is closed
     */
    public BigDecimal nextBigDecimal() {
        // Check cached result
        if ((typeCache != null) && (typeCache instanceof BigDecimal val)) {
            useTypeCache();
            return val;
        }
        setRadix(10);
        clearCaches();
        // Search for next float
        try {
            String s = processFloatToken(next(decimalPattern()));
            return new BigDecimal(s);
        } catch (NumberFormatException nfe) {
            position = matcher.start(); // don't skip bad token
            throw new InputMismatchException(nfe.getMessage());
        }
    }

    /**
     * Resets this scanner.
     *
     * <p> Resetting a scanner discards all of its explicit state
     * information which may have been changed by invocations of
     * {@link #useDelimiter useDelimiter()},
     * {@link #useLocale useLocale()}, or
     * {@link #useRadix useRadix()}.
     *
     * <p> An invocation of this method of the form
     * {@code scanner.reset()} behaves in exactly the same way as the
     * invocation
     *
     * <blockquote><pre>{@code
     *   scanner.useDelimiter("\\p{javaWhitespace}+")
     *          .useLocale(Locale.getDefault(Locale.Category.FORMAT))
     *          .useRadix(10);
     * }</pre></blockquote>
     *
     * @return this scanner
     *
     * @since 1.6
     */
    public Scanner reset() {
        delimPattern = WHITESPACE_PATTERN;
        useLocale(Locale.getDefault(Locale.Category.FORMAT));
        useRadix(10);
        clearCaches();
        modCount++;
        return this;
    }

    /**
     * Returns a stream of delimiter-separated tokens from this scanner. The
     * stream contains the same tokens that would be returned, starting from
     * this scanner's current state, by calling the {@link #next} method
     * repeatedly until the {@link #hasNext} method returns false.
     *
     * <p>The resulting stream is sequential and ordered. All stream elements are
     * non-null.
     *
     * <p>Scanning starts upon initiation of the terminal stream operation, using the
     * current state of this scanner. Subsequent calls to any methods on this scanner
     * other than {@link #close} and {@link #ioException} may return undefined results
     * or may cause undefined effects on the returned stream. The returned stream's source
     * {@code Spliterator} is <em>fail-fast</em> and will, on a best-effort basis, throw a
     * {@link java.util.ConcurrentModificationException} if any such calls are detected
     * during stream pipeline execution.
     *
     * <p>After stream pipeline execution completes, this scanner is left in an indeterminate
     * state and cannot be reused.
     *
     * <p>If this scanner contains a resource that must be released, this scanner
     * should be closed, either by calling its {@link #close} method, or by
     * closing the returned stream. Closing the stream will close the underlying scanner.
     * {@code IllegalStateException} is thrown if the scanner has been closed when this
     * method is called, or if this scanner is closed during stream pipeline execution.
     *
     * <p>This method might block waiting for more input.
     *
     * @apiNote
     * For example, the following code will create a list of
     * comma-delimited tokens from a string:
     *
     * <pre>{@code
     * List<String> result = new Scanner("abc,def,,ghi")
     *     .useDelimiter(",")
     *     .tokens()
     *     .collect(Collectors.toList());
     * }</pre>
     *
     * <p>The resulting list would contain {@code "abc"}, {@code "def"},
     * the empty string, and {@code "ghi"}.
     *
     * @return a sequential stream of token strings
     * @throws IllegalStateException if this scanner is closed
     * @since 9
     */
    public Stream<String> tokens() {
        ensureOpen();
        Stream<String> stream = StreamSupport.stream(new TokenSpliterator(), false);
        return stream.onClose(this::close);
    }

    class TokenSpliterator extends Spliterators.AbstractSpliterator<String> {
        int expectedCount = -1;

        TokenSpliterator() {
            super(Long.MAX_VALUE,
                  Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.ORDERED);
        }

        @Override
        public boolean tryAdvance(Consumer<? super String> cons) {
            if (expectedCount >= 0 && expectedCount != modCount) {
                throw new ConcurrentModificationException();
            }

            if (hasNext()) {
                String token = next();
                expectedCount = modCount;
                cons.accept(token);
                if (expectedCount != modCount) {
                    throw new ConcurrentModificationException();
                }
                return true;
            } else {
                expectedCount = modCount;
                return false;
            }
        }
    }

    /**
     * Returns a stream of match results from this scanner. The stream
     * contains the same results in the same order that would be returned by
     * calling {@code findWithinHorizon(pattern, 0)} and then {@link #match}
     * successively as long as {@link #findWithinHorizon findWithinHorizon()}
     * finds matches.
     *
     * <p>The resulting stream is sequential and ordered. All stream elements are
     * non-null.
     *
     * <p>Scanning starts upon initiation of the terminal stream operation, using the
     * current state of this scanner. Subsequent calls to any methods on this scanner
     * other than {@link #close} and {@link #ioException} may return undefined results
     * or may cause undefined effects on the returned stream. The returned stream's source
     * {@code Spliterator} is <em>fail-fast</em> and will, on a best-effort basis, throw a
     * {@link java.util.ConcurrentModificationException} if any such calls are detected
     * during stream pipeline execution.
     *
     * <p>After stream pipeline execution completes, this scanner is left in an indeterminate
     * state and cannot be reused.
     *
     * <p>If this scanner contains a resource that must be released, this scanner
     * should be closed, either by calling its {@link #close} method, or by
     * closing the returned stream. Closing the stream will close the underlying scanner.
     * {@code IllegalStateException} is thrown if the scanner has been closed when this
     * method is called, or if this scanner is closed during stream pipeline execution.
     *
     * <p>As with the {@link #findWithinHorizon findWithinHorizon()} methods, this method
     * might block waiting for additional input, and it might buffer an unbounded amount of
     * input searching for a match.
     *
     * @apiNote
     * For example, the following code will read a file and return a list
     * of all sequences of characters consisting of seven or more Latin capital
     * letters:
     *
     * <pre>{@code
     * try (Scanner sc = new Scanner(Path.of("input.txt"))) {
     *     Pattern pat = Pattern.compile("[A-Z]{7,}");
     *     List<String> capWords = sc.findAll(pat)
     *                               .map(MatchResult::group)
     *                               .collect(Collectors.toList());
     * }
     * }</pre>
     *
     * @param pattern the pattern to be matched
     * @return a sequential stream of match results
     * @throws NullPointerException if pattern is null
     * @throws IllegalStateException if this scanner is closed
     * @since 9
     */
    public Stream<MatchResult> findAll(Pattern pattern) {
        Objects.requireNonNull(pattern);
        ensureOpen();
        Stream<MatchResult> stream = StreamSupport.stream(new FindSpliterator(pattern), false);
        return stream.onClose(this::close);
    }

    /**
     * Returns a stream of match results that match the provided pattern string.
     * The effect is equivalent to the following code:
     *
     * <pre>{@code
     *     scanner.findAll(Pattern.compile(patString))
     * }</pre>
     *
     * @param patString the pattern string
     * @return a sequential stream of match results
     * @throws NullPointerException if patString is null
     * @throws IllegalStateException if this scanner is closed
     * @throws PatternSyntaxException if the regular expression's syntax is invalid
     * @since 9
     * @see java.util.regex.Pattern
     */
    public Stream<MatchResult> findAll(String patString) {
        Objects.requireNonNull(patString);
        ensureOpen();
        return findAll(patternCache.forName(patString));
    }

    class FindSpliterator extends Spliterators.AbstractSpliterator<MatchResult> {
        final Pattern pattern;
        int expectedCount = -1;
        private boolean advance = false; // true if we need to auto-advance

        FindSpliterator(Pattern pattern) {
            super(Long.MAX_VALUE,
                  Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.ORDERED);
            this.pattern = pattern;
        }

        @Override
        public boolean tryAdvance(Consumer<? super MatchResult> cons) {
            ensureOpen();
            if (expectedCount >= 0) {
                if (expectedCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            } else {
                // init
                matchValid = false;
                matcher.usePattern(pattern);
                expectedCount = modCount;
            }

            while (true) {
                // assert expectedCount == modCount
                if (nextInBuffer()) { // doesn't increment modCount
                    cons.accept(matcher.toMatchResult());
                    if (expectedCount != modCount) {
                        throw new ConcurrentModificationException();
                    }
                    return true;
                }
                if (needInput)
                    readInput(); // doesn't increment modCount
                else
                    return false; // reached end of input
            }
        }

        // reimplementation of findPatternInBuffer with auto-advance on zero-length matches
        private boolean nextInBuffer() {
            if (advance) {
                if (position + 1 > buf.limit()) {
                    if (!sourceClosed)
                        needInput = true;
                    return false;
                }
                position++;
                advance = false;
            }
            matcher.region(position, buf.limit());
            if (matcher.find() && (!matcher.hitEnd() || sourceClosed)) {
                 // Did not hit end, or hit real end
                 position = matcher.end();
                 advance = matcher.start() == position;
                 return true;
            }
            if (!sourceClosed)
                needInput = true;
            return false;
        }
    }

    /** Small LRU cache of Patterns. */
    private static class PatternLRUCache {

        private Pattern[] oa = null;
        private final int size;

        PatternLRUCache(int size) {
            this.size = size;
        }

        boolean hasName(Pattern p, String s) {
            return p.pattern().equals(s);
        }

        void moveToFront(Object[] oa, int i) {
            Object ob = oa[i];
            for (int j = i; j > 0; j--)
                oa[j] = oa[j - 1];
            oa[0] = ob;
        }

        Pattern forName(String name) {
            if (oa == null) {
                Pattern[] temp = new Pattern[size];
                oa = temp;
            } else {
                for (int i = 0; i < oa.length; i++) {
                    Pattern ob = oa[i];
                    if (ob == null)
                        continue;
                    if (hasName(ob, name)) {
                        if (i > 0)
                            moveToFront(oa, i);
                        return ob;
                    }
                }
            }

            // Create a new object
            Pattern ob = Pattern.compile(name);
            oa[oa.length - 1] = ob;
            moveToFront(oa, oa.length - 1);
            return ob;
        }
    }
}
