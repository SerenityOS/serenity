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
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

/**
 * Benchmark text report generator.
 */
public class TextReporter implements Reporter {

    static final int PRECISION = 3;
    static final int INDEX_WIDTH = 3;
    static final int NAME_WIDTH = 30;
    static final int TIME_WIDTH = 10;
    static final int SCORE_WIDTH = 10;
    static final int PROPNAME_WIDTH = 25;
    static final String[] PROPNAMES = { "os.name", "os.arch", "os.version",
        "java.home", "java.vm.version", "java.vm.vendor", "java.vm.name",
        "java.compiler", "java.class.path" };

    OutputStream out;
    String title;

    /**
     * Create TextReporter which writes to the given stream.
     */
    public TextReporter(OutputStream out, String title) {
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
        float total = 0.0f;

        p.println("\n" + title);
        p.println(pad('-', title.length()));
        p.println("");
        for (int i = 0; i < PROPNAMES.length; i++) {
            p.println(fit(PROPNAMES[i] + ":", PROPNAME_WIDTH) +
                    props.getProperty(PROPNAMES[i]));
        }
        p.println("");

        p.println(fit("#", INDEX_WIDTH) + "  " +
                fit("Benchmark Name", NAME_WIDTH) + "  " +
                fit("Time (ms)", TIME_WIDTH) + "  " +
                fit("Score", SCORE_WIDTH));
        p.println(pad('-', INDEX_WIDTH + NAME_WIDTH + TIME_WIDTH +
                    SCORE_WIDTH + 6));

        for (int i = 0; i < binfo.length; i++) {
            BenchInfo b = binfo[i];
            p.print(fit(Integer.toString(i), INDEX_WIDTH) + "  ");
            p.print(fit(b.getName(), NAME_WIDTH) + "  ");
            if (b.getTime() != -1) {
                float score = b.getTime() * b.getWeight();
                total += score;
                p.print(fit(Long.toString(b.getTime()), TIME_WIDTH) + "  ");
                p.println(fit(Util.floatToString(score, PRECISION),
                            SCORE_WIDTH));
            }
            else {
                p.print(fit("--", TIME_WIDTH) + "  ");
                p.println(fit("--", SCORE_WIDTH));
            }
        }
        p.println(pad('-', INDEX_WIDTH + NAME_WIDTH + TIME_WIDTH +
                    SCORE_WIDTH + 6));
        p.println(fit("Total score", INDEX_WIDTH + NAME_WIDTH + TIME_WIDTH +
                    4) + "  " + Util.floatToString(total, PRECISION));
        p.println("");
        p.println("-----");
        p.println("Report generated on " + new Date() + "\n");
        p.println("");
    }

    /**
     * Extend or truncate string so it fits in the given space.
     */
    private static String fit(String s, int len) {
        int slen = s.length();
        StringBuffer buf = new StringBuffer(s);
        buf.setLength(len);
        for (int i = slen; i < len; i++)
            buf.setCharAt(i, ' ');
        return buf.toString();
    }

    /**
     * Return string with given number of chars.
     */
    private static String pad(char c, int len) {
        char[] buf = new char[len];
        Arrays.fill(buf, c);
        return new String(buf);
    }
}
