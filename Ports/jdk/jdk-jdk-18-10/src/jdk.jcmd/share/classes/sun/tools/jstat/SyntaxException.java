/*
 * Copyright (c) 2004, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.StreamTokenizer;
import java.util.Set;
import java.util.Iterator;

/**
 * An exception class for syntax exceptions detected by the options file
 * parser.
 *
 * @author Brian Doherty
 * @since 1.5
 */
@SuppressWarnings("serial") // JDK implementation class
public class SyntaxException extends ParserException {
    private String message;

    public SyntaxException(String message) {
        this.message = message;
    }

    public SyntaxException(int lineno, String expected, String found) {
        message = "Syntax error at line " + lineno
                  + ": Expected " + expected
                  + ", Found " + found;
    }

    public SyntaxException(int lineno, String expected, Token found) {
        message = "Syntax error at line " + lineno
                  + ": Expected " + expected
                  + ", Found " + found.toMessage();
    }

    public SyntaxException(int lineno, Token expected, Token found) {
        message = "Syntax error at line " + lineno
                  + ": Expected " + expected.toMessage()
                  + ", Found " + found.toMessage();
    }

    public SyntaxException(int lineno, Set<String> expected, Token found) {
        StringBuilder msg = new StringBuilder();

        msg.append("Syntax error at line ").append(lineno)
                .append(": Expected one of \'");

        for (String keyWord : expected) {
            msg.append(keyWord).append('|');
        }
        if (!expected.isEmpty()) {
            msg.setLength(msg.length() - 1);
        }

        message = msg.append("\', Found ").append(found.toMessage()).toString();
    }

    public String getMessage() {
        return message;
    }
}

