/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import sun.invoke.util.Wrapper;

import java.lang.invoke.AbstractConstantGroup.BSCIWithCache;
import java.util.Arrays;

import static java.lang.invoke.BootstrapCallInfo.makeBootstrapCallInfo;
import static java.lang.invoke.ConstantGroup.makeConstantGroup;
import static java.lang.invoke.MethodHandleNatives.*;
import static java.lang.invoke.MethodHandleStatics.TRACE_METHOD_LINKAGE;
import static java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.Lookup.IMPL_LOOKUP;

final class BootstrapMethodInvoker {

    /**
     * Factored code for invoking a bootstrap method for invokedynamic
     * or a dynamic constant.
     * @param resultType the expected return type (either CallSite or a constant type)
     * @param bootstrapMethod the BSM to call
     * @param name the method name or constant name
     * @param type the method type or constant type
     * @param info information passed up from the JVM, to derive static arguments
     * @param callerClass the class containing the resolved method call or constant load
     * @param <T> the expected return type
     * @return the expected value, either a CallSite or a constant value
     */
    static <T> T invoke(Class<T> resultType,
                        MethodHandle bootstrapMethod,
                        // Callee information:
                        String name, Object type,
                        // Extra arguments for BSM, if any:
                        Object info,
                        // Caller information:
                        Class<?> callerClass) {
        MethodHandles.Lookup caller = IMPL_LOOKUP.in(callerClass);
        Object result;
        boolean pullMode = isPullModeBSM(bootstrapMethod);  // default value is false
        boolean vmIsPushing = !staticArgumentsPulled(info); // default value is true
        MethodHandle pullModeBSM;
        // match the VM with the BSM
        if (vmIsPushing) {
            // VM is pushing arguments at us
            pullModeBSM = null;
            if (pullMode) {
                bootstrapMethod = pushMePullYou(bootstrapMethod, true);
            }
        } else {
            // VM wants us to pull args from it
            pullModeBSM = pullMode ? bootstrapMethod :
                    pushMePullYou(bootstrapMethod, false);
            bootstrapMethod = null;
        }
        try {
            // As an optimization we special case various known BSMs,
            // such as LambdaMetafactory::metafactory and
            // StringConcatFactory::makeConcatWithConstants.
            //
            // By providing static type information or even invoking
            // exactly, we avoid emitting code to perform runtime
            // checking.
            if (info == null) {
                // VM is allowed to pass up a null meaning no BSM args
                if (type instanceof Class<?> c) {
                    result = bootstrapMethod.invoke(caller, name, c);
                } else {
                    result = bootstrapMethod.invoke(caller, name, (MethodType)type);
                }
            }
            else if (!info.getClass().isArray()) {
                // VM is allowed to pass up a single BSM arg directly

                // Call to StringConcatFactory::makeConcatWithConstants
                // with empty constant arguments?
                if (isStringConcatFactoryBSM(bootstrapMethod.type())) {
                    result = (CallSite)bootstrapMethod
                            .invokeExact(caller, name, (MethodType)type,
                                         (String)info, new Object[0]);
                } else {
                    info = maybeReBox(info);
                    if (type instanceof Class<?> c) {
                        result = bootstrapMethod.invoke(caller, name, c, info);
                    } else {
                        result = bootstrapMethod.invoke(caller, name, (MethodType)type, info);
                    }
                }
            }
            else if (info.getClass() == int[].class) {
                // VM is allowed to pass up a pair {argc, index}
                // referring to 'argc' BSM args at some place 'index'
                // in the guts of the VM (associated with callerClass).
                // The format of this index pair is private to the
                // handshake between the VM and this class only.
                // This supports "pulling" of arguments.
                // The VM is allowed to do this for any reason.
                // The code in this method makes up for any mismatches.
                BootstrapCallInfo<Object> bsci
                    = new VM_BSCI<>(bootstrapMethod, name, type, caller, (int[])info);
                // Pull-mode API is (Lookup, BootstrapCallInfo) -> Object
                result = pullModeBSM.invoke(caller, bsci);
            }
            else {
                // VM is allowed to pass up a full array of resolved BSM args
                Object[] argv = (Object[]) info;

                MethodType bsmType = bootstrapMethod.type();
                if (isLambdaMetafactoryIndyBSM(bsmType) && argv.length == 3) {
                    result = (CallSite)bootstrapMethod
                            .invokeExact(caller, name, (MethodType)type, (MethodType)argv[0],
                                    (MethodHandle)argv[1], (MethodType)argv[2]);
                } else if (isLambdaMetafactoryCondyBSM(bsmType) && argv.length == 3) {
                    result = bootstrapMethod
                            .invokeExact(caller, name, (Class<?>)type, (MethodType)argv[0],
                                    (MethodHandle)argv[1], (MethodType)argv[2]);
                } else if (isStringConcatFactoryBSM(bsmType) && argv.length >= 1) {
                    String recipe = (String)argv[0];
                    Object[] shiftedArgs = Arrays.copyOfRange(argv, 1, argv.length);
                    maybeReBoxElements(shiftedArgs);
                    result = (CallSite)bootstrapMethod.invokeExact(caller, name, (MethodType)type, recipe, shiftedArgs);
                } else if (isLambdaMetafactoryAltMetafactoryBSM(bsmType)) {
                    maybeReBoxElements(argv);
                    result = (CallSite)bootstrapMethod.invokeExact(caller, name, (MethodType)type, argv);
                } else {
                    maybeReBoxElements(argv);
                    if (type instanceof Class<?> c) {
                        result = switch (argv.length) {
                            case 0 -> bootstrapMethod.invoke(caller, name, c);
                            case 1 -> bootstrapMethod.invoke(caller, name, c, argv[0]);
                            case 2 -> bootstrapMethod.invoke(caller, name, c, argv[0], argv[1]);
                            case 3 -> bootstrapMethod.invoke(caller, name, c, argv[0], argv[1], argv[2]);
                            case 4 -> bootstrapMethod.invoke(caller, name, c, argv[0], argv[1], argv[2], argv[3]);
                            case 5 -> bootstrapMethod.invoke(caller, name, c, argv[0], argv[1], argv[2], argv[3], argv[4]);
                            case 6 -> bootstrapMethod.invoke(caller, name, c, argv[0], argv[1], argv[2], argv[3], argv[4], argv[5]);
                            default -> invokeWithManyArguments(bootstrapMethod, caller, name, type, argv);
                        };
                    } else {
                        MethodType mt = (MethodType) type;
                        result = switch (argv.length) {
                            case 0 -> bootstrapMethod.invoke(caller, name, mt);
                            case 1 -> bootstrapMethod.invoke(caller, name, mt, argv[0]);
                            case 2 -> bootstrapMethod.invoke(caller, name, mt, argv[0], argv[1]);
                            case 3 -> bootstrapMethod.invoke(caller, name, mt, argv[0], argv[1], argv[2]);
                            case 4 -> bootstrapMethod.invoke(caller, name, mt, argv[0], argv[1], argv[2], argv[3]);
                            case 5 -> bootstrapMethod.invoke(caller, name, mt, argv[0], argv[1], argv[2], argv[3], argv[4]);
                            case 6 -> bootstrapMethod.invoke(caller, name, mt, argv[0], argv[1], argv[2], argv[3], argv[4], argv[5]);
                            default -> invokeWithManyArguments(bootstrapMethod, caller, name, type, argv);
                        };
                    }
                }
            }
            return widenAndCast(result, resultType);
        }
        catch (Error e) {
            // Pass through an Error, including BootstrapMethodError, any other
            // form of linkage error, such as IllegalAccessError if the bootstrap
            // method is inaccessible, or say ThreadDeath/OutOfMemoryError
            // See the "Linking Exceptions" section for the invokedynamic
            // instruction in JVMS 6.5.
            throw e;
        }
        catch (Throwable ex) {
            // Wrap anything else in BootstrapMethodError
            throw new BootstrapMethodError("bootstrap method initialization exception", ex);
        }
    }


    /**
     * If resultType is a reference type, do Class::cast on the result through
     * an identity function of that type, as-type converted to return
     * the corresponding reference wrapper type for resultType.
     * Works like {@code MethodHandles.identity(resultType).invoke((Object)result)}.
     *
     * This utility function enforces type correctness of bootstrap method results.
     * It is also used to enforce type correctness in other dependently-typed
     * methods, such as classData.
     */
    static <T> T widenAndCast(Object result, Class<T> resultType) throws Throwable {
        if (!resultType.isPrimitive()) {
            return resultType.cast(result);
        }

        Class<T> wrapperType = Wrapper.asWrapperType(resultType);
        if (wrapperType.isInstance(result)) {
            @SuppressWarnings("unchecked")
            T wrapper = (T) result;
            return wrapper;
        }
        // Non-reference conversions are more than just plain casts.
        // By pushing the value through a funnel of the form (T x)->x,
        // the boxed result can be widened as needed.  See MH::asType.
        // Note that this might widen byte into int, float into double, etc
        MethodHandle funnel = MethodHandles.identity(resultType);
        result = funnel.invoke(result);
        // Now it is the wrapper type for resultType.
        return wrapperType.cast(result);
    }

    private static Object invokeWithManyArguments(MethodHandle bootstrapMethod, Lookup caller,
                                                  String name, Object type, Object[] argv) throws Throwable {
        final int NON_SPREAD_ARG_COUNT = 3;  // (caller, name, type)
        final int MAX_SAFE_SIZE = MethodType.MAX_MH_ARITY / 2 - NON_SPREAD_ARG_COUNT;
        if (argv.length >= MAX_SAFE_SIZE) {
            // to be on the safe side, use invokeWithArguments which handles jumbo lists
            Object[] newargv = new Object[NON_SPREAD_ARG_COUNT + argv.length];
            newargv[0] = caller;
            newargv[1] = name;
            newargv[2] = type;
            System.arraycopy(argv, 0, newargv, NON_SPREAD_ARG_COUNT, argv.length);
            return bootstrapMethod.invokeWithArguments(newargv);
        } else {
            MethodType invocationType = MethodType.genericMethodType(NON_SPREAD_ARG_COUNT + argv.length);
            MethodHandle typedBSM = bootstrapMethod.asType(invocationType);
            MethodHandle spreader = invocationType.invokers().spreadInvoker(NON_SPREAD_ARG_COUNT);
            return spreader.invokeExact(typedBSM, (Object) caller, (Object) name, type, argv);
        }
    }

    private static final MethodType LMF_INDY_MT = MethodType.methodType(CallSite.class,
            Lookup.class, String.class, MethodType.class, MethodType.class, MethodHandle.class, MethodType.class);

    private static final MethodType LMF_ALT_MT = MethodType.methodType(CallSite.class,
            Lookup.class, String.class, MethodType.class, Object[].class);

    private static final MethodType LMF_CONDY_MT = MethodType.methodType(Object.class,
            Lookup.class, String.class, Class.class, MethodType.class, MethodHandle.class, MethodType.class);

    private static final MethodType SCF_MT = MethodType.methodType(CallSite.class,
            Lookup.class, String.class, MethodType.class, String.class, Object[].class);

    /**
     * @return true iff the BSM method type exactly matches
     *         {@see java.lang.invoke.StringConcatFactory#makeConcatWithConstants(MethodHandles.Lookup,
     *                 String,MethodType,String,Object...))}
     */
    private static boolean isStringConcatFactoryBSM(MethodType bsmType) {
        return bsmType == SCF_MT;
    }

    /**
     * @return true iff the BSM method type exactly matches
     *         {@see java.lang.invoke.LambdaMetafactory#metafactory(
     *          MethodHandles.Lookup,String,Class,MethodType,MethodHandle,MethodType)}
     */
    private static boolean isLambdaMetafactoryCondyBSM(MethodType bsmType) {
        return bsmType == LMF_CONDY_MT;
    }

    /**
     * @return true iff the BSM method type exactly matches
     *         {@see java.lang.invoke.LambdaMetafactory#metafactory(
     *          MethodHandles.Lookup,String,MethodType,MethodType,MethodHandle,MethodType)}
     */
    private static boolean isLambdaMetafactoryIndyBSM(MethodType bsmType) {
        return bsmType == LMF_INDY_MT;
    }

    /**
     * @return true iff the BSM method type exactly matches
     *         {@see java.lang.invoke.LambdaMetafactory#altMetafactory(
     *          MethodHandles.Lookup,String,MethodType,Object[])}
     */
    private static boolean isLambdaMetafactoryAltMetafactoryBSM(MethodType bsmType) {
        return bsmType == LMF_ALT_MT;
    }

    /** The JVM produces java.lang.Integer values to box
     *  CONSTANT_Integer boxes but does not intern them.
     *  Let's intern them.  This is slightly wrong for
     *  a {@code CONSTANT_Dynamic} which produces an
     *  un-interned integer (e.g., {@code new Integer(0)}).
     */
    private static Object maybeReBox(Object x) {
        if (x instanceof Integer) {
            int xi = (int) x;
            if (xi == (byte) xi)
                x = xi;  // must rebox; see JLS 5.1.7
        }
        return x;
    }

    private static void maybeReBoxElements(Object[] xa) {
        for (int i = 0; i < xa.length; i++) {
            xa[i] = maybeReBox(xa[i]);
        }
    }

    /** Canonical VM-aware implementation of BootstrapCallInfo.
     * Knows how to dig into the JVM for lazily resolved (pull-mode) constants.
     */
    private static final class VM_BSCI<T> extends BSCIWithCache<T> {
        private final int[] indexInfo;
        private final Class<?> caller;  // for index resolution only

        VM_BSCI(MethodHandle bsm, String name, T type,
                Lookup lookup, int[] indexInfo) {
            super(bsm, name, type, indexInfo[0]);
            if (!lookup.hasFullPrivilegeAccess())  //D.I.D.
                throw new AssertionError("bad Lookup object");
            this.caller = lookup.lookupClass();
            this.indexInfo = indexInfo;
            // scoop up all the easy stuff right away:
            prefetchIntoCache(0, size());
        }

        @Override Object fillCache(int i) {
            Object[] buf = { null };
            copyConstants(i, i+1, buf, 0);
            Object res = wrapNull(buf[0]);
            cache[i] = res;
            int next = i + 1;
            if (next < cache.length && cache[next] == null)
                maybePrefetchIntoCache(next, false);  // try to prefetch
            return res;
        }

        @Override public int copyConstants(int start, int end,
                                           Object[] buf, int pos) {
            int i = start, bufi = pos;
            while (i < end) {
                Object x = cache[i];
                if (x == null)  break;
                buf[bufi++] = unwrapNull(x);
                i++;
            }
            // give up at first null and grab the rest in one big block
            if (i >= end)  return i;
            Object[] temp = new Object[end - i];
            if (TRACE_METHOD_LINKAGE) {
                System.out.println("resolving more BSM arguments: " +
                        Arrays.asList(caller.getSimpleName(), Arrays.toString(indexInfo), i, end));
            }
            copyOutBootstrapArguments(caller, indexInfo,
                                      i, end, temp, 0,
                                      true, null);
            for (Object x : temp) {
                x = maybeReBox(x);
                buf[bufi++] = x;
                cache[i++] = wrapNull(x);
            }
            if (end < cache.length && cache[end] == null)
                maybePrefetchIntoCache(end, true);  // try to prefetch
            return i;
        }

        private static final int MIN_PF = 4;
        private void maybePrefetchIntoCache(int i, boolean bulk) {
            int len = cache.length;
            assert(0 <= i && i <= len);
            int pfLimit = i;
            if (bulk)  pfLimit += i;  // exponential prefetch expansion
            // try to prefetch at least MIN_PF elements
            if (pfLimit < i + MIN_PF)  pfLimit = i + MIN_PF;
            if (pfLimit > len || pfLimit < 0)  pfLimit = len;
            // stop prefetching where cache is more full than empty
            int empty = 0, nonEmpty = 0, lastEmpty = i;
            for (int j = i; j < pfLimit; j++) {
                if (cache[j] == null) {
                    empty++;
                    lastEmpty = j;
                } else {
                    nonEmpty++;
                    if (nonEmpty > empty) {
                        pfLimit = lastEmpty + 1;
                        break;
                    }
                    if (pfLimit < len)  pfLimit++;
                }
            }
            if (bulk && empty < MIN_PF && pfLimit < len)
                return;  // not worth the effort
            prefetchIntoCache(i, pfLimit);
        }

        private void prefetchIntoCache(int i, int pfLimit) {
            if (pfLimit <= i)  return;  // corner case
            Object[] temp = new Object[pfLimit - i];
            if (TRACE_METHOD_LINKAGE) {
                System.out.println("prefetching BSM arguments: " +
                        Arrays.asList(caller.getSimpleName(), Arrays.toString(indexInfo), i, pfLimit));
            }
            copyOutBootstrapArguments(caller, indexInfo,
                                      i, pfLimit, temp, 0,
                                      false, NOT_PRESENT);
            for (Object x : temp) {
                if (x != NOT_PRESENT && cache[i] == null) {
                    cache[i] = wrapNull(maybeReBox(x));
                }
                i++;
            }
        }
    }

    /*non-public*/
    static final class PushAdapter {
        // skeleton for push-mode BSM which wraps a pull-mode BSM:
        static Object pushToBootstrapMethod(MethodHandle pullModeBSM,
                                            MethodHandles.Lookup lookup, String name, Object type,
                                            Object... arguments) throws Throwable {
            ConstantGroup cons = makeConstantGroup(Arrays.asList(arguments));
            BootstrapCallInfo<?> bsci = makeBootstrapCallInfo(pullModeBSM, name, type, cons);
            if (TRACE_METHOD_LINKAGE)
                System.out.println("pull-mode BSM gets pushed arguments from fake BSCI");
            return pullModeBSM.invoke(lookup, bsci);
        }

        static final MethodHandle MH_pushToBootstrapMethod;
        static {
            final Class<?> THIS_CLASS = PushAdapter.class;
            try {
                MH_pushToBootstrapMethod = IMPL_LOOKUP
                    .findStatic(THIS_CLASS, "pushToBootstrapMethod",
                                MethodType.methodType(Object.class, MethodHandle.class,
                                        Lookup.class, String.class, Object.class, Object[].class));
            } catch (Throwable ex) {
                throw new InternalError(ex);
            }
        }
    }

    /*non-public*/
    static final class PullAdapter {
        // skeleton for pull-mode BSM which wraps a push-mode BSM:
        static Object pullFromBootstrapMethod(MethodHandle pushModeBSM,
                                              MethodHandles.Lookup lookup,
                                              BootstrapCallInfo<?> bsci)
                throws Throwable {
            int argc = bsci.size();
            switch (argc) {
                case 0:
                    return pushModeBSM.invoke(lookup, bsci.invocationName(), bsci.invocationType());
                case 1:
                    return pushModeBSM.invoke(lookup, bsci.invocationName(), bsci.invocationType(),
                            bsci.get(0));
                case 2:
                    return pushModeBSM.invoke(lookup, bsci.invocationName(), bsci.invocationType(),
                            bsci.get(0), bsci.get(1));
                case 3:
                    return pushModeBSM.invoke(lookup, bsci.invocationName(), bsci.invocationType(),
                            bsci.get(0), bsci.get(1), bsci.get(2));
                case 4:
                    return pushModeBSM.invoke(lookup, bsci.invocationName(), bsci.invocationType(),
                            bsci.get(0), bsci.get(1), bsci.get(2), bsci.get(3));
                case 5:
                    return pushModeBSM.invoke(lookup, bsci.invocationName(), bsci.invocationType(),
                            bsci.get(0), bsci.get(1), bsci.get(2), bsci.get(3), bsci.get(4));
                case 6:
                    return pushModeBSM.invoke(lookup, bsci.invocationName(), bsci.invocationType(),
                            bsci.get(0), bsci.get(1), bsci.get(2), bsci.get(3), bsci.get(4), bsci.get(5));
                default:
                    final int NON_SPREAD_ARG_COUNT = 3;  // (lookup, name, type)
                    final int MAX_SAFE_SIZE = MethodType.MAX_MH_ARITY / 2 - NON_SPREAD_ARG_COUNT;
                    if (argc >= MAX_SAFE_SIZE) {
                        // to be on the safe side, use invokeWithArguments which handles jumbo lists
                        Object[] newargv = new Object[NON_SPREAD_ARG_COUNT + argc];
                        newargv[0] = lookup;
                        newargv[1] = bsci.invocationName();
                        newargv[2] = bsci.invocationType();
                        bsci.copyConstants(0, argc, newargv, NON_SPREAD_ARG_COUNT);
                        return pushModeBSM.invokeWithArguments(newargv);
                    }
                    MethodType invocationType = MethodType.genericMethodType(NON_SPREAD_ARG_COUNT + argc);
                    MethodHandle typedBSM = pushModeBSM.asType(invocationType);
                    MethodHandle spreader = invocationType.invokers().spreadInvoker(NON_SPREAD_ARG_COUNT);
                    Object[] argv = new Object[argc];
                    bsci.copyConstants(0, argc, argv, 0);
                    return spreader.invokeExact(typedBSM, (Object) lookup, (Object) bsci.invocationName(), bsci.invocationType(), argv);
                }
        }

        static final MethodHandle MH_pullFromBootstrapMethod;

        static {
            final Class<?> THIS_CLASS = PullAdapter.class;
            try {
                MH_pullFromBootstrapMethod = IMPL_LOOKUP
                    .findStatic(THIS_CLASS, "pullFromBootstrapMethod",
                                MethodType.methodType(Object.class, MethodHandle.class,
                                        Lookup.class, BootstrapCallInfo.class));
            } catch (Throwable ex) {
                throw new InternalError(ex);
            }
        }
    }

    /** Given a push-mode BSM (taking one argument) convert it to a
     *  pull-mode BSM (taking N pre-resolved arguments).
     *  This method is used when, in fact, the JVM is passing up
     *  pre-resolved arguments, but the BSM is expecting lazy stuff.
     *  Or, when goToPushMode is true, do the reverse transform.
     *  (The two transforms are exactly inverse.)
     */
    static MethodHandle pushMePullYou(MethodHandle bsm, boolean goToPushMode) {
        if (TRACE_METHOD_LINKAGE) {
            System.out.println("converting BSM of type " + bsm.type() + " to "
                    + (goToPushMode ? "push mode" : "pull mode"));
        }
        assert(isPullModeBSM(bsm) == goToPushMode); // there must be a change
        if (goToPushMode) {
            return PushAdapter.MH_pushToBootstrapMethod.bindTo(bsm).withVarargs(true);
        } else {
            return PullAdapter.MH_pullFromBootstrapMethod.bindTo(bsm).withVarargs(false);
        }
    }
}
