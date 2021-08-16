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

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.InternalException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Type;
import com.sun.jdi.VirtualMachine;

public class LocalVariableImpl extends MirrorImpl
                               implements LocalVariable, ValueContainer
{
    private final Method method;
    private final int slot;
    private final Location scopeStart;
    private final Location scopeEnd;
    private final String name;
    private final String signature;
    private String genericSignature = null;

    LocalVariableImpl(VirtualMachine vm, Method method,
                      int slot, Location scopeStart, Location scopeEnd,
                      String name, String signature,
                      String genericSignature) {
        super(vm);
        this.method = method;
        this.slot = slot;
        this.scopeStart = scopeStart;
        this.scopeEnd = scopeEnd;
        this.name = name;
        this.signature = signature;
        if (genericSignature != null && genericSignature.length() > 0) {
            this.genericSignature = genericSignature;
        } else {
            // The Spec says to return null for non-generic types
            this.genericSignature = null;
        }
    }

    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof LocalVariableImpl)) {
            LocalVariableImpl other = (LocalVariableImpl)obj;
            return ((slot() == other.slot()) &&
                    (scopeStart != null) &&
                    (scopeStart.equals(other.scopeStart)) &&
                    (super.equals(obj)));
        } else {
            return false;
        }
    }

    public int hashCode() {
        /*
         * TO DO: Better hash code
         */
        return ((scopeStart.hashCode() << 4) + slot());
    }

    public int compareTo(LocalVariable object) {
        LocalVariableImpl other = (LocalVariableImpl)object;

        int rc = scopeStart.compareTo(other.scopeStart);
        if (rc == 0) {
            rc = slot() - other.slot();
        }
        return rc;
    }

    public String name() {
        return name;
    }

    /**
     * @return a text representation of the declared type
     * of this variable.
     */
    public String typeName() {
        JNITypeParser parser = new JNITypeParser(signature);
        return parser.typeName();
    }

    public Type type() throws ClassNotLoadedException {
        return findType(signature());
    }

    public Type findType(String signature) throws ClassNotLoadedException {
        ReferenceTypeImpl enclosing = (ReferenceTypeImpl)method.declaringType();
        return enclosing.findType(signature);
    }

    public String signature() {
        return signature;
    }

    public String genericSignature() {
        return genericSignature;
    }

    public boolean isVisible(StackFrame frame) {
        validateMirror(frame);
        Method frameMethod = frame.location().method();

        if (!frameMethod.equals(method)) {
            throw new IllegalArgumentException(
                       "frame method different than variable's method");
        }

        // this is here to cover the possibility that we will
        // allow LocalVariables for native methods.  If we do
        // so we will have to re-examinine this.
        if (frameMethod.isNative()) {
            return false;
        }

        return ((scopeStart.compareTo(frame.location()) <= 0)
             && (scopeEnd.compareTo(frame.location()) >= 0));
    }

    public boolean isArgument() {
        try {
            MethodImpl method = (MethodImpl)scopeStart.method();
            return (slot < method.argSlotCount());
        } catch (AbsentInformationException e) {
            // If this variable object exists, there shouldn't be absent info
            throw new InternalException();
        }
    }

    int slot() {
        return slot;
    }

    /*
     * Compilers/VMs can have byte code ranges for variables of the
     * same names that overlap. This is because the byte code ranges
     * aren't necessarily scopes; they may have more to do with the
     * lifetime of the variable's slot, depending on implementation.
     *
     * This method determines whether this variable hides an
     * identically named variable; ie, their byte code ranges overlap
     * this one starts after the given one. If it returns true this
     * variable should be preferred when looking for a single variable
     * with its name when both variables are visible.
     */
    boolean hides(LocalVariable other) {
        LocalVariableImpl otherImpl = (LocalVariableImpl)other;
        if (!method.equals(otherImpl.method) ||
            !name.equals(otherImpl.name)) {
            return false;
        } else {
            return (scopeStart.compareTo(otherImpl.scopeStart) > 0);
        }
    }

    public String toString() {
       return name() + " in " + method.toString() +
              "@" + scopeStart.toString();
    }
}
