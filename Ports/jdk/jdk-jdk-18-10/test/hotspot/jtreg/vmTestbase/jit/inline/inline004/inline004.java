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
 * @summary converted from VM Testbase jit/inline/inline004.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.inline.inline004.inline004
 */

package jit.inline.inline004;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

class inline004_1 {
        final protected static int[] inlineObj = { 1, 2 };
        protected static int   inlineInt = 3;           /* Not final */
        protected final Object MPAT_Super() {
                return this;
        }
}

public class inline004 extends inline004_1 {
        public static final GoldChecker goldChecker = new GoldChecker( "inline004" );

        static int pFlag = 0;
        final static int ITERS=5;
        private int i1 = 0;
        Object obj1 = inline004_1.inlineObj;
        Object obj2 = inline004_1.inlineObj;
        Object obj3 = inline004_1.inlineObj;
        static int[] myIters = new int[14];
        static int[] errFlag = new int[14];
        int intTarg1 = 0;
        int intTarg2 = 0;
        Object objTarg1 = null;
        Object objTarg2 = null;

        private final static int MPAT_Const4_fs_0() {
                return 1;
        }
        private final static Object MPAT_Const4_fs_1() {
                return null;
        }
        private final static int MPAT_GetStatic4_fs_0() {
                return inlineInt;
        }
        private final static Object MPAT_GetStatic4_fs_1() {
                return inlineObj;
        }
        private final int MPAT_Const4_fi_0() {
                return 1;
        }
        private final Object MPAT_Const4_fi_1() {
                return null;
        }
        private final int MPAT_GetField4_fi_0() {
                return i1;
        }
        private final Object MPAT_GetField4_fi_1() {
                return obj1;
        }
        private final void MPAT_PutField4_fi_0(int ival) {
                i1 = ival;
                return;
        }
        private final void MPAT_PutField4_fi_1(Object oval) {
                obj1 = oval;
                return;
        }
        private final void MPAT_PutField4Const4_fi() {
                i1 = -1;
                return;
        }
        private final int MPAT_GetStatic4_fi_0() {
                return inlineInt;
        }
        private final Object MPAT_GetStatic4_fi_1() {
                return inlineObj;
        }
        private final Object MPAT_Handle_fi() {
                return this;
        }

        private static int MPAT_Const4_ns_0() {
                return 1;
        }
        private static Object MPAT_Const4_ns_1() {
                return null;
        }
        private static int MPAT_GetStatic4_ns_0() {
                return inlineInt;
        }
        private static Object MPAT_GetStatic4_ns_1() {
                return inlineObj;
        }
        private int MPAT_Const4_ni_0() {
                return 1;
        }
        private Object MPAT_Const4_ni_1() {
                return null;
        }
        private int MPAT_GetField4_ni_0() {
                return i1;
        }
        private Object MPAT_GetField4_ni_1() {
                return obj1;
        }
        private void MPAT_PutField4_ni_0(int ival) {
                i1 = ival;
                return;
        }
        private void MPAT_PutField4_ni_1(Object oval) {
                obj1 = oval;
                return;
        }
        private void MPAT_PutField4Const4_ni() {
                i1 = -1;
                return;
        }
        private int MPAT_GetStatic4_ni_0() {
                return inlineInt;
        }
        private Object MPAT_GetStatic4_ni_1() {
                return inlineObj;
        }
        private Object MPAT_Handle_ni() {
                return this;
        }

        private void runFinals () {
                int jcount = 0;
                int icount = 0;

                if (pFlag==2) inline004.goldChecker.print("MPAT_Const4_fs_0");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                intTarg1 = MPAT_Const4_fs_0();
                                intTarg2 = MPAT_Const4_ns_0();
                                if (intTarg1 != intTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline004.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline004.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline004.goldChecker.print("MPAT_Const4_fs_1");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                objTarg2 = MPAT_Const4_fs_1();
                                objTarg1 = MPAT_Const4_ns_1();
                                if (objTarg2 != objTarg1) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline004.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline004.goldChecker.println();
                }
                icount++;


                if (pFlag==2) inline004.goldChecker.print("MPAT_GetStatic4_fs_0");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                intTarg2 = MPAT_GetStatic4_fs_0();
                                intTarg1 = MPAT_GetStatic4_ns_0();
                                if (intTarg1 != intTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline004.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline004.goldChecker.println();
                }
                icount++;


                if (pFlag==2) inline004.goldChecker.print("MPAT_GetStatic4_fs_1");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                objTarg2 = MPAT_GetStatic4_fs_1();
                                objTarg1 = MPAT_GetStatic4_ns_1();
                                if (objTarg1 != objTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline004.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline004.goldChecker.println();
                }
                icount++;


                /* Check inlining of instance methods */
                if (pFlag==2) inline004.goldChecker.print("MPAT_Const4_fi_0");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                intTarg1 = MPAT_Const4_fi_0();
                                intTarg2 = MPAT_Const4_ni_0();
                                if (intTarg1 != intTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline004.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline004.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline004.goldChecker.print("MPAT_Const4_fi_1");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                objTarg1 = MPAT_Const4_fi_1();
                                objTarg2 = MPAT_Const4_ni_1();
                                if (objTarg1 != objTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline004.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline004.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline004.goldChecker.print("MPAT_GetStatic4_fi_0");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                intTarg2 = MPAT_GetStatic4_fi_0();
                                intTarg1 = MPAT_GetStatic4_ni_0();
                                if (intTarg1 != intTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline004.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline004.goldChecker.println();
                }
                if (pFlag==2) inline004.goldChecker.print("MPAT_GetStatic4_fi_1");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                objTarg2 = MPAT_GetStatic4_fi_1();
                                objTarg1 = MPAT_GetStatic4_ni_1();
                                if (objTarg1 != objTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline004.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline004.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline004.goldChecker.print("MPAT_GetField4_fi_0");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                intTarg1 = MPAT_GetField4_fi_0();
                                intTarg2 = MPAT_GetField4_ni_0();
                                if (intTarg1 != intTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline004.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline004.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline004.goldChecker.print("MPAT_GetField4_fi_1");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                objTarg2 = MPAT_GetField4_fi_1();
                                objTarg1 = MPAT_GetField4_ni_1();
                                if (objTarg1 != objTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline004.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline004.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline004.goldChecker.print("MPAT_PutField4_fi_0");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                MPAT_PutField4_fi_0(10);
                                intTarg1 = MPAT_GetField4_ni_0();
                                if (intTarg1 != 10) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline004.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline004.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline004.goldChecker.print("MPAT_PutField4_fi_1");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                MPAT_PutField4_fi_1("String1");
                                objTarg1 = MPAT_GetField4_fi_1();
                                if (objTarg1 != "String1") errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline004.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline004.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline004.goldChecker.print("MPAT_PutField4Const4_fi");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                MPAT_PutField4Const4_fi();
                                intTarg2 = MPAT_GetField4_ni_0();
                                if (intTarg2 != -1) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline004.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (pFlag==2) inline004.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline004.goldChecker.print("MPAT_Handle_fi");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                objTarg1 = MPAT_Handle_fi();
                                objTarg2 = MPAT_Handle_ni();
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline004.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline004.goldChecker.println();
                }
                icount++;
        }


        public static int result() {
                int ierr = 0;
                String state = null;
                ByteArrayOutputStream baos = new ByteArrayOutputStream(80);
                byte[] ba = new byte[80];
                String s = null;

                String[] label = new String[14];
                label[0]  = new String("Const4 class method (int)       ");
                label[1]  = new String("Const4 class method (ref)       ");
                label[2]  = new String("GetStatic4 class method (int)   ");
                label[3]  = new String("GetStatic4 class method (ref)   ");
                label[4]  = new String("Const4 instance method (int)    ");
                label[5]  = new String("Const4 instance method (ref)    ");
                label[6]  = new String("GetStatic4 instance method (int)");
                label[7]  = new String("GetStatic4 instance method (ref)");
                label[8] = new String("GetField4 instance method (int) ");
                label[9] = new String("GetField4 instance method (ref) ");
                label[10] = new String("PutField4 instance method (int) ");
                label[11] = new String("PutField4 instance method (ref) ");
                label[12] = new String("PutField4Const4 instance method ");
                label[13] = new String("Handle instance method          ");
                                                                // Report headers
                baos.reset();
                try {
                        for (int i=0; i<ba.length; i++) ba[i]=(byte)' ';
                        s = "Pattern";
                        for (int i=0; i<s.length(); i++)
                                ba[i]=(byte)s.charAt(i);
                        baos.write(ba,0,32);
                }
                catch (IndexOutOfBoundsException e){
                }
                try {
                        for (int i=0; i<ba.length; i++) ba[i]=(byte)' ';
                        baos.write(ba,0,3);
                        s = "Errors";
                        for (int i=0; i<s.length(); i++)
                                ba[i]=(byte)s.charAt(i);
                        baos.write(ba,0,6);
                }
                catch (IndexOutOfBoundsException e){
                }
                try {
                        for (int i=0; i<ba.length; i++) ba[i]=(byte)' ';
                        baos.write(ba,0,3);
                        s = "Iterations";
                        for (int i=0; i<s.length(); i++)
                                ba[i]=(byte)s.charAt(i);
                        baos.write(ba,0,10);
                }
                catch (IndexOutOfBoundsException e){
                }
                try {
                        for (int i=0; i<ba.length; i++) ba[i]=(byte)' ';
                        baos.write(ba,0,3);
                        s = "State";
                        for (int i=0; i<s.length(); i++)
                                ba[i]=(byte)s.charAt(i);
                        baos.write(ba,0,5);
                }
                catch (IndexOutOfBoundsException e){
                }
                inline004.goldChecker.print(baos.toString());
                inline004.goldChecker.println();

                                                                // Report header underlining
                baos.reset();
                try {
                        for (int i=0; i<ba.length; i++) ba[i]=(byte)' ';
                        s = "Pattern";
                        for (int i=0; i<s.length(); i++)
                                ba[i]=(byte)'=';
                        baos.write(ba,0,32);
                }
                catch (IndexOutOfBoundsException e){
                }
                try {
                        for (int i=0; i<ba.length; i++) ba[i]=(byte)' ';
                        baos.write(ba,0,3);
                        s = "Errors";
                        for (int i=0; i<s.length(); i++)
                                ba[i]=(byte)'=';
                        baos.write(ba,0,6);
                }
                catch (IndexOutOfBoundsException e){
                }
                try {
                        for (int i=0; i<ba.length; i++) ba[i]=(byte)' ';
                        baos.write(ba,0,3);
                        s = "Iterations";
                        for (int i=0; i<s.length(); i++)
                                ba[i]=(byte)'=';
                        baos.write(ba,0,10);
                }
                catch (IndexOutOfBoundsException e){
                }
                try {
                        for (int i=0; i<ba.length; i++) ba[i]=(byte)' ';
                        baos.write(ba,0,3);
                        s = "State";
                        for (int i=0; i<s.length(); i++)
                                ba[i]=(byte)'=';
                        baos.write(ba,0,5);
                }
                catch (IndexOutOfBoundsException e){
                }
                inline004.goldChecker.print(baos.toString());
                inline004.goldChecker.println();

                for (int icount=0; icount<14; icount++) {
                        if (myIters[icount] == ITERS && errFlag[icount] == 0)
                                state="PASS";
                        else {
                                ierr++;
                                state="FAIL";
                        }
                        baos.reset();
                        try {
                                for (int i=0; i<ba.length; i++) ba[i]=(byte)' ';
                                for (int i=0; i<label[icount].length(); i++)
                                        ba[i]=(byte)label[icount].charAt(i);
                                baos.write(ba,0,32);
                        }
                        catch (IndexOutOfBoundsException e){
                                inline004.goldChecker.println("0: "+e);
                        }
                        try {
                                for (int i=0; i<ba.length; i++) ba[i]=(byte)' ';
                                baos.write(ba,0,3);
                                s = Integer.toString(errFlag[icount]);
                                for (int i=0; i<s.length(); i++)
                                        ba[i]=(byte)s.charAt(i);
                                baos.write(ba,0,6);
                        }
                        catch (IndexOutOfBoundsException e){
                                inline004.goldChecker.println("1: "+e);
                        }
                        try {
                                for (int i=0; i<ba.length; i++) ba[i]=(byte)' ';
                                baos.write(ba,0,3);
                                s = Integer.toString(myIters[icount]);
                                for (int i=0; i<s.length(); i++)
                                        ba[i]=(byte)s.charAt(i);
                                baos.write(ba,0,10);
                        }
                        catch (IndexOutOfBoundsException e){
                                inline004.goldChecker.println("1: "+e);
                        }
                        try {
                                for (int i=0; i<ba.length; i++) ba[i]=(byte)' ';
                                baos.write(ba,0,3);
                                for (int i=0; i<state.length(); i++)
                                        ba[i]=(byte)state.charAt(i);
                                baos.write(ba,0,5);
                        }
                        catch (IndexOutOfBoundsException e){
                                inline004.goldChecker.println("3: "+e);
                        }
                        inline004.goldChecker.print(baos.toString());
                        inline004.goldChecker.println();
                }
                return ierr;
        }

        public static void main( String args[] ) {
                int ierr;
                inline004 myInline_n = new inline004();
                inline004 myInline_f = new inline004();

                if (args.length > 0 && args[0].equals("-help")) {
                        inline004.goldChecker.println("usage: java inline004 [-print | -report]");
                        inline004.goldChecker.check();
                        return;
                }
                if (args.length > 0 && args[0].equals("-print"))
                        pFlag = 2;
                if (args.length > 0 && args[0].equals("-report"))
                        pFlag = 1;
                for (int ii=0; ii<14; ii++) myIters[ii]=ITERS;
                if (pFlag==2) inline004.goldChecker.println("inline004");

                                /* Give the JIT an initial look at all the methods */
                myInline_f.runFinals();
                ierr = inline004.result();
                if (ierr == 0) {
                        inline004.goldChecker.println("PASSED.");
                }
                else {
                        inline004.goldChecker.println("FAILED. (ierr = " + ierr + ")");
                }
                inline004.goldChecker.check();
        }
}
