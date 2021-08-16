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

#include <stdio.h>
#include <string.h>
#include "jni.h"

#ifndef max
#define max(a,b) ( (a>b) ? a : b )
#endif
#ifndef min
#define min(a,b) ( (a<b) ? a : b )
#endif

/*
 * Validates that a URI path component does not contain any illegal characters
 * - ported from src/share/classes/java/net/URI.java
 */

static jlong L_HEX;
static jlong H_HEX;
static jlong L_PATH;
static jlong H_PATH;

/* Compute the low-order mask for the characters in the given string */
static jlong lowMask(char* s) {
    size_t n = strlen(s);
    jlong m = 0;
    size_t i;
    for (i = 0; i < n; i++) {
        int c = (int)s[i];
        if (c < 64)
            m |= ((jlong)1 << c);
    }
    return m;
}

/* Compute the high-order mask for the characters in the given string */
static jlong highMask(char* s) {
    size_t n = strlen(s);
    jlong m = 0;
    size_t i;
    for (i = 0; i < n; i++) {
        int c = (int)s[i];
        if ((c >= 64) && (c < 128))
            m |= ((jlong)1 << (c - 64));
    }
    return m;
}

/*
 * Compute a low-order mask for the characters
 * between first and last, inclusive
 */
static jlong lowMaskRange(char first, char last) {
    jlong m = 0;
    int f = max(min(first, 63), 0);
    int l = max(min(last, 63), 0);
    int i;

    for (i = f; i <= l; i++)  {
        m |= (jlong)1 << i;
    }
    return m;
}

/*
 * Compute a high-order mask for the characters
 * between first and last, inclusive
 */
static jlong highMaskRange(char first, char last) {
    jlong m = 0;
    int f = max(min(first, 127), 64) - 64;
    int l = max(min(last, 127), 64) - 64;
    int i;
    for (i = f; i <= l; i++) {
        m |= (jlong)1 << i;
    }
    return m;
}

/*
 * Tell whether the given character is permitted by the given mask pair
 */
static int match(int c, jlong lowMask, jlong highMask) {
    if (c >= 0 && c < 64)
        if ((((jlong)1 << c) & lowMask) != 0) return 1;
    if (c >= 64 && c < 128)
        if ((((jlong)1 << (c - 64)) & highMask) != 0) return 1;
    return 0;
}

static void initialize() {
    // digit    = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" |
    //            "8" | "9"
    jlong L_DIGIT = lowMaskRange('0', '9');
    jlong H_DIGIT = 0;

    // upalpha  = "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" |
    //            "J" | "K" | "L" | "M" | "N" | "O" | "P" | "Q" | "R" |
    //            "S" | "T" | "U" | "V" | "W" | "X" | "Y" | "Z"
    jlong L_UPALPHA = 0;
    jlong H_UPALPHA = highMaskRange('A', 'Z');

    // lowalpha = "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" |
    //            "j" | "k" | "l" | "m" | "n" | "o" | "p" | "q" | "r" |
    //            "s" | "t" | "u" | "v" | "w" | "x" | "y" | "z"
    jlong L_LOWALPHA = 0;
    jlong H_LOWALPHA = highMaskRange('a', 'z');

    // alpha         = lowalpha | upalpha
    jlong L_ALPHA = L_LOWALPHA | L_UPALPHA;
    jlong H_ALPHA = H_LOWALPHA | H_UPALPHA;

    // alphanum      = alpha | digit
    jlong L_ALPHANUM = L_DIGIT | L_ALPHA;
    jlong H_ALPHANUM = H_DIGIT | H_ALPHA;

    // mark          = "-" | "_" | "." | "!" | "~" | "*" | "'" |
    //                 "(" | ")"
    jlong L_MARK = lowMask("-_.!~*'()");
    jlong H_MARK = highMask("-_.!~*'()");

    // unreserved    = alphanum | mark
    jlong L_UNRESERVED = L_ALPHANUM | L_MARK;
    jlong H_UNRESERVED = H_ALPHANUM | H_MARK;

    // pchar         = unreserved |
    //                 ":" | "@" | "&" | "=" | "+" | "$" | ","
    jlong L_PCHAR = L_UNRESERVED | lowMask(":@&=+$,");
    jlong H_PCHAR = H_UNRESERVED | highMask(":@&=+$,");

    // hex           = digit | "A" | "B" | "C" | "D" | "E" | "F" |
    //                         "a" | "b" | "c" | "d" | "e" | "f"
    L_HEX = L_DIGIT;
    H_HEX = highMaskRange('A', 'F') | highMaskRange('a', 'f');

    // All valid path characters
    L_PATH = L_PCHAR | lowMask(";/");
    H_PATH = H_PCHAR | highMask(";/");
}


/*
 * Validates that the given URI path component does not contain any
 * illegal characters. Returns 0 if only validate characters are present.
 */
int validatePathChars(const char* path) {
    size_t i, n;

    /* initialize on first usage */
    if (L_HEX == 0) {
        initialize();
    }

    i=0;
    n = strlen(path);
    while (i < n) {
        int c = (int)(signed char)path[i];

        /* definitely not us-ascii */
        if (c < 0) return -1;

        /* start of an escapted character */
        if (c == '%') {
            if (i + 3 <= n) {
                int h1 = (int)(signed char)path[i+1];
                int h2 = (int)(signed char)path[i+2];
                if (h1 < 0 || h2 < 0) return -1;
                if (!match(h1, L_HEX, H_HEX)) return -1;
                if (!match(h2, L_HEX, H_HEX)) return -1;
                i += 3;
            } else {
               /* malformed escape pair */
               return -1;
            }
        } else {
            if (!match(c, L_PATH, H_PATH)) return -1;
            i++;
        }
    }

    return 0;
}
