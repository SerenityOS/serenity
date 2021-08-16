/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.util.*;
import java.nio.file.*;
import java.nio.charset.Charset;
import sun.security.action.GetPropertyAction;

/**
 * Utility methods
 */

class Util {
    private Util() { }

    private static final Charset jnuEncoding = Charset.forName(
        GetPropertyAction.privilegedGetProperty("sun.jnu.encoding"));

    /**
     * Returns {@code Charset} corresponding to the sun.jnu.encoding property
     */
    static Charset jnuEncoding() {
        return jnuEncoding;
    }

    /**
     * Encodes the given String into a sequence of bytes using the {@code Charset}
     * specified by the sun.jnu.encoding property.
     */
    static byte[] toBytes(String s) {
        return s.getBytes(jnuEncoding);
    }

    /**
     * Constructs a new String by decoding the specified array of bytes using the
     * {@code Charset} specified by the sun.jnu.encoding property.
     */
    static String toString(byte[] bytes) {
        return new String(bytes, jnuEncoding);
    }


    /**
     * Splits a string around the given character. The array returned by this
     * method contains each substring that is terminated by the character. Use
     * for simple string spilting cases when needing to avoid loading regex.
     */
    static String[] split(String s, char c) {
        int count = 0;
        for (int i=0; i<s.length(); i++) {
            if (s.charAt(i) == c)
                count++;
        }
        String[] result = new String[count+1];
        int n = 0;
        int last = 0;
        for (int i=0; i<s.length(); i++) {
            if (s.charAt(i) == c) {
                result[n++] = s.substring(last, i);
                last = i + 1;
            }
        }
        result[n] = s.substring(last, s.length());
        return result;
    }

    /**
     * Returns a Set containing the given elements.
     */
    @SafeVarargs
    static <E> Set<E> newSet(E... elements) {
        HashSet<E> set = new HashSet<>();
        for (E e: elements) {
            set.add(e);
        }
        return set;
    }

    /**
     * Returns a Set containing all the elements of the given Set plus
     * the given elements.
     */
    @SafeVarargs
    static <E> Set<E> newSet(Set<E> other, E... elements) {
        HashSet<E> set = new HashSet<>(other);
        for (E e: elements) {
            set.add(e);
        }
        return set;
    }

    /**
     * Returns {@code true} if symbolic links should be followed
     */
    static boolean followLinks(LinkOption... options) {
        boolean followLinks = true;
        for (LinkOption option: options) {
            if (option == LinkOption.NOFOLLOW_LINKS) {
                followLinks = false;
            } else if (option == null) {
                throw new NullPointerException();
            } else {
                throw new AssertionError("Should not get here");
            }
        }
        return followLinks;
    }
}
