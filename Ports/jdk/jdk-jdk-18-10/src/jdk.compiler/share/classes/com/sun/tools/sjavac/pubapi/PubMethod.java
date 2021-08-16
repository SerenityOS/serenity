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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

public class PubMethod implements Serializable {

    private static final long serialVersionUID = -7813050194553446243L;

    Set<Modifier> modifiers;
    List<PubApiTypeParam> typeParams;
    TypeDesc returnType;
    String identifier;
    List<TypeDesc> paramTypes;
    List<TypeDesc> throwDecls;

    public PubMethod(Set<Modifier> modifiers,
                     List<PubApiTypeParam> typeParams,
                     TypeDesc returnType,
                     String identifier,
                     List<TypeDesc> paramTypes,
                     List<TypeDesc> throwDecls) {
        this.modifiers = modifiers;
        this.typeParams = typeParams;
        this.returnType = returnType;
        this.identifier = identifier;
        this.paramTypes = paramTypes;
        this.throwDecls = throwDecls;
    }

    // We need to include return type and type parameters to be sure to have
    // different values for different methods. (A method can be overloaded with
    // the only difference being the upper bound of the return type.)
    public String asSignatureString() {
        StringBuilder sb = new StringBuilder();

        // <A extends String, Serializable, B extends List>
        if (typeParams.size() > 0) {
            sb.append(typeParams.stream()
                                .map(PubApiTypeParam::asString)
                                .collect(Collectors.joining(",", "<", "> ")));
        }
        sb.append(TypeDesc.encodeAsString(returnType));
        sb.append(" ");
        sb.append(identifier);
        sb.append("(");
        sb.append(paramTypes.stream()
                            .map(TypeDesc::encodeAsString)
                            .collect(Collectors.joining(",")));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass())
            return false;
        PubMethod other = (PubMethod) obj;
        return modifiers.equals(other.modifiers)
            && typeParams.equals(other.typeParams)
            && returnType.equals(other.returnType)
            && identifier.equals(other.identifier)
            && paramTypes.equals(other.paramTypes)
            && throwDecls.equals(other.throwDecls);
    }

    @Override
    public int hashCode() {
        return modifiers.hashCode()
             ^ typeParams.hashCode()
             ^ returnType.hashCode()
             ^ identifier.hashCode()
             ^ paramTypes.hashCode()
             ^ throwDecls.hashCode();
    }

    public String toString() {
        return String.format("%s[modifiers: %s, typeParams: %s, retType: %s, identifier: %s, params: %s, throws: %s]",
                             getClass().getSimpleName(),
                             modifiers,
                             typeParams,
                             returnType,
                             identifier,
                             paramTypes,
                             throwDecls);
    }
}
