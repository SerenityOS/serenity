/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.formats.html;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor14;

import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.IndexTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SystemPropertyTree;
import com.sun.source.doctree.ThrowsTree;
import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlId;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.RawHtml;
import jdk.javadoc.internal.doclets.formats.html.markup.TagName;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.DocletElement;
import jdk.javadoc.internal.doclets.toolkit.Resources;
import jdk.javadoc.internal.doclets.toolkit.builders.SerializedFormBuilder;
import jdk.javadoc.internal.doclets.toolkit.taglets.ParamTaglet;
import jdk.javadoc.internal.doclets.toolkit.taglets.TagletWriter;
import jdk.javadoc.internal.doclets.toolkit.util.CommentHelper;
import jdk.javadoc.internal.doclets.toolkit.util.DocLink;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.DocPaths;
import jdk.javadoc.internal.doclets.toolkit.util.IndexItem;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

/**
 * The taglet writer that writes HTML.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */

public class TagletWriterImpl extends TagletWriter {
    /**
     * A class that provides the information about the enclosing context for
     * a series of {@code DocTree} nodes.
     * This context may be used to determine the content that should be generated from the tree nodes.
     */
    static class Context {
        /**
         * Whether or not the trees are appearing in a context of just the first sentence,
         * such as in the summary table of the enclosing element.
         */
        final boolean isFirstSentence;
        /**
         * Whether or not the trees are appearing in the "summary" section of the
         * page for a declaration.
         */
        final boolean inSummary;
        /**
         * The set of enclosing kinds of tags.
         */
        final Set<DocTree.Kind> inTags;

        /**
         * Creates an outermost context, with no enclosing tags.
         *
         * @param isFirstSentence {@code true} if the trees are appearing in a context of just the
         *                        first sentence and {@code false} otherwise
         * @param inSummary       {@code true} if the trees are appearing in the "summary" section
         *                        of the page for a declaration and {@code false} otherwise
         */
        Context(boolean isFirstSentence, boolean inSummary) {
            this(isFirstSentence, inSummary, EnumSet.noneOf(DocTree.Kind.class));
        }

        private Context(boolean isFirstSentence, boolean inSummary, Set<DocTree.Kind> inTags) {
            this.isFirstSentence = isFirstSentence;
            this.inSummary = inSummary;
            this.inTags = inTags;
        }

        /**
         * Creates a new {@code Context} that includes an extra tag kind in the set of enclosing
         * kinds of tags.
         *
         * @param tree the enclosing tree
         *
         * @return the new {@code Context}
         */
        Context within(DocTree tree) {
            var newInTags = EnumSet.copyOf(inTags);
            newInTags.add(tree.getKind());
            return new Context(isFirstSentence, inSummary, newInTags);
        }
    }

    private final HtmlDocletWriter htmlWriter;
    private final HtmlConfiguration configuration;
    private final HtmlOptions options;
    private final Utils utils;
    private final Resources resources;
    private final Contents contents;
    private final Context context;

    // Threshold for length of @see tag label for switching from inline to block layout.
    private static final int SEE_TAG_MAX_INLINE_LENGTH = 30;

    /**
     * Creates a taglet writer.
     *
     * @param htmlWriter      the {@code HtmlDocletWriter} for the page
     * @param isFirstSentence {@code true} if this taglet writer is being used for a
     *                        "first sentence" summary
     */
    public TagletWriterImpl(HtmlDocletWriter htmlWriter, boolean isFirstSentence) {
        this(htmlWriter, isFirstSentence, false);
    }

    /**
     * Creates a taglet writer.
     *
     * @param htmlWriter      the {@code HtmlDocletWriter} for the page
     * @param isFirstSentence {@code true} if this taglet writer is being used for a
     *                        "first sentence" summary, and {@code false} otherwise
     * @param inSummary       {@code true} if this taglet writer is being used for the content
     *                        of a {@code {@summary ...}} tag, and {@code false} otherwise
     */
    public TagletWriterImpl(HtmlDocletWriter htmlWriter, boolean isFirstSentence, boolean inSummary) {
        this(htmlWriter, new Context(isFirstSentence, inSummary));
    }

    /**
     * Creates a taglet writer.
     *
     * @param htmlWriter the {@code HtmlDocletWriter} for the page
     * @param context    the enclosing context for any tags
     */
    public TagletWriterImpl(HtmlDocletWriter htmlWriter, Context context) {
        super(context.isFirstSentence);
        this.htmlWriter = htmlWriter;
        this.context = context;
        configuration = htmlWriter.configuration;
        options = configuration.getOptions();
        utils = configuration.utils;
        resources = configuration.getDocResources();
        contents = configuration.getContents();
    }

    @Override
    public Content getOutputInstance() {
        return new ContentBuilder();
    }

    @Override
    protected Content codeTagOutput(Element element, DocTree tag) {
        CommentHelper ch = utils.getCommentHelper(element);
        Content result = HtmlTree.CODE(Text.of(utils.normalizeNewlines(ch.getText(tag))));
        return result;
    }

    @Override
    protected Content indexTagOutput(Element element, IndexTree tag) {
        CommentHelper ch = utils.getCommentHelper(element);

        String tagText = ch.getText(tag.getSearchTerm());
        if (tagText.charAt(0) == '"' && tagText.charAt(tagText.length() - 1) == '"') {
            tagText = tagText.substring(1, tagText.length() - 1)
                             .replaceAll("\\s+", " ");
        }

        Content desc = htmlWriter.commentTagsToContent(tag, element, tag.getDescription(), context.within(tag));
        String descText = extractText(desc);

        return createAnchorAndSearchIndex(element, tagText, descText, tag);
    }

    // ugly but simple;
    // alternatives would be to walk the Content tree, or to add new functionality to Content
    private String extractText(Content c) {
        return c.toString().replaceAll("<[^>]+>", "");
    }

    @Override
    public Content getDocRootOutput() {
        String path;
        if (htmlWriter.pathToRoot.isEmpty())
            path = ".";
        else
            path = htmlWriter.pathToRoot.getPath();
        return Text.of(path);
    }

    @Override
    public Content deprecatedTagOutput(Element element) {
        ContentBuilder result = new ContentBuilder();
        CommentHelper ch = utils.getCommentHelper(element);
        List<? extends DeprecatedTree> deprs = utils.getDeprecatedTrees(element);
        if (utils.isTypeElement(element)) {
            if (utils.isDeprecated(element)) {
                result.add(HtmlTree.SPAN(HtmlStyle.deprecatedLabel,
                        htmlWriter.getDeprecatedPhrase(element)));
                if (!deprs.isEmpty()) {
                    List<? extends DocTree> commentTrees = ch.getDescription(deprs.get(0));
                    if (!commentTrees.isEmpty()) {
                        result.add(commentTagsToOutput(element, null, commentTrees, false));
                    }
                }
            }
        } else {
            if (utils.isDeprecated(element)) {
                result.add(HtmlTree.SPAN(HtmlStyle.deprecatedLabel,
                        htmlWriter.getDeprecatedPhrase(element)));
                if (!deprs.isEmpty()) {
                    List<? extends DocTree> bodyTrees = ch.getBody(deprs.get(0));
                    Content body = commentTagsToOutput(element, null, bodyTrees, false);
                    if (!body.isEmpty())
                        result.add(HtmlTree.DIV(HtmlStyle.deprecationComment, body));
                }
            } else {
                Element ee = utils.getEnclosingTypeElement(element);
                if (utils.isDeprecated(ee)) {
                    result.add(HtmlTree.SPAN(HtmlStyle.deprecatedLabel,
                        htmlWriter.getDeprecatedPhrase(ee)));
                }
            }
        }
        return result;
    }

    @Override
    protected Content literalTagOutput(Element element, LiteralTree tag) {
        CommentHelper ch = utils.getCommentHelper(element);
        Content result = Text.of(utils.normalizeNewlines(ch.getText(tag)));
        return result;
    }

    @Override
    public Content getParamHeader(ParamTaglet.ParamKind kind) {
        Content header;
        switch (kind) {
            case PARAMETER:         header = contents.parameters ; break;
            case TYPE_PARAMETER:    header = contents.typeParameters ; break;
            case RECORD_COMPONENT:  header = contents.recordComponents ; break;
            default: throw new IllegalArgumentException(kind.toString());
        }
        return HtmlTree.DT(header);
    }

    @Override
    public Content paramTagOutput(Element element, ParamTree paramTag, String paramName) {
        ContentBuilder body = new ContentBuilder();
        CommentHelper ch = utils.getCommentHelper(element);
        // define id attributes for state components so that generated descriptions may refer to them
        boolean defineID = (element.getKind() == ElementKind.RECORD)
                && !paramTag.isTypeParameter();
        Content nameTree = Text.of(paramName);
        body.add(HtmlTree.CODE(defineID ? HtmlTree.SPAN_ID(HtmlIds.forParam(paramName), nameTree) : nameTree));
        body.add(" - ");
        List<? extends DocTree> description = ch.getDescription(paramTag);
        body.add(htmlWriter.commentTagsToContent(paramTag, element, description, context.within(paramTag)));
        return HtmlTree.DD(body);
    }

    @Override
    public Content returnTagOutput(Element element, ReturnTree returnTag, boolean inline) {
        CommentHelper ch = utils.getCommentHelper(element);
        List<? extends DocTree> desc = ch.getDescription(returnTag);
        Content content = htmlWriter.commentTagsToContent(returnTag, element, desc , context.within(returnTag));
        return inline
                ? new ContentBuilder(contents.getContent("doclet.Returns_0", content))
                : new ContentBuilder(HtmlTree.DT(contents.returns), HtmlTree.DD(content));
    }

    @Override
    public Content seeTagOutput(Element holder, List<? extends SeeTree> seeTags) {
        List<Content> links = new ArrayList<>();
        for (DocTree dt : seeTags) {
            links.add(htmlWriter.seeTagToContent(holder, dt, context.within(dt)));
        }
        if (utils.isVariableElement(holder) && ((VariableElement)holder).getConstantValue() != null &&
                htmlWriter instanceof ClassWriterImpl writer) {
            //Automatically add link to constant values page for constant fields.
            DocPath constantsPath =
                    htmlWriter.pathToRoot.resolve(DocPaths.CONSTANT_VALUES);
            String whichConstant =
                    writer.getTypeElement().getQualifiedName() + "." +
                    utils.getSimpleName(holder);
            DocLink link = constantsPath.fragment(whichConstant);
            links.add(htmlWriter.links.createLink(link,
                    contents.getContent("doclet.Constants_Summary")));
        }
        if (utils.isClass(holder) && utils.isSerializable((TypeElement)holder)) {
            //Automatically add link to serialized form page for serializable classes.
            if (SerializedFormBuilder.serialInclude(utils, holder) &&
                      SerializedFormBuilder.serialInclude(utils, utils.containingPackage(holder))) {
                DocPath serialPath = htmlWriter.pathToRoot.resolve(DocPaths.SERIALIZED_FORM);
                DocLink link = serialPath.fragment(utils.getFullyQualifiedName(holder));
                links.add(htmlWriter.links.createLink(link,
                        contents.getContent("doclet.Serialized_Form")));
            }
        }
        if (links.isEmpty()) {
            return Text.EMPTY;
        }
        // Use a different style if any link label is longer than 30 chars or contains commas.
        boolean hasLongLabels = links.stream()
                .anyMatch(c -> c.charCount() > SEE_TAG_MAX_INLINE_LENGTH || c.toString().contains(","));
        HtmlTree seeList = new HtmlTree(TagName.UL)
                .setStyle(hasLongLabels ? HtmlStyle.seeListLong : HtmlStyle.seeList);
        links.stream().filter(Content::isValid).forEach(item -> {
            seeList.add(HtmlTree.LI(item));
        });

        return new ContentBuilder(
                HtmlTree.DT(contents.seeAlso),
                HtmlTree.DD(seeList));
    }

    @Override
    public Content simpleBlockTagOutput(Element element, List<? extends DocTree> simpleTags, String header) {
        CommentHelper ch = utils.getCommentHelper(element);
        ContentBuilder body = new ContentBuilder();
        boolean many = false;
        for (DocTree simpleTag : simpleTags) {
            if (many) {
                body.add(", ");
            }
            List<? extends DocTree> bodyTags = ch.getBody(simpleTag);
            body.add(htmlWriter.commentTagsToContent(simpleTag, element, bodyTags, context.within(simpleTag)));
            many = true;
        }
        return new ContentBuilder(
                HtmlTree.DT(new RawHtml(header)),
                HtmlTree.DD(body));
    }

    @Override
    protected Content systemPropertyTagOutput(Element element, SystemPropertyTree tag) {
        String tagText = tag.getPropertyName().toString();
        return HtmlTree.CODE(createAnchorAndSearchIndex(element, tagText,
                resources.getText("doclet.System_Property"), tag));
    }

    @Override
    public Content getThrowsHeader() {
        return HtmlTree.DT(contents.throws_);
    }

    @Override
    public Content throwsTagOutput(Element element, ThrowsTree throwsTag, TypeMirror substituteType) {
        ContentBuilder body = new ContentBuilder();
        CommentHelper ch = utils.getCommentHelper(element);
        Element exception = ch.getException(throwsTag);
        Content excName;
        if (substituteType != null) {
           excName = htmlWriter.getLink(new HtmlLinkInfo(configuration, HtmlLinkInfo.Kind.MEMBER,
                   substituteType));
        } else if (exception == null) {
            excName = new RawHtml(ch.getExceptionName(throwsTag).toString());
        } else if (exception.asType() == null) {
            excName = new RawHtml(utils.getFullyQualifiedName(exception));
        } else {
            HtmlLinkInfo link = new HtmlLinkInfo(configuration, HtmlLinkInfo.Kind.MEMBER,
                                                 exception.asType());
            link.excludeTypeBounds = true;
            excName = htmlWriter.getLink(link);
        }
        body.add(HtmlTree.CODE(excName));
        List<? extends DocTree> description = ch.getDescription(throwsTag);
        Content desc = htmlWriter.commentTagsToContent(throwsTag, element, description, context.within(throwsTag));
        if (desc != null && !desc.isEmpty()) {
            body.add(" - ");
            body.add(desc);
        }
        HtmlTree result = HtmlTree.DD(body);
        return result;
    }

    @Override
    public Content throwsTagOutput(TypeMirror throwsType) {
        HtmlTree result = HtmlTree.DD(HtmlTree.CODE(htmlWriter.getLink(
                new HtmlLinkInfo(configuration, HtmlLinkInfo.Kind.MEMBER, throwsType))));
        return result;
    }

    @Override
    public Content valueTagOutput(VariableElement field, String constantVal, boolean includeLink) {
        return includeLink
                ? htmlWriter.getDocLink(HtmlLinkInfo.Kind.VALUE_TAG, field, constantVal)
                : Text.of(constantVal);
    }

    @Override
    public Content commentTagsToOutput(DocTree holder, List<? extends DocTree> tags) {
        return commentTagsToOutput(null, holder, tags, false);
    }

    @Override
    public Content commentTagsToOutput(Element element, List<? extends DocTree> tags) {
        return commentTagsToOutput(element, null, tags, false);
    }

    @Override
    public Content commentTagsToOutput(Element holder,
                                       DocTree holderTag,
                                       List<? extends DocTree> tags,
                                       boolean isFirstSentence)
    {
        return htmlWriter.commentTagsToContent(holderTag, holder,
                tags, holderTag == null ? context : context.within(holderTag));
    }

    @Override
    public BaseConfiguration configuration() {
        return configuration;
    }

    @Override
    protected TypeElement getCurrentPageElement() {
        return htmlWriter.getCurrentPageElement();
    }

    private Content createAnchorAndSearchIndex(Element element, String tagText, String desc, DocTree tree) {
        Content result = null;
        if (context.isFirstSentence && context.inSummary || context.inTags.contains(DocTree.Kind.INDEX)) {
            result = Text.of(tagText);
        } else {
            HtmlId id = HtmlIds.forText(tagText, htmlWriter.indexAnchorTable);
            result = HtmlTree.SPAN(id, HtmlStyle.searchTagResult, Text.of(tagText));
            if (options.createIndex() && !tagText.isEmpty()) {
                String holder = new SimpleElementVisitor14<String, Void>() {

                    @Override
                    public String visitModule(ModuleElement e, Void p) {
                        return resources.getText("doclet.module")
                                + " " + utils.getFullyQualifiedName(e);
                    }

                    @Override
                    public String visitPackage(PackageElement e, Void p) {
                        return resources.getText("doclet.package")
                                + " " + utils.getFullyQualifiedName(e);
                    }

                    @Override
                    public String visitType(TypeElement e, Void p) {
                        return utils.getTypeElementKindName(e, true)
                                + " " + utils.getFullyQualifiedName(e);
                    }

                    @Override
                    public String visitExecutable(ExecutableElement e, Void p) {
                        return utils.getFullyQualifiedName(utils.getEnclosingTypeElement(e))
                                + "." + utils.getSimpleName(e)
                                + utils.flatSignature(e, htmlWriter.getCurrentPageElement());
                    }

                    @Override
                    public String visitVariable(VariableElement e, Void p) {
                        return utils.getFullyQualifiedName(utils.getEnclosingTypeElement(e))
                                + "." + utils.getSimpleName(e);
                    }

                    @Override
                    public String visitUnknown(Element e, Void p) {
                        if (e instanceof DocletElement de) {
                            return switch (de.getSubKind()) {
                                case OVERVIEW -> resources.getText("doclet.Overview");
                                case DOCFILE -> getHolderName(de);
                            };
                        } else {
                            return super.visitUnknown(e, p);
                        }
                    }

                    @Override
                    protected String defaultAction(Element e, Void p) {
                        return utils.getFullyQualifiedName(e);
                    }
                }.visit(element);
                IndexItem item = IndexItem.of(element, tree, tagText, holder, desc,
                        new DocLink(htmlWriter.path, id.name()));
                configuration.mainIndex.add(item);
            }
        }
        return result;
    }

    private String getHolderName(DocletElement de) {
        PackageElement pe = de.getPackageElement();
        if (pe.isUnnamed()) {
            // if package is unnamed use enclosing module only if it is named
            Element ee = pe.getEnclosingElement();
            if (ee instanceof ModuleElement && !((ModuleElement)ee).isUnnamed()) {
                return resources.getText("doclet.module") + " " + utils.getFullyQualifiedName(ee);
            }
            return pe.toString(); // "Unnamed package" or similar
        }
        return resources.getText("doclet.package") + " " + utils.getFullyQualifiedName(pe);
    }
}
