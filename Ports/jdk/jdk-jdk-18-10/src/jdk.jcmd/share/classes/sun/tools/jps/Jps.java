/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.tools.jps;

import java.util.*;
import java.net.*;
import sun.jvmstat.monitor.*;

/**
 * Application to provide a listing of monitorable java processes.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class Jps {

    private static Arguments arguments;

    public static void main(String[] args) {
        try {
            arguments = new Arguments(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            Arguments.printUsage(System.err);
            System.exit(1);
        }

        if (arguments.isHelp()) {
            Arguments.printUsage(System.err);
            System.exit(0);
        }

        try {
            HostIdentifier hostId = arguments.hostId();
            MonitoredHost monitoredHost =
                    MonitoredHost.getMonitoredHost(hostId);

            // get the set active JVMs on the specified host.
            Set<Integer> jvms = monitoredHost.activeVms();

            for (Integer jvm: jvms) {
                StringBuilder output = new StringBuilder();
                Throwable lastError = null;

                int lvmid = jvm;

                output.append(String.valueOf(lvmid));

                if (arguments.isQuiet()) {
                    System.out.println(output);
                    continue;
                }

                MonitoredVm vm = null;
                String vmidString = "//" + lvmid + "?mode=r";

                String errorString = null;

                try {
                    // Note: The VM associated with the current VM id may
                    // no longer be running so these queries may fail. We
                    // already added the VM id to the output stream above.
                    // If one of the queries fails, then we try to add a
                    // reasonable message to indicate that the requested
                    // info is not available.

                    errorString = " -- process information unavailable";
                    VmIdentifier id = new VmIdentifier(vmidString);
                    vm = monitoredHost.getMonitoredVm(id, 0);

                    errorString = " -- main class information unavailable";
                    output.append(' ').append(MonitoredVmUtil.mainClass(vm,
                            arguments.showLongPaths()));

                    if (arguments.showMainArgs()) {
                        errorString = " -- main args information unavailable";
                        String mainArgs = MonitoredVmUtil.mainArgs(vm);
                        if (mainArgs != null && mainArgs.length() > 0) {
                            output.append(' ').append(mainArgs);
                        }
                    }
                    if (arguments.showVmArgs()) {
                        errorString = " -- jvm args information unavailable";
                        String jvmArgs = MonitoredVmUtil.jvmArgs(vm);
                        if (jvmArgs != null && jvmArgs.length() > 0) {
                          output.append(' ')
                            .append(
                                // multi-line args are permitted
                                jvmArgs.replace("\n", "\\n").replace("\r", "\\r")
                            );
                        }
                    }
                    if (arguments.showVmFlags()) {
                        errorString = " -- jvm flags information unavailable";
                        String jvmFlags = MonitoredVmUtil.jvmFlags(vm);
                        if (jvmFlags != null && jvmFlags.length() > 0) {
                            output.append(' ').append(jvmFlags);
                        }
                    }

                    errorString = " -- detach failed";
                    monitoredHost.detach(vm);

                    System.out.println(output);

                    errorString = null;
                } catch (URISyntaxException e) {
                    // unexpected as vmidString is based on a validated hostid
                    lastError = e;
                    assert false;
                } catch (Exception e) {
                    lastError = e;
                } finally {
                    if (errorString != null) {
                        /*
                         * we ignore most exceptions, as there are race
                         * conditions where a JVM in 'jvms' may terminate
                         * before we get a chance to list its information.
                         * Other errors, such as access and I/O exceptions
                         * should stop us from iterating over the complete set.
                         */
                        output.append(errorString);
                        if (arguments.isDebug()) {
                            if ((lastError != null)
                                    && (lastError.getMessage() != null)) {
                                output.append("\n\t");
                                output.append(lastError.getMessage());
                            }
                        }
                        System.out.println(output);
                        if (arguments.printStackTrace()) {
                            lastError.printStackTrace();
                        }
                        continue;
                    }
                }
            }
        } catch (MonitorException e) {
            if (e.getMessage() != null) {
                System.err.println(e.getMessage());
            } else {
                Throwable cause = e.getCause();
                if ((cause != null) && (cause.getMessage() != null)) {
                    System.err.println(cause.getMessage());
                } else {
                    e.printStackTrace();
                }
            }
            System.exit(1);
        }
    }
}
