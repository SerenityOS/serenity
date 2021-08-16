/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.parser;

import java.util.HashMap;
import java.util.Map;

import com.sun.source.doctree.AttributeTree.ValueKind;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.tree.DCTree;
import com.sun.tools.javac.tree.DCTree.DCAttribute;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import com.sun.tools.javac.tree.DCTree.DCEndPosTree;
import com.sun.tools.javac.tree.DCTree.DCErroneous;
import com.sun.tools.javac.tree.DCTree.DCIdentifier;
import com.sun.tools.javac.tree.DCTree.DCReference;
import com.sun.tools.javac.tree.DCTree.DCText;
import com.sun.tools.javac.tree.DocTreeMaker;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Position;
import com.sun.tools.javac.util.StringUtils;

import static com.sun.tools.javac.util.LayoutCharacters.EOI;

/**
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class DocCommentParser {
    static class ParseException extends Exception {
        private static final long serialVersionUID = 0;
        ParseException(String key) {
            super(key);
        }
    }

    private enum Phase {PREAMBLE, BODY, POSTAMBLE}

    private final ParserFactory fac;
    private final DiagnosticSource diagSource;
    private final Comment comment;
    private final DocTreeMaker m;
    private final Names names;
    private final boolean isFileContent;

    /** The input buffer, index of most recent character read,
     *  index of one past last character in buffer.
     */
    private char[] buf;
    private int bp;
    private int buflen;

    /** The current character.
     */
    private char ch;

    private int textStart = -1;
    private int lastNonWhite = -1;
    private boolean newline = true;

    private final Map<Name, TagParser> tagParsers;

    public DocCommentParser(ParserFactory fac, DiagnosticSource diagSource,
                            Comment comment, boolean isFileContent) {
        this.fac = fac;
        this.diagSource = diagSource;
        this.comment = comment;
        names = fac.names;
        this.isFileContent = isFileContent;
        m = fac.docTreeMaker;
        tagParsers = createTagParsers();
    }

    public DocCommentParser(ParserFactory fac, DiagnosticSource diagSource, Comment comment) {
        this(fac, diagSource, comment, false);
    }

    public DocCommentParser(ParserFactory fac) {
        this(fac, null, null, false);
    }

    public DCDocComment parse() {
        String c = comment.getText();
        buf = new char[c.length() + 1];
        c.getChars(0, c.length(), buf, 0);
        buf[buf.length - 1] = EOI;
        buflen = buf.length - 1;
        bp = -1;
        nextChar();

        List<DCTree> preamble = isFileContent ? blockContent(Phase.PREAMBLE) : List.nil();
        List<DCTree> body = blockContent(Phase.BODY);
        List<DCTree> tags = blockTags();
        List<DCTree> postamble = isFileContent ? blockContent(Phase.POSTAMBLE) : List.nil();

        int pos = Position.NOPOS;
        if (!preamble.isEmpty())
            pos = preamble.head.pos;
        else if (!body.isEmpty())
            pos = body.head.pos;
        else if (!tags.isEmpty())
            pos = tags.head.pos;
        else if (!postamble.isEmpty())
            pos = postamble.head.pos;

        DCDocComment dc = m.at(pos).newDocCommentTree(comment, body, tags, preamble, postamble);
        return dc;
    }

    void nextChar() {
        ch = buf[bp < buflen ? ++bp : buflen];
        switch (ch) {
            case '\f': case '\n': case '\r':
                newline = true;
        }
    }

    protected List<DCTree> blockContent() {
        return blockContent(Phase.BODY);
    }

    /**
     * Read block content, consisting of text, html and inline tags.
     * Terminated by the end of input, or the beginning of the next block tag:
     * that is, @ as the first non-whitespace character on a line.
     */
    @SuppressWarnings("fallthrough")
    protected List<DCTree> blockContent(Phase phase) {
        ListBuffer<DCTree> trees = new ListBuffer<>();
        textStart = -1;

        loop:
        while (bp < buflen) {
            switch (ch) {
                case '\n': case '\r': case '\f':
                    newline = true;
                    // fallthrough

                case ' ': case '\t':
                    nextChar();
                    break;

                case '&':
                    entity(trees);
                    break;

                case '<':
                    newline = false;
                    if (isFileContent) {
                        switch (phase) {
                            case PREAMBLE:
                                if (isEndPreamble()) {
                                    trees.add(html());
                                    if (textStart == -1) {
                                        textStart = bp;
                                        lastNonWhite = -1;
                                    }
                                    // mark this as the start, for processing purposes
                                    newline = true;
                                    break loop;
                                }
                                break;
                            case BODY:
                                if (isEndBody()) {
                                    addPendingText(trees, lastNonWhite);
                                    break loop;
                                }
                                break;
                            default:
                                // fallthrough
                        }
                    }
                    addPendingText(trees, bp - 1);
                    trees.add(html());

                    if (phase == Phase.PREAMBLE || phase == Phase.POSTAMBLE) {
                        break; // Ignore newlines after html tags, in the meta content
                    }
                    if (textStart == -1) {
                        textStart = bp;
                        lastNonWhite = -1;
                    }
                    break;

                case '{':
                    inlineTag(trees);
                    break;

                case '@':
                    if (newline) {
                        addPendingText(trees, lastNonWhite);
                        break loop;
                    }
                    // fallthrough

                default:
                    newline = false;
                    if (textStart == -1)
                        textStart = bp;
                    lastNonWhite = bp;
                    nextChar();
            }
        }

        if (lastNonWhite != -1)
            addPendingText(trees, lastNonWhite);

        return trees.toList();
    }

    /**
     * Read a series of block tags, including their content.
     * Standard tags parse their content appropriately.
     * Non-standard tags are represented by {@link UnknownBlockTagTree}.
     */
    protected List<DCTree> blockTags() {
        ListBuffer<DCTree> tags = new ListBuffer<>();
        while (bp < buflen && ch == '@')
            tags.add(blockTag());
        return tags.toList();
    }

    /**
     * Read a single block tag, including its content.
     * Standard tags parse their content appropriately.
     * Non-standard tags are represented by {@link UnknownBlockTagTree}.
     */
    protected DCTree blockTag() {
        int p = bp;
        try {
            nextChar();
            if (isIdentifierStart(ch)) {
                Name name = readTagName();
                TagParser tp = tagParsers.get(name);
                if (tp == null) {
                    List<DCTree> content = blockContent();
                    return m.at(p).newUnknownBlockTagTree(name, content);
                } else {
                    if (tp.allowsBlock()) {
                        return tp.parse(p, TagParser.Kind.BLOCK);
                    } else {
                        return erroneous("dc.bad.inline.tag", p);
                    }
                }
            }
            blockContent();

            return erroneous("dc.no.tag.name", p);
        } catch (ParseException e) {
            blockContent();
            return erroneous(e.getMessage(), p);
        }
    }

    protected void inlineTag(ListBuffer<DCTree> list) {
        newline = false;
        nextChar();
        if (ch == '@') {
            addPendingText(list, bp - 2);
            list.add(inlineTag());
            textStart = bp;
            lastNonWhite = -1;
        } else {
            if (textStart == -1)
                textStart = bp - 1;
            lastNonWhite = bp;
        }
    }

    /**
     * Read a single inline tag, including its content.
     * Standard tags parse their content appropriately.
     * Non-standard tags are represented by {@link UnknownInlineTagTree}.
     * Malformed tags may be returned as {@link ErroneousTree}.
     */
    protected DCTree inlineTag() {
        int p = bp - 1;
        try {
            nextChar();
            if (!isIdentifierStart(ch)) {
                return erroneous("dc.no.tag.name", p);
            }
            Name name = readTagName();
            TagParser tp = tagParsers.get(name);
            if (tp == null) {
                skipWhitespace();
                DCTree text = inlineText(WhitespaceRetentionPolicy.REMOVE_ALL);
                nextChar();
                return m.at(p).newUnknownInlineTagTree(name, List.of(text)).setEndPos(bp);
            } else {
                if (!tp.retainWhiteSpace) {
                    skipWhitespace();
                }
                if (tp.allowsInline()) {
                    DCEndPosTree<?> tree = (DCEndPosTree<?>) tp.parse(p, TagParser.Kind.INLINE);
                    return tree.setEndPos(bp);
                } else { // handle block tags (for example, @see) in inline content
                    DCTree text = inlineText(WhitespaceRetentionPolicy.REMOVE_ALL); // skip content
                    nextChar();
                    return m.at(p).newUnknownInlineTagTree(name, List.of(text)).setEndPos(bp);
                }
            }
        } catch (ParseException e) {
            return erroneous(e.getMessage(), p);
        }
    }

    private enum WhitespaceRetentionPolicy {
        RETAIN_ALL,
        REMOVE_FIRST_SPACE,
        REMOVE_ALL
    }

    /**
     * Read plain text content of an inline tag.
     * Matching pairs of { } are skipped; the text is terminated by the first
     * unmatched }. It is an error if the beginning of the next tag is detected.
     */
    private DCText inlineText(WhitespaceRetentionPolicy whitespacePolicy) throws ParseException {
        switch (whitespacePolicy) {
            case REMOVE_ALL:
                skipWhitespace();
                break;
            case REMOVE_FIRST_SPACE:
                if (ch == ' ')
                    nextChar();
                break;
            case RETAIN_ALL:
            default:
                // do nothing
                break;

        }
        int pos = bp;
        int depth = 1;

        loop:
        while (bp < buflen) {
            switch (ch) {
                case '\n': case '\r': case '\f':
                    newline = true;
                    break;

                case ' ': case '\t':
                    break;

                case '{':
                    newline = false;
                    lastNonWhite = bp;
                    depth++;
                    break;

                case '}':
                    if (--depth == 0) {
                        return m.at(pos).newTextTree(newString(pos, bp));
                    }
                    newline = false;
                    lastNonWhite = bp;
                    break;

                default:
                    newline = false;
                    lastNonWhite = bp;
                    break;
            }
            nextChar();
        }
        throw new ParseException("dc.unterminated.inline.tag");
    }

    /**
     * Read Java class name, possibly followed by member
     * Matching pairs of {@literal < >} are skipped. The text is terminated by the first
     * unmatched }. It is an error if the beginning of the next tag is detected.
     */
    // TODO: allowMember is currently ignored
    // TODO: boolean allowMember should be enum FORBID, ALLOW, REQUIRE
    // TODO: improve quality of parse to forbid bad constructions.
    @SuppressWarnings("fallthrough")
    protected DCReference reference(boolean allowMember) throws ParseException {
        int pos = bp;
        int depth = 0;

        // scan to find the end of the signature, by looking for the first
        // whitespace not enclosed in () or <>, or the end of the tag
        loop:
        while (bp < buflen) {
            switch (ch) {
                case '\n': case '\r': case '\f':
                    newline = true;
                    // fallthrough

                case ' ': case '\t':
                    if (depth == 0)
                        break loop;
                    break;

                case '(':
                case '<':
                    newline = false;
                    depth++;
                    break;

                case ')':
                case '>':
                    newline = false;
                    --depth;
                    break;

                case '}':
                    if (bp == pos)
                        return null;
                    newline = false;
                    break loop;

                case '@':
                    if (newline)
                        break loop;
                    // fallthrough

                default:
                    newline = false;

            }
            nextChar();
        }

        // depth < 0 will be caught and reported by ReferenceParser#parse
        if (depth > 0)
            throw new ParseException("dc.unterminated.signature");

        String sig = newString(pos, bp);


        try {
            ReferenceParser.Reference ref = new ReferenceParser(fac).parse(sig);
            return m.at(pos).newReferenceTree(sig,
                    ref.moduleName, ref.qualExpr,
                    ref.member, ref.paramTypes)
                    .setEndPos(bp);
        } catch (ReferenceParser.ParseException parseException) {
            throw new ParseException(parseException.getMessage());
        }

    }

    /**
     * Read Java identifier
     * Matching pairs of { } are skipped; the text is terminated by the first
     * unmatched }. It is an error if the beginning of the next tag is detected.
     */
    @SuppressWarnings("fallthrough")
    protected DCIdentifier identifier() throws ParseException {
        skipWhitespace();
        int pos = bp;

        if (isJavaIdentifierStart(ch)) {
            Name name = readJavaIdentifier();
            return m.at(pos).newIdentifierTree(name);
        }

        throw new ParseException("dc.identifier.expected");
    }

    /**
     * Read a quoted string.
     * It is an error if the beginning of the next tag is detected.
     */
    @SuppressWarnings("fallthrough")
    protected DCText quotedString() {
        int pos = bp;
        nextChar();

        loop:
        while (bp < buflen) {
            switch (ch) {
                case '\n': case '\r': case '\f':
                    newline = true;
                    break;

                case ' ': case '\t':
                    break;

                case '"':
                    nextChar();
                    // trim trailing white-space?
                    return m.at(pos).newTextTree(newString(pos, bp));

                case '@':
                    if (newline)
                        break loop;

            }
            nextChar();
        }
        return null;
    }

    /**
     * Read a term (that is, one word).
     * It is an error if the beginning of the next tag is detected.
     */
    @SuppressWarnings("fallthrough")
    protected DCText inlineWord() {
        int pos = bp;
        int depth = 0;
        loop:
        while (bp < buflen) {
            switch (ch) {
                case '\n':
                    newline = true;
                    // fallthrough

                case '\r': case '\f': case ' ': case '\t':
                    return m.at(pos).newTextTree(newString(pos, bp));

                case '@':
                    if (newline)
                        break loop;

                case '{':
                    depth++;
                    break;

                case '}':
                    if (depth == 0 || --depth == 0)
                        return m.at(pos).newTextTree(newString(pos, bp));
                    break;
            }
            newline = false;
            nextChar();
        }
        return null;
    }

    /**
     * Read general text content of an inline tag, including HTML entities and elements.
     * Matching pairs of { } are skipped; the text is terminated by the first
     * unmatched }. It is an error if the beginning of the next tag is detected.
     */
    @SuppressWarnings("fallthrough")
    private List<DCTree> inlineContent() {
        ListBuffer<DCTree> trees = new ListBuffer<>();

        skipWhitespace();
        int pos = bp;
        int depth = 1;
        textStart = -1;

        loop:
        while (bp < buflen) {

            switch (ch) {
                case '\n': case '\r': case '\f':
                    newline = true;
                    // fall through

                case ' ': case '\t':
                    nextChar();
                    break;

                case '&':
                    entity(trees);
                    break;

                case '<':
                    newline = false;
                    addPendingText(trees, bp - 1);
                    trees.add(html());
                    textStart = bp;
                    lastNonWhite = -1;
                    break;

                case '{':
                    if (textStart == -1)
                        textStart = bp;
                    newline = false;
                    nextChar();
                    if (ch == '@') {
                        addPendingText(trees, bp - 2);
                        trees.add(inlineTag());
                        textStart = bp;
                        lastNonWhite = -1;
                    } else {
                        depth++;
                    }
                    break;

                case '}':
                    newline = false;
                    if (--depth == 0) {
                        addPendingText(trees, bp - 1);
                        nextChar();
                        return trees.toList();
                    }
                    nextChar();
                    break;

                case '@':
                    if (newline)
                        break loop;
                    // fallthrough

                default:
                    if (textStart == -1)
                        textStart = bp;
                    nextChar();
                    break;
            }
        }

        return List.of(erroneous("dc.unterminated.inline.tag", pos));
    }

    protected void entity(ListBuffer<DCTree> list) {
        newline = false;
        addPendingText(list, bp - 1);
        list.add(entity());
        if (textStart == -1) {
            textStart = bp;
            lastNonWhite = -1;
        }
    }

    /**
     * Read an HTML entity.
     * {@literal &identifier; } or {@literal &#digits; } or {@literal &#xhex-digits; }
     */
    protected DCTree entity() {
        int p = bp;
        nextChar();
        Name name = null;
        if (ch == '#') {
            int namep = bp;
            nextChar();
            if (isDecimalDigit(ch)) {
                nextChar();
                while (bp < buflen && isDecimalDigit(ch))
                    nextChar();
                name = names.fromChars(buf, namep, bp - namep);
            } else if (ch == 'x' || ch == 'X') {
                nextChar();
                if (isHexDigit(ch)) {
                    nextChar();
                    while (bp < buflen && isHexDigit(ch))
                        nextChar();
                    name = names.fromChars(buf, namep, bp - namep);
                }
            }
        } else if (isIdentifierStart(ch)) {
            name = readIdentifier();
        }

        if (name == null)
            return erroneous("dc.bad.entity", p);
        else {
            if (ch != ';')
                return erroneous("dc.missing.semicolon", p);
            nextChar();
            return m.at(p).newEntityTree(name);
        }
    }

    /**
     * Returns whether this is the end of the preamble of an HTML file.
     * The preamble ends with start of {@code body} element followed by
     * possible whitespace and the start of a {@code main} element.
     *
     * @return whether this is the end of the preamble
     */
    boolean isEndPreamble() {
        final int savedpos = bp;
        try {
            if (ch == '<')
                nextChar();

            if (isIdentifierStart(ch)) {
                String name = StringUtils.toLowerCase(readIdentifier().toString());
                switch (name) {
                    case "body":
                        // Check if also followed by <main>
                        // 1. skip rest of <body>
                        while (bp < buflen && ch != '>') {
                            nextChar();
                        }
                        if (ch == '>') {
                            nextChar();
                        }
                        // 2. skip any whitespace
                        while (bp < buflen && isWhitespace(ch)) {
                            nextChar();
                        }
                        // 3. check if looking at "<main..."
                        if (ch == '<') {
                            nextChar();
                            if (isIdentifierStart(ch)) {
                                name = StringUtils.toLowerCase(readIdentifier().toString());
                                if (name.equals("main")) {
                                    return false;
                                }
                            }
                        }
                        // if <body> is _not_ followed by <main> then this is the
                        // end of the preamble
                        return true;

                    case "main":
                        // <main> is unconditionally the end of the preamble
                        return true;
                }
            }
            return false;
        } finally {
            bp = savedpos;
            ch = buf[bp];
        }
    }

    /**
     * Returns whether this is the end of the main body of the content in a standalone
     * HTML file.
     * The content ends with the closing tag for a {@code main} or {@code body} element.
     *
     * @return whether this is the end of the main body of the content
     */
    boolean isEndBody() {
        final int savedpos = bp;
        try {
            if (ch == '<')
                nextChar();

            if (ch == '/') {
                nextChar();
                if (isIdentifierStart(ch)) {
                    String name = StringUtils.toLowerCase(readIdentifier().toString());
                    switch (name) {
                        case "body":
                        case "main":
                            return true;
                    }
                }
            }

            return false;
        } finally {
            bp = savedpos;
            ch = buf[bp];
        }

    }

    boolean peek(String s) {
        final int savedpos = bp;
        try {
            if (ch == '<')
                nextChar();

            if (ch == '/') {
                if (s.charAt(0) != ch) {
                    return false;
                } else {
                    s = s.substring(1);
                    nextChar();
                }
            }

            if (isIdentifierStart(ch)) {
                Name name = readIdentifier();
                return StringUtils.toLowerCase(name.toString()).equals(s);
            }
            return false;
        } finally {
            bp = savedpos;
            ch = buf[bp];
        }
    }

    /**
     * Read the start or end of an HTML tag, or an HTML comment
     * {@literal <identifier attrs> } or {@literal </identifier> }
     */
    private DCTree html() {
        int p = bp;
        nextChar();
        if (isIdentifierStart(ch)) {
            Name name = readIdentifier();
            List<DCTree> attrs = htmlAttrs();
            if (attrs != null) {
                boolean selfClosing = false;
                if (ch == '/') {
                    nextChar();
                    selfClosing = true;
                }
                if (ch == '>') {
                    nextChar();
                    DCTree dctree = m.at(p).newStartElementTree(name, attrs, selfClosing).setEndPos(bp);
                    return dctree;
                }
            }
        } else if (ch == '/') {
            nextChar();
            if (isIdentifierStart(ch)) {
                Name name = readIdentifier();
                skipWhitespace();
                if (ch == '>') {
                    nextChar();
                    return m.at(p).newEndElementTree(name).setEndPos(bp);
                }
            }
        } else if (ch == '!') {
            nextChar();
            if (ch == '-') {
                nextChar();
                if (ch == '-') {
                    nextChar();
                    while (bp < buflen) {
                        int dash = 0;
                        while (bp < buflen && ch == '-') {
                            dash++;
                            nextChar();
                        }
                        // Strictly speaking, a comment should not contain "--"
                        // so dash > 2 is an error, dash == 2 implies ch == '>'
                        // See http://www.w3.org/TR/html-markup/syntax.html#syntax-comments
                        // for more details.
                        if (dash >= 2 && ch == '>') {
                            nextChar();
                            return m.at(p).newCommentTree(newString(p, bp));
                        }

                        nextChar();
                    }
                }
            } else if (isIdentifierStart(ch) && peek("doctype")) {
                readIdentifier();
                nextChar();
                skipWhitespace();
                int d = bp;
                while (bp < buflen) {
                    if (ch == '>') {
                        int mark = bp;
                        nextChar();
                        return m.at(d).newDocTypeTree(newString(d, mark));
                    }
                    nextChar();
                }
            }
        }

        bp = p + 1;
        ch = buf[bp];
        return erroneous("dc.malformed.html", p);
    }

    /**
     * Read a series of HTML attributes, terminated by {@literal > }.
     * Each attribute is of the form {@literal identifier[=value] }.
     * "value" may be unquoted, single-quoted, or double-quoted.
     */
    protected List<DCTree> htmlAttrs() {
        ListBuffer<DCTree> attrs = new ListBuffer<>();
        skipWhitespace();

        loop:
        while (bp < buflen && isIdentifierStart(ch)) {
            int namePos = bp;
            Name name = readAttributeName();
            skipWhitespace();
            List<DCTree> value = null;
            ValueKind vkind = ValueKind.EMPTY;
            if (ch == '=') {
                ListBuffer<DCTree> v = new ListBuffer<>();
                nextChar();
                skipWhitespace();
                if (ch == '\'' || ch == '"') {
                    newline = false;
                    vkind = (ch == '\'') ? ValueKind.SINGLE : ValueKind.DOUBLE;
                    char quote = ch;
                    nextChar();
                    textStart = bp;
                    while (bp < buflen && ch != quote) {
                        if (newline && ch == '@') {
                            attrs.add(erroneous("dc.unterminated.string", namePos));
                            // No point trying to read more.
                            // In fact, all attrs get discarded by the caller
                            // and superseded by a malformed.html node because
                            // the html tag itself is not terminated correctly.
                            break loop;
                        }
                        attrValueChar(v);
                    }
                    addPendingText(v, bp - 1);
                    nextChar();
                } else {
                    vkind = ValueKind.UNQUOTED;
                    textStart = bp;
                    while (bp < buflen && !isUnquotedAttrValueTerminator(ch)) {
                        attrValueChar(v);
                    }
                    addPendingText(v, bp - 1);
                }
                skipWhitespace();
                value = v.toList();
            }
            DCAttribute attr = m.at(namePos).newAttributeTree(name, vkind, value);
            attrs.add(attr);
        }

        return attrs.toList();
    }

    protected void attrValueChar(ListBuffer<DCTree> list) {
        switch (ch) {
            case '&':
                entity(list);
                break;

            case '{':
                inlineTag(list);
                break;

            default:
                nextChar();
        }
    }

    protected void addPendingText(ListBuffer<DCTree> list, int textEnd) {
        if (textStart != -1) {
            if (textStart <= textEnd) {
                list.add(m.at(textStart).newTextTree(newString(textStart, textEnd + 1)));
            }
            textStart = -1;
        }
    }

    protected DCErroneous erroneous(String code, int pos) {
        int i = bp - 1;
        loop:
        while (i > pos) {
            switch (buf[i]) {
                case '\f': case '\n': case '\r':
                    newline = true;
                    break;
                case '\t': case ' ':
                    break;
                default:
                    break loop;
            }
            i--;
        }
        textStart = -1;
        return m.at(pos).newErroneousTree(newString(pos, i + 1), diagSource, code);
    }

    protected boolean isIdentifierStart(char ch) {
        return Character.isUnicodeIdentifierStart(ch);
    }

    protected Name readIdentifier() {
        int start = bp;
        nextChar();
        while (bp < buflen && Character.isUnicodeIdentifierPart(ch))
            nextChar();
        return names.fromChars(buf, start, bp - start);
    }

    protected Name readAttributeName() {
        int start = bp;
        nextChar();
        while (bp < buflen && (Character.isUnicodeIdentifierPart(ch) || ch == '-'))
            nextChar();
        return names.fromChars(buf, start, bp - start);
    }

    protected Name readTagName() {
        int start = bp;
        nextChar();
        while (bp < buflen
                && (Character.isUnicodeIdentifierPart(ch) || ch == '.'
                || ch == '-' || ch == ':')) {
            nextChar();
        }
        return names.fromChars(buf, start, bp - start);
    }

    protected boolean isJavaIdentifierStart(char ch) {
        return Character.isJavaIdentifierStart(ch);
    }

    protected Name readJavaIdentifier() {
        int start = bp;
        nextChar();
        while (bp < buflen && Character.isJavaIdentifierPart(ch))
            nextChar();
        return names.fromChars(buf, start, bp - start);
    }

    protected Name readSystemPropertyName() {
        int pos = bp;
        nextChar();
        while (bp < buflen && Character.isUnicodeIdentifierPart(ch) || ch == '.')
            nextChar();
        return names.fromChars(buf, pos, bp - pos);
    }

    protected boolean isDecimalDigit(char ch) {
        return ('0' <= ch && ch <= '9');
    }

    protected boolean isHexDigit(char ch) {
        return ('0' <= ch && ch <= '9')
                || ('a' <= ch && ch <= 'f')
                || ('A' <= ch && ch <= 'F');
    }

    protected boolean isUnquotedAttrValueTerminator(char ch) {
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

    protected boolean isWhitespace(char ch) {
        return Character.isWhitespace(ch);
    }

    protected void skipWhitespace() {
        while (bp < buflen && isWhitespace(ch)) {
            nextChar();
        }
    }

    /**
     * @param start position of first character of string
     * @param end position of character beyond last character to be included
     */
    String newString(int start, int end) {
        return new String(buf, start, end - start);
    }

    private static abstract class TagParser {
        enum Kind { INLINE, BLOCK, EITHER }

        final Kind kind;
        final DCTree.Kind treeKind;
        final boolean retainWhiteSpace;

        TagParser(Kind k, DCTree.Kind tk) {
            kind = k;
            treeKind = tk;
            retainWhiteSpace = false;
        }

        TagParser(Kind k, DCTree.Kind tk, boolean retainWhiteSpace) {
            kind = k;
            treeKind = tk;
            this.retainWhiteSpace = retainWhiteSpace;
        }

        boolean allowsBlock() {
            return kind != Kind.INLINE;
        }

        boolean allowsInline() {
            return kind != Kind.BLOCK;
        }

        DCTree.Kind getTreeKind() {
            return treeKind;
        }

        DCTree parse(int pos, Kind kind) throws ParseException {
            if (kind != this.kind && this.kind != Kind.EITHER) {
                throw new IllegalArgumentException(kind.toString());
            }
            return parse(pos);
        }

        DCTree parse(int pos) throws ParseException {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @see <a href="https://docs.oracle.com/en/java/javase/15/docs/specs/javadoc/doc-comment-spec.html">JavaDoc Tags</a>
     */
    private Map<Name, TagParser> createTagParsers() {
        TagParser[] parsers = {
            // @author name-text
            new TagParser(TagParser.Kind.BLOCK, DCTree.Kind.AUTHOR) {
                @Override
                public DCTree parse(int pos) {
                    List<DCTree> name = blockContent();
                    return m.at(pos).newAuthorTree(name);
                }
            },

            // {@code text}
            new TagParser(TagParser.Kind.INLINE, DCTree.Kind.CODE, true) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    DCText text = inlineText(WhitespaceRetentionPolicy.REMOVE_FIRST_SPACE);
                    nextChar();
                    return m.at(pos).newCodeTree(text);
                }
            },

            // @deprecated deprecated-text
            new TagParser(TagParser.Kind.BLOCK, DCTree.Kind.DEPRECATED) {
                @Override
                public DCTree parse(int pos) {
                    List<DCTree> reason = blockContent();
                    return m.at(pos).newDeprecatedTree(reason);
                }
            },

            // {@docRoot}
            new TagParser(TagParser.Kind.INLINE, DCTree.Kind.DOC_ROOT) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    if (ch == '}') {
                        nextChar();
                        return m.at(pos).newDocRootTree();
                    }
                    inlineText(WhitespaceRetentionPolicy.REMOVE_ALL); // skip unexpected content
                    nextChar();
                    throw new ParseException("dc.unexpected.content");
                }
            },

            // @exception class-name description
            new TagParser(TagParser.Kind.BLOCK, DCTree.Kind.EXCEPTION) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    skipWhitespace();
                    DCReference ref = reference(false);
                    List<DCTree> description = blockContent();
                    return m.at(pos).newExceptionTree(ref, description);
                }
            },

            // @hidden hidden-text
            new TagParser(TagParser.Kind.BLOCK, DCTree.Kind.HIDDEN) {
                @Override
                public DCTree parse(int pos) {
                    List<DCTree> reason = blockContent();
                    return m.at(pos).newHiddenTree(reason);
                }
            },

            // {@index search-term options-description}
            new TagParser(TagParser.Kind.INLINE, DCTree.Kind.INDEX) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    skipWhitespace();
                    if (ch == '}') {
                        throw new ParseException("dc.no.content");
                    }
                    DCTree term = ch == '"' ? quotedString() : inlineWord();
                    if (term == null) {
                        throw new ParseException("dc.no.content");
                    }
                    skipWhitespace();
                    List<DCTree> description = List.nil();
                    if (ch != '}') {
                        description = inlineContent();
                    } else {
                        nextChar();
                    }
                    return m.at(pos).newIndexTree(term, description);
                }
            },

            // {@inheritDoc}
            new TagParser(TagParser.Kind.INLINE, DCTree.Kind.INHERIT_DOC) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    if (ch == '}') {
                        nextChar();
                        return m.at(pos).newInheritDocTree();
                    }
                    inlineText(WhitespaceRetentionPolicy.REMOVE_ALL); // skip unexpected content
                    nextChar();
                    throw new ParseException("dc.unexpected.content");
                }
            },

            // {@link package.class#member label}
            new TagParser(TagParser.Kind.INLINE, DCTree.Kind.LINK) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    DCReference ref = reference(true);
                    List<DCTree> label = inlineContent();
                    return m.at(pos).newLinkTree(ref, label);
                }
            },

            // {@linkplain package.class#member label}
            new TagParser(TagParser.Kind.INLINE, DCTree.Kind.LINK_PLAIN) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    DCReference ref = reference(true);
                    List<DCTree> label = inlineContent();
                    return m.at(pos).newLinkPlainTree(ref, label);
                }
            },

            // {@literal text}
            new TagParser(TagParser.Kind.INLINE, DCTree.Kind.LITERAL, true) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    DCText text = inlineText(WhitespaceRetentionPolicy.REMOVE_FIRST_SPACE);
                    nextChar();
                    return m.at(pos).newLiteralTree(text);
                }
            },

            // @param parameter-name description
            new TagParser(TagParser.Kind.BLOCK, DCTree.Kind.PARAM) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    skipWhitespace();

                    boolean typaram = false;
                    if (ch == '<') {
                        typaram = true;
                        nextChar();
                    }

                    DCIdentifier id = identifier();

                    if (typaram) {
                        if (ch != '>')
                            throw new ParseException("dc.gt.expected");
                        nextChar();
                    }

                    skipWhitespace();
                    List<DCTree> desc = blockContent();
                    return m.at(pos).newParamTree(typaram, id, desc);
                }
            },

            // @provides service-name description
            new TagParser(TagParser.Kind.BLOCK, DCTree.Kind.PROVIDES) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    skipWhitespace();
                    DCReference ref = reference(true);
                    List<DCTree> description = blockContent();
                    return m.at(pos).newProvidesTree(ref, description);
                }
            },

            // @return description  -or-  {@return description}
            new TagParser(TagParser.Kind.EITHER, DCTree.Kind.RETURN) {
                @Override
                public DCTree parse(int pos, Kind kind) {
                    List<DCTree> description;
                    switch (kind) {
                        case BLOCK:
                            description = blockContent();
                            break;
                        case INLINE:
                            description = inlineContent();
                            break;
                        default:
                            throw new IllegalArgumentException(kind.toString());
                    }
                    return m.at(pos).newReturnTree(kind == Kind.INLINE, description);
                }
            },

            // @see reference | quoted-string | HTML
            new TagParser(TagParser.Kind.BLOCK, DCTree.Kind.SEE) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    skipWhitespace();
                    switch (ch) {
                        case '"':
                            DCText string = quotedString();
                            if (string != null) {
                                skipWhitespace();
                                if (ch == '@'
                                        || ch == EOI && bp == buf.length - 1) {
                                    return m.at(pos).newSeeTree(List.<DCTree>of(string));
                                }
                            }
                            break;

                        case '<':
                            List<DCTree> html = blockContent();
                            if (html != null)
                                return m.at(pos).newSeeTree(html);
                            break;

                        case '@':
                            if (newline)
                                throw new ParseException("dc.no.content");
                            break;

                        case EOI:
                            if (bp == buf.length - 1)
                                throw new ParseException("dc.no.content");
                            break;

                        default:
                            if (isJavaIdentifierStart(ch) || ch == '#') {
                                DCReference ref = reference(true);
                                List<DCTree> description = blockContent();
                                return m.at(pos).newSeeTree(description.prepend(ref));
                            }
                    }
                    throw new ParseException("dc.unexpected.content");
                }
            },

            // @serialData data-description
            new TagParser(TagParser.Kind.BLOCK, DCTree.Kind.SERIAL_DATA) {
                @Override
                public DCTree parse(int pos) {
                    List<DCTree> description = blockContent();
                    return m.at(pos).newSerialDataTree(description);
                }
            },

            // @serialField field-name field-type description
            new TagParser(TagParser.Kind.BLOCK, DCTree.Kind.SERIAL_FIELD) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    skipWhitespace();
                    DCIdentifier name = identifier();
                    skipWhitespace();
                    DCReference type = reference(false);
                    List<DCTree> description = null;
                    if (isWhitespace(ch)) {
                        skipWhitespace();
                        description = blockContent();
                    }
                    return m.at(pos).newSerialFieldTree(name, type, description);
                }
            },

            // @serial field-description | include | exclude
            new TagParser(TagParser.Kind.BLOCK, DCTree.Kind.SERIAL) {
                @Override
                public DCTree parse(int pos) {
                    List<DCTree> description = blockContent();
                    return m.at(pos).newSerialTree(description);
                }
            },

            // @since since-text
            new TagParser(TagParser.Kind.BLOCK, DCTree.Kind.SINCE) {
                @Override
                public DCTree parse(int pos) {
                    List<DCTree> description = blockContent();
                    return m.at(pos).newSinceTree(description);
                }
            },

            // {@summary summary-text}
            new TagParser(TagParser.Kind.INLINE, DCTree.Kind.SUMMARY) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    List<DCTree> summary = inlineContent();
                    return m.at(pos).newSummaryTree(summary);
                }
            },

            // {@systemProperty property-name}
            new TagParser(TagParser.Kind.INLINE, DCTree.Kind.SYSTEM_PROPERTY) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    skipWhitespace();
                    if (ch == '}') {
                        throw new ParseException("dc.no.content");
                    }
                    Name propertyName = readSystemPropertyName();
                    if (propertyName == null) {
                        throw new ParseException("dc.no.content");
                    }
                    skipWhitespace();
                    if (ch != '}') {
                        nextChar();
                        throw new ParseException("dc.unexpected.content");
                    } else {
                        nextChar();
                        return m.at(pos).newSystemPropertyTree(propertyName);
                    }
                }
            },

            // @throws class-name description
            new TagParser(TagParser.Kind.BLOCK, DCTree.Kind.THROWS) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    skipWhitespace();
                    DCReference ref = reference(false);
                    List<DCTree> description = blockContent();
                    return m.at(pos).newThrowsTree(ref, description);
                }
            },

            // @uses service-name description
            new TagParser(TagParser.Kind.BLOCK, DCTree.Kind.USES) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    skipWhitespace();
                    DCReference ref = reference(true);
                    List<DCTree> description = blockContent();
                    return m.at(pos).newUsesTree(ref, description);
                }
            },

            // {@value package.class#field}
            new TagParser(TagParser.Kind.INLINE, DCTree.Kind.VALUE) {
                @Override
                public DCTree parse(int pos) throws ParseException {
                    DCReference ref = reference(true);
                    skipWhitespace();
                    if (ch == '}') {
                        nextChar();
                        return m.at(pos).newValueTree(ref);
                    }
                    nextChar();
                    throw new ParseException("dc.unexpected.content");
                }
            },

            // @version version-text
            new TagParser(TagParser.Kind.BLOCK, DCTree.Kind.VERSION) {
                @Override
                public DCTree parse(int pos) {
                    List<DCTree> description = blockContent();
                    return m.at(pos).newVersionTree(description);
                }
            },
        };

        Map<Name, TagParser> tagParsers = new HashMap<>();
        for (TagParser p: parsers)
            tagParsers.put(names.fromString(p.getTreeKind().tagName), p);

        return tagParsers;
    }

}
