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
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import jdk.dynalink.linker.support.Lookup;

/**
 * Base for classes that expose class field and method information to an {@link AbstractJavaLinker}. There are
 * subclasses for instance (bean) and static facet of a class.
 */
abstract class FacetIntrospector {
    private final Class<?> clazz;
    private final boolean instance;
    private final boolean isRestricted;

    protected final AccessibleMembersLookup membersLookup;

    FacetIntrospector(final Class<?> clazz, final boolean instance) {
        this.clazz = clazz;
        this.instance = instance;
        isRestricted = CheckRestrictedPackage.isRestrictedClass(clazz);
        membersLookup = new AccessibleMembersLookup(clazz, instance);
    }

    /**
     * Returns getters for inner classes.
     * @return getters for inner classes.
     */
    abstract Map<String, MethodHandle> getInnerClassGetters();

    /**
     * Returns getter methods for record components.
     * @return getter methods for record components.
     */
    abstract Collection<Method> getRecordComponentGetters();

    /**
     * Returns the fields for the class facet.
     * @return the fields for the class facet.
     */
    Collection<Field> getFields() {
        if(isRestricted) {
            // NOTE: we can't do anything here. Unlike with methods in AccessibleMethodsLookup, we can't just return
            // the fields from a public superclass, because this class might define same-named fields which will shadow
            // the superclass fields, and we have no way to know if they do, since we're denied invocation of
            // getFields(). Therefore, the only correct course of action is to not expose any public fields from a class
            // defined in a restricted package.
            return Collections.emptySet();
        }

        final Field[] fields = clazz.getFields();
        final Collection<Field> cfields = new ArrayList<>(fields.length);
        for(final Field field: fields) {
            final boolean isStatic = Modifier.isStatic(field.getModifiers());
            if(isStatic && clazz != field.getDeclaringClass()) {
                // ignore inherited static fields
                continue;
            }

            if(instance != isStatic && isAccessible(field)) {
                cfields.add(field);
            }
        }
        return cfields;
    }

    boolean isAccessible(final Member m) {
        final Class<?> declaring = m.getDeclaringClass();
        // (declaring == clazz) is just an optimization - we're calling this only from code that operates on a
        // non-restricted class, so if the declaring class is identical to the class being inspected, then forego
        // a potentially expensive restricted-package check.
        return declaring == clazz || !CheckRestrictedPackage.isRestrictedClass(declaring);
    }

    /**
     * Returns all the methods in the facet.
     * @return all the methods in the facet.
     */
    Collection<Method> getMethods() {
        return membersLookup.getMethods();
    }

    MethodHandle unreflectGetter(final Field field) {
        return editMethodHandle(Lookup.PUBLIC.unreflectGetter(field));
    }

    MethodHandle unreflectSetter(final Field field) {
        return editMethodHandle(Lookup.PUBLIC.unreflectSetter(field));
    }

    /**
     * Returns an edited method handle. A facet might need to edit an unreflected method handle before it is usable with
     * the facet. By default, returns the passed method handle unchanged. The class' static facet will introduce a
     * dropArguments.
     * @param mh the method handle to edit.
     * @return the edited method handle.
     */
    abstract MethodHandle editMethodHandle(MethodHandle mh);
}
