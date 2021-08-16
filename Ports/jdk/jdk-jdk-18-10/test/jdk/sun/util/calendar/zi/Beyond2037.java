/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8073446 8262110
 * @summary Tests DST related beyond the year 2037
 * @run testng Beyond2037
 */

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

@Test
public class Beyond2037 {

    @DataProvider
    Object[][] dstTransition() {
        return new Object[][] {
            {"2037/03/08 01:59:59:999", "2037/03/08 01:59:59:999"},
            {"2037/03/08 02:00:00:000", "2037/03/08 03:00:00:000"},
            {"2038/03/14 01:59:59:999", "2038/03/14 01:59:59:999"},
            {"2038/03/14 02:00:00:000", "2038/03/14 03:00:00:000"},
            {"2099/03/08 01:59:59:999", "2099/03/08 01:59:59:999"},
            {"2099/03/08 02:00:00:000", "2099/03/08 03:00:00:000"},
            {"2100/03/14 01:59:59:999", "2100/03/14 01:59:59:999"},
            {"2100/03/14 02:00:00:000", "2100/03/14 03:00:00:000"},
            {"8000/03/12 01:59:59:999", "8000/03/12 01:59:59:999"},
            {"8000/03/12 02:00:00:000", "8000/03/12 03:00:00:000"},
        };
    }

    @Test(dataProvider="dstTransition")
    public void testDstTransition(String source, String expected) throws Exception {
        var timeZone = TimeZone.getTimeZone("America/New_York");
        var sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS" );
        sdf.setTimeZone(timeZone);
        assertEquals(sdf.format(sdf.parse(source)), expected);
    }

    @Test
    public void testGetOffset() throws Exception {
        var timeZone = TimeZone.getTimeZone("PST8PDT");
        var df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(timeZone);
        var tMilli = df.parse("7681-03-09 03:20:49").getTime();
        assertEquals(timeZone.getOffset(tMilli), -25200000);
    }
}
