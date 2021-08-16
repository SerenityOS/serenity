/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.InternalException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.VirtualMachine;

/**
 * Represents non-concrete (that is, native or abstract) methods.
 * Private to MethodImpl.
 */
public class NonConcreteMethodImpl extends MethodImpl {

    private Location location = null;

    NonConcreteMethodImpl(VirtualMachine vm,
                          ReferenceTypeImpl declaringType,
                          long ref, String name, String signature,
                          String genericSignature, int modifiers)
    {
        // The generic signature is set when this is created
        super(vm, declaringType, ref, name, signature,
              genericSignature, modifiers);
    }

    public Location location() {
        if (isAbstract()) {
            return null;
        }
        if (location == null) {
            location = new LocationImpl(vm, this, -1);
        }
        return location;
    }

    public List<Location> allLineLocations(String stratumID,
                                           String sourceName) {
        return new ArrayList<>(0);
    }

    public List<Location> allLineLocations(SDE.Stratum stratum,
                                           String sourceName) {
        return new ArrayList<>(0);
    }

    public List<Location> locationsOfLine(String stratumID,
                                          String sourceName,
                                          int lineNumber) {
        return new ArrayList<>(0);
    }

    public List<Location> locationsOfLine(SDE.Stratum stratum,
                                          String sourceName,
                                          int lineNumber) {
        return new ArrayList<>(0);
    }

    public Location locationOfCodeIndex(long codeIndex) {
        return null;
    }

    public List<LocalVariable> variables() throws AbsentInformationException {
        throw new AbsentInformationException();
    }

    public List<LocalVariable> variablesByName(String name) throws AbsentInformationException {
        throw new AbsentInformationException();
    }

    public List<LocalVariable> arguments() throws AbsentInformationException {
        throw new AbsentInformationException();
    }

    public byte[] bytecodes() {
        return new byte[0];
    }

    int argSlotCount() throws AbsentInformationException {
        throw new InternalException("should not get here");
    }
}
