/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package vm.mlvm.meth.share;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nsk.share.test.TestUtils;
import vm.mlvm.share.Env;

public class TestTypes {

    public static final Class<?>[] TYPES = {
        // void.class
        boolean.class, byte.class, char.class, short.class, int.class, long.class,
        float.class, double.class, Object.class, String.class
    };

    public static final Map<Class<?>, Class<?>> BOX_MAP = new HashMap<Class<?>, Class<?>>();
    public static final Map<Class<?>, Class<?>> UNBOX_MAP = new HashMap<Class<?>, Class<?>>();
    static {
        BOX_MAP.put(boolean.class, Boolean.class);
        BOX_MAP.put(byte.class, Byte.class);
        BOX_MAP.put(char.class, Character.class);
        BOX_MAP.put(short.class, Short.class);
        BOX_MAP.put(int.class, Integer.class);
        BOX_MAP.put(long.class, Long.class);
        BOX_MAP.put(float.class, Float.class);
        BOX_MAP.put(double.class, Double.class);

        for ( Entry<Class<?>, Class<?>> e : BOX_MAP.entrySet() ) {
            UNBOX_MAP.put(e.getValue(), e.getKey());
        }
    }

    public static final Class<?>[] PRIMITIVE_WIDENING_HIERARCHY = {
        byte.class, short.class, char.class, int.class, long.class, float.class, double.class
    };

    public static boolean isBoxedType(Class<?> type) {
        return BOX_MAP.values().contains(type);
    }

    private static void addPrimitiveAndBoxed(List<Class<?>> list, Class<?> type) {
        list.add(type);
        list.add(BOX_MAP.get(type));
    }

    /**
     * WPC = JLS 5.1.2 Widening Primitive Conversions
     * @param type
     * @return
     */
    private static void addWPCAssignableTypesFor(List<Class<?>> result, Class<?> type) {
        if ( type.equals(short.class) ) {
            addPrimitiveAndBoxed(result, byte.class);
        }

        if ( type.equals(int.class) || type.equals(long.class) || type.equals(float.class) || type.equals(double.class) ) {
            for ( int p = 0; p < PRIMITIVE_WIDENING_HIERARCHY.length; p++ ) {
                Class<?> c = PRIMITIVE_WIDENING_HIERARCHY[p];
                addPrimitiveAndBoxed(result, c);

                if ( c.equals(type) )
                    break;
            }
        }
    }

    /**
     * NPC = JLS 5.1.3 Narrowing Primitive Conversions
     *     + JLS 5.1.4 Widening and Narrowing Primitive Conversions
     */
    private static void addNPCAssignableTypesFor(List<Class<?>> result, Class<?> type) {
        // JLS 5.1.4
        if ( type.equals(char.class) ) {
            addPrimitiveAndBoxed(result, byte.class);
        }

        // JLS 5.1.3
        int p = 0;
        for ( ; p < PRIMITIVE_WIDENING_HIERARCHY.length; p++ ) {
            if ( PRIMITIVE_WIDENING_HIERARCHY[p].equals(type) )
                break;
        }

        for ( ; p < PRIMITIVE_WIDENING_HIERARCHY.length; p++ ) {
            addPrimitiveAndBoxed(result, PRIMITIVE_WIDENING_HIERARCHY[p]);
        }
    }

    public static Class<?>[] getAssignableTypesFor(Class<?> type) {
        if ( type.equals(void.class) )
            return new Class<?>[0];

        if ( type.isPrimitive() ) {
            List<Class<?>> result = new LinkedList<Class<?>>();
            addPrimitiveAndBoxed(result, type);
            addWPCAssignableTypesFor(result, type);
            return (Class<?>[]) result.toArray();
        }

        if ( type.equals(Object.class) )
            return new Class<?>[] { Object.class, String.class };

        if ( type.equals(String.class) )
            return new Class<?>[] { String.class };

        throw new IllegalArgumentException("Don't know how to handle type " + type);
    }

    public static Class<?>[] getExplicitlyCastTypesFor(Class<?> type) {
        return TYPES;
    }

    public static boolean canConvertType(Class<?> from, Class<?> to, boolean isRetType) {
        return (Boolean) convert(from, null, to, isRetType, true);
    }

    public static boolean canExplicitlyCastType(Class<?> from, Class<?> to, boolean isRetType) {
        return true; // TODO: can use explicitCaseArguments() to convert "from" to "to"
    }

    /** convert an argument according to the rules defined in MethodHandles.convertArguments() */
    public static Argument convertArgument(Argument from, Class<?> toType, boolean isRetType) throws ClassCastException {
        Class<?> fromType = from.getType();

        if ( fromType.equals(toType) )
            return from;

        Object toValue = convert(fromType, from.getValue(), toType, isRetType, false);
        return new Argument(toType, toValue, from.isPreserved(), from.getTag());
    }

    /** convert an argument according to the rules defined in MethodHandles.convertArguments() */
    private static Object convert(Class<?> fromType, Object fromValue, Class<?> toType, boolean isRetType, boolean dryRun) {
        if ( ! dryRun ) {
            if ( ! fromType.isPrimitive() )
                TestUtils.assertTrue(fromType.isAssignableFrom(fromValue.getClass()), "fromType " + fromType + " is not assignable from the type of fromValue " + fromValue);
            else
                TestUtils.assertTrue(BOX_MAP.get(fromType).isAssignableFrom(fromValue.getClass()), "Boxed fromType " + fromType + " is not assignable from the type of fromValue " + fromValue);
        }

        // JLS 5.1.1 Identity conversion
        if ( fromType.equals(toType) )
            return dryRun ? Boolean.TRUE : fromValue;

        Class<?> exactFromType = fromValue.getClass();

        Throwable cause = null;

        try {
            if ( isRetType ) {
                // If the return type T1 is void, any returned value is discarded
                if ( toType.equals(void.class) )
                    return dryRun ? true : null;

                // If the return type T0 is void and T1 a reference, a null value is introduced.
                if ( fromType.equals(void.class) && ! toType.isPrimitive() )
                    return dryRun ? true : null;

                // If the return type T0 is void and T1 a primitive, a zero value is introduced.
                if ( fromType.equals(void.class) && toType.isPrimitive() ) {
                    return dryRun ? true : BOX_MAP.get(toType).newInstance();
                }
            }

            // If T0 and T1 are references, then a cast to T1 is applied.
            // (The types do not need to be related in any particular way.)
            if ( ! fromType.isPrimitive() && ! toType.isPrimitive() ) {
                return dryRun ? toType.isAssignableFrom(fromType)
                              : toType.cast(fromValue);
            }

            // If T0 and T1 are primitives, then a Java method invocation conversion
            // (JLS 5.3) is applied, if one exists.
            if ( fromType.isPrimitive() && toType.isPrimitive() ) {
                if ( dryRun ) {
                    for ( Class<?> tt : getAssignableTypesFor(toType) ) {
                        if ( tt.equals(fromType) )
                            return true;
                    }

                    return false;
                } else {
                    return PrimitiveTypeConverter.convert(fromValue, toType);
                }
            }

            // If T0 is a primitive and T1 a reference, a boxing conversion is applied
            // if one exists, possibly followed by a reference conversion to a superclass.
            // T1 must be a wrapper class or a supertype of one.
            if ( fromType.isPrimitive() && ! toType.isPrimitive() ) {
                return dryRun ? toType.isAssignableFrom(BOX_MAP.get(fromType))
                              : toType.cast(fromType.cast(fromValue));
            }

            // If T0 is a reference and T1 a primitive, an unboxing conversion will be applied
            // at runtime, possibly followed by a Java method invocation conversion (JLS 5.3)
            // on the primitive value. (These are the widening conversions.) T0 must be
            // a wrapper class or a supertype of one. (In the case where T0 is Object,
            // these are the conversions allowed by java.lang.reflect.Method.invoke.)
            if ( ! fromType.isPrimitive() && toType.isPrimitive() ) {
                if ( dryRun ) {
                    if ( ! BOX_MAP.values().contains(exactFromType) )
                        return false;


                }

                return dryRun ? toType.isAssignableFrom(BOX_MAP.get(fromType))
                              : toType.cast(fromType.cast(fromValue));
            }

        } catch ( Throwable t ) {
            cause = t;
        }

        if ( dryRun )
            return Boolean.FALSE;
        else
            throw (ClassCastException) (new ClassCastException("Can't convert value [" + fromValue + "] from type [" + fromType + "] to type [" + toType + "]")).initCause(cause);
    }

    public static Argument explicitCastArgument(Argument from, Class<?> toType, boolean isRetType) {
        return from; // TODO
    }

    public static Object nextRandomValueForType(Class<?> type) throws InstantiationException, IllegalAccessException {
        if (type.equals(void.class))
            return null;

        if (type.equals(boolean.class) || type.equals(Boolean.class))
            return Boolean.valueOf(Env.getRNG().nextInt(2) == 0);

        if (type.equals(byte.class) || type.equals(Byte.class))
            return Byte.valueOf((byte) Env.getRNG().nextInt(1 << Byte.SIZE));

        if (type.equals(int.class) || type.equals(Integer.class))
            return Integer.valueOf(Env.getRNG().nextInt());

        if (type.equals(short.class) || type.equals(Short.class))
            return Short.valueOf((short) Env.getRNG().nextInt(1 << Short.SIZE));

        if (type.equals(long.class) || type.equals(Long.class))
            return Long.valueOf(Env.getRNG().nextLong());

        if (type.equals(float.class) || type.equals(Float.class))
            return Float.valueOf(Env.getRNG().nextFloat());

        if (type.equals(double.class) || type.equals(Double.class))
            return Double.valueOf(Env.getRNG().nextDouble());

        if (type.equals(char.class) || type.equals(Character.class))
            return Character.valueOf((char) (32 + Env.getRNG().nextInt(96)));

        if (type.equals(Object.class))
            return new Object();

        if (type.equals(String.class)) {
            StringBuilder sb = new StringBuilder();
            for (int i = Env.getRNG().nextInt(100); i > 0; i--)
                sb.append(nextRandomValueForType(char.class));
            return sb.toString();
        }

        throw new IllegalArgumentException("Don't know how to handle type " + type);
    }

    public static int getSlotsCount(Class<?> type) {
        if (type.equals(void.class))
            return 0;

        if ( type.equals(boolean.class) || type.equals(Boolean.class)
          || type.equals(byte.class) || type.equals(Byte.class)
          || type.equals(int.class) || type.equals(Integer.class)
          || type.equals(short.class) || type.equals(Short.class)
          || type.equals(float.class) || type.equals(Float.class)
          || type.equals(char.class) || type.equals(Character.class)
          || Object.class.isAssignableFrom(type) )
            return 1;

        if ( type.equals(long.class) || type.equals(Long.class)
          || type.equals(double.class) || type.equals(Double.class))
            return 2;

        throw new IllegalArgumentException("Don't know how to handle type " + type);
    }
}
