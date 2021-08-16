/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for discovering accessible methods and inner classes. Normally, a public member declared on a class is
 * accessible (that is, it can be invoked from anywhere). However, this is not the case if the class itself is not
 * public, or belongs to a restricted-access package. In that case, it is required to lookup a member in a publicly
 * accessible superclass or implemented interface of the class, and use it instead of the member discovered on the
 * class.
 */
class AccessibleMembersLookup {
    private final Map<MethodSignature, Method> methods;
    private final Map<String, Class<?>> innerClasses;
    private final boolean instance;

    /**
     * Creates a mapping for all accessible methods and inner classes on a class.
     *
     * @param clazz the inspected class
     * @param instance true to inspect instance methods, false to inspect static methods.
     */
    AccessibleMembersLookup(final Class<?> clazz, final boolean instance) {
        this.methods = new HashMap<>();
        this.innerClasses = new LinkedHashMap<>();
        this.instance = instance;
        lookupAccessibleMembers(clazz);
    }

    Collection<Method> getMethods() {
        return methods.values();
    }

    Class<?>[] getInnerClasses() {
        return innerClasses.values().toArray(new Class<?>[0]);
    }

    Method getAccessibleMethod(final Method m) {
        return methods.get(new MethodSignature(m));
    }

    /**
     * A helper class that represents a method signature - name and argument types.
     */
    static final class MethodSignature {
        private final String name;
        private final Class<?>[] args;

        /**
         * Creates a new method signature from arbitrary data.
         *
         * @param name the name of the method this signature represents.
         * @param args the argument types of the method.
         */
        MethodSignature(final String name, final Class<?>[] args) {
            this.name = name;
            this.args = args;
        }

        /**
         * Creates a signature for the given method.
         *
         * @param method the method for which a signature is created.
         */
        MethodSignature(final Method method) {
            this(method.getName(), method.getParameterTypes());
        }

        /**
         * Compares this object to another object
         *
         * @param o the other object
         * @return true if the other object is also a method signature with the same name, same number of arguments, and
         * same types of arguments.
         */
        @Override
        public boolean equals(final Object o) {
            if(o instanceof MethodSignature) {
                final MethodSignature ms = (MethodSignature)o;
                return ms.name.equals(name) && Arrays.equals(args, ms.args);
            }
            return false;
        }

        /**
         * Returns a hash code, consistent with the overridden {@link #equals(Object)}.
         */
        @Override
        public int hashCode() {
            return name.hashCode() ^ Arrays.hashCode(args);
        }

        @Override
        public String toString() {
            final StringBuilder b = new StringBuilder();
            b.append("[MethodSignature ").append(name).append('(');
            if(args.length > 0) {
                b.append(args[0].getCanonicalName());
                for(int i = 1; i < args.length; ++i) {
                    b.append(", ").append(args[i].getCanonicalName());
                }
            }
            return b.append(")]").toString();
        }
    }

    private void lookupAccessibleMembers(final Class<?> clazz) {
        boolean searchSuperTypes;

        if(!CheckRestrictedPackage.isRestrictedClass(clazz)) {
            searchSuperTypes = false;
            for(final Method method: clazz.getMethods()) {
                final boolean isStatic = Modifier.isStatic(method.getModifiers());
                if(instance != isStatic) {
                    final MethodSignature sig = new MethodSignature(method);
                    if(!methods.containsKey(sig)) {
                        final Class<?> declaringClass = method.getDeclaringClass();
                        if(declaringClass != clazz && CheckRestrictedPackage.isRestrictedClass(declaringClass)) {
                            //Sometimes, the declaring class of a method (Method.getDeclaringClass())
                            //retrieved through Class.getMethods() for a public class will be a
                            //non-public superclass. For such a method, we need to find a method with
                            //the same name and signature in a public superclass or implemented
                            //interface.
                            //This typically doesn't happen with classes emitted by a reasonably modern
                            //javac, as it'll create synthetic delegator methods in all public
                            //immediate subclasses of the non-public class. We have, however, observed
                            //this in the wild with class files compiled with older javac that doesn't
                            //generate the said synthetic delegators.
                            searchSuperTypes = true;
                        } else {
                            // don't allow inherited static
                            if (!isStatic || clazz == declaringClass) {
                                methods.put(sig, method);
                            }
                        }
                    }
                }
            }
            for(final Class<?> innerClass: clazz.getClasses()) {
                // Add both static and non-static classes, regardless of instance flag. StaticClassLinker will just
                // expose non-static classes with explicit constructor outer class argument.
                // NOTE: getting inner class objects through getClasses() does not resolve them, so if those classes
                // were not yet loaded, they'll only get loaded in a non-resolved state; no static initializers for
                // them will trigger just by doing this.
                // Don't overwrite an inner class with an inherited inner class with the same name.
                Class<?> previousClass = innerClasses.get(innerClass.getSimpleName());
                if (previousClass == null || previousClass.getDeclaringClass().isAssignableFrom(innerClass.getDeclaringClass())) {
                    innerClasses.put(innerClass.getSimpleName(), innerClass);
                }
            }
        } else {
            searchSuperTypes = true;
        }

        // don't need to search super types for static methods
        if(instance && searchSuperTypes) {
            // If we reach here, the class is either not public, or it is in a restricted package. Alternatively, it is
            // public, but some of its methods claim that their declaring class is non-public. We'll try superclasses
            // and implemented interfaces then looking for public ones.
            for (final Class<?> itf: clazz.getInterfaces()) {
                lookupAccessibleMembers(itf);
            }
            final Class<?> superclass = clazz.getSuperclass();
            if(superclass != null) {
                lookupAccessibleMembers(superclass);
            }
        }
    }
}
