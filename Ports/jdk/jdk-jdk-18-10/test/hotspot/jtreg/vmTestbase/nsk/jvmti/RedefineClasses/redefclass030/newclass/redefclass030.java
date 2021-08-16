/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.RedefineClasses;

import java.io.*;
import java.util.*;
import nsk.share.*;

public class redefclass030 {
    static int status = Consts.TEST_PASSED;

    static Log log;

    // dummy "outer-outer" fields to be changed by the local class
    private int prOuterOuterFl[] = {3,3};
    int packOuterOuterFl[] = {3,3};
    public int pubOuterOuterFl[] = {3,3};
    private static int prStOuterOuterFl[] = {3,3};
    static int packStOuterOuterFl[] = {3,3};
    public static int pubStOuterOuterFl[] = {3,3};

    /**
     * A dummy method accessing an inner private field and thus
     * provoking compiler to add a synthetic access method
     * into the inner class. The same synthetic method should be
     * present in the redefined local class to avoid the exception
     * java.lang.NoSuchMethodError after the redefinition.
     */
    private void checkOuterFields(RedefClassWrapper redefClsWrapper, int index, int expValue) {
        if (redefClsWrapper.prOuterFl[index] != expValue
                || redefClsWrapper.packOuterFl[index] != expValue
                || redefClsWrapper.pubOuterFl[index] != expValue) {
            status = Consts.TEST_FAILED;
            log.complain("TEST FAILED: unexpected values of outer fields of the class"
                + "\n\t\"" + redefClsWrapper.toString() + "\":"
                + "\n\t\tprOuterFl["+ index +"]: got: " + redefClsWrapper.prOuterFl[index]
                + ", expected: " + expValue
                + "\n\t\tpackOuterFl["+ index +"]: got: " + redefClsWrapper.packOuterFl[index]
                + ", expected: " + expValue
                + "\n\t\tpubOuterFl["+ index +"]: got: " + redefClsWrapper.pubOuterFl[index]
                + ", expected: " + expValue);
        }
    }

    /**
     * Class executing the local inner class to be redefined.
     */
    class RedefClassWrapper extends Thread {
        // dummy outer fields to be changed by the local class
        private int prOuterFl[] = {3,3};
        int packOuterFl[] = {3,0};
        public int pubOuterFl[] = {3,3};

        public void run() {
            // dummy local vars to be changed by the local class
            final int outerLocalVar[] = {3,3};

            /**
             * Redefining local inner class.
             */
            class RedefClass {
                int iter;

                // dummy inner fields to be accessed by the outer class
                private int prInnerFl = 3;
                int packInnerFl = 3;
                public int pubInnerFl = 3;

                RedefClass(int iter) {}

                void warmUpMethod() {
                    log.display("new " + this.toString()
                        + ": warmUpMethod()");

                    prOuterOuterFl[0] = 2;
                    packOuterOuterFl[0] = 2;
                    pubOuterOuterFl[0] = 2;
                    prStOuterOuterFl[0] = 2;
                    packStOuterOuterFl[0] = 2;
                    pubStOuterOuterFl[0] = 2;

                    prOuterFl[0] = 2;
                    packOuterFl[0] = 2;
                    pubOuterFl[0] = 2;

                    outerLocalVar[0] = 2;

                    redefclass030HotMethod(0);
                }

                /**
                 * Hotspot method to be compiled
                 */
                void redefclass030HotMethod(int i) {
                    log.display("new " + this.toString()
                        + ": redefclass030HotMethod()");

                    prOuterOuterFl[1] = 2;
                    packOuterOuterFl[1] = 2;
                    pubOuterOuterFl[1] = 2;
                    prStOuterOuterFl[1] = 2;
                    packStOuterOuterFl[1] = 2;
                    pubStOuterOuterFl[1] = 2;

                    prOuterFl[1] = 2;
                    packOuterFl[1] = 2;
                    pubOuterFl[1] = 2;

                    outerLocalVar[1] = 2;
                }
            }
            /*************************************************************/

            /* dummy code accessing an inner private field and thus
               provoking compiler to add a synthetic access method
              into the local class. The same synthetic method should be
              present in the redefined local class to avoid the error
              JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED. */
            RedefClass redefCls = new RedefClass(0);

            if (redefCls.prInnerFl != 1
                     || redefCls.packInnerFl != 1
                     || redefCls.pubInnerFl != 1) {
                status = Consts.TEST_FAILED;
                log.complain("TEST FAILED: unexpected values of inner fields of the local class"
                    + "\n\t\"" + this.toString() + "\":"
                    + "\n\t\tprInnerFl: got: " + redefCls.prInnerFl
                    + ", expected: 1"
                    + "\n\t\tpackInnerFl: got: " + redefCls.packInnerFl
                    + ", expected: 1"
                    + "\n\t\tpubInnerFl: got: " + redefCls.pubInnerFl
                    + ", expected: 1");
            }
            /* end of dummy code */
        }
    }

}
