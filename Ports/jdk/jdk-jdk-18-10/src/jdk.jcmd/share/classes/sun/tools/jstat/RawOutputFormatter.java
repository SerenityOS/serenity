/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
import sun.jvmstat.monitor.*;

/**
 * A class for formatting raw counter output.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class RawOutputFormatter implements OutputFormatter {
    private List<Monitor> logged;
    private String header;
    private boolean printStrings;

    public RawOutputFormatter(List<Monitor> logged, boolean printStrings) {
        this.logged = logged;
        this.printStrings = printStrings;
    }

    public String getHeader() throws MonitorException {
        if (header == null) {
            // build the header string and prune out any unwanted monitors
            StringBuilder headerBuilder = new StringBuilder();
            for (Iterator<Monitor> i = logged.iterator(); i.hasNext(); /* empty */ ) {
                Monitor m = i.next();
                headerBuilder.append(m.getName()).append(' ');
            }
            header = headerBuilder.toString();
        }
        return header;
    }

    public String getRow() throws MonitorException {
        StringBuilder row = new StringBuilder();
        int count = 0;
        for (Iterator<Monitor> i = logged.iterator(); i.hasNext(); /* empty */ ) {
            Monitor m = i.next();
            if (count++ > 0) {
                row.append(" ");
            }
            if (printStrings && m instanceof StringMonitor) {
                row.append("\"").append(m.getValue()).append("\"");
            } else {
                row.append(m.getValue());
            }
        }
        return row.toString();
    }
}
