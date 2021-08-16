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

package sun.tools.jps;

import java.io.*;
import java.net.*;
import sun.jvmstat.monitor.*;

/**
 * Class for processing command line arguments and providing method
 * level access to the command line arguments.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class Arguments {

    private static final boolean debug = Boolean.getBoolean("jps.debug");
    private static final boolean printStackTrace = Boolean.getBoolean(
            "jps.printStackTrace");

    private boolean help;
    private boolean quiet;
    private boolean longPaths;
    private boolean vmArgs;
    private boolean vmFlags;
    private boolean mainArgs;
    private String hostname;
    private HostIdentifier hostId;

    public static void printUsage(PrintStream ps) {
      ps.println("usage: jps [--help]");
      ps.println("       jps [-q] [-mlvV] [<hostid>]");
      ps.println();
      ps.println("Definitions:");
      ps.println("    <hostid>:      <hostname>[:<port>]");
      ps.println("    -? -h --help -help: Print this help message and exit.");
    }

    public Arguments(String[] args) throws IllegalArgumentException {
        int argc = 0;

        if (args.length == 1) {
            if ((args[0].compareTo("-?") == 0)
                || (args[0].compareTo("-h")== 0)
                || (args[0].compareTo("--help")== 0)
                // -help: legacy.
                || (args[0].compareTo("-help")== 0)) {
              help = true;
              return;
            }
        }

        for (argc = 0; (argc < args.length) && (args[argc].startsWith("-"));
                argc++) {
            String arg = args[argc];

            if (arg.compareTo("-q") == 0) {
              quiet = true;
            } else if (arg.startsWith("-")) {
                for (int j = 1; j < arg.length(); j++) {
                    switch (arg.charAt(j)) {
                    case 'm':
                        mainArgs = true;
                        break;
                    case 'l':
                        longPaths = true;
                        break;
                    case 'v':
                        vmArgs = true;
                        break;
                    case 'V':
                        vmFlags = true;
                        break;
                    default:
                        throw new IllegalArgumentException("illegal argument: "
                                                           + args[argc]);
                    }
                }
            } else {
                throw new IllegalArgumentException("illegal argument: "
                                                   + args[argc]);
            }
        }

        switch (args.length - argc) {
        case 0:
            hostname = null;
            break;
        case 1:
            hostname = args[args.length - 1];
            break;
        default:
            throw new IllegalArgumentException("invalid argument count");
        }

        try {
            hostId = new HostIdentifier(hostname);
        } catch (URISyntaxException e) {
            IllegalArgumentException iae =
                    new IllegalArgumentException("Malformed Host Identifier: "
                                                 + hostname);
            iae.initCause(e);
            throw iae;
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean printStackTrace() {
        return printStackTrace;
    }

    public boolean isHelp() {
        return help;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public boolean showLongPaths() {
        return longPaths;
    }

    public boolean showVmArgs() {
        return vmArgs;
    }

    public boolean showVmFlags() {
        return vmFlags;
    }

    public boolean showMainArgs() {
        return mainArgs;
    }

    public String hostname() {
        return hostname;
    }

    public HostIdentifier hostId() {
        return hostId;
    }
}
