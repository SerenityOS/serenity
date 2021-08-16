/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4135104
   @summary If a badly behaved ClassLoader returns null but doesn't
            raise an exception, the VM core dumps.
*/


public class ExceptionHidingLoader extends ClassLoader {

    protected Class findClass(String name) throws ClassNotFoundException {
        return null;
    }

    public static void main(String[] args) throws Exception {
        boolean exception = false;

        try {
            Class.forName("aha", false, new ExceptionHidingLoader());
        } catch (ClassNotFoundException e) {
            /* VM was smart enough to detect the problem and raise an
               exception. */
            exception = true;
        }
        if (!exception) {
            throw new Exception("Bogus loader behavior not being corrected");
        }
    }

}
