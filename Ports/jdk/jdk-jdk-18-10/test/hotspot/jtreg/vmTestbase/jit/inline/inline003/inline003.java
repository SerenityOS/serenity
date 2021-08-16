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
 * @summary converted from VM Testbase jit/inline/inline003.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.inline.inline003.inline003
 */

package jit.inline.inline003;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import nsk.share.TestFailure;
import nsk.share.GoldChecker;

class inline003_1 {
        final protected static int[] inlineClassObj1 = { 1, 2 };
        protected static int[] inlineClassObj2 = { 3, 4};
        final protected static int[] trash = { 10, 11};
}

public class inline003 extends inline003_1 {
        public static final GoldChecker goldChecker = new GoldChecker( "inline003" );

        final private static int ITERS=4;
        private static int pFlag = 0;
        private static int[] myIters = new int[15];

        final private static int[] inlineClassObj3 = { 4, 5 };
        private static int[] inlineClassObj4 = { 5, 6 };

        final private static int[] inlineObj5 = { 6, 7 };
        final private static int[] inlineObj6 = { 7, 8 };
        final private static int[] inlineObj7 = { 8, 9 };
        final private static int[] inlineObj8 = { 9, 10 };

        private Object inlineInstanceObj1 = inline003.inlineObj5;
        private Object inlineInstanceObj2 = inline003.inlineObj6;
        private Object inlineInstanceObj3 = inline003.inlineObj7;
        private Object inlineInstanceObj4 = inline003.inlineObj8;

        private final static Object MPAT_Const4_fs00() {
                return null;
        }
        private final static Object MPAT_GetStatic4_fsc1() {
                return inlineClassObj1;
        }
        private final static Object MPAT_GetStatic4_fsc2() {
                return inlineClassObj2;
        }
        private final static Object MPAT_GetStatic4_fsc3() {
                return inlineClassObj3;
        }
        private final static Object MPAT_GetStatic4_fsc4() {
                return inlineClassObj4;
        }
        private final Object MPAT_GetStatic4_fnc1() {
                return inlineClassObj1;
        }
        private final Object MPAT_GetStatic4_fnc2() {
                return inlineClassObj2;
        }
        private final Object MPAT_GetStatic4_fnc3() {
                return inlineClassObj3;
        }
        private final Object MPAT_GetStatic4_fnc4() {
                return inlineClassObj4;
        }

        private final Object MPAT_Const4_fn00() {
                return null;
        }
        private final Object MPAT_GetField4_fni1() {
                return inlineInstanceObj1;
        }
        private final Object MPAT_GetField4_fni2() {
                return inlineInstanceObj2;
        }
        private final Object MPAT_GetField4_fni3() {
                return inlineInstanceObj3;
        }
        private final Object MPAT_GetField4_fni4() {
                return inlineInstanceObj4;
        }
        private final Object MPAT_Handle_fi() {
                return this;
        }


        private void runFinals () {
                int jcount=0;
                Object locObj = null;
                if (pFlag==2) inline003.goldChecker.print("MPAT_Const4_fs00");
                try {
                        jcount = 0;
                        for (jcount=0; jcount<ITERS; jcount++) {
                                locObj = trash;
                                locObj = MPAT_Const4_fs00();
                                if (locObj != null) break;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline003.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[0] = jcount;
                        if (pFlag==2) inline003.goldChecker.println();
                }
                if (pFlag==2) inline003.goldChecker.print("MPAT_GetStatic4_fsc1");
                try {
                        jcount = 0;
                        for (jcount=0; jcount<ITERS; jcount++) {
                                locObj = trash;
                                locObj = MPAT_GetStatic4_fsc1();
                                if (locObj != inlineClassObj1) break;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline003.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[1] = jcount;
                        if (pFlag==2) inline003.goldChecker.println();
                }
                if (pFlag==2) inline003.goldChecker.print("MPAT_GetStatic4_fsc2");
                try {
                        jcount = 0;
                        for (jcount=0; jcount<ITERS; jcount++) {
                                locObj = trash;
                                locObj = MPAT_GetStatic4_fsc2();
                                if (locObj != inlineClassObj2) break;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline003.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[2] = jcount;
                        if (pFlag==2) inline003.goldChecker.println();
                }
                if (pFlag==2) inline003.goldChecker.print("MPAT_GetStatic4_fsc3");
                try {
                        jcount = 0;
                        for (jcount=0; jcount<ITERS; jcount++) {
                                locObj = trash;
                                locObj = MPAT_GetStatic4_fsc3();
                                if (locObj != inlineClassObj3) break;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline003.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[3] = jcount;
                        if (pFlag==2) inline003.goldChecker.println();
                }
                if (pFlag==2) inline003.goldChecker.print("MPAT_GetStatic4_fsc4");
                try {
                        jcount = 0;
                        for (jcount=0; jcount<ITERS; jcount++) {
                                locObj = trash;
                                locObj = MPAT_GetStatic4_fsc4();
                                if (locObj != inlineClassObj4) break;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline003.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[4] = jcount;
                        if (pFlag==2) inline003.goldChecker.println();
                }
                if (pFlag==2) inline003.goldChecker.print("MPAT_GetStatic4_fnc1");
                try {
                        jcount = 0;
                        for (jcount=0; jcount<ITERS; jcount++) {
                                locObj = trash;
                                locObj = MPAT_GetStatic4_fnc1();
                                if (locObj != inlineClassObj1) break;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline003.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[5] = jcount;
                        if (pFlag==2) inline003.goldChecker.println();
                }
                if (pFlag==2) inline003.goldChecker.print("MPAT_GetStatic4_fnc2");
                try {
                        jcount = 0;
                        for (jcount=0; jcount<ITERS; jcount++) {
                                locObj = trash;
                                locObj = MPAT_GetStatic4_fnc2();
                                if (locObj != inlineClassObj2) break;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline003.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[6] = jcount;
                        if (pFlag==2) inline003.goldChecker.println();
                }
                if (pFlag==2) inline003.goldChecker.print("MPAT_GetStatic4_fnc3");
                try {
                        jcount = 0;
                        for (jcount=0; jcount<ITERS; jcount++) {
                                locObj = trash;
                                locObj = MPAT_GetStatic4_fnc3();
                                if (locObj != inlineClassObj3) break;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline003.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[7] = jcount;
                        if (pFlag==2) inline003.goldChecker.println();
                }
                if (pFlag==2) inline003.goldChecker.print("MPAT_GetStatic4_fnc4");
                try {
                        jcount = 0;
                        for (jcount=0; jcount<ITERS; jcount++) {
                                locObj = trash;
                                locObj = MPAT_GetStatic4_fnc4();
                                if (locObj != inlineClassObj4) break;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline003.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[8] = jcount;
                        if (pFlag==2) inline003.goldChecker.println();
                }
                if (pFlag==2) inline003.goldChecker.print("MPAT_Const4_fn00");
                try {
                        jcount = 0;
                        for (jcount=0; jcount<ITERS; jcount++) {
                                locObj = trash;
                                locObj = MPAT_Const4_fn00();
                                if (locObj != null) break;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline003.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[9] = jcount;
                        if (pFlag==2) inline003.goldChecker.println();
                }
                if (pFlag==2) inline003.goldChecker.print("MPAT_GetField4_fni1");
                try {
                        jcount = 0;
                        for (jcount=0; jcount<ITERS; jcount++) {
                                locObj = trash;
                                locObj = MPAT_GetField4_fni1();
                                if (locObj != inlineInstanceObj1) break;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline003.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[10] = jcount;
                        if (pFlag==2) inline003.goldChecker.println();
                }
                if (pFlag==2) inline003.goldChecker.print("MPAT_GetField4_fni2");
                try {
                        jcount = 0;
                        for (jcount=0; jcount<ITERS; jcount++) {
                                locObj = trash;
                                locObj = MPAT_GetField4_fni2();
                                if (locObj != inlineInstanceObj2) break;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline003.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[11] = jcount;
                        if (pFlag==2) inline003.goldChecker.println();
                }
                if (pFlag==2) inline003.goldChecker.print("MPAT_GetField4_fni3");
                try {
                        jcount = 0;
                        for (jcount=0; jcount<ITERS; jcount++) {
                                locObj = trash;
                                locObj = MPAT_GetField4_fni3();
                                if (locObj != inlineInstanceObj3) break;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline003.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[12] = jcount;
                        if (pFlag==2) inline003.goldChecker.println();
                }
                if (pFlag==2) inline003.goldChecker.print("MPAT_GetField4_fni4");
                try {
                        jcount = 0;
                        for (jcount=0; jcount<ITERS; jcount++) {
                                locObj = trash;
                                locObj = MPAT_GetField4_fni4();
                                if (locObj != inlineInstanceObj4) break;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline003.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[13] = jcount;
                        if (pFlag==2) inline003.goldChecker.println();
                }
                if (pFlag==2) inline003.goldChecker.print("MPAT_Handle_fi");
                try {
                        jcount = 0;
                        for (jcount=0; jcount<ITERS; jcount++) {
                                locObj = trash;
                                locObj = MPAT_Handle_fi();
                                if (locObj != this) break;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline003.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[14] = jcount;
                        if (pFlag==2) inline003.goldChecker.println();
                }
        }


        public static int result() {
                String[] label = {
                        "MPAT_Const4_fs00",
                        "MPAT_GetStatic4_fsc1",
                        "MPAT_GetStatic4_fsc2",
                        "MPAT_GetStatic4_fsc3",
                        "MPAT_GetStatic4_fsc4",
                        "MPAT_GetStatic4_fnc1",
                        "MPAT_GetStatic4_fnc2",
                        "MPAT_GetStatic4_fnc3",
                        "MPAT_GetStatic4_fnc4",
                        "MPAT_Const4_fn00",
                        "MPAT_GetField4_fni1",
                        "MPAT_GetField4_fni2",
                        "MPAT_GetField4_fni3",
                        "MPAT_GetField4_fni4",
                        "MPAT_Handle_fi" };
                ByteArrayOutputStream baos = new ByteArrayOutputStream(80);
                byte[] ba = new byte[80];
                String s = null;
                int ierr = 0;
                for (int icount=0; icount < 15; icount++) {
                        if (pFlag >= 1) {
                                baos.reset();
                                try {
                                        for (int i=0; i<ba.length; i++) ba[i]=(byte)' ';
                                        for (int i=0; i<label[icount].length(); i++)
                                                ba[i]=(byte)label[icount].charAt(i);
                                        baos.write(ba,0,27);
                                }
                                catch (IndexOutOfBoundsException e) {
                                }
                                try {
                                        for (int i=0; i<ba.length; i++) ba[i]=(byte)' ';
                                        baos.write(ba,0,3);
                                        s = Long.toString(myIters[icount]);
                                        for (int i=0; i<s.length(); i++)
                                                ba[i]=(byte)s.charAt(i);
                                        baos.write(ba,0,7);
                                }
                                catch (IndexOutOfBoundsException e) {
                                }
                                inline003.goldChecker.print(baos.toString());
                                inline003.goldChecker.println();
                        }
                        if (myIters[icount] != ITERS) ierr=1;
                }
                return ierr;
        }

        public static void main( String args[] ) {
                int ierr=0;
                inline003 myInline_f = new inline003();


                if (args.length > 0 && args[0].equals("-help")) {
                        inline003.goldChecker.println("usage: java inline003 [-print]");
                        inline003.goldChecker.check();
                        return;
                }
                pFlag = 1;
                if (args.length > 0 && args[0].equals("-print"))
                        pFlag = 2;
                for (int ii=0; ii<15; ii++) myIters[ii]=ITERS;
                if (pFlag==2) inline003.goldChecker.println("inline003");

                                /* Give the JIT an initial look at all the methods */
                myInline_f.runFinals();
                ierr = inline003.result();
                if (ierr == 0) {
                        inline003.goldChecker.println("PASSED.");
                }
                else {
                        inline003.goldChecker.println("FAILED. (ierr = " + ierr + ")");
                }
                inline003.goldChecker.check();
        }
}
