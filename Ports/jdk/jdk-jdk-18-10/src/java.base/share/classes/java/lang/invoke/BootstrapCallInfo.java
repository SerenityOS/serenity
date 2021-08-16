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

import java.lang.invoke.MethodHandles.Lookup;

/**
 * An interface providing full static information about a particular
 * call to a
 * <a href="package-summary.html#bsm">bootstrap method</a> of an
 * dynamic call site or dynamic constant.
 * This information includes the method itself, the associated
 * name and type, and any associated static arguments.
 * <p>
 * If a bootstrap method declares exactly two arguments, and is
 * not of variable arity, then it is fed only two arguments by
 * the JVM, the {@linkplain Lookup lookup object} and an instance
 * of {@code BootstrapCallInfo} which supplies the rest of the
 * information about the call.
 * <p>
 * The API for accessing the static arguments allows the bootstrap
 * method to reorder the resolution (in the constant pool) of the
 * static arguments, and to catch errors resulting from the resolution.
 * This mode of evaluation <em>pulls</em> bootstrap parameters from
 * the JVM under control of the bootstrap method, as opposed to
 * the JVM <em>pushing</em> parameters to a bootstrap method
 * by resolving them all before the bootstrap method is called.
 * @apiNote
 * <p>
 * The {@linkplain Lookup lookup object} is <em>not</em> included in this
 * bundle of information, so as not to obscure the access control
 * logic of the program.
 * In cases where there are many thousands of parameters, it may
 * be preferable to pull their resolved values, either singly or in
 * batches, rather than wait until all of them have been resolved
 * before a constant or call site can be used.
 * <p>
 * A push mode bootstrap method can be adapted to a pull mode
 * bootstrap method, and vice versa.  For example, this generic
 * adapter pops a push-mode bootstrap method from the beginning
 * of the static argument list, eagerly resolves all the remaining
 * static arguments, and invokes the popped method in push mode.
 * The callee has no way of telling that it was not called directly
 * from the JVM.
 * <blockquote><pre>{@code
static Object genericBSM(Lookup lookup, BootstrapCallInfo<Object> bsci)
    throws Throwable {
  ArrayList<Object> args = new ArrayList<>();
  args.add(lookup);
  args.add(bsci.invocationName());
  args.add(bsci.invocationType());
  MethodHandle bsm = (MethodHandle) bsci.get(0);
  List<Object> restOfArgs = bsci.asList().subList(1, bsci.size();
  // the next line eagerly resolves all remaining static arguments:
  args.addAll(restOfArgs);
  return bsm.invokeWithArguments(args);
}
 * }</pre></blockquote>
 *
 * <p>
 * In the other direction, here is a combinator which pops
 * a pull-mode bootstrap method from the beginning of a list of
 * static argument values (already resolved), reformats all of
 * the arguments into a pair of a lookup and a {@code BootstrapCallInfo},
 * and invokes the popped method.  Again the callee has no way of
 * telling it was not called directly by the JVM, except that
 * all of the constant values will appear as resolved.
 * Put another way, if any constant fails to resolve, the
 * callee will not be able to catch the resulting error,
 * since the error will be thrown by the JVM before the
 * bootstrap method is entered.
 * <blockquote><pre>{@code
static Object genericBSM(Lookup lookup, String name, Object type,
                         MethodHandle bsm, Object... args)
    throws Throwable {
  ConstantGroup cons = ConstantGroup.makeConstantGroup(Arrays.asList(args));
  BootstrapCallInfo<Object> bsci = makeBootstrapCallInfo(bsm, name, type, cons);
  return bsm.invoke(lookup, bsci);
}
 * }</pre></blockquote>
 *
 * @since 1.10
 */
// public
interface BootstrapCallInfo<T> extends ConstantGroup {
    /** Returns the bootstrap method for this call.
     * @return the bootstrap method
     */
    MethodHandle bootstrapMethod();

    /** Returns the method name or constant name for this call.
     * @return the method name or constant name
     */
    String invocationName();

    /** Returns the method type or constant type for this call.
     * @return the method type or constant type
     */
    T invocationType();

    /**
     * Make a new bootstrap call descriptor with the given components.
     * @param bsm bootstrap method
     * @param name invocation name
     * @param type invocation type
     * @param constants the additional static arguments for the bootstrap method
     * @param <T> the type of the invocation type, either {@link MethodHandle} or {@link Class}
     * @return a new bootstrap call descriptor with the given components
     */
    static <T> BootstrapCallInfo<T> makeBootstrapCallInfo(MethodHandle bsm,
                                                          String name,
                                                          T type,
                                                          ConstantGroup constants) {
        AbstractConstantGroup.BSCIWithCache<T> bsci = new AbstractConstantGroup.BSCIWithCache<>(bsm, name, type, constants.size());
        final Object NP = AbstractConstantGroup.BSCIWithCache.NOT_PRESENT;
        bsci.initializeCache(constants.asList(NP), NP);
        return bsci;
    }
}
