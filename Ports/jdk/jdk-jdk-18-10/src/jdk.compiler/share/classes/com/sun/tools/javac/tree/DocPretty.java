/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.tree;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.sun.source.doctree.*;
import com.sun.source.doctree.AttributeTree.ValueKind;
import com.sun.tools.javac.util.Convert;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;

/**
 * Prints out a doc-comment tree.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class DocPretty implements DocTreeVisitor<Void,Void> {

    /**
     * The output stream on which trees are printed.
     */
    final Writer out;

    public DocPretty(Writer out) {
        this.out = out;
    }

    /** Visitor method: print expression tree.
     */
    public void print(DocTree tree) throws IOException {
        try {
            if (tree == null)
                print("/*missing*/");
            else {
                tree.accept(this, null);
            }
        } catch (UncheckedIOException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    /**
     * Print string, replacing all non-ascii character with unicode escapes.
     */
    protected void print(Object s) throws IOException {
        out.write(Convert.escapeUnicode(s.toString()));
    }

    /**
     * Print list.
     */
    public void print(List<? extends DocTree> list) throws IOException {
        for (DocTree t: list) {
            print(t);
        }
    }

    /**
     * Print list with separators.
     */
    protected void print(List<? extends DocTree> list, String sep) throws IOException {
        if (list.isEmpty())
            return;
        boolean first = true;
        for (DocTree t: list) {
            if (!first)
                print(sep);
            print(t);
            first = false;
        }
    }

    /** Print new line.
     */
    protected void println() throws IOException {
        out.write(lineSep);
    }

    protected void printTagName(DocTree node) throws IOException {
        out.write("@");
        out.write(node.getKind().tagName);
    }

    final String lineSep = System.getProperty("line.separator");

    /* ************************************************************************
     * Traversal methods
     *************************************************************************/

    /** Exception to propagate IOException through visitXYZ methods */
    private static class UncheckedIOException extends Error {
        static final long serialVersionUID = -4032692679158424751L;
        UncheckedIOException(IOException e) {
            super(e.getMessage(), e);
        }
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitAttribute(AttributeTree node, Void p) {
        try {
            print(node.getName());
            String quote;
            switch (node.getValueKind()) {
                case EMPTY:
                    quote = null;
                    break;
                case UNQUOTED:
                    quote = "";
                    break;
                case SINGLE:
                    quote = "'";
                    break;
                case DOUBLE:
                    quote = "\"";
                    break;
                default:
                    throw new AssertionError();
            }
            if (quote != null) {
                print("=" + quote);
                print(node.getValue());
                print(quote);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitAuthor(AuthorTree node, Void p) {
        try {
            printTagName(node);
            print(" ");
            print(node.getName());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitComment(CommentTree node, Void p) {
        try {
            print(node.getBody());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitDeprecated(DeprecatedTree node, Void p) {
        try {
            printTagName(node);
            if (!node.getBody().isEmpty()) {
                print(" ");
                print(node.getBody());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitDocComment(DocCommentTree node, Void p) {
        try {
            List<? extends DocTree> b = node.getFullBody();
            List<? extends DocTree> t = node.getBlockTags();
            print(b);
            if (!b.isEmpty() && !t.isEmpty())
                print("\n");
            print(t, "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitDocRoot(DocRootTree node, Void p) {
        try {
            print("{");
            printTagName(node);
            print("}");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitDocType(DocTypeTree node, Void p) {
        try {
            print(node.getText());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitEndElement(EndElementTree node, Void p) {
        try {
            print("</");
            print(node.getName());
            print(">");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitEntity(EntityTree node, Void p) {
        try {
            print("&");
            print(node.getName());
            print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitErroneous(ErroneousTree node, Void p) {
        try {
            print(node.getBody());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitHidden(HiddenTree node, Void p) {
        try {
            printTagName(node);
            if (!node.getBody().isEmpty()) {
                print(" ");
                print(node.getBody());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitIdentifier(IdentifierTree node, Void p) {
        try {
            print(node.getName());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitIndex(IndexTree node, Void p) {
        try {
            print("{");
            printTagName(node);
            print(" ");
            print(node.getSearchTerm());
            if (!node.getDescription().isEmpty()) {
                print(" ");
                print(node.getDescription());
            }
            print("}");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitInheritDoc(InheritDocTree node, Void p) {
        try {
            print("{");
            printTagName(node);
            print("}");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitLink(LinkTree node, Void p) {
        try {
            print("{");
            printTagName(node);
            print(" ");
            print(node.getReference());
            if (!node.getLabel().isEmpty()) {
                print(" ");
                print(node.getLabel());
            }
            print("}");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitLiteral(LiteralTree node, Void p) {
        try {
            print("{");
            printTagName(node);
            String body = node.getBody().getBody();
            if (!body.isEmpty() && !Character.isWhitespace(body.charAt(0))) {
                print(" ");
            }
            print(node.getBody());
            print("}");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitParam(ParamTree node, Void p) {
        try {
            printTagName(node);
            print(" ");
            if (node.isTypeParameter()) print("<");
            print(node.getName());
            if (node.isTypeParameter()) print(">");
            if (!node.getDescription().isEmpty()) {
                print(" ");
                print(node.getDescription());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitProvides(ProvidesTree node, Void p) {
        try {
            printTagName(node);
            print(" ");
            print(node.getServiceType());
            if (!node.getDescription().isEmpty()) {
                print(" ");
                print(node.getDescription());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitReference(ReferenceTree node, Void p) {
        try {
            print(node.getSignature());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitReturn(ReturnTree node, Void p) {
        try {
            if (node.isInline()) {
                print("{");
            }
            printTagName(node);
            print(" ");
            print(node.getDescription());
            if (node.isInline()) {
                print("}");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitSee(SeeTree node, Void p) {
        try {
            printTagName(node);
            boolean first = true;
            boolean needSep = true;
            for (DocTree t: node.getReference()) {
                if (needSep) print(" ");
                needSep = (first && (t instanceof ReferenceTree));
                first = false;
                print(t);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitSerial(SerialTree node, Void p) {
        try {
            printTagName(node);
            if (!node.getDescription().isEmpty()) {
                print(" ");
                print(node.getDescription());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitSerialData(SerialDataTree node, Void p) {
        try {
            printTagName(node);
            if (!node.getDescription().isEmpty()) {
                print(" ");
                print(node.getDescription());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitSerialField(SerialFieldTree node, Void p) {
        try {
            printTagName(node);
            print(" ");
            print(node.getName());
            print(" ");
            print(node.getType());
            if (!node.getDescription().isEmpty()) {
                print(" ");
                print(node.getDescription());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitSince(SinceTree node, Void p) {
        try {
            printTagName(node);
            print(" ");
            print(node.getBody());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitStartElement(StartElementTree node, Void p) {
        try {
            print("<");
            print(node.getName());
            List<? extends DocTree> attrs = node.getAttributes();
            if (!attrs.isEmpty()) {
                print(" ");
                print(attrs, " ");
                DocTree last = node.getAttributes().get(attrs.size() - 1);
                if (node.isSelfClosing() && last instanceof AttributeTree attributeTree
                        && attributeTree.getValueKind() == ValueKind.UNQUOTED)
                    print(" ");
            }
            if (node.isSelfClosing())
                print("/");
            print(">");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitSummary(SummaryTree node, Void p) {
        try {
            print("{");
            printTagName(node);
            if (!node.getSummary().isEmpty()) {
                print(" ");
                print(node.getSummary());
            }
            print("}");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitSystemProperty(SystemPropertyTree node, Void p) {
        try {
            print("{");
            printTagName(node);
            print(" ");
            print(node.getPropertyName());
            print("}");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitText(TextTree node, Void p) {
        try {
            print(node.getBody());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitThrows(ThrowsTree node, Void p) {
        try {
            printTagName(node);
            print(" ");
            print(node.getExceptionName());
            if (!node.getDescription().isEmpty()) {
                print(" ");
                print(node.getDescription());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitUnknownBlockTag(UnknownBlockTagTree node, Void p) {
        try {
            print("@");
            print(node.getTagName());
            print(" ");
            print(node.getContent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitUnknownInlineTag(UnknownInlineTagTree node, Void p) {
        try {
            print("{");
            print("@");
            print(node.getTagName());
            print(" ");
            print(node.getContent());
            print("}");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitUses(UsesTree node, Void p) {
        try {
            printTagName(node);
            print(" ");
            print(node.getServiceType());
            if (!node.getDescription().isEmpty()) {
                print(" ");
                print(node.getDescription());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitValue(ValueTree node, Void p) {
        try {
            print("{");
            printTagName(node);
            if (node.getReference() != null) {
                print(" ");
                print(node.getReference());
            }
            print("}");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitVersion(VersionTree node, Void p) {
        try {
            printTagName(node);
            print(" ");
            print(node.getBody());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Void visitOther(DocTree node, Void p) {
        try {
            print("(UNKNOWN: " + node + ")");
            println();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }
}
