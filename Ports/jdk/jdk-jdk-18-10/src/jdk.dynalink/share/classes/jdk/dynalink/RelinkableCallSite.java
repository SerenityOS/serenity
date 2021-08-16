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

package jdk.dynalink;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.support.ChainedCallSite;
import jdk.dynalink.support.SimpleRelinkableCallSite;

/**
 * Interface for call sites managed by a {@link DynamicLinker}. Users of
 * Dynalink must use subclasses of {@link CallSite} that also implement this
 * interface as their call site implementations. There is a readily usable
 * {@link SimpleRelinkableCallSite} subclass that implements monomorphic inline
 * caching strategy as well as {@link ChainedCallSite} that implements a
 * polymorphic inline caching strategy and retains a chain of previously linked
 * method handles. A relinkable call site will be managed by a
 * {@link DynamicLinker} object after being associated with it using its
 * {@link DynamicLinker#link(RelinkableCallSite)} method.
 */
public interface RelinkableCallSite {
    /**
     * Invoked by dynamic linker to initialize the relinkable call site by
     * setting a relink-and-invoke method handle. The call site implementation
     * is supposed to set this method handle as its target using
     * {@link CallSite#setTarget(MethodHandle)}. Relink-and-invoke is the
     * initial method handle set by
     * {@link DynamicLinker#link(RelinkableCallSite)} that will cause the call
     * site to be relinked to an appropriate target on its first invocation
     * based on its arguments, and that linked target will then be invoked
     * (hence the name). This linking protocol effectively delays linking until
     * the call site is invoked with actual arguments and thus ensures that
     * linkers can make nuanced linking decisions based on those arguments and
     * not just on the static method type of the call site.
     * @param relinkAndInvoke a relink-and-invoke method handle supplied by
     * Dynalink.
     */
    public void initialize(MethodHandle relinkAndInvoke);

    /**
     * Returns the descriptor for this call site.
     *
     * @return the descriptor for this call site.
     */
    public CallSiteDescriptor getDescriptor();

    /**
     * This method will be called by the dynamic linker every time the call site
     * is relinked (but see
     * {@link #resetAndRelink(GuardedInvocation, MethodHandle)} for an
     * exception). It will be passed a {@code GuardedInvocation} that the call
     * site should incorporate into its target method handle. When this method
     * is called, the call site is allowed to keep other non-invalidated
     * invocations around for implementation of polymorphic inline caches and
     * compose them with this invocation to form its final target.
     *
     * @param guardedInvocation the guarded invocation that the call site should
     * incorporate into its target method handle.
     * @param relinkAndInvoke a relink-and-invoke method handle. This is a
     * method handle matching the method type of the call site that is supplied
     * by the {@link DynamicLinker} as a callback. It should be used by this
     * call site as the ultimate fallback when it can't invoke its target with
     * the passed arguments. The fallback method is such that when it's invoked,
     * it'll try to obtain an adequate target {@link GuardedInvocation} for the
     * invocation, and subsequently invoke
     * {@link #relink(GuardedInvocation, MethodHandle)} or
     * {@link #resetAndRelink(GuardedInvocation, MethodHandle)}, and finally
     * invoke the target.
     */
    public void relink(GuardedInvocation guardedInvocation, MethodHandle relinkAndInvoke);

    /**
     * This method will be called by the dynamic linker every time the call site
     * is relinked <b>and</b> the linker wishes the call site to throw away any
     * prior linkage state (that is how it differs from
     * {@link #relink(GuardedInvocation, MethodHandle)}). It will be passed a
     * {@code GuardedInvocation} that the call site should use to build its new
     * target method handle. When this method is called, the call site is
     * discouraged from keeping any previous state, and is supposed to only
     * link the current invocation.
     *
     * @param guardedInvocation the guarded invocation that the call site should
     * use to build its target method handle.
     * @param relinkAndInvoke a relink-and-invoke method handle. This is a
     * method handle matching the method type of the call site that is supplied
     * by the {@link DynamicLinker} as a callback. It should be used by this
     * call site as the ultimate fallback when it can't invoke its target with
     * the passed arguments. The fallback method is such that when it's invoked,
     * it'll try to obtain an adequate target {@link GuardedInvocation} for the
     * invocation, and subsequently invoke
     * {@link #relink(GuardedInvocation, MethodHandle)} or
     * {@link #resetAndRelink(GuardedInvocation, MethodHandle)}, and finally
     * invoke the target.
     */
    public void resetAndRelink(GuardedInvocation guardedInvocation, MethodHandle relinkAndInvoke);
}
