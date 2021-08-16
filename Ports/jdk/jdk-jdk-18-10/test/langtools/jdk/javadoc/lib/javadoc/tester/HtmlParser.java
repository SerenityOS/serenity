/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javadoc.tester;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A basic HTML parser. Override the protected methods as needed to get notified
 * of significant items in any file that is read.
 */
public abstract class HtmlParser {

    protected final PrintStream out;
    protected final Function<Path,String> fileReader;

    private Path file;
    private StringReader in;
    private int ch;
    private int lineNumber;
    private boolean inScript;
    private boolean xml;

    HtmlParser(PrintStream out, Function<Path,String> fileReader) {
        this.out = out;
        this.fileReader = fileReader;
    }

    /**
     * Read a file.
     * @param file the file to be read
     * @throws IOException if an error occurs while reading the file
     */
    void read(Path file) throws IOException {
        try (StringReader r = new StringReader(fileReader.apply(file))) {
            this.file = file;
            this.in = r;
            StringBuilder content = new StringBuilder();

            startFile(file);
            try {
                lineNumber = 1;
                xml = false;
                nextChar();

                while (ch != -1) {
                    switch (ch) {

                        case '<':
                            content(content.toString());
                            content.setLength(0);
                            html();
                            break;

                        default:
                            content.append((char) ch);
                            if (ch == '\n') {
                                content(content.toString());
                                content.setLength(0);
                            }
                            nextChar();
                    }
                }
            } finally {
                endFile();
            }
        } catch (IOException e) {
            error(file, lineNumber, e);
        } catch (Throwable t) {
            error(file, lineNumber, t);
            t.printStackTrace(out);
        }
    }


    protected int getLineNumber() {
        return lineNumber;
    }

    /**
     * Called when a file has been opened, before parsing begins.
     * This is always the first notification when reading a file.
     * This implementation does nothing.
     *
     * @param file the file
     */
    protected void startFile(Path file) { }

    /**
     * Called when the parser has finished reading a file.
     * This is always the last notification when reading a file,
     * unless any errors occur while closing the file.
     * This implementation does nothing.
     */
    protected void endFile() { }

    /**
     * Called when a doctype declaration is found, at the beginning of the file.
     * This implementation does nothing.
     * @param s the doctype declaration
     */
    protected void docType(String s) { }

    /**
     * Called when the opening tag of an HTML element is encountered.
     * This implementation does nothing.
     * @param name the name of the tag
     * @param attrs the attribute
     * @param selfClosing whether or not this is a self-closing tag
     */
    protected void startElement(String name, Map<String,String> attrs, boolean selfClosing) { }

    /**
     * Called when the closing tag of an HTML tag is encountered.
     * This implementation does nothing.
     * @param name the name of the tag
     */
    protected void endElement(String name) { }

    /**
     * Called for sequences of character content.
     * @param content the character content
     */
    protected void content(String content) { }

    /**
     * Called when an error has been encountered.
     * @param file the file being read
     * @param lineNumber the line number of line containing the error
     * @param message a description of the error
     */
    protected void error(Path file, int lineNumber, String message) {
        out.println(file + ":" + lineNumber + ": " + message);
    }

    /**
     * Called when an exception has been encountered.
     * @param file the file being read
     * @param lineNumber the line number of the line being read when the exception was found
     * @param t the exception
     */
    protected void error(Path file, int lineNumber, Throwable t) {
        out.println(file + ":" + lineNumber + ": " + t);
    }

    private void nextChar() throws IOException {
        ch = in.read();
        if (ch == '\n')
            lineNumber++;
    }

    /**
     * Read the start or end of an HTML tag, or an HTML comment
     * {@literal <identifier attrs> } or {@literal </identifier> }
     * @throws java.io.IOException if there is a problem reading the file
     */
    private void html() throws IOException {
        nextChar();
        if (isIdentifierStart((char) ch)) {
            String name = readIdentifier().toLowerCase(Locale.US);
            Map<String,String> attrs = htmlAttrs();
            if (attrs != null) {
                boolean selfClosing = false;
                if (ch == '/') {
                    nextChar();
                    selfClosing = true;
                }
                if (ch == '>') {
                    nextChar();
                    startElement(name, attrs, selfClosing);
                    if (name.equals("script")) {
                        inScript = true;
                    }
                    return;
                }
            }
        } else if (ch == '/') {
            nextChar();
            if (isIdentifierStart((char) ch)) {
                String name = readIdentifier().toLowerCase(Locale.US);
                skipWhitespace();
                if (ch == '>') {
                    nextChar();
                    endElement(name);
                    if (name.equals("script")) {
                        inScript = false;
                    }
                    return;
                }
            }
        } else if (ch == '!') {
            nextChar();
            if (ch == '-') {
                nextChar();
                if (ch == '-') {
                    nextChar();
                    while (ch != -1) {
                        int dash = 0;
                        while (ch == '-') {
                            dash++;
                            nextChar();
                        }
                        // Strictly speaking, a comment should not contain "--"
                        // so dash > 2 is an error, dash == 2 implies ch == '>'
                        // See http://www.w3.org/TR/html-markup/syntax.html#syntax-comments
                        // for more details.
                        if (dash >= 2 && ch == '>') {
                            nextChar();
                            return;
                        }

                        nextChar();
                    }
                }
            } else if (ch == '[') {
                nextChar();
                if (ch == 'C') {
                    nextChar();
                    if (ch == 'D') {
                        nextChar();
                        if (ch == 'A') {
                            nextChar();
                            if (ch == 'T') {
                                nextChar();
                                if (ch == 'A') {
                                    nextChar();
                                    if (ch == '[') {
                                        while (true) {
                                            nextChar();
                                            if (ch == ']') {
                                                nextChar();
                                                if (ch == ']') {
                                                    nextChar();
                                                    if (ch == '>') {
                                                        nextChar();
                                                        return;
                                                    }
                                                }
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                StringBuilder sb = new StringBuilder();
                while (ch != -1 && ch != '>') {
                    sb.append((char) ch);
                    nextChar();
                }
                Pattern p = Pattern.compile("(?is)doctype\\s+html\\s?.*");
                String s = sb.toString();
                if (p.matcher(s).matches()) {
                    docType(s);
                    return;
                }
            }
        } else if (ch == '?') {
            nextChar();
            if (ch == 'x') {
                nextChar();
                if (ch == 'm') {
                    nextChar();
                    if (ch == 'l') {
                        Map<String,String> attrs = htmlAttrs();
                        if (ch == '?') {
                            nextChar();
                            if (ch == '>') {
                                nextChar();
                                xml = true;
                                return;
                            }
                        }
                    }
                }

            }
        }

        if (!inScript) {
            error(file, lineNumber, "bad html");
        }
    }

    /**
     * Read a series of HTML attributes, terminated by {@literal > }.
     * Each attribute is of the form {@literal identifier[=value] }.
     * "value" may be unquoted, single-quoted, or double-quoted.
     */
    private Map<String,String> htmlAttrs() throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        skipWhitespace();

        loop:
        while (isIdentifierStart((char) ch)) {
            String name = readAttributeName().toLowerCase(Locale.US);
            skipWhitespace();
            String value = null;
            if (ch == '=') {
                nextChar();
                skipWhitespace();
                if (ch == '\'' || ch == '"') {
                    char quote = (char) ch;
                    nextChar();
                    StringBuilder sb = new StringBuilder();
                    while (ch != -1 && ch != quote) {
                        sb.append((char) ch);
                        nextChar();
                    }
                    value = sb.toString() // hack to replace common entities
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&amp;", "&");
                    nextChar();
                } else {
                    StringBuilder sb = new StringBuilder();
                    while (ch != -1 && !isUnquotedAttrValueTerminator((char) ch)) {
                        sb.append((char) ch);
                        nextChar();
                    }
                    value = sb.toString();
                }
                skipWhitespace();
            }
            map.put(name, value);
        }

        return map;
    }

    private boolean isIdentifierStart(char ch) {
        return Character.isUnicodeIdentifierStart(ch);
    }

    private String readIdentifier() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append((char) ch);
        nextChar();
        while (ch != -1 && Character.isUnicodeIdentifierPart(ch)) {
            sb.append((char) ch);
            nextChar();
        }
        return sb.toString();
    }

    private String readAttributeName() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append((char) ch);
        nextChar();
        while (ch != -1 && Character.isUnicodeIdentifierPart(ch)
                || ch == '-'
                || xml && ch == ':') {
            sb.append((char) ch);
            nextChar();
        }
        return sb.toString();
    }

    private boolean isWhitespace(char ch) {
        return Character.isWhitespace(ch);
    }

    private void skipWhitespace() throws IOException {
        while (isWhitespace((char) ch)) {
            nextChar();
        }
    }

    private boolean isUnquotedAttrValueTerminator(char ch) {
        switch (ch) {
            case '\f': case '\n': case '\r': case '\t':
            case ' ':
            case '"': case '\'': case '`':
            case '=': case '<': case '>':
                return true;
            default:
                return false;
        }
    }
}
