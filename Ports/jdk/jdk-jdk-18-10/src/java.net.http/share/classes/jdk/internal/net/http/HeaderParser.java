/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http;

import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;

/* This is useful for the nightmare of parsing multi-part HTTP/RFC822 headers
 * sensibly:
 * From a String like: 'timeout=15, max=5'
 * create an array of Strings:
 * { {"timeout", "15"},
 *   {"max", "5"}
 * }
 * From one like: 'Basic Realm="FuzzFace" Foo="Biz Bar Baz"'
 * create one like (no quotes in literal):
 * { {"basic", null},
 *   {"realm", "FuzzFace"}
 *   {"foo", "Biz Bar Baz"}
 * }
 * keys are converted to lower case, vals are left as is....
 */
class HeaderParser {

    /* table of key/val pairs */
    String raw;
    String[][] tab;
    int nkeys;
    int asize = 10; // initial size of array is 10

    public HeaderParser(String raw) {
        this.raw = raw;
        tab = new String[asize][2];
        parse();
    }

//    private HeaderParser () { }

//    /**
//     * Creates a new HeaderParser from this, whose keys (and corresponding
//     * values) range from "start" to "end-1"
//     */
//    public HeaderParser subsequence(int start, int end) {
//        if (start == 0 && end == nkeys) {
//            return this;
//        }
//        if (start < 0 || start >= end || end > nkeys) {
//            throw new IllegalArgumentException("invalid start or end");
//        }
//        HeaderParser n = new HeaderParser();
//        n.tab = new String [asize][2];
//        n.asize = asize;
//        System.arraycopy (tab, start, n.tab, 0, (end-start));
//        n.nkeys= (end-start);
//        return n;
//    }

    private void parse() {

        if (raw != null) {
            raw = raw.trim();
            char[] ca = raw.toCharArray();
            int beg = 0, end = 0, i = 0;
            boolean inKey = true;
            boolean inQuote = false;
            int len = ca.length;
            while (end < len) {
                char c = ca[end];
                if ((c == '=') && !inQuote) { // end of a key
                    tab[i][0] = new String(ca, beg, end-beg).toLowerCase(Locale.US);
                    inKey = false;
                    end++;
                    beg = end;
                } else if (c == '\"') {
                    if (inQuote) {
                        tab[i++][1]= new String(ca, beg, end-beg);
                        inQuote=false;
                        do {
                            end++;
                        } while (end < len && (ca[end] == ' ' || ca[end] == ','));
                        inKey=true;
                        beg=end;
                    } else {
                        inQuote=true;
                        end++;
                        beg=end;
                    }
                } else if (c == ' ' || c == ',') { // end key/val, of whatever we're in
                    if (inQuote) {
                        end++;
                        continue;
                    } else if (inKey) {
                        tab[i++][0] = (new String(ca, beg, end-beg)).toLowerCase(Locale.US);
                    } else {
                        tab[i++][1] = (new String(ca, beg, end-beg));
                    }
                    while (end < len && (ca[end] == ' ' || ca[end] == ',')) {
                        end++;
                    }
                    inKey = true;
                    beg = end;
                } else {
                    end++;
                }
                if (i == asize) {
                    asize = asize * 2;
                    String[][] ntab = new String[asize][2];
                    System.arraycopy (tab, 0, ntab, 0, tab.length);
                    tab = ntab;
                }
            }
            // get last key/val, if any
            if (--end > beg) {
                if (!inKey) {
                    if (ca[end] == '\"') {
                        tab[i++][1] = (new String(ca, beg, end-beg));
                    } else {
                        tab[i++][1] = (new String(ca, beg, end-beg+1));
                    }
                } else {
                    tab[i++][0] = (new String(ca, beg, end-beg+1)).toLowerCase(Locale.US);
                }
            } else if (end == beg) {
                if (!inKey) {
                    if (ca[end] == '\"') {
                        tab[i++][1] = String.valueOf(ca[end-1]);
                    } else {
                        tab[i++][1] = String.valueOf(ca[end]);
                    }
                } else {
                    tab[i++][0] = String.valueOf(ca[end]).toLowerCase(Locale.US);
                }
            }
            nkeys=i;
        }
    }

    public String findKey(int i) {
        if (i < 0 || i > asize) {
            return null;
        }
        return tab[i][0];
    }

    public String findValue(int i) {
        if (i < 0 || i > asize) {
            return null;
        }
        return tab[i][1];
    }

    public String findValue(String key) {
        return findValue(key, null);
    }

    public String findValue(String k, String Default) {
        if (k == null) {
            return Default;
        }
        k = k.toLowerCase(Locale.US);
        for (int i = 0; i < asize; ++i) {
            if (tab[i][0] == null) {
                return Default;
            } else if (k.equals(tab[i][0])) {
                return tab[i][1];
            }
        }
        return Default;
    }

    class ParserIterator implements Iterator<String> {
        int index;
        boolean returnsValue; // or key

        ParserIterator (boolean returnValue) {
            returnsValue = returnValue;
        }
        @Override
        public boolean hasNext () {
            return index<nkeys;
        }
        @Override
        public String next () {
            if (index >= nkeys) {
                throw new NoSuchElementException();
            }
            return tab[index++][returnsValue?1:0];
        }
    }

    public Iterator<String> keys () {
        return new ParserIterator (false);
    }

//    public Iterator<String> values () {
//        return new ParserIterator (true);
//    }

    @Override
    public String toString () {
        Iterator<String> k = keys();
        StringBuilder sb = new StringBuilder();
        sb.append("{size=").append(asize).append(" nkeys=").append(nkeys)
                .append(' ');
        for (int i=0; k.hasNext(); i++) {
            String key = k.next();
            String val = findValue (i);
            if (val != null && "".equals (val)) {
                val = null;
            }
            sb.append(" {").append(key).append(val == null ? "" : "," + val)
                    .append('}');
            if (k.hasNext()) {
                sb.append (',');
            }
        }
        sb.append (" }");
        return sb.toString();
    }

//    public int findInt(String k, int Default) {
//        try {
//            return Integer.parseInt(findValue(k, String.valueOf(Default)));
//        } catch (Throwable t) {
//            return Default;
//        }
//    }
}
