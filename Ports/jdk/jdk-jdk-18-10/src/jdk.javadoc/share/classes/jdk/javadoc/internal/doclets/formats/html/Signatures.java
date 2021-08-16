/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.Entity;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.formats.html.markup.TagName;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.DocletConstants;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementKindVisitor14;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.NATIVE;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.Modifier.STRICTFP;
import static javax.lang.model.element.Modifier.SYNCHRONIZED;

public class Signatures {

    public static Content getModuleSignature(ModuleElement mdle, ModuleWriterImpl moduleWriter) {
        Content signature = HtmlTree.DIV(HtmlStyle.moduleSignature);
        Content annotations = moduleWriter.getAnnotationInfo(mdle, true);
        if (!annotations.isEmpty()) {
            signature.add(HtmlTree.SPAN(HtmlStyle.annotations, annotations));
        }
        DocletEnvironment docEnv = moduleWriter.configuration.docEnv;
        String label = mdle.isOpen() && (docEnv.getModuleMode() == DocletEnvironment.ModuleMode.ALL)
                ? "open module" : "module";
        signature.add(label);
        signature.add(" ");
        HtmlTree nameSpan = new HtmlTree(TagName.SPAN).setStyle(HtmlStyle.elementName);
        nameSpan.add(mdle.getQualifiedName().toString());
        signature.add(nameSpan);
        return signature;
    }

    public static Content getPackageSignature(PackageElement pkg, PackageWriterImpl pkgWriter) {
        if (pkg.isUnnamed()) {
            return Text.EMPTY;
        }
        Content signature = HtmlTree.DIV(HtmlStyle.packageSignature);
        Content annotations = pkgWriter.getAnnotationInfo(pkg, true);
        if (!annotations.isEmpty()) {
            signature.add(HtmlTree.SPAN(HtmlStyle.annotations, annotations));
        }
        signature.add("package ");
        HtmlTree nameSpan = new HtmlTree(TagName.SPAN).setStyle(HtmlStyle.elementName);
        nameSpan.add(pkg.getQualifiedName().toString());
        signature.add(nameSpan);
        return signature;
    }

    static class TypeSignature {

        private final TypeElement typeElement;
        private final HtmlDocletWriter writer;
        private final Utils utils;
        private final HtmlConfiguration configuration;
        private Content modifiers;

        private static final Set<String> previewModifiers = Collections.emptySet();

         TypeSignature(TypeElement typeElement, HtmlDocletWriter writer) {
             this.typeElement = typeElement;
             this.writer = writer;
             this.utils = writer.utils;
             this.configuration = writer.configuration;
             this.modifiers = markPreviewModifiers(getModifiers());
         }

        public TypeSignature setModifiers(Content modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public Content toContent() {
            Content content = new ContentBuilder();
            Content annotationInfo = writer.getAnnotationInfo(typeElement, true);
            if (!annotationInfo.isEmpty()) {
                content.add(HtmlTree.SPAN(HtmlStyle.annotations, annotationInfo));
            }
            content.add(HtmlTree.SPAN(HtmlStyle.modifiers, modifiers));

            HtmlTree nameSpan = new HtmlTree(TagName.SPAN).setStyle(HtmlStyle.elementName);
            Content className = Text.of(utils.getSimpleName(typeElement));
            if (configuration.getOptions().linkSource()) {
                writer.addSrcLink(typeElement, className, nameSpan);
            } else {
                nameSpan.addStyle(HtmlStyle.typeNameLabel).add(className);
            }
            HtmlLinkInfo linkInfo = new HtmlLinkInfo(configuration,
                    HtmlLinkInfo.Kind.CLASS_SIGNATURE, typeElement);
            //Let's not link to ourselves in the signature.
            linkInfo.linkToSelf = false;
            nameSpan.add(writer.getTypeParameterLinks(linkInfo));
            content.add(nameSpan);

            if (utils.isRecord(typeElement)) {
                content.add(getRecordComponents());
            }
            if (!utils.isAnnotationType(typeElement)) {
                Content extendsImplements = new HtmlTree(TagName.SPAN)
                        .setStyle(HtmlStyle.extendsImplements);
                if (!utils.isInterface(typeElement)) {
                    TypeMirror superclass = utils.getFirstVisibleSuperClass(typeElement);
                    if (superclass != null) {
                        content.add(DocletConstants.NL);
                        extendsImplements.add("extends ");
                        Content link = writer.getLink(new HtmlLinkInfo(configuration,
                                HtmlLinkInfo.Kind.CLASS_SIGNATURE_PARENT_NAME,
                                superclass));
                        extendsImplements.add(link);
                    }
                }
                List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
                if (!interfaces.isEmpty()) {
                    boolean isFirst = true;
                    for (TypeMirror type : interfaces) {
                        TypeElement tDoc = utils.asTypeElement(type);
                        if (!(utils.isPublic(tDoc) || utils.isLinkable(tDoc))) {
                            continue;
                        }
                        if (isFirst) {
                            extendsImplements.add(DocletConstants.NL);
                            extendsImplements.add(utils.isInterface(typeElement) ? "extends " : "implements ");
                            isFirst = false;
                        } else {
                            extendsImplements.add(", ");
                        }
                        Content link = writer.getLink(new HtmlLinkInfo(configuration,
                                HtmlLinkInfo.Kind.CLASS_SIGNATURE_PARENT_NAME,
                                type));
                        extendsImplements.add(link);
                    }
                }
                if (!extendsImplements.isEmpty()) {
                    content.add(extendsImplements);
                }
            }
            List<? extends TypeMirror> permits = typeElement.getPermittedSubclasses();
            List<? extends TypeMirror> linkablePermits = permits.stream()
                    .filter(t -> utils.isLinkable(utils.asTypeElement(t)))
                    .toList();
            if (!linkablePermits.isEmpty()) {
                Content permitsSpan = new HtmlTree(TagName.SPAN).setStyle(HtmlStyle.permits);
                boolean isFirst = true;
                for (TypeMirror type : linkablePermits) {
                    if (isFirst) {
                        content.add(DocletConstants.NL);
                        permitsSpan.add("permits");
                        permitsSpan.add(" ");
                        isFirst = false;
                    } else {
                        permitsSpan.add(", ");
                    }
                    Content link = writer.getLink(new HtmlLinkInfo(configuration,
                            HtmlLinkInfo.Kind.PERMITTED_SUBCLASSES,
                            type));
                    permitsSpan.add(link);
                }
                if (linkablePermits.size() < permits.size()) {
                    Content c = Text.of(configuration.getDocResources().getText("doclet.not.exhaustive"));
                    permitsSpan.add(" ");
                    permitsSpan.add(HtmlTree.SPAN(HtmlStyle.permitsNote, c));
                }
                content.add(permitsSpan);
            }
            return HtmlTree.DIV(HtmlStyle.typeSignature, content);
        }

        private Content getRecordComponents() {
            Content content = new ContentBuilder();
            content.add("(");
            String sep = "";
            for (RecordComponentElement e : typeElement.getRecordComponents()) {
                content.add(sep);
                writer.getAnnotations(e.getAnnotationMirrors(), false)
                        .forEach(a -> { content.add(a).add(" "); });
                Content link = writer.getLink(new HtmlLinkInfo(configuration, HtmlLinkInfo.Kind.RECORD_COMPONENT,
                        e.asType()));
                content.add(link);
                content.add(Entity.NO_BREAK_SPACE);
                content.add(e.getSimpleName());
                sep = ", ";
            }
            content.add(")");
            return content;
        }

        private Content markPreviewModifiers(List<String> modifiers) {
             Content content = new ContentBuilder();
             String sep = null;
             for (String modifier : modifiers) {
                 if (sep != null) {
                    content.add(sep);
                 }
                 content.add(modifier);
                 if (previewModifiers.contains(modifier)) {
                     content.add(HtmlTree.SUP(writer.links.createLink(
                             configuration.htmlIds.forPreviewSection(typeElement),
                             configuration.contents.previewMark)));
                 }
                 sep = " ";
             }
             content.add(" ");
             return content;
        }

        private List<String> getModifiers() {
            SortedSet<Modifier> modifiers = new TreeSet<>(typeElement.getModifiers());
            modifiers.remove(NATIVE);
            modifiers.remove(STRICTFP);
            modifiers.remove(SYNCHRONIZED);

            return new ElementKindVisitor14<List<String>, SortedSet<Modifier>>() {
                final List<String> list = new ArrayList<>();

                void addVisibilityModifier(Set<Modifier> modifiers) {
                    if (modifiers.contains(PUBLIC)) {
                        list.add("public");
                    } else if (modifiers.contains(PROTECTED)) {
                        list.add("protected");
                    } else if (modifiers.contains(PRIVATE)) {
                        list.add("private");
                    }
                }

                void addStatic(Set<Modifier> modifiers) {
                    if (modifiers.contains(STATIC)) {
                        list.add("static");
                    }
                }

                void addSealed(TypeElement e) {
                    if (e.getModifiers().contains(Modifier.SEALED)) {
                        list.add("sealed");
                    } else if (e.getModifiers().contains(Modifier.NON_SEALED)) {
                        list.add("non-sealed");
                    }
                }

                void addModifiers(Set<Modifier> modifiers) {
                    modifiers.stream()
                            .map(Modifier::toString)
                            .forEachOrdered(list::add);
                }

                @Override
                public List<String> visitTypeAsInterface(TypeElement e, SortedSet<Modifier> mods) {
                    addVisibilityModifier(mods);
                    addStatic(mods);
                    addSealed(e);
                    list.add("interface");
                    return list;
                }

                @Override
                public List<String> visitTypeAsEnum(TypeElement e, SortedSet<Modifier> mods) {
                    addVisibilityModifier(mods);
                    addStatic(mods);
                    list.add("enum");
                    return list;
                }

                @Override
                public List<String> visitTypeAsAnnotationType(TypeElement e, SortedSet<Modifier> mods) {
                    addVisibilityModifier(mods);
                    addStatic(mods);
                    list.add("@interface");
                    return list;
                }

                @Override
                public List<String> visitTypeAsRecord(TypeElement e, SortedSet<Modifier> mods) {
                    mods.remove(FINAL); // suppress the implicit `final`
                    return visitTypeAsClass(e, mods);
                }

                @Override
                public List<String> visitTypeAsClass(TypeElement e, SortedSet<Modifier> mods) {
                    addModifiers(mods);
                    String keyword = e.getKind() == ElementKind.RECORD ? "record" : "class";
                    list.add(keyword);
                    return list;
                }

                @Override
                protected List<String> defaultAction(Element e, SortedSet<Modifier> mods) {
                    addModifiers(mods);
                    return list;
                }

            }.visit(typeElement, modifiers);
        }
    }

    /**
     * A content builder for member signatures.
     */
    static class MemberSignature {

        private final AbstractMemberWriter memberWriter;
        private final Utils utils;

        private final Element element;
        private Content annotations;
        private Content typeParameters;
        private Content returnType;
        private Content parameters;
        private Content exceptions;

        // Threshold for length of type parameters before switching from inline to block representation.
        private static final int TYPE_PARAMS_MAX_INLINE_LENGTH = 50;

        // Threshold for combined length of modifiers, type params and return type before breaking
        // it up with a line break before the return type.
        private static final int RETURN_TYPE_MAX_LINE_LENGTH = 50;

        /**
         * Creates a new member signature builder.
         *
         * @param element the element for which to create a signature
         * @param memberWriter the member writer
         */
        MemberSignature(Element element, AbstractMemberWriter memberWriter) {
            this.element = element;
            this.memberWriter = memberWriter;
            this.utils = memberWriter.utils;
        }

        /**
         * Set the type parameters for an executable member.
         *
         * @param typeParameters the content tree containing the type parameters to add.
         * @return this instance
         */
        MemberSignature setTypeParameters(Content typeParameters) {
            this.typeParameters = typeParameters;
            return this;
        }

        /**
         * Set the return type for an executable member.
         *
         * @param returnType the content tree containing the return type to add.
         * @return this instance
         */
        MemberSignature setReturnType(Content returnType) {
            this.returnType = returnType;
            return this;
        }

        /**
         * Set the type information for a non-executable member.
         *
         * @param type the type of the member.
         * @return this instance
         */
        MemberSignature setType(TypeMirror type) {
            this.returnType = memberWriter.writer.getLink(new HtmlLinkInfo(memberWriter.configuration, HtmlLinkInfo.Kind.MEMBER, type));
            return this;
        }

        /**
         * Set the parameter information of an executable member.
         *
         * @param paramTree the content tree containing the parameter information.
         * @return this instance
         */
        MemberSignature setParameters(Content paramTree) {
            this.parameters = paramTree;
            return this;
        }

        /**
         * Set the exception information of an executable member.
         *
         * @param exceptionTree the content tree containing the exception information
         * @return this instance
         */
        MemberSignature setExceptions(Content exceptionTree) {
            this.exceptions = exceptionTree;
            return this;
        }

        /**
         * Set the annotation information of a member.
         *
         * @param annotationTree the content tree containing the exception information
         * @return this instance
         */
        MemberSignature setAnnotations(Content annotationTree) {
            this.annotations = annotationTree;
            return this;
        }

        /**
         * Returns an HTML tree containing the member signature.
         *
         * @return an HTML tree containing the member signature
         */
        Content toContent() {
            Content content = new ContentBuilder();
            // Position of last line separator.
            int lastLineSeparator = 0;

            // Annotations
            if (annotations != null && !annotations.isEmpty()) {
                content.add(HtmlTree.SPAN(HtmlStyle.annotations, annotations));
                lastLineSeparator = content.charCount();
            }

            // Modifiers
            appendModifiers(content);

            // Type parameters
            if (typeParameters != null && !typeParameters.isEmpty()) {
                lastLineSeparator = appendTypeParameters(content, lastLineSeparator);
            }

            // Return type
            if (returnType != null) {
                content.add(HtmlTree.SPAN(HtmlStyle.returnType, returnType));
                content.add(Entity.NO_BREAK_SPACE);
            }

            // Name
            HtmlTree nameSpan = new HtmlTree(TagName.SPAN).setStyle(HtmlStyle.elementName);
            if (memberWriter.options.linkSource()) {
                Content name = Text.of(memberWriter.name(element));
                memberWriter.writer.addSrcLink(element, name, nameSpan);
            } else {
                nameSpan.add(memberWriter.name(element));
            }
            content.add(nameSpan);

            // Parameters and exceptions
            if (parameters != null) {
                appendParametersAndExceptions(content, lastLineSeparator);
            }

            return HtmlTree.DIV(HtmlStyle.memberSignature, content);
        }

        /**
         * Adds the modifier for the member. The modifiers are ordered as specified
         * by <em>The Java Language Specification</em>.
         *
         * @param htmlTree the content tree to which the modifier information will be added
         */
        private void appendModifiers(Content htmlTree) {
            Set<Modifier> set = new TreeSet<>(element.getModifiers());

            // remove the ones we really don't need
            set.remove(NATIVE);
            set.remove(SYNCHRONIZED);
            set.remove(STRICTFP);

            // According to JLS, we should not be showing public modifier for
            // interface methods and fields.
            if ((utils.isField(element) || utils.isMethod(element))) {
                Element te = element.getEnclosingElement();
                if (utils.isInterface(te) || utils.isAnnotationType(te)) {
                    // Remove the implicit abstract and public modifiers
                    if (utils.isMethod(element)) {
                        set.remove(ABSTRACT);
                    }
                    set.remove(PUBLIC);
                }
            }
            if (!set.isEmpty()) {
                String mods = set.stream().map(Modifier::toString).collect(Collectors.joining(" "));
                htmlTree.add(HtmlTree.SPAN(HtmlStyle.modifiers, Text.of(mods)))
                        .add(Entity.NO_BREAK_SPACE);
            }
        }

        /**
         * Appends the type parameter information to the HTML tree.
         *
         * @param htmlTree          the HTML tree
         * @param lastLineSeparator index of last line separator in the HTML tree
         * @return the new index of the last line separator
         */
        private int appendTypeParameters(Content htmlTree, int lastLineSeparator) {
            // Apply different wrapping strategies for type parameters
            // depending of combined length of type parameters and return type.
            int typeParamLength = typeParameters.charCount();

            if (typeParamLength >= TYPE_PARAMS_MAX_INLINE_LENGTH) {
                htmlTree.add(HtmlTree.SPAN(HtmlStyle.typeParametersLong, typeParameters));
            } else {
                htmlTree.add(HtmlTree.SPAN(HtmlStyle.typeParameters, typeParameters));
            }

            int lineLength = htmlTree.charCount() - lastLineSeparator;
            int newLastLineSeparator = lastLineSeparator;

            // sum below includes length of modifiers plus type params added above
            if (lineLength + returnType.charCount() > RETURN_TYPE_MAX_LINE_LENGTH) {
                htmlTree.add(DocletConstants.NL);
                newLastLineSeparator = htmlTree.charCount();
            } else {
                htmlTree.add(Entity.NO_BREAK_SPACE);
            }

            return newLastLineSeparator;
        }

        /**
         * Appends the parameters and exceptions information to the HTML tree.
         *
         * @param htmlTree          the HTML tree
         * @param lastLineSeparator the index of the last line separator in the HTML tree
         */
        private void appendParametersAndExceptions(Content htmlTree, int lastLineSeparator) {
            // Record current position for indentation of exceptions
            int indentSize = htmlTree.charCount() - lastLineSeparator;

            if (parameters.charCount() == 2) {
                // empty parameters are added without packing
                htmlTree.add(parameters);
            } else {
                htmlTree.add(new HtmlTree(TagName.WBR))
                        .add(HtmlTree.SPAN(HtmlStyle.parameters, parameters));
            }

            // Exceptions
            if (exceptions != null && !exceptions.isEmpty()) {
                CharSequence indent = " ".repeat(Math.max(0, indentSize + 1 - 7));
                htmlTree.add(DocletConstants.NL)
                        .add(indent)
                        .add("throws ")
                        .add(HtmlTree.SPAN(HtmlStyle.exceptions, exceptions));
            }
        }
    }
}
