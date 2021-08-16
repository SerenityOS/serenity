/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.util.*;
import java.io.*;

/*
 * @test
 * @summary compiler accepts all values
 * @author Mahmood Ali
 * @author Yuri Gaevsky
 * @compile TargetTypes.java
 */

@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface A {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface B {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface C {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface D {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface E {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface F {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface G {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface H {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface I {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface J {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface K {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface L {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface M {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface N {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface O {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface P {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface Q {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface R {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface S {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface U {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface V {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface W {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface X {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface Y {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface Z {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface AA {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface AB {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface AC {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface AD {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface AE {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface AF {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface AG {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface AH {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface AI {}
@Target({TYPE_USE, TYPE_PARAMETER, TYPE}) @Retention(RetentionPolicy.RUNTIME) @interface AJ {}

/** wildcard bound */
class T0x1C {
    void m0x1C(List<? extends @A String> lst) {}
}

/** wildcard bound generic/array */
class T0x1D<T> {
    void m0x1D(List<? extends @B List<int[]>> lst) {}
}

/** typecast */
class T0x00 {
    void m0x00(Long l1) {
        Object l2 = (@C Long) l1;
    }
}

/** typecast generic/array */
class T0x01<T> {
    void m0x01(List<T> list) {
        List<T> l = (List<@D T>) list;
    }
}

/** instanceof */
class T0x02 {
    boolean m0x02(String s) {
        return (s instanceof @E String);
    }
}

/** object creation (new) */
class T0x04 {
    void m0x04() {
        new @F ArrayList<String>();
    }
}

/** local variable */
class T0x08 {
    void m0x08() {
      @G String s = null;
    }
}

/** method parameter generic/array */
class T0x0D {
    void m0x0D(HashMap<@H Object, List<@I List<@J Class>>> s1) {}
}

/** method receiver */
class T0x06 {
    void m0x06(@K T0x06 this) {}
}

/** method return type generic/array */
class T0x0B {
    Class<@L Object> m0x0B() { return null; }
}

/** field generic/array */
class T0x0F {
    HashMap<@M Object, @N Object> c1;
}

/** method type parameter */
class T0x20<T, U> {
    <@O T, @P U> void m0x20() {}
}

/** class type parameter */
class T0x22<@Q T, @R U> {
}

/** class type parameter bound */
class T0x10<T extends @S Object> {
}

/** method type parameter bound */
class T0x12<T> {
    <T extends @A Object> void m0x12() {}
}

/** class type parameter bound generic/array */
class T0x11<T extends List<@U T>> {
}


/** method type parameter bound generic/array */
class T0x13 {
    static <T extends Comparable<@V T>> T m0x13() {
        return null;
    }
}

/** class extends/implements generic/array */
class T0x15<T> extends ArrayList<@W T> {
}

/** type test (instanceof) generic/array */
class T0x03<T> {
    void m0x03(T typeObj, Object obj) {
        boolean ok = obj instanceof String @X [];
    }
}

/** object creation (new) generic/array */
class T0x05<T> {
    void m0x05() {
        new ArrayList<@Y T>();
    }
}

/** local variable generic/array */
class T0x09<T> {
    void g() {
        List<@Z String> l = null;
    }

    void a() {
        String @AA [] as = null;
    }
}

/** type argument in constructor call generic/array */
class T0x19 {
    <T> T0x19() {}

    void g() {
       new <List<@AB String>> T0x19();
    }
}

/** type argument in method call generic/array */
class T0x1B<T> {
    void m0x1B() {
        Collections.<T @AC []>emptyList();
    }
}

/** type argument in constructor call */
class T0x18<T> {
    <T> T0x18() {}

    void m() {
        new <@AD Integer> T0x18();
    }
}

/** type argument in method call */
class T0x1A<T,U> {
    public static <T, U> T m() { return null; }
    static void m0x1A() {
        T0x1A.<@AE Integer, @AF Short>m();
    }
}

/** class extends/implements */
class T0x14 extends @AG Object implements @AH Serializable, @AI Cloneable {
}

/** exception type in throws */
class T0x16 {
    void m0x16() throws @AJ Exception {}
}
