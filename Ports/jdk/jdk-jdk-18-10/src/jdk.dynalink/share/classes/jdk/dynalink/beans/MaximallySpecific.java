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
import java.lang.invoke.MethodType;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import jdk.dynalink.linker.ConversionComparator.Comparison;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.support.TypeUtilities;

/**
 * Utility class that encapsulates the algorithm for choosing the maximally specific methods.
 */
class MaximallySpecific {
    /**
     * Given a list of methods, returns a list of maximally specific methods.
     *
     * @param methods the list of methods
     * @param varArgs whether to assume the methods are varargs
     * @return the list of maximally specific methods.
     */
    static List<SingleDynamicMethod> getMaximallySpecificMethods(final List<SingleDynamicMethod> methods, final boolean varArgs) {
        return getMaximallySpecificMethods(methods, varArgs, null, null, SingleDynamicMethod::getMethodType);
    }

     /**
      * Given a list of methods handles, returns a list of maximally specific methods, applying language-runtime
      * specific conversion preferences.
      *
      * @param methods the list of method handles
      * @param varArgs whether to assume the method handles are varargs
      * @param argTypes concrete argument types for the invocation
      * @return the list of maximally specific method handles.
      */
     static List<MethodHandle> getMaximallySpecificMethodHandles(final List<MethodHandle> methods, final boolean varArgs,
             final Class<?>[] argTypes, final LinkerServices ls) {
         return getMaximallySpecificMethods(methods, varArgs, argTypes, ls, MethodHandle::type);
     }

    /**
     * Given a list of methods, returns a list of maximally specific methods, applying language-runtime specific
     * conversion preferences.
     *
     * @param methods the list of methods
     * @param varArgs whether to assume the methods are varargs
     * @param argTypes concrete argument types for the invocation
     * @return the list of maximally specific methods.
     */
    private static <T> List<T> getMaximallySpecificMethods(final List<T> methods, final boolean varArgs,
            final Class<?>[] argTypes, final LinkerServices ls, final Function<T, MethodType> methodTypeGetter) {
        if(methods.size() < 2) {
            return methods;
        }
        final LinkedList<T> maximals = new LinkedList<>();
        for(final T m: methods) {
            final MethodType methodType = methodTypeGetter.apply(m);
            boolean lessSpecific = false;
            for(final Iterator<T> maximal = maximals.iterator(); maximal.hasNext();) {
                final T max = maximal.next();
                switch(isMoreSpecific(methodType, methodTypeGetter.apply(max), varArgs, argTypes, ls)) {
                    case TYPE_1_BETTER: {
                        maximal.remove();
                        break;
                    }
                    case TYPE_2_BETTER: {
                        lessSpecific = true;
                        break;
                    }
                    case INDETERMINATE: {
                        // do nothing
                        break;
                    }
                    default: {
                        throw new AssertionError();
                    }
                }
            }
            if(!lessSpecific) {
                maximals.addLast(m);
            }
        }
        return maximals;
    }

    private static Comparison isMoreSpecific(final MethodType t1, final MethodType t2, final boolean varArgs, final Class<?>[] argTypes,
            final LinkerServices ls) {
        final int pc1 = t1.parameterCount();
        final int pc2 = t2.parameterCount();
        assert varArgs || (pc1 == pc2) && (argTypes == null || argTypes.length == pc1);
        assert (argTypes == null) == (ls == null);
        final int maxPc = Math.max(Math.max(pc1, pc2), argTypes == null ? 0 : argTypes.length);
        boolean t1MoreSpecific = false;
        boolean t2MoreSpecific = false;
        // NOTE: Starting from 1 as overloaded method resolution doesn't depend on 0th element, which is the type of
        // 'this'. We're only dealing with instance methods here, not static methods. Actually, static methods will have
        // a fake 'this' of type StaticClass.
        for(int i = 1; i < maxPc; ++i) {
            final Class<?> c1 = getParameterClass(t1, pc1, i, varArgs);
            final Class<?> c2 = getParameterClass(t2, pc2, i, varArgs);
            if(c1 != c2) {
                final Comparison cmp = compare(c1, c2, argTypes, i, ls);
                if(cmp == Comparison.TYPE_1_BETTER && !t1MoreSpecific) {
                    t1MoreSpecific = true;
                    if(t2MoreSpecific) {
                        return Comparison.INDETERMINATE;
                    }
                }
                if(cmp == Comparison.TYPE_2_BETTER && !t2MoreSpecific) {
                    t2MoreSpecific = true;
                    if(t1MoreSpecific) {
                        return Comparison.INDETERMINATE;
                    }
                }
            }
        }
        if(t1MoreSpecific) {
            return Comparison.TYPE_1_BETTER;
        } else if(t2MoreSpecific) {
            return Comparison.TYPE_2_BETTER;
        }
        return Comparison.INDETERMINATE;
    }

    private static Comparison compare(final Class<?> c1, final Class<?> c2, final Class<?>[] argTypes, final int i, final LinkerServices cmp) {
        if(cmp != null) {
            final Comparison c = cmp.compareConversion(argTypes[i], c1, c2);
            if(c != Comparison.INDETERMINATE) {
                return c;
            }
        }
        if(TypeUtilities.isSubtype(c1, c2)) {
            return Comparison.TYPE_1_BETTER;
        } if(TypeUtilities.isSubtype(c2, c1)) {
            return Comparison.TYPE_2_BETTER;
        }
        return Comparison.INDETERMINATE;
    }

    private static Class<?> getParameterClass(final MethodType t, final int l, final int i, final boolean varArgs) {
        return varArgs && i >= l - 1 ? t.parameterType(l - 1).getComponentType() : t.parameterType(i);
    }
}
