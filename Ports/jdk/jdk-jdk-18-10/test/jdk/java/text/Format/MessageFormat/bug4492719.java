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
 *
 * @bug 4492719
 * @library /java/text/testlib
 * @summary Confirm that Message.parse() interprets time zone which uses "GMT+/-" format correctly and doesn't throw ParseException.
 */

import java.util.*;
import java.text.*;

public class bug4492719 extends IntlTest {

    public static void main(String[] args) throws Exception {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedTimeZone = TimeZone.getDefault();
        MessageFormat mf;
        boolean err =false;

        String[] formats = {
            "short", "medium", "long", "full"
        };
        String[] timezones = {
            "America/Los_Angeles", "GMT", "GMT+09:00", "GMT-8:00",
            "GMT+123", "GMT-1234", "GMT+2", "GMT-13"
        };
        String text;

        Locale.setDefault(Locale.US);

        try {
            for (int i = 0; i < timezones.length; i++) {
                TimeZone.setDefault(TimeZone.getTimeZone(timezones[i]));

                for (int j = 0; j < formats.length; j++) {
                    mf = new MessageFormat("{0,time," + formats[j] + "} - time");
                    text = MessageFormat.format("{0,time," + formats[j] + "} - time",
                                      new Object [] { new Date(123456789012L)});
                    Object[] objs = mf.parse(text);
                }
            }
        } catch (ParseException e) {
            err = true;
            System.err.println("Invalid ParseException occurred : " +
                               e.getMessage());
            System.err.println("    TimeZone=" + TimeZone.getDefault());
        }
        finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedTimeZone);
            if (err) {
                throw new Exception("MessageFormat.parse(\"GMT format\") failed.");
            }
        }
    }
}
