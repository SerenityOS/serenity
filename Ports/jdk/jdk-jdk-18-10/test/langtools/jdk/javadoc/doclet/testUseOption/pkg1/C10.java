/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

/**
 * An implementor
 *
 */
public class C10 extends UsedClass implements UsedInterface, UsedInterfaceA {

    /**
     * Nothing
     */
    public void doNothing() {}

    /**
     * Me too
     */
    public void doNothingA() {}

    /**
     * returns a collection with type param
     * @return something
     */
    public List<? extends UsedClass> foo(){return null;}

    /**
     * returns and takes type param variants
     * @param <T> yeah
     * @return returns a type param
     */
    public <T extends UsedInterface<? super T>> UsedInterfaceA<T> withTypeParametersOfType(Class<? extends UsedInterface> c){return null;}

    /**
     * returns a type param
     * @param <T> a param
     * @return something
     */
    public <T extends UsedInterface>T[] withReturningTypeParameters(){return null;}

    /**
     * a return a type parameter, as a static method
     * @param <T> a type param
     * @param enumType something
     * @param name something
     * @return a trype param
     */
    public static <T extends UsedInterface<T>> T withReturnVariant(Class<T> enumType, String name){return null;}

    /**
     * another variant of a method returning type parameters
     * @param <T> something
     * @param listenerInterface something
     * @param target something
     * @param action something
     * @return a type param
     */
    public <T> T create(UsedInterfaceA<T> listenerInterface, UsedInterface target, String action){return null;}

    /**
     * input is an array
     * @param elements a vararg
     */
    public void addAll(UsedInterface... elements) {}
}
