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

import java.util.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import jdk.javadoc.doclet.Taglet.Location;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.Messages;
import jdk.javadoc.internal.doclets.toolkit.util.CommentHelper;
import jdk.javadoc.internal.doclets.toolkit.util.DocFinder;
import jdk.javadoc.internal.doclets.toolkit.util.DocFinder.Input;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

/**
 * A taglet that represents the {@code @param} tag.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ParamTaglet extends BaseTaglet implements InheritableTaglet {
    public enum ParamKind {
        /** Parameter of an executable element. */
        PARAMETER,
        /** State components of a record. */
        RECORD_COMPONENT,
        /** Type parameters of an executable element or type element. */
        TYPE_PARAMETER
    }

    /**
     * Construct a ParamTaglet.
     */
    public ParamTaglet() {
        super(DocTree.Kind.PARAM, false, EnumSet.of(Location.TYPE, Location.CONSTRUCTOR, Location.METHOD));
    }

    /**
     * Given an array of <code>Parameter</code>s, return
     * a name/rank number map.  If the array is null, then
     * null is returned.
     * @param params The array of parameters (from type or executable member) to
     *               check.
     * @return a name-rank number map.
     */
    private static Map<String, String> getRankMap(Utils utils, List<? extends Element> params){
        if (params == null) {
            return null;
        }
        HashMap<String, String> result = new HashMap<>();
        int rank = 0;
        for (Element e : params) {
            String name = utils.isTypeParameterElement(e)
                    ? utils.getTypeName(e.asType(), false)
                    : utils.getSimpleName(e);
            result.put(name, String.valueOf(rank));
            rank++;
        }
        return result;
    }

    @Override
    public void inherit(DocFinder.Input input, DocFinder.Output output) {
        Utils utils = input.utils;
        if (input.tagId == null) {
            input.isTypeVariableParamTag = ((ParamTree)input.docTreeInfo.docTree).isTypeParameter();
            ExecutableElement ee = (ExecutableElement)input.docTreeInfo.element;
            CommentHelper ch = utils.getCommentHelper(ee);
            List<? extends Element> parameters = input.isTypeVariableParamTag
                    ? ee.getTypeParameters()
                    : ee.getParameters();
            String target = ch.getParameterName(input.docTreeInfo.docTree);
            for (int i = 0 ; i < parameters.size(); i++) {
                Element e = parameters.get(i);
                String pname = input.isTypeVariableParamTag
                        ? utils.getTypeName(e.asType(), false)
                        : utils.getSimpleName(e);
                if (pname.contentEquals(target)) {
                    input.tagId = String.valueOf(i);
                    break;
                }
            }
        }
        ExecutableElement md = (ExecutableElement)input.element;
        CommentHelper ch = utils.getCommentHelper(md);
        List<? extends DocTree> tags = input.isTypeVariableParamTag
                ? utils.getTypeParamTrees(md)
                : utils.getParamTrees(md);
        List<? extends Element> parameters = input.isTypeVariableParamTag
                ? md.getTypeParameters()
                : md.getParameters();
        Map<String, String> rankMap = getRankMap(utils, parameters);
        for (DocTree tag : tags) {
            String paramName = ch.getParameterName(tag);
            if (rankMap.containsKey(paramName) && rankMap.get(paramName).equals((input.tagId))) {
                output.holder = input.element;
                output.holderTag = tag;
                output.inlineTags = ch.getBody(tag);
                return;
            }
        }
    }

    @Override
    public Content getAllBlockTagOutput(Element holder, TagletWriter writer) {
        Utils utils = writer.configuration().utils;
        if (utils.isExecutableElement(holder)) {
            ExecutableElement member = (ExecutableElement) holder;
            Content output = getTagletOutput(ParamKind.TYPE_PARAMETER, member, writer,
                member.getTypeParameters(), utils.getTypeParamTrees(member));
            output.add(getTagletOutput(ParamKind.PARAMETER, member, writer,
                member.getParameters(), utils.getParamTrees(member)));
            return output;
        } else {
            TypeElement typeElement = (TypeElement) holder;
            Content output = getTagletOutput(ParamKind.TYPE_PARAMETER, typeElement, writer,
                typeElement.getTypeParameters(), utils.getTypeParamTrees(typeElement));
            output.add(getTagletOutput(ParamKind.RECORD_COMPONENT, typeElement, writer,
                    typeElement.getRecordComponents(), utils.getParamTrees(typeElement)));
            return output;
        }
    }

    /**
     * Given an array of {@code @param DocTree}s,return its string representation.
     * Try to inherit the param tags that are missing.
     *
     * @param holder            the element that holds the param tags.
     * @param writer            the TagletWriter that will write this tag.
     * @param formalParameters  The array of parameters (from type or executable
     *                          member) to check.
     *
     * @return the content representation of these {@code @param DocTree}s.
     */
    private Content getTagletOutput(ParamKind kind, Element holder,
            TagletWriter writer, List<? extends Element> formalParameters, List<? extends ParamTree> paramTags) {
        Content result = writer.getOutputInstance();
        Set<String> alreadyDocumented = new HashSet<>();
        if (!paramTags.isEmpty()) {
            result.add(
                processParamTags(holder, kind, paramTags,
                getRankMap(writer.configuration().utils, formalParameters), writer, alreadyDocumented)
            );
        }
        if (alreadyDocumented.size() != formalParameters.size()) {
            //Some parameters are missing corresponding @param tags.
            //Try to inherit them.
            result.add(getInheritedTagletOutput(kind, holder,
                writer, formalParameters, alreadyDocumented));
        }
        return result;
    }

    /**
     * Loop through each individual parameter, despite not having a
     * corresponding param tag, try to inherit it.
     */
    private Content getInheritedTagletOutput(ParamKind kind, Element holder,
            TagletWriter writer, List<? extends Element> formalParameters,
            Set<String> alreadyDocumented) {
        Utils utils = writer.configuration().utils;
        Content result = writer.getOutputInstance();
        if ((!alreadyDocumented.contains(null)) && utils.isExecutableElement(holder)) {
            for (int i = 0; i < formalParameters.size(); i++) {
                if (alreadyDocumented.contains(String.valueOf(i))) {
                    continue;
                }
                // This parameter does not have any @param documentation.
                // Try to inherit it.
                Input input = new DocFinder.Input(writer.configuration().utils, holder, this,
                        Integer.toString(i), kind == ParamKind.TYPE_PARAMETER);
                DocFinder.Output inheritedDoc = DocFinder.search(writer.configuration(), input);
                if (inheritedDoc.inlineTags != null && !inheritedDoc.inlineTags.isEmpty()) {
                    Element e = formalParameters.get(i);
                    String lname = kind != ParamKind.TYPE_PARAMETER
                            ? utils.getSimpleName(e)
                            : utils.getTypeName(e.asType(), false);
                    Content content = processParamTag(inheritedDoc.holder, kind, writer,
                            (ParamTree) inheritedDoc.holderTag,
                            lname,
                            alreadyDocumented.isEmpty());
                    result.add(content);
                }
                alreadyDocumented.add(String.valueOf(i));
            }
        }
        return result;
    }

    /**
     * Given an array of {@code @param DocTree}s representing this
     * tag, return its string representation.  Print a warning for param
     * tags that do not map to parameters.  Print a warning for param
     * tags that are duplicated.
     *
     * @param paramTags the array of {@code @param DocTree} to convert.
     * @param writer the TagletWriter that will write this tag.
     * @param alreadyDocumented the set of exceptions that have already
     *        been documented.
     * @param rankMap a {@link java.util.Map} which holds ordering
     *                    information about the parameters.
     * @param rankMap a {@link java.util.Map} which holds a mapping
                of a rank of a parameter to its name.  This is
                used to ensure that the right name is used
                when parameter documentation is inherited.
     * @return the Content representation of this {@code @param DocTree}.
     */
    private Content processParamTags(Element e, ParamKind kind,
            List<? extends ParamTree> paramTags, Map<String, String> rankMap, TagletWriter writer,
            Set<String> alreadyDocumented) {
        Messages messages = writer.configuration().getMessages();
        Content result = writer.getOutputInstance();
        if (!paramTags.isEmpty()) {
            CommentHelper ch = writer.configuration().utils.getCommentHelper(e);
            for (ParamTree dt : paramTags) {
                String name = ch.getParameterName(dt);
                String paramName = kind != ParamKind.TYPE_PARAMETER
                        ? name.toString()
                        : "<" + name + ">";
                if (!rankMap.containsKey(name)) {
                    String key;
                    switch (kind) {
                        case PARAMETER:       key = "doclet.Parameters_warn" ; break;
                        case TYPE_PARAMETER:  key = "doclet.TypeParameters_warn" ; break;
                        case RECORD_COMPONENT: key = "doclet.RecordComponents_warn" ; break;
                        default: throw new IllegalArgumentException(kind.toString());
                }
                    messages.warning(ch.getDocTreePath(dt), key, paramName);
                }
                String rank = rankMap.get(name);
                if (rank != null && alreadyDocumented.contains(rank)) {
                    String key;
                    switch (kind) {
                        case PARAMETER:       key = "doclet.Parameters_dup_warn" ; break;
                        case TYPE_PARAMETER:  key = "doclet.TypeParameters_dup_warn" ; break;
                        case RECORD_COMPONENT: key = "doclet.RecordComponents_dup_warn" ; break;
                        default: throw new IllegalArgumentException(kind.toString());
                }
                    messages.warning(ch.getDocTreePath(dt), key, paramName);
                }
                result.add(processParamTag(e, kind, writer, dt,
                        name, alreadyDocumented.isEmpty()));
                alreadyDocumented.add(rank);
            }
        }
        return result;
    }

    /**
     * Convert the individual ParamTag into Content.
     *
     * @param e               the owner element
     * @param kind            the kind of param tag
     * @param writer          the taglet writer for output writing.
     * @param paramTag        the tag whose inline tags will be printed.
     * @param name            the name of the parameter.  We can't rely on
     *                        the name in the param tag because we might be
     *                        inheriting documentation.
     * @param isFirstParam    true if this is the first param tag being printed.
     *
     */
    private Content processParamTag(Element e, ParamKind kind,
            TagletWriter writer, ParamTree paramTag, String name,
            boolean isFirstParam) {
        Content result = writer.getOutputInstance();
        if (isFirstParam) {
            result.add(writer.getParamHeader(kind));
        }
        result.add(writer.paramTagOutput(e, paramTag, name));
        return result;
    }
}
