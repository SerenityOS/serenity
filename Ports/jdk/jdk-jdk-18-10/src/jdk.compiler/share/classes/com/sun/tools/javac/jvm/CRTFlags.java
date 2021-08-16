/*
 * Copyright (c) 2001, 2005, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.jvm;


/** The CharacterRangeTable flags indicating type of an entry.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public interface CRTFlags {

    /** CRTEntry flags.
     */
    public static final int CRT_STATEMENT       = 0x0001;
    public static final int CRT_BLOCK           = 0x0002;
    public static final int CRT_ASSIGNMENT      = 0x0004;
    public static final int CRT_FLOW_CONTROLLER = 0x0008;
    public static final int CRT_FLOW_TARGET     = 0x0010;
    public static final int CRT_INVOKE          = 0x0020;
    public static final int CRT_CREATE          = 0x0040;
    public static final int CRT_BRANCH_TRUE     = 0x0080;
    public static final int CRT_BRANCH_FALSE    = 0x0100;

    /** The mask for valid flags
     */
    public static final int CRT_VALID_FLAGS = CRT_STATEMENT | CRT_BLOCK | CRT_ASSIGNMENT |
                                              CRT_FLOW_CONTROLLER | CRT_FLOW_TARGET | CRT_INVOKE |
                                              CRT_CREATE | CRT_BRANCH_TRUE | CRT_BRANCH_FALSE;
}
