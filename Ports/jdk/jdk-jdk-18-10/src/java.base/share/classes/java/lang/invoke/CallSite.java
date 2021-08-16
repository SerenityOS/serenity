/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import static java.lang.invoke.MethodHandleStatics.*;
import static java.lang.invoke.MethodHandles.Lookup.IMPL_LOOKUP;

import jdk.internal.vm.annotation.Stable;

/**
 * A {@code CallSite} is a holder for a variable {@link MethodHandle},
 * which is called its {@code target}.
 * An {@code invokedynamic} instruction linked to a {@code CallSite} delegates
 * all calls to the site's current target.
 * A {@code CallSite} may be associated with several {@code invokedynamic}
 * instructions, or it may be "free floating", associated with none.
 * In any case, it may be invoked through an associated method handle
 * called its {@linkplain #dynamicInvoker dynamic invoker}.
 * <p>
 * {@code CallSite} is an abstract class which does not allow
 * direct subclassing by users.  It has three immediate,
 * concrete subclasses that may be either instantiated or subclassed.
 * <ul>
 * <li>If a mutable target is not required, an {@code invokedynamic} instruction
 * may be permanently bound by means of a {@linkplain ConstantCallSite constant call site}.
 * <li>If a mutable target is required which has volatile variable semantics,
 * because updates to the target must be immediately and reliably witnessed by other threads,
 * a {@linkplain VolatileCallSite volatile call site} may be used.
 * <li>Otherwise, if a mutable target is required,
 * a {@linkplain MutableCallSite mutable call site} may be used.
 * </ul>
 * <p>
 * A non-constant call site may be <em>relinked</em> by changing its target.
 * The new target must have the same {@linkplain MethodHandle#type() type}
 * as the previous target.
 * Thus, though a call site can be relinked to a series of
 * successive targets, it cannot change its type.
 * <p>
 * Here is a sample use of call sites and bootstrap methods which links every
 * dynamic call site to print its arguments:
<blockquote><pre>{@code
static void test() throws Throwable {
    // THE FOLLOWING LINE IS PSEUDOCODE FOR A JVM INSTRUCTION
    InvokeDynamic[#bootstrapDynamic].baz("baz arg", 2, 3.14);
}
private static void printArgs(Object... args) {
  System.out.println(java.util.Arrays.deepToString(args));
}
private static final MethodHandle printArgs;
static {
  MethodHandles.Lookup lookup = MethodHandles.lookup();
  Class thisClass = lookup.lookupClass();  // (who am I?)
  printArgs = lookup.findStatic(thisClass,
      "printArgs", MethodType.methodType(void.class, Object[].class));
}
private static CallSite bootstrapDynamic(MethodHandles.Lookup caller, String name, MethodType type) {
  // ignore caller and name, but match the type:
  return new ConstantCallSite(printArgs.asType(type));
}
}</pre></blockquote>
 * @author John Rose, JSR 292 EG
 * @since 1.7
 */
abstract
public class CallSite {

    // The actual payload of this call site.
    // Can be modified using {@link MethodHandleNatives#setCallSiteTargetNormal} or {@link MethodHandleNatives#setCallSiteTargetVolatile}.
    /*package-private*/
    final MethodHandle target;  // Note: This field is known to the JVM.

    /**
     * Make a blank call site object with the given method type.
     * An initial target method is supplied which will throw
     * an {@link IllegalStateException} if called.
     * <p>
     * Before this {@code CallSite} object is returned from a bootstrap method,
     * it is usually provided with a more useful target method,
     * via a call to {@link CallSite#setTarget(MethodHandle) setTarget}.
     * @throws NullPointerException if the proposed type is null
     */
    /*package-private*/
    CallSite(MethodType type) {
        target = makeUninitializedCallSite(type);
    }

    /**
     * Make a call site object equipped with an initial target method handle.
     * @param target the method handle which will be the initial target of the call site
     * @throws NullPointerException if the proposed target is null
     */
    /*package-private*/
    CallSite(MethodHandle target) {
        target.type();  // null check
        this.target = target;
    }

    /**
     * Make a call site object equipped with an initial target method handle.
     * @param targetType the desired type of the call site
     * @param createTargetHook a hook which will bind the call site to the target method handle
     * @throws WrongMethodTypeException if the hook cannot be invoked on the required arguments,
     *         or if the target returned by the hook is not of the given {@code targetType}
     * @throws NullPointerException if the hook returns a null value
     * @throws ClassCastException if the hook returns something other than a {@code MethodHandle}
     * @throws Throwable anything else thrown by the hook function
     */
    /*package-private*/
    CallSite(MethodType targetType, MethodHandle createTargetHook) throws Throwable {
        this(targetType); // need to initialize target to make CallSite.type() work in createTargetHook
        ConstantCallSite selfCCS = (ConstantCallSite) this;
        MethodHandle boundTarget = (MethodHandle) createTargetHook.invokeWithArguments(selfCCS);
        setTargetNormal(boundTarget); // ConstantCallSite doesn't publish CallSite.target
        UNSAFE.storeStoreFence(); // barrier between target and isFrozen updates
    }

    /**
     * {@code CallSite} dependency context.
     * JVM uses CallSite.context to store nmethod dependencies on the call site target.
     */
    private final MethodHandleNatives.CallSiteContext context = MethodHandleNatives.CallSiteContext.make(this);

    /**
     * Returns the type of this call site's target.
     * Although targets may change, any call site's type is permanent, and can never change to an unequal type.
     * The {@code setTarget} method enforces this invariant by refusing any new target that does
     * not have the previous target's type.
     * @return the type of the current target, which is also the type of any future target
     */
    public MethodType type() {
        // warning:  do not call getTarget here, because CCS.getTarget can throw IllegalStateException
        return target.type();
    }

    /**
     * Returns the target method of the call site, according to the
     * behavior defined by this call site's specific class.
     * The immediate subclasses of {@code CallSite} document the
     * class-specific behaviors of this method.
     *
     * @return the current linkage state of the call site, its target method handle
     * @see ConstantCallSite
     * @see VolatileCallSite
     * @see #setTarget
     * @see ConstantCallSite#getTarget
     * @see MutableCallSite#getTarget
     * @see VolatileCallSite#getTarget
     */
    public abstract MethodHandle getTarget();

    /**
     * Updates the target method of this call site, according to the
     * behavior defined by this call site's specific class.
     * The immediate subclasses of {@code CallSite} document the
     * class-specific behaviors of this method.
     * <p>
     * The type of the new target must be {@linkplain MethodType#equals equal to}
     * the type of the old target.
     *
     * @param newTarget the new target
     * @throws NullPointerException if the proposed new target is null
     * @throws WrongMethodTypeException if the proposed new target
     *         has a method type that differs from the previous target
     * @see CallSite#getTarget
     * @see ConstantCallSite#setTarget
     * @see MutableCallSite#setTarget
     * @see VolatileCallSite#setTarget
     */
    public abstract void setTarget(MethodHandle newTarget);

    private void checkTargetChange(MethodHandle newTarget) {
        MethodType oldType = target.type(); // target is always present
        MethodType newType = newTarget.type();  // null check!
        if (newType != oldType) {
            throw wrongTargetType(newTarget, oldType);
        }
    }

    private static WrongMethodTypeException wrongTargetType(MethodHandle target, MethodType type) {
        return new WrongMethodTypeException(String.valueOf(target)+" should be of type "+type);
    }

    /**
     * Produces a method handle equivalent to an invokedynamic instruction
     * which has been linked to this call site.
     * <p>
     * This method is equivalent to the following code:
     * <blockquote><pre>{@code
     * MethodHandle getTarget, invoker, result;
     * getTarget = MethodHandles.publicLookup().bind(this, "getTarget", MethodType.methodType(MethodHandle.class));
     * invoker = MethodHandles.exactInvoker(this.type());
     * result = MethodHandles.foldArguments(invoker, getTarget)
     * }</pre></blockquote>
     *
     * @return a method handle which always invokes this call site's current target
     */
    public abstract MethodHandle dynamicInvoker();

    /*package-private*/
    MethodHandle makeDynamicInvoker() {
        MethodHandle getTarget = getTargetHandle().bindArgumentL(0, this);
        MethodHandle invoker = MethodHandles.exactInvoker(this.type());
        return MethodHandles.foldArguments(invoker, getTarget);
    }

    private static @Stable MethodHandle GET_TARGET;
    private static MethodHandle getTargetHandle() {
        MethodHandle handle = GET_TARGET;
        if (handle != null) {
            return handle;
        }
        try {
            return GET_TARGET = IMPL_LOOKUP.
                    findVirtual(CallSite.class, "getTarget",
                                MethodType.methodType(MethodHandle.class));
        } catch (ReflectiveOperationException e) {
            throw newInternalError(e);
        }
    }

    private static @Stable MethodHandle THROW_UCS;
    private static MethodHandle uninitializedCallSiteHandle() {
        MethodHandle handle = THROW_UCS;
        if (handle != null) {
            return handle;
        }
        try {
            return THROW_UCS = IMPL_LOOKUP.
                findStatic(CallSite.class, "uninitializedCallSite",
                           MethodType.methodType(Object.class, Object[].class));
        } catch (ReflectiveOperationException e) {
            throw newInternalError(e);
        }
    }

    /** This guy is rolled into the default target if a MethodType is supplied to the constructor. */
    private static Object uninitializedCallSite(Object... ignore) {
        throw new IllegalStateException("uninitialized call site");
    }

    private MethodHandle makeUninitializedCallSite(MethodType targetType) {
        MethodType basicType = targetType.basicType();
        MethodHandle invoker = basicType.form().cachedMethodHandle(MethodTypeForm.MH_UNINIT_CS);
        if (invoker == null) {
            invoker = uninitializedCallSiteHandle().asType(basicType);
            invoker = basicType.form().setCachedMethodHandle(MethodTypeForm.MH_UNINIT_CS, invoker);
        }
        // unchecked view is OK since no values will be received or returned
        return invoker.viewAsType(targetType, false);
    }

    // unsafe stuff:
    private static @Stable long TARGET_OFFSET;
    private static long getTargetOffset() {
        long offset = TARGET_OFFSET;
        if (offset > 0) {
            return offset;
        }
        offset = TARGET_OFFSET = UNSAFE.objectFieldOffset(CallSite.class, "target");
        assert(offset > 0);
        return offset;
    }

    /*package-private*/
    final void setTargetNormal(MethodHandle newTarget) {
        checkTargetChange(newTarget);
        MethodHandleNatives.setCallSiteTargetNormal(this, newTarget);
    }

    /*package-private*/
    final MethodHandle getTargetVolatile() {
        return (MethodHandle) UNSAFE.getReferenceVolatile(this, getTargetOffset());
    }

    /*package-private*/
    final void setTargetVolatile(MethodHandle newTarget) {
        checkTargetChange(newTarget);
        MethodHandleNatives.setCallSiteTargetVolatile(this, newTarget);
    }

    // this implements the upcall from the JVM, MethodHandleNatives.linkCallSite:
    /*package-private*/
    static CallSite makeSite(MethodHandle bootstrapMethod,
                             // Callee information:
                             String name, MethodType type,
                             // Extra arguments for BSM, if any:
                             Object info,
                             // Caller information:
                             Class<?> callerClass) {
        CallSite site;
        try {
            Object binding = BootstrapMethodInvoker.invoke(
                    CallSite.class, bootstrapMethod, name, type, info, callerClass);
            if (binding instanceof CallSite) {
                site = (CallSite) binding;
            } else {
                // See the "Linking Exceptions" section for the invokedynamic
                // instruction in JVMS 6.5.
                // Throws a runtime exception defining the cause that is then
                // in the "catch (Throwable ex)" a few lines below wrapped in
                // BootstrapMethodError
                throw new ClassCastException("CallSite bootstrap method failed to produce an instance of CallSite");
            }
            if (!site.getTarget().type().equals(type)) {
                // See the "Linking Exceptions" section for the invokedynamic
                // instruction in JVMS 6.5.
                // Throws a runtime exception defining the cause that is then
                // in the "catch (Throwable ex)" a few lines below wrapped in
                // BootstrapMethodError
                throw wrongTargetType(site.getTarget(), type);
            }
        } catch (Error e) {
            // Pass through an Error, including BootstrapMethodError, any other
            // form of linkage error, such as IllegalAccessError if the bootstrap
            // method is inaccessible, or say ThreadDeath/OutOfMemoryError
            // See the "Linking Exceptions" section for the invokedynamic
            // instruction in JVMS 6.5.
            throw e;
        } catch (Throwable ex) {
            // Wrap anything else in BootstrapMethodError
            throw new BootstrapMethodError("CallSite bootstrap method initialization exception", ex);
        }
        return site;
    }
}
