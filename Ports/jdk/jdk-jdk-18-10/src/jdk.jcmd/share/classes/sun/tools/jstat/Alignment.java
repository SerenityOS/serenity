/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jstat;

import java.util.*;

/**
 * A typesafe enumeration for describing data alignment semantics
 *
 * @author Brian Doherty
 * @since 1.5
 */
public abstract class Alignment {

    private static int nextOrdinal = 0;
    private static HashMap<String, Alignment> map = new HashMap<String, Alignment>();
    private static final String blanks = "                                                                                                                                                               ";
    private final String name;
    private final int value = nextOrdinal++;

    protected abstract String align(String s, int width);

    /**
     * Alignment representing a Centered alignment
     */
    public static final Alignment CENTER = new Alignment("center") {
        protected String align(String s, int width) {
            int length = s.length();
            if (length >= width) {
                return s;
            }

            int pad = width - length;
            int pad2 = pad / 2;
            int padr = pad % 2;
            if (pad2 == 0) {
              // only 0 or 1 character to pad
              return s + blanks.substring(0, padr);
            } else {
              // pad on both sides
              return  blanks.substring(0, pad2) + s +
                      blanks.substring(0, pad2 + padr);
            }
        }
    };

    /**
     * Alignment representing a Left alignment
     */
    public static final Alignment LEFT = new Alignment("left") {
        protected String align(String s, int width) {
            int length = s.length();
            if (length >= width) {
                return s;
            }
            int pad = width - length;
            return s+blanks.substring(0, pad);
        }
    };

    /**
     * Alignment representing a Right alignment
     */
    public static final Alignment RIGHT = new Alignment("right") {
        protected String align(String s, int width) {
            int length = s.length();
            if (length >= width) {
                return s;
            }
            int pad = width - length;
            return blanks.substring(0, pad) + s;
        }
    };

    /**
     * Maps a string value to its corresponding Alignment object.
     *
     * @param   s  an string to match against Alignment objects.
     * @return     The Alignment object matching the given string.
     */
    public static Alignment toAlignment(String s) {
        return map.get(s);
    }

    /**
     * Returns an enumeration of the keys for this enumerated type
     *
     * @return     Set of Key Words for this enumeration.
     */
    public static Set<String> keySet() {
        return map.keySet();
    }

    public String toString() {
        return name;
    }

    private Alignment(String name) {
        this.name = name;
        map.put(name, this);
    }
}
