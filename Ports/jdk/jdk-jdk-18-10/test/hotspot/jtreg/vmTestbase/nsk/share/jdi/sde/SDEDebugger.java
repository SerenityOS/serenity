/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.jdi.sde;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import com.sun.jdi.*;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.StepEvent;
import nsk.share.ClassFileFinder;
import nsk.share.TestBug;
import nsk.share.jdi.EventHandler;
import nsk.share.jdi.TestDebuggerType2;

/*
 * Class is used as base debugger for SDE tests.
 *
 * This class contains several methods called 'prepareDefaultPatchedClassFile_TypeXXX' which create
 * class file with added SourceDebugExtension attribute and return debug information about created file.
 * (this methods was moved in SDEDebugger class if similar generated class files were used in 2 or more tests)
 *
 *
 */
public class SDEDebugger extends TestDebuggerType2 {
    protected static final int INIT_LINE = 32;    // n.s.j.sde.TestClass1::<init>
    protected static final int METHOD1_LINE = 43; // n.s.j.sde.TestClass1::sde_testMethod1
    protected static final int METHOD2_LINE = 54; // n.s.j.sde.TestClass1::sde_testMethod2
    public static String javaStratumName = "Java";

    // Set debug data for "Java" stratum for TestClass1
    public static List<DebugLocation> javaStratumLocations_TestClass1 = new ArrayList<DebugLocation>();

    public static String javaSourceName_TestClass1 = "TestClass1.java";

    public static String javaSourcePath_TestClass1 = "nsk" + File.separator + "share" + File.separator + "jdi"
            + File.separator + "sde" + File.separator + "TestClass1.java";

    static {
        for (int i = 0; i < 8; i++) {
            javaStratumLocations_TestClass1.add(new DebugLocation(javaSourceName_TestClass1, javaSourcePath_TestClass1,
                    "<init>", INIT_LINE + i, INIT_LINE + i));
            javaStratumLocations_TestClass1.add(new DebugLocation(javaSourceName_TestClass1, javaSourcePath_TestClass1,
                    "sde_testMethod1", METHOD1_LINE + i, METHOD1_LINE + i));
            javaStratumLocations_TestClass1.add(new DebugLocation(javaSourceName_TestClass1, javaSourcePath_TestClass1,
                    "sde_testMethod2", METHOD2_LINE + i, METHOD2_LINE + i));
        }
    }

    // insert debug information about "Java" stratum in the given Map<String,
    // LocationsData>
    static protected void addJavaLocations(Map<String, LocationsData> testStratumData, boolean setJavaStratumDefault) {
        LocationsData locationsData = new LocationsData(javaStratumName);

        if (setJavaStratumDefault)
            locationsData.isDefault = true;

        locationsData.paths.add(javaSourcePath_TestClass1);
        locationsData.allLocations.addAll(javaStratumLocations_TestClass1);
        locationsData.sourceLocations.put(javaSourceName_TestClass1, javaStratumLocations_TestClass1);

        testStratumData.put(javaStratumName, locationsData);

    }

    // common base names for test stratum, stratum source name and stratum
    // source path
    public static final String testStratumName = "TestStratum";

    public static final String testStratumSourceName = "testSource";

    public static final String testStratumSourcePath = "testSourcePath";

    // Event listener thread which saves all received StepEvents
    public class StepEventListener extends EventHandler.EventListener {
        // is BreakpointEvent was received (debuggee should stop at breakpoint
        // after StepEvents generation)
        private volatile boolean breakpointEventReceived;

        private List<Location> stepLocations = new ArrayList<Location>();

        public List<Location> stepLocations() {
            return stepLocations;
        }

        public void clearLocations() {
            stepLocations.clear();
        }

        public boolean eventReceived(Event event) {
            if (event instanceof StepEvent) {
                StepEvent stepEvent = (StepEvent) event;

                stepLocations.add(stepEvent.location());

                vm.resume();

                return true;
            }
            // debuggee should stop after event generation
            else if (event instanceof BreakpointEvent) {
                breakpointEventReceived = true;
                vm.resume();

                return true;
            }

            return false;
        }

        public void waitBreakpointEvent() {
            while (!breakpointEventReceived)
                Thread.yield();
        }
    }

    // debug information about location (implements Comparable to make possible
    // using DebugLocation with java.util.Set)
    public static class DebugLocation implements Comparable<DebugLocation> {
        public DebugLocation(String sourceName, String sourcePath, String methodName, int inputLine, int outputLine) {
            this.sourceName = sourceName;
            this.sourcePath = sourcePath;
            this.inputLine = inputLine;
            this.outputLine = outputLine;
            this.methodName = methodName;
        }

        public String sourceName;

        public String sourcePath;

        public String methodName;

        public int inputLine;

        public int outputLine;

        public String toString() {
            return "Line number: " + inputLine + " SourceName: " + sourceName + " SourcePath: " + sourcePath;
        }

        // compare DebugLocation with com.sun.jdi.Location
        public boolean compare(Location location, String stratum) {
            try {
                if (stratum == null) {
                    return (location.lineNumber() == inputLine) && location.sourceName().equals(sourceName)
                            && location.sourcePath().equals(sourcePath);
                } else {
                    return (location.lineNumber(stratum) == inputLine)
                            && location.sourceName(stratum).equals(sourceName)
                            && location.sourcePath(stratum).equals(sourcePath);
                }
            } catch (AbsentInformationException e) {
                return false;
            }
        }

        // used to find locations for given line
        public boolean isConform(String sourceName, int lineNumber) {
            boolean sourceConform = (sourceName == null ? true : this.sourceName.equals(sourceName));

            return sourceConform && (this.inputLine == lineNumber);
        }

        // implements this method to make possible using DebugLocation with java.util.Set
        public int compareTo(DebugLocation location) {
            return (this.sourceName.equals(location.sourceName) && this.inputLine == location.inputLine) ? 0 : 1;
        }
    }

    // Class contains debug information about sources, paths, locations
    // available for class
    public static class LocationsData {
        public LocationsData(String stratumName) {
            this.stratumName = stratumName;
        }

        public List<String> sourceNames() {
            List<String> sourceNames = new ArrayList<String>();
            sourceNames.addAll(sourceLocations.keySet());

            return sourceNames;
        }

        public String stratumName;

        // is stratum default for ReferenceType
        public boolean isDefault;

        // locations for source files
        public Map<String, List<DebugLocation>> sourceLocations = new TreeMap<String, List<DebugLocation>>();

        // all locations for stratum
        public List<DebugLocation> allLocations = new ArrayList<DebugLocation>();

        // all paths for stratum
        public List<String> paths = new ArrayList<String>();
    }

    static protected String locationToString(Location location, String stratum) {
        String result;

        result = "Line number: " + (stratum == null ? location.lineNumber() : location.lineNumber(stratum));

        try {
            result += (" Source name: " + (stratum == null ? location.sourceName() : location.sourceName(stratum)));
        } catch (AbsentInformationException e) {
            result += (" Source name: " + " INFORMATION IS ABSENT");
        }

        try {
            result += (" Source path: " + (stratum == null ? location.sourcePath() : location.sourcePath(stratum)));
        } catch (AbsentInformationException e) {
            result += (" Source path: " + " INFORMATION IS ABSENT");
        }

        return result;
    }

    // seach class file for given class and create copy of this class file in
    // 'testWorkDir' directory
    protected File createLocalClassfileCopy(String className) throws IOException {
        int index = className.lastIndexOf(".");

        String path;

        if (index < 0)
            path = "";
        else
            path = className.substring(0, index).replace(".", "/");

        File dirs = new File(testWorkDir + "/" + path);
        dirs.mkdirs();

        File oldFile = null;
        {
            java.nio.file.Path tmp = ClassFileFinder.findClassFile(className, classpath);
            oldFile = tmp == null ? null : tmp.toFile();
        }
        File newFile = copyFile(oldFile,
                testWorkDir + "/" + className.replace(".", "/") + ".class");

        return newFile;
    }

    // create class file with multiple strata
    protected void savePathcedClassFile(String className, SmapGenerator smapGenerator, String smapFileName) {
        File patchedClassFile = null;

        try {
            patchedClassFile = createLocalClassfileCopy(className);
        } catch (IOException e) {
            e.printStackTrace(log.getOutStream());
            throw new TestBug("Unexpected IO exception: " + e);
        }

        smapFileName = testWorkDir + "/" + smapFileName;
        smapGenerator.setOutputFileName(smapFileName);

        File smapFile = null;

        try {
            smapFile = smapGenerator.saveToFile(smapFileName);
        } catch (IOException e) {
            e.printStackTrace(log.getOutStream());
            throw new TestBug("Unexpected IO exception: " + e);
        }

        log.display("Add for class " + className + " following SDE: ");
        log.display(smapGenerator.getString());

        try {
            InstallSDE.install(patchedClassFile, smapFile, false);
        } catch (IOException e) {
            e.printStackTrace(log.getOutStream());
            throw new TestBug("Unexpected IO exception: " + e);
        }
    }

    // common for SDE tests debuggee name
    protected String debuggeeClassName() {
        if (classpath == null) {
            throw new TestBug("Debugger requires 'testClassPath' parameter");
        }

        return SDEDebuggee.class.getName() + " -testClassPath " + testWorkDir;
    }

    // parses common for all SDE - tests parameters
    protected String[] doInit(String args[], PrintStream out) {
        args = super.doInit(args, out);

        if (classpath == null) {
            throw new TestBug("Debugger requires 'testClassPath' parameter");
        }
        if (testWorkDir == null) {
            throw new TestBug("Debugger requires 'testWorkDir' parameter");
        }

        return args;
    }

    // single input line is mapped to the single output line, test stratum has
    // single source
    protected Map<String, LocationsData> prepareDefaultPatchedClassFile_Type1(String className, int testStratumCount,
            boolean setJavaStratumDefault) {
        /*
         * "Java" "TestStratum"
         *
         * <init>
         * 32 --> 1000, source1
         * 33 --> 1001, source1
         * ...
         * ...
         * 39 --> 1007, source1
         *
         * sde_testMethod1
         *
         * 43 --> 1100, source1
         * 44 --> 1101, source1
         * ...
         * ...
         * 50 --> 1107, source1
         *
         * sde_testMethod1
         * 54 --> 1200, source1
         * 55 --> 1201, source1
         * ...
         * ...
         * 61 --> 1207, source1
         */

        Map<String, LocationsData> testStratumData = new TreeMap<String, LocationsData>();

        String smapFileName = "TestSMAP.smap";
        SmapGenerator smapGenerator = new SmapGenerator();

        for (int i = 0; i < testStratumCount; i++) {
            String stratumName = testStratumName + (i + 1);

            LocationsData locationsData = new LocationsData(stratumName);

            String sourceName = testStratumSourceName + (i + 1);
            String sourcePath = testStratumSourcePath + (i + 1);

            locationsData.paths.add(sourcePath);

            SmapStratum smapStratum = new SmapStratum(stratumName);

            List<DebugLocation> sourceLocations = new ArrayList<DebugLocation>();

            int baseLineNumber = 1000 * (i + 1);

            for (int j = 0; j < 8; j++) {
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "<init>", baseLineNumber + j, INIT_LINE + j));
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "sde_testMethod1", baseLineNumber + 100 + j, METHOD1_LINE + j));
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "sde_testMethod2", baseLineNumber + 200 + j, METHOD2_LINE + j));
            }

            locationsData.sourceLocations.put(sourceName, sourceLocations);
            locationsData.allLocations.addAll(sourceLocations);

            testStratumData.put(stratumName, locationsData);

            smapStratum.addFile(sourceName, sourcePath);

            for (DebugLocation debugLocation : sourceLocations) {
                smapStratum.addLineData(debugLocation.inputLine, sourceName, 1, debugLocation.outputLine, 1);
            }

            // if setJavaStratumDefault == false do first test stratum default
            if (!(setJavaStratumDefault) && (i == 0)) {
                locationsData.isDefault = true;
                smapGenerator.addStratum(smapStratum, true);
            } else
                smapGenerator.addStratum(smapStratum, false);
        }

        savePathcedClassFile(className, smapGenerator, smapFileName);

        addJavaLocations(testStratumData, setJavaStratumDefault);

        return testStratumData;
    }

    // single input line is mapped to the single output line, test stratum has 3
    // sources
    protected Map<String, LocationsData> prepareDefaultPatchedClassFile_Type2(String className, int testStratumCount) {
        /*
         * "Java" "TestStratum"
         *
         * <init>
         * 32 --> 1000, source1, path1
         * 33 --> 1001, source2, path2
         * 34 --> 1002, source3, path3
         * ...
         * ...
         *
         * sde_testMethod1
         * 43 --> 1100, source1, path1
         * 44 --> 1101, source2, path2
         * 45 --> 1102, source3, path3
         * ...
         * ...
         *
         * sde_testMethod2
         * 54 --> 1200, source1, path1
         * 55 --> 1201, source2, path2
         * 56 --> 1207, source3, path3
         * ...
         * ...
         */

        Map<String, LocationsData> testStratumData = new TreeMap<String, LocationsData>();

        String smapFileName = "TestSMAP.smap";
        SmapGenerator smapGenerator = new SmapGenerator();

        for (int i = 0; i < testStratumCount; i++) {
            String stratumName = testStratumName + (i + 1);
            SmapStratum smapStratum = new SmapStratum(stratumName);

            LocationsData locationsData = new LocationsData(stratumName);

            String sourceName1 = testStratumSourceName + (i + 1) + "_1";
            String sourcePath1 = testStratumSourcePath + (i + 1) + "_1";
            locationsData.paths.add(sourcePath1);
            smapStratum.addFile(sourceName1, sourcePath1);

            String sourceName2 = testStratumSourceName + (i + 1) + "_2";
            String sourcePath2 = testStratumSourcePath + (i + 1) + "_2";
            locationsData.paths.add(sourcePath2);
            smapStratum.addFile(sourceName2, sourcePath2);

            String sourceName3 = testStratumSourceName + (i + 1) + "_3";
            String sourcePath3 = testStratumSourcePath + (i + 1) + "_3";
            locationsData.paths.add(sourcePath3);
            smapStratum.addFile(sourceName3, sourcePath3);

            List<DebugLocation> source1Locations = new ArrayList<DebugLocation>();
            List<DebugLocation> source2Locations = new ArrayList<DebugLocation>();
            List<DebugLocation> source3Locations = new ArrayList<DebugLocation>();

            for (int j = 0; j < 8; j++) {
                if (j % 3 == 0) {
                    source1Locations.add(new DebugLocation(sourceName1, sourcePath1,
                            "<init>", 1000 + j, INIT_LINE + j));
                    source1Locations.add(new DebugLocation(sourceName1, sourcePath1,
                            "sde_testMethod1", 1101 + j, METHOD1_LINE + j));
                    source1Locations.add(new DebugLocation(sourceName1, sourcePath1,
                            "sde_testMethod2", 1201 + j, METHOD2_LINE + j));
                } else if (j % 3 == 1) {
                    source2Locations.add(new DebugLocation(sourceName2, sourcePath2,
                            "<init>", 1000 + j, INIT_LINE + j));
                    source2Locations.add(new DebugLocation(sourceName2, sourcePath2,
                            "sde_testMethod1", 1101 + j, METHOD1_LINE + j));
                    source2Locations.add(new DebugLocation(sourceName2, sourcePath2,
                            "sde_testMethod2", 1201 + j, METHOD2_LINE + j));
                } else {
                    source3Locations.add(new DebugLocation(sourceName3, sourcePath3,
                            "<init>", 1000 + j, INIT_LINE + j));
                    source3Locations.add(new DebugLocation(sourceName3, sourcePath3,
                            "sde_testMethod1", 1101 + j, METHOD1_LINE + j));
                    source3Locations.add(new DebugLocation(sourceName3, sourcePath3,
                            "sde_testMethod2", 1201 + j, METHOD2_LINE + j));
                }
            }

            locationsData.sourceLocations.put(sourceName1, source1Locations);
            locationsData.sourceLocations.put(sourceName2, source2Locations);
            locationsData.sourceLocations.put(sourceName3, source3Locations);

            locationsData.allLocations.addAll(source1Locations);
            locationsData.allLocations.addAll(source2Locations);
            locationsData.allLocations.addAll(source3Locations);

            for (DebugLocation debugLocation : locationsData.allLocations) {
                smapStratum.addLineData(
                        debugLocation.inputLine,
                        debugLocation.sourceName,
                        1,
                        debugLocation.outputLine,
                        1);
            }

            smapGenerator.addStratum(smapStratum, false);

            testStratumData.put(stratumName, locationsData);
        }

        savePathcedClassFile(className, smapGenerator, smapFileName);

        addJavaLocations(testStratumData, true);

        return testStratumData;
    }

    // single input line is mapped to two output lines
    protected Map<String, LocationsData> prepareDefaultPatchedClassFile_Type3(String className, int testStratumCount,
            boolean setJavaStratumDefault) {
        /*
         * "Java" "TestStratum"
         *
         * <init>
         * 32 --> 1001, source1
         * 34 --> 1002, source1
         * 36 --> 1003, source1
         * 38 --> 1004, source1
         *
         * sde_testMethod1
         * 43 --> 1101, source1
         * 45 --> 1102, source1
         * 47 --> 1103, source1
         * 49 --> 1104, source1
         *
         * sde_testMethod2
         * 54 --> 1201, source1
         * 56 --> 1202, source1
         * 58 --> 1203, source1
         * 60 --> 1204, source1
         */

        Map<String, LocationsData> testStratumData = new TreeMap<String, LocationsData>();

        String smapFileName = "TestSMAP.smap";
        SmapGenerator smapGenerator = new SmapGenerator();

        for (int i = 0; i < testStratumCount; i++) {
            String stratumName = testStratumName + (i + 1);
            LocationsData locationsData = new LocationsData(stratumName);

            String sourceName = testStratumSourceName + (i + 1);
            String sourcePath = testStratumSourcePath + (i + 1);
            locationsData.paths.add(sourcePath);

            SmapStratum smapStratum = new SmapStratum(stratumName);

            List<DebugLocation> sourceLocations = new ArrayList<DebugLocation>();

            int baseLineNumber = 1000 * (i + 1);

            for (int j = 0; j < 4; j++) {
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "<init>", baseLineNumber + j, INIT_LINE + j * 2));
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "sde_testMethod1", baseLineNumber + 100 + j, METHOD1_LINE + j * 2));
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "sde_testMethod2", baseLineNumber + 200 + j, METHOD2_LINE + j * 2));
            }

            locationsData.allLocations.addAll(sourceLocations);
            locationsData.sourceLocations.put(sourceName, sourceLocations);

            testStratumData.put(stratumName, locationsData);

            smapStratum.addFile(sourceName, sourcePath);

            for (DebugLocation debugLocation : locationsData.allLocations) {
                smapStratum.addLineData(debugLocation.inputLine, sourceName, 1, debugLocation.outputLine, 1);
            }

            // if setJavaStratumDefault == false do first stratum default
            if (!setJavaStratumDefault && (i == 0)) {
                locationsData.isDefault = true;
                smapGenerator.addStratum(smapStratum, true);
            } else
                smapGenerator.addStratum(smapStratum, false);
        }

        savePathcedClassFile(className, smapGenerator, smapFileName);

        addJavaLocations(testStratumData, setJavaStratumDefault);

        return testStratumData;
    }

    // 3 different test stratums define disjoint locations sets
    protected Map<String, LocationsData> prepareDefaultPatchedClassFile_Type4(String className) {
        /*
         * "Java" "TestStratum1" "TestStratum2" "TestStratum3"
         *
         * <init>
         * 32 --> 1000
         * ...
         * ...
         * 39 --> 1007
         *
         * sde_testMethod1
         * 43 --> 1100
         * ...
         * ...
         * 50 --> 1107
         *
         * sde_testMethod2
         * 54 --> 1200
         * ...
         * ...
         * 61 --> 1207
         */
        Map<String, LocationsData> testStratumData = new TreeMap<String, LocationsData>();

        String smapFileName = "TestSMAP.smap";

        SmapGenerator smapGenerator = new SmapGenerator();

        String stratumName = testStratumName + "1";
        LocationsData locationsData = new LocationsData(stratumName);
        List<DebugLocation> sourceLocations = new ArrayList<DebugLocation>();
        String methodName = "<init>";
        for (int i = 0; i < 8; i++) {
            sourceLocations.add(new DebugLocation(
                    testStratumSourceName, testStratumSourcePath, methodName,
                    1000 + i, INIT_LINE + i));
        }
        locationsData.allLocations.addAll(sourceLocations);
        locationsData.sourceLocations.put(testStratumSourceName, sourceLocations);
        testStratumData.put(stratumName, locationsData);

        stratumName = testStratumName + "2";
        locationsData = new LocationsData(stratumName);
        sourceLocations = new ArrayList<DebugLocation>();
        methodName = "sde_testMethod1";
        for (int i = 0; i < 8; i++) {
            sourceLocations.add(new DebugLocation(
                    testStratumSourceName, testStratumSourcePath, methodName,
                    1100 + i, METHOD1_LINE + i));
        }
        locationsData.allLocations.addAll(sourceLocations);
        locationsData.sourceLocations.put(testStratumSourceName, sourceLocations);
        testStratumData.put(stratumName, locationsData);

        stratumName = testStratumName + "3";
        locationsData = new LocationsData(stratumName);
        sourceLocations = new ArrayList<DebugLocation>();
        methodName = "sde_testMethod2";
        for (int i = 0; i < 8; i++) {
            sourceLocations.add(new DebugLocation(testStratumSourceName, testStratumSourcePath,
                    methodName, 1200 + i, METHOD2_LINE + i));
        }
        locationsData.allLocations.addAll(sourceLocations);
        locationsData.sourceLocations.put(testStratumSourceName, sourceLocations);
        testStratumData.put(stratumName, locationsData);

        for (String stratum : testStratumData.keySet()) {
            SmapStratum smapStratum = new SmapStratum(stratum);
            smapStratum.addFile(testStratumSourceName, testStratumSourcePath);

            for (DebugLocation debugLocation : testStratumData.get(stratum).allLocations)
                smapStratum.addLineData(
                        debugLocation.inputLine,
                        debugLocation.sourceName,
                        1,
                        debugLocation.outputLine,
                        1);

            smapGenerator.addStratum(smapStratum, false);
        }

        savePathcedClassFile(className, smapGenerator, smapFileName);

        return testStratumData;
    }

    // single input line is mapped to the single output line, test stratum has 3
    // sources,
    // lines in each method has same numbers, each method has locations in 3
    // sources
    protected Map<String, LocationsData> prepareDefaultPatchedClassFile_Type5(String className, int testStratumCount) {
        /*
         * "Java" "TestStratum"
         *
         * <init>
         * 32 --> 1000, source1, path1
         * 33 --> 1000, source2, path2
         * 34 --> 1000, source3, path3
         * ...
         * ...
         *
         * sde_testMethod1
         * 43 --> 1100, source1, path1
         * 44 --> 1100, source2, path2
         * 45 --> 1100, source3, path3
         * ...
         * ...
         *
         * sde_testMethod2
         * 54 --> 1200, source1, path1
         * 55 --> 1200, source2, path2
         * 56 --> 1200, source3, path3
         * ...
         * ...
         */

        Map<String, LocationsData> testStratumData = new TreeMap<String, LocationsData>();

        String smapFileName = "TestSMAP.smap";
        SmapGenerator smapGenerator = new SmapGenerator();

        for (int i = 0; i < testStratumCount; i++) {
            String stratumName = testStratumName + (i + 1);
            SmapStratum smapStratum = new SmapStratum(stratumName);

            LocationsData locationsData = new LocationsData(stratumName);

            String sourceName1 = testStratumSourceName + (i + 1) + "_1";
            String sourcePath1 = testStratumSourcePath + (i + 1) + "_1";
            locationsData.paths.add(sourcePath1);
            smapStratum.addFile(sourceName1, sourcePath1);

            String sourceName2 = testStratumSourceName + (i + 1) + "_2";
            String sourcePath2 = testStratumSourcePath + (i + 1) + "_2";
            locationsData.paths.add(sourcePath2);
            smapStratum.addFile(sourceName2, sourcePath2);

            String sourceName3 = testStratumSourceName + (i + 1) + "_3";
            String sourcePath3 = testStratumSourcePath + (i + 1) + "_3";
            locationsData.paths.add(sourcePath3);
            smapStratum.addFile(sourceName3, sourcePath3);

            List<DebugLocation> source1Locations = new ArrayList<DebugLocation>();
            List<DebugLocation> source2Locations = new ArrayList<DebugLocation>();
            List<DebugLocation> source3Locations = new ArrayList<DebugLocation>();

            for (int j = 0; j < 8; j++) {
                if (j % 3 == 0) {
                    source1Locations.add(new DebugLocation(sourceName1, sourcePath1,
                            "<init>", 1000, INIT_LINE + j));
                    source1Locations.add(new DebugLocation(sourceName1, sourcePath1,
                            "sde_testMethod1", 1100, METHOD1_LINE + j));
                    source1Locations.add(new DebugLocation(sourceName1, sourcePath1,
                            "sde_testMethod2", 1200, METHOD2_LINE + j));
                } else if (j % 3 == 1) {
                    source2Locations.add(new DebugLocation(sourceName2, sourcePath2,
                            "<init>", 1000, INIT_LINE + j));
                    source2Locations.add(new DebugLocation(sourceName2, sourcePath2,
                            "sde_testMethod1", 1100, METHOD1_LINE + j));
                    source2Locations.add(new DebugLocation(sourceName2, sourcePath2,
                            "sde_testMethod2", 1200, METHOD2_LINE + j));
                } else {
                    source3Locations.add(new DebugLocation(sourceName3, sourcePath3,
                            "<init>", 1000, INIT_LINE + j));
                    source3Locations.add(new DebugLocation(sourceName3, sourcePath3,
                            "sde_testMethod1", 1100, METHOD1_LINE + j));
                    source3Locations.add(new DebugLocation(sourceName3, sourcePath3,
                            "sde_testMethod2", 1200, METHOD2_LINE + j));
                }
            }

            locationsData.sourceLocations.put(sourceName1, source1Locations);
            locationsData.sourceLocations.put(sourceName2, source2Locations);
            locationsData.sourceLocations.put(sourceName3, source3Locations);

            locationsData.allLocations.addAll(source1Locations);
            locationsData.allLocations.addAll(source2Locations);
            locationsData.allLocations.addAll(source3Locations);

            for (DebugLocation debugLocation : locationsData.allLocations) {
                smapStratum.addLineData(
                        debugLocation.inputLine,
                        debugLocation.sourceName,
                        1,
                        debugLocation.outputLine,
                        1);
            }

            smapGenerator.addStratum(smapStratum, false);

            testStratumData.put(stratumName, locationsData);
        }

        savePathcedClassFile(className, smapGenerator, smapFileName);

        return testStratumData;
    }

    public static File copyFile(File srcFile, String newFileName) throws IOException {
        FileChannel inChannel = new FileInputStream(srcFile).getChannel();

        File newFile = new File(newFileName);
        newFile.createNewFile();
        FileChannel outChannel = new FileOutputStream(newFile).getChannel();

        outChannel.transferFrom(inChannel, 0, inChannel.size());

        outChannel.close();
        inChannel.close();

        return newFile;
    }

    // find all locations of method with given name
    // (used to check result of 'Method.allLineLocations()')
    static protected List<DebugLocation> locationsOfMethod(List<DebugLocation> debugLocations, String methodName) {
        List<DebugLocation> result = new ArrayList<DebugLocation>();

        for (DebugLocation debugLocation : debugLocations) {
            if (debugLocation.methodName.equals(methodName))
                result.add(debugLocation);
        }

        return result;
    }

    // find all locations for given line and source name
    // (used to check result of 'Method.locationsOfLine()' and
    // 'ReferenceType.locationsOfLine()')
    static protected List<DebugLocation> locationsOfLine(List<DebugLocation> debugLocations, String sourceName,
            int lineNumber) {
        List<DebugLocation> result = new ArrayList<DebugLocation>();

        for (DebugLocation debugLocation : debugLocations) {
            if (debugLocation.isConform(sourceName, lineNumber))
                result.add(debugLocation);
        }

        return result;
    }

    // find locations unique by line number and source name
    // (used in 'check_ReferenceType_locationsOfLine' and
    // 'check_Method_locationsOfLine' to find all line numbers available for
    // ReferenceType or Method)
    static protected Set<DebugLocation> allUniqueLocations(List<DebugLocation> debugLocations) {
        Set<DebugLocation> result = new TreeSet<DebugLocation>();

        for (DebugLocation debugLocation : debugLocations) {
            if (!result.contains(debugLocation)) {
                result.add(debugLocation);
            }
        }

        return result;
    }

    // check is list of Locations contains only expected locations
    protected void compareLocations(List<Location> locations, List<DebugLocation> expectedLocations, String stratum) {
        boolean success = true;

        List<Location> tempLocations = new LinkedList<Location>(locations);

        List<DebugLocation> tempExpectedLocations = new LinkedList<DebugLocation>(expectedLocations);

        for(Iterator<Location> locationsIterator = tempLocations.iterator(); locationsIterator.hasNext();) {
            boolean isExpected = false;
            Location location = locationsIterator.next();

            for(Iterator<DebugLocation> expectedLocationsIterator = tempExpectedLocations.iterator(); expectedLocationsIterator.hasNext();) {
                DebugLocation expectedLocation = expectedLocationsIterator.next();
                if (expectedLocation.compare(location, stratum)) {
                    isExpected = true;
                    locationsIterator.remove();
                    expectedLocationsIterator.remove();
                    break;
                }
            }
            if (!isExpected) {
                success = false;
                log.complain("Location " + location + " were not found in expected locations");
            }
        }

        if (tempLocations.size() != 0) {
            success = false;
            setSuccess(false);
            log.complain("Not all locations were found in expected");
        }

        if (tempExpectedLocations.size() != 0) {
            success = false;
            setSuccess(false);
            log.complain("Following expected locations were not found in received");
            for (DebugLocation expectedLocation : tempExpectedLocations) {
                log.complain(expectedLocation.toString());
            }
        }

        if (!success) {
            setSuccess(false);
            log.complain("Expected and actual locations differ");

            log.complain("Actual locations: ");
            for (Location location : locations) {
                log.complain(locationToString(location, stratum));
            }

            log.complain("Expected locations: ");
            for (DebugLocation expectedLocation : expectedLocations) {
                log.complain(expectedLocation.toString());
            }
        }
    }

    // test all SDE related methods of ReferenceType
    protected void checkReferenceType(String stratum, ReferenceType referenceType, List<String> expectedSourceNames,
            List<String> expectedSourcePaths, List<DebugLocation> expectedLocations) {
        log.display("Check sourceNames");
        check_ReferenceType_sourceNames(referenceType, stratum, expectedSourceNames);
        log.display("Check sourcePaths");
        check_ReferenceType_sourcePaths(referenceType, stratum, expectedSourcePaths);
        log.display("Check allLocations");
        check_ReferenceType_allLineLocations(referenceType, stratum, expectedLocations);
        log.display("Check locationsOfLine");
        check_ReferenceType_locationsOfLine(referenceType, stratum, false, expectedLocations);

        for (Method method : referenceType.methods()) {
            List<DebugLocation> expectedLocationsOfMethod = locationsOfMethod(expectedLocations, method.name());

            log.display("Check allLineLocations for method '" + method.name() + "'");
            check_Method_allLineLocations(method, stratum, expectedLocationsOfMethod);
            log.display("Check locationsOfLine for method '" + method.name() + "'");
            check_Method_locationsOfLine(method, stratum, false, expectedLocationsOfMethod);
        }
    }

    // check is 'ReferenceType.sourceNames' returns only expected sources
    protected void check_ReferenceType_sourceNames(ReferenceType referenceType, String stratum,
            List<String> expectedSourceNames) {
        try {
            if (stratum == null) {
                String sourceName = referenceType.sourceName();
                String expectedSourceName = expectedSourceNames.get(0);

                if (!sourceName.equals(expectedSourceName)) {
                    setSuccess(false);
                    log.complain("Unexpected result of ReferenceType.sourceName(): " + sourceName + ", expected is "
                            + expectedSourceName);
                }
            } else {
                boolean success = true;

                List<String> sourceNames = referenceType.sourceNames(stratum);

                if (!expectedSourceNames.containsAll(sourceNames)) {
                    success = false;
                    log.complain("ReferenceType.sourceNames() returns unexpected names");
                }

                if (!sourceNames.containsAll(expectedSourceNames)) {
                    success = false;
                    log.complain("Not all expected source names was returned by ReferenceType.sourceNames()");
                }

                if (!success) {
                    log.complain("Expected source names:");
                    for (String name : expectedSourceNames)
                        log.complain(name);
                    log.complain("Actual source names:");
                    for (String name : sourceNames)
                        log.complain(name);
                }
            }
        } catch (AbsentInformationException e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }
    }

    // check is 'ReferenceType.sourcePaths' returns only expected paths
    protected void check_ReferenceType_sourcePaths(ReferenceType referenceType, String stratum,
            List<String> expectedSourcePaths) {
        try {
            boolean success = true;

            List<String> sourcePaths = referenceType.sourcePaths(stratum);

            if (!expectedSourcePaths.containsAll(sourcePaths)) {
                success = false;
                log.complain("ReferenceType.sourcePaths() returns unexpected paths");
            }

            if (!sourcePaths.containsAll(expectedSourcePaths)) {
                success = false;
                log.complain("Not all expected paths was returned by ReferenceType.sourcePaths()");
            }

            if (!success) {
                log.complain("Expected paths:");
                for (String path : expectedSourcePaths)
                    log.complain(path);
                log.complain("Actual paths:");
                for (String path : sourcePaths)
                    log.complain(path);
            }
        } catch (AbsentInformationException e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }
    }

    // check that method 'ReferenceType.allLineLocations' returns only expected
    // locations
    protected void check_ReferenceType_allLineLocations(ReferenceType referenceType, String stratum,
            List<DebugLocation> expectedLocations) {
        try {
            List<Location> locations = referenceType.allLineLocations();
            compareLocations(locations, expectedLocations, stratum);
        } catch (AbsentInformationException e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }

    }

    // check that method 'Method.allLineLocations' returns only expected
    // locations
    protected void check_Method_allLineLocations(Method method, String stratum,
            List<DebugLocation> expectedLocationsOfMethod) {
        try {
            List<Location> methodAllLineLocations = method.allLineLocations();
            compareLocations(methodAllLineLocations, expectedLocationsOfMethod, stratum);
        } catch (AbsentInformationException e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }
    }

    // for each line available for method check result of
    // 'Method.locationsOfLine'
    protected void check_Method_locationsOfLine(Method method, String stratum, boolean allSources,
            List<DebugLocation> expectedLocationsOfMethod) {
        try {
            for (DebugLocation uniqueLocation : allUniqueLocations(expectedLocationsOfMethod)) {
                String sourceName = allSources ? null : uniqueLocation.sourceName;

                List<DebugLocation> expectedLocationsOfLine = locationsOfLine(
                        expectedLocationsOfMethod,
                        sourceName,
                        uniqueLocation.inputLine);

                List<Location> locationsOfLine = (stratum == null) ? method.locationsOfLine(uniqueLocation.inputLine)
                        : method.locationsOfLine(stratum, sourceName, uniqueLocation.inputLine);

                compareLocations(locationsOfLine, expectedLocationsOfLine, stratum);
            }
        } catch (AbsentInformationException e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }
    }

    // for each line available for ReferenceType check result of
    // 'ReferenceType.locationsOfLine'
    protected void check_ReferenceType_locationsOfLine(ReferenceType referenceType, String stratum, boolean allSources,
            List<DebugLocation> expectedLocations) {
        try {
            for (DebugLocation uniqueLocation : allUniqueLocations(expectedLocations)) {
                String sourceName = allSources ? null : uniqueLocation.sourceName;

                List<DebugLocation> expectedLocationsOfLine = locationsOfLine(
                        expectedLocations,
                        sourceName,
                        uniqueLocation.inputLine);

                List<Location> locations = (stratum == null) ? referenceType.locationsOfLine(uniqueLocation.inputLine)
                        : referenceType.locationsOfLine(stratum, sourceName, uniqueLocation.inputLine);

                compareLocations(locations, expectedLocationsOfLine, stratum);
            }
        } catch (AbsentInformationException e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }

    }

    // check that method 'ReferenceType.availableStrata' returns only expected
    // stratums
    protected void check_ReferenceType_availableStrata(ReferenceType referenceType, List<String> expectedStrata) {
        boolean success = true;

        List<String> strata = referenceType.availableStrata();

        if (!expectedStrata.containsAll(strata)) {
            success = false;
            log.complain("ReferenceType.availableStrata() returns unexpected values");
        }

        if (!strata.containsAll(expectedStrata)) {
            success = false;
            log.complain("Not all expected stratums was returned by ReferenceType.availableStrata()");
        }

        if (!success) {
            log.complain("Expected stratums:");
            for (String name : expectedStrata)
                log.complain(name);
            log.complain("Actual stratums:");
            for (String name : strata)
                log.complain(name);
        }
    }

}
