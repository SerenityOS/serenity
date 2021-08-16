/*
 * Copyright (c) 2004, 2010, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jstat;

import java.util.*;
import java.io.*;
import sun.jvmstat.monitor.*;
import sun.jvmstat.monitor.event.*;
import java.util.regex.PatternSyntaxException;

/**
 * Class to sample and output various jvmstat statistics for a target Java
 * a target Java Virtual Machine.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class JStatLogger {

    private MonitoredVm monitoredVm;
    private volatile boolean active = true;

    public JStatLogger(MonitoredVm monitoredVm) {
        this.monitoredVm = monitoredVm;
    }

    /**
     * print the monitors that match the given monitor name pattern string.
     */
    public void printNames(String names, Comparator<Monitor> comparator,
                           boolean showUnsupported, PrintStream out)
                throws MonitorException, PatternSyntaxException {

        // get the set of all monitors
        List<Monitor> items = monitoredVm.findByPattern(names);
        Collections.sort(items, comparator);

        for (Monitor m: items) {
            if (!(m.isSupported() || showUnsupported)) {
                continue;
            }
            out.println(m.getName());
        }
    }

    /**
     * print name=value pairs for the given list of monitors.
     */
    public void printSnapShot(String names, Comparator<Monitor> comparator,
                              boolean verbose, boolean showUnsupported,
                              PrintStream out)
                throws MonitorException, PatternSyntaxException {

        // get the set of all monitors
        List<Monitor> items = monitoredVm.findByPattern(names);
        Collections.sort(items, comparator);

        printList(items, verbose, showUnsupported, out);
    }

    /**
     * print name=value pairs for the given list of monitors.
     */
    public void printList(List<Monitor> list, boolean verbose, boolean showUnsupported,
                          PrintStream out)
                throws MonitorException {

        // print out the name of each available counter
        for (Monitor m: list ) {

            if (!(m.isSupported() || showUnsupported)) {
                continue;
            }

            StringBuilder buffer = new StringBuilder();
            buffer.append(m.getName()).append("=");

            if (m instanceof StringMonitor) {
                buffer.append("\"").append(m.getValue()).append("\"");
            } else {
                buffer.append(m.getValue());
            }

            if (verbose) {
                buffer.append(" ").append(m.getUnits());
                buffer.append(" ").append(m.getVariability());
                buffer.append(" ").append(m.isSupported() ? "Supported"
                                                          : "Unsupported");
            }
            out.println(buffer);
        }
    }

    /**
     * method to for asynchronous termination of sampling loops
     */
    public void stopLogging() {
        active = false;
    }

    /**
     * print samples according to the given format.
     */
    public void logSamples(OutputFormatter formatter, int headerRate,
                           int sampleInterval, int sampleCount, PrintStream out)
                throws MonitorException {

        long iterationCount = 0;
        int printHeaderCount = 0;

        // if printHeader == 0, then only an initial column header is desired.
        int printHeader = headerRate;
        if (printHeader == 0) {
            // print the column header once, disable future printing
            out.println(formatter.getHeader());
            printHeader = -1;
        }

        while (active) {
          // check if it's time to print another column header
          if (printHeader > 0 && --printHeaderCount <= 0) {
              printHeaderCount = printHeader;
              out.println(formatter.getHeader());
          }

          out.println(formatter.getRow());

          // check if we've hit the specified sample count
          if (sampleCount > 0 && ++iterationCount >= sampleCount) {
              break;
          }

          try { Thread.sleep(sampleInterval); } catch (Exception e) { };
        }
    }
}
