/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4290640 4785473
 * @build package1.Class1 package2.Class2 package1.package3.Class3 Assert
 * @run main/othervm Assert
 * @summary Test the assertion facility
 * @author Mike McCloskey
 * @key randomness
 */

import package1.*;
import package2.*;
import package1.package3.*;
import java.io.*;
import java.util.Random;

public class Assert {

    private static Class1 testClass1;
    private static Class2 testClass2;
    private static Class3 testClass3;
    private static final  boolean debug = false;
    private static Random generator = new Random();

    /**
     * The first invocation of this test starts a loop which exhaustively tests
     * the object tree with all of its different settings.
     * There are 7 test objects in a tree that has 7 different places to set
     * assertions untouched/on/off so this tests 3^7 or 2187 different
     * configurations.
     *
     * This test spawns a new VM for each run because assertions are set on or
     * off at class load time. Once the class is loaded its assertion status
     * does not change.
     */
    public static void main(String[] args) throws Exception {

        // Switch values: 0=don't touch, 1=off, 2 = on
        int[] switches = new int[7];

        int switchSource = 0;
        if (args.length == 0) { // This is the controller

            // This code is for an exhaustive test
            //while(switchSource < 2187) {
            //    int temp = switchSource++;

            // This code is for a weaker but faster test
            for(int x=0; x<100; x++) {
                int temp = generator.nextInt(2187);
                for(int i=0; i<7; i++) {
                    switches[i] = temp % 3;
                    temp = temp / 3;
                }

                // Spawn new VM and load classes
                String command = System.getProperty("java.home") +
                    File.separator + "bin" + File.separator + "java Assert";

                StringBuffer commandString = new StringBuffer(command);
                for(int j=0; j<7; j++)
                    commandString.append(" "+switches[j]);

                Process p = null;
                p = Runtime.getRuntime().exec(commandString.toString());

                if (debug) { // See output of test VMs
                    BufferedReader blah = new BufferedReader(
                                          new InputStreamReader(p.getInputStream()));
                    String outString = blah.readLine();
                    while (outString != null) {
                        System.out.println("from BufferedReader:"+outString);
                        outString = blah.readLine();
                    }
                }

                p.waitFor();
                int result = p.exitValue();
                if (debug) { // See which switch configs failed
                    if (result == 0) {
                        for(int k=6; k>=0; k--)
                            System.out.print(switches[k]);
                        System.out.println();
                    } else {
                        System.out.print("Nonzero Exit: ");
                        for(int k=6; k>=0; k--)
                            System.out.print(switches[k]);
                        System.out.println();
                    }
                } else {
                    if (result != 0) {
                        System.err.print("Nonzero Exit: ");
                        for(int k=6; k>=0; k--)
                            System.err.print(switches[k]);
                        System.err.println();
                        throw new RuntimeException("Assertion test failure.");
                    }
                }
            }
        } else { // This is a test spawn
            for(int i=0; i<7; i++)
                switches[i] = Integer.parseInt(args[i]);

            SetAssertionSwitches(switches);
            ConstructClassTree();
            TestClassTree(switches);
        }
    }

    /*
     * Activate/Deactivate the assertions in the tree according to the
     * specified switches.
     */
    private static void SetAssertionSwitches(int[] switches) {
        ClassLoader loader = ClassLoader.getSystemClassLoader();

        if (switches[0] != 0)
            loader.setDefaultAssertionStatus(switches[0]==2);
        if (switches[1] != 0)
            loader.setPackageAssertionStatus("package1", switches[1]==2);
        if (switches[2] != 0)
            loader.setPackageAssertionStatus("package2", switches[2]==2);
        if (switches[3] != 0)
            loader.setPackageAssertionStatus("package1.package3", switches[3]==2);
        if (switches[4] != 0)
            loader.setClassAssertionStatus("package1.Class1", switches[4]==2);
        if (switches[5] != 0)
            loader.setClassAssertionStatus("package2.Class2", switches[5]==2);
        if (switches[6] != 0)
            loader.setClassAssertionStatus("package1.package3.Class3", switches[6]==2);
    }

    /*
     * Verify that the assertions are activated or deactivated as specified
     * by the switches.
     */
    private static void TestClassTree(int[] switches) {

        // Class1 and anonymous inner class
        boolean assertsOn = (switches[4]==2) ? true : (switches[4]==1) ? false :
            (switches[1]==2) ? true : (switches[1]==1) ? false : (switches[0]==2) ?
            true: false;
        testClass1.testAssert(assertsOn);

        // Class1 inner class Class11
        assertsOn = (switches[4]==2) ? true : (switches[4]==1) ? false :
            (switches[1]==2) ? true : (switches[1]==1) ? false : (switches[0]==2) ?
            true: false;
        Class1.Class11.testAssert(assertsOn);

        // Class2
        assertsOn = (switches[5]==2) ? true : (switches[5]==1) ? false :
            (switches[2]==2) ? true : (switches[2]==1) ? false : (switches[0]==2) ?
            true: false;
        testClass2.testAssert(assertsOn);

        // Class3 and anonymous inner class
        assertsOn = (switches[6]==2) ? true : (switches[6]==1) ? false :
                    (switches[3]==2) ? true : (switches[3]==1) ? false :
                    (switches[1]==2) ? true : (switches[1]==1) ? false :
                    (switches[0]==2) ? true: false;
        testClass3.testAssert(assertsOn);

        // Class3 inner class Class31
        assertsOn = (switches[6]==2) ? true : (switches[6]==1) ? false :
                    (switches[3]==2) ? true : (switches[3]==1) ? false :
                    (switches[1]==2) ? true : (switches[1]==1) ? false :
                    (switches[0]==2) ? true : false;
        Class3.Class31.testAssert(assertsOn);

    }

    /*
     * Create the class tree to be tested. Each test run must reload the classes
     * of the tree since assertion status is determined at class load time.
     */
    private static void ConstructClassTree() {
        testClass1 = new Class1();
        testClass2 = new Class2();
        testClass3 = new Class3();
    }


}
