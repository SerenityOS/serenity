/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import sun.jvm.hotspot.debugger.DebuggerException;
import sun.jvm.hotspot.tools.JStack;
import sun.jvm.hotspot.tools.JMap;
import sun.jvm.hotspot.tools.JInfo;
import sun.jvm.hotspot.tools.JSnap;

public class SALauncher {

    private static boolean launcherHelp() {
        System.out.println("    clhsdb       \tcommand line debugger");
        System.out.println("    hsdb         \tui debugger");
        System.out.println("    debugd --help\tto get more information");
        System.out.println("    jstack --help\tto get more information");
        System.out.println("    jmap   --help\tto get more information");
        System.out.println("    jinfo  --help\tto get more information");
        System.out.println("    jsnap  --help\tto get more information");
        return false;
    }

    private static boolean commonHelp(String mode) {
        return commonHelp(mode, false);
    }

    private static boolean commonHelpWithConnect(String mode) {
        return commonHelp(mode, true);
    }

    private static boolean commonHelp(String mode, boolean canConnectToRemote) {
        // --pid <pid>
        // --exe <exe>
        // --core <core>
        // --connect [<id>@]<host>[:registryport]
        System.out.println("    --pid <pid>             To attach to and operate on the given live process.");
        System.out.println("    --core <corefile>       To operate on the given core file.");
        System.out.println("    --exe <executable for corefile>");
        if (canConnectToRemote) {
            System.out.println("    --connect [<serverid>@]<host>[:registryport][/servername] To connect to a remote debug server (debugd).");
        }
        System.out.println();
        System.out.println("    The --core and --exe options must be set together to give the core");
        System.out.println("    file, and associated executable, to operate on. They can use");
        System.out.println("    absolute or relative paths.");
        System.out.println("    The --pid option can be set to operate on a live process.");
        if (canConnectToRemote) {
            System.out.println("    The --connect option can be set to connect to a debug server (debugd).");
            System.out.println("    --core, --pid, and --connect are mutually exclusive.");
        } else {
            System.out.println("    --core and --pid are mutually exclusive.");
        }
        System.out.println();
        System.out.println("    Examples: jhsdb " + mode + " --pid 1234");
        System.out.println("          or  jhsdb " + mode + " --core ./core.1234 --exe ./myexe");
        if (canConnectToRemote) {
            System.out.println("          or  jhsdb " + mode + " --connect serverid@debugserver:1234/servername");
        }
        return false;
    }

    private static boolean debugdHelp() {
        System.out.println("    --serverid <id>         A unique identifier for this debugd server.");
        System.out.println("    --servername <name>     Instance name of debugd server.");
        System.out.println("    --rmiport <port>        Sets the port number to which the RMI connector is bound." +
                " If not specified a random available port is used.");
        System.out.println("    --registryport <port>   Sets the RMI registry port." +
                " This option overrides the system property 'sun.jvm.hotspot.rmi.port'. If not specified," +
                " the system property is used. If the system property is not set, the default port 1099 is used.");
        System.out.println("    --disable-registry      Do not start RMI registry (use already started RMI registry)");
        System.out.println("    --hostname <hostname>   Sets the hostname the RMI connector is bound. The value could" +
                " be a hostname or an IPv4/IPv6 address. This option overrides the system property" +
                " 'java.rmi.server.hostname'. If not specified, the system property is used. If the system" +
                " property is not set, a system hostname is used.");
        return commonHelp("debugd");
    }

    private static boolean jinfoHelp() {
        // --flags -> -flags
        // --sysprops -> -sysprops
        System.out.println("    --flags                 To print VM flags.");
        System.out.println("    --sysprops              To print Java System properties.");
        System.out.println("    <no option>             To print both of the above.");
        return commonHelpWithConnect("jinfo");
    }

    private static boolean jmapHelp() {
        // --heap -> -heap
        // --binaryheap -> -heap:format=b
        // --histo -> -histo
        // --clstats -> -clstats
        // --finalizerinfo -> -finalizerinfo

        System.out.println("    <no option>             To print same info as Solaris pmap.");
        System.out.println("    --heap                  To print java heap summary.");
        System.out.println("    --binaryheap            To dump java heap in hprof binary format.");
        System.out.println("    --dumpfile <name>       The name of the dump file. Only valid with --binaryheap.");
        System.out.println("    --gz <1-9>              The compression level for gzipped dump file. Only valid with --binaryheap.");
        System.out.println("    --histo                 To print histogram of java object heap.");
        System.out.println("    --clstats               To print class loader statistics.");
        System.out.println("    --finalizerinfo         To print information on objects awaiting finalization.");
        return commonHelpWithConnect("jmap");
    }

    private static boolean jstackHelp() {
        // --locks -> -l
        // --mixed -> -m
        System.out.println("    --locks                 To print java.util.concurrent locks.");
        System.out.println("    --mixed                 To print both Java and native frames (mixed mode).");
        return commonHelpWithConnect("jstack");
    }

    private static boolean jsnapHelp() {
        System.out.println("    --all                   To print all performance counters.");
        return commonHelpWithConnect("jsnap");
    }

    private static boolean toolHelp(String toolName) {
        switch (toolName) {
            case "jstack":
                return jstackHelp();
            case "jinfo":
                return jinfoHelp();
            case "jmap":
                return jmapHelp();
            case "jsnap":
                return jsnapHelp();
            case "debugd":
                return debugdHelp();
            case "hsdb":
            case "clhsdb":
                return commonHelpWithConnect(toolName);
            default:
                return launcherHelp();
        }
    }

    private static final String NO_REMOTE = null;

    private static String[] buildAttachArgs(Map<String, String> newArgMap,
                                            boolean allowEmpty) {
        String pid = newArgMap.remove("pid");
        String exe = newArgMap.remove("exe");
        String core = newArgMap.remove("core");
        String connect = newArgMap.remove("connect");
        if (!allowEmpty && (pid == null) && (exe == null) && (connect == NO_REMOTE)) {
            throw new SAGetoptException("You have to set --pid or --exe or --connect.");
        }

        List<String> newArgs = new ArrayList<>();
        for (var entry : newArgMap.entrySet()) {
            newArgs.add(entry.getKey());
            if (entry.getValue() != null) {
                newArgs.add(entry.getValue());
            }
        }

        if (pid != null) { // Attach to live process
            if (exe != null) {
                throw new SAGetoptException("Unnecessary argument: --exe");
            } else if (core != null) {
                throw new SAGetoptException("Unnecessary argument: --core");
            } else if (connect != NO_REMOTE) {
                throw new SAGetoptException("Unnecessary argument: --connect");
            } else if (!pid.matches("^\\d+$")) {
                throw new SAGetoptException("Invalid pid: " + pid);
            }

            newArgs.add(pid);
        } else if (exe != null) {
            if (connect != NO_REMOTE) {
                throw new SAGetoptException("Unnecessary argument: --connect");
            } else if (exe.length() == 0) {
                throw new SAGetoptException("You have to set --exe.");
            }

            newArgs.add(exe);

            if ((core == null) || (core.length() == 0)) {
                throw new SAGetoptException("You have to set --core.");
            }

            newArgs.add(core);
        } else if (connect != NO_REMOTE) {
            newArgs.add(connect);
        }

        return newArgs.toArray(new String[0]);
    }

    /**
     * This method converts jhsdb-style options (oldArgs) to old fashioned
     * style. SALauncher delegates the work to the entry point of each tool.
     * Thus we need to convert the arguments.
     * For example, `jhsdb jstack --mixed` needs to be converted to `jstack -m`.
     *
     * longOptsMap holds the rule how this method should convert the args.
     * The key is the name of jhsdb style, the value is the name of
     * old fashioned style. If you want to convert mixed option in jstack,
     * you need to set "mixed" to the key, and to set "-m" to the value
     * in longOptsMap. If the option have the value, you need to add "=" to
     * the key like "exe=".
     *
     * You also can set the options which cannot be mapped to old fashioned
     * arguments. For example, `jhsdb jmap --binaryheap` cannot be mapped to
     * `jmap` option directly. But you set it to longOptsMap, then you can know
     * the user sets "binaryheap" option, and SALauncher should set
     * "-heap:format:b" to jmap option.
     *
     * This method returns the map of the old fashioned key/val pairs.
     * It can be used to build args in string array at buildAttachArgs().
     */
    private static Map<String, String> parseOptions(String[] oldArgs,
                                                    Map<String, String> longOptsMap) {
        SAGetopt sg = new SAGetopt(oldArgs);
        String[] longOpts = longOptsMap.keySet().toArray(new String[0]);
        Map<String, String> newArgMap = new HashMap<>();

        /*
         * Parse each jhsdb-style option via SAGetopt.
         * SAGetopt parses and validates the argument. If the user passes invalid
         * option, SAGetoptException will be occurred at SAGetopt::next.
         * Thus there is no need to validate it here.
         *
         * We can get option value via SAGetopt::get. If jhsdb-style option has
         * '=' at the tail, we put old fashioned option with it to newArgMap.
         */
        String s;
        while ((s = sg.next(null, longOpts)) != null) {
            var val = longOptsMap.get(s);
            if (val != null) {
                newArgMap.put(val, null);
            } else {
                val = longOptsMap.get(s + "=");
                if (val != null) {
                    newArgMap.put(val, sg.getOptarg());
                }
            }
        }

        return newArgMap;
    }

    private static void runCLHSDB(String[] oldArgs) {
        Map<String, String> longOptsMap = Map.of("exe=", "exe",
                                                 "core=", "core",
                                                 "pid=", "pid",
                                                 "connect=", "connect");
        Map<String, String> newArgMap = parseOptions(oldArgs, longOptsMap);
        CLHSDB.main(buildAttachArgs(newArgMap, true));
    }

    private static void runHSDB(String[] oldArgs) {
        Map<String, String> longOptsMap = Map.of("exe=", "exe",
                                                 "core=", "core",
                                                 "pid=", "pid",
                                                 "connect=", "connect");
        Map<String, String> newArgMap = parseOptions(oldArgs, longOptsMap);
        HSDB.main(buildAttachArgs(newArgMap, true));
    }

    private static void runJSTACK(String[] oldArgs) {
        Map<String, String> longOptsMap = Map.of("exe=", "exe",
                                                 "core=", "core",
                                                 "pid=", "pid",
                                                 "connect=", "connect",
                                                 "mixed", "-m",
                                                 "locks", "-l");
        Map<String, String> newArgMap = parseOptions(oldArgs, longOptsMap);
        JStack jstack = new JStack(false, false);
        jstack.runWithArgs(buildAttachArgs(newArgMap, false));
    }

    private static void runJMAP(String[] oldArgs) {
        Map<String, String> longOptsMap = Map.ofEntries(
                Map.entry("exe=", "exe"),
                Map.entry("core=", "core"),
                Map.entry("pid=", "pid"),
                Map.entry("connect=", "connect"),
                Map.entry("heap", "-heap"),
                Map.entry("binaryheap", "binaryheap"),
                Map.entry("dumpfile=", "dumpfile"),
                Map.entry("gz=", "gz"),
                Map.entry("histo", "-histo"),
                Map.entry("clstats", "-clstats"),
                Map.entry("finalizerinfo", "-finalizerinfo"));
        Map<String, String> newArgMap = parseOptions(oldArgs, longOptsMap);

        boolean requestHeapdump = newArgMap.containsKey("binaryheap");
        String dumpfile = newArgMap.get("dumpfile");
        String gzLevel = newArgMap.get("gz");
        String command = "-heap:format=b";
        if (!requestHeapdump && (dumpfile != null)) {
            throw new IllegalArgumentException("Unexpected argument: dumpfile");
        }
        if (requestHeapdump) {
            if (gzLevel != null) {
                command += ",gz=" + gzLevel;
            }
            if (dumpfile != null) {
                command += ",file=" + dumpfile;
            }
            newArgMap.put(command, null);
        }

        newArgMap.remove("binaryheap");
        newArgMap.remove("dumpfile");
        newArgMap.remove("gz");
        JMap.main(buildAttachArgs(newArgMap, false));
    }

    private static void runJINFO(String[] oldArgs) {
        Map<String, String> longOptsMap = Map.of("exe=", "exe",
                                                 "core=", "core",
                                                 "pid=", "pid",
                                                 "connect=", "connect",
                                                 "flags", "-flags",
                                                 "sysprops", "-sysprops");
        Map<String, String> newArgMap = parseOptions(oldArgs, longOptsMap);
        JInfo.main(buildAttachArgs(newArgMap, false));
    }

    private static void runJSNAP(String[] oldArgs) {
        Map<String, String> longOptsMap = Map.of("exe=", "exe",
                                                 "core=", "core",
                                                 "pid=", "pid",
                                                 "connect=", "connect",
                                                 "all", "-a");
        Map<String, String> newArgMap = parseOptions(oldArgs, longOptsMap);
        JSnap.main(buildAttachArgs(newArgMap, false));
    }

    private static void runDEBUGD(String[] args) {
        // By default SA agent classes prefer Windows process debugger
        // to windbg debugger. SA expects special properties to be set
        // to choose other debuggers. We will set those here before
        // attaching to SA agent.
        System.setProperty("sun.jvm.hotspot.debugger.useWindbgDebugger", "true");

        Map<String, String> longOptsMap = Map.of("exe=", "exe",
                "core=", "core",
                "pid=", "pid",
                "serverid=", "serverid",
                "rmiport=", "rmiport",
                "registryport=", "registryport",
                "disable-registry", "disable-registry",
                "hostname=", "hostname",
                "servername=", "servername");

        Map<String, String> argMap = parseOptions(args, longOptsMap);

        // Run the basic check for the options. If the check fails
        // SAGetoptException will be thrown
        buildAttachArgs(new HashMap<>(argMap), false);

        String serverID = argMap.get("serverid");
        String rmiPortString = argMap.get("rmiport");
        String registryPort = argMap.get("registryport");
        String host = argMap.get("hostname");
        String javaExecutableName = argMap.get("exe");
        String coreFileName = argMap.get("core");
        String pidString = argMap.get("pid");
        String serverName = argMap.get("servername");

        // Set RMI registry port, if specified
        if (registryPort != null) {
            try {
                Integer.parseInt(registryPort);
            } catch (NumberFormatException ex) {
                throw new SAGetoptException("Invalid registry port: " + registryPort);
            }
            System.setProperty("sun.jvm.hotspot.rmi.port", registryPort);
        }

        // Disable RMI registry if specified
        if (argMap.containsKey("disable-registry")) {
            System.setProperty("sun.jvm.hotspot.rmi.startRegistry", "false");
        }

        // Set RMI connector hostname, if specified
        if (host != null && !host.trim().isEmpty()) {
            System.setProperty("java.rmi.server.hostname", host);
        }

        // Set RMI connector port, if specified
        int rmiPort = 0;
        if (rmiPortString != null) {
            try {
                rmiPort = Integer.parseInt(rmiPortString);
            } catch (NumberFormatException ex) {
                throw new SAGetoptException("Invalid RMI connector port: " + rmiPortString);
            }
        }

        final HotSpotAgent agent = new HotSpotAgent();

        if (pidString != null) {
            int pid = 0;
            try {
                pid = Integer.parseInt(pidString);
            } catch (NumberFormatException ex) {
                throw new SAGetoptException("Invalid pid: " + pidString);
            }
            System.err.println("Attaching to process ID " + pid + " and starting RMI services," +
                    " please wait...");
            try {
                agent.startServer(pid, serverID, serverName, rmiPort);
            } catch (DebuggerException e) {
                System.err.print("Error attaching to process or starting server: ");
                e.printStackTrace();
                System.exit(1);
            } catch (NumberFormatException ex) {
                throw new SAGetoptException("Invalid pid: " + pid);
            }
        } else if (javaExecutableName != null) {
            System.err.println("Attaching to core " + coreFileName +
                    " from executable " + javaExecutableName + " and starting RMI services, please wait...");
            try {
                agent.startServer(javaExecutableName, coreFileName, serverID, serverName, rmiPort);
            } catch (DebuggerException e) {
                System.err.print("Error attaching to core file or starting server: ");
                e.printStackTrace();
                System.exit(1);
            }
        }
        // shutdown hook to clean-up the server in case of forced exit.
        Runtime.getRuntime().addShutdownHook(new java.lang.Thread(agent::shutdownServer));
        System.err.println("Debugger attached and RMI services started." + ((rmiPortString != null) ?
                (" RMI connector is bound to port " + rmiPort + ".") : ""));

    }

    // Key: tool name, Value: launcher method
    private static Map<String, Consumer<String[]>> toolMap =
        Map.of("clhsdb", SALauncher::runCLHSDB,
               "hsdb", SALauncher::runHSDB,
               "jstack", SALauncher::runJSTACK,
               "jmap", SALauncher::runJMAP,
               "jinfo", SALauncher::runJINFO,
               "jsnap", SALauncher::runJSNAP,
               "debugd", SALauncher::runDEBUGD);

    public static void main(String[] args) {
        // Provide a help
        if (args.length == 0) {
            launcherHelp();
            return;
        }
        // No arguments imply help for debugd, jstack, jmap, jinfo but launch clhsdb and hsdb
        if (args.length == 1 && !args[0].equals("clhsdb") && !args[0].equals("hsdb")) {
            toolHelp(args[0]);
            return;
        }

        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("-help") || arg.equals("--help")) {
                toolHelp(args[0]);
                return;
            }
        }

        String[] oldArgs = Arrays.copyOfRange(args, 1, args.length);

        try {
            var func = toolMap.get(args[0]);
            if (func == null) {
                throw new SAGetoptException("Unknown tool: " + args[0]);
            } else {
                func.accept(oldArgs);
            }
        } catch (SAGetoptException e) {
            System.err.println(e.getMessage());
            toolHelp(args[0]);
            // Exit with error status
            System.exit(1);
        }
    }
}
