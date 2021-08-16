/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.Introspector;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;

/**
 * @test
 * @bug 8211147
 * @modules java.desktop/com.sun.beans.introspect:open
 */
public final class MethodOrderException {

    public static void main(final String[] args) throws Exception {
        // Public API, fails rarely
        testPublicAPI();
        // Test using internal API, fails always
        testPrivateAPI();
    }

    private static void testPublicAPI() throws Exception {
        Introspector.getBeanInfo(X.class);
    }

    private static void testPrivateAPI() throws Exception {
        Class<?> name = Class.forName(
                "com.sun.beans.introspect.MethodInfo$MethodOrder");
        Field instance = name.getDeclaredField("instance");
        instance.setAccessible(true);
        Comparator<Method> o = (Comparator) instance.get(name);
        List<Method> methods = List.of(X.class.getDeclaredMethods());
        methods.forEach(m1 -> {
            methods.forEach(m2 -> {
                if (o.compare(m1, m2) != -o.compare(m2, m1)) {
                    System.err.println("Method1 = "+ m1);
                    System.err.println("Method2 = "+ m2);
                    throw new RuntimeException("Broken contract!");
                }
            });
        });
    }

    interface X_1 {

        AbstractList x_8();
    }
    interface X_2 {

        Cloneable x_0();
    }
    interface X_3 {

        Serializable x_1();
    }
    interface X_4 {

        Object x_7();
    }
    interface X_5 {

        RandomAccess x_6();
    }
    interface X_6 {

        RandomAccess x_0();
    }
    interface X_7 {

        Serializable x_5();
    }
    interface X_8 {

        Object x_4();
    }
    interface X_9 {

        RandomAccess x_5();
    }
    interface X_10 {

        Cloneable x_5();
    }
    interface X_11 {

        RandomAccess x_9();
    }
    interface X_12 {

        Cloneable x_9();
    }
    interface X_13 {

        Iterable x_2();
    }
    interface X_14 {

        Collection x_7();
    }
    interface X_15 {

        Serializable x_4();
    }
    interface X_16 {

        Cloneable x_7();
    }
    interface X_17 {

        Object x_1();
    }
    interface X_18 {

        ArrayList x_6();
    }
    interface X_19 {

        List x_5();
    }
    interface X_20 {

        Collection x_2();
    }
    interface X_21 {

        List x_1();
    }
    interface X_22 {

        List x_3();
    }
    interface X_23 {

        RandomAccess x_3();
    }
    interface X_24 {

        RandomAccess x_1();
    }
    interface X_25 {

        Object x_6();
    }
    interface X_26 {

        Cloneable x_7();
    }
    interface X_27 {

        Iterable x_0();
    }
    interface X_28 {

        Iterable x_1();
    }
    interface X_29 {

        AbstractList x_7();
    }
    interface X_30 {

        AbstractList x_1();
    }
    interface X_31 {

        Cloneable x_9();
    }
    interface X_32 {

        ArrayList x_6();
    }
    interface X_33 {

        Cloneable x_2();
    }
    interface X_34 {

        Iterable x_6();
    }
    interface X_35 {

        Iterable x_9();
    }
    interface X_36 {

        AbstractList x_9();
    }
    interface X_37 {

        Iterable x_7();
    }
    interface X_38 {

        Iterable x_3();
    }
    interface X_39 {

        Iterable x_9();
    }
    interface X_40 {

        AbstractList x_3();
    }
    interface X_41 {

        List x_0();
    }
    interface X_42 {

        Iterable x_0();
    }
    interface X_43 {

        Iterable x_2();
    }
    interface X_44 {

        ArrayList x_4();
    }
    interface X_45 {

        AbstractList x_4();
    }
    interface X_46 {

        Collection x_4();
    }
    interface X_47 {

        ArrayList x_2();
    }
    interface X_48 {

        ArrayList x_6();
    }
    interface X_49 {

        Serializable x_1();
    }
    interface X_50 {

        Cloneable x_7();
    }
    interface X_51 {

        Collection x_5();
    }
    interface X_52 {

        RandomAccess x_5();
    }
    interface X_53 {

        Collection x_5();
    }
    interface X_54 {

        RandomAccess x_4();
    }
    interface X_55 {

        Collection x_0();
    }
    interface X_56 {

        Collection x_7();
    }
    interface X_57 {

        Iterable x_9();
    }
    interface X_58 {

        List x_3();
    }
    interface X_59 {

        Serializable x_7();
    }
    interface X_60 {

        AbstractCollection x_6();
    }
    interface X_61 {

        AbstractList x_9();
    }
    interface X_62 {

        List x_7();
    }
    interface X_63 {

        AbstractCollection x_3();
    }
    interface X_64 {

        RandomAccess x_4();
    }
    interface X_65 {

        Object x_3();
    }
    interface X_66 {

        RandomAccess x_6();
    }
    interface X_67 {

        Cloneable x_6();
    }
    interface X_68 {

        Cloneable x_3();
    }
    interface X_69 {

        Collection x_5();
    }
    interface X_70 {

        AbstractCollection x_0();
    }
    interface X_71 {

        Object x_8();
    }
    interface X_72 {

        AbstractCollection x_3();
    }
    interface X_73 {

        Serializable x_4();
    }
    interface X_74 {

        AbstractList x_8();
    }
    interface X_75 {

        ArrayList x_1();
    }
    interface X_76 {

        List x_5();
    }
    interface X_77 {

        Object x_0();
    }
    interface X_78 {

        Collection x_0();
    }
    interface X_79 {

        ArrayList x_2();
    }
    interface X_80 {

        ArrayList x_8();
    }
    interface X_81 {

        Cloneable x_3();
    }
    interface X_82 {

        Serializable x_1();
    }
    interface X_83 {

        List x_1();
    }
    interface X_84 {

        Collection x_5();
    }
    interface X_85 {

        RandomAccess x_9();
    }
    interface X_86 {

        AbstractList x_3();
    }
    interface X_87 {

        Cloneable x_6();
    }
    interface X_88 {

        Object x_2();
    }
    interface X_89 {

        ArrayList x_5();
    }
    interface X_90 {

        Iterable x_1();
    }
    interface X_91 {

        ArrayList x_4();
    }
    interface X_92 {

        Iterable x_6();
    }
    interface X_93 {

        Collection x_7();
    }
    interface X_94 {

        Iterable x_2();
    }
    interface X_95 {

        AbstractList x_7();
    }
    interface X_96 {

        RandomAccess x_2();
    }
    interface X_97 {

        RandomAccess x_2();
    }
    interface X_98 {

        List x_6();
    }
    interface X_99 {

        Object x_4();
    }
    interface X_100 {

        Collection x_7();
    }
    static class X
            implements X_1, X_2, X_3, X_4, X_5, X_6, X_7, X_8, X_9, X_10, X_11,
                       X_12, X_13, X_14, X_15, X_16, X_17, X_18, X_19, X_20,
                       X_21, X_22, X_23, X_24, X_25, X_26, X_27, X_28, X_29,
                       X_30, X_31, X_32, X_33, X_34, X_35, X_36, X_37, X_38,
                       X_39, X_40, X_41, X_42, X_43, X_44, X_45, X_46, X_47,
                       X_48, X_49, X_50, X_51, X_52, X_53, X_54, X_55, X_56,
                       X_57, X_58, X_59, X_60, X_61, X_62, X_63, X_64, X_65,
                       X_66, X_67, X_68, X_69, X_70, X_71, X_72, X_73, X_74,
                       X_75, X_76, X_77, X_78, X_79, X_80, X_81, X_82, X_83,
                       X_84, X_85, X_86, X_87, X_88, X_89, X_90, X_91, X_92,
                       X_93, X_94, X_95, X_96, X_97, X_98, X_99, X_100 {

        public ArrayList x_0() {
            return null;
        }

        public ArrayList x_1() {
            return null;
        }

        public ArrayList x_2() {
            return null;
        }

        public ArrayList x_3() {
            return null;
        }

        public ArrayList x_4() {
            return null;
        }

        public ArrayList x_5() {
            return null;
        }

        public ArrayList x_6() {
            return null;
        }

        public ArrayList x_7() {
            return null;
        }

        public ArrayList x_8() {
            return null;
        }

        public ArrayList x_9() {
            return null;
        }
    }
}
