/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Copyright (c) 2008-2012, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tck.java.time.serial;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tck.java.time.AbstractTCKTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamConstants;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Test serialization of ZoneId.
 */
@Test
public class TCKZoneIdSerialization extends AbstractTCKTest {

    //-----------------------------------------------------------------------
    @Test
    public void test_serialization() throws Exception {
        assertSerializable(ZoneId.of("Europe/London"));
        assertSerializable(ZoneId.of("America/Chicago"));
    }

    @Test
    public void test_serialization_format() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos) ) {
            dos.writeByte(7);
            dos.writeUTF("Europe/London");
        }
        byte[] bytes = baos.toByteArray();
        assertSerializedBySer(ZoneId.of("Europe/London"), bytes);
    }

    @Test
    public void test_deserialization_lenient_characters() throws Exception {
        // an ID can be loaded without validation during deserialization
        String id = "QWERTYUIOPASDFGHJKLZXCVBNM~/._+-";
        ZoneId deser = deserialize(id);
        // getId, equals, hashCode, toString and normalized are OK
        assertEquals(deser.getId(), id);
        assertEquals(deser.toString(), id);
        assertEquals(deser, deser);
        assertEquals(deser.hashCode(), deser.hashCode());
        assertEquals(deser.normalized(), deser);
        // getting the rules is not
        try {
            deser.getRules();
            fail();
        } catch (ZoneRulesException ex) {
            // expected
        }
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_deserialization_lenient_badCharacters() throws Exception {
        // an ID can be loaded without validation during deserialization
        // but there is a check to ensure the ID format is valid
        deserialize("|!?");
    }

    @Test(dataProvider="offsetBasedValid")
    public void test_deserialization_lenient_offsetNotAllowed_noPrefix(String input, String resolvedId) throws Exception {
        ZoneId deserialized = deserialize(input);
        assertEquals(deserialized, ZoneId.of(input));
        assertEquals(deserialized, ZoneId.of(resolvedId));
    }

    @Test(dataProvider="offsetBasedValidPrefix")
    public void test_deserialization_lenient_offsetNotAllowed_prefixUTC(String input, String resolvedId, String offsetId) throws Exception {
        ZoneId deserialized = deserialize("UTC" + input);
        assertEquals(deserialized, ZoneId.of("UTC" + input));
        assertEquals(deserialized, ZoneId.of("UTC" + resolvedId));
    }

    @Test(dataProvider="offsetBasedValidPrefix")
    public void test_deserialization_lenient_offsetNotAllowed_prefixGMT(String input, String resolvedId, String offsetId) throws Exception {
        ZoneId deserialized = deserialize("GMT" + input);
        assertEquals(deserialized, ZoneId.of("GMT" + input));
        assertEquals(deserialized, ZoneId.of("GMT" + resolvedId));
    }

    @Test(dataProvider="offsetBasedValidPrefix")
    public void test_deserialization_lenient_offsetNotAllowed_prefixUT(String input, String resolvedId, String offsetId) throws Exception {
        ZoneId deserialized = deserialize("UT" + input);
        assertEquals(deserialized, ZoneId.of("UT" + input));
        assertEquals(deserialized, ZoneId.of("UT" + resolvedId));
    }

    private ZoneId deserialize(String id) throws Exception {
        String serClass = ZoneId.class.getPackage().getName() + ".Ser";
        long serVer = getSUID(ZoneId.class);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeShort(ObjectStreamConstants.STREAM_MAGIC);
            dos.writeShort(ObjectStreamConstants.STREAM_VERSION);
            dos.writeByte(ObjectStreamConstants.TC_OBJECT);
            dos.writeByte(ObjectStreamConstants.TC_CLASSDESC);
            dos.writeUTF(serClass);
            dos.writeLong(serVer);
            dos.writeByte(ObjectStreamConstants.SC_EXTERNALIZABLE | ObjectStreamConstants.SC_BLOCK_DATA);
            dos.writeShort(0);  // number of fields
            dos.writeByte(ObjectStreamConstants.TC_ENDBLOCKDATA);  // end of classdesc
            dos.writeByte(ObjectStreamConstants.TC_NULL);  // no superclasses
            dos.writeByte(ObjectStreamConstants.TC_BLOCKDATA);
            dos.writeByte(1 + 2 + id.length());  // length of data (1 byte + 2 bytes UTF length + 32 bytes UTF)
            dos.writeByte(7);  // ZoneId
            dos.writeUTF(id);
            dos.writeByte(ObjectStreamConstants.TC_ENDBLOCKDATA);  // end of blockdata
        }
        ZoneId deser = null;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            deser = (ZoneId) ois.readObject();
        }
        return deser;
    }

    //-----------------------------------------------------------------------
    // regular factory and .normalized()
    //-----------------------------------------------------------------------
    @DataProvider(name="offsetBasedValid")
    Object[][] data_offsetBasedValid() {
        return new Object[][] {
                {"Z", "Z"},
                {"+0", "Z"},
                {"-0", "Z"},
                {"+00", "Z"},
                {"+0000", "Z"},
                {"+00:00", "Z"},
                {"+000000", "Z"},
                {"+00:00:00", "Z"},
                {"-00", "Z"},
                {"-0000", "Z"},
                {"-00:00", "Z"},
                {"-000000", "Z"},
                {"-00:00:00", "Z"},
                {"+5", "+05:00"},
                {"+01", "+01:00"},
                {"+0100", "+01:00"},
                {"+01:00", "+01:00"},
                {"+010000", "+01:00"},
                {"+01:00:00", "+01:00"},
                {"+12", "+12:00"},
                {"+1234", "+12:34"},
                {"+12:34", "+12:34"},
                {"+123456", "+12:34:56"},
                {"+12:34:56", "+12:34:56"},
                {"-02", "-02:00"},
                {"-5", "-05:00"},
                {"-0200", "-02:00"},
                {"-02:00", "-02:00"},
                {"-020000", "-02:00"},
                {"-02:00:00", "-02:00"},
        };
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="offsetBasedValidPrefix")
    Object[][] data_offsetBasedValidPrefix() {
        return new Object[][] {
                {"", "", "Z"},
                {"+0", "", "Z"},
                {"-0", "", "Z"},
                {"+00", "", "Z"},
                {"+0000", "", "Z"},
                {"+00:00", "", "Z"},
                {"+000000", "", "Z"},
                {"+00:00:00", "", "Z"},
                {"-00", "", "Z"},
                {"-0000", "", "Z"},
                {"-00:00", "", "Z"},
                {"-000000", "", "Z"},
                {"-00:00:00", "", "Z"},
                {"+5", "+05:00", "+05:00"},
                {"+01", "+01:00", "+01:00"},
                {"+0100", "+01:00", "+01:00"},
                {"+01:00", "+01:00", "+01:00"},
                {"+010000", "+01:00", "+01:00"},
                {"+01:00:00", "+01:00", "+01:00"},
                {"+12", "+12:00", "+12:00"},
                {"+1234", "+12:34", "+12:34"},
                {"+12:34", "+12:34", "+12:34"},
                {"+123456", "+12:34:56", "+12:34:56"},
                {"+12:34:56", "+12:34:56", "+12:34:56"},
                {"-02", "-02:00", "-02:00"},
                {"-5", "-05:00", "-05:00"},
                {"-0200", "-02:00", "-02:00"},
                {"-02:00", "-02:00", "-02:00"},
                {"-020000", "-02:00", "-02:00"},
                {"-02:00:00", "-02:00", "-02:00"},
        };
    }



}
