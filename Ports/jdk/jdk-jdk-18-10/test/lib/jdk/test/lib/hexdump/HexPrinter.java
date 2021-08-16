/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.hexdump;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Decode a sequence of bytes to a readable format.
 * <p>
 * The predefined formats are:
 * <DL>
 * <DT>{@link #minimal() Minimal format}:  {@code "Now is the time for Java.\n"}</DT>
 * <DD><pre>    {@code
 *     4e6f77206973207468652074696d6520666f72204a6176612e0a} </pre>
 * </DD>
 *
 * <DT>{@link #simple() Simple format}: {@code "Now is the time for Java.\n"}</DT>
 * <DD><pre>{@code
 *     0: 4e 6f 77 20 69 73 20 74 68 65 20 74 69 6d 65 20  // Now is the time for Java.\n
 *    16: 66 6f 72 20 4a 61 76 61 2e 0a} </pre>
 * </DD>
 *
 * <DT>{@link #canonical() Canonical format}: {@code "Now is the time for Java.\n"}</DT>
 * <DD><pre>{@code
 *     00000000  4e 6f 77 20 69 73 20 74 68 65 20 74 69 6d 65 20 |Now is the time |
 *     00000010  66 6f 72 20 4a 61 76 61 2e 0a                   |for Java.|} </pre>
 * </DD>
 * <DT>{@link #source() Byte array initialization source}: {@code "Now is the time for Java.\n"}</DT>
 * <DD><pre>{@code
 *     (byte) 78, (byte)111, (byte)119, (byte) 32, (byte)105, (byte)115, (byte) 32, (byte)116,  // Now is t
 *     (byte)104, (byte)101, (byte) 32, (byte)116, (byte)105, (byte)109, (byte)101, (byte) 32,  // he time
 *     (byte)102, (byte)111, (byte)114, (byte) 32, (byte) 74, (byte) 97, (byte)118, (byte) 97,  // for Java
 *     (byte) 46, (byte) 10,                                                                    // .\n}</pre>
 * </DD>
 * </DL>
 * <p>
 * The static factories {@link #minimal minimal}, {@link #simple simple},
 * {@link #canonical canonical}, and {@link #source() Java source}
 * return predefined {@code HexPrinter}s for the formats above.
 * HexPrinter holds the formatting parameters that control the width and formatting
 * of each of the offset, byte values, and formatted output.
 * New HexPrinters with different parameters are created using an existing HexPrinter
 * as a template with the methods {@link #formatter(Formatter)},
 * {@link #withBytesFormat(String, int)}, {@link #withOffsetFormat(String)},
 * and {@link #withLineSeparator(String)}.
 * <p>
 * The source of the bytes includes byte arrays, InputStreams, and ByteBuffers.
 * For example, {@link #toString(InputStream)} reads the input and returns a String.
 * Each of the {@code toString(...)} methods immediately reads and
 * formats all of the bytes from the source and returns a String.
 * <p>
 * Each of the  {@code format(...)} methods immediately reads and
 * formats all of the bytes from the source and appends it to the destination.
 * For example, {@link #format(InputStream)} reads the input and
 * appends the output to {@link System#out System.out} unless the
 * {@link #dest(Appendable) destination} is changed to an {@link Appendable}
 * such as {@link PrintStream}, {@link StringBuilder}, or {@link Writer}.
 * <p>
 * {@linkplain Formatter Formatter} functions read and interpret the bytes to show the
 * structure and content of a protocol or data stream.
 * Built-in formatters include {@link HexPrinter#formatter(Class, String) primitives},
 * {@link Formatters#PRINTABLE printable bytes},
 * and {@link Formatters#utf8Parser(DataInputStream, Appendable) UTF-8 strings}.
 * The {@link #formatter(Formatter, String, int) formatter} method sets the
 * formatting function, the delimiter, and the width.
 * Custom formatter functions can be implemented as a lambda, a method, an inner class, or a concrete class.
 * <p>
 * The format of each line is customizable.
 * The {@link #withOffsetFormat(String) withOffsetFormat} method controls
 * the format of the byte offset.
 * The {@link #withBytesFormat(String, int) withBytesFormat} method controls
 * the printing of each byte value including the separator,
 * and the maximum number of byte values per line.
 * The offset and byte values are formatted using the familiar
 * {@link String#format String formats} with spacing
 * and delimiters included in the format string.
 * The {@link #withLineSeparator(String) withLineSeparator} method sets
 * the line separator.
 * <p>
 * Examples:
 * <UL>
 * <LI> Encoding bytes to a minimal string.
 * <pre>{@code
 * byte[] bytes = new byte[] { ' ', 0x41, 0x42, '\n'};
 * String s = HexPrinter.minimal().toString(bytes);
 * Result: "2041420a"
 * }</pre>
 * <LI>Simple formatting of a byte array.
 * <pre>{@code
 * byte[] bytes = new byte[] { ' ', 0x41, 0x42, '\n'};
 * String s = HexPrinter.simple().toString(bytes);
 * Result:    0: 20 41 42 0a                                      //  AB\n
 * }</pre>
 * <LI>Simple formatting of a ByteBuffer.
 * <pre>{@code
 * ByteBuffer bb = ByteBuffer.wrap(bytes);
 * String s = HexPrinter.simple().toString(bb);
 * Result:    0: 20 41 42 0a                                      //  AB\n
 * }</pre>
 * <LI>Simple formatting of ranges of a byte array to System.err.
 * <pre>{@code
 * byte[] bytes = new byte[] { ' ', 0x41, 0x42, 0x43, 0x44, '\n'};
 * HexPrinter hex = HexPrinter.simple()
 *                            .dest(System.err);
 *                            .format(bytes, 1, 2)
 *                            .format(bytes, 3, 2);
 * Result:
 * 1: 41 42                                            // AB
 * 3: 43 44                                            // CD
 * }</pre>
 * </UL>
 * <p>
 * This is a <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * class; programmers should treat instances that are
 * {@linkplain #equals(Object) equal} as interchangeable and should not
 * use instances for synchronization, or unpredictable behavior may
 * occur. For example, in a future release, synchronization may fail.
 * The {@code equals} method should be used for comparisons.
 *
 * <p>
 * This class is immutable and thread-safe.
 */
public final class HexPrinter {

    /**
     * Mnemonics for control characters.
     */
    static final String[] CONTROL_MNEMONICS = {
            "nul", "soh", "stx", "etx", "eot", "enq", "ack", "bel",
            "b", "t", "n", "vt", "f", "r", "so", "si",
            "dle", "dc1", "dc2", "dc3", "dc4", "nak", "syn", "etb",
            "can", "em", "sub", "esc", "fs", "gs", "rs", "us"
    };
    private static final int initBytesCount = 16;   // 16 byte values
    private static final String initBytesFormat = "%02x ";
    private static final int initAnnoWidth = initBytesCount * 4;
    private static final String initAnnoDelim = " // ";

    final Appendable dest;              // Final output target
    final String offsetFormat;          // Byte offset Formatter String
    final String bytesFormat;           // Hex bytes Formatter string
    final int bytesCount;               // Maximum number of byte values per line
    final String annoDelim;             // Annotation delimiter
    final int annoWidth;                // Annotation field width (characters)
    final String lineSeparator;         // End of line separator
    final Formatter annoFormatter;      // formatter function

    /**
     * Construct a new HexPrinter with all new values.
     *
     * @param printer       the formatter
     * @param offsetFormat  the offset format
     * @param bytesFormat   the bytes format
     * @param bytesCount    the count of bytes per line
     * @param annoDelim     the delimiter before the annotation
     * @param annoWidth     the width of the annotation
     * @param lineSeparator the line separator
     * @param dest          the destination
     */
    private HexPrinter(Formatter printer, String offsetFormat, String bytesFormat, int bytesCount,
                       String annoDelim, int annoWidth,
                       String lineSeparator, Appendable dest) {
        this.annoFormatter = Objects.requireNonNull(printer, "formatter");
        this.bytesCount = bytesCount;
        this.bytesFormat = Objects.requireNonNull(bytesFormat, bytesFormat);
        this.offsetFormat = Objects.requireNonNull(offsetFormat, "offsetFormat");
        this.annoDelim = Objects.requireNonNull(annoDelim, "annoDelim");
        this.annoWidth = annoWidth;
        this.lineSeparator = Objects.requireNonNull(lineSeparator, "lineSeparator");
        this.dest = Objects.requireNonNull(dest, "dest");
    }

    /**
     * Returns a new HexPrinter setting the parameters to produce a minimal string.
     * The parameters are set to:
     * <UL>
     * <LI>byte offset format: none {@code ""},
     * <LI>each byte value is formatted as 2 hex digits: {@code "%02x"},
     * <LI>maximum number of byte values per line: unbounded,
     * <LI>delimiter for the annotation: none {@code ""},
     * <LI>formatter: {@link Formatters#NONE does not output a formatted byte}, and
     * <LI>destination: {@link System#out System.out}.
     * </UL>
     * Example,
     * <pre>
     * {@code     byte[] bytes = new byte[] { ' ', 0x41, 0x42, '\n'};
     *     String s = HexPrinter.minimal()
     *             .toString(bytes);
     *     Result: "2041420a"
     * }</pre>
     *
     * @return a new HexPrinter
     */
    public static HexPrinter minimal() {
        return new HexPrinter(Formatters.NONE, "",
                "%02x", initBytesCount,
                "", initAnnoWidth, "",
                System.out);
    }

    /**
     * Returns a new HexPrinter setting the parameters to produce canonical output.
     * The parameters are set to:
     * <UL>
     * <LI>byte offset format: {@code "%08x  "},
     * <LI>each byte value is formatted as 2 hex digits and a space: {@code "%02x "},
     * <LI>maximum number of byte values per line: {@value initBytesCount},
     * <LI>delimiter for the annotation: {@code "|"},
     * <LI>formatter: {@link Formatters#PRINTABLE printable bytes}, and
     * <LI>line separator: "|" + {@link  System#lineSeparator()},
     * <LI>destination: {@link System#out System.out}.
     * </UL>
     * Example,
     * <pre>
     * {@code     byte[] bytes = new byte[] { ' ', 0x41, 0x42, '\n'};
     *     String s = HexPrinter.canonical()
     *             .toString(bytes);
     *
     *     Result: "00000000  20 41 42 0a                                     | AB|"
     * }</pre>
     *
     * @return a new HexPrinter
     */
    public static HexPrinter canonical() {
        return new HexPrinter(Formatters.PRINTABLE, "%08x  ",
                "%02x ", initBytesCount,
                "|", 31, "|" + System.lineSeparator(),
                System.out);
    }

    /**
     * Returns a new HexPrinter setting simple formatting parameters to output
     * to a multi-line string.
     * The parameters are set to:
     * <UL>
     * <LI>byte offset format: hexadecimal width 4, colon, and a space, {@code "%04x: "},
     * <LI>each byte value is formatted as 2 hex digits and a space: {@code "%02x "},
     * <LI>maximum number of byte values per line: {@value initBytesCount},
     * <LI>delimiter for the annotation: {@code " // "},
     * <LI>width for the annotation: {@value initAnnoWidth},
     * <LI>line separator: {@link System#lineSeparator()},
     * <LI>formatter: {@link Formatters#ASCII ASCII bytes}
     * showing printable characters, mnemonics for control chars, and
     * otherwise the decimal byte values,
     * <LI>destination default: {@link System#out System.out}.
     * </UL>
     * Example,
     * <pre>
     * {@code    byte[] bytes = new byte[] { ' ', 0x41, 0x42, '\n'};
     *    String s = HexPrinter.simple()
     *            .toString(bytes);
     *
     *    Result: "    0: 20 41 42 0a                                      //  AB\n"
     * }</pre>
     *
     * @return a new HexPrinter
     */
    public static HexPrinter simple() {
        return new HexPrinter(Formatters.ASCII, "%04x: ",
                initBytesFormat, initBytesCount,
                initAnnoDelim, initAnnoWidth, System.lineSeparator(),
                System.out);
    }

    /**
     * Returns a new HexPrinter setting formatting parameters to output
     * to a multi-line string as a byte array initialization for Java source.
     * The parameters are set to:
     * <UL>
     * <LI>byte offset format: 4 space indent: {@code "    "},
     * <LI>each byte value is formatted as: {@code "(byte)%3d, "},
     * <LI>maximum number of byte values per line: {@code 8},
     * <LI>delimiter for the annotation: {@code " // "},
     * <LI>width for the annotation: {@value initAnnoWidth},
     * <LI>line separator: {@link System#lineSeparator()},
     * <LI>formatter: {@link Formatters#PRINTABLE printable bytes}
     * showing printable characters and otherwise ".",
     * <LI>destination default: {@link System#out System.out}.
     * </UL>
     *
     * @return a new HexPrinter
     */
    public static HexPrinter source() {
        return new HexPrinter(Formatters.PRINTABLE, "    ",
                "(byte)%3d, ", 8,
                " // ", initAnnoWidth, System.lineSeparator(),
                System.out);
    }

    /**
     * Returns a new HexPrinter setting the destination to the Appendable.
     * {@code Appendable} classes include: {@link PrintStream}, {@link Writer},
     * {@link StringBuilder}, and {@link StringBuffer}.
     *
     * @param dest the Appendable destination for the output, non-null
     * @return a new HexPrinter
     * @throws UncheckedIOException if an I/O error occurs
     */
    public HexPrinter dest(Appendable dest) {
        Objects.requireNonNull(dest, "dest");
        return new HexPrinter(annoFormatter, offsetFormat,
                bytesFormat, bytesCount, annoDelim,
                annoWidth, lineSeparator, dest);
    }

    /**
     * The formatter function is called repeatedly to read all of the bytes
     * and append the output.
     * All output is appended and flushed to the destination.
     * <p>
     * The result is equivalent to calling
     * {@code format(new ByteArrayInputStream(source))}.
     *
     * @param source a non-null array of bytes.
     * @return this HexPrinter
     * @throws java.io.UncheckedIOException if an I/O error occurs
     */
    public HexPrinter format(byte[] source) {
        Objects.requireNonNull(source, "byte array must be non-null");
        return format(new ByteArrayInputStream(source));
    }

    /**
     * The formatter function is called repeatedly to read the bytes from offset
     * for length and append the output.
     * All output is appended and flushed to the destination.
     * Only {@code length} bytes starting at the {@code offset} are formatted.
     * <p>
     * The result is equivalent to calling
     * {@code format(new ByteArrayInputStream(source, offset, len))}.
     *
     * @param source a non-null array of bytes.
     * @param offset the offset into the array to start
     * @param length the length of bytes in the array to format
     * @return this HexPrinter
     * @throws java.io.UncheckedIOException if an I/O error occurs
     */
    public HexPrinter format(byte[] source, int offset, int length) {
        Objects.requireNonNull(source, "byte array must be non-null");
        return format(new ByteArrayInputStream(source, offset, length), offset);
    }

    /**
     * The formatter function is called repeatedly to read all of the bytes
     * and append the output.
     * All output is appended and flushed to the destination.
     * <p>
     * The {@code format} method invokes the {@code formatter} to read bytes from the
     * source and append the formatted sequence of byte values to the destination.
     * As the bytes are read they are printed using the {@link #withBytesFormat}
     * to fill the bytes values of the output destination.
     * The output of the {@code formatter} fills the annotation field.
     * A new line is started when either the byte values or annotation
     * is filled to its respective width. The offset of the first byte on the line
     * is inserted at the beginning of each line using {@link #withOffsetFormat(String)}.
     * <p>
     * This method may block indefinitely reading from the input stream,
     * or writing to the output stream. The behavior for the case where
     * the input and/or output stream is asynchronously closed,
     * or the thread interrupted during the transfer, is highly input
     * and output stream specific, and therefore not specified.
     * <p>
     * If an I/O error occurs reading from the input stream or
     * writing to the output stream, then it may do so after some bytes
     * have been read or written. Consequently the input stream
     * may not be at end of stream and one, or both, streams may be
     * in an inconsistent state. It is strongly recommended that both streams
     * be promptly closed if an I/O error occurs.
     *
     * @param source an InputStream to read from, the stream not closed and
     *               is at end-of-file.
     * @return this HexPrinter
     * @throws java.io.UncheckedIOException if an I/O error occurs
     */
    public HexPrinter format(InputStream source) {
        return format(source, 0);
    }

    /**
     * Format an InputStream and supply the initial offset.
     *
     * @param source an InputStream
     * @param offset an offset
     * @return this HexPrinter
     */
    private HexPrinter format(InputStream source, int offset) {
        Objects.requireNonNull(source, "InputStream must be non-null");
        try (AnnotationWriter writer =
                     new AnnotationWriter(this, source, offset, dest)) {
            writer.flush();
            return this;
        }
    }

    /**
     * The formatter function is called for the range of the ByteBuffer's contents.
     * All annotation output is appended and flushed to the output destination.
     * The ByteBuffer position is not used and not modified.
     *
     * @param source a ByteBuffer
     * @param index the index in the ByteBuffer, must be non-negative and
     *              less than {@code limit()}.
     * @param length the length in the ByteBuffer must be non-negative and
     *               no larger than {@code source.limit() - index}
     * @return this HexPrinter
     * @throws java.io.UncheckedIOException if an I/O error occurs
     * @throws java.lang.IndexOutOfBoundsException if the preconditions on
     *          {@code index} and {@code length} do not hold
     */
    public HexPrinter format(ByteBuffer source, int index, int length) {
        Objects.requireNonNull(source, "ByteBuffer must be non-null");
        byte[] bytes = new byte[length];
        source.get(index, bytes, 0, length);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return format(bais, index);
    }

    /**
     * The formatter function is called for the ByteBuffer's contents.
     * The source bytes are from the {@code ByteBuffer.position()}
     * to the {@code ByteBuffer.limit()}.
     * The position is not modified.
     * All output is appended and flushed to the destination.
     *
     * @param source a ByteBuffer
     * @return this HexPrinter
     * @throws java.io.UncheckedIOException if an I/O error occurs
     */
    public HexPrinter format(ByteBuffer source) {
        return format(source, source.position(), source.limit() - source.position());
    }

    /**
     * The formatter function is called repeatedly to read all of the bytes
     * and return a String.
     *
     * @param source a non-null array of bytes.
     * @return the output as a non-null {@code String}
     * @throws java.io.UncheckedIOException if an I/O error occurs
     */
    public String toString(byte[] source) {
        Objects.requireNonNull(source, "byte array must be non-null");
        return toString(new ByteArrayInputStream(source));
    }

    /**
     * The formatter function is called repeatedly to read the bytes from offset
     * for length and return a String.
     * Only {@code length} bytes starting at the {@code offset} are formatted.
     *
     * @param source a non-null array of bytes.
     * @param offset the offset into the array to start
     * @param length the length of bytes in the array to format
     * @return the output as a non-null {@code String}
     * @throws java.io.UncheckedIOException if an I/O error occurs
     */
    public String toString(byte[] source, int offset, int length) {
        Objects.requireNonNull(source, "byte array must be non-null");
        StringBuilder sb = new StringBuilder();
        try (AnnotationWriter writer =
                     new AnnotationWriter(this, new ByteArrayInputStream(source, offset, length),
                             offset, sb)) {
            writer.flush();
            return sb.toString();
        }
    }

    /**
     * The formatter function is called repeatedly to read all of the bytes
     * and return a String.
     * <p>
     * The {@code toString} method invokes the formatter to read bytes from the
     * source and append the formatted sequence of byte values.
     * As the bytes are read they are printed using the {@link #withBytesFormat}
     * to fill the second field of the line.
     * The output of the {@code formatter} fills the annotation field.
     * A new line is started when either the byte values or annotation
     * is filled to its respective width. The offset of the first byte on the line
     * is inserted at the beginning of each line using {@link #withOffsetFormat(String)}.
     * <p>
     * This method may block indefinitely reading from the input stream,
     * or writing to the output stream. The behavior for the case where
     * the input and/or output stream is asynchronously closed,
     * or the thread interrupted during the transfer, is highly input
     * and output stream specific, and therefore not specified.
     * <p>
     * If an I/O error occurs reading from the input stream or
     * writing to the output stream, then it may do so after some bytes
     * have been read or written. Consequently the input stream
     * may not be at end of stream and one, or both, streams may be
     * in an inconsistent state. It is strongly recommended that both streams
     * be promptly closed if an I/O error occurs.
     *
     * @param source an InputStream to read from, the stream not closed and
     *               is at end-of-file upon return.
     * @return the output as a non-null {@code String}
     * @throws java.io.UncheckedIOException if an I/O error occurs
     */
    public String toString(InputStream source) {
        Objects.requireNonNull(source, "InputStream must be non-null");
        StringBuilder sb = new StringBuilder();
        try (AnnotationWriter writer =
                     new AnnotationWriter(this, source, 0, sb)) {
            writer.flush();
            return sb.toString();
        }
    }

    /**
     * The formatter function is called for the range of the ByteBuffer contents
     * and returned as a string.
     * The ByteBuffer position is not used and not modified.
     *
     * @param source a ByteBuffer
     * @param index the index in the ByteBuffer, must be non-negative and
     *              less than {@code limit()}.
     * @param length the length in the ByteBuffer must be non-negative and
     *               no larger than {@code source.limit() - index}
     * @return the output as a non-null {@code String}
     * @throws java.io.UncheckedIOException if an I/O error occurs
     * @throws java.lang.IndexOutOfBoundsException if the preconditions on
     *          {@code index} and {@code length} do not hold
     */
    public String toString(ByteBuffer source, int index, int length) {
        Objects.requireNonNull(source, "ByteBuffer must be non-null");
        byte[] bytes = new byte[length];
        source.get(index, bytes, 0, length);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        StringBuilder sb = new StringBuilder();
        try (AnnotationWriter writer =
                     new AnnotationWriter(this, bais, index, sb)) {
            writer.flush();
            return sb.toString();
        }
    }

    /**
     * The formatter function is called for the ByteBuffer contents
     * and returned as a string.
     * The source bytes are from the {@code ByteBuffer.position()}
     * to the {@code ByteBuffer.limit()}.
     * The position is not modified.
     *
     * @param source a ByteBuffer
     * @return the output as a non-null {@code String}
     * @throws java.io.UncheckedIOException if an I/O error occurs
     */
    public String toString(ByteBuffer source) {
        return toString(source, source.position(), source.limit() - source.position());
    }

    /**
     * Returns a new HexPrinter setting the format for the byte offset.
     * The format string is specified by {@link String#format String format}
     * including any delimiters. For example, {@code "%3x: "}.
     * If the format is an empty string, there is no offset in the output.
     *
     * @param offsetFormat a new format string for the byte offset.
     * @return a new HexPrinter
     */
    public HexPrinter withOffsetFormat(String offsetFormat) {
        Objects.requireNonNull(offsetFormat, "offsetFormat");
        return new HexPrinter(annoFormatter, offsetFormat, bytesFormat, bytesCount,
                annoDelim, annoWidth, lineSeparator, dest);
    }

    /**
     * Returns a new HexPrinter setting the format for each byte value and
     * the maximum number of byte values per line.
     * The format string is specified by {@link String#format String format},
     * including any delimiters or padding. For example, {@code "%02x "}.
     * If the byteFormat is an empty String, there are no byte values in the output.
     *
     * @param byteFormat a format string for each byte
     * @param bytesCount the maximum number of byte values per line; greater than zero
     * @return a new HexPrinter
     * @throws IllegalArgumentException if bytesCount is less than or equal to zero
     */
    public HexPrinter withBytesFormat(String byteFormat, int bytesCount) {
        Objects.requireNonNull(bytesFormat, "bytesFormat");
        if (bytesCount <= 0)
            throw new IllegalArgumentException("bytesCount should be greater than zero");
        return new HexPrinter(annoFormatter, offsetFormat, byteFormat, bytesCount,
                annoDelim, annoWidth, lineSeparator, dest);
    }

    /**
     * Returns a new HexPrinter setting the line separator.
     * The line separator can be set to an empty string or to
     * a string to be added at the end of each line.  It should include the line
     * separator {@link System#lineSeparator()} if a line break is to be output.
     *
     * @param separator the line separator
     * @return a new HexPrinter
     */
    public HexPrinter withLineSeparator(String separator) {
        return new HexPrinter(annoFormatter, offsetFormat, bytesFormat, bytesCount,
                annoDelim, annoWidth, separator, dest);
    }

    /**
     * Returns a new HexPrinter setting the formatter.
     * The widths, delimiters and other parameters are unchanged.
     *
     * @param formatter a non-null Formatter
     * @return a new HexPrinter
     */
    public HexPrinter formatter(Formatter formatter) {
        Objects.requireNonNull(formatter, "Formatter must be non-null");
        return new HexPrinter(formatter, offsetFormat, bytesFormat, bytesCount,
                annoDelim, annoWidth, lineSeparator, dest);
    }

    /**
     * Returns a new HexPrinter setting the formatter, delimiter, and width of the annotation.
     * Note: The annotations may exceed the width.
     *
     * @param formatter a non-null Formatter
     * @param delim     a string delimiter for the annotation
     * @param width     the width of the annotation, non-negative
     * @return a new HexPrinter
     */
    public HexPrinter formatter(Formatter formatter, String delim, int width) {
        Objects.requireNonNull(formatter, "formatter");
        Objects.requireNonNull(delim, "delim");
        return new HexPrinter(formatter, offsetFormat, bytesFormat, bytesCount,
                delim, width, lineSeparator, dest);
    }

    /**
     * Returns a new HexPrinter setting the formatter to format a primitive type
     * using the format string.
     * The format string should include any pre or post spacing and delimiters.
     * <p>
     * This is a convenience function equivalent to finding a formatter using
     * {@link HexPrinter.Formatters#ofPrimitive}.
     * </p>
     *
     * @param primClass a primitive class, for example, {@code int.class}
     * @param fmtString a {@link java.util.Formatter format string}.
     * @return a new HexPrinter
     * @throws IllegalArgumentException if the class is not a primitive class
     */
    public HexPrinter formatter(Class<?> primClass, String fmtString) {
        Formatter formatter = getFormatter(primClass, fmtString);
        return new HexPrinter(formatter, offsetFormat, bytesFormat, bytesCount,
                annoDelim, annoWidth, lineSeparator, dest);
    }

    /**
     * Returns a formatter for the primitive type using the format string.
     * The formatter reads a value of the primitive type from the stream
     * and formats it using the format string.
     * The format string includes any pre or post spacing and delimiters.
     *
     * @param primClass a primitive class, for example, {@code int.class}
     * @param fmtString a {@link java.util.Formatter format string}
     * @return a Formatter for the primitive type using the format string
     */
    static Formatter getFormatter(Class<?> primClass, String fmtString) {
        return new PrimitiveFormatter(primClass, fmtString);
    }

    /**
     * Returns a string describing this HexPrinter.
     * The string indicates the type of the destination and
     * the formatting options.
     *
     * @return a String describing this HexPrinter
     */
    public String toString() {
        return "formatter: " + annoFormatter
                + ", dest: " + dest.getClass().getName()
                + ", offset: \"" + offsetFormat
                + "\", bytes: " + bytesCount
                + " x \"" + bytesFormat + "\""
                + ", delim: \"" + annoDelim + "\""
                + ", width: " + annoWidth
                + ", nl: \"" + expand(lineSeparator) + "\"";
    }

    private String expand(String sep) {
        return sep.replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static class PrimitiveFormatter implements Formatter {

        private final Class<?> primClass;
        private final String fmtString;

        PrimitiveFormatter(Class<?> primClass, String fmtString) {
            Objects.requireNonNull(primClass, "primClass");
            Objects.requireNonNull(fmtString, "fmtString");
            if (!primClass.isPrimitive())
                throw new IllegalArgumentException("Not a primitive type: " + primClass.getName());
            this.primClass = primClass;
            this.fmtString = fmtString;
        }

        public void annotate(DataInputStream in, Appendable out) throws IOException {
            if (primClass == byte.class) {
                int v = in.readByte();
                out.append(String.format(fmtString, v));
            } else if (primClass == boolean.class) {
                boolean v = in.readByte() != 0;
                out.append(String.format(fmtString, v));
            } else if (primClass == short.class | primClass == char.class) {
                int v = in.readShort();
                out.append(String.format(fmtString, v));
            } else if (primClass == float.class) {
                float v = in.readFloat();
                out.append(String.format(fmtString, v));
            } else if (primClass == int.class) {
                int v = in.readInt();
                out.append(String.format(fmtString, v));
            } else if (primClass == double.class) {
                double v = in.readDouble();
                out.append(String.format(fmtString, v));
            } else if (primClass == long.class) {
                long v = in.readLong();
                out.append(String.format(fmtString, v));
            } else {
                throw new AssertionError("missing case on primitive class");
            }
        }

        public String toString() {
            return "(" + primClass.getName() + ", \"" + fmtString + "\")";
        }
    }

    /**
     * Formatter function reads bytes from a stream and
     * appends a readable annotation to the output destination.
     * <p>
     * Each invocation of the {@link #annotate annotate} method reads and annotates
     * a single instance of its protocol or data type.
     * <p>
     * Built-in formatting functions are provided in the {@link Formatters} class.
     * <p>
     * As described by the {@link HexPrinter#toString(InputStream)} method,
     * the {@link #annotate annotate} method is called to read bytes and produce
     * the descriptive annotation.
     * <p>
     * For example, a custom lambda formatter to read a float value (4 bytes) and
     * print as a floating number could be written as a static method.
     * <pre>{@code
     *     // Format 4 bytes read from the input as a float 3.4.
     *     static void annotate(DataInputStream in, Appendable out) throws IOException {
     *         float f = in.readFloat();
     *         out.append(String.format("%3.4f, ", f));
     *     }
     *
     *     byte[] bytes = new byte[] {00 00 00 00 3f 80 00 00 40 00 00 00 40 40 00 00};
     *     HexPrinter pp = HexPrinter.simple()
     *         .withBytesFormat("%02x ", 8)
     *         .formatter(Example::annotate)
     *         .format(bytes);
     *
     * Result:
     *     0: 00 00 00 00 3f 80 00 00  // 0.0000, 1.0000,
     *     8: 40 00 00 00 40 40 00 00  // 2.0000, 3.0000,
     * }</pre>
     *
     * <p>
     * The details of the buffering and calling of the formatter {@code annotate}
     * methods is roughly as follows.
     * The bytes read by the {@code annotate} method are logically buffered
     * for each line of output.
     * The {@code annotate} method writes its description of the bytes read
     * to the output, this output is also buffered.
     * When the number of bytes read exceeds the
     * {@link #withBytesFormat(String, int) byte values count per line},
     * the buffered output exceeds the
     * {@link #formatter(Formatter, String, int) width of the annotation field},
     * or a new line {@code "\n"} character is found in the output then
     * a line of output is assembled and written to the destination Appendable.
     * The formatter's {@code annotate} method is called repeatedly
     * until the input is completely consumed or an exception is thrown.
     * Any remaining buffered bytes or description are flushed to the destination Appendable.
     */
    @FunctionalInterface
    public interface Formatter {

        /**
         * Read bytes from the input stream and append a descriptive annotation
         * to the output destination.
         *
         * @param in  a DataInputStream
         * @param out an Appendable for the output
         * @throws IOException if an I/O error occurs
         */
        void annotate(DataInputStream in, Appendable out) throws IOException;
    }

    /**
     * Built-in formatters for printable byte, ASCII byte, UTF-8 and primitive types.
     * Formatters for primitive types and different formatting options
     * can be found by calling {@link #ofPrimitive(Class, String)}.
     */
    public enum Formatters implements Formatter {
        /**
         * Read a byte, return the value as a single character string
         * if it is printable, otherwise return ".".
         */
        PRINTABLE,
        /**
         * Read a byte and return it as a string.
         * Return the character if it is ASCII, return its mnemonic if it
         * is a control character, otherwise return its decimal value as a string.
         */
        ASCII,
        /**
         * Read a modified UTF-8 string and write it.
         */
        UTF8,
        /**
         * Read a byte and write nothing.
         */
        NONE;

        public void annotate(DataInputStream in, Appendable out) throws IOException {
            switch (this) {
                case PRINTABLE -> bytePrintable(in, out);
                case ASCII -> byteASCII(in, out);
                case UTF8 -> utf8Parser(in, out);
                case NONE -> byteNoneParser(in, out);
            }
        }

        /**
         * Read a byte and return it as a single character string if it is printable,
         * otherwise return ".".
         *
         * @param in  a DataInputStream
         * @param out an Appendable to write to
         * @throws IOException if an I/O error occurs
         */
        static void bytePrintable(DataInputStream in, Appendable out) throws IOException {
            int v = in.readUnsignedByte();
            if (!Character.isISOControl(v) && v < 127) {
                out.append((char) v);
            } else {
                out.append('.');
            }
        }

        /**
         * Read a byte and return it as a string.
         * Append the byte if it is ASCII, its mnemonic if it
         * is a control character, and otherwise its decimal value.
         *
         * @param in  a DataInputStream
         * @param out an Appendable to write to
         * @throws IOException if an I/O error occurs
         */
        static void byteASCII(DataInputStream in, Appendable out) throws IOException {
            int v = in.readUnsignedByte();
            if (v < 32) {
                out.append('\\').append(CONTROL_MNEMONICS[v]);
            } else if (v < 127) {
                out.append((char) v);
            } else {
                out.append('\\').append(Integer.toString(v, 10));
            }
        }

        /**
         * Read a modified UTF-8 string and write it to the output destination.
         *
         * @param in  a DataInputStream
         * @param out an Appendable to write the output to
         * @throws IOException if an I/O error occurs
         */
        static void utf8Parser(DataInputStream in, Appendable out) throws IOException {
            out.append(in.readUTF()).append(" ");
        }

        /**
         * Read a a byte and write nothing.
         *
         * @param in  a DataInputStream
         * @param out an Appendable to write the output to
         * @throws IOException if an I/O error occurs
         */
        static void byteNoneParser(DataInputStream in, Appendable out) throws IOException {
            in.readByte();
        }

        /**
         * Returns a {@code Formatter} for a primitive using the format string.
         * The format string includes any pre or post spacing or delimiters.
         * A value of the primitive is read using the type specific methods
         * of {@link DataInputStream}, formatted using the format string, and
         * written to the output.
         *
         * @param primClass a primitive class, for example, {@code int.class}
         * @param fmtString a {@link java.util.Formatter format string}.
         * @return a Formatter
         */
        public static Formatter ofPrimitive(Class<?> primClass, String fmtString) {
            Objects.requireNonNull(primClass, "primClass");
            Objects.requireNonNull(fmtString, "fmtString");
            return new PrimitiveFormatter(primClass, fmtString);
        }
    }

    /**
     * Internal implementation of the annotation output and processor of annotated output.
     * Created for each new input source and discarded after each use.
     * An OffsetInputStream is created to buffer and count the input bytes.
     *
     */
    private static final class AnnotationWriter extends CharArrayWriter {
        private final transient OffsetInputStream source;
        private final transient DataInputStream in;
        private final transient int baseOffset;
        private final transient HexPrinter params;
        private final transient int bytesSingleWidth;
        private final transient int bytesColWidth;
        private final transient int annoWidth;
        private final transient Appendable dest;

        /**
         * Construct a new AnnotationWriter to process the source into the destination.
         * Initializes the DataInputStream and marking of the input to keep track
         * of bytes as they are read by the formatter.
         * @param params formatting parameters
         * @param source source InputStream
         * @param baseOffset initial offset
         * @param dest destination Appendable
         */
        AnnotationWriter(HexPrinter params, InputStream source, int baseOffset, Appendable dest) {
            this.params = params;
            this.baseOffset = baseOffset;
            Objects.requireNonNull(source, "Source is null");
            this.source = new OffsetInputStream(source);
            this.source.mark(1024);
            this.in = new DataInputStream(this.source);
            this.bytesSingleWidth = String.format(params.bytesFormat, 255).length();
            this.bytesColWidth = params.bytesCount * bytesSingleWidth;
            this.annoWidth = params.annoWidth;
            this.dest = dest;
        }

        @Override
        public void write(int c) {
            super.write(c);
            checkFlush();
        }

        @Override
        public void write(char[] c, int off, int len) {
            super.write(c, off, len);
            for (int i = 0; i < len; i++) {
                if (c[off+i] == '\n') {
                    process();
                    return;
                }
            }
            checkFlush();
        }

        @Override
        public void write(String str, int off, int len) {
            super.write(str, off, len);
            if (str.indexOf('\n') >=0 )
                process();
            else
                checkFlush();
        }

        private void checkFlush() {
            if (size() > annoWidth)
                process();
        }

        /**
         * The annotation printing function is called repeatedly to read all of the bytes
         * in the source stream and annotate the stream.
         * The annotated output is appended to the output dest or buffered.
         * <p>
         *     The HexPrinter is not closed and can be used as a template
         *     to create a new formatter with a new Source or different formatting
         *     options.
         * </p>
         */
        @Override
        public void flush() {
            try {
                while (true) {
                    if (source.markedByteCount() >= params.bytesCount)
                        process();
                    params.annoFormatter.annotate(in, this);
                    if (source.markedByteCount() > 256) {
                        // Normally annotations would cause processing more often
                        // Guard against overrunning the mark/reset buffer.
                        process();
                    }
                }
            } catch (IOException ioe) {
                process();
                if (!(ioe instanceof EOFException)) {
                    throw new UncheckedIOException(ioe);
                }
            } catch (UncheckedIOException uio) {
                process();      // clear out the buffers
                throw uio;
            }
        }

        /**
         * Merge the buffered stream of annotations with the formatted bytes
         * and append them to the dest.
         * <p>
         * The annotation mapping function has read some bytes and buffered
         * some output that corresponds to those bytes.
         * The un-formatted bytes are in the OffsetInputStream after the mark.
         * The stream is reset and the bytes are read again.
         * Each line of the produced one line at a time to the dest.
         * The byte offset is formatted according to the offsetFormat.
         * The bytes after the mark are read and formatted using the bytesFormat
         * and written to the dest up to the bytesWidth.
         * The annotation stream is appended to the dest, but only up to the
         * first newline (if any). The alignment between the annotated stream
         * and the formatted bytes is approximate.
         * New line characters in the annotation cause a new line to be started
         * without regard to the number of formatted bytes. The column of formatted
         * bytes may be incomplete.
         */
        private void process() {
            String info = toString();
            reset();
            int count = source.markedByteCount();
            try {
                source.reset();
                int binColOffset = (int)source.byteOffset();
                while (count > 0 || info.length() > 0) {
                    int offset = binColOffset + baseOffset; // offset of first byte on the line
                    dest.append(String.format(params.offsetFormat, offset));
                    // Compute indent based on offset modulo bytesCount
                    int colOffset = offset % params.bytesCount;
                    int colWidth = colOffset * bytesSingleWidth;
                    dest.append(" ".repeat(colWidth));
                    // Append the bytes that fit on this line
                    int byteCount = Math.min(params.bytesCount - colOffset, count);
                    for (int i = 0; i < byteCount; i++) {
                        int b = source.read();
                        if (b == -1)
                            throw new IllegalStateException("BUG");
                        String s = String.format(params.bytesFormat, b);
                        colWidth += s.length();
                        dest.append(s);
                    }
                    binColOffset += byteCount;
                    count -= byteCount;

                    // Pad out the bytes column to its width
                    dest.append(" ".repeat(Math.max(0, bytesColWidth - colWidth)));
                    dest.append(params.annoDelim);

                    // finish a line and prepare for next line
                    // Add a line from annotation buffer
                    if (info.length() > 0) {
                        int nl = info.indexOf('\n');
                        if (nl < 0) {
                            dest.append(info);
                            info = "";
                        } else {
                            // append up to the newline (ignoring \r if present)
                            dest.append(info, 0,
                                    (nl > 0 && info.charAt(nl - 1) == '\r') ? nl - 1 : nl);
                            info = info.substring(nl + 1);
                        }
                    }
                    dest.append(params.lineSeparator);
                }
            } catch (IOException ioe) {
                try {
                    dest.append("\nIOException during annotations: ")
                        .append(ioe.getMessage())
                        .append("\n");
                } catch (IOException ignore) {
                    // ignore
                }
            }
            // reset the mark for the next line
            source.mark(1024);
        }
    }

    /**
     * Buffered InputStream that keeps track of byte offset.
     */
    private static final class OffsetInputStream extends BufferedInputStream {
        private long byteOffset;
        private long markByteOffset;

        OffsetInputStream(InputStream in) {
            super(in);
            byteOffset = 0;
            markByteOffset = 0;
        }

        long byteOffset() {
            return byteOffset;
        }

        @Override
        public void reset() throws IOException {
            super.reset();
            byteOffset = markByteOffset;
        }

        @Override
        public synchronized void mark(int readlimit) {
            super.mark(readlimit);
            markByteOffset = byteOffset;
        }

        int markedByteCount() {
            if (markpos < 0)
                return 0;
            return pos - markpos;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b >= 0)
                byteOffset++;
            return b;
        }

        @Override
        public long skip(long n) throws IOException {
            long size = super.skip(n);
            byteOffset += size;
            return size;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int size = super.read(b);
            byteOffset += Math.max(size, 0);
            return size;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int size = super.read(b, off, len);
            byteOffset += Math.max(size, 0);
            return size;
        }
    }
}
