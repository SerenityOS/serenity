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

import java.lang.StackWalker.StackFrame;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.Objects;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardedInvocationTransformer;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.support.Lookup;
import jdk.dynalink.linker.support.SimpleLinkRequest;
import jdk.dynalink.support.ChainedCallSite;
import jdk.dynalink.support.SimpleRelinkableCallSite;

/**
 * The linker for {@link RelinkableCallSite} objects. A dynamic linker is a main
 * objects when using Dynalink, it coordinates linking of call sites with
 * linkers of available language runtimes that are represented by
 * {@link GuardingDynamicLinker} objects (you only need to deal with these if
 * you are yourself implementing a language runtime with its own object model
 * and/or type conversions). To use Dynalink, you have to create one or more
 * dynamic linkers using a {@link DynamicLinkerFactory}. Subsequently, you need
 * to invoke its {@link #link(RelinkableCallSite)} method from
 * {@code invokedynamic} bootstrap methods to let it manage all the call sites
 * they create. Usual usage would be to create at least one class per language
 * runtime to contain one linker instance as:
 * <pre>
 *
 * class MyLanguageRuntime {
 *     private static final GuardingDynamicLinker myLanguageLinker = new MyLanguageLinker();
 *     private static final DynamicLinker dynamicLinker = createDynamicLinker();
 *
 *     private static DynamicLinker createDynamicLinker() {
 *         final DynamicLinkerFactory factory = new DynamicLinkerFactory();
 *         factory.setPrioritizedLinker(myLanguageLinker);
 *         return factory.createLinker();
 *     }
 *
 *     public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type) {
 *         return dynamicLinker.link(
 *             new SimpleRelinkableCallSite(
 *                 new CallSiteDescriptor(lookup, parseOperation(name), type)));
 *     }
 *
 *     private static Operation parseOperation(String name) {
 *         ...
 *     }
 * }
 * </pre>
 * The above setup of one static linker instance is often too simple. You will
 * often have your language runtime have a concept of some kind of
 * "context class loader" and you will want to create one dynamic linker per
 * such class loader, to ensure it incorporates linkers for all other language
 * runtimes visible to that class loader (see
 * {@link DynamicLinkerFactory#setClassLoader(ClassLoader)}).
 * <p>
 * There are three components you need to provide in the above example:
 * <ul>
 *
 * <li>You are expected to provide a {@link GuardingDynamicLinker} for your own
 * language. If your runtime doesn't have its own object model or type
 * conversions, you don't need to implement a {@code GuardingDynamicLinker}; you
 * would simply not invoke the {@code setPrioritizedLinker} method on the factory.</li>
 *
 * <li>The performance of the programs can depend on your choice of the class to
 * represent call sites. The above example used
 * {@link SimpleRelinkableCallSite}, but you might want to use
 * {@link ChainedCallSite} instead. You'll need to experiment and decide what
 * fits your runtime the best. You can further subclass either of these or
 * implement your own.</li>
 *
 * <li>You also need to provide {@link CallSiteDescriptor}s to your call sites.
 * They are immutable objects that contain all the information about the call
 * site: the class performing the lookups, the operation being invoked, and the
 * method signature. You will have to supply your own scheme to encode and
 * decode operations in the call site name or static parameters, that is why
 * in the above example the {@code parseOperation} method is left unimplemented.</li>
 *
 * </ul>
 */
public final class DynamicLinker {
    private static final String CLASS_NAME = DynamicLinker.class.getName();
    private static final String RELINK_METHOD_NAME = "relink";

    private static final String INITIAL_LINK_CLASS_NAME = "java.lang.invoke.MethodHandleNatives";
    private static final String INITIAL_LINK_METHOD_NAME = "linkCallSite";
    private static final String INVOKE_PACKAGE_PREFIX = "java.lang.invoke.";

    private static final StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.SHOW_HIDDEN_FRAMES);

    private final LinkerServices linkerServices;
    private final GuardedInvocationTransformer prelinkTransformer;
    private final boolean syncOnRelink;
    private final int unstableRelinkThreshold;

    /**
     * Creates a new dynamic linker.
     *
     * @param linkerServices the linkerServices used by the linker, created by the factory.
     * @param prelinkTransformer see {@link DynamicLinkerFactory#setPrelinkTransformer(GuardedInvocationTransformer)}
     * @param syncOnRelink see {@link DynamicLinkerFactory#setSyncOnRelink(boolean)}
     * @param unstableRelinkThreshold see {@link DynamicLinkerFactory#setUnstableRelinkThreshold(int)}
     */
    DynamicLinker(final LinkerServices linkerServices, final GuardedInvocationTransformer prelinkTransformer,
            final boolean syncOnRelink, final int unstableRelinkThreshold) {
        if(unstableRelinkThreshold < 0) {
            throw new IllegalArgumentException("unstableRelinkThreshold < 0");
        }
        this.linkerServices = linkerServices;
        this.prelinkTransformer = prelinkTransformer;
        this.syncOnRelink = syncOnRelink;
        this.unstableRelinkThreshold = unstableRelinkThreshold;
    }

    /**
     * Links an invokedynamic call site. It will install a method handle into
     * the call site that invokes the relinking mechanism of this linker. Next
     * time the call site is invoked, it will be linked for the actual arguments
     * it was invoked with.
     *
     * @param <T> the particular subclass of {@link RelinkableCallSite} for
     *        which to create a link.
     * @param callSite the call site to link.
     *
     * @return the callSite, for easy call chaining.
     */
    public <T extends RelinkableCallSite> T link(final T callSite) {
        callSite.initialize(createRelinkAndInvokeMethod(callSite, 0));
        return callSite;
    }

    /**
     * Returns the object representing the linker services of this class that
     * are normally exposed to individual {@link GuardingDynamicLinker
     * language-specific linkers}. While as a user of this class you normally
     * only care about the {@link #link(RelinkableCallSite)} method, in certain
     * circumstances you might want to use the lower level services directly;
     * either to lookup specific method handles, to access the type converters,
     * and so on.
     *
     * @return the object representing the linker services of this class.
     */
    public LinkerServices getLinkerServices() {
        return linkerServices;
    }

    private static final MethodHandle RELINK = Lookup.findOwnSpecial(MethodHandles.lookup(), RELINK_METHOD_NAME,
            MethodHandle.class, RelinkableCallSite.class, int.class, Object[].class);

    private MethodHandle createRelinkAndInvokeMethod(final RelinkableCallSite callSite, final int relinkCount) {
        // Make a bound MH of invoke() for this linker and call site
        final MethodHandle boundRelinker = MethodHandles.insertArguments(RELINK, 0, this, callSite, relinkCount);
        // Make a MH that gathers all arguments to the invocation into an Object[]
        final MethodType type = callSite.getDescriptor().getMethodType();
        final MethodHandle collectingRelinker = boundRelinker.asCollector(Object[].class, type.parameterCount());
        return MethodHandles.foldArguments(MethodHandles.exactInvoker(type), collectingRelinker.asType(
                type.changeReturnType(MethodHandle.class)));
    }

    /**
     * Relinks a call site conforming to the invocation arguments.
     *
     * @param callSite the call site itself
     * @param arguments arguments to the invocation
     *
     * @return return the method handle for the invocation
     *
     * @throws Exception rethrows any exception thrown by the linkers
     */
    @SuppressWarnings("unused")
    private MethodHandle relink(final RelinkableCallSite callSite, final int relinkCount, final Object... arguments) throws Exception {
        final CallSiteDescriptor callSiteDescriptor = callSite.getDescriptor();
        final boolean unstableDetectionEnabled = unstableRelinkThreshold > 0;
        final boolean callSiteUnstable = unstableDetectionEnabled && relinkCount >= unstableRelinkThreshold;
        final LinkRequest linkRequest = new SimpleLinkRequest(callSiteDescriptor, callSiteUnstable, arguments);

        GuardedInvocation guardedInvocation = linkerServices.getGuardedInvocation(linkRequest);

        // None found - throw an exception
        if(guardedInvocation == null) {
            throw new NoSuchDynamicMethodException(callSiteDescriptor.toString());
        }

        // Make sure we transform the invocation before linking it into the call site. This is typically used to match the
        // return type of the invocation to the call site.
        guardedInvocation = prelinkTransformer.filter(guardedInvocation, linkRequest, linkerServices);
        Objects.requireNonNull(guardedInvocation);

        int newRelinkCount = relinkCount;
        // Note that the short-circuited "&&" evaluation below ensures we'll increment the relinkCount until
        // threshold + 1 but not beyond that. Threshold + 1 is treated as a special value to signal that resetAndRelink
        // has already executed once for the unstable call site; we only want the call site to throw away its current
        // linkage once, when it transitions to unstable.
        if(unstableDetectionEnabled && newRelinkCount <= unstableRelinkThreshold && newRelinkCount++ == unstableRelinkThreshold) {
            callSite.resetAndRelink(guardedInvocation, createRelinkAndInvokeMethod(callSite, newRelinkCount));
        } else {
            callSite.relink(guardedInvocation, createRelinkAndInvokeMethod(callSite, newRelinkCount));
        }
        if(syncOnRelink) {
            MutableCallSite.syncAll(new MutableCallSite[] { (MutableCallSite)callSite });
        }
        return guardedInvocation.getInvocation();
    }

    /**
     * Returns a stack trace element describing the location of the
     * {@code invokedynamic} call site currently being linked on the current
     * thread. The operation is potentially expensive as it needs to generate a
     * stack trace to inspect it and is intended for use in diagnostics code.
     * For "free-floating" call sites (not associated with an
     * {@code invokedynamic} instruction), the result is not well-defined.
     *
     * @return a stack trace element describing the location of the call site
     *         currently being linked, or null if it is not invoked while a call
     *         site is being linked.
     */
    public static StackTraceElement getLinkedCallSiteLocation() {
        return stackWalker.walk(s -> s
                // Find one of our linking entry points on the stack...
                .dropWhile(f -> !(isRelinkFrame(f) || isInitialLinkFrame(f)))
                .skip(1)
                // ... then look for the first thing calling it that isn't j.l.invoke
                .dropWhile(f -> f.getClassName().startsWith(INVOKE_PACKAGE_PREFIX))
                .findFirst()
                .map(StackFrame::toStackTraceElement)
                .orElse(null)
        );
    }

    /**
     * Returns {@code true} if the frame represents {@code MethodHandleNatives.linkCallSite()},
     * the frame immediately on top of the call site frame when the call site is
     * being linked for the first time.
     *
     * @param frame the frame
     *
     * @return {@code true} if this frame represents {@code MethodHandleNatives.linkCallSite()}.
     */
    private static boolean isInitialLinkFrame(final StackFrame frame) {
        return testFrame(frame, INITIAL_LINK_METHOD_NAME, INITIAL_LINK_CLASS_NAME);
    }

    /**
     * Returns {@code true} if the frame represents {@code DynamicLinker.relink()},
     * the frame immediately on top of the call site frame when the call site is
     * being relinked (linked for second and subsequent times).
     *
     * @param frame the frame
     *
     * @return {@code true} if this frame represents {@code DynamicLinker.relink()}.
     */
    private static boolean isRelinkFrame(final StackFrame frame) {
        return testFrame(frame, RELINK_METHOD_NAME, CLASS_NAME);
    }

    private static boolean testFrame(final StackFrame frame, final String methodName, final String className) {
        return methodName.equals(frame.getMethodName()) && className.equals(frame.getClassName());
    }
}
