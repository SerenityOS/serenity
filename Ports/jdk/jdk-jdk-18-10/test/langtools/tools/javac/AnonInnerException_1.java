/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4266172
 * @summary Verify that constructor for an anonymous class can throw
 * exceptions if its superclass can.
 * @author maddox
 *
 * @run compile AnonInnerException_1.java
 */

class AnonInnerException_1 {

    boolean done = true;

    void foo() throws Exception {

        AnonInnerExceptionAux x =
            new AnonInnerExceptionAux(){};

        /**** This related case should pass, but does not!
        AnonInnerExceptionAux y =
            new AnonInnerExceptionAux() {
              // instance initializer
              {
                  if (done)
                      throw new Exception();
                  if (done)
                      throw new MyException();
              }
        };
        ****/
    }
}

class AnonInnerExceptionAux {
    AnonInnerExceptionAux() throws Exception {}
}

class MyException extends Exception {}
