/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package bench;

import java.awt.Toolkit;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

/**
 * Benchmark XML report generator.  Uses XML format used by other JDK
 * benchmarks.
 */
public class XmlReporter implements Reporter {

    OutputStream out;
    String title;

    /**
     * Create XmlReporter which writes to the given stream.
     */
    public XmlReporter(OutputStream out, String title) {
        this.out = out;
        this.title = title;
    }

    /**
     * Generate text report.
     */
    public void writeReport(BenchInfo[] binfo, Properties props)
        throws IOException
    {
        PrintStream p = new PrintStream(out);

        p.println("<REPORT>");
        p.println("<NAME>" + title + "</NAME>");
        p.println("<DATE>" + new Date() + "</DATE>");
        p.println("<VERSION>" + props.getProperty("java.version") +
                "</VERSION>");
        p.println("<VENDOR>" + props.getProperty("java.vendor") + "</VENDOR>");
        p.println("<DIRECTORY>" + props.getProperty("java.home") +
                "</DIRECTORY>");
        String vmName = props.getProperty("java.vm.name");
        String vmInfo = props.getProperty("java.vm.info");
        String vmString = (vmName != null && vmInfo != null) ?
            vmName + " " + vmInfo : "Undefined";
        p.println("<VM_INFO>" + vmString + "</VM_INFO>");
        p.println("<OS>" + props.getProperty("os.name") +
                " version " + props.getProperty("os.version") + "</OS>");
        p.println("<BIT_DEPTH>" +
                Toolkit.getDefaultToolkit().getColorModel().getPixelSize() +
                "</BIT_DEPTH>");
        p.println();

        p.println("<DATA RUNS=\"" + 1 + "\" TESTS=\"" + binfo.length + "\">");
        for (int i = 0; i < binfo.length; i++) {
            BenchInfo b = binfo[i];
            String score = (b.getTime() != -1) ?
                Double.toString(b.getTime() * b.getWeight()) : "-1";
            p.println(b.getName() + "\t" + score);
        }

        p.println("</DATA>");
        p.println("</REPORT>");
    }
}
