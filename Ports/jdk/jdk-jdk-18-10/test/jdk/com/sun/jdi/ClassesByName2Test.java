/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4406439 4925740
 * @summary ClassesByName2 verifies that all the classes in the loaded class list can be found with classesByName..
 * @author Tim Bell (based on ClassesByName by Robert Field)
 *
 * @modules jdk.jdi
 *          java.desktop
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g ClassesByName2Test.java
 * @run driver ClassesByName2Test
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class ClassesByName2Targ {
    static void bkpt() {
    }

    public static void main(String[] args){
        System.out.println("Howdy!");
        try {

            Thread zero = new Thread ("ZERO") {
                    public void run () {
                        System.setProperty("java.awt.headless", "true");
                        java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();

                    }
                };

            Thread one = new Thread ("ONE") {
                    public void run () {
                        try {
                            java.security.KeyPairGenerator keyGen =
                                java.security.KeyPairGenerator.getInstance("DSA", "SUN");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

            Thread two = new Thread ("TWO") {
                public void run () {
                    try {
                        String s = String.format("%02x", 0xff);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            two.start();
            one.start();
            zero.start();

            try {
                zero.join();
                System.out.println("zero joined");
                one.join();
                System.out.println("one joined");
                two.join();
                System.out.println("two joined");
            } catch (InterruptedException iex) {
                iex.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        bkpt();
        System.out.println("Goodbye from ClassesByName2Targ!");
    }
}

    /********** test program **********/

public class ClassesByName2Test extends TestScaffold {
    volatile boolean stop = false;

    ClassesByName2Test (String args[]) {
        super(args);
    }

    public void breakpointReached(BreakpointEvent event) {
        System.out.println("Got BreakpointEvent: " + event);
        stop = true;
    }

    public void eventSetComplete(EventSet set) {
        // Don't resume.
    }

    public static void main(String[] args)      throws Exception {
        new ClassesByName2Test(args).startTests();
    }

    void breakpointAtMethod(ReferenceType ref, String methodName)
                                           throws Exception {
        List meths = ref.methodsByName(methodName);
        if (meths.size() != 1) {
            throw new Exception("test error: should be one " +
                                methodName);
        }
        Method meth = (Method)meths.get(0);
        BreakpointRequest bkptReq = vm().eventRequestManager().
            createBreakpointRequest(meth.location());
        bkptReq.enable();
        try {
            addListener (this);
        } catch (Exception ex){
            ex.printStackTrace();
            failure("failure: Could not add listener");
            throw new Exception("ClassesByname2Test: failed");
        }
    }

    protected void runTests() throws Exception {
        BreakpointEvent bpe = startToMain("ClassesByName2Targ");

        /*
          Bug 6263966 - Don't just resume because the debuggee can
          complete and disconnect while the following loop is
          accessing it.
        */
        breakpointAtMethod(bpe.location().declaringType(), "bkpt");
        vm().resume();

        /* The test of 'stop' is so that we stop when the debuggee hits
           the bkpt.  The 150 is so we stop if the debuggee
           is slow (eg, -Xcomp -server) - we don't want to
           spend all day waiting for it to get to the bkpt.
        */
        for (int i = 0; i < 150 && !stop; i++) {
            List all = vm().allClasses();
            System.out.println("\n++++ Lookup number: " + i + ".  allClasses() returned " +
                               all.size() + " classes.");
            for (Iterator it = all.iterator(); it.hasNext(); ) {
                ReferenceType cls = (ReferenceType)it.next();
                String name = cls.name();
                List found = vm().classesByName(name);
                if (found.contains(cls)) {
                    //System.out.println("Found class: " + name);
                } else {
                    System.out.println("CLASS NOT FOUND: " + name);
                    throw new Exception("CLASS NOT FOUND (by classesByName): " +
                                        name);
                }
            }
        }

        // In case of a slow debuggee, we don't want to resume the debuggee and wait
        // for it to complete.
        vm().exit(0);

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("ClassesByName2Test: passed");
        } else {
            throw new Exception("ClassesByName2Test: failed");
        }
    }
}
