/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.dcmd;

import jdk.test.lib.process.OutputAnalyzer;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.lang.management.ManagementFactory;

import java.util.HashMap;

/**
 * Executes Diagnostic Commands on the target VM (specified by a host/port combination or a full JMX Service URL) using
 * the JMX interface. If the target is not the current VM, the JMX Remote interface must be enabled beforehand.
 */
public class JMXExecutor extends CommandExecutor {

    private final MBeanServerConnection mbs;

    /**
     * Instantiates a new JMXExecutor targeting the current VM
     */
    public JMXExecutor() {
        super();
        mbs = ManagementFactory.getPlatformMBeanServer();
    }

    /**
     * Instantiates a new JMXExecutor targeting the VM indicated by the given host/port combination or a full JMX
     * Service URL
     *
     * @param target a host/port combination on the format "host:port" or a full JMX Service URL of the target VM
     */
    public JMXExecutor(String target) {
        String urlStr;

        if (target.matches("^\\w[\\w\\-]*(\\.[\\w\\-]+)*:\\d+$")) {
            /* Matches "hostname:port" */
            urlStr = String.format("service:jmx:rmi:///jndi/rmi://%s/jmxrmi", target);
        } else if (target.startsWith("service:")) {
            urlStr = target;
        } else {
            throw new IllegalArgumentException("Could not recognize target string: " + target);
        }

        try {
            JMXServiceURL url = new JMXServiceURL(urlStr);
            JMXConnector c = JMXConnectorFactory.connect(url, new HashMap<>());
            mbs = c.getMBeanServerConnection();
        } catch (IOException e) {
            throw new CommandExecutorException("Could not initiate connection to target: " + target, e);
        }
    }

    protected OutputAnalyzer executeImpl(String cmd) throws CommandExecutorException {
        String stdout = "";
        String stderr = "";

        String[] cmdParts = cmd.split(" ", 2);
        String operation = commandToMethodName(cmdParts[0]);
        Object[] dcmdArgs = produceArguments(cmdParts);
        String[] signature = {String[].class.getName()};

        ObjectName beanName = getMBeanName();

        try {
            stdout = (String) mbs.invoke(beanName, operation, dcmdArgs, signature);
        }

        /* Failures on the "local" side, the one invoking the command. */
        catch (ReflectionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof NoSuchMethodException) {
                /* We want JMXExecutor to match the behavior of the other CommandExecutors */
                String message = "Unknown diagnostic command: " + operation;
                stderr = exceptionTraceAsString(new IllegalArgumentException(message, e));
            } else {
                rethrowExecutorException(operation, dcmdArgs, e);
            }
        }

        /* Failures on the "local" side, the one invoking the command. */
        catch (InstanceNotFoundException | IOException e) {
            rethrowExecutorException(operation, dcmdArgs, e);
        }

        /* Failures on the remote side, the one executing the invoked command. */
        catch (MBeanException e) {
            stdout = exceptionTraceAsString(e);
        }

        return new OutputAnalyzer(stdout, stderr);
    }

    private void rethrowExecutorException(String operation, Object[] dcmdArgs,
                                          Exception e) throws CommandExecutorException {
        String message = String.format("Could not invoke: %s %s", operation,
                String.join(" ", (String[]) dcmdArgs[0]));
        throw new CommandExecutorException(message, e);
    }

    private ObjectName getMBeanName() throws CommandExecutorException {
        String MBeanName = "com.sun.management:type=DiagnosticCommand";

        try {
            return new ObjectName(MBeanName);
        } catch (MalformedObjectNameException e) {
            String message = "MBean not found: " + MBeanName;
            throw new CommandExecutorException(message, e);
        }
    }

    private Object[] produceArguments(String[] cmdParts) {
        Object[] dcmdArgs = {new String[0]}; /* Default: No arguments */

        if (cmdParts.length == 2) {
            dcmdArgs[0] = cmdParts[1].split(" ");
        }
        return dcmdArgs;
    }

    /**
     * Convert from diagnostic command to MBean method name
     *
     * Examples:
     * help            --> help
     * VM.version      --> vmVersion
     * VM.command_line --> vmCommandLine
     */
    private static String commandToMethodName(String cmd) {
        String operation = "";
        boolean up = false; /* First letter is to be lower case */

        /*
         * If a '.' or '_' is encountered it is not copied,
         * instead the next character will be converted to upper case
         */
        for (char c : cmd.toCharArray()) {
            if (('.' == c) || ('_' == c)) {
                up = true;
            } else if (up) {
                operation = operation.concat(Character.toString(c).toUpperCase());
                up = false;
            } else {
                operation = operation.concat(Character.toString(c).toLowerCase());
            }
        }

        return operation;
    }

    private static String exceptionTraceAsString(Throwable cause) {
        StringWriter sw = new StringWriter();
        cause.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}
