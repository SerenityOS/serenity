/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4401223
 * @summary Make sure that GregorianCalendar doesn't cause IllegalArgumentException at some special situations which are related to the Leap Year.
 * @library /java/text/testlib
 */

import java.util.Date;
import java.util.GregorianCalendar;

import static java.util.GregorianCalendar.*;

public class bug4401223 extends IntlTest {

    public void Test4401223a() {
        int status = 0;
        String s = null;

        try {
            @SuppressWarnings("deprecation")
            Date date = new Date(2000 - 1900, FEBRUARY, 29);
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(date);
            gc.setLenient(false);
            gc.set(YEAR, 2001);
            s = "02/29/00 & set(YEAR,2001) = " + gc.getTime().toString();
        } catch (Exception ex) {
            status++;
            s = "Exception occurred for 2/29/00 & set(YEAR,2001): " + ex;
        }
        if (status > 0) {
            errln(s);
        } else {
            logln(s);
        }
    }

    public void Test4401223b() {
        int status = 0;
        String s = null;

        try {
            @SuppressWarnings("deprecation")
            Date date = new Date(2000 - 1900, DECEMBER, 31);
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(date);
            gc.setLenient(false);
            gc.set(YEAR, 2001);

            if (gc.get(YEAR) != 2001
                    || gc.get(MONTH) != DECEMBER
                    || gc.get(DATE) != 31
                    || gc.get(DAY_OF_YEAR) != 365) {
                status++;
                s = "Wrong Date : 12/31/00 & set(YEAR,2001) ---> " + gc.getTime().toString();
            } else {
                s = "12/31/00 & set(YEAR,2001) = " + gc.getTime().toString();
            }
        } catch (Exception ex) {
            status++;
            s = "Exception occurred for 12/31/00 & set(YEAR,2001) : " + ex;
        }
        if (status > 0) {
            errln(s);
        } else {
            logln(s);
        }
    }

    public static void main(String[] args) throws Exception {
        new bug4401223().run(args);
    }
}
