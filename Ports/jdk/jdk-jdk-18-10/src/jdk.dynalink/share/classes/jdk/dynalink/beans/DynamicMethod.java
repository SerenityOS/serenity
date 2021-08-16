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
import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.linker.LinkerServices;

/**
 * Represents a single dynamic method. A "dynamic" method can be bound to a single Java method, or can be bound to all
 * overloaded methods of the same name on a class. Getting an invocation of a dynamic method bound to multiple
 * overloaded methods will perform overload resolution (actually, it will perform partial overloaded resolution at link
 * time, but if that fails to identify exactly one target method, it will generate a method handle that will perform the
 * rest of the overload resolution at invocation time for actual argument types).
 */
abstract class DynamicMethod {
    private final String name;

    DynamicMethod(final String name) {
        this.name = name;
    }

    String getName() {
        return name;
    }

    /**
     * Creates an invocation for the dynamic method. If the method is overloaded, it will perform overloaded method
     * resolution based on the specified method type. The resulting resolution can either identify a single method to be
     * invoked among the overloads, or it can identify multiple ones. In the latter case, the returned method handle
     * will perform further overload resolution among these candidates at every invocation. If the method to be invoked
     * is a variable arguments (vararg) method, it will pack the extra arguments in an array before the invocation of
     * the underlying method if it is not already done.
     *
     * @param callSiteDescriptor the descriptor of the call site
     * @param linkerServices linker services. Used for language-specific type conversions.
     * @return an invocation suitable for calling the method from the specified call site.
     */
    abstract MethodHandle getInvocation(CallSiteDescriptor callSiteDescriptor, LinkerServices linkerServices);

    /**
     * Returns a single dynamic method representing a single underlying Java method (possibly selected among several
     * overloads) with formal parameter types exactly matching the passed signature.
     * @param paramTypes the comma-separated list of requested parameter type names. The names will match both
     * qualified and unqualified type names.
     * @return a single dynamic method representing a single underlying Java method, or null if none of the Java methods
     * behind this dynamic method exactly match the requested parameter types.
     */
    abstract SingleDynamicMethod getMethodForExactParamTypes(String paramTypes);

    /**
     * True if this dynamic method already contains a method with an identical signature as the passed in method.
     * @param method the method to check
     * @return true if it already contains an equivalent method.
     */
    abstract boolean contains(SingleDynamicMethod method);

    static String getClassAndMethodName(final Class<?> clazz, final String name) {
        final String clazzName = clazz.getCanonicalName();
        return (clazzName == null ? clazz.getName() : clazzName) + "." + name;
    }

    @Override
    public String toString() {
        return "[" + getClass().getName() + " " + getName() + "]";
    }

    /**
     * True if this method happens to be a constructor method.
     *
     * @return true if this represents a constructor.
     */
    boolean isConstructor() {
        return false;
    }
}
