/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdi;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.TypeComponent;
import com.sun.jdi.VirtualMachine;

abstract public class TypeComponentImpl extends MirrorImpl
    implements TypeComponent
{
    protected final long ref;
    protected final String name;
    protected final String signature;
    protected final String genericSignature;
    protected final ReferenceTypeImpl declaringType;
    private final int modifiers;

    TypeComponentImpl(VirtualMachine vm, ReferenceTypeImpl declaringType,
                      long ref, String name, String signature,
                      String genericSignature, int modifiers) {
        // The generic signature is set when this is created.
        super(vm);
        this.declaringType = declaringType;
        this.ref = ref;
        this.name = name;
        this.signature = signature;
        if (genericSignature != null && genericSignature.length() != 0) {
            this.genericSignature = genericSignature;
        } else {
            this.genericSignature = null;
        }
        this.modifiers = modifiers;
    }

    public String name() {
        return name;
    }

    public String signature() {
        return signature;
    }
    public String genericSignature() {
        return genericSignature;
    }

    public int modifiers() {
        return modifiers;
    }

    public ReferenceType declaringType() {
        return declaringType;
    }

    public boolean isStatic() {
        return isModifierSet(VMModifiers.STATIC);
    }

    public boolean isFinal() {
        return isModifierSet(VMModifiers.FINAL);
    }

    public boolean isPrivate() {
        return isModifierSet(VMModifiers.PRIVATE);
    }

    public boolean isPackagePrivate() {
        return !isModifierSet(VMModifiers.PRIVATE |
                              VMModifiers.PROTECTED |
                              VMModifiers.PUBLIC);
    }

    public boolean isProtected() {
        return isModifierSet(VMModifiers.PROTECTED);
    }

    public boolean isPublic() {
        return isModifierSet(VMModifiers.PUBLIC);
    }

    public boolean isSynthetic() {
        return isModifierSet(VMModifiers.SYNTHETIC);
    }

    long ref() {
        return ref;
    }

    boolean isModifierSet(int compareBits) {
        return (modifiers & compareBits) != 0;
    }
}
