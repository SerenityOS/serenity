/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package org.openjdk.tests.shapegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.StringReader;

import static java.lang.Character.isLetter;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.isWhitespace;

/**
 * Parse a type template definition string
 *
 *   input     :: classDef
 *   classDef  :: letter [ ( classDef* ) ]
 *
 * @author Robert Field
 */
public class TTParser extends StringReader {

    private Map<Character, TTNode> letterMap = new HashMap<>();
    private char ch;

    private final String def;

    public TTParser(String s) {
        super(s);
        this.def = s;
    }

    private void advance() throws IOException {
        do {
            ch = (char)read();
        } while (isWhitespace(ch));
    }

    public TTNode parse() {
        try {
            advance();
            return classDef();
        } catch (IOException t) {
            throw new RuntimeException(t);
        }
    }

    private TTNode classDef() throws IOException {
        if (!isLetter(ch)) {
            if (ch == (char)-1) {
                throw new IOException("Unexpected end of type template in " + def);
            } else {
                throw new IOException("Unexpected character in type template: " + (Character)ch + " in " + def);
            }
        }
        char nodeCh = ch;
        TTNode node = letterMap.get(nodeCh);
        boolean canBeClass = isUpperCase(nodeCh);
        advance();
        if (node == null) {
            List<TTNode> subtypes = new ArrayList<>();
            if (ch == '(') {
                advance();
                while (ch != ')') {
                    subtypes.add(classDef());
                }
                advance();
            }
            node = new TTNode(subtypes, canBeClass);
            letterMap.put(nodeCh, node);
        }
        return node;
    }
}
