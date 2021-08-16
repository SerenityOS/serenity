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

package sun.jvmstat.perfdata.monitor;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Class for parsing alias files. File format is expected to follow
 * the following syntax:
 *
 *     alias name [alias]*
 *
 * Java style comments can occur anywhere within the file.
 * @author Brian Doherty
 * @since 1.5
 */
public class AliasFileParser {
    private static final String ALIAS = "alias";
    // 8028357 removed old, inefficient debug logging

    // other variables
    private URL inputfile;
    private StreamTokenizer st;
    private Token currentToken;

    AliasFileParser(URL inputfile) {
        this.inputfile = inputfile;
    }

    // value class to hold StreamTokenizer token values
    private class Token {
        public String sval;
        public int ttype;

        public Token(int ttype, String sval) {
            this.ttype = ttype;
            this.sval = sval;
        }
    }

    /**
     * method to get the next token as a Token type
     */
    private void nextToken() throws IOException {
        st.nextToken();
        currentToken = new Token(st.ttype, st.sval);
    }

    /**
     * method to match the current Token to a specified token type and
     * value Throws a SyntaxException if token doesn't match.
     */
    private void match(int ttype, String token)
                 throws IOException, SyntaxException {

        if ((currentToken.ttype == ttype)
                && (currentToken.sval.compareTo(token) == 0)) {
            nextToken();
        } else {
            throw new SyntaxException(st.lineno());
        }
    }


    /*
     * method to match the current Token to a specified token type.
     * Throws a SyntaxException if token doesn't match.
     */
    private void match(int ttype) throws IOException, SyntaxException {
        if (currentToken.ttype == ttype) {
            nextToken();
        } else {
            throw new SyntaxException(st.lineno());
        }
    }

    private void match(String token) throws IOException, SyntaxException {
        match(StreamTokenizer.TT_WORD, token);
    }

    /**
     * method to parse the given input file.
     */
    public void parse(Map<String, ArrayList<String>> map) throws SyntaxException, IOException {

        if (inputfile == null) {
            return;
        }

        BufferedReader r = new BufferedReader(
                new InputStreamReader(inputfile.openStream()));
        st = new StreamTokenizer(r);

        // allow both forms of commenting styles
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.wordChars('_','_');

        nextToken();

        while (currentToken.ttype != StreamTokenizer.TT_EOF) {
            // look for the start symbol
            if ((currentToken.ttype != StreamTokenizer.TT_WORD)
                    || (currentToken.sval.compareTo(ALIAS) != 0)) {
                nextToken();
                continue;
            }

            match(ALIAS);
            String name = currentToken.sval;
            match(StreamTokenizer.TT_WORD);

            ArrayList<String> aliases = new ArrayList<String>();

            do {
                aliases.add(currentToken.sval);
                match(StreamTokenizer.TT_WORD);

            } while ((currentToken.ttype != StreamTokenizer.TT_EOF)
                     && (currentToken.sval.compareTo(ALIAS) != 0));

            map.put(name, aliases);
        }
    }
}
