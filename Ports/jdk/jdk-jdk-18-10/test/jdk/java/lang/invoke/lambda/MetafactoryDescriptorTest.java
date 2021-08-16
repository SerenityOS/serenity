/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8035776 8173587
 * @summary metafactory should fail if instantiatedMethodType does not match sam/bridge descriptors
 */
import java.lang.invoke.*;
import java.util.*;

public class MetafactoryDescriptorTest {

    static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    static MethodType mt(Class<?> ret, Class<?>... params) {
        return MethodType.methodType(ret, params);
    }

    public interface I {}

    public static class C {
        public static void m_void(String arg) {}
        public static boolean m_boolean(String arg) { return true; }
        public static char m_char(String arg) { return 'x'; }
        public static byte m_byte(String arg) { return 12; }
        public static short m_short(String arg) { return 12; }
        public static int m_int(String arg) { return 12; }
        public static long m_long(String arg) { return 12; }
        public static float m_float(String arg) { return 12; }
        public static double m_double(String arg) { return 12; }
        public static String m_String(String arg) { return ""; }
        public static Integer m_Integer(String arg) { return 23; }
        public static Object m_Object(String arg) { return new Object(); }

        public static String n_boolean(boolean arg) { return ""; }
        public static String n_char(char arg) { return ""; }
        public static String n_byte(byte arg) { return ""; }
        public static String n_short(short arg) { return ""; }
        public static String n_int(int arg) { return ""; }
        public static String n_long(long arg) { return ""; }
        public static String n_float(float arg) { return ""; }
        public static String n_double(double arg) { return ""; }
        public static String n_String(String arg) { return ""; }
        public static String n_Integer(Integer arg) { return ""; }
        public static String n_Object(Object arg) { return ""; }

        public static MethodHandle getM(Class<?> c) {
            try {
                return lookup.findStatic(C.class, "m_" + c.getSimpleName(), mt(c, String.class));
            }
            catch (NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        public static MethodHandle getN(Class<?> c) {
            if (c == void.class) return null;
            try {
                return lookup.findStatic(C.class, "n_" + c.getSimpleName(), mt(String.class, c));
            }
            catch (NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static void main(String... args) {
        Class<?>[] t = { void.class, boolean.class, char.class,
                         byte.class, short.class, int.class, long.class, float.class, double.class,
                         String.class, Integer.class, Object.class };

        for (int i = 0; i < t.length; i++) {
            MethodHandle m = C.getM(t[i]);
            MethodHandle n = C.getN(t[i]); // null for void.class
            for (int j = 0; j < t.length; j++) {
                boolean correctRet = t[j].isAssignableFrom(t[i]) || conversions.contains(t[i], t[j]);
                test(correctRet, m, mt(t[i], String.class), mt(t[j], String.class));
                testBridge(correctRet, m, mt(t[i], String.class), mt(t[i], String.class),
                           mt(t[j], Object.class));
                testBridge(correctRet, m, mt(t[i], String.class), mt(t[i], String.class),
                           mt(t[i], CharSequence.class), mt(t[j], Object.class));

                if (t[i] != void.class && t[j] != void.class) {
                    boolean correctParam = t[j].isAssignableFrom(t[i]);
                    test(correctParam, n, mt(String.class, t[i]), mt(String.class, t[j]));
                    testBridge(correctParam, n, mt(String.class, t[i]), mt(String.class, t[i]),
                            mt(Object.class, t[j]));
                    testBridge(correctParam, n, mt(String.class, t[i]), mt(String.class, t[i]),
                            mt(CharSequence.class, t[i]), mt(Object.class, t[j]));
                }

            }
        }
    }

    static void test(boolean correct, MethodHandle mh, MethodType instMT, MethodType samMT) {
        tryMetafactory(correct, mh, instMT, samMT);
        tryAltMetafactory(correct, mh, instMT, samMT);
    }

    static void testBridge(boolean correct, MethodHandle mh, MethodType instMT, MethodType samMT, MethodType... bridgeMTs) {
        tryAltMetafactory(correct, mh, instMT, samMT, bridgeMTs);
    }

    static void tryMetafactory(boolean correct, MethodHandle mh, MethodType instMT, MethodType samMT) {
        try {
            LambdaMetafactory.metafactory(lookup, "run", mt(I.class),
                                          samMT, mh, instMT);
            if (!correct) {
                throw new AssertionError("Unexpected linkage without error:" +
                                         " impl=" + mh +
                                         ", inst=" + instMT +
                                         ", sam=" + samMT);
            }
        }
        catch (LambdaConversionException e) {
            if (correct) {
                throw new AssertionError("Unexpected linkage error:" +
                                         " e=" + e +
                                         ", impl=" + mh +
                                         ", inst=" + instMT +
                                         ", sam=" + samMT);
            }
        }
    }

    static void tryAltMetafactory(boolean correct, MethodHandle mh, MethodType instMT, MethodType samMT,
                                  MethodType... bridgeMTs) {
        boolean bridge = bridgeMTs.length > 0;
        Object[] args = new Object[bridge ? 5+bridgeMTs.length : 4];
        args[0] = samMT;
        args[1] = mh;
        args[2] = instMT;
        args[3] = bridge ? LambdaMetafactory.FLAG_BRIDGES : 0;
        if (bridge) {
            args[4] = bridgeMTs.length;
            for (int i = 0; i < bridgeMTs.length; i++) args[5+i] = bridgeMTs[i];
        }
        try {
            LambdaMetafactory.altMetafactory(lookup, "run", mt(I.class), args);
            if (!correct) {
                throw new AssertionError("Unexpected linkage without error:" +
                                         " impl=" + mh +
                                         ", inst=" + instMT +
                                         ", sam=" + samMT +
                                         ", bridges=" + Arrays.toString(bridgeMTs));
            }
        }
        catch (LambdaConversionException e) {
            if (correct) {
                throw new AssertionError("Unexpected linkage error:" +
                                         " e=" + e +
                                         ", impl=" + mh +
                                         ", inst=" + instMT +
                                         ", sam=" + samMT +
                                         ", bridges=" + Arrays.toString(bridgeMTs));
            }
        }
    }

    private static class ConversionTable {
        private final Map<Class<?>, Set<Class<?>>> pairs = new HashMap<>();

        public void put(Class<?> from, Class<?> to) {
            Set<Class<?>> set = pairs.computeIfAbsent(from, f -> new HashSet<>());
            set.add(to);
        }

        public boolean contains(Class<?> from, Class<?> to) {
            return pairs.containsKey(from) && pairs.get(from).contains(to);
        }
    }

    private static ConversionTable conversions = new ConversionTable();
    static {
        conversions.put(char.class, int.class);
        conversions.put(char.class, long.class);
        conversions.put(char.class, float.class);
        conversions.put(char.class, double.class);
        conversions.put(char.class, Character.class);
        conversions.put(char.class, Object.class);
        conversions.put(Character.class, char.class);
        conversions.put(Character.class, int.class);
        conversions.put(Character.class, long.class);
        conversions.put(Character.class, float.class);
        conversions.put(Character.class, double.class);

        conversions.put(byte.class, short.class);
        conversions.put(byte.class, int.class);
        conversions.put(byte.class, long.class);
        conversions.put(byte.class, float.class);
        conversions.put(byte.class, double.class);
        conversions.put(byte.class, Byte.class);
        conversions.put(byte.class, Object.class);
        conversions.put(Byte.class, byte.class);
        conversions.put(Byte.class, short.class);
        conversions.put(Byte.class, int.class);
        conversions.put(Byte.class, long.class);
        conversions.put(Byte.class, float.class);
        conversions.put(Byte.class, double.class);

        conversions.put(short.class, int.class);
        conversions.put(short.class, long.class);
        conversions.put(short.class, float.class);
        conversions.put(short.class, double.class);
        conversions.put(short.class, Short.class);
        conversions.put(short.class, Object.class);
        conversions.put(Short.class, short.class);
        conversions.put(Short.class, int.class);
        conversions.put(Short.class, long.class);
        conversions.put(Short.class, float.class);
        conversions.put(Short.class, double.class);

        conversions.put(int.class, long.class);
        conversions.put(int.class, float.class);
        conversions.put(int.class, double.class);
        conversions.put(int.class, Integer.class);
        conversions.put(int.class, Object.class);
        conversions.put(Integer.class, int.class);
        conversions.put(Integer.class, long.class);
        conversions.put(Integer.class, float.class);
        conversions.put(Integer.class, double.class);

        conversions.put(long.class, float.class);
        conversions.put(long.class, double.class);
        conversions.put(long.class, Long.class);
        conversions.put(long.class, Object.class);
        conversions.put(Long.class, long.class);
        conversions.put(Long.class, float.class);
        conversions.put(Long.class, double.class);

        conversions.put(float.class, double.class);
        conversions.put(float.class, Float.class);
        conversions.put(float.class, Object.class);
        conversions.put(Float.class, float.class);
        conversions.put(Float.class, double.class);

        conversions.put(double.class, Double.class);
        conversions.put(double.class, Object.class);
        conversions.put(Double.class, double.class);

        conversions.put(boolean.class, Boolean.class);
        conversions.put(boolean.class, Object.class);
        conversions.put(Boolean.class, boolean.class);
    }

}
