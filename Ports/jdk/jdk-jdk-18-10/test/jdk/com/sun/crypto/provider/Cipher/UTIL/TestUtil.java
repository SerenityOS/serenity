/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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

import java.util.jar.*;
import java.io.*;

public class TestUtil {

    private boolean isUnlimited;
    private static TestUtil instance = null;

    static {
        instance = new TestUtil();
    }

    private TestUtil() {
        try {
            isUnlimited = isUnlimitedPolicy();
        } catch (Exception ex) {
            RuntimeException re = new RuntimeException
                ("Cannot locate the jurisdiction policy files");
            re.initCause(ex);
            throw re;
        }
    }

    private static boolean isUnlimitedPolicy() throws IOException {
        if (instance == null) {
            String jreDir = System.getProperty("java.home");
            String localPolicyPath = jreDir + File.separator + "lib" +
                File.separator + "security" + File.separator +
                "local_policy.jar";
            JarFile localPolicy = new JarFile(localPolicyPath);
            if (localPolicy.getEntry("exempt_local.policy") == null) {
                return true;
            } else {
                return false;
            }
        } else {
            return instance.isUnlimited;
        }
    }

    public static void handleSE(SecurityException ex)
        throws SecurityException {
        if (instance == null) {
            instance = new TestUtil();
        }
        if ((instance.isUnlimited) ||
            !(ex.getMessage().startsWith("Unsupported keysize"))) {
            throw ex;
        }
    }
}
