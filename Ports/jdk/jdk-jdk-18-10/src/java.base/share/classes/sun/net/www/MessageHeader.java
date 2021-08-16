/*
 * Copyright (c) 1995, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*-
 *      news stream opener
 */

package sun.net.www;

import java.io.*;
import java.util.Collections;
import java.util.*;

/** An RFC 844 or MIME message header.  Includes methods
    for parsing headers from incoming streams, fetching
    values, setting values, and printing headers.
    Key values of null are legal: they indicate lines in
    the header that don't have a valid key, but do have
    a value (this isn't legal according to the standard,
    but lines like this are everywhere). */
public
class MessageHeader {
    private String keys[];
    private String values[];
    private int nkeys;

    public MessageHeader () {
        grow();
    }

    public MessageHeader (InputStream is) throws java.io.IOException {
        parseHeader(is);
    }

    /**
     * Returns list of header names in a comma separated list
     */
    public synchronized String getHeaderNamesInList() {
        StringJoiner joiner = new StringJoiner(",");
        for (int i=0; i<nkeys; i++) {
            joiner.add(keys[i]);
        }
        return joiner.toString();
    }

    /**
     * Reset a message header (all key/values removed)
     */
    public synchronized void reset() {
        keys = null;
        values = null;
        nkeys = 0;
        grow();
    }

    /**
     * Find the value that corresponds to this key.
     * It finds only the first occurrence of the key.
     * @param k the key to find.
     * @return null if not found.
     */
    public synchronized String findValue(String k) {
        if (k == null) {
            for (int i = nkeys; --i >= 0;)
                if (keys[i] == null)
                    return values[i];
        } else
            for (int i = nkeys; --i >= 0;) {
                if (k.equalsIgnoreCase(keys[i]))
                    return values[i];
            }
        return null;
    }

    // return the location of the key
    public synchronized int getKey(String k) {
        for (int i = nkeys; --i >= 0;)
            if ((keys[i] == k) ||
                (k != null && k.equalsIgnoreCase(keys[i])))
                return i;
        return -1;
    }

    public synchronized String getKey(int n) {
        if (n < 0 || n >= nkeys) return null;
        return keys[n];
    }

    public synchronized String getValue(int n) {
        if (n < 0 || n >= nkeys) return null;
        return values[n];
    }

    /** Deprecated: Use multiValueIterator() instead.
     *
     *  Find the next value that corresponds to this key.
     *  It finds the first value that follows v. To iterate
     *  over all the values of a key use:
     *  <pre>
     *          for(String v=h.findValue(k); v!=null; v=h.findNextValue(k, v)) {
     *              ...
     *          }
     *  </pre>
     */
    public synchronized String findNextValue(String k, String v) {
        boolean foundV = false;
        if (k == null) {
            for (int i = nkeys; --i >= 0;)
                if (keys[i] == null)
                    if (foundV)
                        return values[i];
                    else if (values[i] == v)
                        foundV = true;
        } else
            for (int i = nkeys; --i >= 0;)
                if (k.equalsIgnoreCase(keys[i]))
                    if (foundV)
                        return values[i];
                    else if (values[i] == v)
                        foundV = true;
        return null;
    }

    /**
     * Removes bare Negotiate and Kerberos headers when an "NTLM ..."
     * appears. All Performed on headers with key being k.
     * @return true if there is a change
     */
    public boolean filterNTLMResponses(String k) {
        boolean found = false;
        for (int i=0; i<nkeys; i++) {
            if (k.equalsIgnoreCase(keys[i])
                    && values[i] != null && values[i].length() > 5
                    && values[i].substring(0, 5).equalsIgnoreCase("NTLM ")) {
                found = true;
                break;
            }
        }
        if (found) {
            int j = 0;
            for (int i=0; i<nkeys; i++) {
                if (k.equalsIgnoreCase(keys[i]) && (
                        "Negotiate".equalsIgnoreCase(values[i]) ||
                        "Kerberos".equalsIgnoreCase(values[i]))) {
                    continue;
                }
                if (i != j) {
                    keys[j] = keys[i];
                    values[j] = values[i];
                }
                j++;
            }
            if (j != nkeys) {
                nkeys = j;
                return true;
            }
        }
        return false;
    }

    class HeaderIterator implements Iterator<String> {
        int index = 0;
        int next = -1;
        String key;
        boolean haveNext = false;
        Object lock;

        public HeaderIterator (String k, Object lock) {
            key = k;
            this.lock = lock;
        }
        public boolean hasNext () {
            synchronized (lock) {
                if (haveNext) {
                    return true;
                }
                while (index < nkeys) {
                    if (key.equalsIgnoreCase (keys[index])) {
                        haveNext = true;
                        next = index++;
                        return true;
                    }
                    index ++;
                }
                return false;
            }
        }
        public String next() {
            synchronized (lock) {
                if (haveNext) {
                    haveNext = false;
                    return values [next];
                }
                if (hasNext()) {
                    return next();
                } else {
                    throw new NoSuchElementException ("No more elements");
                }
            }
        }
        public void remove () {
            throw new UnsupportedOperationException ("remove not allowed");
        }
    }

    /**
     * return an Iterator that returns all values of a particular
     * key in sequence
     */
    public Iterator<String> multiValueIterator (String k) {
        return new HeaderIterator (k, this);
    }

    public synchronized Map<String, List<String>> getHeaders() {
        return getHeaders(null);
    }

    public synchronized Map<String, List<String>> getHeaders(String[] excludeList) {
        return filterAndAddHeaders(excludeList, null);
    }

    public synchronized Map<String, List<String>> filterAndAddHeaders(
            String[] excludeList, Map<String, List<String>>  include) {
        boolean skipIt = false;
        Map<String, List<String>> m = new HashMap<>();
        for (int i = nkeys; --i >= 0;) {
            if (excludeList != null) {
                // check if the key is in the excludeList.
                // if so, don't include it in the Map.
                for (int j = 0; j < excludeList.length; j++) {
                    if ((excludeList[j] != null) &&
                        (excludeList[j].equalsIgnoreCase(keys[i]))) {
                        skipIt = true;
                        break;
                    }
                }
            }
            if (!skipIt) {
                List<String> l = m.get(keys[i]);
                if (l == null) {
                    l = new ArrayList<>();
                    m.put(keys[i], l);
                }
                l.add(values[i]);
            } else {
                // reset the flag
                skipIt = false;
            }
        }

        if (include != null) {
                for (Map.Entry<String,List<String>> entry: include.entrySet()) {
                List<String> l = m.get(entry.getKey());
                if (l == null) {
                    l = new ArrayList<>();
                    m.put(entry.getKey(), l);
                }
                l.addAll(entry.getValue());
            }
        }

        for (String key : m.keySet()) {
            m.put(key, Collections.unmodifiableList(m.get(key)));
        }

        return Collections.unmodifiableMap(m);
    }

    /** Check if a line of message header looks like a request line.
     * This method does not perform a full validation but simply
     * returns false if the line does not end with 'HTTP/[1-9].[0-9]'
     * @param line the line to check.
     * @return true if the line might be a request line.
     */
    private static boolean isRequestline(String line) {
        String k = line.trim();
        int i = k.lastIndexOf(' ');
        if (i <= 0) return false;
        int len = k.length();
        if (len - i < 9) return false;

        char c1 = k.charAt(len-3);
        char c2 = k.charAt(len-2);
        char c3 = k.charAt(len-1);
        if (c1 < '1' || c1 > '9') return false;
        if (c2 != '.') return false;
        if (c3 < '0' || c3 > '9') return false;

        return (k.substring(i+1, len-3).equalsIgnoreCase("HTTP/"));
    }

    /** Prints the key-value pairs represented by this
        header. Also prints the RFC required blank line
        at the end. Omits pairs with a null key. Omits
        colon if key-value pair is the requestline. */
    public void print(PrintStream p) {
        // no synchronization: use cloned arrays instead.
        String[] k; String[] v; int n;
        synchronized (this) { n = nkeys; k = keys.clone(); v = values.clone(); }
        print(n, k, v, p);
    }


    /** Prints the key-value pairs represented by this
        header. Also prints the RFC required blank line
        at the end. Omits pairs with a null key. Omits
        colon if key-value pair is the requestline. */
    private  static void print(int nkeys, String[] keys, String[] values, PrintStream p) {
        for (int i = 0; i < nkeys; i++)
            if (keys[i] != null) {
                StringBuilder sb = new StringBuilder(keys[i]);
                if (values[i] != null) {
                    sb.append(": " + values[i]);
                } else if (i != 0 || !isRequestline(keys[i])) {
                    sb.append(":");
                }
                p.print(sb.append("\r\n"));
            }
        p.print("\r\n");
        p.flush();
    }

    /** Adds a key value pair to the end of the
        header.  Duplicates are allowed */
    public synchronized void add(String k, String v) {
        grow();
        keys[nkeys] = k;
        values[nkeys] = v;
        nkeys++;
    }

    /** Prepends a key value pair to the beginning of the
        header.  Duplicates are allowed */
    public synchronized void prepend(String k, String v) {
        grow();
        for (int i = nkeys; i > 0; i--) {
            keys[i] = keys[i-1];
            values[i] = values[i-1];
        }
        keys[0] = k;
        values[0] = v;
        nkeys++;
    }

    /** Overwrite the previous key/val pair at location 'i'
     * with the new k/v.  If the index didn't exist before
     * the key/val is simply tacked onto the end.
     */

    public synchronized void set(int i, String k, String v) {
        grow();
        if (i < 0) {
            return;
        } else if (i >= nkeys) {
            add(k, v);
        } else {
            keys[i] = k;
            values[i] = v;
        }
    }


    /** grow the key/value arrays as needed */

    private void grow() {
        if (keys == null || nkeys >= keys.length) {
            String[] nk = new String[nkeys + 4];
            String[] nv = new String[nkeys + 4];
            if (keys != null)
                System.arraycopy(keys, 0, nk, 0, nkeys);
            if (values != null)
                System.arraycopy(values, 0, nv, 0, nkeys);
            keys = nk;
            values = nv;
        }
    }

    /**
     * Remove the key from the header. If there are multiple values under
     * the same key, they are all removed.
     * Nothing is done if the key doesn't exist.
     * After a remove, the other pairs' order are not changed.
     * @param k the key to remove
     */
    public synchronized void remove(String k) {
        if(k == null) {
            for (int i = 0; i < nkeys; i++) {
                while (keys[i] == null && i < nkeys) {
                    for(int j=i; j<nkeys-1; j++) {
                        keys[j] = keys[j+1];
                        values[j] = values[j+1];
                    }
                    nkeys--;
                }
            }
        } else {
            for (int i = 0; i < nkeys; i++) {
                while (k.equalsIgnoreCase(keys[i]) && i < nkeys) {
                    for(int j=i; j<nkeys-1; j++) {
                        keys[j] = keys[j+1];
                        values[j] = values[j+1];
                    }
                    nkeys--;
                }
            }
        }
    }

    /** Sets the value of a key.  If the key already
        exists in the header, it's value will be
        changed.  Otherwise a new key/value pair will
        be added to the end of the header. */
    public synchronized void set(String k, String v) {
        for (int i = nkeys; --i >= 0;)
            if (k.equalsIgnoreCase(keys[i])) {
                values[i] = v;
                return;
            }
        add(k, v);
    }

    /** Set's the value of a key only if there is no
     *  key with that value already.
     */

    public synchronized void setIfNotSet(String k, String v) {
        if (findValue(k) == null) {
            add(k, v);
        }
    }

    /** Convert a message-id string to canonical form (strips off
        leading and trailing {@literal <>s}) */
    public static String canonicalID(String id) {
        if (id == null)
            return "";
        int st = 0;
        int len = id.length();
        boolean substr = false;
        int c;
        while (st < len && ((c = id.charAt(st)) == '<' ||
                            c <= ' ')) {
            st++;
            substr = true;
        }
        while (st < len && ((c = id.charAt(len - 1)) == '>' ||
                            c <= ' ')) {
            len--;
            substr = true;
        }
        return substr ? id.substring(st, len) : id;
    }

    /** Parse a MIME header from an input stream. */
    public void parseHeader(InputStream is) throws java.io.IOException {
        synchronized (this) {
            nkeys = 0;
        }
        mergeHeader(is);
    }

    /** Parse and merge a MIME header from an input stream. */
    @SuppressWarnings("fallthrough")
    public void mergeHeader(InputStream is) throws java.io.IOException {
        if (is == null)
            return;
        char s[] = new char[10];
        int firstc = is.read();
        while (firstc != '\n' && firstc != '\r' && firstc >= 0) {
            int len = 0;
            int keyend = -1;
            int c;
            boolean inKey = firstc > ' ';
            s[len++] = (char) firstc;
    parseloop:{
                while ((c = is.read()) >= 0) {
                    switch (c) {
                      case ':':
                        if (inKey && len > 0)
                            keyend = len;
                        inKey = false;
                        break;
                      case '\t':
                        c = ' ';
                      /*fall through*/
                      case ' ':
                        inKey = false;
                        break;
                      case '\r':
                      case '\n':
                        firstc = is.read();
                        if (c == '\r' && firstc == '\n') {
                            firstc = is.read();
                            if (firstc == '\r')
                                firstc = is.read();
                        }
                        if (firstc == '\n' || firstc == '\r' || firstc > ' ')
                            break parseloop;
                        /* continuation */
                        c = ' ';
                        break;
                    }
                    if (len >= s.length) {
                        char ns[] = new char[s.length * 2];
                        System.arraycopy(s, 0, ns, 0, len);
                        s = ns;
                    }
                    s[len++] = (char) c;
                }
                firstc = -1;
            }
            while (len > 0 && s[len - 1] <= ' ')
                len--;
            String k;
            if (keyend <= 0) {
                k = null;
                keyend = 0;
            } else {
                k = String.copyValueOf(s, 0, keyend);
                if (keyend < len && s[keyend] == ':')
                    keyend++;
                while (keyend < len && s[keyend] <= ' ')
                    keyend++;
            }
            String v;
            if (keyend >= len)
                v = new String();
            else
                v = String.copyValueOf(s, keyend, len - keyend);
            add(k, v);
        }
    }

    public synchronized String toString() {
        String result = super.toString() + nkeys + " pairs: ";
        for (int i = 0; i < keys.length && i < nkeys; i++) {
            result += "{"+keys[i]+": "+values[i]+"}";
        }
        return result;
    }
}
