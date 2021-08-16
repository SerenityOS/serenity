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

/*
 * @test ProtectionDomainVerificationTest
 * @bug 8149064
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver ProtectionDomainVerificationTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;

public class ProtectionDomainVerificationTest {

    public static void main(String... args) throws Exception {

        // -Xlog:protectiondomain=trace
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xlog:protectiondomain=trace",
                                                                  "-Xmx128m",
                                                                  "-Djava.security.manager=allow",
                                                                  Hello.class.getName(), "security_manager");
        new OutputAnalyzer(pb.start())
        .shouldHaveExitValue(0)
        .shouldContain("[protectiondomain] Checking package access")
        .shouldContain("[protectiondomain] adding protection domain for class");

        // -Xlog:protectiondomain=debug
        pb = ProcessTools.createJavaProcessBuilder("-Xlog:protectiondomain=debug",
                                                                  "-Xmx128m",
                                                                  "-Djava.security.manager=allow",
                                                                  Hello.class.getName(), "security_manager");
        new OutputAnalyzer(pb.start())
        .shouldHaveExitValue(0)
        .shouldContain("[protectiondomain] Checking package access")
        .shouldNotContain("[protectiondomain] adding protection domain for class");

        // -Xlog:protectiondomain=debug
        pb = ProcessTools.createJavaProcessBuilder("-Xlog:protectiondomain=trace",
                                                   "-Xmx128m",
                                                   "-Djava.security.manager=disallow",
                                                   Hello.class.getName());
        new OutputAnalyzer(pb.start())
        .shouldHaveExitValue(0)
        .shouldNotContain("[protectiondomain] Checking package access")
        .shouldNotContain("pd set count = #");
    }

    public static class Hello {
        public static void main(String[] args) {
            if (args.length == 1) {
              // Need a security manager to trigger logging.
              System.setSecurityManager(new SecurityManager());
            }
            System.out.print("Hello!");
        }
    }
}
