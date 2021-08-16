/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.aod;

import java.io.File;

import nsk.share.*;

public class Utils {

    // prevent class instantiation
    private Utils() {
    }

    public static final long JPS_WORK_TIMEOUT = 60000; // 1 min

    /**
     * Find id of VM with certain value of property key.  Method findVMIdUsingJPS
     * runs 'jps -v' and seeks in jps output line containing this unique string,
     * discovered string should also contain VM id.
     *
     * @param jdkPath - path to jdk
     * @param key     - name of property
     * @param value   - value of property
     * @return VM id
     */
    public static String findVMIdUsingJPS(String jdkPath, String key, String value) {
        try {
            if (value == null) {
                return null;
            }
            String idString = key + "=" + value;
            String jpsPath = jdkPath + File.separator + "bin" + File.separator + "jps";

            while (true) {
                ProcessExecutor executor = new ProcessExecutor(jpsPath + " -v", JPS_WORK_TIMEOUT, "jps -v");
                executor.startProcess();
                executor.waitForProcess();

                if (executor.getExitCode() != 0) {
                    throw new Failure("jps finished with non-zero code " + executor.getExitCode());
                }

                for (String jpsOutLine : executor.getProcessOut()) {
                    if (jpsOutLine.contains(idString)) {
                        if (jpsOutLine.indexOf(' ') < 0)
                            throw new Failure("Unexpected format of the jps output '" + jpsOutLine + " (line doesn't contain space)");

                        return jpsOutLine.substring(0, jpsOutLine.indexOf(' '));
                    }
                }
                Thread.sleep(100);
            }
        } catch (Failure f) {
            throw f;
        } catch (Throwable t) {
            throw new Failure("Unexpected exception during jps execution: " + t, t);
        }
    }

    public static String findCurrentVMIdUsingJPS(String jdkPath) {
        /*
         * VM should be run with special property which allows to find VM id using jps
         * (see comments for method Utils.findVMIdUsingJPS)
         */
        String applicationId = System.getProperty(AODTestRunner.targetAppIdProperty);
        if (applicationId == null)
            throw new TestBug("Property '" + AODTestRunner.targetAppIdProperty + "' isn't defined");

        String targetVMId = Utils.findVMIdUsingJPS(jdkPath, AODTestRunner.targetAppIdProperty, applicationId);

        return targetVMId;
    }

}
