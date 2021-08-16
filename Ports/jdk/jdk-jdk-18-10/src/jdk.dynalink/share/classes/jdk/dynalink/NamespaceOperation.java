/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
   Copyright 2016 Attila Szegedi

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

package jdk.dynalink;

import java.util.Arrays;
import java.util.Objects;

/**
 * Describes an operation that operates on at least one {@link Namespace} of
 * an object. E.g. a property getter would be described as
 * <pre>
 * Operation propertyGetter = new NamespaceOperation(
 *     StandardOperation.GET,
 *     StandardNamespace.PROPERTY);
 * </pre>
 * They are often combined with {@link NamedOperation}, e.g. to express a
 * property getter for a property named "color", you would construct:
 * <pre>
 * Operation colorPropertyGetter = new NamedOperation(
 *     new NamespaceOperation(
 *         StandardOperation.GET,
 *         StandardNamespace.PROPERTY),
 *     "color");
 * </pre>
 * <p>While {@code NamespaceOperation} can be constructed directly, it is often convenient
 * to use the {@link Operation#withNamespace(Namespace)} and {@link Operation#withNamespaces(Namespace...)} factory
 * methods instead, e.g.:
 * <pre>
 * Operation getElementOrPropertyEmpty =
 *     StandardOperation.GET
 *         .withNamespace(StandardNamespace.PROPERTY)
 *         .named("color");
 * </pre>
 * <h2>Operations on multiple namespaces</h2>
 * If multiple namespaces are specified, the namespaces are treated as
 * alternatives to each other in order of preference. The semantics of
 * such operation is "first applicable".
 * That is, a composite of {@code GET:PROPERTY|ELEMENT:color} should be
 * interpreted as <i>get the property named "color" on the object, but if the
 * property does not exist, then get the collection element named "color"
 * instead</i>.
 * <p>
 * Operations with multiple namespaces are helpful in implementation of languages that
 * don't distinguish between one or more of the namespaces, or when expressing operations
 * against objects that can be considered both ordinary objects and collections, e.g. Java
 * {@link java.util.Map} objects. A {@code GET:PROPERTY|ELEMENT:empty} operation
 * against a Java map will always match
 * the {@link java.util.Map#isEmpty()} property, but
 * {@code GET:ELEMENT|PROPERTY:empty} will actually match a map element with
 * key {@code "empty"} if the map contains that key, and only fall back to the
 * {@code isEmpty()} property getter if the map does not contain the key. If
 * the source language mandates this semantics, it can be easily achieved using
 * operations on multiple namespaces.
 * <p>
 * Even if the language itself doesn't distinguish between some of the
 * namespaces, it can be helpful to map different syntaxes to different namespace orderings.
 * E.g. the source expression {@code obj.color} could map to
 * {@code GET:PROPERTY|ELEMENT|METHOD:color}, but a different source
 * expression that looks like collection element access {@code obj[key]} could
 * be expressed instead as {@code GET:ELEMENT|PROPERTY|METHOD} in order to favor the
 * element semantics. Finally, if the retrieved value is subsequently called, then it makes sense
 * to bring {@code METHOD} to the front of the namespace list: the getter part of the
 * source expression {@code obj.color()} could be
 * {@code GET:METHOD|PROPERTY|ELEMENT:color} and the one for
 * {@code obj[key]()} could be {@code GET:METHOD|ELEMENT|PROPERTY}.
 * <p>
 * The base operation of a namespace operation can not itself be a namespace or named
 * operation, but rather one of simple operations such are elements of
 * {@link StandardOperation}. A namespace operation itself can serve as the base
 * operation of a named operation, though; a typical way to construct e.g. the
 * {@code GET:ELEMENT|PROPERTY:empty} from above would be:
 * <pre>
 * Operation getElementOrPropertyEmpty = StandardOperation.GET
 *     .withNamespaces(
 *         StandardNamespace.ELEMENT,
 *         StandardNamespace.PROPERTY)
 *     .named("empty");
 * </pre>
 */
public final class NamespaceOperation implements Operation {
    private final Operation baseOperation;
    private final Namespace[] namespaces;

    /**
     * Constructs a new namespace operation.
     * @param baseOperation the base operation that operates on one or more namespaces.
     * @param namespaces one or more namespaces this operation operates on.
     * @throws IllegalArgumentException if less than one namespace is
     * specified, or the base operation is itself a {@link NamespaceOperation} or a
     * {@link NamedOperation}.
     * @throws NullPointerException if either the {@code namespaces} array or any of its
     * elements are {@code null}, or if {@code baseOperation} is {@code null}.
     */
    public NamespaceOperation(final Operation baseOperation, final Namespace... namespaces) {
        this.baseOperation = Objects.requireNonNull(baseOperation, "baseOperation is null");
        if (baseOperation instanceof NamedOperation) {
            throw new IllegalArgumentException("baseOperation is a NamedOperation");
        } else if (baseOperation instanceof NamespaceOperation) {
           throw new IllegalArgumentException("baseOperation is a NamespaceOperation");
        }

        this.namespaces = Objects.requireNonNull(namespaces, "namespaces array is null").clone();
        if (namespaces.length < 1) {
            throw new IllegalArgumentException("Must specify at least one namespace");
        }
        for(int i = 0; i < namespaces.length; ++i) {
            final int fi = i;
            Objects.requireNonNull(namespaces[i], () -> "operations[" + fi + "] is null");
        }
    }

    /**
     * Returns the base operation of this named operation.
     * @return the base operation of this named operation.
     */
    public Operation getBaseOperation() {
        return baseOperation;
    }

    /**
     * Returns the namespaces in this namespace operation. The returned
     * array is a copy and changes to it don't have effect on this
     * object.
     * @return the namespaces in this namespace operation.
     */
    public Namespace[] getNamespaces() {
        return namespaces.clone();
    }

    /**
     * Returns the number of namespaces in this namespace operation.
     * @return the number of namespaces in this namespace operation.
     */
    public int getNamespaceCount() {
        return namespaces.length;
    }

    /**
     * Returns the i-th namespace in this namespace operation.
     * @param i the namespace index
     * @return the i-th namespace in this namespace operation.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */
    public Namespace getNamespace(final int i) {
        try {
            return namespaces[i];
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException(Integer.toString(i));
        }
    }

    /**
     * Returns true if this namespace operation contains a namespace equal to
     * the specified namespace.
     * @param namespace the namespace being searched for. Must not be null.
     * @return true if the if this namespace operation contains a namespace
     * equal to the specified namespace.
     */
    public boolean contains(final Namespace namespace) {
        Objects.requireNonNull(namespace);
        for(final Namespace component: namespaces) {
            if (component.equals(namespace)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the other object is also a namespace operation and their
     * base operation and namespaces are equal.
     * @param obj the object to compare to
     * @return true if this object is equal to the other one, false otherwise.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof NamespaceOperation) {
            final NamespaceOperation other = (NamespaceOperation)obj;
            return baseOperation.equals(other.baseOperation) && Arrays.equals(namespaces, other.namespaces);
        }
        return false;
    }

    /**
     * Returns the hash code of this namespace operation. Defined to be equal
     * to {@code baseOperation.hashCode() + 31 * Arrays.hashCode(namespaces)}.
     */
    @Override
    public int hashCode() {
        return baseOperation.hashCode() + 31 * Arrays.hashCode(namespaces);
    }

    /**
     * Returns the string representation of this namespace operation. Defined to
     * be the {@code toString} of its base operation, followed by a colon character,
     * followed with the list of its namespaces separated with the vertical line
     * character (e.g. {@code "GET:PROPERTY|ELEMENT"}).
     * @return the string representation of this namespace operation.
     */
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append(baseOperation).append(':');
        b.append(namespaces[0]);
        for(int i = 1; i < namespaces.length; ++i) {
            b.append('|').append(namespaces[i]);
        }
        return b.toString();
    }

    /**
     * If the passed operation is a namespace operation, returns its
     * {@link #getBaseOperation()}, otherwise returns the operation as is.
     * @param op the operation
     * @return the base operation of the passed operation.
     */
    public static Operation getBaseOperation(final Operation op) {
        return op instanceof NamespaceOperation ? ((NamespaceOperation )op).getBaseOperation() : op;
    }

    /**
     * If the passed operation is a namespace operation, returns its
     * {@link #getNamespaces()}, otherwise returns an empty array.
     * @param op the operation
     * @return the namespaces of the passed operation.
     */
    public static Namespace[] getNamespaces(final Operation op) {
        return op instanceof NamespaceOperation ? ((NamespaceOperation)op).getNamespaces() : new Namespace[0];
    }

    /**
     * Returns true if the specified operation is a {@link NamespaceOperation}
     * and its base operation is equal to the specified operation, and it
     * contains the specified namespace. If it is not a {@link NamespaceOperation},
     * then it returns false.
     * @param op the operation. Must not be null.
     * @param baseOperation the base operation being searched for. Must not be null.
     * @param namespace the namespace being searched for. Must not be null.
     * @return true if the if the passed operation is a {@link NamespaceOperation},
     * its base operation equals the searched base operation, and contains a namespace
     * equal to the searched namespace.
     */
    public static boolean contains(final Operation op, final Operation baseOperation, final Namespace namespace) {
        if (op instanceof NamespaceOperation) {
            final NamespaceOperation no = (NamespaceOperation)op;
            return no.baseOperation.equals(baseOperation) && no.contains(namespace);
        }
        return false;
    }
}
