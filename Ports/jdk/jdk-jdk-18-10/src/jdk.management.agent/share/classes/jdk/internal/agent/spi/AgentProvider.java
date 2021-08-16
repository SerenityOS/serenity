/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.agent.spi;

import java.util.Properties;

/**
 * Service interface for management agent
 */
public abstract class AgentProvider {

    /**
     * Instantiates a new AgentProvider.
     *
     * @throws SecurityException if the subclass (and calling code) does not
     * have
     * {@code RuntimePermission("sun.management.spi.AgentProvider.subclass")}
     */
    protected AgentProvider() {
        this(checkSubclassPermission());
    }

    private AgentProvider(Void unused) {
    }

    private static Void checkSubclassPermission() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission(AgentProvider.class.getName() + ".subclass"));
        }
        return null;
    }

    /**
     * Gets the name of the agent provider.
     *
     * @return name of agent provider
     */
    public abstract String getName();

    /**
     * Initializes and starts the agent.
     *
     * @throws IllegalStateException if this agent has already been started.
     */
    public abstract void startAgent();

    /**
     * Initializes and starts the agent at given port and with given properties
     *
     * @param props environment variables for agent
     *
     * @throws IllegalStateException if this agent has already been started.
     */
    public abstract void startAgent(Properties props);

    /**
     * Checks if agent is started and not terminated.
     *
     * @return true if agent is running, false otherwise.
     */
    public abstract boolean isActive();

    /**
     * Stops this agent.
     *
     * @throws IllegalStateException if this agent is not started.
     */
    public abstract void stopAgent();
}
