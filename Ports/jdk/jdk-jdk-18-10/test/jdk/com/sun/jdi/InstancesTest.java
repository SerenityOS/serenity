/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5024119
 * @summary Add ReferenceType.getAllInstances () method to JDI.
 * @author jjh
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g InstancesTest.java
 * @run driver InstancesTest
 */

/*
 *  To run this test do this:
 *     runregress -no InstancesTest <cmd line options>
 *
 *  where <cmd line options> are the options to be used to
 *  launch the debuggee, with the classname prefixed with @@.
 *  For example, this would run java2d demo as the debuggee:
 *     runregress -no InstancesTest -classpath
 *                                    $jdkDir/demo/jfc/Java2D/Java2Demo.jar \
 *                                    -client @@java2d.Java2Demo
 *
 * In this mode, the specified debuggee is launched in debug mode,
 * the debugger waits 20 secs, and then connects to the debuggee, suspends
 * it, and 'debugs' it.
 *
 * If <cmd line options> is not specified, then the InstancesTarg class below
 * is run as the debuggee.
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

class InstancesFiller {
    // This many instances of this are created.
    static int FILLER_COUNT = 200000;
    static InstancesFiller[] lotsAndLots = new InstancesFiller[
                                               InstancesFiller.FILLER_COUNT];
    int xx;
    InstancesFiller(int p1) {
        xx = p1;
    }
}

class InstancesTarg {
    // This many instances + 1 of this class are created.
    static int TARG_COUNT = 1000;
    static InstancesTarg theInstancesTarg;
    static InstancesTarg[] allInstancesTargs;

    // Each instance will point to the theInstancesTarg
    InstancesTarg oneInstancesTarg;

    public static void bkpt() {
    }

    public static void main(String[] args) {
        System.out.println("Howdy!");
        for (int ii = 0; ii < InstancesFiller.lotsAndLots.length; ii++) {
            InstancesFiller.lotsAndLots[ii] = new InstancesFiller(ii);
        }

        theInstancesTarg = new InstancesTarg();
        allInstancesTargs = new InstancesTarg[InstancesTarg.TARG_COUNT];
        for (int ii = 0; ii < InstancesTarg.TARG_COUNT; ii++) {
            allInstancesTargs[ii] = new InstancesTarg();
            allInstancesTargs[ii].oneInstancesTarg = theInstancesTarg;
        }
        bkpt();

        System.out.println("Goodbye from InstancesTarg!");
    }
}

/********** test program **********/

public class InstancesTest extends TestScaffold {
    static String targetName = "InstancesTarg";
    ReferenceType targetClass;
    ThreadReference mainThread;

    InstancesTest(String args[]) {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        /*
         * If args contains @@xxxx, then that is the
         * name of the class we are to run.
         */
        for (int ii = 0; ii < args.length; ii ++) {
            if (args[ii].startsWith("@@")) {
                targetName = args[ii] = args[ii].substring(2);
                break;
            }
        }
        new InstancesTest(args).startTests();
    }

    /*
     * Used to sort a list of ReferenceTypes by
     * instance count.
     */
    class ToSort implements Comparable<ToSort> {
        long count;
        ReferenceType rt;

        public ToSort(long count, ReferenceType rt) {
            this.count = count;
            this.rt = rt;
        }

        public int compareTo(ToSort obj) {
            if (count < obj.count) return -1;
            if (count == obj.count) return 0;
            return 1;
        }
    }

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        int CUT_OFF = 1000;
        BreakpointEvent bpe;
        bpe = startToMain(targetName);
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();

        if (targetName.equals("InstancesTarg")) {
            resumeTo("InstancesTarg", "bkpt", "()V");
        } else {
            // Let debuggee run for awhile to get classes loaded
            vm().resume();
            try {
                System.err.println("Press <enter> to continue");
                System.in.read();
                System.err.println("running...");

            } catch(Exception e) {
            }
            vm().suspend();
        }

        // Get all classes.
        long start = System.currentTimeMillis();
        List<ReferenceType> allClasses = vm().allClasses();
        long end = System.currentTimeMillis();
        System.out.println( allClasses.size() +
                            " classes from vm.allClasses() took " +
                            (end - start) + " ms");

        long[] counts;

        // Test for NPE
        {
            boolean pass = false;
            try {
                counts = vm().instanceCounts(null);
            } catch (NullPointerException ee) {
                pass = true;
            }
            if (!pass) {
                failure("failure: NullPointerException not thrown on instanceCounts(null)");
            }
        }

        // Test for 0 length array
        {
            List<ReferenceType>someClasses = new ArrayList(2);
            counts = vm().instanceCounts(someClasses);
            if (counts.length != 0) {
                failure("failure: instanceCounts with a zero length array fails: " +
                        counts.length);
            }
        }

        // Test various values of maxInstances
        if (targetClass.name().equals("InstancesTarg")) {
            List<ObjectReference> noInstances = targetClass.instances(0);
            if (noInstances.size() != InstancesTarg.TARG_COUNT + 1) {
                failure("failure: instances(0): " + noInstances.size() + ", for " + targetClass);
            }
            noInstances = targetClass.instances(1);
            if (noInstances.size() != 1) {
                failure("failure: instances(1): " + noInstances.size() + ", for " + targetClass);
            }
            boolean pass = false;
            try {
                noInstances = targetClass.instances(-1);
            } catch (IllegalArgumentException ee) {
                pass = true;
            }
            if (!pass) {
                failure("failure: instances(-1) did not get an exception");
            }
        }

        // Instance counts for all classes
        start = System.currentTimeMillis();
        counts = vm().instanceCounts(allClasses);
        end = System.currentTimeMillis();

        if (counts.length == 0) {
            System.out.println("failure: No instances found");
            throw new Exception("InstancesTest: failed");
        }

        // Create a list of ReferenceTypes sorted by instance count
        int size = 0;
        List<ToSort> sorted = new ArrayList(allClasses.size());
        for (int ii = 0; ii < allClasses.size(); ii++) {
            System.out.println(counts[ii] + "   " + allClasses.get(ii));
            size += counts[ii];
            ToSort tos = new ToSort(counts[ii], allClasses.get(ii));
            sorted.add(tos);
        }

        System.out.println("instance counts for " + counts.length +
                           " classes got " + size + " instances and took " +
                            (end - start) + " ms");


        boolean gotInstancesFiller = false;
        boolean gotInstancesTarg = false;

        Collections.sort(sorted);
        for (int ii = sorted.size() - 1; ii >= 0 ; ii--) {
            ToSort xxx = sorted.get(ii);
            if (xxx.count <= CUT_OFF) {
                break;
            }
            if (xxx.rt.name().equals("InstancesFiller") &&
                xxx.count == InstancesFiller.FILLER_COUNT) {
                gotInstancesFiller = true;
            }
            if (xxx.rt.name().equals("InstancesTarg") &&
                xxx.count == InstancesTarg.TARG_COUNT + 1) {
                gotInstancesTarg = true;
            }
        }
        if (!gotInstancesFiller) {
                failure("failure: Expected " + InstancesFiller.FILLER_COUNT +
                        " instances of InstancesFiller");
        }
        if (!gotInstancesTarg) {
            failure("failure: Expected " + (InstancesTarg.TARG_COUNT + 1) +
                    " instances of InstancesTarg");
        }

        // Instances, one class at a time, in sorted order, printing each line
        if (true) {
            System.out.println("\nGetting instances for one class " +
                               "at a time (limited) in sorted order");
            List<ReferenceType> rtList = new ArrayList(1);
            rtList.add(null);
            long start1 = System.currentTimeMillis();
            size = 0;
            long count = 0;
            for (int ii = sorted.size() - 1; ii >= 0 ; ii--) {
                ToSort xxx = sorted.get(ii);
                if (xxx.count <= CUT_OFF) {
                    break;
                }
                rtList.set(0, xxx.rt);
                start = System.currentTimeMillis();
                List<ObjectReference> oneInstances = xxx.rt.instances(19999999);
                end = System.currentTimeMillis();
                size += oneInstances.size();
                count++;
                System.out.println("Expected " + xxx.count + " instances, got " +
                                   oneInstances.size() +
                                   " instances for " + sorted.get(ii).rt +
                                   " in " + (end - start) + " ms");

                if (xxx.rt.name().equals("InstancesFiller") &&
                    oneInstances.size() != InstancesFiller.FILLER_COUNT) {
                    failure("failure: Expected " + InstancesFiller.FILLER_COUNT +
                            " instances of InstancesFiller");
                }
                if (xxx.rt.name().equals("InstancesTarg") &&
                    oneInstances.size() != InstancesTarg.TARG_COUNT + 1) {
                    failure("failure: Expected " + (InstancesTarg.TARG_COUNT + 1) +
                            " instances of InstancesTarg");
                }

            }

            end = System.currentTimeMillis();

            System.out.println(size + " instances via making one vm.instances" +
                               " call for each of " + count +
                               " classes took " + (end - start1) + " ms");
            System.out.println("Per class = " +
                               (end - start) / allClasses.size() + " ms");
        }

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("InstancesTest: passed");
        } else {
            throw new Exception("InstancesTest: failed");
        }
    }
}
