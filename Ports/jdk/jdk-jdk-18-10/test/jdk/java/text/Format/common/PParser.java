/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;

/*
 * assignment : key = value;
 * key        : string
 * value      : string | array | dict
 * nValue     : , value
 * array      : ( value nValue )
 * nAssignment: , assignment|value
 * dict       : { assignment* }
 * string     : "*" or anything but a ,(){}=
 *
 * special characters: ,(){}=
 */

public class PParser {
    protected static final int      OPEN_PAIR = 1;
    protected static final int      CLOSE_PAIR = 2;
    protected static final int      OPEN_ARRAY = 3;
    protected static final int      CLOSE_ARRAY = 4;
    protected static final int      MORE = 5;
    protected static final int      EQUAL = 6;
    protected static final int      STRING = 7;
    protected static final int      WS = 8;

    protected Reader          reader;
    protected boolean         bufferedToken;
    protected StringBuffer    stringBuffer = new StringBuffer();
    protected int             lastChar;
    protected int             lastToken;
    protected int             lineNumber;
    protected int             column;

    public PParser() {
    }

    public Map<String,Object> parse(Reader r) throws IOException {
        this.reader = r;
        bufferedToken = false;
        lineNumber = 0;
        column = 0;
        if (getToken() != OPEN_PAIR) {
            error("No initial open");
        }
        return parsePair();
    }

    protected Object parseValue(int lookAhead) throws IOException {
        int           token;

        if (lookAhead == -1) {
            token = getToken();
        } else {
            token = lookAhead;
        }
        switch (token) {
        case STRING:
            return stringBuffer.toString();
        case OPEN_ARRAY:
            return parseArray();
        case OPEN_PAIR:
            return parsePair();
        default:
            error("Expecting value");
        }
        return null;
    }

    protected Object parseArray() throws IOException {
        List<Object> array = new ArrayList<>();
        int token;

        while ((token = getToken()) != CLOSE_ARRAY) {
            if (token == MORE) {
                token = getToken();
            }
            if (token != CLOSE_ARRAY) {
                array.add(parseValue(token));
            }
        }
        return array;
    }

    protected Map<String,Object> parsePair() throws IOException {
        Map<String,Object> ht = new HashMap<>(11);
        int token;

        while ((token = getToken()) != CLOSE_PAIR) {
            if (token != STRING) {
                error("Pair expecting string got");
            }
            String     key = stringBuffer.toString();

            if (getToken() != EQUAL) {
                error("Expecting = ");
            }

            Object     value = parseValue(-1);
            ht.put(key, value);
        }
        return ht;
    }

    protected void ungetToken() {
        if (bufferedToken) {
            error("Can not buffer more than one token");
        }
        bufferedToken = true;
    }

    protected int getToken() throws IOException {
        int token = getToken(false, false);

        return token;
    }

    @SuppressWarnings("fallthrough")
    protected int getToken(boolean wantsWS, boolean inString)
        throws IOException {
        if (bufferedToken) {
            bufferedToken = false;
            if (lastToken != WS || wantsWS) {
                return lastToken;
            }
        }
        while ((lastChar = reader.read()) != -1) {
            // If a line starts with '#', skip the line.
            if (column == 0 && lastChar == '#') {
                while ((lastChar = reader.read()) != -1
                       && lastChar != '\n') {
                }
                if (lastChar == -1) {
                    break;
                }
            }

            column++;
            switch(lastChar) {
            case '\n':
                lineNumber++;
                column = 0;
            case ' ':
            case '\r':
            case '\t':
                if (wantsWS) {
                    lastToken = WS;
                    return WS;
                }
                break;
            case ',':
                lastToken = MORE;
                return MORE;
            case '(':
                lastToken = OPEN_ARRAY;
                return OPEN_ARRAY;
            case ')':
                lastToken = CLOSE_ARRAY;
                return CLOSE_ARRAY;
            case '{':
                lastToken = OPEN_PAIR;
                return OPEN_PAIR;
            case '}':
                lastToken = CLOSE_PAIR;
                return CLOSE_PAIR;
            case '=':
                lastToken = EQUAL;
                return EQUAL;
            case '"':
                lastToken = STRING;
                if (!inString) {
                    stringBuffer.setLength(0);
                    while (true) {
                        getToken(true, true);
                        if (lastChar == '"') {
                            lastToken = STRING;
                            return STRING;
                        }
                        stringBuffer.append((char)lastChar);
                    }
                }
                return STRING;
            default:
                lastToken = STRING;
                if (!inString) {
                    stringBuffer.setLength(0);
                    stringBuffer.append((char)lastChar);
                    while (getToken(true, true) == STRING) {
                        if (lastChar == '"') {
                            error("Unexpected quote");
                        }
                        stringBuffer.append((char)lastChar);
                    }
                    ungetToken();
                }
                return STRING;
            }
        }
        return -1;
    }

    protected void error(String errorString) {
        throw new RuntimeException(errorString + " at line " + lineNumber + " column " + column);
    }

    @SuppressWarnings("unchecked")
    public static void dump(Object o) {
        if (o instanceof String) {
            System.out.print(o);
        } else if(o instanceof List) {
            dump(" (");
            ((List)o).forEach((l) -> {
                dump(l);
                dump(" -- ");
            });
            dump(" )");
        } else {
            Map<String,Object> ht = (Map<String,Object>)o;
            dump(" {");
            ht.keySet().forEach(l -> {
                dump(l);
                dump(" = ");
                dump(ht.get(l));
                dump(";");
            });
            dump(" }");
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("need filename");
        } else {
            try {
                FileReader fr = new FileReader(args[0]);
                PParser parser = new PParser();
                Map<String,Object> ht = parser.parse(fr);

                dump(ht);
                System.out.println();
            }
            catch (IOException ioe) {
                System.out.println("Couldn't parse: " + ioe);
            }
        }
    }
}
