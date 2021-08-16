/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.dns;

import javax.naming.CommunicationException;
import javax.naming.InvalidNameException;

import java.io.IOException;

import java.nio.charset.StandardCharsets;


/**
 * The ResourceRecord class represents a DNS resource record.
 * The string format is based on the master file representation in
 * RFC 1035.
 *
 * @author Scott Seligman
 */


public class ResourceRecord {

    /*
     * Resource record type codes
     */
    static final int TYPE_A     =  1;
    static final int TYPE_NS    =  2;
    static final int TYPE_CNAME =  5;
    static final int TYPE_SOA   =  6;
    static final int TYPE_PTR   = 12;
    static final int TYPE_HINFO = 13;
    static final int TYPE_MX    = 15;
    static final int TYPE_TXT   = 16;
    static final int TYPE_AAAA  = 28;
    static final int TYPE_SRV   = 33;
    static final int TYPE_NAPTR = 35;
    static final int QTYPE_AXFR = 252;          // zone transfer
    static final int QTYPE_STAR = 255;          // query type "*"

    /*
     * Mapping from resource record type codes to type name strings.
     */
    static final String rrTypeNames[] = {
        null, "A", "NS", null, null,
        "CNAME", "SOA", null, null, null,
        null, null, "PTR", "HINFO", null,
        "MX", "TXT", null, null, null,
        null, null, null, null, null,
        null, null, null, "AAAA", null,
        null, null, null, "SRV", null,
        "NAPTR"
    };

    /*
     * Resource record class codes
     */
    static final int CLASS_INTERNET = 1;
    static final int CLASS_HESIOD   = 2;
    static final int QCLASS_STAR    = 255;      // query class "*"

    /*
     * Mapping from resource record type codes to class name strings.
     */
    static final String rrClassNames[] = {
        null, "IN", null, null, "HS"
    };

    /*
     * Maximum number of compression references in labels.
     * Used to detect compression loops.
     */
    private static final int MAXIMUM_COMPRESSION_REFERENCES = 16;

    byte[] msg;                 // DNS message
    int msgLen;                 // msg size (in octets)
    boolean qSection;           // true if this RR is part of question section
                                // and therefore has no ttl or rdata
    int offset;                 // offset of RR w/in msg
    int rrlen;                  // number of octets in encoded RR
    DnsName name;               // name field of RR, including root label
    int rrtype;                 // type field of RR
    String rrtypeName;          // name of rrtype
    int rrclass;                // class field of RR
    String rrclassName;         // name of rrclass
    int ttl = 0;                // ttl field of RR
    int rdlen = 0;              // number of octets of rdata
    Object rdata = null;        // rdata -- most are String, unknown are byte[]


    /*
     * Constructs a new ResourceRecord.  The encoded data of the DNS
     * message is contained in msg; data for this RR begins at msg[offset].
     * If qSection is true this RR is part of a question section.  It's
     * not a true resource record in that case, but is treated as if it
     * were a shortened one (with no ttl or rdata).  If decodeRdata is
     * false, the rdata is not decoded (and getRdata() will return null)
     * unless this is an SOA record.
     *
     * @throws CommunicationException if a decoded domain name isn't valid.
     * @throws ArrayIndexOutOfBoundsException given certain other corrupt data.
     */
    ResourceRecord(byte[] msg, int msgLen, int offset,
                   boolean qSection, boolean decodeRdata)
            throws CommunicationException {

        this.msg = msg;
        this.msgLen = msgLen;
        this.offset = offset;
        this.qSection = qSection;
        decode(decodeRdata);
    }

    public String toString() {
        String text = name + " " + rrclassName + " " + rrtypeName;
        if (!qSection) {
            text += " " + ttl + " " +
                ((rdata != null) ? rdata : "[n/a]");
        }
        return text;
    }

    /*
     * Returns the name field of this RR, including the root label.
     */
    public DnsName getName() {
        return name;
    }

    /*
     * Returns the number of octets in the encoded RR.
     */
    public int size() {
        return rrlen;
    }

    public int getType() {
        return rrtype;
    }

    public int getRrclass() {
        return rrclass;
    }

    public Object getRdata() {
        return rdata;
    }


    public static String getTypeName(int rrtype) {
        return valueToName(rrtype, rrTypeNames);
    }

    public static int getType(String typeName) {
        return nameToValue(typeName, rrTypeNames);
    }

    public static String getRrclassName(int rrclass) {
        return valueToName(rrclass, rrClassNames);
    }

    public static int getRrclass(String className) {
        return nameToValue(className, rrClassNames);
    }

    private static String valueToName(int val, String[] names) {
        String name = null;
        if ((val > 0) && (val < names.length)) {
            name = names[val];
        } else if (val == QTYPE_STAR) {         // QTYPE_STAR == QCLASS_STAR
            name = "*";
        }
        if (name == null) {
            name = Integer.toString(val);
        }
        return name;
    }

    private static int nameToValue(String name, String[] names) {
        if (name.isEmpty()) {
            return -1;                          // invalid name
        } else if (name.equals("*")) {
            return QTYPE_STAR;                  // QTYPE_STAR == QCLASS_STAR
        }
        if (Character.isDigit(name.charAt(0))) {
            try {
                return Integer.parseInt(name);
            } catch (NumberFormatException e) {
            }
        }
        for (int i = 1; i < names.length; i++) {
            if ((names[i] != null) &&
                    name.equalsIgnoreCase(names[i])) {
                return i;
            }
        }
        return -1;                              // unknown name
    }

    /*
     * Compares two SOA record serial numbers using 32-bit serial number
     * arithmetic as defined in RFC 1982.  Serial numbers are unsigned
     * 32-bit quantities.  Returns a negative, zero, or positive value
     * as the first serial number is less than, equal to, or greater
     * than the second.  If the serial numbers are not comparable the
     * result is undefined.  Note that the relation is not transitive.
     */
    public static int compareSerialNumbers(long s1, long s2) {
        long diff = s2 - s1;
        if (diff == 0) {
            return 0;
        } else if ((diff > 0 &&  diff <= 0x7FFFFFFF) ||
                   (diff < 0 && -diff >  0x7FFFFFFF)) {
            return -1;
        } else {
            return 1;
        }
    }


    /*
     * Decodes the binary format of the RR.
     * May throw ArrayIndexOutOfBoundsException given corrupt data.
     */
    private void decode(boolean decodeRdata) throws CommunicationException {
        int pos = offset;       // index of next unread octet

        name = new DnsName();                           // NAME
        pos = decodeName(pos, name);

        rrtype = getUShort(pos);                        // TYPE
        rrtypeName = (rrtype < rrTypeNames.length)
            ? rrTypeNames[rrtype]
            : null;
        if (rrtypeName == null) {
            rrtypeName = Integer.toString(rrtype);
        }
        pos += 2;

        rrclass = getUShort(pos);                       // CLASS
        rrclassName = (rrclass < rrClassNames.length)
            ? rrClassNames[rrclass]
            : null;
        if (rrclassName == null) {
            rrclassName = Integer.toString(rrclass);
        }
        pos += 2;

        if (!qSection) {
            ttl = getInt(pos);                          // TTL
            pos += 4;

            rdlen = getUShort(pos);                     // RDLENGTH
            pos += 2;

            rdata = (decodeRdata ||                     // RDATA
                     (rrtype == TYPE_SOA))
                ? decodeRdata(pos)
                : null;
            if (rdata instanceof DnsName) {
                rdata = rdata.toString();
            }
            pos += rdlen;
        }

        rrlen = pos - offset;

        msg = null;     // free up for GC
    }

    /*
     * Returns the 1-byte unsigned value at msg[pos].
     */
    private int getUByte(int pos) {
        return (msg[pos] & 0xFF);
    }

    /*
     * Returns the 2-byte unsigned value at msg[pos].  The high
     * order byte comes first.
     */
    private int getUShort(int pos) {
        return (((msg[pos] & 0xFF) << 8) |
                (msg[pos + 1] & 0xFF));
    }

    /*
     * Returns the 4-byte signed value at msg[pos].  The high
     * order byte comes first.
     */
    private int getInt(int pos) {
        return ((getUShort(pos) << 16) | getUShort(pos + 2));
    }

    /*
     * Returns the 4-byte unsigned value at msg[pos].  The high
     * order byte comes first.
     */
    private long getUInt(int pos) {
        return (getInt(pos) & 0xffffffffL);
    }

    /*
     * Returns the name encoded at msg[pos], including the root label.
     */
    private DnsName decodeName(int pos) throws CommunicationException {
        DnsName n = new DnsName();
        decodeName(pos, n);
        return n;
    }

    /*
     * Prepends to "n" the domain name encoded at msg[pos], including the root
     * label.  Returns the index into "msg" following the name.
     */
    private int decodeName(int pos, DnsName n) throws CommunicationException {
        int endPos = -1;
        int level = 0;
        try {
            while (true) {
                if (level > MAXIMUM_COMPRESSION_REFERENCES)
                    throw new IOException("Too many compression references");
                int typeAndLen = msg[pos] & 0xFF;
                if (typeAndLen == 0) {                  // end of name
                    ++pos;
                    n.add(0, "");
                    break;
                } else if (typeAndLen <= 63) {          // regular label
                    ++pos;
                    n.add(0, new String(msg, pos, typeAndLen,
                        StandardCharsets.ISO_8859_1));
                    pos += typeAndLen;
                } else if ((typeAndLen & 0xC0) == 0xC0) { // name compression
                    ++level;
                    // cater for the case where the name pointed to is itself
                    // compressed: we don't want endPos to be reset by the second
                    // compression level.
                    int ppos = pos;
                    if (endPos == -1) endPos = pos + 2;
                    pos = getUShort(pos) & 0x3FFF;
                    if (debug) {
                        dprint("decode: name compression at " + ppos
                                + " -> " + pos + " endPos=" + endPos);
                        assert endPos > 0;
                        assert pos < ppos;
                        assert pos >= Header.HEADER_SIZE;
                    }
                } else
                    throw new IOException("Invalid label type: " + typeAndLen);
            }
        } catch (IOException | InvalidNameException e) {
            CommunicationException ce =new CommunicationException(
                "DNS error: malformed packet");
            ce.initCause(e);
            throw ce;
        }
        if (endPos == -1)
            endPos = pos;
        return endPos;
    }

    /*
     * Returns the rdata encoded at msg[pos].  The format is dependent
     * on the rrtype and rrclass values, which have already been set.
     * The length of the encoded data is rdlen, which has already been
     * set.
     * The rdata of records with unknown type/class combinations is
     * returned in a newly-allocated byte array.
     */
    private Object decodeRdata(int pos) throws CommunicationException {
        if (rrclass == CLASS_INTERNET) {
            switch (rrtype) {
            case TYPE_A:
                return decodeA(pos);
            case TYPE_AAAA:
                return decodeAAAA(pos);
            case TYPE_CNAME:
            case TYPE_NS:
            case TYPE_PTR:
                return decodeName(pos);
            case TYPE_MX:
                return decodeMx(pos);
            case TYPE_SOA:
                return decodeSoa(pos);
            case TYPE_SRV:
                return decodeSrv(pos);
            case TYPE_NAPTR:
                return decodeNaptr(pos);
            case TYPE_TXT:
                return decodeTxt(pos);
            case TYPE_HINFO:
                return decodeHinfo(pos);
            }
        }
        // Unknown RR type/class
        if (debug) {
            dprint("Unknown RR type for RR data: " + rrtype + " rdlen=" + rdlen
                   + ", pos=" + pos +", msglen=" + msg.length + ", remaining="
                   + (msg.length-pos));
        }
        byte[] rd = new byte[rdlen];
        System.arraycopy(msg, pos, rd, 0, rdlen);
        return rd;
    }

    /*
     * Returns the rdata of an MX record that is encoded at msg[pos].
     */
    private String decodeMx(int pos) throws CommunicationException {
        int preference = getUShort(pos);
        pos += 2;
        DnsName name = decodeName(pos);
        return (preference + " " + name);
    }

    /*
     * Returns the rdata of an SOA record that is encoded at msg[pos].
     */
    private String decodeSoa(int pos) throws CommunicationException {
        DnsName mname = new DnsName();
        pos = decodeName(pos, mname);
        DnsName rname = new DnsName();
        pos = decodeName(pos, rname);

        long serial = getUInt(pos);
        pos += 4;
        long refresh = getUInt(pos);
        pos += 4;
        long retry = getUInt(pos);
        pos += 4;
        long expire = getUInt(pos);
        pos += 4;
        long minimum = getUInt(pos);    // now used as negative TTL
        pos += 4;

        return (mname + " " + rname + " " + serial + " " +
                refresh + " " + retry + " " + expire + " " + minimum);
    }

    /*
     * Returns the rdata of an SRV record that is encoded at msg[pos].
     * See RFC 2782.
     */
    private String decodeSrv(int pos) throws CommunicationException {
        int priority = getUShort(pos);
        pos += 2;
        int weight =   getUShort(pos);
        pos += 2;
        int port =     getUShort(pos);
        pos += 2;
        DnsName target = decodeName(pos);
        return (priority + " " + weight + " " + port + " " + target);
    }

    /*
     * Returns the rdata of an NAPTR record that is encoded at msg[pos].
     * See RFC 2915.
     */
    private String decodeNaptr(int pos) throws CommunicationException {
        int order = getUShort(pos);
        pos += 2;
        int preference = getUShort(pos);
        pos += 2;
        StringBuffer flags = new StringBuffer();
        pos += decodeCharString(pos, flags);
        StringBuffer services = new StringBuffer();
        pos += decodeCharString(pos, services);
        StringBuffer regexp = new StringBuffer(rdlen);
        pos += decodeCharString(pos, regexp);
        DnsName replacement = decodeName(pos);

        return (order + " " + preference + " " + flags + " " +
                services + " " + regexp + " " + replacement);
    }

    /*
     * Returns the rdata of a TXT record that is encoded at msg[pos].
     * The rdata consists of one or more <character-string>s.
     */
    private String decodeTxt(int pos) {
        StringBuffer buf = new StringBuffer(rdlen);
        int end = pos + rdlen;
        while (pos < end) {
            pos += decodeCharString(pos, buf);
            if (pos < end) {
                buf.append(' ');
            }
        }
        return buf.toString();
    }

    /*
     * Returns the rdata of an HINFO record that is encoded at msg[pos].
     * The rdata consists of two <character-string>s.
     */
    private String decodeHinfo(int pos) {
        StringBuffer buf = new StringBuffer(rdlen);
        pos += decodeCharString(pos, buf);
        buf.append(' ');
        pos += decodeCharString(pos, buf);
        return buf.toString();
    }

    /*
     * Decodes the <character-string> at msg[pos] and adds it to buf.
     * If the string contains one of the meta-characters ' ', '\\', or
     * '"', then the result is quoted and any embedded '\\' or '"'
     * chars are escaped with '\\'.  Empty strings are also quoted.
     * Returns the size of the encoded string, including the initial
     * length octet.
     */
    private int decodeCharString(int pos, StringBuffer buf) {
        int start = buf.length();       // starting index of this string
        int len = getUByte(pos++);      // encoded string length
        boolean quoted = (len == 0);    // quote string if empty
        for (int i = 0; i < len; i++) {
            int c = getUByte(pos++);
            quoted |= (c == ' ');
            if ((c == '\\') || (c == '"')) {
                quoted = true;
                buf.append('\\');
            }
            buf.append((char) c);
        }
        if (quoted) {
            buf.insert(start, '"');
            buf.append('"');
        }
        return (len + 1);       // size includes initial octet
    }

    /*
     * Returns the rdata of an A record, in dotted-decimal format,
     * that is encoded at msg[pos].
     */
    private String decodeA(int pos) {
        return ((msg[pos] & 0xff) + "." +
                (msg[pos + 1] & 0xff) + "." +
                (msg[pos + 2] & 0xff) + "." +
                (msg[pos + 3] & 0xff));
    }

    /*
     * Returns the rdata of an AAAA record, in colon-separated format,
     * that is encoded at msg[pos].  For example:  4321:0:1:2:3:4:567:89ab.
     * See RFCs 1886 and 2373.
     */
    private String decodeAAAA(int pos) {
        int[] addr6 = new int[8];  // the unsigned 16-bit words of the address
        for (int i = 0; i < 8; i++) {
            addr6[i] = getUShort(pos);
            pos += 2;
        }

        // Find longest sequence of two or more zeros, to compress them.
        int curBase = -1;
        int curLen = 0;
        int bestBase = -1;
        int bestLen = 0;
        for (int i = 0; i < 8; i++) {
            if (addr6[i] == 0) {
                if (curBase == -1) {    // new sequence
                    curBase = i;
                    curLen = 1;
                } else {                // extend sequence
                    ++curLen;
                    if ((curLen >= 2) && (curLen > bestLen)) {
                        bestBase = curBase;
                        bestLen = curLen;
                    }
                }
            } else {                    // not in sequence
                curBase = -1;
            }
        }

        // If addr begins with at least 6 zeros and is not :: or ::1,
        // or with 5 zeros followed by 0xffff, use the text format for
        // IPv4-compatible or IPv4-mapped addresses.
        if (bestBase == 0) {
            if ((bestLen == 6) ||
                    ((bestLen == 7) && (addr6[7] > 1))) {
                return ("::" + decodeA(pos - 4));
            } else if ((bestLen == 5) && (addr6[5] == 0xffff)) {
                return ("::ffff:" + decodeA(pos - 4));
            }
        }

        // If bestBase != -1, compress zeros in [bestBase, bestBase+bestLen)
        boolean compress = (bestBase != -1);

        StringBuilder sb = new StringBuilder(40);
        if (bestBase == 0) {
            sb.append(':');
        }
        for (int i = 0; i < 8; i++) {
            if (!compress || (i < bestBase) || (i >= bestBase + bestLen)) {
                sb.append(Integer.toHexString(addr6[i]));
                if (i < 7) {
                    sb.append(':');
                }
            } else if (compress && (i == bestBase)) {  // first compressed zero
                sb.append(':');
            }
        }

        return sb.toString();
    }

    //-------------------------------------------------------------------------

    private static final boolean debug = false;

    private static void dprint(String mess) {
        if (debug) {
            System.err.println("DNS: " + mess);
        }
    }

}
