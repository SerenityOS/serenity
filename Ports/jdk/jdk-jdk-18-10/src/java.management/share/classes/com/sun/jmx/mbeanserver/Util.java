/*
 * Copyright (c) 2005, 2008, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.mbeanserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class Util {
    public static ObjectName newObjectName(String string) {
        try {
            return new ObjectName(string);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static <K, V> Map<K, V> newMap() {
        return new HashMap<K, V>();
    }

    static <K, V> Map<K, V> newSynchronizedMap() {
        return Collections.synchronizedMap(Util.<K, V>newMap());
    }

    static <K, V> IdentityHashMap<K, V> newIdentityHashMap() {
        return new IdentityHashMap<K, V>();
    }

    static <K, V> Map<K, V> newSynchronizedIdentityHashMap() {
        Map<K, V> map = newIdentityHashMap();
        return Collections.synchronizedMap(map);
    }

    static <K, V> SortedMap<K, V> newSortedMap() {
        return new TreeMap<K, V>();
    }

    static <K, V> SortedMap<K, V> newSortedMap(Comparator<? super K> comp) {
        return new TreeMap<K, V>(comp);
    }

    static <K, V> Map<K, V> newInsertionOrderMap() {
        return new LinkedHashMap<K, V>();
    }

    static <E> Set<E> newSet() {
        return new HashSet<E>();
    }

    static <E> Set<E> newSet(Collection<E> c) {
        return new HashSet<E>(c);
    }

    static <E> List<E> newList() {
        return new ArrayList<E>();
    }

    static <E> List<E> newList(Collection<E> c) {
        return new ArrayList<E>(c);
    }

    /* This method can be used by code that is deliberately violating the
     * allowed checked casts.  Rather than marking the whole method containing
     * the code with @SuppressWarnings, you can use a call to this method for
     * the exact place where you need to escape the constraints.  Typically
     * you will "import static" this method and then write either
     *    X x = cast(y);
     * or, if that doesn't work (e.g. X is a type variable)
     *    Util.<X>cast(y);
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object x) {
        return (T) x;
    }

    /**
     * Computes a descriptor hashcode from its names and values.
     * @param names  the sorted array of descriptor names.
     * @param values the array of descriptor values.
     * @return a hash code value, as described in {@link #hashCode(Descriptor)}
     */
    public static int hashCode(String[] names, Object[] values) {
        int hash = 0;
        for (int i = 0; i < names.length; i++) {
            Object v = values[i];
            int h;
            if (v == null) {
                h = 0;
            } else if (v instanceof Object[]) {
                h = Arrays.deepHashCode((Object[]) v);
            } else if (v.getClass().isArray()) {
                h = Arrays.deepHashCode(new Object[]{v}) - 31;
            // hashcode of a list containing just v is
            // v.hashCode() + 31, see List.hashCode()
            } else {
                h = v.hashCode();
            }
            hash += names[i].toLowerCase().hashCode() ^ h;
        }
        return hash;
    }

    /** Match a part of a string against a shell-style pattern.
        The only pattern characters recognized are <code>?</code>,
        standing for any one character,
        and <code>*</code>, standing for any string of
        characters, including the empty string. For instance,
        {@code wildmatch("sandwich","sa?d*ch",1,4,1,4)} will match
        {@code "and"} against {@code "a?d"}.

        @param str  the string containing the sequence to match.
        @param pat  a string containing a pattern to match the sub string
                    against.
        @param stri   the index in the string at which matching should begin.
        @param strend the index in the string at which the matching should
                      end.
        @param pati   the index in the pattern at which matching should begin.
        @param patend the index in the pattern at which the matching should
                      end.

        @return true if and only if the string matches the pattern.
    */
    /* The algorithm is a classical one.  We advance pointers in
       parallel through str and pat.  If we encounter a star in pat,
       we remember its position and continue advancing.  If at any
       stage we get a mismatch between str and pat, we look to see if
       there is a remembered star.  If not, we fail.  If so, we
       retreat pat to just past that star and str to the position
       after the last one we tried, and we let the match advance
       again.

       Even though there is only one remembered star position, the
       algorithm works when there are several stars in the pattern.
       When we encounter the second star, we forget the first one.
       This is OK, because if we get to the second star in A*B*C
       (where A etc are arbitrary strings), we have already seen AXB.
       We're therefore setting up a match of *C against the remainder
       of the string, which will match if that remainder looks like
       YC, so the whole string looks like AXBYC.
    */
    private static boolean wildmatch(final String str, final String pat,
            int stri, final int strend, int pati, final int patend) {

        // System.out.println("matching "+pat.substring(pati,patend)+
        //        " against "+str.substring(stri, strend));
        int starstri; // index for backtrack if "*" attempt fails
        int starpati; // index for backtrack if "*" attempt fails, +1

        starstri = starpati = -1;

        /* On each pass through this loop, we either advance pati,
           or we backtrack pati and advance starstri.  Since starstri
           is only ever assigned from pati, the loop must terminate.  */
        while (true) {
            if (pati < patend) {
                final char patc = pat.charAt(pati);
                switch (patc) {
                case '?':
                    if (stri == strend)
                        break;
                    stri++;
                    pati++;
                    continue;
                case '*':
                    pati++;
                    starpati = pati;
                    starstri = stri;
                    continue;
                default:
                    if (stri < strend && str.charAt(stri) == patc) {
                        stri++;
                        pati++;
                        continue;
                    }
                    break;
                }
            } else if (stri == strend)
                return true;

            // Mismatched, can we backtrack to a "*"?
            if (starpati < 0 || starstri == strend)
                return false;

            // Retry the match one position later in str
            pati = starpati;
            starstri++;
            stri = starstri;
        }
    }

    /** Match a string against a shell-style pattern.  The only pattern
        characters recognized are <code>?</code>, standing for any one
        character, and <code>*</code>, standing for any string of
        characters, including the empty string.

        @param str the string to match.
        @param pat the pattern to match the string against.

        @return true if and only if the string matches the pattern.
    */
    public static boolean wildmatch(String str, String pat) {
        return wildmatch(str,pat,0,str.length(),0,pat.length());
    }
}
