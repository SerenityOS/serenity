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

/**
 * An object that describes a dynamic operation. Dynalink defines a set of
 * standard operations with the {@link StandardOperation} class, as well as a
 * way to express the target {@link Namespace namespace(s)} of an operation
 * on an object using {@link NamespaceOperation} and finally a way to attach
 * a fixed target name to an operation using {@link NamedOperation}.
 * When presenting examples in this documentation, we will refer to standard
 * operations using their name (e.g. {@code GET}), to namespace operations
 * by separating their base operation with a colon from their namespace
 * (e.g. {@code GET:PROPERTY}), or in case of multiple namespaces we will
 * further separate those with the vertical line character (e.g.
 * {@code GET:PROPERTY|ELEMENT}), and finally we will refer to named operations
 * by separating the base operation and the name with the colon character (e.g.
 * {@code GET:PROPERTY|ELEMENT:color}).
 */
public interface Operation {
    /**
     * Returns a {@link NamespaceOperation} using this operation as its base.
     * @param namespace the namespace that is the target of the namespace operation.
     * @return a {@link NamespaceOperation} with this operation as its base and the specified
     * namespace as its target.
     * @throws IllegalArgumentException if this operation is already a namespace operation or a named operation.
     * @throws NullPointerException if {@code namespace} is null.
     */
    default NamespaceOperation withNamespace(final Namespace namespace) {
        return withNamespaces(namespace);
    }

    /**
     * Returns a {@link NamespaceOperation} using this operation as its base.
     * @param namespaces the namespaces that are the target of the namespace operation.
     * @return a {@link NamespaceOperation} with this operation as its base and the specified
     * namespaces as its targets.
     * @throws IllegalArgumentException if this operation is already a namespace operation or a named operation.
     * @throws NullPointerException if {@code namespace} or any of its elements is null.
     */
    default NamespaceOperation withNamespaces(final Namespace... namespaces) {
        return new NamespaceOperation(this, namespaces);
    }

    /**
     * Returns a {@link NamedOperation} using this operation as its base.
     * @param name the name that is the target of the named operation.
     * @return a {@link NamedOperation} with this operation as its base and the specified name.
     * @throws IllegalArgumentException if this operation is already a named operation.
     * @throws NullPointerException if {@code name} is null.
     */
    default NamedOperation named(final Object name) {
        return new NamedOperation(this, name);
    }
}
