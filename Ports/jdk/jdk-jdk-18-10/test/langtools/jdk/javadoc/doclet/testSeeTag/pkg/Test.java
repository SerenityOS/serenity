/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package pkg;
import java.util.List;

/** @see List */
public class Test {

   /**
    * Testing different combos of see tags, including some weird formatting.
    * @see InnerOne#foo()
    * @see InnerOne#bar(
    *                   Object
    *                   )
    * @see <a href="http://docs.oracle.com/javase/7/docs/technotes/tools/windows/javadoc.html#see">Javadoc</a>
    * @see InnerOne#baz(float priority) something
    * @see InnerOne#format( java .lang.String  , java.  lang.Object ... )
    */
    public void foo() {}

    public static class InnerOne {
        /**
         * The rains come down in africa.
         */
        public void foo() {}

        /**
         * Killimanjaro.
         * @param o
         * @see baz
         */
        public void bar(Object o) {}

        /**
         * Of course the serengeti.
         * @param GravitationalConstant
         */
        public void baz(float GravitationalConstant) {}

        /**
         * Test for multiple args and varargs.
         */
        public static String format(String s, Object... args) {
            return String.format(s, args);
        }
    }
}

