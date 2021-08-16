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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Copyright (c) 2009-2012, Stephen Colebourne & Michael Nascimento Santos
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
package tck.java.time.temporal.serial;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.temporal.ValueRange;
import java.util.Arrays;

import org.testng.annotations.Test;

import tck.java.time.AbstractTCKTest;

/**
 * Test serialization of ValueRange.
 */
@Test
public class TCKValueRangeSerialization extends AbstractTCKTest {

    //-----------------------------------------------------------------------
    // Serialization
    //-----------------------------------------------------------------------
    public void test_serialization() throws Exception {
        ValueRange range = ValueRange.of(1, 2, 3, 4);
        assertSerializable(range);
    }


    /**
     * Verify Serialized bytes of a ValueRange.
     * @throws IOException if thrown during serialization is an unexpected test tailure
     */
    public void test_valueRangeSerialized() throws IOException {
        byte[] expected = {
            (byte)172, (byte)237,   0,   5, 115, 114,   0,  29, 106,  97, /* \u00ac \u00ed \u0000 \u0005 s r \u0000 \u001d j a */
            118,  97,  46, 116, 105, 109, 101,  46, 116, 101, /* v a . t i m e . t e */
            109, 112, 111, 114,  97, 108,  46,  86,  97, 108, /* m p o r a l . V a l */
            117, 101,  82,  97, 110, 103, 101, (byte)154, 113, (byte)169, /* u e R a n g e \u009a q \u00a9 */
             86, (byte)242, (byte)205,  90, (byte)184,   2,   0,   4,  74,   0, /* V \u00f2 \u00cd Z \u00b8 \u0002 \u0000 \u0004 J \u0000 */
             10, 109,  97, 120,  76,  97, 114, 103, 101, 115, /*  m a x L a r g e s */
            116,  74,   0,  11, 109,  97, 120,  83, 109,  97, /* t J \u0000 \u000b m a x S m a */
            108, 108, 101, 115, 116,  74,   0,  10, 109, 105,/* l l e s t J \u0000 m i */
            110,  76,  97, 114, 103, 101, 115, 116,  74,   0, /* n L a r g e s t J \u0000 */
             11, 109, 105, 110,  83, 109,  97, 108, 108, 101, /* \u000b m i n S m a l l e */
            115, 116, 120, 112,   0,   0,   0,   0,   0,   0, /* s t x p \u0000 \u0000 \u0000 \u0000 \u0000 \u0000 */
              0,  40,   0,   0,   0,   0,   0,   0,   0,  30, /* \u0000 ( \u0000 \u0000 \u0000 \u0000 \u0000 \u0000 \u0000 \u001e */
              0,   0,   0,   0,   0,   0,   0,  20,   0,   0, /* \u0000 \u0000 \u0000 \u0000 \u0000 \u0000 \u0000 \u0014 \u0000 \u0000 */
              0,   0,   0,   0,   0,  10,                     /* \u0000 \u0000 \u0000 \u0000 \u0000 */
        };

        ValueRange range = ValueRange.of(10, 20, 30, 40);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos) ) {
            oos.writeObject(range);

            byte[] actual = baos.toByteArray();
            assertEquals(actual, expected, "Serialized bytes incorrect");
        }
    }

    @Test
    public void test_invalid_serialform() throws Exception {
        byte[] template = {
            (byte)172, (byte)237,   0,   5, 115, 114,   0,  29, 106,  97, /* \u00ac \u00ed \u0000 \u0005 s r \u0000 \u001d j a */
            118,  97,  46, 116, 105, 109, 101,  46, 116, 101, /* v a . t i m e . t e */
            109, 112, 111, 114,  97, 108,  46,  86,  97, 108, /* m p o r a l . V a l */
            117, 101,  82,  97, 110, 103, 101, (byte)154, 113, (byte)169, /* u e R a n g e \u009a q \u00a9 */
             86, (byte)242, (byte)205,  90, (byte)184,   2,   0,   4,  74,   0, /* V \u00f2 \u00cd Z \u00b8 \u0002 \u0000 \u0004 J \u0000 */
             10, 109,  97, 120,  76,  97, 114, 103, 101, 115, /*  m a x L a r g e s */
            116,  74,   0,  11, 109,  97, 120,  83, 109,  97, /* t J \u0000 \u000b m a x S m a */
            108, 108, 101, 115, 116,  74,   0,  10, 109, 105,/* l l e s t J \u0000 m i */
            110,  76,  97, 114, 103, 101, 115, 116,  74,   0, /* n L a r g e s t J \u0000 */
             11, 109, 105, 110,  83, 109,  97, 108, 108, 101, /* \u000b m i n S m a l l e */
            115, 116, 120, 112,   0,   0,   0,   0,   0,   0, /* s t x p \u0000 \u0000 \u0000 \u0000 \u0000 \u0000 */
              0,  40,   0,   0,   0,   0,   0,   0,   0,  30, /* \u0000 ( \u0000 \u0000 \u0000 \u0000 \u0000 \u0000 \u0000 \u001e */
              0,   0,   0,   0,   0,   0,   0,  20,   0,   0, /* \u0000 \u0000 \u0000 \u0000 \u0000 \u0000 \u0000 \u0014 \u0000 \u0000 */
              0,   0,   0,   0,   0,  10,                     /* \u0000 \u0000 \u0000 \u0000 \u0000 */
        };

        // minSmallest > minLargest, insert invalid values and deserialize
        byte[] bad1 = {0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 4};
        byte[] val = Arrays.copyOf(template, template.length);
        System.arraycopy(bad1, 0, val, 114, bad1.length);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(val))) {
            in.readObject();
            fail("Invalid minSmallest > minLargest " + ValueRange.class.getName());
        } catch (InvalidObjectException ioe) {
            // Expected exception
        }

        // maxSmallest > maxLargest, insert invalid values and deserialize
        byte[] bad2 = {0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 4, 0, 0, 0, 3};
        val = Arrays.copyOf(template, template.length);
        System.arraycopy(bad1, 0, val, 114, bad2.length);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(val))) {
            in.readObject();
            fail("Invalid maxSmallest > maxLargest " + ValueRange.class.getName());
        } catch (InvalidObjectException ioe) {
            // Expected exception
        }

        // minLagest > maxLargest, insert invalid values and deserialize
        byte[] bad3 = {0, 0, 0, 1, 0, 0, 0, 5, 0, 0, 0, 3, 0, 0, 0, 4};
        val = Arrays.copyOf(template, template.length);
        System.arraycopy(bad1, 0, val, 114, bad3.length);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(val))) {
            in.readObject();
            fail("Invalid minLagest > maxLargest " + ValueRange.class.getName());
        } catch (InvalidObjectException ioe) {
            // Expected exception
        }
    }

}
