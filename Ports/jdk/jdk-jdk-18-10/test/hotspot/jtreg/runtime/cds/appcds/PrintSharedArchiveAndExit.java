/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @summary test the -XX:+PrintSharedArchiveAndExit flag
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @compile test-classes/HelloMore.java
 * @run main/othervm/timeout=3600 PrintSharedArchiveAndExit
 */

import java.io.File;
import jdk.test.lib.process.OutputAnalyzer;

public class PrintSharedArchiveAndExit {
  private static void check(OutputAnalyzer output, int ret, boolean checkContain, String... matches) throws Exception {
    // Tests specific to this test
    TestCommon.checkExecReturn(output, ret, checkContain, matches);

    // In all test case, we should never print out the following due to
    // PrintSharedArchiveAndExit. JVM should have been terminated
    // before reaching these outputs.
    TestCommon.checkExecReturn(output, ret, false,
                               "Usage:",            // JVM help message
                               "java version",      // JVM version
                               "Hello World");      // output from the Hello.class in hello.jar
  }

  private static void log(String msg) {
    System.out.println(">---------------------------------------------------------------------");
    System.out.println(msg);
    System.out.println("<---------------------------------------------------------------------");
  }

  public static void main(String[] args) throws Exception {
    String appJar = JarBuilder.getOrCreateHelloJar();
    String appJar2 = JarBuilder.build("PrintSharedArchiveAndExit-more", "HelloMore");

    String cp = appJar + File.pathSeparator + appJar2;
    String lastCheckMsg = "checking shared classpath entry: " + appJar2; // the last JAR to check

    TestCommon.testDump(cp, TestCommon.list("Hello", "HelloMore"));

    log("Normal execution -- all the JAR paths should be checked");
    TestCommon.run(
        "-cp", cp,
        "-XX:+PrintSharedArchiveAndExit")
      .ifNoMappingFailure(output -> check(output, 0, true, lastCheckMsg));

    TestCommon.run(
        "-cp", cp,
        "-XX:+PrintSharedArchiveAndExit",
        "-XX:+PrintSharedDictionary")  // Test PrintSharedDictionary as well.
      .ifNoMappingFailure(output -> check(output, 0, true, lastCheckMsg, "java.lang.Object"));

    log("Normal execution -- Make sure -version, help message and app main()\n" +
        "class are not invoked. These are checked inside check().");
    TestCommon.run("-cp", cp, "-XX:+PrintSharedArchiveAndExit", "-version")
      .ifNoMappingFailure(output -> check(output, 0, true, lastCheckMsg));

    TestCommon.run("-cp", cp, "-XX:+PrintSharedArchiveAndExit", "-help")
      .ifNoMappingFailure(output -> check(output, 0, true, lastCheckMsg));

    TestCommon.run("-cp", cp, "-XX:+PrintSharedArchiveAndExit", "Hello")
      .ifNoMappingFailure(output -> check(output, 0, true, lastCheckMsg));

    log("Execution with simple errors -- with 'simple' errors like missing or modified\n" +
        "JAR files, the VM should try to continue to print the remaining information.\n" +
        "Use an invalid Boot CP -- all the JAR paths should be checked");
    TestCommon.run(
        "-cp", cp,
        "-Xbootclasspath/a:foo.jar",
        "-XX:+PrintSharedArchiveAndExit")
      .ifNoMappingFailure(output -> check(output, 1, true, lastCheckMsg, "[BOOT classpath mismatch, "));

    log("Use an App CP shorter than the one at dump time -- all the JAR paths should be checked");
    TestCommon.run(
        "-cp", ".",
        "-XX:+PrintSharedArchiveAndExit")
      .ifNoMappingFailure(output -> check(output, 1, true, lastCheckMsg, "Run time APP classpath is shorter than the one at dump time: ."));

    log("Use an invalid App CP -- all the JAR paths should be checked.\n" +
        "Non-existing jar files will be ignored.");
    String invalidCP = "non-existing-dir" + File.pathSeparator + cp;
    TestCommon.run(
        "-cp", invalidCP,
        "-XX:+PrintSharedArchiveAndExit")
      .ifNoMappingFailure(output -> check(output, 0, true, lastCheckMsg));

    log("Changed modification time of hello.jar -- all the JAR paths should be checked");
    (new File(appJar)).setLastModified(System.currentTimeMillis() + 2000);
    TestCommon.run(
        "-cp", cp,
        "-XX:+PrintSharedArchiveAndExit")
      .ifNoMappingFailure(output -> check(output, 1, true, lastCheckMsg, "[Timestamp mismatch]"));

    log("Even if hello.jar is out of date, we should still be able to print the dictionary.");
    TestCommon.run(
        "-cp", cp,
        "-XX:+PrintSharedArchiveAndExit",
        "-XX:+PrintSharedDictionary")  // Test PrintSharedDictionary as well.
      .ifNoMappingFailure(output -> check(output, 1, true, lastCheckMsg, "java.lang.Object"));


    log("Remove hello.jar -- all the JAR paths should be checked");
    (new File(appJar)).delete();
    TestCommon.run(
        "-cp", cp,
        "-XX:+PrintSharedArchiveAndExit")
      .ifNoMappingFailure(output -> check(output, 1, true, lastCheckMsg, "[Required classpath entry does not exist: " + appJar + "]"));

    log("Execution with major errors -- with 'major' errors like the JSA file\n" +
        "is missing, we should stop immediately to avoid crashing the JVM.");
    TestCommon.run(
        "-cp", cp,
        "-XX:+PrintSharedArchiveAndExit",
        "-XX:SharedArchiveFile=./no-such-fileappcds.jsa")
      .ifNoMappingFailure(output -> check(output, 1, false, lastCheckMsg));
  }
}
