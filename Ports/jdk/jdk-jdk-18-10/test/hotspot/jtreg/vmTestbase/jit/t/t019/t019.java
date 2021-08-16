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
 * @summary converted from VM Testbase jit/t/t019.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t019.t019
 */

package jit.t.t019;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// opc_baload, opc_bastore,
// opc_caload, opc_castore,
// opc_saload, opc_sastore

public class t019
{
    public static final GoldChecker goldChecker = new GoldChecker( "t019" );

    public static void main(String argv[])
    {
        byte b[] = new byte[4];
        char c[] = new char[4];
        short s[] = new short[4];

        b[0] = 0; b[1] = 127; b[2] = (byte) 128; b[3] = (byte) 255;
        for(int i=0; i<4; i+=1)
            t019.goldChecker.println("b[" + i + "] == " + b[i]);

        t019.goldChecker.println();
        c[0] = 0; c[1] = 32767; c[2] = 32768; c[3] = 65535;
        for(int i=0; i<4; i+=1)
            t019.goldChecker.println("(int) c[" + i + "] == " + (int) c[i]);

        t019.goldChecker.println();
        s[0] = 0; s[1] = 32767; s[2] = (short) 32768; s[3] = (short) 65535;
        for(int i=0; i<4; i+=1)
            t019.goldChecker.println("s[" + i + "] == " + s[i]);
        t019.goldChecker.check();
    }
}
