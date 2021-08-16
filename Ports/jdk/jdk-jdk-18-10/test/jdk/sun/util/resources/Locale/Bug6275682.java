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
 * @summary Verifying that the language names starts with lowercase in spanish
 * @modules jdk.localedata
 * @bug 6275682
*/

import java.util.Locale;

public class Bug6275682 {

   public static void main (String[] args) throws Exception {
        Locale es = new Locale ("es");
        String[] isoLangs = es.getISOLanguages ();
        String error = "";

        for (int i = 0; i < isoLangs.length; i++) {
            Locale current = new Locale (isoLangs[i]);
            String localeString = current.getDisplayLanguage (es);
            String startLetter = localeString.substring (0,1);
            if (!startLetter.toLowerCase (es).equals (startLetter)){
                error = error + "\n\t"+ isoLangs[i] + " " + localeString;
            }
        }

        if (error.length () > 0){
            throw new Exception ("\nFollowing language names starts with upper-case letter: "
                    + error + "\nLower-case expected!");
        }

    }
}
