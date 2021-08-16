/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8056897 8254073
 * @modules jdk.compiler/com.sun.tools.javac.parser
 *          jdk.compiler/com.sun.tools.javac.util
 * @summary Proper lexing of various token kinds.
 */

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import com.sun.tools.javac.parser.JavaTokenizer;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.parser.Tokens.Token;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

import static com.sun.tools.javac.parser.Tokens.TokenKind.*;

public class JavaLexerTest {
    static final TestTuple[] PASSING_TESTS = {
            new TestTuple(FLOATLITERAL, "0.0f"),
            new TestTuple(FLOATLITERAL, "0.0F"),
            new TestTuple(FLOATLITERAL, ".0F"),
            new TestTuple(FLOATLITERAL, "0.F"),
            new TestTuple(FLOATLITERAL, "0E0F"),
            new TestTuple(FLOATLITERAL, "0E+0F"),
            new TestTuple(FLOATLITERAL, "0E-0F"),

            new TestTuple(DOUBLELITERAL, "0.0d"),
            new TestTuple(DOUBLELITERAL, "0.0D"),
            new TestTuple(DOUBLELITERAL, ".0D"),
            new TestTuple(DOUBLELITERAL, "0.D"),
            new TestTuple(DOUBLELITERAL, "0E0D"),
            new TestTuple(DOUBLELITERAL, "0E+0D"),
            new TestTuple(DOUBLELITERAL, "0E-0D"),
            new TestTuple(DOUBLELITERAL, "0x0.0p0d"),
            new TestTuple(DOUBLELITERAL, "0xff.0p8d"),

            new TestTuple(STRINGLITERAL, "\"\\u2022\""),
            new TestTuple(STRINGLITERAL, "\"\\b\\t\\n\\f\\r\\\'\\\"\\\\\""),

            new TestTuple(CHARLITERAL,   "\'\\b\'"),
            new TestTuple(CHARLITERAL,   "\'\\t\'"),
            new TestTuple(CHARLITERAL,   "\'\\n\'"),
            new TestTuple(CHARLITERAL,   "\'\\f\'"),
            new TestTuple(CHARLITERAL,   "\'\\r\'"),
            new TestTuple(CHARLITERAL,   "\'\\'\'"),
            new TestTuple(CHARLITERAL,   "\'\\\\'"),
            new TestTuple(CHARLITERAL,   "\'\\\'\'"),
            new TestTuple(CHARLITERAL,   "\'\\\"\'"),

            new TestTuple(IDENTIFIER,    "abc\\u0005def"),
    };

    static final TestTuple[] FAILING_TESTS = {
            new TestTuple(LONGLITERAL,   "0bL"),
            new TestTuple(LONGLITERAL,   "0b20L"),
            new TestTuple(LONGLITERAL,   "0xL"),
            new TestTuple(INTLITERAL,    "0xG000L", "0x"),

            new TestTuple(DOUBLELITERAL, "0E*0F", "0E"),

            new TestTuple(DOUBLELITERAL, "0E*0D", "0E"),
            new TestTuple(INTLITERAL,    "0xp8d", "0x"),
            new TestTuple(DOUBLELITERAL, "0x8pd", "0x8pd"),
            new TestTuple(INTLITERAL,    "0xpd", "0x"),

            new TestTuple(STRINGLITERAL, "\"\\u20\""),
            new TestTuple(STRINGLITERAL, "\"\\u\""),
            new TestTuple(STRINGLITERAL, "\"\\uG000\""),
            new TestTuple(STRINGLITERAL, "\"\\u \""),
            new TestTuple(ERROR,         "\"\\q\""),
            new TestTuple(EOF,           "\\u", ""),

            new TestTuple(ERROR,         "\'\'"),
            new TestTuple(ERROR,         "\'\\q\'", "\'\\"),
    };

    static class TestTuple {
        String input;
        TokenKind kind;
        String expected;

        TestTuple(TokenKind kind, String input, String expected) {
            this.input = input;
            this.kind = kind;
            this.expected = expected;
        }

        TestTuple(TokenKind kind, String input) {
            this(kind, input, input);
        }
    }

    void test(TestTuple test, boolean willFail) throws Exception {
        Context ctx = new Context();
        Log log = Log.instance(ctx);

        log.useSource(new SimpleJavaFileObject(new URI("mem://Test.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return test.input;
            }
        });

        char[] inputArr = test.input.toCharArray();
        JavaTokenizer tokenizer = new JavaTokenizer(ScannerFactory.instance(ctx), inputArr, inputArr.length) {};
        Token token = tokenizer.readToken();
        boolean failed = log.nerrors != 0;
        boolean normal = failed == willFail;

        if (!normal) {
            System.err.println("input: " + test.input);
            String message = willFail ? "Expected to fail: " : "Expected to pass: ";
            throw new AssertionError(message + test.input);
        }

        String actual = test.input.substring(token.pos, token.endPos);

        if (token.kind != test.kind) {
            System.err.println("input: " + test.input);
            throw new AssertionError("Unexpected token kind: " + token.kind.name());
        }

        if (!Objects.equals(test.expected, actual)) {
            System.err.println("input: " + test.input);
            throw new AssertionError("Unexpected token content: " + actual);
        }
    }

    void run() throws Exception {
        for (TestTuple test : PASSING_TESTS) {
            test(test, false);
        }

        for (TestTuple test : FAILING_TESTS) {
            test(test, true);
        }
    }

    public static void main(String[] args) throws Exception {
        new JavaLexerTest().run();
    }
}
