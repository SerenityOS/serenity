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
 * Defines the standard dynamic operations. The operations {@link #GET} and {@link #SET} must
 * be used as part of a {@link NamespaceOperation}. {@link NamedOperation} can then be further used on these
 * {@link NamespaceOperation}s to bind the name parameter of {@link #GET} and {@link #SET} operations, in which case it
 * disappears from their type signature.
 * {@link NamedOperation} can also be used to decorate {@link #CALL} and {@link #NEW} operations with a
 * diagnostic name, and as such it does not affect their type signature.
 */
public enum StandardOperation implements Operation {
    /**
     * Get the value from a namespace defined on an object. Call sites with this
     * operation should have a signature of
     * <code>(receiver,&nbsp;name)&rarr;value</code> or
     * <code>(receiver)&rarr;value</code> when used with {@link NamedOperation}, with
     * all parameters and return type being of any type (either primitive or
     * reference). This operation must always be used as part of a {@link NamespaceOperation}.
     */
    GET,
    /**
     * Set the value in a namespace defined on an object. Call sites with this
     * operation should have a signature of
     * <code>(receiver,&nbsp;name,&nbsp;value)&rarr;void</code> or
     * <code>(receiver,&nbsp;value)&rarr;void</code> when used with {@link NamedOperation},
     * with all parameters and return type being of any type (either primitive
     * or reference). This operation must always be used as part of a {@link NamespaceOperation}.
     */
    SET,
    /**
     * Removes the value from a namespace defined on an object. Call sites with this
     * operation should have a signature of
     * <code>(receiver,&nbsp;name)&rarr;void</code> or
     * <code>(receiver)&rarr;void</code> when used with {@link NamedOperation},
     * with all parameters being of any type (either primitive
     * or reference). This operation must always be used as part of a {@link NamespaceOperation}.
     */
    REMOVE,
    /**
     * Call a callable object. Call sites with this operation should have a
     * signature of <code>(callable,&nbsp;receiver,&nbsp;arguments...)&rarr;value</code>,
     * with all parameters and return type being of any type (either primitive or
     * reference). Typically, the callables are presumed to be methods of an object, so
     * an explicit receiver value is always passed to the callable before the arguments.
     * If a callable has no concept of a receiver, it is free to ignore the value of the
     * receiver argument.
     * The {@code CALL} operation is allowed to be used with a
     * {@link NamedOperation} even though it does not take a name. Using it with
     * a named operation won't affect its signature; the name is solely meant to
     * be used as a diagnostic description for error messages.
     */
    CALL,
    /**
     * Call a constructor object. Call sites with this operation should have a
     * signature of <code>(constructor,&nbsp;arguments...)&rarr;value</code>, with all
     * parameters and return type being of any type (either primitive or
     * reference). The {@code NEW} operation is allowed to be used with a
     * {@link NamedOperation} even though it does not take a name. Using it with
     * a named operation won't affect its signature; the name is solely meant to
     * be used as a diagnostic description for error messages.
     */
    NEW
}
