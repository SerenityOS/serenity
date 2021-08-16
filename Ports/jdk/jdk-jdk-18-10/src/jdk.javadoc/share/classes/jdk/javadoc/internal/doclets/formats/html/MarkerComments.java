/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.javadoc.internal.doclets.formats.html.markup.Comment;

/**
 *  Marker comments to identify regions in the generated files.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class MarkerComments {

    /**
     * Marker to identify start of top navigation bar.
     */
    public static final Comment START_OF_TOP_NAVBAR =
            new Comment("========= START OF TOP NAVBAR =======");

    /**
     * Marker to identify end of top navigation bar.
     */
    public static final Comment END_OF_TOP_NAVBAR =
            new Comment("========= END OF TOP NAVBAR =========");

    /**
     * Marker to identify start of module description.
     */
    public static final Comment START_OF_MODULE_DESCRIPTION =
            new Comment("============ MODULE DESCRIPTION ===========");

    /**
     * Marker to identify start of modules summary.
     */
    public static final Comment START_OF_MODULES_SUMMARY =
            new Comment("============ MODULES SUMMARY ===========");

    /**
     * Marker to identify start of packages summary.
     */
    public static final Comment START_OF_PACKAGES_SUMMARY =
            new Comment("============ PACKAGES SUMMARY ===========");

    /**
     * Marker to identify start of services summary.
     */
    public static final Comment START_OF_SERVICES_SUMMARY =
            new Comment("============ SERVICES SUMMARY ===========");

    /**
     * Marker to identify start of class data.
     */
    public static final Comment START_OF_CLASS_DATA =
            new Comment("======== START OF CLASS DATA ========");

    /**
     * Marker to identify end of class data.
     */
    public static final Comment END_OF_CLASS_DATA =
            new Comment("========= END OF CLASS DATA =========");

    /**
     * Marker to identify start of nested class summary.
     */
    public static final Comment START_OF_NESTED_CLASS_SUMMARY =
            new Comment("======== NESTED CLASS SUMMARY ========");

    /**
     * Marker to identify start of annotation type optional member summary.
     */
    public static final Comment START_OF_ANNOTATION_TYPE_OPTIONAL_MEMBER_SUMMARY =
            new Comment("=========== ANNOTATION TYPE OPTIONAL MEMBER SUMMARY ===========");

    /**
     * Marker to identify start of annotation interface optional member summary.
     */
    public static final Comment START_OF_ANNOTATION_INTERFACE_OPTIONAL_MEMBER_SUMMARY =
            new Comment("=========== ANNOTATION INTERFACE OPTIONAL MEMBER SUMMARY ===========");

    /**
     * Marker to identify start of annotation type required member summary.
     */
    public static final Comment START_OF_ANNOTATION_TYPE_REQUIRED_MEMBER_SUMMARY =
            new Comment("=========== ANNOTATION TYPE REQUIRED MEMBER SUMMARY ===========");

    /**
     * Marker to identify start of annotation interface required member summary.
     */
    public static final Comment START_OF_ANNOTATION_INTERFACE_REQUIRED_MEMBER_SUMMARY =
            new Comment("=========== ANNOTATION INTERFACE REQUIRED MEMBER SUMMARY ===========");

    /**
     * Marker to identify start of constructor summary.
     */
    public static final Comment START_OF_CONSTRUCTOR_SUMMARY =
            new Comment("======== CONSTRUCTOR SUMMARY ========");

    /**
     * Marker to identify start of enum constants summary.
     */
    public static final Comment START_OF_ENUM_CONSTANT_SUMMARY =
            new Comment("=========== ENUM CONSTANT SUMMARY ===========");

    /**
     * Marker to identify start of field summary.
     */
    public static final Comment START_OF_FIELD_SUMMARY =
            new Comment("=========== FIELD SUMMARY ===========");

    /**
     * Marker to identify start of properties summary.
     */
    public static final Comment START_OF_PROPERTY_SUMMARY =
            new Comment("=========== PROPERTY SUMMARY ===========");

    /**
     * Marker to identify start of method summary.
     */
    public static final Comment START_OF_METHOD_SUMMARY =
            new Comment("========== METHOD SUMMARY ===========");

    /**
     * Marker to identify start of annotation type details.
     */
    public static final Comment START_OF_ANNOTATION_TYPE_DETAILS =
            new Comment("============ ANNOTATION TYPE MEMBER DETAIL ===========");

    /**
     * Marker to identify start of annotation interface details.
     */
    public static final Comment START_OF_ANNOTATION_INTERFACE_DETAILS =
            new Comment("============ ANNOTATION INTERFACE MEMBER DETAIL ===========");

    /**
     * Marker to identify start of method details.
     */
    public static final Comment START_OF_METHOD_DETAILS =
            new Comment("============ METHOD DETAIL ==========");

    /**
     * Marker to identify start of field details.
     */
    public static final Comment START_OF_FIELD_DETAILS =
            new Comment("============ FIELD DETAIL ===========");

    /**
     * Marker to identify start of property details.
     */
    public static final Comment START_OF_PROPERTY_DETAILS =
            new Comment("============ PROPERTY DETAIL ===========");

    /**
     * Marker to identify start of constructor details.
     */
    public static final Comment START_OF_CONSTRUCTOR_DETAILS =
            new Comment("========= CONSTRUCTOR DETAIL ========");

    /**
     * Marker to identify start of enum constants details.
     */
    public static final Comment START_OF_ENUM_CONSTANT_DETAILS =
            new Comment("============ ENUM CONSTANT DETAIL ===========");

}
