/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
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
package tck.java.time.temporal.serial;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tck.java.time.AbstractTCKTest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.DayOfWeek;
import java.time.temporal.WeekFields;
import java.util.Arrays;

/**
 * Test serialization of WeekFields.
 */
@Test
public class TCKWeekFieldsSerialization extends AbstractTCKTest {

    //-----------------------------------------------------------------------
    @Test(dataProvider="weekFields")
    public void test_serializable_singleton(DayOfWeek firstDayOfWeek, int minDays) throws IOException, ClassNotFoundException {
        WeekFields weekDef = WeekFields.of(firstDayOfWeek, minDays);
        assertSerializableSame(weekDef);  // spec state singleton
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="weekFields")
    Object[][] data_weekFields() {
        Object[][] objects = new Object[49][];
        int i = 0;
        for (DayOfWeek firstDayOfWeek : DayOfWeek.values()) {
            for (int minDays = 1; minDays <= 7; minDays++) {
                objects[i++] = new Object[] {firstDayOfWeek, minDays};
            }
        }
        return objects;
    }

    @Test
    public void test_invalid_serialform() throws Exception {
        WeekFields wf = WeekFields.of(DayOfWeek.MONDAY, 7);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(64);
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(wf);
        byte[] template = baos.toByteArray();

        // (minimalDays = 5) {
        byte[] good1 = {0, 0, 0, 5};
        byte[] val = Arrays.copyOf(template, template.length);
        System.arraycopy(good1, 0, val, 105, good1.length);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(val))) {
            Object o = in.readObject();
            assertEquals(o, WeekFields.of(DayOfWeek.MONDAY, 5), "Should be MONDAY, min = 5");
        } catch (Exception ioe) {
            fail("Unexpected exception " + ioe);
        }

        // (minimalDays < 1) {
        byte[] bad1 = {0, 0, 0, 0};
        val = Arrays.copyOf(template, template.length);
        System.arraycopy(bad1, 0, val, 105, bad1.length);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(val))) {
            in.readObject();
            fail("Invalid minimalDays < 1 " + WeekFields.class.getName());
        } catch (Exception ioe) {
            // Expected exception
        }

        // (minimalDays > 7) {
        byte[] bad2 = {0, 0, 0, 8};
        val = Arrays.copyOf(template, template.length);
        System.arraycopy(bad2, 0, val, 105, bad2.length);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(val))) {
            in.readObject();
            fail("Invalid minimalDays > 7 " + WeekFields.class.getName());
        } catch (Exception ioe) {
            // Expected exception
        }

        // (StartDay = null) {
        byte[] bad3 = {0x70};
        val = Arrays.copyOf(template, 110);
        System.arraycopy(bad3, 0, val, 105 + 4, bad3.length);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(val))) {
            in.readObject();
            fail("Invalid startDay == null " + WeekFields.class.getName());
        } catch (Exception ioe) {
            // Expected exception
        }

    }

}
