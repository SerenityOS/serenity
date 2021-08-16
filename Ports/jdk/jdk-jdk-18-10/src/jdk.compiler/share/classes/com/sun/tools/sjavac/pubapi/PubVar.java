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
import java.util.Optional;
import java.util.Set;

import javax.lang.model.element.Modifier;

public class PubVar implements Serializable {

    private static final long serialVersionUID = 5806536061153374575L;

    public final Set<Modifier> modifiers;
    public final TypeDesc type;
    public final String identifier;
    private final String constValue;

    public PubVar(Set<Modifier> modifiers,
                  TypeDesc type,
                  String identifier,
                  String constValue) {
        this.modifiers = modifiers;
        this.type = type;
        this.identifier = identifier;
        this.constValue = constValue;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass())
            return false;
        PubVar other = (PubVar) obj;
        return modifiers.equals(other.modifiers)
            && type.equals(other.type)
            && identifier.equals(other.identifier)
            && getConstValue().equals(other.getConstValue());
    }

    @Override
    public int hashCode() {
        return modifiers.hashCode()
             ^ type.hashCode()
             ^ identifier.hashCode()
             ^ getConstValue().hashCode();
    }

    public String toString() {
        return String.format("%s[modifiers: %s, type: %s, identifier: %s, constValue: %s]",
                             getClass().getSimpleName(),
                             modifiers,
                             type,
                             identifier,
                             constValue);
    }

    public Optional<String> getConstValue() {
        return Optional.ofNullable(constValue);
    }
}
