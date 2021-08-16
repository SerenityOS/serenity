/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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

package javax.naming.ldap;

import java.util.List;
import java.util.ArrayList;

import javax.naming.InvalidNameException;

/*
 * RFC2253Parser implements a recursive descent parser for a single DN.
 */
final class Rfc2253Parser {

        private final String name;      // DN being parsed
        private final char[] chars;     // characters in LDAP name being parsed
        private final int len;  // length of "chars"
        private int cur = 0;    // index of first unconsumed char in "chars"

        /*
         * Given an LDAP DN in string form, returns a parser for it.
         */
        Rfc2253Parser(String name) {
            this.name = name;
            len = name.length();
            chars = name.toCharArray();
        }

        /*
         * Parses the DN, returning a List of its RDNs.
         */
        // public List<Rdn> getDN() throws InvalidNameException {

        List<Rdn> parseDn() throws InvalidNameException {
            cur = 0;

            // ArrayList<Rdn> rdns =
            //  new ArrayList<Rdn>(len / 3 + 10);  // leave room for growth

            ArrayList<Rdn> rdns =
                new ArrayList<>(len / 3 + 10);  // leave room for growth

            if (len == 0) {
                return rdns;
            }

            rdns.add(doParse(new Rdn()));
            while (cur < len) {
                if (chars[cur] == ',' || chars[cur] == ';') {
                    ++cur;
                    rdns.add(0, doParse(new Rdn()));
                } else {
                    throw new InvalidNameException("Invalid name: " + name);
                }
            }
            return rdns;
        }

        /*
         * Parses the DN, if it is known to contain a single RDN.
         */
        Rdn parseRdn() throws InvalidNameException {
            return parseRdn(new Rdn());
        }

        /*
         * Parses the DN, if it is known to contain a single RDN.
         */
        Rdn parseRdn(Rdn rdn) throws InvalidNameException {
            rdn = doParse(rdn);
            if (cur < len) {
                throw new InvalidNameException("Invalid RDN: " + name);
            }
            return rdn;
        }

        /*
         * Parses the next RDN and returns it.  Throws an exception if
         * none is found.  Leading and trailing whitespace is consumed.
         */
         private Rdn doParse(Rdn rdn) throws InvalidNameException {

            while (cur < len) {
                consumeWhitespace();
                String attrType = parseAttrType();
                consumeWhitespace();
                if (cur >= len || chars[cur] != '=') {
                    throw new InvalidNameException("Invalid name: " + name);
                }
                ++cur;          // consume '='
                consumeWhitespace();
                String value = parseAttrValue();
                consumeWhitespace();

                rdn.put(attrType, Rdn.unescapeValue(value));
                if (cur >= len || chars[cur] != '+') {
                    break;
                }
                ++cur;          // consume '+'
            }
            rdn.sort();
            return rdn;
        }

        /*
         * Returns the attribute type that begins at the next unconsumed
         * char.  No leading whitespace is expected.
         * This routine is more generous than RFC 2253.  It accepts
         * attribute types composed of any nonempty combination of Unicode
         * letters, Unicode digits, '.', '-', and internal space characters.
         */
        private String parseAttrType() throws InvalidNameException {

            final int beg = cur;
            while (cur < len) {
                char c = chars[cur];
                if (Character.isLetterOrDigit(c) ||
                        c == '.' ||
                        c == '-' ||
                        c == ' ') {
                    ++cur;
                } else {
                    break;
                }
            }
            // Back out any trailing spaces.
            while ((cur > beg) && (chars[cur - 1] == ' ')) {
                --cur;
            }

            if (beg == cur) {
                throw new InvalidNameException("Invalid name: " + name);
            }
            return new String(chars, beg, cur - beg);
        }

        /*
         * Returns the attribute value that begins at the next unconsumed
         * char.  No leading whitespace is expected.
         */
        private String parseAttrValue() throws InvalidNameException {

            if (cur < len && chars[cur] == '#') {
                return parseBinaryAttrValue();
            } else if (cur < len && chars[cur] == '"') {
                return parseQuotedAttrValue();
            } else {
                return parseStringAttrValue();
            }
        }

        private String parseBinaryAttrValue() throws InvalidNameException {
            final int beg = cur;
            ++cur;                      // consume '#'
            while ((cur < len) &&
                    Character.isLetterOrDigit(chars[cur])) {
                ++cur;
            }
            return new String(chars, beg, cur - beg);
        }

        private String parseQuotedAttrValue() throws InvalidNameException {

            final int beg = cur;
            ++cur;                      // consume '"'

            while ((cur < len) && chars[cur] != '"') {
                if (chars[cur] == '\\') {
                    ++cur;              // consume backslash, then what follows
                }
                ++cur;
            }
            if (cur >= len) {   // no closing quote
                throw new InvalidNameException("Invalid name: " + name);
            }
            ++cur;      // consume closing quote

            return new String(chars, beg, cur - beg);
        }

        private String parseStringAttrValue() throws InvalidNameException {

            final int beg = cur;
            int esc = -1;       // index of the most recently escaped character

            while ((cur < len) && !atTerminator()) {
                if (chars[cur] == '\\') {
                    ++cur;              // consume backslash, then what follows
                    esc = cur;
                }
                ++cur;
            }
            if (cur > len) {            // 'twas backslash followed by nothing
                throw new InvalidNameException("Invalid name: " + name);
            }

            // Trim off (unescaped) trailing whitespace.
            int end;
            for (end = cur; end > beg; end--) {
                if (!isWhitespace(chars[end - 1]) || (esc == end - 1)) {
                    break;
                }
            }
            return new String(chars, beg, end - beg);
        }

        private void consumeWhitespace() {
            while ((cur < len) && isWhitespace(chars[cur])) {
                ++cur;
            }
        }

        /*
         * Returns true if next unconsumed character is one that terminates
         * a string attribute value.
         */
        private boolean atTerminator() {
            return (cur < len &&
                    (chars[cur] == ',' ||
                        chars[cur] == ';' ||
                        chars[cur] == '+'));
        }

        /*
         * Best guess as to what RFC 2253 means by "whitespace".
         */
        private static boolean isWhitespace(char c) {
            return (c == ' ' || c == '\r');
        }
    }
