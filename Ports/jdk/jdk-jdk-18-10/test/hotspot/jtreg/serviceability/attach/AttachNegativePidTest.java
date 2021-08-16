/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test Verifies that negative pids are correctly rejected
 * @bug 8229957
 * @requires os.family != "windows"
 * @library /test/lib
 * @modules jdk.attach/com.sun.tools.attach
 * @run driver AttachNegativePidTest
 */

import java.io.IOException;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.AttachNotSupportedException;

import jdk.test.lib.apps.LingeredApp;

public class AttachNegativePidTest {

    public static void main(String... args) throws Exception {
        LingeredApp app = null;
        try {
            app = LingeredApp.startApp();
            String strPID = Long.toString(-1 * app.getPid());
            try {
                VirtualMachine.attach(strPID);
            } catch (AttachNotSupportedException anse) {
                // Passed
                return;
            }
            throw new RuntimeException("There is no expected AttachNotSupportedException for " + strPID);
        } finally {
            LingeredApp.stopApp(app);
        }
    }

}
