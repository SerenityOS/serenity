/*
 * Copyright (c) 1999, 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4266172 4054256
 * @summary Verify that instance initializer of inner class can throw checked exception.
 *
 * (Was Verify that constructor for an anonymous class cannot throw exceptions that its
 * superclass constructor cannot.  This restriction has been lifted -- 4054256.)
 *
 * @author maddox
 *
 * @run compile AnonInnerException_2.java
 */

class AnonInnerException_2 {

    boolean done = true;

    void foo() throws Exception {

        AnonInnerExceptionAux y =
            new AnonInnerExceptionAux() {
              // instance initializer
              {
                  if (done)
                      throw new java.io.IOException();
              }
        };
    }
}

class AnonInnerExceptionAux {
    AnonInnerExceptionAux() throws MyException {}
}

class MyException extends Exception {}
