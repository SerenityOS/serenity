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
 * @summary converted from VM Testbase jit/inline/inline007.
 * VM Testbase keywords: [jit, quick]
 * VM Testbase readme:
 * Inline007 is similar to inline004 in that it tests inlining.
 * Inline004 still functions, but is not testing inlining, in
 * JDK 1.2e.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.inline.inline007.inline007
 */

package jit.inline.inline007;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

class inline007_1 {
        final protected static int[] inlineObj = { 1, 2 };
        protected static int   inlineInt = 3;           /* Not final */
}

class inline007Sup extends inline007_1 {
        private int i1 = 0;
        Object obj1 = inline007_1.inlineObj;
        Object obj2 = inline007_1.inlineObj;
        Object obj3 = inline007_1.inlineObj;

        static int MPAT_Const4_fs_0() {
                return 1;
        }
        static Object MPAT_Const4_fs_1() {
                return null;
        }
        static int MPAT_GetStatic4_fs_0() {
                return inlineInt;
        }
        static Object MPAT_GetStatic4_fs_1() {
                return inlineObj;
        }
        int MPAT_Const4_fi_0() {
                return 1;
        }
        Object MPAT_Const4_fi_1() {
                return null;
        }
        int MPAT_GetField4_fi_0() {
                return i1;
        }
        Object MPAT_GetField4_fi_1() {
                return obj1;
        }
        void MPAT_PutField4_fi_0(int ival) {
                i1 = ival;
                return;
        }
        void MPAT_PutField4_fi_1(Object oval) {
                obj1 = oval;
                return;
        }
        void MPAT_PutField4Const4_fi() {
                i1 = -1;
                return;
        }
        int MPAT_GetStatic4_fi_0() {
                return inlineInt;
        }
        Object MPAT_GetStatic4_fi_1() {
                return inlineObj;
        }
        Object MPAT_Handle_fi() {
                return this;
        }
}

class inline007Sub extends inline007Sup {
        private int i1 = 0;
        Object obj1 = inline007_1.inlineObj;
        Object obj2 = inline007_1.inlineObj;
        Object obj3 = inline007_1.inlineObj;

        static int MPAT_Const4_fs_0() {
                return 1;
        }
        static Object MPAT_Const4_fs_1() {
                return null;
        }
        static int MPAT_GetStatic4_fs_0() {
                return inlineInt;
        }
        static Object MPAT_GetStatic4_fs_1() {
                return inlineObj;
        }
        int MPAT_Const4_fi_0() {
                return 1;
        }
        Object MPAT_Const4_fi_1() {
                return null;
        }
        int MPAT_GetField4_fi_0() {
                return i1;
        }
        Object MPAT_GetField4_fi_1() {
                return obj1;
        }
        void MPAT_PutField4_fi_0(int ival) {
                i1 = ival;
                return;
        }
        void MPAT_PutField4_fi_1(Object oval) {
                obj1 = oval;
                return;
        }
        void MPAT_PutField4Const4_fi() {
                i1 = -1;
                return;
        }
        int MPAT_GetStatic4_fi_0() {
                return inlineInt;
        }
        Object MPAT_GetStatic4_fi_1() {
                return inlineObj;
        }
        Object MPAT_Handle_fi() {
                return this;
        }
}

public class inline007 extends inline007_1 {
        public static final GoldChecker goldChecker = new GoldChecker( "inline007" );

        static int[] myIters = new int[14];
        static int[] errFlag = new int[14];
        static int pFlag = 0;
        final static int ITERS=5;
        int intTarg1 = 0;
        int intTarg2 = 0;
        Object objTarg1 = null;
        Object objTarg2 = null;
        inline007Sub inline007sub = new inline007Sub();
        inline007Sup inline007sup = inline007sub;

        private void runFinals () {
                int jcount = 0;
                int icount = 0;

                if (pFlag==2) inline007.goldChecker.print("MPAT_Const4_fs_0");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                intTarg1 = inline007Sub.MPAT_Const4_fs_0();
                                intTarg2 = inline007Sup.MPAT_Const4_fs_0();
                                if (intTarg1 != intTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline007.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline007.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline007.goldChecker.print("MPAT_Const4_fs_1");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                objTarg2 = inline007Sub.MPAT_Const4_fs_1();
                                objTarg1 = inline007Sup.MPAT_Const4_fs_1();
                                if (objTarg2 != objTarg1) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline007.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline007.goldChecker.println();
                }
                icount++;


                if (pFlag==2) inline007.goldChecker.print("MPAT_GetStatic4_fs_0");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                intTarg2 = inline007Sub.MPAT_GetStatic4_fs_0();
                                intTarg1 = inline007Sup.MPAT_GetStatic4_fs_0();
                                if (intTarg1 != intTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline007.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline007.goldChecker.println();
                }
                icount++;


                if (pFlag==2) inline007.goldChecker.print("MPAT_GetStatic4_fs_1");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                objTarg2 = inline007Sub.MPAT_GetStatic4_fs_1();
                                objTarg1 = inline007Sup.MPAT_GetStatic4_fs_1();
                                if (objTarg1 != objTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline007.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline007.goldChecker.println();
                }
                icount++;


                /* Check inlining of instance methods */
                if (pFlag==2) inline007.goldChecker.print("MPAT_Const4_fi_0");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                intTarg1 = inline007sub.MPAT_Const4_fi_0();
                                intTarg2 = inline007sup.MPAT_Const4_fi_0();
                                if (intTarg1 != intTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline007.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline007.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline007.goldChecker.print("MPAT_Const4_fi_1");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                objTarg1 = inline007sub.MPAT_Const4_fi_1();
                                objTarg2 = inline007sup.MPAT_Const4_fi_1();
                                if (objTarg1 != objTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline007.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline007.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline007.goldChecker.print("MPAT_GetStatic4_fi_0");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                intTarg2 = inline007sub.MPAT_GetStatic4_fi_0();
                                intTarg1 = inline007sup.MPAT_GetStatic4_fi_0();
                                if (intTarg1 != intTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline007.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline007.goldChecker.println();
                }
                if (pFlag==2) inline007.goldChecker.print("MPAT_GetStatic4_fi_1");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                objTarg2 = inline007sub.MPAT_GetStatic4_fi_1();
                                objTarg1 = inline007sup.MPAT_GetStatic4_fi_1();
                                if (objTarg1 != objTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline007.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline007.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline007.goldChecker.print("MPAT_GetField4_fi_0");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                intTarg1 = inline007sub.MPAT_GetField4_fi_0();
                                intTarg2 = inline007sup.MPAT_GetField4_fi_0();
                                if (intTarg1 != intTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline007.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline007.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline007.goldChecker.print("MPAT_GetField4_fi_1");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                objTarg2 = inline007sub.MPAT_GetField4_fi_1();
                                objTarg1 = inline007sup.MPAT_GetField4_fi_1();
                                if (objTarg1 != objTarg2) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline007.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline007.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline007.goldChecker.print("MPAT_PutField4_fi_0");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                inline007sub.MPAT_PutField4_fi_0(10);
                                intTarg1 = inline007sup.MPAT_GetField4_fi_0();
                                if (intTarg1 != 10) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline007.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline007.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline007.goldChecker.print("MPAT_PutField4_fi_1");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                inline007sub.MPAT_PutField4_fi_1("String1");
                                objTarg1 = inline007sub.MPAT_GetField4_fi_1();
                                if (objTarg1 != "String1") errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline007.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline007.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline007.goldChecker.print("MPAT_PutField4Const4_fi");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                inline007sub.MPAT_PutField4Const4_fi();
                                intTarg2 = inline007sup.MPAT_GetField4_fi_0();
                                if (intTarg2 != -1) errFlag[icount]++;
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline007.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (pFlag==2) inline007.goldChecker.println();
                }
                icount++;

                if (pFlag==2) inline007.goldChecker.print("MPAT_Handle_fi");
                try {
                        for (jcount=0; jcount<ITERS; jcount++) {
                                objTarg1 = inline007sub.MPAT_Handle_fi();
                                objTarg2 = inline007sup.MPAT_Handle_fi();
                        }
                } catch (Exception e) {
                        if (pFlag==2) inline007.goldChecker.print(": iteration="+(jcount+1)+": "+e);
                } finally {
                        myIters[icount] = jcount;
                        if (jcount != ITERS) errFlag[icount]++;
                        if (pFlag==2) inline007.goldChecker.println();
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
                inline007.goldChecker.print(baos.toString());
                inline007.goldChecker.println();

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
                inline007.goldChecker.print(baos.toString());
                inline007.goldChecker.println();

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
                                inline007.goldChecker.println("0: "+e);
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
                                inline007.goldChecker.println("1: "+e);
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
                                inline007.goldChecker.println("1: "+e);
                        }
                        try {
                                for (int i=0; i<ba.length; i++) ba[i]=(byte)' ';
                                baos.write(ba,0,3);
                                for (int i=0; i<state.length(); i++)
                                        ba[i]=(byte)state.charAt(i);
                                baos.write(ba,0,5);
                        }
                        catch (IndexOutOfBoundsException e){
                                inline007.goldChecker.println("3: "+e);
                        }
                        inline007.goldChecker.print(baos.toString());
                        inline007.goldChecker.println();
                }
                return ierr;
        }

        public static void main( String args[] ) {
                int ierr;
                inline007 myInline_n = new inline007();
                inline007 myInline_f = new inline007();

                if (args.length > 0 && args[0].equals("-help")) {
                        inline007.goldChecker.println("usage: java inline007 [-print | -report]");
                        inline007.goldChecker.check();
                        return;
                }
                if (args.length > 0 && args[0].equals("-print"))
                        pFlag = 2;
                if (args.length > 0 && args[0].equals("-report"))
                        pFlag = 1;
                for (int ii=0; ii<14; ii++) myIters[ii]=ITERS;
                if (pFlag==2) inline007.goldChecker.println("inline007");

                                /* Give the JIT an initial look at all the methods */
                myInline_f.runFinals();
                ierr = inline007.result();
                if (ierr == 0) {
                        inline007.goldChecker.println("PASSED.");
                }
                else {
                        inline007.goldChecker.println("FAILED. (ierr = " + ierr + ")");
                }
                inline007.goldChecker.check();
        }
}
