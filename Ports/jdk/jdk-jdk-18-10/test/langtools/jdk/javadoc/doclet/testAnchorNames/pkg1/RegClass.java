/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package pkg1;

import java.io.Serializable;
import java.util.Map;

/**
 * @serial This is the serial tag's comment.
 */
public class RegClass implements Serializable {

    /**
     * Normal field in class.
     */
    public String field;

    /**
     * Normal field in class.
     */
    public String method$$;

    /**
     * Filed staring with $.
     */
    public String $field;

    /**
     * Filed staring with underscore.
     */
    public String _field;

    /**
     * Serial field
     * @serial
     */
    public boolean t_e$t;

    /**
     * Field in class with a $ in the name.
     */
    public String fieldInCla$$;

    /**
     * Field name as just an underscore.
     */
    public int _;

    /**
     * Field name as just a $.
     */
    public int $;

    /**
     * Field name with underscore and $.
     */
    public int _$;

    /**
     * Field name with $ and underscore.
     */
    public int $_;

    /**
     * An array.
     */
    public int arr[];

    /**
     * Another array.
     */
    public int[] arr1;

    /**
     * A constant field.
     */
    public static final int S_$$$$$INT = 0;

    /**
     * Another field.
     */
    public DeprMemClass d____mc;

    /**
     * An enum.
     */
    public static enum Te$t_Enum {
        FLD_1,
        $FLD2
    };

    /**
     * A constructor.
     */
    public RegClass(String p, int i) {
    }

    /**
     * Method in Class.
     * @param p a string
     */
    public void _methodInClass(String p) {
    }

    /**
     * Method in Class.
     * @param p a string
     * @param i an int
     */
    public void _methodInClas$(String p, int i) {
    }

    /**
     * Method with $ in the name.
     * @param p a string array
     */
    public void methodInCla$s(String[] p) {
    }

    /**
     * Method with D[] as a parameter.
     * @param p an array of D
     */
    public void methodD(D[] p) {
    }

    /**
     * Method with $A as a parameter.
     * @param p an object of $A
     */
    public void methodD($A p) {
    }

    /**
     * Serial test.
     * @serialData This is a serial data comment.
     * @return null
     */
    protected Object $readResolve(){return null;}

    /**
     * Simple method.
     */
    public void method() {}

    /**
     * Generics.
     */
    public static <A> void foo(Map<A, Map<A, A>> map) {}

    /**
     * A nested class.
     */
    public class _NestedClas$ {}

    /**
     * Nested class D.
     */
    class D {}

    /**
     * Nested class $A.
     */
    class $A {}
}
