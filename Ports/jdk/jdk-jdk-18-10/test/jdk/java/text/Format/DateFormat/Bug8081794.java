/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8081794
 * @summary ParsePosition getErrorIndex should return correct index
 */
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Bug8081794 {

    public static void main(String[] args) {
        String date = "13 Jan 2005 21:45:34 ABC";
        String format = "dd MMM yyyy HH:mm:ss z";
        ParsePosition pp = new ParsePosition(0);
        pp.setIndex(0);
        SimpleDateFormat sd = new SimpleDateFormat(format, Locale.ENGLISH);
        Date d = sd.parse(date, pp);
        int errorIndex = pp.getErrorIndex();
        if (errorIndex == 21) {
            System.out.println(": passed");
        } else {
            System.out.println(": failed");
            throw new RuntimeException("Failed with wrong index: " + errorIndex);
        }
    }
}
