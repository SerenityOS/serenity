/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.VmIdentifier;

/**
 * Class for processing command line arguments and providing method
 * level access to arguments.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class Arguments {

    private static final boolean debug = Boolean.getBoolean("jstat.debug");
    private static final boolean showUnsupported =
            Boolean.getBoolean("jstat.showUnsupported");

    private static final String JVMSTAT_USERDIR = ".jvmstat";
    private static final String OPTIONS_FILENAME = "jstat_options";
    private static final String UNSUPPORTED_OPTIONS_FILENAME = "jstat_unsupported_options";
    private static final String ALL_NAMES = "\\w*";

    private Comparator<Monitor> comparator;
    private int headerRate;
    private boolean help;
    private boolean list;
    private boolean options;
    private boolean constants;
    private boolean constantsOnly;
    private boolean strings;
    private boolean timestamp;
    private boolean snap;
    private boolean verbose;
    private String specialOption;
    private String names;

    private OptionFormat optionFormat;

    private int count = -1;
    private int interval = -1;
    private String vmIdString;

    private VmIdentifier vmId;

    public static void printUsage(PrintStream ps) {
        ps.println("Usage: jstat --help|-options");
        ps.println("       jstat -<option> [-t] [-h<lines>] <vmid> [<interval> [<count>]]");
        ps.println();
        ps.println("Definitions:");
        ps.println("  <option>      An option reported by the -options option");
        ps.println("  <vmid>        Virtual Machine Identifier. A vmid takes the following form:");
        ps.println("                     <lvmid>[@<hostname>[:<port>]]");
        ps.println("                Where <lvmid> is the local vm identifier for the target");
        ps.println("                Java virtual machine, typically a process id; <hostname> is");
        ps.println("                the name of the host running the target Java virtual machine;");
        ps.println("                and <port> is the port number for the rmiregistry on the");
        ps.println("                target host. See the jvmstat documentation for a more complete");
        ps.println("                description of the Virtual Machine Identifier.");
        ps.println("  <lines>       Number of samples between header lines.");
        ps.println("  <interval>    Sampling interval. The following forms are allowed:");
        ps.println("                    <n>[\"ms\"|\"s\"]");
        ps.println("                Where <n> is an integer and the suffix specifies the units as ");
        ps.println("                milliseconds(\"ms\") or seconds(\"s\"). The default units are \"ms\".");
        ps.println("  <count>       Number of samples to take before terminating.");
        ps.println("  -J<flag>      Pass <flag> directly to the runtime system.");
        ps.println("  -? -h --help  Prints this help message.");
        ps.println("  -help         Prints this help message.");

        // undocumented options:
        //   -list [<vmid>]  - list counter names
        //   -snap <vmid>    - snapshot counter values as name=value pairs
        //   -name <pattern> - output counters matching given pattern
        //   -a              - sort in ascending order (default)
        //   -d              - sort in descending order
        //   -v              - verbose output  (-snap)
        //   -constants      - output constants with -name output
        //   -strings        - output strings with -name output
        //   -help           - same as -? ...
    }

    private static int toMillis(String s) throws IllegalArgumentException {

        String[] unitStrings = { "ms", "s" }; // ordered from most specific to
                                              // least specific
        String unitString = null;
        String valueString = s;

        for (int i = 0; i < unitStrings.length; i++) {
            int index = s.indexOf(unitStrings[i]);
            if (index > 0) {
                unitString = s.substring(index);
                valueString = s.substring(0, index);
                break;
            }
        }

        try {
            int value = Integer.parseInt(valueString);

            if (unitString == null || unitString.compareTo("ms") == 0) {
                return value;
            } else if (unitString.compareTo("s") == 0) {
                return value * 1000;
            } else {
                throw new IllegalArgumentException(
                        "Unknow time unit: " + unitString);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Could not convert interval: " + s);
        }
    }

    public Arguments(String[] args) throws IllegalArgumentException {
        int argc = 0;

        if (args.length == 0) {
            help = true;
            return;
        }

        if ((args[0].compareTo("-?") == 0)
                || (args[0].compareTo("-h") == 0)
                || (args[0].compareTo("--help") == 0)
                // -help: legacy.
                || (args[0].compareTo("-help") == 0)) {
            help = true;
            return;
        } else if (args[0].compareTo("-options") == 0) {
            options = true;
            return;
        } else if (args[0].compareTo("-list") == 0) {
            list = true;
            if (args.length > 2) {
              throw new IllegalArgumentException("invalid argument count");
            }
            // list can take one arg - a vmid - fall through for arg processing
            argc++;
        }

        for ( ; (argc < args.length) && (args[argc].startsWith("-")); argc++) {
            String arg = args[argc];

            if (arg.compareTo("-a") == 0) {
                comparator = new AscendingMonitorComparator();
            } else if (arg.compareTo("-d") == 0) {
                comparator =  new DescendingMonitorComparator();
            } else if (arg.compareTo("-t") == 0) {
                timestamp = true;
            } else if (arg.compareTo("-v") == 0) {
                verbose = true;
            } else if ((arg.compareTo("-constants") == 0)
                       || (arg.compareTo("-c") == 0)) {
                constants = true;
            } else if ((arg.compareTo("-strings") == 0)
                       || (arg.compareTo("-s") == 0)) {
                strings = true;
            } else if (arg.startsWith("-h")) {
                String value;
                if (arg.compareTo("-h") != 0) {
                    value = arg.substring(2);
                } else {
                    argc++;
                    if (argc >= args.length) {
                        throw new IllegalArgumentException(
                                "-h requires an integer argument");
                    }
                    value = args[argc];
                }
                try {
                    headerRate = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    headerRate = -1;
                }
                if (headerRate < 0) {
                    throw new IllegalArgumentException(
                            "illegal -h argument: " + value);
                }
            } else if (arg.startsWith("-name")) {
                if (arg.startsWith("-name=")) {
                    names = arg.substring(7);
                } else {
                    argc++;
                    if (argc >= args.length) {
                        throw new IllegalArgumentException(
                                "option argument expected");
                    }
                    names = args[argc];
                }
            } else {
                /*
                 * there are scenarios here: special jstat_options file option
                 * or the rare case of a negative lvmid. The negative lvmid
                 * can occur in some operating environments (such as Windows
                 * 95/98/ME), so we provide for this case here by checking if
                 * the argument has any numerical characters. This assumes that
                 * there are no special jstat_options that contain numerical
                 * characters in their name.
                 */

                // extract the lvmid part of possible lvmid@host.domain:port
                String lvmidStr = null;
                int at_index = args[argc].indexOf('@');
                if (at_index < 0) {
                    lvmidStr = args[argc];
                } else {
                    lvmidStr = args[argc].substring(0, at_index);
                }

                // try to parse the lvmid part as an integer
                try {
                    int vmid = Integer.parseInt(lvmidStr);
                    // it parsed, assume a negative lvmid and continue
                    break;
                } catch (NumberFormatException nfe) {
                    // it didn't parse. check for the -snap or jstat_options
                    // file options.
                    if ((argc == 0) && (args[argc].compareTo("-snap") == 0)) {
                        snap = true;
                    } else if (argc == 0) {
                        specialOption = args[argc].substring(1);
                    } else {
                        throw new IllegalArgumentException(
                                "illegal argument: " + args[argc]);
                    }
                }
            }
        }

        // prevent 'jstat <pid>' from being accepted as a valid argument
        if (!(specialOption != null || list || snap || names != null)) {
            throw new IllegalArgumentException("-<option> required");
        }

        switch (args.length - argc) {
        case 3:
            if (snap) {
                throw new IllegalArgumentException("invalid argument count");
            }
            try {
                count = Integer.parseInt(args[args.length-1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("illegal count value: "
                                                   + args[args.length-1]);
            }
            interval = toMillis(args[args.length-2]);
            vmIdString = args[args.length-3];
            break;
        case 2:
            if (snap) {
                throw new IllegalArgumentException("invalid argument count");
            }
            interval = toMillis(args[args.length-1]);
            vmIdString = args[args.length-2];
            break;
        case 1:
            vmIdString = args[args.length-1];
            break;
        case 0:
            if (!list) {
                throw new IllegalArgumentException("invalid argument count");
            }
            break;
        default:
            throw new IllegalArgumentException("invalid argument count");
        }

        // set count and interval to their default values if not set above.
        if (count == -1 && interval == -1) {
            // default is for a single sample
            count = 1;
            interval = 0;
        }

        // validate arguments
        if (comparator == null) {
            comparator = new AscendingMonitorComparator();
        }

        // allow ',' characters to separate names, convert to '|' chars
        names = (names == null) ? ALL_NAMES : names.replace(',', '|');

        // verify that the given pattern parses without errors
        try {
            Pattern pattern = Pattern.compile(names);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Bad name pattern: "
                                               + e.getMessage());
        }

        // verify that the special option is valid and get it's formatter
        if (specialOption != null) {
            OptionFinder finder = new OptionFinder(optionsSources());
            optionFormat = finder.getOptionFormat(specialOption, timestamp);
            if (optionFormat == null) {
                throw new IllegalArgumentException("Unknown option: -"
                                                   + specialOption);
            }
        }

        // verify that the vm identifier is valied
        try {
            vmId = new VmIdentifier(vmIdString);
        } catch (URISyntaxException e) {
            IllegalArgumentException iae = new IllegalArgumentException(
                    "Malformed VM Identifier: " + vmIdString);
            iae.initCause(e);
            throw iae;
        }
    }

    public Comparator<Monitor> comparator() {
        return comparator;
    }

    public boolean isHelp() {
        return help;
    }

    public boolean isList() {
        return list;
    }

    public boolean isSnap() {
        return snap;
    }

    public boolean isOptions() {
        return options;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean printConstants() {
        return constants;
    }

    public boolean isConstantsOnly() {
        return constantsOnly;
    }

    public boolean printStrings() {
        return strings;
    }

    public boolean showUnsupported() {
        return showUnsupported;
    }

    public int headerRate() {
        return headerRate;
    }

    public String counterNames() {
        return names;
    }

    public VmIdentifier vmId() {
        return vmId;
    }

    public String vmIdString() {
        return vmIdString;
    }

    public int sampleInterval() {
        return interval;
    }

    public int sampleCount() {
        return count;
    }

    public boolean isTimestamp() {
        return timestamp;
    }

    public boolean isSpecialOption() {
        return specialOption != null;
    }

    public String specialOption() {
        return specialOption;
    }

    public OptionFormat optionFormat() {
        return optionFormat;
    }

    public List<URL> optionsSources() {
        List<URL> sources = new ArrayList<URL>();
        int i = 0;

        String filename = OPTIONS_FILENAME;

        try {
            String userHome = System.getProperty("user.home");
            String userDir = userHome + "/" + JVMSTAT_USERDIR;
            File home = new File(userDir + "/" + filename);
            sources.add(home.toURI().toURL());
        } catch (Exception e) {
            if (debug) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
            throw new IllegalArgumentException("Internal Error: Bad URL: "
                                               + e.getMessage());
        }
        URL u = this.getClass().getResource("resources/" + filename);
        assert u != null;
        sources.add(u);

        if (showUnsupported) {
            u = this.getClass().getResource("resources/" +  UNSUPPORTED_OPTIONS_FILENAME);
            assert u != null;
            sources.add(u);
        }
        return sources;
    }
}
