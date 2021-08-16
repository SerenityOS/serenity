/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * ASN.1 stream formatter; a debugging utility for visualizing the contents of an ASN.1 stream.
 * The ANS1Formatter can be used standalone by calling the {@link #annotate(DataInputStream)}
 * or {@link #annotate(DataInputStream, Appendable)} methods.
 * The ASN1Formatter implements the Formatter interface so it can be used
 * with the {@code HexPrinter} as a formatter to display the ASN.1 tagged values
 * with the corresponding bytes.
 * <p>
 * The formatter reads a single tag from the stream and prints a description
 * of the tag and its contents. If the tag is a constructed tag, set or sequence,
 * each of the contained tags is read and printed.
 * Generally, each tagged value is printed on a separate line. For constructed and application
 * tags the nested tagged values are indented.
 * There are few consistency checks and an improperly encoded stream may produce
 * unpredictable output.
 * <p>
 * For example, to show the contents of a stream from a file.
 * <pre>{@code
 *         Path path = Path.of(DIR, "xxx.pem");
 *         try (InputStream certStream = Files.newInputStream(path)) {
 *             while (certStream.read() != '\n') {
 *                 // Skip first line "-----BEGIN CERTIFICATE-----"
 *             }
 *             // Mime decoder for Certificate
 *             InputStream wis = Base64.getMimeDecoder().wrap(certStream);
 *             DataInputStream dis = new DataInputStream(wis);
 *             String result = ASN1Formatter.formatter().annotate(dis);
 *             System.out.println(result);
 *         } catch (EOFException eof) {
 *             // done
 *         }
 * }</pre>
 * When used as the formatter with {@code jdk.test.lib.hexdump.HexPrinter},
 * the formatted ASN.1 stream is displayed with the corresponding hex dump
 * of the stream contents.
 * <pre>{@code
 *         Path path = Path.of(DIR, "xxx.pem");
 *         try (InputStream certStream = Files.newInputStream(path)) {
 *             while (certStream.read() != '\n') {
 *                 // Skip first line "-----BEGIN CERTIFICATE-----"
 *             }
 *             // Mime decoder for Certificate
 *             InputStream wis = Base64.getMimeDecoder().wrap(certStream);
 *
 *             HexPrinter p = HexPrinter.simple()
 *                                  .formatter(ASN1Formatter.formatter(), "; ", 100);
 *             String result = p.toString(wis);
 *             System.out.println(result);
 *         } catch (EOFException eof) {
 *             // done
 *         }
 * }</pre>
 */
public class ASN1Formatter implements HexPrinter.Formatter {

    /**
     * Returns an ASN1Formatter.
     * @return an ASN1Formatter
     */
    public static ASN1Formatter formatter() {
        return new ASN1Formatter();
    }

    /**
     * Create a ANS1Formatter.
     */
    private ASN1Formatter() {
    }

    /**
     * Read bytes from the stream and annotate the stream as an ASN.1 stream.
     * A single well formed tagged-value is read and annotated.
     *
     * @param in  a DataInputStream
     */
    public String annotate(DataInputStream in) {
        StringBuilder sb = new StringBuilder();
        try {
            this.annotate(in, sb);
        } catch (IOException e) {
            /*
             * Formatters are designed to be nested, where one formatter can call another and the valuable output
             * is the formatted string that has been accumulated from the beginning of the stream.
             *
             * The choice of DataInputStream was chosen for the convenience of the methods to read different types.
             * and (declared) exceptions are an unwelcome artifact.
             *
             * If an exception was percolated up and the formatted output discarded, it would defeat the purpose.
             * So we just catch it here and still return useful information about the stream to this point.
             */
        }
        return sb.toString();
    }

    /**
     * Read bytes from the stream and annotate the stream as a ASN.1.
     * A single well formed tagged-value is read and annotated.
     *
     * @param in  a DataInputStream
     * @param out an Appendable for the output
     * @throws IOException if an I/O error occurs
     */
    public void annotate(DataInputStream in, Appendable out) throws IOException {
        annotate(in, out, -1, "");
    }

    /**
     * Read bytes from the stream and annotate the stream as ASN.1.
     *
     * @param in  a DataInputStream
     * @param out an Appendable for the output
     * @param available the number of bytes to read from the stream (if greater than zero)
     * @param prefix a string to prefix each line of output, used for indentation
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("fallthrough")
    private int annotate(DataInputStream in, Appendable out, int available, String prefix) throws IOException {
        int origAvailable = available;
        while (available != 0 || origAvailable < 0) {
            // Read the tag
            int tag = in.readByte() & 0xff;
            available--;
            if (tagType(tag) == 0x1f) {
                // Multi-byte tag
                tag = 0;
                int tagbits;
                do {
                    tagbits = in.readByte();
                    available--;
                    tag = (tag << 7) | (tagbits & 0x7f);
                } while ((tagbits & 0x80) == 0x80);
            }
            // Decode the length
            int len = in.readByte() & 0xff;
            available--;
            if (len > 0x80) {
                // multi-byte encoded length
                int nbytes = len & 0x7f;
                if (nbytes > 4) {
                    out.append("***** Tag: ")
                            .append(tagName(tag))
                            .append(", Range of length error: ")
                            .append(Integer.toString(len))
                            .append(" bytes");
                    out.append(System.lineSeparator());
                    return available;       // return the unread length
                }
                len = 0;
                for (; nbytes > 0; nbytes--) {
                    int inc = in.readByte() & 0xff;
                    len = (len << 8) | inc;
                    available -= 1;
                }
            } else if (len == 0x80) {
                // Tag with Indefinite-length; flag the length as unknown
                len = -1;
            } else if (available < 0 && origAvailable < 0) {
                // started out unknown; set available to the length of this tagged value
                available = len;
            }
            out.append(prefix);     // start with indent
            switch (tag) {
                case TAG_EndOfContent:    // End-of-contents octets; len == 0
                    out.append("END-OF-CONTENT");
                    out.append(System.lineSeparator());
                    // end of indefinite-length constructed, return zero remaining
                    return 0;
                case TAG_Integer:
                case TAG_Enumerated:
                    switch (len) {
                        case 1:
                            out.append(String.format("BYTE %d. ", in.readByte()));
                            available -= 1;
                            break;
                        case 2:
                            out.append(String.format("SHORT %d. ", in.readShort()));
                            available -= 2;
                            break;
                        case 4:
                            out.append(String.format("INTEGER %d. ", in.readInt()));
                            available -= 4;
                            break;
                        case 8:
                            out.append(String.format("LONG %d. ", in.readLong()));
                            available -= 8;
                            break;
                        default:
                            byte[] bytes = new byte[len];
                            int l = in.read(bytes);
                            BigInteger big = new BigInteger(bytes);
                            out.append("BIG INTEGER [" + len + "] ");
                            out.append(big.toString());
                            out.append(".");
                            available -= len;
                            break;
                    }
                    break;
                case TAG_ObjectId:
                    byte[] oid = new byte[len];
                    int l1 = in.read(oid);
                    available -= l1;
                    String s = oidName(oid);
                    out.append(tagName(tag) + " [" + len + "] ");
                    out.append(s);
                    out.append(' ');
                    break;

                case TAG_OctetString:
                case TAG_UtcTime:
                case TAG_GeneralizedTime:
                    out.append(tagName(tag) + " [" + len + "] ");
                    // fall through
                case TAG_PrintableString:
                case TAG_IA5String:
                case TAG_GeneralString: {
                    // Check if the contents are too long or not printable
                    byte[] buf = new byte[Math.min(32, len)];
                    int l = in.read(buf, 0, buf.length);
                    if (countPrintable(buf, l) > l / 2) {
                        // If more than 1/2 are printable, show the string
                        out.append("'");
                        for (int i = 0; i < l; i++) {
                            char c = toASNPrintable((char) buf[i]);
                            out.append((c > 0) ? c : '.');
                        }
                        out.append("'");
                        if (l < len) {
                            out.append("..."); // identify the truncation
                        }
                        out.append(' ');
                    } else {
                        out.append("<Unprintable> ");
                    }
                    // Skip the rest
                    while (l < len) {
                        l += (int)in.skip(len - l);
                    }
                    available -= len;
                    break;
                }
                case TAG_Null:
                    out.append("NULL ");
                    break;
                case TAG_Boolean:
                    int b = in.readByte();
                    available--;
                    out.append((b == 0) ? "FALSE " : "TRUE ");
                    break;
                case TAG_UTF8String:
                    out.append(getString(in, len, StandardCharsets.UTF_8));
                    out.append(' ');
                    available -= len;
                    break;
                case TAG_T61String:
                    out.append(getString(in, len, StandardCharsets.ISO_8859_1));
                    out.append(' ');
                    available -= len;
                    break;
                case TAG_UniversalString:
                case TAG_BMPString:
                    out.append(getString(in, len, StandardCharsets.UTF_16BE));
                    out.append(' ');
                    available -= len;
                    break;
                case TAG_BitString:
                    out.append(String.format("%s [%d]", tagName(tag), len));
                    do {
                        var skipped = in.skip(len);
                        len -= skipped;
                        available -= skipped;
                    } while (len > 0);
                    break;
                default: {
                    if (tag == TAG_Sequence ||
                            tag == TAG_Set ||
                            isApplication(tag) ||
                            isConstructed(tag)) {
                        String lenStr = (len < 0) ? "INDEFINITE" : Integer.toString(len);
                        // Handle nesting
                        if (isApplication(tag)) {
                            out.append(String.format("APPLICATION %d. [%s] {%n", tagType(tag), lenStr));
                        } else {
                            out.append(String.format("%s [%s]%n", tagName(tag), lenStr));
                        }
                        int remaining = annotate(in, out, len, prefix + "  ");
                        if (len > 0) {
                            available -= len - remaining;
                        }
                        continue;
                    } else {
                        // Any other tag not already handled, dump the bytes
                        out.append(String.format("%s[%d]: ", tagName(tag), len));
                        formatBytes(in, out, len);
                        available -= len;
                        break;
                    }
                }
            }
            out.append(System.lineSeparator());
        }
        return available;
    }

    /**
     * Reads bytes from the stream and annotates them as hexadecimal.
     * @param in an inputStream
     * @param out the Appendable for the formatted bytes
     * @param len the number of bytes to read
     * @throws IOException if an I/O error occurs
     */
    private void formatBytes(DataInputStream in, Appendable out, int len) throws IOException {
        int b = in.readByte() & 0xff;
        out.append(String.format("%02x", b));
        for (int i = 1; i < len; i++) {
            b = in.readByte() & 0xff;
            out.append(String.format(",%02x", b));
        }
    }

    /**
     * Returns a string read from the stream converted to the Charset.
     * @param in an inputStream
     * @param len the number of bytes to read
     * @param charset the Charset
     * @return a string read from the stream converted to the Charset.
     * @throws IOException if an I/O error occurs
     */
    private String getString(DataInputStream in, int len, Charset charset) throws IOException {
        byte[] bytes = new byte[len];
        int l = in.read(bytes);
        return new String(bytes, charset);
    }

    /**
     * Returns the tagname based on the tag value.
     * @param tag the tag value
     * @return a String representation of the tag.
     */
    private String tagName(int tag) {
        String tagString = (isConstructed(tag) ? "CONSTRUCTED "  : "") + tagNames[tagType(tag)];
        switch (tag & 0xc0) {
            case TAG_APPLICATION:
                return "APPLICATION " + tagString;
            case TAG_PRIVATE:
                return "PRIVATE " + tagString;
            case TAG_CONTEXT:
                return tagString;
            case TAG_UNIVERSAL:
                if (tag > 0 && tag < tagNames.length)
                    return tagNames[tag];
                if (tag == TAG_Set)
                    return "SET";
                if (tag == TAG_Sequence)
                    return "SEQUENCE";
                return "UNIVERSAL " + tagString;
            default:
                return "TAG__" + (tag & 0xc0);
        }
    }

    /**
     * Returns the string representation of the OID extracted from the bytes.
     * The numeric OID is looked up in the internal sun.security.util.KnownOIDs class.
     * If a name is found it is added to the returned string.
     * @param bytes bytes holding the OID
     * @return a string representing OID or the numeric OID
     */
    private static String oidName(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 4);
        int first = bytes[0] / 40;
        int second = bytes[0] % 40;
        sb.append(first).append('.').append(second);
        int valn = 0;
        for (int i = 1; i < bytes.length; i++) {
            valn = valn * 128 + (bytes[i] & 0x7f);
            if ((bytes[i] & 0x80) == 0) {
                sb.append('.');
                sb.append(valn);
                valn = 0;
            }
        }
        String noid = sb.toString();
        try {
            // Look up the OID; if the class is not accessible just return the numeric form
            Class<?> cl = Class.forName("sun.security.util.KnownOIDs");
            Method findMatch = cl.getDeclaredMethod("findMatch", String.class);
            Object oid = findMatch.invoke(null, noid);
            return (oid == null) ? noid : noid + " (" + oid.toString() + ")";
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return noid;
        }
    }

    /**
     * Returns a printable character or zero for unprintable.
     * @param ch a character
     * @return a printable character or zero for unprintable
     */
    private static char toASNPrintable(char ch) {
        if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
                (ch >= '0' && ch <= '9')) {
            return ch;
        } else {
            switch (ch) {
                case ' ':       /* space */
                case '\'':      /* apostrophe */
                case '(':       /* left paren */
                case ')':       /* right paren */
                case '+':       /* plus */
                case ',':       /* comma */
                case '-':       /* hyphen */
                case '.':       /* period */
                case '/':       /* slash */
                case ':':       /* colon */
                case '=':       /* equals */
                case '?':       /* question mark */
                    return ch;
                default:
                    return 0;
            }
        }
    }

    /**
     * Returns the number of printable characters in the buffer range.
     * @param bytes a byte array
     * @param len the length of the bytes to check
     * @return the count of printable bytes
     */
    private static int countPrintable(byte[] bytes, int len) {
        int count = 0;
        for (int i = 0; i < len; i++) {
            count += (toASNPrintable((char)bytes[i]) > 0) ? 1 : 0;
        }
        return count;
    }

    /** The tag class types */
    private static final int TAG_UNIVERSAL = 0x00;
    private static final int TAG_APPLICATION = 0x40;
    private static final int TAG_CONTEXT = 0x80;
    private static final int TAG_PRIVATE = 0xc0;

    /*
     * The type starts at the first byte of the encoding, and
     * is one of these TAG_* values.  That may be all the type
     * data that is needed.
     */

    /*
     * These tags are the "universal" tags ... they mean the same
     * in all contexts.  (Mask with 0x1f -- five bits.)
     */

    private int tagType(int tag) {
        return tag & TAG_MASK;
    }

    private static final byte TAG_MASK = 0x1f;

    /** Tag value indicating an ASN.1 "EndOfContent" value. */
    private static final byte    TAG_EndOfContent = 0x0;

    /** Tag value indicating an ASN.1 "BOOLEAN" value. */
    private static final byte    TAG_Boolean = 0x1;

    /** Tag value indicating an ASN.1 "INTEGER" value. */
    private static final byte    TAG_Integer = 0x2;

    /** Tag value indicating an ASN.1 "BIT STRING" value. */
    private static final byte    TAG_BitString = 0x3;

    /** Tag value indicating an ASN.1 "OCTET STRING" value. */
    private static final byte    TAG_OctetString = 0x4;

    /** Tag value indicating an ASN.1 "NULL" value. */
    private static final byte    TAG_Null = 0x5;

    /** Tag value indicating an ASN.1 "OBJECT IDENTIFIER" value. */
    private static final byte    TAG_ObjectId = 0x6;

    /** Tag value including an ASN.1 "ENUMERATED" value */
    private static final byte    TAG_Enumerated = 0xA;

    /** Tag value indicating an ASN.1 "UTF8String" value. */
    private static final byte    TAG_UTF8String = 0xC;

    /** Tag value including a "printable" string */
    private static final byte    TAG_PrintableString = 0x13;

    /** Tag value including a "teletype" string */
    private static final byte    TAG_T61String = 0x14;

    /** Tag value including an ASCII string */
    private static final byte    TAG_IA5String = 0x16;

    /** Tag value indicating an ASN.1 "UTCTime" value. */
    private static final byte    TAG_UtcTime = 0x17;

    /** Tag value indicating an ASN.1 "GeneralizedTime" value. */
    private static final byte    TAG_GeneralizedTime = 0x18;

    /** Tag value indicating an ASN.1 "GenerallString" value. */
    private static final byte    TAG_GeneralString = 0x1B;

    /** Tag value indicating an ASN.1 "UniversalString" value. */
    private static final byte    TAG_UniversalString = 0x1C;

    /** Tag value indicating an ASN.1 "BMPString" value. */
    private static final byte    TAG_BMPString = 0x1E;

    // CONSTRUCTED seq/set

    /**
     * Tag value indicating an ASN.1
     * "SEQUENCE" (zero to N elements, order is significant).
     */
    private static final byte    TAG_Sequence = 0x30;

    /**
     * Tag value indicating an ASN.1
     * "SET" (zero to N members, order does not matter).
     */
    private static final byte    TAG_Set = 0x31;

    /*
     * These values are the high order bits for the other kinds of tags.
     */

    /**
     * Returns true if the tag class is UNIVERSAL.
     */
    private boolean isUniversal(int tag)      { return ((tag & 0xc0) == 0x0); }

    /**
     * Returns true if the tag class is APPLICATION.
     */
    private boolean isApplication(int tag)    { return ((tag & 0xc0) == TAG_APPLICATION); }

     /** Returns true iff the CONSTRUCTED bit is set in the type tag. */
    private boolean isConstructed(int tag)    { return ((tag & 0x20) == 0x20); }

    // Names for tags.
    private static final String[] tagNames = new String[] {
            "ANY", "BOOLEAN", "INTEGER", "BIT STRING", "OCTET STRING",
            "NULL", "OBJECT ID", "OBJECT DESCRIPTION", "EXTERNAL", "REAL",
            "ENUMERATION", "u11", "UTF8 STRING", "u13", "u14",
            "u15", "SEQUENCE", "SET", "NUMERIC STRING", "STRING",
            "T61", "VIDEOTEXT", "IA5", "UTCTIME", "GENERAL TIME",
            "GRAPHIC STRING", "ISO64STRING", "GENERAL STRING", "UNIVERSAL STRING", "u29",
            "BMP STRING", "MULTIBYTE TAG",
    };

    /**
     * Simple utility to open and print contents of a file as ASN.1.
     * If the first line of the file looks like a certificate
     * it will be Base64 decoded and passed to the ASN.1 formatter.
     * @param args file names
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage:  <asn.1 files>");
            return;
        }
        ASN1Formatter fmt = ASN1Formatter.formatter();
        for (String file : args) {
            System.out.printf("%s%n", file);
            try (InputStream fis = Files.newInputStream(Path.of(file));
                 BufferedInputStream is = new BufferedInputStream(fis);
                 InputStream in = wrapIfBase64Mime(is)) {

                DataInputStream dis = new DataInputStream(in);
                HexPrinter p = HexPrinter.simple()
                        .dest(System.out)
                        .formatter(ASN1Formatter.formatter(), "; ", 100);
                p.format(dis);
            } catch (EOFException eof) {
                System.out.println();
            } catch (IOException ioe) {
                System.out.printf("%s: %s%n", file, ioe);
            }
        }
    }

    /**
     * Returns an InputStream, either the original on a wrapped to decode Base64Mime encoding.
     * @param bis an InputStream
     * @return the InputStream or the wrapped decoder of Base64Mime.
     * @throws IOException if an I/O error occurs
     */
    private static InputStream wrapIfBase64Mime(BufferedInputStream bis) throws IOException {
        bis.mark(256);
        DataInputStream dis = new DataInputStream(bis);
        String line1 = dis.readLine(); // Good enough for our purposes
        if (line1.startsWith("-----") && line1.endsWith("-----")) {
            // Probable Base64 Mime encoding
            return Base64.getMimeDecoder().wrap(bis);
        }
        bis.reset();
        return bis;
    }

}
