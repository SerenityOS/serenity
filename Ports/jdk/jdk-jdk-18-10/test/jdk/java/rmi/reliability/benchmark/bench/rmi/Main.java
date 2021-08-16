/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @summary The RMI benchmark test. This java class is used to run the test
 *          under JTREG.
 * @library ../../../../testlibrary ../../
 * @modules java.desktop
 *          java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary bench.BenchInfo bench.HtmlReporter bench.Util
 * bench.Benchmark bench.Reporter bench.XmlReporter bench.ConfigFormatException
 * bench.Harness bench.TextReporter bench.rmi.BenchServer
 * bench.rmi.DoubleArrayCalls bench.rmi.LongCalls bench.rmi.ShortCalls
 * bench.rmi.BenchServerImpl bench.rmi.DoubleCalls bench.rmi.Main
 * bench.rmi.SmallObjTreeCalls bench.rmi.BooleanArrayCalls
 * bench.rmi.ExceptionCalls bench.rmi.NullCalls bench.rmi.BooleanCalls
 * bench.rmi.ExportObjs bench.rmi.ObjArrayCalls bench.rmi.ByteArrayCalls
 * bench.rmi.FloatArrayCalls bench.rmi.ObjTreeCalls bench.rmi.ByteCalls
 * bench.rmi.FloatCalls bench.rmi.ProxyArrayCalls bench.rmi.CharArrayCalls
 * bench.rmi.IntArrayCalls bench.rmi.RemoteObjArrayCalls bench.rmi.CharCalls
 * bench.rmi.IntCalls bench.rmi.ClassLoading bench.rmi.LongArrayCalls
 * bench.rmi.ShortArrayCalls
 * bench.rmi.altroot.Node
 * @run main/othervm/policy=policy.all/timeout=1800 bench.rmi.Main -server -c config
 * @author Mike Warres, Nigel Daley
 */

package bench.rmi;

import bench.ConfigFormatException;
import bench.Harness;
import bench.HtmlReporter;
import bench.Reporter;
import bench.TextReporter;
import bench.XmlReporter;
import static bench.rmi.Main.OutputFormat.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * RMI/Serialization benchmark tests.
 */
public class Main {

    /**
     * RMI-specific benchmark harness.
     */
    static class RMIHarness extends Harness {
        /**
         * Construct new RMI benchmark harness.
         */
        RMIHarness(InputStream in) throws IOException, ConfigFormatException {
            super(in);
        }

        /**
         * Cleanup both client and server side in between each benchmark.
         */
        @Override
        protected void cleanup() {
            System.gc();
            if (Main.runmode == CLIENT) {
                try {
                    Main.server.gc();
                } catch (RemoteException e) {
                    System.err.println("Warning: server gc failed: " + e);
                }
            }
        }
    }

    static final String CONFFILE = "config";
    static final String VERSION = "1.3";
    static final String REGNAME = "server";

    static final int SAMEVM = 0;
    static final int CLIENT = 1;
    static final int SERVER = 2;

    static enum OutputFormat {

        TEXT {
            @Override
            Reporter getReport(String title) {
                return new TextReporter(repstr, title);
            }
        },
        HTML {

            @Override
            Reporter getReport(String title) {
                return new HtmlReporter(repstr, title);
            }
        },
        XML {
            @Override
            Reporter getReport(String title) {
                return new XmlReporter(repstr, title);
            }
        };

        abstract Reporter getReport(String title);
    };

    static final String TEST_SRC_PATH = System.getProperty("test.src") + File.separator;

    static boolean verbose;
    static boolean list;
    static boolean exitOnTimer;
    static int testDurationSeconds;
    static volatile boolean exitRequested;
    static Timer timer;
    static OutputFormat format = TEXT;
    static int runmode;
    static String confFile;
    static InputStream confstr;
    static String repFile;
    static OutputStream repstr;
    static String host;
    static int port;
    static RMIHarness harness;
    static Reporter reporter;
    static BenchServer server;
    static BenchServerImpl serverImpl;

    /**
     * Returns reference to benchmark server.
     *
     * @return a benchmark server
     */
    public static BenchServer getBenchServer() {
        return server;
    }

    /**
     * Prints help message.
     */
    static void usage() {
        PrintStream p = System.err;
        p.println("\nUsage: java -jar rmibench.jar [-options]");
        p.println("\nwhere options are:");
        p.println("  -h                   print this message");
        p.println("  -v                   verbose mode");
        p.println("  -l                   list configuration file");
        p.println("  -t <num hours>       repeat benchmarks for specified number of hours");
        p.println("  -o <file>            specify output file");
        p.println("  -c <file>            specify (non-default) "
                + "configuration file");
        p.println("  -html                format output as html "
                + "(default is text)");
        p.println("  -xml                 format output as xml");
        p.println("  -server              run benchmark server ");
        p.println("  -client <host:port>  run benchmark client using server "
                + "on specified host/port");
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
     * Benchmark mainline.
     *
     * @param args
     */
    public static void main(String[] args) {
        setupSecurity();
        parseArgs(args);
        setupStreams();
        if (list) {
            listConfig();
        } else {
            setupServer();
            switch (runmode) {
                case SAMEVM:
                case CLIENT:
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
                    break;
                case SERVER:
                    //Setup for client mode, server will fork client process
                    //after its initiation.
                    List<String> clientProcessStr = new ArrayList<>();
                    clientProcessStr.add(System.getProperty("test.jdk") +
                            File.separator + "bin" + File.separator + "java");
                    String classpath = System.getProperty("java.class.path");
                    if (classpath != null) {
                        clientProcessStr.add("-cp");
                        clientProcessStr.add(classpath);
                    }
                    clientProcessStr.add("-Djava.security.policy=" + TEST_SRC_PATH + "policy.all");
                    clientProcessStr.add("-Djava.security.manager=allow");
                    clientProcessStr.add("-Dtest.src=" + TEST_SRC_PATH);
                    clientProcessStr.add("bench.rmi.Main"); //Client mode
                    if (verbose) {
                        clientProcessStr.add("-v");
                    }
                    if (list) {
                        clientProcessStr.add("-l");
                    }
                    clientProcessStr.add("-client");
                    clientProcessStr.add("localhost:" + port);

                    if (exitOnTimer) {
                        clientProcessStr.add("-t");
                        clientProcessStr.add(String.valueOf(testDurationSeconds / 3600));
                    }
                    if (repFile != null) {
                        clientProcessStr.add("-o");
                        clientProcessStr.add(repFile);
                    }
                    if (confFile != null) {
                        clientProcessStr.add("-c");
                        clientProcessStr.add(confFile);
                    }
                    switch (format) {
                        case HTML:
                            clientProcessStr.add("-html");
                            break;
                        case XML:
                            clientProcessStr.add("-xml");
                            break;
                    }

                    try {
                        Process client = new ProcessBuilder(clientProcessStr).
                                inheritIO().start();
                        try {
                            client.waitFor();
                            int exitValue = client.exitValue();
                            if (0 != exitValue) {
                                die("Error: error happened in client process, exitValue = " + exitValue);
                            }
                        } finally {
                            client.destroyForcibly();
                        }
                    } catch (IOException ex) {
                        die("Error: Unable start client process, ex=" + ex.getMessage());
                    } catch (InterruptedException ex) {
                        die("Error: Error happening to client process, ex=" + ex.getMessage());
                    }
                    break;
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
                    if (++i >= args.length) {
                        die("Error: no timeout value specified");
                    }
                    try {
                        exitOnTimer = true;
                        testDurationSeconds = Integer.parseInt(args[i]) * 3600;
                    } catch (NumberFormatException e) {
                        die("Error: unable to determine timeout value");
                    }
                    break;
                case "-o":
                    if (++i >= args.length) {
                        die("Error: no output file specified");
                    }
                    try {
                        repFile = args[i];
                        repstr = new FileOutputStream(repFile);
                    } catch (FileNotFoundException e) {
                        die("Error: unable to open \"" + args[i] + "\"");
                    }
                    break;
                case "-c":
                    if (++i >= args.length) {
                        die("Error: no config file specified");
                    }
                    confFile = args[i];
                    String confFullPath = TEST_SRC_PATH + confFile;
                    try {
                        confstr = new FileInputStream(confFullPath);
                    } catch (FileNotFoundException e) {
                        die("Error: unable to open \"" + confFullPath + "\"");
                    }
                    break;
                case "-html":
                    if (format != TEXT) {
                        die("Error: conflicting formats");
                    }
                    format = HTML;
                    break;
                case "-xml":
                    if (format != TEXT) {
                        die("Error: conflicting formats");
                    }
                    format = XML;
                    break;
                case "-client":
                    if (runmode == CLIENT) {
                        die("Error: multiple -client options");
                    }
                    if (runmode == SERVER) {
                        die("Error: -client and -server options conflict");
                    }
                    if (++i >= args.length) {
                        die("Error: -client missing host/port");
                    }
                    try {
                        String[] hostAndPort = args[i].split(":");
                        if (hostAndPort.length != 2) {
                            die("Error: Invalid format host/port:" + args[i]);
                        }
                        host = hostAndPort[0];
                        port = Integer.parseInt(hostAndPort[1]);
                    } catch (NumberFormatException e) {
                        die("Error: illegal host/port specified for -client");
                    }
                    runmode = CLIENT;
                    break;
                case "-server":
                    if (runmode == CLIENT) {
                        die("Error: -client and -server options conflict");
                    }
                    if (runmode == SERVER) {
                        die("Error: multiple -server options");
                    }
                    try {
                        //This is the hack code because named package class has
                        //difficulty in accessing unnamed package class. This
                        //should be removed ater JDK-8003358 is finished.
                        port = (int) Class.forName("TestLibrary")
                                .getMethod("getUnusedRandomPort")
                                .invoke(null);
                    } catch (ReflectiveOperationException ex) {
                        die("Error: can't get a free port " + ex);
                    }
                    runmode = SERVER;
                    break;
                default:
                    usage();
                    die("Illegal option: \"" + args[i] + "\"");
            }
        }
    }

    /**
     * Set up security manager and policy, if not set already.
     */
    static void setupSecurity() {
        if (System.getSecurityManager() != null) {
            return;
        }

        /* As of 1.4, it is too late to set the security policy
         * file at this point so these line have been commented out.
         */
        //System.setProperty("java.security.policy",
        //      Main.class.getResource("/bench/rmi/policy.all").toString());
        System.setSecurityManager(new SecurityManager());
    }

    /**
     * Set up configuration file and report streams, if not set already.
     */
    static void setupStreams() {
        if (repstr == null) {
            repstr = System.out;
        }
        if (confstr == null) {
            confstr = Main.class.getResourceAsStream(TEST_SRC_PATH + CONFFILE);
        }
        if (confstr == null) {
            die("Error: unable to find default config file");
        }
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
     * Setup benchmark server.
     */
    static void setupServer() {
        switch (runmode) {
            case SAMEVM:
                try {
                    serverImpl = new BenchServerImpl();
                    server = (BenchServer) RemoteObject.toStub(serverImpl);
                } catch (RemoteException e) {
                    die("Error: failed to create local server: " + e);
                }
                if (verbose)
                    System.out.println("Benchmark server created locally");
                break;

            case CLIENT:
                try {
                    Registry reg = LocateRegistry.getRegistry(host, port);
                    server = (BenchServer) reg.lookup(REGNAME);
                } catch (NotBoundException | RemoteException e) {
                    die("Error: failed to connect to server: " + e);
                }
                if (server == null) {
                    die("Error: server not found");
                }
                if (verbose) {
                    System.out.println("Connected to benchmark server on " +
                            host + ":" + port);
                }
                break;

            case SERVER:
                try {
                    Registry reg = LocateRegistry.createRegistry(port);
                    serverImpl = new BenchServerImpl();
                    reg.bind(REGNAME, serverImpl);
                } catch (AlreadyBoundException | RemoteException e) {
                    die("Error: failed to initialize server: " + e);
                }
                if (verbose) {
                    System.out.println("Benchmark server started on port " +
                            port);
                }
                break;

            default:
                throw new InternalError("illegal runmode");
        }
    }

    /**
     * Set up the timer to end the test.
     *
     * @param delay the amount of delay, in seconds, before requesting the
     * process exit
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
            harness = new RMIHarness(confstr);
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
        reporter = format.getReport("RMI Benchmark, v" + VERSION);
    }

    /**
     * Run benchmarks.
     */
    static void runBenchmarks() {
        harness.runBenchmarks(reporter, verbose);
    }
}
