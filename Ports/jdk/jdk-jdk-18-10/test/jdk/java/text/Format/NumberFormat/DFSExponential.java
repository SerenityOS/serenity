/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4068067
 * @library /java/text/testlib
 * @summary test NumberFormat with exponential separator symbols. It also tests the new
 *          public methods in DecimalFormatSymbols, setExponentSeparator() and
 *          getExponentSeparator()
 */

import java.util.*;
import java.text.*;

public class DFSExponential extends IntlTest
{

    public static void main(String[] args) throws Exception {
        new DFSExponential().run(args);
    }


   public void DFSExponenTest() throws Exception {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        String pat[] = { "0.####E0", "00.000E00", "##0.####E000", "0.###E0;[0.###E0]"  };
        double val[] = { 0.01234, 123456789, 1.23e300, -3.141592653e-271 };
        long lval[] = { 0, -1, 1, 123456789 };
        String valFormat[][] = {
                {"1.234x10^-2", "1.2346x10^8", "1.23x10^300", "-3.1416x10^-271"},
                {"12.340x10^-03", "12.346x10^07", "12.300x10^299", "-31.416x10^-272"},
                {"12.34x10^-003", "123.4568x10^006", "1.23x10^300", "-314.1593x10^-273"},
                {"1.234x10^-2", "1.235x10^8", "1.23x10^300", "[3.142x10^-271]"},
        };


        int ival = 0, ilval = 0;
        logln("Default exponent separator: "+sym.getExponentSeparator());
        try {
            sym.setExponentSeparator("x10^");
        } catch (NullPointerException e){
            errln("null String was passed to set an exponent separator symbol");
            throw new RuntimeException("Test Malfunction: null String was passed to set an exponent separator symbol" );
        }
        logln("Current exponent separator: "+sym.getExponentSeparator());

        for (int p=0; p<pat.length; ++p){
            DecimalFormat fmt = new DecimalFormat(pat[p], sym);
            logln("     Pattern: " + fmt.toPattern());
            String locPattern = fmt.toLocalizedPattern();
            logln("     Localized pattern: "+locPattern);
            //fmt.applyLocalizedPattern(locPattern);
            //System.out.println("      fmt.applyLocalizedPattern(): "+fmt.toLocalizedPattern());

            for (int v=0; v<val.length; ++v) {
                String s = fmt.format(val[v]);
                logln("         " + val[v]+" --> "+s);
                if(valFormat[p][v].equals(s)){
                    logln(": Passed");
                }else{
                   errln(" Failed: Should be formatted as "+valFormat[p][v]+ "but got "+s);
                   throw new RuntimeException(" Failed: Should be formatted as "+valFormat[p][v]+ "but got "+s);
                }
           }
         } //end of the first for loop
   }
}
