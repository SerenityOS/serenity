/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase jit/misctests/putfield00802.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.misctests.putfield00802.putfield00802
 */

package jit.misctests.putfield00802;

import java.io.PrintStream;
import nsk.share.TestFailure;

public class putfield00802 {

    public double dou_v;
    public static putfield00802 always_null;

    public static void main(String args[]) {
      System.exit(run(args, System.out) + 95/*STATUS_TEMP*/);
    }

    public static int run(String args[], PrintStream out) {
        try {
            always_null.dou_v = 17.0;
            //int i = 1;                        // (1)
        } catch (NullPointerException e) {      // (2)
            return 0/*STATUS_PASSED*/;
        }
        return 2/*STATUS_FAILED*/;
    }
}
