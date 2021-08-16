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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.IOException;
import java.util.Vector;


/**
 * Benchmark harness.  Responsible for parsing config file and running
 * benchmarks.
 */
public class Harness {

    BenchInfo[] binfo;

    /**
     * Create new benchmark harness with given configuration and reporter.
     * Throws ConfigFormatException if there was an error parsing the config
     * file.
     * <p>
     * <b>Config file syntax:</b>
     * <p>
     * '#' marks the beginning of a comment.  Blank lines are ignored.  All
     * other lines should adhere to the following format:
     * <pre>
     *     &lt;weight&gt; &lt;name&gt; &lt;class&gt; [&lt;args&gt;]
     * </pre>
     * &lt;weight&gt; is a floating point value which is multiplied times the
     * benchmark's execution time to determine its weighted score.  The
     * total score of the benchmark suite is the sum of all weighted scores
     * of its benchmarks.
     * <p>
     * &lt;name&gt; is a name used to identify the benchmark on the benchmark
     * report.  If the name contains whitespace, the quote character '"' should
     * be used as a delimiter.
     * <p>
     * &lt;class&gt; is the full name (including the package) of the class
     * containing the benchmark implementation.  This class must implement
     * bench.Benchmark.
     * <p>
     * [&lt;args&gt;] is a variable-length list of runtime arguments to pass to
     * the benchmark.  Arguments containing whitespace should use the quote
     * character '"' as a delimiter.
     * <p>
     * <b>Example:</b>
     * <pre>
     *      3.5 "My benchmark" bench.serial.Test first second "third arg"
     * </pre>
     */
    public Harness(InputStream in) throws IOException, ConfigFormatException {
        Vector bvec = new Vector();
        StreamTokenizer tokens = new StreamTokenizer(new InputStreamReader(in));

        tokens.resetSyntax();
        tokens.wordChars(0, 255);
        tokens.whitespaceChars(0, ' ');
        tokens.commentChar('#');
        tokens.quoteChar('"');
        tokens.eolIsSignificant(true);

        tokens.nextToken();
        while (tokens.ttype != StreamTokenizer.TT_EOF) {
            switch (tokens.ttype) {
                case StreamTokenizer.TT_WORD:
                case '"':                       // parse line
                    bvec.add(parseBenchInfo(tokens));
                    break;

                default:                        // ignore
                    tokens.nextToken();
                    break;
            }
        }
        binfo = (BenchInfo[]) bvec.toArray(new BenchInfo[bvec.size()]);
    }

    BenchInfo parseBenchInfo(StreamTokenizer tokens)
        throws IOException, ConfigFormatException
    {
        float weight = parseBenchWeight(tokens);
        String name = parseBenchName(tokens);
        Benchmark bench = parseBenchClass(tokens);
        String[] args = parseBenchArgs(tokens);
        if (tokens.ttype == StreamTokenizer.TT_EOL)
            tokens.nextToken();
        return new BenchInfo(bench, name, weight, args);
    }

    float parseBenchWeight(StreamTokenizer tokens)
        throws IOException, ConfigFormatException
    {
        float weight;
        switch (tokens.ttype) {
            case StreamTokenizer.TT_WORD:
            case '"':
                try {
                    weight = Float.parseFloat(tokens.sval);
                } catch (NumberFormatException e) {
                    throw new ConfigFormatException("illegal weight value \"" +
                            tokens.sval + "\" on line " + tokens.lineno());
                }
                tokens.nextToken();
                return weight;

            default:
                throw new ConfigFormatException("missing weight value on line "
                        + tokens.lineno());
        }
    }

    String parseBenchName(StreamTokenizer tokens)
        throws IOException, ConfigFormatException
    {
        String name;
        switch (tokens.ttype) {
            case StreamTokenizer.TT_WORD:
            case '"':
                name = tokens.sval;
                tokens.nextToken();
                return name;

            default:
                throw new ConfigFormatException("missing benchmark name on " +
                        "line " + tokens.lineno());
        }
    }

    Benchmark parseBenchClass(StreamTokenizer tokens)
        throws IOException, ConfigFormatException
    {
        Benchmark bench;
        switch (tokens.ttype) {
            case StreamTokenizer.TT_WORD:
            case '"':
                try {
                    Class cls = Class.forName(tokens.sval);
                    bench = (Benchmark) cls.newInstance();
                } catch (Exception e) {
                    throw new ConfigFormatException("unable to instantiate " +
                            "benchmark \"" + tokens.sval + "\" on line " +
                            tokens.lineno());
                }
                tokens.nextToken();
                return bench;

            default:
                throw new ConfigFormatException("missing benchmark class " +
                        "name on line " + tokens.lineno());
        }
    }

    String[] parseBenchArgs(StreamTokenizer tokens)
        throws IOException, ConfigFormatException
    {
        Vector vec = new Vector();
        for (;;) {
            switch (tokens.ttype) {
                case StreamTokenizer.TT_EOF:
                case StreamTokenizer.TT_EOL:
                    return (String[]) vec.toArray(new String[vec.size()]);

                case StreamTokenizer.TT_WORD:
                case '"':
                    vec.add(tokens.sval);
                    tokens.nextToken();
                    break;

                default:
                    throw new ConfigFormatException("unrecognized arg token " +
                            "on line " + tokens.lineno());
            }
        }
    }

    /**
     * Run benchmarks, writing results to the given reporter.
     */
    public void runBenchmarks(Reporter reporter, boolean verbose) {
        for (int i = 0; i < binfo.length; i++) {
            if (verbose)
                System.out.println("Running benchmark " + i + " (" +
                        binfo[i].getName() + ")");
            try {
                binfo[i].runBenchmark();
            } catch (Exception e) {
                System.err.println("Error: benchmark " + i + " failed: " + e);
                e.printStackTrace();
            }
            cleanup();
        }
        try {
            reporter.writeReport(binfo, System.getProperties());
        } catch (IOException e) {
            System.err.println("Error: failed to write benchmark report");
        }
    }

    /**
     * Clean up method that is invoked after the completion of each benchmark.
     * The default implementation calls System.gc(); subclasses may override
     * this to perform additional cleanup measures.
     */
    protected void cleanup() {
        System.gc();
    }
}
