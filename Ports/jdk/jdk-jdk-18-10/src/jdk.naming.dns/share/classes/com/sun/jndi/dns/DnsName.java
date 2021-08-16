/*
 * Copyright (c) 2000, 2011, Oracle and/or its affiliates. All rights reserved.
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


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;

import javax.naming.*;


/**
 * {@code DnsName} implements compound names for DNS as specified by
 * RFCs 1034 and 1035, and as updated and clarified by RFCs 1123 and 2181.
 *
 * <p> The labels in a domain name correspond to JNDI atomic names.
 * Each label must be less than 64 octets in length, and only the
 * optional root label at the end of the name may be 0 octets long.
 * The sum of the lengths of all labels in a name, plus the number of
 * non-root labels plus 1, must be less than 256.  The textual
 * representation of a domain name consists of the labels, escaped as
 * needed, dot-separated, and ordered right-to-left.
 *
 * <p> A label consists of a sequence of octets, each of which may
 * have any value from 0 to 255.
 *
 * <p> <em>Host names</em> are a subset of domain names.
 * Their labels contain only ASCII letters, digits, and hyphens, and
 * none may begin or end with a hyphen.  While names not conforming to
 * these rules may be valid domain names, they will not be usable by a
 * number of DNS applications, and should in most cases be avoided.
 *
 * <p> DNS does not specify an encoding (such as UTF-8) to use for
 * octets with non-ASCII values.  As of this writing there is some
 * work going on in this area, but it is not yet finalized.
 * {@code DnsName} currently converts any non-ASCII octets into
 * characters using ISO-LATIN-1 encoding, in effect taking the
 * value of each octet and storing it directly into the low-order byte
 * of a Java character and <i>vice versa</i>.  As a consequence, no
 * character in a DNS name will ever have a non-zero high-order byte.
 * When the work on internationalizing domain names has stabilized
 * (see for example <i>draft-ietf-idn-idna-10.txt</i>), {@code DnsName}
 * may be updated to conform to that work.
 *
 * <p> Backslash ({@code \}) is used as the escape character in the
 * textual representation of a domain name.  The character sequence
 * `{@code \DDD}', where {@code DDD} is a 3-digit decimal number
 * (with leading zeros if needed), represents the octet whose value
 * is {@code DDD}.  The character sequence `{@code \C}', where
 * {@code C} is a character other than {@code '0'} through
 * {@code '9'}, represents the octet whose value is that of
 * {@code C} (again using ISO-LATIN-1 encoding); this is particularly
 * useful for escaping {@code '.'} or backslash itself.  Backslash is
 * otherwise not allowed in a domain name.  Note that escape characters
 * are interpreted when a name is parsed.  So, for example, the character
 * sequences `{@code S}', `{@code \S}', and `{@code \083}' each
 * represent the same one-octet name.  The {@code toString()} method
 * does not generally insert escape sequences except where necessary.
 * If, however, the {@code DnsName} was constructed using unneeded
 * escapes, those escapes may appear in the {@code toString} result.
 *
 * <p> Atomic names passed as parameters to methods of
 * {@code DnsName}, and those returned by them, are unescaped.  So,
 * for example, <code>(new&nbsp;DnsName()).add("a.b")</code> creates an
 * object representing the one-label domain name {@code a\.b}, and
 * calling {@code get(0)} on this object returns {@code "a.b"}.
 *
 * <p> While DNS names are case-preserving, comparisons between them
 * are case-insensitive.  When comparing names containing non-ASCII
 * octets, {@code DnsName} uses case-insensitive comparison
 * between pairs of ASCII values, and exact binary comparison
 * otherwise.

 * <p> A {@code DnsName} instance is not synchronized against
 * concurrent access by multiple threads.
 *
 * @author Scott Seligman
 */


public final class DnsName implements Name {

    // If non-null, the domain name represented by this DnsName.
    private String domain = "";

    // The labels of this domain name, as a list of strings.  Index 0
    // corresponds to the leftmost (least significant) label:  note that
    // this is the reverse of the ordering used by the Name interface.
    private ArrayList<String> labels = new ArrayList<>();

    // The number of octets needed to carry this domain name in a DNS
    // packet.  Equal to the sum of the lengths of each label, plus the
    // number of non-root labels, plus 1.  Must remain less than 256.
    private short octets = 1;


    /**
     * Constructs a {@code DnsName} representing the empty domain name.
     */
    public DnsName() {
    }

    /**
     * Constructs a {@code DnsName} representing a given domain name.
     *
     * @param   name    the domain name to parse
     * @throws InvalidNameException if {@code name} does not conform
     *          to DNS syntax.
     */
    public DnsName(String name) throws InvalidNameException {
        parse(name);
    }

    /*
     * Returns a new DnsName with its name components initialized to
     * the components of "n" in the range [beg,end).  Indexing is as
     * for the Name interface, with 0 being the most significant.
     */
    private DnsName(DnsName n, int beg, int end) {
        // Compute indexes into "labels", which has least-significant label
        // at index 0 (opposite to the convention used for "beg" and "end").
        int b = n.size() - end;
        int e = n.size() - beg;
        labels.addAll(n.labels.subList(b, e));

        if (size() == n.size()) {
            domain = n.domain;
            octets = n.octets;
        } else {
            for (String label: labels) {
                if (label.length() > 0) {
                    octets += (short) (label.length() + 1);
                }
            }
        }
    }


    public String toString() {
        if (domain == null) {
            StringBuilder buf = new StringBuilder();
            for (String label: labels) {
                if (buf.length() > 0 || label.length() == 0) {
                    buf.append('.');
                }
                escape(buf, label);
            }
            domain = buf.toString();
        }
        return domain;
    }

    /**
     * Does this domain name follow <em>host name</em> syntax?
     */
    public boolean isHostName() {
        for (String label: labels) {
            if (!isHostNameLabel(label)) {
                return false;
            }
        }
        return true;
    }

    public short getOctets() {
        return octets;
    }

    public int size() {
        return labels.size();
    }

    public boolean isEmpty() {
        return (size() == 0);
    }

    public int hashCode() {
        int h = 0;
        for (int i = 0; i < size(); i++) {
            h = 31 * h + getKey(i).hashCode();
        }
        return h;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Name) || (obj instanceof CompositeName)) {
            return false;
        }
        Name n = (Name) obj;
        return ((size() == n.size()) &&         // shortcut:  do sizes differ?
                (compareTo(obj) == 0));
    }

    public int compareTo(Object obj) {
        Name n = (Name) obj;
        return compareRange(0, size(), n);      // never 0 if sizes differ
    }

    public boolean startsWith(Name n) {
        return ((size() >= n.size()) &&
                (compareRange(0, n.size(), n) == 0));
    }

    public boolean endsWith(Name n) {
        return ((size() >= n.size()) &&
                (compareRange(size() - n.size(), size(), n) == 0));
    }

    public String get(int pos) {
        if (pos < 0 || pos >= size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int i = size() - pos - 1;       // index of "pos" component in "labels"
        return labels.get(i);
    }

    public Enumeration<String> getAll() {
        return new Enumeration<String>() {
            int pos = 0;
            public boolean hasMoreElements() {
                return (pos < size());
            }
            public String nextElement() {
                if (pos < size()) {
                    return get(pos++);
                }
                throw new java.util.NoSuchElementException();
            }
        };
    }

    public Name getPrefix(int pos) {
        return new DnsName(this, 0, pos);
    }

    public Name getSuffix(int pos) {
        return new DnsName(this, pos, size());
    }

    public Object clone() {
        return new DnsName(this, 0, size());
    }

    public Object remove(int pos) {
        if (pos < 0 || pos >= size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int i = size() - pos - 1;     // index of element to remove in "labels"
        String label = labels.remove(i);
        int len = label.length();
        if (len > 0) {
            octets -= (short) (len + 1);
        }
        domain = null;          // invalidate "domain"
        return label;
    }

    public Name add(String comp) throws InvalidNameException {
        return add(size(), comp);
    }

    public Name add(int pos, String comp) throws InvalidNameException {
        if (pos < 0 || pos > size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        // Check for empty labels:  may have only one, and only at end.
        int len = comp.length();
        if ((pos > 0 && len == 0) ||
            (pos == 0 && hasRootLabel())) {
                throw new InvalidNameException(
                        "Empty label must be the last label in a domain name");
        }
        // Check total name length.
        if (len > 0) {
            if (octets + len + 1 >= 256) {
                throw new InvalidNameException("Name too long");
            }
            octets += (short) (len + 1);
        }

        int i = size() - pos;   // index for insertion into "labels"
        verifyLabel(comp);
        labels.add(i, comp);

        domain = null;          // invalidate "domain"
        return this;
    }

    public Name addAll(Name suffix) throws InvalidNameException {
        return addAll(size(), suffix);
    }

    public Name addAll(int pos, Name n) throws InvalidNameException {
        if (n instanceof DnsName) {
            // "n" is a DnsName so we can insert it as a whole, rather than
            // verifying and inserting it component-by-component.
            // More code, but less work.
            DnsName dn = (DnsName) n;

            if (dn.isEmpty()) {
                return this;
            }
            // Check for empty labels:  may have only one, and only at end.
            if ((pos > 0 && dn.hasRootLabel()) ||
                (pos == 0 && hasRootLabel())) {
                    throw new InvalidNameException(
                        "Empty label must be the last label in a domain name");
            }

            short newOctets = (short) (octets + dn.octets - 1);
            if (newOctets > 255) {
                throw new InvalidNameException("Name too long");
            }
            octets = newOctets;
            int i = size() - pos;       // index for insertion into "labels"
            labels.addAll(i, dn.labels);

            // Preserve "domain" if we're appending or prepending,
            // otherwise invalidate it.
            if (isEmpty()) {
                domain = dn.domain;
            } else if (domain == null || dn.domain == null) {
                domain = null;
            } else if (pos == 0) {
                domain += (dn.domain.equals(".") ? "" : ".") + dn.domain;
            } else if (pos == size()) {
                domain = dn.domain + (domain.equals(".") ? "" : ".") + domain;
            } else {
                domain = null;
            }

        } else if (n instanceof CompositeName) {
            n = (DnsName) n;            // force ClassCastException

        } else {                // "n" is a compound name, but not a DnsName.
            // Add labels least-significant first:  sometimes more efficient.
            for (int i = n.size() - 1; i >= 0; i--) {
                add(pos, n.get(i));
            }
        }
        return this;
    }


    boolean hasRootLabel() {
        return (!isEmpty() &&
                get(0).isEmpty());
    }

    /*
     * Helper method for public comparison methods.  Lexicographically
     * compares components of this name in the range [beg,end) with
     * all components of "n".  Indexing is as for the Name interface,
     * with 0 being the most significant.  Returns negative, zero, or
     * positive as these name components are less than, equal to, or
     * greater than those of "n".
     */
    private int compareRange(int beg, int end, Name n) {
        if (n instanceof CompositeName) {
            n = (DnsName) n;                    // force ClassCastException
        }
        // Loop through labels, starting with most significant.
        int minSize = Math.min(end - beg, n.size());
        for (int i = 0; i < minSize; i++) {
            String label1 = get(i + beg);
            String label2 = n.get(i);

            int j = size() - (i + beg) - 1;     // index of label1 in "labels"
            // assert (label1 == labels.get(j));

            int c = compareLabels(label1, label2);
            if (c != 0) {
                return c;
            }
        }
        return ((end - beg) - n.size());        // longer range wins
    }

    /*
     * Returns a key suitable for hashing the label at index i.
     * Indexing is as for the Name interface, with 0 being the most
     * significant.
     */
    String getKey(int i) {
        return keyForLabel(get(i));
    }


    /*
     * Parses a domain name, setting the values of instance vars accordingly.
     */
    private void parse(String name) throws InvalidNameException {

        StringBuilder label = new StringBuilder();      // label being parsed

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

            if (c == '\\') {                    // found an escape sequence
                c = getEscapedOctet(name, i++);
                if (isDigit(name.charAt(i))) {  // sequence is \DDD
                    i += 2;                     // consume remaining digits
                }
                label.append(c);

            } else if (c != '.') {              // an unescaped octet
                label.append(c);

            } else {                            // found '.' separator
                add(0, label.toString());       // check syntax, then add label
                                                //   to end of name
                label.delete(0, i);             // clear buffer for next label
            }
        }

        // If name is neither "." nor "", the octets (zero or more)
        // from the rightmost dot onward are now added as the final
        // label of the name.  Those two are special cases in that for
        // all other domain names, the number of labels is one greater
        // than the number of dot separators.
        if (!name.isEmpty() && !name.equals(".")) {
            add(0, label.toString());
        }

        domain = name;          // do this last, since add() sets it to null
    }

    /*
     * Returns (as a char) the octet indicated by the escape sequence
     * at a given position within a domain name.
     * @throws InvalidNameException if a valid escape sequence is not found.
     */
    private static char getEscapedOctet(String name, int pos)
                                                throws InvalidNameException {
        try {
            // assert (name.charAt(pos) == '\\');
            char c1 = name.charAt(++pos);
            if (isDigit(c1)) {          // sequence is `\DDD'
                char c2 = name.charAt(++pos);
                char c3 = name.charAt(++pos);
                if (isDigit(c2) && isDigit(c3)) {
                    return (char)
                        ((c1 - '0') * 100 + (c2 - '0') * 10 + (c3 - '0'));
                } else {
                    throw new InvalidNameException(
                            "Invalid escape sequence in " + name);
                }
            } else {                    // sequence is `\C'
                return c1;
            }
        } catch (IndexOutOfBoundsException e) {
            throw new InvalidNameException(
                    "Invalid escape sequence in " + name);
        }
    }

    /*
     * Checks that this label is valid.
     * @throws InvalidNameException if label is not valid.
     */
    private static void verifyLabel(String label) throws InvalidNameException {
        if (label.length() > 63) {
            throw new InvalidNameException(
                    "Label exceeds 63 octets: " + label);
        }
        // Check for two-byte characters.
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if ((c & 0xFF00) != 0) {
                throw new InvalidNameException(
                        "Label has two-byte char: " + label);
            }
        }
    }

    /*
     * Does this label conform to host name syntax?
     */
    private static boolean isHostNameLabel(String label) {
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (!isHostNameChar(c)) {
                return false;
            }
        }
        return !(label.startsWith("-") || label.endsWith("-"));
    }

    private static boolean isHostNameChar(char c) {
        return (c == '-' ||
                c >= 'a' && c <= 'z' ||
                c >= 'A' && c <= 'Z' ||
                c >= '0' && c <= '9');
    }

    private static boolean isDigit(char c) {
        return (c >= '0' && c <= '9');
    }

    /*
     * Append a label to buf, escaping as needed.
     */
    private static void escape(StringBuilder buf, String label) {
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (c == '.' || c == '\\') {
                buf.append('\\');
            }
            buf.append(c);
        }
    }

    /*
     * Compares two labels, ignoring case for ASCII values.
     * Returns negative, zero, or positive as the first label
     * is less than, equal to, or greater than the second.
     * See keyForLabel().
     */
    private static int compareLabels(String label1, String label2) {
        int min = Math.min(label1.length(), label2.length());
        for (int i = 0; i < min; i++) {
            char c1 = label1.charAt(i);
            char c2 = label2.charAt(i);
            if (c1 >= 'A' && c1 <= 'Z') {
                c1 += 'a' - 'A';                        // to lower case
            }
            if (c2 >= 'A' && c2 <= 'Z') {
                c2 += 'a' - 'A';                        // to lower case
            }
            if (c1 != c2) {
                return (c1 - c2);
            }
        }
        return (label1.length() - label2.length());     // the longer one wins
    }

    /*
     * Returns a key suitable for hashing a label.  Two labels map to
     * the same key iff they are equal, taking possible case-folding
     * into account.  See compareLabels().
     */
    private static String keyForLabel(String label) {
        StringBuilder sb = new StringBuilder(label.length());
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                c += 'a' - 'A';                         // to lower case
            }
            sb.append(c);
        }
        return sb.toString();
    }


    /**
     * Serializes only the domain name string, for compactness and to avoid
     * any implementation dependency.
     *
     * @serialData      The domain name string.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        s.writeObject(toString());
    }

    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        try {
            parse((String) s.readObject());
        } catch (InvalidNameException e) {
            // shouldn't happen
            throw new java.io.StreamCorruptedException(
                    "Invalid name: " + domain);
        }
    }

    private static final long serialVersionUID = 7040187611324710271L;
}
