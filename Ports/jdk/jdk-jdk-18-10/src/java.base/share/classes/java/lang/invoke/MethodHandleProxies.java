/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.invoke.WrapperInstance;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import sun.reflect.misc.ReflectUtil;
import static java.lang.invoke.MethodHandleStatics.*;

/**
 * This class consists exclusively of static methods that help adapt
 * method handles to other JVM types, such as interfaces.
 *
 * @since 1.7
 */
public class MethodHandleProxies {

    private MethodHandleProxies() { }  // do not instantiate

    /**
     * Produces an instance of the given single-method interface which redirects
     * its calls to the given method handle.
     * <p>
     * A single-method interface is an interface which declares a uniquely named method.
     * When determining the uniquely named method of a single-method interface,
     * the public {@code Object} methods ({@code toString}, {@code equals}, {@code hashCode})
     * are disregarded as are any default (non-abstract) methods.
     * For example, {@link java.util.Comparator} is a single-method interface,
     * even though it re-declares the {@code Object.equals} method and also
     * declares default methods, such as {@code Comparator.reverse}.
     * <p>
     * The interface must be public and not {@linkplain Class#isSealed() sealed}.
     * No additional access checks are performed.
     * <p>
     * The resulting instance of the required type will respond to
     * invocation of the type's uniquely named method by calling
     * the given target on the incoming arguments,
     * and returning or throwing whatever the target
     * returns or throws.  The invocation will be as if by
     * {@code target.invoke}.
     * The target's type will be checked before the
     * instance is created, as if by a call to {@code asType},
     * which may result in a {@code WrongMethodTypeException}.
     * <p>
     * The uniquely named method is allowed to be multiply declared,
     * with distinct type descriptors.  (E.g., it can be overloaded,
     * or can possess bridge methods.)  All such declarations are
     * connected directly to the target method handle.
     * Argument and return types are adjusted by {@code asType}
     * for each individual declaration.
     * <p>
     * The wrapper instance will implement the requested interface
     * and its super-types, but no other single-method interfaces.
     * This means that the instance will not unexpectedly
     * pass an {@code instanceof} test for any unrequested type.
     * <p style="font-size:smaller;">
     * <em>Implementation Note:</em>
     * Therefore, each instance must implement a unique single-method interface.
     * Implementations may not bundle together
     * multiple single-method interfaces onto single implementation classes
     * in the style of {@link java.awt.AWTEventMulticaster}.
     * <p>
     * The method handle may throw an <em>undeclared exception</em>,
     * which means any checked exception (or other checked throwable)
     * not declared by the requested type's single abstract method.
     * If this happens, the throwable will be wrapped in an instance of
     * {@link java.lang.reflect.UndeclaredThrowableException UndeclaredThrowableException}
     * and thrown in that wrapped form.
     * <p>
     * Like {@link java.lang.Integer#valueOf Integer.valueOf},
     * {@code asInterfaceInstance} is a factory method whose results are defined
     * by their behavior.
     * It is not guaranteed to return a new instance for every call.
     * <p>
     * Because of the possibility of {@linkplain java.lang.reflect.Method#isBridge bridge methods}
     * and other corner cases, the interface may also have several abstract methods
     * with the same name but having distinct descriptors (types of returns and parameters).
     * In this case, all the methods are bound in common to the one given target.
     * The type check and effective {@code asType} conversion is applied to each
     * method type descriptor, and all abstract methods are bound to the target in common.
     * Beyond this type check, no further checks are made to determine that the
     * abstract methods are related in any way.
     * <p>
     * Future versions of this API may accept additional types,
     * such as abstract classes with single abstract methods.
     * Future versions of this API may also equip wrapper instances
     * with one or more additional public "marker" interfaces.
     * <p>
     * If a security manager is installed, this method is caller sensitive.
     * During any invocation of the target method handle via the returned wrapper,
     * the original creator of the wrapper (the caller) will be visible
     * to context checks requested by the security manager.
     *
     * @param <T> the desired type of the wrapper, a single-method interface
     * @param intfc a class object representing {@code T}
     * @param target the method handle to invoke from the wrapper
     * @return a correctly-typed wrapper for the given target
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if the {@code intfc} is not a
     *         valid argument to this method
     * @throws WrongMethodTypeException if the target cannot
     *         be converted to the type required by the requested interface
     */
    // Other notes to implementors:
    // <p>
    // No stable mapping is promised between the single-method interface and
    // the implementation class C.  Over time, several implementation
    // classes might be used for the same type.
    // <p>
    // If the implementation is able
    // to prove that a wrapper of the required type
    // has already been created for a given
    // method handle, or for another method handle with the
    // same behavior, the implementation may return that wrapper in place of
    // a new wrapper.
    // <p>
    // This method is designed to apply to common use cases
    // where a single method handle must interoperate with
    // an interface that implements a function-like
    // API.  Additional variations, such as single-abstract-method classes with
    // private constructors, or interfaces with multiple but related
    // entry points, must be covered by hand-written or automatically
    // generated adapter classes.
    //
    @SuppressWarnings("removal")
    @CallerSensitive
    public static <T> T asInterfaceInstance(final Class<T> intfc, final MethodHandle target) {
        if (!intfc.isInterface() || !Modifier.isPublic(intfc.getModifiers()))
            throw newIllegalArgumentException("not a public interface", intfc.getName());
        if (intfc.isSealed())
            throw newIllegalArgumentException("a sealed interface", intfc.getName());
        final MethodHandle mh;
        if (System.getSecurityManager() != null) {
            final Class<?> caller = Reflection.getCallerClass();
            final ClassLoader ccl = caller != null ? caller.getClassLoader() : null;
            ReflectUtil.checkProxyPackageAccess(ccl, intfc);
            mh = ccl != null ? bindCaller(target, caller) : target;
        } else {
            mh = target;
        }
        ClassLoader proxyLoader = intfc.getClassLoader();
        if (proxyLoader == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader(); // avoid use of BCP
            proxyLoader = cl != null ? cl : ClassLoader.getSystemClassLoader();
        }
        final Method[] methods = getSingleNameMethods(intfc);
        if (methods == null)
            throw newIllegalArgumentException("not a single-method interface", intfc.getName());
        final MethodHandle[] vaTargets = new MethodHandle[methods.length];
        for (int i = 0; i < methods.length; i++) {
            Method sm = methods[i];
            MethodType smMT = MethodType.methodType(sm.getReturnType(), sm.getParameterTypes());
            MethodHandle checkTarget = mh.asType(smMT);  // make throw WMT
            checkTarget = checkTarget.asType(checkTarget.type().changeReturnType(Object.class));
            vaTargets[i] = checkTarget.asSpreader(Object[].class, smMT.parameterCount());
        }
        final ConcurrentHashMap<Method, MethodHandle> defaultMethodMap =
                hasDefaultMethods(intfc) ? new ConcurrentHashMap<>() : null;
        final InvocationHandler ih = new InvocationHandler() {
                private Object getArg(String name) {
                    if ((Object)name == "getWrapperInstanceTarget")  return target;
                    if ((Object)name == "getWrapperInstanceType")    return intfc;
                    throw new AssertionError();
                }
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    for (int i = 0; i < methods.length; i++) {
                        if (method.equals(methods[i]))
                            return vaTargets[i].invokeExact(args);
                    }
                    if (method.getDeclaringClass() == WrapperInstance.class)
                        return getArg(method.getName());
                    if (isObjectMethod(method))
                        return callObjectMethod(proxy, method, args);
                    if (isDefaultMethod(method)) {
                        return callDefaultMethod(defaultMethodMap, proxy, intfc, method, args);
                    }
                    throw newInternalError("bad proxy method: "+method);
                }
            };

        final Object proxy;
        if (System.getSecurityManager() != null) {
            // sun.invoke.WrapperInstance is a restricted interface not accessible
            // by any non-null class loader.
            final ClassLoader loader = proxyLoader;
            proxy = AccessController.doPrivileged(new PrivilegedAction<>() {
                public Object run() {
                    return Proxy.newProxyInstance(
                            loader,
                            new Class<?>[]{ intfc, WrapperInstance.class },
                            ih);
                }
            });
        } else {
            proxy = Proxy.newProxyInstance(proxyLoader,
                                           new Class<?>[]{ intfc, WrapperInstance.class },
                                           ih);
        }
        return intfc.cast(proxy);
    }

    private static MethodHandle bindCaller(MethodHandle target, Class<?> hostClass) {
        return MethodHandleImpl.bindCaller(target, hostClass).withVarargs(target.isVarargsCollector());
    }

    /**
     * Determines if the given object was produced by a call to {@link #asInterfaceInstance asInterfaceInstance}.
     * @param x any reference
     * @return true if the reference is not null and points to an object produced by {@code asInterfaceInstance}
     */
    public static boolean isWrapperInstance(Object x) {
        return x instanceof WrapperInstance;
    }

    private static WrapperInstance asWrapperInstance(Object x) {
        try {
            if (x != null)
                return (WrapperInstance) x;
        } catch (ClassCastException ex) {
        }
        throw newIllegalArgumentException("not a wrapper instance");
    }

    /**
     * Produces or recovers a target method handle which is behaviorally
     * equivalent to the unique method of this wrapper instance.
     * The object {@code x} must have been produced by a call to {@link #asInterfaceInstance asInterfaceInstance}.
     * This requirement may be tested via {@link #isWrapperInstance isWrapperInstance}.
     * @param x any reference
     * @return a method handle implementing the unique method
     * @throws IllegalArgumentException if the reference x is not to a wrapper instance
     */
    public static MethodHandle wrapperInstanceTarget(Object x) {
        return asWrapperInstance(x).getWrapperInstanceTarget();
    }

    /**
     * Recovers the unique single-method interface type for which this wrapper instance was created.
     * The object {@code x} must have been produced by a call to {@link #asInterfaceInstance asInterfaceInstance}.
     * This requirement may be tested via {@link #isWrapperInstance isWrapperInstance}.
     * @param x any reference
     * @return the single-method interface type for which the wrapper was created
     * @throws IllegalArgumentException if the reference x is not to a wrapper instance
     */
    public static Class<?> wrapperInstanceType(Object x) {
        return asWrapperInstance(x).getWrapperInstanceType();
    }

    private static boolean isObjectMethod(Method m) {
        return switch (m.getName()) {
            case "toString" -> m.getReturnType() == String.class
                               && m.getParameterCount() == 0;
            case "hashCode" -> m.getReturnType() == int.class
                               && m.getParameterCount() == 0;
            case "equals"   -> m.getReturnType() == boolean.class
                               && m.getParameterCount() == 1
                               && m.getParameterTypes()[0] == Object.class;
            default -> false;
        };
    }

    private static Object callObjectMethod(Object self, Method m, Object[] args) {
        assert(isObjectMethod(m)) : m;
        return switch (m.getName()) {
            case "toString" -> self.getClass().getName() + "@" + Integer.toHexString(self.hashCode());
            case "hashCode" -> System.identityHashCode(self);
            case "equals"   -> (self == args[0]);
            default -> null;
        };
    }

    private static Method[] getSingleNameMethods(Class<?> intfc) {
        ArrayList<Method> methods = new ArrayList<>();
        String uniqueName = null;
        for (Method m : intfc.getMethods()) {
            if (isObjectMethod(m))  continue;
            if (!Modifier.isAbstract(m.getModifiers()))  continue;
            String mname = m.getName();
            if (uniqueName == null)
                uniqueName = mname;
            else if (!uniqueName.equals(mname))
                return null;  // too many abstract methods
            methods.add(m);
        }
        if (uniqueName == null)  return null;
        return methods.toArray(new Method[methods.size()]);
    }

    private static boolean isDefaultMethod(Method m) {
        return !Modifier.isAbstract(m.getModifiers());
    }

    private static boolean hasDefaultMethods(Class<?> intfc) {
        for (Method m : intfc.getMethods()) {
            if (!isObjectMethod(m) &&
                !Modifier.isAbstract(m.getModifiers())) {
                return true;
            }
        }
        return false;
    }

    private static Object callDefaultMethod(ConcurrentHashMap<Method, MethodHandle> defaultMethodMap,
                             Object self, Class<?> intfc, Method m, Object[] args) throws Throwable {
        assert(isDefaultMethod(m) && !isObjectMethod(m)) : m;

        // Lazily compute the associated method handle from the method
        MethodHandle dmh = defaultMethodMap.computeIfAbsent(m, mk -> {
            try {
                // Look up the default method for special invocation thereby
                // avoiding recursive invocation back to the proxy
                MethodHandle mh = MethodHandles.Lookup.IMPL_LOOKUP.findSpecial(
                        intfc, mk.getName(),
                        MethodType.methodType(mk.getReturnType(), mk.getParameterTypes()),
                        self.getClass());
                return mh.asSpreader(Object[].class, mk.getParameterCount());
            } catch (NoSuchMethodException | IllegalAccessException e) {
                // The method is known to exist and should be accessible, this
                // method would not be called unless the invokeinterface to the
                // default (public) method passed access control checks
                throw new InternalError(e);
            }
        });
        return dmh.invoke(self, args);
    }
}
