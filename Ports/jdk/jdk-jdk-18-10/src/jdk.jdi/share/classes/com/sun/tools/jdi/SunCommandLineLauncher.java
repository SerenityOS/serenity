/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdi;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Random;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.Transport;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.connect.spi.TransportService;

public class SunCommandLineLauncher extends AbstractLauncher {

    static private final String ARG_HOME = "home";
    static private final String ARG_OPTIONS = "options";
    static private final String ARG_MAIN = "main";
    static private final String ARG_INIT_SUSPEND = "suspend";
    static private final String ARG_QUOTE = "quote";
    static private final String ARG_VM_EXEC = "vmexec";

    TransportService transportService;
    Transport transport;
    boolean usingSharedMemory = false;

    TransportService transportService() {
        return transportService;
    }

    public Transport transport() {
        return transport;
    }

    public SunCommandLineLauncher() {
        super();

        /**
         * By default this connector uses either the shared memory
         * transport or the socket transport
         */
        try {
            transportService = (TransportService)Class.
                forName("com.sun.tools.jdi.SharedMemoryTransportService").
                getDeclaredConstructor().newInstance();
            transport = new Transport() {
                public String name() {
                    return "dt_shmem";
                }
            };
            usingSharedMemory = true;
        } catch (ClassNotFoundException |
                 UnsatisfiedLinkError |
                 InstantiationException |
                 InvocationTargetException |
                 IllegalAccessException |
                 NoSuchMethodException x) {
        };
        if (transportService == null) {
            transportService = new SocketTransportService();
            transport = new Transport() {
                public String name() {
                    return "dt_socket";
                }
            };
        }

        addStringArgument(
                ARG_HOME,
                getString("sun.home.label"),
                getString("sun.home"),
                System.getProperty("java.home"),
                false);
        addStringArgument(
                ARG_OPTIONS,
                getString("sun.options.label"),
                getString("sun.options"),
                "",
                false);
        addStringArgument(
                ARG_MAIN,
                getString("sun.main.label"),
                getString("sun.main"),
                "",
                true);

        addBooleanArgument(
                ARG_INIT_SUSPEND,
                getString("sun.init_suspend.label"),
                getString("sun.init_suspend"),
                true,
                false);

        addStringArgument(
                ARG_QUOTE,
                getString("sun.quote.label"),
                getString("sun.quote"),
                "\"",
                true);
        addStringArgument(
                ARG_VM_EXEC,
                getString("sun.vm_exec.label"),
                getString("sun.vm_exec"),
                "java",
                true);
    }

    static boolean hasWhitespace(String string) {
        int length = string.length();
        for (int i = 0; i < length; i++) {
            if (Character.isWhitespace(string.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public VirtualMachine
        launch(Map<String, ? extends Connector.Argument> arguments)
        throws IOException, IllegalConnectorArgumentsException,
               VMStartException
    {
        VirtualMachine vm;

        String home = argument(ARG_HOME, arguments).value();
        String options = argument(ARG_OPTIONS, arguments).value();
        String mainClassAndArgs = argument(ARG_MAIN, arguments).value();
        boolean wait = ((BooleanArgumentImpl)argument(ARG_INIT_SUSPEND,
                                                  arguments)).booleanValue();
        String quote = argument(ARG_QUOTE, arguments).value();
        String exe = argument(ARG_VM_EXEC, arguments).value();
        String exePath = null;

        if (quote.length() > 1) {
            throw new IllegalConnectorArgumentsException("Invalid length",
                                                         ARG_QUOTE);
        }

        if ((options.indexOf("-Djava.compiler=") != -1) &&
            (options.toLowerCase().indexOf("-djava.compiler=none") == -1)) {
            throw new IllegalConnectorArgumentsException("Cannot debug with a JIT compiler",
                                                         ARG_OPTIONS);
        }

        /*
         * Start listening.
         * If we're using the shared memory transport then we pick a
         * random address rather than using the (fixed) default.
         * Random() uses System.currentTimeMillis() as the seed
         * which can be a problem on windows (many calls to
         * currentTimeMillis can return the same value), so
         * we do a few retries if we get an IOException (we
         * assume the IOException is the filename is already in use.)
         */
        TransportService.ListenKey listenKey;
        if (usingSharedMemory) {
            Random rr = new Random();
            int failCount = 0;
            while(true) {
                try {
                    String address = "javadebug" +
                        String.valueOf(rr.nextInt(100000));
                    listenKey = transportService().startListening(address);
                    break;
                } catch (IOException ioe) {
                    if (++failCount > 5) {
                        throw ioe;
                    }
                }
            }
        } else {
            listenKey = transportService().startListening();
        }
        String address = listenKey.address();

        try {
            if (home.length() > 0) {
                exePath = home + File.separator + "bin" + File.separator + exe;
            } else {
                exePath = exe;
            }
            // Quote only if necessary in case the quote arg value is bogus
            if (hasWhitespace(exePath)) {
                exePath = quote + exePath + quote;
            }

            String xrun = "transport=" + transport().name() +
                          ",address=" + address +
                          ",suspend=" + (wait? 'y' : 'n');
            // Quote only if necessary in case the quote arg value is bogus
            if (hasWhitespace(xrun)) {
                xrun = quote + xrun + quote;
            }

            String command = exePath + ' ' +
                             options + ' ' +
                             "-Xdebug " +
                             "-Xrunjdwp:" + xrun + ' ' +
                             mainClassAndArgs;

            // System.err.println("Command: \"" + command + '"');
            vm = launch(tokenizeCommand(command, quote.charAt(0)), address, listenKey,
                        transportService());
        } finally {
            transportService().stopListening(listenKey);
        }

        return vm;
    }

    public String name() {
        return "com.sun.jdi.CommandLineLaunch";
    }

    public String description() {
        return getString("sun.description");
    }
}
