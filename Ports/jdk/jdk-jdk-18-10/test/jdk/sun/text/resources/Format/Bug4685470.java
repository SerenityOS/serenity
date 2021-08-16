/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 *@test
 *@bug 4685470
 *@summary verify whether contain pattern for the day of week in sch and tch's default FULL pattern
 */

import java.util.*;
import java.text.*;

public class Bug4685470
{
   public static void main(String args[])
   {
        int result = 0;
        Bug4685470 testsuite = new Bug4685470();

        if(!testsuite.TestSCH()) result ++;
        if(!testsuite.TestTCH()) result ++;

        if(result > 0) throw new RuntimeException();
   }

   private boolean TestSCH()
   {
      Date now = new Date();
      DateFormat s = DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.FULL,Locale.SIMPLIFIED_CHINESE);

      return Test(s.format(now), getDayofWeek(now, Locale.SIMPLIFIED_CHINESE), "\"EEEE\" in " + Locale.SIMPLIFIED_CHINESE.toString());
   }

   private boolean TestTCH()
   {
      Date now = new Date();
      DateFormat s = DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.FULL,Locale.TRADITIONAL_CHINESE);

      return Test(s.format(now), getDayofWeek(now, Locale.TRADITIONAL_CHINESE), "\"EEEE\" in " + Locale.TRADITIONAL_CHINESE.toString());
   }

   private boolean Test(String parent, String child, String patterninfo)
   {
      boolean result = true;

      if( ! contains(parent, child)){
        System.out.println("Full date: " + parent);
        System.out.println("Which should contain the day of the week: " + child);
        System.out.println("DateFormat.FULL don't contain pattern for the day of the week : " + patterninfo);

        result = false;
      }

      return result;
   }

   private boolean contains(String parent, String child)
   {
        boolean result = false;

        if(parent.length() < child.length()) result = false;
        else {
                for ( int i = 0; i < parent.length() - child.length(); i++){
                        result = parent.regionMatches(i, child, 0, child.length());
                        if ( result == true) break;
                }
        }

        return result;
   }

   private String getDayofWeek(Date date, Locale loc){
        return (new SimpleDateFormat("EEEE", loc)).format(date);
   }
}
