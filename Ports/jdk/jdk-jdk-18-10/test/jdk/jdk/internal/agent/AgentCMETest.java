/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright (c) 2012 IBM Corporation
 */

/**
 * @test
 * @bug 7164191
 * @summary properties.putAll API may fail with ConcurrentModifcationException on multi-thread scenario
 * @modules jdk.management.agent/jdk.internal.agent
 * @author Deven You
 */

import java.util.Properties;
import jdk.internal.agent.Agent;

public class AgentCMETest {
    static Class<?> agentClass;

    /**
     * In jdk.internal.agent.Agent.loadManagementProperties(), call
     * properties.putAll API may fail with ConcurrentModifcationException if the
     * system properties are modified simultaneously by another thread
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Start...");

        final Properties properties = System.getProperties();
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 100; i++) {
                    properties.put(String.valueOf(i), "");
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                }
            }
        });
        t1.start();

        for (int i = 0; i < 10000; i++) {
            Agent.loadManagementProperties();
        }

        System.out.println("Finished...");
    }
}
