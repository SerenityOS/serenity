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

package jdk.javadoc.internal.doclets.toolkit.taglets;

import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.IndexTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SystemPropertyTree;
import com.sun.source.doctree.ThrowsTree;
import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.taglets.Taglet.UnsupportedTagletOperationException;
import jdk.javadoc.internal.doclets.toolkit.util.CommentHelper;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

/**
 * The interface for the taglet writer.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */

public abstract class TagletWriter {

    /**
     * True if we only want to write the first sentence.
     */
    protected final boolean isFirstSentence;

    protected TagletWriter(boolean isFirstSentence) {
        this.isFirstSentence = isFirstSentence;
    }

    /**
     * Returns an instance of an output object.
     *
     * @return an instance of an output object
     */
    public abstract Content getOutputInstance();

    /**
     * Returns the output for a {@code {@code ...}} tag.
     *
     * @param element The element that owns the doc comment
     * @param tag     the tag
     *
     * @return the output
     */
    protected abstract Content codeTagOutput(Element element, DocTree tag);

    /**
     * Returns the output for a {@code {@index...}} tag.
     *
     * @param element The element that owns the doc comment
     * @param tag     the tag
     *
     * @return the output
     */
    protected abstract Content indexTagOutput(Element element, IndexTree tag);

    /**
     * Returns the output for a {@code {@docRoot}} tag.
     *
     * @return the output
     */
    protected abstract Content getDocRootOutput();

    /**
     * Returns the output for a {@code @deprecated} tag.
     *
     * @param element The element that owns the doc comment
     *
     * @return the output
     */
    protected abstract Content deprecatedTagOutput(Element element);

    /**
     * Returns the output for a {@code {@literal ...}} tag.
     *
     * @param element The element that owns the doc comment
     * @param tag     the tag
     *
     * @return the output
     */
    protected abstract Content literalTagOutput(Element element, LiteralTree tag);

    /**
     * Returns the header for the {@code @param} tags.
     *
     * @param kind the kind of header that is required
     *
     * @return the header
     */
    protected abstract Content getParamHeader(ParamTaglet.ParamKind kind);

    /**
     * Returns the output for a {@code @param} tag.
     * Note we cannot rely on the name in the tag, because we might be
     * inheriting the tag.
     *
     * @param element   The element that owns the doc comment
     * @param paramTag  the parameter to document
     * @param paramName the name of the parameter
     *
     * @return the output
     */
    protected abstract Content paramTagOutput(Element element, ParamTree paramTag, String paramName);

    /**
     * Returns the output for a {@code @return} tag.
     *
     * @param element   the element that owns the doc comment
     * @param returnTag the return tag to document
     * @param inline    whether this should be written as an inline instance or block instance
     *
     * @return the output
     */
    protected abstract Content returnTagOutput(Element element, ReturnTree returnTag, boolean inline);

    /**
     * Returns the output for {@code @see} tags.
     *
     * @param element The element that owns the doc comment
     * @param seeTags the list of tags
     *
     * @return the output
     */
    protected abstract Content seeTagOutput(Element element, List<? extends SeeTree> seeTags);

    /**
     * Returns the output for a series of simple tags.
     *
     * @param element    The element that owns the doc comment
     * @param simpleTags the list of simple tags
     * @param header     the header for the series of tags
     *
     * @return the output
     */
    protected abstract Content simpleBlockTagOutput(Element element, List<? extends DocTree> simpleTags, String header);

    /**
     * Returns the output for a {@code {@systemProperty...}} tag.
     *
     * @param element           The element that owns the doc comment
     * @param systemPropertyTag the system property tag
     *
     * @return the output
     */
    protected abstract Content systemPropertyTagOutput(Element element, SystemPropertyTree systemPropertyTag);

    /**
     * Returns the header for the {@code @throws} tag.
     *
     * @return the header for the throws tag
     */
    protected abstract Content getThrowsHeader();

    /**
     * Returns the output for a {@code @throws} tag.
     *
     * @param element        The element that owns the doc comment
     * @param throwsTag      the throws tag
     * @param substituteType instantiated type of a generic type-variable, or null
     *
     * @return the output
     */
    protected abstract Content throwsTagOutput(Element element, ThrowsTree throwsTag, TypeMirror substituteType);

    /**
     * Returns the output for a default {@code @throws} tag.
     *
     * @param throwsType the type that is thrown
     *
     * @return the output
     */
    protected abstract Content throwsTagOutput(TypeMirror throwsType);

    /**
     * Returns the output for a {@code {@value}} tag.
     *
     * @param field       the constant field that holds the value tag
     * @param constantVal the constant value to document
     * @param includeLink true if we should link the constant text to the
     *                    constant field itself
     *
     * @return the output
     */
    protected abstract Content valueTagOutput(VariableElement field,
        String constantVal, boolean includeLink);

    /**
     * Returns the main type element of the current page or null for pages that don't have one.
     *
     * @return the type element of the current page or null.
     */
    protected abstract TypeElement getCurrentPageElement();

    /**
     * Returns the content generated from the block tags for a given element.
     * The content is generated according to the order of the list of taglets.
     * The result is a possibly-empty list of the output generated by each
     * of the given taglets for all of the tags they individually support.
     *
     * @param tagletManager the manager that manages the taglets
     * @param element       the element that we are to write tags for
     * @param taglets       the taglets for the tags to write
     *
     * @return the content
     */
    public Content getBlockTagOutput(TagletManager tagletManager,
                                    Element element,
                                    List<Taglet> taglets) {
        for (Taglet t : taglets) {
            if (!t.isBlockTag()) {
                throw new IllegalArgumentException(t.getName());
            }
        }

        Content output = getOutputInstance();
        Utils utils = configuration().utils;
        tagletManager.checkTags(element, utils.getBlockTags(element), false);
        tagletManager.checkTags(element, utils.getFullBody(element), true);
        for (Taglet taglet : taglets) {
            if (utils.isTypeElement(element) && taglet instanceof ParamTaglet) {
                // The type parameters and state components are documented in a special
                // section away from the tag info, so skip here.
                continue;
            }

            if (element.getKind() == ElementKind.MODULE && taglet instanceof BaseTaglet t) {
                switch (t.getTagKind()) {
                    // @uses and @provides are handled separately, so skip here.
                    // See ModuleWriterImpl.computeModulesData
                    case USES:
                    case PROVIDES:
                        continue;
                }
            }

            if (taglet instanceof DeprecatedTaglet) {
                //Deprecated information is documented "inline", not in tag info
                //section.
                continue;
            }

            if (taglet instanceof SimpleTaglet st && !st.enabled) {
                // taglet has been disabled
                continue;
            }

            try {
                Content tagletOutput = taglet.getAllBlockTagOutput(element, this);
                if (tagletOutput != null) {
                    tagletManager.seenTag(taglet.getName());
                    output.add(tagletOutput);
                }
            } catch (UnsupportedTagletOperationException e) {
                // malformed taglet:
                // claims to support block tags (see Taglet.isBlockTag) but does not provide the
                // appropriate method, Taglet.getAllBlockTagOutput.
            }
        }
        return output;
    }

    /**
     * Returns the content generated from an inline tag in the doc comment for a given element,
     * or {@code null} if the tag is not supported or does not return any output.
     *
     * @param holder        the element associated with the doc comment
     * @param tagletManager the taglet manager for the current doclet
     * @param holderTag     the tag that holds this inline tag, or {@code null} if
     *                      there is no tag that holds it
     * @param inlineTag     the inline tag to be documented
     *
     * @return the content, or {@code null}
     */
    public Content getInlineTagOutput(Element holder,
                                      TagletManager tagletManager,
                                      DocTree holderTag,
                                      DocTree inlineTag) {

        Map<String, Taglet> inlineTags = tagletManager.getInlineTaglets();
        CommentHelper ch = configuration().utils.getCommentHelper(holder);
        final String inlineTagName = ch.getTagName(inlineTag);
        Taglet t = inlineTags.get(inlineTagName);
        if (t == null) {
            return null;
        }

        try {
            Content tagletOutput = t.getInlineTagOutput(holder,
                    holderTag != null && t.getName().equals("inheritDoc") ? holderTag : inlineTag,
                    this);
            tagletManager.seenTag(t.getName());
            return tagletOutput;
        } catch (UnsupportedTagletOperationException e) {
            // malformed taglet:
            // claims to support inline tags (see Taglet.isInlineTag) but does not provide the
            // appropriate method, Taglet.getInlineTagOutput.
            return null;
        }
    }

    /**
     * Converts inline tags and text to content, expanding the
     * inline tags along the way.  Called wherever text can contain
     * an inline tag, such as in comments or in free-form text arguments
     * to block tags.
     *
     * @param holderTree the tree that holds the documentation
     * @param trees      list of {@code DocTree} nodes containing text and inline tags (often alternating)
     *                   present in the text of interest for this doc
     *
     * @return the generated content
     */
    public abstract Content commentTagsToOutput(DocTree holderTree, List<? extends DocTree> trees);

    /**
     * Converts inline tags and text to content, expanding the
     * inline tags along the way.  Called wherever text can contain
     * an inline tag, such as in comments or in free-form text arguments
     * to block tags.
     *
     * @param element The element that owns the documentation
     * @param trees  list of {@code DocTree} nodes containing text and inline tags (often alternating)
     *               present in the text of interest for this doc
     *
     * @return the generated content
     */
    public abstract Content commentTagsToOutput(Element element, List<? extends DocTree> trees);

    /**
     * Converts inline tags and text to content, expanding the
     * inline tags along the way.  Called wherever text can contain
     * an inline tag, such as in comments or in free-form text arguments
     * to non-inline tags.
     *
     * @param element         the element where comment resides
     * @param holder          the tag that holds the documentation
     * @param trees           array of text tags and inline tags (often alternating)
     *                        present in the text of interest for this doc
     * @param isFirstSentence true if this is the first sentence
     *
     * @return the generated content
     */
    public abstract Content commentTagsToOutput(Element element, DocTree holder,
                                                List<? extends DocTree> trees, boolean isFirstSentence);

    /**
     * Returns an instance of the configuration used for this doclet.
     *
     * @return an instance of the configuration used for this doclet
     */
    public abstract BaseConfiguration configuration();
}
