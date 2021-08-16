/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4097450
 */

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class bug4097450
{
    public static void main(String args[])
    {
        //
        // Date parse requiring 4 digit year.
        //
        String[]  dstring = {"97","1997",  "97","1997","01","2001",  "01","2001"
                             ,  "1",
                             "1","11",  "11","111", "111"};
        String[]  dformat = {"yy",  "yy","yyyy","yyyy","yy",  "yy","yyyy","yyyy"
                             ,
                             "yy","yyyy","yy","yyyy", "yy","yyyy"};
        boolean[] dresult = {true, false, false,  true,true, false, false,  true
                             ,false,
                             false,true, false,false, false};
        SimpleDateFormat formatter;
        SimpleDateFormat resultFormatter = new SimpleDateFormat("yyyy");

        System.out.println("Format\tSource\tResult");
        System.out.println("-------\t-------\t-------");
        for (int i = 0; i < dstring.length; i++)
        {
            System.out.print(dformat[i] + "\t" + dstring[i] + "\t");
            formatter = new SimpleDateFormat(dformat[i]);
            try {
                System.out.print(resultFormatter.format(formatter.parse(dstring[
                                                                                i])));
                //if ( !dresult[i] ) System.out.print("   <-- error!");
            }
            catch (ParseException exception) {
                //if ( dresult[i] ) System.out.print("   <-- error!");
                System.out.print("exception --> " + exception);
            }
            System.out.println();
        }
    }
}
