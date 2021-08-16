/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8206120
 * @summary Test whether lenient era is accepted in JapaneseImperialCalendar
 * @run testng/othervm JapaneseLenientEraTest
 */

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

@Test
public class JapaneseLenientEraTest {

    @DataProvider(name="lenientEra")
    Object[][] names() {
        return new Object[][] {
            // lenient era/year, strict era/year
            { "Meiji 123", "Heisei 2" },
            { "Sh\u014dwa 65", "Heisei 2" },
            { "Heisei 32", "Reiwa 2" },
        };
    }

    @Test(dataProvider="lenientEra")
    public void testLenientEra(String lenient, String strict) throws Exception {
        Calendar c = new Calendar.Builder()
            .setCalendarType("japanese")
            .build();
        DateFormat df = new SimpleDateFormat("GGGG y-M-d", Locale.ROOT);
        df.setCalendar(c);
        Date lenDate = df.parse(lenient + "-01-01");
        df.setLenient(false);
        Date strDate = df.parse(strict + "-01-01");
        assertEquals(lenDate, strDate);
    }
}
