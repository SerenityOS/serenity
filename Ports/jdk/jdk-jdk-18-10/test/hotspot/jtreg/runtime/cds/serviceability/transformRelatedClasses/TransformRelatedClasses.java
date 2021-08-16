/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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


// This is the main test class for testing transformation of related classes
// in combination with CDS, to ensure these features work well together.
// The relationships that can be tested using this test class are:
// superclass/subclass, and interface/implementor relationships.
//
// The test uses combinatorial approach.
// For details on test table and test cases see main() method in this class.
//
// This test consists of multiple classes for better flexibility and reuse,
// and also relies on certain common utility code.
// Here are the details on the structure of the test
//
// Structure of the test:
// TransformRelatedClasses -- common main test driver
//     The TransformRelatedClasses is invoked from test driver classes:
//     TransformInterfaceAndImplementor, TransformSuperAndSubClasses
//     It is responsible for preparing test artifacts (test jar, agent jar
//     and the shared archive), running test cases and checking the results.
// The following test classes below are launched in a sub-process with use
// of shared archive:
//     SuperClazz, SubClass -- super/sub class pair under test
//     Interface, Implementor -- classes under test
// This test will transform these classes, based on the test case data,
// by changing a predefined unique string in each class.
// For more details, see the test classes' code and comments.
//
// Other related classes:
//     TestEntry - a class representing a single test case, as test entry in the table
//     TransformTestCommon - common methods for transformation test cases
//
// Other utility/helper classes and files used in this test:
//     TransformerAgent - an agent that is used when JVM-under-test is executed
//         to transform specific strings inside specified classes
//     TransformerAgent.mf - accompanies transformer agent

import java.io.File;
import java.util.ArrayList;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.helpers.ClassFileInstaller;


public class TransformRelatedClasses {
    static final String archiveName = "./TransformRelatedClasses.jsa";
    static String agentClasses[] = {
        "TransformerAgent",
        "TransformerAgent$SimpleTransformer",
        "TransformUtil"
    };

    String parent;
    String child;
    String[] testClasses = new String[2];
    String[] testNames = new String[2];
    String testJar;
    String agentJar;


    private static void log(String msg) {
        System.out.println("TransformRelatedClasses: " + msg);
    }


    // This class is intended to test 2 parent-child relationships:
    // 1. Base Class (parent) and Derived Class (child)
    // 2. Interface (parent) and Implementor (child)
    //    Parameters to main(): parent, child
    public static void main(String args[]) throws Exception {
        TransformRelatedClasses test = new TransformRelatedClasses(args[0], args[1]);
        test.prepare();

        // Test Table
        // TestEntry:  (testCaseId, transformParent, tranformChild,
        //             isParentExpectedShared, isChildExpectedShared)
        ArrayList<TestEntry> testTable = new ArrayList<>();

        // base case - no tranformation - all expected to be shared
        testTable.add(new TestEntry(0, false, false, true, true));

        // transform parent only - both parent and child should not be shared
        testTable.add(new TestEntry(1, true, false, false, false));

        // transform parent and child - both parent and child should not be shared
        testTable.add(new TestEntry(2, true, true, false, false));

        // transform child only - parent should still be shared, but not child
        testTable.add(new TestEntry(3, false, true, true, false));

        // run the tests
        for (TestEntry entry : testTable) {
            test.runTest(entry);
        }
    }


    public TransformRelatedClasses(String parent, String child) {
        log("Constructor: parent = " + parent + ", child = " + child);
        this.parent = parent;
        this.child = child;
        testClasses[0] = parent;
        testClasses[1] = child;
        testNames[0] = parent.replace('.', '/');
        testNames[1] = child.replace('.', '/');
    }


    // same test jar and archive can be used for all test cases
    private void prepare() throws Exception {
        // create agent jar
        // Agent is the same for all test cases
        String pathToManifest = "../../../../testlibrary/jvmti/TransformerAgent.mf";
        agentJar = ClassFileInstaller.writeJar("TransformerAgent.jar",
                       ClassFileInstaller.Manifest.fromSourceFile(pathToManifest),
                                           agentClasses);

        // create a test jar
        testJar =
            ClassFileInstaller.writeJar(parent + "-" + child + ".jar",
                                           testClasses);

        // create an archive
        String classList =
            CDSTestUtils.makeClassList("transform-" + parent, testNames).getPath();

        CDSTestUtils.createArchiveAndCheck("-Xbootclasspath/a:" + testJar,
            "-XX:ExtraSharedClassListFile=" + classList);
    }


    private void runTest(TestEntry entry) throws Exception {
        log("runTest(): testCaseId = " + entry.testCaseId);

        // execute with archive
        String agentParam = "-javaagent:" + agentJar + "=" +
            TransformTestCommon.getAgentParams(entry, parent, child);

        CDSOptions opts = new CDSOptions()
            .addPrefix("-Xbootclasspath/a:" + testJar, "-Xlog:class+load=info")
            .setUseVersion(false)
            .addSuffix( "-showversion",agentParam, child);

        OutputAnalyzer out = CDSTestUtils.runWithArchive(opts);
        TransformTestCommon.checkResults(entry, out, parent, child);
    }
}
