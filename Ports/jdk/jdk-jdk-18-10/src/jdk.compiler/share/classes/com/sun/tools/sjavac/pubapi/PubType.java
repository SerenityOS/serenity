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
import java.util.Set;

import javax.lang.model.element.Modifier;

public class PubType implements Serializable {

    private static final long serialVersionUID = -7423416049253889793L;

    public final Set<Modifier> modifiers;
    public final String fqName;
    public final PubApi pubApi;

    public PubType(Set<Modifier> modifiers,
                   String fqName,
                   PubApi pubApi) {
        this.modifiers = modifiers;
        this.fqName = fqName;
        this.pubApi = pubApi;
    }

    public String getFqName() {
        return fqName.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass())
            return false;
        PubType other = (PubType) obj;
        return modifiers.equals(other.modifiers)
            && fqName.equals(other.fqName)
            && pubApi.equals(other.pubApi);
    }

    @Override
    public int hashCode() {
        return modifiers.hashCode() ^ fqName.hashCode() ^ pubApi.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s[modifiers: %s, fqName: %s, pubApi: %s]",
                             getClass().getSimpleName(),
                             modifiers,
                             fqName,
                             pubApi);
    }
}
