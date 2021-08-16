/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represent an ISO Object Identifier.
 *
 * <P>Object Identifiers are arbitrary length hierarchical identifiers.
 * The individual components are numbers, and they define paths from the
 * root of an ISO-managed identifier space.  You will sometimes see a
 * string name used instead of (or in addition to) the numerical id.
 * These are synonyms for the numerical IDs, but are not widely used
 * since most sites do not know all the requisite strings, while all
 * sites can parse the numeric forms.
 *
 * <P>So for example, JavaSoft has the sole authority to assign the
 * meaning to identifiers below the 1.3.6.1.4.1.42.2.17 node in the
 * hierarchy, and other organizations can easily acquire the ability
 * to assign such unique identifiers.
 *
 * @author David Brownell
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 */

public final class ObjectIdentifier implements Serializable {
    /*
     * The maximum encoded OID length, excluding the ASN.1 encoding tag and
     * length.
     *
     * In theory, there is no maximum size for OIDs.  However, there are some
     * limitation in practice.
     *
     * RFC 5280 mandates support for OIDs that have arc elements with values
     * that are less than 2^28 (that is, they MUST be between 0 and
     * 268,435,455, inclusive), and implementations MUST be able to handle
     * OIDs with up to 20 elements (inclusive).  Per RFC 5280, an encoded
     * OID should be less than 80 bytes for safe interoperability.
     *
     * This class could be used for protocols other than X.509 certificates.
     * To be safe, a relatively large but still reasonable value is chosen
     * as the restriction in JDK.
     */
    private static final int MAXIMUM_OID_SIZE = 4096;    // 2^12

    /**
     * We use the DER value (no tag, no length) as the internal format
     * @serial
     */
    private byte[] encoding = null;

    private transient volatile String stringForm;

    /*
     * IMPORTANT NOTES FOR CODE CHANGES (bug 4811968) IN JDK 1.7.0
     * ===========================================================
     *
     * (Almost) serialization compatibility with old versions:
     *
     * serialVersionUID is unchanged. Old field "component" is changed to
     * type Object so that "poison" (unknown object type for old versions)
     * can be put inside if there are huge components that cannot be saved
     * as integers.
     *
     * New version use the new filed "encoding" only.
     *
     * Below are all 4 cases in a serialization/deserialization process:
     *
     * 1. old -> old: Not covered here
     * 2. old -> new: There's no "encoding" field, new readObject() reads
     *    "components" and "componentLen" instead and inits correctly.
     * 3. new -> new: "encoding" field exists, new readObject() uses it
     *    (ignoring the other 2 fields) and inits correctly.
     * 4. new -> old: old readObject() only recognizes "components" and
     *    "componentLen" fields. If no huge components are involved, they
     *    are serialized as legal values and old object can init correctly.
     *    Otherwise, old object cannot recognize the form (component not int[])
     *    and throw a ClassNotFoundException at deserialization time.
     *
     * Therfore, for the first 3 cases, exact compatibility is preserved. In
     * the 4th case, non-huge OID is still supportable in old versions, while
     * huge OID is not.
     */
    @java.io.Serial
    private static final long serialVersionUID = 8697030238860181294L;

    /**
     * Changed to Object
     * @serial
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private Object      components   = null;          // path from root

    /**
     * @serial
     */
    private int         componentLen = -1;            // how much is used.

    // Is the components field calculated?
    private transient boolean   componentsCalculated = false;

    @java.io.Serial
    private void readObject(ObjectInputStream is)
            throws IOException, ClassNotFoundException {
        is.defaultReadObject();

        if (encoding == null) {  // from an old version
            int[] comp = (int[])components;
            if (componentLen > comp.length) {
                componentLen = comp.length;
            }

            // Check the estimated size before it is too late. The check
            // will be performed again in init().
            checkOidSize(componentLen);
            init(comp, componentLen);
        } else {
            checkOidSize(encoding.length);
            check(encoding);
        }
    }

    @java.io.Serial
    private void writeObject(ObjectOutputStream os)
            throws IOException {
        if (!componentsCalculated) {
            int[] comps = toIntArray();
            if (comps != null) {    // every one understands this
                components = comps;
                componentLen = comps.length;
            } else {
                components = HugeOidNotSupportedByOldJDK.theOne;
            }
            componentsCalculated = true;
        }
        os.defaultWriteObject();
    }

    static class HugeOidNotSupportedByOldJDK implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;
        static HugeOidNotSupportedByOldJDK theOne =
                new HugeOidNotSupportedByOldJDK();
    }

    /**
     * Constructs, from a string.  This string should be of the form 1.23.56.
     * Validity check included.
     */
    private ObjectIdentifier(String oid) throws IOException {
        int ch = '.';
        int start = 0;
        int end = 0;

        int pos = 0;
        byte[] tmp = new byte[oid.length()];
        int first = 0, second;
        int count = 0;

        try {
            String comp = null;
            do {
                int length = 0; // length of one section
                end = oid.indexOf(ch,start);
                if (end == -1) {
                    comp = oid.substring(start);
                    length = oid.length() - start;
                } else {
                    comp = oid.substring(start,end);
                    length = end - start;
                }

                if (length > 9) {
                    BigInteger bignum = new BigInteger(comp);
                    if (count == 0) {
                        checkFirstComponent(bignum);
                        first = bignum.intValue();
                    } else {
                        if (count == 1) {
                            checkSecondComponent(first, bignum);
                            bignum = bignum.add(BigInteger.valueOf(40*first));
                        } else {
                            checkOtherComponent(count, bignum);
                        }
                        pos += pack7Oid(bignum, tmp, pos);
                    }
                } else {
                    int num = Integer.parseInt(comp);
                    if (count == 0) {
                        checkFirstComponent(num);
                        first = num;
                    } else {
                        if (count == 1) {
                            checkSecondComponent(first, num);
                            num += 40 * first;
                        } else {
                            checkOtherComponent(count, num);
                        }
                        pos += pack7Oid(num, tmp, pos);
                    }
                }
                start = end + 1;
                count++;

                checkOidSize(pos);
            } while (end != -1);

            checkCount(count);
            encoding = new byte[pos];
            System.arraycopy(tmp, 0, encoding, 0, pos);
            this.stringForm = oid;
        } catch (IOException ioe) { // already detected by checkXXX
            throw ioe;
        } catch (Exception e) {
            throw new IOException("ObjectIdentifier() -- Invalid format: "
                    + e.toString(), e);
        }
    }

    // Called by DerValue::getOID. No need to clone input.
    ObjectIdentifier(byte[] encoding) throws IOException {
        checkOidSize(encoding.length);
        check(encoding);
        this.encoding = encoding;
    }

    /**
     * Reads an ObjectIdentifier from a DerInputStream.
     * @param in the input stream
     * @throws IOException if there is an encoding error
     */
    public ObjectIdentifier(DerInputStream in) throws IOException {
        encoding = in.getDerValue().getOID().encoding;
    }

    private void init(int[] components, int length) throws IOException {
        int pos = 0;
        byte[] tmp = new byte[length * 5 + 1];  // +1 for empty input

        if (components[1] < Integer.MAX_VALUE - components[0] * 40) {
            pos += pack7Oid(components[0] * 40 + components[1], tmp, pos);
        } else {
            BigInteger big = BigInteger.valueOf(components[1]);
            big = big.add(BigInteger.valueOf(components[0] * 40));
            pos += pack7Oid(big, tmp, pos);
        }

        for (int i = 2; i < length; i++) {
            pos += pack7Oid(components[i], tmp, pos);

            checkOidSize(pos);
        }

        encoding = new byte[pos];
        System.arraycopy(tmp, 0, encoding, 0, pos);
    }

    // oid cache index'ed by the oid string
    private static ConcurrentHashMap<String,ObjectIdentifier> oidTable =
            new ConcurrentHashMap<>();

    /**
     * Returns an ObjectIdentifier instance for the specific String.
     *
     * If the String is not a valid OID string, an IOException is thrown.
     */
    public static ObjectIdentifier of(String oidStr) throws IOException {
        // check cache first
        ObjectIdentifier oid = oidTable.get(oidStr);
        if (oid == null) {
            oid = new ObjectIdentifier(oidStr);
            oidTable.put(oidStr, oid);
        }
        return oid;
    }

    /**
     * Returns an ObjectIdentifier instance for the specific KnownOIDs.
     */
    public static ObjectIdentifier of(KnownOIDs o) {
        // check cache first
        String oidStr = o.value();
        ObjectIdentifier oid = oidTable.get(oidStr);
        if (oid == null) {
            try {
                oid = new ObjectIdentifier(oidStr);
            } catch (IOException ioe) {
                // should not happen as oid string for KnownOIDs is internal
                throw new RuntimeException(ioe);
            }
            oidTable.put(oidStr, oid);
        }
        return oid;
    }

    /*
     * n.b. the only public interface is DerOutputStream.putOID()
     */
    void encode(DerOutputStream out) throws IOException {
        out.write (DerValue.tag_ObjectId, encoding);
    }

    /**
     * Compares this identifier with another, for equality.
     *
     * @return true iff the names are identical.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ObjectIdentifier == false) {
            return false;
        }
        ObjectIdentifier other = (ObjectIdentifier)obj;
        return Arrays.equals(encoding, other.encoding);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(encoding);
    }

    /**
     * Private helper method for serialization. To be compatible with old
     * versions of JDK.
     * @return components in an int array, if all the components are less than
     *         Integer.MAX_VALUE. Otherwise, null.
     */
    private int[] toIntArray() {
        int length = encoding.length;
        int[] result = new int[20];
        int which = 0;
        int fromPos = 0;
        for (int i = 0; i < length; i++) {
            if ((encoding[i] & 0x80) == 0) {
                // one section [fromPos..i]
                if (i - fromPos + 1 > 4) {
                    BigInteger big = new BigInteger(pack(encoding,
                            fromPos, i-fromPos+1, 7, 8));
                    if (fromPos == 0) {
                        result[which++] = 2;
                        BigInteger second =
                                big.subtract(BigInteger.valueOf(80));
                        if (second.compareTo(
                                BigInteger.valueOf(Integer.MAX_VALUE)) == 1) {
                            return null;
                        } else {
                            result[which++] = second.intValue();
                        }
                    } else {
                        if (big.compareTo(
                                BigInteger.valueOf(Integer.MAX_VALUE)) == 1) {
                            return null;
                        } else {
                            result[which++] = big.intValue();
                        }
                    }
                } else {
                    int retval = 0;
                    for (int j = fromPos; j <= i; j++) {
                        retval <<= 7;
                        byte tmp = encoding[j];
                        retval |= (tmp & 0x07f);
                    }
                    if (fromPos == 0) {
                        if (retval < 80) {
                            result[which++] = retval / 40;
                            result[which++] = retval % 40;
                        } else {
                            result[which++] = 2;
                            result[which++] = retval - 80;
                        }
                    } else {
                        result[which++] = retval;
                    }
                }
                fromPos = i+1;
            }
            if (which >= result.length) {
                result = Arrays.copyOf(result, which + 10);
            }
        }
        return Arrays.copyOf(result, which);
    }

    /**
     * Returns a string form of the object ID.  The format is the
     * conventional "dot" notation for such IDs, without any
     * user-friendly descriptive strings, since those strings
     * will not be understood everywhere.
     */
    @Override
    public String toString() {
        String s = stringForm;
        if (s == null) {
            int length = encoding.length;
            StringBuilder sb = new StringBuilder(length * 4);

            int fromPos = 0;
            for (int i = 0; i < length; i++) {
                if ((encoding[i] & 0x80) == 0) {
                    // one section [fromPos..i]
                    if (fromPos != 0) {  // not the first segment
                        sb.append('.');
                    }
                    if (i - fromPos + 1 > 4) { // maybe big integer
                        BigInteger big = new BigInteger(
                                pack(encoding, fromPos, i-fromPos+1, 7, 8));
                        if (fromPos == 0) {
                            // first section encoded with more than 4 bytes,
                            // must be 2.something
                            sb.append("2.");
                            sb.append(big.subtract(BigInteger.valueOf(80)));
                        } else {
                            sb.append(big);
                        }
                    } else { // small integer
                        int retval = 0;
                        for (int j = fromPos; j <= i; j++) {
                            retval <<= 7;
                            byte tmp = encoding[j];
                            retval |= (tmp & 0x07f);
                        }
                        if (fromPos == 0) {
                            if (retval < 80) {
                                sb.append(retval/40);
                                sb.append('.');
                                sb.append(retval%40);
                            } else {
                                sb.append("2.");
                                sb.append(retval - 80);
                            }
                        } else {
                            sb.append(retval);
                        }
                    }
                    fromPos = i+1;
                }
            }
            s = sb.toString();
            stringForm = s;
        }
        return s;
    }

    /**
     * Repack all bits from input to output. On the both sides, only a portion
     * (from the least significant bit) of the 8 bits in a byte is used. This
     * number is defined as the number of useful bits (NUB) for the array. All
     * used bits from the input byte array and repacked into the output in the
     * exactly same order. The output bits are aligned so that the final bit of
     * the input (the least significant bit in the last byte), when repacked as
     * the final bit of the output, is still at the least significant position.
     * Zeroes will be padded on the left side of the first output byte if
     * necessary. All unused bits in the output are also zeroed.
     *
     * For example: if the input is 01001100 with NUB 8, the output which
     * has a NUB 6 will look like:
     *      00000001 00001100
     * The first 2 bits of the output bytes are unused bits. The other bits
     * turn out to be 000001 001100. While the 8 bits on the right are from
     * the input, the left 4 zeroes are padded to fill the 6 bits space.
     *
     * @param in        the input byte array
     * @param ioffset   start point inside <code>in</code>
     * @param ilength   number of bytes to repack
     * @param iw        NUB for input
     * @param ow        NUB for output
     * @return          the repacked bytes
     */
    private static byte[] pack(byte[] in,
            int ioffset, int ilength, int iw, int ow) {
        assert (iw > 0 && iw <= 8): "input NUB must be between 1 and 8";
        assert (ow > 0 && ow <= 8): "output NUB must be between 1 and 8";

        if (iw == ow) {
            return in.clone();
        }

        int bits = ilength * iw;    // number of all used bits
        byte[] out = new byte[(bits+ow-1)/ow];

        // starting from the 0th bit in the input
        int ipos = 0;

        // the number of padding 0's needed in the output, skip them
        int opos = (bits+ow-1)/ow*ow-bits;

        while(ipos < bits) {
            int count = iw - ipos%iw;   // unpacked bits in current input byte
            if (count > ow - opos%ow) { // free space available in output byte
                count = ow - opos%ow;   // choose the smaller number
            }

            // and move them!
            out[opos/ow] |=                     // paste!
                (((in[ioffset+ipos/iw]+256)     // locate the byte (+256 so that it's never negative)
                    >> (iw-ipos%iw-count)) &    // move to the end of a byte
                  ((1 << (count))-1))           // zero out all other bits
                        << (ow-opos%ow-count);  // move to the output position
            ipos += count;  // advance
            opos += count;  // advance
        }
        return out;
    }

    /**
     * Repack from NUB 8 to a NUB 7 OID sub-identifier, remove all
     * unnecessary 0 headings, set the first bit of all non-tail
     * output bytes to 1 (as ITU-T Rec. X.690 8.19.2 says), and
     * paste it into an existing byte array.
     * @param out the existing array to be pasted into
     * @param ooffset the starting position to paste
     * @return the number of bytes pasted
     */
    private static int pack7Oid(byte[] in,
            int ioffset, int ilength, byte[] out, int ooffset) {
        byte[] pack = pack(in, ioffset, ilength, 8, 7);
        int firstNonZero = pack.length-1;   // paste at least one byte
        for (int i=pack.length-2; i>=0; i--) {
            if (pack[i] != 0) {
                firstNonZero = i;
            }
            pack[i] |= 0x80;
        }
        System.arraycopy(pack, firstNonZero,
                out, ooffset, pack.length-firstNonZero);
        return pack.length-firstNonZero;
    }

    /**
     * Repack from NUB 7 to NUB 8, remove all unnecessary 0
     * headings, and paste it into an existing byte array.
     * @param out the existing array to be pasted into
     * @param ooffset the starting position to paste
     * @return the number of bytes pasted
     */
    private static int pack8(byte[] in,
            int ioffset, int ilength, byte[] out, int ooffset) {
        byte[] pack = pack(in, ioffset, ilength, 7, 8);
        int firstNonZero = pack.length-1;   // paste at least one byte
        for (int i=pack.length-2; i>=0; i--) {
            if (pack[i] != 0) {
                firstNonZero = i;
            }
        }
        System.arraycopy(pack, firstNonZero,
                out, ooffset, pack.length-firstNonZero);
        return pack.length-firstNonZero;
    }

    /**
     * Pack the int into a OID sub-identifier DER encoding
     */
    private static int pack7Oid(int input, byte[] out, int ooffset) {
        byte[] b = new byte[4];
        b[0] = (byte)(input >> 24);
        b[1] = (byte)(input >> 16);
        b[2] = (byte)(input >> 8);
        b[3] = (byte)(input);
        return pack7Oid(b, 0, 4, out, ooffset);
    }

    /**
     * Pack the BigInteger into a OID subidentifier DER encoding
     */
    private static int pack7Oid(BigInteger input, byte[] out, int ooffset) {
        byte[] b = input.toByteArray();
        return pack7Oid(b, 0, b.length, out, ooffset);
    }

    /**
     * Private methods to check validity of OID. They must be --
     * 1. at least 2 components
     * 2. all components must be non-negative
     * 3. the first must be 0, 1 or 2
     * 4. if the first is 0 or 1, the second must be <40
     */

    /**
     * Check the DER encoding. Since DER encoding defines that the integer bits
     * are unsigned, so there's no need to check the MSB.
     */
    private static void check(byte[] encoding) throws IOException {
        int length = encoding.length;
        if (length < 1 ||      // too short
                (encoding[length - 1] & 0x80) != 0) {  // not ended
            throw new IOException("ObjectIdentifier() -- " +
                    "Invalid DER encoding, not ended");
        }
        for (int i=0; i<length; i++) {
            // 0x80 at the beginning of a subidentifier
            if (encoding[i] == (byte)0x80 &&
                    (i==0 || (encoding[i-1] & 0x80) == 0)) {
                throw new IOException("ObjectIdentifier() -- " +
                        "Invalid DER encoding, useless extra octet detected");
            }
        }
    }

    private static void checkCount(int count) throws IOException {
        if (count < 2) {
            throw new IOException("ObjectIdentifier() -- " +
                    "Must be at least two oid components ");
        }
    }

    private static void checkFirstComponent(int first) throws IOException {
        if (first < 0 || first > 2) {
            throw new IOException("ObjectIdentifier() -- " +
                    "First oid component is invalid ");
        }
    }

    private static void checkFirstComponent(
            BigInteger first) throws IOException {
        if (first.signum() == -1 || first.compareTo(BigInteger.TWO) > 0) {
            throw new IOException("ObjectIdentifier() -- " +
                    "First oid component is invalid ");
        }
    }

    private static void checkSecondComponent(
            int first, int second) throws IOException {
        if (second < 0 || first != 2 && second > 39) {
            throw new IOException("ObjectIdentifier() -- " +
                    "Second oid component is invalid ");
        }
    }

    private static void checkSecondComponent(
            int first, BigInteger second) throws IOException {
        if (second.signum() == -1 ||
                first != 2 &&
                second.compareTo(BigInteger.valueOf(39)) == 1) {
            throw new IOException("ObjectIdentifier() -- " +
                    "Second oid component is invalid ");
        }
    }

    private static void checkOtherComponent(int i, int num) throws IOException {
        if (num < 0) {
            throw new IOException("ObjectIdentifier() -- " +
                    "oid component #" + (i+1) + " must be non-negative ");
        }
    }

    private static void checkOtherComponent(
            int i, BigInteger num) throws IOException {
        if (num.signum() == -1) {
            throw new IOException("ObjectIdentifier() -- " +
                    "oid component #" + (i+1) + " must be non-negative ");
        }
    }

    private static void checkOidSize(int oidLength) throws IOException {
        if (oidLength > MAXIMUM_OID_SIZE) {
            throw new IOException(
                    "ObjectIdentifier encoded length exceeds " +
                    "the restriction in JDK (OId length(>=): " + oidLength +
                    ", Restriction: " + MAXIMUM_OID_SIZE + ")");
        }
    }
}
