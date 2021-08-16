/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.main;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Various utility methods for processing Java tool command line arguments.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class CommandLine {
    /**
     * Process Win32-style command files for the specified command line
     * arguments and return the resulting arguments. A command file argument
     * is of the form '@file' where 'file' is the name of the file whose
     * contents are to be parsed for additional arguments. The contents of
     * the command file are parsed using StreamTokenizer and the original
     * '@file' argument replaced with the resulting tokens. Recursive command
     * files are not supported. The '@' character itself can be quoted with
     * the sequence '@@'.
     * @param args the arguments that may contain @files
     * @return the arguments, with @files expanded
     * @throws IOException if there is a problem reading any of the @files
     */
    public static List<String> parse(List<String> args) throws IOException {
        List<String> newArgs = new ArrayList<>();
        appendParsedCommandArgs(newArgs, args);
        return newArgs;
    }

    private static void appendParsedCommandArgs(List<String> newArgs, List<String> args) throws IOException {
        for (String arg : args) {
            if (arg.length() > 1 && arg.charAt(0) == '@') {
                arg = arg.substring(1);
                if (arg.charAt(0) == '@') {
                    newArgs.add(arg);
                } else {
                    loadCmdFile(arg, newArgs);
                }
            } else {
                newArgs.add(arg);
            }
        }
    }

    /**
     * Process the given environment variable and appends any Win32-style
     * command files for the specified command line arguments and return
     * the resulting arguments. A command file argument
     * is of the form '@file' where 'file' is the name of the file whose
     * contents are to be parsed for additional arguments. The contents of
     * the command file are parsed using StreamTokenizer and the original
     * '@file' argument replaced with the resulting tokens. Recursive command
     * files are not supported. The '@' character itself can be quoted with
     * the sequence '@@'.
     * @param envVariable the env variable to process
     * @param args the arguments that may contain @files
     * @return the arguments, with environment variable's content and expansion of @files
     * @throws IOException if there is a problem reading any of the @files
     * @throws com.sun.tools.javac.main.CommandLine.UnmatchedQuote
     */
    public static List<String> parse(String envVariable, List<String> args)
            throws IOException, UnmatchedQuote {

        List<String> inArgs = new ArrayList<>();
        appendParsedEnvVariables(inArgs, envVariable);
        inArgs.addAll(args);
        List<String> newArgs = new ArrayList<>();
        appendParsedCommandArgs(newArgs, inArgs);
        return newArgs;
    }

    private static void loadCmdFile(String name, List<String> args) throws IOException {
        try (Reader r = Files.newBufferedReader(Paths.get(name), Charset.defaultCharset())) {
            Tokenizer t = new Tokenizer(r);
            String s;
            while ((s = t.nextToken()) != null) {
                args.add(s);
            }
        }
    }

    public static class Tokenizer {
        private final Reader in;
        private int ch;

        public Tokenizer(Reader in) throws IOException {
            this.in = in;
            ch = in.read();
        }

        public String nextToken() throws IOException {
            skipWhite();
            if (ch == -1) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            char quoteChar = 0;

            while (ch != -1) {
                switch (ch) {
                    case ' ':
                    case '\t':
                    case '\f':
                        if (quoteChar == 0) {
                            return sb.toString();
                        }
                        sb.append((char) ch);
                        break;

                    case '\n':
                    case '\r':
                        return sb.toString();

                    case '\'':
                    case '"':
                        if (quoteChar == 0) {
                            quoteChar = (char) ch;
                        } else if (quoteChar == ch) {
                            quoteChar = 0;
                        } else {
                            sb.append((char) ch);
                        }
                        break;

                    case '\\':
                        if (quoteChar != 0) {
                            ch = in.read();
                            switch (ch) {
                                case '\n':
                                case '\r':
                                    while (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t' || ch == '\f') {
                                        ch = in.read();
                                    }
                                    continue;

                                case 'n':
                                    ch = '\n';
                                    break;
                                case 'r':
                                    ch = '\r';
                                    break;
                                case 't':
                                    ch = '\t';
                                    break;
                                case 'f':
                                    ch = '\f';
                                    break;
                            }
                        }
                        sb.append((char) ch);
                        break;

                    default:
                        sb.append((char) ch);
                }

                ch = in.read();
            }

            return sb.toString();
        }

        void skipWhite() throws IOException {
            while (ch != -1) {
                switch (ch) {
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                    case '\f':
                        break;

                    case '#':
                        ch = in.read();
                        while (ch != '\n' && ch != '\r' && ch != -1) {
                            ch = in.read();
                        }
                        break;

                    default:
                        return;
                }

                ch = in.read();
            }
        }
    }

    @SuppressWarnings("fallthrough")
    private static void appendParsedEnvVariables(List<String> newArgs, String envVariable)
            throws UnmatchedQuote {

        if (envVariable == null) {
            return;
        }
        String in = System.getenv(envVariable);
        if (in == null || in.trim().isEmpty()) {
            return;
        }

        final char NUL = (char)0;
        final int len = in.length();

        int pos = 0;
        StringBuilder sb = new StringBuilder();
        char quote = NUL;
        char ch;

        loop:
        while (pos < len) {
            ch = in.charAt(pos);
            switch (ch) {
                case '\"': case '\'':
                    if (quote == NUL) {
                        quote = ch;
                    } else if (quote == ch) {
                        quote = NUL;
                    } else {
                        sb.append(ch);
                    }
                    pos++;
                    break;
                case '\f': case '\n': case '\r': case '\t': case ' ':
                    if (quote == NUL) {
                        newArgs.add(sb.toString());
                        sb.setLength(0);
                        while (ch == '\f' || ch == '\n' || ch == '\r' || ch == '\t' || ch == ' ') {
                            pos++;
                            if (pos >= len) {
                                break loop;
                            }
                            ch = in.charAt(pos);
                        }
                        break;
                    }
                    // fall through
                default:
                    sb.append(ch);
                    pos++;
            }
        }
        if (sb.length() != 0) {
            newArgs.add(sb.toString());
        }
        if (quote != NUL) {
            throw new UnmatchedQuote(envVariable);
        }
    }

    public static class UnmatchedQuote extends Exception {
        private static final long serialVersionUID = 0;

        public final String variableName;

        UnmatchedQuote(String variable) {
            this.variableName = variable;
        }
    }
}
