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

/*
 * @test
 * @bug 8012557
 * @summary Check that 8012557 is fixed, that interface lambda
 *          methods are private
 * @author  Robert Field
 * @compile SAM.java
 * @compile A.java
 * @compile B.java
 * @compile C.java
 * @run main PrivateLambdas
 *
 * Unless the lambda methods are private, this will fail with:
 *  AbstractMethodError:
 *        Conflicting default methods: A.lambda$0 B.lambda$0 C.lambda$0
 */

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

interface X extends A, B, C {
   default String u() { return " "; }
   default String name() {
      return A.super.name() + B.super.name() + C.super.name();
   }
}

public class PrivateLambdas implements X {
   public static void main(String[] args) throws Exception {

      // Check that all the lambda methods are private instance synthetic
      for (Class<?> k : new Class<?>[] { A.class, B.class, C.class }) {
         Method[] methods = k.getDeclaredMethods();
         int lambdaCount = 0;
         for(Method m : methods) {
            if (m.getName().startsWith("lambda$")) {
               ++lambdaCount;
               int mod = m.getModifiers();
               if ((mod & Modifier.PRIVATE) == 0) {
                  throw new Exception("Expected " + m + " to be private");
               }
               if (!m.isSynthetic()) {
                  throw new Exception("Expected " + m + " to be synthetic");
               }
               if ((mod & Modifier.STATIC) != 0) {
                  throw new Exception("Expected " + m + " to be instance method");
               }
            }
         }
         if (lambdaCount == 0) {
            throw new Exception("Expected at least one lambda method");
         }
      }

      /*
       * Unless the lambda methods are private, this will fail with:
       *  AbstractMethodError:
       *        Conflicting default methods: A.lambda$0 B.lambda$0 C.lambda$0
       */
      X x = new PrivateLambdas();
      if (!x.name().equals(" A B C")) {
         throw new Exception("Expected ' A B C' got: " + x.name());
      }
   }
}
