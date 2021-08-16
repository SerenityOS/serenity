/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EnumSet;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import com.sun.source.doctree.DocTree;
import jdk.javadoc.doclet.Taglet.Location;
import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.Messages;
import jdk.javadoc.internal.doclets.toolkit.util.CommentHelper;
import jdk.javadoc.internal.doclets.toolkit.util.DocFinder;
import jdk.javadoc.internal.doclets.toolkit.util.DocFinder.Input;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

/**
 * An inline Taglet representing the {@code inheritDoc} tag. This tag should only
 * be used with a method.  It is used to inherit documentation from overridden
 * and implemented methods.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */

public class InheritDocTaglet extends BaseTaglet {

    /**
     * Construct a new InheritDocTaglet.
     */
    public InheritDocTaglet () {
        super(DocTree.Kind.INHERIT_DOC, true, EnumSet.of(Location.TYPE, Location.METHOD));
    }

    /**
     * Given an element, a {@code DocTree} in the element's doc comment
     * replace all occurrences of @inheritDoc with documentation from its
     * superclass or superinterface.
     *
     * @param writer the writer that is writing the output.
     * @param e the {@link Element} that we are documenting.
     * @param holderTag the tag that holds the inheritDoc tag or null for type
     * (class) docs.
     * @param isFirstSentence true if we only want to inherit the first sentence.
     */
    private Content retrieveInheritedDocumentation(TagletWriter writer,
            Element e, DocTree holderTag, boolean isFirstSentence) {
        Content replacement = writer.getOutputInstance();
        BaseConfiguration configuration = writer.configuration();
        Messages messages = configuration.getMessages();
        Utils utils = configuration.utils;
        CommentHelper ch = utils.getCommentHelper(e);
        Taglet inheritableTaglet = holderTag == null
                ? null
                : configuration.tagletManager.getTaglet(ch.getTagName(holderTag));
        if (inheritableTaglet != null &&
            !(inheritableTaglet instanceof InheritableTaglet)) {
                String message = utils.getSimpleName(e) +
                    ((utils.isExecutableElement(e))
                        ? utils.flatSignature((ExecutableElement)e, writer.getCurrentPageElement())
                        : "");
                //This tag does not support inheritance.
                messages.warning(e, "doclet.noInheritedDoc", message);
        }
        Input input = new DocFinder.Input(utils, e,
                (InheritableTaglet) inheritableTaglet, new DocFinder.DocTreeInfo(holderTag, e),
                isFirstSentence, true);
        DocFinder.Output inheritedDoc = DocFinder.search(configuration, input);
        if (inheritedDoc.isValidInheritDocTag) {
            if (!inheritedDoc.inlineTags.isEmpty()) {
                replacement = writer.commentTagsToOutput(inheritedDoc.holder, inheritedDoc.holderTag,
                    inheritedDoc.inlineTags, isFirstSentence);
            }

        } else {
            String message = utils.getSimpleName(e) +
                    ((utils.isExecutableElement(e))
                        ? utils.flatSignature((ExecutableElement)e, writer.getCurrentPageElement())
                        : "");
            messages.warning(e, "doclet.noInheritedDoc", message);
        }
        return replacement;
    }

    @Override
    public Content getInlineTagOutput(Element e, DocTree tag, TagletWriter tagletWriter) {
        DocTree inheritTag = (tag.getKind() == DocTree.Kind.INHERIT_DOC) ? null : tag;
        return retrieveInheritedDocumentation(tagletWriter, e,
                inheritTag, tagletWriter.isFirstSentence);
    }
}
