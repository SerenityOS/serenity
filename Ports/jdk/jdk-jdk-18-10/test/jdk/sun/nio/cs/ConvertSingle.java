/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4094987
   @summary Verify that malformed expression exceptions are thrown
       but no internal errors in certain pathologial cases.

 */


import java.io.*;
import java.nio.charset.*;

public class ConvertSingle {

    public static void main(String args[]) throws Exception {
        // This conversion is pathologically bad - it is attempting to
        // read unicode from an ascii encoded string.
        // The orignal bug: A internal error in ISR results if the
        // byte counter in ByteToCharUnicode
        // is not advanced as the input is consumed.

        try{
            String s = "\n";
            byte ss[] = null;
            String sstring = "x";
            ss = s.getBytes();
            ByteArrayInputStream BAIS = new ByteArrayInputStream(ss);
            InputStreamReader ISR = new InputStreamReader(BAIS, "Unicode");
            BufferedReader BR = new BufferedReader(ISR);
            sstring = BR.readLine();
            BR.close();
            System.out.println(sstring);
        } catch (MalformedInputException e){
            // Right error
            return;
        } catch (java.lang.InternalError e) {
            throw new Exception("ByteToCharUnicode is failing incorrectly for "
                                + " single byte input");
        }

    }

}
