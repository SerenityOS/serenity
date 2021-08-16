/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

import sun.nio.cs.UTF_32BE;
import sun.util.calendar.CalendarDate;
import sun.util.calendar.CalendarSystem;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.*;

import static java.nio.charset.StandardCharsets.*;

/**
 * Represents a single DER-encoded value.  DER encoding rules are a subset
 * of the "Basic" Encoding Rules (BER), but they only support a single way
 * ("Definite" encoding) to encode any given value.
 *
 * <P>All DER-encoded data are triples <em>{type, length, data}</em>.  This
 * class represents such tagged values as they have been read (or constructed),
 * and provides structured access to the encoded data.
 *
 * <P>At this time, this class supports only a subset of the types of DER
 * data encodings which are defined.  That subset is sufficient for parsing
 * most X.509 certificates, and working with selected additional formats
 * (such as PKCS #10 certificate requests, and some kinds of PKCS #7 data).
 *
 * A note with respect to T61/Teletex strings: From RFC 1617, section 4.1.3
 * and RFC 5280, section 8, we assume that this kind of string will contain
 * ISO-8859-1 characters only.
 *
 *
 * @author David Brownell
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 */
public class DerValue {

    /** The tag class types */
    public static final byte TAG_UNIVERSAL = (byte)0x000;
    public static final byte TAG_APPLICATION = (byte)0x040;
    public static final byte TAG_CONTEXT = (byte)0x080;
    public static final byte TAG_PRIVATE = (byte)0x0c0;

    /*
     * The type starts at the first byte of the encoding, and
     * is one of these tag_* values.  That may be all the type
     * data that is needed.
     */

    /*
     * These tags are the "universal" tags ... they mean the same
     * in all contexts.  (Mask with 0x1f -- five bits.)
     */

    /** Tag value indicating an ASN.1 "BOOLEAN" value. */
    public static final byte    tag_Boolean = 0x01;

    /** Tag value indicating an ASN.1 "INTEGER" value. */
    public static final byte    tag_Integer = 0x02;

    /** Tag value indicating an ASN.1 "BIT STRING" value. */
    public static final byte    tag_BitString = 0x03;

    /** Tag value indicating an ASN.1 "OCTET STRING" value. */
    public static final byte    tag_OctetString = 0x04;

    /** Tag value indicating an ASN.1 "NULL" value. */
    public static final byte    tag_Null = 0x05;

    /** Tag value indicating an ASN.1 "OBJECT IDENTIFIER" value. */
    public static final byte    tag_ObjectId = 0x06;

    /** Tag value including an ASN.1 "ENUMERATED" value */
    public static final byte    tag_Enumerated = 0x0A;

    /** Tag value indicating an ASN.1 "UTF8String" value. */
    public static final byte    tag_UTF8String = 0x0C;

    /** Tag value including a "printable" string */
    public static final byte    tag_PrintableString = 0x13;

    /** Tag value including a "teletype" string */
    public static final byte    tag_T61String = 0x14;

    /** Tag value including an ASCII string */
    public static final byte    tag_IA5String = 0x16;

    /** Tag value indicating an ASN.1 "UTCTime" value. */
    public static final byte    tag_UtcTime = 0x17;

    /** Tag value indicating an ASN.1 "GeneralizedTime" value. */
    public static final byte    tag_GeneralizedTime = 0x18;

    /** Tag value indicating an ASN.1 "GenerallString" value. */
    public static final byte    tag_GeneralString = 0x1B;

    /** Tag value indicating an ASN.1 "UniversalString" value. */
    public static final byte    tag_UniversalString = 0x1C;

    /** Tag value indicating an ASN.1 "BMPString" value. */
    public static final byte    tag_BMPString = 0x1E;

    // CONSTRUCTED seq/set

    /**
     * Tag value indicating an ASN.1
     * "SEQUENCE" (zero to N elements, order is significant).
     */
    public static final byte    tag_Sequence = 0x30;

    /**
     * Tag value indicating an ASN.1
     * "SEQUENCE OF" (one to N elements, order is significant).
     */
    public static final byte    tag_SequenceOf = 0x30;

    /**
     * Tag value indicating an ASN.1
     * "SET" (zero to N members, order does not matter).
     */
    public static final byte    tag_Set = 0x31;

    /**
     * Tag value indicating an ASN.1
     * "SET OF" (one to N members, order does not matter).
     */
    public static final byte    tag_SetOf = 0x31;

    // This class is mostly immutable except that:
    //
    // 1. resetTag() modifies the tag
    // 2. the data field is mutable
    //
    // For compatibility, data, getData() and resetTag() are preserved.
    // A modern caller should call withTag() or data() instead.
    //
    // Also, some constructors have not cloned buffer, so the data could
    // be modified externally.

    public /*final*/ byte tag;
    final byte[] buffer;
    private final int start;
    final int end;
    private final boolean allowBER;

    // Unsafe. Legacy. Never null.
    public final DerInputStream data;

    /*
     * These values are the high order bits for the other kinds of tags.
     */

    /**
     * Returns true if the tag class is UNIVERSAL.
     */
    public boolean isUniversal()      { return ((tag & 0x0c0) == 0x000); }

    /**
     * Returns true if the tag class is APPLICATION.
     */
    public boolean isApplication()    { return ((tag & 0x0c0) == 0x040); }

    /**
     * Returns true iff the CONTEXT SPECIFIC bit is set in the type tag.
     * This is associated with the ASN.1 "DEFINED BY" syntax.
     */
    public boolean isContextSpecific() { return ((tag & 0x0c0) == 0x080); }

    /**
     * Returns true iff the CONTEXT SPECIFIC TAG matches the passed tag.
     */
    public boolean isContextSpecific(byte cntxtTag) {
        if (!isContextSpecific()) {
            return false;
        }
        return ((tag & 0x01f) == cntxtTag);
    }

    boolean isPrivate()        { return ((tag & 0x0c0) == 0x0c0); }

    /** Returns true iff the CONSTRUCTED bit is set in the type tag. */
    public boolean isConstructed()    { return ((tag & 0x020) == 0x020); }

    /**
     * Returns true iff the CONSTRUCTED TAG matches the passed tag.
     */
    public boolean isConstructed(byte constructedTag) {
        if (!isConstructed()) {
            return false;
        }
        return ((tag & 0x01f) == constructedTag);
    }

    /**
     * Creates a new DerValue by specifying all its fields.
     */
    DerValue(byte tag, byte[] buffer, int start, int end, boolean allowBER) {
        if ((tag & 0x1f) == 0x1f) {
            throw new IllegalArgumentException("Tag number over 30 is not supported");
        }
        this.tag = tag;
        this.buffer = buffer;
        this.start = start;
        this.end = end;
        this.allowBER = allowBER;
        this.data = data();
    }

    /**
     * Creates a PrintableString or UTF8string DER value from a string.
     */
    public DerValue(String value) {
        this(isPrintableString(value) ? tag_PrintableString : tag_UTF8String,
                value);
    }

    private static boolean isPrintableString(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!isPrintableStringChar(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a string type DER value from a String object
     * @param stringTag the tag for the DER value to create
     * @param value the String object to use for the DER value
     */
    public DerValue(byte stringTag, String value) {
        this(stringTag, string2bytes(stringTag, value), false);
    }

    private static byte[] string2bytes(byte stringTag, String value) {
        Charset charset = switch (stringTag) {
            case tag_PrintableString, tag_IA5String, tag_GeneralString -> US_ASCII;
            case tag_T61String -> ISO_8859_1;
            case tag_BMPString -> UTF_16BE;
            case tag_UTF8String -> UTF_8;
            case tag_UniversalString -> Charset.forName("UTF_32BE");
            default -> throw new IllegalArgumentException("Unsupported DER string type");
        };
        return value.getBytes(charset);
    }

    DerValue(byte tag, byte[] buffer, boolean allowBER) {
        this(tag, buffer, 0, buffer.length, allowBER);
    }

    /**
     * Creates a DerValue from a tag and some DER-encoded data.
     *
     * This is a public constructor.
     *
     * @param tag the DER type tag
     * @param buffer the DER-encoded data
     */
    public DerValue(byte tag, byte[] buffer) {
        this(tag, buffer.clone(), true);
    }

    /**
     * Wraps an DerOutputStream. All bytes currently written
     * into the stream will become the content of the newly
     * created DerValue.
     *
     * Attention: do not reset the DerOutputStream after this call.
     * No array copying is made.
     *
     * @param tag the tag
     * @param out the DerOutputStream
     * @returns a new DerValue using out as its content
     */
    public static DerValue wrap(byte tag, DerOutputStream out) {
        return new DerValue(tag, out.buf(), 0, out.size(), false);
    }

    /**
     * Parse an ASN.1/BER encoded datum. The entire encoding must hold exactly
     * one datum, including its tag and length.
     *
     * This is a public constructor.
     */
    public DerValue(byte[] encoding) throws IOException {
        this(encoding.clone(), 0, encoding.length, true, false);
    }

    /**
     * Parse an ASN.1 encoded datum from a byte array.
     *
     * @param buf the byte array containing the DER-encoded datum
     * @param offset where the encoded datum starts inside {@code buf}
     * @param len length of bytes to parse inside {@code buf}
     * @param allowBER whether BER is allowed
     * @param allowMore whether extra bytes are allowed after the encoded datum.
     *                  If true, {@code len} can be bigger than the length of
     *                  the encoded datum.
     *
     * @throws IOException if it's an invalid encoding or there are extra bytes
     *                     after the encoded datum and {@code allowMore} is false.
     */
    DerValue(byte[] buf, int offset, int len, boolean allowBER, boolean allowMore)
            throws IOException {

        if (len < 2) {
            throw new IOException("Too short");
        }
        int pos = offset;
        tag = buf[pos++];
        if ((tag & 0x1f) == 0x1f) {
            throw new IOException("Tag number over 30 at " + offset + " is not supported");
        }
        int lenByte = buf[pos++];

        int length;
        if (lenByte == (byte) 0x80) { // indefinite length
            if (!allowBER) {
                throw new IOException("Indefinite length encoding " +
                        "not supported with DER");
            }
            if (!isConstructed()) {
                throw new IOException("Indefinite length encoding " +
                        "not supported with non-constructed data");
            }

            // Reconstruct data source
            buf = DerIndefLenConverter.convertStream(
                    new ByteArrayInputStream(buf, pos, len - (pos - offset)), tag);
            offset = 0;
            len = buf.length;
            pos = 2;

            if (tag != buf[0]) {
                throw new IOException("Indefinite length encoding not supported");
            }
            lenByte = buf[1];
            if (lenByte == (byte) 0x80) {
                throw new IOException("Indefinite len conversion failed");
            }
        }

        if ((lenByte & 0x080) == 0x00) { // short form, 1 byte datum
            length = lenByte;
        } else {                     // long form
            lenByte &= 0x07f;
            if (lenByte > 4) {
                throw new IOException("Invalid lenByte");
            }
            if (len < 2 + lenByte) {
                throw new IOException("Not enough length bytes");
            }
            length = 0x0ff & buf[pos++];
            lenByte--;
            if (length == 0 && !allowBER) {
                // DER requires length value be encoded in minimum number of bytes
                throw new IOException("Redundant length bytes found");
            }
            while (lenByte-- > 0) {
                length <<= 8;
                length += 0x0ff & buf[pos++];
            }
            if (length < 0) {
                throw new IOException("Invalid length bytes");
            } else if (length <= 127 && !allowBER) {
                throw new IOException("Should use short form for length");
            }
        }
        // pos is now at the beginning of the content
        if (len - length < pos - offset) {
            throw new EOFException("not enough content");
        }
        if (len - length > pos - offset && !allowMore) {
            throw new IOException("extra data at the end");
        }
        this.buffer = buf;
        this.start = pos;
        this.end = pos + length;
        this.allowBER = allowBER;
        this.data = data();
    }

    // Get an ASN1/DER encoded datum from an input stream w/ additional
    // arg to control whether DER checks are enforced.
    DerValue(InputStream in, boolean allowBER) throws IOException {
        this.tag = (byte)in.read();
        if ((tag & 0x1f) == 0x1f) {
            throw new IOException("Tag number over 30 is not supported");
        }
        int length = DerInputStream.getLength(in);
        if (length == -1) { // indefinite length encoding found
            if (!allowBER) {
                throw new IOException("Indefinite length encoding " +
                        "not supported with DER");
            }
            if (!isConstructed()) {
                throw new IOException("Indefinite length encoding " +
                        "not supported with non-constructed data");
            }
            this.buffer = DerIndefLenConverter.convertStream(in, tag);
            ByteArrayInputStream bin = new ByteArrayInputStream(this.buffer);
            if (tag != bin.read()) {
                throw new IOException
                        ("Indefinite length encoding not supported");
            }
            length = DerInputStream.getDefiniteLength(bin);
            this.start = this.buffer.length - bin.available();
            this.end = this.start + length;
            // position of in is undetermined. Precisely, it might be n-bytes
            // after DerValue, and these n bytes are at the end of this.buffer
            // after this.end.
        } else {
            this.buffer = IOUtils.readExactlyNBytes(in, length);
            this.start = 0;
            this.end = length;
            // position of in is right after the DerValue
        }
        this.allowBER = allowBER;
        this.data = data();
    }

    /**
     * Get an ASN1/DER encoded datum from an input stream.  The
     * stream may have additional data following the encoded datum.
     * In case of indefinite length encoded datum, the input stream
     * must hold only one datum, i.e. all bytes in the stream might
     * be consumed. Otherwise, only one DerValue will be consumed.
     *
     * @param in the input stream holding a single DER datum,
     *  which may be followed by additional data
     */
    public DerValue(InputStream in) throws IOException {
        this(in, true);
    }

    /**
     * Encode an ASN1/DER encoded datum onto a DER output stream.
     */
    public void encode(DerOutputStream out) throws IOException {
        out.write(tag);
        out.putLength(end - start);
        out.write(buffer, start, end - start);
        data.pos = data.end; // Compatibility. Reach end.
    }

    /**
     * Returns a new DerInputStream pointing at the start of this
     * DerValue's content.
     *
     * @return the new DerInputStream value
     */
    public final DerInputStream data() {
        return new DerInputStream(buffer, start, end - start, allowBER);
    }

    /**
     * Returns the data field inside this class directly.
     *
     * Both this method and the {@link #data} field should be avoided.
     * Consider using {@link #data()} instead.
     */
    public final DerInputStream getData() {
        return data;
    }

    public final byte getTag() {
        return tag;
    }

    /**
     * Returns an ASN.1 BOOLEAN
     *
     * @return the boolean held in this DER value
     */
    public boolean getBoolean() throws IOException {
        if (tag != tag_Boolean) {
            throw new IOException("DerValue.getBoolean, not a BOOLEAN " + tag);
        }
        if (end - start != 1) {
            throw new IOException("DerValue.getBoolean, invalid length "
                                        + (end - start));
        }
        data.pos = data.end; // Compatibility. Reach end.
        return buffer[start] != 0;
    }

    /**
     * Returns an ASN.1 OBJECT IDENTIFIER.
     *
     * @return the OID held in this DER value
     */
    public ObjectIdentifier getOID() throws IOException {
        if (tag != tag_ObjectId) {
            throw new IOException("DerValue.getOID, not an OID " + tag);
        }
        data.pos = data.end; // Compatibility. Reach end.
        return new ObjectIdentifier(Arrays.copyOfRange(buffer, start, end));
    }

    /**
     * Returns an ASN.1 OCTET STRING
     *
     * @return the octet string held in this DER value
     */
    public byte[] getOctetString() throws IOException {

        if (tag != tag_OctetString && !isConstructed(tag_OctetString)) {
            throw new IOException(
                "DerValue.getOctetString, not an Octet String: " + tag);
        }
        // Note: do not attempt to call buffer.read(bytes) at all. There's a
        // known bug that it returns -1 instead of 0.
        if (end - start == 0) {
            return new byte[0];
        }

        data.pos = data.end; // Compatibility. Reach end.
        if (!isConstructed()) {
            return Arrays.copyOfRange(buffer, start, end);
        } else {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DerInputStream dis = data();
            while (dis.available() > 0) {
                bout.write(dis.getDerValue().getOctetString());
            }
            return bout.toByteArray();
        }
    }

    /**
     * Returns an ASN.1 INTEGER value as an integer.
     *
     * @return the integer held in this DER value.
     */
    public int getInteger() throws IOException {
        return getIntegerInternal(tag_Integer);
    }

    private int getIntegerInternal(byte expectedTag) throws IOException {
        BigInteger result = getBigIntegerInternal(expectedTag, false);
        if (result.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
            throw new IOException("Integer below minimum valid value");
        }
        if (result.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new IOException("Integer exceeds maximum valid value");
        }
        return result.intValue();
    }

    /**
     * Returns an ASN.1 INTEGER value as a BigInteger.
     *
     * @return the integer held in this DER value as a BigInteger.
     */
    public BigInteger getBigInteger() throws IOException {
        return getBigIntegerInternal(tag_Integer, false);
    }

    /**
     * Returns an ASN.1 INTEGER value as a positive BigInteger.
     * This is just to deal with implementations that incorrectly encode
     * some values as negative.
     *
     * @return the integer held in this DER value as a BigInteger.
     */
    public BigInteger getPositiveBigInteger() throws IOException {
        return getBigIntegerInternal(tag_Integer, true);
    }

    /**
     * Returns a BigInteger value
     *
     * @param makePositive whether to always return a positive value,
     *   irrespective of actual encoding
     * @return the integer as a BigInteger.
     */
    private BigInteger getBigIntegerInternal(byte expectedTag, boolean makePositive) throws IOException {
        if (tag != expectedTag) {
            throw new IOException("DerValue.getBigIntegerInternal, not expected " + tag);
        }
        if (end == start) {
            throw new IOException("Invalid encoding: zero length Int value");
        }
        data.pos = data.end; // Compatibility. Reach end.
        if (!allowBER && (end - start >= 2 && (buffer[start] == 0) && (buffer[start + 1] >= 0))) {
            throw new IOException("Invalid encoding: redundant leading 0s");
        }
        return makePositive
                ? new BigInteger(1, buffer, start, end - start)
                : new BigInteger(buffer, start, end - start);
    }

    /**
     * Returns an ASN.1 ENUMERATED value.
     *
     * @return the integer held in this DER value.
     */
    public int getEnumerated() throws IOException {
        return getIntegerInternal(tag_Enumerated);
    }

    /**
     * Returns an ASN.1 BIT STRING value.  The bit string must be byte-aligned.
     *
     * @return the bit string held in this value
     */
    public byte[] getBitString() throws IOException {
        return getBitString(false);
    }

    /**
     * Returns an ASN.1 BIT STRING value that need not be byte-aligned.
     *
     * @return a BitArray representing the bit string held in this value
     */
    public BitArray getUnalignedBitString() throws IOException {
        return getUnalignedBitString(false);
    }

    /**
     * Returns the name component as a Java string, regardless of its
     * encoding restrictions (ASCII, T61, Printable, IA5, BMP, UTF8).
     */
    // TBD: Need encoder for UniversalString before it can be handled.
    public String getAsString() throws IOException {
        return switch (tag) {
            case tag_UTF8String -> getUTF8String();
            case tag_PrintableString -> getPrintableString();
            case tag_T61String -> getT61String();
            case tag_IA5String -> getIA5String();
            case tag_UniversalString -> getUniversalString();
            case tag_BMPString -> getBMPString();
            case tag_GeneralString -> getGeneralString();
            default -> null;
        };
    }

    /**
     * Returns an ASN.1 BIT STRING value, with the tag assumed implicit
     * based on the parameter.  The bit string must be byte-aligned.
     *
     * @param tagImplicit if true, the tag is assumed implicit.
     * @return the bit string held in this value
     */
    public byte[] getBitString(boolean tagImplicit) throws IOException {
        if (!tagImplicit) {
            if (tag != tag_BitString) {
                throw new IOException("DerValue.getBitString, not a bit string "
                        + tag);
            }
        }
        if (end == start) {
            throw new IOException("Invalid encoding: zero length bit string");
        }
        int numOfPadBits = buffer[start];
        if ((numOfPadBits < 0) || (numOfPadBits > 7)) {
            throw new IOException("Invalid number of padding bits");
        }
        // minus the first byte which indicates the number of padding bits
        byte[] retval = Arrays.copyOfRange(buffer, start + 1, end);
        if (numOfPadBits != 0) {
            // get rid of the padding bits
            retval[end - start - 2] &= (0xff << numOfPadBits);
        }
        data.pos = data.end; // Compatibility. Reach end.
        return retval;
    }

    /**
     * Returns an ASN.1 BIT STRING value, with the tag assumed implicit
     * based on the parameter.  The bit string need not be byte-aligned.
     *
     * @param tagImplicit if true, the tag is assumed implicit.
     * @return the bit string held in this value
     */
    public BitArray getUnalignedBitString(boolean tagImplicit)
            throws IOException {
        if (!tagImplicit) {
            if (tag != tag_BitString) {
                throw new IOException("DerValue.getBitString, not a bit string "
                        + tag);
            }
        }
        if (end == start) {
            throw new IOException("Invalid encoding: zero length bit string");
        }
        data.pos = data.end; // Compatibility. Reach end.
        int numOfPadBits = buffer[start];
        if ((numOfPadBits < 0) || (numOfPadBits > 7)) {
            throw new IOException("Invalid number of padding bits");
        }
        if (end == start + 1) {
            return new BitArray(0);
        } else {
            return new BitArray(((end - start - 1) << 3) - numOfPadBits,
                    Arrays.copyOfRange(buffer, start + 1, end));
        }
    }

    /**
     * Helper routine to return all the bytes contained in the
     * DerInputStream associated with this object.
     */
    public byte[] getDataBytes() throws IOException {
        data.pos = data.end; // Compatibility. Reach end.
        return Arrays.copyOfRange(buffer, start, end);
    }

    private String readStringInternal(byte expectedTag, Charset cs) throws IOException {
        if (tag != expectedTag) {
            throw new IOException("Incorrect string type " + tag + " is not " + expectedTag);
        }
        data.pos = data.end; // Compatibility. Reach end.
        return new String(buffer, start, end - start, cs);
    }

    /**
     * Returns an ASN.1 STRING value
     *
     * @return the printable string held in this value
     */
    public String getPrintableString() throws IOException {
        return readStringInternal(tag_PrintableString, US_ASCII);
    }

    /**
     * Returns an ASN.1 T61 (Teletype) STRING value
     *
     * @return the teletype string held in this value
     */
    public String getT61String() throws IOException {
        return readStringInternal(tag_T61String, ISO_8859_1);
    }

    /**
     * Returns an ASN.1 IA5 (ASCII) STRING value
     *
     * @return the ASCII string held in this value
     */
    public String getIA5String() throws IOException {
        return readStringInternal(tag_IA5String, US_ASCII);
    }

    /**
     * Returns the ASN.1 BMP (Unicode) STRING value as a Java string.
     *
     * @return a string corresponding to the encoded BMPString held in
     * this value
     */
    public String getBMPString() throws IOException {
        // BMPString is the same as Unicode in big endian, unmarked format.
        return readStringInternal(tag_BMPString, UTF_16BE);
    }

    /**
     * Returns the ASN.1 UTF-8 STRING value as a Java String.
     *
     * @return a string corresponding to the encoded UTF8String held in
     * this value
     */
    public String getUTF8String() throws IOException {
        return readStringInternal(tag_UTF8String, UTF_8);
    }

    /**
     * Returns the ASN.1 GENERAL STRING value as a Java String.
     *
     * @return a string corresponding to the encoded GeneralString held in
     * this value
     */
    public String getGeneralString() throws IOException {
        return readStringInternal(tag_GeneralString, US_ASCII);
    }

    /**
     * Returns the ASN.1 UNIVERSAL (UTF-32) STRING value as a Java String.
     *
     * @return a string corresponding to the encoded UniversalString held in
     * this value
     */
    public String getUniversalString() throws IOException {
        return readStringInternal(tag_UniversalString, new UTF_32BE());
    }

    /**
     * Reads the ASN.1 NULL value
     */
    public void getNull() throws IOException {
        if (tag != tag_Null) {
            throw new IOException("DerValue.getNull, not NULL: " + tag);
        }
        if (end != start) {
            throw new IOException("NULL should contain no data");
        }
    }

    /**
     * Private helper routine to extract time from the der value.
     * @param generalized true if Generalized Time is to be read, false
     * if UTC Time is to be read.
     */
    private Date getTimeInternal(boolean generalized) throws IOException {

        /*
         * UTC time encoded as ASCII chars:
         *       YYMMDDhhmmZ
         *       YYMMDDhhmmssZ
         *       YYMMDDhhmm+hhmm
         *       YYMMDDhhmm-hhmm
         *       YYMMDDhhmmss+hhmm
         *       YYMMDDhhmmss-hhmm
         * UTC Time is broken in storing only two digits of year.
         * If YY < 50, we assume 20YY;
         * if YY >= 50, we assume 19YY, as per RFC 5280.
         *
         * Generalized time has a four-digit year and allows any
         * precision specified in ISO 8601. However, for our purposes,
         * we will only allow the same format as UTC time, except that
         * fractional seconds (millisecond precision) are supported.
         */

        int year, month, day, hour, minute, second, millis;
        String type;
        int pos = start;
        int len = end - start;

        if (generalized) {
            type = "Generalized";
            year = 1000 * toDigit(buffer[pos++], type);
            year += 100 * toDigit(buffer[pos++], type);
            year += 10 * toDigit(buffer[pos++], type);
            year += toDigit(buffer[pos++], type);
            len -= 2; // For the two extra YY
        } else {
            type = "UTC";
            year = 10 * toDigit(buffer[pos++], type);
            year += toDigit(buffer[pos++], type);

            if (year < 50) {             // origin 2000
                year += 2000;
            } else {
                year += 1900;   // origin 1900
            }
        }

        month = 10 * toDigit(buffer[pos++], type);
        month += toDigit(buffer[pos++], type);

        day = 10 * toDigit(buffer[pos++], type);
        day += toDigit(buffer[pos++], type);

        hour = 10 * toDigit(buffer[pos++], type);
        hour += toDigit(buffer[pos++], type);

        minute = 10 * toDigit(buffer[pos++], type);
        minute += toDigit(buffer[pos++], type);

        len -= 10; // YYMMDDhhmm

        /*
         * We allow for non-encoded seconds, even though the
         * IETF-PKIX specification says that the seconds should
         * always be encoded even if it is zero.
         */

        millis = 0;
        if (len > 2) {
            second = 10 * toDigit(buffer[pos++], type);
            second += toDigit(buffer[pos++], type);
            len -= 2;
            // handle fractional seconds (if present)
            if (generalized && (buffer[pos] == '.' || buffer[pos] == ',')) {
                len --;
                if (len == 0) {
                    throw new IOException("Parse " + type +
                            " time, empty fractional part");
                }
                pos++;
                int precision = 0;
                while (buffer[pos] != 'Z' &&
                        buffer[pos] != '+' &&
                        buffer[pos] != '-') {
                    // Validate all digits in the fractional part but
                    // store millisecond precision only
                    int thisDigit = toDigit(buffer[pos], type);
                    precision++;
                    len--;
                    if (len == 0) {
                        throw new IOException("Parse " + type +
                                " time, invalid fractional part");
                    }
                    pos++;
                    switch (precision) {
                        case 1 -> millis += 100 * thisDigit;
                        case 2 -> millis += 10 * thisDigit;
                        case 3 -> millis += thisDigit;
                    }
                }
                if (precision == 0) {
                    throw new IOException("Parse " + type +
                            " time, empty fractional part");
                }
            }
        } else
            second = 0;

        if (month == 0 || day == 0
                || month > 12 || day > 31
                || hour >= 24 || minute >= 60 || second >= 60) {
            throw new IOException("Parse " + type + " time, invalid format");
        }

        /*
         * Generalized time can theoretically allow any precision,
         * but we're not supporting that.
         */
        CalendarSystem gcal = CalendarSystem.getGregorianCalendar();
        CalendarDate date = gcal.newCalendarDate(null); // no time zone
        date.setDate(year, month, day);
        date.setTimeOfDay(hour, minute, second, millis);
        long time = gcal.getTime(date);

        /*
         * Finally, "Z" or "+hhmm" or "-hhmm" ... offsets change hhmm
         */
        if (! (len == 1 || len == 5)) {
            throw new IOException("Parse " + type + " time, invalid offset");
        }

        int hr, min;

        switch (buffer[pos++]) {
            case '+':
                if (len != 5) {
                    throw new IOException("Parse " + type + " time, invalid offset");
                }
                hr = 10 * toDigit(buffer[pos++], type);
                hr += toDigit(buffer[pos++], type);
                min = 10 * toDigit(buffer[pos++], type);
                min += toDigit(buffer[pos++], type);

                if (hr >= 24 || min >= 60) {
                    throw new IOException("Parse " + type + " time, +hhmm");
                }

                time -= ((hr * 60) + min) * 60 * 1000;
                break;

            case '-':
                if (len != 5) {
                    throw new IOException("Parse " + type + " time, invalid offset");
                }
                hr = 10 * toDigit(buffer[pos++], type);
                hr += toDigit(buffer[pos++], type);
                min = 10 * toDigit(buffer[pos++], type);
                min += toDigit(buffer[pos++], type);

                if (hr >= 24 || min >= 60) {
                    throw new IOException("Parse " + type + " time, -hhmm");
                }

                time += ((hr * 60) + min) * 60 * 1000;
                break;

            case 'Z':
                if (len != 1) {
                    throw new IOException("Parse " + type + " time, invalid format");
                }
                break;

            default:
                throw new IOException("Parse " + type + " time, garbage offset");
        }
        return new Date(time);
    }

    /**
     * Converts byte (represented as a char) to int.
     * @throws IOException if integer is not a valid digit in the specified
     *    radix (10)
     */
    private static int toDigit(byte b, String type) throws IOException {
        if (b < '0' || b > '9') {
            throw new IOException("Parse " + type + " time, invalid format");
        }
        return b - '0';
    }

    /**
     * Returns a Date if the DerValue is UtcTime.
     *
     * @return the Date held in this DER value
     */
    public Date getUTCTime() throws IOException {
        if (tag != tag_UtcTime) {
            throw new IOException("DerValue.getUTCTime, not a UtcTime: " + tag);
        }
        if (end - start < 11 || end - start > 17)
            throw new IOException("DER UTC Time length error");

        data.pos = data.end; // Compatibility. Reach end.
        return getTimeInternal(false);
    }

    /**
     * Returns a Date if the DerValue is GeneralizedTime.
     *
     * @return the Date held in this DER value
     */
    public Date getGeneralizedTime() throws IOException {
        if (tag != tag_GeneralizedTime) {
            throw new IOException(
                "DerValue.getGeneralizedTime, not a GeneralizedTime: " + tag);
        }
        if (end - start < 13)
            throw new IOException("DER Generalized Time length error");

        data.pos = data.end; // Compatibility. Reach end.
        return getTimeInternal(true);
    }

    /**
     * Bitwise equality comparison.  DER encoded values have a single
     * encoding, so that bitwise equality of the encoded values is an
     * efficient way to establish equivalence of the unencoded values.
     *
     * @param o the object being compared with this one
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DerValue)) {
            return false;
        }
        DerValue other = (DerValue) o;
        if (tag != other.tag) {
            return false;
        }
        if (buffer == other.buffer && start == other.start && end == other.end) {
            return true;
        }
        return Arrays.equals(buffer, start, end, other.buffer, other.start, other.end);
    }

    /**
     * Returns a printable representation of the value.
     *
     * @return printable representation of the value
     */
    @Override
    public String toString() {
        return String.format("DerValue(%02x, %s, %d, %d)",
                0xff & tag, buffer, start, end);
    }

    /**
     * Returns a DER-encoded value, such that if it's passed to the
     * DerValue constructor, a value equivalent to "this" is returned.
     *
     * @return DER-encoded value, including tag and length.
     */
    public byte[] toByteArray() throws IOException {
        data.pos = data.start; // Compatibility. At head.
        // Minimize content duplication by writing out tag and length only
        DerOutputStream out = new DerOutputStream();
        out.write(tag);
        out.putLength(end - start);
        int headLen = out.size();
        byte[] result = Arrays.copyOf(out.buf(), end - start + headLen);
        System.arraycopy(buffer, start, result, headLen, end - start);
        return result;
    }

    /**
     * For "set" and "sequence" types, this function may be used
     * to return a DER stream of the members of the set or sequence.
     * This operation is not supported for primitive types such as
     * integers or bit strings.
     */
    public DerInputStream toDerInputStream() throws IOException {
        if (tag == tag_Sequence || tag == tag_Set)
            return data;
        throw new IOException("toDerInputStream rejects tag type " + tag);
    }

    /**
     * Get the length of the encoded value.
     */
    public int length() {
        return end - start;
    }

    /**
     * Determine if a character is one of the permissible characters for
     * PrintableString:
     * A-Z, a-z, 0-9, space, apostrophe (39), left and right parentheses,
     * plus sign, comma, hyphen, period, slash, colon, equals sign,
     * and question mark.
     *
     * Characters that are *not* allowed in PrintableString include
     * exclamation point, quotation mark, number sign, dollar sign,
     * percent sign, ampersand, asterisk, semicolon, less than sign,
     * greater than sign, at sign, left and right square brackets,
     * backslash, circumflex (94), underscore, back quote (96),
     * left and right curly brackets, vertical line, tilde,
     * and the control codes (0-31 and 127).
     *
     * This list is based on X.680 (the ASN.1 spec).
     */
    public static boolean isPrintableStringChar(char ch) {
        if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
            (ch >= '0' && ch <= '9')) {
            return true;
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
                    return true;
                default:
                    return false;
            }
        }
    }

    /**
     * Create the tag of the attribute.
     *
     * @param tagClass the tag class type, one of UNIVERSAL, CONTEXT,
     *               APPLICATION or PRIVATE
     * @param form if true, the value is constructed, otherwise it
     * is primitive.
     * @param val the tag value
     */
    public static byte createTag(byte tagClass, boolean form, byte val) {
        if (val < 0 || val > 30) {
            throw new IllegalArgumentException("Tag number over 30 is not supported");
        }
        byte tag = (byte)(tagClass | val);
        if (form) {
            tag |= (byte)0x20;
        }
        return (tag);
    }

    /**
     * Set the tag of the attribute. Commonly used to reset the
     * tag value used for IMPLICIT encodings.
     *
     * This method should be avoided, consider using withTag() instead.
     *
     * @param tag the tag value
     */
    public void resetTag(byte tag) {
        this.tag = tag;
    }

    /**
     * Returns a new DerValue with a different tag. This method is used
     * to convert a DerValue decoded from an IMPLICIT encoding to its real
     * tag. The content is not checked against the tag in this method.
     *
     * @param newTag the new tag
     * @return a new DerValue
     */
    public DerValue withTag(byte newTag) {
        return new DerValue(newTag, buffer, start, end, allowBER);
    }

    /**
     * Returns a hashcode for this DerValue.
     *
     * @return a hashcode for this DerValue.
     */
    @Override
    public int hashCode() {
        int result = tag;
        for (int i = start; i < end; i++) {
            result = 31 * result + buffer[i];
        }
        return result;
    }

    /**
     * Reads the sub-values in a constructed DerValue.
     *
     * @param expectedTag the expected tag, or zero if we don't check.
     *                    This is useful when this DerValue is IMPLICIT.
     * @param startLen estimated number of sub-values
     * @return the sub-values in an array
     */
    DerValue[] subs(byte expectedTag, int startLen) throws IOException {
        if (expectedTag != 0 && expectedTag != tag) {
            throw new IOException("Not the correct tag");
        }
        List<DerValue> result = new ArrayList<>(startLen);
        DerInputStream dis = data();
        while (dis.available() > 0) {
            result.add(dis.getDerValue());
        }
        return result.toArray(new DerValue[0]);
    }

    public void clear() {
        Arrays.fill(buffer, start, end, (byte)0);
    }
}
