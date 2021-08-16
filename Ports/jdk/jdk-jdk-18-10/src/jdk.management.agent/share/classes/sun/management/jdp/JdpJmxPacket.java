/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package sun.management.jdp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A packet to broadcasts JMX URL
 *
 * Fields:
 *
 * <ul>
 * <li>UUID - broadcast session ID, changed every time when we start/stop
 * discovery service</li>
 * <li>JMX_URL - URL to connect to JMX service</li>
 * <li>MAIN_CLASS - optional name of main class, filled from sun.java.command stripped for
 * security reason to first space</li>
 * <li>INSTANCE_NAME - optional custom name of particular instance as provided by customer</li>
 * </ul>
 */
public final class JdpJmxPacket
       extends JdpGenericPacket
       implements JdpPacket {

    /**
     * Session ID
     */
    public final static String UUID_KEY = "DISCOVERABLE_SESSION_UUID";
    /**
     * Name of main class
     */
    public final static String MAIN_CLASS_KEY = "MAIN_CLASS";
    /**
     * JMX service URL
     */
    public final static String JMX_SERVICE_URL_KEY = "JMX_SERVICE_URL";
    /**
     * Name of Java instance
     */
    public final static String INSTANCE_NAME_KEY = "INSTANCE_NAME";
    /**
     * PID of java process, optional presented if it could be obtained
     */
    public final static String PROCESS_ID_KEY = "PROCESS_ID";
    /**
     * Hostname of rmi server, optional presented if user overrides rmi server
     * hostname by java.rmi.server.hostname property
     */
    public final static String RMI_HOSTNAME_KEY = "RMI_HOSTNAME";
    /**
     * Configured broadcast interval, optional
     */
    public final static String BROADCAST_INTERVAL_KEY = "BROADCAST_INTERVAL";

    private UUID id;
    private String mainClass;
    private String jmxServiceUrl;
    private String instanceName;
    private String processId;
    private String rmiHostname;
    private String broadcastInterval;

    /**
     * Create new instance from user provided data. Set mandatory fields
     *
     * @param id - java instance id
     * @param jmxServiceUrl - JMX service url
     */
    public JdpJmxPacket(UUID id, String jmxServiceUrl) {
        this.id = id;
        this.jmxServiceUrl = jmxServiceUrl;
    }

    /**
     * Create new instance from network data Parse packet and set fields.
     *
     * @param data - raw packet data as it came from a Net
     * @throws JdpException
     */
    public JdpJmxPacket(byte[] data)
            throws JdpException {
        JdpPacketReader reader;

        reader = new JdpPacketReader(data);
        Map<String, String> p = reader.getDiscoveryDataAsMap();

        String sId = p.get(UUID_KEY);
        this.id = (sId == null) ? null : UUID.fromString(sId);
        this.jmxServiceUrl = p.get(JMX_SERVICE_URL_KEY);
        this.mainClass = p.get(MAIN_CLASS_KEY);
        this.instanceName = p.get(INSTANCE_NAME_KEY);
        this.processId = p.get(PROCESS_ID_KEY);
        this.rmiHostname = p.get(RMI_HOSTNAME_KEY);
        this.broadcastInterval = p.get(BROADCAST_INTERVAL_KEY);
    }

    /**
     * Set main class field
     *
     * @param mainClass - main class of running app
     */
    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    /**
     * Set instance name field
     *
     * @param instanceName - name of instance as provided by customer
     */
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /**
     * @return id of discovery session
     */
    public UUID getId() {
        return id;
    }

    /**
     *
     * @return main class field
     */
    public String getMainClass() {
        return mainClass;
    }

    /**
     *
     * @return JMX service URL
     */
    public String getJmxServiceUrl() {
        return jmxServiceUrl;
    }

    /**
     *
     * @return instance name
     */
    public String getInstanceName() {
        return instanceName;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getRmiHostname() {
        return rmiHostname;
    }

    public void setRmiHostname(String rmiHostname) {
        this.rmiHostname = rmiHostname;
    }

    public String getBroadcastInterval() {
        return broadcastInterval;
    }

    public void setBroadcastInterval(String broadcastInterval) {
        this.broadcastInterval = broadcastInterval;
    }

    /**
     *
     * @return assembled packet ready to be sent across a Net
     * @throws IOException
     */
    @Override
    public byte[] getPacketData() throws IOException {
        // Assemble packet from fields to byte array
        JdpPacketWriter writer;
        writer = new JdpPacketWriter();
        writer.addEntry(UUID_KEY, (id == null) ? null : id.toString());
        writer.addEntry(MAIN_CLASS_KEY, mainClass);
        writer.addEntry(JMX_SERVICE_URL_KEY, jmxServiceUrl);
        writer.addEntry(INSTANCE_NAME_KEY, instanceName);
        writer.addEntry(PROCESS_ID_KEY, processId);
        writer.addEntry(RMI_HOSTNAME_KEY, rmiHostname);
        writer.addEntry(BROADCAST_INTERVAL_KEY, broadcastInterval);

        return writer.getPacketBytes();
    }

    /**
     *
     * @return packet hash code
     */
    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + id.hashCode();
        hash = hash * 31 + jmxServiceUrl.hashCode();
        return hash;
    }

    /**
     * Compare two packets
     *
     * @param o - packet to compare
     * @return either packet equals or not
     */
    @Override
    public boolean equals(Object o) {

        if (o == null || ! (o instanceof JdpJmxPacket) ){
            return false;
        }

        JdpJmxPacket p = (JdpJmxPacket) o;
        return  Objects.equals(id, p.getId()) && Objects.equals(jmxServiceUrl, p.getJmxServiceUrl());
    }
}
