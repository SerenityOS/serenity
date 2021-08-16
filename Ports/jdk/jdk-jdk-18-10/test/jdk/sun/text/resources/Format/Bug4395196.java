/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4395196 4930708 4900884 4890240 8008577
 * @modules jdk.localedata
 * @summary verify the ko DateFormat
 * @run main/othervm -Djava.locale.providers=JRE,SPI Bug4395196
 */

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Bug4395196
{
        public static void main(String[] arg)
        {
                int result = 0;
                Locale loc = new Locale("ko","KR");
                Date now = new Date(108, Calendar.APRIL, 9);

                DateFormat df =
                   DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT,loc);
                SimpleDateFormat sdf = new SimpleDateFormat("",loc);
                sdf.applyPattern("yyyy. M. d a h:mm");
                if( !sdf.format(now).equals(df.format(now))){
                 result++;
                 System.out.println("error at " + sdf.format(now));
                 }
                df =  DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM,loc);
                sdf.applyPattern("yyyy'\ub144' M'\uc6d4' d'\uc77c' '('EE')' a h:mm:ss");
                if( !sdf.format(now).equals(df.format(now))){
                 result++;
                 System.out.println("error at " + sdf.format(now));
                 }
                df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG,loc);
                sdf.applyPattern("yyyy. M. d a h'\uc2dc' mm'\ubd84' ss'\ucd08'");
                if( !sdf.format(now).equals(df.format(now))){
                 result++;
                 System.out.println("error at " + sdf.format(now));
                 }
                df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.FULL,loc);
                sdf.applyPattern("yy. M. d a h'\uc2dc' mm'\ubd84' ss'\ucd08' z");
                if( !sdf.format(now).equals(df.format(now))){
                 result++;
                 System.out.println("error at " + sdf.format(now));
                 }

           if(result > 0) throw new RuntimeException();
}}
