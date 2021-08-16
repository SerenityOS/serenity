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
 * @bug 4442855
 * @modules jdk.localedata
 * @summary verify the era's translation for tradition chinese
 */

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Bug4442855
{
public static void main(String[] argv){
        int result = 0;
        Bug4442855 testsuite = new Bug4442855();

        if( !testsuite.TestAD()) result ++;
        if( !testsuite.TestBC()) result ++;
        if( result > 0 ) throw new RuntimeException();

}

private boolean TestAD(){
        Locale zhTWloc = new Locale("zh", "TW");
        SimpleDateFormat sdf = new SimpleDateFormat("G", zhTWloc);

        return Test(sdf.format(new Date()), "\u897f\u5143", "AD");
}

private boolean TestBC(){
        Locale zhTWloc = new Locale("zh", "TW");
        SimpleDateFormat sdf = new SimpleDateFormat("G", zhTWloc);

        Calendar cdar = sdf.getCalendar();
        cdar.set(-2000, 1, 1);
        return Test(sdf.format(cdar.getTime()), "\u897f\u5143\u524d", "BC");
}

private boolean Test(String parent, String child, String info){
        boolean retval = true;

        if(!parent.equals(child)){
                System.out.println("Error translation for " + info + " in TCH: " + parent);
                System.out.println("Which should be: " + child );
                retval = false;
        }

        return retval;
}
}
