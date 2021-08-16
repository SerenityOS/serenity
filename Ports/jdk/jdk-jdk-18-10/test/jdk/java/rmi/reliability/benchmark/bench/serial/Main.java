/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @summary The Serialization benchmark test. This java class is used to run the
 *          test under JTREG.
 * @library ../../
 * @modules java.desktop
 * @build bench.BenchInfo bench.HtmlReporter bench.Util bench.Benchmark
 * @build bench.Reporter bench.XmlReporter bench.ConfigFormatException
 * @build bench.Harness bench.TextReporter
 * @build bench.serial.BooleanArrays bench.serial.Booleans
 * @build bench.serial.ByteArrays bench.serial.Bytes bench.serial.CharArrays
 * @build bench.serial.Chars bench.serial.ClassDesc bench.serial.Cons
 * @build bench.serial.CustomDefaultObjTrees bench.serial.CustomObjTrees
 * @build bench.serial.DoubleArrays bench.serial.Doubles
 * @build bench.serial.ExternObjTrees bench.serial.FloatArrays
 * @build bench.serial.Floats bench.serial.GetPutFieldTrees
 * @build bench.serial.IntArrays bench.serial.Ints bench.serial.LongArrays
 * @build bench.serial.Longs bench.serial.Main bench.serial.ObjArrays
 * @build bench.serial.ObjTrees bench.serial.ProxyArrays
 * @build bench.serial.ProxyClassDesc bench.serial.RepeatObjs
 * @build bench.serial.ReplaceTrees bench.serial.ShortArrays
 * @build bench.serial.Shorts bench.serial.SmallObjTrees
 * @build bench.serial.StreamBuffer bench.serial.Strings
 * @run main/othervm/timeout=1800 -Xss2m bench.serial.Main -c jtreg-config
 * @author Mike Warres, Nigel Daley
 */

// The -Xss2m supplies additional stack space, as bench.serial.ClassDesc
// consumes a considerable amount of stack.

package bench.serial;

import bench.ConfigFormatException;
import bench.Harness;
import bench.HtmlReporter;
import bench.Reporter;
import bench.TextReporter;
import bench.XmlReporter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Object serialization benchmark mainline.
 */
public class Main {

    static final String CONFFILE = "config";
    static final String VERSION = "1.3";
    static final String TEST_SRC_PATH = System.getProperty("test.src") + File.separator;

    static final int TEXT = 0;
    static final int HTML = 1;
    static final int XML = 2;

    static boolean verbose;
    static boolean list;
    static boolean exitOnTimer;
    static int testDurationSeconds;
    static volatile boolean exitRequested;
    static Timer timer;
    static int format = TEXT;
    static InputStream confstr;
    static OutputStream repstr;
    static Harness harness;
    static Reporter reporter;

    /**
     * Print help message.
     */
    static void usage() {
        PrintStream p = System.err;
        p.println("\nUsage: java -jar serialbench.jar [-options]");
        p.println("\nwhere options are:");
        p.println("  -h              print this message");
        p.println("  -v              verbose mode");
        p.println("  -l              list configuration file");
        p.println("  -t <num hours>  repeat benchmarks for specified number of hours");
        p.println("  -o <file>       specify output file");
        p.println("  -c <file>       specify (non-default) configuration file");
        p.println("  -html           format output as html (default is text)");
        p.println("  -xml            format output as xml");
    }

    /**
     * Throw RuntimeException that wrap message.
     *
     * @param mesg a message will be wrapped in the RuntimeException.
     */
    static void die(String mesg) {
        throw new RuntimeException(mesg);
    }

    /**
     * Mainline parses command line, then hands off to benchmark harness.
     *
     * @param args
     */
    public static void main(String[] args) {
        parseArgs(args);
        setupStreams();
        if (list) {
            listConfig();
        } else {
            setupHarness();
            setupReporter();
            if (exitOnTimer) {
                setupTimer(testDurationSeconds);
                do {
                    runBenchmarks();
                } while (!exitRequested);
            } else {
                runBenchmarks();
            }
        }
    }

    /**
     * Parse command-line arguments.
     */
    static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                    usage();
                    System.exit(0);
                    break;
                case "-v":
                    verbose = true;
                    break;
                case "-l":
                    list = true;
                    break;
                case "-t":
                    if (++i >= args.length)
                        die("Error: no timeout value specified");
                    try {
                        exitOnTimer = true;
                        testDurationSeconds = Integer.parseInt(args[i]) * 3600;
                    } catch (NumberFormatException e) {
                        die("Error: unable to determine timeout value");
                    }
                    break;
                case "-o":
                    if (++i >= args.length)
                        die("Error: no output file specified");
                    try {
                        repstr = new FileOutputStream(args[i]);
                    } catch (FileNotFoundException e) {
                        die("Error: unable to open \"" + args[i] + "\"");
                    }
                    break;
                case "-c":
                    if (++i >= args.length)
                        die("Error: no config file specified");
                    String confFileName = TEST_SRC_PATH + args[i];
                    try {
                        confstr = new FileInputStream(confFileName);
                    } catch (FileNotFoundException e) {
                        die("Error: unable to open \"" + confFileName + "\"");
                    }
                    break;
                case "-html":
                    if (format != TEXT)
                        die("Error: conflicting formats");
                    format = HTML;
                    break;
                case "-xml":
                    if (format != TEXT)
                        die("Error: conflicting formats");
                    format = XML;
                    break;
                default:
                    usage();
                    die("Illegal option: \"" + args[i] + "\"");
            }
        }
    }

    /**
     * Set up configuration file and report streams, if not set already.
     */
    static void setupStreams() {
        if (repstr == null)
            repstr = System.out;
        if (confstr == null)
            confstr = Main.class.getResourceAsStream(TEST_SRC_PATH + CONFFILE);
        if (confstr == null)
            die("Error: unable to find default config file");
    }

    /**
     * Print contents of configuration file to selected output stream.
     */
    static void listConfig() {
        try {
            byte[] buf = new byte[256];
            int len;
            while ((len = confstr.read(buf)) != -1)
                repstr.write(buf, 0, len);
        } catch (IOException e) {
            die("Error: failed to list config file");
        }
    }

    /**
     * Set up the timer to end the test.
     *
     * @param delay the amount of delay, in seconds, before requesting
     * the process exit
     */
    static void setupTimer(int delay) {
        timer = new Timer(true);
        timer.schedule(
            new TimerTask() {
                @Override
                public void run() {
                    exitRequested = true;
                }
            },
            delay * 1000);
    }

    /**
     * Set up benchmark harness.
     */
    static void setupHarness() {
        try {
            harness = new Harness(confstr);
        } catch (ConfigFormatException e) {
            String errmsg = e.getMessage();
            if (errmsg != null) {
                die("Error parsing config file: " + errmsg);
            } else {
                die("Error: illegal config file syntax");
            }
        } catch (IOException e) {
            die("Error: failed to read config file");
        }
    }

    /**
     * Setup benchmark reporter.
     */
    static void setupReporter() {
        String title = "Object Serialization Benchmark, v" + VERSION;
        switch (format) {
            case TEXT:
                reporter = new TextReporter(repstr, title);
                break;

            case HTML:
                reporter = new HtmlReporter(repstr, title);
                break;

            case XML:
                reporter = new XmlReporter(repstr, title);
                break;

            default:
                die("Error: unrecognized format type");
        }
    }

    /**
     * Run benchmarks.
     */
    static void runBenchmarks() {
        harness.runBenchmarks(reporter, verbose);
    }
}
