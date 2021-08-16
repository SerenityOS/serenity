/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap;

import javax.naming.NamingException;
import javax.naming.directory.InvalidSearchFilterException;

import java.io.IOException;

/**
 * LDAP (RFC-1960) and LDAPv3 (RFC-2254) search filters.
 *
 * @author Xuelei Fan
 * @author Vincent Ryan
 * @author Jagane Sundar
 * @author Rosanna Lee
 */

final class Filter {

    /**
     * First convert filter string into byte[].
     * For LDAP v3, the conversion uses Unicode -> UTF8
     * For LDAP v2, the conversion uses Unicode -> ISO 8859 (Latin-1)
     *
     * Then parse the byte[] as a filter, converting \hh to
     * a single byte, and encoding the resulting filter
     * into the supplied BER buffer
     */
    static void encodeFilterString(BerEncoder ber, String filterStr,
        boolean isLdapv3) throws IOException, NamingException {

        if ((filterStr == null) || (filterStr.isEmpty())) {
            throw new InvalidSearchFilterException("Empty filter");
        }
        byte[] filter;
        int filterLen;
        if (isLdapv3) {
            filter = filterStr.getBytes("UTF8");
        } else {
            filter = filterStr.getBytes("8859_1");
        }
        filterLen = filter.length;
        if (dbg) {
            dbgIndent = 0;
            System.err.println("String filter: " + filterStr);
            System.err.println("size: " + filterLen);
            dprint("original: ", filter, 0, filterLen);
        }

        encodeFilter(ber, filter, 0, filterLen);
    }

    private static void encodeFilter(BerEncoder ber, byte[] filter,
        int filterStart, int filterEnd) throws IOException, NamingException {

        if (dbg) {
            dprint("encFilter: ",  filter, filterStart, filterEnd);
            dbgIndent++;
        }

        if ((filterEnd - filterStart) <= 0) {
            throw new InvalidSearchFilterException("Empty filter");
        }

        int nextOffset;
        int parens, balance;
        boolean escape;

        parens = 0;

        int filtOffset[] = new int[1];

        for (filtOffset[0] = filterStart; filtOffset[0] < filterEnd;) {
            switch (filter[filtOffset[0]]) {
            case '(':
                filtOffset[0]++;
                parens++;
                switch (filter[filtOffset[0]]) {
                case '&':
                    encodeComplexFilter(ber, filter,
                        LDAP_FILTER_AND, filtOffset, filterEnd);
                    // filtOffset[0] has pointed to char after right paren
                    parens--;
                    break;

                case '|':
                    encodeComplexFilter(ber, filter,
                        LDAP_FILTER_OR, filtOffset, filterEnd);
                    // filtOffset[0] has pointed to char after right paren
                    parens--;
                    break;

                case '!':
                    encodeComplexFilter(ber, filter,
                        LDAP_FILTER_NOT, filtOffset, filterEnd);
                    // filtOffset[0] has pointed to char after right paren
                    parens--;
                    break;

                default:
                    balance = 1;
                    escape = false;
                    nextOffset = filtOffset[0];
                    while (nextOffset < filterEnd && balance > 0) {
                        if (!escape) {
                            if (filter[nextOffset] == '(')
                                balance++;
                            else if (filter[nextOffset] == ')')
                                balance--;
                        }
                        if (filter[nextOffset] == '\\' && !escape)
                            escape = true;
                        else
                            escape = false;
                        if (balance > 0)
                            nextOffset++;
                    }
                    if (balance != 0)
                        throw new InvalidSearchFilterException(
                                  "Unbalanced parenthesis");

                    encodeSimpleFilter(ber, filter, filtOffset[0], nextOffset);

                    // points to the char after right paren.
                    filtOffset[0] = nextOffset + 1;

                    parens--;
                    break;

                }
                break;

            case ')':
                //
                // End of sequence
                //
                ber.endSeq();
                filtOffset[0]++;
                parens--;
                break;

            case ' ':
                filtOffset[0]++;
                break;

            default:    // assume simple type=value filter
                encodeSimpleFilter(ber, filter, filtOffset[0], filterEnd);
                filtOffset[0] = filterEnd; // force break from outer
                break;
            }

            if (parens < 0) {
                throw new InvalidSearchFilterException(
                                                "Unbalanced parenthesis");
            }
        }

        if (parens != 0) {
            throw new InvalidSearchFilterException("Unbalanced parenthesis");
        }

        if (dbg) {
            dbgIndent--;
        }

    }

    // called by the LdapClient.compare method
    static byte[] unescapeFilterValue(byte[] orig, int start, int end)
        throws NamingException {
        boolean escape = false, escStart = false;
        int ival;
        byte ch;

        if (dbg) {
            dprint("unescape: " , orig, start, end);
        }

        int len = end - start;
        byte tbuf[] = new byte[len];
        int j = 0;
        for (int i = start; i < end; i++) {
            ch = orig[i];
            if (escape) {
                // Try LDAP V3 escape (\xx)
                if ((ival = Character.digit(ch, 16)) < 0) {

                    /**
                     * If there is no hex char following a '\' when
                     * parsing a LDAP v3 filter (illegal by v3 way)
                     * we fallback to the way we unescape in v2.
                     */
                    if (escStart) {
                        // V2: \* \( \)
                        escape = false;
                        tbuf[j++] = ch;
                    } else {
                        // escaping already started but we can't find 2nd hex
                        throw new InvalidSearchFilterException("invalid escape sequence: " + orig);
                    }
                } else {
                    if (escStart) {
                        tbuf[j] = (byte)(ival<<4);
                        escStart = false;
                    } else {
                        tbuf[j++] |= (byte)ival;
                        escape = false;
                    }
                }
            } else if (ch != '\\') {
                tbuf[j++] = ch;
                escape = false;
            } else {
                escStart = escape = true;
            }
        }
        byte[] answer = new byte[j];
        System.arraycopy(tbuf, 0, answer, 0, j);
        if (dbg) {
            Ber.dumpBER(System.err, "", answer, 0, j);
        }
        return answer;
    }

    private static int indexOf(byte[] str, char ch, int start, int end) {
        for (int i = start; i < end; i++) {
            if (str[i] == ch)
                return i;
        }
        return -1;
    }

    private static int indexOf(byte[] str, String target, int start, int end) {
        int where = indexOf(str, target.charAt(0), start, end);
        if (where >= 0) {
            for (int i = 1; i < target.length(); i++) {
                if (str[where+i] != target.charAt(i)) {
                    return -1;
                }
            }
        }
        return where;
    }

    private static int findUnescaped(byte[] str, char ch, int start, int end) {
        while (start < end) {
            int where = indexOf(str, ch, start, end);

            /*
             * Count the immediate preceding '\' to find out if
             * this is an escaped '*'. This is a made-up way for
             * parsing an escaped '*' in v2. This is how the other leading
             * SDK vendors interpret v2.
             * For v3 we fallback to the way we parse "\*" in v2.
             * It's not legal in v3 to use "\*" to escape '*'; the right
             * way is to use "\2a" instead.
             */
            int backSlashPos;
            int backSlashCnt = 0;
            for (backSlashPos = where - 1;
                    ((backSlashPos >= start) && (str[backSlashPos] == '\\'));
                    backSlashPos--, backSlashCnt++);

            // if at start of string, or not there at all, or if not escaped
            if (where == start || where == -1 || ((backSlashCnt % 2) == 0))
                return where;

            // start search after escaped star
            start = where + 1;
        }
        return -1;
    }


    private static void encodeSimpleFilter(BerEncoder ber, byte[] filter,
        int filtStart, int filtEnd) throws IOException, NamingException {

        if (dbg) {
            dprint("encSimpleFilter: ", filter, filtStart, filtEnd);
            dbgIndent++;
        }

        String type, value;
        int valueStart, valueEnd, typeStart, typeEnd;

        int eq;
        if ((eq = indexOf(filter, '=', filtStart, filtEnd)) == -1) {
            throw new InvalidSearchFilterException("Missing 'equals'");
        }


        valueStart = eq + 1;        // value starts after equal sign
        valueEnd = filtEnd;
        typeStart = filtStart;      // beginning of string

        int ftype;

        switch (filter[eq - 1]) {
        case '<':
            ftype = LDAP_FILTER_LE;
            typeEnd = eq - 1;
            break;
        case '>':
            ftype = LDAP_FILTER_GE;
            typeEnd = eq - 1;
            break;
        case '~':
            ftype = LDAP_FILTER_APPROX;
            typeEnd = eq - 1;
            break;
        case ':':
            ftype = LDAP_FILTER_EXT;
            typeEnd = eq - 1;
            break;
        default:
            typeEnd = eq;
            //initializing ftype to make the compiler happy
            ftype = 0x00;
            break;
        }

        if (dbg) {
            System.err.println("type: " + typeStart + ", " + typeEnd);
            System.err.println("value: " + valueStart + ", " + valueEnd);
        }

        // check validity of type
        //
        // RFC4512 defines the type as the following ABNF:
        //     attr = attributedescription
        //     attributedescription = attributetype options
        //     attributetype = oid
        //     oid = descr / numericoid
        //     descr = keystring
        //     keystring = leadkeychar *keychar
        //     leadkeychar = ALPHA
        //     keychar = ALPHA / DIGIT / HYPHEN
        //     numericoid = number 1*( DOT number )
        //     number  = DIGIT / ( LDIGIT 1*DIGIT )
        //     options = *( SEMI option )
        //     option = 1*keychar
        //
        // And RFC4515 defines the extensible type as the following ABNF:
        //     attr [dnattrs] [matchingrule] / [dnattrs] matchingrule
        int optionsStart = -1;
        int extensibleStart = -1;
        if ((filter[typeStart] >= '0' && filter[typeStart] <= '9') ||
            (filter[typeStart] >= 'A' && filter[typeStart] <= 'Z') ||
            (filter[typeStart] >= 'a' && filter[typeStart] <= 'z')) {

            boolean isNumericOid =
                filter[typeStart] >= '0' && filter[typeStart] <= '9';
            for (int i = typeStart + 1; i < typeEnd; i++) {
                // ';' is an indicator of attribute options
                if (filter[i] == ';') {
                    if (isNumericOid && filter[i - 1] == '.') {
                        throw new InvalidSearchFilterException(
                                    "invalid attribute description");
                    }

                    // attribute options
                    optionsStart = i;
                    break;
                }

                // ':' is an indicator of extensible rules
                if (filter[i] == ':' && ftype == LDAP_FILTER_EXT) {
                    if (isNumericOid && filter[i - 1] == '.') {
                        throw new InvalidSearchFilterException(
                                    "invalid attribute description");
                    }

                    // extensible matching
                    extensibleStart = i;
                    break;
                }

                if (isNumericOid) {
                    // numeric object identifier
                    if ((filter[i] == '.' && filter[i - 1] == '.') ||
                        (filter[i] != '.' &&
                            !(filter[i] >= '0' && filter[i] <= '9'))) {
                        throw new InvalidSearchFilterException(
                                    "invalid attribute description");
                    }
                } else {
                    // descriptor
                    // The underscore ("_") character is not allowed by
                    // the LDAP specification. We allow it here to
                    // tolerate the incorrect use in practice.
                    if (filter[i] != '-' && filter[i] != '_' &&
                        !(filter[i] >= '0' && filter[i] <= '9') &&
                        !(filter[i] >= 'A' && filter[i] <= 'Z') &&
                        !(filter[i] >= 'a' && filter[i] <= 'z')) {
                        throw new InvalidSearchFilterException(
                                    "invalid attribute description");
                    }
                }
            }
        } else if (ftype == LDAP_FILTER_EXT && filter[typeStart] == ':') {
            // extensible matching
            extensibleStart = typeStart;
        } else {
            throw new InvalidSearchFilterException(
                                    "invalid attribute description");
        }

        // check attribute options
        if (optionsStart > 0) {
            for (int i = optionsStart + 1; i < typeEnd; i++) {
                if (filter[i] == ';') {
                    if (filter[i - 1] == ';') {
                        throw new InvalidSearchFilterException(
                                    "invalid attribute description");
                    }
                    continue;
                }

                // ':' is an indicator of extensible rules
                if (filter[i] == ':' && ftype == LDAP_FILTER_EXT) {
                    if (filter[i - 1] == ';') {
                        throw new InvalidSearchFilterException(
                                    "invalid attribute description");
                    }

                    // extensible matching
                    extensibleStart = i;
                    break;
                }

                // The underscore ("_") character is not allowed by
                // the LDAP specification. We allow it here to
                // tolerate the incorrect use in practice.
                if (filter[i] != '-' && filter[i] != '_' &&
                        !(filter[i] >= '0' && filter[i] <= '9') &&
                        !(filter[i] >= 'A' && filter[i] <= 'Z') &&
                        !(filter[i] >= 'a' && filter[i] <= 'z')) {
                    throw new InvalidSearchFilterException(
                                    "invalid attribute description");
                }
            }
        }

        // check extensible matching
        if (extensibleStart > 0) {
            boolean isMatchingRule = false;
            for (int i = extensibleStart + 1; i < typeEnd; i++) {
                if (filter[i] == ':') {
                    throw new InvalidSearchFilterException(
                                    "invalid attribute description");
                } else if ((filter[i] >= '0' && filter[i] <= '9') ||
                           (filter[i] >= 'A' && filter[i] <= 'Z') ||
                           (filter[i] >= 'a' && filter[i] <= 'z')) {
                    boolean isNumericOid = filter[i] >= '0' && filter[i] <= '9';
                    i++;
                    for (int j = i; j < typeEnd; j++, i++) {
                        // allows no more than two extensible rules
                        if (filter[j] == ':') {
                            if (isMatchingRule) {
                                throw new InvalidSearchFilterException(
                                            "invalid attribute description");
                            }
                            if (isNumericOid && filter[j - 1] == '.') {
                                throw new InvalidSearchFilterException(
                                            "invalid attribute description");
                            }

                            isMatchingRule = true;
                            break;
                        }

                        if (isNumericOid) {
                            // numeric object identifier
                            if ((filter[j] == '.' && filter[j - 1] == '.') ||
                                (filter[j] != '.' &&
                                    !(filter[j] >= '0' && filter[j] <= '9'))) {
                                throw new InvalidSearchFilterException(
                                            "invalid attribute description");
                            }
                        } else {
                            // descriptor
                            // The underscore ("_") character is not allowed by
                            // the LDAP specification. We allow it here to
                            // tolerate the incorrect use in practice.
                            if (filter[j] != '-' && filter[j] != '_' &&
                                !(filter[j] >= '0' && filter[j] <= '9') &&
                                !(filter[j] >= 'A' && filter[j] <= 'Z') &&
                                !(filter[j] >= 'a' && filter[j] <= 'z')) {
                                throw new InvalidSearchFilterException(
                                            "invalid attribute description");
                            }
                        }
                    }
                } else {
                    throw new InvalidSearchFilterException(
                                    "invalid attribute description");
                }
            }
        }

        // ensure the latest byte is not isolated
        if (filter[typeEnd - 1] == '.' || filter[typeEnd - 1] == ';' ||
                                          filter[typeEnd - 1] == ':') {
            throw new InvalidSearchFilterException(
                "invalid attribute description");
        }

        if (typeEnd == eq) { // filter type is of "equal"
            if (findUnescaped(filter, '*', valueStart, valueEnd) == -1) {
                ftype = LDAP_FILTER_EQUALITY;
            } else if (filter[valueStart] == '*' &&
                            valueStart == (valueEnd - 1)) {
                ftype = LDAP_FILTER_PRESENT;
            } else {
                encodeSubstringFilter(ber, filter,
                    typeStart, typeEnd, valueStart, valueEnd);
                return;
            }
        }

        if (ftype == LDAP_FILTER_PRESENT) {
            ber.encodeOctetString(filter, ftype, typeStart, typeEnd-typeStart);
        } else if (ftype == LDAP_FILTER_EXT) {
            encodeExtensibleMatch(ber, filter,
                typeStart, typeEnd, valueStart, valueEnd);
        } else {
            ber.beginSeq(ftype);
                ber.encodeOctetString(filter, Ber.ASN_OCTET_STR,
                    typeStart, typeEnd - typeStart);
                ber.encodeOctetString(
                    unescapeFilterValue(filter, valueStart, valueEnd),
                    Ber.ASN_OCTET_STR);
            ber.endSeq();
        }

        if (dbg) {
            dbgIndent--;
        }
    }

    private static void encodeSubstringFilter(BerEncoder ber, byte[] filter,
        int typeStart, int typeEnd, int valueStart, int valueEnd)
        throws IOException, NamingException {

        if (dbg) {
            dprint("encSubstringFilter: type ", filter, typeStart, typeEnd);
            dprint(", val : ", filter, valueStart, valueEnd);
            dbgIndent++;
        }

        ber.beginSeq(LDAP_FILTER_SUBSTRINGS);
            ber.encodeOctetString(filter, Ber.ASN_OCTET_STR,
                    typeStart, typeEnd-typeStart);
            ber.beginSeq(LdapClient.LBER_SEQUENCE);
                int index;
                int previndex = valueStart;
                while ((index = findUnescaped(filter, '*', previndex, valueEnd)) != -1) {
                    if (previndex == valueStart) {
                      if (previndex < index) {
                          if (dbg)
                              System.err.println(
                                  "initial: " + previndex + "," + index);
                        ber.encodeOctetString(
                            unescapeFilterValue(filter, previndex, index),
                            LDAP_SUBSTRING_INITIAL);
                      }
                    } else {
                      if (previndex < index) {
                          if (dbg)
                              System.err.println("any: " + previndex + "," + index);
                        ber.encodeOctetString(
                            unescapeFilterValue(filter, previndex, index),
                            LDAP_SUBSTRING_ANY);
                      }
                    }
                    previndex = index + 1;
                }
                if (previndex < valueEnd) {
                    if (dbg)
                        System.err.println("final: " + previndex + "," + valueEnd);
                  ber.encodeOctetString(
                      unescapeFilterValue(filter, previndex, valueEnd),
                      LDAP_SUBSTRING_FINAL);
                }
            ber.endSeq();
        ber.endSeq();

        if (dbg) {
            dbgIndent--;
        }
    }

    // The complex filter types look like:
    //     "&(type=val)(type=val)"
    //     "|(type=val)(type=val)"
    //     "!(type=val)"
    //
    // The filtOffset[0] pointing to the '&', '|', or '!'.
    //
    private static void encodeComplexFilter(BerEncoder ber, byte[] filter,
        int filterType, int filtOffset[], int filtEnd)
        throws IOException, NamingException {

        if (dbg) {
            dprint("encComplexFilter: ", filter, filtOffset[0], filtEnd);
            dprint(", type: " + Integer.toString(filterType, 16));
            dbgIndent++;
        }

        filtOffset[0]++;

        ber.beginSeq(filterType);

            int[] parens = findRightParen(filter, filtOffset, filtEnd);
            encodeFilterList(ber, filter, filterType, parens[0], parens[1]);

        ber.endSeq();

        if (dbg) {
            dbgIndent--;
        }

    }

    //
    // filter at filtOffset[0] - 1 points to a (. Find ) that matches it
    // and return substring between the parens. Adjust filtOffset[0] to
    // point to char after right paren
    //
    private static int[] findRightParen(byte[] filter, int filtOffset[], int end)
    throws IOException, NamingException {

        int balance = 1;
        boolean escape = false;
        int nextOffset = filtOffset[0];

        while (nextOffset < end && balance > 0) {
            if (!escape) {
                if (filter[nextOffset] == '(')
                    balance++;
                else if (filter[nextOffset] == ')')
                    balance--;
            }
            if (filter[nextOffset] == '\\' && !escape)
                escape = true;
            else
                escape = false;
            if (balance > 0)
                nextOffset++;
        }
        if (balance != 0) {
            throw new InvalidSearchFilterException("Unbalanced parenthesis");
        }

        // String tmp = filter.substring(filtOffset[0], nextOffset);

        int[] tmp = new int[] {filtOffset[0], nextOffset};

        filtOffset[0] = nextOffset + 1;

        return tmp;

    }

    //
    // Encode filter list of type "(filter1)(filter2)..."
    //
    private static void encodeFilterList(BerEncoder ber, byte[] filter,
        int filterType, int start, int end) throws IOException, NamingException {

        if (dbg) {
            dprint("encFilterList: ", filter, start, end);
            dbgIndent++;
        }

        int filtOffset[] = new int[1];
        int listNumber = 0;
        for (filtOffset[0] = start; filtOffset[0] < end; filtOffset[0]++) {
            if (Character.isSpaceChar((char)filter[filtOffset[0]]))
                continue;

            if ((filterType == LDAP_FILTER_NOT) && (listNumber > 0)) {
                throw new InvalidSearchFilterException(
                    "Filter (!) cannot be followed by more than one filters");
            }

            if (filter[filtOffset[0]] == '(') {
                continue;
            }

            int[] parens = findRightParen(filter, filtOffset, end);

            // add enclosing parens
            int len = parens[1]-parens[0];
            byte[] newfilter = new byte[len+2];
            System.arraycopy(filter, parens[0], newfilter, 1, len);
            newfilter[0] = (byte)'(';
            newfilter[len+1] = (byte)')';
            encodeFilter(ber, newfilter, 0, newfilter.length);

            listNumber++;
        }

        if (dbg) {
            dbgIndent--;
        }

    }

    //
    // Encode extensible match
    //
    private static void encodeExtensibleMatch(BerEncoder ber, byte[] filter,
        int matchStart, int matchEnd, int valueStart, int valueEnd)
        throws IOException, NamingException {

        boolean matchDN = false;
        int colon;
        int colon2;
        int i;

        ber.beginSeq(LDAP_FILTER_EXT);

            // test for colon separator
            if ((colon = indexOf(filter, ':', matchStart, matchEnd)) >= 0) {

                // test for match DN
                if ((i = indexOf(filter, ":dn", colon, matchEnd)) >= 0) {
                    matchDN = true;
                }

                // test for matching rule
                if (((colon2 = indexOf(filter, ':', colon + 1, matchEnd)) >= 0)
                    || (i == -1)) {

                    if (i == colon) {
                        ber.encodeOctetString(filter, LDAP_FILTER_EXT_RULE,
                            colon2 + 1, matchEnd - (colon2 + 1));

                    } else if ((i == colon2) && (i >= 0)) {
                        ber.encodeOctetString(filter, LDAP_FILTER_EXT_RULE,
                            colon + 1, colon2 - (colon + 1));

                    } else {
                        ber.encodeOctetString(filter, LDAP_FILTER_EXT_RULE,
                            colon + 1, matchEnd - (colon + 1));
                    }
                }

                // test for attribute type
                if (colon > matchStart) {
                    ber.encodeOctetString(filter,
                        LDAP_FILTER_EXT_TYPE, matchStart, colon - matchStart);
                }
            } else {
                ber.encodeOctetString(filter, LDAP_FILTER_EXT_TYPE, matchStart,
                    matchEnd - matchStart);
            }

            ber.encodeOctetString(
                unescapeFilterValue(filter, valueStart, valueEnd),
                LDAP_FILTER_EXT_VAL);

            /*
             * This element is defined in RFC-2251 with an ASN.1 DEFAULT tag.
             * However, for Active Directory interoperability it is transmitted
             * even when FALSE.
             */
            ber.encodeBoolean(matchDN, LDAP_FILTER_EXT_DN);

        ber.endSeq();
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // some debug print code that does indenting. Useful for debugging
    // the filter generation code
    //
    ////////////////////////////////////////////////////////////////////////////

    private static final boolean dbg = false;
    private static int dbgIndent = 0;

    private static void dprint(String msg) {
        dprint(msg, new byte[0], 0, 0);
    }

    private static void dprint(String msg, byte[] str) {
        dprint(msg, str, 0, str.length);
    }

    private static void dprint(String msg, byte[] str, int start, int end) {
        String dstr = "  ";
        int i = dbgIndent;
        while (i-- > 0) {
            dstr += "  ";
        }
        dstr += msg;

        System.err.print(dstr);
        for (int j = start; j < end; j++) {
            System.err.print((char)str[j]);
        }
        System.err.println();
    }

    /////////////// Constants used for encoding filter //////////////

    static final int LDAP_FILTER_AND = 0xa0;
    static final int LDAP_FILTER_OR = 0xa1;
    static final int LDAP_FILTER_NOT = 0xa2;
    static final int LDAP_FILTER_EQUALITY = 0xa3;
    static final int LDAP_FILTER_SUBSTRINGS = 0xa4;
    static final int LDAP_FILTER_GE = 0xa5;
    static final int LDAP_FILTER_LE = 0xa6;
    static final int LDAP_FILTER_PRESENT = 0x87;
    static final int LDAP_FILTER_APPROX = 0xa8;
    static final int LDAP_FILTER_EXT = 0xa9;            // LDAPv3

    static final int LDAP_FILTER_EXT_RULE = 0x81;       // LDAPv3
    static final int LDAP_FILTER_EXT_TYPE = 0x82;       // LDAPv3
    static final int LDAP_FILTER_EXT_VAL = 0x83;        // LDAPv3
    static final int LDAP_FILTER_EXT_DN = 0x84;         // LDAPv3

    static final int LDAP_SUBSTRING_INITIAL = 0x80;
    static final int LDAP_SUBSTRING_ANY = 0x81;
    static final int LDAP_SUBSTRING_FINAL = 0x82;
}
