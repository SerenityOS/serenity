/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 * @author Martin Buchholz
 */

import java.util.*;
import static java.lang.Character.*;

public class DumpCharProperties {
    final static Locale turkish = new Locale("tr");

    static String charProps(int i) {
        String s = new String(new int[]{i},0,1);
        return String.format
            ("%b %b %b %b %b %b %b %b %b %b %b %b %d %d %d %d %d %b %b %d %d %b %d %d",
             isLowerCase(i),
             isUpperCase(i),
             isTitleCase(i),
             isDigit(i),
             isDefined(i),
             isLetter(i),
             isLetterOrDigit(i),
             isJavaIdentifierStart(i),
             isJavaIdentifierPart(i),
             isUnicodeIdentifierStart(i),
             isUnicodeIdentifierPart(i),
             isIdentifierIgnorable(i),
             toLowerCase(i),
             toUpperCase(i),
             toTitleCase(i),
             digit(i, 16),
             getNumericValue(i),
             isSpaceChar(i),
             isWhitespace(i),
             getType(i),
             getDirectionality(i),
             isMirrored(i),
             (int) s.toUpperCase(Locale.GERMAN).charAt(0),
             (int) s.toUpperCase(turkish).charAt(0));
    }

    public static void main(String[] args) throws Throwable {
        for (int i = 0; i < 17*0x10000; i++) {
            System.out.println(charProps(i));
        }
    }
}
