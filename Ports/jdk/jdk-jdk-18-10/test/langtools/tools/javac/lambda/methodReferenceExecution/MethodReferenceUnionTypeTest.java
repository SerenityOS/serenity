/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8191655
 * @summary LambdaConversionException: Invalid receiver type interface; not a subtype of implementation type interface
 * @run main MethodReferenceUnionTypeTest
 */

import java.util.function.Consumer;
public class MethodReferenceUnionTypeTest {
   static String blah = "NONE";
   <T> void forAll(Consumer<T> consumer, T value) { consumer.accept(value); }

   public void secondTest() {
       try {
          throwing();
        } catch (A | B ex) {
            forAll(Picture::draw, ex);
        }
   }

   void throwing() throws A, B { throw new A();}

   interface Shape { void draw(); }
   interface Marker { }
   interface Picture { void draw();  }

   class A extends Exception implements Marker, Picture { public void draw() { blah = "A"; }}
   class B extends Exception implements Marker, Picture, Shape { public void draw() {}}

   public static void main(String[] args) {
       new MethodReferenceUnionTypeTest().secondTest();
       if (!blah.equals("A"))
            throw new AssertionError("Incorrect output");
   }
}