/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
   Copyright 2015 Attila Szegedi

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

import java.util.Objects;

/**
 * Operation that associates a name with another operation. Typically used with
 * operations that normally take a name or an index to bind them to a fixed
 * name. E.g.
 * <pre>
 *     new NamedOperation(
 *         new NamespaceOperation(
 *             StandardOperation.GET,
 *             StandardNamespace.PROPERTY),
 *         "color")
 * </pre>
 * will be a named operation for getting the property named "color" on the
 * object it is applied to, and
 * <pre>
 *     new NamedOperation(
 *         new NamespaceOperation(
 *             StandardOperation.GET,
 *             StandardNamespace.ELEMENT),
 *         3)
 * </pre>
 * will be a named operation for getting the element at index 3 from the collection
 * it is applied to ("name" in this context is akin to "address" and encompasses both
 * textual names, numeric indices, or any other kinds of addressing that linkers can
 * understand). In these cases, the expected signature of the call site for the
 * operation will change to no longer include the name parameter. Specifically,
 * the documentation for all {@link StandardOperation} members describes how
 * they are affected by being incorporated into a named operation.
 * <p>While {@code NamedOperation} can be constructed directly, it is often convenient
 * to use the {@link Operation#named(Object)} factory method instead, e.g.:
 * <pre>
 *    StandardOperation.GET
 *        .withNamespace(StandardNamespace.ELEMENT),
 *        .named(3)
 *     )
 * </pre>
 * <p>
 * Even though {@code NamedOperation} is most often used with {@link NamespaceOperation} as
 * its base, it can have other operations as its base too (except another named operation).
 * Specifically, {@link StandardOperation#CALL} as well as {@link StandardOperation#NEW} can
 * both be used with {@code NamedOperation} directly. The contract for these operations is such
 * that when they are used as named operations, their name is only used for diagnostic messages,
 * usually containing the textual representation of the source expression that retrieved the
 * callee, e.g. {@code StandardOperation.CALL.named("window.open")}.
 * </p>
 */
public final class NamedOperation implements Operation {
    private final Operation baseOperation;
    private final Object name;

    /**
     * Creates a new named operation.
     * @param baseOperation the base operation that is associated with a name.
     * @param name the name associated with the base operation. Note that the
     * name is not necessarily a string, but can be an arbitrary object. As the
     * name is used for addressing, it can be an {@link Integer} when meant
     * to be used as an index into an array or list etc.
     * @throws NullPointerException if either {@code baseOperation} or
     * {@code name} is null.
     * @throws IllegalArgumentException if {@code baseOperation} is itself a
     * {@code NamedOperation}.
     */
    public NamedOperation(final Operation baseOperation, final Object name) {
        if (baseOperation instanceof NamedOperation) {
            throw new IllegalArgumentException("baseOperation is a NamedOperation");
        }
        this.baseOperation = Objects.requireNonNull(baseOperation, "baseOperation is null");
        this.name = Objects.requireNonNull(name, "name is null");
    }

    /**
     * Returns the base operation of this named operation.
     * @return the base operation of this named operation.
     */
    public Operation getBaseOperation() {
        return baseOperation;
    }

    /**
     * Returns the name of this named operation.
     * @return the name of this named operation.
     */
    public Object getName() {
        return name;
    }

    /**
     * Finds or creates a named operation that differs from this one only in the name.
     * @param newName the new name to replace the old name with.
     * @return a named operation with the changed name.
     * @throws NullPointerException if the name is null.
     */
    public final NamedOperation changeName(final String newName) {
        return new NamedOperation(baseOperation, newName);
    }

    /**
     * Compares this named operation to another object. Returns true if the
     * other object is also a named operation, and both their base operations
     * and name are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof NamedOperation) {
            final NamedOperation other = (NamedOperation)obj;
            return baseOperation.equals(other.baseOperation) && name.equals(other.name);
        }
        return false;
    }

    /**
     * Returns the hash code of this named operation. It is defined to be equal
     * to {@code baseOperation.hashCode() + 31 * name.hashCode()}.
     */
    @Override
    public int hashCode() {
        return baseOperation.hashCode() + 31 * name.hashCode();
    }

    /**
     * Returns the string representation of this named operation. It is defined
     * to be equal to {@code baseOperation.toString() + ":" + name.toString()}.
     */
    @Override
    public String toString() {
        return baseOperation.toString() + ":" + name.toString();
    }

    /**
     * If the passed operation is a named operation, returns its
     * {@link #getBaseOperation()}, otherwise returns the operation as is.
     * @param op the operation
     * @return the base operation of the passed operation.
     */
    public static Operation getBaseOperation(final Operation op) {
        return op instanceof NamedOperation ? ((NamedOperation)op).baseOperation : op;
    }

    /**
     * If the passed operation is a named operation, returns its
     * {@link #getName()}, otherwise returns null. Note that a named operation
     * object can never have a null name, therefore returning null is indicative
     * that the passed operation is not, in fact, a named operation.
     * @param op the operation
     * @return the name in the passed operation, or null if it is not a named
     * operation.
     */
    public static Object getName(final Operation op) {
        return op instanceof NamedOperation ? ((NamedOperation)op).name : null;
    }
}
