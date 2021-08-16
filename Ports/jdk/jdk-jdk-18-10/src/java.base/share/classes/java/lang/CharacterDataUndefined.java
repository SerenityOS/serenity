/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/** The CharacterData class encapsulates the large tables found in
    Java.lang.Character. */

class CharacterDataUndefined extends CharacterData {

    int getProperties(int ch) {
        return 0;
    }

    int getType(int ch) {
        return Character.UNASSIGNED;
    }

    boolean isJavaIdentifierStart(int ch) {
        return false;
    }

    boolean isJavaIdentifierPart(int ch) {
        return false;
    }

    boolean isUnicodeIdentifierStart(int ch) {
        return false;
    }

    boolean isUnicodeIdentifierPart(int ch) {
        return false;
    }

    boolean isIdentifierIgnorable(int ch) {
        return false;
    }

    int toLowerCase(int ch) {
        return ch;
    }

    int toUpperCase(int ch) {
        return ch;
    }

    int toTitleCase(int ch) {
        return ch;
    }

    int digit(int ch, int radix) {
        return -1;
    }

    int getNumericValue(int ch) {
        return -1;
    }

    boolean isDigit(int ch) {
        return false;
    }

    boolean isLowerCase(int ch) {
        return false;
    }

    boolean isUpperCase(int ch) {
        return false;
    }

    boolean isWhitespace(int ch) {
        return false;
    }

    byte getDirectionality(int ch) {
        return Character.DIRECTIONALITY_UNDEFINED;
    }

    boolean isMirrored(int ch) {
        return false;
    }

    static final CharacterData instance = new CharacterDataUndefined();
    private CharacterDataUndefined() {};
}
