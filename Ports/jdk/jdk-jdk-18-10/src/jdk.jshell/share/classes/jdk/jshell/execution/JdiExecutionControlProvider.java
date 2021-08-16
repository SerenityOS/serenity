/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jshell.execution;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

/**
 * A provider of remote JDI-controlled execution engines.
 *
 * @author Robert Field
 * @since 9
 */
public class JdiExecutionControlProvider implements ExecutionControlProvider {

    /**
     * The remote agent to launch.
     */
    public static final String PARAM_REMOTE_AGENT = "remoteAgent";

    /**
     * Milliseconds before connect timeout.
     */
    public static final String PARAM_TIMEOUT = "timeout";

    /**
     * The local hostname to connect to.
     */
    public static final String PARAM_HOST_NAME = "hostname";

    /**
     * Should JDI-controlled launching be used?
     */
    public static final String PARAM_LAUNCH = "launch";

    /**
     * Default time-out expressed in milliseconds.
     */
    private static final int DEFAULT_TIMEOUT = 5000;

    /**
     * Create an instance.  An instance can be used to
     * {@linkplain  #generate generate} an {@link ExecutionControl} instance
     * that uses the Java Debug Interface as part of the control of a remote
     * process.
     */
    public JdiExecutionControlProvider() {
    }

    /**
     * The unique name of this {@code ExecutionControlProvider}.
     *
     * @return "jdi"
     */
    @Override
    public String name() {
        return "jdi";
    }

    /**
     * Create and return the default parameter map for this
     * {@code ExecutionControlProvider}. The map can optionally be modified;
     * Modified or unmodified it can be passed to
     * {@link #generate(jdk.jshell.spi.ExecutionEnv, java.util.Map) }.
     * <table class="striped">
     * <caption>Parameters</caption>
     *   <thead>
     *   <tr>
     *     <th scope="col">Parameter</th>
     *     <th scope="col">Description</th>
     *     <th scope="col">Constant Field</th>
     *   </tr>
     *   </thead>
     *   <tbody>
     *   <tr>
     *     <th scope="row">remoteAgent</th>
     *     <td>the remote agent to launch</td>
     *     <td>{@link #PARAM_REMOTE_AGENT}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">timeout</th>
     *     <td>milliseconds before connect timeout</td>
     *     <td>{@link #PARAM_TIMEOUT}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">launch</th>
     *     <td>"true" for JDI controlled launch</td>
     *     <td>{@link #PARAM_LAUNCH}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">hostname</th>
     *     <td>connect to the named of the local host ("" for discovered)</td>
     *     <td>{@link #PARAM_HOST_NAME}</td>
     *   </tr>
     *   </tbody>
     * </table>
     *
     * @return the default parameter map
     */
    @Override
    public Map<String, String> defaultParameters() {
        Map<String, String> dp = new HashMap<>();
        dp.put(PARAM_REMOTE_AGENT, RemoteExecutionControl.class.getName());
        dp.put(PARAM_TIMEOUT, "" + DEFAULT_TIMEOUT);
        dp.put(PARAM_HOST_NAME, "");
        dp.put(PARAM_LAUNCH, "false");
        return dp;
    }

    @Override
    public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters)
            throws IOException {
        Map<String, String> dp  = defaultParameters();
        if (parameters == null) {
            parameters = dp;
        }
        String remoteAgent = parameters.getOrDefault(PARAM_REMOTE_AGENT, dp.get(PARAM_REMOTE_AGENT));
        int timeout = Integer.parseUnsignedInt(
                parameters.getOrDefault(PARAM_TIMEOUT, dp.get(PARAM_TIMEOUT)));
        String host = parameters.getOrDefault(PARAM_HOST_NAME, dp.get(PARAM_HOST_NAME));
        String sIsLaunch = parameters.getOrDefault(PARAM_LAUNCH, dp.get(PARAM_LAUNCH)).toLowerCase(Locale.ROOT);
        boolean isLaunch = sIsLaunch.length() > 0
                && ("true".startsWith(sIsLaunch) || "yes".startsWith(sIsLaunch));
        return JdiDefaultExecutionControl.create(env, remoteAgent, isLaunch, host, timeout);
    }

}
