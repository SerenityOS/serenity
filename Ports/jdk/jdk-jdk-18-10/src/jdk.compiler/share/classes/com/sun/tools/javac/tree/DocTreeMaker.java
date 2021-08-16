/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.lang.model.element.Name;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.sun.source.doctree.AttributeTree.ValueKind;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTree.Kind;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.IdentifierTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTreeFactory;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.parser.ReferenceParser;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.tree.DCTree.DCAttribute;
import com.sun.tools.javac.tree.DCTree.DCAuthor;
import com.sun.tools.javac.tree.DCTree.DCComment;
import com.sun.tools.javac.tree.DCTree.DCDeprecated;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import com.sun.tools.javac.tree.DCTree.DCDocRoot;
import com.sun.tools.javac.tree.DCTree.DCDocType;
import com.sun.tools.javac.tree.DCTree.DCEndElement;
import com.sun.tools.javac.tree.DCTree.DCEntity;
import com.sun.tools.javac.tree.DCTree.DCErroneous;
import com.sun.tools.javac.tree.DCTree.DCHidden;
import com.sun.tools.javac.tree.DCTree.DCIdentifier;
import com.sun.tools.javac.tree.DCTree.DCIndex;
import com.sun.tools.javac.tree.DCTree.DCInheritDoc;
import com.sun.tools.javac.tree.DCTree.DCLink;
import com.sun.tools.javac.tree.DCTree.DCLiteral;
import com.sun.tools.javac.tree.DCTree.DCParam;
import com.sun.tools.javac.tree.DCTree.DCProvides;
import com.sun.tools.javac.tree.DCTree.DCReference;
import com.sun.tools.javac.tree.DCTree.DCReturn;
import com.sun.tools.javac.tree.DCTree.DCSee;
import com.sun.tools.javac.tree.DCTree.DCSerial;
import com.sun.tools.javac.tree.DCTree.DCSerialData;
import com.sun.tools.javac.tree.DCTree.DCSerialField;
import com.sun.tools.javac.tree.DCTree.DCSince;
import com.sun.tools.javac.tree.DCTree.DCStartElement;
import com.sun.tools.javac.tree.DCTree.DCSummary;
import com.sun.tools.javac.tree.DCTree.DCSystemProperty;
import com.sun.tools.javac.tree.DCTree.DCText;
import com.sun.tools.javac.tree.DCTree.DCThrows;
import com.sun.tools.javac.tree.DCTree.DCUnknownBlockTag;
import com.sun.tools.javac.tree.DCTree.DCUnknownInlineTag;
import com.sun.tools.javac.tree.DCTree.DCUses;
import com.sun.tools.javac.tree.DCTree.DCValue;
import com.sun.tools.javac.tree.DCTree.DCVersion;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Pair;
import com.sun.tools.javac.util.Position;
import com.sun.tools.javac.util.StringUtils;


/**
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class DocTreeMaker implements DocTreeFactory {

    /** The context key for the tree factory. */
    protected static final Context.Key<DocTreeMaker> treeMakerKey = new Context.Key<>();

    // A subset of block tags, which acts as sentence breakers, appearing
    // anywhere but the zero'th position in the first sentence.
    final Set<String> sentenceBreakTags;

    /** Get the TreeMaker instance. */
    public static DocTreeMaker instance(Context context) {
        DocTreeMaker instance = context.get(treeMakerKey);
        if (instance == null)
            instance = new DocTreeMaker(context);
        return instance;
    }

    /** The position at which subsequent trees will be created.
     */
    public int pos = Position.NOPOS;

    /** Access to diag factory for ErroneousTrees. */
    private final JCDiagnostic.Factory diags;

    private final JavacTrees trees;

    /** Utility class to parse reference signatures. */
    private final ReferenceParser referenceParser;

    /** Create a tree maker with NOPOS as initial position.
     */
    protected DocTreeMaker(Context context) {
        context.put(treeMakerKey, this);
        diags = JCDiagnostic.Factory.instance(context);
        this.pos = Position.NOPOS;
        trees = JavacTrees.instance(context);
        referenceParser = new ReferenceParser(ParserFactory.instance(context));
        sentenceBreakTags = Set.of("H1", "H2", "H3", "H4", "H5", "H6", "PRE", "P");
    }

    /** Reassign current position.
     */
    @Override @DefinedBy(Api.COMPILER_TREE)
    public DocTreeMaker at(int pos) {
        this.pos = pos;
        return this;
    }

    /** Reassign current position.
     */
    public DocTreeMaker at(DiagnosticPosition pos) {
        this.pos = (pos == null ? Position.NOPOS : pos.getStartPosition());
        return this;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCAttribute newAttributeTree(Name name, ValueKind vkind, List<? extends DocTree> value) {
        DCAttribute tree = new DCAttribute(name, vkind, cast(value));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCAuthor newAuthorTree(List<? extends DocTree> name) {
        DCAuthor tree = new DCAuthor(cast(name));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCLiteral newCodeTree(TextTree text) {
        DCLiteral tree = new DCLiteral(Kind.CODE, (DCText) text);
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCComment newCommentTree(String text) {
        DCComment tree = new DCComment(text);
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCDeprecated newDeprecatedTree(List<? extends DocTree> text) {
        DCDeprecated tree = new DCDeprecated(cast(text));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCDocComment newDocCommentTree(List<? extends DocTree> fullBody, List<? extends DocTree> tags) {
        return newDocCommentTree(fullBody, tags, Collections.emptyList(), Collections.emptyList());
    }

    public DCDocComment newDocCommentTree(Comment comment,
                                          List<? extends DocTree> fullBody,
                                          List<? extends DocTree> tags,
                                          List<? extends DocTree> preamble,
                                          List<? extends DocTree> postamble) {
        Pair<List<DCTree>, List<DCTree>> pair = splitBody(fullBody);
        DCDocComment tree = new DCDocComment(comment, cast(fullBody), pair.fst, pair.snd,
                cast(tags), cast(preamble), cast(postamble));
        tree.pos = pos;
        return tree;
    }

    /*
     * Primarily to produce a DocCommentTree when given a
     * first sentence and a body, this is useful, in cases
     * where the trees are being synthesized by a tool.
     */
    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCDocComment newDocCommentTree(List<? extends DocTree> fullBody,
                                          List<? extends DocTree> tags,
                                          List<? extends DocTree> preamble,
                                          List<? extends DocTree> postamble) {
        ListBuffer<DCTree> lb = new ListBuffer<>();
        lb.addAll(cast(fullBody));
        List<DCTree> fBody = lb.toList();

        // A dummy comment to keep the diagnostics logic happy.
        Comment c = new Comment() {
            @Override
            public String getText() {
                return null;
            }

            @Override
            public int getSourcePos(int index) {
                return Position.NOPOS;
            }

            @Override
            public CommentStyle getStyle() {
                return CommentStyle.JAVADOC;
            }

            @Override
            public boolean isDeprecated() {
                return false;
            }
        };
        Pair<List<DCTree>, List<DCTree>> pair = splitBody(fullBody);
        DCDocComment tree = new DCDocComment(c, fBody, pair.fst, pair.snd, cast(tags),
                                             cast(preamble), cast(postamble));
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCDocRoot newDocRootTree() {
        DCDocRoot tree = new DCDocRoot();
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCDocType newDocTypeTree(String text) {
        DCDocType tree = new DCDocType(text);
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCEndElement newEndElementTree(Name name) {
        DCEndElement tree = new DCEndElement(name);
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCEntity newEntityTree(Name name) {
        DCEntity tree = new DCEntity(name);
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCErroneous newErroneousTree(String text, Diagnostic<JavaFileObject> diag) {
        DCErroneous tree = new DCErroneous(text, (JCDiagnostic) diag);
        tree.pos = pos;
        return tree;
    }

    public DCErroneous newErroneousTree(String text, DiagnosticSource diagSource, String code, Object... args) {
        DCErroneous tree = new DCErroneous(text, diags, diagSource, code, args);
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCThrows newExceptionTree(ReferenceTree name, List<? extends DocTree> description) {
        // TODO: verify the reference is just to a type (not a field or method)
        DCThrows tree = new DCThrows(Kind.EXCEPTION, (DCReference) name, cast(description));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCHidden newHiddenTree(List<? extends DocTree> text) {
        DCHidden tree = new DCHidden(cast(text));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCIdentifier newIdentifierTree(Name name) {
        DCIdentifier tree = new DCIdentifier(name);
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCIndex newIndexTree(DocTree term, List<? extends DocTree> description) {
        DCIndex tree = new DCIndex((DCTree) term, cast(description));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCInheritDoc newInheritDocTree() {
        DCInheritDoc tree = new DCInheritDoc();
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCLink newLinkTree(ReferenceTree ref, List<? extends DocTree> label) {
        DCLink tree = new DCLink(Kind.LINK, (DCReference) ref, cast(label));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCLink newLinkPlainTree(ReferenceTree ref, List<? extends DocTree> label) {
        DCLink tree = new DCLink(Kind.LINK_PLAIN, (DCReference) ref, cast(label));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCLiteral newLiteralTree(TextTree text) {
        DCLiteral tree = new DCLiteral(Kind.LITERAL, (DCText) text);
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCParam newParamTree(boolean isTypeParameter, IdentifierTree name, List<? extends DocTree> description) {
        DCParam tree = new DCParam(isTypeParameter, (DCIdentifier) name, cast(description));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCProvides newProvidesTree(ReferenceTree name, List<? extends DocTree> description) {
        DCProvides tree = new DCProvides((DCReference) name, cast(description));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCReference newReferenceTree(String signature) {
        try {
            ReferenceParser.Reference ref = referenceParser.parse(signature);
            DCReference tree = new DCReference(signature, ref.moduleName, ref.qualExpr, ref.member, ref.paramTypes);
            tree.pos = pos;
            return tree;
        } catch (ReferenceParser.ParseException e) {
            throw new IllegalArgumentException("invalid signature", e);
        }
    }

    public DCReference newReferenceTree(String signature, JCTree.JCExpression moduleName, JCTree qualExpr, Name member, List<JCTree> paramTypes) {
        DCReference tree = new DCReference(signature, moduleName, qualExpr, member, paramTypes);
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCReturn newReturnTree(List<? extends DocTree> description) {
        return newReturnTree(false, description);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCReturn newReturnTree(boolean isInline, List<? extends DocTree> description) {
        DCReturn tree = new DCReturn(isInline, cast(description));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCSee newSeeTree(List<? extends DocTree> reference) {
        DCSee tree = new DCSee(cast(reference));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCSerial newSerialTree(List<? extends DocTree> description) {
        DCSerial tree = new DCSerial(cast(description));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCSerialData newSerialDataTree(List<? extends DocTree> description) {
        DCSerialData tree = new DCSerialData(cast(description));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCSerialField newSerialFieldTree(IdentifierTree name, ReferenceTree type, List<? extends DocTree> description) {
        DCSerialField tree = new DCSerialField((DCIdentifier) name, (DCReference) type, cast(description));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCSince newSinceTree(List<? extends DocTree> text) {
        DCSince tree = new DCSince(cast(text));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCStartElement newStartElementTree(Name name, List<? extends DocTree> attrs, boolean selfClosing) {
        DCStartElement tree = new DCStartElement(name, cast(attrs), selfClosing);
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCSummary newSummaryTree(List<? extends DocTree> text) {
        DCSummary tree = new DCSummary(cast(text));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCSystemProperty newSystemPropertyTree(Name propertyName) {
        DCSystemProperty tree = new DCSystemProperty(propertyName);
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCText newTextTree(String text) {
        DCText tree = new DCText(text);
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCThrows newThrowsTree(ReferenceTree name, List<? extends DocTree> description) {
        // TODO: verify the reference is just to a type (not a field or method)
        DCThrows tree = new DCThrows(Kind.THROWS, (DCReference) name, cast(description));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCUnknownBlockTag newUnknownBlockTagTree(Name name, List<? extends DocTree> content) {
        DCUnknownBlockTag tree = new DCUnknownBlockTag(name, cast(content));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCUnknownInlineTag newUnknownInlineTagTree(Name name, List<? extends DocTree> content) {
        DCUnknownInlineTag tree = new DCUnknownInlineTag(name, cast(content));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCUses newUsesTree(ReferenceTree name, List<? extends DocTree> description) {
        DCUses tree = new DCUses((DCReference) name, cast(description));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCValue newValueTree(ReferenceTree ref) {
        // TODO: verify the reference is to a constant value
        DCValue tree = new DCValue((DCReference) ref);
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DCVersion newVersionTree(List<? extends DocTree> text) {
        DCVersion tree = new DCVersion(cast(text));
        tree.pos = pos;
        return tree;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public List<DocTree> getFirstSentence(List<? extends DocTree> list) {
        Pair<List<DCTree>, List<DCTree>> pair = splitBody(list);
        return new ArrayList<>(pair.fst);
    }

    /*
     * Breaks up the body tags into the first sentence and its successors.
     * The first sentence is determined with the presence of a period,
     * block tag, or a sentence break, as returned by the BreakIterator.
     * Trailing whitespaces are trimmed.
     */
    private Pair<List<DCTree>, List<DCTree>> splitBody(Collection<? extends DocTree> list) {
        // pos is modified as we create trees, therefore
        // we save the pos and restore it later.
        final int savedpos = this.pos;
        try {
            ListBuffer<DCTree> body = new ListBuffer<>();
            // split body into first sentence and body
            ListBuffer<DCTree> fs = new ListBuffer<>();
            if (list.isEmpty()) {
                return new Pair<>(fs.toList(), body.toList());
            }
            boolean foundFirstSentence = false;
            ArrayList<DocTree> alist = new ArrayList<>(list);
            ListIterator<DocTree> itr = alist.listIterator();
            while (itr.hasNext()) {
                boolean isFirst = !itr.hasPrevious();
                DocTree dt = itr.next();
                int spos = ((DCTree) dt).pos;
                if (foundFirstSentence) {
                    body.add((DCTree) dt);
                    continue;
                }
                switch (dt.getKind()) {
                    case RETURN:
                    case SUMMARY:
                        foundFirstSentence = true;
                        break;
                    case TEXT:
                        DCText tt = (DCText) dt;
                        String s = tt.getBody();
                        DocTree peekedNext = itr.hasNext()
                                ? alist.get(itr.nextIndex())
                                : null;
                        int sbreak = getSentenceBreak(s, peekedNext);
                        if (sbreak > 0) {
                            s = s.substring(0, sbreak).stripTrailing();
                            DCText text = this.at(spos).newTextTree(s);
                            fs.add(text);
                            foundFirstSentence = true;
                            int nwPos = skipWhiteSpace(tt.getBody(), sbreak);
                            if (nwPos > 0) {
                                DCText text2 = this.at(spos + nwPos).newTextTree(tt.getBody().substring(nwPos));
                                body.add(text2);
                            }
                            continue;
                        } else if (itr.hasNext()) {
                            // if the next doctree is a break, remove trailing spaces
                            peekedNext = alist.get(itr.nextIndex());
                            boolean sbrk = isSentenceBreak(peekedNext, false);
                            if (sbrk) {
                                DocTree next = itr.next();
                                s = s.stripTrailing();
                                DCText text = this.at(spos).newTextTree(s);
                                fs.add(text);
                                body.add((DCTree) next);
                                foundFirstSentence = true;
                                continue;
                            }
                        }
                        break;
                    default:
                        if (isSentenceBreak(dt, isFirst)) {
                            body.add((DCTree) dt);
                            foundFirstSentence = true;
                            continue;
                        }
                        break;
                }
                fs.add((DCTree) dt);
            }
            return new Pair<>(fs.toList(), body.toList());
        } finally {
            this.pos = savedpos;
        }
    }

    private boolean isTextTree(DocTree tree) {
        return tree.getKind() == Kind.TEXT;
    }

    /*
     * Computes the first sentence break, a simple dot-space algorithm.
     */
    private int defaultSentenceBreak(String s) {
        // scan for period followed by whitespace
        int period = -1;
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case '.':
                    period = i;
                    break;

                case ' ':
                case '\f':
                case '\n':
                case '\r':
                case '\t':
                    if (period >= 0) {
                        return i;
                    }
                    break;

                default:
                    period = -1;
                    break;
            }
        }
        return -1;
    }

    /*
     * Computes the first sentence, if using a default breaker,
     * the break is returned, if not then a -1, indicating that
     * more doctree elements are required to be examined.
     *
     * BreakIterator.next points to the the start of the following sentence,
     * and does not provide an easy way to disambiguate between "sentence break",
     * "possible sentence break" and "not a sentence break" at the end of the input.
     * For example, BreakIterator.next returns the index for the end
     * of the string for all of these examples,
     * using vertical bars to delimit the bounds of the example text
     * |Abc|        (not a valid end of sentence break, if followed by more text)
     * |Abc.|       (maybe a valid end of sentence break, depending on the following text)
     * |Abc. |      (maybe a valid end of sentence break, depending on the following text)
     * |"Abc." |    (maybe a valid end of sentence break, depending on the following text)
     * |Abc.  |     (definitely a valid end of sentence break)
     * |"Abc."  |   (definitely a valid end of sentence break)
     * Therefore, we have to probe further to determine whether
     * there really is a sentence break or not at the end of this run of text.
     */
    private int getSentenceBreak(String s, DocTree dt) {
        BreakIterator breakIterator = trees.getBreakIterator();
        if (breakIterator == null) {
            return defaultSentenceBreak(s);
        }
        breakIterator.setText(s);
        final int sbrk = breakIterator.next();
        // This is the last doctree, found the droid we are looking for
        if (dt == null) {
            return sbrk;
        }

        // If the break is well within the span of the string ie. not
        // at EOL, then we have a clear break.
        if (sbrk < s.length() - 1) {
            return sbrk;
        }

        if (isTextTree(dt)) {
            // Two adjacent text trees, a corner case, perhaps
            // produced by a tool synthesizing a doctree. In
            // this case, does the break lie within the first span,
            // then we have the droid, otherwise allow the callers
            // logic to handle the break in the adjacent doctree.
            TextTree ttnext = (TextTree) dt;
            String combined = s + ttnext.getBody();
            breakIterator.setText(combined);
            int sbrk2 = breakIterator.next();
            if (sbrk < sbrk2) {
                return sbrk;
            }
        }

        // Is the adjacent tree a sentence breaker ?
        if (isSentenceBreak(dt, false)) {
            return sbrk;
        }

        // At this point the adjacent tree is either a javadoc tag ({@..),
        // html tag (<..) or an entity (&..). Perform a litmus test, by
        // concatenating a sentence, to validate the break earlier identified.
        String combined = s + "Dummy Sentence.";
        breakIterator.setText(combined);
        int sbrk2 = breakIterator.next();
        if (sbrk2 <= sbrk) {
            return sbrk2;
        }
        return -1; // indeterminate at this time
    }

    private boolean isSentenceBreak(Name tagName) {
        return sentenceBreakTags.contains(StringUtils.toUpperCase(tagName.toString()));
    }

    private boolean isSentenceBreak(DocTree dt, boolean isFirstDocTree) {
        switch (dt.getKind()) {
            case START_ELEMENT:
                    StartElementTree set = (StartElementTree)dt;
                    return !isFirstDocTree && ((DCTree) dt).pos > 1 && isSentenceBreak(set.getName());
            case END_ELEMENT:
                    EndElementTree eet = (EndElementTree)dt;
                    return !isFirstDocTree && ((DCTree) dt).pos > 1 && isSentenceBreak(eet.getName());
            default:
                return false;
        }
    }

    /*
     * Returns the position of the the first non-white space
     */
    private int skipWhiteSpace(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isWhitespace(c)) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private List<DCTree> cast(List<? extends DocTree> list) {
        return (List<DCTree>) list;
    }
}
