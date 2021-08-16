/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
package tck.java.time.chrono;

import static java.time.temporal.ChronoField.ERA;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.time.chrono.Era;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseEra;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for JapaneseEra
 * @bug 8068278
 */
@Test
public class TCKJapaneseEra {

    @DataProvider(name = "JapaneseEras")
    Object[][] data_of_eras() {
        return new Object[][] {
                    {JapaneseEra.REIWA, "Reiwa", 3},
                    {JapaneseEra.HEISEI, "Heisei", 2},
                    {JapaneseEra.SHOWA, "Showa", 1},
                    {JapaneseEra.TAISHO, "Taisho", 0},
                    {JapaneseEra.MEIJI, "Meiji", -1},
        };
    }

    @DataProvider(name = "InvalidJapaneseEras")
    Object[][] data_of_invalid_eras() {
        return new Object[][] {
                {-2},
                {-3},
                {4},
                {Integer.MIN_VALUE},
                {Integer.MAX_VALUE},
        };
    }

    //-----------------------------------------------------------------------
    // JapaneseEra value test
    //-----------------------------------------------------------------------
    @Test(dataProvider="JapaneseEras")
    public void test_valueOf(JapaneseEra era , String eraName, int eraValue) {
        assertEquals(era.getValue(), eraValue);
        assertEquals(JapaneseEra.of(eraValue), era);
        assertEquals(JapaneseEra.valueOf(eraName), era);
    }

    //-----------------------------------------------------------------------
    // values()
    //-----------------------------------------------------------------------
    @Test
    public void test_values() {
        List<Era> eraList = JapaneseChronology.INSTANCE.eras();
        JapaneseEra[] eras = JapaneseEra.values();
        assertEquals(eraList.size(), eras.length);
        for (JapaneseEra era : eras) {
            assertTrue(eraList.contains(era));
        }
    }

    //-----------------------------------------------------------------------
    // range()
    //-----------------------------------------------------------------------
    @Test
    public void test_range() {
        // eras may be added after release
        for (JapaneseEra era : JapaneseEra.values()) {
            assertEquals(era.range(ERA).getMinimum(), -1);
            assertEquals(era.range(ERA).getLargestMinimum(), -1);
            assertEquals(era.range(ERA).getSmallestMaximum(), era.range(ERA).getMaximum());
            assertEquals(era.range(ERA).getMaximum() >= 2, true);
        }
    }

    //-----------------------------------------------------------------------
    // JapaneseChronology.INSTANCE.eraOf invalid era test
    //-----------------------------------------------------------------------
    @Test(dataProvider="InvalidJapaneseEras", expectedExceptions=java.time.DateTimeException.class)
    public void test_outofrange(int era) {
        JapaneseChronology.INSTANCE.eraOf(era);
    }
}
