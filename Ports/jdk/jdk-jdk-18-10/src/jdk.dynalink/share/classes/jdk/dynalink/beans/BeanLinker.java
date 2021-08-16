/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.dynalink.beans;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.Namespace;
import jdk.dynalink.Operation;
import jdk.dynalink.StandardNamespace;
import jdk.dynalink.StandardOperation;
import jdk.dynalink.beans.GuardedInvocationComponent.ValidationType;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.TypeBasedGuardingDynamicLinker;
import jdk.dynalink.linker.support.Guards;
import jdk.dynalink.linker.support.Lookup;
import jdk.dynalink.linker.support.TypeUtilities;

/**
 * A class that provides linking capabilities for a single POJO class. Normally not used directly, but managed by
 * {@link BeansLinker}. Most of the functionality is provided by the {@link AbstractJavaLinker} superclass; this
 * class adds length and element operations for arrays and collections.
 */
class BeanLinker extends AbstractJavaLinker implements TypeBasedGuardingDynamicLinker {
    BeanLinker(final Class<?> clazz) {
        super(clazz, Guards.getClassGuard(clazz), Guards.getInstanceOfGuard(clazz));
        if(clazz.isArray()) {
            // Some languages won't have a notion of manipulating collections. Exposing "length" on arrays as an
            // explicit property is beneficial for them.
            setPropertyGetter("length", GET_ARRAY_LENGTH, ValidationType.IS_ARRAY);
        } else if(Collection.class.isAssignableFrom(clazz)) {
            setPropertyGetter("length", GET_COLLECTION_LENGTH, ValidationType.INSTANCE_OF);
        } else if(Map.class.isAssignableFrom(clazz)) {
            setPropertyGetter("length", GET_MAP_LENGTH, ValidationType.INSTANCE_OF);
        }
    }

    @Override
    public boolean canLinkType(final Class<?> type) {
        return type == clazz;
    }

    @Override
    FacetIntrospector createFacetIntrospector() {
        return new BeanIntrospector(clazz);
    }

    @Override
    protected GuardedInvocationComponent getGuardedInvocationComponent(final ComponentLinkRequest req) throws Exception {
        if (req.namespaces.isEmpty()) {
            return null;
        }
        final Namespace ns = req.namespaces.get(0);
        if (ns == StandardNamespace.ELEMENT) {
            final Operation op = req.baseOperation;
            if (op == StandardOperation.GET) {
                return getElementGetter(req.popNamespace());
            } else if (op == StandardOperation.SET) {
                return getElementSetter(req.popNamespace());
            } else if (op == StandardOperation.REMOVE) {
                return getElementRemover(req.popNamespace());
            }
        }
        return super.getGuardedInvocationComponent(req);
    }

    @Override
    SingleDynamicMethod getConstructorMethod(final String signature) {
        return null;
    }

    private static final MethodHandle GET_LIST_ELEMENT = Lookup.PUBLIC.findVirtual(List.class, "get",
            MethodType.methodType(Object.class, int.class));

    private static final MethodHandle GET_MAP_ELEMENT = Lookup.PUBLIC.findVirtual(Map.class, "get",
            MethodType.methodType(Object.class, Object.class));

    private static final MethodHandle LIST_GUARD = Guards.getInstanceOfGuard(List.class);
    private static final MethodHandle MAP_GUARD = Guards.getInstanceOfGuard(Map.class);

    private static final MethodHandle NULL_GETTER_1;
    private static final MethodHandle NULL_GETTER_2;
    static {
        final MethodHandle constantNull = MethodHandles.constant(Object.class, null);
        NULL_GETTER_1 = dropObjectArguments(constantNull, 1);
        NULL_GETTER_2 = dropObjectArguments(constantNull, 2);
    }

    private static MethodHandle dropObjectArguments(final MethodHandle m, final int n) {
        return MethodHandles.dropArguments(m, 0, Collections.nCopies(n, Object.class));
    }

    private enum CollectionType {
        ARRAY, LIST, MAP
    }

    private GuardedInvocationComponent getElementGetter(final ComponentLinkRequest req) throws Exception {
        final CallSiteDescriptor callSiteDescriptor = req.getDescriptor();
        final Object name = req.name;
        final boolean isFixedKey = name != null;
        assertParameterCount(callSiteDescriptor, isFixedKey ? 1 : 2);
        final LinkerServices linkerServices = req.linkerServices;
        final MethodType callSiteType = callSiteDescriptor.getMethodType();
        final GuardedInvocationComponent nextComponent = getNextComponent(req);

        final GuardedInvocationComponentAndCollectionType gicact = guardedInvocationComponentAndCollectionType(
                callSiteType, linkerServices, MethodHandles::arrayElementGetter, GET_LIST_ELEMENT, GET_MAP_ELEMENT);

        if (gicact == null) {
            // Can't retrieve elements for objects that are neither arrays, nor list, nor maps.
            return nextComponent;
        }

        final Object typedName = getTypedName(name, gicact.collectionType == CollectionType.MAP, linkerServices);
        if (typedName == INVALID_NAME) {
            return nextComponent;
        }

        return guardComponentWithRangeCheck(gicact, callSiteType, nextComponent,
                new Binder(linkerServices, callSiteType, typedName), isFixedKey ? NULL_GETTER_1 : NULL_GETTER_2);
    }

    private static class GuardedInvocationComponentAndCollectionType {
        final GuardedInvocationComponent gic;
        final CollectionType collectionType;

        GuardedInvocationComponentAndCollectionType(final GuardedInvocationComponent gic, final CollectionType collectionType) {
            this.gic = gic;
            this.collectionType = collectionType;
        }
    }

    private GuardedInvocationComponentAndCollectionType guardedInvocationComponentAndCollectionType(
            final MethodType callSiteType, final LinkerServices linkerServices,
            final Function<Class<?>, MethodHandle> arrayMethod, final MethodHandle listMethod, final MethodHandle mapMethod) {
        final Class<?> declaredType = callSiteType.parameterType(0);
        // If declared type of receiver at the call site is already an array, a list or map, bind without guard. Thing
        // is, it'd be quite stupid of a call site creator to go though invokedynamic when it knows in advance they're
        // dealing with an array, or a list or map, but hey...
        // Note that for arrays and lists, using LinkerServices.asType() will ensure that any language specific linkers
        // in use will get a chance to perform any (if there's any) implicit conversion to integer for the indices.
        if(declaredType.isArray() && arrayMethod != null) {
            return new GuardedInvocationComponentAndCollectionType(
                    createInternalFilteredGuardedInvocationComponent(arrayMethod.apply(declaredType), linkerServices),
                    CollectionType.ARRAY);
        } else if(List.class.isAssignableFrom(declaredType)) {
            return new GuardedInvocationComponentAndCollectionType(
                    createInternalFilteredGuardedInvocationComponent(listMethod, linkerServices),
                    CollectionType.LIST);
        } else if(Map.class.isAssignableFrom(declaredType)) {
            return new GuardedInvocationComponentAndCollectionType(
                    createInternalFilteredGuardedInvocationComponent(mapMethod, linkerServices),
                    CollectionType.MAP);
        } else if(clazz.isArray() && arrayMethod != null) {
            return new GuardedInvocationComponentAndCollectionType(
                    getClassGuardedInvocationComponent(linkerServices.filterInternalObjects(arrayMethod.apply(clazz)), callSiteType),
                    CollectionType.ARRAY);
        } else if(List.class.isAssignableFrom(clazz)) {
            return new GuardedInvocationComponentAndCollectionType(
                    createInternalFilteredGuardedInvocationComponent(listMethod, Guards.asType(LIST_GUARD, callSiteType),
                            List.class, ValidationType.INSTANCE_OF, linkerServices),
                    CollectionType.LIST);
        } else if(Map.class.isAssignableFrom(clazz)) {
            return new GuardedInvocationComponentAndCollectionType(
                    createInternalFilteredGuardedInvocationComponent(mapMethod, Guards.asType(MAP_GUARD, callSiteType),
                            Map.class, ValidationType.INSTANCE_OF, linkerServices),
                    CollectionType.MAP);
        }
        return null;
    }

    private static final Object INVALID_NAME = new Object();

    private static Object getTypedName(final Object name, final boolean isMap, final LinkerServices linkerServices) throws Exception {
        // Convert the key to a number if we're working with a list or array
        if (!isMap && name != null) {
            final Integer integer = convertKeyToInteger(name, linkerServices);
            if (integer == null || integer < 0) {
                // key is not a non-negative integer, it can never address an
                // array or list element
                return INVALID_NAME;
            }
            return integer;
        }
        return name;
    }

    private static GuardedInvocationComponent guardComponentWithRangeCheck(
            final GuardedInvocationComponentAndCollectionType gicact, final MethodType callSiteType,
            final GuardedInvocationComponent nextComponent, final Binder binder, final MethodHandle noOp) {

        final MethodHandle checkGuard;
        switch(gicact.collectionType) {
            case LIST:
                checkGuard = binder.convertArgToNumber(RANGE_CHECK_LIST);
                break;
            case MAP:
                checkGuard = binder.linkerServices.filterInternalObjects(CONTAINS_MAP);
                break;
            case ARRAY:
                checkGuard = binder.convertArgToNumber(RANGE_CHECK_ARRAY);
                break;
            default:
                throw new AssertionError();
        }

        // If there's no next component, produce a fixed no-op one
        final GuardedInvocationComponent finalNextComponent;
        if (nextComponent != null) {
            finalNextComponent = nextComponent;
        } else {
            finalNextComponent = createGuardedInvocationComponentAsType(noOp, callSiteType, binder.linkerServices);
        }

        final GuardedInvocationComponent gic = gicact.gic;
        final GuardedInvocation gi = gic.getGuardedInvocation();

        final MethodPair matchedInvocations = matchReturnTypes(binder.bind(gi.getInvocation()),
                finalNextComponent.getGuardedInvocation().getInvocation());

        return finalNextComponent.compose(matchedInvocations.guardWithTest(binder.bindTest(checkGuard)), gi.getGuard(),
                gic.getValidatorClass(), gic.getValidationType());
    }

    private static GuardedInvocationComponent createInternalFilteredGuardedInvocationComponent(
            final MethodHandle invocation, final LinkerServices linkerServices) {
        return new GuardedInvocationComponent(linkerServices.filterInternalObjects(invocation));
    }

    private static GuardedInvocationComponent createGuardedInvocationComponentAsType(
            final MethodHandle invocation, final MethodType fromType, final LinkerServices linkerServices) {
        return new GuardedInvocationComponent(linkerServices.asType(invocation, fromType));
    }

    private static GuardedInvocationComponent createInternalFilteredGuardedInvocationComponent(
            final MethodHandle invocation, final MethodHandle guard, final Class<?> validatorClass,
            final ValidationType validationType, final LinkerServices linkerServices) {
        return new GuardedInvocationComponent(linkerServices.filterInternalObjects(invocation), guard,
                validatorClass, validationType);
    }

    private static Integer convertKeyToInteger(final Object fixedKey, final LinkerServices linkerServices) throws Exception {
        if (fixedKey instanceof Integer) {
            return (Integer)fixedKey;
        }

        final Number n;
        if (fixedKey instanceof Number) {
            n = (Number)fixedKey;
        } else {
            final Class<?> keyClass = fixedKey.getClass();
            if(linkerServices.canConvert(keyClass, Number.class)) {
                final Object val;
                try {
                    val = linkerServices.getTypeConverter(keyClass, Number.class).invoke(fixedKey);
                } catch(Exception|Error e) {
                    throw e;
                } catch(final Throwable t) {
                    throw new RuntimeException(t);
                }
                if(!(val instanceof Number)) {
                    return null; // not a number
                }
                n = (Number)val;
            } else if (fixedKey instanceof String){
                try {
                    return Integer.valueOf((String)fixedKey);
                } catch(final NumberFormatException e) {
                    // key is not a number
                    return null;
                }
            } else {
                return null;
            }
        }

        if(n instanceof Integer) {
            return (Integer)n;
        }
        final int intIndex = n.intValue();
        final double doubleValue = n.doubleValue();
        if(intIndex != doubleValue && !Double.isInfinite(doubleValue)) { // let infinites trigger IOOBE
            return null; // not an exact integer
        }
        return intIndex;
    }

    /**
     * Contains methods to adapt an item getter/setter method handle to the requested type, optionally binding it to a
     * fixed key first.
     */
    private static class Binder {
        private final LinkerServices linkerServices;
        private final MethodType methodType;
        private final Object fixedKey;

        Binder(final LinkerServices linkerServices, final MethodType methodType, final Object fixedKey) {
            this.linkerServices = linkerServices;
            this.methodType = fixedKey == null ? methodType : methodType.insertParameterTypes(1, fixedKey.getClass());
            this.fixedKey = fixedKey;
        }

        /*private*/ MethodHandle bind(final MethodHandle handle) {
            return bindToFixedKey(linkerServices.asTypeLosslessReturn(handle, methodType));
        }

        /*private*/ MethodHandle bindTest(final MethodHandle handle) {
            return bindToFixedKey(Guards.asType(handle, methodType));
        }

        /*private*/ MethodHandle convertArgToNumber(final MethodHandle mh) {
            final Class<?> sourceType = methodType.parameterType(1);
            if(TypeUtilities.isMethodInvocationConvertible(sourceType, Number.class)) {
                return mh;
            } else if(linkerServices.canConvert(sourceType, Number.class)) {
                final MethodHandle converter = linkerServices.getTypeConverter(sourceType, Number.class);
                return MethodHandles.filterArguments(mh, 1, converter.asType(converter.type().changeReturnType(
                        mh.type().parameterType(1))));
            }
            return mh;
        }

        private MethodHandle bindToFixedKey(final MethodHandle handle) {
            return fixedKey == null ? handle : MethodHandles.insertArguments(handle, 1, fixedKey);
        }
    }

    private static final MethodHandle RANGE_CHECK_ARRAY = findRangeCheck(Object.class);
    private static final MethodHandle RANGE_CHECK_LIST = findRangeCheck(List.class);
    private static final MethodHandle CONTAINS_MAP = Lookup.PUBLIC.findVirtual(Map.class, "containsKey",
            MethodType.methodType(boolean.class, Object.class));

    private static MethodHandle findRangeCheck(final Class<?> collectionType) {
        return Lookup.findOwnStatic(MethodHandles.lookup(), "rangeCheck", boolean.class, collectionType, Object.class);
    }

    @SuppressWarnings("unused")
    private static boolean rangeCheck(final Object array, final Object index) {
        if(!(index instanceof Number)) {
            return false;
        }
        final Number n = (Number)index;
        final int intIndex = n.intValue();
        if (intIndex != n.doubleValue()) {
            return false;
        }
        return 0 <= intIndex && intIndex < Array.getLength(array);
    }

    @SuppressWarnings("unused")
    private static boolean rangeCheck(final List<?> list, final Object index) {
        if(!(index instanceof Number)) {
            return false;
        }
        final Number n = (Number)index;
        final int intIndex = n.intValue();
        if (intIndex != n.doubleValue()) {
            return false;
        }
        return 0 <= intIndex && intIndex < list.size();
    }

    @SuppressWarnings("unused")
    private static void noOp() {
    }

    private static final MethodHandle SET_LIST_ELEMENT = Lookup.PUBLIC.findVirtual(List.class, "set",
            MethodType.methodType(Object.class, int.class, Object.class));

    private static final MethodHandle PUT_MAP_ELEMENT = Lookup.PUBLIC.findVirtual(Map.class, "put",
            MethodType.methodType(Object.class, Object.class, Object.class));

    private static final MethodHandle NO_OP_1;
    private static final MethodHandle NO_OP_2;
    private static final MethodHandle NO_OP_3;
    static {
        final MethodHandle noOp = Lookup.findOwnStatic(MethodHandles.lookup(), "noOp", void.class);
        NO_OP_1 = dropObjectArguments(noOp, 1);
        NO_OP_2 = dropObjectArguments(noOp, 2);
        NO_OP_3 = dropObjectArguments(noOp, 3);
    }

    private GuardedInvocationComponent getElementSetter(final ComponentLinkRequest req) throws Exception {
        final CallSiteDescriptor callSiteDescriptor = req.getDescriptor();
        final Object name = req.name;
        final boolean isFixedKey = name != null;
        assertParameterCount(callSiteDescriptor, isFixedKey ? 2 : 3);
        final LinkerServices linkerServices = req.linkerServices;
        final MethodType callSiteType = callSiteDescriptor.getMethodType();

        final GuardedInvocationComponentAndCollectionType gicact = guardedInvocationComponentAndCollectionType(
                callSiteType, linkerServices, MethodHandles::arrayElementSetter, SET_LIST_ELEMENT, PUT_MAP_ELEMENT);

        if(gicact == null) {
            return getNextComponent(req);
        }

        final boolean isMap = gicact.collectionType == CollectionType.MAP;

        // In contrast to, say, getElementGetter, we only compute the nextComponent if the target object is not a map,
        // as maps will always succeed in setting the element and will never need to fall back to the next component
        // operation.
        final GuardedInvocationComponent nextComponent = isMap ? null : getNextComponent(req);

        final Object typedName = getTypedName(name, isMap, linkerServices);
        if (typedName == INVALID_NAME) {
            return nextComponent;
        }

        final GuardedInvocationComponent gic = gicact.gic;
        final GuardedInvocation gi = gic.getGuardedInvocation();
        final Binder binder = new Binder(linkerServices, callSiteType, typedName);
        final MethodHandle invocation = gi.getInvocation();

        if (isMap) {
            return gic.replaceInvocation(binder.bind(invocation));
        }

        return guardComponentWithRangeCheck(gicact, callSiteType, nextComponent, binder, isFixedKey ? NO_OP_2 : NO_OP_3);
    }

    private static final MethodHandle REMOVE_LIST_ELEMENT = Lookup.PUBLIC.findVirtual(List.class, "remove",
            MethodType.methodType(Object.class, int.class));

    private static final MethodHandle REMOVE_MAP_ELEMENT = Lookup.PUBLIC.findVirtual(Map.class, "remove",
            MethodType.methodType(Object.class, Object.class));

    private GuardedInvocationComponent getElementRemover(final ComponentLinkRequest req) throws Exception {
        final CallSiteDescriptor callSiteDescriptor = req.getDescriptor();
        final Object name = req.name;
        final boolean isFixedKey = name != null;
        assertParameterCount(callSiteDescriptor, isFixedKey ? 1 : 2);
        final LinkerServices linkerServices = req.linkerServices;
        final MethodType callSiteType = callSiteDescriptor.getMethodType();
        final GuardedInvocationComponent nextComponent = getNextComponent(req);

        final GuardedInvocationComponentAndCollectionType gicact = guardedInvocationComponentAndCollectionType(
                callSiteType, linkerServices, null, REMOVE_LIST_ELEMENT, REMOVE_MAP_ELEMENT);

        if (gicact == null) {
            // Can't remove elements for objects that are neither lists, nor maps.
            return nextComponent;
        }

        final Object typedName = getTypedName(name, gicact.collectionType == CollectionType.MAP, linkerServices);
        if (typedName == INVALID_NAME) {
            return nextComponent;
        }

        return guardComponentWithRangeCheck(gicact, callSiteType, nextComponent,
                new Binder(linkerServices, callSiteType, typedName), isFixedKey ? NO_OP_1: NO_OP_2);
    }

    private static final MethodHandle GET_COLLECTION_LENGTH = Lookup.PUBLIC.findVirtual(Collection.class, "size",
            MethodType.methodType(int.class));

    private static final MethodHandle GET_MAP_LENGTH = Lookup.PUBLIC.findVirtual(Map.class, "size",
            MethodType.methodType(int.class));

    private static final MethodHandle GET_ARRAY_LENGTH = Lookup.PUBLIC.findStatic(Array.class, "getLength",
            MethodType.methodType(int.class, Object.class));

    private static void assertParameterCount(final CallSiteDescriptor descriptor, final int paramCount) {
        if(descriptor.getMethodType().parameterCount() != paramCount) {
            throw new BootstrapMethodError(descriptor.getOperation() + " must have exactly " + paramCount + " parameters.");
        }
    }
}
