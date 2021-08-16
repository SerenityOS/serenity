/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file, and Oracle licenses the original version of this file under the BSD
 * license:
 */
/*
   Copyright 2009-2013 Attila Szegedi

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are
   met:
   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
   * Neither the name of the copyright holder nor the names of
     contributors may be used to endorse or promote products derived from
     this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
   IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL COPYRIGHT HOLDER
   BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
   WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
   OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
   ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package jdk.dynalink.beans;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jdk.dynalink.SecureLookupSupplier;
import jdk.dynalink.internal.InternalTypeUtilities;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.support.Lookup;

/**
 * Represents a subset of overloaded methods for a certain method name on a certain class. It can be either a fixarg or
 * a vararg subset depending on the subclass. The method is for a fixed number of arguments though (as it is generated
 * for a concrete call site). As such, all methods in the subset can be invoked with the specified number of arguments
 * (exactly matching for fixargs, or having less than or equal fixed arguments, for varargs).
 */
class OverloadedMethod {
    private final Map<ClassString, MethodHandle> argTypesToMethods = new ConcurrentHashMap<>();
    private final OverloadedDynamicMethod parent;
    private final ClassLoader callSiteClassLoader;
    private final MethodType callSiteType;
    private final MethodHandle invoker;
    private final LinkerServices linkerServices;
    private final SecureLookupSupplier lookupSupplier;
    private final ArrayList<MethodHandle> fixArgMethods;
    private final ArrayList<MethodHandle> varArgMethods;

    OverloadedMethod(final List<MethodHandle> methodHandles,
            final OverloadedDynamicMethod parent,
            final ClassLoader callSiteClassLoader,
            final MethodType callSiteType,
            final LinkerServices linkerServices,
            final SecureLookupSupplier lookupSupplier) {
        this.parent = parent;
        this.callSiteClassLoader = callSiteClassLoader;
        final Class<?> commonRetType = getCommonReturnType(methodHandles);
        this.callSiteType = callSiteType.changeReturnType(commonRetType);
        this.linkerServices = linkerServices;
        this.lookupSupplier = lookupSupplier;

        fixArgMethods = new ArrayList<>(methodHandles.size());
        varArgMethods = new ArrayList<>(methodHandles.size());
        final int argNum = callSiteType.parameterCount();
        for(final MethodHandle mh: methodHandles) {
            if(mh.isVarargsCollector()) {
                final MethodHandle asFixed = mh.asFixedArity();
                if(argNum == asFixed.type().parameterCount()) {
                    fixArgMethods.add(asFixed);
                }
                varArgMethods.add(mh);
            } else {
                fixArgMethods.add(mh);
            }
        }
        fixArgMethods.trimToSize();
        varArgMethods.trimToSize();

        final MethodHandle bound = SELECT_METHOD.bindTo(this);
        final MethodHandle collecting = SingleDynamicMethod.collectArguments(bound, argNum).asType(
                callSiteType.changeReturnType(MethodHandle.class));
        invoker = linkerServices.asTypeLosslessReturn(MethodHandles.foldArguments(
                MethodHandles.exactInvoker(this.callSiteType), collecting), callSiteType);
    }

    MethodHandle getInvoker() {
        return invoker;
    }

    private static final MethodHandle SELECT_METHOD = Lookup.findOwnSpecial(MethodHandles.lookup(), "selectMethod",
            MethodHandle.class, Object[].class);

    @SuppressWarnings("unused")
    private MethodHandle selectMethod(final Object[] args) {
        final Class<?>[] argTypes = new Class<?>[args.length];
        for(int i = 0; i < argTypes.length; ++i) {
            final Object arg = args[i];
            argTypes[i] = arg == null ? ClassString.NULL_CLASS : arg.getClass();
        }
        final ClassString classString = new ClassString(argTypes);
        MethodHandle method = argTypesToMethods.get(classString);
        if(method == null) {
            List<MethodHandle> methods = classString.getMaximallySpecifics(fixArgMethods, linkerServices, false);
            if(methods.isEmpty()) {
                methods = classString.getMaximallySpecifics(varArgMethods, linkerServices, true);
            }
            switch(methods.size()) {
                case 0: {
                    method = getNoSuchMethodThrower(argTypes);
                    break;
                }
                case 1: {
                    final List<MethodHandle> fmethods = methods;
                    method = linkerServices.getWithLookup(
                            ()->SingleDynamicMethod.getInvocation(fmethods.get(0), callSiteType, linkerServices),
                            lookupSupplier);
                    break;
                }
                default: {
                    // This is unfortunate - invocation time ambiguity.
                    method = getAmbiguousMethodThrower(argTypes, methods);
                    break;
                }
            }
            // Avoid keeping references to unrelated classes; this ruins the
            // performance a bit, but avoids class loader memory leaks.
            if(classString.isVisibleFrom(callSiteClassLoader)) {
                argTypesToMethods.put(classString, method);
            }
        }
        return method;
    }

    private MethodHandle getNoSuchMethodThrower(final Class<?>[] argTypes) {
        return adaptThrower(MethodHandles.insertArguments(THROW_NO_SUCH_METHOD, 0, this, argTypes));
    }

    private static final MethodHandle THROW_NO_SUCH_METHOD = Lookup.findOwnSpecial(MethodHandles.lookup(),
            "throwNoSuchMethod", void.class, Class[].class);

    @SuppressWarnings("unused")
    private void throwNoSuchMethod(final Class<?>[] argTypes) throws NoSuchMethodException {
        if(varArgMethods.isEmpty()) {
            throw new NoSuchMethodException("None of the fixed arity signatures " + getSignatureList(fixArgMethods) +
                    " of method " + parent.getName() + " match the argument types " + argTypesString(argTypes));
        }
        throw new NoSuchMethodException("None of the fixed arity signatures " + getSignatureList(fixArgMethods) +
                " or the variable arity signatures " + getSignatureList(varArgMethods) + " of the method " +
                parent.getName() + " match the argument types " + argTypesString(argTypes));
    }

    private MethodHandle getAmbiguousMethodThrower(final Class<?>[] argTypes, final List<MethodHandle> methods) {
        return adaptThrower(MethodHandles.insertArguments(THROW_AMBIGUOUS_METHOD, 0, this, argTypes, methods));
    }

    private MethodHandle adaptThrower(final MethodHandle rawThrower) {
        return MethodHandles.dropArguments(rawThrower, 0, callSiteType.parameterList()).asType(callSiteType);
    }

    private static final MethodHandle THROW_AMBIGUOUS_METHOD = Lookup.findOwnSpecial(MethodHandles.lookup(),
            "throwAmbiguousMethod", void.class, Class[].class, List.class);

    @SuppressWarnings("unused")
    private void throwAmbiguousMethod(final Class<?>[] argTypes, final List<MethodHandle> methods) throws NoSuchMethodException {
        final String arity = methods.get(0).isVarargsCollector() ? "variable" : "fixed";
        throw new NoSuchMethodException("Can't unambiguously select between " + arity + " arity signatures " +
                getSignatureList(methods) + " of the method " + parent.getName() + " for argument types " +
                argTypesString(argTypes));
    }

    private static String argTypesString(final Class<?>[] classes) {
        final StringBuilder b = new StringBuilder().append('[');
        appendTypes(b, classes, false);
        return b.append(']').toString();
    }

    private static String getSignatureList(final List<MethodHandle> methods) {
        final StringBuilder b = new StringBuilder().append('[');
        final Iterator<MethodHandle> it = methods.iterator();
        if(it.hasNext()) {
            appendSig(b, it.next());
            while(it.hasNext()) {
                appendSig(b.append(", "), it.next());
            }
        }
        return b.append(']').toString();
    }

    private static void appendSig(final StringBuilder b, final MethodHandle m) {
        b.append('(');
        appendTypes(b, m.type().parameterArray(), m.isVarargsCollector());
        b.append(')');
    }

    private static void appendTypes(final StringBuilder b, final Class<?>[] classes, final boolean varArg) {
        final int l = classes.length;
        if(!varArg) {
            if(l > 1) {
                b.append(classes[1].getCanonicalName());
                for(int i = 2; i < l; ++i) {
                    b.append(", ").append(classes[i].getCanonicalName());
                }
            }
        } else {
            for(int i = 1; i < l - 1; ++i) {
                b.append(classes[i].getCanonicalName()).append(", ");
            }
            b.append(classes[l - 1].getComponentType().getCanonicalName()).append("...");
        }
    }

    private static Class<?> getCommonReturnType(final List<MethodHandle> methodHandles) {
        final Iterator<MethodHandle> it = methodHandles.iterator();
        Class<?> retType = it.next().type().returnType();
        while(it.hasNext()) {
            retType = InternalTypeUtilities.getCommonLosslessConversionType(retType, it.next().type().returnType());
        }
        return retType;
    }
}
