/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.javadoc.internal.doclets.formats.html.markup.TagName;

/**
 * Aliases for HTML heading tags (H1..H6) for different kinds of pages.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
class Headings {
    /**
     * Standard top-level heading for the page title for all pages.
     */
    static final TagName PAGE_TITLE_HEADING = TagName.H1;

    /**
     * Standard second-level heading for sundry pages that do
     * not have their own page group.
     */
    static final TagName CONTENT_HEADING = TagName.H2;

    /**
     * Standard third-level heading for sundry pages that do
     * not have their own page group.
     */
    static final TagName SUB_HEADING = TagName.H3;

    /**
     * Headings for the page for a module declaration.
     */
    static class ModuleDeclaration {
        static final TagName SUMMARY_HEADING = TagName.H2;
    }

    /**
     * Headings for the page for a type declaration.
     * This includes classes, interfaces, enums and annotation types.
     */
    static class TypeDeclaration {
        /**
         * Heading for the different summary lists:
         * Field Summary, Constructor Summary, Method Summary, etc.
         */
        static final TagName SUMMARY_HEADING = TagName.H2;

        /**
         * Subheading within a summary for the inherited elements:
         * inherited methods, etc
         */
        static final TagName INHERITED_SUMMARY_HEADING = TagName.H3;

        /**
         * Heading for the different detail lists:
         * Field Details, Constructor Details, Method Details, etc.
         */
        static final TagName DETAILS_HEADING = TagName.H2;

        /**
         * Subheading with a Details list for an individual element.
         */
        static final TagName MEMBER_HEADING = TagName.H3;
    }

    /**
     * Headings for the Constants Summary page.
     */
    static class ConstantsSummary {
        static final TagName PACKAGE_HEADING = TagName.H2;
    }

    /**
     * Headings for the Serialized Form page.
     */
    static class SerializedForm {
        /**
         * Heading for the package name, preceding a list of types.
         */
        static final TagName PACKAGE_HEADING = TagName.H2;

        /**
         * Heading for a type name within a package.
         */
        static final TagName CLASS_HEADING = TagName.H3;

        /**
         * Subheading for info within a type.
         */
        static final TagName CLASS_SUBHEADING = TagName.H4;

        /**
         * Heading for an individual member element within a type.
         */
        static final TagName MEMBER_HEADING = TagName.H5;
    }

    /**
     * Headings for a type Use page.
     */
    static class TypeUse {
        static final TagName SUMMARY_HEADING = TagName.H2;
    }

    /**
     * Headings for index frames pages.
     */
    static class IndexFrames {
        /** Heading for a list of module names in an index frame. */
        static final TagName MODULE_HEADING = TagName.H2;
        /** Heading for a list of package names in an index frame. */
        static final TagName PACKAGE_HEADING = TagName.H2;
    }
}
