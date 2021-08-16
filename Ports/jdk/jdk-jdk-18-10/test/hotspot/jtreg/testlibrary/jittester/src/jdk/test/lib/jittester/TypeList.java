/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.jittester;

import jdk.test.lib.jittester.types.TypeBoolean;
import jdk.test.lib.jittester.types.TypeByte;
import jdk.test.lib.jittester.types.TypeChar;
import jdk.test.lib.jittester.types.TypeDouble;
import jdk.test.lib.jittester.types.TypeFloat;
import jdk.test.lib.jittester.types.TypeInt;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.types.TypeLong;
import jdk.test.lib.jittester.types.TypeShort;
import jdk.test.lib.jittester.types.TypeVoid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class TypeList {
    public static final TypeVoid VOID = new TypeVoid();
    public static final TypeBoolean BOOLEAN = new TypeBoolean();
    public static final TypeByte BYTE = new TypeByte();
    public static final TypeChar CHAR = new TypeChar();
    public static final TypeShort SHORT = new TypeShort();
    public static final TypeInt INT = new TypeInt();
    public static final TypeLong LONG = new TypeLong();
    public static final TypeFloat FLOAT = new TypeFloat();
    public static final TypeDouble DOUBLE = new TypeDouble();
    public static final TypeKlass OBJECT = new TypeKlass("java.lang.Object");
    public static final TypeKlass STRING = new TypeKlass("java.lang.String", TypeKlass.FINAL);

    private static final List<Type> TYPES = new ArrayList<>();
    private static final List<Type> BUILTIN_TYPES = new ArrayList<>();
    private static final List<Type> BUILTIN_INT_TYPES = new ArrayList<>();
    private static final List<Type> BUILTIN_FP_TYPES = new ArrayList<>();
    private static final List<TypeKlass> REFERENCE_TYPES = new ArrayList<>();

    static {
        BUILTIN_INT_TYPES.add(BOOLEAN);
        BUILTIN_INT_TYPES.add(BYTE);
        BUILTIN_INT_TYPES.add(CHAR);
        BUILTIN_INT_TYPES.add(SHORT);
        BUILTIN_INT_TYPES.add(INT);
        BUILTIN_INT_TYPES.add(LONG);
        BUILTIN_FP_TYPES.add(FLOAT);
        BUILTIN_FP_TYPES.add(DOUBLE);

        BUILTIN_TYPES.addAll(BUILTIN_INT_TYPES);
        BUILTIN_TYPES.addAll(BUILTIN_FP_TYPES);

        TYPES.addAll(BUILTIN_TYPES);

        if (!ProductionParams.disableArrays.value()) {
            TYPES.addAll(REFERENCE_TYPES);
        }

        STRING.addParent(OBJECT.getName());
        STRING.setParent(OBJECT);
        add(STRING);
        add(OBJECT);
    }

    public static Collection<Type> getAll() {
        return TYPES;
    }

    public static Collection<Type> getBuiltIn() {
        return BUILTIN_TYPES;
    }

    public static Collection<Type> getBuiltInInt() {
        return BUILTIN_INT_TYPES;
    }

    protected static Collection<Type> getBuiltInFP() {
        return BUILTIN_FP_TYPES;
    }

    protected static Collection<TypeKlass> getReferenceTypes() {
        return REFERENCE_TYPES;
    }

    protected static boolean isBuiltInFP(Type t) {
        return BUILTIN_FP_TYPES.contains(t);
    }

    public static boolean isBuiltInInt(Type t) {
        return BUILTIN_INT_TYPES.contains(t);
    }

    public static boolean isBuiltIn(Type t) {
        return isBuiltInInt(t) || isBuiltInFP(t) || t.equals(VOID);
    }

    protected static boolean isIn(Type t) {
        return TYPES.contains(t);
    }

    public static boolean isReferenceType(Type t) {
        return REFERENCE_TYPES.contains(t);
    }

    public static Type find(Type t) {
        int i = TYPES.indexOf(t);
        if (i != -1) {
            return TYPES.get(i);
        }
        return null;
    }

    protected static Type findReferenceType(Type t) {
        int i = REFERENCE_TYPES.indexOf(t);
        if (i != -1) {
            return REFERENCE_TYPES.get(i);
        }
        return null;
    }

    public static Type find(String name) {
        for (Type t : TYPES) {
            if (t.getName().equals(name)) {
                return t;
            }
        }
        return null;
    }

    public static void add(TypeKlass t) {
        REFERENCE_TYPES.add(t);
        TYPES.add(t);
    }

    protected static void remove(Type t) {
        REFERENCE_TYPES.remove(t);
        TYPES.remove(t);
    }

    public static void removeAll() {
        Predicate<? super String> isNotBasic = s -> s.startsWith("Test_");
        Predicate<? super Type> isNotBasicType = t -> isNotBasic.test(t.getName());
        REFERENCE_TYPES.stream()
                       .map(TypeKlass::getChildrenNames)
                       .forEach(l -> l.removeIf(isNotBasic));
        TYPES.removeIf(isNotBasicType);
        REFERENCE_TYPES.removeIf(isNotBasicType);
    }
}
