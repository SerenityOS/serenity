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

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.TagName;
import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.Resources;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.DocPaths;
import jdk.javadoc.internal.doclets.toolkit.util.DocletConstants;
import jdk.javadoc.internal.doclets.toolkit.util.Utils.ElementFlag;
import jdk.javadoc.internal.doclets.toolkit.util.links.LinkFactory;
import jdk.javadoc.internal.doclets.toolkit.util.links.LinkInfo;

/**
 * A factory that returns a link given the information about it.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class HtmlLinkFactory extends LinkFactory {

    private final HtmlDocletWriter m_writer;
    private final DocPaths docPaths;

    public HtmlLinkFactory(HtmlDocletWriter writer) {
        super(writer.configuration.utils);
        m_writer = writer;
        docPaths = writer.configuration.docPaths;
    }

    @Override
    protected Content newContent() {
        return new ContentBuilder();
    }

    @Override
    protected Content getClassLink(LinkInfo linkInfo) {
        BaseConfiguration configuration = m_writer.configuration;
        HtmlLinkInfo classLinkInfo = (HtmlLinkInfo) linkInfo;
        TypeElement typeElement = classLinkInfo.typeElement;
        // Create a tool tip if we are linking to a class or interface.  Don't
        // create one if we are linking to a member.
        String title = "";
        boolean hasWhere = classLinkInfo.where != null && classLinkInfo.where.length() != 0;
        if (!hasWhere) {
            boolean isTypeLink = classLinkInfo.type != null &&
                     utils.isTypeVariable(utils.getComponentType(classLinkInfo.type));
            title = getClassToolTip(typeElement, isTypeLink);
        }
        Content label = classLinkInfo.getClassLinkLabel(configuration);
        Set<ElementFlag> flags;
        Element target;
        boolean showPreview = !classLinkInfo.skipPreview;
        if (!hasWhere && showPreview) {
            flags = utils.elementFlags(typeElement);
            target = typeElement;
        } else if ((classLinkInfo.context == HtmlLinkInfo.Kind.SEE_TAG || classLinkInfo.context == HtmlLinkInfo.Kind.MEMBER_DEPRECATED_PREVIEW) &&
                   classLinkInfo.targetMember != null && showPreview) {
            flags = utils.elementFlags(classLinkInfo.targetMember);
            target = classLinkInfo.targetMember;
        } else {
            flags = EnumSet.noneOf(ElementFlag.class);
            target = null;
        }

        Content link = new ContentBuilder();
        if (utils.isIncluded(typeElement)) {
            if (configuration.isGeneratedDoc(typeElement) && !utils.hasHiddenTag(typeElement)) {
                DocPath filename = getPath(classLinkInfo);
                if (linkInfo.linkToSelf || typeElement != m_writer.getCurrentPageElement()) {
                        link.add(m_writer.links.createLink(
                                filename.fragment(classLinkInfo.where),
                                label,
                                classLinkInfo.style,
                                title));
                        if (flags.contains(ElementFlag.PREVIEW)) {
                            link.add(HtmlTree.SUP(m_writer.links.createLink(
                                    filename.fragment(m_writer.htmlIds.forPreviewSection(target).name()),
                                    m_writer.contents.previewMark)));
                        }
                        return link;
                }
            }
        } else {
            Content crossLink = m_writer.getCrossClassLink(
                typeElement, classLinkInfo.where,
                label, classLinkInfo.style, true);
            if (crossLink != null) {
                link.add(crossLink);
                if (flags.contains(ElementFlag.PREVIEW)) {
                    link.add(HtmlTree.SUP(m_writer.getCrossClassLink(
                        typeElement,
                        m_writer.htmlIds.forPreviewSection(target).name(),
                        m_writer.contents.previewMark,
                        null, false)));
                }
                return link;
            }
        }
        // Can't link so just write label.
        link.add(label);
        if (flags.contains(ElementFlag.PREVIEW)) {
            link.add(HtmlTree.SUP(m_writer.contents.previewMark));
        }
        return link;
    }

    @Override
    protected Content getTypeParameterLinks(LinkInfo linkInfo) {
        Content links = newContent();
        List<TypeMirror> vars = new ArrayList<>();
        TypeMirror ctype = linkInfo.type != null
                ? utils.getComponentType(linkInfo.type)
                : null;
        if (linkInfo.executableElement != null) {
            linkInfo.executableElement.getTypeParameters().forEach(t -> vars.add(t.asType()));
        } else if (linkInfo.type != null && utils.isDeclaredType(linkInfo.type)) {
            vars.addAll(((DeclaredType) linkInfo.type).getTypeArguments());
        } else if (ctype != null && utils.isDeclaredType(ctype)) {
            vars.addAll(((DeclaredType) ctype).getTypeArguments());
        } else if (ctype == null && linkInfo.typeElement != null) {
            linkInfo.typeElement.getTypeParameters().forEach(t -> vars.add(t.asType()));
        } else {
            // Nothing to document.
            return links;
        }
        if (!vars.isEmpty()) {
            links.add("<");
            boolean many = false;
            for (TypeMirror t : vars) {
                if (many) {
                    links.add(",");
                    links.add(new HtmlTree(TagName.WBR));
                    if (((HtmlLinkInfo) linkInfo).getContext() == HtmlLinkInfo.Kind.MEMBER_TYPE_PARAMS) {
                        links.add(DocletConstants.NL);
                    }
                }
                links.add(getTypeParameterLink(linkInfo, t));
                many = true;
            }
            links.add(">");
        }
        return links;
    }

    /**
     * Returns a link to the given type parameter.
     *
     * @param linkInfo     the information about the link to construct
     * @param typeParam the type parameter to link to
     * @return the link
     */
    protected Content getTypeParameterLink(LinkInfo linkInfo, TypeMirror typeParam) {
        HtmlLinkInfo typeLinkInfo = new HtmlLinkInfo(m_writer.configuration,
                ((HtmlLinkInfo) linkInfo).getContext(), typeParam).skipPreview(true);
        typeLinkInfo.excludeTypeBounds = linkInfo.excludeTypeBounds;
        typeLinkInfo.excludeTypeParameterLinks = linkInfo.excludeTypeParameterLinks;
        typeLinkInfo.linkToSelf = linkInfo.linkToSelf;
        return getLink(typeLinkInfo);
    }

    @Override
    public Content getTypeAnnotationLinks(LinkInfo linkInfo) {
        ContentBuilder links = new ContentBuilder();
        List<? extends AnnotationMirror> annotations;
        if (utils.isAnnotated(linkInfo.type)) {
            annotations = linkInfo.type.getAnnotationMirrors();
        } else if (utils.isTypeVariable(linkInfo.type)) {
            // TODO: use the context for now, and special case for Receiver_Types,
            // which takes the default case.
            switch (((HtmlLinkInfo)linkInfo).context) {
                case MEMBER_TYPE_PARAMS:
                case EXECUTABLE_MEMBER_PARAM:
                case CLASS_SIGNATURE:
                    Element element = utils.typeUtils.asElement(linkInfo.type);
                    annotations = element.getAnnotationMirrors();
                    break;
                default:
                    annotations = linkInfo.type.getAnnotationMirrors();
                    break;
            }

        } else {
            return links;
        }

        if (annotations.isEmpty())
            return links;

        m_writer.getAnnotations(annotations, false)
                .forEach(a -> {
                    links.add(a);
                    links.add(" ");
                });

        return links;
    }

    /**
     * Given a class, return the appropriate tool tip.
     *
     * @param typeElement the class to get the tool tip for.
     * @return the tool tip for the appropriate class.
     */
    private String getClassToolTip(TypeElement typeElement, boolean isTypeLink) {
        Resources resources = m_writer.configuration.getDocResources();
        if (isTypeLink) {
            return resources.getText("doclet.Href_Type_Param_Title",
                    utils.getSimpleName(typeElement));
        } else if (utils.isInterface(typeElement)){
            return resources.getText("doclet.Href_Interface_Title",
                m_writer.getLocalizedPackageName(utils.containingPackage(typeElement)));
        } else if (utils.isAnnotationType(typeElement)) {
            return resources.getText("doclet.Href_Annotation_Title",
                m_writer.getLocalizedPackageName(utils.containingPackage(typeElement)));
        } else if (utils.isEnum(typeElement)) {
            return resources.getText("doclet.Href_Enum_Title",
                m_writer.getLocalizedPackageName(utils.containingPackage(typeElement)));
        } else {
            return resources.getText("doclet.Href_Class_Title",
                m_writer.getLocalizedPackageName(utils.containingPackage(typeElement)));
        }
    }

    /**
     * Return path to the given file name in the given package. So if the name
     * passed is "Object.html" and the name of the package is "java.lang", and
     * if the relative path is "../.." then returned string will be
     * "../../java/lang/Object.html"
     *
     * @param linkInfo the information about the link.
     */
    private DocPath getPath(HtmlLinkInfo linkInfo) {
        return m_writer.pathToRoot.resolve(docPaths.forClass(linkInfo.typeElement));
    }
}
