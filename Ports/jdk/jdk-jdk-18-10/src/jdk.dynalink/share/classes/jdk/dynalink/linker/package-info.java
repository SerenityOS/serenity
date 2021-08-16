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

/**
 * <p>
 * Contains interfaces and classes needed by language runtimes to implement
 * their own language-specific object models and type conversions. The main
 * entry point is the
 * {@link jdk.dynalink.linker.GuardingDynamicLinker} interface. It needs to be
 * implemented in order to provide linking for the runtime's own object model.
 * A language runtime can have more than one guarding dynamic linker
 * implementation. When a runtime is configuring Dynalink for itself, it will
 * normally set these guarding linkers as the prioritized linkers in its
 * {@link jdk.dynalink.DynamicLinkerFactory} (and maybe some of them as fallback
 * linkers, for e.g. handling "method not found" and similar errors in a
 * language-specific manner if no other linker managed to handle the operation.)
 * </p><p>
 * A language runtime that wishes to make at least some of its linkers available
 * to other language runtimes for interoperability will need to use a
 * {@link jdk.dynalink.linker.GuardingDynamicLinkerExporter}.
 * </p><p>
 * Most language runtimes will be able to implement their own linking logic by
 * implementing {@link jdk.dynalink.linker.TypeBasedGuardingDynamicLinker}
 * instead of {@link jdk.dynalink.linker.GuardingDynamicLinker}; it allows for
 * faster type-based linking dispatch.
 * </p><p>
 * Language runtimes that allow type conversions other than those provided by
 * Java will need to have their guarding dynamic linker (or linkers) also
 * implement the {@link jdk.dynalink.linker.GuardingTypeConverterFactory}
 * interface to provide the logic for these conversions.
 * </p>
 * @since 9
 */
package jdk.dynalink.linker;
