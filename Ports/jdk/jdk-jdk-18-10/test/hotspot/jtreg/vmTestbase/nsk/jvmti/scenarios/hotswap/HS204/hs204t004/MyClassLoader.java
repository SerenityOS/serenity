/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jvmti.scenarios.hotswap.HS204.hs204t004;
/*
   A dummy class loader for keeping a break point at loadClass method of this class.
*/
public class MyClassLoader extends ClassLoader {
        public String name;
        public MyClassLoader(ClassLoader loader) {
                super(loader);
                name = "nsk.jvmti.scenarios.hotswap.HS204.hs204t004.MyClassLoader";
        }
        public Class loadClass(String className)  throws ClassNotFoundException {
                Class cls = super.loadClass(className);
        // Delegate that back to the parent.
                System.out.println("JAVA->CLASSLOADER In Side the class Loader.. ");
                return cls;
        }
        public String toString() {
                return "MyClassLoader";
        }
}
