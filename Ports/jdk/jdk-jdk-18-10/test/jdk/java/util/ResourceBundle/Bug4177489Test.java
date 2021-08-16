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
    @test
    @summary test Resource Bundle for bug 4177489
    @build Bug4177489_Resource Bug4177489_Resource_jf
    @run main Bug4177489Test
    @bug 4177489
*/
/*
 *
 *
 * (C) Copyright IBM Corp. 1999 - All Rights Reserved
 *
 * The original version of this source code and documentation is
 * copyrighted and owned by IBM. These materials are provided
 * under terms of a License Agreement between IBM and Sun.
 * This technology is protected by multiple US and International
 * patents. This notice and attribution to IBM may not be removed.
 *
 */

import java.util.*;
import java.io.*;

public class Bug4177489Test extends RBTestFmwk {
    public static void main(String[] args) throws Exception {
        new Bug4177489Test().run(args);
    }

    public void testIt() throws Exception {
        ResourceBundle rb = ResourceBundle.getBundle( "Bug4177489_Resource" );
        Locale l = rb.getLocale();
        if (l.toString().length() > 0) {
            errln("ResourceBundle didn't handle resource class name with '_' in it.");
        }

        Locale loc = new Locale("jf", "");
        ResourceBundle rb2 = ResourceBundle.getBundle( "Bug4177489_Resource", loc );
        if (!loc.equals(rb2.getLocale())) {
            errln("ResourceBundle didn't return proper locale name:"+rb2.getLocale());
        }

        loc = new Locale("jf", "JF");
        ResourceBundle rb3 = ResourceBundle.getBundle("Bug4177489_Resource", loc);
        if (!loc.equals(rb3.getLocale())) {
            errln("ResourceBundle didn't return proper locale name for property bundle:"+rb3.getLocale());
        }
    }
}
