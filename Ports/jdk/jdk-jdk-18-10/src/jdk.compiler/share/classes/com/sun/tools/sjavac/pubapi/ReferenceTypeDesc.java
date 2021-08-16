/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.sjavac.pubapi;

import java.io.Serializable;

import javax.lang.model.type.TypeKind;

public class ReferenceTypeDesc extends TypeDesc implements Serializable {

    private static final long serialVersionUID = 3357616754544796372L;

    // Example: "java.util.Vector<java.lang.String>"
    String javaType;

    public ReferenceTypeDesc(String javaType) {
        super(TypeKind.DECLARED);
        this.javaType = javaType;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj))
            return false;
        return javaType.equals(((ReferenceTypeDesc) obj).javaType);
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ javaType.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s[type: %s]", getClass().getSimpleName(), javaType);
    }
}
