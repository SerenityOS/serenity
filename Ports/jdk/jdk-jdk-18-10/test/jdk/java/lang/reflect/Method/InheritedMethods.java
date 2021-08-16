/*
 * Copyright (c) 2002, 2011, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4471738
   @summary Failure to properly traverse class hierarchy in Class.getMethod()
*/

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public class InheritedMethods {
    public static void main(String[] args) throws Exception { new InheritedMethods(); }
    InheritedMethods() throws Exception {
        Class c = Foo.class;
        Method m = c.getMethod("removeAll", new Class[] { Collection.class });
        if (m.getDeclaringClass() != java.util.List.class) {
          throw new RuntimeException("TEST FAILED");
        }
    }
    interface Foo extends List { }
}
