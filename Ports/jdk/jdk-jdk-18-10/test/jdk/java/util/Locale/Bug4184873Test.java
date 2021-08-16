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
    @test
    @summary test that locale invariants are preserved across serialization
    @library /java/text/testlib
    @run main Bug4184873Test
    @bug 4184873
*/
/*
 *
 *
 * (C) Copyright IBM Corp. 1996 - 1999 - All Rights Reserved
 *
 * Portions copyright (c) 2007 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies. Please refer to the file "copyright.html"
 * for further important copyright and licensing information.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

import java.util.*;
import java.io.*;

/**
 *  A Locale can never contain the following language codes: he, yi or id.
 */
public class Bug4184873Test extends IntlTest {
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("prepTest")) {
            prepTest();
        } else {
            new Bug4184873Test().run(args);
        }
    }

    public void testIt() throws Exception {
        verify("he");
        verify("yi");
        verify("id");
    }

    private void verify(String lang) {
        try {
            ObjectInputStream in = getStream(lang);
            if (in != null) {
                final Locale loc = (Locale)in.readObject();
                final Locale expected = new Locale(lang, "XX");
                if (!(expected.equals(loc))) {
                    errln("Locale didn't maintain invariants for: "+lang);
                    errln("         got: "+loc);
                    errln("    excpeted: "+expected);
                } else {
                    logln("Locale "+lang+" worked");
                }
                in.close();
            }
        } catch (Exception e) {
            errln(e.toString());
        }
    }

    private ObjectInputStream getStream(String lang) {
        try {
            final File f = new File(System.getProperty("test.src", "."), "Bug4184873_"+lang);
            return new ObjectInputStream(new FileInputStream(f));
        } catch (Exception e) {
            errln(e.toString());
            return null;
        }
    }

    /**
     * Create serialized output files of the test locales.  After they are created, these test
     * files should be corrupted (by hand) to contain invalid locale name values.
     */
    private static void prepTest() {
        outputLocale("he");
        outputLocale("yi");
        outputLocale("id");
    }

    private static void outputLocale(String lang) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(
                    new FileOutputStream("Bug4184873_"+lang));
            out.writeObject(new Locale(lang, "XX"));
            out.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

}
