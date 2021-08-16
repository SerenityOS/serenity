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
import java.util.Collection;
import java.util.List;
public class A {

    /**
     * Constructor
     * @param a class
     */
    public A(UsedClass a) {}

    /**
     * test inner classes
     */
    public static class C {

        /**
         * inner classes constructor
         * @param u a param
         * @param array a param
         */
        public C(UsedClass u, Object[] array){}

        /**
         * inner classes constructor
         * @param u a param
         * @param collection a param
         */
        public C(UsedClass u, Collection collection){}

        /**
         * inner classes constructor
         * @param u a param
         * @param list a param
         */
        public C(UsedClass u, List list){}
    }
}
