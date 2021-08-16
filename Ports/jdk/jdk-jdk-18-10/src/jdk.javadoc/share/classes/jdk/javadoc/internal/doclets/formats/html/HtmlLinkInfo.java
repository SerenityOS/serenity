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

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;
import jdk.javadoc.internal.doclets.toolkit.util.links.LinkInfo;


/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class HtmlLinkInfo extends LinkInfo {

    public enum Kind {
        DEFAULT,

        /**
         * Indicate that the link appears in a class documentation.
         */
        CLASS,

        /**
         * Indicate that the link appears in member documentation.
         */
        MEMBER,

        /**
         * Indicate that the link appears in member documentation on the Deprecated or Preview page.
         */
        MEMBER_DEPRECATED_PREVIEW,

        /**
         * Indicate that the link appears in class use documentation.
         */
        CLASS_USE,

        /**
         * Indicate that the link appears in index documentation.
         */
        INDEX,

        /**
         * Indicate that the link appears in constant value summary.
         */
        CONSTANT_SUMMARY,

        /**
         * Indicate that the link appears in serialized form documentation.
         */
        SERIALIZED_FORM,

        /**
         * Indicate that the link appears in serial member documentation.
         */
        SERIAL_MEMBER,

        /**
         * Indicate that the link appears in package documentation.
         */
        PACKAGE,

        /**
         * Indicate that the link appears in see tag documentation.
         */
        SEE_TAG,

        /**
         * Indicate that the link appears in value tag documentation.
         */
        VALUE_TAG,

        /**
         * Indicate that the link appears in tree documentation.
         */
        TREE,

        /**
         * The header in the class documentation.
         */
        CLASS_HEADER,

        /**
         * The signature in the class documentation.
         */
        CLASS_SIGNATURE,

        /**
         * The return type of a method.
         */
        RETURN_TYPE,

        /**
         * The return type of a method in a member summary.
         */
        SUMMARY_RETURN_TYPE,

        /**
         * The type of a method/constructor parameter.
         */
        EXECUTABLE_MEMBER_PARAM,

        /**
         * Super interface links.
         */
        SUPER_INTERFACES,

        /**
         * Implemented interface links.
         */
        IMPLEMENTED_INTERFACES,

        /**
         * Implemented class links.
         */
        IMPLEMENTED_CLASSES,

        /**
         * Subinterface links.
         */
        SUBINTERFACES,

        /**
         * Subclasses links.
         */
        SUBCLASSES,

        /**
         * The signature in the class documentation (implements/extends portion).
         */
        CLASS_SIGNATURE_PARENT_NAME,

        /**
         * Permitted subclasses of a sealed type.
         */
        PERMITTED_SUBCLASSES,

        /**
         * The header for method documentation copied from parent.
         */
        EXECUTABLE_ELEMENT_COPY,

        /**
         * Method "specified by" link.
         */
        METHOD_SPECIFIED_BY,

        /**
         * Method "overrides" link.
         */
        METHOD_OVERRIDES,

        /**
         * Annotation link.
         */
        ANNOTATION,

        /**
         * The parent nodes in the class tree.
         */
        CLASS_TREE_PARENT,

        /**
         * The type parameters of a method or constructor.
         */
        MEMBER_TYPE_PARAMS,

        /**
         * Indicate that the link appears in class use documentation.
         */
        CLASS_USE_HEADER,

        /**
         * The header for property documentation copied from parent.
         */
        PROPERTY_COPY,

        /**
         * A receiver type.
         */
        RECEIVER_TYPE,

        /**
         * A record component within a class signature.
         */
        RECORD_COMPONENT,

        /**
         * A type thrown from a method.
         */
        THROWS_TYPE
    }

    public final HtmlConfiguration configuration;

    /**
     * The location of the link.
     */
    public Kind context = Kind.DEFAULT;

    /**
     * The value of the marker #.
     */
    public String where = "";

    /**
     * The member this link points to (if any).
     */
    public Element targetMember;

    /**
     * Optional style for the link.
     */
    public HtmlStyle style = null;

    public final Utils utils;

    /**
     * Construct a LinkInfo object.
     *
     * @param configuration the configuration data for the doclet
     * @param context    the context of the link.
     * @param ee   the member to link to.
     */
    public HtmlLinkInfo(HtmlConfiguration configuration, Kind context, ExecutableElement ee) {
        this.configuration = configuration;
        this.utils = configuration.utils;
        this.executableElement = ee;
        setContext(context);
    }

    @Override
    protected Content newContent() {
        return new ContentBuilder();
    }

    /**
     * Construct a LinkInfo object.
     *
     * @param configuration the configuration data for the doclet
     * @param context    the context of the link.
     * @param typeElement   the class to link to.
     */
    public HtmlLinkInfo(HtmlConfiguration configuration, Kind context, TypeElement typeElement) {
        this.configuration = configuration;
        this.utils = configuration.utils;
        this.typeElement = typeElement;
        setContext(context);
    }

    /**
     * Construct a LinkInfo object.
     *
     * @param configuration the configuration data for the doclet
     * @param context    the context of the link.
     * @param type       the class to link to.
     */
    public HtmlLinkInfo(HtmlConfiguration configuration, Kind context, TypeMirror type) {
        this.configuration = configuration;
        this.utils = configuration.utils;
        this.type = type;
        setContext(context);
    }

    /**
     * Set the label for the link.
     * @param label plain-text label for the link
     */
    public HtmlLinkInfo label(CharSequence label) {
        this.label = Text.of(label);
        return this;
    }

    /**
     * Set the label for the link.
     */
    public HtmlLinkInfo label(Content label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the style to be used for the link.
     */
    public HtmlLinkInfo style(HtmlStyle style) {
        this.style = style;
        return this;
    }

    /**
     * Set whether or not this is a link to a varargs parameter.
     */
    public HtmlLinkInfo varargs(boolean varargs) {
        this.isVarArg = varargs;
        return this;
    }

    /**
     * Set the fragment specifier for the link.
     */
    public HtmlLinkInfo where(String where) {
        this.where = where;
        return this;
    }

    /**
     * Set the member this link points to (if any).
     */
    public HtmlLinkInfo targetMember(Element el) {
        this.targetMember = el;
        return this;
    }

    /**
     * Set whether or not the preview flags should be skipped for this link.
     */
    public HtmlLinkInfo skipPreview(boolean skipPreview) {
        this.skipPreview = skipPreview;
        return this;
    }

    public Kind getContext() {
        return context;
    }

    /**
     * This method sets the link attributes to the appropriate values
     * based on the context.
     *
     * @param c the context id to set.
     */
    public final void setContext(Kind c) {
        switch (c) {
            case ANNOTATION:
            case IMPLEMENTED_INTERFACES:
            case SUPER_INTERFACES:
            case SUBINTERFACES:
            case CLASS_TREE_PARENT:
            case TREE:
            case CLASS_SIGNATURE_PARENT_NAME:
            case PERMITTED_SUBCLASSES:
                excludeTypeParameterLinks = true;
                excludeTypeBounds = true;
                break;

            case PACKAGE:
            case CLASS_USE:
            case CLASS_HEADER:
            case CLASS_SIGNATURE:
            case RECEIVER_TYPE:
                excludeTypeParameterLinks = true;
                break;

            case RETURN_TYPE:
            case SUMMARY_RETURN_TYPE:
            case EXECUTABLE_MEMBER_PARAM:
            case THROWS_TYPE:
                excludeTypeBounds = true;
                break;
        }
        context = c;
    }

    /**
     * Return true if this link is linkable and false if we can't link to the
     * desired place.
     *
     * @return true if this link is linkable and false if we can't link to the
     * desired place.
     */
    @Override
    public boolean isLinkable() {
        return configuration.utils.isLinkable(typeElement);
    }

    @Override
    public boolean includeTypeParameterLinks() {
        return switch (context) {
            case IMPLEMENTED_INTERFACES,
                 SUPER_INTERFACES,
                 SUBINTERFACES,
                 CLASS_TREE_PARENT,
                 TREE,
                 CLASS_SIGNATURE_PARENT_NAME,
                 PERMITTED_SUBCLASSES,
                 PACKAGE,
                 CLASS_USE,
                 CLASS_HEADER,
                 CLASS_SIGNATURE,
                 RECEIVER_TYPE,
                 MEMBER_TYPE_PARAMS -> true;

            case IMPLEMENTED_CLASSES,
                 SUBCLASSES,
                 EXECUTABLE_ELEMENT_COPY,
                 PROPERTY_COPY,
                 CLASS_USE_HEADER -> false;

            default -> label == null || label.isEmpty();
        };
    }

    @Override
    public String toString() {
        return "HtmlLinkInfo{" +
                "context=" + context +
                ", where=" + where +
                ", style=" + style +
                super.toString() + '}';
    }
}
