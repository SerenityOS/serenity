/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.shellsupport.doc;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Stack;

import javax.lang.model.element.Name;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.InlineTagTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.StringUtils;

/**A javadoc to plain text formatter.
 *
 */
public class JavadocFormatter {

    private static final String CODE_RESET = "\033[0m";
    private static final String CODE_HIGHLIGHT = "\033[1m";
    private static final String CODE_UNDERLINE = "\033[4m";

    private final int lineLimit;
    private final boolean escapeSequencesSupported;

    /** Construct the formatter.
     *
     * @param lineLimit maximum line length
     * @param escapeSequencesSupported whether escape sequences are supported
     */
    public JavadocFormatter(int lineLimit, boolean escapeSequencesSupported) {
        this.lineLimit = lineLimit;
        this.escapeSequencesSupported = escapeSequencesSupported;
    }

    private static final int MAX_LINE_LENGTH = 95;
    private static final int SHORTEST_LINE = 30;
    private static final int INDENT = 4;

    /**Format javadoc to plain text.
     *
     * @param header element caption that should be used
     * @param javadoc to format
     * @return javadoc formatted to plain text
     */
    public String formatJavadoc(String header, String javadoc) {
        try {
            StringBuilder result = new StringBuilder();

            result.append(escape(CODE_HIGHLIGHT)).append(header).append(escape(CODE_RESET)).append("\n");

            if (javadoc == null) {
                return result.toString();
            }

            JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, null, null, null, null, null);
            DocTrees trees = DocTrees.instance(task);
            DocCommentTree docComment = trees.getDocCommentTree(new SimpleJavaFileObject(new URI("mem://doc.html"), Kind.HTML) {
                @Override @DefinedBy(Api.COMPILER)
                public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                    return "<body>" + javadoc + "</body>";
                }
            });

            new FormatJavadocScanner(result, task).scan(docComment, null);

            addNewLineIfNeeded(result);

            return result.toString();
        } catch (URISyntaxException ex) {
            throw new InternalError("Unexpected exception", ex);
        }
    }

    enum HtmlTag {
        HTML,
        H1, H2, H3, H4, H5, H6,
        BLOCKQUOTE, P, PRE,
        IMG,
        OL, UL, LI,
        DL, DT, DD,
        TABLE, TR, TD, TH;

        private static final Map<String, HtmlTag> index = new HashMap<>();
        static {
            for (HtmlTag t: values()) {
                index.put(StringUtils.toLowerCase(t.name()), t);
            }
        }

        public static HtmlTag get(Name tagName) {
            return index.get(StringUtils.toLowerCase(tagName.toString()));
        }
    }

    private class FormatJavadocScanner extends DocTreeScanner<Object, Object> {
        private final StringBuilder result;
        private final JavacTask task;
        private final DocTrees trees;
        private int reflownTo;
        private int indent;
        private int limit = Math.min(lineLimit, MAX_LINE_LENGTH);
        private boolean pre;
        private Map<StartElementTree, Integer> tableColumns;

        public FormatJavadocScanner(StringBuilder result, JavacTask task) {
            this.result = result;
            this.task = task;
            this.trees = DocTrees.instance(task);
        }

        @Override @DefinedBy(Api.COMPILER_TREE)
        public Object visitDocComment(DocCommentTree node, Object p) {
            tableColumns = countTableColumns(node);
            reflownTo = result.length();
            scan(node.getFirstSentence(), p);
            scan(node.getBody(), p);
            reflow(result, reflownTo, indent, limit);
            for (Sections current : docSections.keySet()) {
                boolean seenAny = false;
                for (DocTree t : node.getBlockTags()) {
                    if (current.matches(t)) {
                        if (!seenAny) {
                            seenAny = true;
                            startSection(current);
                        }

                        scan(t, null);
                    }
                }
                if (current == Sections.RETURNS && !seenAny) {
                    List<? extends DocTree> firstSentence = node.getFirstSentence();
                    if (firstSentence.size() == 1
                            && firstSentence.get(0).getKind() == DocTree.Kind.RETURN) {
                        startSection(current);
                        scan(firstSentence.get(0), true);
                    }
                }
            }
            return null;
        }

        private void startSection(Sections current) {
            if (result.charAt(result.length() - 1) != '\n')
                result.append("\n");
            result.append("\n");
            result.append(escape(CODE_UNDERLINE))
                    .append(docSections.get(current))
                    .append(escape(CODE_RESET))
                    .append("\n");

        }

        @Override @DefinedBy(Api.COMPILER_TREE)
        public Object visitText(TextTree node, Object p) {
            String text = node.getBody();
            if (!pre) {
                text = text.replaceAll("[ \t\r\n]+", " ").trim();
                if (text.isEmpty()) {
                    text = " ";
                }
            } else {
                text = text.replaceAll("\n", "\n" + indentString(indent));
            }
            result.append(text);
            return null;
        }

        @Override @DefinedBy(Api.COMPILER_TREE)
        public Object visitLink(LinkTree node, Object p) {
            if (!node.getLabel().isEmpty()) {
                scan(node.getLabel(), p);
            } else {
                result.append(node.getReference().getSignature());
            }
            return null;
        }

        @Override @DefinedBy(Api.COMPILER_TREE)
        public Object visitParam(ParamTree node, Object p) {
            return formatDef(node.getName().getName(), node.getDescription());
        }

        @Override @DefinedBy(Api.COMPILER_TREE)
        public Object visitThrows(ThrowsTree node, Object p) {
            return formatDef(node.getExceptionName().getSignature(), node.getDescription());
        }

        public Object formatDef(CharSequence name, List<? extends DocTree> description) {
            result.append(name);
            result.append(" - ");
            reflownTo = result.length();
            indent = name.length() + 3;

            if (limit - indent < SHORTEST_LINE) {
                result.append("\n");
                result.append(indentString(INDENT));
                indent = INDENT;
                reflownTo += INDENT;
            }
            try {
                return scan(description, null);
            } finally {
                reflow(result, reflownTo, indent, limit);
                result.append("\n");
            }
        }

        @Override @DefinedBy(Api.COMPILER_TREE)
        public Object visitLiteral(LiteralTree node, Object p) {
            return scan(node.getBody(), p);
        }

        /**
         * {@inheritDoc}
         * {@code @return} is a bimodal tag and can be used as either a block tag or an inline
         * tag. If the parameter {@code p} is {@code null}, the node will be formatted according to
         * the value of {@link ReturnTree#isInline()}. If the parameter is not {@code null}, the node will
         * be formatted as a block tag.
         * @param node  {@inheritDoc}
         * @param p     not {@code null} to force the node to be formatted as a block tag
         * @return
         */
        @Override @DefinedBy(Api.COMPILER_TREE)
        public Object visitReturn(ReturnTree node, Object p) {
            if (node.isInline() && p == null) {
                String MARKER = "{0}";
                int p0 = inlineReturns.indexOf(MARKER);
                result.append(inlineReturns, 0, p0);
                try {
                    return super.visitReturn(node, p);
                } finally {
                    result.append(inlineReturns.substring(p0 + MARKER.length()));
                }
            } else {
                reflownTo = result.length();
                try {
                    return super.visitReturn(node, p);
                } finally {
                    reflow(result, reflownTo, 0, limit);
                }
            }
        }

        Stack<Integer> listStack = new Stack<>();
        Stack<Integer> defStack = new Stack<>();
        Stack<Integer> tableStack = new Stack<>();
        Stack<List<Integer>> cellsStack = new Stack<>();
        Stack<List<Boolean>> headerStack = new Stack<>();

        @Override @DefinedBy(Api.COMPILER_TREE)
        public Object visitStartElement(StartElementTree node, Object p) {
            switch (getHtmlTag(node.getName())) {
                case P:
                    if (lastNode!= null && lastNode.getKind() == DocTree.Kind.START_ELEMENT &&
                        HtmlTag.get(((StartElementTree) lastNode).getName()) == HtmlTag.LI) {
                        //ignore
                        break;
                    }
                    reflowTillNow();
                    addNewLineIfNeeded(result);
                    result.append(indentString(indent));
                    reflownTo = result.length();
                    break;
                case BLOCKQUOTE:
                    reflowTillNow();
                    indent += INDENT;
                    break;
                case PRE:
                    reflowTillNow();
                    pre = true;
                    break;
                case UL:
                    reflowTillNow();
                    listStack.push(-1);
                    indent += INDENT;
                    break;
                case OL:
                    reflowTillNow();
                    listStack.push(1);
                    indent += INDENT;
                    break;
                case DL:
                    reflowTillNow();
                    defStack.push(indent);
                    break;
                case LI:
                    reflowTillNow();
                    if (!listStack.empty()) {
                        addNewLineIfNeeded(result);

                        int top = listStack.pop();

                        if (top == (-1)) {
                            result.append(indentString(indent - 2));
                            result.append("* ");
                        } else {
                            result.append(indentString(indent - 3));
                            result.append("" + top++ + ". ");
                        }

                        listStack.push(top);

                        reflownTo = result.length();
                    }
                    break;
                case DT:
                    reflowTillNow();
                    if (!defStack.isEmpty()) {
                        addNewLineIfNeeded(result);
                        indent = defStack.peek();
                        result.append(escape(CODE_HIGHLIGHT));
                    }
                    break;
                case DD:
                    reflowTillNow();
                    if (!defStack.isEmpty()) {
                        if (indent == defStack.peek()) {
                            result.append(escape(CODE_RESET));
                        }
                        addNewLineIfNeeded(result);
                        indent = defStack.peek() + INDENT;
                        result.append(indentString(indent));
                    }
                    break;
                case H1: case H2: case H3:
                case H4: case H5: case H6:
                    reflowTillNow();
                    addNewLineIfNeeded(result);
                    result.append("\n")
                          .append(escape(CODE_UNDERLINE));
                    reflownTo = result.length();
                    break;
                case TABLE:
                    int columns = tableColumns.get(node);

                    if (columns == 0) {
                        break; //broken input
                    }

                    reflowTillNow();
                    addNewLineIfNeeded(result);
                    reflownTo = result.length();

                    tableStack.push(limit);

                    limit = (limit - 1) / columns - 3;

                    for (int sep = 0; sep < (limit + 3) * columns + 1; sep++) {
                        result.append("-");
                    }

                    result.append("\n");

                    break;
                case TR:
                    if (cellsStack.size() >= tableStack.size()) {
                        //unclosed <tr>:
                        handleEndElement(node.getName());
                    }
                    cellsStack.push(new ArrayList<>());
                    headerStack.push(new ArrayList<>());
                    break;
                case TH:
                case TD:
                    if (cellsStack.isEmpty()) {
                        //broken code
                        break;
                    }
                    reflowTillNow();
                    result.append("\n");
                    reflownTo = result.length();
                    cellsStack.peek().add(result.length());
                    headerStack.peek().add(HtmlTag.get(node.getName()) == HtmlTag.TH);
                    break;
                case IMG:
                    for (DocTree attr : node.getAttributes()) {
                        if (attr.getKind() != DocTree.Kind.ATTRIBUTE) {
                            continue;
                        }
                        AttributeTree at = (AttributeTree) attr;
                        if ("alt".equals(StringUtils.toLowerCase(at.getName().toString()))) {
                            addSpaceIfNeeded(result);
                            scan(at.getValue(), null);
                            addSpaceIfNeeded(result);
                            break;
                        }
                    }
                    break;
                default:
                    addSpaceIfNeeded(result);
                    break;
            }
            return null;
        }

        @Override @DefinedBy(Api.COMPILER_TREE)
        public Object visitEndElement(EndElementTree node, Object p) {
            handleEndElement(node.getName());
            return super.visitEndElement(node, p);
        }

        private void handleEndElement(Name name) {
            switch (getHtmlTag(name)) {
                case BLOCKQUOTE:
                    indent -= INDENT;
                    break;
                case PRE:
                    pre = false;
                    addNewLineIfNeeded(result);
                    reflownTo = result.length();
                    break;
                case UL: case OL:
                    if (listStack.isEmpty()) { //ignore stray closing tag
                        break;
                    }
                    reflowTillNow();
                    listStack.pop();
                    indent -= INDENT;
                    addNewLineIfNeeded(result);
                    break;
                case DL:
                    if (defStack.isEmpty()) {//ignore stray closing tag
                        break;
                    }
                    reflowTillNow();
                    if (indent == defStack.peek()) {
                        result.append(escape(CODE_RESET));
                    }
                    indent = defStack.pop();
                    addNewLineIfNeeded(result);
                    break;
                case H1: case H2: case H3:
                case H4: case H5: case H6:
                    reflowTillNow();
                    result.append(escape(CODE_RESET))
                          .append("\n");
                    reflownTo = result.length();
                    break;
                case TABLE:
                    if (cellsStack.size() >= tableStack.size()) {
                        //unclosed <tr>:
                        handleEndElement(task.getElements().getName("tr"));
                    }

                    if (tableStack.isEmpty()) {
                        break;
                    }

                    limit = tableStack.pop();
                    break;
                case TR:
                    if (cellsStack.isEmpty()) {
                        break;
                    }

                    reflowTillNow();

                    List<Integer> cells = cellsStack.pop();
                    List<Boolean> headerFlags = headerStack.pop();
                    List<String[]> content = new ArrayList<>();
                    int maxLines = 0;

                    result.append("\n");

                    while (!cells.isEmpty()) {
                        int currentCell = cells.remove(cells.size() - 1);
                        String[] lines = result.substring(currentCell, result.length()).split("\n");

                        result.delete(currentCell - 1, result.length());

                        content.add(lines);
                        maxLines = Math.max(maxLines, lines.length);
                    }

                    Collections.reverse(content);

                    for (int line = 0; line < maxLines; line++) {
                        for (int column = 0; column < content.size(); column++) {
                            String[] lines = content.get(column);
                            String currentLine = line < lines.length ? lines[line] : "";
                            result.append("| ");
                            boolean header = headerFlags.get(column);
                            if (header) {
                                result.append(escape(CODE_HIGHLIGHT));
                            }
                            result.append(currentLine);
                            if (header) {
                                result.append(escape(CODE_RESET));
                            }
                            int padding = limit - currentLine.length();
                            if (padding > 0)
                                result.append(indentString(padding));
                            result.append(" ");
                        }
                        result.append("|\n");
                    }

                    for (int sep = 0; sep < (limit + 3) * content.size() + 1; sep++) {
                        result.append("-");
                    }

                    result.append("\n");

                    reflownTo = result.length();
                    break;
                case TD:
                case TH:
                    break;
                default:
                    addSpaceIfNeeded(result);
                    break;
            }
        }

        @Override @DefinedBy(Api.COMPILER_TREE)
        public Object visitEntity(EntityTree node, Object p) {
            String value = trees.getCharacters(node);
            result.append(value == null ? node.toString() : value);
            return super.visitEntity(node, p);

        }

        private DocTree lastNode;

        @Override @DefinedBy(Api.COMPILER_TREE)
        public Object scan(DocTree node, Object p) {
            if (node instanceof InlineTagTree) {
                addSpaceIfNeeded(result);
            }
            try {
                return super.scan(node, p);
            } finally {
                if (node instanceof InlineTagTree) {
                    addSpaceIfNeeded(result);
                }
                lastNode = node;
            }
        }

        private void reflowTillNow() {
            while (result.length() > 0 && result.charAt(result.length() - 1) == ' ')
                result.delete(result.length() - 1, result.length());
            reflownTo = Math.min(reflownTo, result.length());
            reflow(result, reflownTo, indent, limit);
            reflownTo = result.length();
        }
    };

    private String escape(String sequence) {
        return this.escapeSequencesSupported ? sequence : "";
    }

    private static final Map<Sections, String> docSections = new LinkedHashMap<>();
    private static final String inlineReturns;

    static {
        ResourceBundle bundle =
                ResourceBundle.getBundle("jdk.internal.shellsupport.doc.resources.javadocformatter");
        docSections.put(Sections.TYPE_PARAMS, bundle.getString("CAP_TypeParameters"));
        docSections.put(Sections.PARAMS, bundle.getString("CAP_Parameters"));
        docSections.put(Sections.RETURNS, bundle.getString("CAP_Returns"));
        docSections.put(Sections.THROWS, bundle.getString("CAP_Thrown_Exceptions"));
        inlineReturns = bundle.getString("Inline_Returns");
    }

    private static String indentString(int indent) {
        char[] content = new char[indent];
        Arrays.fill(content, ' ');
        return new String(content);
    }

    private static void reflow(StringBuilder text, int from, int indent, int limit) {
        int lineStart = from;

        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }

        int lineChars = from - lineStart;
        int pointer = from;
        int lastSpace = -1;

        while (pointer < text.length()) {
            if (text.charAt(pointer) == ' ')
                lastSpace = pointer;
            if (lineChars >= limit) {
                if (lastSpace != (-1)) {
                    text.setCharAt(lastSpace, '\n');
                    text.insert(lastSpace + 1, indentString(indent));
                    lineChars = indent + pointer - lastSpace - 1;
                    pointer += indent;
                    lastSpace = -1;
                }
            }
            lineChars++;
            pointer++;
        }
    }

    private static void addNewLineIfNeeded(StringBuilder text) {
        if (text.length() > 0 && text.charAt(text.length() - 1) != '\n') {
            text.append("\n");
        }
    }

    private static void addSpaceIfNeeded(StringBuilder text) {
        if (text.length() == 0)
            return ;

        char last = text.charAt(text.length() - 1);

        if (last != ' ' && last != '\n') {
            text.append(" ");
        }
    }

    private static HtmlTag getHtmlTag(Name name) {
        HtmlTag tag = HtmlTag.get(name);

        return tag != null ? tag : HtmlTag.HTML; //using HtmlTag.HTML as default no-op value
    }

    private static Map<StartElementTree, Integer> countTableColumns(DocCommentTree dct) {
        Map<StartElementTree, Integer> result = new IdentityHashMap<>();

        new DocTreeScanner<Void, Void>() {
            private StartElementTree currentTable;
            private int currentMaxColumns;
            private int currentRowColumns;

            @Override @DefinedBy(Api.COMPILER_TREE)
            public Void visitStartElement(StartElementTree node, Void p) {
                switch (getHtmlTag(node.getName())) {
                    case TABLE: currentTable = node; break;
                    case TR:
                        currentMaxColumns = Math.max(currentMaxColumns, currentRowColumns);
                        currentRowColumns = 0;
                        break;
                    case TD:
                    case TH: currentRowColumns++; break;
                }
                return super.visitStartElement(node, p);
            }

            @Override @DefinedBy(Api.COMPILER_TREE)
            public Void visitEndElement(EndElementTree node, Void p) {
                if (HtmlTag.get(node.getName()) == HtmlTag.TABLE) {
                    closeTable();
                }
                return super.visitEndElement(node, p);
            }

            @Override @DefinedBy(Api.COMPILER_TREE)
            public Void visitDocComment(DocCommentTree node, Void p) {
                try {
                    return super.visitDocComment(node, p);
                } finally {
                    closeTable();
                }
            }

            private void closeTable() {
                if (currentTable != null) {
                    result.put(currentTable, Math.max(currentMaxColumns, currentRowColumns));
                    currentTable = null;
                }
            }
        }.scan(dct, null);

        return result;
    }

    private enum Sections {
        TYPE_PARAMS {
            @Override public boolean matches(DocTree t) {
                return t.getKind() == DocTree.Kind.PARAM && ((ParamTree) t).isTypeParameter();
            }
        },
        PARAMS {
            @Override public boolean matches(DocTree t) {
                return t.getKind() == DocTree.Kind.PARAM && !((ParamTree) t).isTypeParameter();
            }
        },
        RETURNS {
            @Override public boolean matches(DocTree t) {
                return t.getKind() == DocTree.Kind.RETURN;
            }
        },
        THROWS {
            @Override public boolean matches(DocTree t) {
                return t.getKind() == DocTree.Kind.THROWS;
            }
        };

        public abstract boolean matches(DocTree t);
    }
}
