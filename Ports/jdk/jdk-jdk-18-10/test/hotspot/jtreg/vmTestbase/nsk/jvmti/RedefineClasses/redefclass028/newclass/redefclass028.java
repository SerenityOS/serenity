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

public class redefclass028 {
    static int status = Consts.TEST_PASSED;

    static Log log;

    // dummy outer fields to be changed by the inner class
    private int prOuterFl[] = {0,0,0};
    int packOuterFl[] = {0,0,0};
    public int pubOuterFl[] = {0,0,0};
    private static int prStOuterFl[] = {0,0,0};
    static int packStOuterFl[] = {0,0,0};
    public static int pubStOuterFl[] = {0,0,0};

    /* a dummy outer private field to be accessed or not in
       the inner class and thus provoking compiler to add or not
       a synthetic access method into the outer class. */
    private char prOuterFieldToBeAccessed = 'a';

    /**
     * A copy of outer method accessing an inner private field
     * and thus provoking compiler to add synthetic access method
     * into the inner class. The same synthetic method should be
     * present in the redefined inner class to avoid the error
     * JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED.
     */
    private void checkInnerFields(RedefClass redefCls, int expValue) {
        if (redefCls.prInnerFl != expValue
                || redefCls.packInnerFl != expValue
                || redefCls.pubInnerFl != expValue) {
            status = Consts.TEST_FAILED;
            log.complain("TEST FAILED: unexpected values of inner fields:"
                + "\n\tprInnerFl: got: " + redefCls.prInnerFl
                + ", expected: " + expValue
                + "\n\tpackInnerFl: got: " + redefCls.packInnerFl
                + ", expected: " + expValue
                + "\n\tpubInnerFl: got: " + redefCls.pubInnerFl
                + ", expected: " + expValue);
        }
    }

    /**
     * Redefining inner class.
     */
    class RedefClass extends Thread {
        boolean stopMe = false;
        int iter;

        // dummy inner fields to be accessed by the outer class
        private int prInnerFl = 2;
        int packInnerFl = 2;
        public int pubInnerFl = 2;

        RedefClass(int iter) {}

        /**
         * This method will have an active stack frame during
         * inner class redefinition, so this version should not
         * be invoked and thus outer fields should not have
         * values equal 2.
         */
        public void run() {
            status = Consts.TEST_FAILED;
            log.complain("TEST FAILED: new " + this.getName()
                + ": redefined run() having an active stack frame during redefinition was invoked");

            prOuterFl[0] = 2;
            packOuterFl[0] = 2;
            pubOuterFl[0] = 2;
            prStOuterFl[0] = 2;
            packStOuterFl[0] = 2;
            pubStOuterFl[0] = 2;

            log.display("new " + this.getName()
                + ": exiting");
        }

        void warmUpMethod() {
            log.display("new " + this.getName()
                + ": warmUpMethod()");

            prOuterFl[1] = 2;
            packOuterFl[1] = 2;
            pubOuterFl[1] = 2;
            prStOuterFl[1] = 2;
            packStOuterFl[1] = 2;
            pubStOuterFl[1] = 2;

            redefclass028HotMethod(0);
        }

        /**
         * Hotspot method to be compiled
         */
        void redefclass028HotMethod(int i) {
            log.display("new " + this.getName()
                + ": redefclass028HotMethod()");

            prOuterFl[2] = 2;
            packOuterFl[2] = 2;
            pubOuterFl[2] = 2;
            prStOuterFl[2] = 2;
            packStOuterFl[2] = 2;
            pubStOuterFl[2] = 2;
        }

        /**
         * New version of dummy method which is not accessing
         * a private field of the outer class.
         * So compiler should not add a synthetic access method
         * into the outer class.
         */
         void methAccessingOuterPrivateField() {}
    }
}
