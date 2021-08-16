/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import com.sun.source.doctree.DocTree;
import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;
import jdk.javadoc.internal.doclets.toolkit.taglets.InheritableTaglet;

/**
 * Search for the requested documentation.  Inherit documentation if necessary.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class DocFinder {

    public static final class DocTreeInfo {
        public final DocTree docTree;
        public final Element element;

        public DocTreeInfo() {
            this.docTree = null;
            this.element = null;
        }

        public DocTreeInfo(DocTree docTree, Element baseElement) {
            this.docTree = docTree;
            this.element = baseElement;
        }

        @Override
        public String toString() {
            return "DocTreeInfo{" + "docTree=" + docTree + ", element=" + element + '}';
        }
    }

    /**
     * The class that encapsulates the input.
     */
    public static class Input {
        /**
         * The element to search documentation from.
         */
        public Element element;
        /**
         * The taglet to search for documentation on behalf of. Null if we want
         * to search for overall documentation.
         */
        public InheritableTaglet taglet = null;

        /**
         * The id of the tag to retrieve documentation for.
         */
        public String tagId = null;

        /**
         * The tag to retrieve documentation for.  This is only used for the
         * inheritDoc tag.
         */
        public final DocTreeInfo docTreeInfo;

        /**
         * True if we only want to search for the first sentence.
         */
        public boolean isFirstSentence = false;

        /**
         * True if we are looking for documentation to replace the inheritDocTag.
         */
        public boolean isInheritDocTag = false;

        /**
         * Used to distinguish between type variable param tags and regular
         * param tags.
         */
        public boolean isTypeVariableParamTag = false;

        public final Utils utils;

        public Input(Utils utils, Element element, InheritableTaglet taglet, DocTreeInfo dtInfo,
                boolean isFirstSentence, boolean isInheritDocTag) {
            this.utils = utils;
            this.element = element;
            this.taglet = taglet;
            this.isFirstSentence = isFirstSentence;
            this.isInheritDocTag = isInheritDocTag;
            this.docTreeInfo = dtInfo;
        }

        public Input(Utils utils, Element element, InheritableTaglet taglet, String tagId) {
            this(utils, element);
            this.taglet = taglet;
            this.tagId = tagId;
        }

        public Input(Utils utils, Element element, InheritableTaglet taglet, String tagId,
            boolean isTypeVariableParamTag) {
            this(utils, element);
            this.taglet = taglet;
            this.tagId = tagId;
            this.isTypeVariableParamTag = isTypeVariableParamTag;
        }

        public Input(Utils utils, Element element, InheritableTaglet taglet) {
            this(utils, element);
            this.taglet = taglet;
        }

        public Input(Utils utils, Element element) {
            if (element == null)
                throw new NullPointerException();
            this.element = element;
            this.utils = utils;
            this.docTreeInfo = new DocTreeInfo();
        }

        public Input(Utils utils, Element element, boolean isFirstSentence) {
            this(utils, element);
            this.isFirstSentence = isFirstSentence;
        }

        public Input copy(Utils utils) {
            if (this.element == null) {
                throw new NullPointerException();
            }
            Input clone = new Input(utils, this.element, this.taglet, this.docTreeInfo,
                    this.isFirstSentence, this.isInheritDocTag);
            clone.tagId = this.tagId;
            clone.isTypeVariableParamTag = this.isTypeVariableParamTag;
            return clone;
        }

        /**
         * For debugging purposes
         * @return string representation
         */
        @Override
        public String toString() {
            String encl = element == null ? "" : element.getEnclosingElement().toString() + "::";
            return "Input{" + "element=" + encl + element
                    + ", taglet=" + taglet
                    + ", tagId=" + tagId + ", tag=" + docTreeInfo
                    + ", isFirstSentence=" + isFirstSentence
                    + ", isInheritDocTag=" + isInheritDocTag
                    + ", isTypeVariableParamTag=" + isTypeVariableParamTag
                    + ", utils=" + utils + '}';
        }
    }

    /**
     * The class that encapsulates the output.
     */
    public static class Output {
        /**
         * The tag that holds the documentation.  Null if documentation
         * is not held by a tag.
         */
        public DocTree holderTag;

        /**
         * The Doc object that holds the documentation.
         */
        public Element holder;

        /**
         * The inherited documentation.
         */
        public List<? extends DocTree> inlineTags = Collections.emptyList();

        /**
         * False if documentation could not be inherited.
         */
        public boolean isValidInheritDocTag = true;

        /**
         * When automatically inheriting throws tags, you sometime must inherit
         * more than one tag.  For example if the element declares that it throws
         * IOException and the overridden element has throws tags for IOException and
         * ZipException, both tags would be inherited because ZipException is a
         * subclass of IOException.  This subclass of DocFinder.Output allows
         * multiple tag inheritance.
         */
        public List<DocTree> tagList  = new ArrayList<>();

        /**
         * Returns a string representation for debugging purposes
         * @return string
         */
        @Override
        public String toString() {
            String encl = holder == null ? "" : holder.getEnclosingElement().toString() + "::";
            return "Output{" + "holderTag=" + holderTag
                    + ", holder=" + encl + holder
                    + ", inlineTags=" + inlineTags
                    + ", isValidInheritDocTag=" + isValidInheritDocTag
                    + ", tagList=" + tagList + '}';
        }
    }

    /**
     * Search for the requested comments in the given element.  If it does not
     * have comments, return documentation from the overridden element if possible.
     * If the overridden element does not exist or does not have documentation to
     * inherit, search for documentation to inherit from implemented methods.
     *
     * @param input the input object used to perform the search.
     *
     * @return an Output object representing the documentation that was found.
     */
    public static Output search(BaseConfiguration configuration, Input input) {
        Output output = new Output();
        Utils utils = configuration.utils;
        if (input.isInheritDocTag) {
            //Do nothing because "element" does not have any documentation.
            //All it has is {@inheritDoc}.
        } else if (input.taglet == null) {
            //We want overall documentation.
            output.inlineTags = input.isFirstSentence
                    ? utils.getFirstSentenceTrees(input.element)
                    : utils.getFullBody(input.element);
            output.holder = input.element;
        } else {
            input.taglet.inherit(input, output);
        }

        if (output.inlineTags != null && !output.inlineTags.isEmpty()) {
            return output;
        }
        output.isValidInheritDocTag = false;
        Input inheritedSearchInput = input.copy(configuration.utils);
        inheritedSearchInput.isInheritDocTag = false;
        if (utils.isMethod(input.element)) {
            ExecutableElement overriddenMethod = utils.overriddenMethod((ExecutableElement) input.element);
            if (overriddenMethod != null) {
                inheritedSearchInput.element = overriddenMethod;
                output = search(configuration, inheritedSearchInput);
                output.isValidInheritDocTag = true;
                if (!output.inlineTags.isEmpty()) {
                    return output;
                }
            }
            //NOTE:  When we fix the bug where ClassDoc.interfaceTypes() does
            //       not pass all implemented interfaces, we will use the
            //       appropriate element here.
            TypeElement encl = utils.getEnclosingTypeElement(input.element);
            VisibleMemberTable vmt = configuration.getVisibleMemberTable(encl);
            List<ExecutableElement> implementedMethods =
                    vmt.getImplementedMethods((ExecutableElement)input.element);
            for (ExecutableElement implementedMethod : implementedMethods) {
                inheritedSearchInput.element = implementedMethod;
                output = search(configuration, inheritedSearchInput);
                output.isValidInheritDocTag = true;
                if (!output.inlineTags.isEmpty()) {
                    return output;
                }
            }
        } else if (utils.isTypeElement(input.element)) {
            TypeMirror t = ((TypeElement) input.element).getSuperclass();
            Element superclass = utils.asTypeElement(t);
            if (superclass != null) {
                inheritedSearchInput.element = superclass;
                output = search(configuration, inheritedSearchInput);
                output.isValidInheritDocTag = true;
                if (!output.inlineTags.isEmpty()) {
                    return output;
                }
            }
        }
        return output;
    }
}
