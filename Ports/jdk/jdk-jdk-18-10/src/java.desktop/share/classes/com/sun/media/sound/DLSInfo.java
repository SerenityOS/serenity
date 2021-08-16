/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.sound;

/**
 * This class is used to store information  to describe soundbanks, instruments
 * and samples. It is stored inside a "INFO" List Chunk inside DLS files.
 *
 * @author Karl Helgason
 */
public final class DLSInfo {

    /**
     * (INAM) Title or subject.
     */
    public String name = "untitled";
    /**
     * (ICRD) Date of creation, the format is: YYYY-MM-DD.
     *        For example 2007-01-01 for 1. january of year 2007.
     */
    public String creationDate = null;
    /**
     * (IENG) Name of engineer who created the object.
     */
    public String engineers = null;
    /**
     * (IPRD) Name of the product which the object is intended for.
     */
    public String product = null;
    /**
     * (ICOP) Copyright information.
     */
    public String copyright = null;
    /**
     * (ICMT) General comments. Doesn't contain newline characters.
     */
    public String comments = null;
    /**
     * (ISFT) Name of software package used to create the file.
     */
    public String tools = null;
    /**
     * (IARL) Where content is archived.
     */
    public String archival_location = null;
    /**
     * (IART) Artists of original content.
     */
    public String artist = null;
    /**
     * (ICMS) Names of persons or orginizations who commissioned the file.
     */
    public String commissioned = null;
    /**
     * (IGNR) Genre of the work.
     *        Example: jazz, classical, rock, etc.
     */
    public String genre = null;
    /**
     * (IKEY) List of keyword that describe the content.
     *        Examples: FX, bird, piano, etc.
     */
    public String keywords = null;
    /**
     * (IMED) Describes original medium of the data.
     *        For example: record, CD, etc.
     */
    public String medium = null;
    /**
     * (ISBJ) Description of the content.
     */
    public String subject = null;
    /**
     * (ISRC) Name of person or orginization who supplied
     *        original material for the file.
     */
    public String source = null;
    /**
     * (ISRF) Source media for sample data is from.
     *        For example: CD, TV, etc.
     */
    public String source_form = null;
    /**
     * (ITCH) Technician who sample the file/object.
     */
    public String technician = null;
}
