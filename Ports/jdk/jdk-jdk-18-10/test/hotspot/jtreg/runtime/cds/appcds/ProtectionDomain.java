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
 * @summary AppCDS handling of protection domain.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/ProtDomain.java
 * @compile test-classes/ProtDomainB.java
 * @compile test-classes/JimageClassProtDomain.java
 * @run driver ProtectionDomain
 */

import jdk.test.lib.process.OutputAnalyzer;

public class ProtectionDomain {
  public static void main(String[] args) throws Exception {
    JarBuilder.build("prot_domain", "ProtDomain", "ProtDomainB", "ProtDomainOther",
                     "ProtDomainBOther", "JimageClassProtDomain");

    String appJar = TestCommon.getTestJar("prot_domain.jar");
    TestCommon.testDump(appJar,
         TestCommon.list("ProtDomain",
                         "ProtDomainBOther",
                         "java/util/Dictionary",
                         "com/sun/tools/javac/Main",
                         "jdk/nio/zipfs/ZipInfo",
                         "java/net/URL",
                         "com/sun/jndi/dns/DnsName"));

    // First class is loaded from CDS, second class is loaded from JAR
    TestCommon.run("-cp", appJar, "ProtDomain")
        .assertNormalExit("Protection Domains match");

    // First class is loaded from JAR, second class is loaded from CDS
    TestCommon.run("-cp", appJar, "ProtDomainB")
        .assertNormalExit("Protection Domains match");

    // Test ProtectionDomain for application and extension module classes from the
    // "modules" jimage
    TestCommon.run("-cp", appJar, "JimageClassProtDomain")
        .assertNormalExit(output -> output.shouldNotContain(
                          "Failed: Protection Domains do not match"));
  }
}
