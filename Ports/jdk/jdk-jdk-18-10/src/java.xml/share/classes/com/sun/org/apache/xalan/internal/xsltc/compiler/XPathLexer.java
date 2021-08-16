/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 *
 */
package com.sun.org.apache.xalan.internal.xsltc.compiler;
import com.sun.java_cup.internal.runtime.Symbol;


/**
 * @LastModified: Oct 2017
 */
class XPathLexer implements com.sun.java_cup.internal.runtime.Scanner {
        private final int YY_BUFFER_SIZE = 512;
        private final int YY_F = -1;
        private final int YY_NO_STATE = -1;
        private final int YY_NOT_ACCEPT = 0;
        private final int YY_START = 1;
        private final int YY_END = 2;
        private final int YY_NO_ANCHOR = 4;
        private final int YY_BOL = 65536;
        private final int YY_EOF = 65537;
        public final int YYEOF = -1;

        int last, beforeLast;
        void initialize() {
            last = beforeLast = -1;
        }
        static boolean isWhitespace(int c) {
            return (c == ' ' || c == '\t' || c == '\r' || c == '\n'  || c == '\f');
        }
        /**
         * If symbol is not followed by '::' or '(', then treat it as a
         * name instead of an axis or function (Jira-1912).
         */
        Symbol disambiguateAxisOrFunction(int ss) throws Exception {
            // Peek in the input buffer without changing the internal state
            int index = yy_buffer_index;
            // Skip whitespace
            while (index < yy_buffer_read && isWhitespace(yy_buffer[index])) {
                index++;
            }
            // If end of buffer, can't disambiguate :(
            if (index >= yy_buffer_read) {
                // Can't disambiguate, so return as symbol
                return new Symbol(ss);
            }
            // Return symbol if next token is '::' or '('
            return (yy_buffer[index] == ':' && yy_buffer[index+1] == ':' ||
                    yy_buffer[index] == '(') ?
                    newSymbol(ss) : newSymbol(sym.QNAME, yytext());
        }
        /**
         * If symbol is first token or if it follows any of the operators
         * listed in http://www.w3.org/TR/xpath#exprlex then treat as a
         * name instead of a keyword (Jira-1912). Look two tokens behind
         * to desambiguate expressions like "* and *" or "and * and".
         */
        @SuppressWarnings("fallthrough")
        Symbol disambiguateOperator(int ss) throws Exception {
            switch (last) {
            case sym.STAR:
                if (beforeLast != sym.QNAME) break;
            case -1:    // first token
            case sym.ATSIGN:
            case sym.DCOLON:
            case sym.LPAREN:
            case sym.LBRACK:
            case sym.COMMA:
            case sym.AND:
            case sym.OR:
            case sym.MOD:
            case sym.DIV:
            case sym.SLASH:
            case sym.DSLASH:
            case sym.VBAR:
            case sym.PLUS:
            case sym.MINUS:
            case sym.EQ:
            case sym.NE:
            case sym.LT:
            case sym.LE:
            case sym.GT:
            case sym.GE:
                return newSymbol(sym.QNAME, yytext());
            }
            return newSymbol(ss);
        }
        Symbol newSymbol(int ss) {
            beforeLast = last;
            last = ss;
            return new Symbol(ss);
        }
        Symbol newSymbol(int ss, String value) {
            beforeLast = last;
            last = ss;
            return new Symbol(ss, value);
        }
        Symbol newSymbol(int ss, Long value) {
            beforeLast = last;
            last = ss;
            return new Symbol(ss, value);
        }
        Symbol newSymbol(int ss, Double value) {
            beforeLast = last;
            last = ss;
            return new Symbol(ss, value);
        }
        private java.io.BufferedReader yy_reader;
        private int yy_buffer_index;
        private int yy_buffer_read;
        private int yy_buffer_start;
        private int yy_buffer_end;
        private char yy_buffer[];
        private boolean yy_at_bol;
        private int yy_lexical_state;

        XPathLexer (java.io.Reader reader) {
                this ();
                if (null == reader) {
                        throw (new Error("Error: Bad input stream initializer."));
                }
                yy_reader = new java.io.BufferedReader(reader);
        }

        XPathLexer (java.io.InputStream instream) {
                this ();
                if (null == instream) {
                        throw (new Error("Error: Bad input stream initializer."));
                }
                yy_reader = new java.io.BufferedReader(new java.io.InputStreamReader(instream));
        }

        private XPathLexer () {
                yy_buffer = new char[YY_BUFFER_SIZE];
                yy_buffer_read = 0;
                yy_buffer_index = 0;
                yy_buffer_start = 0;
                yy_buffer_end = 0;
                yy_at_bol = true;
                yy_lexical_state = YYINITIAL;
        }

        private boolean yy_eof_done = false;
        private final int YYINITIAL = 0;
        private final int yy_state_dtrans[] = {
                0
        };
        private void yybegin (int state) {
                yy_lexical_state = state;
        }
        private int yy_advance ()
                throws java.io.IOException {
                int next_read;
                int i;
                int j;

                if (yy_buffer_index < yy_buffer_read) {
                        return yy_buffer[yy_buffer_index++];
                }

                if (0 != yy_buffer_start) {
                        i = yy_buffer_start;
                        j = 0;
                        while (i < yy_buffer_read) {
                                yy_buffer[j] = yy_buffer[i];
                                ++i;
                                ++j;
                        }
                        yy_buffer_end = yy_buffer_end - yy_buffer_start;
                        yy_buffer_start = 0;
                        yy_buffer_read = j;
                        yy_buffer_index = j;
                        next_read = yy_reader.read(yy_buffer,
                                        yy_buffer_read,
                                        yy_buffer.length - yy_buffer_read);
                        if (-1 == next_read) {
                                return YY_EOF;
                        }
                        yy_buffer_read = yy_buffer_read + next_read;
                }

                while (yy_buffer_index >= yy_buffer_read) {
                        if (yy_buffer_index >= yy_buffer.length) {
                                yy_buffer = yy_double(yy_buffer);
                        }
                        next_read = yy_reader.read(yy_buffer,
                                        yy_buffer_read,
                                        yy_buffer.length - yy_buffer_read);
                        if (-1 == next_read) {
                                return YY_EOF;
                        }
                        yy_buffer_read = yy_buffer_read + next_read;
                }
                return yy_buffer[yy_buffer_index++];
        }
        private void yy_move_end () {
                if (yy_buffer_end > yy_buffer_start &&
                    '\n' == yy_buffer[yy_buffer_end-1])
                        yy_buffer_end--;
                if (yy_buffer_end > yy_buffer_start &&
                    '\r' == yy_buffer[yy_buffer_end-1])
                        yy_buffer_end--;
        }
        private boolean yy_last_was_cr=false;
        private void yy_mark_start () {
                yy_buffer_start = yy_buffer_index;
        }
        private void yy_mark_end () {
                yy_buffer_end = yy_buffer_index;
        }
        private void yy_to_mark () {
                yy_buffer_index = yy_buffer_end;
                yy_at_bol = (yy_buffer_end > yy_buffer_start) &&
                            ('\r' == yy_buffer[yy_buffer_end-1] ||
                             '\n' == yy_buffer[yy_buffer_end-1] ||
                             2028/*LS*/ == yy_buffer[yy_buffer_end-1] ||
                             2029/*PS*/ == yy_buffer[yy_buffer_end-1]);
        }
        private java.lang.String yytext () {
                return (new java.lang.String(yy_buffer,
                        yy_buffer_start,
                        yy_buffer_end - yy_buffer_start));
        }
        private int yylength () {
                return yy_buffer_end - yy_buffer_start;
        }
        private char[] yy_double (char buf[]) {
                int i;
                char newbuf[];
                newbuf = new char[2*buf.length];
                for (i = 0; i < buf.length; ++i) {
                        newbuf[i] = buf[i];
                }
                return newbuf;
        }
        private final int YY_E_INTERNAL = 0;
        private final int YY_E_MATCH = 1;
        private java.lang.String yy_error_string[] = {
                "Error: Internal error.\n",
                "Error: Unmatched input.\n"
        };
        private void yy_error (int code,boolean fatal) {
                java.lang.System.out.print(yy_error_string[code]);
                java.lang.System.out.flush();
                if (fatal) {
                        throw new Error("Fatal Error.\n");
                }
        }
        static private int[][] unpackFromString(int size1, int size2, String st) {
                int colonIndex = -1;
                String lengthString;
                int sequenceLength = 0;
                int sequenceInteger = 0;

                int commaIndex;
                String workString;

                int res[][] = new int[size1][size2];
                for (int i= 0; i < size1; i++) {
                        for (int j= 0; j < size2; j++) {
                                if (sequenceLength != 0) {
                                        res[i][j] = sequenceInteger;
                                        sequenceLength--;
                                        continue;
                                }
                                commaIndex = st.indexOf(',');
                                workString = (commaIndex==-1) ? st :
                                        st.substring(0, commaIndex);
                                st = st.substring(commaIndex+1);
                                colonIndex = workString.indexOf(':');
                                if (colonIndex == -1) {
                                        res[i][j]=Integer.parseInt(workString);
                                        continue;
                                }
                                lengthString =
                                        workString.substring(colonIndex+1);
                                sequenceLength=Integer.parseInt(lengthString);
                                workString=workString.substring(0,colonIndex);
                                sequenceInteger=Integer.parseInt(workString);
                                res[i][j] = sequenceInteger;
                                sequenceLength--;
                        }
                }
                return res;
        }
        private int yy_acpt[] = {
                /* 0 */ YY_NOT_ACCEPT,
                /* 1 */ YY_NO_ANCHOR,
                /* 2 */ YY_NO_ANCHOR,
                /* 3 */ YY_NO_ANCHOR,
                /* 4 */ YY_NO_ANCHOR,
                /* 5 */ YY_NO_ANCHOR,
                /* 6 */ YY_NO_ANCHOR,
                /* 7 */ YY_NO_ANCHOR,
                /* 8 */ YY_NO_ANCHOR,
                /* 9 */ YY_NO_ANCHOR,
                /* 10 */ YY_NO_ANCHOR,
                /* 11 */ YY_NO_ANCHOR,
                /* 12 */ YY_NO_ANCHOR,
                /* 13 */ YY_NO_ANCHOR,
                /* 14 */ YY_NO_ANCHOR,
                /* 15 */ YY_NO_ANCHOR,
                /* 16 */ YY_NO_ANCHOR,
                /* 17 */ YY_NO_ANCHOR,
                /* 18 */ YY_NO_ANCHOR,
                /* 19 */ YY_NO_ANCHOR,
                /* 20 */ YY_NO_ANCHOR,
                /* 21 */ YY_NO_ANCHOR,
                /* 22 */ YY_NO_ANCHOR,
                /* 23 */ YY_NO_ANCHOR,
                /* 24 */ YY_NO_ANCHOR,
                /* 25 */ YY_NO_ANCHOR,
                /* 26 */ YY_NO_ANCHOR,
                /* 27 */ YY_NO_ANCHOR,
                /* 28 */ YY_NO_ANCHOR,
                /* 29 */ YY_NO_ANCHOR,
                /* 30 */ YY_NO_ANCHOR,
                /* 31 */ YY_NO_ANCHOR,
                /* 32 */ YY_NO_ANCHOR,
                /* 33 */ YY_NO_ANCHOR,
                /* 34 */ YY_NO_ANCHOR,
                /* 35 */ YY_NO_ANCHOR,
                /* 36 */ YY_NO_ANCHOR,
                /* 37 */ YY_NO_ANCHOR,
                /* 38 */ YY_NO_ANCHOR,
                /* 39 */ YY_NO_ANCHOR,
                /* 40 */ YY_NO_ANCHOR,
                /* 41 */ YY_NO_ANCHOR,
                /* 42 */ YY_NO_ANCHOR,
                /* 43 */ YY_NO_ANCHOR,
                /* 44 */ YY_NO_ANCHOR,
                /* 45 */ YY_NO_ANCHOR,
                /* 46 */ YY_NO_ANCHOR,
                /* 47 */ YY_NO_ANCHOR,
                /* 48 */ YY_NO_ANCHOR,
                /* 49 */ YY_NO_ANCHOR,
                /* 50 */ YY_NO_ANCHOR,
                /* 51 */ YY_NO_ANCHOR,
                /* 52 */ YY_NO_ANCHOR,
                /* 53 */ YY_NO_ANCHOR,
                /* 54 */ YY_NO_ANCHOR,
                /* 55 */ YY_NO_ANCHOR,
                /* 56 */ YY_NO_ANCHOR,
                /* 57 */ YY_NO_ANCHOR,
                /* 58 */ YY_NO_ANCHOR,
                /* 59 */ YY_NO_ANCHOR,
                /* 60 */ YY_NO_ANCHOR,
                /* 61 */ YY_NO_ANCHOR,
                /* 62 */ YY_NO_ANCHOR,
                /* 63 */ YY_NO_ANCHOR,
                /* 64 */ YY_NOT_ACCEPT,
                /* 65 */ YY_NO_ANCHOR,
                /* 66 */ YY_NO_ANCHOR,
                /* 67 */ YY_NO_ANCHOR,
                /* 68 */ YY_NO_ANCHOR,
                /* 69 */ YY_NOT_ACCEPT,
                /* 70 */ YY_NO_ANCHOR,
                /* 71 */ YY_NO_ANCHOR,
                /* 72 */ YY_NOT_ACCEPT,
                /* 73 */ YY_NO_ANCHOR,
                /* 74 */ YY_NO_ANCHOR,
                /* 75 */ YY_NOT_ACCEPT,
                /* 76 */ YY_NO_ANCHOR,
                /* 77 */ YY_NO_ANCHOR,
                /* 78 */ YY_NOT_ACCEPT,
                /* 79 */ YY_NO_ANCHOR,
                /* 80 */ YY_NOT_ACCEPT,
                /* 81 */ YY_NO_ANCHOR,
                /* 82 */ YY_NOT_ACCEPT,
                /* 83 */ YY_NO_ANCHOR,
                /* 84 */ YY_NOT_ACCEPT,
                /* 85 */ YY_NO_ANCHOR,
                /* 86 */ YY_NOT_ACCEPT,
                /* 87 */ YY_NO_ANCHOR,
                /* 88 */ YY_NOT_ACCEPT,
                /* 89 */ YY_NO_ANCHOR,
                /* 90 */ YY_NOT_ACCEPT,
                /* 91 */ YY_NO_ANCHOR,
                /* 92 */ YY_NOT_ACCEPT,
                /* 93 */ YY_NO_ANCHOR,
                /* 94 */ YY_NOT_ACCEPT,
                /* 95 */ YY_NO_ANCHOR,
                /* 96 */ YY_NOT_ACCEPT,
                /* 97 */ YY_NO_ANCHOR,
                /* 98 */ YY_NOT_ACCEPT,
                /* 99 */ YY_NO_ANCHOR,
                /* 100 */ YY_NOT_ACCEPT,
                /* 101 */ YY_NO_ANCHOR,
                /* 102 */ YY_NOT_ACCEPT,
                /* 103 */ YY_NO_ANCHOR,
                /* 104 */ YY_NOT_ACCEPT,
                /* 105 */ YY_NO_ANCHOR,
                /* 106 */ YY_NOT_ACCEPT,
                /* 107 */ YY_NO_ANCHOR,
                /* 108 */ YY_NOT_ACCEPT,
                /* 109 */ YY_NO_ANCHOR,
                /* 110 */ YY_NOT_ACCEPT,
                /* 111 */ YY_NO_ANCHOR,
                /* 112 */ YY_NOT_ACCEPT,
                /* 113 */ YY_NO_ANCHOR,
                /* 114 */ YY_NOT_ACCEPT,
                /* 115 */ YY_NO_ANCHOR,
                /* 116 */ YY_NOT_ACCEPT,
                /* 117 */ YY_NO_ANCHOR,
                /* 118 */ YY_NOT_ACCEPT,
                /* 119 */ YY_NO_ANCHOR,
                /* 120 */ YY_NOT_ACCEPT,
                /* 121 */ YY_NO_ANCHOR,
                /* 122 */ YY_NOT_ACCEPT,
                /* 123 */ YY_NO_ANCHOR,
                /* 124 */ YY_NOT_ACCEPT,
                /* 125 */ YY_NO_ANCHOR,
                /* 126 */ YY_NOT_ACCEPT,
                /* 127 */ YY_NO_ANCHOR,
                /* 128 */ YY_NO_ANCHOR,
                /* 129 */ YY_NO_ANCHOR,
                /* 130 */ YY_NO_ANCHOR,
                /* 131 */ YY_NO_ANCHOR,
                /* 132 */ YY_NO_ANCHOR,
                /* 133 */ YY_NO_ANCHOR,
                /* 134 */ YY_NO_ANCHOR,
                /* 135 */ YY_NO_ANCHOR,
                /* 136 */ YY_NO_ANCHOR,
                /* 137 */ YY_NO_ANCHOR,
                /* 138 */ YY_NO_ANCHOR,
                /* 139 */ YY_NO_ANCHOR,
                /* 140 */ YY_NO_ANCHOR,
                /* 141 */ YY_NO_ANCHOR,
                /* 142 */ YY_NO_ANCHOR,
                /* 143 */ YY_NO_ANCHOR,
                /* 144 */ YY_NO_ANCHOR,
                /* 145 */ YY_NO_ANCHOR,
                /* 146 */ YY_NO_ANCHOR,
                /* 147 */ YY_NO_ANCHOR,
                /* 148 */ YY_NO_ANCHOR,
                /* 149 */ YY_NO_ANCHOR,
                /* 150 */ YY_NO_ANCHOR,
                /* 151 */ YY_NO_ANCHOR,
                /* 152 */ YY_NO_ANCHOR,
                /* 153 */ YY_NO_ANCHOR,
                /* 154 */ YY_NO_ANCHOR,
                /* 155 */ YY_NO_ANCHOR,
                /* 156 */ YY_NO_ANCHOR,
                /* 157 */ YY_NO_ANCHOR,
                /* 158 */ YY_NO_ANCHOR,
                /* 159 */ YY_NO_ANCHOR,
                /* 160 */ YY_NO_ANCHOR,
                /* 161 */ YY_NO_ANCHOR,
                /* 162 */ YY_NO_ANCHOR,
                /* 163 */ YY_NO_ANCHOR,
                /* 164 */ YY_NO_ANCHOR,
                /* 165 */ YY_NO_ANCHOR,
                /* 166 */ YY_NO_ANCHOR,
                /* 167 */ YY_NO_ANCHOR,
                /* 168 */ YY_NO_ANCHOR,
                /* 169 */ YY_NO_ANCHOR,
                /* 170 */ YY_NO_ANCHOR,
                /* 171 */ YY_NO_ANCHOR,
                /* 172 */ YY_NO_ANCHOR,
                /* 173 */ YY_NO_ANCHOR,
                /* 174 */ YY_NO_ANCHOR,
                /* 175 */ YY_NO_ANCHOR,
                /* 176 */ YY_NO_ANCHOR,
                /* 177 */ YY_NO_ANCHOR,
                /* 178 */ YY_NO_ANCHOR,
                /* 179 */ YY_NO_ANCHOR,
                /* 180 */ YY_NO_ANCHOR,
                /* 181 */ YY_NO_ANCHOR,
                /* 182 */ YY_NO_ANCHOR,
                /* 183 */ YY_NO_ANCHOR,
                /* 184 */ YY_NO_ANCHOR,
                /* 185 */ YY_NOT_ACCEPT,
                /* 186 */ YY_NOT_ACCEPT,
                /* 187 */ YY_NO_ANCHOR,
                /* 188 */ YY_NOT_ACCEPT,
                /* 189 */ YY_NO_ANCHOR,
                /* 190 */ YY_NOT_ACCEPT,
                /* 191 */ YY_NO_ANCHOR,
                /* 192 */ YY_NO_ANCHOR,
                /* 193 */ YY_NO_ANCHOR,
                /* 194 */ YY_NO_ANCHOR,
                /* 195 */ YY_NO_ANCHOR,
                /* 196 */ YY_NO_ANCHOR,
                /* 197 */ YY_NO_ANCHOR,
                /* 198 */ YY_NO_ANCHOR,
                /* 199 */ YY_NO_ANCHOR,
                /* 200 */ YY_NO_ANCHOR,
                /* 201 */ YY_NO_ANCHOR,
                /* 202 */ YY_NO_ANCHOR,
                /* 203 */ YY_NO_ANCHOR,
                /* 204 */ YY_NO_ANCHOR,
                /* 205 */ YY_NO_ANCHOR,
                /* 206 */ YY_NO_ANCHOR,
                /* 207 */ YY_NO_ANCHOR,
                /* 208 */ YY_NO_ANCHOR,
                /* 209 */ YY_NO_ANCHOR,
                /* 210 */ YY_NO_ANCHOR,
                /* 211 */ YY_NO_ANCHOR,
                /* 212 */ YY_NO_ANCHOR,
                /* 213 */ YY_NO_ANCHOR,
                /* 214 */ YY_NO_ANCHOR,
                /* 215 */ YY_NO_ANCHOR,
                /* 216 */ YY_NO_ANCHOR,
                /* 217 */ YY_NO_ANCHOR,
                /* 218 */ YY_NO_ANCHOR,
                /* 219 */ YY_NO_ANCHOR,
                /* 220 */ YY_NO_ANCHOR,
                /* 221 */ YY_NO_ANCHOR,
                /* 222 */ YY_NO_ANCHOR,
                /* 223 */ YY_NO_ANCHOR,
                /* 224 */ YY_NO_ANCHOR,
                /* 225 */ YY_NO_ANCHOR,
                /* 226 */ YY_NO_ANCHOR,
                /* 227 */ YY_NO_ANCHOR,
                /* 228 */ YY_NO_ANCHOR,
                /* 229 */ YY_NO_ANCHOR,
                /* 230 */ YY_NO_ANCHOR,
                /* 231 */ YY_NO_ANCHOR,
                /* 232 */ YY_NO_ANCHOR,
                /* 233 */ YY_NO_ANCHOR
        };
        static private int yy_cmap[] = unpackFromString(1,65538,
"54:9,27:2,54,27:2,54:18,27,17,53,54,15,54:2,55,25,26,1,3,11,4,13,2,56:10,10" +
",54,18,16,19,54,12,44,57:3,46,57:3,51,57:4,48,52,43,57,47,50,45,57:3,49,57:" +
"2,41,54,42,54,58,54,35,38,29,5,21,39,33,36,6,57,20,37,8,28,9,30,57,31,32,23" +
",34,7,40,24,22,57,54,14,54:58,60,54:8,57:23,54,57:31,54,57:58,58:2,57:11,58" +
":2,57:8,58,57:53,58,57:68,58:9,57:36,58:3,57:2,58:4,57:30,58:56,57:89,58:18" +
",57:7,58:62,60:70,54:26,60:2,54:14,58:14,54,58:7,57,58,57:3,58,57,58,57:20," +
"58,57:44,58,57:7,58:3,57,58,57,58,57,58,57,58,57:18,58:13,57:12,58,57:66,58" +
",57:12,58,57:36,58:14,57:53,58:2,57:2,58:2,57:2,58:3,57:28,58:2,57:8,58:2,5" +
"7:2,58:55,57:38,58:2,57,58:7,57:38,58:73,57:27,58:5,57:3,58:46,57:26,58:6,5" +
"7:10,58:21,59:10,58:7,57:71,58:2,57:5,58,57:15,58,57:4,58,57,58:15,57:2,58:" +
"9,59:10,58:523,57:53,58:3,57,58:26,57:10,58:4,59:10,58:21,57:8,58:2,57:2,58" +
":2,57:22,58,57:7,58,57,58:3,57:4,58:34,57:2,58,57:3,58:4,59:10,57:2,58:19,5" +
"7:6,58:4,57:2,58:2,57:22,58,57:7,58,57:2,58,57:2,58,57:2,58:31,57:4,58,57,5" +
"8:7,59:10,58:2,57:3,58:16,57:7,58,57,58,57:3,58,57:22,58,57:7,58,57:2,58,57" +
":5,58:3,57,58:34,57,58:5,59:10,58:21,57:8,58:2,57:2,58:2,57:22,58,57:7,58,5" +
"7:2,58:2,57:4,58:3,57,58:30,57:2,58,57:3,58:4,59:10,58:21,57:6,58:3,57:3,58" +
",57:4,58:3,57:2,58,57,58,57:2,58:3,57:2,58:3,57:3,58:3,57:8,58,57:3,58:45,5" +
"9:9,58:21,57:8,58,57:3,58,57:23,58,57:10,58,57:5,58:38,57:2,58:4,59:10,58:2" +
"1,57:8,58,57:3,58,57:23,58,57:10,58,57:5,58:36,57,58,57:2,58:4,59:10,58:21," +
"57:8,58,57:3,58,57:23,58,57:16,58:38,57:2,58:4,59:10,58:145,57:46,58,57,58," +
"57:2,58:12,57:6,58:10,59:10,58:39,57:2,58,57,58:2,57:2,58,57,58:2,57,58:6,5" +
"7:4,58,57:7,58,57:3,58,57,58,57,58:2,57:2,58,57:2,58,57,58,57:2,58:9,57,58:" +
"2,57:5,58:11,59:10,58:70,59:10,58:22,57:8,58,57:33,58:310,57:38,58:10,57:39" +
",58:9,57,58,57:2,58,57:3,58,57,58,57:2,58,57:5,58:41,57,58,57,58,57,58:11,5" +
"7,58,57,58,57,58:3,57:2,58:3,57,58:5,57:3,58,57,58,57,58,57,58,57,58:3,57:2" +
",58:3,57:2,58,57,58:40,57,58:9,57,58:2,57,58:2,57:2,58:7,57:2,58,57,58,57:7" +
",58:40,57,58:4,57,58:8,57,58:3078,57:156,58:4,57:90,58:6,57:22,58:2,57:6,58" +
":2,57:38,58:2,57:6,58:2,57:8,58,57,58,57,58,57,58,57:31,58:2,57:53,58,57:7," +
"58,57,58:3,57:3,58,57:7,58:3,57:4,58:2,57:6,58:4,57:13,58:5,57:3,58,57:7,58" +
":3,54:12,58:2,54:98,58:182,57,58:3,57:2,58:2,57,58:81,57:3,58:13,54:2672,58" +
":1008,54:17,58:64,57:84,58:12,57:90,58:10,57:40,58:31443,57:11172,58:92,54:" +
"8448,58:1232,54:32,58:526,54:2,0:2")[0];

        static private int yy_rmap[] = unpackFromString(1,234,
"0,1:2,2,1:2,3,4,1,5,6,1:3,7,8,1:5,9,1,10:2,1:3,11,1:5,12,10,1,10:5,1:2,10,1" +
":2,13,1,10,1,14,10,15,16,1:2,10:4,17,1:2,18,19,20,21,22,23,24,25,26,27,1,25" +
",10,28:2,29,5,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,5" +
"0,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,7" +
"5,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,1" +
"00,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118," +
"119,120,121,122,123,124,125,126,127,128,129,130,131,132,10,133,134,135,136," +
"137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155" +
",156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,172,173,17" +
"4,175,176,177,178,179,180,181")[0];

        static private int yy_nxt[][] = unpackFromString(182,61,
"1,2,3,4,5,6,65,184,204,70,7,8,9,10,11,12,13,66,14,15,211,184:2,215,184,16,1" +
"7,18,218,220,221,184,222,184:2,223,184:3,224,184,19,20,184:10,71,74,77,21,1" +
"84:2,67,74,-1:63,22,-1:62,184:2,73,184:3,64,-1:2,76,-1:6,184,79,184:3,-1:3," +
"184:13,-1:2,184:10,-1:3,76,184,76:3,-1:10,25,-1:51,26,-1:72,27,-1:42,28,-1:" +
"2,28,-1:17,30,-1:26,69,-1:2,72,-1:30,31,-1:57,34,-1:42,21,-1:2,21,-1:5,184:" +
"6,64,-1:2,76,-1:6,184:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:56,28,-" +
"1:2,28,-1:57,34,-1:2,34,-1:5,155,184:5,64,-1:2,76,-1:6,184:5,-1:3,184:13,-1" +
":2,184:10,-1:3,76,184,76:3,-1:4,209,184:5,64,-1:2,76,-1:6,184:5,-1:3,184:13" +
",-1:2,184:10,-1:3,76,184,76:3,-1:4,233,184:5,64,-1:2,76,-1:6,184:5,-1:3,184" +
":13,-1:2,184:10,-1:3,76,184,76:3,-1:4,158,184:5,64,-1:2,76,-1:6,184:5,-1:3," +
"184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,122,-1" +
",124,183,184:12,-1:2,184:10,-1:3,76,184,76:3,-1,36,-1:3,103:5,-1:2,80,-1:7," +
"103:5,-1:3,103:13,-1:2,103:10,-1:4,103:3,-1:5,184,23,184:4,64,-1:2,76,-1:6," +
"184:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:16,29,-1:48,184:6,64,-1:2" +
",68,-1:6,184:5,-1:3,184:13,-1:2,184:10,-1:3,67,184,76,67,76,-1:4,184:6,64,-" +
"1:2,76,-1:6,184:5,-1:3,184:13,-1:2,184:10,-1:3,68,184,76,68,76,-1:44,82,-1:" +
"20,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:3,24,184:9,-1:2,184:10,-1:3,76,184," +
"76:3,-1,75:52,32,75:7,-1:49,84,-1:15,184:3,35,184:2,64,-1:2,76,-1:6,184:5,-" +
"1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1,78:54,33,78:5,-1:4,184:6,64,-1:2" +
",76,-1:6,184:5,-1:3,184:4,105,184:8,-1:2,184:10,-1:3,76,184,76:3,-1:4,184,3" +
"7,184:4,64,-1:2,76,-1:6,184:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4" +
"5,185,-1:19,184:6,64,-1:2,76,-1:6,184:2,38,184:2,-1:3,184:13,-1:2,184:10,-1" +
":3,76,184,76:3,-1:43,86,-1:21,184:6,64,-1:2,76,-1:6,184:4,191,-1:3,184:13,-" +
"1:2,184:10,-1:3,76,184,76:3,-1:47,186,-1:17,184,107,184:4,64,-1:2,76,-1:6,1" +
"84:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:46,96,-1:18,184:4,193,184," +
"64,-1:2,76,-1:6,184:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:26,42,-1:" +
"38,184:2,205,184:3,64,-1:2,76,-1:6,184:5,-1:3,184:13,-1:2,184:10,-1:3,76,18" +
"4,76:3,-1:25,100,-1,92,-1:37,184:5,192,64,-1:2,76,-1:6,184,228,184:3,-1:3,1" +
"84:13,-1:2,184:10,-1:3,76,184,76:3,-1:26,43,-1:38,184:6,64,-1:2,76,-1:6,184" +
":5,-1:3,184:3,206,184:9,-1:2,184:10,-1:3,76,184,76:3,-1:47,104,-1:17,184:6," +
"64,-1:2,76,-1:6,184:5,-1:3,184:9,111,184:3,-1:2,184:10,-1:3,76,184,76:3,-1:" +
"50,190,-1:14,184:6,64,-1:2,76,-1:6,184:3,113,184,-1:3,184:13,-1:2,184:10,-1" +
":3,76,184,76:3,-1:26,45,-1:38,184,39,184:4,64,-1:2,76,-1:6,184:5,-1:3,184,2" +
"12,184:11,-1:2,184:10,-1:3,76,184,76:3,-1:26,46,-1:38,103:6,-1:3,103,-1:6,1" +
"03:5,-1:3,103:13,-1:2,103:10,-1:3,103:5,-1:48,106,-1:16,184:6,64,-1:2,76,-1" +
":6,184:5,-1:3,184,216,184:11,-1:2,184:10,-1:3,76,184,76:3,-1:19,48,-1:45,18" +
"4:6,64,-1:2,76,-1:6,184,119,184:3,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3," +
"-1:51,114,-1:13,184:4,123,184,64,-1:2,76,-1:6,184:5,-1:3,184:13,-1:2,184:10" +
",-1:3,76,184,76:3,-1:26,50,-1:38,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:11,40" +
",184,-1:2,184:10,-1:3,76,184,76:3,-1:25,116,-1,112,-1:37,184:6,64,-1:2,76,-" +
"1:6,184:5,-1:3,184:3,128,184:9,-1:2,184:10,-1:3,76,184,76:3,-1:52,118,-1:12" +
",184:6,64,-1:2,76,-1:6,184:5,-1:3,184:9,129,184:3,-1:2,184:10,-1:3,76,184,7" +
"6:3,-1:26,55,-1:38,184:6,64,-1:2,76,-1:6,184:3,130,184,90,-1,92,184:13,-1:2" +
",184:10,-1:3,76,184,76:3,-1:48,120,-1:16,184:6,64,-1:2,76,-1:6,184,131,184:" +
"3,94,-1,188,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:19,56,-1:45,184:6,64,-1:" +
"2,76,-1:6,184:5,-1:3,184:4,132,184:8,-1:2,184:10,-1:3,76,184,76:3,-1:26,62," +
"-1:38,184:6,64,-1:2,76,-1:6,184,208,184:3,-1:3,184:13,-1:2,184:10,-1:3,76,1" +
"84,76:3,-1:25,126,-1,124,-1:37,184,41,184:4,64,-1:2,76,-1:6,184:5,-1:3,184:" +
"13,-1:2,184:10,-1:3,76,184,76:3,-1:26,63,-1:38,184:6,64,-1:2,76,-1:6,184:5," +
"-1:3,135,184:12,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:2,136,184:3,64,-1:2,7" +
"6,-1:6,184:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:5,138,64,-1:" +
"2,76,-1:6,184:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2" +
",76,-1:6,184:3,130,184,-1:2,92,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184" +
":6,64,-1:2,76,-1:6,184,131,184:3,-1:2,188,184:13,-1:2,184:10,-1:3,76,184,76" +
":3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:2,139,184:10,-1:2,184:10,-1:3," +
"76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:4,197,184:8,-1:2,184:" +
"10,-1:3,76,184,76:3,-1:4,184,140,184:4,64,-1:2,76,-1:6,184:5,-1:3,184:13,-1" +
":2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:3,44,184,-1:3,184" +
":13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:" +
"10,141,184:2,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:3," +
"142,184,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6" +
",184:5,-1:3,184:12,225,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-" +
"1:6,184:5,-1:3,184:7,143,184:5,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:2,145," +
"184:3,64,-1:2,76,-1:6,184:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,1" +
"84:6,64,-1:2,76,-1:6,184:5,-1:3,184:6,146,184:6,-1:2,184:10,-1:3,76,184,76:" +
"3,-1:4,184:5,147,64,-1:2,76,-1:6,184:5,-1:3,184:13,-1:2,184:10,-1:3,76,184," +
"76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184,148,184:11,-1:2,184:10,-1:3," +
"76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:3,149,184,110,-1,112,184:13,-1:2" +
",184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,150,184:12,-" +
"1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:3,151,184,-1:3,1" +
"84:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,18" +
"4:3,47,184:9,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184,49" +
",184:3,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6," +
"184:3,149,184,-1:2,112,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1" +
":2,76,-1:6,184:5,-1:3,184:5,51,184:7,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:" +
"6,64,-1:2,76,-1:6,184,52,184:3,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:" +
"4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:5,53,184:7,-1:2,184:10,-1:3,76,184,7" +
"6:3,-1:4,184:6,64,-1:2,76,-1:6,184:3,54,184,-1:3,184:13,-1:2,184:10,-1:3,76" +
",184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:5,156,184:7,-1:2,184:10" +
",-1:3,76,184,76:3,-1:4,184:5,157,64,-1:2,76,-1:6,184:5,-1:3,184:13,-1:2,184" +
":10,-1:3,76,184,76:3,-1:4,159,184:5,64,-1:2,76,-1:6,184:5,-1:3,184:13,-1:2," +
"184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:3,160,184" +
":9,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:5,161,64,-1:2,76,-1:6,184:5,-1:3,1" +
"84:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:2,162,184:3,64,-1:2,76,-1:6,184" +
":5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,213,184:5,64,-1:2,76,-1:6," +
"184:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,1" +
"84:5,-1:3,184:3,226,184:9,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,7" +
"6,-1:6,184:5,-1:3,217,184:12,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:" +
"2,76,-1:6,184:5,-1:3,184:10,164,184:2,-1:2,184:10,-1:3,76,184,76:3,-1:4,184" +
":6,64,-1:2,76,-1:6,184:5,-1:3,184:9,167,184:3,-1:2,184:10,-1:3,76,184,76:3," +
"-1:4,184:6,64,-1:2,76,-1:6,184,168,184:3,-1:3,184:13,-1:2,184:10,-1:3,76,18" +
"4,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:3,170,184,-1:3,184:13,-1:2,184:10,-1:" +
"3,76,184,76:3,-1:4,184:2,171,184:3,64,-1:2,76,-1:6,184:5,-1:3,184:13,-1:2,1" +
"84:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:9,172,184:" +
"3,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184,173,184:3,-1:" +
"3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3" +
",184:3,174,184:9,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,18" +
"4:5,-1:3,175,184:12,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6" +
",184:5,-1:3,184:11,57,184,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,7" +
"6,-1:6,184:5,-1:3,184:9,177,184:3,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,6" +
"4,-1:2,76,-1:6,184:5,-1:3,184:6,178,184:6,-1:2,184:10,-1:3,76,184,76:3,-1:4" +
",184:6,64,-1:2,76,-1:6,184:5,-1:3,184:5,58,184:7,-1:2,184:10,-1:3,76,184,76" +
":3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:5,59,184:7,-1:2,184:10,-1:3,76" +
",184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:11,60,184,-1:2,184:10,-" +
"1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184,179,184:11,-1:2,1" +
"84:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:3,180,184,-1:3,184:13" +
",-1:2,184:10,-1:3,76,184,76:3,-1:4,184:2,181,184:3,64,-1:2,76,-1:6,184:5,-1" +
":3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:5,182,64,-1:2,76,-1:6,184:5" +
",-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5," +
"-1:3,61,184:12,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:" +
"5,-1:2,124,183,184:12,-1:2,184:10,-1:3,76,184,76:3,-1:45,88,-1:61,98,-1:18," +
"184:4,109,184,64,-1:2,76,-1:6,184:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:" +
"3,-1:25,102,-1,188,-1:37,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:9,115,184:3,-" +
"1:2,184:10,-1:3,76,184,76:3,-1:50,108,-1:14,184:6,64,-1:2,76,-1:6,184:3,117" +
",184,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,18" +
"4:5,-1:3,184,195,184:11,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76," +
"-1:6,184,121,184:3,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-" +
"1:2,76,-1:6,184:5,-1:3,184:4,137,184:8,-1:2,184:10,-1:3,76,184,76:3,-1:4,18" +
"4:6,64,-1:2,76,-1:6,184,133,184:3,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3," +
"-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,198,184:12,-1:2,184:10,-1:3,76,184,76" +
":3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:4,229,184:8,-1:2,184:10,-1:3,7" +
"6,184,76:3,-1:4,184,200,184:4,64,-1:2,76,-1:6,184:5,-1:3,184:13,-1:2,184:10" +
",-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:3,144,184,-1:3,184:13,-1:2" +
",184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:7,210,18" +
"4:5,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,152," +
"184:12,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:2,163,184:3,64,-1:2,76,-1:6,18" +
"4:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184" +
":5,-1:3,176,184:12,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:5,81,64,-1:2,76,-1" +
":6,184:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:" +
"6,184:5,-1:3,184:9,125,184:3,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:" +
"2,76,-1:6,184,127,184:3,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6" +
",64,-1:2,76,-1:6,184,134,184:3,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:" +
"4,184:6,64,-1:2,76,-1:6,184:5,-1:3,199,184:12,-1:2,184:10,-1:3,76,184,76:3," +
"-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:4,202,184:8,-1:2,184:10,-1:3,76,1" +
"84,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,153,184:12,-1:2,184:10,-1:3,7" +
"6,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184,83,184:3,-1:3,184:13,-1:2,184:10," +
"-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184,194,184:3,-1:3,184:13,-1:2," +
"184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:4,165,184" +
":8,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,154,1" +
"84:12,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184,85,184:3," +
"-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184,196" +
",184:3,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6," +
"184:5,-1:3,184:4,166,184:8,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:5,87,64,-1" +
":2,76,-1:6,184:5,-1:3,184:7,89,184:5,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:" +
"6,64,-1:2,76,-1:6,184:5,-1:3,184:4,169,184:8,-1:2,184:10,-1:3,76,184,76:3,-" +
"1:4,184:5,187,64,-1:2,76,-1:6,184:5,-1:3,184:8,91,184:4,-1:2,184:10,-1:3,76" +
",184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:3,93,184:3,95,184:5,-1:" +
"2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184,97,184:3,-1:3,184:" +
"13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:3,99,184,-1:" +
"3,101,184:12,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:5,189,64,-1:2,76,-1:6,18" +
"4:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:2,201,184:3,64,-1:2,7" +
"6,-1:6,184:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,219,184:5,64,-1:" +
"2,76,-1:6,184:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:2,203,184" +
":3,64,-1:2,76,-1:6,184:5,-1:3,184:13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:" +
"6,64,-1:2,76,-1:6,184:5,-1:3,184,207,184:11,-1:2,184:10,-1:3,76,184,76:3,-1" +
":4,184:2,214,184:3,64,-1:2,76,-1:6,184:5,-1:3,184:13,-1:2,184:10,-1:3,76,18" +
"4,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:9,227,184:3,-1:2,184:10,-1" +
":3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:10,230,184:2,-1:2," +
"184:10,-1:3,76,184,76:3,-1:4,184:2,231,184:3,64,-1:2,76,-1:6,184:5,-1:3,184" +
":13,-1:2,184:10,-1:3,76,184,76:3,-1:4,184:6,64,-1:2,76,-1:6,184:5,-1:3,184:" +
"4,232,184:8,-1:2,184:10,-1:3,76,184,76:3");

        @SuppressWarnings("fallthrough") // at case 18 and -1
        public com.sun.java_cup.internal.runtime.Symbol next_token ()
                throws java.io.IOException,
Exception

                {
                int yy_lookahead;
                int yy_anchor = YY_NO_ANCHOR;
                int yy_state = yy_state_dtrans[yy_lexical_state];
                int yy_next_state = YY_NO_STATE;
                int yy_last_accept_state = YY_NO_STATE;
                boolean yy_initial = true;
                int yy_this_accept;

                yy_mark_start();
                yy_this_accept = yy_acpt[yy_state];
                if (YY_NOT_ACCEPT != yy_this_accept) {
                        yy_last_accept_state = yy_state;
                        yy_mark_end();
                }
                while (true) {
                        if (yy_initial && yy_at_bol) yy_lookahead = YY_BOL;
                        else yy_lookahead = yy_advance();
                        yy_next_state = YY_F;
                        yy_next_state = yy_nxt[yy_rmap[yy_state]][yy_cmap[yy_lookahead]];
                        if (YY_EOF == yy_lookahead && true == yy_initial) {

return newSymbol(sym.EOF);
                        }
                        if (YY_F != yy_next_state) {
                                yy_state = yy_next_state;
                                yy_initial = false;
                                yy_this_accept = yy_acpt[yy_state];
                                if (YY_NOT_ACCEPT != yy_this_accept) {
                                        yy_last_accept_state = yy_state;
                                        yy_mark_end();
                                }
                        }
                        else {
                                if (YY_NO_STATE == yy_last_accept_state) {
                                        throw (new Error("Lexical Error: Unmatched Input."));
                                }
                                else {
                                        yy_anchor = yy_acpt[yy_last_accept_state];
                                        if (0 != (YY_END & yy_anchor)) {
                                                yy_move_end();
                                        }
                                        yy_to_mark();
                                        switch (yy_last_accept_state) {
                                        case 1:

                                        case -2:
                                                break;
                                        case 2:
                                                { return newSymbol(sym.STAR); }
                                        case -3:
                                                break;
                                        case 3:
                                                { return newSymbol(sym.SLASH); }
                                        case -4:
                                                break;
                                        case 4:
                                                { return newSymbol(sym.PLUS); }
                                        case -5:
                                                break;
                                        case 5:
                                                { return newSymbol(sym.MINUS); }
                                        case -6:
                                                break;
                                        case 6:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -7:
                                                break;
                                        case 7:
                                                { throw new Exception(yytext()); }
                                        case -8:
                                                break;
                                        case 8:
                                                { return newSymbol(sym.COMMA); }
                                        case -9:
                                                break;
                                        case 9:
                                                { return newSymbol(sym.ATSIGN); }
                                        case -10:
                                                break;
                                        case 10:
                                                { return newSymbol(sym.DOT); }
                                        case -11:
                                                break;
                                        case 11:
                                                { return newSymbol(sym.VBAR); }
                                        case -12:
                                                break;
                                        case 12:
                                                { return newSymbol(sym.DOLLAR); }
                                        case -13:
                                                break;
                                        case 13:
                                                { return newSymbol(sym.EQ); }
                                        case -14:
                                                break;
                                        case 14:
                                                { return newSymbol(sym.LT); }
                                        case -15:
                                                break;
                                        case 15:
                                                { return newSymbol(sym.GT); }
                                        case -16:
                                                break;
                                        case 16:
                                                { return newSymbol(sym.LPAREN); }
                                        case -17:
                                                break;
                                        case 17:
                                                { return newSymbol(sym.RPAREN); }
                                        case -18:
                                                break;
                                        case 18:
                                                { /* ignore white space. */ }
                                        case -19:
                                                break;
                                        case 19:
                                                { return newSymbol(sym.LBRACK); }
                                        case -20:
                                                break;
                                        case 20:
                                                { return newSymbol(sym.RBRACK); }
                                        case -21:
                                                break;
                                        case 21:
                                                { return newSymbol(sym.INT, Long.valueOf(yytext())); }
                                        case -22:
                                                break;
                                        case 22:
                                                { return newSymbol(sym.DSLASH); }
                                        case -23:
                                                break;
                                        case 23:
                                                { return disambiguateAxisOrFunction(sym.ID); }
                                        case -24:
                                                break;
                                        case 24:
                                                { return disambiguateOperator(sym.OR); }
                                        case -25:
                                                break;
                                        case 25:
                                                { return newSymbol(sym.DCOLON); }
                                        case -26:
                                                break;
                                        case 26:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -27:
                                                break;
                                        case 27:
                                                { return newSymbol(sym.DDOT); }
                                        case -28:
                                                break;
                                        case 28:
                                                { return newSymbol(sym.REAL, Double.valueOf(yytext())); }
                                        case -29:
                                                break;
                                        case 29:
                                                { return newSymbol(sym.NE); }
                                        case -30:
                                                break;
                                        case 30:
                                                { return newSymbol(sym.LE); }
                                        case -31:
                                                break;
                                        case 31:
                                                { return newSymbol(sym.GE); }
                                        case -32:
                                                break;
                                        case 32:
                                                { return newSymbol(sym.Literal,
                              yytext().substring(1, yytext().length() - 1)); }
                                        case -33:
                                                break;
                                        case 33:
                                                { return newSymbol(sym.Literal,
                              yytext().substring(1, yytext().length() - 1)); }
                                        case -34:
                                                break;
                                        case 34:
                                                { return newSymbol(sym.REAL, Double.valueOf(yytext())); }
                                        case -35:
                                                break;
                                        case 35:
                                                { return disambiguateOperator(sym.DIV); }
                                        case -36:
                                                break;
                                        case 36:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -37:
                                                break;
                                        case 37:
                                                { return disambiguateOperator(sym.MOD); }
                                        case -38:
                                                break;
                                        case 38:
                                                { return disambiguateAxisOrFunction(sym.KEY); }
                                        case -39:
                                                break;
                                        case 39:
                                                { return disambiguateOperator(sym.AND); }
                                        case -40:
                                                break;
                                        case 40:
                                                { return disambiguateAxisOrFunction(sym.SELF); }
                                        case -41:
                                                break;
                                        case 41:
                                                { return disambiguateAxisOrFunction(sym.CHILD); }
                                        case -42:
                                                break;
                                        case 42:
                                                { return newSymbol(sym.TEXT); }
                                        case -43:
                                                break;
                                        case 43:
                                                { return newSymbol(sym.NODE); }
                                        case -44:
                                                break;
                                        case 44:
                                                { return disambiguateAxisOrFunction(sym.PARENT); }
                                        case -45:
                                                break;
                                        case 45:
                                                { return newSymbol(sym.TEXT); }
                                        case -46:
                                                break;
                                        case 46:
                                                { return newSymbol(sym.NODE); }
                                        case -47:
                                                break;
                                        case 47:
                                                { return disambiguateAxisOrFunction(sym.ANCESTOR); }
                                        case -48:
                                                break;
                                        case 48:
                                                { initialize(); return new Symbol(sym.PATTERN); }
                                        case -49:
                                                break;
                                        case 49:
                                                { return disambiguateAxisOrFunction(sym.NAMESPACE); }
                                        case -50:
                                                break;
                                        case 50:
                                                { return newSymbol(sym.COMMENT); }
                                        case -51:
                                                break;
                                        case 51:
                                                { return disambiguateAxisOrFunction(sym.PRECEDING); }
                                        case -52:
                                                break;
                                        case 52:
                                                { return disambiguateAxisOrFunction(sym.ATTRIBUTE); }
                                        case -53:
                                                break;
                                        case 53:
                                                { return disambiguateAxisOrFunction(sym.FOLLOWING); }
                                        case -54:
                                                break;
                                        case 54:
                                                { return disambiguateAxisOrFunction(sym.DESCENDANT); }
                                        case -55:
                                                break;
                                        case 55:
                                                { return newSymbol(sym.COMMENT); }
                                        case -56:
                                                break;
                                        case 56:
                                                { initialize(); return new Symbol(sym.EXPRESSION); }
                                        case -57:
                                                break;
                                        case 57:
                                                { return disambiguateAxisOrFunction(sym.ANCESTORORSELF); }
                                        case -58:
                                                break;
                                        case 58:
                                                { return disambiguateAxisOrFunction(sym.PRECEDINGSIBLING); }
                                        case -59:
                                                break;
                                        case 59:
                                                { return disambiguateAxisOrFunction(sym.FOLLOWINGSIBLING); }
                                        case -60:
                                                break;
                                        case 60:
                                                { return disambiguateAxisOrFunction(sym.DESCENDANTORSELF); }
                                        case -61:
                                                break;
                                        case 61:
                                                { return disambiguateAxisOrFunction(sym.PIPARAM); }
                                        case -62:
                                                break;
                                        case 62:
                                                { return newSymbol(sym.PI); }
                                        case -63:
                                                break;
                                        case 63:
                                                { return newSymbol(sym.PI); }
                                        case -64:
                                                break;
                                        case 65:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -65:
                                                break;
                                        case 66:
                                                { throw new Exception(yytext()); }
                                        case -66:
                                                break;
                                        case 67:
                                                { return newSymbol(sym.INT, Long.valueOf(yytext())); }
                                        case -67:
                                                break;
                                        case 68:
                                                { return newSymbol(sym.REAL, Double.valueOf(yytext())); }
                                        case -68:
                                                break;
                                        case 70:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -69:
                                                break;
                                        case 71:
                                                { throw new Exception(yytext()); }
                                        case -70:
                                                break;
                                        case 73:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -71:
                                                break;
                                        case 74:
                                                { throw new Exception(yytext()); }
                                        case -72:
                                                break;
                                        case 76:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -73:
                                                break;
                                        case 77:
                                                { throw new Exception(yytext()); }
                                        case -74:
                                                break;
                                        case 79:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -75:
                                                break;
                                        case 81:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -76:
                                                break;
                                        case 83:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -77:
                                                break;
                                        case 85:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -78:
                                                break;
                                        case 87:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -79:
                                                break;
                                        case 89:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -80:
                                                break;
                                        case 91:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -81:
                                                break;
                                        case 93:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -82:
                                                break;
                                        case 95:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -83:
                                                break;
                                        case 97:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -84:
                                                break;
                                        case 99:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -85:
                                                break;
                                        case 101:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -86:
                                                break;
                                        case 103:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -87:
                                                break;
                                        case 105:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -88:
                                                break;
                                        case 107:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -89:
                                                break;
                                        case 109:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -90:
                                                break;
                                        case 111:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -91:
                                                break;
                                        case 113:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -92:
                                                break;
                                        case 115:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -93:
                                                break;
                                        case 117:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -94:
                                                break;
                                        case 119:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -95:
                                                break;
                                        case 121:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -96:
                                                break;
                                        case 123:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -97:
                                                break;
                                        case 125:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -98:
                                                break;
                                        case 127:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -99:
                                                break;
                                        case 128:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -100:
                                                break;
                                        case 129:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -101:
                                                break;
                                        case 130:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -102:
                                                break;
                                        case 131:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -103:
                                                break;
                                        case 132:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -104:
                                                break;
                                        case 133:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -105:
                                                break;
                                        case 134:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -106:
                                                break;
                                        case 135:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -107:
                                                break;
                                        case 136:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -108:
                                                break;
                                        case 137:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -109:
                                                break;
                                        case 138:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -110:
                                                break;
                                        case 139:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -111:
                                                break;
                                        case 140:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -112:
                                                break;
                                        case 141:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -113:
                                                break;
                                        case 142:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -114:
                                                break;
                                        case 143:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -115:
                                                break;
                                        case 144:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -116:
                                                break;
                                        case 145:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -117:
                                                break;
                                        case 146:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -118:
                                                break;
                                        case 147:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -119:
                                                break;
                                        case 148:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -120:
                                                break;
                                        case 149:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -121:
                                                break;
                                        case 150:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -122:
                                                break;
                                        case 151:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -123:
                                                break;
                                        case 152:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -124:
                                                break;
                                        case 153:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -125:
                                                break;
                                        case 154:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -126:
                                                break;
                                        case 155:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -127:
                                                break;
                                        case 156:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -128:
                                                break;
                                        case 157:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -129:
                                                break;
                                        case 158:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -130:
                                                break;
                                        case 159:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -131:
                                                break;
                                        case 160:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -132:
                                                break;
                                        case 161:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -133:
                                                break;
                                        case 162:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -134:
                                                break;
                                        case 163:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -135:
                                                break;
                                        case 164:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -136:
                                                break;
                                        case 165:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -137:
                                                break;
                                        case 166:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -138:
                                                break;
                                        case 167:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -139:
                                                break;
                                        case 168:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -140:
                                                break;
                                        case 169:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -141:
                                                break;
                                        case 170:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -142:
                                                break;
                                        case 171:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -143:
                                                break;
                                        case 172:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -144:
                                                break;
                                        case 173:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -145:
                                                break;
                                        case 174:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -146:
                                                break;
                                        case 175:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -147:
                                                break;
                                        case 176:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -148:
                                                break;
                                        case 177:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -149:
                                                break;
                                        case 178:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -150:
                                                break;
                                        case 179:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -151:
                                                break;
                                        case 180:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -152:
                                                break;
                                        case 181:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -153:
                                                break;
                                        case 182:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -154:
                                                break;
                                        case 183:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -155:
                                                break;
                                        case 184:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -156:
                                                break;
                                        case 187:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -157:
                                                break;
                                        case 189:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -158:
                                                break;
                                        case 191:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -159:
                                                break;
                                        case 192:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -160:
                                                break;
                                        case 193:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -161:
                                                break;
                                        case 194:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -162:
                                                break;
                                        case 195:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -163:
                                                break;
                                        case 196:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -164:
                                                break;
                                        case 197:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -165:
                                                break;
                                        case 198:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -166:
                                                break;
                                        case 199:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -167:
                                                break;
                                        case 200:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -168:
                                                break;
                                        case 201:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -169:
                                                break;
                                        case 202:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -170:
                                                break;
                                        case 203:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -171:
                                                break;
                                        case 204:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -172:
                                                break;
                                        case 205:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -173:
                                                break;
                                        case 206:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -174:
                                                break;
                                        case 207:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -175:
                                                break;
                                        case 208:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -176:
                                                break;
                                        case 209:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -177:
                                                break;
                                        case 210:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -178:
                                                break;
                                        case 211:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -179:
                                                break;
                                        case 212:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -180:
                                                break;
                                        case 213:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -181:
                                                break;
                                        case 214:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -182:
                                                break;
                                        case 215:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -183:
                                                break;
                                        case 216:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -184:
                                                break;
                                        case 217:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -185:
                                                break;
                                        case 218:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -186:
                                                break;
                                        case 219:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -187:
                                                break;
                                        case 220:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -188:
                                                break;
                                        case 221:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -189:
                                                break;
                                        case 222:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -190:
                                                break;
                                        case 223:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -191:
                                                break;
                                        case 224:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -192:
                                                break;
                                        case 225:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -193:
                                                break;
                                        case 226:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -194:
                                                break;
                                        case 227:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -195:
                                                break;
                                        case 228:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -196:
                                                break;
                                        case 229:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -197:
                                                break;
                                        case 230:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -198:
                                                break;
                                        case 231:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -199:
                                                break;
                                        case 232:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -200:
                                                break;
                                        case 233:
                                                { return newSymbol(sym.QNAME, yytext()); }
                                        case -201:
                                                break;
                                        default:
                                                yy_error(YY_E_INTERNAL,false);
                                        case -1:
                                        }
                                        yy_initial = true;
                                        yy_state = yy_state_dtrans[yy_lexical_state];
                                        yy_next_state = YY_NO_STATE;
                                        yy_last_accept_state = YY_NO_STATE;
                                        yy_mark_start();
                                        yy_this_accept = yy_acpt[yy_state];
                                        if (YY_NOT_ACCEPT != yy_this_accept) {
                                                yy_last_accept_state = yy_state;
                                                yy_mark_end();
                                        }
                                }
                        }
                }
        }
}
