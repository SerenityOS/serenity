/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdeprscan;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

/**
 * A simple data class that contains language model data (TypeElement, etc.)
 * about a deprecated API.
 */
public class DeprData {
    final ElementKind kind;
    final TypeElement type; // null if data loaded from file
    final String typeName;
    final String nameSig;
    final String since;
    final boolean forRemoval;

    public DeprData(ElementKind kind, TypeElement type, String typeName, String nameSig,
                    String since, boolean forRemoval) {
        this.kind = kind;
        this.type = type;
        this.typeName = typeName;
        this.nameSig = nameSig;
        this.since = since;
        this.forRemoval = forRemoval;
    }

    public boolean isForRemoval() {
        return forRemoval;
    }

    @Override
    public String toString() {
        return String.format("DeprData(%s,%s,%s,%s,%s,%s)",
                             kind, type, typeName, nameSig, since, forRemoval);
    }
}
