/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.jdi.ClassType;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;

final public class InterfaceTypeImpl extends InvokableTypeImpl
                                     implements InterfaceType {

    private static class IResult implements InvocationResult {
        final private JDWP.InterfaceType.InvokeMethod rslt;

        public IResult(JDWP.InterfaceType.InvokeMethod rslt) {
            this.rslt = rslt;
        }

        @Override
        public ObjectReferenceImpl getException() {
            return rslt.exception;
        }

        @Override
        public ValueImpl getResult() {
            return rslt.returnValue;
        }

    }

    private SoftReference<List<InterfaceType>> superinterfacesRef = null;

    protected InterfaceTypeImpl(VirtualMachine aVm,long aRef) {
        super(aVm, aRef);
    }

    public List<InterfaceType> superinterfaces() {
        List<InterfaceType> superinterfaces = (superinterfacesRef == null) ? null :
                                     superinterfacesRef.get();
        if (superinterfaces == null) {
            superinterfaces = getInterfaces();
            superinterfaces = Collections.unmodifiableList(superinterfaces);
            superinterfacesRef = new SoftReference<List<InterfaceType>>(superinterfaces);
        }
        return superinterfaces;
    }

    public List<InterfaceType> subinterfaces() {
        List<InterfaceType> subs = new ArrayList<InterfaceType>();
        vm.forEachClass(refType -> {
            if (refType instanceof InterfaceType) {
                InterfaceType interfaze = (InterfaceType)refType;
                if (interfaze.isPrepared() && interfaze.superinterfaces().contains(this)) {
                    subs.add(interfaze);
                }
            }
        });
        return subs;
    }

    public List<ClassType> implementors() {
        List<ClassType> implementors = new ArrayList<ClassType>();
        vm.forEachClass(refType -> {
            if (refType instanceof ClassType) {
                ClassType clazz = (ClassType)refType;
                if (clazz.isPrepared() && clazz.interfaces().contains(this)) {
                    implementors.add(clazz);
                }
            }
        });
        return implementors;
    }

    public boolean isInitialized() {
        return isPrepared();
    }

    public String toString() {
       return "interface " + name() + " (" + loaderString() + ")";
    }

    @Override
    InvocationResult waitForReply(PacketStream stream) throws JDWPException {
        return new IResult(JDWP.InterfaceType.InvokeMethod.waitForReply(vm, stream));
    }

    @Override
    CommandSender getInvokeMethodSender(final ThreadReferenceImpl thread,
                                        final MethodImpl method,
                                        final ValueImpl[] args,
                                        final int options) {
        return () ->
            JDWP.InterfaceType.InvokeMethod.enqueueCommand(vm,
                                                           InterfaceTypeImpl.this,
                                                           thread,
                                                           method.ref(),
                                                           args,
                                                           options);
    }

    @Override
    ClassType superclass() {
        return null;
    }

    @Override
    boolean isAssignableTo(ReferenceType type) {
        if (type.name().equals("java.lang.Object")) {
            // interfaces are always assignable to j.l.Object
            return true;
        }
        return super.isAssignableTo(type);
    }

    @Override
    List<InterfaceType> interfaces() {
        return superinterfaces();
    }

    @Override
    boolean canInvoke(Method method) {
        // method must be directly in this interface
        return this.equals(method.declaringType());
    }
}
