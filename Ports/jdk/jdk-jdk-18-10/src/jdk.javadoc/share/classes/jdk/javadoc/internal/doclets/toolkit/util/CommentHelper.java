/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.AttributeTree.ValueKind;
import com.sun.source.doctree.AuthorTree;
import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.CommentTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.IdentifierTree;
import com.sun.source.doctree.InlineTagTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ProvidesTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SerialDataTree;
import com.sun.source.doctree.SerialFieldTree;
import com.sun.source.doctree.SerialTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UsesTree;
import com.sun.source.doctree.ValueTree;
import com.sun.source.doctree.VersionTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SimpleDocTreeVisitor;
import com.sun.source.util.TreePath;
import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;

import static com.sun.source.doctree.DocTree.Kind.*;

/**
 *  A utility class.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class CommentHelper {
    private final BaseConfiguration configuration;
    public final TreePath path;
    public final DocCommentTree dcTree;
    public final Element element;
    private Element overriddenElement;

    public static final String SPACER = " ";

    /**
     * Creates a utility class to encapsulate the contextual information for a doc comment tree.
     *
     * @param configuration the configuration
     * @param element       the element for which this is a doc comment
     * @param path          the path for the element
     * @param dcTree        the doc comment
     */
    public CommentHelper(BaseConfiguration configuration, Element element, TreePath path, DocCommentTree dcTree) {
        this.configuration = configuration;
        this.element = element;
        this.path = path;
        this.dcTree = dcTree;
    }

    public void setOverrideElement(Element ove) {
        if (this.element == ove) {
            throw new AssertionError("cannot set given element as overridden element");
        }
        overriddenElement = ove;
    }

    public String getTagName(DocTree dtree) {
        switch (dtree.getKind()) {
            case AUTHOR:
            case DEPRECATED:
            case PARAM:
            case PROVIDES:
            case RETURN:
            case SEE:
            case SERIAL_DATA:
            case SERIAL_FIELD:
            case THROWS:
            case UNKNOWN_BLOCK_TAG:
            case USES:
            case VERSION:
                return ((BlockTagTree) dtree).getTagName();
            case UNKNOWN_INLINE_TAG:
                return ((InlineTagTree) dtree).getTagName();
            case ERRONEOUS:
                return "erroneous";
            default:
                return dtree.getKind().tagName;
        }
    }

    public boolean isTypeParameter(DocTree dtree) {
        if (dtree.getKind() == PARAM) {
            return ((ParamTree)dtree).isTypeParameter();
        }
        return false;
    }

    public String getParameterName(DocTree dtree) {
        if (dtree.getKind() == PARAM) {
            return ((ParamTree) dtree).getName().toString();
        } else {
            return null;
        }
    }

    Element getElement(ReferenceTree rtree) {
        Utils utils = configuration.utils;
        // likely a synthesized tree
        if (path == null) {
            // NOTE: this code path only supports module/package/type signatures
            //       and not member signatures. For more complete support,
            //       set a suitable path and avoid this branch.
            TypeMirror symbol = utils.getSymbol(rtree.getSignature());
            if (symbol == null) {
                return null;
            }
            return configuration.docEnv.getTypeUtils().asElement(symbol);
        }
        DocTreePath docTreePath = getDocTreePath(rtree);
        if (docTreePath == null) {
            return null;
        }
        DocTrees doctrees = configuration.docEnv.getDocTrees();
        return doctrees.getElement(docTreePath);
    }

    public TypeMirror getType(ReferenceTree rtree) {
        DocTreePath docTreePath = getDocTreePath(rtree);
        if (docTreePath != null) {
            DocTrees doctrees = configuration.docEnv.getDocTrees();
            return doctrees.getType(docTreePath);
        }
        return null;
    }

    public Element getException(ThrowsTree tt) {
        return getElement(tt.getExceptionName());
    }

    public List<? extends DocTree> getDescription(DocTree dtree) {
        return getTags(dtree);
    }

    public String getText(List<? extends DocTree> list) {
        StringBuilder sb = new StringBuilder();
        for (DocTree dt : list) {
            sb.append(getText0(dt));
        }
        return sb.toString();
    }

    public String getText(DocTree dt) {
        return getText0(dt).toString();
    }

    private StringBuilder getText0(DocTree dt) {
        final StringBuilder sb = new StringBuilder();
        new SimpleDocTreeVisitor<Void, Void>() {
            @Override
            public Void visitAttribute(AttributeTree node, Void p) {
                sb.append(SPACER).append(node.getName().toString());
                if (node.getValueKind() == ValueKind.EMPTY) {
                    return null;
                }

                sb.append("=");
                String quote;
                switch (node.getValueKind()) {
                    case DOUBLE:
                        quote = "\"";
                        break;
                    case SINGLE:
                        quote = "'";
                        break;
                    default:
                        quote = "";
                        break;
                }
                sb.append(quote);
                node.getValue().forEach(dt -> dt.accept(this, null));
                sb.append(quote);
                return null;
            }

            @Override
            public Void visitEndElement(EndElementTree node, Void p) {
                sb.append("</")
                        .append(node.getName().toString())
                        .append(">");
                return null;
            }

            @Override
            public Void visitEntity(EntityTree node, Void p) {
                sb.append(node.toString());
                return null;
            }

            @Override
            public Void visitLink(LinkTree node, Void p) {
                if (node.getReference() == null) {
                    return null;
                }

                node.getReference().accept(this, null);
                node.getLabel().forEach(dt -> dt.accept(this, null));
                return null;
            }

            @Override
            public Void visitLiteral(LiteralTree node, Void p) {
                if (node.getKind() == CODE) {
                    sb.append("<").append(node.getKind().tagName).append(">");
                }
                sb.append(node.getBody().toString());
                if (node.getKind() == CODE) {
                    sb.append("</").append(node.getKind().tagName).append(">");
                }
                return null;
            }

            @Override
            public Void visitReference(ReferenceTree node, Void p) {
                sb.append(node.getSignature());
                return null;
            }

            @Override
            public Void visitSee(SeeTree node, Void p) {
                node.getReference().forEach(dt -> dt.accept(this, null));
                return null;
            }

            @Override
            public Void visitSerial(SerialTree node, Void p) {
                node.getDescription().forEach(dt -> dt.accept(this, null));
                return null;
            }

            @Override
            public Void visitStartElement(StartElementTree node, Void p) {
                sb.append("<");
                sb.append(node.getName().toString());
                node.getAttributes().forEach(dt -> dt.accept(this, null));
                sb.append(node.isSelfClosing() ? "/>" : ">");
                return null;
            }

            @Override
            public Void visitText(TextTree node, Void p) {
                sb.append(node.getBody());
                return null;
            }

            @Override
            public Void visitUnknownBlockTag(UnknownBlockTagTree node, Void p) {
                node.getContent().forEach(dt -> dt.accept(this, null));
                return null;
            }

            @Override
            public Void visitValue(ValueTree node, Void p) {
                return node.getReference().accept(this, null);
            }

            @Override
            protected Void defaultAction(DocTree node, Void p) {
                sb.append(node.toString());
                return null;
            }
        }.visit(dt, null);
        return sb;
    }

    public String getLabel(DocTree dtree) {
        return new SimpleDocTreeVisitor<String, Void>() {
            @Override
            public String visitLink(LinkTree node, Void p) {
                return node.getLabel().stream()
                        .map(dt -> getText(dt))
                        .collect(Collectors.joining());
            }

            @Override
            public String visitSee(SeeTree node, Void p) {
                Utils utils = configuration.utils;
                return node.getReference().stream()
                        .filter(utils::isText)
                        .map(dt -> ((TextTree) dt).getBody())
                        .collect(Collectors.joining());
            }

            @Override
            protected String defaultAction(DocTree node, Void p) {
                return "";
            }
        }.visit(dtree, null);
    }

    public TypeElement getReferencedClass(DocTree dtree) {
        Utils utils = configuration.utils;
        Element e = getReferencedElement(dtree);
        if (e == null) {
            return null;
        } else if (utils.isTypeElement(e)) {
            return (TypeElement) e;
        } else if (!utils.isPackage(e) && !utils.isModule(e)) {
            return utils.getEnclosingTypeElement(e);
        }
        return null;
    }

    public String getReferencedModuleName(DocTree dtree) {
        String s = getReferencedSignature(dtree);
        if (s == null || s.contains("#") || s.contains("(")) {
            return null;
        }
        int n = s.indexOf("/");
        return (n == -1) ? s : s.substring(0, n);
    }

    public Element getReferencedMember(DocTree dtree) {
        Utils utils = configuration.utils;
        Element e = getReferencedElement(dtree);
        if (e == null) {
            return null;
        }
        return (utils.isExecutableElement(e) || utils.isVariableElement(e)) ? e : null;
    }

    public String getReferencedMemberName(DocTree dtree) {
        String s = getReferencedSignature(dtree);
        if (s == null) {
            return null;
        }
        int n = s.indexOf("#");
        return (n == -1) ? null : s.substring(n + 1);
    }

    public PackageElement getReferencedPackage(DocTree dtree) {
        Element e = getReferencedElement(dtree);
        if (e != null) {
            Utils utils = configuration.utils;
            return utils.containingPackage(e);
        }
        return null;
    }

    public ModuleElement getReferencedModule(DocTree dtree) {
        Element e = getReferencedElement(dtree);
        if (e != null && configuration.utils.isModule(e)) {
            return (ModuleElement) e;
        }
        return null;
    }


    public List<? extends DocTree> getFirstSentenceTrees(List<? extends DocTree> body) {
        return configuration.docEnv.getDocTrees().getFirstSentence(body);
    }

    public List<? extends DocTree> getFirstSentenceTrees(DocTree dtree) {
        return getFirstSentenceTrees(getBody(dtree));
    }

    private Element getReferencedElement(DocTree dtree) {
        return new ReferenceDocTreeVisitor<Element>() {
            @Override
            public Element visitReference(ReferenceTree node, Void p) {
                return getElement(node);
            }
        }.visit(dtree, null);
    }

    public TypeMirror getReferencedType(DocTree dtree) {
        return new ReferenceDocTreeVisitor<TypeMirror>() {
            @Override
            public TypeMirror visitReference(ReferenceTree node, Void p) {
                return getType(node);
            }
        }.visit(dtree, null);
    }

    public TypeElement getServiceType(DocTree dtree) {
        Element e = getReferencedElement(dtree);
        if (e != null) {
            Utils utils = configuration.utils;
            return utils.isTypeElement(e) ? (TypeElement) e : null;
        }
        return null;
    }

    public  String getReferencedSignature(DocTree dtree) {
        return new ReferenceDocTreeVisitor<String>() {
            @Override
            public String visitReference(ReferenceTree node, Void p) {
                return normalizeSignature(node.getSignature());
            }
        }.visit(dtree, null);
    }

    @SuppressWarnings("fallthrough")
    private static String normalizeSignature(String sig) {
        if (sig == null
                || (!sig.contains(" ") && !sig.contains("\n")
                 && !sig.contains("\r") && !sig.endsWith("/"))) {
            return sig;
        }
        StringBuilder sb = new StringBuilder();
        char lastChar = 0;
        for (int i = 0; i < sig.length(); i++) {
            char ch = sig.charAt(i);
            switch (ch) {
                case '\n':
                case '\r':
                case '\f':
                case '\t':
                case ' ':
                    // Add at most one space char, or none if it isn't needed
                    switch (lastChar) {
                        case 0:
                        case'(':
                        case'<':
                        case ' ':
                        case '.':
                            break;
                        default:
                            sb.append(' ');
                            lastChar = ' ';
                            break;
                    }
                    break;
                case ',':
                case '>':
                case ')':
                case '.':
                    // Remove preceding space character
                    if (lastChar == ' ') {
                        sb.setLength(sb.length() - 1);
                    }
                    // fallthrough
                default:
                    sb.append(ch);
                    lastChar = ch;
            }
        }
        // Delete trailing slash
        if (lastChar == '/') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private static class ReferenceDocTreeVisitor<R> extends SimpleDocTreeVisitor<R, Void> {
        @Override
        public R visitSee(SeeTree node, Void p) {
            for (DocTree dt : node.getReference()) {
                return visit(dt, null);
            }
            return null;
        }

        @Override
        public R visitLink(LinkTree node, Void p) {
            return visit(node.getReference(), null);
        }

        @Override
        public R visitProvides(ProvidesTree node, Void p) {
            return visit(node.getServiceType(), null);
        }

        @Override
        public R visitValue(ValueTree node, Void p) {
            return visit(node.getReference(), null);
        }

        @Override
        public R visitSerialField(SerialFieldTree node, Void p) {
            return visit(node.getType(), null);
        }

        @Override
        public R visitUses(UsesTree node, Void p) {
            return visit(node.getServiceType(), null);
        }

        @Override
        protected R defaultAction(DocTree node, Void p) {
            return null;
        }
    }

    public List<? extends DocTree> getReference(DocTree dtree) {
        return dtree.getKind() == SEE ? ((SeeTree)dtree).getReference() : null;
    }

    public ReferenceTree getExceptionName(DocTree dtree) {
        return (dtree.getKind() == THROWS || dtree.getKind() == EXCEPTION)
                ? ((ThrowsTree)dtree).getExceptionName()
                : null;
    }

    public IdentifierTree getName(DocTree dtree) {
        switch (dtree.getKind()) {
            case PARAM:
                return ((ParamTree)dtree).getName();
            case SERIAL_FIELD:
                return ((SerialFieldTree)dtree).getName();
            default:
                return null;
            }
    }

    public List<? extends DocTree> getTags(DocTree dtree) {
        return new SimpleDocTreeVisitor<List<? extends DocTree>, Void>() {
            List<? extends DocTree> asList(String content) {
                List<DocTree> out = new ArrayList<>();
                out.add(configuration.cmtUtils.makeTextTree(content));
                return out;
            }

            @Override
            public List<? extends DocTree> visitAuthor(AuthorTree node, Void p) {
                return node.getName();
            }

            @Override
            public List<? extends DocTree> visitComment(CommentTree node, Void p) {
                return asList(node.getBody());
            }

            @Override
            public List<? extends DocTree> visitDeprecated(DeprecatedTree node, Void p) {
                return node.getBody();
            }

            @Override
            public List<? extends DocTree> visitDocComment(DocCommentTree node, Void p) {
                return node.getBody();
            }

            @Override
            public List<? extends DocTree> visitLiteral(LiteralTree node, Void p) {
                return asList(node.getBody().getBody());
            }

            @Override
            public List<? extends DocTree> visitProvides(ProvidesTree node, Void p) {
                 return node.getDescription();
            }

            @Override
            public List<? extends DocTree> visitSince(SinceTree node, Void p) {
                return node.getBody();
            }

            @Override
            public List<? extends DocTree> visitText(TextTree node, Void p) {
                return asList(node.getBody());
            }

            @Override
            public List<? extends DocTree> visitVersion(VersionTree node, Void p) {
                return node.getBody();
            }

            @Override
            public List<? extends DocTree> visitParam(ParamTree node, Void p) {
               return node.getDescription();
            }

            @Override
            public List<? extends DocTree> visitReturn(ReturnTree node, Void p) {
                return node.getDescription();
            }

            @Override
            public List<? extends DocTree> visitSee(SeeTree node, Void p) {
                return node.getReference();
            }

            @Override
            public List<? extends DocTree> visitSerial(SerialTree node, Void p) {
                return node.getDescription();
            }

            @Override
            public List<? extends DocTree> visitSerialData(SerialDataTree node, Void p) {
                return node.getDescription();
            }

            @Override
            public List<? extends DocTree> visitSerialField(SerialFieldTree node, Void p) {
                return node.getDescription();
            }

            @Override
            public List<? extends DocTree> visitThrows(ThrowsTree node, Void p) {
                 return node.getDescription();
            }

            @Override
            public List<? extends DocTree> visitUnknownBlockTag(UnknownBlockTagTree node, Void p) {
                return node.getContent();
            }

            @Override
            public List<? extends DocTree> visitUses(UsesTree node, Void p) {
                 return node.getDescription();
            }

            @Override
            protected List<? extends DocTree> defaultAction(DocTree node, Void p) {
               return Collections.emptyList();
            }
        }.visit(dtree, null);
    }

    public List<? extends DocTree> getBody(DocTree dtree) {
        return getTags(dtree);
    }

    public ReferenceTree getType(DocTree dtree) {
        if (dtree.getKind() == SERIAL_FIELD) {
            return ((SerialFieldTree) dtree).getType();
        } else {
            return null;
        }
    }

    public DocTreePath getDocTreePath(DocTree dtree) {
        if (dcTree == null && overriddenElement != null) {
            // This is an inherited comment, return path from ancestor.
            return configuration.utils.getCommentHelper(overriddenElement).getDocTreePath(dtree);
        } else if (path == null || dcTree == null || dtree == null) {
            return null;
        }
        DocTreePath dtPath = DocTreePath.getPath(path, dcTree, dtree);
        if (dtPath == null && overriddenElement != null) {
            // The overriding element has a doc tree, but it doesn't contain what we're looking for.
            return configuration.utils.getCommentHelper(overriddenElement).getDocTreePath(dtree);
        }
        return dtPath;
    }

    public Element getOverriddenElement() {
        return overriddenElement;
    }

    /**
     * For debugging purposes only. Do not rely on this for other things.
     * @return a string representation.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CommentHelper{" + "path=" + path + ", dcTree=" + dcTree);
        sb.append(", element=");
        sb.append(element.getEnclosingElement());
        sb.append("::");
        sb.append(element);
        sb.append(", overriddenElement=");
        if (overriddenElement != null) {
            sb.append(overriddenElement.getEnclosingElement());
            sb.append("::");
            sb.append(overriddenElement);
        } else {
            sb.append("<none>");
        }
        sb.append('}');
        return sb.toString();
    }
}
