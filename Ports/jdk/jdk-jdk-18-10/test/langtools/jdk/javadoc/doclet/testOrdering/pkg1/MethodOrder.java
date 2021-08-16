/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MethodOrder {
    /**
     * method test for ordering parameters
     * @return UsedClass something
     */
    public UsedClass m(){return null;}
    /**
     * method test for ordering parameters
     * @param i a param
     * @return UsedClass something
     */
    public UsedClass m(int i) {return null;}

    /**
     * method test for ordering parameters
     * @param i1 a param
     * @param i2 a param
     * @return something
     */
    public UsedClass m(int i1, int i2) {return null;}

    /**
     * method test for ordering parameters
     * @param array a param
     * @return something
     */
    public UsedClass m(byte[] array) {return null;}

    /**
     * method test for ordering parameters
     * @param in a param
     * @return something
     */
    public UsedClass m(Integer in) {return null;}

    /**
     * method test for ordering parameters
     * @param i1 a param
     * @param i2 a param
     * @return something
     */
    public UsedClass m(Integer i1, Integer i2) {return null;}

    /**
     * method test for ordering parameters
     * @param i1 a param
     * @param i2 a param
     * @return something
     */
    public UsedClass m(int i1, Integer i2) {return null;}

    /**
     * method test for ordering parameters
     * @param i1 a param
     * @param i2 a param
     * @return something
     */
    public UsedClass m(Integer i1, int i2) {return null;}

    /**
     * method test for ordering parameters
     * @param d a param
     * @return something
     */
    public UsedClass m(double d) {return null;}

    /**
     * method test for ordering parameters
     * @param i1 a param
     * @param i2 a param
     * @return something
     */
    public UsedClass m(double i1, double i2) {return null;}

    /**
     * method test for ordering parameters
     * @param in a param
     * @return something
     */
    public UsedClass m(Double in) {return null;}

    /**
     * method test for ordering parameters
     * @param i1 a param
     * @param i2 a param
     * @return something
     */
    public UsedClass m(Double i1, Double i2) {return null;}

    /**
     * method test for ordering parameters
     * @param i1 a param
     * @param i2 a param
     * @return something
     */
    public UsedClass m(double i1, Double i2) {return null;}

    /**
     * method test for ordering parameters
     * @param l1 param
     * @param xenon param
     * @return something
     */
    public UsedClass m(long l1, Long... xenon) {return null;}

    /**
     * method test for ordering parameters
     * @param l1 param
     * @return something
     */
    public UsedClass m(long l1) {return null;}

    /**
     *  method test for ordering parameters
     * @param l1 param
     * @param l2 param
     * @return something
     */
    public UsedClass m(long l1, Long l2) {return null;}

    /**
     *  method test for ordering parameters
     * @param l1 param
     * @param l2 param
     * @return something
     */
    public UsedClass m(long l1, long l2) {return null;}

    /**
     * method test for ordering parameters
     * @param array a param
     * @return something
     */
    public UsedClass m(Object[] array);

    /**
     * method test for ordering parameters
     * @param arrayarray two dimensional array
     * @return something
     */
    public UsedClass m(Object[][] arrayarray);

    /**
     * method test for ordering parameters
     * @param i1 a param
     * @param i2 a param
     * @return something
     */
    public UsedClass m(Double i1, double i2) {return null;}

    /**
     * method test for ordering parameters
     * @param collection a param
     * @return something
     */
    public UsedClass m(Collection collection) {return null;}

    /**
     * method test for ordering parameters
     * @param list a param
     * @return something
     */
    public UsedClass m(List list) {return null;}

    /**
     * method test for ordering parameters
     * @param collection a param
     * @return something
     */
    public UsedClass m(ArrayList<UsedClass> collection) {return null;}

    /**
     * method test for ordering parameters
     * @param u use a type param
     */
    public void tpm(UsedClass<?> u) {}

    /**
     * method test for ordering parameters
     * @param u1 use a type param
     * @param u2 use a type param
     */
    public void tpm(UsedClass<?> u1, UsedClass<?> u2) {}

    /**
     * method test for ordering parameters
     * @param u use a type param
     * @param array use a type param and an array
     */
    public void tpm(UsedClass<?> u, UsedClass<?>[] array) {}

    /**
     * method test for ordering parameters
     * @param u use type param with extends
     * @param a some string
     */
    public void tpm(UsedClass<? extends UsedClass> u, String a) {}
}
