/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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


import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.testng.annotations.Test;
import sun.management.jdp.JdpJmxPacket;

import static org.testng.Assert.assertEquals;


/**
 * These tests are unit tests used for test development.
 * These are not meant to be by automatically run by JTREG.
 * They exists to support test development and should be run by the test developer.
 * <p/>
 * <p/>
 * The JDP packet format:
 * <p/>
 * packet = header + payload
 * <p/>
 * header  = magic + version
 * magic       = 4 bytes: 0xCOFFEE42
 * version     = 2 bytes: 01 (As of 2013-05-01)
 * <p/>
 * payload = list key/value pairs
 * keySize     = 2 bytes
 * key         = (keySize) bytes
 * valueSize   = 2 bytes
 * value       = (valueSize) bytes
 * <p/>
 * <p/>
 * Two entries are mandatory in the payload:
 * UUID            (JdpJmxPacket.UUID_KEY)
 * JMX service URL (JdpJmxPacket.JMX_SERVICE_URL_KEY)
 * <p/>
 * These two entries are optional:
 * Main Class      (JdpJmxPacket.MAIN_CLASS_KEY)
 * Instance name   (JdpJmxPacket.INSTANCE_NAME_KEY)
 *
 * @author Alex Schenkman
 *         <p/>
 *         Using TestNG framework.
 */
public class PacketTest {

    final int MAGIC = 0xC0FFEE42;
    final UUID id = UUID.randomUUID();
    final String mainClass = "org.happy.Feet";
    final String jmxServiceUrl = "fake://jmxUrl";
    final String instanceName = "Joe";

    private JdpJmxPacket createDefaultPacket() {
        JdpJmxPacket packet = new JdpJmxPacket(id, jmxServiceUrl);
        return packet;
    }

    private JdpJmxPacket createFullPacket() {
        JdpJmxPacket packet = new JdpJmxPacket(id, jmxServiceUrl);
        packet.setMainClass(mainClass);
        packet.setInstanceName("Joe");
        return packet;
    }

    @Test
    public void testMagic() throws IOException {
        byte[] rawData = createFullPacket().getPacketData();
        int magic = JdpTestUtil.decode4ByteInt(rawData, 0);
        assertEquals(MAGIC, magic, "MAGIC does not match!");
    }

    @Test
    public void testVersion() throws IOException {
        byte[] rawData = createFullPacket().getPacketData();
        assertEquals(1, JdpTestUtil.decode2ByteInt(rawData, 4));
    }

    @Test
    public void testAllEntries() throws IOException {
        byte[] rawData = createFullPacket().getPacketData();
        Map<String, String> payload = JdpTestUtil.readPayload(rawData);

        assertEquals(4, payload.size());
        assertEquals(mainClass, payload.get(JdpJmxPacket.MAIN_CLASS_KEY));
        assertEquals(id.toString(), payload.get(JdpJmxPacket.UUID_KEY));
        assertEquals(jmxServiceUrl, payload.get(JdpJmxPacket.JMX_SERVICE_URL_KEY));
        assertEquals(instanceName, payload.get(JdpJmxPacket.INSTANCE_NAME_KEY));
    }

    public void testDefaultEntries() throws IOException {
        byte[] rawData = createDefaultPacket().getPacketData();
        Map<String, String> payload = JdpTestUtil.readPayload(rawData);

        assertEquals(2, payload.size());
        assertEquals(id.toString(), payload.get(JdpJmxPacket.UUID_KEY));
        assertEquals(jmxServiceUrl, payload.get(JdpJmxPacket.JMX_SERVICE_URL_KEY));
    }
}
