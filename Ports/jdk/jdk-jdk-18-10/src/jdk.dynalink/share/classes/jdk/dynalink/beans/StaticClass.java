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

import java.io.Serializable;
import java.util.Objects;
import jdk.dynalink.StandardOperation;

/**
 * Object that allows access to the static members of a class (its static
 * methods, properties, and fields), as well as construction of instances using
 * {@link StandardOperation#NEW} operation. In Dynalink, {@link Class} objects
 * are not treated specially and act as ordinary Java objects; you can use e.g.
 * {@code GET:PROPERTY:superclass} as a property getter to
 * invoke {@code clazz.getSuperclass()}. On the other hand, you can not use
 * {@code Class} objects to access static members of a class, nor to create new
 * instances of the class using {@code NEW}. This is consistent with how
 * {@code Class} objects behave in Java: in Java, you write e.g.
 * {@code new BitSet()} instead of {@code new BitSet.class()}. Similarly, you
 * write {@code System.out} and not {@code System.class.out}. It is this aspect
 * of using a class name as the constructor and a namespace for static members
 * that {@code StaticClass} embodies.
 * <p>
 * Objects of this class are recognized by the {@link BeansLinker} as being
 * special, and operations on them will be linked against the represented class'
 * static members. The {@code "class"} synthetic property is additionally
 * recognized and returns the Java {@link Class} object, just as in Java
 * {@code System.class} evaluates to the {@code Class} object for the
 * {@code} System class. Conversely, {@link Class} objects exposed through
 * {@link BeansLinker} expose the {@code "static"} synthetic property which
 * returns their {@code StaticClass} object (there is no equivalent to this in
 * Java).
 * <p>
 * In summary, instances of this class act as namespaces for static members and
 * as constructors for classes, much the same way as specifying a class name in
 * Java language does, except that in Java this is just a syntactic element,
 * while in Dynalink they are expressed as actual objects.
 * <p>{@code StaticClass} objects representing Java array types will act as
 * constructors taking a single int argument and create an array of the
 * specified size.
 * <p>
 * If the class has several constructors, {@link StandardOperation#NEW} on
 * {@code StaticClass} will try to select the most specific applicable
 * constructor. You might want to expose a mechanism in your language for
 * selecting a constructor with an explicit signature through
 * {@link BeansLinker#getConstructorMethod(Class, String)}.
 */
public final class StaticClass implements Serializable {
    private static final ClassValue<StaticClass> staticClasses = new ClassValue<>() {
        @Override
        protected StaticClass computeValue(final Class<?> type) {
            return new StaticClass(type);
        }
    };

    private static final long serialVersionUID = 1L;

    /**
     * The runtime {@code Class} object whose static members this
     * {@code StaticClass} represents.
     */
    private final Class<?> clazz;

    /*private*/ StaticClass(final Class<?> clazz) {
        this.clazz = Objects.requireNonNull(clazz);
    }

    /**
     * Retrieves the {@link StaticClass} instance for the specified class.
     * @param clazz the class for which the static facet is requested.
     * @return the {@link StaticClass} instance representing the specified class.
     */
    public static StaticClass forClass(final Class<?> clazz) {
        return staticClasses.get(clazz);
    }

    /**
     * Returns the represented Java class.
     * @return the represented Java class.
     */
    public Class<?> getRepresentedClass() {
        return clazz;
    }

    @Override
    public String toString() {
        return "StaticClass[" + clazz.getName() + "]";
    }

    /**
     * Returns {@link #forClass(Class)} for the underlying {@code clazz} field
     * ensuring that deserialization doesn't create non-canonical instances.
     * @return {@link #forClass(Class)} for the underlying {@code clazz} field.
     */
    private Object readResolve() {
        return forClass(clazz);
    }
}
