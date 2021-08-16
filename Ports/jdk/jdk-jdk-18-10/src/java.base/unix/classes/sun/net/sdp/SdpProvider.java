/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.sdp;

import sun.net.NetHooks;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.*;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintStream;

import sun.net.sdp.SdpSupport;
import sun.security.action.GetPropertyAction;

/**
 * A NetHooks provider that converts sockets from the TCP to SDP protocol prior
 * to binding or connecting.
 */

public class SdpProvider extends NetHooks.Provider {
    // maximum port
    private static final int MAX_PORT = 65535;

    // indicates if SDP is enabled and the rules for when the protocol is used
    private final boolean enabled;
    private final List<Rule> rules;

    // logging for debug purposes
    private PrintStream log;

    public SdpProvider() {
        Properties props = GetPropertyAction.privilegedGetProperties();
        // if this property is not defined then there is nothing to do.
        String file = props.getProperty("com.sun.sdp.conf");
        if (file == null) {
            this.enabled = false;
            this.rules = null;
            return;
        }

        // load configuration file
        List<Rule> list = null;
        try {
            list = loadRulesFromFile(file);
        } catch (IOException e) {
            fail("Error reading %s: %s", file, e.getMessage());
        }

        // check if debugging is enabled
        PrintStream out = null;
        String logfile = props.getProperty("com.sun.sdp.debug");
        if (logfile != null) {
            out = System.out;
            if (!logfile.isEmpty()) {
                try {
                    out = new PrintStream(logfile);
                } catch (IOException ignore) { }
            }
        }

        this.enabled = !list.isEmpty();
        this.rules = list;
        this.log = out;
    }

    // supported actions
    private static enum Action {
        BIND,
        CONNECT;
    }

    // a rule for matching a bind or connect request
    private static interface Rule {
        boolean match(Action action, InetAddress address, int port);
    }

    // rule to match port[-end]
    private static class PortRangeRule implements Rule {
        private final Action action;
        private final int portStart;
        private final int portEnd;
        PortRangeRule(Action action, int portStart, int portEnd) {
            this.action = action;
            this.portStart = portStart;
            this.portEnd = portEnd;
        }
        Action action() {
            return action;
        }
        @Override
        public boolean match(Action action, InetAddress address, int port) {
            return (action == this.action &&
                    port >= this.portStart &&
                    port <= this.portEnd);
        }
    }

    // rule to match address[/prefix] port[-end]
    private static class AddressPortRangeRule extends PortRangeRule {
        private final byte[] addressAsBytes;
        private final int prefixByteCount;
        private final byte mask;
        AddressPortRangeRule(Action action, InetAddress address,
                             int prefix, int port, int end)
        {
            super(action, port, end);
            this.addressAsBytes = address.getAddress();
            this.prefixByteCount = prefix >> 3;
            this.mask = (byte)(0xff << (8 - (prefix % 8)));
        }
        @Override
        public boolean match(Action action, InetAddress address, int port) {
            if (action != action())
                return false;
            byte[] candidate = address.getAddress();
            // same address type?
            if (candidate.length != addressAsBytes.length)
                return false;
            // check bytes
            for (int i=0; i<prefixByteCount; i++) {
                if (candidate[i] != addressAsBytes[i])
                    return false;
            }
            // check remaining bits
            if ((prefixByteCount < addressAsBytes.length) &&
                ((candidate[prefixByteCount] & mask) !=
                 (addressAsBytes[prefixByteCount] & mask)))
                    return false;
            return super.match(action, address, port);
        }
    }

    // parses port:[-end]
    private static int[] parsePortRange(String s) {
        int pos = s.indexOf('-');
        try {
            int[] result = new int[2];
            if (pos < 0) {
                boolean all = s.equals("*");
                result[0] = all ? 0 : Integer.parseInt(s);
                result[1] = all ? MAX_PORT : result[0];
            } else {
                String low = s.substring(0, pos);
                if (low.isEmpty()) low = "*";
                String high = s.substring(pos+1);
                if (high.isEmpty()) high = "*";
                result[0] = low.equals("*") ? 0 : Integer.parseInt(low);
                result[1] = high.equals("*") ? MAX_PORT : Integer.parseInt(high);
            }
            return result;
        } catch (NumberFormatException e) {
            return new int[0];
        }
    }

    private static void fail(String msg, Object... args) {
        Formatter f = new Formatter();
        f.format(msg, args);
        throw new RuntimeException(f.out().toString());
    }

    // loads rules from the given file
    // Each non-blank/non-comment line must have the format:
    // ("bind" | "connect") 1*LWSP-char (hostname | ipaddress["/" prefix])
    //     1*LWSP-char ("*" | port) [ "-" ("*" | port) ]
    private static List<Rule> loadRulesFromFile(String file)
        throws IOException
    {
        Scanner scanner = new Scanner(new File(file));
        try {
            List<Rule> result = new ArrayList<>();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();

                // skip blank lines and comments
                if (line.isEmpty() || line.charAt(0) == '#')
                    continue;

                // must have 3 fields
                String[] s = line.split("\\s+");
                if (s.length != 3) {
                    fail("Malformed line '%s'", line);
                    continue;
                }

                // first field is the action ("bind" or "connect")
                Action action = null;
                for (Action a: Action.values()) {
                    if (s[0].equalsIgnoreCase(a.name())) {
                        action = a;
                        break;
                    }
                }
                if (action == null) {
                    fail("Action '%s' not recognized", s[0]);
                    continue;
                }

                // * port[-end]
                int[] ports = parsePortRange(s[2]);
                if (ports.length == 0) {
                    fail("Malformed port range '%s'", s[2]);
                    continue;
                }

                // match all addresses
                if (s[1].equals("*")) {
                    result.add(new PortRangeRule(action, ports[0], ports[1]));
                    continue;
                }

                // hostname | ipaddress[/prefix]
                int pos = s[1].indexOf('/');
                try {
                    if (pos < 0) {
                        // hostname or ipaddress (no prefix)
                        InetAddress[] addresses = InetAddress.getAllByName(s[1]);
                        for (InetAddress address: addresses) {
                            int prefix =
                                (address instanceof Inet4Address) ? 32 : 128;
                            result.add(new AddressPortRangeRule(action, address,
                                prefix, ports[0], ports[1]));
                        }
                    } else {
                        // ipaddress/prefix
                        InetAddress address = InetAddress
                            .getByName(s[1].substring(0, pos));
                        int prefix = -1;
                        try {
                            prefix = Integer.parseInt(s[1], pos + 1,
                                s[1].length(), 10);
                            if (address instanceof Inet4Address) {
                                // must be 1-31
                                if (prefix < 0 || prefix > 32) prefix = -1;
                            } else {
                                // must be 1-128
                                if (prefix < 0 || prefix > 128) prefix = -1;
                            }
                        } catch (NumberFormatException e) {
                        }

                        if (prefix > 0) {
                            result.add(new AddressPortRangeRule(action,
                                        address, prefix, ports[0], ports[1]));
                        } else {
                            fail("Malformed prefix '%s'", s[1]);
                            continue;
                        }
                    }
                } catch (UnknownHostException uhe) {
                    fail("Unknown host or malformed IP address '%s'", s[1]);
                    continue;
                }
            }
            return result;
        } finally {
            scanner.close();
        }
    }

    // converts unbound TCP socket to a SDP socket if it matches the rules
    private void convertTcpToSdpIfMatch(FileDescriptor fdObj,
                                               Action action,
                                               InetAddress address,
                                               int port)
        throws IOException
    {
        boolean matched = false;
        for (Rule rule: rules) {
            if (rule.match(action, address, port)) {
                SdpSupport.convertSocket(fdObj);
                matched = true;
                break;
            }
        }
        if (log != null) {
            String addr = (address instanceof Inet4Address) ?
                address.getHostAddress() : "[" + address.getHostAddress() + "]";
            if (matched) {
                log.format("%s to %s:%d (socket converted to SDP protocol)\n", action, addr, port);
            } else {
                log.format("%s to %s:%d (no match)\n", action, addr, port);
            }
        }
    }

    @Override
    public void implBeforeTcpBind(FileDescriptor fdObj,
                              InetAddress address,
                              int port)
        throws IOException
    {
        if (enabled)
            convertTcpToSdpIfMatch(fdObj, Action.BIND, address, port);
    }

    @Override
    public void implBeforeTcpConnect(FileDescriptor fdObj,
                                InetAddress address,
                                int port)
        throws IOException
    {
        if (enabled)
            convertTcpToSdpIfMatch(fdObj, Action.CONNECT, address, port);
    }
}
