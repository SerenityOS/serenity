/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package build.tools.pandocfilter.json;

import java.util.*;

class JSONParser {
    private int pos = 0;
    private String input;

    JSONParser() {
    }

    private IllegalStateException failure(String message) {
        return new IllegalStateException(String.format("[%d]: %s : %s", pos, message, input));
    }

    private char current() {
        return input.charAt(pos);
    }

    private void advance() {
        pos++;
    }

    private boolean hasInput() {
        return pos < input.length();
    }

    private void expectMoreInput(String message) {
        if (!hasInput()) {
            throw failure(message);
        }
    }

    private char next(String message) {
        advance();
        if (!hasInput()) {
            throw failure(message);
        }
        return current();
    }


    private void expect(char c) {
        var msg = String.format("Expected character %c", c);

        var n = next(msg);
        if (n != c) {
            throw failure(msg);
        }
    }

    private void assume(char c, String message) {
        expectMoreInput(message);
        if (current() != c) {
            throw failure(message);
        }
    }

    private JSONBoolean parseBoolean() {
        if (current() == 't') {
            expect('r');
            expect('u');
            expect('e');
            advance();
            return new JSONBoolean(true);
        }

        if (current() == 'f') {
            expect('a');
            expect('l');
            expect('s');
            expect('e');
            advance();
            return new JSONBoolean(false);
        }

        throw failure("a boolean can only be 'true' or 'false'");
    }

    private JSONValue parseNumber() {
        var isInteger = true;
        var builder = new StringBuilder();

        if (current() == '-') {
            builder.append(current());
            advance();
            expectMoreInput("a number cannot consist of only '-'");
        }

        if (current() == '0') {
            builder.append(current());
            advance();

            if (hasInput() && current() == '.') {
                isInteger = false;
                builder.append(current());
                advance();

                expectMoreInput("a number cannot end with '.'");

                if (!isDigit(current())) {
                    throw failure("must be at least one digit after '.'");
                }

                while (hasInput() && isDigit(current())) {
                    builder.append(current());
                    advance();
                }
            }
        } else {
            while (hasInput() && isDigit(current())) {
                builder.append(current());
                advance();
            }

            if (hasInput() && current() == '.') {
                isInteger = false;
                builder.append(current());
                advance();

                expectMoreInput("a number cannot end with '.'");

                if (!isDigit(current())) {
                    throw failure("must be at least one digit after '.'");
                }

                while (hasInput() && isDigit(current())) {
                    builder.append(current());
                    advance();
                }
            }
        }

        if (hasInput() && (current() == 'e' || current() == 'E')) {
            isInteger = false;

            builder.append(current());
            advance();
            expectMoreInput("a number cannot end with 'e' or 'E'");

            if (current() == '+' || current() == '-') {
                builder.append(current());
                advance();
            }

            if (!isDigit(current())) {
                throw failure("a digit must follow {'e','E'}{'+','-'}");
            }

            while (hasInput() && isDigit(current())) {
                builder.append(current());
                advance();
            }
        }

        var value = builder.toString();
        return isInteger ? new JSONNumber(Long.parseLong(value)) :
                           new JSONDecimal(Double.parseDouble(value));

    }

    private JSONString parseString() {
        var missingEndChar = "string is not terminated with '\"'";
        var builder = new StringBuilder();
        for (var c = next(missingEndChar); c != '"'; c = next(missingEndChar)) {
            if (c == '\\') {
                var n = next(missingEndChar);
                switch (n) {
                    case '"':
                        builder.append("\"");
                        break;
                    case '\\':
                        builder.append("\\");
                        break;
                    case '/':
                        builder.append("/");
                        break;
                    case 'b':
                        builder.append("\b");
                        break;
                    case 'f':
                        builder.append("\f");
                        break;
                    case 'n':
                        builder.append("\n");
                        break;
                    case 'r':
                        builder.append("\r");
                        break;
                    case 't':
                        builder.append("\t");
                        break;
                    case 'u':
                        var u1 = next(missingEndChar);
                        var u2 = next(missingEndChar);
                        var u3 = next(missingEndChar);
                        var u4 = next(missingEndChar);
                        var cp = Integer.parseInt(String.format("%c%c%c%c", u1, u2, u3, u4), 16);
                        builder.append(new String(new int[]{cp}, 0, 1));
                        break;
                    default:
                        throw failure(String.format("Unexpected escaped character '%c'", n));
                }
            } else {
                builder.append(c);
            }
        }

        advance(); // step beyond closing "
        return new JSONString(builder.toString());
    }

    private JSONArray parseArray() {
        var error = "array is not terminated with ']'";
        var list = new ArrayList<JSONValue>();

        advance(); // step beyond opening '['
        consumeWhitespace();
        expectMoreInput(error);

        while (current() != ']') {
            var val = parseValue();
            list.add(val);

            expectMoreInput(error);
            if (current() == ',') {
                advance();
            }
            expectMoreInput(error);
        }

        advance(); // step beyond closing ']'
        return new JSONArray(list.toArray(new JSONValue[0]));
    }

    public JSONNull parseNull() {
        expect('u');
        expect('l');
        expect('l');
        advance();
        return new JSONNull();
    }

    public JSONObject parseObject() {
        var error = "object is not terminated with '}'";
        var map = new HashMap<String, JSONValue>();

        advance(); // step beyond opening '{'
        consumeWhitespace();
        expectMoreInput(error);

        while (current() != '}') {
            var key = parseValue();
            if (!(key instanceof JSONString)) {
                throw failure("a field must of type string");
            }

            if (!hasInput() || current() != ':') {
                throw failure("a field must be followed by ':'");
            }
            advance(); // skip ':'

            var val = parseValue();
            map.put(key.asString(), val);

            expectMoreInput(error);
            if (current() == ',') {
                advance();
            }
            expectMoreInput(error);
        }

        advance(); // step beyond '}'
        return new JSONObject(map);
    }

    private boolean isDigit(char c) {
        return c == '0' ||
               c == '1' ||
               c == '2' ||
               c == '3' ||
               c == '4' ||
               c == '5' ||
               c == '6' ||
               c == '7' ||
               c == '8' ||
               c == '9';
    }

    private boolean isStartOfNumber(char c) {
        return isDigit(c) || c == '-';
    }

    private boolean isStartOfString(char c) {
        return c == '"';
    }

    private boolean isStartOfBoolean(char c) {
        return c == 't' || c == 'f';
    }

    private boolean isStartOfArray(char c) {
        return c == '[';
    }

    private boolean isStartOfNull(char c) {
        return c == 'n';
    }

    private boolean isWhitespace(char c) {
        return c == '\r' ||
               c == '\n' ||
               c == '\t' ||
               c == ' ';
    }

    private boolean isStartOfObject(char c) {
        return c == '{';
    }

    private void consumeWhitespace() {
        while (hasInput() && isWhitespace(current())) {
            advance();
        }
    }

    public JSONValue parseValue() {
        JSONValue ret = null;

        consumeWhitespace();
        if (hasInput()) {
            var c = current();

            if (isStartOfNumber(c)) {
                ret = parseNumber();
            } else if (isStartOfString(c)) {
                ret = parseString();
            } else if (isStartOfBoolean(c)) {
                ret = parseBoolean();
            } else if (isStartOfArray(c)) {
                ret = parseArray();
            } else if (isStartOfNull(c)) {
                ret = parseNull();
            } else if (isStartOfObject(c)) {
                ret = parseObject();
            } else {
                throw failure("not a valid start of a JSON value");
            }
        }
        consumeWhitespace();

        return ret;
    }

    public JSONValue parse(String s) {
        if (s == null || s.equals("")) {
            return null;
        }

        pos = 0;
        input = s;

        var result = parseValue();
        if (hasInput()) {
            throw failure("can only have one top-level JSON value");
        }
        return result;
    }
}
