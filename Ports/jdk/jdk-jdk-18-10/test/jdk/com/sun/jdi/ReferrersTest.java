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
 * @bug 5089849
 * @summary Add support for backtracking reference graph.
 * @author jjh
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g ReferrersTest.java
 * @run driver ReferrersTest
 */

/*
 *  To run this test do this:
 *     runregress -no ReferrersTest <cmd line options>
 *
 *  where <cmd line options> are the options to be used to
 *  launch the debuggee, with the classname prefixed with @@.
 *  For example, this would run java2d demo as the debuggee:
 *     runregress -no ReferrersTest -classpath
 *                                    $jdkDir/demo/jfc/Java2D/Java2Demo.jar \
 *                                    -client @@java2d.Java2Demo
 *
 * In this mode, the specified debuggee is launched in debug mode,
 * and the debugger waits for a keystroke before connecting to the debuggee.
 *
 * If <cmd line options> is not specified, then the ReferrersTarg class below
 * is run as the debuggee.
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

class ReferrersFiller {
    // This many instances of this are created.
    static int FILLER_COUNT = 20000;
    static ReferrersFiller[] lotsAndLots = new ReferrersFiller[
                                               ReferrersFiller.FILLER_COUNT];
    int xx;
    ReferrersFiller(int p1) {
        xx = p1;
    }
}

class ReferrersTarg {
    // This many instances + 1 of this class are created.
    static int TARG_COUNT = 10;
    static ReferrersTarg theReferrersTarg;
    static ReferrersTarg[] allReferrersTargs;

    // Each instance will point to the theReferrersTarg
    ReferrersTarg oneReferrersTarg;

    public static void bkpt() {
    }

    public static void main(String[] args) {
        System.out.println("Howdy!");
        for (int ii = 0; ii < ReferrersFiller.lotsAndLots.length; ii++) {
            ReferrersFiller.lotsAndLots[ii] = new ReferrersFiller(ii);
        }

        theReferrersTarg = new ReferrersTarg();
        allReferrersTargs = new ReferrersTarg[ReferrersTarg.TARG_COUNT];
        for (int ii = 0; ii < ReferrersTarg.TARG_COUNT; ii++) {
            allReferrersTargs[ii] = new ReferrersTarg();
            allReferrersTargs[ii].oneReferrersTarg = theReferrersTarg;
        }
        bkpt();

        System.out.println("Goodbye from ReferrersTarg!");
    }
}

/********** test program **********/

public class ReferrersTest extends TestScaffold {
    static String targetName = "ReferrersTarg";
    ReferenceType targetClass;
    ThreadReference mainThread;

    ReferrersTest(String args[]) {
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
        new ReferrersTest(args).startTests();
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

        if (targetName.equals("ReferrersTarg")) {
            resumeTo("ReferrersTarg", "bkpt", "()V");
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
        if (targetClass.name().equals("ReferrersTarg")) {
            List<ObjectReference> noInstances = targetClass.instances(0);
            if (noInstances.size() != ReferrersTarg.TARG_COUNT + 1) {
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
            throw new Exception("ReferrersTest: failed");
        }

        // Create a list of ReferenceTypes sorted by instance count
        int size = 0;
        List<ToSort> sorted = new ArrayList(allClasses.size());
        for (int ii = 0; ii < allClasses.size(); ii++) {
            size += counts[ii];
            ToSort tos = new ToSort(counts[ii], allClasses.get(ii));
            sorted.add(tos);
        }

        System.out.println("instance counts for " + counts.length +
                           " classes got " + size + " instances and took " +
                            (end - start) + " ms");


        boolean gotReferrersFiller = false;
        boolean gotReferrersTarg = false;

        Collections.sort(sorted);
        for (int ii = sorted.size() - 1; ii >= 0 ; ii--) {
            ToSort xxx = sorted.get(ii);

            if (xxx.rt.name().equals("ReferrersFiller") &&
                xxx.count == ReferrersFiller.FILLER_COUNT) {
                gotReferrersFiller = true;
            }
            if (xxx.rt.name().equals("ReferrersTarg") &&
                xxx.count == ReferrersTarg.TARG_COUNT + 1) {
                gotReferrersTarg = true;
            }
        }
        if (!gotReferrersFiller) {
            failure("failure: Expected " + ReferrersFiller.FILLER_COUNT +
                        " instances of ReferrersFiller");
        }
        if (!gotReferrersTarg) {
            failure("failure: Expected " + (ReferrersTarg.TARG_COUNT + 1) +
                    " instances of ReferrersTarg");
        }

        List<List<ObjectReference>> allInstances = new ArrayList(10);

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

                if (xxx.rt.name().equals("ReferrersFiller") &&
                    oneInstances.size() != ReferrersFiller.FILLER_COUNT) {
                    failure("failure: Expected " + ReferrersFiller.FILLER_COUNT +
                            " instances of ReferrersFiller");
                }
                if (xxx.rt.name().equals("ReferrersTarg") &&
                    oneInstances.size() != ReferrersTarg.TARG_COUNT + 1) {
                    failure("failure: Expected " + (ReferrersTarg.TARG_COUNT + 1) +
                            " instances of ReferrersTarg");
                }
                allInstances.add(oneInstances);
            }

            end = System.currentTimeMillis();

            System.out.println(size + " instances via making one vm.instances" +
                               " call for each of " + count +
                               " classes took " + (end - start1) + " ms");
            System.out.println("Per class = " +
                               (end - start) / allClasses.size() + " ms");
        }


        // referrers

        // Test various values of maxReferrers
        if (targetClass.name().equals("ReferrersTarg")) {
            Field field1 = targetClass.fieldByName("theReferrersTarg");
            ObjectReference anInstance = (ObjectReference)targetClass.getValue(field1);
            List<ObjectReference> noReferrers = anInstance.referringObjects(0);
            if (noReferrers.size() != ReferrersTarg.TARG_COUNT + 1 ) {
                failure("failure: referringObjects(0) got " + noReferrers.size() +
                        ", for " + anInstance);
            }
            noReferrers = anInstance.referringObjects(1);
            if (noReferrers.size() != 1 ) {
                failure("failure: referringObjects(1) got " + noReferrers.size() +
                        ", for " + anInstance);
            }
            boolean pass = false;
            try {
                noReferrers = anInstance.referringObjects(-1);
            } catch (IllegalArgumentException ee) {
                pass = true;
            }
            if (!pass) {
                failure("failure: referringObjects(-1) did not get an exception");
            }
        }

        List<ObjectReference> allReferrers = null;
        List<ObjectReference> someInstances = new ArrayList();
        if (targetName.equals("ReferrersTarg")) {
            Field field1 = targetClass.fieldByName("theReferrersTarg");
            ObjectReference val = (ObjectReference)targetClass.getValue(field1);
            someInstances.add(val);
            allReferrers = val.referringObjects(99999);  //LIMIT
            if (allReferrers.size() != ReferrersTarg.TARG_COUNT + 1) {
                failure("failure: expected " + (ReferrersTarg.TARG_COUNT + 1) +
                        "referrers, but got " + allReferrers.size() +
                        " referrers for " + val);
            }
        } else {
            // referrers
            // Create someInstances to find the referrers of.
            for (int ii = 0; ii < allClasses.size(); ii++) {
                List<ObjectReference> objRefList = allInstances.get(ii);
                if (objRefList != null) {
                    int asize = objRefList.size();
                    if (false) {
                        System.out.println(asize + ", " + allClasses.get(ii));
                    }
                    // Remember one instance per class to get referrers
                    if (asize > 0) {
                        someInstances.add(objRefList.get(0));
                    }
                }
            }
        }

        for (ObjectReference objRef: someInstances) {
            //System.out.println( "Getting referrers for " + objRef);
            start = System.currentTimeMillis();
            if ( true) {
                showReferrers(objRef, 0, 0, 0);
            } else {
                allReferrers = objRef.referringObjects(99999);  //LIMIT
                end = System.currentTimeMillis();
                if (true || allReferrers.size() > 1) {
                    System.out.println( allReferrers.size() + " referrers for " + objRef + " took " + (end - start) + " ms");
                }
            }
        }

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("ReferrersTest: passed");
        } else {
            throw new Exception("ReferrersTest: failed");
        }
    }
    void indent(int level) {
        for (int ii = 0; ii < level; ii++) {
            System.out.print("    ");
        }
    }


    Map<ObjectReference, Object> visited = new HashMap(100);
    void showReferrers(ObjectReference objRef, int level, int total, int which) {

        if (level == 0) {
            visited.clear();
        } else {
            if (visited.containsKey(objRef)) {
                indent(level);
                System.out.println("(" + which + ")" + ":<pruned> " + objRef);
                return;
            }
            visited.put(objRef, null);
            indent(level);
            //System.out.println(which + "/" + total + ": " + objRef + " took " + time + " ms");
        }

        List<ObjectReference> allReferrers = null;

        //System.out.println( "Getting referrers for " + objRef);
        long start, end;
        start = System.currentTimeMillis();
        allReferrers = objRef.referringObjects(99999);  //LIMIT
        end = System.currentTimeMillis();

        if (which == 0) {
            System.out.println(allReferrers.size() + " referrers for " + objRef + " took " + (end - start) + " ms");
        } else {
            System.out.println("(" + which + ") "  + objRef);
            indent(level);
            System.out.println("    " + allReferrers.size() + " referrers for " + objRef + " took " + (end - start) + " ms");
        }

        // We have to stop going up a referrer chain in some cases
        Type rt = objRef.type();
        if (rt instanceof ClassType) {
            ClassType ct = (ClassType)rt;
            String name = ct.name();
            if (name.equals("sun.awt.SoftCache$ValueCell")) {
                return;
            }
            if (name.equals("java.lang.ref.Finalizer")) {
                return;
            }
            if (name.equals("java.lang.ref.SoftReference")) {
                return;
            }
            // oh oh, should really check for a subclass of ClassLoader :-)
            if (name.indexOf("ClassLoader") >= 0) {
                return;
            }
            // No doubt there are other reasons to stop ...
        }
        int itemNumber = 1;
        int allSize = allReferrers.size();
        for (ObjectReference objx: allReferrers) {
            showReferrers(objx, level + 1, allSize, itemNumber++);
        }
    }
}
