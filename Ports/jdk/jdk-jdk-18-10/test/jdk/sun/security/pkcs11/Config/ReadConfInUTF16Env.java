/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8187023
 * @summary Pkcs11 config file should be assumed in ISO-8859-1
 * @library /test/lib
 * @run testng/othervm ReadConfInUTF16Env
 */

import jdk.test.lib.process.ProcessTools;
import org.testng.annotations.Test;

import java.security.Provider;
import java.security.Security;

public class ReadConfInUTF16Env {

    @Test
    public void testReadConfInUTF16Env() throws Exception {
        String[] testCommand = new String[] { "-Dfile.encoding=UTF-16",
                TestSunPKCS11Provider.class.getName()};
        ProcessTools.executeTestJvm(testCommand).shouldHaveExitValue(0);
    }

    static class TestSunPKCS11Provider {
        public static void main(String[] args) throws Exception {
            Provider p = Security.getProvider("SunPKCS11");
            if (p == null) {
                System.out.println("Skipping test - no PKCS11 provider available");
                return;
            }
            System.out.println(p.getName());
        }
    }
}
