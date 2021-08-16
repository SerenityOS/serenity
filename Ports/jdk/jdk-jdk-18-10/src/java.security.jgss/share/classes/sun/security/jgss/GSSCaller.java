/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss;

/**
 * Denotes what client is calling the JGSS-API. The object can be sent deep
 * into the mechanism level so that special actions can be performed for
 * different callers.
 */
public class GSSCaller {
    public static final GSSCaller CALLER_UNKNOWN = new GSSCaller("UNKNOWN");
    public static final GSSCaller CALLER_INITIATE = new GSSCaller("INITIATE");
    public static final GSSCaller CALLER_ACCEPT = new GSSCaller("ACCEPT");

    private String name;
    GSSCaller(String s) {
        name = s;
    }
    @Override
    public String toString() {
        return "GSSCaller{" + name + '}';
    }
}

