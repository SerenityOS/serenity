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

/*
 * @test
 * @bug 4040456
 * @summary Test biginteger constructor with i18n string
 */

import java.math.*;

/**
 * This class tests to see if creating a biginteger with an
 * unicode japanese zero and one succeeds
 *
 */
public class UnicodeConstructor {

      public static void main(String args[]) {

         try {
             // the code for japanese zero
             BigInteger b1 = new BigInteger("\uff10");
             System.err.println(b1.toString());

             // Japanese 1010
             BigInteger b2 = new BigInteger("\uff11\uff10\uff11\uff10");
             System.err.println(b2.toString());
          }
          catch (ArrayIndexOutOfBoundsException e) {
              throw new RuntimeException(
                       "BigInteger is not accepting unicode initializers.");
          }

     }

}
