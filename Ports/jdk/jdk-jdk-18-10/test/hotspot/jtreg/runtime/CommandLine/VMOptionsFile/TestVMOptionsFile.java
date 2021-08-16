/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8061999 8135195 8136552
 * @summary Test "-XX:VMOptionsFile" VM option
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules jdk.management
 * @run driver TestVMOptionsFile
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import jdk.test.lib.Asserts;
import jdk.test.lib.management.DynamicVMOption;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestVMOptionsFile {

    /* Various valid VM Option files */
    private static final String VM_OPTION_FILE_EMPTY = "optionfile_empty";
    private static final String VM_OPTION_FILE_TABS_AND_SPACES = "optionfile_only_tabsandspaces";
    private static final String VM_OPTION_FILE_1 = "optionfile_1";
    private static final String VM_OPTION_FILE_2 = "optionFILE_2";
    private static final String VM_OPTION_FILE_3 = "optionfile_3";
    private static final String VM_OPTION_FILE_QUOTE = "optionfile_quote";
    private static final String VM_OPTION_FILE_BIG = "optionfile_big";
    private static final int REPEAT_COUNT = 512;
    /* Name of the file with flags for VM_OPTION_FILE_2 Option file */
    private static final String FLAGS_FILE = "flags_file";
    /* VM Option file with a lot of options with quote on separate lines */
    private static final String VM_OPTION_FILE_LOT_OF_OPTIONS_QUOTE = "optionfile_lot_of_options_quote";
    /* Number of properties defined in VM_OPTION_FILE_LOT_OF_OPTIONS_QUOTE */
    private static final int NUM_OF_PROP_IN_FILE_LOT_OF_OPTIONS_QUOTE = 70;
    /* VM Option file with long property */
    private static final String VM_OPTION_FILE_WITH_LONG_PROPERTY = "optionfile_long_property";
    private static final String LONG_PROPERTY_NAME = "veryl'" + String.format("%1536s", "").replace(' ', 'o') + "ng'name";
    private static final String LONG_PROPERTY_VALUE = String.format("%2096s", "").replaceAll("    ", "long");
    /* 2 VM Option files with unmatched quotes */
    private static final String VM_OPTION_FILE_UNMATCHED_QUOTE_1 = "optionfile_unmatched_quote_1";
    private static final String VM_OPTION_FILE_UNMATCHED_QUOTE_2 = "optionfile_unmatched_quote_2";
    /* VM Option file with bad option in it */
    private static final String VM_OPTION_FILE_WITH_BAD_OPTION = "optionfile_bad_option";
    /* VM Option file with "-XX:VMOptionsFile=" option in it */
    private static final String VM_OPTION_FILE_WITH_VM_OPTION_FILE = "optionfile_with_optionfile";
    /* VM Option file with "-XX:VMOptionsFile=" option in it, where file is the same option file */
    private static final String VM_OPTION_FILE_WITH_SAME_VM_OPTION_FILE = "optionfile_with_same_optionfile";
    /* VM Option file without read permissions(not accessible) */
    private static final String VM_OPTION_FILE_WITHOUT_READ_PERMISSIONS = "optionfile_wo_read_perm";
    /* VM Option file which does not exist */
    private static final String NOT_EXISTING_FILE = "not_exist_junk2123";

    /* JAVA_TOOL_OPTIONS environment variable */
    private static final String JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS";
    /* _JAVA_OPTIONS environment variable */
    private static final String JAVA_OPTIONS = "_JAVA_OPTIONS";

    /* Exit code for JVM, zero - for success, non-zero for failure */
    private static final int JVM_SUCCESS = 0;
    private static final int JVM_FAIL_WITH_EXIT_CODE_1 = 1;

    /* Current working directory */
    private static final String CURRENT_DIR = System.getProperty("user.dir");

    /* Source directory */
    private static final String SOURCE_DIR = System.getProperty("test.src", ".");

    /* VM Options which are passed to the JVM */
    private static final List<String> VMParams = new ArrayList<>();
    /* Argument passed to the PrintPropertyAndOptions.main */
    private static final Set<String> appParams = new LinkedHashSet<>();

    private static OutputAnalyzer output;

    private static final String PRINT_PROPERTY_FORMAT = "Property %s=%s";
    private static final String PRINT_VM_OPTION_FORMAT = "Virtual Machine option %s=%s";

    /*
     * Get absoulte path to file from folder with sources
     */
    private static String getAbsolutePathFromSource(String fileName) {
        return SOURCE_DIR + File.separator + fileName;
    }

    /*
     * Make file non-readable by modifying its permissions.
     * If file supports "posix" attributes, then modify it.
     * Otherwise check for "acl" attributes.
     */
    private static void makeFileNonReadable(String file) throws IOException {
        Path filePath = Paths.get(file);
        Set<String> supportedAttr = filePath.getFileSystem().supportedFileAttributeViews();

        if (supportedAttr.contains("posix")) {
            Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("-w--w----"));
        } else if (supportedAttr.contains("acl")) {
            UserPrincipal fileOwner = Files.getOwner(filePath);

            AclFileAttributeView view = Files.getFileAttributeView(filePath, AclFileAttributeView.class);

            AclEntry entry = AclEntry.newBuilder()
                    .setType(AclEntryType.DENY)
                    .setPrincipal(fileOwner)
                    .setPermissions(AclEntryPermission.READ_DATA)
                    .build();

            List<AclEntry> acl = view.getAcl();
            acl.add(0, entry);
            view.setAcl(acl);
        }
    }

    private static void copyFromSource(String fileName) throws IOException {
        Files.copy(Paths.get(getAbsolutePathFromSource(fileName)),
                Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void createOptionFiles() throws IOException {
        FileWriter fw = new FileWriter(VM_OPTION_FILE_WITH_VM_OPTION_FILE);

        /* Create VM option file with following parameters "-XX:VMOptionFile=<absolute_path_to_the_VM_option_file> */
        fw.write("-XX:VMOptionsFile=" + getAbsolutePathFromSource(VM_OPTION_FILE_1));
        fw.close();

        /* Create VM option file with following parameters "-XX:MinHeapFreeRatio=12 -XX:VMOptionFile=<absolute_path_to_the_same_VM_option_file> */
        fw = new FileWriter(VM_OPTION_FILE_WITH_SAME_VM_OPTION_FILE);
        fw.write("-XX:MinHeapFreeRatio=12 -XX:VMOptionsFile=" + (new File(VM_OPTION_FILE_WITH_SAME_VM_OPTION_FILE)).getCanonicalPath());
        fw.close();

        /* Create VM option file with long property */
        fw = new FileWriter(VM_OPTION_FILE_WITH_LONG_PROPERTY);
        fw.write("-D" + LONG_PROPERTY_NAME + "=" + LONG_PROPERTY_VALUE);
        fw.close();

        /* Create big VM option file */
        fw = new FileWriter(VM_OPTION_FILE_BIG);
        fw.write("-XX:MinHeapFreeRatio=17\n");
        for (int i = 0; i < REPEAT_COUNT; i++) {
            if (i == REPEAT_COUNT / 2) {
                fw.write("-XX:+PrintVMOptions ");
            }
            fw.write("-Dmy.property=value" + (i + 1) + "\n");
        }
        fw.write("-XX:MaxHeapFreeRatio=85\n");
        fw.close();

        /* Copy valid VM option file and change its permission to make it not accessible */
        Files.copy(Paths.get(getAbsolutePathFromSource(VM_OPTION_FILE_1)),
                Paths.get(VM_OPTION_FILE_WITHOUT_READ_PERMISSIONS),
                StandardCopyOption.REPLACE_EXISTING);

        makeFileNonReadable(VM_OPTION_FILE_WITHOUT_READ_PERMISSIONS);

        /* Copy valid VM option file to perform test with relative path */
        copyFromSource(VM_OPTION_FILE_2);

        /* Copy flags file to the current working folder */
        copyFromSource(FLAGS_FILE);

        /* Create a new empty file */
        new File(VM_OPTION_FILE_EMPTY).createNewFile();
    }

    /*
     * Add parameters to the VM Parameters list
     */
    private static void addVMParam(String... params) {
        VMParams.addAll(Arrays.asList(params));
    }

    /*
     * Add VM option name to the application arguments list
     */
    private static void addVMOptionsToCheck(String... params) {
        for (String param : params) {
            appParams.add("vmoption=" + param);
        }
    }

    /*
     * Add property to the VM Params list and to the application arguments list
     */
    private static void addProperty(String propertyName, String propertyValue) {
        addVMParam("-D" + propertyName + "=" + propertyValue);
    }

    /*
     * Add "-XX:VMOptionsfile" parameter to the VM Params list
     */
    private static void addVMOptionsFile(String fileName) {
        addVMParam("-XX:VMOptionsFile=" + fileName);
    }

    private static void outputShouldContain(String expectedString) {
        output.shouldContain(expectedString);
    }

    private static void outputShouldNotContain(String expectedString) {
        output.shouldNotContain(expectedString);
    }

    private static ProcessBuilder createProcessBuilder() throws Exception {
        ProcessBuilder pb;
        List<String> runJava = new ArrayList<>();

        runJava.addAll(VMParams);
        runJava.add(PrintPropertyAndOptions.class.getName());
        runJava.addAll(appParams);

        pb = ProcessTools.createJavaProcessBuilder(runJava);

        VMParams.clear();
        appParams.clear();

        return pb;
    }

    private static void runJavaCheckExitValue(ProcessBuilder pb, int expectedExitValue) throws Exception {
        output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(expectedExitValue);
    }

    private static void runJavaCheckExitValue(int expectedExitValue) throws Exception {
        runJavaCheckExitValue(createProcessBuilder(), expectedExitValue);
    }

    /*
     * Update environment variable in passed ProcessBuilder object to the passed value
     */
    private static void updateEnvironment(ProcessBuilder pb, String name, String value) {
        pb.environment().put(name, value);
    }

    /*
     * Check property value by examining output
     */
    private static void checkProperty(String property, String expectedValue) {
        outputShouldContain(String.format(PRINT_PROPERTY_FORMAT, property, expectedValue));
    }

    /*
     * Check VM Option value by examining output
     */
    private static void checkVMOption(String vmOption, String expectedValue) {
        outputShouldContain(String.format(PRINT_VM_OPTION_FORMAT, vmOption, expectedValue));
    }

    private static void testVMOptions() throws Exception {
        ProcessBuilder pb;

        /* Check that empty VM Option file is accepted without errors */
        addVMOptionsFile(VM_OPTION_FILE_EMPTY);

        runJavaCheckExitValue(JVM_SUCCESS);

        /* Check that VM Option file with tabs and spaces is accepted without errors */
        addVMOptionsFile(getAbsolutePathFromSource(VM_OPTION_FILE_TABS_AND_SPACES));

        runJavaCheckExitValue(JVM_SUCCESS);

        /* Check that parameters are gotten from first VM Option file. Pass absolute path to the VM Option file */
        addVMParam("-showversion");
        addVMOptionsFile(getAbsolutePathFromSource(VM_OPTION_FILE_1));
        addVMOptionsToCheck("SurvivorRatio", "MinHeapFreeRatio");

        runJavaCheckExitValue(JVM_SUCCESS);
        outputShouldContain("interpreted mode");
        checkProperty("optfile_1", "option_file_1");
        checkVMOption("SurvivorRatio", "16");
        checkVMOption("MinHeapFreeRatio", "22");

        /*
         * Check that parameters are gotten from second VM Option file which also contains flags file.
         * Flags file and option file contains NewRatio, but since options from VM Option file
         * are processed later NewRatio should be set to value from VM Option file
         * Pass relative path to the VM Option file in form "vmoptionfile"
         */
        addVMOptionsFile(VM_OPTION_FILE_2);
        addVMOptionsToCheck("UseGCOverheadLimit", "NewRatio", "MinHeapFreeRatio", "MaxFDLimit", "AlwaysPreTouch");

        runJavaCheckExitValue(JVM_SUCCESS);
        checkProperty("javax.net.ssl.keyStorePassword", "someVALUE123+");
        checkVMOption("UseGCOverheadLimit", "true");
        checkVMOption("NewRatio", "4");
        checkVMOption("MinHeapFreeRatio", "3");
        checkVMOption("MaxFDLimit", "true");
        checkVMOption("AlwaysPreTouch", "false");

        /* Check that parameters are gotten from third VM Option file which contains a mix of the options */
        addVMParam("-showversion");
        addVMOptionsFile(getAbsolutePathFromSource(VM_OPTION_FILE_3));
        addVMOptionsToCheck("UseGCOverheadLimit", "NewRatio");

        runJavaCheckExitValue(JVM_SUCCESS);
        outputShouldContain("interpreted mode");
        checkProperty("other.secret.data", "qwerty");
        checkProperty("property", "second");
        checkVMOption("UseGCOverheadLimit", "false");
        checkVMOption("NewRatio", "16");

        /* Check that quotes are processed normally in VM Option file */
        addVMParam("-showversion");
        addVMOptionsFile(getAbsolutePathFromSource(VM_OPTION_FILE_QUOTE));
        addVMOptionsToCheck("ErrorFile");

        runJavaCheckExitValue(JVM_SUCCESS);

        outputShouldContain("interpreted mode");
        checkProperty("my.quote.single", "Property in single quote. Here a double qoute\" Add some slashes \\/");
        checkProperty("my.quote.double", "Double qoute. Include single '.");
        checkProperty("javax.net.ssl.trustStorePassword", "data @+NEW");
        checkVMOption("ErrorFile", "./my error file");

        /*
         * Verify that VM Option file accepts a file with 70 properties and with two options on separate
         * lines and properties that use quotes a lot.
         */
        addVMOptionsFile(getAbsolutePathFromSource(VM_OPTION_FILE_LOT_OF_OPTIONS_QUOTE));
        addVMOptionsToCheck("MinHeapFreeRatio", "MaxHeapFreeRatio");

        runJavaCheckExitValue(JVM_SUCCESS);

        for (int i = 1; i <= NUM_OF_PROP_IN_FILE_LOT_OF_OPTIONS_QUOTE; i++) {
            checkProperty(String.format("prop%02d", i), String.format("%02d", i));
        }
        checkVMOption("MinHeapFreeRatio", "7");
        checkVMOption("MaxHeapFreeRatio", "96");

        /*
         * Verify that VM Option file accepts a file with very long property.
         */
        addVMOptionsFile(VM_OPTION_FILE_WITH_LONG_PROPERTY);

        runJavaCheckExitValue(JVM_SUCCESS);

        checkProperty(LONG_PROPERTY_NAME.replaceAll("'", ""), LONG_PROPERTY_VALUE);

        /*
         * Verify that VM Option file accepts a big VM Option file
         */
        addVMOptionsFile(VM_OPTION_FILE_BIG);
        addVMOptionsToCheck("MinHeapFreeRatio");
        addVMOptionsToCheck("MaxHeapFreeRatio");

        runJavaCheckExitValue(JVM_SUCCESS);

        outputShouldContain("VM option '+PrintVMOptions'");
        checkProperty("my.property", "value" + REPEAT_COUNT);
        checkVMOption("MinHeapFreeRatio", "17");
        checkVMOption("MaxHeapFreeRatio", "85");

        /* Pass VM Option file in _JAVA_OPTIONS environment variable */
        addVMParam("-showversion");
        addVMOptionsToCheck("SurvivorRatio", "MinHeapFreeRatio");
        pb = createProcessBuilder();

        updateEnvironment(pb, JAVA_OPTIONS, "-XX:VMOptionsFile=" + getAbsolutePathFromSource(VM_OPTION_FILE_1));

        runJavaCheckExitValue(pb, JVM_SUCCESS);
        outputShouldContain("interpreted mode");
        checkProperty("optfile_1", "option_file_1");
        checkVMOption("SurvivorRatio", "16");
        checkVMOption("MinHeapFreeRatio", "22");

        /* Pass VM Option file in JAVA_TOOL_OPTIONS environment variable */
        addVMOptionsToCheck("UseGCOverheadLimit", "NewRatio", "MinHeapFreeRatio", "MaxFDLimit", "AlwaysPreTouch");
        pb = createProcessBuilder();

        updateEnvironment(pb, JAVA_TOOL_OPTIONS, "-XX:VMOptionsFile=" + VM_OPTION_FILE_2);

        runJavaCheckExitValue(pb, JVM_SUCCESS);
        checkProperty("javax.net.ssl.keyStorePassword", "someVALUE123+");
        checkVMOption("UseGCOverheadLimit", "true");
        checkVMOption("NewRatio", "4");
        checkVMOption("MinHeapFreeRatio", "3");
        checkVMOption("MaxFDLimit", "true");
        checkVMOption("AlwaysPreTouch", "false");
    }

    private static ProcessBuilder prepareTestCase(int testCase) throws Exception {
        ProcessBuilder pb;

        Asserts.assertTrue(0 < testCase && testCase < 6, "testCase should be from 1 to 5");

        addVMParam("-showversion");
        addVMOptionsToCheck("MinHeapFreeRatio", "SurvivorRatio", "NewRatio");

        if (testCase < 5) {
            addVMParam("-XX:Flags=flags_file", "-XX:-PrintVMOptions");
            addProperty("shared.property", "command_line_before");
            addProperty("clb", "unique_command_line_before");
            addVMParam("-XX:MinHeapFreeRatio=7");
        }

        if (testCase < 4) {
            addVMOptionsFile(getAbsolutePathFromSource(VM_OPTION_FILE_1));
        }

        if (testCase < 3) {
            addVMParam("-XX:MinHeapFreeRatio=9", "-XX:-PrintVMOptions");
            addProperty("shared.property", "command_line_after");
            addProperty("cla", "unique_command_line_after");
        }

        /* Create ProcessBuilder after all setup is done to update environment variables */
        pb = createProcessBuilder();

        if (testCase < 2) {
            updateEnvironment(pb, JAVA_OPTIONS, "-Dshared.property=somevalue -Djo=unique_java_options "
                    + "-XX:MinHeapFreeRatio=18 -Dshared.property=java_options -XX:MinHeapFreeRatio=11 -XX:+PrintVMOptions");
        }

        if (testCase < 6) {
            updateEnvironment(pb, JAVA_TOOL_OPTIONS, "-Dshared.property=qwerty -Djto=unique_java_tool_options "
                    + "-XX:MinHeapFreeRatio=15 -Dshared.property=java_tool_options -XX:MinHeapFreeRatio=6 -XX:+PrintVMOptions");
        }

        return pb;
    }

    private static void testVMOptionsLastArgumentsWins() throws Exception {
        ProcessBuilder pb;

        /*
         * "shared.property" property and "MinHeapFreeRatio" XX VM Option are defined
         * in flags file, JAVA_TOOL_OPTIONS and _JAVA_OPTIONS environment variables,
         * on command line before VM Option file, on command line after VM Option file
         * and also in VM Option file. Verify that last argument wins. Also check
         * unique properties and VM Options.
         * Here is the order of options processing and last argument wins:
         *    1) Flags file
         *    2) JAVA_TOOL_OPTIONS environment variables
         *    3) Pseudo command line from launcher
         *    4) _JAVA_OPTIONS
         * In every category arguments processed from left to right and from up to down
         * and the last processed arguments wins, i.e. if argument is defined several
         * times the value of argument will be equal to the last processed argument.
         *
         * "shared.property" property and "MinHeapFreeRatio" should be equal to the
         * value from _JAVA_OPTIONS environment variable
         */
        pb = prepareTestCase(1);

        runJavaCheckExitValue(pb, JVM_SUCCESS);

        outputShouldContain("interpreted mode");
        outputShouldContain("VM option '+PrintVMOptions'");
        checkProperty("shared.property", "java_options");
        checkVMOption("MinHeapFreeRatio", "11");
        /* Each category defines its own properties */
        checkProperty("jto", "unique_java_tool_options");
        checkProperty("jo", "unique_java_options");
        checkProperty("clb", "unique_command_line_before");
        checkProperty("optfile_1", "option_file_1");
        checkProperty("cla", "unique_command_line_after");
        /* SurvivorRatio defined only in VM Option file */
        checkVMOption("SurvivorRatio", "16");
        /* NewRatio defined only in flags file */
        checkVMOption("NewRatio", "5");

        /*
         * The same as previous but without _JAVA_OPTIONS environment variable.
         * "shared.property" property and "MinHeapFreeRatio" should be equal to the
         * value from pseudo command line after VM Option file
         */
        pb = prepareTestCase(2);

        runJavaCheckExitValue(pb, JVM_SUCCESS);

        outputShouldContain("interpreted mode");
        outputShouldNotContain("VM option '+PrintVMOptions'");
        checkProperty("shared.property", "command_line_after");
        checkVMOption("MinHeapFreeRatio", "9");

        /*
         * The same as previous but without arguments in pseudo command line after
         * VM Option file.
         * "shared.property" property and "MinHeapFreeRatio" should be equal to the
         * value from VM Option file.
         */
        pb = prepareTestCase(3);

        runJavaCheckExitValue(pb, JVM_SUCCESS);

        outputShouldContain("interpreted mode");
        outputShouldContain("VM option '+PrintVMOptions'");
        checkProperty("shared.property", "vmoptfile");
        checkVMOption("MinHeapFreeRatio", "22");

        /*
         * The same as previous but without arguments in VM Option file.
         * "shared.property" property and "MinHeapFreeRatio" should be equal to the
         * value from pseudo command line.
         */
        pb = prepareTestCase(4);

        runJavaCheckExitValue(pb, JVM_SUCCESS);

        outputShouldNotContain("VM option '+PrintVMOptions'");
        checkProperty("shared.property", "command_line_before");
        checkVMOption("MinHeapFreeRatio", "7");

        /*
         * The same as previous but without arguments from pseudo command line.
         * "shared.property" property and "MinHeapFreeRatio" should be equal to the
         * value from JAVA_TOOL_OPTIONS environment variable.
         */
        pb = prepareTestCase(5);

        runJavaCheckExitValue(pb, JVM_SUCCESS);

        outputShouldContain("VM option '+PrintVMOptions'");
        checkProperty("shared.property", "java_tool_options");
        checkVMOption("MinHeapFreeRatio", "6");
    }

    private static void testVMOptionsInvalid() throws Exception {
        ProcessBuilder pb;

        /* Pass directory instead of file */
        addVMOptionsFile(CURRENT_DIR);

        runJavaCheckExitValue(JVM_FAIL_WITH_EXIT_CODE_1);

        /* Pass not existing file */
        addVMOptionsFile(getAbsolutePathFromSource(NOT_EXISTING_FILE));

        runJavaCheckExitValue(JVM_FAIL_WITH_EXIT_CODE_1);
        outputShouldContain("Could not open options file");

        /* Pass VM option file with bad option */
        addVMOptionsFile(getAbsolutePathFromSource(VM_OPTION_FILE_WITH_BAD_OPTION));

        runJavaCheckExitValue(JVM_FAIL_WITH_EXIT_CODE_1);
        outputShouldContain("Unrecognized VM option");

        /* Pass VM option file with same VM option file option in it */
        addVMOptionsFile(VM_OPTION_FILE_WITH_SAME_VM_OPTION_FILE);

        runJavaCheckExitValue(JVM_FAIL_WITH_EXIT_CODE_1);
        outputShouldContain("A VM options file may not refer to a VM options file. Specification of '-XX:VMOptionsFile=<file-name>' in the options file");

        /* Pass VM option file with VM option file option in it */
        addVMOptionsFile(VM_OPTION_FILE_WITH_VM_OPTION_FILE);

        runJavaCheckExitValue(JVM_FAIL_WITH_EXIT_CODE_1);
        outputShouldContain("A VM options file may not refer to a VM options file. Specification of '-XX:VMOptionsFile=<file-name>' in the options file");

        /* Pass VM option file which is not accessible (without read permissions) */
        addVMOptionsFile(getAbsolutePathFromSource(VM_OPTION_FILE_WITHOUT_READ_PERMISSIONS));

        runJavaCheckExitValue(JVM_FAIL_WITH_EXIT_CODE_1);
        outputShouldContain("Could not open options file");

        /* Pass two VM option files */
        addVMOptionsFile(getAbsolutePathFromSource(VM_OPTION_FILE_1));
        addVMOptionsFile(VM_OPTION_FILE_2);

        runJavaCheckExitValue(JVM_FAIL_WITH_EXIT_CODE_1);
        outputShouldContain("is already specified in the");

        /* Pass empty option file i.e. pass "-XX:VMOptionsFile=" */
        addVMOptionsFile("");

        runJavaCheckExitValue(JVM_FAIL_WITH_EXIT_CODE_1);
        outputShouldContain("Could not open options file");

        /* Pass VM option file with unmatched single quote */
        addVMOptionsFile(getAbsolutePathFromSource(VM_OPTION_FILE_UNMATCHED_QUOTE_1));

        runJavaCheckExitValue(JVM_FAIL_WITH_EXIT_CODE_1);
        outputShouldContain("Unmatched quote in");

        /* Pass VM option file with unmatched double quote in X option */
        addVMOptionsFile(getAbsolutePathFromSource(VM_OPTION_FILE_UNMATCHED_QUOTE_2));

        runJavaCheckExitValue(JVM_FAIL_WITH_EXIT_CODE_1);
        outputShouldContain("Unmatched quote in");
    }

    public static void main(String[] args) throws Exception {
        /*
         * Preparation before actual testing - create two VM Option files
         * which contains VM Option file in it and copy other files to the
         * current working folder
         */
        createOptionFiles();

        testVMOptions(); /* Test VM Option file general functionality */
        testVMOptionsLastArgumentsWins(); /* Verify that last argument wins */
        testVMOptionsInvalid(); /* Test invalid VM Option file functionality */

    }

    public static class PrintPropertyAndOptions {

        public static void main(String[] arguments) {
            String vmOption;
            Properties properties = System.getProperties();

            for (String propertyName : properties.stringPropertyNames()) {
                System.out.println(String.format(PRINT_PROPERTY_FORMAT, propertyName, System.getProperty(propertyName, "NOT DEFINED")));
            }

            for (String arg : arguments) {
                if (arg.startsWith("vmoption=")) {
                    vmOption = arg.substring(9);
                    System.out.println(String.format(PRINT_VM_OPTION_FORMAT, vmOption, new DynamicVMOption(vmOption).getValue()));
                }
            }
        }
    }
}
