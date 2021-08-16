/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

/**
 * Benchmark html report generator.
 */
public class HtmlReporter implements Reporter {

    static final int PRECISION = 3;
    static final String[] PROPNAMES = { "os.name", "os.arch", "os.version",
        "java.home", "java.vm.version", "java.vm.vendor", "java.vm.name",
        "java.compiler", "java.class.path" };

    OutputStream out;
    String title;

    /**
     * Create HtmlReporter which writes to the given stream.
     */
    public HtmlReporter(OutputStream out, String title) {
        this.out = out;
        this.title = title;
    }

    /**
     * Generate html report.
     */
    public void writeReport(BenchInfo[] binfo, Properties props)
        throws IOException
    {
        PrintStream p = new PrintStream(out);
        float total = 0.0f;

        p.println("<html>");
        p.println("<head>");
        p.println("<title>" + title + "</title>");
        p.println("</head>");
        p.println("<body bgcolor=\"#ffffff\">");
        p.println("<h3>" + title + "</h3>");
        p.println("<hr>");

        p.println("<table border=0>");
        for (int i = 0; i < PROPNAMES.length; i++) {
            p.println("<tr><td>" + PROPNAMES[i] + ": <td>" +
                    props.getProperty(PROPNAMES[i]));
        }
        p.println("</table>");

        p.println("<p>");
        p.println("<table border=1>");
        p.println("<tr><th># <th>Benchmark Name <th>Time (ms) <th>Score");

        for (int i = 0; i < binfo.length; i++) {
            BenchInfo b = binfo[i];
            p.print("<tr><td>" + i + " <td>" + b.getName());
            if (b.getTime() != -1) {
                float score = b.getTime() * b.getWeight();
                total += score;
                p.println(" <td>" + b.getTime() + " <td>" +
                        Util.floatToString(score, PRECISION));
            }
            else {
                p.println(" <td>-- <td>--");
            }
        }

        p.println("<tr><td colspan=3><b>Total score</b> <td><b>" +
                Util.floatToString(total, PRECISION) + "</b>");
        p.println("</table>");
        p.println("<p>");
        p.println("<hr>");
        p.println("<i>Report generated on " + new Date() + "</i>");
        p.println("</body>");
        p.println("</html>");
    }
}
