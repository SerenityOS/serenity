/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6865265
 * @summary JVM crashes with "missing exception handler" error
 * @author volker.simonis@sap.com
 *
 * @run main/othervm -XX:CompileThreshold=100 -Xbatch -Xss512k
 *      compiler.runtime.StackOverflowBug
 */

package compiler.runtime;

public class StackOverflowBug {

    public static int run() {
        try {
            try {
                return run();
            } catch (Throwable e) {
                // Notice that the class 'Throwable' is NOT resolved by the verifier,
                // because the verifier only checks if 'Throwable' is assignable to
                // 'java.lang.Throwable' and this check succeeds immediately if the two
                // types have equal names (see 'VerificationType::is_assignable_from' which
                // is called from 'ClassVerifier::verify_exception_handler_table').
                // This is strange, because if the two classes have different names,
                // 'is_assignable_from()' calls 'is_reference_assignable_from()' which resolves
                // both classes by calling 'SystemDictionary::resolve_or_fail()'. This call
                // also takes into account the current class loader (i.e. the one which was used
                // to load this class) and would place a corresponding
                // "java.lang.Throwable / current-Classloader" entry into the system dictionary.
                // This would in turn allow C2 to see 'java.lang.Throwable' as "loaded"
                // (see 'Parse::catch_inline_exceptions()') when this method is compiled.
                return 42;
            }
        } finally {
        }
    }

    public static void main(String argv[]) {
        run();
    }
}

/*
  public static int run();
    Code:
       0: invokestatic  #2                  // Method run:()I
       3: istore_0
       4: iload_0
       5: ireturn
       6: astore_0
       7: bipush        42
       9: istore_1
      10: iload_1
      11: ireturn
      12: astore_2
      13: aload_2
      14: athrow
    Exception table:
       from    to  target type
           0     4     6   Class java/lang/Throwable
           0     4    12   any
           6    10    12   any
          12    13    12   any

 */
