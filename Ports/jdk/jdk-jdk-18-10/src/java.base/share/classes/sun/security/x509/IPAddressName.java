/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.x509;

import java.io.IOException;
import java.lang.Integer;
import java.net.InetAddress;
import java.util.Arrays;
import sun.security.util.HexDumpEncoder;
import sun.security.util.BitArray;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

/**
 * This class implements the IPAddressName as required by the GeneralNames
 * ASN.1 object.  Both IPv4 and IPv6 addresses are supported using the
 * formats specified in IETF PKIX RFC 5280.
 * <p>
 * [RFC 5280 4.2.1.6 Subject Alternative Name]
 * When the subjectAltName extension contains an iPAddress, the address
 * MUST be stored in the octet string in "network byte order", as
 * specified in [RFC791].  The least significant bit (LSB) of each octet
 * is the LSB of the corresponding byte in the network address.  For IP
 * version 4, as specified in [RFC791], the octet string MUST contain
 * exactly four octets.  For IP version 6, as specified in
 * [RFC 2460], the octet string MUST contain exactly sixteen octets.
 * <p>
 * [RFC 5280 4.2.1.10 Name Constraints]
 * The syntax of iPAddress MUST be as described in Section 4.2.1.6 with
 * the following additions specifically for name constraints.  For IPv4
 * addresses, the iPAddress field of GeneralName MUST contain eight (8)
 * octets, encoded in the style of RFC 4632 (CIDR) to represent an
 * address range [RFC 4632].  For IPv6 addresses, the iPAddress field
 * MUST contain 32 octets similarly encoded.  For example, a name
 * constraint for "class C" subnet 192.0.2.0 is represented as the
 * octets C0 00 02 00 FF FF FF 00, representing the CIDR notation
 * 192.0.2.0/24 (mask 255.255.255.0).
 * <p>
 * @see GeneralName
 * @see GeneralNameInterface
 * @see GeneralNames
 *
 *
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 */
public class IPAddressName implements GeneralNameInterface {
    private byte[] address;
    private boolean isIPv4;
    private String name;

    /**
     * Create the IPAddressName object from the passed encoded Der value.
     *
     * @param derValue the encoded DER IPAddressName.
     * @exception IOException on error.
     */
    public IPAddressName(DerValue derValue) throws IOException {
        this(derValue.getOctetString());
    }

    /**
     * Create the IPAddressName object with the specified octets.
     *
     * @param address the IP address
     * @throws IOException if address is not a valid IPv4 or IPv6 address
     */
    public IPAddressName(byte[] address) throws IOException {
        /*
         * A valid address must consist of 4 bytes of address and
         * optional 4 bytes of 4 bytes of mask, or 16 bytes of address
         * and optional 16 bytes of mask.
         */
        if (address.length == 4 || address.length == 8) {
            isIPv4 = true;
        } else if (address.length == 16 || address.length == 32) {
            isIPv4 = false;
        } else {
            throw new IOException("Invalid IPAddressName");
        }
        this.address = address;
    }

    /**
     * Create an IPAddressName from a String.
     * [IETF RFC1338 Supernetting {@literal &} IETF RFC1519 Classless Inter-Domain
     * Routing (CIDR)] For IPv4 addresses, the forms are
     * "b1.b2.b3.b4" or "b1.b2.b3.b4/m1.m2.m3.m4", where b1 - b4 are decimal
     * byte values 0-255 and m1 - m4 are decimal mask values
     * 0 - 255.
     * <p>
     * [IETF RFC2373 IP Version 6 Addressing Architecture]
     * For IPv6 addresses, the forms are "a1:a2:...:a8" or "a1:a2:...:a8/n",
     * where a1-a8 are hexadecimal values representing the eight 16-bit pieces
     * of the address. If /n is used, n is a decimal number indicating how many
     * of the leftmost contiguous bits of the address comprise the prefix for
     * this subnet. Internally, a mask value is created using the prefix length.
     *
     * @param name String form of IPAddressName
     * @throws IOException if name can not be converted to a valid IPv4 or IPv6
     *     address
     */
    public IPAddressName(String name) throws IOException {

        if (name == null || name.isEmpty()) {
            throw new IOException("IPAddress cannot be null or empty");
        }
        if (name.charAt(name.length() - 1) == '/') {
            throw new IOException("Invalid IPAddress: " + name);
        }

        if (name.indexOf(':') >= 0) {
            // name is IPv6: uses colons as value separators
            // Parse name into byte-value address components and optional
            // prefix
            parseIPv6(name);
            isIPv4 = false;
        } else if (name.indexOf('.') >= 0) {
            //name is IPv4: uses dots as value separators
            parseIPv4(name);
            isIPv4 = true;
        } else {
            throw new IOException("Invalid IPAddress: " + name);
        }
    }

    /**
     * Parse an IPv4 address.
     *
     * @param name IPv4 address with optional mask values
     * @throws IOException on error
     */
    private void parseIPv4(String name) throws IOException {

        // Parse name into byte-value address components
        int slashNdx = name.indexOf('/');
        if (slashNdx == -1) {
            address = InetAddress.getByName(name).getAddress();
        } else {
            address = new byte[8];

            // parse mask
            byte[] mask = InetAddress.getByName
                (name.substring(slashNdx+1)).getAddress();

            // parse base address
            byte[] host = InetAddress.getByName
                (name.substring(0, slashNdx)).getAddress();

            System.arraycopy(host, 0, address, 0, 4);
            System.arraycopy(mask, 0, address, 4, 4);
        }
    }

    /**
     * Parse an IPv6 address.
     *
     * @param name String IPv6 address with optional /<prefix length>
     *             If /<prefix length> is present, address[] array will
     *             be 32 bytes long, otherwise 16.
     * @throws IOException on error
     */
    private static final int MASKSIZE = 16;
    private void parseIPv6(String name) throws IOException {

        int slashNdx = name.indexOf('/');
        if (slashNdx == -1) {
            address = InetAddress.getByName(name).getAddress();
        } else {
            address = new byte[32];
            byte[] base = InetAddress.getByName
                (name.substring(0, slashNdx)).getAddress();
            System.arraycopy(base, 0, address, 0, 16);

            // append a mask corresponding to the num of prefix bits specified
            int prefixLen = Integer.parseInt(name.substring(slashNdx+1));
            if (prefixLen < 0 || prefixLen > 128) {
                throw new IOException("IPv6Address prefix length (" +
                        prefixLen + ") in out of valid range [0,128]");
            }

            // create new bit array initialized to zeros
            BitArray bitArray = new BitArray(MASKSIZE * 8);

            // set all most significant bits up to prefix length
            for (int i = 0; i < prefixLen; i++)
                bitArray.set(i, true);
            byte[] maskArray = bitArray.toByteArray();

            // copy mask bytes into mask portion of address
            for (int i = 0; i < MASKSIZE; i++)
                address[MASKSIZE+i] = maskArray[i];
        }
    }

    /**
     * Return the type of the GeneralName.
     */
    public int getType() {
        return NAME_IP;
    }

    /**
     * Encode the IPAddress name into the DerOutputStream.
     *
     * @param out the DER stream to encode the IPAddressName to.
     * @exception IOException on encoding errors.
     */
    public void encode(DerOutputStream out) throws IOException {
        out.putOctetString(address);
    }

    /**
     * Return a printable string of IPaddress
     */
    public String toString() {
        try {
            return "IPAddress: " + getName();
        } catch (IOException ioe) {
            // dump out hex rep for debugging purposes
            HexDumpEncoder enc = new HexDumpEncoder();
            return "IPAddress: " + enc.encodeBuffer(address);
        }
    }

    /**
     * Return a standard String representation of IPAddress.
     * See IPAddressName(String) for the formats used for IPv4
     * and IPv6 addresses.
     *
     * @throws IOException if the IPAddress cannot be converted to a String
     */
    public String getName() throws IOException {
        if (name != null)
            return name;

        if (isIPv4) {
            //IPv4 address or subdomain
            byte[] host = new byte[4];
            System.arraycopy(address, 0, host, 0, 4);
            name = InetAddress.getByAddress(host).getHostAddress();
            if (address.length == 8) {
                byte[] mask = new byte[4];
                System.arraycopy(address, 4, mask, 0, 4);
                name = name + '/' +
                       InetAddress.getByAddress(mask).getHostAddress();
            }
        } else {
            //IPv6 address or subdomain
            byte[] host = new byte[16];
            System.arraycopy(address, 0, host, 0, 16);
            name = InetAddress.getByAddress(host).getHostAddress();
            if (address.length == 32) {
                // IPv6 subdomain: display prefix length

                // copy subdomain into new array and convert to BitArray
                byte[] maskBytes = new byte[16];
                for (int i=16; i < 32; i++)
                    maskBytes[i-16] = address[i];
                BitArray ba = new BitArray(16*8, maskBytes);
                // Find first zero bit
                int i=0;
                for (; i < 16*8; i++) {
                    if (!ba.get(i))
                        break;
                }
                name = name + '/' + i;
                // Verify remaining bits 0
                for (; i < 16*8; i++) {
                    if (ba.get(i)) {
                        throw new IOException("Invalid IPv6 subdomain - set " +
                            "bit " + i + " not contiguous");
                    }
                }
            }
        }
        return name;
    }

    /**
     * Returns this IPAddress name as a byte array.
     */
    public byte[] getBytes() {
        return address.clone();
    }

    /**
     * Compares this name with another, for equality.
     *
     * @return true iff the names are identical.
     */
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (!(obj instanceof IPAddressName))
            return false;

        IPAddressName otherName = (IPAddressName)obj;
        byte[] other = otherName.address;

        if (other.length != address.length)
            return false;

        if (address.length == 8 || address.length == 32) {
            // Two subnet addresses
            // Mask each and compare masked values
            int maskLen = address.length/2;
            for (int i=0; i < maskLen; i++) {
                byte maskedThis = (byte)(address[i] & address[i+maskLen]);
                byte maskedOther = (byte)(other[i] & other[i+maskLen]);
                if (maskedThis != maskedOther) {
                    return false;
                }
            }
            // Now compare masks
            for (int i=maskLen; i < address.length; i++)
                if (address[i] != other[i])
                    return false;
            return true;
        } else {
            // Two IPv4 host addresses or two IPv6 host addresses
            // Compare bytes
            return Arrays.equals(other, address);
        }
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    public int hashCode() {
        int retval = 0;

        for (int i=0; i<address.length; i++)
            retval += address[i] * i;

        return retval;
    }

    /**
     * Return type of constraint inputName places on this name:<ul>
     *   <li>NAME_DIFF_TYPE = -1: input name is different type from name
     *       (i.e. does not constrain).
     *   <li>NAME_MATCH = 0: input name matches name.
     *   <li>NAME_NARROWS = 1: input name narrows name (is lower in the naming
     *       subtree)
     *   <li>NAME_WIDENS = 2: input name widens name (is higher in the naming
     *       subtree)
     *   <li>NAME_SAME_TYPE = 3: input name does not match or narrow name, but
     *       is same type.
     * </ul>.  These results are used in checking NameConstraints during
     * certification path verification.
     * <p>
     * [RFC 5280 4.2.1.10 Name Constraints]
     * The syntax of iPAddress MUST be as described in Section 4.2.1.6 with
     * the following additions specifically for name constraints.  For IPv4
     * addresses, the iPAddress field of GeneralName MUST contain eight (8)
     * octets, encoded in the style of RFC 4632 (CIDR) to represent an
     * address range [RFC 4632].  For IPv6 addresses, the iPAddress field
     * MUST contain 32 octets similarly encoded.  For example, a name
     * constraint for "class C" subnet 192.0.2.0 is represented as the
     * octets C0 00 02 00 FF FF FF 00, representing the CIDR notation
     * 192.0.2.0/24 (mask 255.255.255.0).
     *
     * @param inputName to be checked for being constrained
     * @return constraint type above
     * @throws UnsupportedOperationException if name is not exact match, but
     * narrowing and widening are not supported for this name type.
     */
    public int constrains(GeneralNameInterface inputName)
    throws UnsupportedOperationException {
        int constraintType;
        if (inputName == null)
            constraintType = NAME_DIFF_TYPE;
        else if (inputName.getType() != NAME_IP)
            constraintType = NAME_DIFF_TYPE;
        else if (((IPAddressName)inputName).equals(this))
            constraintType = NAME_MATCH;
        else {
            IPAddressName otherName = (IPAddressName)inputName;
            byte[] otherAddress = otherName.address;
            if (otherAddress.length == 4 && address.length == 4)
                // Two host addresses
                constraintType = NAME_SAME_TYPE;
            else if ((otherAddress.length == 8 && address.length == 8) ||
                     (otherAddress.length == 32 && address.length == 32)) {
                // Two subnet addresses
                // See if one address fully encloses the other address
                boolean otherSubsetOfThis = true;
                boolean thisSubsetOfOther = true;
                boolean thisEmpty = false;
                boolean otherEmpty = false;
                int maskOffset = address.length/2;
                for (int i=0; i < maskOffset; i++) {
                    if ((byte)(address[i] & address[i+maskOffset]) != address[i])
                        thisEmpty=true;
                    if ((byte)(otherAddress[i] & otherAddress[i+maskOffset]) != otherAddress[i])
                        otherEmpty=true;
                    if (!(((byte)(address[i+maskOffset] & otherAddress[i+maskOffset]) == address[i+maskOffset]) &&
                          ((byte)(address[i]   & address[i+maskOffset])      == (byte)(otherAddress[i] & address[i+maskOffset])))) {
                        otherSubsetOfThis = false;
                    }
                    if (!(((byte)(otherAddress[i+maskOffset] & address[i+maskOffset])      == otherAddress[i+maskOffset]) &&
                          ((byte)(otherAddress[i]   & otherAddress[i+maskOffset]) == (byte)(address[i] & otherAddress[i+maskOffset])))) {
                        thisSubsetOfOther = false;
                    }
                }
                if (thisEmpty || otherEmpty) {
                    if (thisEmpty && otherEmpty)
                        constraintType = NAME_MATCH;
                    else if (thisEmpty)
                        constraintType = NAME_WIDENS;
                    else
                        constraintType = NAME_NARROWS;
                } else if (otherSubsetOfThis)
                    constraintType = NAME_NARROWS;
                else if (thisSubsetOfOther)
                    constraintType = NAME_WIDENS;
                else
                    constraintType = NAME_SAME_TYPE;
            } else if (otherAddress.length == 8 || otherAddress.length == 32) {
                //Other is a subnet, this is a host address
                int i = 0;
                int maskOffset = otherAddress.length/2;
                for (; i < maskOffset; i++) {
                    // Mask this address by other address mask and compare to other address
                    // If all match, then this address is in other address subnet
                    if ((address[i] & otherAddress[i+maskOffset]) != otherAddress[i])
                        break;
                }
                if (i == maskOffset)
                    constraintType = NAME_WIDENS;
                else
                    constraintType = NAME_SAME_TYPE;
            } else if (address.length == 8 || address.length == 32) {
                //This is a subnet, other is a host address
                int i = 0;
                int maskOffset = address.length/2;
                for (; i < maskOffset; i++) {
                    // Mask other address by this address mask and compare to this address
                    if ((otherAddress[i] & address[i+maskOffset]) != address[i])
                        break;
                }
                if (i == maskOffset)
                    constraintType = NAME_NARROWS;
                else
                    constraintType = NAME_SAME_TYPE;
            } else {
                constraintType = NAME_SAME_TYPE;
            }
        }
        return constraintType;
    }

    /**
     * Return subtree depth of this name for purposes of determining
     * NameConstraints minimum and maximum bounds and for calculating
     * path lengths in name subtrees.
     *
     * @return distance of name from root
     * @throws UnsupportedOperationException if not supported for this name type
     */
    public int subtreeDepth() throws UnsupportedOperationException {
        throw new UnsupportedOperationException
            ("subtreeDepth() not defined for IPAddressName");
    }
}
