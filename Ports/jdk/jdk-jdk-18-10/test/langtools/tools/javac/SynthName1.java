/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4462714
 * @summary using of synthetic names in local class causes ClassFormatError
 * @author gafter
 *
 * @compile SynthName1.java
 * @run main SynthName1
 */

import java.io.PrintStream;

public class SynthName1 {
    public static void main(String args[]) {
        run(args, System.out);
    }
    public static void run(String args[],PrintStream out) {
        int  res1, res2;
        Intf ob = meth(1, 2);

        res1 = ob.getFirst();
        res2 = ob.getSecond();

        if ( res1 == 1 && res2 == 2 )
            return;
        out.println("Failed:  res1=" + res1 + ", res2=" + res2);
        throw new Error("test failed!");
    }
    interface Intf {
        int getFirst();
        int getSecond();
    }
    static Intf meth(final int prm1, final int zzz) {
        class InnClass implements Intf {
            Object val$prm1 = null;
            // int    val$zzz  = 10;
            Object locVar;
            public int getFirst() {
                locVar = val$prm1;
                return prm1;
            }
            public int getSecond() {
                return zzz;
            }
        }
        return new InnClass();
    }
}
