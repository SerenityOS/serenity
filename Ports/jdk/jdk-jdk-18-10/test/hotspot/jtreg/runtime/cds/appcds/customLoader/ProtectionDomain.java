/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary AppCDS handling of protection domain in custom loaders.
 *
 * @requires vm.cds
 * @requires vm.cds.custom.loaders
 *
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile test-classes/ProtDomain.java
 * @run driver ProtectionDomain
 */

public class ProtectionDomain {
    public static void main(String[] args) throws Exception {
        String appJar = JarBuilder.build("ProtectionDomain-app", "ProtDomain");

        String customJar = JarBuilder.build("ProtectionDomain-custom",
            "ProtDomainClassForArchive", "ProtDomainNotForArchive");
        String[] classlist = new String[] {
            "java/lang/Object id: 1",
            "ProtDomain id: 2 super: 1 source: " + appJar,
            "ProtDomainClassForArchive id: 3 super: 1 source: " + customJar
        };

        TestCommon.testDump(appJar, classlist);

        // First class is loaded from CDS, second class is loaded from JAR
        TestCommon.checkExec(TestCommon.exec(appJar, "-verbose:class", "ProtDomain", customJar),
            "[class,load] ProtDomainClassForArchive source: shared objects file",
            "[class,load] ProtDomainNotForArchive source: file");
    }
}
