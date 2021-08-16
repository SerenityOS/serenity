/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8058927
 * @summary Make sure array class has the right class loader
 * @run main ShowClassLoader
 */

public class ShowClassLoader {

    public static void main(String[] args) {
        Object[] oa = new Object[0];
        ShowClassLoader[] sa = new ShowClassLoader[0];

        System.out.println("Classloader for Object[] is " + oa.getClass().getClassLoader());
        System.out.println("Classloader for SCL[] is " + sa.getClass().getClassLoader() );

        if (sa.getClass().getClassLoader() == null) {
            throw new RuntimeException("Wrong class loader");
        }
    }
}
