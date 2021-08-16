/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdb.unwatch.unwatch002;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class unwatch002a {
    /* TEST DEPENDANT VARIABLES AND CONSTANTS */
    static final String PACKAGE_NAME = "nsk.jdb.unwatch.unwatch002";

    public static void main(String args[]) {
       unwatch002a _unwatch002a = new unwatch002a();
       System.exit(unwatch002.JCK_STATUS_BASE + _unwatch002a.runIt(args, System.out));
    }

    static void breakHere () {}

    public int runIt(String args[], PrintStream out) {
        JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        breakHere();
        updateFields(false);
        fields.updateFields(false);

        breakHere();
        updateFields(false);
        fields.updateFields(false);

        log.display("Debuggee PASSED");
        return unwatch002.PASSED;
    }

    static    boolean fS0, fS1[], fS2[][];
    static    Boolean FS0, FS1[], FS2[][];

    interface Inter {}
    Inter     I0, I1[], I2[][];

    // assign new values to fields
    void updateFields(boolean flag) {

        fS0 = flag ? fS0 : false;
        fS1 = flag ? fS1 : new boolean[] {fS0};
        fS2 = flag ? fS2 : new boolean[][] {fS1};

        FS0 = flag ? FS0 : Boolean.valueOf(false);
        FS1 = flag ? FS1 : new Boolean[] {FS0};
        FS2 = flag ? FS2 : new Boolean[][] {FS1};

        I0  = flag ? I0  : new CheckedFields();
        I1  = flag ? I1  : new CheckedFields[]   {new CheckedFields()};
        I2  = flag ? I2  : new CheckedFields[][] {new CheckedFields[] {new CheckedFields()}};
    }

    class CheckedFields implements Inter {

        private   byte    fP0, fP1[], fP2[][];
        public    char    fU0, fU1[], fU2[][];
        protected double  fR0, fR1[], fR2[][];
        transient float   fT0, fT1[], fT2[][];
        volatile  long    fV0, fV1[], fV2[][];

        private   Byte      FP0, FP1[], FP2[][];
        public    Character FU0, FU1[], FU2[][];
        protected Double    FR0, FR1[], FR2[][];
        transient Float     FT0, FT1[], FT2[][];
        volatile  Long      FV0, FV1[], FV2[][];

        // assign new values to fields
        void updateFields(boolean flag) {

            fP0 = flag ? fP0 : Byte.MIN_VALUE ;
            fU0 = flag ? fU0 : Character.MIN_VALUE;
            fR0 = flag ? fR0 : Double.MIN_VALUE;
            fT0 = flag ? fT0 : Float.MIN_VALUE;
            fV0 = flag ? fV0 : Integer.MIN_VALUE;

            FP0 = flag ? FP0 : Byte.valueOf(Byte.MIN_VALUE) ;
            FU0 = flag ? FU0 : Character.valueOf(Character.MIN_VALUE);
            FR0 = flag ? FR0 : Double.valueOf(Double.MIN_VALUE);
            FT0 = flag ? FT0 : Float.valueOf(Float.MIN_VALUE);
            FV0 = flag ? FV0 : Long.valueOf(Long.MIN_VALUE);

            fP1 = flag ? fP1 : new byte[] {fP0};
            fP2 = flag ? fP2 : new byte[][] {fP1};
            fU1 = flag ? fU1 : new char[] {fU0};
            fU2 = flag ? fU2 : new char[][] {fU1};
            fR1 = flag ? fR1 : new double[] {fR0};
            fR2 = flag ? fR2 : new double[][] {fR1};
            fT1 = flag ? fT1 : new float[] {fT0};
            fT2 = flag ? fT2 : new float[][] {fT1};
            fV1 = flag ? fV1 : new long[] {fV0};
            fV2 = flag ? fV2 : new long[][] {fV1};

            FP1 = flag ? FP1 : new Byte[] {FP0};
            FP2 = flag ? FP2 : new Byte[][] {FP1};
            FU1 = flag ? FU1 : new Character[] {FU0};
            FU2 = flag ? FU2 : new Character[][] {FU1};
            FR1 = flag ? FR1 : new Double[] {FR0};
            FR2 = flag ? FR2 : new Double[][] {FR1};
            FT1 = flag ? FT1 : new Float[] {FT0};
            FT2 = flag ? FT2 : new Float[][] {FT1};
            FV1 = flag ? FV1 : new Long[] {FV0};
            FV2 = flag ? FV2 : new Long[][] {FV1};
        }
    }

    CheckedFields fields;

    public unwatch002a() {
        fields = new CheckedFields();
    }
}
