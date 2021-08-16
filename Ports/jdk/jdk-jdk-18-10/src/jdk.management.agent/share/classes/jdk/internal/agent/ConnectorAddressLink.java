/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.agent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.internal.perf.Perf;
import sun.management.counter.Units;
import sun.management.counter.Counter;
import sun.management.counter.perf.PerfInstrumentation;

/**
 * A utility class to support the exporting and importing of the address
 * of a connector server using the instrumentation buffer.
 *
 * @since 1.5
 */
public class ConnectorAddressLink {
    /**
     * A simple wrapper for the perf-counter backing {@linkplain ByteBuffer}
     */
    private static final class PerfHandle {
        private ByteBuffer bb;

        private PerfHandle(ByteBuffer bb) {
            this.bb = bb.order(ByteOrder.nativeOrder());
        }

        private void putLong(long l) {
            this.bb = bb.clear();
            this.bb.asLongBuffer().put(l);
        }
    }

    private static final String CONNECTOR_ADDRESS_COUNTER =
            "sun.management.JMXConnectorServer.address";
    private static final String REMOTE_CONNECTOR_STATE_COUNTER =
            "sun.management.JMXConnectorServer.remote.enabled";

    /*
     * The format of the jvmstat counters representing the properties of
     * a given out-of-the-box JMX remote connector will be as follows:
     *
     * sun.management.JMXConnectorServer.<counter>.<key>=<value>
     *
     * where:
     *
     *     counter = index computed by this class which uniquely identifies
     *               an out-of-the-box JMX remote connector running in this
     *               Java virtual machine.
     *     key/value = a given key/value pair in the map supplied to the
     *                 exportRemote() method.
     *
     * For example,
     *
     * sun.management.JMXConnectorServer.0.remoteAddress=service:jmx:rmi:///jndi/rmi://myhost:5000/jmxrmi
     * sun.management.JMXConnectorServer.0.authenticate=false
     * sun.management.JMXConnectorServer.0.ssl=false
     * sun.management.JMXConnectorServer.0.sslRegistry=false
     * sun.management.JMXConnectorServer.0.sslNeedClientAuth=false
     */
    private static final String REMOTE_CONNECTOR_COUNTER_PREFIX =
            "sun.management.JMXConnectorServer.";

    /*
     * JMX remote connector counter (it will be incremented every
     * time a new out-of-the-box JMX remote connector is created).
     */
    private static final AtomicInteger counter = new AtomicInteger();

    private static PerfHandle remotePerfHandle = null;

    /**
     * Exports the specified connector address to the instrumentation buffer
     * so that it can be read by this or other Java virtual machines running
     * on the same system.
     *
     * @param address The connector address.
     */
    public static void export(String address) {
        if (address == null || address.length() == 0) {
            throw new IllegalArgumentException("address not specified");
        }
        Perf perf = Perf.getPerf();
        perf.createString(
            CONNECTOR_ADDRESS_COUNTER, 1, Units.STRING.intValue(), address);
    }

    public static void unexportRemote() {
        unexport(remotePerfHandle);
    }

    private static void unexport(PerfHandle ph) {
        if (ph != null) {
            ph.putLong(-1L);
        }
    }

    /**
     * Imports the connector address from the instrument buffer
     * of the specified Java virtual machine.
     *
     * @param vmid an identifier that uniquely identifies a local Java virtual
     * machine, or <code>0</code> to indicate the current Java virtual machine.
     *
     * @return the value of the connector address, or <code>null</code> if the
     * target VM has not exported a connector address.
     *
     * @throws IOException An I/O error occurred while trying to acquire the
     * instrumentation buffer.
     */
    public static String importFrom(int vmid) throws IOException {
        Perf perf = Perf.getPerf();
        ByteBuffer bb;
        try {
            bb = perf.attach(vmid, "r");
        } catch (IllegalArgumentException iae) {
            throw new IOException(iae.getMessage());
        }
        List<Counter> counters =
                new PerfInstrumentation(bb).findByPattern(CONNECTOR_ADDRESS_COUNTER);
        Iterator<Counter> i = counters.iterator();
        if (i.hasNext()) {
            Counter c = i.next();
            return (String) c.getValue();
        } else {
            return null;
        }
    }

    /**
     * Exports the specified remote connector address and associated
     * configuration properties to the instrumentation buffer so that
     * it can be read by this or other Java virtual machines running
     * on the same system.
     *
     * @param properties The remote connector address properties.
     */
    public static void exportRemote(Map<String, String> properties) {
        final int index = counter.getAndIncrement();
        Perf perf = Perf.getPerf();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            perf.createString(REMOTE_CONNECTOR_COUNTER_PREFIX + index + "." +
                    entry.getKey(), 1, Units.STRING.intValue(), entry.getValue());
        }
        if (remotePerfHandle != null) {
            remotePerfHandle.putLong(index);
        } else {
            remotePerfHandle = new PerfHandle(
                perf.createLong(REMOTE_CONNECTOR_STATE_COUNTER, 1, Units.NONE.intValue(), (long)index)
            );
        }
    }

    /**
     * Imports the remote connector address and associated
     * configuration properties from the instrument buffer
     * of the specified Java virtual machine.
     *
     * @param vmid an identifier that uniquely identifies a local Java virtual
     * machine, or <code>0</code> to indicate the current Java virtual machine.
     *
     * @return a map containing the remote connector's properties, or an empty
     * map if the target VM has not exported the remote connector's properties.
     *
     * @throws IOException An I/O error occurred while trying to acquire the
     * instrumentation buffer.
     */
    public static Map<String, String> importRemoteFrom(int vmid) throws IOException {
        Perf perf = Perf.getPerf();
        ByteBuffer bb;
        try {
            bb = perf.attach(vmid, "r");
        } catch (IllegalArgumentException iae) {
            throw new IOException(iae.getMessage());
        }
        List<Counter> counters = new PerfInstrumentation(bb).getAllCounters();
        Map<String, String> properties = new HashMap<>();
        for (Counter c : counters) {
            String name =  c.getName();
            if (name.startsWith(REMOTE_CONNECTOR_COUNTER_PREFIX) &&
                    !name.equals(CONNECTOR_ADDRESS_COUNTER)) {
                properties.put(name, c.getValue().toString());
            }
        }
        return properties;
    }
}
